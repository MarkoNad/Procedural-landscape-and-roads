package roads;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import entities.Entity;
import models.RawModel;
import models.TexturedModel;
import objConverter.ModelData;
import renderEngine.Loader;
import textures.ModelTexture;

public class TunnelManager {

	private static final Logger LOGGER = Logger.getLogger(TunnelManager.class.getName());
	private static final double EPS = 1e-6;
	
	private final Loader loader;
	private final String innerTunnelTexturePath;
	private final String outerTunnelTexturePath;
	private final String faceTexturePath;
	
	private List<Tunnel> tunnels;
	private List<Entity> allTunnelEntities;
	
	public TunnelManager(Road road, List<TunnelData> tunnelData, int subdivisions, float wallThickness,
			float tunnelInnerTextureDepth, float tunnelOuterTextureDepth, float faceTextureWidth,
			float faceTextureHeight, String innerTunnelTexturePath, String outerTunnelTexturePath,
			String faceTexturePath, Loader loader) {
		this.loader = loader;
		this.innerTunnelTexturePath = innerTunnelTexturePath;
		this.outerTunnelTexturePath = outerTunnelTexturePath;
		this.faceTexturePath = faceTexturePath;
		
		this.tunnels = createTunnels(road, tunnelData, subdivisions, wallThickness, tunnelInnerTextureDepth,
				tunnelOuterTextureDepth, faceTextureWidth, faceTextureHeight);
	}

	// it is expected that tunnel endpoints are ordered the same way as the trajectory
	private List<Tunnel> createTunnels(Road road, List<TunnelData> tunnelData, int subdivisions,
			float wallThickness, float tunnelInnerTextureDepth, float tunnelOuterTextureDepth,
			float faceTextureWidth, float faceTextureHeight) {
		List<Tunnel> tunnels = new ArrayList<>();
		
		List<Vector3f> roadCenterTrajectory = road.getCenterTrajectory();
		List<Vector3f> roadLeftTrajectory = road.getLeftTrajectory();
		List<Vector3f> roadRightTrajectory = road.getRightTrajectory();
		
		for(TunnelData tunnelDatum : tunnelData) {
			Vector3f firstEndpoint = tunnelDatum.getFirstEndpointLocation();
			Vector3f secondEndpoint = tunnelDatum.getSecondEndpointLocation();
			
			List<Vector3f> tunnelLeftTrajectory = new ArrayList<>();
			List<Vector3f> tunnelRightTrajectory = new ArrayList<>();
			
			int trajectoryIndex = 0;
			Vector3f trajectoryPoint = roadCenterTrajectory.get(trajectoryIndex);
			while(!samePoint(firstEndpoint, trajectoryPoint, EPS)) {
				trajectoryIndex++;
				trajectoryPoint = roadCenterTrajectory.get(trajectoryIndex);
			}

			while(!samePoint(secondEndpoint, trajectoryPoint, EPS)) {
				tunnelLeftTrajectory.add(roadLeftTrajectory.get(trajectoryIndex));
				tunnelRightTrajectory.add(roadRightTrajectory.get(trajectoryIndex));
				
				trajectoryIndex++;
				trajectoryPoint = roadCenterTrajectory.get(trajectoryIndex);
			}
			tunnelLeftTrajectory.add(roadLeftTrajectory.get(trajectoryIndex));
			tunnelRightTrajectory.add(roadRightTrajectory.get(trajectoryIndex));
			
			Tunnel tunnel = new Tunnel(tunnelLeftTrajectory, tunnelRightTrajectory, subdivisions,
					wallThickness, tunnelInnerTextureDepth, tunnelOuterTextureDepth, faceTextureWidth,
					faceTextureHeight);
			tunnels.add(tunnel);
		}
		
		return tunnels;
	}

	private boolean samePoint(Vector3f firstEndpoint, Vector3f trajectoryPoint, double eps) {
		return Vector3f.sub(firstEndpoint, trajectoryPoint, null).lengthSquared() <= eps * eps;
	}

	public List<Entity> getAllTunnelEntities() {
		if(allTunnelEntities == null) {
			LOGGER.info("Generating tunnel textured models.");
			allTunnelEntities = generateTunnelEntites(
					tunnels,
					loader,
					innerTunnelTexturePath,
					outerTunnelTexturePath,
					faceTexturePath);
		}
		
		return allTunnelEntities;
	}
	
	private List<Entity> generateTunnelEntites(List<Tunnel> tunnels, Loader loader,
			String innerTunnelTexturePath, String outerTunnelTexturePath, String faceTexturePath) {
		List<Entity> tunnelEntities = new ArrayList<>();
		
		for(Tunnel tunnel : tunnels) {
			ModelData innerRing = tunnel.getInnerRing();
			ModelData outerRing = tunnel.getOuterRing();
			ModelData entranceFace = tunnel.getEntranceFace();
			ModelData exitFace = tunnel.getExitFace();
			
			RawModel innerRingModel = loader.loadToVAO(
					innerRing.getVertices(),
					innerRing.getTextureCoords(),
					innerRing.getNormals(),
					innerRing.getIndices());
			RawModel outerRingModel = loader.loadToVAO(
					outerRing.getVertices(),
					outerRing.getTextureCoords(),
					outerRing.getNormals(),
					outerRing.getIndices());
			RawModel entranceFaceModel = loader.loadToVAO(
					entranceFace.getVertices(),
					entranceFace.getTextureCoords(),
					entranceFace.getNormals(),
					entranceFace.getIndices());
			RawModel exitFaceModel = loader.loadToVAO(
					exitFace.getVertices(),
					exitFace.getTextureCoords(),
					exitFace.getNormals(),
					exitFace.getIndices());
			
			TexturedModel innerRingTM = new TexturedModel(
					innerRingModel,
					new ModelTexture(loader.loadTexture(innerTunnelTexturePath)));
			TexturedModel outerRingTM = new TexturedModel(
					outerRingModel,
					new ModelTexture(loader.loadTexture(outerTunnelTexturePath)));
			TexturedModel entranceFaceTM = new TexturedModel(
					entranceFaceModel,
					new ModelTexture(loader.loadTexture(faceTexturePath)));
			TexturedModel exitFaceTM = new TexturedModel(
					exitFaceModel,
					new ModelTexture(loader.loadTexture(faceTexturePath)));
			
			Entity innerRingEntity = new Entity(innerRingTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
			Entity outerRingEntity = new Entity(outerRingTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
			Entity entranceFaceEntity = new Entity(entranceFaceTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
			Entity exitFaceEntity = new Entity(exitFaceTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
			
			tunnelEntities.add(innerRingEntity);
			tunnelEntities.add(outerRingEntity);
			tunnelEntities.add(entranceFaceEntity);
			tunnelEntities.add(exitFaceEntity);
		}
		
		return tunnelEntities;
	}

}
