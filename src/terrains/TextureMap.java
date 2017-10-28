package terrains;

import java.util.ArrayList;
import java.util.List;

public class TextureMap {
	
	private List<TexMap> texMaps;
	private HeightVariationMap variationMap;
	private static final float INTERP = 20;
	private final float maxHeight;
	
	public TextureMap(float maxHeight) {
		this.variationMap = new HeightVariationMap(0, 50, 0.005f);
		this.texMaps = new ArrayList<>();
		this.maxHeight = maxHeight;
		createTexMaps();
	}
	
	private void createTexMaps() {
		texMaps.add(new TexMap(0, 100 - INTERP / 2, INTERP));
		texMaps.add(new TexMap(100 + INTERP / 2, 500 - INTERP / 2, INTERP));
		texMaps.add(new TexMap(500 + INTERP / 2, 2000 - INTERP / 2, INTERP));
	}
	
	// height can be computed directly from coordinates, it is provided here to avoid duplicate calculations
	public float[] getTextures(float height, float xcoord, float zcoord) {
		float[] texStrengths = new float[texMaps.size()];
		
		float modifiedHeight = (float) (height + variationMap.getVariation(xcoord, zcoord) * Math.pow(4 * (height + 100) / maxHeight, 1.5));
		for(int i = 0; i < texMaps.size(); i++) {
			texStrengths[i] = texMaps.get(i).getStrength(modifiedHeight);
		}
		
		return texStrengths;
	}
	
	private static class TexMap {
		private float start;
		private float end;
		private float intermediate;
		
		public TexMap(float start, float end, float intermediate) {
			if(start > end) {
				throw new IllegalArgumentException("Invalid influence function definition, condition is: start < end.");
			}
			
			this.start = start;
			this.end = end;
			this.intermediate = intermediate;
		}

		public float getStrength(float height) {
			if(height < start - intermediate || height > end + intermediate) return 0;
			if(height >= start && height <= end) return 1;
			if(height < start) return (height - (start - intermediate)) / intermediate;
			return (end + intermediate - height) / intermediate;
		}
		
	}
	
	private static class HeightVariationMap {
		
		private final float frequency;
		private OpenSimplexNoise simplexNoiseGenerator;
		private float amplitude;

		public HeightVariationMap(long seed, float amplitude, float frequency) {
			this.simplexNoiseGenerator = new OpenSimplexNoise(seed);
			this.amplitude = amplitude;
			this.frequency = frequency;
		}
		
		public float getVariation(float xcoord, float zcoord) {
			return (float) (amplitude * simplexNoiseGenerator.eval(xcoord * frequency, zcoord * frequency));
		}
		
	}

}
