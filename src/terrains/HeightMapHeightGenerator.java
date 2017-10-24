package terrains;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class HeightMapHeightGenerator implements IHeightGenerator {
	
	private static final float MAX_PIXEL_COLOR = 256 * 256 * 256;
	private static final float MAX_HEIGHT = 40;
	
	private BufferedImage image;
	
	public HeightMapHeightGenerator(String fileName) {
		try {
			this.image = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public float getHeight(float x, float z) {
		if(x < 0 || z < 0 || x >= image.getHeight() || z >= image.getWidth()) {
			return 0;
		}
		
		float height = image.getRGB((int)x, (int)z);
		height += MAX_PIXEL_COLOR / 2.0f;
		height /= (MAX_PIXEL_COLOR / 2.0f);
		height *= MAX_HEIGHT;

		return height;
	}
	
	public int getImageWidth() {
		return image.getWidth();
	}
	
	public int getImageHeight() {
		return image.getHeight();
	}

}
