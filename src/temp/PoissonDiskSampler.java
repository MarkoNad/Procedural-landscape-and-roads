package temp;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import terrains.BiomesMap;
import terrains.IHeightGenerator;

public class PoissonDiskSampler {
	private final static int DEFAULT_POINTS_TO_GENERATE = 30;
	public final static int MAX_POINTS = 100000;
	
	private final int pointsToGenerate;
	private final Point p0, p1;
	private final Point dimensions;
	private final double cellSize;
	private final double minDist;
	private final int gridWidth, gridHeight;
	private final BiomesMap distribution;
	private final IHeightGenerator heightMap;

	public PoissonDiskSampler(double x0, double y0, double x1, double y1, double minDist,
			BiomesMap distribution, IHeightGenerator heightMap, int pointsToGenerate) {
		y0 -= y1; // y represents z axis in world space (y = -z)
		y1 -= y1;
		p0 = new Point(x0, y0);
		p1 = new Point(x1, y1);
		dimensions = new Point(x1 - x0, y1 - y0);

		this.minDist = minDist;
		this.distribution = distribution;
		this.heightMap = heightMap;
		this.pointsToGenerate = pointsToGenerate;
		cellSize = minDist / Math.sqrt(2);
		gridWidth = (int) (dimensions.x / cellSize) + 1;
		gridHeight = (int) (dimensions.y / cellSize) + 1;
	}

	public PoissonDiskSampler(double x0, double y0, double x1, double y1, double minDist,
			BiomesMap distribution, IHeightGenerator heightGenerator) {
		this(x0, y0, x1, y1, minDist, distribution, heightGenerator, DEFAULT_POINTS_TO_GENERATE);
	}

	public List<Vector3f> sample() {
		List<Point> activeList = new LinkedList<>();
		List<Vector3f> pointList = new LinkedList<>();
		@SuppressWarnings("unchecked")
		List<Point> grid[][] = new List[gridWidth][gridHeight];

		for (int i = 0; i < gridWidth; i++) {
			for (int j = 0; j < gridHeight; j++) {
				grid[i][j] = new LinkedList<>();
			}
		}

		addFirstPoint(grid, activeList, pointList);

		while (!activeList.isEmpty() && (pointList.size() < MAX_POINTS)) {
			int listIndex = MathUtil.random.nextInt(activeList.size());

			Point point = activeList.get(listIndex);
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

	private boolean addNextPoint(List<Point>[][] grid, List<Point> activeList, List<Vector3f> pointList, Point point) {
		boolean found = false;
		//double fraction = distribution.getDouble((int) point.x, (int) point.y);
		double fraction = 1 - distribution.getTreeDensity((float)point.x, (float)point.y);
		//System.out.println(fraction);
		//double fraction = 0.3;
		//double fraction = 1;
		Point q = generateRandomAround(point, fraction * minDist);

		if ((q.x >= p0.x) && (q.x < p1.x) && (q.y > p0.y) && (q.y < p1.y)) {
			Vector2DInt qIndex = pointDoubleToInt(q, p0, cellSize);

			boolean tooClose = false;

			for (int i = Math.max(0, qIndex.x - 2); (i < Math.min(gridWidth, qIndex.x + 3)) && !tooClose; i++) {
				for (int j = Math.max(0, qIndex.y - 2); (j < Math.min(gridHeight, qIndex.y + 3)) && !tooClose; j++) {
					for (Point gridPoint : grid[i][j]) {
						if (Point.distance(gridPoint, q) < minDist * fraction) {
							tooClose = true;
						}
					}
				}
			}

			if (!tooClose) {
				found = true;
				activeList.add(q);
				pointList.add(new Vector3f((float)q.x, heightMap.getHeight((float)q.x, (float)q.y), (float)q.y));
				grid[qIndex.x][qIndex.y].add(q);
			}
		}

		return found;
	}

	private void addFirstPoint(List<Point>[][] grid, List<Point> activeList, List<Vector3f> pointList) {
		double d = MathUtil.random.nextDouble();
		double xr = p0.x + dimensions.x * (d);

		d = MathUtil.random.nextDouble();
		double yr = p0.y + dimensions.y * (d);

		Point p = new Point(xr, yr);
		Vector2DInt index = pointDoubleToInt(p, p0, cellSize);

		grid[index.x][index.y].add(p);
		activeList.add(p);
		pointList.add(new Vector3f((float)p.x, heightMap.getHeight((float)p.x, (float)p.y), (float)p.y));
	}

	static Vector2DInt pointDoubleToInt(Point pointDouble, Point origin, double cellSize) {
		return new Vector2DInt((int) ((pointDouble.x - origin.x) / cellSize),
				(int) ((pointDouble.y - origin.y) / cellSize));
	}

	static Point generateRandomAround(Point centre, double minDist) {
		double d = MathUtil.random.nextDouble();
		double radius = (minDist + minDist * (d));

		d = MathUtil.random.nextDouble();
		double angle = 2 * Math.PI * (d);

		double newX = radius * Math.sin(angle);
		double newY = radius * Math.cos(angle);

		Point randomPoint = new Point(centre.x + newX, centre.y + newY);

		return randomPoint;
	}
}