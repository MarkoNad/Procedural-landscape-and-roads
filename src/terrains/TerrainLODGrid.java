package terrains;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import renderEngine.Loader;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.Point2Df;
import toolbox.Point2Di;

public class TerrainLODGrid {
	
	private static final Logger LOGGER = Logger.getLogger(TerrainLODGrid.class.getName());
	
	private final Map<Point2Di, Map<Integer, Terrain>> grid;
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
		float totalWidth = domainUpperRightLimit.getX() - domainLowerLeftLimit.getX();
		float totalDepth = domainLowerLeftLimit.getZ() - domainUpperRightLimit.getZ();
		
		int xPatches = (int) (totalWidth / patchSize);
		int zPatches = (int) (totalDepth / patchSize);
		
		System.out.println("x patches: " + xPatches);
		System.out.println("z patches: " + zPatches);
		
		if(xPatches == 0 || zPatches == 0) {
			LOGGER.warning("Number of terrains to generate is 0.");
		}
		
		LOGGER.info("Generating terrain...");
		
		for(int row = 0; row < zPatches; row++) {
			for(int col = 0; col < xPatches; col++) {
				float xUpperLeft = col * patchSize;
				float zUpperLeft = -(row + 1) * patchSize;
				
				Point2Di gridPoint = new Point2Di(col, -row);
				
				for(int level : distanceToLODLevel.values()) {
					float vertsPerUnit = lodLevelToVertsPerUnit.get(level);
					System.out.println("row: " + row + ", col: " + col + ", level: " + level + ", vpu: " + vertsPerUnit);
					
					Runnable generationTask = new Runnable() {
						@Override
						public void run() {
							System.out.println("A");

							Terrain patch = generatePatch(xUpperLeft, zUpperLeft, patchSize, vertsPerUnit);
							System.out.println("B");

							Map<Integer, Terrain> terrainsAtPoint = grid.get(gridPoint);
							System.out.println("C");
							
							if(terrainsAtPoint == null) {
								terrainsAtPoint = new ConcurrentHashMap<>();
								grid.put(gridPoint, terrainsAtPoint);
							}
							System.out.println("D");
							
							terrainsAtPoint.put(level, patch);
							System.out.println("E");
							
							setTerrainsAdded(true);
							System.out.println("F");
							//LOGGER.finer("Added patch at (" + gridPoint.getX() + ", " + gridPoint.getZ() + ").");
						}
					};
					
					LOGGER.finer(pool.isPresent() ? "Submitting patch for generation." : "Generating patch.");
					
					if(pool.isPresent()) {
						pool.get().submit(generationTask);
					} else {
						generationTask.run();
					}
				}
			}
		}
		
		LOGGER.info("Terrain generated / all patches submitted for generation.");
	}

	private Terrain generatePatch(float xUpperLeft, float zUpperLeft, float size, float vertsPerUnit) {
//		return new Terrain(xUpperLeft, zUpperLeft, translation, size, size, vertsPerUnit,
//				xTiles, zTiles, loader, texturePack, blendMap, heightMap, biomesMap);
		return new Terrain(xUpperLeft, zUpperLeft, translation, size, size, vertsPerUnit,
				xTiles, zTiles, texturePack, blendMap, heightMap, biomesMap);
	}
	
	public List<Terrain> proximityTerrains(Vector3f position, float tolerance) {
		if(proximityTerrainsCache == null ||
				lastRetrievalPosition == null ||
				terrainsAdded ||
				Vector3f.sub(lastRetrievalPosition, position, null).lengthSquared() >= tolerance * tolerance) {
			LOGGER.fine("Recalculating terrain patches.");
			setTerrainsAdded(false);
			lastRetrievalPosition = new Vector3f(position);
			proximityTerrainsCache = calcProximityTerrains(position);
		}
		
		System.out.println("grid size: " + grid.size());

		return proximityTerrainsCache;
	}
	
	private synchronized void setTerrainsAdded(boolean terrainsAdded) {
		this.terrainsAdded = terrainsAdded;
		LOGGER.finer("Terrains added: " + terrainsAdded);
	}

	private List<Terrain> calcProximityTerrains(Vector3f position) {
		LOGGER.finest("Calculating proximity terrains.");
		
		int x = (int) (position.x / patchSize);
		int z = (int) (position.z / patchSize);
		
		List<Terrain> terrainPatches = new ArrayList<>();
		
		for(int row = z - cellSearchRange; row <= z + cellSearchRange; row++) {
			for(int col = x - cellSearchRange; col <= x + cellSearchRange; col++) {
				Point2Di gridPoint = new Point2Di(col, row);
				//LOGGER.finest("Grid point to retrieve: " + gridPoint);
				
				Map<Integer, Terrain> terrainsAtPoint = grid.get(gridPoint);
				 
				// no terrains are generated at this point yet, or they will not be at all
				if(terrainsAtPoint == null) {
					//LOGGER.finest("No terrains at " + gridPoint);
					continue;
				}
				 
				int lodLevel = determineLOD(gridPoint, position);
				if(lodLevel == -1) {
					continue;
				}
				
				Terrain terrainPatch = terrainsAtPoint.get(lodLevel);
				
				// terrain is too far, no suitable LOD
				if(terrainPatch == null) {
					//LOGGER.warning("Attempted to reach a too far terrain patch.");
					continue;
				}
				
				if(terrainPatch.getModel() == null) {
					terrainPatch.setModel(loader);
				}
				
				terrainPatches.add(terrainPatch);
				//LOGGER.finest("Added to terrain patches.");
			}
		}
		
		//LOGGER.finest("Returning terrains: " + terrainPatches.size());
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
