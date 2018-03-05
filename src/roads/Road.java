package roads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import renderEngine.Loader;
import terrains.IHeightGenerator;
import toolbox.CatmullRomSpline;
import toolbox.Globals;
import toolbox.Point2Df;

public class Road {
	
	private final float groundOffset; // distance of road from ground, to avoid jittering
	private Loader loader;
	private List<Vector3f> trajectory;
	private IHeightGenerator heightMap;
	private final float width;
	private float textureLen;
	private RawModel model;
	private boolean heightCorrection;
	
	private List<Vector3f> leftTrajectory;
	private List<Vector3f> rightTrajectory;
	
	public Road(List<Point2Df> waypoints2D, Loader loader, float width, float textureLen,
			float segmentLen, float groundOffset, IHeightGenerator heightMap, boolean heightCorrection) {
		if(waypoints2D == null || waypoints2D.size() < 2) {
			throw new IllegalArgumentException("At least two waypoints are required.");
		}
		
		if(heightCorrection && heightMap == null) {
			throw new IllegalArgumentException("Height map was null - cannot perform height correction.");
		}
		
		List<Vector3f> waypoints3D = assignHeightsToWaypoints(waypoints2D, heightMap);

		this.loader = loader;
		this.width = width;
		this.textureLen = textureLen;
		this.groundOffset = groundOffset;
		this.heightMap = heightMap;
		this.heightCorrection = heightCorrection;
		
		this.trajectory = generateTrajectory(waypoints3D, segmentLen);
		adhereTrajectoryToHeightMap(this.trajectory, heightMap);
		model = generate();
	}
	
	public Road(Loader loader, List<Vector3f> trajectory, float width, float textureLen,
			float segmentLen, float groundOffset) {
		if(trajectory == null || trajectory.isEmpty()) {
			throw new IllegalArgumentException("Trajectory cannot be empty.");
		}
		
		this.loader = loader;
		this.width = width;
		this.textureLen = textureLen;
		this.groundOffset = groundOffset;
		this.heightMap = null;
		this.heightCorrection = false;
		
		this.trajectory = trajectory;
		model = generate();
	}
	
	public Road(Loader loader, List<Vector3f> waypoints, float width, float textureLen, float segmentLen,
			float groundOffset, IHeightGenerator heightMap, boolean heightCorrection) {
		if(waypoints == null || waypoints.size() < 2) {
			throw new IllegalArgumentException("At least two waypoints are required.");
		}
		
		if(heightCorrection && heightMap == null) {
			throw new IllegalArgumentException("Height map was null - cannot perform height correction.");
		}
		
		this.loader = loader;
		this.width = width;
		this.textureLen = textureLen;
		this.groundOffset = groundOffset;
		this.heightMap = heightMap;
		this.heightCorrection = heightCorrection;
		
		this.trajectory = generateTrajectory(waypoints, segmentLen);
		adhereTrajectoryToHeightMap(this.trajectory, heightMap);
		model = generate();
	}
	
	public List<Vector3f> getCenterTrajectory() {
		return Collections.unmodifiableList(trajectory);
	}
	
	public List<Vector3f> getLeftTrajectory() {
		return Collections.unmodifiableList(leftTrajectory);
	}
	
	public List<Vector3f> getRightTrajectory() {
		return Collections.unmodifiableList(rightTrajectory);
	}
	
	private List<Vector3f> assignHeightsToWaypoints(List<Point2Df> waypoints2D, IHeightGenerator heightMap) {
		List<Vector3f> waypoints3D = new ArrayList<>();
		
		waypoints2D.forEach(p -> {
			float height = heightMap.getHeight(p.getX(), p.getZ());
			waypoints3D.add(new Vector3f(p.getX(), height, p.getZ()));
		});
		
		return waypoints3D;
	}
	
	public static List<Vector3f> generateTrajectory(List<Vector3f> waypoints, float segmentLength) {
		CatmullRomSpline curve = new CatmullRomSpline(waypoints, segmentLength);
		return curve.getCurvePointsCopy();
	}
	
	public static List<Vector3f> generateTrajectory(List<Vector3f> waypoints, float segmentLength, 
			IHeightGenerator heightMap) {
		List<Vector3f> trajectory = generateTrajectory(waypoints, segmentLength);
		adhereTrajectoryToHeightMap(trajectory, heightMap);
		return trajectory;
	}
	
