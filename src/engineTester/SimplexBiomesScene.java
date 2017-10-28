package engineTester;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;

import entities.Camera;
import entities.Entity;
import entities.FloatingCamera;
import entities.Light;
import entities.Player;
import entities.standard.Cube;
import models.RawModel;
import models.TexturedModel;
import objConverter.ModelData;
import objConverter.OBJFileLoader;
import renderEngine.DisplayManager;
import renderEngine.Loader;
import renderEngine.MasterRenderer;
import renderEngine.OBJLoader;
import terrains.IHeightGenerator;
import terrains.SimplexHeightGenerator;
import terrains.Terrain;
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

		ModelTexture dragonTexture = new ModelTexture(loader.loadTexture("stall"));
		dragonTexture.setShineDamper(10);
		dragonTexture.setReflectivity(1);
		
		RawModel dragonModel = OBJLoader.loadObjModel("dragon", loader);
		TexturedModel texturedModel = new TexturedModel(dragonModel, dragonTexture);
		Entity entity = new Entity(texturedModel, new Vector3f(100, -5, -20), 0, 0, 0, 1);
		
		ModelData data = OBJFileLoader.loadOBJ("tree");
		RawModel treeModel = loader.loadToVAO(data.getVertices(), data.getTextureCoords(), data.getNormals(), data.getIndices());
		TexturedModel tree = new TexturedModel(treeModel, new ModelTexture(loader.loadTexture("tree")));
		
		RawModel bunnyModel = OBJLoader.loadObjModel("stanfordBunny", loader);
		TexturedModel stanfordBunny = new TexturedModel(bunnyModel, new ModelTexture(loader.loadTexture("white")));
		Player player = new Player(stanfordBunny, new Vector3f(100, 0, -50), 0, 0, 0, 1);
		
		TexturedModel grass = new TexturedModel(OBJLoader.loadObjModel("grassModel", loader), new ModelTexture(loader.loadTexture("grassTexture")));
		grass.getTexture().setHasTransparency(true);
		grass.getTexture().setUsesFakeLighting(true);
		
		TexturedModel fern = new TexturedModel(OBJLoader.loadObjModel("fern", loader), new ModelTexture(loader.loadTexture("fern")));
		fern.getTexture().setHasTransparency(true);
		fern.getTexture().setUsesFakeLighting(true);
		
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("sand"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("snow"));
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));
		
		IHeightGenerator heightGenerator = new SimplexHeightGenerator(0);
		float width = 4000;
		float depth = 4000;
		float xTiles = width / 800f;
		float zTiles = depth / 800f;
		Terrain terrain = new Terrain(0f, -4000f, new Vector3f(), width, depth, 0.15f, xTiles, zTiles, loader, texturePack, blendMap, heightGenerator);
		
		Entity cube = new Cube(loader);
		List<Entity> gridElems = new ArrayList<>();
		float vertsPerMeter = 0.15f;
		int xVertices = (int) (width * vertsPerMeter);
		int zVertices = (int) (depth * vertsPerMeter);
		for(int z = 0; z < 50; z++) {
			for(int x = 0; x < 50; x++) {
				float xcoord = x / (float)(xVertices - 1) * width;
				float zcoord = z / (float)(zVertices - 1) * depth;
				float height = heightGenerator.getHeight(xcoord, zcoord);
				gridElems.add(new Entity(fern, new Vector3f(xcoord, 0, zcoord), 0, 0, 0, 0.1f));
			}
		}
		
		List<Entity> entities = new ArrayList<>();
		entities.addAll(gridElems);
		entities.add(cube);
		Random rand = new Random();
		
		for(int i = 0; i < 500; i++) {
			float x = rand.nextFloat() * width;
			float z = -rand.nextFloat() * depth;
			float y = heightGenerator.getHeight(x, z);
			entities.add(new Entity(tree, new Vector3f(x, y, z), 0, 0, 0, 7));
			
			x = rand.nextFloat() * width;
			z = -rand.nextFloat() * depth;
			y = heightGenerator.getHeight(x, z);
			entities.add(new Entity(grass, new Vector3f(x, y, z), 0, 0, 0, 2.0f));
			
			x = rand.nextFloat() * width;
			z = -rand.nextFloat() * depth;
			y = heightGenerator.getHeight(x, z);
			entities.add(new Entity(fern, new Vector3f(x, y, z), 0, 0, 0, 1));
		}
		float meter = 7f;
//		entities.add(new Entity(tree, new Vector3f(0, 0, 0), 0, 0, 0, 5));
//		entities.add(new Entity(tree, new Vector3f(0, 0, -meter), 0, 0, 0, 5));
//		entities.add(new Entity(tree, new Vector3f(meter, 0, 0), 0, 0, 0, 5));
//		entities.add(new Entity(tree, new Vector3f(meter, 0, -meter), 0, 0, 0, 5));

		while(!Display.isCloseRequested()) {
			entity.increaseRotation(0, 0.5f, 0);
			camera.update();
			player.move();
			renderer.processEntity(player);
			
			renderer.processTerrain(terrain);
			renderer.processEntity(entity);
			entities.forEach(e -> renderer.processEntity(e));
			renderer.render(light, camera);
			
			DisplayManager.updateDisplay();
		}
		
		renderer.cleanUp();
		loader.cleanUp();
		DisplayManager.closeDisplay();
	}
	
}
