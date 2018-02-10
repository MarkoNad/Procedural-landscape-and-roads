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

import toolbox.PoissonDiskSampler;
import toolbox.QueueProduct;
import toolbox.Sampler;

public class TreePlacer {
	
	public static final QueueProduct<Map<TreeType, List<Vector3f>>> THREAD_POISON = new QueueProduct<>(null);
	
	private static final int samplerQueueSize = 5000;
	private static final int outQueueSize = 5000;
	
	private static final int BATCH_SIZE = 500;

	private IHeightGenerator heightMap;
	private BiomesMap biomesMap;
	private Sampler sampler;

	public TreePlacer(IHeightGenerator heightMap, BiomesMap biomesMap, Sampler sampler) {
		this.heightMap = heightMap;
		this.biomesMap = biomesMap;
		this.sampler = sampler;
	}
	
	public BlockingQueue<QueueProduct<Map<TreeType, List<Vector3f>>>>
	computeLocationsInBackground(ExecutorService pool) {
		BlockingQueue<QueueProduct<List<Point2D.Float>>> inQueue =
				new ArrayBlockingQueue<>(samplerQueueSize);
		BlockingQueue<QueueProduct<Map<TreeType, List<Vector3f>>>> outQueue =
				new ArrayBlockingQueue<>(outQueueSize);
		
		pool.submit(new Runnable() {
			@Override
			public void run() {
				try {
					sampler.sample(BATCH_SIZE, inQueue);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		});
		
		pool.submit(new Runnable() {
			@Override
			public void run() {
				while(true) {
					List<Point2D.Float> locations = null;
					try {
						QueueProduct<List<Point2D.Float>> product = inQueue.take();
						
						if(product == PoissonDiskSampler.THREAD_POISON) {
							outQueue.put(TreePlacer.THREAD_POISON);
							System.out.println("Tree placer received poison.");
							System.out.println("Tree placer put poison.");
							break;
						}
						
						locations = product.getValue();
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
						outQueue.put(new QueueProduct<>(locationsPerType));
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
