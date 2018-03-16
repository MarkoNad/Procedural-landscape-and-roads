package hr.fer.zemris.engine.renderer;

import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;

import hr.fer.zemris.engine.entity.Camera;
import hr.fer.zemris.engine.entity.Entity;
import hr.fer.zemris.engine.entity.Light;
import hr.fer.zemris.engine.model.RawModel;
import hr.fer.zemris.engine.model.TexturedModel;
import hr.fer.zemris.engine.shader.NormalMappingShader;
import hr.fer.zemris.engine.texture.ModelTexture;
import hr.fer.zemris.engine.util.MatrixUtils;

public class NormalMappingRenderer {
	
	private NormalMappingShader shader;
	
	public NormalMappingRenderer(NormalMappingShader shader, Matrix4f projectionMatrix) {
		this.shader = shader;
		
		shader.start();
		shader.loadProjectionMatrix(projectionMatrix);
		shader.connectTextureUnits();
		shader.stop();
	}
	
	public void render(Map<TexturedModel, List<Entity>> entities, Light light, Camera camera) {
		prepare(light, camera);
		
		for(TexturedModel model : entities.keySet()) {
			prepareTexturedModel(model);
			
			List<Entity> batch = entities.get(model);
			for(Entity entity : batch) {
				prepareInstance(entity);
				GL11.glDrawElements(GL11.GL_TRIANGLES,
									model.getRawModel().getVertexCount(),
									GL11.GL_UNSIGNED_INT,
									0);
			}
			
			unbindTexturedModel();
		}
	}
	
	private void prepareTexturedModel(TexturedModel model) {
		RawModel rawModel = model.getRawModel();
		
		GL30.glBindVertexArray(rawModel.getVaoID());
		
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL20.glEnableVertexAttribArray(2);
		GL20.glEnableVertexAttribArray(3);
		
		ModelTexture texture = model.getTexture();
		if(texture.hasTransparency()) MasterRenderer.disableCulling();
		shader.loadShineVariables(texture.getShineDamper(), texture.getReflectivity());
		shader.loadFakeLightingVariable(texture.usesFakeLighting());
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexture().getTextureID());
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexture().getNormalMapID());
	}
	
	private void unbindTexturedModel() {
		MasterRenderer.enableCulling();
		
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL20.glDisableVertexAttribArray(2);
		GL20.glDisableVertexAttribArray(3);
		
		GL30.glBindVertexArray(0);
	}
	
	private void prepareInstance(Entity entity) {
		Matrix4f transformationMatrix = MatrixUtils.createTransformationMatrix(entity.getPosition(),
				entity.getRotX(), entity.getRotY(), entity.getRotZ(), entity.getScale());
		shader.loadTransformationMatrix(transformationMatrix);
	}
	
	private void prepare(Light light, Camera camera) {
		shader.loadSkyColour(MasterRenderer.RED, MasterRenderer.GREEN, MasterRenderer.BLUE);
		Matrix4f viewMatrix = MatrixUtils.createViewMatrix(camera);
		
		shader.loadLight(light, viewMatrix);
		shader.loadViewMatrix(viewMatrix);
	}
	
}
