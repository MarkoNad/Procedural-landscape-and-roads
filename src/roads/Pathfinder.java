package roads;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.lwjgl.util.vector.Vector3f;

import search.AStar;
import search.IHeuristics;
import search.IProblem;
import search.Node;
import terrains.IHeightGenerator;
import toolbox.CatmullRomSpline;
import toolbox.Point2Df;
import toolbox.Point2Di;

public class Pathfinder {
	
	private static final Logger LOGGER = Logger.getLogger(Pathfinder.class.getName());
	
	private final IHeightGenerator heightGenerator;
	private final PathfindingProblem searchProblem;
	private final IHeuristics<Point2Di> heuristics;
	
	private final float minimalTunnelDepth;
	private TrajectoryPostprocessor trajectoryPostprocessor;
	
	List<PathPoint3D> waypointsCache = null;
	List<Vector3f> trajectoryCache = null;
	
	public Pathfinder(Point2Df start, Point2Df goal, Point2Df domainLowerLeftLimit,
			Point2Df domainUpperRightLimit, IHeightGenerator heightGenerator, 
			float cellSize, boolean allowTunnels, float minimalTunnelDepth) {
		this.heightGenerator = heightGenerator;
		this.minimalTunnelDepth = minimalTunnelDepth;
		searchProblem = setupProblem(start, goal, domainLowerLeftLimit, domainUpperRightLimit,
				heightGenerator, cellSize, allowTunnels);
		heuristics = setupHeuristics(goal);
	}
	
	public List<EndpointData> findTunnelEndpoints() {
		return trajectoryPostprocessor.getTunnelEndpoints();
	}
	
	public List<Vector3f> findWaypoints() {
		if(waypointsCache == null) {
			waypointsCache = generateWaypoints();
		}
		
		return waypointsCache.stream().map(pp -> pp.getLocation()).collect(Collectors.toList());
	}
	
	private List<PathPoint3D> generateWaypoints() {
		long start = System.nanoTime();
		
		AStar<Point2Di> astar = new AStar<>(searchProblem, heuristics);
		Node<Point2Di> goal = astar.search();
		
		double duration = (System.nanoTime() - start) * 1e-9;
		LOGGER.log(Level.FINE, "Astar duration:" + duration);
		
		List<Point2Di> gridPath = goal.reconstructPath();
		List<PathPoint> path = postProcessPath(gridPath, searchProblem);
		List<PathPoint3D> waypoints = setWaypointHeights(path, heightGenerator);
		
		return waypoints;
	}
	
	public List<Vector3f> findTrajectory(float segmentLength) {
		if(trajectoryCache == null) {
			trajectoryCache = generateTrajectory(segmentLength);
		}
		
		return trajectoryCache;
	}
	
	private List<Vector3f> generateTrajectory(float segmentLength) {
		List<Vector3f> waypoints = findWaypoints();
		
		CatmullRomSpline curve = new CatmullRomSpline(waypoints, segmentLength);
		List<Vector3f> trajectory = curve.getCurvePointsCopy();
		
		trajectoryPostprocessor = new TrajectoryPostprocessor(trajectory, waypointsCache,
				heightGenerator, minimalTunnelDepth);
		
		return trajectoryPostprocessor.getCorrectedTrajectory();
	}
	
	public List<List<Vector3f>> findModifierTrajectories(float offset) {
		List<List<Vector3f>> shiftedModifiers = new ArrayList<>();
		
		for(List<Vector3f> originalModifier : trajectoryPostprocessor.getModifierTrajectories()) {
			List<Vector3f> shiftedModifier = originalModifier
					.stream()
					.map(tp -> new Vector3f(tp.x, tp.y + offset, tp.z))
					.collect(Collectors.toList());
			shiftedModifiers.add(shiftedModifier);
		}
		
		return shiftedModifiers;
	}
	
