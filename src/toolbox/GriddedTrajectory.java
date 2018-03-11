package toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.lwjgl.util.vector.Vector3f;

public class GriddedTrajectory {
	
	private final List<TrajectoryPoint> trajectoryPoints;
	private final Map<Point2Di, List<TrajectoryPoint>> grid;
	private final float gridCellSize;
	
	public static class TrajectoryPoint {
		private final Vector3f location;
		private final Optional<Vector3f> previous;
		private final Optional<Vector3f> next;
		
		public TrajectoryPoint(Vector3f location, Vector3f previous, Vector3f next) {
			this.location = location;
			this.previous = Optional.ofNullable(previous);
			this.next = Optional.ofNullable(next);
		}

		public Vector3f getLocation() {
			return location;
		}
		
		public Optional<Vector3f> getNext() {
			return next;
		}
		
		public Optional<Vector3f> getPrevious() {
			return previous;
		}
		
	}
	
	public GriddedTrajectory(List<Vector3f> trajectory, float influenceDistance) {
		if(trajectory == null || trajectory.isEmpty()) {
			throw new IllegalArgumentException("Invalid trajectory definition.");
		}
		
		this.trajectoryPoints = new ArrayList<>();
		this.gridCellSize = influenceDistance;
		this.grid = new HashMap<>();
		
		populateGrid(trajectory);
	}
	
	private void populateGrid(List<Vector3f> trajectory) {
		for(int i = 0; i < trajectory.size(); i++) {
			Vector3f curr = trajectory.get(i);
			Vector3f prev = i == 0 ? null : trajectory.get(i - 1);
			Vector3f next = i == trajectory.size() - 1 ? null : trajectory.get(i + 1);
			
			TrajectoryPoint tp = new TrajectoryPoint(curr, prev, next);
			this.trajectoryPoints.add(tp);
			
			Point2Di cell = cellIndex(curr.x, curr.z);
			
			List<TrajectoryPoint> pointsInCell = grid.get(cell);
			if(pointsInCell == null) {
				pointsInCell = new ArrayList<>();
				grid.put(cell, pointsInCell);
			}

			pointsInCell.add(tp);
		}
	}
	
	public Point2Di cellIndex(float x, float z) {
		Point2Di cellIndex = new Point2Di();
		cellIndex(x, z, cellIndex);
		return cellIndex;
	}
	
	public void cellIndex(float x, float z, Point2Di buffer) {
		int gridX = (int) (x / gridCellSize);
		int gridZ = (int) (z / gridCellSize);
		
		buffer.setX(gridX);
		buffer.setZ(gridZ);
	}
	
	public float getInfluenceDistance() {
		return gridCellSize;
	}
	
	public Optional<List<TrajectoryPoint>> getPointsInCell(Point2Di cellIndex) {
		return Optional.ofNullable(grid.get(cellIndex));
	}
	
}
