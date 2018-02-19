package search;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class AStarTest {

	@Test
	public void testAStar() {
		Point goal = new Point(300, 300);
		
		IProblem<Point> searchProblem = new IProblem<Point>() {
			@Override
			public Iterable<Point> getSuccessors(Point state) {
				return Arrays.asList(
					new Point(state.x - 1, state.y),
					new Point(state.x + 1, state.y),
					new Point(state.x, state.y - 1),
					new Point(state.x, state.y + 1)
				);
			}
	
			@Override
			public double getTransitionCost(Point first, Point second) {
				return Math.abs(first.x - second.x) + Math.abs(first.y - second.y);
			}
	
			@Override
			public boolean isGoal(Point state) {
				return state.x == goal.x && state.y == goal.y;
			}
	
			@Override
			public Point getInitialState() {
				return new Point(0, 0);
			}
			
		};
	
		AStar<Point> astar = new AStar<>(searchProblem, s -> 0.0);
		Node<Point> result = astar.search();
		
		assertEquals(result.getState(), goal);
	}
	
	private static class Point {
		
		private int x;
		private int y;

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
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
