package hr.fer.zemris.engine.demo;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;

import hr.fer.zemris.engine.entity.Camera;
import hr.fer.zemris.engine.entity.Entity;
import hr.fer.zemris.engine.entity.FloatingCamera;
import hr.fer.zemris.engine.entity.LODGrid;
import hr.fer.zemris.engine.entity.Light;
import hr.fer.zemris.engine.model.RawModel;
import hr.fer.zemris.engine.model.TexturedModel;
import hr.fer.zemris.engine.model.TexturedModelComp;
import hr.fer.zemris.engine.obj.ModelData;
import hr.fer.zemris.engine.obj.OBJFileLoader;
import hr.fer.zemris.engine.renderer.DisplayManager;
import hr.fer.zemris.engine.renderer.Loader;
import hr.fer.zemris.engine.renderer.MasterRenderer;
import hr.fer.zemris.engine.road.Pathfinder;
import hr.fer.zemris.engine.road.Road;
import hr.fer.zemris.engine.road.TunnelManager;
import hr.fer.zemris.engine.search.AStar;
import hr.fer.zemris.engine.terrain.BiomesMap;
import hr.fer.zemris.engine.terrain.IHeightMap;
import hr.fer.zemris.engine.terrain.ITerrain;
import hr.fer.zemris.engine.terrain.ImageHeightMap;
import hr.fer.zemris.engine.terrain.MutableHeightMap;
import hr.fer.zemris.engine.terrain.NoiseMap;
import hr.fer.zemris.engine.terrain.TerrainLODGrid;
import hr.fer.zemris.engine.terrain.TreePlacer;
import hr.fer.zemris.engine.terrain.TreeType;
import hr.fer.zemris.engine.texture.ModelTexture;
import hr.fer.zemris.engine.texture.TerrainTexture;
import hr.fer.zemris.engine.texture.TerrainTexturePack;
import hr.fer.zemris.engine.util.CatmullRomSpline3D;
import hr.fer.zemris.engine.util.Globals;
import hr.fer.zemris.engine.util.Point2Df;
import hr.fer.zemris.engine.util.Point2Di;
import hr.fer.zemris.engine.util.PoissonDiskSampler;
import hr.fer.zemris.engine.util.QueueProduct;
import hr.fer.zemris.engine.util.Range;
import hr.fer.zemris.engine.util.SamplerUtility.SamplingType;
import hr.fer.zemris.engine.util.TriFunction;

public class Medvednica {

	public static void main(String[] args) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		Globals.initializeThreadPool(3);
		
		DisplayManager.createDisplay();
		
		Loader loader = new Loader();
		Camera camera = new FloatingCamera(new Vector3f(9350.0f, 500.0f, 12000.0f), 20f, 100f, 500f, 300f, 12.5f);
		Light light = new Light(new Vector3f(6000, 10000, 25000), new Vector3f(1, 1, 1));
		MasterRenderer renderer = new MasterRenderer();

		TexturedModel firLOD1 = load("fir_lod1", "fir_lod1", loader);
		TexturedModel chestnutLOD1 = load("chestnut_lod1", "chestnut_lod1", loader);
		TexturedModel chestnutTreetop = load("chestnut_treetop", "chestnut_treetop", loader);
		TexturedModel chestnutTrunk = load("chestnut_trunk", "chestnut_trunk", loader);
		TexturedModel firTreetop = load("fir_treetop", "fir_treetop", loader);
		TexturedModel firTrunk = load("fir_trunk", "fir_trunk", loader);

		chestnutTreetop.getTexture().setHasTransparency(true);
		firTreetop.getTexture().setHasTransparency(true);
		firLOD1.getTexture().setHasTransparency(true);
		chestnutLOD1.getTexture().setHasTransparency(true);
		chestnutLOD1.getTexture().setUsesFakeLighting(true);
		
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
		chestnutLods.put(1100f, chestnutLOD1Comp);

		NavigableMap<Float, TexturedModelComp> firLods = new TreeMap<>();
		firLods.put(200f, firLOD0Comp);
		firLods.put(1100f, firLOD1Comp);
		
		Map<TreeType, NavigableMap<Float, TexturedModelComp>> lodLevelsForType = new HashMap<>();
		lodLevelsForType.put(TreeType.OAK, chestnutLods);
		lodLevelsForType.put(TreeType.PINE, firLods);
		
		// terrain setup
		float size = 12000.0f;
		
