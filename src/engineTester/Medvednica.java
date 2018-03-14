package engineTester;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import roads.Pathfinder;
import roads.Road;
import roads.TunnelManager;
import search.AStar;
import terrains.BiomesMap;
import terrains.IHeightGenerator;
import terrains.ImageHeightMap;
import terrains.MutableHeightMap;
import terrains.Terrain;
import terrains.TerrainLODGrid;
import terrains.TreePlacer;
import terrains.TreeType;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.CatmullRomSpline3D;
import toolbox.Globals;
import toolbox.Point2Df;
import toolbox.Point2Di;
import toolbox.PoissonDiskSampler;
import toolbox.QueueProduct;
import toolbox.Range;
import toolbox.SamplerUtility.SamplingType;
import toolbox.TriFunction;

public class Medvednica {

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
		BufferedImage heightImage = null;
		try {
			//heightImage = ImageIO.read(new File("res/heightMap10.png"));
			//heightImage = ImageIO.read(new File("res/medvednicaHeightMap.png"));
			heightImage = ImageIO.read(new File("res/medvednicaHeightMap12.png"));
			//heightImage = ImageIO.read(new File("res/istra30.png"));
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		
//		double pixelDistance = 50.0; // TODO
//		double vertsPerUnit = 1.0 / pixelDistance; // TODO
		//double pixelDistance = 30.0;
		double pixelDistance = 12000.0f / 1080.0f;
		System.out.println("pixel distance: " + pixelDistance);
		double vertsPerUnit = 1.0 / pixelDistance * 1.5f;
		System.out.println("verts per unit: " + (float)vertsPerUnit);
		
		NavigableMap<Float, Integer> distanceToLODLevel = new TreeMap<>();
		distanceToLODLevel.put(3000f, 0);
		//distanceToLODLevel.put(10000f, 1);
		distanceToLODLevel.put(20000f, 2);
		
		Map<Integer, Float> lodLevelToVertsPerUnit = new HashMap<>();
		lodLevelToVertsPerUnit.put(0, 0.15f); // distance 10
		//lodLevelToVertsPerUnit.put(0, (float)vertsPerUnit);
		//lodLevelToVertsPerUnit.put(1, 0.025f); // distance 40
		lodLevelToVertsPerUnit.put(2, 0.0125f);
		
		
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("cliff3"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("snow"));
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));
		
		//SimplexHeightGenerator heightGenerator = new SimplexHeightGenerator(1, 9000f, 0.0001f, 2f, 5, 0.4f, 0.2f, 5f);
		MutableHeightMap heightGenerator = new ImageHeightMap(heightImage, 135.0, 1041.0, pixelDistance);
		List<Range> textureRanges = Arrays.asList(new Range(0, 600), new Range(600, 900), new Range(900, heightGenerator.getMaxHeight()));
//		TriFunction<Float, Float, Float, Float> textureVariation = (x, h, z) -> {
//			NoiseMap texVariationMap = new NoiseMap(450f, 0.0005f, 0);
//			final float maxHeight = textureRanges.get(textureRanges.size() - 1).getEnd();
//			return (float) (texVariationMap.getNoise(x, z) * Math.pow(4 * (h + 1000) / maxHeight, 1.5));
//		};
		TriFunction<Float, Float, Float, Float> textureVariation = (x, h, z) -> 0.0f;
		BiomesMap biomesMap = new BiomesMap(heightGenerator, textureRanges, 50f, textureVariation);
		
		float size = (float) (12000f);
		
		Point2Df domainLowerLeftLimit = new Point2Df(0f, size);
		Point2Df domainUpperRightLimit = new Point2Df(size, 0f);

