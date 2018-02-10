package terrains;

import org.lwjgl.util.vector.Vector3f;

import toolbox.OpenSimplexNoise;

public class SimplexHeightGenerator implements IHeightGenerator {

	private static final float DEFAULT_PREFERRED_HEIGHT = 9000.0f;
	private static final float DEFAULT_BASE_FREQUENCY_MODIFIER = 0.0001f; // 0.001
	private static final float DEFAULT_FREQ_INCREASE_FACTOR = 2f;
	private static final float DEFAULT_OCTAVES = 5;
	private static final float DEFAULT_ROUGHNESS = 0.4f; // 0.4
	private static final float DEFAULT_SAMPLING_DISTANCE = 1.5f; // 2f
	private static final float DEFAULT_HEIGHT_BIAS = 0.2f;
	private static final float DEFAULT_HEIGHT_VARIATION = 5f;
	
	private final float preferredHeight;
	private final float baseFrequencyModifier;
	private final float freqIncreaseFactor;
	private final float octaves;
	private final float roughness;
	private final float samplingDistance;
	private final float heightBias; // larger values result with more mountains, must be positive or 0
	private final float heightVariation;
	
	private OpenSimplexNoise simplexNoiseGenerator;
	
	public SimplexHeightGenerator(long seed) {
		this(seed, DEFAULT_PREFERRED_HEIGHT, DEFAULT_BASE_FREQUENCY_MODIFIER,
				DEFAULT_FREQ_INCREASE_FACTOR, DEFAULT_OCTAVES, DEFAULT_ROUGHNESS,
				DEFAULT_SAMPLING_DISTANCE, DEFAULT_HEIGHT_BIAS, DEFAULT_HEIGHT_VARIATION);
	}

	public SimplexHeightGenerator(long seed, float maxHeight, float baseFrequencyModifier,
			float freqIncreaseFactor, float octaves, float roughness, float samplingDistance, 
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
	}

	@Override
	public float getHeight(float x, float z) {
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

	@Override
	public Vector3f getNormal(float x, float z) {
		float heightL = getHeight(x - samplingDistance, z);
		float heightR = getHeight(x + samplingDistance, z);
		float heightD = getHeight(x, z - samplingDistance);
		float heightU = getHeight(x, z + samplingDistance);
		
		Vector3f normal = new Vector3f(heightL - heightR, 2.0f, heightD - heightU);
		normal.normalise();
		
		return normal;
	}
	
	@Override
	public float getMaxHeight() {
		//return (float) (Math.pow(1.0f + heightBias, heightVariation) * preferredHeight);
		return preferredHeight;
	}

}