		BufferedImage heightImage = null;
		try {
			heightImage = ImageIO.read(new File("resources/medvednicaHeightMap12.png")); // 12 km x 12 km
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}

		double pixelDistance = size / 1080.0f; // 12 km / (1081 - 1 pixels)
		
		NavigableMap<Float, Integer> distanceToLODLevel = new TreeMap<>();
		distanceToLODLevel.put(3000f, 0);
		distanceToLODLevel.put(10000f, 1);
		distanceToLODLevel.put(20000f, 2);
		
		Map<Integer, Float> lodLevelToVertsPerUnit = new HashMap<>();
		lodLevelToVertsPerUnit.put(0, 0.15f); // distance 10
		lodLevelToVertsPerUnit.put(1, 0.025f); // distance 40
		lodLevelToVertsPerUnit.put(2, 0.0125f);
		
		
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("cliff3"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("snow"));
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));
		
		MutableHeightMap heightGenerator = new ImageHeightMap(heightImage, 135.0, 1041.0, pixelDistance);
		List<Range> textureRanges = Arrays.asList(new Range(0, 600), new Range(600, 900), new Range(900, heightGenerator.getMaxHeight()));
		NoiseMap texVariationMap = new NoiseMap(40f, 0.005f, 0);
		TriFunction<Float, Float, Float, Float> textureVariation = (x, h, z) -> texVariationMap.getNoise(x, z);
		BiomesMap biomesMap = new BiomesMap(heightGenerator, textureRanges, 50f, textureVariation, new Random(0));
		
		Point2Df domainLowerLeftLimit = new Point2Df(0f, size);
		Point2Df domainUpperRightLimit = new Point2Df(size, 0f);

		Pathfinder pathfinder = new Pathfinder(
				AStar<Point2Di>::new, // algorithm
				CatmullRomSpline3D::new, // spline
				new Point2Df(9350f, 11950f), // start,
				new Point2Df(4000f, 0f), // goal,
				domainLowerLeftLimit,
				domainUpperRightLimit,
				heightGenerator,
				15f, // cellSize
				false, // allowTunnels
				15f, // minimum tunnel depth
				10, // endpointOffset
				8, // maskOffset
				4500f, // tunnelInnerRadius
				6000f, // tunnelOuterRadius
				100, // tunnelCandidates
				true, // limitTunnelCandidates
				new Random(0), // random,
				3, // roadRange,
				0.09, // maxRoadSlopePercent,
				1.75, //maxRoadCurvature,
				1.0, // roadLengthMultiplier,
				80.0, // roadSlopeMultiplier,
				10.0, // roadCurvatureMultiplier,
				2.0, // roadSlopeExponent,
				3.0, // roadCurvatureExponent,
				0.25, // maxTunnelSlopePercent,
				1.75, // maxTunnelCurvature,
				10.0, // tunnelLengthMultiplier,
				200.0, // tunnelSlopeMultiplier,
				10.0, // tunnelCurvatureMultiplier,
				2.0, // tunnelSlopeExponent,
				3.0, // tunnelCurvatureExponent,
				SamplingType.FARTHEST // roadSamplingType
		);

		// configuration for AStar and GreedyBestFirstSearch comparison
