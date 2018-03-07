package toolbox;

public class Point2Di {
		
	private int x;
	private int z;

	public Point2Di(int x, int z) {
		this.x = x;
		this.z = z;
	}
	
	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getZ() {
		return z;
	}
	
	public void setZ(int z) {
		this.z = z;
	}
	
	public static float distance(Point2Di p1, Point2Di p2) {
		return (float) Math.hypot(p1.x - p2.x, p1.z - p2.z);
	}
	
	public static int l1Distance(Point2Di p1, Point2Di p2) {
		return Math.abs(p1.x - p2.x) + Math.abs(p1.z - p2.z);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + z;
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
		Point2Di other = (Point2Di) obj;
		if (x != other.x)
			return false;
		if (z != other.z)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + z + ")";
	}
	
}