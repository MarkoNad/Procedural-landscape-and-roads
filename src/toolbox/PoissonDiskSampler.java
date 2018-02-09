package toolbox;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

public class PoissonDiskSampler extends Sampler {
	
	/** Number of points in the annulus of currently active one. */
	private static final int DEFAULT_POINTS_TO_GENERATE = 30;
	
	/** If max number of points is not specified, this one is used. */
	private static final int DEFAULT_MAX_POINTS = 100_000;
	
	/** 
	 * Minimal allowed value for the inner radius of the circular crown in 
	 * which the next point will be chosen. This is also minimum possible 
	 * distance between two points.
	 */
	private final float minInnerRadius;
	
	/**
	 * Maximal allowed value for the inner radius of the circular crown in 
	 * which the next point will be chosen (Maximal possible outer radius
	 * of the circular crown will be twice this value). This value is also 
	 * half of the maximum possible distance between two points.
	 */
	private final float maxInnerRadius;
	
	private final Point2D.Float dimensions;
	private final float cellSize;
	private final int gridWidth;
	private final int gridHeight;
	private final BiFunction<Float, Float, Float> distribution;
	private final int pointsToGenerate;
	private final int maxPoints;
	private final Random random;
	
	private boolean samplingDone;

	public PoissonDiskSampler(float x0, float z0, float x1, float z1, float minInnerRadius,
			float maxInnerRadius, BiFunction<Float, Float, Float> distribution, long seed,
			int pointsToGenerate, int maxPoints) {
		// x i z coordinates are received, but the algorithm works with x (left - right) and y 
		//(forward - backward). World negative Z axis is equivalent to Y axis here, 
		// so this coordinate system needs to be translated to match the world space. 
		super(new Point2D.Float(x0, z1), new Point2D.Float(x1, z0));
		z0 *= -1;
		z1 *= -1;
		
		this.dimensions = new Point2D.Float(x1 - x0, z1 - z0);
		this.minInnerRadius = minInnerRadius;
		this.maxInnerRadius = maxInnerRadius;
		this.distribution = distribution;
		this.random = new Random(seed);
		this.pointsToGenerate = pointsToGenerate;
		this.maxPoints = maxPoints;
		this.cellSize = (float) (maxInnerRadius / Math.sqrt(2));
		this.gridWidth = (int) (dimensions.x / cellSize) + 1;
		this.gridHeight = (int) (dimensions.y / cellSize) + 1;
		samplingDone = false;
	}

	public PoissonDiskSampler(float x0, float z0, float x1, float z1, float minInnerRadius,
			float maxInnerRadius, BiFunction<Float, Float, Float> distribution, long seed) {
		this(x0, z0, x1, z1, minInnerRadius, maxInnerRadius, distribution, seed,
				DEFAULT_POINTS_TO_GENERATE, DEFAULT_MAX_POINTS);
	}
	
	@Override
	public boolean samplingDone() {
		return samplingDone;
	}
	
	@Override
	public List<Point2D.Float> sample() {
		samplingDone = false;
		
		List<Point2D.Float> activeList = new LinkedList<>();
		List<Point2D.Float> pointList = new LinkedList<>();
		@SuppressWarnings("unchecked")
		List<Point2D.Float> grid[][] = new List[gridWidth][gridHeight];

		for(int i = 0; i < gridWidth; i++) {
			for(int j = 0; j < gridHeight; j++) {
				grid[i][j] = new LinkedList<>();
			}
		}

		addFirstPoint(grid, activeList, pointList);

		while(!activeList.isEmpty() && (pointList.size() < maxPoints)) {
			int listIndex = random.nextInt(activeList.size());

			Point2D.Float point = activeList.get(listIndex);
			boolean found = false;

			for(int k = 0; k < pointsToGenerate; k++) {
				found |= addNextPoint(grid, activeList, pointList, point);
			}

			if(!found) {
				activeList.remove(listIndex);
			}
		}

		samplingDone = true;
		return pointList;
	}
	