//		Terrain terrain = new Terrain(0f, 0f, new Vector3f(), size, size, (float)vertsPerUnit,
//				5f, 5f, texturePack, blendMap, heightGenerator, biomesMap, loader);
		
		Pathfinder pathfinder = new Pathfinder(
				AStar<Point2Di>::new, // algorithm
				CatmullRomSpline3D::new, // spline
				new Point2Df(9350f, 11950f), // start,
				new Point2Df(4000f, 0f), // goal,
				domainLowerLeftLimit,
				domainUpperRightLimit,
				heightGenerator,
				20f, // cellSize
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
				0.1, // maxRoadSlopePercent,
				2.0, //1.75, //maxRoadCurvature,
				1.0, // roadLengthMultiplier,
				80.0, // 80.0, // roadSlopeMultiplier,
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
				SamplingType.NEAREST_UNIQUE // roadSamplingType
		);

//		Pathfinder pathfinder = new Pathfinder(
//				AStar<Point2Di>::new, // algorithm
//				CatmullRomSpline3D::new, // spline
//				new Point2Df(9350f, 11950f), // start,
//				new Point2Df(4000f, 0f), // goal,
//				domainLowerLeftLimit,
//				domainUpperRightLimit,
//				heightGenerator,
//				20f, // cellSize
//				false, // allowTunnels
//				15f, // minimum tunnel depth
//				10, // endpointOffset
//				8, // maskOffset
//				4500f, // tunnelInnerRadius
//				6000f, // tunnelOuterRadius
//				100, // tunnelCandidates
//				true, // limitTunnelCandidates
//				new Random(0), // random,
//				3, // roadRange,
//				0.1, // maxRoadSlopePercent,
//				2.0, //1.75, //maxRoadCurvature,
//				1.0, // roadLengthMultiplier,
//				80.0, // 80.0, // roadSlopeMultiplier,
//				10.0, // roadCurvatureMultiplier,
//				2.0, // roadSlopeExponent,
//				3.0, // roadCurvatureExponent,
//				0.25, // maxTunnelSlopePercent,
//				1.75, // maxTunnelCurvature,
//				10.0, // tunnelLengthMultiplier,
//				200.0, // tunnelSlopeMultiplier,
//				10.0, // tunnelCurvatureMultiplier,
//				2.0, // tunnelSlopeExponent,
//				3.0, // tunnelCurvatureExponent,
//				SamplingType.NEAREST_UNIQUE // roadSamplingType
//		);
		
