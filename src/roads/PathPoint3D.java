package roads;

import org.lwjgl.util.vector.Vector3f;

public class PathPoint3D {
	
	private Vector3f location;
	private boolean isTunnelEndpoint;
	private boolean isInTunnel;
	
	public PathPoint3D(Vector3f location, boolean isTunnelEndpoint, boolean isInTunnel) {
		this.location = location;
		this.isTunnelEndpoint = isTunnelEndpoint;
		this.isInTunnel = isInTunnel;
	}
	
	public Vector3f getLocation() {
		return location;
	}
	
	public boolean isInTunnel() {
		return isInTunnel;
	}
	
	public boolean isTunnelEndpoint() {
		return isTunnelEndpoint;
	}

	@Override
	public String toString() {
		return "PathPoint3D [location=" + location + ", isTunnelEndpoint=" + isTunnelEndpoint + ", isInTunnel="
				+ isInTunnel + "]";
	}

}
