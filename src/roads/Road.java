package roads;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import renderEngine.Loader;
import terrains.IHeightGenerator;
import toolbox.CatmullRomSpline;
import toolbox.Constants;

public class Road {
	
	private static final float DEFAULT_SEGMENT_LEN = 50f; // in OpenGL units
	private static final float GROUND_OFFSET = 7f; // distance of road from ground, to avoid jittering
	private Loader loader;
	private List<Vector3f> trajectory;
	private IHeightGenerator heightMap;
	private float width;
	private float textureLen;
	private RawModel model;

	public Road(Loader loader, List<Vector3f> waypoints, IHeightGenerator heightMap, float width, float textureLen, float segmentLen) {
		if(waypoints == null || waypoints.size() <= 1) {
			throw new IllegalArgumentException("At least two waypoints are required.");
		}
		if(heightMap == null) {
			throw new IllegalArgumentException("Heightmap cannot be null.");
		}
		
		this.loader = loader;
		this.trajectory = generateTrajectory(waypoints);
		this.heightMap = heightMap;
		this.width = width;
		this.textureLen = textureLen;
		
		model = generate();
	}
	
	public Road(Loader loader, List<Vector3f> waypoints, IHeightGenerator heightMap, float width, float textureLen) {
		this(loader, waypoints, heightMap, width, textureLen, DEFAULT_SEGMENT_LEN);
	}
	
	private List<Vector3f> generateTrajectory(List<Vector3f> waypoints) {
		CatmullRomSpline curve = new CatmullRomSpline(waypoints, DEFAULT_SEGMENT_LEN);//TODO: replace default with arg
		return curve.getCurvePoints();
	}

	public RawModel getModel() {
		return model;
	}
	
