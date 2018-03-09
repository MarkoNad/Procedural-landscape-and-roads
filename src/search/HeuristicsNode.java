package search;

import java.util.Optional;

public class HeuristicsNode<S> extends Node<S> {
	
	private final double estimatedTotalCost;

	public HeuristicsNode(S state, Optional<Node<S>> predecessor, double cost, double estimatedTotalCost) {
		super(state, predecessor, cost);
		this.estimatedTotalCost = estimatedTotalCost;
	}
	
	public double getEstimatedTotalCost() {
		return estimatedTotalCost;
	}
	
	@Override
	public String toString() {
		return String.format("state: %s, cost: %f, estimated total cost: %f, predecessor: %s", 
				state, cost, estimatedTotalCost, predecessor);
	}

}
