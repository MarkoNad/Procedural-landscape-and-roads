package hr.fer.zemris.engine.search;

import java.util.Optional;

public interface IProblem<S> {
	
	public S getInitialState();
	public boolean isGoal(S state);
	public Iterable<S> getSuccessors(S state);
	public double getTransitionCost(S first, S second, Optional<S> firstsPredecessor);
	public double getMaximumCost();
	
}
