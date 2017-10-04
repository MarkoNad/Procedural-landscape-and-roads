package entities;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector3f;

public class Camera {
	
	private Vector3f position = new Vector3f(0, 0, 0);
	private float pitch;
	private float yaw;
	private float roll;
	
	private static final float LOW_SPEED = 0.2f;
	private static final float HIGH_SPEED = 1;
	
	public Camera() {}
	
	public Camera(Vector3f position) {
		this.position = position;
	}
	
	public void move() {
		float cameraSpeed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? HIGH_SPEED : LOW_SPEED;
		
//		if(Keyboard.isKeyDown(Keyboard.KEY_W)) position.z -= cameraSpeed;
//		if(Keyboard.isKeyDown(Keyboard.KEY_D)) position.x += cameraSpeed;
//		if(Keyboard.isKeyDown(Keyboard.KEY_A)) position.x -= cameraSpeed;
//		if(Keyboard.isKeyDown(Keyboard.KEY_S)) position.z += cameraSpeed;
//		if(Keyboard.isKeyDown(Keyboard.KEY_Q)) position.y -= cameraSpeed;
//		if(Keyboard.isKeyDown(Keyboard.KEY_E)) position.y += cameraSpeed;
	}

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
