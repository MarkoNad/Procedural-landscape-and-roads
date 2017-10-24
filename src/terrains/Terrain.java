package terrains;

import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import renderEngine.Loader;
import textures.TerrainTexture;
import textures.TerrainTexturePack;

public class Terrain {

	private static final float SIZE = 800;
	
	private int xVertices; // x resolution
	private int zVertices;

	private float x;
	private float z;

	private RawModel model;
	
	private TerrainTexturePack texturePack;
	private TerrainTexture blendMap;
	
	private IHeightGenerator heightGenerator;

//	public Terrain(int gridX, int gridZ, Loader loader, TerrainTexturePack texturePack, 
//			TerrainTexture blendMap, int xVertices, int zVertices) {
//		this(gridX, gridZ, loader, texturePack, blendMap, (x, y) -> 0, xVertices, zVertices);
//	}
	
	public Terrain(int gridX, int gridZ, Loader loader, TerrainTexturePack texturePack, 
			TerrainTexture blendMap, IHeightGenerator heightGenerator, int xVertices, int zVertices) {
		this.texturePack = texturePack;
		this.blendMap = blendMap;
		this.x = gridX * SIZE;
		this.z = gridZ * SIZE;
		this.heightGenerator = heightGenerator;
		this.xVertices = xVertices;
		this.zVertices = zVertices;
		this.model = generateTerrain(loader);
	}

	public float getX() {
		return x;
	}

	public float getZ() {
		return z;
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
		int count = xVertices * zVertices;
		
		float[] vertices = new float[count * 3];
		float[] normals = new float[count * 3];
		float[] textureCoords = new float[count * 2];
		int[] indices = new int[6 * (xVertices - 1) * (zVertices - 1)];
		
		int vertexPointer = 0;
		for (int z = 0; z < zVertices; z++) {
			for (int x = 0; x < xVertices; x++) {
				vertices[vertexPointer * 3] = (float) x / ((float) xVertices - 1) * SIZE;
				vertices[vertexPointer * 3 + 1] = heightGenerator.getHeight(x, z);
				vertices[vertexPointer * 3 + 2] = (float) z / ((float) zVertices - 1) * SIZE;
				
				Vector3f normal = heightGenerator.getNormal(x, z);
				normals[vertexPointer * 3] = normal.x;
				normals[vertexPointer * 3 + 1] = normal.y;
				normals[vertexPointer * 3 + 2] = normal.z;
				
				textureCoords[vertexPointer * 2] = (float) x / ((float) xVertices - 1);
				textureCoords[vertexPointer * 2 + 1] = (float) z / ((float) zVertices - 1);
				
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
		
		return loader.loadToVAO(vertices, textureCoords, normals, indices);
	}

}
