package roads;

import org.lwjgl.util.vector.Vector3f;

public class EndpointData {
	
	private Vector3f location;
	private Vector3f orientation;
	
	public EndpointData(Vector3f location, Vector3f orientation) {
		this.location = location;
		this.orientation = orientation;
	}
	
	public Vector3f getLocation() {
		return location;
	}
	
	public Vector3f getOrientation() {
		return orientation;
	}

}
