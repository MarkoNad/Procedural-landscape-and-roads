package hr.fer.zemris.engine.search;

public interface IHeuristics<S> {
	
	public double getEstimatedCost(S state);

}
