package toolbox;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

public class PoissonDiskSampler extends Sampler {
	private static final int DEFAULT_POINTS_TO_GENERATE = 30;
	private static final int MAX_POINTS = 100000;
	
	private final int pointsToGenerate;
	private final Point2D.Float dimensions;
	private final float cellSize;
	private final float minDist;
	private final int gridWidth, gridHeight;
	private final BiFunction<Float, Float, Float> distribution;
	private final Random random;

	public PoissonDiskSampler(float x0, float z0, float x1, float z1, float minDist,
			BiFunction<Float, Float, Float> distribution, long seed, int pointsToGenerate) {
		
		// x i z coordinates are received, but the algorithm works with x (left - right) and y 
		//(forward - backward). World negative Z axis is equivalent to Y axis here, 
		// so this coordinate system needs to be translated to match the world space. 
		super(new Point2D.Float(x0, z0 - z1), new Point2D.Float(x1, z1 - z1)); 
		
		dimensions = new Point2D.Float(x1 - x0, z1 - z0);
		this.minDist = minDist;
		this.distribution = distribution;
		this.random = new Random(seed);
		this.pointsToGenerate = pointsToGenerate;
		cellSize = (float) (minDist / Math.sqrt(2));
		gridWidth = (int) (dimensions.x / cellSize) + 1;
		gridHeight = (int) (dimensions.y / cellSize) + 1;
	}

	public PoissonDiskSampler(float x0, float z0, float x1, float z1, float minDist,
			BiFunction<Float, Float, Float> distribution, long seed) {
		this(x0, z0, x1, z1, minDist, distribution, seed, DEFAULT_POINTS_TO_GENERATE);
	}
	
	@Override
	public List<Point2D.Float> sample() {
		List<Point2D.Float> activeList = new LinkedList<>();
		List<Point2D.Float> pointList = new LinkedList<>();
		@SuppressWarnings("unchecked")
		List<Point2D.Float> grid[][] = new List[gridWidth][gridHeight];

		for (int i = 0; i < gridWidth; i++) {
			for (int j = 0; j < gridHeight; j++) {
				grid[i][j] = new LinkedList<>();
			}
		}

		addFirstPoint(grid, activeList, pointList);

		while (!activeList.isEmpty() && (pointList.size() < MAX_POINTS)) {
			int listIndex = random.nextInt(activeList.size());

			Point2D.Float point = activeList.get(listIndex);
			boolean found = false;

			for (int k = 0; k < pointsToGenerate; k++) {
				found |= addNextPoint(grid, activeList, pointList, point);
			}

			if (!found) {
				activeList.remove(listIndex);
			}
		}

		return pointList;
	}

	private boolean addNextPoint(List<Point2D.Float>[][] grid, List<Point2D.Float> activeList,
			List<Point2D.Float> pointList, Point2D.Float point) {
		boolean found = false;
		float fraction = distribution.apply(point.x, point.y);
		Point2D.Float q = generateRandomAround(point, fraction * minDist);

		if ((q.x >= p0.x) && (q.x < p1.x) && (q.y > p0.y) && (q.y < p1.y)) {
			Point qIndex = pointFloatToInt(q, p0, cellSize);

			boolean tooClose = false;

			for (int i = Math.max(0, qIndex.x - 2); (i < Math.min(gridWidth, qIndex.x + 3)) && !tooClose; i++) {
				for (int j = Math.max(0, qIndex.y - 2); (j < Math.min(gridHeight, qIndex.y + 3)) && !tooClose; j++) {
					for (Point2D.Float gridPoint : grid[i][j]) {
						if (gridPoint.distance(q) < minDist * fraction) {
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
		float xr = p0.x + dimensions.x * (d);

		d = random.nextFloat();
		float yr = p0.y + dimensions.y * (d);

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
		float radius = (minDist + minDist * (d));

		d = random.nextFloat();
		float angle = (float) (2 * Math.PI * (d));

		float newX = (float) (radius * Math.sin(angle));
		float newY = (float) (radius * Math.cos(angle));

		Point2D.Float randomPoint = new Point2D.Float(centre.x + newX, centre.y + newY);

		return randomPoint;
	}
}