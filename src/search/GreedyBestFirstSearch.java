package search;

import java.util.HashSet;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

public class GreedyBestFirstSearch<S> implements ISearchAlgorithm<S> {

	private final IProblem<S> problem;
	private final IHeuristics<S> heuristics;
	
	public GreedyBestFirstSearch(IProblem<S> problem, IHeuristics<S> heuristics) {
		if(problem == null) {
			throw new IllegalArgumentException("Problem definition cannot be null.");
		}
		if(heuristics == null) {
			throw new IllegalArgumentException("Heuristics cannot be null.");
		}
		
		this.problem = problem;
		this.heuristics = heuristics;
	}
	
	@Override
	public Optional<Node<S>> search() {
		PriorityQueue<Node<S>> openQueue = new PriorityQueue<>(
				(n1, n2) -> Double.compare(
						heuristics.getEstimatedCost(n1.getState()),
						heuristics.getEstimatedCost(n2.getState())));
		Set<S> closedStates = new HashSet<>();
		
		S initialState = problem.getInitialState();
		openQueue.add(new HeuristicsNode<>(
				initialState,
				Optional.empty(),
				0.0,
				heuristics.getEstimatedCost(initialState)));
		
		while(!openQueue.isEmpty()) {
			Node<S> current = openQueue.remove();
			
			if(problem.isGoal(current.getState())) {
				return Optional.of(current);
			}
			
			if(current.getCost() + 1e-6 >= problem.getMaximumCost()) {
				continue;
			}
			
			closedStates.add(current.getState());
			
			for(S successor : problem.getSuccessors(current.getState())) {
				if(closedStates.contains(successor)) continue;
				
				double transitionCost = problem.getTransitionCost(
						current.getState(),
						successor,
						current.getPredecessor().map(node -> node.getState()));
				
				Node<S> succNode = new HeuristicsNode<>(
						successor,
						Optional.of(current),
						current.getCost() + transitionCost,
						current.getCost() + transitionCost + heuristics.getEstimatedCost(successor));
				
				openQueue.add(succNode);
			}
		}
		
		return Optional.empty();
	}

	@Override
	public String getName() {
		return "Greedy best-first";
	}

}
