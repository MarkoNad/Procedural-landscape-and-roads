package terrains;

import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import renderEngine.Loader;
import textures.TerrainTexture;
import textures.TerrainTexturePack;

public class Terrain {

	private final float width;
	private final float depth;
	private final float vertsPerMeter;
	
	private final float xTiles; // how many times the texture will be repeated in x direction
	private final float zTiles;

	private float xUpperLeft; // true x coordinate of upper left corner
	private float zUpperLeft;
	private Vector3f translation; // how much will be the terrain translated

	private RawModel model;
	
	private TerrainTexturePack texturePack;
	private TerrainTexture blendMap;
	private TextureMap textureMap;
	
	private IHeightGenerator heightGenerator;
	
	public Terrain(Loader loader, TerrainTexturePack texturePack, TerrainTexture blendMap) {
		this(0f, -800f, new Vector3f(), 800f, 800f, 0.2f, 1f, 1f, loader, texturePack, blendMap, new UniformHeightGenerator());
	}
	
	public Terrain(float xUpperLeft, float zUpperLeft, Vector3f position, float width, float depth,
			float vertsPerMeter, float xTiles, float zTiles, Loader loader, TerrainTexturePack texturePack,
			TerrainTexture blendMap, IHeightGenerator heightGenerator) {
		this.xUpperLeft = xUpperLeft;
		this.zUpperLeft = zUpperLeft;
		this.translation = position;
		this.width = width;
		this.depth = depth;
		this.vertsPerMeter = vertsPerMeter;
		this.xTiles = xTiles;
		this.zTiles = zTiles;
		this.texturePack = texturePack;
		this.blendMap = blendMap;
		this.heightGenerator = heightGenerator;
		this.textureMap = new TextureMap(heightGenerator.getMaxHeight());
		this.model = generateTerrain(loader);
	}
	
	public Vector3f getTranslation() {
		return translation;
	}

	public RawModel getModel() {
		return model;
	}

	public TerrainTexturePack getTexturePack() {
		return texturePack;
	}

	public TerrainTexture getBlendMap() {
		return blendMap;
	}

	private RawModel generateTerrain(Loader loader) {
		int xVertices = (int) (width * vertsPerMeter);
		int zVertices = (int) (depth * vertsPerMeter);
		int count = xVertices * zVertices;
		
		float[] vertices = new float[count * 3];
		float[] normals = new float[count * 3];
		float[] textureCoords = new float[count * 2];
		float[] textureInfluences = new float[count * 3]; // two textures per vertex
		int[] indices = new int[6 * (xVertices - 1) * (zVertices - 1)];
		
		int vertexPointer = 0;
		for (int z = 0; z < zVertices; z++) {
			for (int x = 0; x < xVertices; x++) {
				float xcoord = x / (float)(xVertices - 1) * width + xUpperLeft;
				float zcoord = z / (float)(zVertices - 1) * depth + zUpperLeft;
				float height = heightGenerator.getHeight(xcoord, zcoord);
				
				vertices[vertexPointer * 3] = xcoord;
				vertices[vertexPointer * 3 + 1] = height;
				vertices[vertexPointer * 3 + 2] = zcoord;
				
				Vector3f normal = heightGenerator.getNormal(xcoord, zcoord);
				normals[vertexPointer * 3] = normal.x;
				normals[vertexPointer * 3 + 1] = normal.y;
				normals[vertexPointer * 3 + 2] = normal.z;
				
				textureCoords[vertexPointer * 2] = (float) x / ((float) xVertices - 1) * xTiles;
				textureCoords[vertexPointer * 2 + 1] = (float) z / ((float) zVertices - 1) * zTiles;
				
				float[] texStrengths = textureMap.getTextures(height, xcoord, zcoord);
				textureInfluences[vertexPointer * 3] = texStrengths[0];
				textureInfluences[vertexPointer * 3 + 1] = texStrengths[1];
				textureInfluences[vertexPointer * 3 + 2] = texStrengths[2];
				
				vertexPointer++;
			}
		}
		
		int pointer = 0;
		for (int gz = 0; gz < zVertices - 1; gz++) {
			for (int gx = 0; gx < xVertices - 1; gx++) {
				int topLeft = (gz * xVertices) + gx;
				int topRight = topLeft + 1;
				int bottomLeft = ((gz + 1) * xVertices) + gx;
				int bottomRight = bottomLeft + 1;
				
				indices[pointer++] = topLeft;
				indices[pointer++] = bottomLeft;
				indices[pointer++] = topRight;
				indices[pointer++] = topRight;
				indices[pointer++] = bottomLeft;
				indices[pointer++] = bottomRight;
			}
		}
		
		//return loader.loadToVAO(vertices, textureCoords, normals, indices);
//		for(int i = 0; i < textureInfluences.length / 3.0f; i++) {
//			if(textureInfluences[i * 3 + 2] > 0) System.out.println("sand: " + textureInfluences[i * 3] + ", grass: " + textureInfluences[3 * i + 1] + ", snow: " + textureInfluences[3 * i + 2]);
//		}
		return loader.loadToVAO(vertices, textureCoords, normals, indices, textureInfluences);
	}

}
