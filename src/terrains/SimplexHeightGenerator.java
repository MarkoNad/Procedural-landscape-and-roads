package terrains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import toolbox.OpenSimplexNoise;
import toolbox.Point2Di;

public class SimplexHeightGenerator implements IHeightGenerator {
	
	private static final Logger LOGGER = Logger.getLogger(SimplexHeightGenerator.class.getName());

	private static final float DEFAULT_PREFERRED_HEIGHT = 9000.0f;
	private static final float DEFAULT_BASE_FREQUENCY_MODIFIER = 0.0001f; // 0.001
	private static final float DEFAULT_FREQ_INCREASE_FACTOR = 2f;
	private static final int DEFAULT_OCTAVES = 5;
	private static final float DEFAULT_ROUGHNESS = 0.4f; // 0.4
	//private static final float DEFAULT_SAMPLING_DISTANCE = 1.5f; // 2f // TODO
	private static final float DEFAULT_SAMPLING_DISTANCE = 40f; // 2f
	private static final float DEFAULT_HEIGHT_BIAS = 0.2f;
	private static final float DEFAULT_HEIGHT_VARIATION = 5f;
	
	private final float preferredHeight;
	private final float baseFrequencyModifier;
	private final float freqIncreaseFactor;
	private final int octaves;
	private final float roughness;
	private final float samplingDistance;
	private final float heightBias; // larger values result with more mountains, must be positive or 0
	private final float heightVariation;
	
	private OpenSimplexNoise simplexNoiseGenerator;

	private List<TrajectoryData> trajectories;
	
	public SimplexHeightGenerator(long seed) {
		this(seed, DEFAULT_PREFERRED_HEIGHT, DEFAULT_BASE_FREQUENCY_MODIFIER,
				DEFAULT_FREQ_INCREASE_FACTOR, DEFAULT_OCTAVES, DEFAULT_ROUGHNESS,
				DEFAULT_SAMPLING_DISTANCE, DEFAULT_HEIGHT_BIAS, DEFAULT_HEIGHT_VARIATION);
	}

	public SimplexHeightGenerator(long seed, float maxHeight, float baseFrequencyModifier,
			float freqIncreaseFactor, int octaves, float roughness, float samplingDistance, 
			float heightBias, float heightVariation) {
		if(heightBias < 0) {
			throw new IllegalArgumentException("Height bias must be non-negative.");
		}
		
		this.preferredHeight = maxHeight;
		this.baseFrequencyModifier = baseFrequencyModifier;
		this.freqIncreaseFactor = freqIncreaseFactor;
		this.octaves = octaves;
		this.roughness = roughness;
		this.samplingDistance = samplingDistance;
		this.heightBias = heightBias;
		this.heightVariation = heightVariation;
		this.simplexNoiseGenerator = new OpenSimplexNoise(seed);
		this.trajectories = new ArrayList<>();
	}

	@Override
	public float getHeight(float x, float z) {
		float finalHeight = getBaseHeight(x, z);

		for(TrajectoryData trajectoryData : trajectories) {
			finalHeight = getInterpolatedHeight(x, z, finalHeight, trajectoryData);
		}
		
		return finalHeight;
	}
	
	@Override
	public float getHeightApprox(float x, float z) {
		return getBaseHeight(x, z);
	}

	@Override
	public Vector3f getNormal(float x, float z) {
		return getGenericNormal(x, z, this::getHeight);
	}
	
	@Override
	public Vector3f getNormalApprox(float x, float z) {
		return getGenericNormal(x, z, this::getHeightApprox);
	}
	
	private float getBaseHeight(float x, float z) {
		float totalNoise = 0;
		float normalizer = 0;
		
		for(int i = 0; i < octaves; i++) {
			float frequency = (float) (baseFrequencyModifier * Math.pow(freqIncreaseFactor, i));
			float amplitude = (float) (Math.pow(roughness, i));
			normalizer += amplitude;
			totalNoise += getNormalizedNoise(x * frequency, z * frequency) * amplitude;
		}
		
		totalNoise /= normalizer;
		totalNoise = (float) Math.pow(totalNoise + heightBias, heightVariation);
		
		float height = totalNoise * preferredHeight;
		return height;
	}
	
	private float getNormalizedNoise(float x, float z) {
		return (float) (0.5 * (simplexNoiseGenerator.eval(x, z) + 1.0f));
	}
	
	private Vector3f getGenericNormal(float x, float z, BiFunction<Float, Float, Float> heightGetter) {
		float heightL = heightGetter.apply(x - samplingDistance, z);
		float heightR = heightGetter.apply(x + samplingDistance, z);
		float heightD = heightGetter.apply(x, z - samplingDistance);
		float heightU = heightGetter.apply(x, z + samplingDistance);
		
		Vector3f normal = new Vector3f(heightL - heightR, 2 * samplingDistance, heightD - heightU);
		normal.normalise();
		
		return normal;
	}
	
