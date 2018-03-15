package terrains;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

public class ImageHeightMap extends MutableHeightMap {

	private static final Logger LOGGER = Logger.getLogger(ImageHeightMap.class.getName());
	private static final int MAX_PIXEL_COLOR = 256 * 256 * 256;
	
	private final double maxHeight;
	private final double pixelDistance;
	private final double[][] heightMap;
	private final int xVerts;
	private final int zVerts;

	public ImageHeightMap(BufferedImage heightMapImage, double minHeight, double maxHeight,
			double pixelDistance) {
		super((float) pixelDistance);
		
		this.maxHeight = maxHeight;
		this.pixelDistance = pixelDistance;
		this.xVerts = heightMapImage.getWidth();
		this.zVerts = heightMapImage.getHeight();
		this.heightMap = generateHeightMap(heightMapImage, minHeight, maxHeight);
	}
	
	private double[][] generateHeightMap(BufferedImage heightMapImage, double minHeight, 
			double maxHeight) {
		double[][] heightMap = new double[heightMapImage.getWidth()][heightMapImage.getHeight()];

		double minAfterScale = Double.POSITIVE_INFINITY;
		double maxAfterScale = Double.NEGATIVE_INFINITY;
		
		for(int y = 0; y < heightMapImage.getHeight(); y++) {
			for(int x = 0; x < heightMapImage.getWidth(); x++) {
				double height = heightMapImage.getRGB(x, y);

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

		LOGGER.fine("min height after scaling with " + MAX_PIXEL_COLOR + ": " + minAfterScale);
		LOGGER.fine("max height after scaling with " + MAX_PIXEL_COLOR + ": " + maxAfterScale);
		LOGGER.fine("min height total: " + minFoundHeightFinal);
		LOGGER.fine("max height total: " + maxFoundHeightFinal);
		
		return heightMap;
	}
	
	@Override
	protected float getBaseHeight(float x, float z) {
		if(x + 1e-6 < 0.0) x = 0.0f;
		if(z + 1e-6 < 0.0) z = 0.0f;
		if(x > (xVerts - 1) * pixelDistance + 1e-6) x = (float) ((xVerts - 1) * pixelDistance);
		if(z > (zVerts - 1) * pixelDistance) z = (float) ((zVerts - 1) * pixelDistance);

		double xGrid = x / pixelDistance;
		double zGrid = z / pixelDistance;

		int leftX = (int) xGrid;
		int upZ = (int) zGrid; // up is forward, towards -z
		int rightX = leftX == xVerts - 1 ? leftX : leftX + 1;
		int downZ = upZ == zVerts - 1 ? upZ : upZ + 1;
		
		double u = xGrid - leftX;
		double v = zGrid - upZ;

		double heightLeftUp = heightMap[leftX][upZ];
		double heightLeftDown = heightMap[leftX][downZ];
		double heightRightUp = heightMap[rightX][upZ];
		double heightRightDown = heightMap[rightX][downZ];
		
		double heightUp = (1.0 - u) * heightLeftUp + u * heightRightUp;
		double heightDown = (1.0 - u) * heightLeftDown + u * heightRightDown;
		double height = (1.0 - v) * heightUp + v * heightDown;

		return (float) height;
	}

	@Override
	public float getMaxHeight() {
		return (float) maxHeight;
	}

}
