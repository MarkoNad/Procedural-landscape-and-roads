package engineTester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;

import entities.Camera;
import entities.Entity;
import entities.FPSCamera;
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
import terrains.IHeightGenerator;
import terrains.SimplexHeightGenerator;
import terrains.Terrain;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.OpenSimplexNoise;
import toolbox.Range;

public class DebugScene {
	
	public static void main(String[] args) {
		DisplayManager.createDisplay();
		
		Loader loader = new Loader();
		Light light = new Light(new Vector3f(3000, 2000, 2000), new Vector3f(1, 1, 1));
		//Camera camera = new FloatingCamera(new Vector3f(0.0f, 100.0f, 0.0f));
		MasterRenderer renderer = new MasterRenderer();

		ModelTexture dragonTexture = new ModelTexture(loader.loadTexture("stall"));
		dragonTexture.setShineDamper(10);
		dragonTexture.setReflectivity(1);
		
		RawModel dragonModel = OBJLoader.loadObjModel("dragon", loader);
		TexturedModel texturedModel = new TexturedModel(dragonModel, dragonTexture);
		Entity entity = new Entity(texturedModel, new Vector3f(100, -5, -20), 0, 0, 0, 1);
		
		//ModelData data = OBJFileLoader.loadOBJ("tree");
		//RawModel treeModel = loader.loadToVAO(data.getVertices(), data.getTextureCoords(), data.getNormals(), data.getIndices());
		//TexturedModel tree = new TexturedModel(treeModel, new ModelTexture(loader.loadTexture("tree")));
		
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
		float xTiles = width / 100f * 0.5f;
		float zTiles = depth / 100f * 0.5f;
		float vertsPerMeter = 0.025f;
		//Terrain terrain = new Terrain(0f, -8000f, new Vector3f(), width, depth, 0.15f, xTiles, zTiles, loader, texturePack, blendMap, heightGenerator);
		Terrain terrain = new Terrain(0f, -depth, new Vector3f(), width, depth, vertsPerMeter, xTiles, zTiles, texturePack, blendMap, heightGenerator, biomesMap, loader);

		// per vertex
		List<Entity> gridElems = new ArrayList<>();
		int xVertices = (int) (width * vertsPerMeter);
		int zVertices = (int) (depth * vertsPerMeter);
		for(int z = 0; z < 50; z++) {
			for(int x = 0; x < 50; x++) {
				float xcoord = x / (float)(xVertices - 1) * width;
				float zcoord = -z / (float)(zVertices - 1) * depth;
				float height = heightGenerator.getHeightApprox(xcoord, zcoord);
				gridElems.add(new Entity(fern, new Vector3f(xcoord, height, zcoord), 0, 0, 0, 0.5f));
			}
		}
		
		// per meter
		List<Entity> meterElems = new ArrayList<>();
		final float zOffset = -5000;
		for(int z = 0; z < 50; z++) {
			for(int x = 0; x < 50; x++) {
				float height = heightGenerator.getHeightApprox(x + 100, -z + zOffset);
				meterElems.add(new Entity(fern, new Vector3f(x + 100, height, -z + zOffset), 0, 0, 0, 0.2f));
			}
		}
//		
//		OpenSimplexNoise noise = new OpenSimplexNoise();
//		IHeightGenerator noiseHGenerator = new IHeightGenerator() {
//			private float amplitude = 100;
//			private float freq = 0.01f;
//			private final float SAMPLING_DISTANCE = 1.5f;
//			@Override
//			public Vector3f getNormal(float x, float z) {
//				float heightL = getHeight(x - SAMPLING_DISTANCE, z);
//				float heightR = getHeight(x + SAMPLING_DISTANCE, z);
//				float heightD = getHeight(x, z - SAMPLING_DISTANCE);
//				float heightU = getHeight(x, z + SAMPLING_DISTANCE);
//				
//				Vector3f normal = new Vector3f(heightL - heightR, 2.0f, heightD - heightU);
//				normal.normalise();
//				
//				return normal;
//			}
//			
//			@Override
//			public float getMaxHeight() {
//				return 1000;
//			}
//			
//			@Override
//			public float getHeight(float x, float z) {
//				float height = (float) (noise.eval(x * freq, z * freq) + 1) * amplitude / 2;
//				System.out.println(height);
//				return height;
//				//return (float) (noise.eval(x * freq, z * freq) + 1) * amplitude / 2;
//			}
//		};
		Terrain noiseTerrain = new Terrain(0f, -20000, new Vector3f(), 20000, 20000, vertsPerMeter, xTiles, zTiles, texturePack, blendMap, noiseHGenerator, biomesMap, loader);
		
		List<Entity> entities = new ArrayList<>();
//		TreePlacer placer = new TreePlacer(noiseHGenerator, 0, 2000, -2000, 0, 50, 100, 70);
//		List<Vector3f> locations = placer.computeLocations();
//		System.out.println(locations.size());
//		
//		for(Vector3f location : locations) {
//			System.out.println(location);
//			entities.add(new Entity(tree, location, 0, 0, 0, 25));
//		}
		
		entities.addAll(gridElems);
		entities.addAll(meterElems);
//		Entity cube = new Cube(loader);
//		entities.add(cube);
//		entities.add(new Entity(cube, new Vector3f(0, 0, 0), 0, 0, 0, 1));
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
		
		Entity barrelEntity2 = new Entity(barrel, new Vector3f(100.0f, heightGenerator.getHeightApprox(100f, -5000f), -5000.0f), 0, 0, 0, 1f);
		
		nmEntites.add(barrelEntity);
		nmEntites.add(crateEntity);
		nmEntites.add(boulderEntity);
		
		nmEntites.add(barrelEntity2);
		
		Camera camera = new FPSCamera(new Vector3f(100.0f, 0.0f, -5000.0f), heightGenerator);

		while(!Display.isCloseRequested()) {
			entity.increaseRotation(0, 0.5f, 0);
			camera.update();
			player.move();
			renderer.processEntity(player);
			
			barrelEntity.increaseRotation(0, 0.5f, 0);
			crateEntity.increaseRotation(0, 0.5f, 0);
			boulderEntity.increaseRotation(0, 0.5f, 0);
			barrelEntity2.increaseRotation(0, 0.5f, 0);
			nmEntites.forEach(e -> renderer.processNMEntity(e));
			
			renderer.processTerrain(terrain);
			renderer.processTerrain(noiseTerrain);
			renderer.processEntity(entity);
			entities.forEach(e -> renderer.processEntity(e));
			renderer.render(light, camera);
			
			DisplayManager.updateDisplay();
		}
		
		renderer.cleanUp();
		loader.cleanUp();
		DisplayManager.closeDisplay();
	}
	
//	private static TexturedModel load(String objFile, String pngFile, Loader loader) {
//		ModelData data = OBJFileLoader.loadOBJ(objFile);
//		RawModel model = loader.loadToVAO(data.getVertices(), data.getTextureCoords(), data.getNormals(), data.getIndices());
//		return new TexturedModel(model, new ModelTexture(loader.loadTexture(pngFile)));
//	}
	
