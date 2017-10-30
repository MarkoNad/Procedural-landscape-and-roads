package terrains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Vector3f;

import terrains.BiomesMap.TreeType;

public class TreePlacer {

	private static NoiseMap noiseMap = new NoiseMap(100, 0.01f, 0);
	private IHeightGenerator heightMap;
	private BiomesMap biomesMap;
	private final float xmin;
	private final float xmax;
	private final float zmin;
	private final float zmax;
	private final float outerStep;
	private final float innerStep;
	private final float baseRadius;

	public TreePlacer(IHeightGenerator heightMap, BiomesMap biomesMap, float xmin, float xmax, float zmin, float zmax,
			float outerStep, float innerStep, float baseRadius) {
		this.heightMap = heightMap;
		this.biomesMap = biomesMap;
		this.xmin = xmin;
		this.xmax = xmax;
		this.zmin = zmin;
		this.zmax = zmax;
		this.outerStep = outerStep;
		this.innerStep = innerStep;
		this.baseRadius = baseRadius;
	}

	public Map<TreeType, List<Vector3f>> computeLocations() {
		Map<TreeType, List<Vector3f>> locations = new HashMap<>();
		for(TreeType type : TreeType.values()) {
			locations.put(type, new ArrayList<>());
		}
		
		for (int z = (int) zmin; z < zmax; z += outerStep) {
			for (int x = (int) xmin; x < xmax; x += outerStep) {
				float maxNoise = noiseMap.getPrenormalizedNoise(x, z);
				float radius = (float) (baseRadius * Math.pow((1 / biomesMap.getTreeDensity(x, z)), 2f));
				//float radius = baseRadius * (1 - biomesMap.getTreeDensity(x, z)) * (1 - biomesMap.getTreeDensity(x, z));
				
				for(int zn = (int) (z - radius); zn < z + radius; zn += innerStep) {
					for(int xn = (int) (x - radius); xn < x + radius; xn += innerStep) {
						float noise = noiseMap.getPrenormalizedNoise(xn, zn);
						if(noise > maxNoise) maxNoise = noise;
					}
				}
				
				if(Math.abs(noiseMap.getPrenormalizedNoise(x, z) - maxNoise) <= 1e-6) {
					TreeType type = biomesMap.getTreeType(x, z);
					locations.get(type).add(new Vector3f(x, heightMap.getHeight(x, z), z));
				}
			}
		}
		
		return locations;
	}

}
