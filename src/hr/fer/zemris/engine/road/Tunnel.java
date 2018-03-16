package hr.fer.zemris.engine.road;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import hr.fer.zemris.engine.obj.ModelData;

public class Tunnel {
	
	private static final Logger LOGGER = Logger.getLogger(Tunnel.class.getName());
	private static final double EPS = 1e-6;
	
	private final ModelData innerRing;
	private final ModelData outerRing;
	private final ModelData entranceFace;
	private final ModelData exitFace;
	private final ModelData entranceMask;
	private final ModelData exitMask;
	
	public Tunnel(List<Vector3f> leftTrajectory, List<Vector3f> rightTrajectory, List<Vector3f> centerTrajectory,
			int subdivisions, float wallThickness, Vector3f entranceMaskLocation, Vector3f exitMaskLocation,
			float tunnelInnerTextureDepth, float tunnelOuterTextureDepth, float faceTextureWidth,
			float faceTextureHeight, float maskTextureWidth, float maskTextureHeight) {
		if(leftTrajectory == null ||
				rightTrajectory == null ||
				leftTrajectory.size() < 2 ||
				rightTrajectory.size() < 2 ||
				leftTrajectory.size() != rightTrajectory.size()) {
			throw new IllegalArgumentException("Invalid edge trajectories - "
					+ "at least two points at each side are needed.");
		}
		
		if(subdivisions < 1) {
			throw new IllegalArgumentException("At least one subdivision is necessary.");
		}
		
		if(wallThickness <= 0f) {
			throw new IllegalArgumentException("Wall thickness must be positive.");
		}
		
		float roadWidth = Vector3f.sub(rightTrajectory.get(0), leftTrajectory.get(0), null).length();
		float innerRadius = roadWidth / 2f;
		float outerRadius = innerRadius + wallThickness;
		
		innerRing = generateRing(
				leftTrajectory,
				rightTrajectory,
				subdivisions,
				innerRadius,
				tunnelInnerTextureDepth,
				true);
		
		outerRing = generateRing(
				leftTrajectory,
				rightTrajectory,
				subdivisions,
				outerRadius,
				tunnelOuterTextureDepth,
				false);
		
		entranceFace = generateFace(
				leftTrajectory,
				rightTrajectory,
				subdivisions,
				innerRadius,
				outerRadius,
				faceTextureWidth,
				faceTextureHeight,
				true);
		
		exitFace = generateFace(
				leftTrajectory,
				rightTrajectory,
				subdivisions,
				innerRadius,
				outerRadius,
				faceTextureWidth,
				faceTextureHeight,
				false);
		
		entranceMask = generateMask(
				leftTrajectory,
				rightTrajectory,
				centerTrajectory,
				entranceMaskLocation,
				subdivisions,
				innerRadius,
				maskTextureWidth,
				maskTextureHeight,
				false);
		
		exitMask = generateMask(
				leftTrajectory,
				rightTrajectory,
				centerTrajectory,
				exitMaskLocation,
				subdivisions,
				innerRadius,
				maskTextureWidth,
				maskTextureHeight,
				true);
	}

	public ModelData getInnerRing() {
		return innerRing;
	}
	
	public ModelData getOuterRing() {
		return outerRing;
	}
	
	public ModelData getEntranceFace() {
		return entranceFace;
	}
	
	public ModelData getExitFace() {
		return exitFace;
	}
	
	public ModelData getEntranceMask() {
		return entranceMask;
	}
	
	public ModelData getExitMask() {
		return exitMask;
	}
	
	/////////////////////////////////////////
	// Tunnel body generation
	/////////////////////////////////////////

	private ModelData generateRing(List<Vector3f> leftTrajectory, List<Vector3f> rightTrajectory,
			int subdivisions, float radius, float textureDepth, boolean generateInnerSide) {
		final int vertCount = (subdivisions + 2) * leftTrajectory.size();
		
		float[] vertices = generateRingVertices(
				leftTrajectory,
				rightTrajectory,
				subdivisions,
				radius,
				vertCount);
		
		float[] normals = generateRingNormals(
				leftTrajectory,
				subdivisions,
				vertCount);
		
		float[] textureCoords = generateRingTextureCoordinates(
				leftTrajectory,
				rightTrajectory,
				subdivisions,
				radius,
				vertCount,
				textureDepth);

		int[] indices = generateRingIndices(leftTrajectory, subdivisions, generateInnerSide);
		
		return new ModelData(vertices, textureCoords, normals, null, indices, -1f);
	}

