package entities.standard;

import org.lwjgl.util.vector.Vector3f;

import entities.Entity;
import models.TexturedModel;
import renderEngine.Loader;
import textures.ModelTexture;

public class Cube extends Entity {
	
	private static float[] vertices;
	private static float[] textureCoords;
	private static float[] normals;
	private static int[] indices;
	private static Vector3f position = new Vector3f();
	
	static {
		vertices = new float[] {
			// front
			-1.0f, -1.0f,  1.0f,
		     1.0f, -1.0f,  1.0f,
		     1.0f,  1.0f,  1.0f,
		    -1.0f,  1.0f,  1.0f,
		    // back
		    -1.0f, -1.0f, -1.0f,
		     1.0f, -1.0f, -1.0f,
		     1.0f,  1.0f, -1.0f,
		    -1.0f,  1.0f, -1.0f,
		};
		
		textureCoords = new float[] {
			0f, 0f,
			1f, 0f,
			1f, 1f,
			0f, 1f,
			0f, 0f,
			1f, 0f,
			1f, 1f,
			0f, 1f
		};
		
		normals = new float[] {
			0f, 1f, 0f,
			0f, 1f, 0f,
			0f, 1f, 0f,
			0f, 1f, 0f,
			0f, 1f, 0f,
			0f, 1f, 0f,
			0f, 1f, 0f,
			0f, 1f, 0f,
			0f, 1f, 0f,
		};
		
		indices = new int[] {
			// front
			0, 1, 2,
			2, 3, 0,
			// top
			1, 5, 6,
			6, 2, 1,
			// back
			7, 6, 5,
			5, 4, 7,
			// bottom
			4, 0, 3,
			3, 7, 4,
			// left
			4, 5, 1,
			1, 0, 4,
			// right
			3, 2, 6,
			6, 7, 3,
		};
		
//		vertices = new float[] {
//				-1.0f,-1.0f,-1.0f, // triangle 1 : begin
//			    -1.0f,-1.0f, 1.0f,
//			    -1.0f, 1.0f, 1.0f, // triangle 1 : end
//			    1.0f, 1.0f,-1.0f, // triangle 2 : begin
//			    -1.0f,-1.0f,-1.0f,
//			    -1.0f, 1.0f,-1.0f, // triangle 2 : end
//			    1.0f,-1.0f, 1.0f,
//			    -1.0f,-1.0f,-1.0f,
//			    1.0f,-1.0f,-1.0f,
//			    1.0f, 1.0f,-1.0f,
//			    1.0f,-1.0f,-1.0f,
//			    -1.0f,-1.0f,-1.0f,
//			    -1.0f,-1.0f,-1.0f,
//			    -1.0f, 1.0f, 1.0f,
//			    -1.0f, 1.0f,-1.0f,
//			    1.0f,-1.0f, 1.0f,
//			    -1.0f,-1.0f, 1.0f,
//			    -1.0f,-1.0f,-1.0f,
//			    -1.0f, 1.0f, 1.0f,
//			    -1.0f,-1.0f, 1.0f,
//			    1.0f,-1.0f, 1.0f,
//			    1.0f, 1.0f, 1.0f,
//			    1.0f,-1.0f,-1.0f,
//			    1.0f, 1.0f,-1.0f,
//			    1.0f,-1.0f,-1.0f,
//			    1.0f, 1.0f, 1.0f,
//			    1.0f,-1.0f, 1.0f,
//			    1.0f, 1.0f, 1.0f,
//			    1.0f, 1.0f,-1.0f,
//			    -1.0f, 1.0f,-1.0f,
//			    1.0f, 1.0f, 1.0f,
//			    -1.0f, 1.0f,-1.0f,
//			    -1.0f, 1.0f, 1.0f,
//			    1.0f, 1.0f, 1.0f,
//			    -1.0f, 1.0f, 1.0f,
//			    1.0f,-1.0f, 1.0f
//		};
//	36		
//		textureCoords = new float[] {
//				 0.000059f, 1.0f-0.000004f,
//				    0.000103f, 1.0f-0.336048f,
//				    0.335973f, 1.0f-0.335903f,
//				    1.000023f, 1.0f-0.000013f,
//				    0.667979f, 1.0f-0.335851f,
//				    0.999958f, 1.0f-0.336064f,
//				    0.667979f, 1.0f-0.335851f,
//				    0.336024f, 1.0f-0.671877f,
//				    0.667969f, 1.0f-0.671889f,
//				    1.000023f, 1.0f-0.000013f,
//				    0.668104f, 1.0f-0.000013f,
//				    0.667979f, 1.0f-0.335851f,
//				    0.000059f, 1.0f-0.000004f,
//				    0.335973f, 1.0f-0.335903f,
//				    0.336098f, 1.0f-0.000071f,
//				    0.667979f, 1.0f-0.335851f,
//				    0.335973f, 1.0f-0.335903f,
//				    0.336024f, 1.0f-0.671877f,
//				    1.000004f, 1.0f-0.671847f,
//				    0.999958f, 1.0f-0.336064f,
//				    0.667979f, 1.0f-0.335851f,
//				    0.668104f, 1.0f-0.000013f,
//				    0.335973f, 1.0f-0.335903f,
//				    0.667979f, 1.0f-0.335851f,
//				    0.335973f, 1.0f-0.335903f,
//				    0.668104f, 1.0f-0.000013f,
//				    0.336098f, 1.0f-0.000071f,
//				    0.000103f, 1.0f-0.336048f,
//				    0.000004f, 1.0f-0.671870f,
//				    0.336024f, 1.0f-0.671877f,
//				    0.000103f, 1.0f-0.336048f,
//				    0.336024f, 1.0f-0.671877f,
//				    0.335973f, 1.0f-0.335903f,
//				    0.667969f, 1.0f-0.671889f,
//				    1.000004f, 1.0f-0.671847f,
//				    0.667979f, 1.0f-0.335851f
//		};
//		
//		normals = new float[] {
//			0f, 1f, 0f,
//			0f, 1f, 0f,
//			0f, 1f, 0f,
//			0f, 1f, 0f,
//			0f, 1f, 0f,
//			0f, 1f, 0f,
//			0f, 1f, 0f,
//			0f, 1f, 0f,
//			0f, 1f, 0f,
//		};
//		
//		indices = new int[] {
//			// front
//			0, 1, 2,
//			2, 3, 0,
//			// top
//			1, 5, 6,
//			6, 2, 1,
//			// back
//			7, 6, 5,
//			5, 4, 7,
//			// bottom
//			4, 0, 3,
//			3, 7, 4,
//			// left
//			4, 5, 1,
//			1, 0, 4,
//			// right
//			3, 2, 6,
//			6, 7, 3,
//		};
		
	}

	private Cube(TexturedModel model, Vector3f position, float rotX, float rotY, float rotZ, float scale) {
		super(model, position, rotX, rotY, rotZ, scale);
	}
	
	public Cube(Loader loader) {
		super(new TexturedModel(loader.loadToVAO(vertices, textureCoords, normals, indices), new ModelTexture(loader.loadTexture("cube"))), position, 0f, 0f, 0f, 1f);
	}

}
