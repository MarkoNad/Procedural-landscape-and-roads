package terrains;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import renderEngine.Loader;
import textures.TerrainTexture;
import textures.TerrainTexturePack;

public class TerrainMend implements ITerrain {
	
	private static final Logger LOGGER = Logger.getLogger(TerrainMend.class.getName());
	private static final int NUM_TEXTURES = 3;
	
	private final TerrainTexturePack texturePack;
	private final TerrainTexture blendMap;
	private final Vector3f translation;
	private final RawModel model;

	public TerrainMend(float xUpperLeft, float zUpperLeft, float size, float thisVertsPerUnit,
			float otherVertsPerUnit, IHeightGenerator heightMap, ITextureMap textureMap,
			TerrainTexturePack texturePack, Loader loader, TerrainTexture blendMap, Vector3f translation,
			float textureWidth, float textureDepth, boolean isRight) {
		
		this.texturePack = texturePack;
		this.blendMap = blendMap;
		this.translation = translation;
		
		this.model = load(
				xUpperLeft,
				zUpperLeft,
				size,
				thisVertsPerUnit,
				otherVertsPerUnit,
				heightMap,
				textureMap,
				textureWidth,
				textureDepth,
				isRight,
				loader);
	}
	
	private RawModel load(float xUpperLeft, float zUpperLeft, float size,
			float thisVertsPerUnit, float otherVertsPerUnit, IHeightGenerator heightMap,
			ITextureMap textureMap, float textureWidth, float textureDepth, boolean isRight,
			Loader loader) {
		TerrainData mendData = generateTerrainData(
				xUpperLeft,
				zUpperLeft,
				size,
				thisVertsPerUnit,
				otherVertsPerUnit,
				heightMap,
				textureMap,
				textureWidth,
				textureDepth,
				isRight);
		
		RawModel mendModel = loader.loadToVAO(
				mendData.getVertices(),
				mendData.getTextureCoords(),
				mendData.getNormals(),
				mendData.getTangents(),
				mendData.getIndices(),
				mendData.getTextureInfluences());
		
		return mendModel;
	}

	@Override
	public Optional<RawModel> getModel() {
		return Optional.of(model);
	}

	@Override
	public TerrainTexturePack getTexturePack() {
		return this.texturePack;
	}

	@Override
	public TerrainTexture getBlendMap() {
		return this.blendMap;
	}

	@Override
	public Vector3f getTranslation() {
		return this.translation;
	}
	
