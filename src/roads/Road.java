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
		Vector3f right = (Vector3f) Vector3f.cross(direction, Constants.Y_AXIS, null).normalise(null).scale(width);
		Vector3f leftV = Vector3f.add(right.negate(null), waypoints.get(0), null);
		Vector3f rightV = Vector3f.add(right, waypoints.get(0), null);
		float leftH = heightMap.getHeight(leftV.x, leftV.z);
		float rightH = heightMap.getHeight(rightV.x, rightV.z);
		float h = Math.max(leftH, rightH);
		vertices[0] = leftV.x;
		//vertices[1] = h;
		vertices[2] = leftV.z;
		vertices[0 + 3 * waypoints.size()] = rightV.x;
		//vertices[1 + 3 * waypoints.size()] = h;
		vertices[2 + 3 * waypoints.size()] = rightV.z;
		//waypoints.get(0).setY(h);

		
		// last waypoint
		direction = Vector3f.sub(waypoints.get(waypoints.size()-1), waypoints.get(waypoints.size()-2), null);
		right = (Vector3f) Vector3f.cross(direction, Constants.Y_AXIS, null).normalise(null).scale(width);
		leftV = Vector3f.add(right.negate(null), waypoints.get(waypoints.size()-1), null);
		rightV = Vector3f.add(right, waypoints.get(waypoints.size()-1), null);
		leftH = heightMap.getHeight(leftV.x, leftV.z);
		rightH = heightMap.getHeight(rightV.x, rightV.z);
		h = Math.max(leftH, rightH);
		vertices[(waypoints.size() - 1) * 3] = leftV.x;
		//vertices[(waypoints.size() - 1) * 3 + 1] = h;
		vertices[(waypoints.size() - 1) * 3 + 2] = leftV.z;
		vertices[(2 * waypoints.size() - 1) * 3] = rightV.x;
		//vertices[(2 * waypoints.size() - 1) * 3 + 1] = h;
		vertices[(2 * waypoints.size() - 1) * 3 + 2] = rightV.z;
		//waypoints.get(waypoints.size() - 1).setY(h);
		
		for(int wpPointer = 1; wpPointer < waypoints.size() - 1; wpPointer++) {
			Vector3f prev = waypoints.get(wpPointer - 1);
			Vector3f curr = waypoints.get(wpPointer);
			Vector3f next = waypoints.get(wpPointer + 1);
			
			Vector3f prevDirection = Vector3f.sub(curr, prev, null).normalise(null);
			Vector3f nextDirection = Vector3f.sub(next, curr, null).normalise(null);
			
			prevDirection.y = 0;
			nextDirection.y = 0;
			
			// vector pointing to the right of the prev and next directions, used for centerline
			Vector3f prevRight = (Vector3f) Vector3f.cross(prevDirection, Constants.Y_AXIS, null).normalise(null);
			Vector3f nextRight = (Vector3f) Vector3f.cross(nextDirection, Constants.Y_AXIS, null).normalise(null);
			Vector3f centerlineDir = Vector3f.add(prevRight, nextRight, null).normalise(null);
			
			// centerline of angle between prev and next direction vectors, points to the right
			System.out.println("centerline: " + centerlineDir);
			
			// road curvature at current waypoint
			float angle = Vector3f.angle(nextDirection, prevDirection.negate(null));
			System.out.println("angle: " + angle);
			
			// distance of new two vertices from the waypoint
			float offset = (float) (width / Math.sin(angle / 2f));
			System.out.println("offset: " + offset);
			
			float leftx = curr.x - centerlineDir.x * offset;
			float leftz = curr.z - centerlineDir.z * offset;
			
			float rightx = curr.x + centerlineDir.x * offset;
			float rightz = curr.z + centerlineDir.z * offset;
			
			float leftHeight = heightMap.getHeight(leftx, leftz);
			float rightHeight = heightMap.getHeight(rightx, rightz);
			float height = Math.max(leftHeight, rightHeight);
			float lefty = height;
			float righty = height;
			
			vertices[wpPointer * 3] = leftx;
			//vertices[wpPointer * 3 + 1] = lefty;
			vertices[wpPointer * 3 + 2] = leftz;
			
			vertices[(waypoints.size() + wpPointer) * 3] = rightx;
			//vertices[(waypoints.size() + wpPointer) * 3 + 1] = righty;
			vertices[(waypoints.size() + wpPointer) * 3 + 2] = rightz;
			
			curr.setY(height);
		}
		
		// waypoints now have appropriate height (max(left vertex, right vertex))
		
		for(int wpPointer = 0; wpPointer < waypoints.size(); wpPointer++) {
//			Vector3f prev = waypoints.get(wpPointer - 1);
//			Vector3f curr = waypoints.get(wpPointer);
//			Vector3f next = waypoints.get(wpPointer + 1);
//			
//			Vector3f prevDirection = Vector3f.sub(curr, prev, null).normalise(null);
//			Vector3f nextDirection = Vector3f.sub(next, curr, null).normalise(null);
//			
//			Vector3f prevRight = (Vector3f) Vector3f.cross(prevDirection, Constants.Y_AXIS, null).normalise(null);
//			Vector3f nextRight = (Vector3f) Vector3f.cross(nextDirection, Constants.Y_AXIS, null).normalise(null);
//			
//			Vector3f prevUp = (Vector3f) Vector3f.cross(prevRight, prevDirection, null).normalise(null);
//			Vector3f nextUp = (Vector3f) Vector3f.cross(nextRight, nextDirection, null).normalise(null);
//			
//			Vector3f normal = Vector3f.add(prevUp, nextUp, null).normalise(null);
//			System.out.println("normal: " + normal);
//			
//			normals[wpPointer * 3] = normal.x;
//			normals[wpPointer * 3 + 1] = normal.y;
//			normals[wpPointer * 3 + 2] = normal.z;
//			
//			normals[(waypoints.size() + wpPointer) * 3] = normal.x;
//			normals[(waypoints.size() + wpPointer) * 3 + 1] = normal.y;
//			normals[(waypoints.size() + wpPointer) * 3 + 2] = normal.z;
			
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
