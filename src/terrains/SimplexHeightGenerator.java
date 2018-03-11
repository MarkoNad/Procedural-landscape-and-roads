package terrains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import toolbox.GriddedTrajectory;
import toolbox.GriddedTrajectory.TrajectoryPoint;
import toolbox.OpenSimplexNoise;
import toolbox.Point2Di;

public class SimplexHeightGenerator implements IHeightGenerator {
	
	private static final Logger LOGGER = Logger.getLogger(SimplexHeightGenerator.class.getName());

	private static final float DIFF = 1e-1f;
	private static final float DEFAULT_PREFERRED_HEIGHT = 9000.0f;
	private static final float DEFAULT_BASE_FREQUENCY_MODIFIER = 0.0001f;
	private static final float DEFAULT_FREQ_INCREASE_FACTOR = 2f;
	private static final int DEFAULT_OCTAVES = 5;
	private static final float DEFAULT_ROUGHNESS = 0.4f;
	private static final float DEFAULT_HEIGHT_BIAS = 0.2f;
	private static final float DEFAULT_HEIGHT_VARIATION = 5f;
	
	private final float preferredHeight;
	private final float baseFrequencyModifier;
	private final float freqIncreaseFactor;
	private final int octaves;
	private final float roughness;
	private final float heightBias; // larger values result with more mountains, must be positive or 0
	private final float heightVariation;
	
	private OpenSimplexNoise simplexNoiseGenerator;

	private List<GriddedTrajectory> trajectories;
	private Map<GriddedTrajectory, Function<Float, Float>> trajectoryInfluences;
	
	public SimplexHeightGenerator(long seed) {
		this(seed, DEFAULT_PREFERRED_HEIGHT, DEFAULT_BASE_FREQUENCY_MODIFIER,
				DEFAULT_FREQ_INCREASE_FACTOR, DEFAULT_OCTAVES, DEFAULT_ROUGHNESS,
				DEFAULT_HEIGHT_BIAS, DEFAULT_HEIGHT_VARIATION);
	}

	public SimplexHeightGenerator(long seed, float maxHeight, float baseFrequencyModifier,
			float freqIncreaseFactor, int octaves, float roughness, float heightBias,
			float heightVariation) {
		if(heightBias < 0) {
			throw new IllegalArgumentException("Height bias must be non-negative.");
		}
		
		this.preferredHeight = maxHeight;
		this.baseFrequencyModifier = baseFrequencyModifier;
		this.freqIncreaseFactor = freqIncreaseFactor;
		this.octaves = octaves;
		this.roughness = roughness;
		this.heightBias = heightBias;
		this.heightVariation = heightVariation;
		this.simplexNoiseGenerator = new OpenSimplexNoise(seed);
		this.trajectories = new ArrayList<>();
		this.trajectoryInfluences = new HashMap<>();
	}

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
		float heightL = heightGetter.apply(x - DIFF, z);
		float heightR = heightGetter.apply(x + DIFF, z);
		float heightD = heightGetter.apply(x, z - DIFF);
		float heightU = heightGetter.apply(x, z + DIFF);
		
		Vector3f normal = new Vector3f(
				(heightL - heightR) / (2f * DIFF),
				1f,
				(heightD - heightU) / (2f * DIFF));
		
		normal.normalise();

		return normal;
	}
	
	@Override
	public float getMaxHeight() {
		//return (float) (Math.pow(1.0f + heightBias, heightVariation) * preferredHeight);
		return preferredHeight;
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
