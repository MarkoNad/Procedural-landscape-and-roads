package terrains;

public class NoiseMap {
	
	private float amplitude;
	private float freq;
	private OpenSimplexNoise noise;
	
	public NoiseMap(float amplitude, float freq, long seed) {
		this.amplitude = amplitude;
		this.freq = freq;
		this.noise = new OpenSimplexNoise(seed);
	}
	
//	public float rawNoise(float x, float z) {
//		return (float) noise.eval(x * freq, z * freq);
//	}

	public float getNoise(float x, float z) {
		return (float) (noise.eval(x * freq, z * freq)) * amplitude;
	}
	
	public float getPrenormalizedNoise(float x, float z) {
		return (float) (noise.eval(x * freq, z * freq) + 1) / 2 * amplitude;
	}

}
