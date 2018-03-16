package hr.fer.zemris.engine.renderer;

import java.util.List;
import java.util.Optional;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;

import hr.fer.zemris.engine.model.RawModel;
import hr.fer.zemris.engine.shader.TerrainShader;
import hr.fer.zemris.engine.terrain.ITerrain;
import hr.fer.zemris.engine.texture.TerrainTexturePack;
import hr.fer.zemris.engine.util.MatrixUtils;

public class TerrainRenderer {
	
	private TerrainShader shader;
	
	public TerrainRenderer(TerrainShader shader, Matrix4f projectionMatrix) {
		this.shader = shader;
		
		shader.start();
		shader.loadProjectionMatrix(projectionMatrix);
		shader.connectTextureUnits();
		shader.stop();
	}
	
	public void render(List<ITerrain> terrains) {
		for(ITerrain terrain : terrains) {
			prepareTerrain(terrain);
			loadModelMatrix(terrain);
			
			GL11.glDrawElements(GL11.GL_TRIANGLES, terrain.getModel().get().getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
			
			unbindTexturedModel();
		}
	}
	
	private void prepareTerrain(ITerrain terrain) {
		Optional<RawModel> rawModel = terrain.getModel();
		if(!rawModel.isPresent()) {
			throw new IllegalArgumentException("Terrain raw model was not set.");
		}
		
		GL30.glBindVertexArray(rawModel.get().getVaoID());
		
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL20.glEnableVertexAttribArray(2);
		GL20.glEnableVertexAttribArray(3);
		
		bindTextures(terrain);
		shader.loadShineVariables(1, 0);
	}
	
	private void bindTextures(ITerrain terrain) {
		TerrainTexturePack texturePack = terrain.getTexturePack();
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texturePack.getBackgroundTexture().getTextureID());
		
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texturePack.getrTexture().getTextureID());
		
		GL13.glActiveTexture(GL13.GL_TEXTURE2);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texturePack.getgTexture().getTextureID());
		
		GL13.glActiveTexture(GL13.GL_TEXTURE3);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texturePack.getbTexture().getTextureID());
		
		GL13.glActiveTexture(GL13.GL_TEXTURE4);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, terrain.getBlendMap().getTextureID());
	}
	
	private void unbindTexturedModel() {
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL20.glDisableVertexAttribArray(2);
		GL20.glDisableVertexAttribArray(3);
		
		GL30.glBindVertexArray(0);
	}
	
	private void loadModelMatrix(ITerrain terrain) {
		Matrix4f transformationMatrix = MatrixUtils.createTransformationMatrix(terrain.getTranslation(), 0, 0, 0, 1);
		shader.loadTransformationMatrix(transformationMatrix);
	}

}