	private List<PathPoint3D> setWaypointHeights(List<PathPoint> pathPoints, IHeightGenerator heightMap) {
		List<PathPoint3D> waypoints = new ArrayList<>();
		
		Point2Df firstTunnelEndpoint = null;
		float firstEndpointY = -1;
		Point2Df secondTunnelEndpoint = null;
		float secondEndpointY = -1;
		
		for(int i = 0; i < pathPoints.size(); i++) {
			PathPoint pp = pathPoints.get(i);
			Point2Df p = pp.location;
			
			// tunnel started, define both end points
			if(pp.isTunnelEndpoint && firstTunnelEndpoint == null) {
				firstTunnelEndpoint = p;
				firstEndpointY = heightGenerator.getHeightApprox(p.getX(), p.getZ());
				
				secondTunnelEndpoint = findNextTunnelEndpoint(i, pathPoints);
				secondEndpointY = heightGenerator.getHeightApprox(secondTunnelEndpoint.getX(), secondTunnelEndpoint.getZ());
				
				System.out.println("first: " + firstTunnelEndpoint);
				System.out.println("second: " + secondTunnelEndpoint);

				Vector3f newWaypointLoc = new Vector3f(p.getX(), firstEndpointY, p.getZ());
				waypoints.add(new PathPoint3D(newWaypointLoc, true, true));
				
				LOGGER.finer("A star point: " + p.toString() + firstEndpointY + " TUNNEL ENDPOINT");
				continue;
			}
			
			// tunnel ended, erase endpoint information
			if(pp.isTunnelEndpoint && firstTunnelEndpoint != null) {
				firstTunnelEndpoint = null;
				firstEndpointY = -1;

				float height = heightGenerator.getHeightApprox(p.getX(), p.getZ());
				Vector3f newWaypointLoc = new Vector3f(p.getX(), height, p.getZ());
				waypoints.add(new PathPoint3D(newWaypointLoc, true, true));
				
				// this endpoint is possibly also start for another tunnel
				if(i + 1 < pathPoints.size() && pathPoints.get(i + 1).isInTunnel) {
					LOGGER.finer("A star point: " + p.toString() + height + " TUNNEL DOUBLE ENDPOINT");
					
					firstTunnelEndpoint = p;
					firstEndpointY = height;
					secondTunnelEndpoint = findNextTunnelEndpoint(i, pathPoints);
					secondEndpointY = heightGenerator.getHeightApprox(secondTunnelEndpoint.getX(), secondTunnelEndpoint.getZ());
				} else {
					LOGGER.finer("A star point: " + p.toString() + height + " TUNNEL ENDPOINT");
				}
				
				System.out.println("double first: " + firstTunnelEndpoint);
				System.out.println("double second: " + secondTunnelEndpoint);
				
				continue;
			}
			
			// point outside tunnel
			if(firstTunnelEndpoint == null) {
				float height = heightGenerator.getHeightApprox(p.getX(), p.getZ());
				Vector3f newWaypointLoc = new Vector3f(p.getX(), height, p.getZ());
				waypoints.add(new PathPoint3D(newWaypointLoc, false, false));
				
				LOGGER.finer("A star point: " + p.toString() + height);
				if(pp.isInTunnel) {
					LOGGER.severe("Point should not be in tunnel.");
				}
				
				continue;
			}
			
			// point in tunnel
			if(firstTunnelEndpoint != null) {
				float lowerY = firstEndpointY < secondEndpointY ? firstEndpointY : secondEndpointY;
				float higherY = firstEndpointY < secondEndpointY ? secondEndpointY : firstEndpointY;
				
				Point2Df lowerTunnelEndpoint = firstEndpointY < secondEndpointY ? firstTunnelEndpoint : secondTunnelEndpoint;
				
				float deltaX = Math.abs(secondTunnelEndpoint.getX() - firstTunnelEndpoint.getX());
				float deltaZ = Math.abs(secondTunnelEndpoint.getZ() - firstTunnelEndpoint.getZ());
				
				float fraction;
				if(Math.abs(deltaX) > 1e-6) {
					fraction = Math.abs(p.getX() - lowerTunnelEndpoint.getX()) / deltaX;
				} else if(Math.abs(deltaZ) > 1e-6) {
					fraction = Math.abs(p.getZ() - lowerTunnelEndpoint.getZ()) / deltaZ;
				} else {
					throw new IllegalStateException("Tunnel start and end endpoints are the same.");
				}
				
				System.out.println(deltaX);
				float height = lowerY + (higherY - lowerY) * fraction;

				Vector3f newWaypointLoc = new Vector3f(p.getX(), height, p.getZ());
				waypoints.add(new PathPoint3D(newWaypointLoc, false, true));
				
				LOGGER.finer("A star point: " + p.toString() + height + " TUNNEL");
				if(!pp.isInTunnel) {
					LOGGER.severe("Point should be in tunnel.");
				}
				
				continue;
			}
		}

		return waypoints;
	}
	
