package roads;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import renderEngine.Loader;
import terrains.IHeightGenerator;
import toolbox.Constants;

public class Road {
	
	private Loader loader;
	private List<Vector3f> waypoints;
	private float width;
	private IHeightGenerator heightMap;
	private RawModel model;

	public Road(Loader loader, List<Vector3f> waypoints, float width, IHeightGenerator heightMap) {
		if(waypoints == null || waypoints.size() <= 1) {
			throw new IllegalArgumentException("At least two waypoints are required.");
		}
		
		this.loader = loader;
		this.waypoints = waypoints;
		this.width = width;
		this.heightMap = heightMap;
		
		model = generate();
	}
	
	public RawModel getModel() {
		return model;
	}
	
	private RawModel generate() {
		final int vertCount = 2 * waypoints.size();
		final int tripletsOffset = 3 * waypoints.size();
		final int tuplesOffset = 2 * waypoints.size();
		
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
		
		// first waypoint
		Vector3f direction = Vector3f.sub(waypoints.get(1), waypoints.get(0), null);
		Vector3f right = Vector3f.cross(direction, Constants.Y_AXIS, null).normalise(null);
		Vector3f leftV = Vector3f.add(right.negate(null), waypoints.get(0), null);
		Vector3f rightV = Vector3f.add(right, waypoints.get(0), null);
		vertices[0] = leftV.x;
		vertices[1] = leftV.y;
		vertices[2] = leftV.z;
		vertices[0 + tripletsOffset] = rightV.x;
		vertices[1 + tripletsOffset] = rightV.y;
		vertices[2 + tripletsOffset] = rightV.z;
		
		// last waypoint
		direction = Vector3f.sub(waypoints.get(waypoints.size()-1), waypoints.get(waypoints.size()-2), null);
		right = Vector3f.cross(direction, Constants.Y_AXIS, null).normalise(null);
		leftV = Vector3f.add(right.negate(null), waypoints.get(waypoints.size()-1), null);
		rightV = Vector3f.add(right, waypoints.get(waypoints.size()-1), null);
		vertices[(waypoints.size() - 1) * 3] = leftV.x;
		vertices[(waypoints.size() - 1) * 3 + 1] = leftV.y;
		vertices[(waypoints.size() - 1) * 3 + 2] = leftV.z;
		vertices[(2 * waypoints.size() - 1) * 3] = rightV.x;
		vertices[(2 * waypoints.size() - 1) * 3 + 1] = rightV.y;
		vertices[(2 * waypoints.size() - 1) * 3 + 2] = rightV.z;
		
		// uncomment
		
//		for(int wpPointer = 1; wpPointer < waypoints.size() - 1; wpPointer++) {
//			Vector3f prev = waypoints.get(wpPointer - 1);
//			Vector3f curr = waypoints.get(wpPointer);
//			Vector3f next = waypoints.get(wpPointer + 1);
//			
//			Vector3f prevDirection = Vector3f.sub(curr, prev, null);
//			Vector3f nextDirection = Vector3f.sub(next, curr, null);
//			
//			prevDirection.y = 0;
//			nextDirection.y = 0;
//			
//			// centerline of angle between prev and next direction vectors
//			Vector3f centerlineDir = Vector3f.sub(nextDirection.normalise(null), prevDirection.normalise(null), null).normalise(null);
//			
//			// road curvature at current waypoint
//			float angle = Vector3f.angle(nextDirection, prevDirection.negate(null));
//			
//			// distance from new two vertices from the waypoint
//			float offset = (float) (width / Math.sin(angle / 2f));
//			
//			float upx = curr.x + ;
//			float upy = 0;
//			float upz = waypoint.z - width / 2f;
//			
//			float downx = waypoint.x;
//			float downy = 0;
//			float downz = waypoint.z + width / 2f;
//			
//			vertices[wpPointer * 3] = upx;
//			vertices[wpPointer * 3 + 1] = upy;
//			vertices[wpPointer * 3 + 2] = upz;
//			
//			vertices[(waypoints.size() + wpPointer) * 3] = downx;
//			vertices[(waypoints.size() + wpPointer) * 3 + 1] = downy;
//			vertices[(waypoints.size() + wpPointer) * 3 + 2] = downz;
//		}
		
		for(int wpPointer = 0; wpPointer < waypoints.size(); wpPointer++) {
			normals[wpPointer * 3] = 0f;
			normals[wpPointer * 3 + 1] = 1f;
			normals[wpPointer * 3 + 2] = 0f;
			
			normals[(waypoints.size() + wpPointer) * 3] = 0f;
			normals[(waypoints.size() + wpPointer) * 3 + 1] = 1f;
			normals[(waypoints.size() + wpPointer) * 3 + 2] = 0f;
			
			textureCoords[wpPointer * 2] = 0f;
			textureCoords[wpPointer * 2 + 1] = wpPointer % 2;
			
			textureCoords[(waypoints.size() + wpPointer) * 2] = 1f;
			textureCoords[(waypoints.size() + wpPointer) * 2 + 1] = wpPointer % 2;
		}
		
		System.out.println("waypoints: " + waypoints.size());
		System.out.println("vertices: " + vertCount);
		System.out.println("vert array: " + vertices.length);
		
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