	@Override
	public float getMaxHeight() {
		//return (float) (Math.pow(1.0f + heightBias, heightVariation) * preferredHeight);
		return preferredHeight;
	}
	
	private static class TrajectoryData {
		private final List<Vector3f> trajectory;
		private final Function<Float, Float> influenceDistribution;
		private final Map<Point2Di, List<TrajectoryPoint>> grid;
		private final float gridCellSize;
		
		public TrajectoryData(List<Vector3f> trajectory, Function<Float, Float> influenceDistribution,
				float influenceDistance) {
			if(trajectory == null || trajectory.isEmpty()) {
				throw new IllegalArgumentException("Invalid trajectory definition.");
			}
			
			this.trajectory = trajectory;
			this.influenceDistribution = influenceDistribution;
			this.gridCellSize = influenceDistance;
			this.grid = new HashMap<>();
			
			populateGrid();
		}
		
		private void populateGrid() {
			for(int i = 0; i < trajectory.size(); i++) {
				Vector3f curr = trajectory.get(i);
				Vector3f prev = i == 0 ? null : trajectory.get(i - 1);
				Vector3f next = i == trajectory.size() - 1 ? null : trajectory.get(i + 1);
				TrajectoryPoint tp = new TrajectoryPoint(curr, prev, next);
				
				Point2Di cell = index(curr.x, curr.z);
				
				List<TrajectoryPoint> pointsInCell = grid.get(cell);
				if(pointsInCell == null) {
					pointsInCell = new ArrayList<>();
					grid.put(cell, pointsInCell);
				}

				pointsInCell.add(tp);
			}
		}
		
		private Point2Di index(float x, float z) {
			int gridX = (int) (x / gridCellSize);
			int gridZ = (int) (z / gridCellSize);
			return new Point2Di(gridX, gridZ);
		}
		
	}
	
	private static class TrajectoryPoint {
		private final Vector3f point;
		private final Vector3f previous;
		private final Vector3f next;
		
		public TrajectoryPoint(Vector3f point, Vector3f previous, Vector3f next) {
			this.point = point;
			this.previous = previous;
			this.next = next;
		}

	}
	
	public void updateHeight(List<Vector3f> trajectory, Function<Float, Float> influenceDistribution, 
			float influenceDistance) {
		TrajectoryData data = new TrajectoryData(trajectory, influenceDistribution, influenceDistance);
		trajectories.add(data);
	}
	
	private float getInterpolatedHeight(float x, float z, float originalHeight,
			TrajectoryData trajectoryData) {
		int middleX = (int) (x / trajectoryData.gridCellSize);
		int middleZ = (int) (z / trajectoryData.gridCellSize);
		
		float minDistSquared = -1;
		TrajectoryPoint nearestTPoint = null;
		
		Point2Di indexBuf = new Point2Di(middleX, middleZ);
		for(int gridZ = middleZ - 1; gridZ <= middleZ + 1; gridZ++) {
			for(int gridX = middleX - 1; gridX <= middleX + 1; gridX++) {
				indexBuf.setX(gridX);
				indexBuf.setZ(gridZ);

				List<TrajectoryPoint> pointsInCell = trajectoryData.grid.get(indexBuf);
				
				if(pointsInCell == null) continue;
				
				for(TrajectoryPoint tp : pointsInCell) {
					Vector3f p = tp.point;
					
					float distSquared = (p.x - x) * (p.x - x) + (p.z - z) * (p.z - z);
					
					if(minDistSquared == -1 || distSquared < minDistSquared) {
						minDistSquared = distSquared;
						nearestTPoint = tp;
					}
				}
			}
		}
		
		if(nearestTPoint == null) return originalHeight;

		Vector3f previousP = nearestTPoint.previous;
		Vector3f nextP = nearestTPoint.next;
		
		float secondMinDistSquared = -1;
		Vector3f secondNearestPoint = null;
		
		if(previousP != null) {
			float distSquared = (previousP.x - x) * (previousP.x - x) + (previousP.z - z) * (previousP.z - z);
			secondMinDistSquared = distSquared;
			secondNearestPoint = previousP;
		}
		
		if(nextP != null) {
			float distSquared = (nextP.x - x) * (nextP.x - x) + (nextP.z - z) * (nextP.z - z);
			if(secondMinDistSquared == -1 || distSquared < secondMinDistSquared) {
				secondMinDistSquared = distSquared;
				secondNearestPoint = nextP;
			}
		}
		
		float distanceFromTrajectorySquared = Math.min(minDistSquared, secondMinDistSquared);
		float dist = (float) Math.sqrt(distanceFromTrajectorySquared);
		
		// influence of update height; must be between 0 and 1
		float influence = trajectoryData.influenceDistribution.apply(dist);
		if(influence < 0 || influence > 1) {
			LOGGER.severe("Invalid influence of additional height value: " + influence);
		}
		
		float trajectoryHeight = Math.min(nearestTPoint.point.y, secondNearestPoint.y);
		
		float newHeight = influence * trajectoryHeight + (1 - influence) * originalHeight;
		return newHeight;
	}

}
