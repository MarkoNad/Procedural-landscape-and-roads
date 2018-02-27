package roads;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import objConverter.ModelData;

public class Tunnel {
	
	private final ModelData innerRing;
	private final ModelData outerRing;
	
	public Tunnel(List<Vector3f> leftTrajectory, List<Vector3f> rightTrajectory, int subdivisions, 
			float wallThickness, float tunnelInnerTextureDepth, float tunnelOuterTextureDepth) {
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
		
		innerRing = generateRing(
				leftTrajectory,
				rightTrajectory,
				subdivisions,
				roadWidth,
				tunnelInnerTextureDepth,
				true);
		
		outerRing = generateRing(
				leftTrajectory,
				rightTrajectory,
				subdivisions,
				roadWidth + wallThickness,
				tunnelOuterTextureDepth,
				true);
	}
	
	public ModelData getInnerRing() {
		return innerRing;
	}

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
				rightTrajectory,
				subdivisions,
				radius,
				vertCount,
				generateInnerSide);
		
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
	
	private float[] generateRingNormals(List<Vector3f> leftTrajectory, List<Vector3f> rightTrajectory,
			int subdivisions, float radius, int vertCount, boolean generateInnerSide) {
		float[] normals = new float[vertCount * 3];

		for(int subdiv = 0; subdiv < subdivisions + 2; subdiv++) {
			for(int vPointer = 0; vPointer < leftTrajectory.size(); vPointer++) {
				int startIdx = (subdiv * leftTrajectory.size() + vPointer) * 3;
				
				// TODO calculate normals
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

}