	private float[] generateRingVertices(List<Vector3f> leftTrajectory, List<Vector3f> rightTrajectory,
			int subdivisions, float radius, int vertCount) {
		float[] vertices = new float[vertCount * 3];
		
		double subdivAngle = Math.PI / (double)(subdivisions + 1);
		
		for(int subdiv = 0; subdiv < subdivisions + 2; subdiv++) {
			final float angle = (float) (Math.PI - subdiv * subdivAngle);
			
			for(int vPointer = 0; vPointer < leftTrajectory.size(); vPointer++) {
				Vector3f left = leftTrajectory.get(vPointer);
				Vector3f right = rightTrajectory.get(vPointer);
				
				Vector3f center = (Vector3f) Vector3f.add(left, right, null).scale(0.5f);
				
				float deltaRight = (float) (radius * Math.cos(angle));
				float deltaUp = (float) (radius * Math.sin(angle));
				
				Vector3f rightVector = Vector3f.sub(right, center, null).normalise(null);
				
				float x = center.x + rightVector.x * deltaRight;
				float y = center.y + deltaUp;
				float z = center.z + rightVector.z * deltaRight;
				
				int startIdx = (subdiv * leftTrajectory.size() + vPointer) * 3;
				
				vertices[startIdx] = x;
				vertices[startIdx + 1] = y;
				vertices[startIdx + 2] = z;
			}
		}
		
		return vertices;
	}
	
	private float[] generateRingNormals(List<Vector3f> leftTrajectory, int subdivisions, int vertCount) {
		float[] normals = new float[vertCount * 3];

		for(int subdiv = 0; subdiv < subdivisions + 2; subdiv++) {
			for(int vPointer = 0; vPointer < leftTrajectory.size(); vPointer++) {
				int startIdx = (subdiv * leftTrajectory.size() + vPointer) * 3;

				normals[startIdx] = 0f;
				normals[startIdx + 1] = 1f;
				normals[startIdx + 2] = 0f;
			}
		}
		
		return normals;
	}

	private float[] generateRingTextureCoordinates(List<Vector3f> leftTrajectory,
			List<Vector3f> rightTrajectory, int subdivisions, float radius, int vertCount, 
			float textureDepth) {
		float[] textureCoords = new float[vertCount * 2];
		
		List<Float> leftDistances = determineDistances(leftTrajectory);
		List<Float> rightDistances = determineDistances(rightTrajectory);
		
		for(int subdiv = 0; subdiv < subdivisions + 2; subdiv++) {
			float fraction = subdiv / (float)(subdivisions + 1);
			float ucoord = 1f - fraction;
			
			for(int vPointer = 0; vPointer < leftTrajectory.size(); vPointer++) {
				float leftDistance = leftDistances.get(vPointer);
				float rightDistance = rightDistances.get(vPointer);
				
				
				float interpolatedDistance = leftDistance * (1f - fraction) + rightDistance * fraction;
				float vcoord = interpolatedDistance / textureDepth;
				
				int startIdx = (subdiv * leftTrajectory.size() + vPointer) * 2;
				
				textureCoords[startIdx] = ucoord;
				textureCoords[startIdx + 1] = vcoord;
			}
		}
		
		return textureCoords;
	}
	
	private List<Float> determineDistances(List<Vector3f> trajectory) {
		List<Float> pointDistances = new ArrayList<>();
		pointDistances.add(0f);
		
		float totalDistance = 0f;
		
		for(int i = 1; i < trajectory.size(); i++) {
			Vector3f p0 = trajectory.get(i - 1);
			Vector3f p1 = trajectory.get(i);
			
			float segmentLength = Vector3f.sub(p0, p1, null).length();
			totalDistance += segmentLength;
			
			pointDistances.add(totalDistance);
		}

		return pointDistances;
	}
	
