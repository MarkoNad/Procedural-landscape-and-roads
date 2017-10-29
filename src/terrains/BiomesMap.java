package terrains;

import org.lwjgl.util.vector.Vector3f;

public class BiomesMap {
	
	private static NoiseMap noiseMap = new NoiseMap(450, 0.01f, 0);
	private NoiseMap moistureMap;
	private IHeightGenerator heightMap;
	private static final Vector3f Y_AXIS = new Vector3f(0f, 1f, 0f);
	
	public static enum TreeType {
		OAK,
		PINE
	}
	
	public BiomesMap(IHeightGenerator heightMap) {
		this.heightMap = heightMap;
		moistureMap = new NoiseMap(1, 0.00013f, 1);
	}
	
	public TreeType getTreeType(float x, float z) {
		float height = heightMap.getHeight(x, z);
		float modifiedHeight = height + noiseMap.getNoise(x, z);
		if(modifiedHeight < 600) return TreeType.OAK;
		return TreeType.PINE;
	}
	
	public float getTreeDensity(float x, float z) {
//		float slope = Vector3f.angle(Y_AXIS, heightMap.getNormal(x, z));
//		float moisture = moistureMap.getPrenormalizedNoise(x, z);
//		return (float) (Math.cos(slope) + moisture);
		
		float slope = Vector3f.angle(Y_AXIS, heightMap.getNormal(x, z));
		float moisture = moistureMap.getPrenormalizedNoise(x, z);
		//return (float) Math.cos(slope) * moisture;
		
		return (float) Math.cos(slope);
	}

}
