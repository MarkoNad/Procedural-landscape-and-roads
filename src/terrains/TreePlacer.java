package terrains;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

public class TreePlacer {

	private IHeightGenerator noiseMap;
	private float xmin;
	private float xmax;
	private float zmin;
	private float zmax;
	private float outerStep;
	private float innerStep;
	private float r;

	public TreePlacer(IHeightGenerator heightMap, float xmin, float xmax, float zmin, float zmax, float outerStep, float innerStep, float r) {
		this.noiseMap = heightMap;
		this.xmin = xmin;
		this.xmax = xmax;
		this.zmin = zmin;
		this.zmax = zmax;
		this.outerStep = outerStep;
		this.innerStep = innerStep;
		this.r = r;
	}

	public List<Vector3f> computeLocations() {
		List<Vector3f> locations = new ArrayList<>();
		
		for (int z = (int) zmin; z < zmax; z += outerStep) {
			for (int x = (int) xmin; x < xmax; x += outerStep) {
				System.out.println("z: " + z + ", x: " + x);
				//float maxHeight = 0;
				float maxHeight = noiseMap.getHeight(x, z);
				
				for(int zn = (int) (z - r); zn < z + r; zn += innerStep) {
					for(int xn = (int) (x - r); xn < x + r; xn += innerStep) {
						float height = noiseMap.getHeight(xn, zn);
						if(height > maxHeight) maxHeight = height;
						System.out.println("  zn: " + zn + ", xn: " + xn + ", height: " + height);
					}
				}
				
				if(Math.abs(noiseMap.getHeight(x, z) - maxHeight) <= 1e-6) {
					locations.add(new Vector3f(x, maxHeight, z));
				}
				
//				locations.add(new Vector3f(x, maxHeight, z));
			}
		}
		
		return locations;
	}

}
