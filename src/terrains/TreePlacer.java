package terrains;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import org.lwjgl.util.vector.Vector3f;

import toolbox.Sampler;

public class TreePlacer {

	private IHeightGenerator heightMap;
	private BiomesMap biomesMap;
	private Sampler sampler;

	public TreePlacer(IHeightGenerator heightMap, BiomesMap biomesMap, Sampler sampler) {
		this.heightMap = heightMap;
		this.biomesMap = biomesMap;
		this.sampler = sampler;
	}
	
	public BlockingQueue<Map<TreeType, List<Vector3f>>> computeLocationsInBackground(ExecutorService pool) {
		BlockingQueue<List<Point2D.Float>> inQueue = new ArrayBlockingQueue<>(5000);
		BlockingQueue<Map<TreeType, List<Vector3f>>> outQueue = new ArrayBlockingQueue<>(5000);
		
		pool.submit(new Runnable() {
			@Override
			public void run() {
				try {
					sampler.sample(10000, inQueue);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		});
		
		pool.submit(new Runnable() {
			@Override
			public void run() {
				while(!sampler.samplingDone()) {
					List<Point2D.Float> locations = null;
					try {
						locations = inQueue.take();
						System.out.println("Placer taken from queue " + locations.size() +
								" points. In queue size: " + inQueue.size());
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(1);
					}
					
					Map<TreeType, List<Vector3f>> locationsPerType = new HashMap<>();
					for(TreeType type : TreeType.values()) {
						locationsPerType.put(type, new ArrayList<>());
					}
					
					for(Point2D.Float location : locations) {
						TreeType type = biomesMap.getTreeType(location.x, location.y);
						float height = heightMap.getHeight(location.x, location.y);
						locationsPerType.get(type).add(new Vector3f(location.x, height, location.y));
					}
					
					try {
						outQueue.put(locationsPerType);
						System.out.println("Placer put to queue " + locationsPerType.values().stream().mapToInt(l -> l.size()).sum() +
								" points. Out queue size: " + outQueue.size());
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(1);
					}
					
					System.out.println("Placer in loop.");
				}
			}
		});
		
		return outQueue;
	}
	
	public Map<TreeType, List<Vector3f>> computeLocations() {
		Map<TreeType, List<Vector3f>> locationsPerType = new HashMap<>();
		for(TreeType type : TreeType.values()) {
			locationsPerType.put(type, new ArrayList<>());
		}

		List<Point2D.Float> locations = sampler.sample();
		for(Point2D.Float location : locations) {
			TreeType type = biomesMap.getTreeType(location.x, location.y);
			float height = heightMap.getHeight(location.x, location.y);
			locationsPerType.get(type).add(new Vector3f(location.x, height, location.y));
		}
		
		return locationsPerType;
	}

}
