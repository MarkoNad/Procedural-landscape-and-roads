package terrains;

import java.util.ArrayList;
import java.util.List;

public class TextureMap {
	
	private List<TexMap> texMaps;
	
	public TextureMap() {
		this.texMaps = new ArrayList<>();
		createTexMaps();
	}
	
	private void createTexMaps() {
		texMaps.add(new TexMap(-1, 0, 500, 600));
		texMaps.add(new TexMap(500, 600, 1000, 1001));
	}
	
	public float[] getTextures(float height) {
		float[] texStrengths = new float[texMaps.size()];
		for(int i = 0; i < texMaps.size(); i++) {
			texStrengths[i] = texMaps.get(i).getStrength(height);
		}
		return texStrengths;
	}
	
	private static class TexMap {
		private float a;
		private float b;
		private float c;
		private float d;
		
		public TexMap(float a, float b, float c, float d) {
			this.a = a;
			this.b = b;
			this.c = c;
			this.d = d;
		}
		
		public float getStrength(float height) {
			if(height < a || height > d) return 0;
			if(height >= b && height <= c) return 1;
			if(height < b) return (height - a) / (b - a);
			return (d - height) / (d - c);
		}
		
	}

}
