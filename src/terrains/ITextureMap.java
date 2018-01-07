package terrains;

public interface ITextureMap {
	
	/**
	 * Returns an array of texture strengths at given position. Strengths need to sum 
	 * to 1. Size of array returned is equal to the number of texture definitions in 
	 * the implementation.
	 * 
	 * @param xcoord x coordinate, in OpenGL units
	 * @param ycoord y coordinate, in OpenGL units
	 * @param zcoord z coordinate, in OpenGL units
	 * @return array of texture strengths that sum up to 1
	 */
	public float[] textureInfluences(float xcoord, float ycoord, float zcoord);
	public void textureInfluences(float xcoord, float ycoord, float zcoord, float[] buffer);
	
	/**
	 * Number of texture influences returned by this map.
	 * @return number of texture influences returned by this map
	 */
	public int getNumberOfInfluences();

}
