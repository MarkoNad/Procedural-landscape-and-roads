package terrains;

import toolbox.OpenSimplexNoise;

public class SimplexHeightGenerator extends MutableHeightMap {

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

	public SimplexHeightGenerator(long seed) {
		this(seed, DEFAULT_PREFERRED_HEIGHT, DEFAULT_BASE_FREQUENCY_MODIFIER,
				DEFAULT_FREQ_INCREASE_FACTOR, DEFAULT_OCTAVES, DEFAULT_ROUGHNESS,
				DEFAULT_HEIGHT_BIAS, DEFAULT_HEIGHT_VARIATION);
	}

	public SimplexHeightGenerator(long seed, float maxHeight, float baseFrequencyModifier,
			float freqIncreaseFactor, int octaves, float roughness, float heightBias,
			float heightVariation) {
		super(DIFF);
		
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
	}
	
	@Override
	protected float getBaseHeight(float x, float z) {
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
	public float getMaxHeight() {
		//return (float) (Math.pow(1.0f + heightBias, heightVariation) * preferredHeight);
		return preferredHeight;
	}

}
