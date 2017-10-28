package engineTester;

import java.util.ArrayList;
import java.util.List;

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
import terrains.IHeightGenerator;
import terrains.OpenSimplexNoise;
import terrains.SimplexHeightGenerator;
import terrains.Terrain;
import terrains.TreePlacer;
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

		TexturedModel tree = load("tree", "tree", loader);
		TexturedModel grass = load("grassModel", "grassTexture", loader);
		TexturedModel fern = load("fern", "fern", loader);
		
		grass.getTexture().setHasTransparency(true);
		grass.getTexture().setUsesFakeLighting(true);
		fern.getTexture().setHasTransparency(true);
		fern.getTexture().setUsesFakeLighting(true);
		
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
		TreePlacer placer = new TreePlacer(noiseHGenerator, 0, 2000, -2000, 0, 50, 100, 70);
		List<Vector3f> locations = placer.computeLocations();
		
		for(Vector3f location : locations) {
			entities.add(new Entity(tree, location, 0, 0, 0, 25));
		}
		
//		Random rand = new Random();
//		for(int i = 0; i < 500; i++) {
//			float x = rand.nextFloat() * 2000 + 2000;
//			float z = -rand.nextFloat() * 2000;
//			float y = heightGenerator.getHeight(x, z);
//			entities.add(new Entity(tree, new Vector3f(x, y, z), 0, 0, 0, 25));
//			
//			x = rand.nextFloat() * width;
//			z = -rand.nextFloat() * depth;
//			y = heightGenerator.getHeight(x, z);
//			entities.add(new Entity(grass, new Vector3f(x, y, z), 0, 0, 0, 2.0f));
//			
//			x = rand.nextFloat() * width;
//			z = -rand.nextFloat() * depth;
//			y = heightGenerator.getHeight(x, z);
//			entities.add(new Entity(fern, new Vector3f(x, y, z), 0, 0, 0, 1));
//		}

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
	
	private static IHeightGenerator noiseHGenerator = new IHeightGenerator() {
		private float amplitude = 100;
		private float freq = 0.01f;
		private final float SAMPLING_DISTANCE = 1.5f;
		private OpenSimplexNoise noise = new OpenSimplexNoise();
		
		@Override
		public Vector3f getNormal(float x, float z) {
			float heightL = getHeight(x - SAMPLING_DISTANCE, z);
			float heightR = getHeight(x + SAMPLING_DISTANCE, z);
			float heightD = getHeight(x, z - SAMPLING_DISTANCE);
			float heightU = getHeight(x, z + SAMPLING_DISTANCE);
			
			Vector3f normal = new Vector3f(heightL - heightR, 2.0f, heightD - heightU);
			normal.normalise();
			
			return normal;
		}
		
		@Override
		public float getMaxHeight() {
			return 1000;
		}
		
		@Override
		public float getHeight(float x, float z) {
			float height = (float) (noise.eval(x * freq, z * freq) + 1) * amplitude / 2;
			System.out.println(height);
			return height;
			//return (float) (noise.eval(x * freq, z * freq) + 1) * amplitude / 2;
		}
	};
	
}