	private static void adhereTrajectoryToHeightMap(List<Vector3f> trajectory, IHeightGenerator heightMap) {
		trajectory.forEach(p -> {
			float height = heightMap.getHeight(p.getX(), p.getZ());
			p.setY(height);
		});
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
		
		leftTrajectory = new ArrayList<>();
		rightTrajectory = new ArrayList<>();
		
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
			
			prevDirection.y = 0; // TODO #issue1
			nextDirection.y = 0;
			
			// vector pointing to the right of the prev and next directions, used for centerline
			Vector3f prevRight = (Vector3f) Vector3f.cross(prevDirection, Globals.Y_AXIS, null).normalise(null);
			Vector3f nextRight = (Vector3f) Vector3f.cross(nextDirection, Globals.Y_AXIS, null).normalise(null);
			
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
			
			float y = curr.y;
			
			if(heightCorrection) {
				float leftHeight = heightMap.getHeight(leftx, leftz);
				float rightHeight = heightMap.getHeight(rightx, rightz);
				y = Math.max(leftHeight, rightHeight);
			}

			y += groundOffset;
			curr.setY(y);
			
			vertices[wpPointer * 3] = leftx;
			vertices[wpPointer * 3 + 1] = y;
			vertices[wpPointer * 3 + 2] = leftz;
			
			vertices[(trajectory.size() + wpPointer) * 3] = rightx;
			vertices[(trajectory.size() + wpPointer) * 3 + 1] = y;
			vertices[(trajectory.size() + wpPointer) * 3 + 2] = rightz;
		}
		
		// if ground offset was provided or height correction requested waypoints now have
		// corrected height (max(left vertex, right vertex))
		
		setupEdgeNormals(trajectory, normals);
		
		for(int wpPointer = 1; wpPointer < trajectory.size() - 1; wpPointer++) {
			Vector3f prev = trajectory.get(wpPointer - 1);
			Vector3f curr = trajectory.get(wpPointer);
			Vector3f next = trajectory.get(wpPointer + 1);
			
			Vector3f prevDirection = Vector3f.sub(curr, prev, null).normalise(null);
			Vector3f nextDirection = Vector3f.sub(next, curr, null).normalise(null);
			
			Vector3f prevRight = (Vector3f) Vector3f.cross(prevDirection, Globals.Y_AXIS, null).normalise(null);
			Vector3f nextRight = (Vector3f) Vector3f.cross(nextDirection, Globals.Y_AXIS, null).normalise(null);
			
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
		
		List<Float> pointDistances = determineDistances(trajectory);
		for(int wpPointer = 0; wpPointer < trajectory.size(); wpPointer++) {
			float vcoord = pointDistances.get(wpPointer) / textureLen;
			
			textureCoords[wpPointer * 2] = 0f;
			textureCoords[wpPointer * 2 + 1] = vcoord;
			
			textureCoords[(trajectory.size() + wpPointer) * 2] = 1f;
			textureCoords[(trajectory.size() + wpPointer) * 2 + 1] = vcoord;
		}
		
		setupIndices(indices, trajectory);
		
		cacheEdgeTrajectories(vertices);

		return loader.loadToVAO(vertices, textureCoords, normals, indices);
	}
	
	private void cacheEdgeTrajectories(float[] vertices) {
		int trajectoryPoints = vertices.length / 3 / 2;
		
		for(int i = 0; i < trajectoryPoints; i++) {
			float leftX = vertices[i * 3];
			float leftY = vertices[i * 3 + 1];
			float leftZ = vertices[i * 3 + 2];
			
			Vector3f leftVertex = new Vector3f(leftX, leftY, leftZ);
			leftTrajectory.add(leftVertex);
			
			float rightX = vertices[(trajectoryPoints + i) * 3];
			float rightY = vertices[(trajectoryPoints + i) * 3 + 1];
			float rightZ = vertices[(trajectoryPoints + i) * 3 + 2];
			
			Vector3f rightVertex = new Vector3f(rightX, rightY, rightZ);
			rightTrajectory.add(rightVertex);
		}
	}