	private int[] generateRingIndices(List<Vector3f> leftTrajectory, int subdivisions, boolean cw) {
		int[] indices = new int[6 * (subdivisions + 1) * (leftTrajectory.size() - 1)];
		
		int indicesPointer = 0;
		
		for(int subdiv = 0; subdiv < subdivisions + 1; subdiv++) {
			for(int vPointer = 0; vPointer < leftTrajectory.size() - 1; vPointer++) {
				int topLeft = subdiv * leftTrajectory.size() + vPointer;
				int topRight = topLeft + 1;
				int bottomLeft = topLeft + leftTrajectory.size();
				int bottomRight = bottomLeft + 1;
				
				if(cw) { // clockwise indexing
					indices[indicesPointer++] = topLeft;
					indices[indicesPointer++] = topRight;
					indices[indicesPointer++] = bottomLeft;
					indices[indicesPointer++] = topRight;
					indices[indicesPointer++] = bottomRight;
					indices[indicesPointer++] = bottomLeft;
				} else {
					indices[indicesPointer++] = topLeft;
					indices[indicesPointer++] = bottomLeft;
					indices[indicesPointer++] = topRight;
					indices[indicesPointer++] = topRight;
					indices[indicesPointer++] = bottomLeft;
					indices[indicesPointer++] = bottomRight;
				}
			}
		}
		
		return indices;
	}
	
	/////////////////////////////////////////
	// Tunnel face generation
	/////////////////////////////////////////
	
	private ModelData generateFace(List<Vector3f> leftTrajectory, List<Vector3f> rightTrajectory,
			int subdivisions, float innerRadius, float outerRadius, float faceTextureWidth,
			float faceTextureHeight, boolean isEntrance) {
		final int vertCount = (subdivisions + 2) * 2;
		
		float[] vertices = generateFaceVertices(
				vertCount,
				leftTrajectory,
				rightTrajectory,
				subdivisions,
				innerRadius,
				outerRadius,
				isEntrance);
		
		float[] normals = generateFaceNormals(
				vertCount,
				subdivisions);
		
		float[] textureCoords = generateFaceTextureCoordinates(
				subdivisions,
				innerRadius,
				outerRadius,
				vertCount,
				faceTextureWidth,
				faceTextureHeight);

		int[] indices = generateFaceIndices(subdivisions, isEntrance);
		
		return new ModelData(vertices, textureCoords, normals, null, indices, -1f);
	}

	private float[] generateFaceVertices(int vertCount, List<Vector3f> leftTrajectory,
			List<Vector3f> rightTrajectory, int subdivisions, float innerRadius, float outerRadius,
			boolean isEntrance) {
		float[] vertices = new float[vertCount * 3];
		
		double subdivAngle = Math.PI / (double)(subdivisions + 1);
		
		int trajectoryPointIndex = isEntrance ? 0 : leftTrajectory.size() - 1;
		Vector3f left = leftTrajectory.get(trajectoryPointIndex);
		Vector3f right = rightTrajectory.get(trajectoryPointIndex);
		Vector3f center = (Vector3f) Vector3f.add(left, right, null).scale(0.5f);
		Vector3f rightVector = Vector3f.sub(right, center, null).normalise(null);
		
		// outer vertices are placed first, then inner vertices
		for(int subdiv = 0; subdiv < subdivisions + 2; subdiv++) {
			final float angle = (float) (Math.PI - subdiv * subdivAngle);

			float deltaRightOuter = (float) (outerRadius * Math.cos(angle));
			float deltaUpOuter = (float) (outerRadius * Math.sin(angle));
			
			float deltaRightInner = (float) (innerRadius * Math.cos(angle));
			float deltaUpInner = (float) (innerRadius * Math.sin(angle));

			float outerX = center.x + rightVector.x * deltaRightOuter;
			float outerY = center.y + deltaUpOuter;
			float outerZ = center.z + rightVector.z * deltaRightOuter;
			
			float innerX = center.x + rightVector.x * deltaRightInner;
			float innerY = center.y + deltaUpInner;
			float innerZ = center.z + rightVector.z * deltaRightInner;
			
			vertices[subdiv * 3] = innerX;
			vertices[subdiv * 3 + 1] = innerY;
			vertices[subdiv * 3 + 2] = innerZ;
			
			vertices[(subdivisions + 2 + subdiv) * 3] = outerX;
			vertices[(subdivisions + 2 + subdiv) * 3 + 1] = outerY;
			vertices[(subdivisions + 2 + subdiv) * 3 + 2] = outerZ;
		}
		
		return vertices;
	}
	