	private Point2Df findNextTunnelEndpoint(int i, List<PathPoint> pathPoints) {
		if(i + 1 >= pathPoints.size()) {
			throw new IllegalStateException("Tunnel start endpoint without matching end endpoint found.");
		}
		
		for(int j = i + 1; j < pathPoints.size(); j++) {
			PathPoint pp2 = pathPoints.get(j);
			if(!pp2.isInTunnel) LOGGER.severe("Waypoint between tunnel endpoints not in tunnel.");
			if(pp2.isTunnelEndpoint) {
				return pp2.location;
			}
		}
		
		throw new IllegalStateException("No endpoint after requested one found.");
	}
	
	private static class PathPoint {
		private Point2Df location;
		private boolean isTunnelEndpoint;
		private boolean isInTunnel;
		
		public PathPoint(Point2Df location, boolean isTunnelEndpoint, boolean isInTunnel) {
			this.location = location;
			this.isTunnelEndpoint = isTunnelEndpoint;
			this.isInTunnel = isInTunnel;
		}

	}
	
	private List<PathPoint> postProcessPath(List<Point2Di> gridPoints, PathfindingProblem problem) {
		LOGGER.fine("Post processing A star path. Initial num of points: " + gridPoints.size());
		
		List<PathPoint> processed = new ArrayList<>();
		
		boolean nextPointIsTunnelEndpoint = false;
		
		for(int i = 0; i < gridPoints.size() - 1; i++) {
			Point2Df curr = problem.gridToReal(gridPoints.get(i));
			Point2Df next = problem.gridToReal(gridPoints.get(i + 1));
			
			float dist = Point2Df.distance(curr, next);
			
//			System.out.println("dist: " + dist);
			
			if(dist <= 2 * problem.getCellSize()) {
				PathPoint currTP = new PathPoint(curr, nextPointIsTunnelEndpoint, nextPointIsTunnelEndpoint);
				processed.add(currTP);
				nextPointIsTunnelEndpoint = false;
				continue;
			}
			
			int additionalPatches = (int) (dist / problem.getCellSize());
			float patchSize = dist / (float)additionalPatches;
			Point2Df direction = Point2Df.normalize(Point2Df.sub(next, curr));
			
//			System.out.println("cell size: " + problem.getCellSize());
//			System.out.println("additional patches: " + additionalPatches);
//			System.out.println("patch size: " + patchSize);
//			System.out.println("direction x: " + direction.getX());
//			System.out.println("direction z: " + direction.getZ());

			for(int j = 0; j < additionalPatches; j++) {
				float x = curr.getX() + direction.getX() * patchSize * j;
				float z = curr.getZ() + direction.getZ() * patchSize * j;
				
				Point2Df newPoint = new Point2Df(x, z);
				PathPoint newTP = new PathPoint(newPoint, j == 0, true);
				processed.add(newTP);
			}
			
			nextPointIsTunnelEndpoint = true;
		}
		
		Point2Df lastPoint = problem.gridToReal(gridPoints.get(gridPoints.size() - 1));
		PathPoint lastTP = new PathPoint(lastPoint, nextPointIsTunnelEndpoint, nextPointIsTunnelEndpoint);
		processed.add(lastTP);
		
		LOGGER.fine("Post processing A star path finised. Num of points: " + processed.size());
		
		return processed;
	}

	private IHeuristics<Point2Di> setupHeuristics(Point2Df goal) {
		double goalY = heightGenerator.getHeightApprox(goal.getX(), goal.getZ());
		
		return new IHeuristics<Point2Di>() {
			@Override
			public double getEstimatedCost(Point2Di state) {
				Point2Df point = searchProblem.gridToReal(state);
				
				double pointY = heightGenerator.getHeightApprox(point.getX(), point.getZ());
				
				double distance = Math.sqrt(
						Math.pow(point.getX() - goal.getX(), 2.0) + 
						Math.pow(pointY - goalY, 2.0) +
						Math.pow(point.getZ() - goal.getZ(), 2.0));
				
				return distance;
			}
		};
	}
	
	private PathfindingProblem setupProblem(Point2Df start, Point2Df goal,
			Point2Df domainLowerLeftLimit, Point2Df domainUpperRightLimit,
			IHeightGenerator heightGenerator, float cellSize, boolean allowTunnels) {
		return new PathfindingProblem(start, goal, domainLowerLeftLimit, domainUpperRightLimit,
				heightGenerator, cellSize, allowTunnels);
	}
	
	private static class PathfindingProblem implements IProblem<Point2Di> {
		
		private static final float samplingDist = 50f;
		
