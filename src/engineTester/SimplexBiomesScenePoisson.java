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
import models.TexturedModelComp;
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
		float width = 20000;
		float depth = 20000;
		float xTiles = width / 200f;
		float zTiles = depth / 200f;
		float vertsPerMeter = 0.025f;
		Terrain terrain = new Terrain(0f, -depth, new Vector3f(), width, depth, vertsPerMeter, xTiles,
				zTiles, loader, texturePack, blendMap, heightGenerator);

		BiomesMap biomesMap = new BiomesMap(heightGenerator);
		
		BiFunction<Float, Float, Float> distribution = (x, z) -> Math.max(0.25f, 1 - biomesMap.getTreeDensity(x, z));
		PoissonDiskSampler sampler = new PoissonDiskSampler(0, 0, 20000, -20000, 600, distribution, 1);
		
		TreePlacer placer = new TreePlacer(heightGenerator, biomesMap, sampler);
		
		long start = System.nanoTime();
		Map<TreeType, List<Vector3f>> locationsPerType = placer.computeLocations();
		long duration = System.nanoTime() - start;
		System.out.println(duration * 1e-9 + " seconds.");
		System.out.println("Size: " + (locationsPerType.get(TreeType.OAK).size() + locationsPerType.get(TreeType.PINE).size()));
		
		LODGrid grid = new LODGrid(2000, scaleForModel, lodLevelsForType);
		grid.addToGrid(locationsPerType);

		while(!Display.isCloseRequested()) {
			camera.update();
			
			renderer.processTerrain(terrain);
			List<Entity> entities = grid.proximityEntities(camera.getPosition());
			
			entities.add(new Entity(chestnutLOD1, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 180f));
			entities.add(new Entity(chestnutTreetop, new Vector3f(250f, 0f, 0f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(chestnutTrunk, new Vector3f(250f, 0f, 0f), 0f, 0f, 0f, 15f));
			entities.add(new Entity(firLOD1, new Vector3f(500f, 0f, 0f), 0f, 0f, 0f, 140f));
			entities.add(new Entity(firTreetop, new Vector3f(750f, 0f, 0f), 0f, 0f, 0f, 60f));
			entities.add(new Entity(firTrunk, new Vector3f(750f, 0f, 0f), 0f, 0f, 0f, 60f));
			
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