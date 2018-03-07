package search;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Node<S> implements Comparable<Node<S>> {

	protected final S state;
	protected final Optional<Node<S>> predecessor;
	protected final double cost;
	
	public Node(S state, Optional<Node<S>> predecessor, double cost) {
		this.state = state;
		this.predecessor = predecessor;
		this.cost = cost;
	}
	
	public S getState() {
		return state;
	}
	
	public Optional<Node<S>> getPredecessor() {
		return predecessor;
	}
	
	public double getCost() {
		return cost;
	}
	
	public List<S> reconstructPath() {
		LinkedList<S> path = new LinkedList<>();
		path.addFirst(this.state);
		
		Node<S> current = this;
		
		while(current.getPredecessor().isPresent()) {
			current = current.getPredecessor().get();
			path.addFirst(current.getState());
		}
		
		return path;
	}

	@Override
	public int compareTo(Node<S> other) {
		return Double.compare(this.cost, other.cost);
	}
	
	@Override
	public String toString() {
		return String.format("state: %s, cost: %f", state, cost);
	}
	
}
