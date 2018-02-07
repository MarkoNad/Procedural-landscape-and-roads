package search;

public interface IHeuristics<S> {
	
	public double getEstimatedCost(S state);

}
