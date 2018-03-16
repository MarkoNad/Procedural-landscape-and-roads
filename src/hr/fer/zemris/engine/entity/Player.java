package hr.fer.zemris.engine.entity;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector3f;

import hr.fer.zemris.engine.model.TexturedModel;
import hr.fer.zemris.engine.renderer.DisplayManager;

public class Player extends Entity {
	
	private static final float RUN_SPEED = 20;
	private static final float TURN_SPEED = 160;
	private static final float GRAVITY = -50;
	private static final float JUMP_POWER = 30;
	
	private static final float TERRAIN_HEIGHT = 0;
	
	private float currentSpeed = 0;
	private float currentTurnSpeed = 0;
	private float upwardsSpeed = 0;

	public Player(TexturedModel model, Vector3f position, float rotX, float rotY, float rotZ, float scale) {
		super(model, position, rotX, rotY, rotZ, scale);
	}
	
	public void move() {
		checkInputs();
		increaseRotation(0, currentTurnSpeed * DisplayManager.getFrameTimeSeconds(), 0);
		
		float distance = currentSpeed * DisplayManager.getFrameTimeSeconds();
		float dx = (float) (distance * Math.sin(Math.toRadians(getRotY())));
		float dz = (float) (distance * Math.cos(Math.toRadians(getRotY())));
		increasePosition(dx, 0, dz);
		
		upwardsSpeed += GRAVITY * DisplayManager.getFrameTimeSeconds();
		increasePosition(0, upwardsSpeed * DisplayManager.getFrameTimeSeconds(), 0);
		if(getPosition().y < TERRAIN_HEIGHT) {
			upwardsSpeed = 0;
			getPosition().y = TERRAIN_HEIGHT;
		}
	}
	
	private void jump() {
		if(isInAir()) return;
		upwardsSpeed = JUMP_POWER;
	}
	
	private boolean isInAir() {
		return getPosition().y > TERRAIN_HEIGHT;
	}

	private void checkInputs() {
		if(Keyboard.isKeyDown(Keyboard.KEY_W)) {
			currentSpeed = RUN_SPEED;
		} else if(Keyboard.isKeyDown(Keyboard.KEY_S)) {
			currentSpeed = -RUN_SPEED;
		} else {
			currentSpeed = 0;
		}
		
		if(Keyboard.isKeyDown(Keyboard.KEY_D)) {
			currentTurnSpeed = -TURN_SPEED;
		} else if(Keyboard.isKeyDown(Keyboard.KEY_A)) {
			currentTurnSpeed = TURN_SPEED;
		} else {
			currentTurnSpeed = 0;
		}
		
		if(Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
			jump();
		}
	}

}