	private List<Float> determineDistances(List<Vector3f> trajectory) {
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
	
	private void setupEdgeVertices(List<Vector3f> trajectory, float[] vertices) {
		// first waypoint vertices
		Vector3f directionFirst = Vector3f.sub(trajectory.get(1), trajectory.get(0), null);
		Vector3f rightFirst = (Vector3f) Vector3f.cross(directionFirst, Globals.Y_AXIS, null).normalise(null).scale(0.5f * width);
		Vector3f leftVFirst = Vector3f.add(rightFirst.negate(null), trajectory.get(0), null);
		Vector3f rightVFirst = Vector3f.add(rightFirst, trajectory.get(0), null);
		
		float heightFirst = trajectory.get(0).y;
		
		if(heightCorrection) {
			float leftHeightFirst = heightMap.getHeight(leftVFirst.x, leftVFirst.z);
			float rightHeightFirst = heightMap.getHeight(rightVFirst.x, rightVFirst.z);
			heightFirst = Math.max(leftHeightFirst, rightHeightFirst);
		}
		
		heightFirst += groundOffset;
		trajectory.get(0).setY(heightFirst);
		
		vertices[0] = leftVFirst.x;
		vertices[1] = heightFirst;
		vertices[2] = leftVFirst.z;
		vertices[0 + 3 * trajectory.size()] = rightVFirst.x;
		vertices[1 + 3 * trajectory.size()] = heightFirst;
		vertices[2 + 3 * trajectory.size()] = rightVFirst.z;
		
		
		// last waypoint vertices
		Vector3f directionLast = Vector3f.sub(trajectory.get(trajectory.size()-1), trajectory.get(trajectory.size()-2), null);
		Vector3f rightLast = (Vector3f) Vector3f.cross(directionLast, Globals.Y_AXIS, null).normalise(null).scale(0.5f * width);
		Vector3f leftVLast = Vector3f.add(rightLast.negate(null), trajectory.get(trajectory.size()-1), null);
		Vector3f rightVLast = Vector3f.add(rightLast, trajectory.get(trajectory.size()-1), null);
		
		float heightLast = trajectory.get(trajectory.size()-1).y;
		
		if(heightCorrection) {
			float leftHeightLast = heightMap.getHeight(leftVLast.x, leftVLast.z);
			float rightHeightLast = heightMap.getHeight(rightVLast.x, rightVLast.z);
			heightLast = Math.max(leftHeightLast, rightHeightLast);
		}
		
		heightLast += groundOffset;
		trajectory.get(trajectory.size() - 1).setY(heightLast);
		
		vertices[(trajectory.size() - 1) * 3] = leftVLast.x;
		vertices[(trajectory.size() - 1) * 3 + 1] = heightLast;
		vertices[(trajectory.size() - 1) * 3 + 2] = leftVLast.z;
		vertices[(2 * trajectory.size() - 1) * 3] = rightVLast.x;
		vertices[(2 * trajectory.size() - 1) * 3 + 1] = heightLast;
		vertices[(2 * trajectory.size() - 1) * 3 + 2] = rightVLast.z;
	}
	
	private void setupEdgeNormals(List<Vector3f> trajectory, float[] normals) {
		// first waypoint normals
		Vector3f directionFirst = Vector3f.sub(trajectory.get(1), trajectory.get(0), null);
		Vector3f rightFirst = (Vector3f) Vector3f.cross(directionFirst, Globals.Y_AXIS, null).normalise(null);
		Vector3f normalFirst = (Vector3f) Vector3f.cross(rightFirst, directionFirst, null).normalise(null);
		
		normals[0] = normalFirst.x;
		normals[1] = normalFirst.y;
		normals[2] = normalFirst.z;
		normals[3 * trajectory.size()] = normalFirst.x;
		normals[3 * trajectory.size() + 1] = normalFirst.y;
		normals[3 * trajectory.size() + 2] = normalFirst.z;
		
		// last waypoint normals
		Vector3f directionLast = Vector3f.sub(trajectory.get(trajectory.size() - 1), trajectory.get(trajectory.size() - 2), null);
		Vector3f rightLast = (Vector3f) Vector3f.cross(directionLast, Globals.Y_AXIS, null).normalise(null);
		Vector3f normalLast = (Vector3f) Vector3f.cross(rightLast, directionLast, null).normalise(null);
		
		normals[(trajectory.size() - 1) * 3] = normalLast.x;
		normals[(trajectory.size() - 1) * 3 + 1] = normalLast.y;
		normals[(trajectory.size() - 1) * 3 + 2] = normalLast.z;
		normals[(2 * trajectory.size() - 1) * 3] = normalLast.x;
		normals[(2 * trajectory.size() - 1) * 3 + 1] = normalLast.y;
		normals[(2 * trajectory.size() - 1) * 3 + 2] = normalLast.z;
	}
	
	private void setupIndices(int[] indices, List<Vector3f> trajectory) {
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
