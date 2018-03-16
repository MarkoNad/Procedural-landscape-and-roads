package hr.fer.zemris.engine.entity;

import org.lwjgl.util.vector.Vector3f;

public abstract class Camera {

	protected Vector3f position;
	
	protected float pitch;
	protected float yaw;
	protected float roll;

	public Camera(Vector3f position) {
		this.position = position;
	}
	
	public Camera() {
		this(new Vector3f(0.0f, 0.0f, 0.0f));
	}
	
	public abstract void update();

	public Vector3f getPosition() {
		return position;
	}

	public float getPitch() {
		return pitch;
	}

	public float getYaw() {
		return yaw;
	}

	public float getRoll() {
		return roll;
	}

}
