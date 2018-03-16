package hr.fer.zemris.engine.util;

public interface TriFunction<A, B, C, R> {
	
	public R apply(A a, B b, C c);

}
