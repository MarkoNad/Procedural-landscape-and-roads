package renderEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import models.RawModel;

@Deprecated
public class OBJLoader {

	public static RawModel loadObjModel(String fileName, Loader loader) {
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get("res/" + fileName + ".obj"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("Couldn't load .obj file.");
			e.printStackTrace();
			return null;
		}
		
		List<Vector3f> vertices = new ArrayList<>();
		List<Vector2f> textures = new ArrayList<>();
		List<Vector3f> normals = new ArrayList<>();
		List<Integer> indices = new ArrayList<>();
		
		for(String line : lines) {
			String[] parts = line.split("\\s+");
			
			if(line.matches("^v\\s+.*$")) {
				Vector3f vertex = new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
				vertices.add(vertex);
			} else if (line.matches("^vt\\s+.*$")) {
				Vector2f texture = new Vector2f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
				textures.add(texture);
			} else if (line.matches("^vn\\s+.*$")) {
				Vector3f normal = new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
				normals.add(normal);
			} else if (line.matches("^f\\s+.*$")) {
				break;
			}
		}
		
		float[] textureArray = new float[vertices.size() * 2];
		float[] normalsArray = new float[vertices.size() * 3];
		
		for(String line : lines) {
			if(!line.matches("^f\\s+.*$")) continue;
			
			String[] lineParts = line.split("\\s+");
			
			String[] vertex1 = lineParts[1].split("/");
			String[] vertex2 = lineParts[2].split("/");
			String[] vertex3 = lineParts[3].split("/");
			
			processVertex(vertex1, indices, textures, normals, textureArray, normalsArray);
			processVertex(vertex2, indices, textures, normals, textureArray, normalsArray);
			processVertex(vertex3, indices, textures, normals, textureArray, normalsArray);
		}
		
		float[] verticesArray = new float[vertices.size() * 3];
		int[] indicesArray = new int[indices.size()];
		
		int index = 0;
		for(Vector3f vertex : vertices) {
			verticesArray[index++] = vertex.x;
			verticesArray[index++] = vertex.y;
			verticesArray[index++] = vertex.z;
		}
		
		for(int i = 0; i < indices.size(); i++) {
			indicesArray[i] = indices.get(i);
		}
		
		return loader.loadToVAO(verticesArray, textureArray, normalsArray, indicesArray);
	}
	
	private static void processVertex(String[] vertex, List<Integer> indices, List<Vector2f> textures, 
			List<Vector3f> normals, float[] textureArray, float[] normalsArray) {
		int vertexPointer = Integer.parseInt(vertex[0]) - 1;
		
		indices.add(vertexPointer);
		
		Vector2f texture = textures.get(Integer.parseInt(vertex[1]) - 1);
		Vector3f normal = normals.get(Integer.parseInt(vertex[2]) - 1);
		
		textureArray[2 * vertexPointer] = texture.x;
		textureArray[2 * vertexPointer + 1] = 1 - texture.y;
		
		normalsArray[3 * vertexPointer] = normal.x;
		normalsArray[3 * vertexPointer + 1] = normal.y;
		normalsArray[3 * vertexPointer + 2] = normal.z;
	}
	
}
