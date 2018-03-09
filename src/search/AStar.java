package search;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

public class AStar<S> implements ISearchAlgorithm<S> {
	
	private final IProblem<S> problem;
	private final IHeuristics<S> heuristics;
	
	public AStar(IProblem<S> problem, IHeuristics<S> heuristics) {
		this.problem = problem;
		this.heuristics = heuristics;
	}

	@Override
	public Optional<Node<S>> search() {
		S initialState = problem.getInitialState();
		
		Map<S, HeuristicsNode<S>> openMap = new HashMap<>();
		Map<S, HeuristicsNode<S>> closedMap = new HashMap<>();
		Queue<HeuristicsNode<S>> openQueue = new PriorityQueue<>(
			(a, b) -> Double.compare(a.getEstimatedTotalCost(), b.getEstimatedTotalCost()));

		HeuristicsNode<S> startNode = new HeuristicsNode<>(
				initialState, Optional.empty(), 0.0, heuristics.getEstimatedCost(initialState));
		
		openMap.put(initialState, startNode);
		openQueue.add(startNode);

		while(!openMap.isEmpty()) {
			HeuristicsNode<S> current = openQueue.remove();
			openMap.remove(current.getState());
			closedMap.put(current.getState(), current);
			
			if(problem.isGoal(current.getState())) {
				return Optional.of(current);
			}
			
			if(current.cost >= problem.getMaximumCost()) {
				return Optional.empty();
			}
			
			for(S succState : problem.getSuccessors(current.getState())) {
				if(closedMap.containsKey(succState)) continue;
				
				double transitionCost = problem.getTransitionCost(
						current.getState(),
						succState,
						current.getPredecessor().map(node -> node.getState()));
				
				HeuristicsNode<S> candidateSucc = new HeuristicsNode<>(
					succState,
					Optional.of(current),
					current.getCost() + transitionCost,
					current.getCost() + transitionCost + heuristics.getEstimatedCost(succState));
				
				HeuristicsNode<S> existingSuccInOpen = openMap.get(succState);
				if(existingSuccInOpen != null && existingSuccInOpen.getCost() < candidateSucc.getCost()) {
					continue;
				}
				
				HeuristicsNode<S> existingSuccInClosed = closedMap.get(succState);
				if(existingSuccInClosed != null && existingSuccInClosed.getCost() < candidateSucc.getCost()) {
					continue;
				}
				
				if(existingSuccInOpen != null) {
					openMap.remove(succState);
					openQueue.remove(existingSuccInOpen);
				}
				
				openMap.put(succState, candidateSucc);
				openQueue.add(candidateSucc);
			}
		}
		
		return Optional.empty();
	}

	@Override
	public String getName() {
		return "A Star";
	}
}
