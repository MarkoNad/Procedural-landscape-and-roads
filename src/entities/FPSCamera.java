package entities;

import org.lwjgl.util.vector.Vector3f;

import terrains.IHeightGenerator;

public class FPSCamera extends FloatingCamera {
	
	private final float groundOffset;
	private final IHeightGenerator heightMap;
	
	public FPSCamera(Vector3f position, IHeightGenerator heightMap, float crawlSpeed,
			float baseLowSpeed, float baseHighSpeed, float acceleration, float rotationSpeed, 
			float groundOffset) {
		super(position, crawlSpeed, baseLowSpeed, baseHighSpeed, acceleration, rotationSpeed);
		this.heightMap = heightMap;
		this.groundOffset = groundOffset;
	}

	@Override
	public void update() {
		super.update();
	}

	@Override
	protected void move() {
		super.move();
		position.y = heightMap.getHeightApprox(position.x, position.z) + groundOffset;
	}

}
