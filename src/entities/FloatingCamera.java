package entities;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import renderEngine.DisplayManager;

public class FloatingCamera extends Camera {
	
	private static float ACCELERATION = 30f;
	private static float BASE_SPEED = 20f;
	private static float BASE_LARGE_SPEED = 60f;
	private static float MAX_SPEED = 200;
	private static final float ROTATION_SPEED = 12.5f;
	
	private float movementSpeed = 20;
	
	public FloatingCamera() {
		super();
		Mouse.setGrabbed(true);
		position.y = 5;
	}

	@Override
	public void update() {
		pitch -= Mouse.getDY() * DisplayManager.getFrameTimeSeconds() * ROTATION_SPEED;
		yaw += Mouse.getDX() * DisplayManager.getFrameTimeSeconds() * ROTATION_SPEED;
		
		float distance = movementSpeed * DisplayManager.getFrameTimeSeconds();
		float xzDistance = (float) (distance * Math.cos(Math.toRadians(pitch)));
		float yDistance = - (float) (distance * Math.sin(Math.toRadians(pitch)));
		
		if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			if(movementSpeed < BASE_LARGE_SPEED) movementSpeed = BASE_LARGE_SPEED;
			movementSpeed += ACCELERATION * DisplayManager.getFrameTimeSeconds();
			if(movementSpeed >= MAX_SPEED) movementSpeed = MAX_SPEED;
		} else {
			movementSpeed = BASE_SPEED;
		}
		
		move(distance, xzDistance, yDistance);
	}

	protected void move(float distance, float xzDistance, float yDistance) {
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
