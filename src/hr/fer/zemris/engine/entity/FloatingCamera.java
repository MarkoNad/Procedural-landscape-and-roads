package hr.fer.zemris.engine.entity;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector3f;

import hr.fer.zemris.engine.renderer.DisplayManager;

public class FloatingCamera extends Camera {
	
	private static final float DEFAULT_ACCELERATION = 300f;
	private static final float DEFAULT_CRAWL_SPEED = 20f;
	private static final float DEFAULT_BASE_LOW_SPEED = 100f;
	private static final float DEFAULT_BASE_HIGH_SPEED = 1000f;
	private static final float DEFAULT_ROTATION_SPEED = 12.5f;
	
	private final float acceleration;
	private final float crawlSpeed;
	private final float baseLowSpeed;
	private final float baseHighSpeed;
	private final float rotationSpeed;
	
	private float movementSpeed;
	
	public FloatingCamera(Vector3f position, float crawlSpeed, float baseLowSpeed,
			float baseHighSpeed, float acceleration, float rotationSpeed) {
		super(position);
		this.baseLowSpeed = baseLowSpeed;
		this.baseHighSpeed = baseHighSpeed;
		this.crawlSpeed = crawlSpeed;
		this.acceleration = acceleration;
		this.rotationSpeed = rotationSpeed;
		movementSpeed = baseLowSpeed;
		Mouse.setGrabbed(true);
	}
	
	public FloatingCamera(Vector3f position) {
		this(position,
			DEFAULT_CRAWL_SPEED,
			DEFAULT_BASE_LOW_SPEED,
			DEFAULT_BASE_HIGH_SPEED,
			DEFAULT_ACCELERATION,
			DEFAULT_ROTATION_SPEED
		);
	}
	
	public FloatingCamera() {
		this(new Vector3f());
	}

	@Override
	public void update() {
		rotate();
		move();
	}
	
	protected void rotate() {
		pitch -= Mouse.getDY() * DisplayManager.getFrameTimeSeconds() * rotationSpeed;
		if(pitch > 90) pitch = 90;
		if(pitch < -90) pitch = -90;
		
		yaw += Mouse.getDX() * DisplayManager.getFrameTimeSeconds() * rotationSpeed;
	}

	protected void move() {
		if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			if(movementSpeed < baseHighSpeed) movementSpeed = baseHighSpeed;
			movementSpeed += acceleration * DisplayManager.getFrameTimeSeconds();
		} else if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
			movementSpeed = crawlSpeed;
		} else {
			movementSpeed = baseLowSpeed;
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
		position.y += yDistance;
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
