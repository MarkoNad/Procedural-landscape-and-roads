package roads;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import terrains.IHeightGenerator;

public class TrajectoryPostprocessor {
	
	private static final Logger LOGGER = Logger.getLogger(TrajectoryPostprocessor.class.getName());
	private static final double EPS = 1e-6;
	
	private List<List<Vector3f>> modifierTrajectories;
	private List<Vector3f> trajectory;
	private List<TunnelData> tunnelData;
	
	public TrajectoryPostprocessor(List<Vector3f> initialTrajectory, List<PathPoint3D> pathPoints, 
			IHeightGenerator heightMap, float minimalTunnelDepth, int endpointOffset, int maskOffset) {
		modifierTrajectories = new ArrayList<>();
		trajectory = new ArrayList<>();
		tunnelData = new ArrayList<>();
		
		process(initialTrajectory, pathPoints, heightMap, minimalTunnelDepth, endpointOffset, maskOffset);
	}

	public List<Vector3f> getCorrectedTrajectory() {
		return trajectory;
	}
	
	public List<List<Vector3f>> getModifierTrajectories() {
		return modifierTrajectories;
	}
	
	public List<TunnelData> getTunnelsData() {
		return tunnelData;
	}
	
	private void process(List<Vector3f> initialTrajectory, List<PathPoint3D> pathPoints, 
			IHeightGenerator heightMap, float minimalTunnelDepth, int endpointOffset,
			int maskOffset) {
		List<Vector3f> newModifier = new ArrayList<>();
		
		int pi = 0; // path index
		int ti = 0; // trajectory index
		
		while(true) {
			PathPoint3D curr = pathPoints.get(pi);
			pi = nextEndpointOrEndIndex(pi + 1, pathPoints);
			PathPoint3D next = pathPoints.get(pi);
			
			// end is reached
			if(curr.equals(next)) {
				if(curr.isTunnelEndpoint()) LOGGER.severe("Tunnel endpoint at end of trajectory.");
				
				Vector3f itp = initialTrajectory.get(ti); // initial-trajectory point
				
				float surfaceHeight = heightMap.getHeight(itp.x, itp.z);
				Vector3f correctedTP = new Vector3f(itp.x, surfaceHeight, itp.z);
				
				trajectory.add(correctedTP);
				newModifier.add(correctedTP);
				modifierTrajectories.add(newModifier);
				
				break;
			}
			
			// process tunnel
			if(curr.isTunnelEndpoint() && next.isTunnelEndpoint()) {
				Vector3f itp = initialTrajectory.get(ti);
				
				boolean entranceExcavationDone = false;
				boolean tunnelBodyDone = false;
				
				TunnelData tunnelDatum = new TunnelData();
				tunnelData.add(tunnelDatum);
				
				while(!samePoint(itp, next, EPS)) {
					float surfaceHeight = heightMap.getHeight(itp.x, itp.z);
					float depth = surfaceHeight - itp.y;
					
					trajectory.add(itp);
					
					// process entrance
					if(!entranceExcavationDone) {
						newModifier.add(itp);
						
						if(depth > minimalTunnelDepth) {
							entranceExcavationDone = true;
							LOGGER.finer("Entrance excavation done.");

							modifierTrajectories.add(newModifier);
							LOGGER.finer("Added modifier.");
							
							newModifier = new ArrayList<>();

//							Vector3f entranceDirection = determineDirection(initialTrajectory, ti, true);
//							tunnelDatum.setFirstEndpointLocation(itp);
//							tunnelDatum.setFirstEndpointOrientation(entranceDirection);
							
							int startIndex = ti - endpointOffset;
							if(startIndex < 0) startIndex = 0;
							Vector3f entranceLocation = initialTrajectory.get(startIndex);
							Vector3f entranceDirection = determineDirection(initialTrajectory, startIndex, true);
							
							int maskIndex = ti - maskOffset;
							if(maskIndex < 0) maskIndex = 0;
							Vector3f maskLocation = initialTrajectory.get(maskIndex);
							
							tunnelDatum.setFirstEndpointLocation(entranceLocation);
							tunnelDatum.setFirstEndpointOrientation(entranceDirection);
							tunnelDatum.setFirstEndpointMask(maskLocation);
						}

						itp = initialTrajectory.get(++ti);
						continue;
					}
					
					// depth is enough here, no excavations - tunnel
					if(entranceExcavationDone && !tunnelBodyDone) {
						if(depth < minimalTunnelDepth) {
							tunnelBodyDone = true;
							LOGGER.fine("Tunnel body done.");
							
							newModifier.add(itp);
							
//							Vector3f exitDirection = determineDirection(initialTrajectory, ti, false);
//							tunnelDatum.setSecondEndpointLocation(itp);
//							tunnelDatum.setSecondEndpointOrientation(exitDirection);
							
							int endIndex = ti + endpointOffset;
							if(endIndex >= initialTrajectory.size()) endIndex = initialTrajectory.size() - 1;
							Vector3f exitLocation = initialTrajectory.get(endIndex);
							
							int maskIndex = ti + maskOffset;
							if(maskIndex >= initialTrajectory.size()) maskIndex = initialTrajectory.size() - 1;
							Vector3f maskLocation = initialTrajectory.get(maskIndex);
							
							Vector3f exitDirection = determineDirection(initialTrajectory, endIndex, false);
							tunnelDatum.setSecondEndpointLocation(exitLocation);
							tunnelDatum.setSecondEndpointOrientation(exitDirection);
							tunnelDatum.setSecondEndpointMask(maskLocation);
						}

						itp = initialTrajectory.get(++ti);
						continue;
					}
					
					// excavate exit
					newModifier.add(itp);
					
					itp = initialTrajectory.get(++ti);
				}
				
				LOGGER.finer("Tunnel completed.");
				continue;
			}
			
			// process road segment
			Vector3f itp = initialTrajectory.get(ti);
			
			while(!samePoint(itp, next, EPS)) {
				float height = heightMap.getHeight(itp.x, itp.z);
				Vector3f tp = new Vector3f(itp.x, height, itp.z);
				
				trajectory.add(tp);
				newModifier.add(tp);
				
				itp = initialTrajectory.get(++ti);
			}
			
			LOGGER.fine("Road segment body done.");
		}
	}
	
