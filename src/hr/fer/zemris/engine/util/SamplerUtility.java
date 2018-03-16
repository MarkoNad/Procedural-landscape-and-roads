package hr.fer.zemris.engine.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SamplerUtility {

	public static List<Point2Di> sampleCircularCrown(Point2Di center, float innerRadius, float outerRadius,
			float cellSize, boolean avoidSameDirections, int samples, Random random) {
		if(samples != -1 && samples < 0) {
			throw new IllegalArgumentException("Cannot limit number of samples to less than 0.");
		}
		
		int outerDist = (int) Math.ceil(outerRadius / cellSize);

		List<Point2Di> candidatePoints = new ArrayList<>();
		
		for(int z = -outerDist; z <= outerDist; z++) {
			for(int x = -outerDist; x <= outerDist; x++) {
				float realX = x * cellSize;
				float realZ = z * cellSize;
				float distSquared = realX * realX + realZ * realZ;
				
				if(distSquared > outerRadius * outerRadius) continue;
				if(distSquared < innerRadius * innerRadius) continue;
				if(avoidSameDirections && gcd(Math.abs(x), Math.abs(z)) != 1) continue;

				Point2Di tunnelPoint = new Point2Di(center.getX() + x, center.getZ() + z);
				candidatePoints.add(tunnelPoint);
			}
		}

		if(samples == -1) return candidatePoints;
		return randomElements(candidatePoints, samples, random);
	}
	
	public static List<Point2Di> sampleCircularCrown(Point2Di center, float innerRadius, float outerRadius,
			float cellSize, boolean avoidSameDirections) {
		return sampleCircularCrown(center, innerRadius, outerRadius, cellSize, avoidSameDirections, -1, null);
	}
	
	public static List<Point2Di> sampleSquare(Point2Di center, int outerRadius, SamplingType samplingType, 
			boolean excludeCenter) {
		List<Point2Di> candidatePoints = new ArrayList<>();

		for(int z = -outerRadius; z <= outerRadius; z++) {
			for(int x = -outerRadius; x <= outerRadius; x++) {
				switch(samplingType) {
				case ALL:
					break;
				case FARTHEST:
					if(!(z == -outerRadius || z == outerRadius || x == -outerRadius || x == outerRadius)) {
						continue;
					}
					break;
				case NEAREST_UNIQUE:
					if(gcd(Math.abs(x), Math.abs(z)) != 1) continue;
					break;
				}
				
				if(excludeCenter && x == 0 && z == 0) continue;

				Point2Di tunnelPoint = new Point2Di(center.getX() + x, center.getZ() + z);
				candidatePoints.add(tunnelPoint);
			}
		}

		return candidatePoints;
	}

	private static int gcd(int a, int b) {
		return b == 0 ? a : gcd(b, a % b);
	}
	
	private static <T> List<T> randomElements(List<T> elements, int n, Random random) {
		int listLen = elements.size();

		for(int i = 0; i < Math.min(n, listLen); i++) {
			Collections.swap(elements, i, i + random.nextInt(listLen - i));
		}

		return elements.subList(0, Math.min(n, listLen));
	}
	
	public static enum SamplingType {
		FARTHEST,
		NEAREST_UNIQUE,
		ALL
	}
	
}
