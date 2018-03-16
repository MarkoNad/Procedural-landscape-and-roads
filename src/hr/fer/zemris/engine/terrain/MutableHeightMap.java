package hr.fer.zemris.engine.terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import hr.fer.zemris.engine.util.GriddedTrajectory;
import hr.fer.zemris.engine.util.Point2Di;
import hr.fer.zemris.engine.util.GriddedTrajectory.TrajectoryPoint;

public abstract class MutableHeightMap implements IHeightMap {
	
	private static final Logger LOGGER = Logger.getLogger(MutableHeightMap.class.getName());

	private final float diff;
	private List<GriddedTrajectory> trajectories;
	private Map<GriddedTrajectory, Function<Float, Float>> trajectoryInfluences;
	
	public MutableHeightMap(float diff) {
		this.diff = diff;
		this.trajectories = new ArrayList<>();
		this.trajectoryInfluences = new HashMap<>();
	}
	
	protected abstract float getBaseHeight(float x, float z);
	
	@Override
	public float getHeight(float x, float z) {
		float finalHeight = getBaseHeight(x, z);

		for(GriddedTrajectory griddedTrajectory : trajectories) {
			finalHeight = getInterpolatedHeight(x, z, finalHeight, griddedTrajectory);
		}
		
		return finalHeight;
	}
	
	@Override
	public float getHeightApprox(float x, float z) {
		return getBaseHeight(x, z);
	}
	
	@Override
	public Vector3f getNormal(float x, float z) {
		return getGenericNormal(x, z, diff, this::getHeight);
	}
	
	@Override
	public Vector3f getNormalApprox(float x, float z) {
		return getGenericNormal(x, z, diff, this::getHeightApprox);
	}
	
	private Vector3f getGenericNormal(float x, float z, float diff,
			BiFunction<Float, Float, Float> heightGetter) {
		float heightL = heightGetter.apply(x - diff, z);
		float heightR = heightGetter.apply(x + diff, z);
		float heightD = heightGetter.apply(x, z - diff);
		float heightU = heightGetter.apply(x, z + diff);
		
		Vector3f normal = new Vector3f(
				(heightL - heightR) / (2f * diff),
				1f,
				(heightD - heightU) / (2f * diff));
		
		normal.normalise();

		return normal;
	}

	public void updateHeight(List<Vector3f> trajectory, Function<Float, Float> influenceDistribution, 
			float influenceDistance) {
		GriddedTrajectory griddedTrajectory = new GriddedTrajectory(trajectory, influenceDistance);
		trajectories.add(griddedTrajectory);
		trajectoryInfluences.put(griddedTrajectory, influenceDistribution);
	}
	
	private float getInterpolatedHeight(float x, float z, float originalHeight,
			GriddedTrajectory griddedTrajectory) {
		Point2Di middleCellIndex = griddedTrajectory.cellIndex(x, z);
		int middleX = (int) (middleCellIndex.getX());
		int middleZ = (int) (middleCellIndex.getZ());
		
		float minDistSquared = -1;
		TrajectoryPoint nearestTPoint = null;
		
		Point2Di indexBuf = new Point2Di(middleX, middleZ);
		for(int gridZ = middleZ - 1; gridZ <= middleZ + 1; gridZ++) {
			for(int gridX = middleX - 1; gridX <= middleX + 1; gridX++) {
				indexBuf.setX(gridX);
				indexBuf.setZ(gridZ);

				Optional<List<TrajectoryPoint>> pointsInCell = griddedTrajectory.getPointsInCell(indexBuf);
				if(!pointsInCell.isPresent()) continue;
				
				for(TrajectoryPoint tp : pointsInCell.get()) {
					Vector3f p = tp.getLocation();
					
					float distSquared = (p.x - x) * (p.x - x) + (p.z - z) * (p.z - z);
					
					if(minDistSquared == -1 || distSquared < minDistSquared) {
						minDistSquared = distSquared;
						nearestTPoint = tp;
					}
				}
			}
		}
		
		if(nearestTPoint == null) return originalHeight;

		Optional<Vector3f> previousP = nearestTPoint.getPrevious();
		Optional<Vector3f> nextP = nearestTPoint.getNext();
		
		float secondMinDistSquared = -1;
		Vector3f secondNearestPoint = null;
		
		if(previousP.isPresent()) {
			float distSquared = (previousP.get().x - x) * (previousP.get().x - x) +
					(previousP.get().z - z) * (previousP.get().z - z);
			secondMinDistSquared = distSquared;
			secondNearestPoint = previousP.get();
		}
		
		if(nextP.isPresent()) {
			float distSquared = (nextP.get().x - x) * (nextP.get().x - x) +
					(nextP.get().z - z) * (nextP.get().z - z);
			if(secondMinDistSquared == -1 || distSquared < secondMinDistSquared) {
				secondMinDistSquared = distSquared;
				secondNearestPoint = nextP.get();
			}
		}
		
		float distanceFromTrajectorySquared = Math.min(minDistSquared, secondMinDistSquared);
		float dist = (float) Math.sqrt(distanceFromTrajectorySquared);
		
		// influence of update height; must be between 0 and 1
		float influence = trajectoryInfluences.get(griddedTrajectory).apply(dist);
		if(influence < 0 || influence > 1) {
			LOGGER.severe("Invalid influence of additional height value: " + influence);
		}
		
		float trajectoryHeight = Math.min(nearestTPoint.getLocation().y, secondNearestPoint.y);
		
		float newHeight = influence * trajectoryHeight + (1 - influence) * originalHeight;
		return newHeight;
	}

}
