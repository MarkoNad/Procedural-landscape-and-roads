package entities;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector3f;

public class Camera {
	
	private float distanceFromPlayer = 50;
	private float angleAroundPlayer = 0;
	
	private Vector3f position = new Vector3f(0, 0, 0);
	private float pitch;
	private float yaw;
	private float roll;
	
	private Player player;
	
	public Camera(Player player) {
		this.player = player;
	}
	
	public void move() {
		calculateZoom();
		calculatePitch();
		calculateAngleAroundPlayer();
		
		float horizontalDistance = calculateHorizontalDistance();
		float verticalDistance = calculateVerticalDistance();
		calculateCameraPosition(horizontalDistance, verticalDistance);
		
		yaw = 180 - (player.getRotY() + angleAroundPlayer);
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
	
	private void calculateCameraPosition(float horizDistance, float vertDistance) {
		float theta = player.getRotY() + angleAroundPlayer;
		float xOffset = (float) (horizDistance * Math.sin(Math.toRadians(theta)));
		float zOffset = (float) (horizDistance * Math.cos(Math.toRadians(theta)));
		position.y = player.getPosition().y + vertDistance;
		position.x = player.getPosition().x - xOffset;
		position.z = player.getPosition().z - zOffset;
	}
	
	private float calculateHorizontalDistance() {
		return (float) (distanceFromPlayer * Math.cos(Math.toRadians(pitch)));
	}
	
	private float calculateVerticalDistance() {
		return (float) (distanceFromPlayer * Math.sin(Math.toRadians(pitch)));
	}
	
	private void calculateZoom() {
		distanceFromPlayer -= Mouse.getDWheel() * 0.1f;
	}
	
	private void calculatePitch() {
		//if(!Mouse.isButtonDown(1)) return;
		if(!Mouse.isButtonDown(0)) return;
		pitch -= Mouse.getDY() * 0.1f;
	}
	
	private void calculateAngleAroundPlayer() {
		if(!Mouse.isButtonDown(0)) return;
		angleAroundPlayer -= Mouse.getDX() * 0.3f;
	}

}
