package toolbox;

import java.util.List;

public interface ITrajectory<S> {

	public List<S> getPoints();
	public List<S> getPointsCopy();
	public List<Float> getPointDistances();
	public List<S> getControlPoints();
	public List<S> getControlPointsCopy();
	
}
