package engineTester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;

import entities.Camera;
import entities.Entity;
import entities.FloatingCamera;
import entities.Light;
import entities.Player;
import models.RawModel;
import models.TexturedModel;
import objConverter.ModelData;
import objConverter.OBJFileLoader;
import renderEngine.DisplayManager;
import renderEngine.Loader;
import renderEngine.MasterRenderer;
import renderEngine.OBJLoader;
import terrains.BiomesMap;
import terrains.HeightMapHeightGenerator;
import terrains.Terrain;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.Range;

public class HeightMapScene {
	
	public static void main(String[] args) {
		DisplayManager.createDisplay();
		
		Loader loader = new Loader();
		Light light = new Light(new Vector3f(3000, 2000, 2000), new Vector3f(1, 1, 1));
		Camera camera = new FloatingCamera();
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
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("dirt"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("pinkFlowers"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("path"));
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));
		
		HeightMapHeightGenerator heightGenerator = new HeightMapHeightGenerator("res/heightMap.png");
		List<Range> textureRanges = Arrays.asList(new Range(0, 700), new Range(700, 3000), new Range(3000, heightGenerator.getMaxHeight()));
		BiomesMap biomesMap = new BiomesMap(heightGenerator, textureRanges, 500f);
		//Terrain terrain = new Terrain(0, -1, loader, texturePack, blendMap, heightGenerator, heightGenerator.getXPoints(), heightGenerator.getYPoints());
		Terrain terrain = new Terrain(0f, -800f, new Vector3f(), 800f, 800f, 0.15f, 1f, 1f, loader, texturePack, blendMap, heightGenerator, biomesMap);
		
		List<Entity> entities = new ArrayList<>();
		Random rand = new Random();
		
		for(int i = 0; i < 500; i++) {
			entities.add(new Entity(tree, new Vector3f(rand.nextFloat() * 800, 0, - rand.nextFloat() * 800), 0, 0, 0, 5));
			entities.add(new Entity(grass, new Vector3f(rand.nextFloat() * 800, 0, - rand.nextFloat() * 800), 0, 0, 0, 2));
			entities.add(new Entity(fern, new Vector3f(rand.nextFloat() * 800, 0, - rand.nextFloat() * 800), 0, 0, 0, 1));
		}

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
