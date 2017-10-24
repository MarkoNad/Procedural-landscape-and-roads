package terrains;

import org.lwjgl.util.vector.Vector3f;

public interface IHeightGenerator {
	
	public float getHeight(float x, float y);
	public Vector3f getNormal(int x, int z);

}
