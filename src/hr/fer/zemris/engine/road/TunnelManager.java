package hr.fer.zemris.engine.road;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import hr.fer.zemris.engine.entity.Entity;
import hr.fer.zemris.engine.model.RawModel;
import hr.fer.zemris.engine.model.TexturedModel;
import hr.fer.zemris.engine.obj.ModelData;
import hr.fer.zemris.engine.renderer.Loader;
import hr.fer.zemris.engine.texture.ModelTexture;

public class TunnelManager {

	private static final Logger LOGGER = Logger.getLogger(TunnelManager.class.getName());
	private static final double EPS = 1e-6;
	
	private final Loader loader;
	private final String innerTunnelTexturePath;
	private final String outerTunnelTexturePath;
	private final String faceTexturePath;
	private final String maskTexturePath;
	
	private List<Tunnel> tunnels;
	private List<Entity> allTunnelEntities;
	
	public TunnelManager(Road road, List<TunnelData> tunnelData, int subdivisions, float wallThickness,
			float tunnelInnerTextureDepth, float tunnelOuterTextureDepth, float faceTextureWidth,
			float faceTextureHeight, float maskTextureWidth, float maskTextureHeight,
			String innerTunnelTexturePath, String outerTunnelTexturePath, String faceTexturePath,
			String maskTexturePath, Loader loader) {
		this.loader = loader;
		this.innerTunnelTexturePath = innerTunnelTexturePath;
		this.outerTunnelTexturePath = outerTunnelTexturePath;
		this.faceTexturePath = faceTexturePath;
		this.maskTexturePath = maskTexturePath;
		
		LOGGER.info("Tunnel manager generating tunnel meshes.");
		
		this.tunnels = createTunnels(road, tunnelData, subdivisions, wallThickness, tunnelInnerTextureDepth,
				tunnelOuterTextureDepth, faceTextureWidth, faceTextureHeight, maskTextureWidth,
				maskTextureHeight);
	}

	// it is expected that tunnel endpoints are ordered the same way as the trajectory
	private List<Tunnel> createTunnels(Road road, List<TunnelData> tunnelData, int subdivisions,
			float wallThickness, float tunnelInnerTextureDepth, float tunnelOuterTextureDepth,
			float faceTextureWidth, float faceTextureHeight, float maskTextureWidth, 
			float maskTextureHeight) {
		List<Tunnel> tunnels = new ArrayList<>();
		
		List<Vector3f> roadCenterTrajectory = road.getCenterTrajectory();
		List<Vector3f> roadLeftTrajectory = road.getLeftTrajectory();
		List<Vector3f> roadRightTrajectory = road.getRightTrajectory();
		
		for(TunnelData tunnelDatum : tunnelData) {
			Vector3f firstEndpoint = tunnelDatum.getFirstEndpointLocation();
			Vector3f secondEndpoint = tunnelDatum.getSecondEndpointLocation();
			
			LOGGER.finer("First endpoint: " + firstEndpoint);
			LOGGER.finer("Second endpoint: " + secondEndpoint);
			LOGGER.finer("Tunnel datum: " + tunnelDatum);
			
			if(firstEndpoint == null || secondEndpoint == null) {
				LOGGER.severe("Null endpoints: " + firstEndpoint + ", " + secondEndpoint);
				continue;
			}
			
			List<Vector3f> tunnelLeftTrajectory = new ArrayList<>();
			List<Vector3f> tunnelRightTrajectory = new ArrayList<>();
			List<Vector3f> tunnelCenterTrajectory = new ArrayList<>();
			
			int trajectoryIndex = 0;
			Vector3f trajectoryPoint = roadCenterTrajectory.get(trajectoryIndex);
			while(!samePoint(firstEndpoint, trajectoryPoint, EPS)) {
				trajectoryIndex++;
				trajectoryPoint = roadCenterTrajectory.get(trajectoryIndex);
			}

			while(!samePoint(secondEndpoint, trajectoryPoint, EPS)) {
				tunnelLeftTrajectory.add(roadLeftTrajectory.get(trajectoryIndex));
				tunnelRightTrajectory.add(roadRightTrajectory.get(trajectoryIndex));
				tunnelCenterTrajectory.add(roadCenterTrajectory.get(trajectoryIndex));
				
				trajectoryIndex++;
				trajectoryPoint = roadCenterTrajectory.get(trajectoryIndex);
			}
			tunnelLeftTrajectory.add(roadLeftTrajectory.get(trajectoryIndex));
			tunnelRightTrajectory.add(roadRightTrajectory.get(trajectoryIndex));
			tunnelCenterTrajectory.add(roadCenterTrajectory.get(trajectoryIndex));
			
			Vector3f entranceMaskLocation = tunnelDatum.getFirstEndpointMask();
			Vector3f exitMaskLocation = tunnelDatum.getSecondEndpointMask();
			
			Tunnel tunnel = new Tunnel(tunnelLeftTrajectory, tunnelRightTrajectory,
					tunnelCenterTrajectory, subdivisions, wallThickness, entranceMaskLocation,
					exitMaskLocation, tunnelInnerTextureDepth, tunnelOuterTextureDepth,
					faceTextureWidth, faceTextureHeight, maskTextureWidth, maskTextureHeight);
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
					faceTexturePath,
					maskTexturePath);
		}
		
		return allTunnelEntities;
	}
	
	private List<Entity> generateTunnelEntites(List<Tunnel> tunnels, Loader loader,
			String innerTunnelTexturePath, String outerTunnelTexturePath, String faceTexturePath,
			String maskTexturePath) {
		List<Entity> tunnelEntities = new ArrayList<>();
		
		for(Tunnel tunnel : tunnels) {
			ModelData innerRing = tunnel.getInnerRing();
			ModelData outerRing = tunnel.getOuterRing();
			ModelData entranceFace = tunnel.getEntranceFace();
			ModelData exitFace = tunnel.getExitFace();
			ModelData entranceMask = tunnel.getEntranceMask();
			ModelData exitMask = tunnel.getExitMask();
			
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
			RawModel entranceMaskModel = loader.loadToVAO(
					entranceMask.getVertices(),
					entranceMask.getTextureCoords(),
					entranceMask.getNormals(),
					entranceMask.getIndices());
			RawModel exitMaskModel = loader.loadToVAO(
					exitMask.getVertices(),
					exitMask.getTextureCoords(),
					exitMask.getNormals(),
					exitMask.getIndices());
			
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
			TexturedModel entranceMaskTM = new TexturedModel(
					entranceMaskModel,
					new ModelTexture(loader.loadTexture(maskTexturePath)));
			TexturedModel exitMaskTM = new TexturedModel(
					exitMaskModel,
					new ModelTexture(loader.loadTexture(maskTexturePath)));
			
			Entity innerRingEntity = new Entity(innerRingTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
			Entity outerRingEntity = new Entity(outerRingTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
			Entity entranceFaceEntity = new Entity(entranceFaceTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
			Entity exitFaceEntity = new Entity(exitFaceTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
			Entity entranceMaskEntity = new Entity(entranceMaskTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
			Entity exitMaskEntity = new Entity(exitMaskTM, new Vector3f(0f, 0f, 0f), 0f, 0f, 0f, 1f);
			
			tunnelEntities.add(innerRingEntity);
			tunnelEntities.add(outerRingEntity);
			tunnelEntities.add(entranceFaceEntity);
			tunnelEntities.add(exitFaceEntity);
			tunnelEntities.add(entranceMaskEntity);
			tunnelEntities.add(exitMaskEntity);
		}
		
		return tunnelEntities;
	}

}
