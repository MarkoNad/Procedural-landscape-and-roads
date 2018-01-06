package textures;

public class ModelTexture {
	
	private static final int NO_ID = -1;

	private final int textureID;
	private final int normalMapID;
	
	private float shineDamper = 1;
	private float reflectivity = 0;
	
	private boolean hasTransparency = false;
	private boolean usesFakeLighting = false;

	public ModelTexture(int textureID) {
		this(textureID, NO_ID);
	}
	
	public ModelTexture(int textureID, int normalMapID) {
		this.textureID = textureID;
		this.normalMapID = normalMapID;
	}

	public boolean hasTransparency() {
		return hasTransparency;
	}

	public void setHasTransparency(boolean hasTransparency) {
		this.hasTransparency = hasTransparency;
	}

	public boolean usesFakeLighting() {
		return usesFakeLighting;
	}

	public void setUsesFakeLighting(boolean usesFakeLighting) {
		this.usesFakeLighting = usesFakeLighting;
	}

	public int getTextureID() {
		return textureID;
	}
	
	public int getNormalMapID() {
		return normalMapID;
	}

	public float getShineDamper() {
		return shineDamper;
	}

	public void setShineDamper(float shineDamper) {
		this.shineDamper = shineDamper;
	}

	public float getReflectivity() {
		return reflectivity;
	}

	public void setReflectivity(float reflectivity) {
		this.reflectivity = reflectivity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (hasTransparency ? 1231 : 1237);
		result = prime * result + Float.floatToIntBits(reflectivity);
		result = prime * result + Float.floatToIntBits(shineDamper);
		result = prime * result + textureID;
		result = prime * result + (usesFakeLighting ? 1231 : 1237);
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
		ModelTexture other = (ModelTexture) obj;
		if (hasTransparency != other.hasTransparency)
			return false;
		if (Float.floatToIntBits(reflectivity) != Float.floatToIntBits(other.reflectivity))
			return false;
		if (Float.floatToIntBits(shineDamper) != Float.floatToIntBits(other.shineDamper))
			return false;
		if (textureID != other.textureID)
			return false;
		if (usesFakeLighting != other.usesFakeLighting)
			return false;
		return true;
	}
	
}