	private Vector3f determineDirection(List<Vector3f> trajectory, int trajectoryIndex, boolean isEntrance) {
		Vector3f enpointLoc = trajectory.get(trajectoryIndex);
		Vector3f direction = null;
		
		if(isEntrance) {
			if(trajectoryIndex - 1 > 0) {
				Vector3f previous = trajectory.get(trajectoryIndex - 1);
				direction = Vector3f.sub(previous, enpointLoc, null);
			} else {
				Vector3f next = trajectory.get(trajectoryIndex + 1);
				direction = Vector3f.sub(enpointLoc, next, null);
			}
		} else {
			if(trajectoryIndex + 1 < trajectory.size()) {
				Vector3f next = trajectory.get(trajectoryIndex + 1);
				direction = Vector3f.sub(next, enpointLoc, null);
			} else {
				Vector3f previous = trajectory.get(trajectoryIndex - 1);
				direction = Vector3f.sub(enpointLoc, previous, null);
			}
		}
		
		direction.normalise();
		return direction;
	}

	private int nextEndpointOrEndIndex(int searchStart, List<PathPoint3D> pathPoints) {
		for(int i = searchStart; i < pathPoints.size(); i++) {
			PathPoint3D point = pathPoints.get(i);
			if(point.isTunnelEndpoint()) return i;
		}
		
		return pathPoints.size() - 1;
	}

	private boolean samePoint(Vector3f tp, PathPoint3D pp, double eps) {
		Vector3f ppLocation = pp.getLocation();
		
		return Vector3f.sub(tp, ppLocation, null).lengthSquared() <= eps * eps;
	}

}