	private RawModel generate() {
		final int vertCount = 2 * trajectory.size();
		
		float[] vertices = new float[vertCount * 3];
		float[] normals = new float[vertCount * 3];
		float[] textureCoords = new float[vertCount * 2];
		int[] indices = new int[6 * (trajectory.size() - 1)];
		
		/*
		 * The road is drawn as follows:
		 * 
		 * left
		 * .__.__.__.__
		 * ./|./|./|./|
		 * right
		 */

		setupEdgeVertices(trajectory, vertices);
		
		for(int wpPointer = 1; wpPointer < trajectory.size() - 1; wpPointer++) {
			Vector3f prev = trajectory.get(wpPointer - 1);
			Vector3f curr = trajectory.get(wpPointer);
			Vector3f next = trajectory.get(wpPointer + 1);
			
			Vector3f prevDirection = Vector3f.sub(curr, prev, null).normalise(null);
			Vector3f nextDirection = Vector3f.sub(next, curr, null).normalise(null);
			
			prevDirection.y = 0;
			nextDirection.y = 0;
			
			// vector pointing to the right of the prev and next directions, used for centerline
			Vector3f prevRight = (Vector3f) Vector3f.cross(prevDirection, Constants.Y_AXIS, null).normalise(null);
			Vector3f nextRight = (Vector3f) Vector3f.cross(nextDirection, Constants.Y_AXIS, null).normalise(null);
			
			// centerline of angle between prev and next direction vectors, points to the right
			Vector3f centerlineDir = Vector3f.add(prevRight, nextRight, null).normalise(null);
			
			// road curvature at current waypoint
			float angle = Vector3f.angle(nextDirection, prevDirection.negate(null));
			
			// distance of new two vertices from the waypoint
			float offset = (float) (0.5 * width / Math.sin(angle / 2f));
			
			float leftx = curr.x - centerlineDir.x * offset;
			float leftz = curr.z - centerlineDir.z * offset;
			
			float rightx = curr.x + centerlineDir.x * offset;
			float rightz = curr.z + centerlineDir.z * offset;
			
			float leftHeight = heightMap.getHeight(leftx, leftz);
			float rightHeight = heightMap.getHeight(rightx, rightz);
			float height = Math.max(leftHeight, rightHeight) + GROUND_OFFSET;
			float lefty = height;
			float righty = height;
			
			vertices[wpPointer * 3] = leftx;
			vertices[wpPointer * 3 + 1] = lefty;
			vertices[wpPointer * 3 + 2] = leftz;
			
			vertices[(trajectory.size() + wpPointer) * 3] = rightx;
			vertices[(trajectory.size() + wpPointer) * 3 + 1] = righty;
			vertices[(trajectory.size() + wpPointer) * 3 + 2] = rightz;
			
			curr.setY(height);
		}
		
		// waypoints now have appropriate height (max(left vertex, right vertex))
		
		setupEdgeNormals(trajectory, normals);
		
		for(int wpPointer = 1; wpPointer < trajectory.size() - 1; wpPointer++) {
			Vector3f prev = trajectory.get(wpPointer - 1);
			Vector3f curr = trajectory.get(wpPointer);
			Vector3f next = trajectory.get(wpPointer + 1);
			
			Vector3f prevDirection = Vector3f.sub(curr, prev, null).normalise(null);
			Vector3f nextDirection = Vector3f.sub(next, curr, null).normalise(null);
			
			Vector3f prevRight = (Vector3f) Vector3f.cross(prevDirection, Constants.Y_AXIS, null).normalise(null);
			Vector3f nextRight = (Vector3f) Vector3f.cross(nextDirection, Constants.Y_AXIS, null).normalise(null);
			
			Vector3f prevUp = (Vector3f) Vector3f.cross(prevRight, prevDirection, null).normalise(null);
			Vector3f nextUp = (Vector3f) Vector3f.cross(nextRight, nextDirection, null).normalise(null);
			
			Vector3f normal = Vector3f.add(prevUp, nextUp, null).normalise(null);
			
			normals[wpPointer * 3] = normal.x;
			normals[wpPointer * 3 + 1] = normal.y;
			normals[wpPointer * 3 + 2] = normal.z;
			
			normals[(trajectory.size() + wpPointer) * 3] = normal.x;
			normals[(trajectory.size() + wpPointer) * 3 + 1] = normal.y;
			normals[(trajectory.size() + wpPointer) * 3 + 2] = normal.z;
		}
		
		List<Float> pointDistances = determineDistances();
		for(int wpPointer = 0; wpPointer < trajectory.size(); wpPointer++) {
			float vcoord = pointDistances.get(wpPointer) / textureLen;
			
			textureCoords[wpPointer * 2] = 0f;
			textureCoords[wpPointer * 2 + 1] = vcoord;
			
			textureCoords[(trajectory.size() + wpPointer) * 2] = 1f;
			textureCoords[(trajectory.size() + wpPointer) * 2 + 1] = vcoord;
		}
		
		setupIndices(indices);

		return loader.loadToVAO(vertices, textureCoords, normals, indices);
	}
	
	private List<Float> determineDistances() {
		List<Float> waypointDistances = new ArrayList<>();
		waypointDistances.add(0f);
		
		float totalDistance = 0f;
		
		for(int i = 1; i < trajectory.size(); i++) {
			Vector3f p0 = trajectory.get(i - 1);
			Vector3f p1 = trajectory.get(i);
			
			float segmentLength = Vector3f.sub(p0, p1, null).length();
			totalDistance += segmentLength;
			
			waypointDistances.add(totalDistance);
		}

		return waypointDistances;
	}
	
