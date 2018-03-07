package roads;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

import search.IProblem;
import terrains.IHeightGenerator;
import toolbox.Point2Df;
import toolbox.Point2Di;
import toolbox.SamplerUtility;
import toolbox.SamplerUtility.SamplingType;

public class PathfindingProblem implements IProblem<Point2Di> {
	
	private static final Logger LOGGER = Logger.getLogger(PathfindingProblem.class.getName());
	// how many times height is sampled to determine if a road goes through mountain
	private static final int tunnelSamples = 5;
	private final float samplingDist;
	
	private final Point2Df origin;
	private final Point2Di goal;
	private final Point2Df domainLowerLeftLimit;
	private final Point2Df domainUpperRightLimit;
	private final IHeightGenerator heightGenerator;
	private final boolean allowTunnels;

	private final float cellSize;
	private final int roadRange;
	private final float tunnelInnerRadius;
	private final float tunnelOuterRadius;
	private final int tunnelCandidates;
	private final boolean limitTunnelCandidates;
	private final Random random;

	// road parameters
	private final double maxRoadSlopePercent; // in percentage
	private final double maxRoadCurvature; // in radians
	private final double roadLengthMultiplier;
	private final double roadSlopeMultiplier;
	private final double roadCurvatureMultiplier;
	private final double roadSlopeExponent;
	private final double roadCurvatureExponent;
	
	private final SamplingType roadSamplingType;
	
	// tunnel parameters
	private final double maxTunnelSlopePercent; // in percentage
	private final double maxTunnelCurvature; // in radians
	private final double tunnelLengthMultiplier;
	private final double tunnelSlopeMultiplier;
	private final double tunnelCurvatureMultiplier;
	private final double tunnelSlopeExponent;
	private final double tunnelCurvatureExponent;

	public PathfindingProblem(Point2Df origin, Point2Df goal, Point2Df domainLowerLeftLimit,
			Point2Df domainUpperRightLimit, IHeightGenerator heightGenerator, float cellSize, boolean allowTunnels,
			 float tunnelInnerRadius, float tunnelOuterRadius, int tunnelCandidates,
			boolean limitTunnelCandidates, Random random, int roadRange, double maxRoadSlopePercent, double maxRoadCurvature,
			double roadLengthMultiplier, double roadSlopeMultiplier, double roadCurvatureMultiplier,
			double roadSlopeExponent, double roadCurvatureExponent, double maxTunnelSlopePercent,
			double maxTunnelCurvature, double tunnelLengthMultiplier, double tunnelSlopeMultiplier,
			double tunnelCurvatureMultiplier, double tunnelSlopeExponent, double tunnelCurvatureExponent,
			SamplingType roadSamplingType) {
		this.origin = origin;
		this.domainLowerLeftLimit = domainLowerLeftLimit;
		this.domainUpperRightLimit = domainUpperRightLimit;
		this.heightGenerator = heightGenerator;
		this.allowTunnels = allowTunnels;
		this.cellSize = cellSize;
		this.samplingDist = this.cellSize / (float)tunnelSamples;
		this.roadRange = roadRange;
		this.tunnelInnerRadius = tunnelInnerRadius;
		this.tunnelOuterRadius = tunnelOuterRadius;
		this.tunnelCandidates = tunnelCandidates;
		this.limitTunnelCandidates = limitTunnelCandidates;
		this.random = random;
		this.maxRoadSlopePercent = maxRoadSlopePercent;
		this.maxRoadCurvature = maxRoadCurvature;
		this.roadLengthMultiplier = roadLengthMultiplier;
		this.roadSlopeMultiplier = roadSlopeMultiplier;
		this.roadCurvatureMultiplier = roadCurvatureMultiplier;
		this.roadSlopeExponent = roadSlopeExponent;
		this.roadCurvatureExponent = roadCurvatureExponent;
		this.maxTunnelSlopePercent = maxTunnelSlopePercent;
		this.maxTunnelCurvature = maxTunnelCurvature;
		this.tunnelLengthMultiplier = tunnelLengthMultiplier;
		this.tunnelSlopeMultiplier = tunnelSlopeMultiplier;
		this.tunnelCurvatureMultiplier = tunnelCurvatureMultiplier;
		this.tunnelSlopeExponent = tunnelSlopeExponent;
		this.tunnelCurvatureExponent = tunnelCurvatureExponent;
		this.roadSamplingType = roadSamplingType;
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
		return (float) (roadRange * cellSize * Math.sqrt(2.0));
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
		return SamplerUtility.sampleSquare(p, roadRange, roadSamplingType, true);
	}
	
