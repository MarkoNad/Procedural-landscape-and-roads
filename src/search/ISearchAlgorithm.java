package search;

import java.util.Optional;

public interface ISearchAlgorithm<S> {
	
	public Optional<Node<S>> search();
	public String getName();

}
