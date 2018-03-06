package roads;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

import search.IProblem;
import terrains.IHeightGenerator;
import toolbox.CircularCrownSampler;
import toolbox.Point2Df;
import toolbox.Point2Di;

public class PathfindingProblem implements IProblem<Point2Di> {
	
	private static final Logger LOGGER = Logger.getLogger(PathfindingProblem.class.getName());
	private static final float samplingDist = 50f;
	
	private final Point2Df origin;
	private final Point2Di goal;
	private final Point2Df domainLowerLeftLimit;
	private final Point2Df domainUpperRightLimit;
	private final IHeightGenerator heightGenerator;
	private final boolean allowTunnels;

	private final float cellSize;
	private final float tunnelInnerRadius;
	private final float tunnelOuterRadius;
	private final int tunnelCandidates;
	private final boolean limitTunnelCandidates;
	private final Random random;
	private final int step = 1;

	public PathfindingProblem(Point2Df origin, Point2Df goal, Point2Df domainLowerLeftLimit, 
			Point2Df domainUpperRightLimit, IHeightGenerator heightGenerator, float cellSize,
			boolean allowTunnels, float tunnelInnerRadius, float tunnelOuterRadius,
			int tunnelCandidates, boolean limitTunnelCandidates, Random random) {
		this.origin = origin;
		this.domainLowerLeftLimit = domainLowerLeftLimit;
		this.domainUpperRightLimit = domainUpperRightLimit;
		this.heightGenerator = heightGenerator;
		//this.cellSize = cellSize;
		this.cellSize = 100f; // TODO
		this.allowTunnels = allowTunnels;
		this.tunnelInnerRadius = tunnelInnerRadius;
		this.tunnelOuterRadius = tunnelOuterRadius;
		this.tunnelCandidates = tunnelCandidates;
		this.limitTunnelCandidates = limitTunnelCandidates;
		this.random = random;
		this.goal = realToGrid(goal);
	}
	
	public float getCellSize() {
		return cellSize;
	}
	
	public Point2Df gridToReal(Point2Di gridPoint) {
		float realX = origin.getX() + gridPoint.getX() * cellSize;
		float realZ = origin.getZ() + gridPoint.getZ() * cellSize;
		return new Point2Df(realX, realZ);
	}
	
	private Point2Di realToGrid(Point2Df realPoint) {
		int gridX = (int)((realPoint.getX() - origin.getX()) / cellSize);
		int gridZ = (int)((realPoint.getZ() - origin.getZ()) / cellSize);
		return new Point2Di(gridX, gridZ);
	}
	
	public float getMax2DRoadSize() {
		return 2f * cellSize; // TODO update when neighborhood is defined for roads
	}

	@Override
	public Point2Di getInitialState() {
		return realToGrid(origin);
	}

	@Override
	public boolean isGoal(Point2Di state) {
		return state.equals(goal);
	}

	@Override
	public Iterable<Point2Di> getSuccessors(Point2Di p) {
		List<Point2Di> candidates = generateRoadCandidates(p);
		
		if(allowTunnels) {
			List<Point2Di> tunnelCandidatePoints = generateTunnelCandidates(p, cellSize,
					tunnelInnerRadius, tunnelOuterRadius, tunnelCandidates, random);
			candidates.addAll(tunnelCandidatePoints);
		}
		
		return candidates;
	}
	
	@Override
	public double getMaximumCost() {
		return Double.POSITIVE_INFINITY;
	}
	
	private List<Point2Di> generateRoadCandidates(Point2Di p) {
		return new ArrayList<>(Arrays.asList(
				new Point2Di(p.getX(), p.getZ() - step),
				new Point2Di(p.getX() + step, p.getZ() - step),
				new Point2Di(p.getX() + step, p.getZ()),
				new Point2Di(p.getX() + step, p.getZ() + step),
				new Point2Di(p.getX(), p.getZ() + step),
				new Point2Di(p.getX() - step, p.getZ() + step),
				new Point2Di(p.getX() - step, p.getZ()),
				new Point2Di(p.getX() - step, p.getZ() - step))
			);
	}
	
	private List<Point2Di> generateTunnelCandidates(Point2Di p, float cellSize, float innerRadius,
			float outerRadius, int tunnelCandidates, Random random) {
		return limitTunnelCandidates ?
				CircularCrownSampler.sample(p, innerRadius, outerRadius, cellSize, true, tunnelCandidates, random) :
				CircularCrownSampler.sample(p, innerRadius, outerRadius, cellSize, true);
	}

	@Override
	public double getTransitionCost(Point2Di current, Point2Di candidate, Optional<Point2Di> previous) {
		if(Point2Df.distance(gridToReal(current), gridToReal(candidate)) <= getMax2DRoadSize()) {
			return roadCost(current, candidate, previous);
		} else {
			return tunnelCost(current, candidate, previous);
		}
	}
	
