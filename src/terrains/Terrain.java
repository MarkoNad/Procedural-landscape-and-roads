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

	private float xUpperLeft;
	private float zUpperLeft;

	private RawModel model;
	
	private TerrainTexturePack texturePack;
	private TerrainTexture blendMap;
	
	private IHeightGenerator heightGenerator;
	
	public Terrain(Loader loader, TerrainTexturePack texturePack, TerrainTexture blendMap) {
		this(0f, -1f, 800f, 800f, 0.2f, loader, texturePack, blendMap, new UniformHeightGenerator());
	}
	
	public Terrain(float xUpperLeft, float zUpperLeft, float width, float depth, float vertsPerMeter, Loader loader,
			TerrainTexturePack texturePack, TerrainTexture blendMap, IHeightGenerator heightGenerator) {
		this.xUpperLeft = xUpperLeft;
		this.zUpperLeft = zUpperLeft;
		this.width = width;
		this.depth = depth;
		this.vertsPerMeter = vertsPerMeter;
		this.texturePack = texturePack;
		this.blendMap = blendMap;
		this.heightGenerator = heightGenerator;
		this.model = generateTerrain(loader);
	}

	public float getX() {
		return xUpperLeft;
	}

	public float getZ() {
		return zUpperLeft;
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
		int[] indices = new int[6 * (xVertices - 1) * (zVertices - 1)];
		
		//float neighbourDistance = 
		
		int vertexPointer = 0;
		for (int z = 0; z < zVertices; z++) {
			for (int x = 0; x < xVertices; x++) {
				float xcoord = x / (float)(xVertices - 1) * width;
				float zcoord = z / (float)(zVertices - 1) * depth;
				
				vertices[vertexPointer * 3] = xcoord;
				vertices[vertexPointer * 3 + 1] = heightGenerator.getHeight(xcoord, zcoord);
				vertices[vertexPointer * 3 + 2] = zcoord;
				
				//Vector3f normal = heightGenerator.getNormal(x, z);
				Vector3f normal = heightGenerator.getNormal(xcoord, zcoord);
				if(Math.abs(normal.x) > 1e-6 || Math.abs(normal.z) > 1e-6) System.out.println(normal);
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
