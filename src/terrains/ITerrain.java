package terrains;

import java.util.Optional;

import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import renderEngine.Loader;
import textures.TerrainTexture;
import textures.TerrainTexturePack;

public interface ITerrain {

	public Optional<RawModel> getModel();
	public void setModel(Loader loader);
	public TerrainTexturePack getTexturePack();
	public TerrainTexture getBlendMap();
	public Vector3f getTranslation();
	
}