		private final Point2Df origin;
		private final Point2Di goal;
		private final Point2Df domainLowerLeftLimit;
		private final Point2Df domainUpperRightLimit;
		private final IHeightGenerator heightGenerator;
		private final boolean allowTunnels;

		private final float cellSize;
		private final float tunnelInnerRadius = 4500f;
		private final float tunnelOuterRadius = 6000f;
		private final int step = 1;

		public PathfindingProblem(Point2Df origin, Point2Df goal, Point2Df domainLowerLeftLimit, 
				Point2Df domainUpperRightLimit, IHeightGenerator heightGenerator,
				float cellSize, boolean allowTunnels) {
			this.origin = origin;
			this.domainLowerLeftLimit = domainLowerLeftLimit;
			this.domainUpperRightLimit = domainUpperRightLimit;
			this.heightGenerator = heightGenerator;
			this.cellSize = cellSize;
			this.allowTunnels = allowTunnels;
			this.goal = realToGrid(goal);
		}
		
		public float getCellSize() {
			return cellSize;
		}
		
		public Point2Df gridToReal(Point2Di gridPoint) {
			float realX = origin.getX() + gridPoint.getX() * cellSize;
			float realZ = origin.getZ() + gridPoint.getZ() * cellSize;
			return new Point2Df(realX, realZ);
		}
		
		private Point2Di realToGrid(Point2Df realPoint) {
			int gridX = (int)((realPoint.getX() - origin.getX()) / cellSize);
			int gridZ = (int)((realPoint.getZ() - origin.getZ()) / cellSize);
			return new Point2Di(gridX, gridZ);
		}

		@Override
		public Point2Di getInitialState() {
			return realToGrid(origin);
		}

		@Override
		public boolean isGoal(Point2Di state) {
			return state.equals(goal);
		}

		@Override
		public Iterable<Point2Di> getSuccessors(Point2Di p) {
			// TODO disallow double tunnels
			List<Point2Di> candidates = generateRoadCandidates(p);
			
			if(allowTunnels) {
				List<Point2Di> tunnelCandidates = generateTunnelCandidates(p, tunnelInnerRadius, tunnelOuterRadius);
				candidates.addAll(tunnelCandidates);
			}
			
			return candidates;
		}
		
		private List<Point2Di> generateRoadCandidates(Point2Di p) {
			return new ArrayList<>(Arrays.asList(
					new Point2Di(p.getX(), p.getZ() - step),
					new Point2Di(p.getX() + step, p.getZ() - step),
					new Point2Di(p.getX() + step, p.getZ()),
					new Point2Di(p.getX() + step, p.getZ() + step),
					new Point2Di(p.getX(), p.getZ() + step),
					new Point2Di(p.getX() - step, p.getZ() + step),
					new Point2Di(p.getX() - step, p.getZ()),
					new Point2Di(p.getX() - step, p.getZ() - step))
				);
		}
		
		private List<Point2Di> generateTunnelCandidates(Point2Di p, float innerRadius, float outerRadius) {
			int innerDist = (int) (innerRadius / cellSize);
			int outerDist = (int) (outerRadius / cellSize);
			
			List<Point2Di> tunnelPoints = new ArrayList<>();
			
			for(int z = p.getZ() - outerDist; z <= p.getZ() + outerDist; z++) {
				for(int x = p.getX() - outerDist; x <= p.getX() + outerDist; x++) {
					int distSquared = (p.getX() - x) *  (p.getX() - x) + (p.getZ() - z) *  (p.getZ() - z);
					
					if(distSquared > outerDist * outerDist) continue;
					if(distSquared < innerDist * innerDist) continue;
					if(gcd(Math.abs(x), Math.abs(z)) != 1) continue;
					
					Point2Di tunnelPoint = new Point2Di(x, z);
					tunnelPoints.add(tunnelPoint);
				}
			}

			return tunnelPoints;
		}
		
		private int gcd(int a, int b) {
			return b == 0 ? a : gcd(b, a % b);
		}

		@Override
		public double getTransitionCost(Point2Di first, Point2Di second) {
			if(Point2Di.l1Distance(first, second) > 2 * step) {
				return tunnelCost(first, second);
			} else {
				return roadCost(first, second);
			}
		}
		
