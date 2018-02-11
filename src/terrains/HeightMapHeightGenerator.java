package terrains;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.lwjgl.util.vector.Vector3f;

public class HeightMapHeightGenerator implements IHeightGenerator {
	
	private static final float MAX_PIXEL_COLOR = 256 * 256 * 256;
	private static final float MAX_HEIGHT = 40;
	
	private BufferedImage image;
	private float[][] heights;
	
	public HeightMapHeightGenerator(String fileName) {
		try {
			this.image = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		heights = new float[getXPoints()][getYPoints()];
		calculateHeights();
	}

	private void calculateHeights() {
		for(int z = 0; z < getYPoints(); z++) {
			for(int x = 0; x < getXPoints(); x++) {
				float height = image.getRGB((int)x, (int)z);
				
				height += MAX_PIXEL_COLOR / 2.0f;
				height /= (MAX_PIXEL_COLOR / 2.0f);
				height *= MAX_HEIGHT;
				
				heights[x][z] = height;
			}
		}
	}

	@Override
	public float getHeight(float x, float z) {
		if(x < 0 || z < 0 || x >= image.getHeight() || z >= image.getWidth()) {
			return 0;
		}
		
		return heights[(int)x][(int)z];
	}
	
	@Override
	public Vector3f getNormal(float x, float z) {
		float heightL = getHeight(x - 1, z);
		float heightR = getHeight(x + 1, z);
		float heightD = getHeight(x, z - 1);
		float heightU = getHeight(x, z + 1);
		
		if(heightL > 0 || heightR > 0 || heightD > 0 || heightU > 0) {
			System.out.println(heightL);
			System.out.println(heightR);
			System.out.println(heightD);
			System.out.println(heightU);
			System.out.println();
		}
		
		Vector3f normal = new Vector3f(heightL - heightR, 2.0f, heightD - heightU);
		normal.normalise();
		
		return normal;
	}
	
	public int getXPoints() {
		return image.getWidth();
	}
	
	public int getYPoints() {
		return image.getHeight();
	}
	
	@Override
	public float getMaxHeight() {
		return MAX_HEIGHT;
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