//		Pathfinder pathfinder = new Pathfinder(
//				AStar<Point2Di>::new, // algorithm
//				CatmullRomSpline3D::new, // spline
//				new Point2Df(9350f, 11950f), // start,
//				new Point2Df(4000f, 0f), // goal,
//				domainLowerLeftLimit,
//				domainUpperRightLimit,
//				heightGenerator,
//				20f, // cellSize
//				false, // allowTunnels
//				15f, // minimum tunnel depth
//				10, // endpointOffset
//				8, // maskOffset
//				4500f, // tunnelInnerRadius
//				6000f, // tunnelOuterRadius
//				100, // tunnelCandidates
//				true, // limitTunnelCandidates
//				new Random(0), // random,
//				3, // roadRange,
//				0.2, // maxRoadSlopePercent,
//				1.75, //maxRoadCurvature,
//				1.0, // roadLengthMultiplier,
//				800.0, // 80.0, // roadSlopeMultiplier,
//				10.0, // roadCurvatureMultiplier,
//				2.0, // roadSlopeExponent,
//				3.0, // roadCurvatureExponent,
//				0.25, // maxTunnelSlopePercent,
//				1.75, // maxTunnelCurvature,
//				10.0, // tunnelLengthMultiplier,
//				200.0, // tunnelSlopeMultiplier,
//				10.0, // tunnelCurvatureMultiplier,
//				2.0, // tunnelSlopeExponent,
//				3.0, // tunnelCurvatureExponent,
//				SamplingType.NEAREST_UNIQUE // roadSamplingType
//		);
		
		Optional<List<Vector3f>> roadTrajectory = pathfinder.findTrajectory(1f);
		Optional<Road> maybeRoad = roadTrajectory.map(trajectory -> new Road(loader, trajectory, 10, 12, 0.0f));
		Optional<Entity> maybeRoadEntity = maybeRoad.map(road -> setupRoad(loader, heightGenerator, road));

		// 14.2 is a bit more than 10 * sqrt(2), 10 is road width
		Function<Float, Float> influenceFn = x -> x <= 14.2f ? 1f : 1 - Math.min((x - 14.2f) / 9.2f, 1f);
		pathfinder.findModifierTrajectories(-0.05f).ifPresent(modifiers -> modifiers.forEach(m -> heightGenerator.updateHeight(m, influenceFn, 15f)));
		
		float texWidth = 5f;
		float texDepth = 5f;
		float patchSize = 500f;
		TerrainLODGrid terrainLODGrid = new TerrainLODGrid(distanceToLODLevel, lodLevelToVertsPerUnit, patchSize, texWidth, texDepth,
				new Vector3f(), loader, texturePack, blendMap, heightGenerator, biomesMap, domainLowerLeftLimit, domainUpperRightLimit,
				Optional.of(Globals.getThreadPool()));

		BiFunction<Float, Float, Float> distribution = (x, z) -> (float)Math.pow(1 - biomesMap.getTreeDensity(x, z), 2.0);
		PoissonDiskSampler sampler = new PoissonDiskSampler(9350, 12000, 10000, 11500, 10f, 50f, distribution, 1, 30, 10_000_000, new Point2D.Float(9350f, 12000f));
		
		TreePlacer placer = new TreePlacer(heightGenerator, biomesMap, sampler);
		pathfinder.findModifierTrajectories(0.0f).ifPresent(ts -> ts.forEach(t -> placer.addNoTreeZone(t, 7.0f)));
		ExecutorService pool = Globals.getThreadPool();
		BlockingQueue<QueueProduct<Map<TreeType, List<Vector3f>>>> locationsPerType = placer.computeLocationsInBackground(pool);

		LODGrid grid = new LODGrid(2000, scaleForModel, lodLevelsForType);
		grid.addToGrid(locationsPerType, pool);

		//Camera camera = new FPSCamera(new Vector3f(100.0f, 0.0f, -5000.0f), heightGenerator, 1f, 2f, 50f, 50f, 12.5f);
		//Camera camera = new FloatingCamera(new Vector3f(0.0f, 10.0f, 0.0f));
		Camera camera = new FloatingCamera(new Vector3f(9350.0f, 200.0f, 12000.0f));
		
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
		
//		Entity barrelEntity = new Entity(barrel, new Vector3f(-10.0f, 0.0f, 0.0f), 0, 0, 0, 1f);
//		Entity crateEntity = new Entity(crate, new Vector3f(-20.0f, 0.0f, 0.0f), 0, 0, 0, 0.05f);
//		Entity boulderEntity = new Entity(boulder, new Vector3f(-30.0f, 0.0f, 0.0f), 0, 0, 0, 1f);
//		Entity barrelEntity2 = new Entity(barrel, new Vector3f(100.0f, heightGenerator.getHeightApprox(100f, -5000f) + 0.5f, -5000.0f), 0, 0, 0, 1f);
//		Entity crateEntity2 = new Entity(crate, new Vector3f(105.0f, heightGenerator.getHeightApprox(105f, -5000f) + 0.5f, -5000.0f), 0, 0, 0, 1f);
//		Entity chestnutEntityTop = new Entity(chestnutTreetop, new Vector3f(110.0f, heightGenerator.getHeightApprox(110f, -5000f), -5000.0f), 0, 0, 0, 1f);
//		Entity chestnutEntityTrunk = new Entity(chestnutTrunk, new Vector3f(110.0f, heightGenerator.getHeightApprox(110f, -5000f), -5000.0f), 0, 0, 0, 1f);
//		Entity cubeEntity = new Entity(cube, new Vector3f(95.0f, heightGenerator.getHeightApprox(95f, -5000f) + 0.5f, -5000.0f), 0, 0, 0, 1f);
//		Entity cubeEntity2 = new Entity(cube, new Vector3f(95.01f, heightGenerator.getHeightApprox(95f, -5000.01f) + 1.5f, -5000.0f), 0, 0.01f, 0, 1f);
		
		Entity barrelEntity = new Entity(barrel, new Vector3f(-10.0f, 0.0f, 0.0f), 0, 0, 0, 1f);
		Entity crateEntity = new Entity(crate, new Vector3f(-20.0f, 0.0f, 0.0f), 0, 0, 0, 0.05f);
		Entity boulderEntity = new Entity(boulder, new Vector3f(-30.0f, 0.0f, 0.0f), 0, 0, 0, 1f);
		Entity barrelEntity2 = new Entity(barrel, new Vector3f(100.0f, 0f, -5000.0f), 0, 0, 0, 1f);
		Entity crateEntity2 = new Entity(crate, new Vector3f(105.0f, 0f, -5000.0f), 0, 0, 0, 1f);
		Entity chestnutEntityTop = new Entity(chestnutTreetop, new Vector3f(110.0f, 0f, -5000.0f), 0, 0, 0, 1f);
		Entity chestnutEntityTrunk = new Entity(chestnutTrunk, new Vector3f(110.0f, 0f, -5000.0f), 0, 0, 0, 1f);
		Entity cubeEntity = new Entity(cube, new Vector3f(95.0f, 0f, -5000.0f), 0, 0, 0, 1f);
		Entity cubeEntity2 = new Entity(cube, new Vector3f(95.01f, 0f, -5000.0f), 0, 0.01f, 0, 1f);
		
		nmEntites.add(barrelEntity);
		nmEntites.add(crateEntity);
		nmEntites.add(boulderEntity);
		
		nmEntites.add(barrelEntity2);
		nmEntites.add(crateEntity2);
		
		final float terrainLODTolerance = 200f;
		
		light = new Light(new Vector3f(500, 1000, -500), new Vector3f(1, 1, 1));
		
