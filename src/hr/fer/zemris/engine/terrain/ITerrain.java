package hr.fer.zemris.engine.terrain;

import java.util.Optional;

import org.lwjgl.util.vector.Vector3f;

import hr.fer.zemris.engine.model.RawModel;
import hr.fer.zemris.engine.renderer.Loader;
import hr.fer.zemris.engine.texture.TerrainTexture;
import hr.fer.zemris.engine.texture.TerrainTexturePack;

public interface ITerrain {

	public Optional<RawModel> getModel();
	public void setModel(Loader loader);
	public TerrainTexturePack getTexturePack();
	public TerrainTexture getBlendMap();
	public Vector3f getTranslation();
	
}
