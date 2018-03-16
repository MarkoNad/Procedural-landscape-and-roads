package hr.fer.zemris.engine.obj;
 
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;
 
public class Vertex {
     
    private static final int NO_INDEX = -1;
     
    private Vector3f position;
    private int textureIndex = NO_INDEX;
    private int normalIndex = NO_INDEX;
    private Vertex duplicateVertex = null;
    private int index;
    private float length;
    private List<Vector3f> tangents;
    private Vector3f averagedTangent;
    
    public Vertex(int index, Vector3f position, List<Vector3f> tangents) {
        this.index = index;
        this.position = position;
        this.length = position.length();
        this.tangents = tangents;
    }
    
    public Vertex(int index, Vector3f position){
        this(index, position, new ArrayList<>());
    }
    
    public Vertex(Vector3f position){
        this(NO_INDEX, position);
    }
    
    public void addTangent(Vector3f tangent) {
    	tangents.add(tangent);
    }
    
    public Vertex duplicate(int newIndex) {
    	return new Vertex(newIndex, position, tangents);
    }
    
    private void averageTangents() {
    	if(tangents.isEmpty()) return;
    	
    	averagedTangent = new Vector3f(0.0f, 0.0f, 0.0f);
    	
    	for(Vector3f tangent : tangents) {
    		Vector3f.add(averagedTangent, tangent, averagedTangent);
    	}
    	
    	averagedTangent.normalise();
    }
    
    public Vector3f getAveragedTangent() {
    	if(averagedTangent == null) averageTangents();
		return averagedTangent;
	}
    
    public int getIndex(){
        return index;
    }
     
    public float getLength(){
        return length;
    }
     
    public boolean isSet(){
        return textureIndex!=NO_INDEX && normalIndex!=NO_INDEX;
    }
     
    public boolean hasSameTextureAndNormal(int textureIndexOther,int normalIndexOther){
        return textureIndexOther == textureIndex && normalIndexOther == normalIndex;
    }
     
    public void setTextureIndex(int textureIndex){
        this.textureIndex = textureIndex;
    }
     
    public void setNormalIndex(int normalIndex){
        this.normalIndex = normalIndex;
    }
 
    public Vector3f getPosition() {
        return position;
    }
 
    public int getTextureIndex() {
        return textureIndex;
    }
 
    public int getNormalIndex() {
        return normalIndex;
    }
 
    public Vertex getDuplicateVertex() {
        return duplicateVertex;
    }
 
    public void setDuplicateVertex(Vertex duplicateVertex) {
        this.duplicateVertex = duplicateVertex;
    }
 
}