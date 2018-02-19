package terrains;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.lwjgl.util.vector.Vector3f;

import renderEngine.Loader;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.Point2Df;
import toolbox.Point2Di;

public class TerrainLODGrid {
	
	private static final Logger LOGGER = Logger.getLogger(TerrainLODGrid.class.getName());
	
	private final Map<Point2Di, ConcurrentNavigableMap<Integer, Terrain>> grid;
	private final NavigableMap<Float, Integer> distanceToLODLevel;
	private final Map<Integer, Float> lodLevelToVertsPerUnit;
	
	private final float patchSize;
	private final float xTiles;
	private final float zTiles;
	private final Vector3f translation;
	private final Loader loader;
	private final TerrainTexturePack texturePack;
	private final TerrainTexture blendMap;
	private final IHeightGenerator heightMap;
	private final BiomesMap biomesMap;
	private final Point2Df domainLowerLeftLimit;
	private final Point2Df domainUpperRightLimit;
	private final Optional<ExecutorService> pool;
	
	private final int cellSearchRange;
	
	private List<Terrain> proximityTerrainsCache;
	private Vector3f lastRetrievalPosition;
	private boolean terrainsAdded;

	public TerrainLODGrid(NavigableMap<Float, Integer> distanceToLODLevel,
			Map<Integer, Float> lodLevelToVertsPerUnit, float patchSize, float xTiles, float zTiles,
			Vector3f translation, Loader loader, TerrainTexturePack texturePack,
			TerrainTexture blendMap, IHeightGenerator heightMap, BiomesMap biomesMap,
			Point2Df domainLowerLeftLimit, Point2Df domainUpperRightLimit,
			Optional<ExecutorService> pool) {

		if(distanceToLODLevel == null || distanceToLODLevel.isEmpty()) {
			throw new IllegalArgumentException("LOD ranges were not specified.");
		}
		
		if(domainUpperRightLimit.getX() <= domainLowerLeftLimit.getX() ||
				domainUpperRightLimit.getZ() >= domainLowerLeftLimit.getZ()) {
			throw new IllegalArgumentException("Invalid domain definition.");
		}
		
		if(pool == null) {
			throw new IllegalArgumentException("Thread pool was null.");
		}
		
		if(distanceToLODLevel == null || 
				lodLevelToVertsPerUnit == null || 	
				!(distanceToLODLevel.values().containsAll(lodLevelToVertsPerUnit.keySet()) && 
				lodLevelToVertsPerUnit.keySet().containsAll(distanceToLODLevel.values()))) {
			throw new IllegalArgumentException("LOD levels are not well defined.");
		}
		
		boolean hasNegativeLodId = distanceToLODLevel.values().stream().anyMatch(lod -> lod < 0);
		if(hasNegativeLodId) {
			throw new IllegalArgumentException("LOD labels need to be non-negative integers.");
		}
		
		this.grid = new ConcurrentHashMap<>();
		this.distanceToLODLevel = distanceToLODLevel;
		this.lodLevelToVertsPerUnit = lodLevelToVertsPerUnit;
		this.patchSize = patchSize;
		this.xTiles = xTiles;
		this.zTiles = zTiles;
		this.translation = translation;
		this.loader = loader;
		this.texturePack = texturePack;
		this.blendMap = blendMap;
		this.heightMap = heightMap;
		this.biomesMap = biomesMap;
		this.domainLowerLeftLimit = domainLowerLeftLimit;
		this.domainUpperRightLimit = domainUpperRightLimit;
		this.pool = pool;
		
		this.cellSearchRange = cellSearchRange();
		
		generateTerrains();
	}
	
	private void generateTerrains() {
		LOGGER.info("Generating base terrain patches...");
		
		Point2Di start = index(domainLowerLeftLimit);
		
		int xPatches = (int) Math.ceil((domainUpperRightLimit.getX() - domainLowerLeftLimit.getX()) / patchSize);
		int zPatches = (int) Math.ceil((domainLowerLeftLimit.getZ() - domainUpperRightLimit.getZ()) / patchSize);
		
		Point2Di end = new Point2Di(start.getX() + xPatches - 1, start.getZ() - (zPatches - 1));
		
		if(end.getX() - start.getX() < 0 || start.getZ() - end.getZ() < 0) {
			LOGGER.warning("Number of terrains to generate is 0.");
		}
		
		List<Integer> revSortedLevels = distanceToLODLevel
				.values()
				.stream()
				.sorted((a, b) -> Integer.compare(b, a))
				.collect(Collectors.toList());
		
		for(int level : revSortedLevels) { // generate lower details first (lower LOD ID -> higher detail)
			for(int z = start.getZ(); z >= end.getZ(); z--) {
				for(int x = start.getX(); x <= end.getX(); x++) {
					submitPatchForGeneration(x, z, level);
				}
			}
		}
		
		LOGGER.info(pool.isPresent() ?
				"All base terrain patches submitted for generation." :
				"Base terrain patches generated.");
	}
	
