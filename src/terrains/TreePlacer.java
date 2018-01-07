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

}