	@Override
	public void sample(int batchSize, BlockingQueue<List<Point2D.Float>> batchQueue)
			throws InterruptedException {
		samplingDone = false;
		
		List<Point2D.Float> activeList = new LinkedList<>();
		List<Point2D.Float> pointList = new LinkedList<>();
		@SuppressWarnings("unchecked")
		List<Point2D.Float> grid[][] = new List[gridWidth][gridHeight];

		for(int i = 0; i < gridWidth; i++) {
			for(int j = 0; j < gridHeight; j++) {
				grid[i][j] = new LinkedList<>();
			}
		}

		addFirstPoint(grid, activeList, pointList);

		while(!activeList.isEmpty() && (pointList.size() < maxPoints)) {
			int listIndex = random.nextInt(activeList.size());

			Point2D.Float point = activeList.get(listIndex);
			boolean found = false;

			for(int k = 0; k < pointsToGenerate; k++) {
				found |= addNextPoint(grid, activeList, pointList, point);
				if(pointList.size() == batchSize) {
					batchQueue.put(pointList);
					System.out.println("Sampler put to queue " + pointList.size() + " points. Queue size: " + batchQueue.size());
					pointList = new LinkedList<>();
				}
			}

			if(!found) {
				activeList.remove(listIndex);
			}
		}
		
		samplingDone = true;
	}

	private boolean addNextPoint(List<Point2D.Float>[][] grid, List<Point2D.Float> activeList,
			List<Point2D.Float> pointList, Point2D.Float point) {
		boolean found = false;
		
		float fraction = distribution.apply(point.x, point.y);
		if(fraction < 0) throw new IllegalStateException("Density distribution returned value less than 0.");
		
		float minDist =  minInnerRadius + fraction * (maxInnerRadius - minInnerRadius);
		
		Point2D.Float q = generateRandomAround(point, minDist);

		if ((q.x >= p0.x) && (q.x < p1.x) && (q.y > p0.y) && (q.y < p1.y)) {
			Point qIndex = pointFloatToInt(q, p0, cellSize);

			boolean tooClose = false;

			for (int i = Math.max(0, qIndex.x - 2); (i < Math.min(gridWidth, qIndex.x + 3)) && !tooClose; i++) {
				for (int j = Math.max(0, qIndex.y - 2); (j < Math.min(gridHeight, qIndex.y + 3)) && !tooClose; j++) {
					for (Point2D.Float gridPoint : grid[i][j]) {
						if (gridPoint.distance(q) < minDist) {
							tooClose = true;
						}
					}
				}
			}

			if (!tooClose) {
				found = true;
				activeList.add(q);
				pointList.add(q);
				grid[qIndex.x][qIndex.y].add(q);
			}
		}

		return found;
	}

	private void addFirstPoint(List<Point2D.Float>[][] grid, List<Point2D.Float> activeList, List<Point2D.Float> pointList) {
		float d = random.nextFloat();
		float xr = p0.x + dimensions.x * d;

		d = random.nextFloat();
		float yr = p0.y + dimensions.y * d;

		Point2D.Float p = new Point2D.Float(xr, yr);
		Point index = pointFloatToInt(p, p0, cellSize);

		grid[index.x][index.y].add(p);
		activeList.add(p);
		pointList.add(p);
	}

	private Point pointFloatToInt(Point2D.Float pointFloat, Point2D.Float origin, float cellSize) {
		return new Point((int) ((pointFloat.x - origin.x) / cellSize),
				(int) ((pointFloat.y - origin.y) / cellSize));
	}

	private Point2D.Float generateRandomAround(Point2D.Float centre, float minDist) {
		float d = random.nextFloat();
		float radius = (minDist + minDist * d);

		d = random.nextFloat();
		float angle = (float) (2 * Math.PI * d);

		float newX = (float) (radius * Math.sin(angle));
		float newY = (float) (radius * Math.cos(angle));

		Point2D.Float randomPoint = new Point2D.Float(centre.x + newX, centre.y + newY);

		return randomPoint;
	}
}