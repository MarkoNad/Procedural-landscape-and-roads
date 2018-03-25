package hr.fer.zemris.engine.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.BiFunction;

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
import hr.fer.zemris.engine.road.Road;
import hr.fer.zemris.engine.terrain.BiomesMap;
import hr.fer.zemris.engine.terrain.IHeightMap;
import hr.fer.zemris.engine.terrain.NoiseMap;
import hr.fer.zemris.engine.terrain.SimplexHeightGenerator;
import hr.fer.zemris.engine.terrain.Terrain;
import hr.fer.zemris.engine.terrain.TreePlacer;
import hr.fer.zemris.engine.terrain.TreeType;
import hr.fer.zemris.engine.texture.ModelTexture;
import hr.fer.zemris.engine.texture.TerrainTexture;
import hr.fer.zemris.engine.texture.TerrainTexturePack;
import hr.fer.zemris.engine.util.CatmullRomSpline3D;
import hr.fer.zemris.engine.util.PoissonDiskSampler;
import hr.fer.zemris.engine.util.Range;
import hr.fer.zemris.engine.util.TriFunction;

public class CustomRoadScene {
	
	public static void main(String[] args) {
		DisplayManager.createDisplay();
		
		Loader loader = new Loader();
		Light light = new Light(new Vector3f(3000, 2000, 2000), new Vector3f(1, 1, 1));
		Camera camera = new FloatingCamera(new Vector3f(0.0f, 2000.0f, 0.0f));
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
		firLOD1.getTexture().setUsesFakeLighting(true);
		
		TexturedModelComp firLOD1Comp = new TexturedModelComp(firLOD1);
		TexturedModelComp chestnutLOD1Comp = new TexturedModelComp(chestnutLOD1);
		TexturedModelComp chestnutLOD0Comp = new TexturedModelComp(chestnutTreetop, chestnutTrunk);
		TexturedModelComp firLOD0Comp = new TexturedModelComp(firTrunk, firTreetop);

		Map<TexturedModelComp, Float> scaleForModel = new HashMap<>();
		scaleForModel.put(firLOD0Comp, 60.0f);
		scaleForModel.put(firLOD1Comp, 280.0f);
		scaleForModel.put(chestnutLOD0Comp, 15.0f);
		scaleForModel.put(chestnutLOD1Comp, 380.0f);
		
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
		
		IHeightMap heightGenerator = new SimplexHeightGenerator(1);
		List<Range> textureRanges = Arrays.asList(new Range(0, 700), new Range(700, 3000), new Range(3000, heightGenerator.getMaxHeight()));
		TriFunction<Float, Float, Float, Float> textureVariation = (x, h, z) -> {
			NoiseMap texVariationMap = new NoiseMap(450f, 0.0005f, 0);
			final float maxHeight = textureRanges.get(textureRanges.size() - 1).getEnd();
			return (float) (texVariationMap.getNoise(x, z) * Math.pow(4 * (h + 1000) / maxHeight, 1.5));
		};
		BiomesMap biomesMap = new BiomesMap(heightGenerator, textureRanges, 500f, textureVariation, new Random(0));
		float width = 20000;
		float depth = 20000;
		float texWidth = 200f;
		float texDepth = 200f;
		float vertsPerMeter = 0.025f;
		Terrain terrain = new Terrain(0f, -depth, new Vector3f(), width, depth, vertsPerMeter, texWidth,
				texDepth, texturePack, blendMap, heightGenerator, biomesMap, loader);

		BiFunction<Float, Float, Float> distribution = (x, z) -> (float)Math.pow(1 - biomesMap.getTreeDensity(x, z), 2.0);
		PoissonDiskSampler sampler = new PoissonDiskSampler(0, 0, 20000, -20000, 130f, 650f, distribution, 1);
		TreePlacer placer = new TreePlacer(heightGenerator, biomesMap, sampler);
		Map<TreeType, List<Vector3f>> locationsPerType = placer.computeLocations();
		
		LODGrid grid = new LODGrid(2000, scaleForModel, lodLevelsForType);
		grid.addToGrid(locationsPerType);
		
		Entity road = setupRoad(loader, heightGenerator);

		while(!Display.isCloseRequested()) {
			camera.update();
			
			renderer.processTerrain(terrain);
			List<Entity> entities = grid.proximityEntities(camera.getPosition());
			entities.forEach(e -> renderer.processEntity(e));
			renderer.processEntity(road);
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
	
	private static Entity setupRoad(Loader loader, IHeightMap heightGenerator) {
		List<Vector3f> waypoints = new ArrayList<>();
		
		waypoints.add(new Vector3f(0, 0, -2000));
		waypoints.add(new Vector3f(100, 0, -2000));
		waypoints.add(new Vector3f(500, 0, -2000));
		waypoints.add(new Vector3f(1000, 0, -2500));
		waypoints.add(new Vector3f(2000, 0, -3500));
		waypoints.add(new Vector3f(3000, 0, -3500));
		waypoints.add(new Vector3f(4000, 0, -2500));
		waypoints.add(new Vector3f(6000, 0, -2000));
		waypoints.add(new Vector3f(7000, 0, -2500));
		waypoints.add(new Vector3f(8000, 0, -2200));
		waypoints.add(new Vector3f(9000, 0, -2000));
		waypoints.add(new Vector3f(10000, 0, -1500));
		waypoints.add(new Vector3f(10500, 0, -500));
		waypoints.add(new Vector3f(10500, 0, -100));
		waypoints.add(new Vector3f(10500, 0, 0));
		
		Road road = new Road(loader, waypoints, 250, 200, 50, 7f, heightGenerator, true, CatmullRomSpline3D::new);
		TexturedModel roadTM = new TexturedModel(road.getModel(), new ModelTexture(loader.loadTexture("road")));
		roadTM.getTexture().setHasTransparency(true);
		return new Entity(roadTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
	}
	
}
