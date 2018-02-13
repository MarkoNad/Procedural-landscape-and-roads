package terrains;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import toolbox.OpenSimplexNoise;

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
	
	private Map<List<Vector3f>, Function<Float, Float>> trajectoryToInfluence;
	
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
		this.trajectoryToInfluence = new HashMap<>();
	}
	
	public void updateHeight(List<Vector3f> trajectory, Function<Float, Float> influenceDistribution) {
		trajectoryToInfluence.put(trajectory, influenceDistribution);
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
	
//	private float getInterpolatedHeight(float x, float z, List<Vector3f> trajectory,
//			Function<Float, Float> influenceDistribution, float originalHeight) {
//		float minDistSquared = -1;
//		Vector3f nearestPoint = null;
//		
//		for(Vector3f p : trajectory) {
//			float distSquared = (p.x - x) * (p.x - x) + (p.z - z) * (p.z - z);
//			if(minDistSquared == -1 || distSquared < minDistSquared) {
//				minDistSquared = distSquared;
//				nearestPoint = p;
//			}
//		}
//		
//		float dist = (float) Math.sqrt(minDistSquared);
//		
//		// influence of update height; must be between 0 and 1
//		float influence = influenceDistribution.apply(dist);
//		if(influence < 0 || influence > 1) {
//			LOGGER.severe("Invalid influence of additional height value: " + influence);
//		}
//		
//		float trajectoryHeight = nearestPoint.y;
//		
//		float newHeight = influence * trajectoryHeight + (1 - influence) * originalHeight;
//		return newHeight;
//	}
	
	private float getInterpolatedHeight(float x, float z, List<Vector3f> trajectory,
			Function<Float, Float> influenceDistribution, float originalHeight) {
		float minDistSquared = -1;
		float secondMinDistSquared = -1;
		Vector3f nearestPoint = null;
		Vector3f secondNearestPoint = null;
		
		for(Vector3f p : trajectory) {
			float distSquared = (p.x - x) * (p.x - x) + (p.z - z) * (p.z - z);
			
			if(minDistSquared == -1 && secondMinDistSquared == -1) {
				minDistSquared = distSquared;
				secondMinDistSquared = distSquared;
				nearestPoint = p;
				secondNearestPoint = p;
				continue;
			}
			
			if(distSquared < minDistSquared) {
				secondMinDistSquared = minDistSquared;
				secondNearestPoint = nearestPoint;
				minDistSquared = distSquared;
				nearestPoint = p;
				continue;
			}
			
			if(distSquared < secondMinDistSquared) {
				secondMinDistSquared = distSquared;
				secondNearestPoint = p;
				System.out.println("here1"); // TODO
				continue;
			}
		}
		
		float distanceFromTrajectorySquared = Math.min(minDistSquared, secondMinDistSquared);
		float dist = (float) Math.sqrt(distanceFromTrajectorySquared);
		
		// influence of update height; must be between 0 and 1
		float influence = influenceDistribution.apply(dist);
		if(influence < 0 || influence > 1) {
			LOGGER.severe("Invalid influence of additional height value: " + influence);
		}
		
		///float trajectoryHeight = nearestPoint.y;
		float trajectoryHeight = Math.min(nearestPoint.y, secondNearestPoint.y);
		
		float newHeight = influence * trajectoryHeight + (1 - influence) * originalHeight;
		return newHeight;
	}

	@Override
	public float getHeight(float x, float z) {
		// expensive // TODO
		float finalHeight = getBaseHeight(x, z);

		for(Map.Entry<List<Vector3f>, Function<Float, Float>> modifier : trajectoryToInfluence.entrySet()) {
			finalHeight = getInterpolatedHeight(x, z, modifier.getKey(), modifier.getValue(), finalHeight);
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

}
