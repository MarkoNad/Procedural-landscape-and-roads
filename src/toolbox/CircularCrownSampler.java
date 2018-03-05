package toolbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CircularCrownSampler {

	public static List<Point2Di> sample(Point2Di center, float innerRadius, float outerRadius,
			float cellSize, boolean avoidSameDirections, int samples, Random random) {
		if(samples != -1 && samples < 0) {
			throw new IllegalArgumentException("Cannot limit number of samples to less than 0.");
		}
		
		int innerDist = (int) (innerRadius / cellSize);
		int outerDist = (int) (outerRadius / cellSize);
		
		List<Point2Di> candidatePoints = new ArrayList<>();
		
		for(int z = center.getZ() - outerDist; z <= center.getZ() + outerDist; z++) {
			for(int x = center.getX() - outerDist; x <= center.getX() + outerDist; x++) {
				int distSquared = 
						(center.getX() - x) *  (center.getX() - x) + 
						(center.getZ() - z) *  (center.getZ() - z);
				
				if(distSquared > outerDist * outerDist) continue;
				if(distSquared < innerDist * innerDist) continue;
				if(avoidSameDirections && gcd(Math.abs(x), Math.abs(z)) != 1) continue;
				
				Point2Di tunnelPoint = new Point2Di(x, z);
				candidatePoints.add(tunnelPoint);
			}
		}
		
		if(samples == -1) return candidatePoints;
		return randomElements(candidatePoints, samples, random);
	}
	
	public static List<Point2Di> sample(Point2Di center, float innerRadius, float outerRadius,
			float cellSize, boolean avoidSameDirections) {
		return sample(center, innerRadius, outerRadius, cellSize, avoidSameDirections, -1, null);
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
	
}
