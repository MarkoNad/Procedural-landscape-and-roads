package terrains;

import java.awt.image.BufferedImage;

import org.lwjgl.util.vector.Vector3f;

public class ImageHeightMap implements IHeightGenerator {

	private static final int MAX_PIXEL_COLOR = 256 * 256 * 256;
	
	private final double maxHeight;
	private final double vertexDistance;
	private final double[][] heightMap;
	private final int xVerts;
	private final int zVerts;

	public ImageHeightMap(BufferedImage heightMapImage, double maxHeight, double vertexDistance) {
		this.maxHeight = maxHeight;
		this.vertexDistance = vertexDistance;
		this.xVerts = heightMapImage.getWidth();
		this.zVerts = heightMapImage.getHeight();
		this.heightMap = generateHeightMap(heightMapImage, maxHeight);
		
		for(int z = 0; z < heightMap[0].length; z++) {
			for(int x = 0; x < heightMap.length; x++) {
				System.out.println("Map x: " + x + ", z: " + z + ", height: " + heightMap[x][z]);
			}
		}
	}

	private double[][] generateHeightMap(BufferedImage heightMapImage, double maxHeight) {
		double[][] heightMap = new double[heightMapImage.getWidth()][heightMapImage.getHeight()];
		
		for(int y = 0; y < heightMapImage.getHeight(); y++) {
			for(int x = 0; x < heightMapImage.getWidth(); x++) {
				double height = heightMapImage.getRGB(x, y);

				height += MAX_PIXEL_COLOR;
				height /= MAX_PIXEL_COLOR;
				height *= maxHeight;
				
				heightMap[x][y] = height;
			}
		}
		
		return heightMap;
	}

	@Override
	public float getHeight(float x, float z) {
		if(x + 1e-6 < 0.0 || 
				z + 1e-6 < 0.0 || 
				x > (heightMap.length - 1) * vertexDistance + 1e-6 || // heightMap.length == width
				z > (heightMap[0].length - 1) * vertexDistance + 1e-6) {
			System.out.println("x z out of map bounds; x: " + x + ", z: " + z);
			return 0.0f;
		}
		
		/*
		 * Quad:
		 * 1 2
		 * ._.
		 * |_|
		 * 3 4
		 */
		
		double xGrid = x / vertexDistance;
		double zGrid = z / vertexDistance;
		
		System.out.println("Grid x: " + xGrid + ", z: " + zGrid);

		int leftX = (int) xGrid;
		int upZ = (int) zGrid; // up is forward, towards -z
		int rightX = leftX == xVerts - 1 ? leftX : leftX + 1;
		int downZ = upZ == zVerts - 1 ? upZ : upZ + 1;
		
		double u = xGrid - leftX;
		double v = zGrid - upZ;
		
		System.out.println("u: " + u);
		System.out.println("v: " + v);
		
		double heightLeftUp = heightMap[leftX][upZ];
		double heightLeftDown = heightMap[leftX][downZ];
		double heightRightUp = heightMap[rightX][upZ];
		double heightRightDown = heightMap[rightX][downZ];
		
		double heightUp = (1.0 - u) * heightLeftUp + u * heightRightUp;
		double heightDown = (1.0 - u) * heightLeftDown + u * heightRightDown;
		double height = (1.0 - v) * heightUp + v * heightDown;

		System.out.println("Height at x: " + x + ", z: " + z + ": " + height);
		
		return (float) height;
	}
	
	@Override
	public Vector3f getNormal(float x, float z) {
		float heightL = getHeight((float) (x - vertexDistance), z);
		float heightR = getHeight((float) (x + vertexDistance), z);
		float heightD = getHeight(x, (float) (z + vertexDistance));
		float heightU = getHeight(x, (float) (z - vertexDistance));

		Vector3f normal = new Vector3f(heightL - heightR, 2.0f, heightU - heightD);
		normal.normalise();
		
		return normal;
	}
	
//	@Override
//	public Vector3f getNormal(float x, float z) {
//		float heightL = getHeight(x - 1, z);
//		float heightR = getHeight(x + 1, z);
//		float heightD = getHeight(x, z - 1);
//		float heightU = getHeight(x, z + 1);
//
//		Vector3f normal = new Vector3f(heightL - heightR, 2.0f, heightD - heightU);
//		normal.normalise();
//		
//		return normal;
//	}

	@Override
	public float getMaxHeight() {
		return (float) maxHeight;
	}

	@Override
	public float getHeightApprox(float x, float z) {
		return getHeight(x, z);
	}

	@Override
	public Vector3f getNormalApprox(float x, float z) {
		return getNormal(x, z);
	}

}
