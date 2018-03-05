package roads;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.lwjgl.util.vector.Vector3f;

import search.AStar;
import search.IHeuristics;
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
	private final int endpointOffset;
	private final int maskOffset;
	private TrajectoryPostprocessor trajectoryPostprocessor;
	
	List<PathPoint3D> waypointsCache = null;
	List<Vector3f> trajectoryCache = null;
	
	public Pathfinder(Point2Df start, Point2Df goal, Point2Df domainLowerLeftLimit,
			Point2Df domainUpperRightLimit, IHeightGenerator heightGenerator, 
			float cellSize, boolean allowTunnels, float minimalTunnelDepth,
			int endpointOffset, int maskOffset, float tunnelInnerRadius, float tunnelOuterRadius,
			int tunnelCandidates, boolean limitTunnelCandidates, Random random) {
		this.heightGenerator = heightGenerator;
		this.minimalTunnelDepth = minimalTunnelDepth;
		this.endpointOffset = endpointOffset;
		this.maskOffset = maskOffset;
		
		this.searchProblem = new PathfindingProblem(start, goal, domainLowerLeftLimit,
				domainUpperRightLimit, heightGenerator, cellSize, allowTunnels, tunnelInnerRadius,
				tunnelOuterRadius, tunnelCandidates, limitTunnelCandidates, random);
		this.heuristics = setupHeuristics(goal);
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
	
	public List<TunnelData> findTunnelsData() {
		return trajectoryPostprocessor.getTunnelsData();
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
		gridPath.forEach(p -> LOGGER.finer("A star point: " + p));
		
		List<PathPoint> path = postProcessPath(gridPath, searchProblem);
		path.forEach(p -> LOGGER.finer("Path point: " + p));
		
		List<PathPoint3D> waypoints = setWaypointHeights(path, heightGenerator);
		waypoints.forEach(p -> LOGGER.finer("Postprocessed point: " + p));
		
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
				heightGenerator, minimalTunnelDepth, endpointOffset, maskOffset);
		
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
			if(pp.entrance) {
				firstTunnelEndpoint = p;
				firstEndpointY = heightGenerator.getHeightApprox(p.getX(), p.getZ());
				
				secondTunnelEndpoint = findNextExit(i, pathPoints);
				secondEndpointY = heightGenerator.getHeightApprox(secondTunnelEndpoint.getX(), 
						secondTunnelEndpoint.getZ());
				
				System.out.println("first: " + firstTunnelEndpoint);
				System.out.println("second: " + secondTunnelEndpoint);

				Vector3f newWaypointLoc = new Vector3f(p.getX(), firstEndpointY, p.getZ());
				waypoints.add(new PathPoint3D(newWaypointLoc, pp.entrance, pp.exit, pp.body));
				
				LOGGER.finer("Postprocessed point: " + p.toString() + firstEndpointY + 
						" TUNNEL ENTRANCE" + (pp.exit ? "AND EXIT" : ""));
				continue;
			}
			
			// tunnel ended, erase endpoint information
			if(pp.exit) {
				float height = heightGenerator.getHeightApprox(p.getX(), p.getZ());
				Vector3f newWaypointLoc = new Vector3f(p.getX(), height, p.getZ());
				waypoints.add(new PathPoint3D(newWaypointLoc, pp.entrance, pp.exit, pp.body));
				
				System.out.println("double first: " + firstTunnelEndpoint);
				System.out.println("double second: " + secondTunnelEndpoint);
				
				LOGGER.finer("Postprocessed point: " + p.toString() + firstEndpointY + " TUNNEL EXIT");
				continue;
			}
			
			// point outside tunnel
			if(!pp.body) {
				float height = heightGenerator.getHeightApprox(p.getX(), p.getZ());
				Vector3f newWaypointLoc = new Vector3f(p.getX(), height, p.getZ());
				waypoints.add(new PathPoint3D(newWaypointLoc, pp.entrance, pp.exit, pp.body));
				
				LOGGER.finer("Postprocessed point: ROAD" + p.toString() + height);
				continue;
			}
			
			// point in tunnel
			if(pp.body) {
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

				float height = lowerY + (higherY - lowerY) * fraction;

				Vector3f newWaypointLoc = new Vector3f(p.getX(), height, p.getZ());
				waypoints.add(new PathPoint3D(newWaypointLoc, pp.entrance, pp.exit, pp.body));
				
				LOGGER.finer("Postprocessed point: " + p.toString() + height + " TUNNEL BODY");
				continue;
			}
			
			LOGGER.severe("Non-classified point.");
		}

		return waypoints;
	}
	
	private Point2Df findNextExit(int i, List<PathPoint> pathPoints) {
		if(i + 1 >= pathPoints.size()) {
			throw new IllegalStateException("Tunnel entrance without matching exit found.");
		}
		
		for(int j = i + 1; j < pathPoints.size(); j++) {
			PathPoint pp = pathPoints.get(j);
			if(!pp.body && !pp.exit) LOGGER.severe("Waypoint between tunnel endpoints not in tunnel.");
			if(pp.exit) return pp.location;
		}
		
		throw new IllegalStateException("No exit after requested entrance found.");
	}
	
	private static class PathPoint {
		
		private Point2Df location;
		private boolean entrance;
		private boolean exit;
		private boolean body;
		
		public PathPoint(Point2Df location, boolean entrance, boolean exit, boolean body) {
			this.location = location;
			this.entrance = entrance;
			this.exit = exit;
			this.body = body;
		}

		@Override
		public String toString() {
			return "PathPoint [location=" + location + ", entrance=" + entrance + ", exit=" + exit + ", body=" + body
					+ "]";
		}
		
	}
	
	private List<PathPoint> postProcessPath(List<Point2Di> gridPoints, PathfindingProblem problem) {
		LOGGER.fine("Post processing A star path. Initial num of points: " + gridPoints.size());
		
		List<PathPoint> processed = new ArrayList<>();
		
		boolean nextIsEndpoint = false;
		
		for(int i = 0; i < gridPoints.size() - 1; i++) {
			Point2Df curr = problem.gridToReal(gridPoints.get(i));
			Point2Df next = problem.gridToReal(gridPoints.get(i + 1));
			
			float dist = Point2Df.distance(curr, next);

			if(dist <= problem.getMax2DRoadSize()) {
				PathPoint currTP = new PathPoint(curr, false, nextIsEndpoint, false);
				processed.add(currTP);
				nextIsEndpoint = false;
				continue;
			}
			
			int additionalPatches = (int) (dist / problem.getCellSize());
			float patchSize = dist / (float)additionalPatches;
			Point2Df direction = Point2Df.normalize(Point2Df.sub(next, curr));

			for(int j = 0; j < additionalPatches; j++) {
				float x = curr.getX() + direction.getX() * patchSize * j;
				float z = curr.getZ() + direction.getZ() * patchSize * j;
				
				Point2Df newPoint = new Point2Df(x, z);
				PathPoint newTP = new PathPoint(newPoint, j == 0, nextIsEndpoint && j == 0, j != 0);
				processed.add(newTP);
			}
			
			nextIsEndpoint = true;
		}
		
		Point2Df lastPoint = problem.gridToReal(gridPoints.get(gridPoints.size() - 1));
		PathPoint lastTP = new PathPoint(lastPoint, false, nextIsEndpoint, false);
		processed.add(lastTP);
		
		LOGGER.fine("Post processing A star path finised. Num of points: " + processed.size());
		
		return processed;
	}

}
