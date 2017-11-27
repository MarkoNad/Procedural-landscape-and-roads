//package toolbox;
//
//import java.util.List;
//
//import org.lwjgl.util.vector.Vector3f;
//
//public class BezierCurve {
//	
//	public static enum BezierType {
//		APPROXIMATION,
//		INTERPOLATION
//	}
//
//	private final List<Vector3f> controlPoints;
//	private final int resolution;
//	
//	public BezierCurve(List<Vector3f> controlPoints, BezierType type, int resolution) {
//		if(controlPoints == null) {
//			throw new IllegalArgumentException("Control points cannot be null.");
//		}
//		if(controlPoints.size() < 4) {
//			throw new IllegalArgumentException("At least four points are required.");
//		}
//		
//		this.resolution = resolution;
//
//		if(type.equals(BezierType.APPROXIMATION)) {
//			this.controlPoints = controlPoints;
//		} else {
//			this.controlPoints = convertToApproxPoints(controlPoints);
//		}
//	}
//	
//	private List<Vector3f> convertToApproxPoints(List<Vector3f> interpPoints) {
//		throw new UnsupportedOperationException("Not implemented yet");
////		List<Vector3f> controlPoints = new ArrayList<>();
////		return controlPoints;
//	}
//
//	public List<Vector3f> getCurvePoints() {
//		throw new UnsupportedOperationException("Not implemented yet");
////		List<Vector3f> points = new ArrayList<>();
////		
////		Vector3f p0 = controlPoints.get(0);
////		Vector3f p1 = controlPoints.get(1);
////		Vector3f p2 = controlPoints.get(2);
////		Vector3f p3 = controlPoints.get(3);
////		
////		for(int step = 0; step <= resolution; step++) {
////			float t = step / (float)resolution;
////			
////			float t2 = t * t;
////			float t3 = t2 * t;
////			
////			float first = (float) Math.pow(1 - t, 3);
////			float second = 3 * (t - 2 * t2 + t3);
////			float third = 3 * (t2 - t3);
////			float fourth = t3;
////			
////			float bezierx = first * p0.x + second * p1.x + third * p2.x + fourth * p3.x;
////			float beziery = first * p0.y + second * p1.y + third * p2.y + fourth * p3.y;
////			float bezierz = first * p0.z + second * p1.z + third * p2.z + fourth * p3.z;
////			
////			points.add(new Vector3f(bezierx, beziery, bezierz));
////		}
////		
////		return points;
//	}
//	
//	public List<Vector3f> getControlPoints() {
//		return controlPoints;
//	}
//	
//}