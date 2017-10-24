package textures;

public class TerrainTexturePack {
	
	private TerrainTexture backgroundTexture;
	private TerrainTexture rTexture;
	private TerrainTexture gTexture;
	private TerrainTexture bTexture;
	
	public TerrainTexturePack(TerrainTexture backgroundTexture, TerrainTexture rTexture, TerrainTexture gTexture, TerrainTexture bTexture) {
		this.backgroundTexture = backgroundTexture;
		this.rTexture = rTexture;
		this.gTexture = gTexture;
		this.bTexture = bTexture;
	}

	public TerrainTexture getBackgroundTexture() {
		return backgroundTexture;
	}

	public TerrainTexture getrTexture() {
		return rTexture;
	}

	public TerrainTexture getgTexture() {
		return gTexture;
	}

	public TerrainTexture getbTexture() {
		return bTexture;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bTexture == null) ? 0 : bTexture.hashCode());
		result = prime * result + ((backgroundTexture == null) ? 0 : backgroundTexture.hashCode());
		result = prime * result + ((gTexture == null) ? 0 : gTexture.hashCode());
		result = prime * result + ((rTexture == null) ? 0 : rTexture.hashCode());
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
		TerrainTexturePack other = (TerrainTexturePack) obj;
		if (bTexture == null) {
			if (other.bTexture != null)
				return false;
		} else if (!bTexture.equals(other.bTexture))
			return false;
		if (backgroundTexture == null) {
			if (other.backgroundTexture != null)
				return false;
		} else if (!backgroundTexture.equals(other.backgroundTexture))
			return false;
		if (gTexture == null) {
			if (other.gTexture != null)
				return false;
		} else if (!gTexture.equals(other.gTexture))
			return false;
		if (rTexture == null) {
			if (other.rTexture != null)
				return false;
		} else if (!rTexture.equals(other.rTexture))
			return false;
		return true;
	}

}