	private void submitPatchForGeneration(int x, int z, int level) {
		float xUpperLeft = x * patchSize;
		float zUpperLeft = (z - 1) * patchSize;
		
		Point2Di gridPoint = new Point2Di(x, z);
		
		float vertsPerUnit = lodLevelToVertsPerUnit.get(level);
		Runnable generationTask = new Runnable() {
			@Override
			public void run() {
				Terrain patch = generatePatch(xUpperLeft, zUpperLeft, patchSize, vertsPerUnit);

				ConcurrentNavigableMap<Integer, Terrain> terrainsAtPoint = grid.get(gridPoint);

				if(terrainsAtPoint == null) {
					terrainsAtPoint = new ConcurrentSkipListMap<>();
					grid.put(gridPoint, terrainsAtPoint);
				}

				terrainsAtPoint.put(level, patch);

				setTerrainsAdded(true);
				LOGGER.finest("Added terrain patch at (" + gridPoint.getX() + ", " +
				gridPoint.getZ() + ") with LOD " + level + ".");
			}
		};
		
		LOGGER.finest(pool.isPresent() ? "Submitting patch for generation." : "Generating patch.");
		
		if(pool.isPresent()) {
			pool.get().submit(generationTask);
		} else {
			generationTask.run();
		}
	}
	
	private Point2Di index(Point2Df point) {
		int x = (int) (point.getX() / patchSize);
		int z = (int) (point.getZ() / patchSize);
		return new Point2Di(x, z);
	}

	private Terrain generatePatch(float xUpperLeft, float zUpperLeft, float size, float vertsPerUnit) {
		return new Terrain(xUpperLeft, zUpperLeft, translation, size, size, vertsPerUnit,
				xTiles, zTiles, texturePack, blendMap, heightMap, biomesMap);
	}
	
	public void addLODAtTrajectory(List<Vector3f> trajectory, int level, float drawDistance,
			float vertsPerUnit) {
		lodLevelToVertsPerUnit.put(level, vertsPerUnit);
		distanceToLODLevel.put(drawDistance, level);
		
		Set<Point2Di> newLodPoints = cellsWithTrajectory(trajectory);
		
		LOGGER.info("Generating additional terrain patches...");
		
		for(Point2Di gridPoint : newLodPoints) {
			submitPatchForGeneration(gridPoint.getX(), gridPoint.getZ(), level);
		}
		
		LOGGER.info(pool.isPresent() ?
				"All additional terrain patches submitted for generation." :
				"Additional terrain patches generated.");
	}
	
	private Set<Point2Di> cellsWithTrajectory(List<Vector3f> trajectory) {
		Set<Point2Di> cells = new HashSet<>();
		
		Point2Di ll = index(domainLowerLeftLimit); // lower left grid point (cell)
		Point2Di ur = index(domainUpperRightLimit); // upper right grid point (cell)
		
		for(Vector3f tp : trajectory) { // tp - trajectory point
			Point2Di cell = index(new Point2Df(tp.x, tp.z));
			
			if(cell.getX() < ll.getX() || cell.getX() > ur.getX() ||
					cell.getZ() > ll.getZ() || cell.getZ() < ur.getZ()) {
				continue;
			}
			
			cells.add(cell);
		}
		
		return cells;
	}
	
	public List<Terrain> proximityTerrains(Vector3f position, float tolerance) {
		if(proximityTerrainsCache == null ||
				lastRetrievalPosition == null ||
				terrainsAdded ||
				Vector3f.sub(lastRetrievalPosition, position, null).lengthSquared() >= tolerance * tolerance) {
			LOGGER.finer("Recalculating terrain patches.");
			setTerrainsAdded(false);
			lastRetrievalPosition = new Vector3f(position);
			proximityTerrainsCache = calcProximityTerrains(position);
		}

		return proximityTerrainsCache;
	}
	
	private synchronized void setTerrainsAdded(boolean terrainsAdded) {
		this.terrainsAdded = terrainsAdded;
		LOGGER.finer("Terrains added: " + terrainsAdded + "; need refresh.");
	}

	private List<Terrain> calcProximityTerrains(Vector3f position) {
		int x = (int) (position.x / patchSize);
		int z = (int) (position.z / patchSize);
		
		List<Terrain> terrainPatches = new ArrayList<>();
		
		for(int row = z - cellSearchRange; row <= z + cellSearchRange; row++) {
			for(int col = x - cellSearchRange; col <= x + cellSearchRange; col++) {
				Point2Di gridPoint = new Point2Di(col, row);

				ConcurrentNavigableMap<Integer, Terrain> terrainsAtPoint = grid.get(gridPoint);
				 
				// no terrains are generated at this point yet, or they will not be at all
				if(terrainsAtPoint == null) {
					continue;
				}
				
				int lodLevel = determineLOD(gridPoint, position);
				
				// terrain is too far, no suitable LOD
				if(lodLevel == -1) {
					continue;
				}

				Entry<Integer, Terrain> terrainWithEqualOrLesserLOD = terrainsAtPoint.ceilingEntry(lodLevel);
				
				// no terrain with specified or lesser LOD is generated
				if(terrainWithEqualOrLesserLOD == null) {
					continue;
				}
				
				Terrain terrainPatch = terrainWithEqualOrLesserLOD.getValue();
				
				if(terrainPatch.getModel() == null) {
					terrainPatch.setModel(loader);
				}
				
				terrainPatches.add(terrainPatch);
			}
		}

		return terrainPatches;
	}
	
	private int determineLOD(Point2Di gridPoint, Vector3f position) {
		float gridX = (gridPoint.getX() + 0.5f) * patchSize;
		float gridZ = (gridPoint.getZ() - 0.5f) * patchSize;
		float distanceFromCenter = (float) Math.hypot(gridX - position.x, gridZ - position.z);
		
		Map.Entry<Float, Integer> lodEntry = distanceToLODLevel.ceilingEntry(distanceFromCenter);
		return lodEntry == null ? -1 : lodEntry.getValue();
	}

	private int cellSearchRange() {
		float renderDistance = distanceToLODLevel.lastKey();
		return (int) Math.ceil(renderDistance / patchSize);
	}

}
