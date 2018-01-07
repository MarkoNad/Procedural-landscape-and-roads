package terrains;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import objConverter.Vertex;
import renderEngine.Loader;
import textures.TerrainTexture;
import textures.TerrainTexturePack;

public class Terrain {
	
	/** Every point's color of this terrain is made of 3 textures (3 texture influences)
	 * are needed from the texture map. */
	private static final int NUM_TEXTURES = 3;

	private final float width;
	private final float depth;
	private final float vertsPerMeter;

	private final float xTiles; // how many times the texture will be repeated
								// in x direction
	private final float zTiles;

	private float xUpperLeft; // true x coordinate of upper left corner
	private float zUpperLeft;
	private Vector3f translation; // how much will be the terrain translated

	private RawModel model;

	private TerrainTexturePack texturePack;
	private TerrainTexture blendMap;
	private ITextureMap textureMap;

	private IHeightGenerator heightGenerator;

	public Terrain(Loader loader, TerrainTexturePack texturePack, TerrainTexture blendMap,
			ITextureMap textureMap) {
		this(0f, -800f, new Vector3f(), 800f, 800f, 0.2f, 1f, 1f, loader, texturePack,
				blendMap, new UniformHeightGenerator(), textureMap);
	}

	public Terrain(float xUpperLeft, float zUpperLeft, Vector3f position, float width, float depth,
			float vertsPerMeter, float xTiles, float zTiles, Loader loader,
			TerrainTexturePack texturePack, TerrainTexture blendMap, IHeightGenerator heightGenerator,
			ITextureMap textureMap) {
		if(textureMap.getNumberOfInfluences() != NUM_TEXTURES) {
			throw new IllegalArgumentException("Terrain needs " + NUM_TEXTURES + " texture influences per "
					+ "vertex, provided texture map has " + textureMap.getNumberOfInfluences());
		}
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
		this.textureMap = textureMap;
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
		float[] textureInfluences = new float[count * 3]; // two textures per vertex (3?)
		int[] indices = new int[6 * (xVertices - 1) * (zVertices - 1)];

		int vertexPointer = 0;
		float[] texStrengthsBuffer = new float[NUM_TEXTURES];
		
		for (int z = 0; z < zVertices; z++) {
			for (int x = 0; x < xVertices; x++) {
				float xcoord = x / (float) (xVertices - 1) * width + xUpperLeft;
				float zcoord = z / (float) (zVertices - 1) * depth + zUpperLeft;
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

				textureMap.textureInfluences(xcoord, height, zcoord, texStrengthsBuffer);
				textureInfluences[vertexPointer * 3] = texStrengthsBuffer[0];
				textureInfluences[vertexPointer * 3 + 1] = texStrengthsBuffer[1];
				textureInfluences[vertexPointer * 3 + 2] = texStrengthsBuffer[2];

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

		float[] tangents = calculateTangents(vertices, textureCoords, zVertices, xVertices);

		return loader.loadToVAO(vertices, textureCoords, normals, tangents, indices, textureInfluences);
	}

	private static float[] calculateTangents(float[] vertices, float[] textureCoords, int zVertices, int xVertices) {
		int count = xVertices * zVertices;

		List<Vertex> verticesList = new ArrayList<>();
		List<Vector2f> texturesList = new ArrayList<>();

		for (int i = 0; i < vertices.length / 3; i++) {
			float x = vertices[3 * i];
			float y = vertices[3 * i + 1];
			float z = vertices[3 * i + 2];

			float u = vertices[2 * i];
			float v = vertices[2 * i + 1];

			verticesList.add(new Vertex(i, new Vector3f(x, y, z)));
			texturesList.add(new Vector2f(u, v));
		}

		for (int gz = 0; gz < zVertices - 1; gz++) {
			for (int gx = 0; gx < xVertices - 1; gx++) {
				int topLeftBaseIndex = gz * xVertices + gx;
				int topRightBaseIndex = topLeftBaseIndex + 1;
				int bottomLeftBaseIndex = (gz + 1) * xVertices + gx;
				int bottomRightBaseIndex = bottomLeftBaseIndex + 1;

				Vertex topLeft = verticesList.get(topLeftBaseIndex);
				Vertex topRight = verticesList.get(topRightBaseIndex);
				Vertex bottomLeft = verticesList.get(bottomLeftBaseIndex);
				Vertex bottomRight = verticesList.get(bottomRightBaseIndex);

				Vector2f uvTopLeft = texturesList.get(topLeftBaseIndex);
				Vector2f uvTopRight = texturesList.get(topRightBaseIndex);
				Vector2f uvBottomLeft = texturesList.get(bottomLeftBaseIndex);
				Vector2f uvBottomRight = texturesList.get(bottomRightBaseIndex);

				calculateTangentsForFace(topLeft, bottomLeft, topRight, uvTopLeft, uvBottomLeft, uvTopRight);
				calculateTangentsForFace(topRight, bottomLeft, bottomRight, uvTopRight, uvBottomLeft, uvBottomRight);
			}
		}

		float[] tangentsArray = new float[count * 3];

		for (int i = 0; i < verticesList.size(); i++) {
			Vertex currentVertex = verticesList.get(i);
			Vector3f tangent = currentVertex.getAveragedTangent();

			tangentsArray[i * 3] = tangent.x;
			tangentsArray[i * 3 + 1] = tangent.y;
			tangentsArray[i * 3 + 2] = tangent.z;
		}

		return tangentsArray;
	}

	private static void calculateTangentsForFace(Vertex v0, Vertex v1, Vertex v2, Vector2f uv0, Vector2f uv1,
			Vector2f uv2) {
		Vector3f deltaPos1 = Vector3f.sub(v1.getPosition(), v0.getPosition(), null);
		Vector3f deltaPos2 = Vector3f.sub(v2.getPosition(), v0.getPosition(), null);

		Vector2f deltaUv1 = Vector2f.sub(uv1, uv0, null);
		Vector2f deltaUv2 = Vector2f.sub(uv2, uv0, null);

		float r = 1.0f / (deltaUv1.x * deltaUv2.y - deltaUv1.y * deltaUv2.x);

		deltaPos1.scale(deltaUv2.y);
		deltaPos2.scale(deltaUv1.y);
		Vector3f tangent = Vector3f.sub(deltaPos1, deltaPos2, null);
		tangent.scale(r);

		v0.addTangent(tangent);
		v1.addTangent(tangent);
		v2.addTangent(tangent);
	}

}
