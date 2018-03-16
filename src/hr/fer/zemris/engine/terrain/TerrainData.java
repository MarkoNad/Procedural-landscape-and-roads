package hr.fer.zemris.engine.terrain;
 
public class TerrainData {
 
    private float[] vertices;
    private float[] textureCoords;
    private float[] normals;
    private float[] tangents;
    private int[] indices;
    private float[] textureInfluences;
 
    public TerrainData(float[] vertices, float[] textureCoords, float[] normals,
    		float tangents[], int[] indices, float[] textureInfluences) {
        this.vertices = vertices;
        this.textureCoords = textureCoords;
        this.normals = normals;
        this.tangents = tangents;
        this.indices = indices;
        this.textureInfluences = textureInfluences;
    }
 
    public float[] getVertices() {
        return vertices;
    }
 
    public float[] getTextureCoords() {
        return textureCoords;
    }
 
    public float[] getNormals() {
        return normals;
    }
    
    public float[] getTangents() {
		return tangents;
	}
 
    public int[] getIndices() {
        return indices;
    }
 
    public float[] getTextureInfluences() {
		return textureInfluences;
	}
 
}