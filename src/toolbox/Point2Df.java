package toolbox;

public class Point2Df {
		
	private final float x;
	private final float z;

	public Point2Df(float x, float z) {
		this.x = x;
		this.z = z;
	}
	
	public float getX() {
		return x;
	}
	
	public float getZ() {
		return z;
	}
	
	public static float distance(Point2Df p1, Point2Df p2) {
		return (float) Math.hypot(p1.x - p2.x, p1.z - p2.z);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(z);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Point2Df other = (Point2Df) obj;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
			return false;
		if (Float.floatToIntBits(z) != Float.floatToIntBits(other.z))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + z + ")";
	}
	
}