	private void setupEdgeVertices(List<Vector3f> waypoints, float[] vertices) {
		// first waypoint vertices
		Vector3f directionFirst = Vector3f.sub(waypoints.get(1), waypoints.get(0), null);
		Vector3f rightFirst = (Vector3f) Vector3f.cross(directionFirst, Constants.Y_AXIS, null).normalise(null).scale(0.5f * width);
		Vector3f leftVFirst = Vector3f.add(rightFirst.negate(null), waypoints.get(0), null);
		Vector3f rightVFirst = Vector3f.add(rightFirst, waypoints.get(0), null);
		
		float leftHeightFirst = heightMap.getHeight(leftVFirst.x, leftVFirst.z);
		float rightHeightFirst = heightMap.getHeight(rightVFirst.x, rightVFirst.z);
		float heightFirst = Math.max(leftHeightFirst, rightHeightFirst) + GROUND_OFFSET;
		
		vertices[0] = leftVFirst.x;
		vertices[1] = heightFirst;
		vertices[2] = leftVFirst.z;
		vertices[0 + 3 * waypoints.size()] = rightVFirst.x;
		vertices[1 + 3 * waypoints.size()] = heightFirst;
		vertices[2 + 3 * waypoints.size()] = rightVFirst.z;
		
		waypoints.get(0).setY(heightFirst);
		
		
		// last waypoint vertices
		Vector3f directionLast = Vector3f.sub(waypoints.get(waypoints.size()-1), waypoints.get(waypoints.size()-2), null);
		Vector3f rightLast = (Vector3f) Vector3f.cross(directionLast, Constants.Y_AXIS, null).normalise(null).scale(0.5f * width);
		Vector3f leftVLast = Vector3f.add(rightLast.negate(null), waypoints.get(waypoints.size()-1), null);
		Vector3f rightVLast = Vector3f.add(rightLast, waypoints.get(waypoints.size()-1), null);
		
		float leftHeightLast = heightMap.getHeight(leftVLast.x, leftVLast.z);
		float rightHeightLast = heightMap.getHeight(rightVLast.x, rightVLast.z);
		float heightLast = Math.max(leftHeightLast, rightHeightLast) + GROUND_OFFSET;
		
		vertices[(waypoints.size() - 1) * 3] = leftVLast.x;
		vertices[(waypoints.size() - 1) * 3 + 1] = heightLast;
		vertices[(waypoints.size() - 1) * 3 + 2] = leftVLast.z;
		vertices[(2 * waypoints.size() - 1) * 3] = rightVLast.x;
		vertices[(2 * waypoints.size() - 1) * 3 + 1] = heightLast;
		vertices[(2 * waypoints.size() - 1) * 3 + 2] = rightVLast.z;
		
		waypoints.get(waypoints.size() - 1).setY(heightLast);
	}
	
	private void setupEdgeNormals(List<Vector3f> waypoints, float[] normals) {
		// first waypoint normals
		Vector3f directionFirst = Vector3f.sub(waypoints.get(1), waypoints.get(0), null);
		Vector3f rightFirst = (Vector3f) Vector3f.cross(directionFirst, Constants.Y_AXIS, null).normalise(null);
		Vector3f normalFirst = (Vector3f) Vector3f.cross(rightFirst, directionFirst, null).normalise(null);
		
		normals[0] = normalFirst.x;
		normals[1] = normalFirst.y;
		normals[2] = normalFirst.z;
		normals[3 * waypoints.size()] = normalFirst.x;
		normals[3 * waypoints.size() + 1] = normalFirst.y;
		normals[3 * waypoints.size() + 2] = normalFirst.z;
		
		// last waypoint normals
		Vector3f directionLast = Vector3f.sub(waypoints.get(waypoints.size() - 1), waypoints.get(waypoints.size() - 2), null);
		Vector3f rightLast = (Vector3f) Vector3f.cross(directionLast, Constants.Y_AXIS, null).normalise(null);
		Vector3f normalLast = (Vector3f) Vector3f.cross(rightLast, directionLast, null).normalise(null);
		
		normals[(waypoints.size() - 1) * 3] = normalLast.x;
		normals[(waypoints.size() - 1) * 3 + 1] = normalLast.y;
		normals[(waypoints.size() - 1) * 3 + 2] = normalLast.z;
		normals[(2 * waypoints.size() - 1) * 3] = normalLast.x;
		normals[(2 * waypoints.size() - 1) * 3 + 1] = normalLast.y;
		normals[(2 * waypoints.size() - 1) * 3 + 2] = normalLast.z;
	}
	
	private void setupIndices(int[] indices) {
		int pointer = 0;
		for(int i = 0; i < trajectory.size() - 1; i++) {
			int topLeft = i;
			int topRight = topLeft + 1;
			int bottomLeft = trajectory.size() + i;
			int bottomRight = bottomLeft + 1;
			
			indices[pointer++] = topLeft;
			indices[pointer++] = bottomLeft;
			indices[pointer++] = topRight;
			indices[pointer++] = topRight;
			indices[pointer++] = bottomLeft;
			indices[pointer++] = bottomRight;
		}
	}
	
}
