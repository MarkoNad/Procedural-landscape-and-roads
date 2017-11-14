package roads;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import renderEngine.Loader;
import terrains.IHeightGenerator;
import terrains.TextureMap;

public class Road {
	
	private Loader loader;
	private List<Vector3f> waypoints;
	private float width;
	private IHeightGenerator heightMap;
	private RawModel model;
	private TextureMap textureMap;

	
	public Road(Loader loader, List<Vector3f> waypoints, float width, IHeightGenerator heightMap) {
		if(waypoints == null || waypoints.size() <= 1) {
			throw new IllegalArgumentException("At least two waypoints are required.");
		}
		
		this.loader = loader;
		this.waypoints = waypoints;
		this.width = width;
		this.heightMap = heightMap;
		
		textureMap = new TextureMap(heightMap.getMaxHeight());
		
		model = generate();
		//model = generateTerrain(loader);
	}
	
	public RawModel getModel() {
		return model;
	}
	
	private RawModel generate() {
		int vertCount = 2 * waypoints.size();
		float[] vertices = new float[vertCount * 3];
		float[] normals = new float[vertCount * 3];
		float[] textureCoords = new float[vertCount * 2];
		int[] indices = new int[6 * (waypoints.size() - 1)];
		
		/*
		 * The road is drawn as follows:
		 * 
		 * up
		 * .__.__.__.__
		 * ./|./|./|./|
		 * down
		 */
		for(int wpPointer = 0; wpPointer < waypoints.size(); wpPointer++) {
			Vector3f waypoint = waypoints.get(wpPointer);
			
//			float upx = waypoint.x - width / 2f;
//			float upy = 0;
//			float upz = waypoint.z;
			float upx = waypoint.x;
			float upy = 0;
			float upz = waypoint.z - width / 2f;
			
			vertices[wpPointer * 3] = upx;
			vertices[wpPointer * 3 + 1] = upy;
			vertices[wpPointer * 3 + 2] = upz;
			
			normals[wpPointer * 3] = 0f;
			normals[wpPointer * 3 + 1] = 1f;
			normals[wpPointer * 3 + 2] = 0f;
			
			textureCoords[wpPointer * 2] = 0f;
			textureCoords[wpPointer * 2 + 1] = wpPointer % 2;
		}
		
		System.out.println("waypoints: " + waypoints.size());
		System.out.println("vertices: " + vertCount);
		System.out.println("vert array: " + vertices.length);
		
		for(int wpPointer = 0; wpPointer < waypoints.size(); wpPointer++) {
			Vector3f waypoint = waypoints.get(wpPointer);
			
//			float downx = waypoint.x + width / 2f;
//			float downy = 0;
//			float downz = waypoint.z;
			float downx = waypoint.x;
			float downy = 0;
			float downz = waypoint.z + width / 2f;
			
			System.out.println("(waypoints.size() + wpPointer) * 3:" + (waypoints.size() + wpPointer) * 3);
			vertices[(waypoints.size() + wpPointer) * 3] = downx;
			vertices[(waypoints.size() + wpPointer) * 3 + 1] = downy;
			vertices[(waypoints.size() + wpPointer) * 3 + 2] = downz;
			
			normals[(waypoints.size() + wpPointer) * 3] = 0f;
			normals[(waypoints.size() + wpPointer) * 3 + 1] = 1f;
			normals[(waypoints.size() + wpPointer) * 3 + 2] = 0f;
			
			textureCoords[(waypoints.size() + wpPointer) * 2] = 1f;
			textureCoords[(waypoints.size() + wpPointer) * 2 + 1] = wpPointer % 2;
		}
		
		int pointer = 0;
		for(int i = 0; i < waypoints.size() - 1; i++) {
			int topLeft = i;
			int topRight = topLeft + 1;
			int bottomLeft = waypoints.size() + i;
			int bottomRight = bottomLeft + 1;
			
			indices[pointer++] = topLeft;
			indices[pointer++] = bottomLeft;
			indices[pointer++] = topRight;
			indices[pointer++] = topRight;
			indices[pointer++] = bottomLeft;
			indices[pointer++] = bottomRight;
		}
		
		System.out.println(Arrays.toString(vertices));
		System.out.println(Arrays.toString(textureCoords));
		System.out.println(Arrays.toString(normals));
		System.out.println(Arrays.toString(indices));

		return loader.loadToVAO(vertices, textureCoords, normals, indices);
	}
	
}
