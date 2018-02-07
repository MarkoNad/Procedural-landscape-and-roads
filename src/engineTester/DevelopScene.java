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
import search.AStar;
import search.IProblem;
import search.Node;
import terrains.BiomesMap;
import terrains.IHeightGenerator;
import terrains.NoiseMap;
import terrains.SimplexHeightGenerator;
import terrains.Terrain;
import terrains.TreePlacer;
import terrains.TreeType;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.PoissonDiskSampler;
import toolbox.Range;
import toolbox.TriFunction;

public class DevelopScene {
	
	public static void main(String[] args) {
		findPath();
		
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
		TriFunction<Float, Float, Float, Float> textureVariation = (x, h, z) -> {
			NoiseMap texVariationMap = new NoiseMap(450f, 0.0005f, 0);
			final float maxHeight = textureRanges.get(textureRanges.size() - 1).getEnd();
			return (float) (texVariationMap.getNoise(x, z) * Math.pow(4 * (h + 1000) / maxHeight, 1.5));
		};
		BiomesMap biomesMap = new BiomesMap(heightGenerator, textureRanges, 500f, textureVariation);
		float width = 20000;
		float depth = 20000;
		float xTiles = width / 200f;
		float zTiles = depth / 200f;
		float vertsPerMeter = 0.025f;
		Terrain terrain = new Terrain(0f, -depth, new Vector3f(), width, depth, vertsPerMeter, xTiles,
				zTiles, loader, texturePack, blendMap, heightGenerator, biomesMap);

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
	
	private static Entity setupRoad(Loader loader, IHeightGenerator heightGenerator) {
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
		
		Road road = new Road(loader, waypoints, heightGenerator, 250, 200, 100);
		TexturedModel roadTM = new TexturedModel(road.getModel(), new ModelTexture(loader.loadTexture("road")));
		roadTM.getTexture().setHasTransparency(true);
		return new Entity(roadTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
	}
	
	private static void findPath() {
		long start = System.nanoTime();
		
		IProblem<Vector3f> searchProblem = new IProblem<Vector3f>() {
			
			private Vector3f end = new Vector3f(50f, 0f, 50f);
			
			private final Vector3f left = new Vector3f(-10f, 0f, 0f);
			private final Vector3f right = new Vector3f(10f, 0f, 0f);
			private final Vector3f forward = new Vector3f(0f, 0f, -10f);
			private final Vector3f backward = new Vector3f(0f, 0f, 10f);
			
			private static final double EPS = 1e-6;
			
			@Override
			public Iterable<Vector3f> getSuccessors(Vector3f state) {
				return Arrays.asList(
					Vector3f.add(state, right, null),
					Vector3f.add(state, left, null),
					Vector3f.add(state, forward, null),
					Vector3f.add(state, backward, null)
				);
			}

			@Override
			public double getTransitionCost(Vector3f first, Vector3f second) {
				return Vector3f.sub(second, first, null).length();
			}

			@Override
			public boolean isGoal(Vector3f state) {
				return Math.abs(state.x - end.x) <= EPS &&
					Math.abs(state.y - end.y) <= EPS && 
					Math.abs(state.z - end.z) <= EPS;
			}

			@Override
			public Vector3f getInitialState() {
				return new Vector3f(0f, 0f, 0f);
			}
			
		};
		

//		IProblem<Point> searchProblem = new IProblem<Point>() {
//			private Point end = new Point(300, 300);
//			
//			@Override
//			public Iterable<Point> getSuccessors(Point state) {
//				return Arrays.asList(
//					new Point(state.x - 1, state.y),
//					new Point(state.x + 1, state.y),
//					new Point(state.x, state.y - 1),
//					new Point(state.x, state.y + 1)
//				);
//			}
//	
//			@Override
//			public double getTransitionCost(Point first, Point second) {
//				return Math.abs(first.x - second.x) + Math.abs(first.y - second.y);
//			}
//	
//			@Override
//			public boolean isGoal(Point state) {
//				return state.x == end.x && state.y == end.y;
//			}
//
//			@Override
//			public Point getInitialState() {
//				return new Point(0, 0);
//			}
//			
//		};
		
//		AStar<Point> astar = new AStar<>(searchProblem, s -> 0.0);
//		Node<Point> goal = astar.search();
		
		AStar<Vector3f> astar = new AStar<>(searchProblem, s -> 0.0);
		Node<Vector3f> goal = astar.search();
		
		double duration = (System.nanoTime() - start) * 1e-9;
		System.out.println("duration:" + duration);
		
		for(Vector3f point : goal.reconstructPath()) {
			System.out.println(point);
		}
	}
	
private static class Point {
		
		private int x;
		private int y;

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public int getX() {
			return x;
		}
		
		public int getY() {
			return y;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Point other = (Point) obj;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "(" + x + ", " + y + ")";
		}
		
	}
	
}