		private double roadCost(Point2Di first, Point2Di second) {
			Point2Df p1 = gridToReal(first);
			Point2Df p2 = gridToReal(second);
			
			if(p2.getX() < domainLowerLeftLimit.getX() ||
					p2.getX() > domainUpperRightLimit.getX() ||
					p2.getZ() > domainLowerLeftLimit.getZ() || 
					p2.getZ() < domainUpperRightLimit.getZ()) {
				return Double.POSITIVE_INFINITY;
			}
			
			double totalCost = 0.0;

			double y1 = heightGenerator.getHeightApprox(p1.getX(), p1.getZ());
			double y2 = heightGenerator.getHeightApprox(p2.getX(), p2.getZ());
			
			double distance = Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2.0) + 
					Math.pow(y1 - y2, 2.0) + Math.pow(p1.getZ() - p2.getZ(), 2.0));
			double distanceCost = distance;
			
			double slope = Math.abs(y2 - y1) / distance;
			double slopeCost = Math.pow(slope, 2.0) * 10000f;
			
//			System.out.println(String.format("slope:   %.2f, slope cost:    %.2f", slope, slopeCost));
//			System.out.println(String.format("distance:%.2f, distance cost: %.2f", distance, distanceCost));
			
			totalCost += distanceCost;
			totalCost += slopeCost;
			
			return totalCost;
		}
		
		private double tunnelCost(Point2Di first, Point2Di second) {
			Point2Df p1 = gridToReal(first);
			Point2Df p2 = gridToReal(second);
			
			if(p2.getX() < domainLowerLeftLimit.getX() ||
					p2.getX() > domainUpperRightLimit.getX() ||
					p2.getZ() > domainLowerLeftLimit.getZ() || 
					p2.getZ() < domainUpperRightLimit.getZ()) {
				return Double.POSITIVE_INFINITY;
			}
			
			double totalCost = 0.0;

			double y1 = heightGenerator.getHeightApprox(p1.getX(), p1.getZ());
			double y2 = heightGenerator.getHeightApprox(p2.getX(), p2.getZ());
			
			if(!goesThroughMountain(p1, p2, (float)y1, (float)y2, samplingDist, heightGenerator)) {
				return Double.POSITIVE_INFINITY;
			}
			
			double distance = Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2.0) + 
					Math.pow(y1 - y2, 2.0) + Math.pow(p1.getZ() - p2.getZ(), 2.0));
			double distanceCost = distance * 1.1;
			
			double slope = Math.abs(y2 - y1) / distance;
			double slopeCost = Math.pow(slope, 2.0) * 10000f;
			
//			System.out.println(String.format("TUNNEL slope:   %.2f, slope cost:    %.2f", slope, slopeCost));
//			System.out.println(String.format("TUNNEL distance:%.2f, distance cost: %.2f", distance, distanceCost));
			
			totalCost += distanceCost;
			totalCost += slopeCost;
			
			return totalCost;
		}
		
		private boolean goesThroughMountain(Point2Df p1, Point2Df p2, float y1, float y2,
				float samplingDist, IHeightGenerator heightMap) {
			Point2Df direction = Point2Df.sub(p2, p1);
			direction = Point2Df.normalize(direction);
			
			final float eps = 1e-3f;
			
			float dist = Point2Df.distance(p1, p2);
			float d = dist / samplingDist;
			int samples = (int)d;
			if(d - (int)d < eps) samples--;
			
			if(samples <= 0) {
				LOGGER.severe("Number of tunnel samples is invalid: " + samples);
				return false;
			}
			
			final float lowerY = y1 < y2 ? y1 : y2;
			final float higherY = y1 < y2 ? y2 : y1;
			
			final Point2Df lowerP = y1 < y2 ? p1 : p2;
			
			final float deltaX = Math.abs(p2.getX() - p1.getX());
			final float deltaZ = Math.abs(p2.getZ() - p1.getZ());

			for(int i = 0; i < samples; i++) {
				float x = p1.getX() + direction.getX() * samplingDist * (i + 1);
				float z = p1.getZ() + direction.getZ() * samplingDist * (i + 1);

				float sampleHeight = heightMap.getHeightApprox(x, z);
				
				float fraction;
				if(Math.abs(deltaX) > 1e-6) {
					fraction = Math.abs(x - lowerP.getX()) / deltaX;
				} else if(Math.abs(deltaZ) > 1e-6) {
					fraction = Math.abs(z - lowerP.getZ()) / deltaZ;
				} else {
					LOGGER.severe("Tried to check if two same points are going through mountain.");
					return false;
				}

				float minAllowedHeight = lowerY + (higherY - lowerY) * fraction;
				
				if(sampleHeight <= minAllowedHeight) return false;
			}
			
			return true;
		}

	}

}