//		Pathfinder pathfinder = new Pathfinder(
//			AStar<Point2Di>::new, // algorithm
////			GreedyBestFirstSearch<Point2Di>::new, // algorithm
//			CatmullRomSpline3D::new, // spline
//			new Point2Df(9350f, 11950f), // start,
//			new Point2Df(4000f, 0f), // goal,
//			domainLowerLeftLimit,
//			domainUpperRightLimit,
//			heightGenerator,
//			50f, // cellSize
//			false, // allowTunnels
//			15f, // minimum tunnel depth
//			10, // endpointOffset
//			8, // maskOffset
//			4500f, // tunnelInnerRadius
//			6000f, // tunnelOuterRadius
//			100, // tunnelCandidates
//			true, // limitTunnelCandidates
//			new Random(0), // random,
//			3, // roadRange,
//			0.1, // maxRoadSlopePercent,
//			1.5, //maxRoadCurvature,
//			1.0, // roadLengthMultiplier,
//			80.0, // 150.0, // roadSlopeMultiplier,
//			10.0, //30.0, // roadCurvatureMultiplier,
//			2.0, // roadSlopeExponent,
//			3.0, // roadCurvatureExponent,
//			0.25, // maxTunnelSlopePercent,
//			1.75, // maxTunnelCurvature,
//			10.0, // tunnelLengthMultiplier,
//			200.0, // tunnelSlopeMultiplier,
//			10.0, // tunnelCurvatureMultiplier,
//			2.0, // tunnelSlopeExponent,
//			3.0, // tunnelCurvatureExponent,
//			SamplingType.NEAREST_UNIQUE // roadSamplingType
//		);

		Optional<List<Vector3f>> roadTrajectory = pathfinder.findTrajectory(1f);
		Optional<Road> maybeRoad = roadTrajectory.map(trajectory -> new Road(loader, trajectory, 10, 12, 0.0f));
		Optional<Entity> maybeRoadEntity = maybeRoad.map(road -> setupRoad(loader, heightGenerator, road));

		// 14.2 is a bit more than 10 * sqrt(2), 10 is road width
		Function<Float, Float> influenceFn = x -> x <= 14.2f ? 1f : 1 - Math.min((x - 14.2f) / 9.2f, 1f);
		pathfinder.findModifierTrajectories(-0.05f).ifPresent(modifiers -> modifiers.forEach(m -> heightGenerator.updateHeight(m, influenceFn, 15f)));

		TerrainLODGrid terrainLODGrid = new TerrainLODGrid(distanceToLODLevel, lodLevelToVertsPerUnit, 500f, 5f, 5f,
				new Vector3f(), loader, texturePack, blendMap, heightGenerator, biomesMap, domainLowerLeftLimit, domainUpperRightLimit,
				Optional.of(Globals.getThreadPool()));

		BiFunction<Float, Float, Float> distribution = (x, z) -> (float)Math.pow(1 - biomesMap.getTreeDensity(x, z), 2.0);
		PoissonDiskSampler sampler = new PoissonDiskSampler(domainLowerLeftLimit.getX(), domainLowerLeftLimit.getZ(),
				domainUpperRightLimit.getX(), domainUpperRightLimit.getZ(), 10f, 15f, distribution, 1, 30, 10_000_000,
				new Point2D.Float(camera.getPosition().x, camera.getPosition().z));
		
		TreePlacer placer = new TreePlacer(heightGenerator, biomesMap, sampler);
		pathfinder.findModifierTrajectories(0.0f).ifPresent(ts -> ts.forEach(t -> placer.addNoTreeZone(t, 7.0f)));
		ExecutorService pool = Globals.getThreadPool();
		BlockingQueue<QueueProduct<Map<TreeType, List<Vector3f>>>> locationsPerType = placer.computeLocationsInBackground(pool);

		LODGrid grid = new LODGrid(2000, scaleForModel, lodLevelsForType);
		grid.addToGrid(locationsPerType, pool);

		Optional<List<Entity>> tunnelPartEntities = maybeRoad.map(road -> {
			TunnelManager tunnelManager = new TunnelManager(road, pathfinder.findTunnelsData().get(), 5, 1.0f, 50f,
					50f, 50f, 50f, 50f, 50f, "tunnel", "tunnel", "tunnel", "black", loader);
			return tunnelManager.getAllTunnelEntities();
		});

		while(!Display.isCloseRequested()) {
			camera.update();

			tunnelPartEntities.ifPresent(parts -> parts.forEach(p -> renderer.processEntity(p)));
			maybeRoadEntity.ifPresent(roadEntity -> renderer.processEntity(roadEntity));

			List<ITerrain> terrains = terrainLODGrid.proximityTerrains(camera.getPosition(), 200f);
			terrains.forEach(t -> renderer.processTerrain(t));
			
			List<Entity> entities = grid.proximityEntities(camera.getPosition());
			entities.forEach(e -> renderer.processEntity(e));
			
			renderer.render(light, camera);
			
			DisplayManager.updateDisplay();
		}
		
		renderer.cleanUp();
		loader.cleanUp();
		DisplayManager.closeDisplay();
	}
	
	private static Entity setupRoad(Loader loader, IHeightMap heightGenerator, Road road) {
		TexturedModel roadTM = new TexturedModel(road.getModel(), new ModelTexture(loader.loadTexture("road")));
		roadTM.getTexture().setHasTransparency(true);
		return new Entity(roadTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
	}

	private static TexturedModel load(String objFile, String pngFile, Loader loader) {
		ModelData data = OBJFileLoader.loadOBJ(objFile, false, true);
		RawModel model = loader.loadToVAO(data.getVertices(), data.getTextureCoords(),
				data.getNormals(), data.getIndices());
		return new TexturedModel(model, new ModelTexture(loader.loadTexture(pngFile)));
	}

}
