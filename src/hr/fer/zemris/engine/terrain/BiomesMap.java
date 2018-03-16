package hr.fer.zemris.engine.terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.lwjgl.util.vector.Vector3f;

import hr.fer.zemris.engine.util.Globals;
import hr.fer.zemris.engine.util.Range;
import hr.fer.zemris.engine.util.TriFunction;
import hr.fer.zemris.engine.util.WeightedFunctions;

public class BiomesMap implements ITextureMap {
	
	private static final float EPS = 1e-6f;
	private static final float DEFAULT_INTERPOLATION_INTERVAL = 0f;
	
	private static final NoiseMap DEFAULT_TREE_TYPE_VARIATION_MAP = new NoiseMap(450, 0.01f, 0);
	private static final NoiseMap DEFAULT_MOISTURE_MAP = new NoiseMap(1, 0.0003f, 1);
	private static final float DEFAULT_LOWER_TREE_THRESHOLD = 600.0f;
	private static final float DEFAULT_UPPER_TREE_THRESHOLD = 800.0f;
	
	private final IHeightMap heightMap;
	private final NoiseMap treeTypeVariationMap;
	private final NoiseMap moistureMap;
	private final float upperTreeThreshold;
	private final float lowerTreeThreshold;
	private final List<Function<Float, Float>> weightedFunctions;
	private final TriFunction<Float, Float, Float, Float> textureVariation;
	private final Random random;

	public BiomesMap(IHeightMap heightMap, List<Range> textureRanges, float interpolationInterval,
			TriFunction<Float, Float, Float, Float> textureVariation, NoiseMap treeTypeVariationMap,
			NoiseMap moistureMap, float lowerTreeThreshold, float upperTreeThreshold, Random random) {
		this.heightMap = heightMap;
		this.textureVariation = textureVariation;
		this.treeTypeVariationMap = treeTypeVariationMap;
		this.moistureMap = moistureMap;
		this.lowerTreeThreshold = lowerTreeThreshold;
		this.upperTreeThreshold = upperTreeThreshold;
		this.random = random;
		this.weightedFunctions = createWeightedFunctions(textureRanges, interpolationInterval);
	}
	
	public BiomesMap(IHeightMap heightMap, List<Range> textureRanges, float interpolationInterval,
			TriFunction<Float, Float, Float, Float> textureVariation, Random random) {
		this(heightMap, textureRanges, interpolationInterval, textureVariation,
				DEFAULT_TREE_TYPE_VARIATION_MAP, DEFAULT_MOISTURE_MAP, DEFAULT_LOWER_TREE_THRESHOLD,
				DEFAULT_UPPER_TREE_THRESHOLD, random);
	}
	
	public BiomesMap(IHeightMap heightMap, List<Range> textureRanges, Random random) {
		this(heightMap, textureRanges, DEFAULT_INTERPOLATION_INTERVAL, (x, y, z) -> 0f,
				DEFAULT_TREE_TYPE_VARIATION_MAP, DEFAULT_MOISTURE_MAP, DEFAULT_LOWER_TREE_THRESHOLD,
				DEFAULT_UPPER_TREE_THRESHOLD, random);
	}
	
	public BiomesMap(IHeightMap heightMap, List<Range> textureRanges, float interpolationInterval,
			Random random) {
		this(heightMap, textureRanges, interpolationInterval, (x, y, z) -> 0f,
				DEFAULT_TREE_TYPE_VARIATION_MAP, DEFAULT_MOISTURE_MAP, DEFAULT_LOWER_TREE_THRESHOLD,
				DEFAULT_UPPER_TREE_THRESHOLD, random);
	}
	
	public BiomesMap(IHeightMap heightMap, List<Range> textureRanges,
			TriFunction<Float, Float, Float, Float> textureVariation, Random random) {
		this(heightMap, textureRanges, DEFAULT_INTERPOLATION_INTERVAL, textureVariation,
				DEFAULT_TREE_TYPE_VARIATION_MAP, DEFAULT_MOISTURE_MAP, DEFAULT_LOWER_TREE_THRESHOLD,
				DEFAULT_UPPER_TREE_THRESHOLD, random);
	}
	
