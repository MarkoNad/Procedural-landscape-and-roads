package engineTester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;

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
import terrains.BiomesMap;
import terrains.IHeightGenerator;
import terrains.SimplexHeightGenerator;
import terrains.Terrain;
import terrains.TreePlacer;
import terrains.TreeType;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.CatmullRomSpline3D;
import toolbox.ITrajectory;
import toolbox.PoissonDiskSampler;
import toolbox.Range;

public class ExperimentScene {
	
	public static void main(String[] args) {
		DisplayManager.createDisplay();
		
		Loader loader = new Loader();
		Light light = new Light(new Vector3f(3000, 2000, 2000), new Vector3f(1, 1, 1));
		Camera camera = new FloatingCamera(new Vector3f(0.0f, 100.0f, 0.0f));
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
		
		TexturedModelComp firLOD1Comp = new TexturedModelComp(firLOD1);
		TexturedModelComp chestnutLOD1Comp = new TexturedModelComp(chestnutLOD1);
		TexturedModelComp chestnutLOD0Comp = new TexturedModelComp(chestnutTreetop, chestnutTrunk);
		TexturedModelComp firLOD0Comp = new TexturedModelComp(firTrunk, firTreetop);

		Map<TexturedModelComp, Float> scaleForModel = new HashMap<>();
		scaleForModel.put(firLOD0Comp, 60.0f);
		scaleForModel.put(firLOD1Comp, 140.0f);
		scaleForModel.put(chestnutLOD0Comp, 15.0f);
		scaleForModel.put(chestnutLOD1Comp, 190.0f);
		
		NavigableMap<Float, TexturedModelComp> chestnutLods = new TreeMap<>();
		chestnutLods.put(4000f, chestnutLOD0Comp);
		chestnutLods.put(20000f, chestnutLOD1Comp);

		NavigableMap<Float, TexturedModelComp> firLods = new TreeMap<>();
		firLods.put(4000f, firLOD0Comp);
		firLods.put(20000f, firLOD1Comp);
		
		Map<TreeType, NavigableMap<Float, TexturedModelComp>> lodLevelsForType = new HashMap<>();
		lodLevelsForType.put(TreeType.OAK, chestnutLods);
		lodLevelsForType.put(TreeType.PINE, firLods);
		
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("cliff3"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("snow"));
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));
		
		IHeightGenerator heightGenerator = new SimplexHeightGenerator(1);
		List<Range> textureRanges = Arrays.asList(new Range(0, 700), new Range(700, 3000), new Range(3000, heightGenerator.getMaxHeight()));
		BiomesMap biomesMap = new BiomesMap(heightGenerator, textureRanges, 500f);
		float width = 20000;
		float depth = 20000;
		float texWidth = 200f;
		float texDepth = 200f;
		float vertsPerMeter = 0.025f;
		Terrain terrain = new Terrain(0f, -depth, new Vector3f(), width, depth, vertsPerMeter, texWidth,
				texDepth, texturePack, blendMap, heightGenerator, biomesMap, loader);
		
		List<Vector3f> waypoints = new ArrayList<>();
		waypoints.add(new Vector3f(0, 0, 0));
		waypoints.add(new Vector3f(500, 0, 0));
		waypoints.add(new Vector3f(1000, 0, 500));
		waypoints.add(new Vector3f(1500, 0, 0));
		waypoints.add(new Vector3f(2000, 0, 0));
		waypoints.add(new Vector3f(2500, 0, -500));
		waypoints.add(new Vector3f(2500, 0, -1000));
		waypoints.add(new Vector3f(2000, 0, -1500));
		waypoints.add(new Vector3f(1500, 0, -1500));
		waypoints.add(new Vector3f(1000, 0, -1500));
		waypoints.add(new Vector3f(500, 0, -1000));
		waypoints.add(new Vector3f(0, 0, -1000));
		waypoints.add(new Vector3f(-500, 0, -1500));
		Road road = new Road(loader, waypoints, 250, 200, 50, 7, heightGenerator, true);
		TexturedModel roadTM = new TexturedModel(road.getModel(), new ModelTexture(loader.loadTexture("road")));
		roadTM.getTexture().setHasTransparency(true);
		
		//BiFunction<Float, Float, Float> distribution = (x, z) -> Math.max(0.25f, 1 - biomesMap.getTreeDensity(x, z));
		BiFunction<Float, Float, Float> distribution = (x, z) -> (float)Math.pow(1 - biomesMap.getTreeDensity(x, z), 2.0);
		PoissonDiskSampler sampler = new PoissonDiskSampler(0, 0, 20000, -20000, 130f, 5 * 130f, distribution, 1);
		
		TreePlacer placer = new TreePlacer(heightGenerator, biomesMap, sampler);
		
		long start = System.nanoTime();
		Map<TreeType, List<Vector3f>> locationsPerType = placer.computeLocations();
		long duration = System.nanoTime() - start;
		System.out.println(duration * 1e-9 + " seconds.");
		System.out.println("Size: " + (locationsPerType.get(TreeType.OAK).size() + locationsPerType.get(TreeType.PINE).size()));
		
		LODGrid grid = new LODGrid(2000, scaleForModel, lodLevelsForType);
		grid.addToGrid(locationsPerType);
		
		List<Vector3f> waypoints2 = new ArrayList<>();
		waypoints2.add(new Vector3f(0, 0, -2000));
		waypoints2.add(new Vector3f(100, 0, -2000));
		waypoints2.add(new Vector3f(500, 0, -2000));
		waypoints2.add(new Vector3f(1000, 0, -2500));
		waypoints2.add(new Vector3f(2000, 0, -3500));
		waypoints2.add(new Vector3f(3000, 0, -3500));
		waypoints2.add(new Vector3f(4000, 0, -2500));
		waypoints2.add(new Vector3f(6000, 0, -2000));
		waypoints2.add(new Vector3f(7000, 0, -2500));
		waypoints2.add(new Vector3f(8000, 0, -2200));
		waypoints2.add(new Vector3f(9000, 0, -2000));
		waypoints2.add(new Vector3f(10000, 0, -1500));
		waypoints2.add(new Vector3f(10500, 0, -500));
		waypoints2.add(new Vector3f(10500, 0, -100));
		waypoints2.add(new Vector3f(10500, 0, 0));
		Road road2 = new Road(loader, waypoints2, 250, 200, 100, 7f, heightGenerator, true);
		TexturedModel roadTM2 = new TexturedModel(road2.getModel(), new ModelTexture(loader.loadTexture("road")));
		roadTM2.getTexture().setHasTransparency(true);
		Entity road2Entity = new Entity(roadTM2, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
		
		List<Vector3f> ctrlPoints = new ArrayList<>();
		ctrlPoints.add(new Vector3f(0, 0, 0));
		ctrlPoints.add(new Vector3f(200, 0, 200));
		ctrlPoints.add(new Vector3f(400, 0, 200));
		ctrlPoints.add(new Vector3f(600, 0, 0));
		ctrlPoints.add(new Vector3f(800, 0, 0));
		//BezierCurve curve = new BezierCurve(ctrlPoints, BezierType.APPROXIMATION, 10);
		ITrajectory<Vector3f> curve = new CatmullRomSpline3D(ctrlPoints, 10);
		
		for(Vector3f bpoint : curve.getPoints()) {
			System.out.println(bpoint);
		}
		
		List<Entity> nmEntites = new ArrayList<>();
		ModelData data = OBJFileLoader.loadOBJ("barrel");
		RawModel model = loader.loadToVAO(data.getVertices(), data.getTextureCoords(), data.getNormals(), data.getTangents(), data.getIndices());
		TexturedModel barrel = new TexturedModel(model, new ModelTexture(loader.loadTexture("barrel"), loader.loadTexture("barrelNormal")));
		barrel.getTexture().setShineDamper(10);
		barrel.getTexture().setReflectivity(0.5f);
		barrel.getTexture().setUsesFakeLighting(false);
		nmEntites.add(new Entity(barrel, new Vector3f(-100.0f, 0.0f, 0.0f), 0, 0, 0, 1f));

		while(!Display.isCloseRequested()) {
			camera.update();
			
			renderer.processTerrain(terrain);
			List<Entity> entities = grid.proximityEntities(camera.getPosition());
			
			entities.add(new Entity(roadTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f));
			entities.add(road2Entity);
//			entities.add(new Entity(chestnutLOD1, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 180f));
//			entities.add(new Entity(chestnutTreetop, new Vector3f(250f, 0f, 0f), 0f, 0f, 0f, 15f));
//			entities.add(new Entity(chestnutTrunk, new Vector3f(250f, 0f, 0f), 0f, 0f, 0f, 15f));
//			entities.add(new Entity(firLOD1, new Vector3f(500f, 0f, 0f), 0f, 0f, 0f, 140f));
//			entities.add(new Entity(firTreetop, new Vector3f(750f, 0f, 0f), 0f, 0f, 0f, 60f));
//			entities.add(new Entity(firTrunk, new Vector3f(750f, 0f, 0f), 0f, 0f, 0f, 60f));
			
			entities.add(new Entity(chestnutTreetop, new Vector3f(0, 0f, 0f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(chestnutTrunk, new Vector3f(0, 0f, 0f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(chestnutTreetop, new Vector3f(500, 0f, 0f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(chestnutTrunk, new Vector3f(500, 0f, 0f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(chestnutTreetop, new Vector3f(1000, 0f, 500f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(chestnutTrunk, new Vector3f(1000, 0f, 500f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(chestnutTreetop, new Vector3f(1500, 0f, 0f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(chestnutTrunk, new Vector3f(1500, 0f, 0f), 0f, 0f, 0f, 15f));
			
			entities.add(new Entity(barrel, new Vector3f(0.0f, 0.0f, 0.0f), 0, 0, 0, 10f));
			
			//entities.add(new Entity(chestnutTreetop, new Vector3f(0, 0f, 500f), 0f, 0f, 0f, 1f));
			//entities.add(new Entity(chestnutTrunk, new Vector3f(0, 0f, 500f), 0f, 0f, 0f, 1f));
			//entities.add(new Entity(chestnutLOD1, new Vector3f(40, 0f, 500f), 0f, 0f, 0f, 20f));
			entities.add(new Entity(firTreetop, new Vector3f(-200, 0f, 500f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(firTrunk, new Vector3f(-200, 0f, 500f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(firLOD1, new Vector3f(-150, 0f, 500f), 0f, 0f, 0f, 35f));
			
			
			for(Vector3f bpoint : curve.getPoints()) {
				entities.add(new Entity(chestnutTreetop, new Vector3f(bpoint.x, bpoint.y, bpoint.z), 0f, 0f, 0f, 5f));
				entities.add(new Entity(chestnutTrunk, new Vector3f(bpoint.x, bpoint.y, bpoint.z), 0f, 0f, 0f, 5f));
			}
			for(Vector3f point : ctrlPoints) {
				entities.add(new Entity(firTreetop, new Vector3f(point.x, point.y, point.z), 0f, 0f, 0f, 20f));
				entities.add(new Entity(firTrunk, new Vector3f(point.x, point.y, point.z), 0f, 0f, 0f, 20f));
			}
			
			entities.forEach(e -> renderer.processEntity(e));
			nmEntites.forEach(e -> renderer.processNMEntity(e));
			renderer.render(light, camera);
			
			DisplayManager.updateDisplay();
		}
		
		renderer.cleanUp();
		loader.cleanUp();
		DisplayManager.closeDisplay();
	}
	
	private static TexturedModel load(String objFile, String pngFile, Loader loader) {
		ModelData data = OBJFileLoader.loadOBJ(objFile);
		RawModel model = loader.loadToVAO(data.getVertices(), data.getTextureCoords(), data.getNormals(), data.getIndices());
		return new TexturedModel(model, new ModelTexture(loader.loadTexture(pngFile)));
	}
	
}
