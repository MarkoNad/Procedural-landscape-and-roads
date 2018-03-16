package terrains;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

import textures.TerrainTexture;
import textures.TerrainTexturePack;

public class TerrainMends {

	private final Map<Integer, Map<Integer, ITerrain>> middleLodTo_rightLodToMend;
	private final Map<Integer, Map<Integer, ITerrain>> middleLodTo_downLodToMend;

	public TerrainMends(float xUpperLeft, float zUpperLeft, float size,
			Map<Integer, Float> lodLevelToVertsPerUnit, boolean generateRight, boolean generateDown,
			IHeightMap heightMap, ITextureMap textureMap, TerrainTexturePack texturePack,
			TerrainTexture blendMap, Vector3f translation, float textureWidth,
			float textureDepth) {
		this.middleLodTo_rightLodToMend = new HashMap<>();
		this.middleLodTo_downLodToMend = new HashMap<>();
		
		generateMends(xUpperLeft, zUpperLeft, size, lodLevelToVertsPerUnit, generateRight, generateDown,
				heightMap, textureMap, texturePack, blendMap, translation, textureWidth,
				textureDepth);
	}

	private void generateMends(float xUpperLeft, float zUpperLeft, float size,
			Map<Integer, Float> lodLevelToVertsPerUnit, boolean generateRight, boolean generateDown,
			IHeightMap heightMap, ITextureMap textureMap, TerrainTexturePack texturePack,
			TerrainTexture blendMap, Vector3f translation, float textureWidth,
			float textureDepth) {
		Set<Integer> levels = lodLevelToVertsPerUnit.keySet();
		
		for(int middleLod : levels) {
			if(generateRight) {
				Map<Integer, ITerrain> rightLodToMend = new HashMap<>();
				middleLodTo_rightLodToMend.put(middleLod, rightLodToMend);
			}
			
			if(generateDown) {
				Map<Integer, ITerrain> downLodToMend = new HashMap<>();
				middleLodTo_downLodToMend.put(middleLod, downLodToMend);
			}
			
			for(int otherLod : levels) {
				if(middleLod == otherLod) continue;
				
				float middleVertsPerUnit = lodLevelToVertsPerUnit.get(middleLod);
				float otherVertsPerUnit = lodLevelToVertsPerUnit.get(otherLod);
				
				if(generateRight) {
					ITerrain rightMend = new TerrainMend(
							xUpperLeft,
							zUpperLeft,
							size,
							middleVertsPerUnit,
							otherVertsPerUnit,
							heightMap,
							textureMap,
							texturePack,
							blendMap,
							translation,
							textureWidth,
							textureDepth,
							true);
					
					Map<Integer, ITerrain> rightLodToMend = middleLodTo_rightLodToMend.get(middleLod);
					rightLodToMend.put(otherLod, rightMend);
				}
				
				if(generateDown) {
					ITerrain downMend = new TerrainMend(
							xUpperLeft,
							zUpperLeft,
							size,
							middleVertsPerUnit,
							otherVertsPerUnit,
							heightMap,
							textureMap,
							texturePack,
							blendMap,
							translation,
							textureWidth,
							textureDepth,
							false);
					
					Map<Integer, ITerrain> downLodToMend = middleLodTo_downLodToMend.get(middleLod);
					downLodToMend.put(otherLod, downMend);
				}
			}
		}
	}

	public Optional<ITerrain> rightMend(int middleLOD, int rightLOD) {
		if(middleLOD == rightLOD) return Optional.empty();
		return Optional.of(middleLodTo_rightLodToMend.get(middleLOD).get(rightLOD));
	}

	public Optional<ITerrain> downMend(int middleLOD, int downLOD) {
		if(middleLOD == downLOD) return Optional.empty();
		return Optional.of(middleLodTo_downLodToMend.get(middleLOD).get(downLOD));
	}

}
