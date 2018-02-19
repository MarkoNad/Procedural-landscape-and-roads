package terrains;

import org.lwjgl.util.vector.Vector3f;

public interface IHeightGenerator {
	
	public float getHeight(float x, float z);
	public float getHeightApprox(float x, float z);
	public Vector3f getNormal(float x, float z);
	public Vector3f getNormalApprox(float x, float z);
	public float getMaxHeight();

}
