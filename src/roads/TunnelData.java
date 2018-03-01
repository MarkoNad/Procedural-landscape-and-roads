package roads;

import org.lwjgl.util.vector.Vector3f;

public class TunnelData {
	
	private Vector3f firstEndpointLocation;
	private Vector3f firstEndpointOrientation;
	private Vector3f secondEndpointLocation;
	private Vector3f secondEndpointOrientation;
	
	public TunnelData() {
	}
	
	public TunnelData(Vector3f firstEndpointLocation, Vector3f firstEndpointOrientation,
			Vector3f secondEndpointLocation, Vector3f secondEndpointOrientation) {
		this.firstEndpointLocation = firstEndpointLocation;
		this.firstEndpointOrientation = firstEndpointOrientation;
		this.secondEndpointLocation = secondEndpointLocation;
		this.secondEndpointOrientation = secondEndpointOrientation;
	}

	public Vector3f getFirstEndpointLocation() {
		return firstEndpointLocation;
	}

	public void setFirstEndpointLocation(Vector3f firstEndpointLocation) {
		this.firstEndpointLocation = firstEndpointLocation;
	}

	public Vector3f getFirstEndpointOrientation() {
		return firstEndpointOrientation;
	}

	public void setFirstEndpointOrientation(Vector3f firstEndpointOrientation) {
		this.firstEndpointOrientation = firstEndpointOrientation;
	}

	public Vector3f getSecondEndpointLocation() {
		return secondEndpointLocation;
	}

	public void setSecondEndpointLocation(Vector3f secondEndpointLocation) {
		this.secondEndpointLocation = secondEndpointLocation;
	}

	public Vector3f getSecondEndpointOrientation() {
		return secondEndpointOrientation;
	}

	public void setSecondEndpointOrientation(Vector3f secondEndpointOrientation) {
		this.secondEndpointOrientation = secondEndpointOrientation;
	}

}
