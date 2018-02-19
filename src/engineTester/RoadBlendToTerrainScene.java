package engineTester;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;

import controller.LODGrid;
import entities.Camera;
import entities.Entity;
import entities.FloatingCamera;
import entities.Light;
import models.RawModel;
import models.TexturedModel;
import models.TexturedModelComp;
import objConverter.ModelData;
import objConverter.OBJFileLoader;
import renderEngine.DisplayManager;
import renderEngine.Loader;
import renderEngine.MasterRenderer;
import roads.Road;
import search.AStar;
import search.IProblem;
import search.Node;
import terrains.BiomesMap;
import terrains.IHeightGenerator;
import terrains.NoiseMap;
import terrains.SimplexHeightGenerator;
import terrains.Terrain;
import terrains.TerrainLODGrid;
import terrains.TreePlacer;
import terrains.TreeType;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.Globals;
import toolbox.Point2Df;
import toolbox.PoissonDiskSampler;
import toolbox.QueueProduct;
import toolbox.Range;
import toolbox.TriFunction;

public class RoadBlendToTerrainScene {
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	public static void main(String[] args) {
		DisplayManager.createDisplay();
		
		Loader loader = new Loader();
		Light light = new Light(new Vector3f(3000, 2000, 2000), new Vector3f(1, 1, 1));
		MasterRenderer renderer = new MasterRenderer();

		TexturedModel firLOD1 = load("fir_lod1", "fir_lod1", loader);
		TexturedModel chestnutLOD1 = load("chestnut_lod1", "chestnut_lod1", loader);
		TexturedModel chestnutTreetop = load("chestnut_treetop", "chestnut_treetop", loader);
		TexturedModel chestnutTrunk = load("chestnut_trunk", "chestnut_trunk", loader);
		TexturedModel firTreetop = load("fir_treetop", "fir_treetop", loader);
		TexturedModel firTrunk = load("fir_trunk", "fir_trunk", loader);
		TexturedModel fern = load("fern", "fern", loader);
		TexturedModel cube = load("cube", "cube", loader);

		chestnutTreetop.getTexture().setHasTransparency(true);
		firTreetop.getTexture().setHasTransparency(true);
		firLOD1.getTexture().setHasTransparency(true);
		chestnutLOD1.getTexture().setHasTransparency(true);
		fern.getTexture().setHasTransparency(true);
		fern.getTexture().setUsesFakeLighting(true);
		
		TexturedModelComp firLOD1Comp = new TexturedModelComp(firLOD1);
		TexturedModelComp chestnutLOD1Comp = new TexturedModelComp(chestnutLOD1);
		TexturedModelComp chestnutLOD0Comp = new TexturedModelComp(chestnutTreetop, chestnutTrunk);
		TexturedModelComp firLOD0Comp = new TexturedModelComp(firTrunk, firTreetop);

		Map<TexturedModelComp, Float> scaleForModel = new HashMap<>();
		scaleForModel.put(firLOD0Comp, 10.0f);
		scaleForModel.put(firLOD1Comp, 10.0f);
		scaleForModel.put(chestnutLOD0Comp, 15.0f);
		scaleForModel.put(chestnutLOD1Comp, 15.0f);
		
		NavigableMap<Float, TexturedModelComp> chestnutLods = new TreeMap<>();
		chestnutLods.put(200f, chestnutLOD0Comp);
		chestnutLods.put(2000f, chestnutLOD1Comp);

		NavigableMap<Float, TexturedModelComp> firLods = new TreeMap<>();
		firLods.put(200f, firLOD0Comp);
		firLods.put(2000f, firLOD1Comp);
		
		Map<TreeType, NavigableMap<Float, TexturedModelComp>> lodLevelsForType = new HashMap<>();
		lodLevelsForType.put(TreeType.OAK, chestnutLods);
		lodLevelsForType.put(TreeType.PINE, firLods);
		
		// terrain setup
		NavigableMap<Float, Integer> distanceToLODLevel = new TreeMap<>();
		distanceToLODLevel.put(2000f, 0);
		distanceToLODLevel.put(5000f, 1);
		distanceToLODLevel.put(20000f, 2);
		
		Map<Integer, Float> lodLevelToVertsPerUnit = new HashMap<>();
		lodLevelToVertsPerUnit.put(0, 0.2f); // distance 5
		lodLevelToVertsPerUnit.put(1, 0.025f); // distance 40
		lodLevelToVertsPerUnit.put(2, 0.0125f);
		
		
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("cliff3"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("snow"));
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));
		
		SimplexHeightGenerator heightGenerator = new SimplexHeightGenerator(1, 9000f, 0.0001f, 2f, 5, 0.4f, 0.2f, 5f);
		List<Range> textureRanges = Arrays.asList(new Range(0, 700), new Range(700, 3000), new Range(3000, heightGenerator.getMaxHeight()));
		TriFunction<Float, Float, Float, Float> textureVariation = (x, h, z) -> {
			NoiseMap texVariationMap = new NoiseMap(450f, 0.0005f, 0);
			final float maxHeight = textureRanges.get(textureRanges.size() - 1).getEnd();
			return (float) (texVariationMap.getNoise(x, z) * Math.pow(4 * (h + 1000) / maxHeight, 1.5));
		};
		BiomesMap biomesMap = new BiomesMap(heightGenerator, textureRanges, 500f, textureVariation);
		
		List<Vector3f> roadWaypoints = findPath(heightGenerator);
		final float segmentLen = 1f;
		Road roadRawModel = new Road(loader, roadWaypoints, 10, 20, segmentLen, 0.02f, heightGenerator, false);
		Entity road = setupRoad(loader, heightGenerator, roadWaypoints, roadRawModel);
		
		final float modifierSegementLen = 1f;
		List<Vector3f> modifierTrajectory = Road.generateTrajectory(roadWaypoints, modifierSegementLen, heightGenerator);
		heightGenerator.updateHeight(modifierTrajectory, x -> x <= 10f ? 1f : 1 - Math.min((x - 10f) / 5f, 1f), 15f);
		
		float patchSize = 1000f;
		float texWidth = 5f;
		float texDepth = 5f;
		Point2Df domainLowerLeftLimit = new Point2Df(5000f, -10000f);
		Point2Df domainUpperRightLimit = new Point2Df(10_000f, -15_000f);
		TerrainLODGrid terrainLODGrid = new TerrainLODGrid(distanceToLODLevel, lodLevelToVertsPerUnit, patchSize, texWidth, texDepth,
				new Vector3f(), loader, texturePack, blendMap, heightGenerator, biomesMap, domainLowerLeftLimit, domainUpperRightLimit,
				Optional.of(Globals.getThreadPool()));

		BiFunction<Float, Float, Float> distribution = (x, z) -> (float)Math.pow(1 - biomesMap.getTreeDensity(x, z), 2.0);
		PoissonDiskSampler sampler = new PoissonDiskSampler(0, 0, 20000, -20000, 10f, 50f, distribution, 1, 30, 10_000_000, new Point2D.Float(0f, 0f));
		
		TreePlacer placer = new TreePlacer(heightGenerator, biomesMap, sampler);
		ExecutorService pool = Globals.getThreadPool();
		BlockingQueue<QueueProduct<Map<TreeType, List<Vector3f>>>> locationsPerType = placer.computeLocationsInBackground(pool);

		LODGrid grid = new LODGrid(2000, scaleForModel, lodLevelsForType);
		grid.addToGrid(locationsPerType, pool);

		//Camera camera = new FPSCamera(new Vector3f(100.0f, 0.0f, -5000.0f), heightGenerator, 1f, 2f, 50f, 50f, 12.5f);
		Camera camera = new FloatingCamera(new Vector3f(100.0f, 1000.0f, -5000.0f));

		List<Entity> nmEntites = new ArrayList<>();

		TexturedModel barrel = loadNM(loader, "barrel", "barrel", "barrelNormal");
		TexturedModel crate = loadNM(loader, "crate", "crate", "crateNormal");
		TexturedModel boulder = loadNM(loader, "boulder", "boulder", "boulderNormal");
		
		barrel.getTexture().setShineDamper(10);
		barrel.getTexture().setReflectivity(0.5f);
		barrel.getTexture().setUsesFakeLighting(false);
		crate.getTexture().setShineDamper(10);
		crate.getTexture().setReflectivity(0.5f);
		crate.getTexture().setUsesFakeLighting(false);
		boulder.getTexture().setShineDamper(10);
		boulder.getTexture().setReflectivity(0.5f);
		boulder.getTexture().setUsesFakeLighting(false);
		
		Entity barrelEntity = new Entity(barrel, new Vector3f(-10.0f, 0.0f, 0.0f), 0, 0, 0, 1f);
		Entity crateEntity = new Entity(crate, new Vector3f(-20.0f, 0.0f, 0.0f), 0, 0, 0, 0.05f);
		Entity boulderEntity = new Entity(boulder, new Vector3f(-30.0f, 0.0f, 0.0f), 0, 0, 0, 1f);
		
		Entity barrelEntity2 = new Entity(barrel, new Vector3f(100.0f, heightGenerator.getHeightApprox(100f, -5000f) + 0.5f, -5000.0f), 0, 0, 0, 1f);
		Entity crateEntity2 = new Entity(crate, new Vector3f(105.0f, heightGenerator.getHeightApprox(105f, -5000f) + 0.5f, -5000.0f), 0, 0, 0, 1f);
		Entity chestnutEntityTop = new Entity(chestnutTreetop, new Vector3f(110.0f, heightGenerator.getHeightApprox(110f, -5000f), -5000.0f), 0, 0, 0, 1f);
		Entity chestnutEntityTrunk = new Entity(chestnutTrunk, new Vector3f(110.0f, heightGenerator.getHeightApprox(110f, -5000f), -5000.0f), 0, 0, 0, 1f);
		
		Entity cubeEntity = new Entity(cube, new Vector3f(95.0f, heightGenerator.getHeightApprox(95f, -5000f) + 0.5f, -5000.0f), 0, 0, 0, 1f);
		Entity cubeEntity2 = new Entity(cube, new Vector3f(95.01f, heightGenerator.getHeightApprox(95f, -5000.01f) + 1.5f, -5000.0f), 0, 0.01f, 0, 1f);
		
		nmEntites.add(barrelEntity);
		nmEntites.add(crateEntity);
		nmEntites.add(boulderEntity);
		
		nmEntites.add(barrelEntity2);
		nmEntites.add(crateEntity2);
		
		final float terrainLODTolerance = 200f;
		
		while(!Display.isCloseRequested()) {
			camera.update();
			
			barrelEntity.increaseRotation(0, 0.5f, 0);
			crateEntity.increaseRotation(0, 0.5f, 0);
			boulderEntity.increaseRotation(0, 0.5f, 0);
			barrelEntity2.increaseRotation(0, 0.5f, 0);
			crateEntity2.increaseRotation(0, 0.5f, 0);

			List<Entity> entities = grid.proximityEntities(camera.getPosition());
			List<Terrain> terrains = terrainLODGrid.proximityTerrains(camera.getPosition(), terrainLODTolerance);
			roadWaypoints.forEach(p -> entities.add(new Entity(chestnutTrunk, new Vector3f(p.x, heightGenerator.getHeightApprox(p.x, p.z), p.z), 0f, 0f, 0f, 2f)));
			entities.add(chestnutEntityTrunk);
			entities.add(chestnutEntityTop);
			entities.add(cubeEntity);
			entities.add(cubeEntity2);
			
			entities.forEach(e -> renderer.processEntity(e));
			nmEntites.forEach(e -> renderer.processNMEntity(e));
			terrains.forEach(t -> renderer.processTerrain(t));
			renderer.processEntity(road);
			renderer.render(light, camera);
			
			DisplayManager.updateDisplay();
		}
		
		renderer.cleanUp();
		loader.cleanUp();
		DisplayManager.closeDisplay();
	}
	
	private static Entity setupRoad(Loader loader, IHeightGenerator heightGenerator,
			List<Vector3f> waypoints, Road road) {
		//Road road = new Road(loader, waypoints, heightGenerator, 250, 200, 50f);
		TexturedModel roadTM = new TexturedModel(road.getModel(), new ModelTexture(loader.loadTexture("road")));
		roadTM.getTexture().setHasTransparency(true);
		return new Entity(roadTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
	}

	private static List<Vector3f> findPath(IHeightGenerator heightGenerator) {
		long start = System.nanoTime();
		
		IProblem<Point2Df> searchProblem = new IProblem<Point2Df>() {
			private Point2Df end = new Point2Df(20000f, -20000f);
			private final float step = 500f;
//			private final int succCount = 8;
			private final float tolerance = 5000f;
			
			@Override
			public Iterable<Point2Df> getSuccessors(Point2Df state) {
//				List<Point2Df> successors = new ArrayList<>();
//				
//				for(int i = 0; i < succCount; i++) {
//					double angle = 2.0 * Math.PI * i / succCount;
//					System.out.println(angle);
//					
//					float deltaX = (float) (step * Math.cos(angle));
//					float deltaZ = (float) (step * Math.sin(angle));
//					
//					successors.add(new Point2Df(state.getX() + deltaX, state.getZ() + deltaZ));
//				}
//				
//				return successors;
				
				return Arrays.asList(
					new Point2Df(state.getX(), state.getZ() - step),
					new Point2Df(state.getX() + step, state.getZ() - step),
					new Point2Df(state.getX() + step, state.getZ()),
					new Point2Df(state.getX() + step, state.getZ() + step),
					new Point2Df(state.getX(), state.getZ() + step),
					new Point2Df(state.getX() - step, state.getZ() + step),
					new Point2Df(state.getX() - step, state.getZ()),
					new Point2Df(state.getX() - step, state.getZ() - step)
				);
			}
	
			@Override
			public double getTransitionCost(Point2Df p1, Point2Df p2) {
				double totalCost = 0.0;

				double y1 = heightGenerator.getHeightApprox(p1.getX(), p1.getZ());
				double y2 = heightGenerator.getHeightApprox(p2.getX(), p2.getZ());
				double distance = Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2.0) + 
						Math.pow(y1 - y2, 2.0) + Math.pow(p1.getZ() - p2.getZ(), 2.0));
				double distanceCost = distance;
				
				Vector3f normal1 = heightGenerator.getNormalApprox(p1.getX(), p1.getZ());
				Vector3f normal2 = heightGenerator.getNormalApprox(p2.getX(), p2.getZ());
				double deltaSlope = Vector3f.angle(normal1, normal2);
				double deltaSlopeCost = deltaSlope * 1000.0;
				
				double slope = Vector3f.angle(Globals.Y_AXIS, normal2);
				double slopeCost = slope * 10_000.0;
				
				totalCost += distanceCost;
				totalCost += deltaSlopeCost;
				totalCost += slopeCost;
				
				return totalCost; 
			}
	
			@Override
			public boolean isGoal(Point2Df point) {
				//return state.equals(end);
				return Point2Df.distance(point, end) <= tolerance;
			}
	
			@Override
			public Point2Df getInitialState() {
				return new Point2Df(0, 0);
			}
			
		};
		
		AStar<Point2Df> astar = new AStar<>(searchProblem, s -> 0.0);
		Node<Point2Df> goal = astar.search();
		
		double duration = (System.nanoTime() - start) * 1e-9;
		LOGGER.log(Level.FINE, "Astar duration:" + duration);
		
		List<Vector3f> waypoints = new ArrayList<>();
		
		for(Point2Df point : goal.reconstructPath()) {
			float height = heightGenerator.getHeightApprox(point.getX(), point.getZ());;
			waypoints.add(new Vector3f(point.getX(), height, point.getZ()));
			LOGGER.log(Level.FINER, "A star point: " + point.toString());
		}
		
		return waypoints;
	}
	
	private static TexturedModel load(String objFile, String pngFile, Loader loader) {
		ModelData data = OBJFileLoader.loadOBJ(objFile, false, true);
		RawModel model = loader.loadToVAO(data.getVertices(), data.getTextureCoords(),
				data.getNormals(), data.getIndices());
		return new TexturedModel(model, new ModelTexture(loader.loadTexture(pngFile)));
	}
	
	private static TexturedModel loadNM(Loader loader, String modelFile, String textureFile, String normalMapFile) {
		ModelData data = OBJFileLoader.loadOBJ(modelFile, false, true);
		RawModel model = loader.loadToVAO(data.getVertices(), data.getTextureCoords(), data.getNormals(), data.getTangents(), data.getIndices());
		return new TexturedModel(model, new ModelTexture(loader.loadTexture(textureFile), loader.loadTexture(normalMapFile)));
	}
	
}