	private double roadCost(Point2Di currentGP, Point2Di candidateGP, Optional<Point2Di> previousGP) {
		Point2Df current = gridToReal(currentGP);
		Point2Df candidate = gridToReal(candidateGP);
		Optional<Point2Df> previous = previousGP.map(prevGP -> gridToReal(prevGP));
		
		if(candidate.getX() < domainLowerLeftLimit.getX() ||
				candidate.getX() > domainUpperRightLimit.getX() ||
				candidate.getZ() > domainLowerLeftLimit.getZ() || 
				candidate.getZ() < domainUpperRightLimit.getZ()) {
			return Double.POSITIVE_INFINITY;
		}

		double y1 = heightGenerator.getHeightApprox(current.getX(), current.getZ());
		double y2 = heightGenerator.getHeightApprox(candidate.getX(), candidate.getZ());
		
		double distance = Math.sqrt(
				Math.pow(current.getX() - candidate.getX(), 2.0) + 
				Math.pow(y1 - y2, 2.0) + 
				Math.pow(current.getZ() - candidate.getZ(), 2.0));
		double distanceCost = distance;
		
		final double angleThreshold = percentageToAngle(0.5);
		double slope = Math.asin(Math.abs(y2 - y1) / distance);
		double slopeCost = slope > angleThreshold ? Double.POSITIVE_INFINITY : distance * Math.pow(slope, 2.0) * 80.0;
		
		System.out.println(String.format("distance:%.2f, distance cost:  %.2f", distance, distanceCost));
		System.out.println(String.format("slope:   %.2f, slope cost:     %.2f", slope, slopeCost));
		
		double curvatureCost = 0.0;
		if(previous.isPresent()) {
			Point2Df direction1 = Point2Df.sub(current, previous.get());
			Point2Df direction2 = Point2Df.sub(candidate, current);
			double angle = Point2Df.angle(direction1, direction2);
			curvatureCost = Math.pow(angle, 2.0) * 10.0;
			System.out.println(String.format("angle:   %.2f, curvature cost: %.2f", angle, curvatureCost));
		}

		System.out.println();
		
		double totalCost = distanceCost + slopeCost + curvatureCost;
		return totalCost;
	}
	
	private double percentageToAngle(double p) {
		return p / Math.sqrt(1 + p * p);
	}
	
	private double tunnelCost(Point2Di first, Point2Di second, Optional<Point2Di> previous) {
		Point2Df p1 = gridToReal(first);
		Point2Df p2 = gridToReal(second);
		
		if(p2.getX() < domainLowerLeftLimit.getX() ||
				p2.getX() > domainUpperRightLimit.getX() ||
				p2.getZ() > domainLowerLeftLimit.getZ() || 
				p2.getZ() < domainUpperRightLimit.getZ()) {
			return Double.POSITIVE_INFINITY;
		}
		
		double totalCost = 0.0;

		double y1 = heightGenerator.getHeightApprox(p1.getX(), p1.getZ());
		double y2 = heightGenerator.getHeightApprox(p2.getX(), p2.getZ());
		
		if(!goesThroughMountain(p1, p2, (float)y1, (float)y2, samplingDist, heightGenerator)) {
			return Double.POSITIVE_INFINITY;
		}
		
		double distance = Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2.0) + 
				Math.pow(y1 - y2, 2.0) + Math.pow(p1.getZ() - p2.getZ(), 2.0));
		double distanceCost = distance * 1.1;
		
		double slope = Math.abs(y2 - y1) / distance;
		double slopeCost = Math.pow(slope, 2.0) * 10000f;
		
//		System.out.println(String.format("TUNNEL slope:   %.2f, slope cost:    %.2f", slope, slopeCost));
//		System.out.println(String.format("TUNNEL distance:%.2f, distance cost: %.2f", distance, distanceCost));
		
		totalCost += distanceCost;
		totalCost += slopeCost;
		
		return totalCost;
	}
	
	private boolean goesThroughMountain(Point2Df p1, Point2Df p2, float y1, float y2,
			float samplingDist, IHeightGenerator heightMap) {
		Point2Df direction = Point2Df.sub(p2, p1);
		direction = Point2Df.normalize(direction);
		
		final float eps = 1e-3f;
		
		float dist = Point2Df.distance(p1, p2);
		float d = dist / samplingDist;
		int samples = (int)d;
		if(d - (int)d < eps) samples--;
		
		if(samples <= 0) {
			LOGGER.severe("Number of tunnel samples is invalid: " + samples);
			return false;
		}
		
		final float lowerY = y1 < y2 ? y1 : y2;
		final float higherY = y1 < y2 ? y2 : y1;
		
		final Point2Df lowerP = y1 < y2 ? p1 : p2;
		
		final float deltaX = Math.abs(p2.getX() - p1.getX());
		final float deltaZ = Math.abs(p2.getZ() - p1.getZ());

		for(int i = 0; i < samples; i++) {
			float x = p1.getX() + direction.getX() * samplingDist * (i + 1);
			float z = p1.getZ() + direction.getZ() * samplingDist * (i + 1);

			float sampleHeight = heightMap.getHeightApprox(x, z);
			
			float fraction;
			if(Math.abs(deltaX) > 1e-6) {
				fraction = Math.abs(x - lowerP.getX()) / deltaX;
			} else if(Math.abs(deltaZ) > 1e-6) {
				fraction = Math.abs(z - lowerP.getZ()) / deltaZ;
			} else {
				LOGGER.severe("Tried to check if two same points are going through mountain.");
				return false;
			}

			float minAllowedHeight = lowerY + (higherY - lowerY) * fraction;
			
			if(sampleHeight <= minAllowedHeight) return false;
		}
		
		return true;
	}

}
