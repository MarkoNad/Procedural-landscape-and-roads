package engineTester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;

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
import terrains.IHeightGenerator;
import terrains.SimplexHeightGenerator;
import terrains.Terrain;
import terrains.TreePlacer;
import terrains.BiomesMap.TreeType;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;

public class SimplexBiomesScene {
	
	public static void main(String[] args) {
		DisplayManager.createDisplay();
		
		Loader loader = new Loader();
		Light light = new Light(new Vector3f(3000, 2000, 2000), new Vector3f(1, 1, 1));
		Camera camera = new FloatingCamera(new Vector3f(0.0f, 100.0f, 0.0f));
		MasterRenderer renderer = new MasterRenderer();

		TexturedModel pine = load("tree", "tree", loader);
		TexturedModel oak = load("oak-lp", "snow", loader);
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
		scaleForModel.put(pine, 30.0f);
		
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("cliff3"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("snow"));
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));
		
		IHeightGenerator heightGenerator = new SimplexHeightGenerator(0);
		float width = 20000;
		float depth = 20000;
		float xTiles = width / 200f;
		float zTiles = depth / 200f;
		float vertsPerMeter = 0.025f;
		Terrain terrain = new Terrain(0f, -depth, new Vector3f(), width, depth, vertsPerMeter, xTiles,
				zTiles, loader, texturePack, blendMap, heightGenerator);

		List<Entity> entities = new ArrayList<>();
		BiomesMap biomesMap = new BiomesMap(heightGenerator);
		TreePlacer placer = new TreePlacer(heightGenerator, biomesMap, 0, 15000, -10000, 0, 50, 100, 70);
		//TreePlacer placer = new TreePlacer(heightGenerator, biomesMap, 0, 10000, -5000, 0, 50, 100, 70);
		
		long start = System.nanoTime();
		Map<TreeType, List<Vector3f>> locations = placer.computeLocations();
		long duration = System.nanoTime() - start;
		System.out.println(duration + " " + duration * 1e-9);
		
		for(TreeType type : locations.keySet()) {
			TexturedModel model = modelForType.get(type);
			float scale = scaleForModel.get(model);
			
			for(Vector3f location : locations.get(type)) {
				entities.add(new Entity(model, location, 0, 0, 0, scale));
			}
		}

		while(!Display.isCloseRequested()) {
			camera.update();
			
			renderer.processTerrain(terrain);
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
