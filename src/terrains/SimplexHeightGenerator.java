package terrains;

import org.lwjgl.util.vector.Vector3f;

public class SimplexHeightGenerator implements IHeightGenerator {
	
	private static final float HEIGHT = 70.0f;
	private static final float FREQUENCY_MODIFIER = 0.005f;
	private static final float SAMPLING_DISTANCE = 2f; // in meters
	
	private OpenSimplexNoise simplexNoiseGenerator;
	
	public SimplexHeightGenerator(long seed) {
		this.simplexNoiseGenerator = new OpenSimplexNoise(seed);
	}

	@Override
	public float getHeight(float x, float z) {
		x *= FREQUENCY_MODIFIER;
		z *= FREQUENCY_MODIFIER;
		
		return (float) (0.5 * (simplexNoiseGenerator.eval(x, z) + 1.0) * HEIGHT);
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
