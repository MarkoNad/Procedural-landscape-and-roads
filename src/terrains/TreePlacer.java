package terrains;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Vector3f;

import terrains.BiomesMap.TreeType;
import toolbox.Sampler;

public class TreePlacer {

	//private static NoiseMap noiseMap = new NoiseMap(100, 0.01f, 0);
	private IHeightGenerator heightMap;
	private BiomesMap biomesMap;
	private Sampler sampler;

	public TreePlacer(IHeightGenerator heightMap, BiomesMap biomesMap, Sampler sampler) {
		this.heightMap = heightMap;
		this.biomesMap = biomesMap;
		this.sampler = sampler;
	}
	
	public Map<TreeType, List<Vector3f>> computeLocations() {
		Map<TreeType, List<Vector3f>> locationsPerType = new HashMap<>();
		for(TreeType type : TreeType.values()) {
			locationsPerType.put(type, new ArrayList<>());
		}
		
		List<Point2D.Float> locations = sampler.sample();
		for(Point2D.Float location : locations) {
			TreeType type = biomesMap.getTreeType(location.x, location.y);
			float height = heightMap.getHeight(location.x, location.y);
			locationsPerType.get(type).add(new Vector3f(location.x, height, location.y));
		}
		
		return locationsPerType;
}

//	public Map<TreeType, List<Vector3f>> computeLocations() {
//		Map<TreeType, List<Vector3f>> locations = new HashMap<>();
//		for(TreeType type : TreeType.values()) {
//			locations.put(type, new ArrayList<>());
//		}
//		
//		for (int z = (int) zmin; z < zmax; z += outerStep) {
//			for (int x = (int) xmin; x < xmax; x += outerStep) {
//				float maxNoise = noiseMap.getPrenormalizedNoise(x, z);
//				float radius = (float) (baseRadius * Math.pow((1 / biomesMap.getTreeDensity(x, z)), 2f));
//				//float radius = baseRadius * (1 - biomesMap.getTreeDensity(x, z)) * (1 - biomesMap.getTreeDensity(x, z));
//				
//				for(int zn = (int) (z - radius); zn < z + radius; zn += innerStep) {
//					for(int xn = (int) (x - radius); xn < x + radius; xn += innerStep) {
//						float noise = noiseMap.getPrenormalizedNoise(xn, zn);
//						if(noise > maxNoise) maxNoise = noise;
//					}
//				}
//				
//				if(Math.abs(noiseMap.getPrenormalizedNoise(x, z) - maxNoise) <= 1e-6) {
//					TreeType type = biomesMap.getTreeType(x, z);
//					locations.get(type).add(new Vector3f(x, heightMap.getHeight(x, z), z));
//				}
//			}
//		}
//		
//		return locations;
//	}

}