//		List<Entity> entities = new ArrayList<>();
//		entities.add(chestnutEntityTrunk);
//		entities.add(chestnutEntityTop);
//		entities.add(cubeEntity);
//		entities.add(cubeEntity2);
		
		Optional<List<Entity>> tunnelPartEntities = maybeRoad.map(road -> {
			TunnelManager tunnelManager = new TunnelManager(road, pathfinder.findTunnelsData().get(), 5, 1.0f, 50f,
					50f, 50f, 50f, 50f, 50f, "tunnel", "tunnel", "tunnel", "black", loader);
			return tunnelManager.getAllTunnelEntities();
		});

		while(!Display.isCloseRequested()) {
			camera.update();
			
//			renderer.processTerrain(terrain);
			
			tunnelPartEntities.ifPresent(parts -> parts.forEach(p -> renderer.processEntity(p)));
			maybeRoadEntity.ifPresent(roadEntity -> renderer.processEntity(roadEntity));
			
			barrelEntity.increaseRotation(0, 0.5f, 0);
			crateEntity.increaseRotation(0, 0.5f, 0);
			boulderEntity.increaseRotation(0, 0.5f, 0);
			//barrelEntity2.increaseRotation(0, 0.5f, 0);
			//crateEntity2.increaseRotation(0, 0.5f, 0);

			List<Terrain> terrains = terrainLODGrid.proximityTerrains(camera.getPosition(), terrainLODTolerance);
			terrains.forEach(t -> renderer.processTerrain(t));
			
			List<Entity> entities = grid.proximityEntities(camera.getPosition());
			entities.forEach(e -> renderer.processEntity(e));
			nmEntites.forEach(e -> renderer.processNMEntity(e));
			renderer.render(light, camera);
			
			DisplayManager.updateDisplay();
		}
		
		renderer.cleanUp();
		loader.cleanUp();
		DisplayManager.closeDisplay();
	}
	
	private static Entity setupRoad(Loader loader, IHeightGenerator heightGenerator, Road road) {
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
	
	private static TexturedModel loadNM(Loader loader, String modelFile, String textureFile, String normalMapFile) {
		ModelData data = OBJFileLoader.loadOBJ(modelFile, false, true);
		RawModel model = loader.loadToVAO(data.getVertices(), data.getTextureCoords(), data.getNormals(), data.getTangents(), data.getIndices());
		return new TexturedModel(model, new ModelTexture(loader.loadTexture(textureFile), loader.loadTexture(normalMapFile)));
	}

}
