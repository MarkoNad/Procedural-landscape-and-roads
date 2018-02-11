package terrains;

import org.lwjgl.util.vector.Vector3f;

public class UniformHeightGenerator implements IHeightGenerator {

	@Override
	public float getHeight(float x, float z) {
		return 0;
	}

	@Override
	public Vector3f getNormal(float x, float z) {
		return new Vector3f(0.0f, 1.0f, 0.0f);
	}
	
	@Override
	public float getMaxHeight() {
		return 0;
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