	private static IHeightGenerator noiseHGenerator = new IHeightGenerator() {
//		private float amplitude = 100;
//		private float freq = 0.01f;
		private float amplitude = 3000;
		private float freq = 0.0003f;
		private final float SAMPLING_DISTANCE = 1.5f;
		private OpenSimplexNoise noise = new OpenSimplexNoise(1);
		
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
//			float height = (float) (noise.eval(x * freq, z * freq) + 1) / 2;
//			height = 0.8f * height;
//			height = (float) (Math.pow(height, 2) * amplitude);
//			return height;
			
			float moisture = (float) (noise.eval(x * freq, z * freq) + 1) / 2;
			float factor = 1.5f;
			moisture *= factor;
			moisture = (float) Math.pow(moisture * factor - factor, 2);
			return moisture * amplitude;
		}

		@Override
		public float getHeightApprox(float x, float z) {
			return getHeight(x, z);
		}

		@Override
		public Vector3f getNormalApprox(float x, float z) {
			return getNormal(x, z);
		}
	};
	
	private static TexturedModel loadNM(Loader loader, String modelFile, String textureFile, String normalMapFile) {
		ModelData data = OBJFileLoader.loadOBJ(modelFile);
		RawModel model = loader.loadToVAO(data.getVertices(), data.getTextureCoords(), data.getNormals(), data.getTangents(), data.getIndices());
		return new TexturedModel(model, new ModelTexture(loader.loadTexture(textureFile), loader.loadTexture(normalMapFile)));
	}
	
}
