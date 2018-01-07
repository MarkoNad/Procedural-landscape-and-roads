package terrains;

import org.lwjgl.util.vector.Vector3f;

import toolbox.OpenSimplexNoise;

public class SimplexHeightGenerator implements IHeightGenerator {
	
/*
	Configs:
	
	##########################
	CONF 1 
	private static final float HEIGHT = 200.0f;
	private static final float BASE_FREQUENCY_MODIFIER = 0.0025f;
	private static final float FREQ_INCREASE_FACTOR = 2f;
	private static final float OCTAVES = 5;
	private static final float ROUGHNESS = 0.5f;
	private static final float SAMPLING_DISTANCE = 2f; // in meters
	
	@Override
	public float getHeight(float x, float z) {
		float totalNoise = 0;
		float normalizer = 0;
		
		for(int i = 0; i < OCTAVES; i++) {
			float frequency = (float) (BASE_FREQUENCY_MODIFIER * Math.pow(FREQ_INCREASE_FACTOR, i));
			float amplitude = (float) (Math.pow(ROUGHNESS, i));
			normalizer += amplitude;
			totalNoise += getNormalizedNoise(x * frequency, z * frequency) * amplitude;
		}
		
		totalNoise /= normalizer;
		totalNoise = (float) Math.pow(totalNoise, 1.3f);
		
		float height = totalNoise * HEIGHT;
		return height;
	}
	##########################
	
	CONF 2: hills
	##########################
	private static final float HEIGHT = 300.0f;
	private static final float BASE_FREQUENCY_MODIFIER = 0.001f;
	private static final float FREQ_INCREASE_FACTOR = 2f;
	private static final float OCTAVES = 5;
	private static final float ROUGHNESS = 0.4f;
	private static final float SAMPLING_DISTANCE = 2f; // in meters

	@Override
	public float getHeight(float x, float z) {
		float totalNoise = 0;
		float normalizer = 0;
		
		for(int i = 0; i < OCTAVES; i++) {
			float frequency = (float) (BASE_FREQUENCY_MODIFIER * Math.pow(FREQ_INCREASE_FACTOR, i));
			float amplitude = (float) (Math.pow(ROUGHNESS, i));
			normalizer += amplitude;
			totalNoise += getNormalizedNoise(x * frequency, z * frequency) * amplitude;
		}
		
		totalNoise /= normalizer;
		totalNoise = (float) Math.pow(totalNoise + 0.3f, 3f);
		
		float height = totalNoise * HEIGHT;
		return height;
	}
	##########################
	
	CONF 3: mountains
	##########################
	private static final float HEIGHT = 1000.0f;
	private static final float BASE_FREQUENCY_MODIFIER = 0.001f;
	private static final float FREQ_INCREASE_FACTOR = 2f;
	private static final float OCTAVES = 5;
	private static final float ROUGHNESS = 0.4f;
	private static final float SAMPLING_DISTANCE = 2f; // in meters
	
	@Override
	public float getHeight(float x, float z) {
		float totalNoise = 0;
		float normalizer = 0;
		
		for(int i = 0; i < OCTAVES; i++) {
			float frequency = (float) (BASE_FREQUENCY_MODIFIER * Math.pow(FREQ_INCREASE_FACTOR, i));
			float amplitude = (float) (Math.pow(ROUGHNESS, i));
			normalizer += amplitude;
			totalNoise += getNormalizedNoise(x * frequency, z * frequency) * amplitude;
		}
		
		totalNoise /= normalizer;
		totalNoise = (float) Math.pow(totalNoise + 0.3f, 6f);
		
		float height = totalNoise * HEIGHT;
		return height;
	}
	##########################
*/
	
	private static final float MAX_HEIGHT = 9000.0f;
	private static final float BASE_FREQUENCY_MODIFIER = 0.0001f; // 0.001
	private static final float FREQ_INCREASE_FACTOR = 2f;
	private static final float OCTAVES = 5;
	private static final float ROUGHNESS = 0.4f; // 0.4
	//private static final float SAMPLING_DISTANCE = 2f; // in meters
	private static final float SAMPLING_DISTANCE = 1.5f; // in meters
	
	private OpenSimplexNoise simplexNoiseGenerator;
	
	public SimplexHeightGenerator(long seed) {
		this.simplexNoiseGenerator = new OpenSimplexNoise(seed);
	}

	@Override
	public float getHeight(float x, float z) {
		float totalNoise = 0;
		float normalizer = 0;
		
		for(int i = 0; i < OCTAVES; i++) {
			float frequency = (float) (BASE_FREQUENCY_MODIFIER * Math.pow(FREQ_INCREASE_FACTOR, i));
			float amplitude = (float) (Math.pow(ROUGHNESS, i));
			normalizer += amplitude;
			totalNoise += getNormalizedNoise(x * frequency, z * frequency) * amplitude;
		}
		
		totalNoise /= normalizer;
		//totalNoise = (float) Math.pow(totalNoise + 0.25f, 6f);
		totalNoise = (float) Math.pow(totalNoise + 0.2f, 5f);
		
		float height = totalNoise * MAX_HEIGHT;
		return height;
	}
	
	private float getNormalizedNoise(float x, float z) {
		return (float) (0.5 * (simplexNoiseGenerator.eval(x, z) + 1.0f));
	}

	@Override
	public Vector3f getNormal(float x, float z) {
		float heightL = getHeight(x - SAMPLING_DISTANCE, z);
		float heightR = getHeight(x + SAMPLING_DISTANCE, z);
		float heightD = getHeight(x, z - SAMPLING_DISTANCE);
		float heightU = getHeight(x, z + SAMPLING_DISTANCE);
		
		Vector3f normal = new Vector3f(heightL - heightR, 2.0f, heightD - heightU);
		normal.normalise();
		
		return normal;
	}
	
	@Override
	public float getMaxHeight() {
		return MAX_HEIGHT;
	}

}
