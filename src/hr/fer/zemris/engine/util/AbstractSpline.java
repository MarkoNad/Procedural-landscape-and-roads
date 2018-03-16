package hr.fer.zemris.engine.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractSpline<S> implements ITrajectory<S> {
	
	protected final List<S> controlPoints;
	protected final List<S> splinePoints;
	protected final List<Float> trajectoryPointsDistances; // distances[0] = 0, distances[1] = distance(p0, p1)
	
	public AbstractSpline(List<S> controlPoints, float segmentLen) {
		if(controlPoints == null) {
			throw new IllegalArgumentException("Control points cannot be null.");
		}
		
		if(controlPoints.size() < 2) {
			throw new IllegalArgumentException("At least two points are required.");
		}
		
		this.controlPoints = controlPoints;
		this.splinePoints = generateCurve(controlPoints, segmentLen);
		this.trajectoryPointsDistances = determineLengths(this.splinePoints);
	}
	
	@Override
	public List<S> getPoints() {
		return Collections.unmodifiableList(splinePoints);
	}
	
	@Override
	public List<S> getPointsCopy() {
		return Collections.unmodifiableList(new ArrayList<>(splinePoints));
	}
	
	@Override
	public List<Float> getPointDistances() {
		return Collections.unmodifiableList(trajectoryPointsDistances);
	}
	
	@Override
	public List<S> getControlPoints() {
		return Collections.unmodifiableList(controlPoints);
	}
	
	@Override
	public List<S> getControlPointsCopy() {
		return Collections.unmodifiableList(new ArrayList<>(controlPoints));
	}

	protected abstract List<S> generateCurve(List<S> controlPoints, float segmentLen);
	protected abstract List<Float> determineLengths(List<S> splinePoints);

}