	private float[] generateFaceNormals(int vertCount, int subdivisions) {
		float[] normals = new float[vertCount * 3];

		for(int subdiv = 0; subdiv < subdivisions + 2; subdiv++) {
			normals[subdiv * 3] = 0f;
			normals[subdiv * 3 + 1] = 1f;
			normals[subdiv * 3 + 2] = 0f;
			
			normals[(subdivisions + 2 + subdiv) * 3] = 0f;
			normals[(subdivisions + 2 + subdiv) * 3 + 1] = 1f;
			normals[(subdivisions + 2 + subdiv) * 3 + 2] = 0f;
		}
		
		return normals;
	}
	
	private float[] generateFaceTextureCoordinates(int subdivisions, float innerRadius,
			float outerRadius, int vertCount, float faceTextureWidth, float faceTextureHeight) {
		float[] textureCoords = new float[vertCount * 2];
		
		double subdivAngle = Math.PI / (double)(subdivisions + 1);
		
		for(int subdiv = 0; subdiv < subdivisions + 2; subdiv++) {
			float angle = (float) (Math.PI - subdiv * subdivAngle);

			float innerUCoord = (float) ((outerRadius + innerRadius * Math.cos(angle)) / faceTextureWidth);
			float outerUCoord = (float) ((outerRadius + outerRadius * Math.cos(angle)) / faceTextureWidth);
			
			float outerVCoord = (float) (outerRadius * Math.sin(angle) / faceTextureHeight);
			float innerVCoord = (float) (innerRadius * Math.sin(angle) / faceTextureHeight);
				
			textureCoords[subdiv * 2] = outerUCoord;
			textureCoords[subdiv * 2 + 1] = outerVCoord;
			
			textureCoords[(subdivisions + 2 + subdiv) * 2] = innerUCoord;
			textureCoords[(subdivisions + 2 + subdiv) * 2 + 1] = innerVCoord;
		}
		
		return textureCoords;
	}
	
	private int[] generateFaceIndices(int subdivisions, boolean cw) {
		int[] indices = new int[6 * (subdivisions + 1)];
		
		int indicesPointer = 0;
		
		for(int subdiv = 0; subdiv < subdivisions + 1; subdiv++) {
			int topLeft = subdiv;
			int topRight = topLeft + 1;
			int bottomLeft = topLeft + subdivisions + 2;
			int bottomRight = bottomLeft + 1;
			
			if(cw) { // clockwise indexing
				indices[indicesPointer++] = topLeft;
				indices[indicesPointer++] = topRight;
				indices[indicesPointer++] = bottomLeft;
				indices[indicesPointer++] = topRight;
				indices[indicesPointer++] = bottomRight;
				indices[indicesPointer++] = bottomLeft;
			} else {
				indices[indicesPointer++] = topLeft;
				indices[indicesPointer++] = bottomLeft;
				indices[indicesPointer++] = topRight;
				indices[indicesPointer++] = topRight;
				indices[indicesPointer++] = bottomLeft;
				indices[indicesPointer++] = bottomRight;
			}
		}
		
		return indices;
	}
	
	/////////////////////////////////////////
	// Tunnel mask generation
	/////////////////////////////////////////

	private ModelData generateMask(List<Vector3f> leftTrajectory, List<Vector3f> rightTrajectory,
			List<Vector3f> centerTrajectory, Vector3f maskLocation, int subdivisions, float innerRadius,
			float maskTextureWidth, float maskTextureHeight, boolean cw) {
		final int vertCount = subdivisions + 2 + 1;
		
		float[] vertices = generateMaskVertices(
				vertCount,
				leftTrajectory,
				rightTrajectory,
				centerTrajectory,
				maskLocation,
				subdivisions,
				innerRadius);
		
		float[] normals = generateMaskNormals(vertCount);
		
		float[] textureCoords = generateMaskTextureCoordinates(
				subdivisions,
				innerRadius,
				vertCount,
				maskTextureWidth,
				maskTextureHeight);

		int[] indices = generateMaskIndices(subdivisions, cw);
		
		return new ModelData(vertices, textureCoords, normals, null, indices, -1f);
	}