	public TreeType getTreeType(float x, float z) {
		float height = heightMap.getHeightApprox(x, z);
		float modifiedHeight = height + treeTypeVariationMap.getNoise(x, z);
		
		if(modifiedHeight < lowerTreeThreshold) return TreeType.OAK;
		if(modifiedHeight > upperTreeThreshold) return TreeType.PINE;
		
		double r = random.nextDouble();
		double pineProbability = (modifiedHeight - lowerTreeThreshold) /
				(upperTreeThreshold - lowerTreeThreshold);
		return r < pineProbability ? TreeType.OAK : TreeType.PINE;
	}
	
	public float getTreeDensity(float x, float z) {
		float slope = Vector3f.angle(Globals.Y_AXIS, heightMap.getNormalApprox(x, z));
		float moisture = moistureMap.getPrenormalizedNoise(x, z);
		return (float) (Math.cos(slope) * moisture);
	}

	private List<Function<Float, Float>> createWeightedFunctions(List<Range> ranges,
			float interpolationInterval) {
		List<Function<Float, Float>> weightedFunctions = new ArrayList<>();
		
		if(ranges.size() == 1) {
			weightedFunctions.add(WeightedFunctions.constantFunction());
			return weightedFunctions;
		}
		
		final float d = interpolationInterval / 2.0f;
		
		Range firstRange = ranges.get(0);
		
		if(Math.abs(firstRange.getEnd() - ranges.get(1).getStart()) > EPS) {
			throw new IllegalArgumentException("One texture range has to start where " +
					"the previous one ends; end was" + firstRange.getEnd() + 
					", and start: " + ranges.get(1).getStart());
		}
		
		weightedFunctions.add(WeightedFunctions.lFunction(
				firstRange.getEnd() - d,
				firstRange.getEnd() + d)
		);
		
		for(int i = 1; i < ranges.size() - 1; i++) {
			Range curr = ranges.get(i);
			Range next = ranges.get(i + 1);
			
			if(Math.abs(curr.getEnd() - next.getStart()) > EPS) {
				throw new IllegalArgumentException("One texture range has to start where " +
						"the previous one ends; end was" + curr.getEnd() + 
						", and start: " + next.getStart());
			}
			
			
			weightedFunctions.add(WeightedFunctions.piFunction(
					curr.getStart() - d,
					curr.getStart() + d,
					curr.getEnd() - d,
					curr.getEnd() + d)
			);
		}
		
		Range lastRange = ranges.get(ranges.size() - 1);
		
		if(Math.abs(ranges.get(ranges.size() - 2).getEnd() - lastRange.getStart()) > EPS) {
			throw new IllegalArgumentException("One texture range has to start where " +
					"the previous one ends; end was" + ranges.get(ranges.size() - 2).getEnd() + 
					", and start: " + lastRange.getStart());
		}
		
		weightedFunctions.add(WeightedFunctions.gammaFunction(
				lastRange.getStart() - d,
				lastRange.getStart() + d)
		);
		
		return weightedFunctions;
	}
	
	// height can be computed directly from coordinates if heightmap is provided, 
	// it is expected here to avoid duplicate calculations
	@Override
	public float[] textureInfluences(float xcoord, float height, float zcoord) {
		float[] texStrengths = new float[weightedFunctions.size()];
		
		float modifiedHeight = height + textureVariation.apply(xcoord, height, zcoord);
		
		for(int i = 0; i < weightedFunctions.size(); i++) {
			texStrengths[i] = weightedFunctions.get(i).apply(modifiedHeight);
		}
		
		return texStrengths;
	}
	
	@Override
	public void textureInfluences(float xcoord, float height, float zcoord, float[] buffer) {
		float modifiedHeight = height + textureVariation.apply(xcoord, height, zcoord);
		
		for(int i = 0; i < weightedFunctions.size(); i++) {
			buffer[i] = weightedFunctions.get(i).apply(modifiedHeight);
		}
	}

	@Override
	public int getNumberOfInfluences() {
		return weightedFunctions.size();
	}

}
