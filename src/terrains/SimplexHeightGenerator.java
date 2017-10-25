package terrains;

import org.lwjgl.util.vector.Vector3f;

public class SimplexHeightGenerator implements IHeightGenerator {
	
	private static final float HEIGHT = 200.0f;
	private static final float BASE_FREQUENCY_MODIFIER = 0.0025f;
	private static final float FREQ_INCREASE_FACTOR = 2f;
	private static final float SAMPLING_DISTANCE = 2f; // in meters
	
	private final float octaves = 1;
	private final float roughness = 0.2f;
	
	private OpenSimplexNoise simplexNoiseGenerator;
	
	public SimplexHeightGenerator(long seed) {
		this.simplexNoiseGenerator = new OpenSimplexNoise(seed);
	}

	@Override
	public float getHeight(float x, float z) {
//		x *= FREQUENCY_MODIFIER;
//		z *= FREQUENCY_MODIFIER;
//		
//		float height = (float) (0.5 * (simplexNoiseGenerator.eval(x, z) + 1.0) * HEIGHT);
//		height = (float) Math.exp(height / 10);
//		return height;
		
//		return (float) (0.5 * (simplexNoiseGenerator.eval(x * BASE_FREQUENCY_MODIFIER, z * BASE_FREQUENCY_MODIFIER) + 1.0) * HEIGHT);
		
//		float height = 0;
//		float denominator = (float) Math.pow(FREQ_DIMINISH_FACTOR, octaves - 1);
//		for(int i = 0; i < octaves; i++) {
//			float frequency = (float) (BASE_FREQUENCY_MODIFIER * Math.pow(FREQ_DIMINISH_FACTOR, i) / denominator);
//			float amplitude = (float) (HEIGHT * Math.pow(roughness, i));
//			height += getNormalizedNoise(x * frequency, z * frequency) * amplitude;
//		}
//		height = (float) Math.exp(height / 12f);
//		return height;
		
//		float height = 0;
//		float normalizer = 0;
//		for(int i = 0; i < octaves; i++) {
//			float frequency = (float) (BASE_FREQUENCY_MODIFIER * Math.pow(FREQ_INCREASE_FACTOR, i - octaves + 1));
//			float amplitude = (float) (Math.pow(roughness, i));
//			normalizer += Math.pow(roughness, i);
//			height += getNormalizedNoise(x * frequency, z * frequency) * amplitude;
//		}
//		height /= normalizer;
//		height *= HEIGHT;
//		height = (float) Math.pow(height, 1.3f);
//		return height;
		
		float totalNoise = 0;
		float normalizer = 0;
		for(int i = 0; i < octaves; i++) {
			float frequency = (float) (BASE_FREQUENCY_MODIFIER * Math.pow(FREQ_INCREASE_FACTOR, i - octaves + 1));
			float amplitude = (float) (Math.pow(roughness, i));
			normalizer += amplitude;
			totalNoise += getNormalizedNoise(x * frequency, z * frequency) * amplitude;
		}
		totalNoise /= normalizer;
		totalNoise = (float) Math.pow(totalNoise, 1.3f);
		
		float height = totalNoise * HEIGHT;
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

}
