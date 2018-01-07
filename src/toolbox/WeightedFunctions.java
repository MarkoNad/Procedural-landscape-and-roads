package toolbox;

import java.util.function.Function;

public class WeightedFunctions {
	
	private static final float EPS = 1e-6f;
	
	public static Function<Float, Float> lFunction(float alpha, float beta) {
		if(alpha > beta + EPS) {
			throw new IllegalArgumentException("Beta needs to be >= alpha; " +
					"alpha = " + alpha + ", beta = " + beta);
		}
		
		return x -> {
			if(x < alpha) return 1f;
			if(x >= beta) return 0f;
			return (beta - x) / (float)(beta - alpha);
		};
	}
	
	public static Function<Float, Float> gammaFunction(float alpha, float beta) {
		if(alpha > beta + EPS) {
			throw new IllegalArgumentException("Beta needs to be >= alpha; " +
					"alpha = " + alpha + ", beta = " + beta);
		}
		
		return x -> {
			if(x < alpha) return 0f;
			if(x >= beta) return 1f;
			return (x - alpha) / (float)(beta - alpha);
		};
	}
	
	public static Function<Float, Float> lambdaFunction(float alpha, float beta, float gamma) {
		if(alpha > beta + EPS || beta > gamma + EPS) {
			throw new IllegalArgumentException("The following condition is not met: gamma >= beta >= alpha." +
					"alpha = " + alpha + ", beta = " + beta + ", gamma = " + gamma);
		}
		
		return x -> {
			if(x < alpha || x >= gamma) return 0f;
			if(x >= alpha && x < beta) return (x - alpha) / (float)(beta - alpha);
			return (gamma - x) / (float)(gamma - beta);
		};
	}
	
	public static Function<Float, Float> piFunction(float alpha, float beta, float gamma, float delta) {
		if(alpha > beta + EPS || beta > gamma + EPS || gamma > delta + EPS) {
			throw new IllegalArgumentException("The following condition is not met: delta >= gamma >= beta >= alpha." +
					"alpha = " + alpha + ", beta = " + beta + ", gamma = " + gamma + "delta = " + delta);
		}
		
		return x -> {
			if(x < alpha || x >= delta) return 0f;
			if(x >= alpha && x < beta) return (x - alpha) / (float)(beta - alpha);
			if(x >= beta && x < gamma) return 1f;
			return (delta - x) / (float)(delta - gamma);
		};
	}
	
	public static Function<Float, Float> constantFunction() {
		return x -> 1f;
	}

}
