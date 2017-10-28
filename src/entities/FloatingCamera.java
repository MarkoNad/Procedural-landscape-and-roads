package entities;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector3f;

import renderEngine.DisplayManager;

public class FloatingCamera extends Camera {
	
	private static float ACCELERATION = 80f;
	private static float CRAWL_SPEED = 20f;
	private static float BASE_SPEED = 100f;
	private static float BASE_LARGE_SPEED = 200f;
	private static final float ROTATION_SPEED = 12.5f;
	
	private float movementSpeed = 20;
	
	public FloatingCamera(Vector3f position) {
		super();
		Mouse.setGrabbed(true);
		this.position = position;
	}
	
	public FloatingCamera() {
		this(new Vector3f());
	}

	@Override
	public void update() {
		rotate();
		move();
	}
	
	private void rotate() {
		pitch -= Mouse.getDY() * DisplayManager.getFrameTimeSeconds() * ROTATION_SPEED;
		if(pitch > 90) pitch = 90;
		if(pitch < -90) pitch = -90;
		
		yaw += Mouse.getDX() * DisplayManager.getFrameTimeSeconds() * ROTATION_SPEED;
	}

	private void move() {
		if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			if(movementSpeed < BASE_LARGE_SPEED) movementSpeed = BASE_LARGE_SPEED;
			movementSpeed += ACCELERATION * DisplayManager.getFrameTimeSeconds();
		} else if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
			movementSpeed = CRAWL_SPEED;
		} else {
			movementSpeed = BASE_SPEED;
		}
		
		float distance = movementSpeed * DisplayManager.getFrameTimeSeconds();
		float xzDistance = (float) (distance * Math.cos(Math.toRadians(pitch)));
		float yDistance = - (float) (distance * Math.sin(Math.toRadians(pitch)));
		
		if(Keyboard.isKeyDown(Keyboard.KEY_W)) {
			moveForward(xzDistance, yDistance);
		} else if(Keyboard.isKeyDown(Keyboard.KEY_S)) {
			moveBack(xzDistance, yDistance);
		}
		
		if(Keyboard.isKeyDown(Keyboard.KEY_E)) {
			moveUp(distance);
		} else if(Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			moveDown(distance);
		}
		
		if(Keyboard.isKeyDown(Keyboard.KEY_D)) {
			strafeRight(xzDistance);
		} else if(Keyboard.isKeyDown(Keyboard.KEY_A)) {
			strafeLeft(xzDistance);
		}
	}
	
	private void moveForward(float xzDistance, float yDistance) {
		position.x += xzDistance * Math.sin(Math.toRadians(yaw));
		position.z -= xzDistance * Math.cos(Math.toRadians(yaw));
		position.y += yDistance * Math.cos(Math.toRadians(pitch));
	}
	
	private void moveBack(float xzDistance, float yDistance) {
		moveForward(-xzDistance, -yDistance);
	}
	
	private void strafeRight(float distance) {
		position.x += distance * Math.sin(Math.toRadians(yaw + 90));
		position.z -= distance * Math.cos(Math.toRadians(yaw + 90));
	}
	
	private void strafeLeft(float distance) {
		strafeRight(-distance);
	}
	
	private void moveUp(float distance) {
		position.y += distance;
	}
	
	private void moveDown(float distance) {
		moveUp(-distance);
	}

}
