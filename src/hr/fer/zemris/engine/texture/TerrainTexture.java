package hr.fer.zemris.engine.texture;

public class TerrainTexture {

	private int textureID;

	public TerrainTexture(int textureID) {
		this.textureID = textureID;
	}

	public int getTextureID() {
		return textureID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + textureID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TerrainTexture other = (TerrainTexture) obj;
		if (textureID != other.textureID)
			return false;
		return true;
	}
	
}