	private float[] generateMaskVertices(int vertCount, List<Vector3f> leftTrajectory,
			List<Vector3f> rightTrajectory, List<Vector3f> centerTrajectory, Vector3f maskLocation,
			int subdivisions, float innerRadius) {
		float[] vertices = new float[vertCount * 3];
		
		double subdivAngle = Math.PI / (double)(subdivisions + 1);
		
		int trajectoryPointIndex = indexOf(maskLocation, centerTrajectory, EPS);
		Vector3f left = leftTrajectory.get(trajectoryPointIndex);
		Vector3f right = rightTrajectory.get(trajectoryPointIndex);
		Vector3f center = (Vector3f) Vector3f.add(left, right, null).scale(0.5f);
		Vector3f rightVector = Vector3f.sub(right, center, null).normalise(null);

		for(int subdiv = 0; subdiv < subdivisions + 2; subdiv++) {
			final float angle = (float) (Math.PI - subdiv * subdivAngle);

			float deltaRight = (float) (innerRadius * Math.cos(angle));
			float deltaUp = (float) (innerRadius * Math.sin(angle));

			float innerX = center.x + rightVector.x * deltaRight;
			float innerY = center.y + deltaUp;
			float innerZ = center.z + rightVector.z * deltaRight;
			
			vertices[3 + subdiv * 3] = innerX;
			vertices[3 + subdiv * 3 + 1] = innerY;
			vertices[3 + subdiv * 3 + 2] = innerZ;
		}
		
		// center point
		vertices[0] = center.x;
		vertices[1] = center.y;
		vertices[2] = center.z;
		
		return vertices;
	}

	private int indexOf(Vector3f maskLocation, List<Vector3f> centerTrajectory, double eps) {
		for(int i = 0; i < centerTrajectory.size(); i++) {
			Vector3f tp = centerTrajectory.get(i);
			
			if(Vector3f.sub(maskLocation, tp, null).lengthSquared() <= eps * eps) {
				return i;
			}
		}
		
		LOGGER.severe("Cannot determine location of tunnel endpoint mask.");
		return 0;
	}

	private float[] generateMaskNormals(int vertCount) {
		float[] normals = new float[vertCount * 3];
		
		for(int i = 0; i < vertCount; i++) {
			normals[i * 3] = 0f;
			normals[i * 3 + 1] = 1f;
			normals[i * 3 + 2] = 0f;
		}
		
		return normals;
	}

	private float[] generateMaskTextureCoordinates(int subdivisions, float innerRadius, int vertCount,
			float maskTextureWidth, float maskTextureHeight) {
		float[] textureCoords = new float[vertCount * 2];
		
		double subdivAngle = Math.PI / (double)(subdivisions + 1);
		
		for(int subdiv = 0; subdiv < subdivisions + 2; subdiv++) {
			float angle = (float) (Math.PI - subdiv * subdivAngle);

			float uCoord = (float) ((innerRadius + innerRadius * Math.cos(angle)) / maskTextureWidth);
			float vCoord = (float) (innerRadius * Math.sin(angle) / maskTextureHeight);

			textureCoords[2 + subdiv * 2] = uCoord;
			textureCoords[2 + subdiv * 2 + 1] = vCoord;
		}
		
		textureCoords[0] = innerRadius / maskTextureWidth;
		textureCoords[1] = innerRadius / maskTextureHeight;
		
		return textureCoords;
	}

	private int[] generateMaskIndices(int subdivisions, boolean cw) {
		int[] indices = new int[3 * (subdivisions + 1)];
		
		int indicesPointer = 0;
		
		for(int subdiv = 0; subdiv < subdivisions + 1; subdiv++) {
			int center = 0;
			int left = 1 + subdiv;
			int right = left + 1;
			
			if(cw) { // clockwise indexing
				indices[indicesPointer++] = left;
				indices[indicesPointer++] = right;
				indices[indicesPointer++] = center;
			} else {
				indices[indicesPointer++] = right;
				indices[indicesPointer++] = left;
				indices[indicesPointer++] = center;
			}
		}
		
		return indices;
	}

}
