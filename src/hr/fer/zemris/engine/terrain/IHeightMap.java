package hr.fer.zemris.engine.terrain;

import org.lwjgl.util.vector.Vector3f;

public interface IHeightMap {
	
	public float getHeight(float x, float z);
	public float getHeightApprox(float x, float z);
	public Vector3f getNormal(float x, float z);
	public Vector3f getNormalApprox(float x, float z);
	public float getMaxHeight();

}
