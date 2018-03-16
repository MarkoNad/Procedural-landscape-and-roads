package hr.fer.zemris.engine.entity.prefab;

import org.lwjgl.util.vector.Vector3f;

import hr.fer.zemris.engine.entity.Entity;
import hr.fer.zemris.engine.model.TexturedModel;
import hr.fer.zemris.engine.renderer.Loader;
import hr.fer.zemris.engine.texture.ModelTexture;

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
	}

	private Cube(TexturedModel model, Vector3f position, float rotX, float rotY, float rotZ, float scale) {
		super(model, position, rotX, rotY, rotZ, scale);
	}
	
	public Cube(Loader loader) {
		super(new TexturedModel(loader.loadToVAO(vertices, textureCoords, normals, indices), new ModelTexture(loader.loadTexture("cube"))), position, 0f, 0f, 0f, 1f);
	}

}
