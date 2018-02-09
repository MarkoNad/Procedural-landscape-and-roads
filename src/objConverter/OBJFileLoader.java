package objConverter;
 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
 
public class OBJFileLoader {
     
    private static final String RES_LOC = "res/";
    
    public static ModelData loadOBJ(String objFileName) {
    	return loadOBJ(objFileName, false);
    }
 
    /**
     * Loads vertices, textures and normals from .obj file and creates tangents and indices. If 
     * <code>prescaleToUnitCube</code> is <code>true</code>, vertices are scaled so that the 
     * object is centered at the origin inside a unit cube (all coordinates are from interval 
     * [-0.5, 0.5]).
     * 
     * @param objFileName .obj file name, without containing folder and extension
     * @param prescaleToUnitCube if <code>true</code>, vertices are scaled so that the 
     * object is centered at the origin inside a unit cube (all coordinates are from interval 
     * [-0.5, 0.5]).
     * @return the model data
     */
    public static ModelData loadOBJ(String objFileName, boolean prescaleToUnitCube) {
        List<Vertex> vertices = new ArrayList<Vertex>();
        List<Vector2f> textures = new ArrayList<Vector2f>();
        List<Vector3f> normals = new ArrayList<Vector3f>();
        List<Integer> indices = new ArrayList<Integer>();
        
        String path = RES_LOC + objFileName + ".obj";
        List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(path));
		} catch (IOException e) {
			System.out.println("Problem reading file: " + path + ".");
			e.printStackTrace();
			return null;
		}
        List<String> faceLines = new ArrayList<>();
        
        float xmax = 0;
        float xmin = 0;
        float ymax = 0;
        float ymin = 0;
        float zmax = 0;
        float zmin = 0;
        boolean boundariesInitialized = false;
        
        for(String line : lines) {
        	if (line.startsWith("v ")) {
                String[] currentLine = line.split(" ");
                Vector3f vertex = new Vector3f((float) Float.valueOf(currentLine[1]),
                        (float) Float.valueOf(currentLine[2]),
                        (float) Float.valueOf(currentLine[3]));
                Vertex newVertex = new Vertex(vertices.size(), vertex);
                vertices.add(newVertex);
                
                if(!boundariesInitialized) {
                	xmax = vertex.x;
                	xmin = vertex.x;
                	ymax = vertex.y;
                	ymin = vertex.y;
                	zmax = vertex.z;
                	zmin = vertex.z;
                	boundariesInitialized = true;
                }
                
                xmax = Math.max(xmax, vertex.x);
                ymax = Math.max(ymax, vertex.y);
                zmax = Math.max(zmax, vertex.z);
                xmin = Math.min(xmin, vertex.x);
                ymin = Math.min(ymin, vertex.y);
                zmin = Math.min(zmin, vertex.z);
            } else if (line.startsWith("vt ")) {
                String[] currentLine = line.split(" ");
                Vector2f texture = new Vector2f((float) Float.valueOf(currentLine[1]),
                        (float) Float.valueOf(currentLine[2]));
                textures.add(texture);
            } else if (line.startsWith("vn ")) {
                String[] currentLine = line.split(" ");
                Vector3f normal = new Vector3f((float) Float.valueOf(currentLine[1]),
                        (float) Float.valueOf(currentLine[2]),
                        (float) Float.valueOf(currentLine[3]));
                normals.add(normal);
            } else if(line.startsWith("f ")) {
            	faceLines.add(line);
            }
        }
        
        if(prescaleToUnitCube) {
        	scaleModel(vertices, xmax, xmin, ymax, ymin, zmax, zmin);
        }
        
        for(String fLine : faceLines) {
        	String[] currentLine = fLine.split(" ");
        	
            String[] vertex1 = currentLine[1].split("/");
            String[] vertex2 = currentLine[2].split("/");
            String[] vertex3 = currentLine[3].split("/");
            
            Vertex v0 = processVertex(vertex1, vertices, indices);
            Vertex v1 = processVertex(vertex2, vertices, indices);
            Vertex v2 = processVertex(vertex3, vertices, indices);
            
            calculateTangents(v0, v1, v2, textures);
        }
        
        removeUnusedVertices(vertices);
        
        float[] verticesArray = new float[vertices.size() * 3];
        float[] texturesArray = new float[vertices.size() * 2];
        float[] normalsArray = new float[vertices.size() * 3];
        float[] tangentsArray = new float[vertices.size() * 3];
        float furthest = convertDataToArrays(vertices, textures, normals, verticesArray, texturesArray, normalsArray, tangentsArray);
        int[] indicesArray = convertIndicesListToArray(indices);
        
        ModelData data = new ModelData(verticesArray, texturesArray, normalsArray, tangentsArray, indicesArray, furthest);
        return data;
    }

	private static void scaleModel(List<Vertex> vertices, float xmax, float xmin, float ymax,
			float ymin, float zmax, float zmin) {
		final float xLength = xmax - xmin;
		final float yLength = ymax - ymin;
		final float zLength = zmax - zmin;
		
		final float xOffset = xLength / 2.0f;
		final float yOffset = yLength / 2.0f;
		final float zOffset = zLength / 2.0f;
		
		final float maxLength = Math.max(Math.max(xLength, yLength), zLength);
		
		for(Vertex vertex : vertices) {
			Vector3f pos = vertex.getPosition();
			pos.setX((pos.getX() - xOffset) / maxLength);
			pos.setY((pos.getY() - yOffset) / maxLength);
			pos.setZ((pos.getZ() - zOffset) / maxLength);
		}
	}

	private static Vertex processVertex(String[] vertex, List<Vertex> vertices, List<Integer> indices) {
        int index = Integer.parseInt(vertex[0]) - 1;
        Vertex currentVertex = vertices.get(index);
        int textureIndex = Integer.parseInt(vertex[1]) - 1;
        int normalIndex = Integer.parseInt(vertex[2]) - 1;
        if (!currentVertex.isSet()) {
            currentVertex.setTextureIndex(textureIndex);
            currentVertex.setNormalIndex(normalIndex);
            indices.add(index);
            return currentVertex;
        } else {
           return dealWithAlreadyProcessedVertex(currentVertex, textureIndex, normalIndex, indices, vertices);
        }
    }
	
    private static void calculateTangents(Vertex v0, Vertex v1, Vertex v2, List<Vector2f> textures) {
		Vector3f deltaPos1 = Vector3f.sub(v1.getPosition(), v0.getPosition(), null);
		Vector3f deltaPos2 = Vector3f.sub(v2.getPosition(), v0.getPosition(), null);
		
		Vector2f uv0 = textures.get(v0.getTextureIndex());
		Vector2f uv1 = textures.get(v1.getTextureIndex());
		Vector2f uv2 = textures.get(v2.getTextureIndex());
		
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
 
    private static int[] convertIndicesListToArray(List<Integer> indices) {
        int[] indicesArray = new int[indices.size()];
        for (int i = 0; i < indicesArray.length; i++) {
            indicesArray[i] = indices.get(i);
        }
        return indicesArray;
    }
 
    private static float convertDataToArrays(List<Vertex> vertices, List<Vector2f> textures,
            List<Vector3f> normals, float[] verticesArray, float[] texturesArray,
            float[] normalsArray, float[] tangentsArray) {
        float furthestPoint = 0;
        
        for (int i = 0; i < vertices.size(); i++) {
            Vertex currentVertex = vertices.get(i);
            
            if (currentVertex.getLength() > furthestPoint) {
                furthestPoint = currentVertex.getLength();
            }
            
            Vector3f position = currentVertex.getPosition();
            Vector2f textureCoord = textures.get(currentVertex.getTextureIndex());
            Vector3f normalVector = normals.get(currentVertex.getNormalIndex());
            Vector3f tangent = currentVertex.getAveragedTangent();
            
            verticesArray[i * 3] = position.x;
            verticesArray[i * 3 + 1] = position.y;
            verticesArray[i * 3 + 2] = position.z;
            
            texturesArray[i * 2] = textureCoord.x;
            texturesArray[i * 2 + 1] = 1 - textureCoord.y;
            
            normalsArray[i * 3] = normalVector.x;
            normalsArray[i * 3 + 1] = normalVector.y;
            normalsArray[i * 3 + 2] = normalVector.z;
            
            tangentsArray[i * 3] = tangent.x;
            tangentsArray[i * 3 + 1] = tangent.y;
            tangentsArray[i * 3 + 2] = tangent.z;
        }
        
        return furthestPoint;
    }
 
    private static Vertex dealWithAlreadyProcessedVertex(Vertex previousVertex, int newTextureIndex,
            int newNormalIndex, List<Integer> indices, List<Vertex> vertices) {
        if (previousVertex.hasSameTextureAndNormal(newTextureIndex, newNormalIndex)) {
            indices.add(previousVertex.getIndex());
            return previousVertex;
        } else {
            Vertex anotherVertex = previousVertex.getDuplicateVertex();
            if (anotherVertex != null) {
                return dealWithAlreadyProcessedVertex(anotherVertex, newTextureIndex, newNormalIndex,
                        indices, vertices);
            } else {
                Vertex duplicateVertex = new Vertex(vertices.size(), previousVertex.getPosition());
                duplicateVertex.setTextureIndex(newTextureIndex);
                duplicateVertex.setNormalIndex(newNormalIndex);
                previousVertex.setDuplicateVertex(duplicateVertex);
                vertices.add(duplicateVertex);
                indices.add(duplicateVertex.getIndex());
                return duplicateVertex;
            }
 
        }
    }
     
    private static void removeUnusedVertices(List<Vertex> vertices) {
        for(Vertex vertex : vertices){
            if(!vertex.isSet()){
                vertex.setTextureIndex(0);
                vertex.setNormalIndex(0);
            }
        }
    }
 
}