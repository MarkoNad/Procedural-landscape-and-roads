package toolbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.lwjgl.util.vector.Vector3f;

public class CatmullRomSpline3D extends AbstractSpline<Vector3f> {
	
	public CatmullRomSpline3D(List<Vector3f> controlPoints, float segmentLen) {
		super(controlPoints, segmentLen);
	}
	
	@Override
	public List<Vector3f> getPointsCopy() {
		return splinePoints
				.stream()
				.map(p -> new Vector3f(p.x, p.y, p.z))
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	@Override
	public List<Vector3f> getControlPointsCopy() {
		return Collections.unmodifiableList(controlPoints
				.stream()
				.map(p -> new Vector3f(p.x, p.y, p.z))
				.collect(Collectors.toCollection(ArrayList::new)));
	}

	@Override
	protected List<Vector3f> generateCurve(List<Vector3f> controlPoints, float segmentLen) {
		List<Vector3f> splinePoints = new ArrayList<>();

		for(int point = 0; point < controlPoints.size() - 1; point++) {
			Vector3f p1 = controlPoints.get(point);
			Vector3f p2 = controlPoints.get(point + 1);
			
			Vector3f p0 = point == 0 ? p1 : controlPoints.get(point - 1);
			Vector3f p3 = point == controlPoints.size() - 2 ? p2 : controlPoints.get(point + 2);
			
			// calculate the segment between p1 and p2
			float distance = Vector3f.sub(p2, p1, null).length();
			int npoints = Math.max(2, (int) (distance / segmentLen) + 1);
			
			// edge point is not included
			for(int i = 0; i < npoints - 1; i++) {
				float t = i / (float)(npoints - 1);
				
				float t2 = t * t;
				float t3 = t2 * t;
				
				float curvex = 0.5f * (
						2 * p1.x + 
						t * (-p0.x + p2.x) + 
						t2 * (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) + 
						t3 * (-p0.x + 3 * p1.x - 3 * p2.x + p3.x)
				);
				float curvey = 0.5f * (
						2 * p1.y + 
						t * (-p0.y + p2.y) + 
						t2 * (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) + 
						t3 * (-p0.y + 3 * p1.y - 3 * p2.y + p3.y)
				);
				float curvez = 0.5f * (
						2 * p1.z + 
						t * (-p0.z + p2.z) + 
						t2 * (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) + 
						t3 * (-p0.z + 3 * p1.z - 3 * p2.z + p3.z)
				);
				
				splinePoints.add(new Vector3f(curvex, curvey, curvez));
			}
		}
		
		Vector3f lastPoint = controlPoints.get(controlPoints.size() - 1);
		splinePoints.add(new Vector3f(lastPoint.x, lastPoint.y, lastPoint.z));
		
		return splinePoints;
	}
	
	@Override
	protected List<Float> determineLengths(List<Vector3f> splinePoints) {
		List<Float> trajectoryPointsDistances = new ArrayList<>();
		trajectoryPointsDistances.add(0f);
		
		float totalDistance = 0f;
		
		for(int i = 1; i < splinePoints.size(); i++) {
			Vector3f p0 = splinePoints.get(i - 1);
			Vector3f p1 = splinePoints.get(i);
			
			float segmentLength = Vector3f.sub(p0, p1, null).length();
			totalDistance += segmentLength;
			
			trajectoryPointsDistances.add(totalDistance);
		}
		
		return trajectoryPointsDistances;
	}

}
