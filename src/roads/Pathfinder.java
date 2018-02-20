package roads;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import search.AStar;
import search.IHeuristics;
import search.IProblem;
import search.Node;
import terrains.IHeightGenerator;
import toolbox.Globals;
import toolbox.Point2Df;
import toolbox.Point2Di;

public class Pathfinder {
	
	private static final Logger LOGGER = Logger.getLogger(Pathfinder.class.getName());
	
	private final IHeightGenerator heightGenerator;
	private final PathfindingProblem searchProblem;
	private final IHeuristics<Point2Di> heuristics;
	
	public Pathfinder(Point2Df start, Point2Df goal, IHeightGenerator heightGenerator, 
			float cellSize) {
		this.heightGenerator = heightGenerator;
		searchProblem = setupProblem(start, goal, heightGenerator, cellSize);
		heuristics = setupHeuristics();
	}

	public List<Vector3f> findPath() {
		long start = System.nanoTime();
		
		AStar<Point2Di> astar = new AStar<>(searchProblem, heuristics);
		Node<Point2Di> goal = astar.search();
		
		double duration = (System.nanoTime() - start) * 1e-9;
		LOGGER.log(Level.FINE, "Astar duration:" + duration);
		
		List<Vector3f> waypoints = new ArrayList<>();
		
		for(Point2Di gridPoint : goal.reconstructPath()) {
			Point2Df point = searchProblem.gridToReal(gridPoint);
			float height = heightGenerator.getHeightApprox(point.getX(), point.getZ());;
			waypoints.add(new Vector3f(point.getX(), height, point.getZ()));
			LOGGER.log(Level.FINER, "A star point: " + point.toString());
		}
		
		return waypoints;
	}

	private IHeuristics<Point2Di> setupHeuristics() {
		return s -> 0.0;
	}
	
	private PathfindingProblem setupProblem(Point2Df start, Point2Df goal,
			IHeightGenerator heightGenerator, float cellSize) {
		return new PathfindingProblem(start, goal, heightGenerator, cellSize);
	}
	
	private static class PathfindingProblem implements IProblem<Point2Di> {
		
		private final Point2Df origin;
		private final Point2Di goal;
		private final IHeightGenerator heightGenerator;

		private final float cellSize;
		private final int step = 1;

		public PathfindingProblem(Point2Df origin, Point2Df goal, IHeightGenerator heightGenerator, 
				float cellSize) {
			this.origin = origin;
			this.heightGenerator = heightGenerator;
			this.cellSize = cellSize;
			this.goal = realToGrid(goal);
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
		public Iterable<Point2Di> getSuccessors(Point2Di state) {
			return Arrays.asList(
					new Point2Di(state.getX(), state.getZ() - step),
					new Point2Di(state.getX() + step, state.getZ() - step),
					new Point2Di(state.getX() + step, state.getZ()),
					new Point2Di(state.getX() + step, state.getZ() + step),
					new Point2Di(state.getX(), state.getZ() + step),
					new Point2Di(state.getX() - step, state.getZ() + step),
					new Point2Di(state.getX() - step, state.getZ()),
					new Point2Di(state.getX() - step, state.getZ() - step)
				);
		}

		@Override
		public double getTransitionCost(Point2Di first, Point2Di second) {
			Point2Df p1 = gridToReal(first);
			Point2Df p2 = gridToReal(second);
			
			double totalCost = 0.0;

			double y1 = heightGenerator.getHeightApprox(p1.getX(), p1.getZ());
			double y2 = heightGenerator.getHeightApprox(p2.getX(), p2.getZ());
			double distance = Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2.0) + 
					Math.pow(y1 - y2, 2.0) + Math.pow(p1.getZ() - p2.getZ(), 2.0));
			double distanceCost = distance;
			
			Vector3f normal1 = heightGenerator.getNormalApprox(p1.getX(), p1.getZ());
			Vector3f normal2 = heightGenerator.getNormalApprox(p2.getX(), p2.getZ());
			double deltaSlope = Vector3f.angle(normal1, normal2);
			double deltaSlopeCost = deltaSlope * 1000.0;
			
			double slope = Vector3f.angle(Globals.Y_AXIS, normal2);
			double slopeCost = slope * 10_000.0;
			
			totalCost += distanceCost;
			totalCost += deltaSlopeCost;
			totalCost += slopeCost;
			
			return totalCost;
		}
		
	}

}
