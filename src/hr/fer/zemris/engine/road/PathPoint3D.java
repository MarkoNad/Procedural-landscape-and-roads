package hr.fer.zemris.engine.road;

import org.lwjgl.util.vector.Vector3f;

public class PathPoint3D {
	
	private Vector3f location;
	private boolean entrance;
	private boolean exit;
	private boolean body;
	
	public PathPoint3D(Vector3f location, boolean entrance, boolean exit, boolean body) {
		this.location = location;
		this.entrance = entrance;
		this.exit = exit;
		this.body = body;
	}

	public Vector3f getLocation() {
		return location;
	}

	public boolean isEntrance() {
		return entrance;
	}

	public boolean isExit() {
		return exit;
	}

	public boolean isBody() {
		return body;
	}

	@Override
	public String toString() {
		return "PathPoint3D [loc=" + location + ", entrance=" + entrance + ", exit=" + 
				exit + ", body=" + body + "]";
	}

}
