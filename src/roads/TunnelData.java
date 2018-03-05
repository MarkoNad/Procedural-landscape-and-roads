package roads;

import org.lwjgl.util.vector.Vector3f;

public class TunnelData {
	
	private Vector3f firstEndpointLocation;
	private Vector3f firstEndpointOrientation;
	private Vector3f secondEndpointLocation;
	private Vector3f secondEndpointOrientation;
	private Vector3f firstEndpointMask;
	private Vector3f secondEndpointMask;
	
	public TunnelData() {
	}

	public TunnelData(Vector3f firstEndpointLocation, Vector3f firstEndpointOrientation,
			Vector3f secondEndpointLocation, Vector3f secondEndpointOrientation,
			Vector3f firstEndpointMask, Vector3f secondEndpointMask) {
		this.firstEndpointLocation = firstEndpointLocation;
		this.firstEndpointOrientation = firstEndpointOrientation;
		this.secondEndpointLocation = secondEndpointLocation;
		this.secondEndpointOrientation = secondEndpointOrientation;
		this.firstEndpointMask = firstEndpointMask;
		this.secondEndpointMask = secondEndpointMask;
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

	public Vector3f getFirstEndpointMask() {
		return firstEndpointMask;
	}

	public void setFirstEndpointMask(Vector3f firstEndpointMask) {
		this.firstEndpointMask = firstEndpointMask;
	}

	public Vector3f getSecondEndpointMask() {
		return secondEndpointMask;
	}

	public void setSecondEndpointMask(Vector3f secondEndpointMask) {
		this.secondEndpointMask = secondEndpointMask;
	}

}
