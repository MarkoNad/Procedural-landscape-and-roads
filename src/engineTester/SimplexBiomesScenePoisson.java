package engineTester;

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
import objConverter.ModelData;
import objConverter.OBJFileLoader;
import renderEngine.DisplayManager;
import renderEngine.Loader;
import renderEngine.MasterRenderer;
import terrains.BiomesMap;
import terrains.BiomesMap.TreeType;
import terrains.IHeightGenerator;
import terrains.SimplexHeightGenerator;
import terrains.Terrain;
import terrains.TreePlacer;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.PoissonDiskSampler;

public class SimplexBiomesScenePoisson {
	
	public static void main(String[] args) {
		DisplayManager.createDisplay();
		
		Loader loader = new Loader();
		Light light = new Light(new Vector3f(3000, 2000, 2000), new Vector3f(1, 1, 1));
		Camera camera = new FloatingCamera(new Vector3f(0.0f, 100.0f, 0.0f));
		MasterRenderer renderer = new MasterRenderer();

		TexturedModel pine = load("tree", "tree", loader);
		TexturedModel oak = load("oak-lp", "snow", loader);
		TexturedModel oakLOD1 = load("oak-lp", "tree", loader);
		TexturedModel oakLOD2 = load("oak-lp", "cliff3", loader);
		TexturedModel grass = load("grassModel", "grassTexture", loader);
		TexturedModel fern = load("fern", "fern", loader);
		
		grass.getTexture().setHasTransparency(true);
		grass.getTexture().setUsesFakeLighting(true);
		fern.getTexture().setHasTransparency(true);
		fern.getTexture().setUsesFakeLighting(true);
		
		Map<TreeType, TexturedModel> modelForType = new HashMap<>();
		modelForType.put(TreeType.OAK, oak);
		modelForType.put(TreeType.PINE, pine);

		Map<TexturedModel, Float> scaleForModel = new HashMap<>();
		scaleForModel.put(oak, 1.0f);
		scaleForModel.put(oakLOD1, 1.0f);
		scaleForModel.put(oakLOD2, 1.0f);
		scaleForModel.put(pine, 30.0f);
		
		NavigableMap<Float, TexturedModel> oakLods = new TreeMap<>();
		oakLods.put(1000f, oak);
		oakLods.put(5000f, oakLOD1);
		oakLods.put(10000f, oakLOD2);
		NavigableMap<Float, TexturedModel> pineLods = new TreeMap<>();
		pineLods.put(1000f, pine);
		pineLods.put(5000f, pine);
		pineLods.put(10000f, pine);
		Map<TreeType, NavigableMap<Float, TexturedModel>> lodLevelsForType = new HashMap<>();
		lodLevelsForType.put(TreeType.OAK, oakLods);
		lodLevelsForType.put(TreeType.PINE, pineLods);
		
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("cliff3"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("snow"));
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));
		
		IHeightGenerator heightGenerator = new SimplexHeightGenerator(1);
		float width = 20000;
		float depth = 20000;
		float xTiles = width / 200f;
		float zTiles = depth / 200f;
		float vertsPerMeter = 0.025f;
		Terrain terrain = new Terrain(0f, -depth, new Vector3f(), width, depth, vertsPerMeter, xTiles,
				zTiles, loader, texturePack, blendMap, heightGenerator);

		BiomesMap biomesMap = new BiomesMap(heightGenerator);
		
		BiFunction<Float, Float, Float> distribution = (x, z) -> Math.max(0.25f, 1 - biomesMap.getTreeDensity(x, z));
		PoissonDiskSampler sampler = new PoissonDiskSampler(0, 0, 10000, -15000, 400, distribution, 1);
		
		TreePlacer placer = new TreePlacer(heightGenerator, biomesMap, sampler);
		
		long start = System.nanoTime();
		Map<TreeType, List<Vector3f>> locationsPerType = placer.computeLocations();
		long duration = System.nanoTime() - start;
		System.out.println(duration * 1e-9 + " seconds.");
		System.out.println("Size: " + (locationsPerType.get(TreeType.OAK).size() + locationsPerType.get(TreeType.PINE).size()));
		
		//LODGrid grid = new LODGrid(500, scaleForModel, lodLevelsForType);
		LODGrid grid = new LODGrid(2000, scaleForModel, lodLevelsForType);
		grid.addToGrid(locationsPerType);

		while(!Display.isCloseRequested()) {
			camera.update();
			
			renderer.processTerrain(terrain);
			List<Entity> entities = grid.proximityEntities(camera.getPosition());
			entities.forEach(e -> renderer.processEntity(e));
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