	private TerrainData generateTerrainData(float xUpperLeft, float zUpperLeft, float size,
			float thisVertsPerUnit, float otherVertsPerUnit, IHeightGenerator heightMap,
			ITextureMap textureMap, float textureWidth, float textureDepth, boolean isRight) {
		float xStart = isRight ? xUpperLeft + size : xUpperLeft;
		float zStart = isRight ? zUpperLeft : zUpperLeft + size;

		int thisVerticesCount = (int) (Math.round(size * thisVertsPerUnit) + 1);
		int otherVerticesCount = (int) (Math.round(size * otherVertsPerUnit) + 1);
		
		List<Vector3f> thisVertices = new LinkedList<>();
		List<Vector3f> otherVertices = new LinkedList<>();
		
		for(int i = 0; i < thisVerticesCount; i++) {
			float xcoord = isRight ? xStart : i / (float) (thisVerticesCount - 1) * size + xUpperLeft;
			float zcoord = isRight ? i / (float) (thisVerticesCount - 1) * size + zUpperLeft : zStart;
			float height = heightMap.getHeight(xcoord, zcoord);
			thisVertices.add(new Vector3f(xcoord, height, zcoord));
		}
		
		for(int i = 0; i < otherVerticesCount; i++) {
			float xcoord = isRight ? xStart : i / (float) (otherVerticesCount - 1) * size + xUpperLeft;
			float zcoord = isRight ? i / (float) (otherVerticesCount - 1) * size + zUpperLeft : zStart;
			float height = heightMap.getHeight(xcoord, zcoord);
			otherVertices.add(new Vector3f(xcoord, height, zcoord));
		}

		int toInsert = Math.abs(thisVerticesCount - otherVerticesCount);
		List<Vector3f> receivingList = thisVerticesCount < otherVerticesCount ? thisVertices : otherVertices;
		
		while(toInsert > 0) {
			List<Vector3f> potentialVerts = new ArrayList<>();

			Vector3f previous = receivingList.get(0);
			for(Vector3f current : receivingList) {
				if(current.equals(previous)) continue;
				
				Vector3f potentialVert = Vector3f.add(previous, current, null);
				potentialVert.scale(0.5f);
				potentialVerts.add(potentialVert);
				
				previous = current;
			}
			
			int insertIndex = 1;
			for(Vector3f potentialVert : potentialVerts) {
				if(toInsert == 0) break;
				
				receivingList.add(insertIndex, potentialVert);
				
				insertIndex += 2;
				toInsert--;
			}
		}
		
		if(thisVertices.size() != otherVertices.size()) {
			LOGGER.severe("Terrain mend lists have invalid number of vertices.");
		}
		
		final int count = thisVertices.size() * 2;
		float[] vertices = new float[count * 3];
		float[] normals = new float[count * 3];
		float[] textureCoords = new float[count * 2];
		float[] textureInfluences = new float[count * NUM_TEXTURES]; // 3 textures per vertex (r, g, b)
		float[] tangents = new float[count * 3];
		int[] indices = new int[6 * (thisVertices.size() - 1)];
		
		float[] texStrengthsBuffer = new float[NUM_TEXTURES];
		
		for(int i = 0; i < thisVertices.size(); i++) {
			Vector3f thisVert = thisVertices.get(i);
			Vector3f otherVert = otherVertices.get(i);

			vertices[i * 3] = thisVert.x;
			vertices[i * 3 + 1] = thisVert.y;
			vertices[i * 3 + 2] = thisVert.z;
			
			vertices[(thisVertices.size() + i) * 3] = otherVert.x;
			vertices[(thisVertices.size() + i) * 3 + 1] = otherVert.y;
			vertices[(thisVertices.size() + i) * 3 + 2] = otherVert.z;

			
			Vector3f thisNormal = heightMap.getNormalApprox(thisVert.x, thisVert.y);
			normals[i * 3] = thisNormal.x;
			normals[i * 3 + 1] = thisNormal.y;
			normals[i * 3 + 2] = thisNormal.z;
			
			Vector3f otherNormal = heightMap.getNormalApprox(otherVert.x, otherVert.y);
			normals[(thisVertices.size() + i) * 3] = otherNormal.x;
			normals[(thisVertices.size() + i) * 3 + 1] = otherNormal.y;
			normals[(thisVertices.size() + i) * 3 + 2] = otherNormal.z;

			
			textureCoords[i * 2] = thisVert.x / textureWidth;
			textureCoords[i * 2 + 1] = thisVert.z / textureDepth;
			
			textureCoords[i * 2] = otherVert.x / textureWidth;
			textureCoords[i * 2 + 1] = otherVert.z / textureDepth;

			
			textureMap.textureInfluences(thisVert.x, thisVert.y, thisVert.z, texStrengthsBuffer);
			textureInfluences[i * 3] = texStrengthsBuffer[0];
			textureInfluences[i * 3 + 1] = texStrengthsBuffer[1];
			textureInfluences[i * 3 + 2] = texStrengthsBuffer[2];
			
			textureMap.textureInfluences(otherVert.x, otherVert.y, otherVert.z, texStrengthsBuffer);
			textureInfluences[(thisVertices.size() + i) * NUM_TEXTURES] = texStrengthsBuffer[0];
			textureInfluences[(thisVertices.size() + i) * NUM_TEXTURES + 1] = texStrengthsBuffer[1];
			textureInfluences[(thisVertices.size() + i) * NUM_TEXTURES + 2] = texStrengthsBuffer[2];
		}
		
		int pointer = 0;// TODO
		for(int i = 0; i < thisVertices.size() - 1; i++) {
			int topLeft = i;
			int topRight = topLeft + 1;
			int bottomLeft = topLeft + thisVertices.size();
			int bottomRight = bottomLeft + 1;

			indices[pointer++] = topLeft;
			indices[pointer++] = bottomLeft;
			indices[pointer++] = topRight;
			indices[pointer++] = topRight;
			indices[pointer++] = bottomLeft;
			indices[pointer++] = bottomRight;
		}
//		int pointer = 0;
//		for(int i = 0; i < thisVertices.size() - 1; i++) {
//			int topLeft = i;
//			int topRight = topLeft + 1;
//			int bottomLeft = topLeft + thisVertices.size();
//			int bottomRight = bottomLeft + 1;
//
//			indices[pointer++] = topLeft;
//			indices[pointer++] = topRight;
//			indices[pointer++] = bottomLeft;
//			indices[pointer++] = topRight;
//			indices[pointer++] = bottomRight;
//			indices[pointer++] = bottomLeft;
//		}
		
		return new TerrainData(vertices, textureCoords, normals, tangents, indices, textureInfluences);
	}

}
