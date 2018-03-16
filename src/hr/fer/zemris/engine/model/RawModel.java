package hr.fer.zemris.engine.model;

public class RawModel {

	private int vaoID;
	private int vertexCount;

	public RawModel(int vaoID, int vertexCount) {
		this.vaoID = vaoID;
		this.vertexCount = vertexCount;
	}

	public int getVaoID() {
		return vaoID;
	}
	
	public int getVertexCount() {
		return vertexCount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + vaoID;
		result = prime * result + vertexCount;
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
		RawModel other = (RawModel) obj;
		if (vaoID != other.vaoID)
			return false;
		if (vertexCount != other.vertexCount)
			return false;
		return true;
	}

}
