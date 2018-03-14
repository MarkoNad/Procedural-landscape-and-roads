package terrains;

import java.awt.image.BufferedImage;

public class ImageHeightMap extends MutableHeightMap {

	private static final int MAX_PIXEL_COLOR = 256 * 256 * 256;
	private static final float DIFF = 12000.0f / 1080.0f;
	
	private final double maxHeight;
	private final double vertexDistance;
	private final double[][] heightMap;
	private final int xVerts;
	private final int zVerts;

	public ImageHeightMap(BufferedImage heightMapImage, double minHeight, double maxHeight,
			double vertexDistance) {
		super(DIFF);
		
		this.maxHeight = maxHeight;
		this.vertexDistance = vertexDistance;
		this.xVerts = heightMapImage.getWidth();
		this.zVerts = heightMapImage.getHeight();
		this.heightMap = generateHeightMap(heightMapImage, minHeight, maxHeight);
		
		for(int z = 0; z < heightMap[0].length; z++) {
			for(int x = 0; x < heightMap.length; x++) {
				//System.out.println("Map x: " + x + ", z: " + z + ", height: " + heightMap[x][z]);
			}
		}
	}
	
	private double[][] generateHeightMap(BufferedImage heightMapImage, double minHeight, 
			double maxHeight) {
		double[][] heightMap = new double[heightMapImage.getWidth()][heightMapImage.getHeight()];
		
		double minOriginalHeight = Double.POSITIVE_INFINITY; // remove
		double maxOriginaldHeight = Double.NEGATIVE_INFINITY; // remove
		double minAfterScale = Double.POSITIVE_INFINITY;
		double maxAfterScale = Double.NEGATIVE_INFINITY;
		
		for(int y = 0; y < heightMapImage.getHeight(); y++) {
			for(int x = 0; x < heightMapImage.getWidth(); x++) {
				double height = heightMapImage.getRGB(x, y);
				
//				int rgb = heightMapImage.getRGB(x, y);
//				int a = (rgb >> 24) & 0xFF;
//				int r = (rgb >> 16) & 0xFF;
//				int g = (rgb >> 8) & 0xFF;
//				int b = rgb & 0xFF;
//				
//				System.out.println("rgb: " + rgb);
//				System.out.println("height: " + height);
//				System.out.println("a: " + a);
//				System.out.println("r: " + r);
//				System.out.println("g: " + g);
//				System.out.println("b: " + b);
//				System.out.println("a: " + Integer.toBinaryString(a));

				if(height > maxOriginaldHeight) maxOriginaldHeight = height;
				if(height < minOriginalHeight) minOriginalHeight = height;

				height += MAX_PIXEL_COLOR;
				height /= MAX_PIXEL_COLOR;
				
				if(height > maxAfterScale) maxAfterScale = height;
				if(height < minAfterScale) minAfterScale = height;
			}
		}
		
		double minFoundHeightFinal = Float.MAX_VALUE;
		double maxFoundHeightFinal = Float.MIN_VALUE;
		
		final double intervalsRatio = (maxHeight - minHeight) / (maxAfterScale - minAfterScale);
		
		for(int y = 0; y < heightMapImage.getHeight(); y++) {
			for(int x = 0; x < heightMapImage.getWidth(); x++) {
				double percentage = heightMapImage.getRGB(x, y);

				percentage += MAX_PIXEL_COLOR;
				percentage /= MAX_PIXEL_COLOR;
				
				double height = minHeight + intervalsRatio * (percentage - minAfterScale);

				if(height > maxFoundHeightFinal) maxFoundHeightFinal = height;
				if(height < minFoundHeightFinal) minFoundHeightFinal = height;

				heightMap[x][y] = height;
			}
		}
		
		System.out.println("min height: " + maxOriginaldHeight);
		System.out.println("max height: " + minOriginalHeight);
		System.out.println("min height after scaling with " + MAX_PIXEL_COLOR + ": " + minAfterScale);
		System.out.println("max height after scaling with " + MAX_PIXEL_COLOR + ": " + maxAfterScale);
		System.out.println("min height true: " + minFoundHeightFinal);
		System.out.println("max height true: " + maxFoundHeightFinal);
		
		return heightMap;
	}
	
	@Override
	protected float getBaseHeight(float x, float z) {
		if(x + 1e-6 < 0.0 || 
				z + 1e-6 < 0.0 || 
				x > (heightMap.length - 1) * vertexDistance + 1e-6 || // heightMap.length == width
				z > (heightMap[0].length - 1) * vertexDistance + 1e-6) {
//			System.out.println("x z out of map bounds; x: " + x + ", z: " + z);
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
		
//		System.out.println("Grid x: " + xGrid + ", z: " + zGrid);

		int leftX = (int) xGrid;
		int upZ = (int) zGrid; // up is forward, towards -z
		int rightX = leftX == xVerts - 1 ? leftX : leftX + 1;
		int downZ = upZ == zVerts - 1 ? upZ : upZ + 1;
		
		double u = xGrid - leftX;
		double v = zGrid - upZ;
		
//		System.out.println("u: " + u);
//		System.out.println("v: " + v);
		
		double heightLeftUp = heightMap[leftX][upZ];
		double heightLeftDown = heightMap[leftX][downZ];
		double heightRightUp = heightMap[rightX][upZ];
		double heightRightDown = heightMap[rightX][downZ];
		
		double heightUp = (1.0 - u) * heightLeftUp + u * heightRightUp;
		double heightDown = (1.0 - u) * heightLeftDown + u * heightRightDown;
		double height = (1.0 - v) * heightUp + v * heightDown;

//		System.out.println("Height at x: " + x + ", z: " + z + ": " + height);
		
		return (float) height;
	}

	@Override
	public float getMaxHeight() {
		return (float) maxHeight;
	}

}