	private List<Point2Di> generateTunnelCandidates(Point2Di p, float cellSize, float innerRadius,
			float outerRadius, int tunnelCandidates, Random random) {
		return limitTunnelCandidates ?
				SamplerUtility.sampleCircularCrown(p, innerRadius, outerRadius, cellSize, true, tunnelCandidates, random) :
				SamplerUtility.sampleCircularCrown(p, innerRadius, outerRadius, cellSize, true);
	}

	@Override
	public double getTransitionCost(Point2Di current, Point2Di candidate, Optional<Point2Di> previous) {
		if(Point2Df.distance(gridToReal(current), gridToReal(candidate)) <= getMax2DRoadSize() + 1e-6) {
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
		double distanceCost = distance * roadLengthMultiplier;

		double slope = Math.asin(Math.abs(y2 - y1) / distance);
		if(slope > percentageToAngle(maxRoadSlopePercent)) return Double.POSITIVE_INFINITY;
		double slopeCost = distance * Math.pow(slope, roadSlopeExponent) * roadSlopeMultiplier;
		
//		System.out.format("distance: %10.2f, distance cost:  %10.2f\n", distance, distanceCost);
//		System.out.format("slope:    %10.2f, slope cost:     %10.2f\n", Math.toDegrees(slope), slopeCost);
		
		double curvatureCost = 0.0;
		if(previous.isPresent()) {
			Point2Df direction1 = Point2Df.sub(current, previous.get());
			Point2Df direction2 = Point2Df.sub(candidate, current);

			double angle = Point2Df.angle(direction1, direction2);
			if(angle > maxRoadCurvature) return Double.POSITIVE_INFINITY;
			
			curvatureCost = Math.pow(angle, roadCurvatureExponent) * roadCurvatureMultiplier;
			
			//System.out.format("angle:    %10.2f, curvature cost: %10.2f\n", Math.toDegrees(angle), curvatureCost);
		}
		
//		System.out.println();
		
		double totalCost = distanceCost + slopeCost + curvatureCost;
		return totalCost;
	}
	
	private double percentageToAngle(double p) {
		return p / Math.sqrt(1 + p * p);
	}
	
	private double tunnelCost(Point2Di currentGP, Point2Di candidateGP, Optional<Point2Di> previousGP) {
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
		
		if(!goesThroughMountain(current, candidate, (float)y1, (float)y2, samplingDist, heightGenerator)) {
			return Double.POSITIVE_INFINITY;
		}
		
		double distance = Math.sqrt(
				Math.pow(current.getX() - candidate.getX(), 2.0) + 
				Math.pow(y1 - y2, 2.0) + 
				Math.pow(current.getZ() - candidate.getZ(), 2.0));
		double distanceCost = distance * tunnelLengthMultiplier;

		double slope = Math.asin(Math.abs(y2 - y1) / distance);
		if(slope > percentageToAngle(maxTunnelSlopePercent)) return Double.POSITIVE_INFINITY;
		double slopeCost = distance * Math.pow(slope, tunnelSlopeExponent) * tunnelSlopeMultiplier;
		
//		System.out.format("distance: %10.2f, distance cost:  %10.2f\n", distance, distanceCost);
//		System.out.format("slope:    %10.2f, slope cost:     %10.2f\n", Math.toDegrees(slope), slopeCost);
		
		double curvatureCost = 0.0;
		if(previous.isPresent()) {
			Point2Df direction1 = Point2Df.sub(current, previous.get());
			Point2Df direction2 = Point2Df.sub(candidate, current);

			double angle = Point2Df.angle(direction1, direction2);
			if(angle > maxTunnelCurvature) return Double.POSITIVE_INFINITY;
			
			curvatureCost = Math.pow(angle, tunnelCurvatureExponent) * tunnelCurvatureMultiplier;
			
			//System.out.format("angle:    %10.2f, curvature cost: %10.2f\n", Math.toDegrees(angle), curvatureCost);
		}
		
//		System.out.println();
		
		double totalCost = distanceCost + slopeCost + curvatureCost;
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
				LOGGER.severe("Tried to check if two same points were going through mountain.");
				return false;
			}

			float minAllowedHeight = lowerY + (higherY - lowerY) * fraction;
			
			if(sampleHeight <= minAllowedHeight) return false;
		}
		
		return true;
	}

}
