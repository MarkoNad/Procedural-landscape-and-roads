package terrains;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.lwjgl.util.vector.Vector3f;

import toolbox.Range;
import toolbox.TriFunction;
import toolbox.WeightedFunctions;

public class BiomesMap implements ITextureMap {
	
	private static final Vector3f Y_AXIS = new Vector3f(0f, 1f, 0f);
	private static final float EPS = 1e-6f;
	private static final float DEFAULT_INTERPOLATION_INTERVAL = 0f;
	
	private static NoiseMap noiseMap = new NoiseMap(450, 0.01f, 0);
	private NoiseMap moistureMap = new NoiseMap(1, 0.0003f, 1);
	private final IHeightGenerator heightMap;

	private final List<Function<Float, Float>> weightedFunctions;
	private final TriFunction<Float, Float, Float, Float> textureVariation;

	public BiomesMap(IHeightGenerator heightMap, List<Range> textureRanges, float interpolationInterval,
			TriFunction<Float, Float, Float, Float> textureVariation) {
		this.heightMap = heightMap;
		this.textureVariation = textureVariation;
		this.weightedFunctions = createWeightedFunctions(textureRanges, interpolationInterval);
	}
	
	public BiomesMap(IHeightGenerator heightMap, List<Range> textureRanges) {
		this(heightMap, textureRanges, DEFAULT_INTERPOLATION_INTERVAL, (x, y, z) -> 0f);
	}
	
	public BiomesMap(IHeightGenerator heightMap, List<Range> textureRanges, float interpolationInterval) {
		this(heightMap, textureRanges, interpolationInterval, (x, y, z) -> 0f);
	}
	
	public BiomesMap(IHeightGenerator heightMap, List<Range> textureRanges,
			TriFunction<Float, Float, Float, Float> textureVariation) {
		this(heightMap, textureRanges, DEFAULT_INTERPOLATION_INTERVAL, textureVariation);
	}
	
	public static enum TreeType {
		OAK,
		PINE
	}
	
	public TreeType getTreeType(float x, float z) {
		float height = heightMap.getHeight(x, z);
		float modifiedHeight = height + noiseMap.getNoise(x, z);
		if(modifiedHeight < 600) return TreeType.OAK;
		return TreeType.PINE;
	}
	
	public float getTreeDensity(float x, float z) {
		float slope = Vector3f.angle(Y_AXIS, heightMap.getNormal(x, z));
		float moisture = moistureMap.getPrenormalizedNoise(x, z);
		//float moisture = getMoisture(x, z);
		return (float) (Math.cos(slope) * moisture);
	}
	
//	private float getMoisture(float x, float z) {
//		float moisture = moistureMap.getPrenormalizedNoise(x, z);
//		//moisture = (float)  Math.pow((moisture + 0.5) / 1.5, 2);
//		return moisture;
//	}
	
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
