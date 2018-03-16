package hr.fer.zemris.engine.road;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import hr.fer.zemris.engine.terrain.IHeightMap;

public class TrajectoryPostprocessor {
	
	private static final Logger LOGGER = Logger.getLogger(TrajectoryPostprocessor.class.getName());
	private static final double EPS = 1e-6;
	
	private List<List<Vector3f>> modifierTrajectories;
	private List<Vector3f> trajectory;
	private List<TunnelData> tunnelData;
	
	public TrajectoryPostprocessor(List<Vector3f> initialTrajectory, List<PathPoint3D> pathPoints, 
			IHeightMap heightMap, float minimalTunnelDepth, int endpointOffset, int maskOffset) {
		modifierTrajectories = new ArrayList<>();
		trajectory = new ArrayList<>();
		tunnelData = new ArrayList<>();
		
		LOGGER.info("Trajectory postprocessor determining tunnel endpoints.");
		LOGGER.finer("Trajectory postprocessor received points:");
		pathPoints.forEach(p -> LOGGER.finer(p.toString()));
		
		process(initialTrajectory, pathPoints, heightMap, minimalTunnelDepth, endpointOffset, maskOffset);
		
		tunnelData.forEach(td -> LOGGER.fine(td.toString()));
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
			IHeightMap heightMap, float minimalTunnelDepth, int endpointOffset,
			int maskOffset) {
		List<Vector3f> newModifier = new ArrayList<>();
		
		int pi = 0; // path index
		int ti = 0; // trajectory index
		
		while(true) {
			PathPoint3D curr = pathPoints.get(pi);
			
			// end is reached
			if(pi == pathPoints.size() - 1) {
				if(curr.isExit()) LOGGER.severe("Tunnel exit at the end of trajectory.");
				
				Vector3f itp = initialTrajectory.get(ti); // initial-trajectory point

				trajectory.add(itp);
				newModifier.add(itp);
				modifierTrajectories.add(newModifier);
				
				break;
			}
			
			// process tunnel
			if(curr.isEntrance()) {
				pi = nextExitIndex(pi + 1, pathPoints);
				PathPoint3D exit = pathPoints.get(pi);
				Vector3f itp = initialTrajectory.get(ti);
				Vector3f tunnelBodyEnd = tunnelBodyEnd(initialTrajectory, curr, exit,
						minimalTunnelDepth, heightMap);
				
				boolean entranceExcavationDone = false;
				boolean tunnelBodyDone = false;
				
				TunnelData tunnelDatum = new TunnelData();
				tunnelData.add(tunnelDatum);
				
				while(!samePoint(itp, exit, EPS)) {
					float surfaceHeight = heightMap.getHeight(itp.x, itp.z);
					float depth = surfaceHeight - itp.y;
					
					trajectory.add(itp);
					
					// process entrance
					if(!entranceExcavationDone) {
						newModifier.add(itp);
						
						if(depth + 1e-6 >= minimalTunnelDepth) {
							entranceExcavationDone = true;
							LOGGER.finer("Entrance excavation done.");

							modifierTrajectories.add(newModifier);
							LOGGER.finer("Added modifier.");
							
							newModifier = new ArrayList<>();
							
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
						if(samePoint(itp, tunnelBodyEnd, EPS)) {
							tunnelBodyDone = true;
							LOGGER.fine("Tunnel body done.");
							
							newModifier.add(itp);

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
							
							LOGGER.fine("Added data of tunnel with length: " +
									Vector3f.sub(tunnelDatum.getFirstEndpointLocation(),
											tunnelDatum.getSecondEndpointLocation(), null).length());
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
			pi = nextEntranceOrEndIndex(pi + 1, pathPoints);
			PathPoint3D roadEnd = pathPoints.get(pi);
			
			while(!samePoint(itp, roadEnd, EPS)) {
				trajectory.add(itp);
				newModifier.add(itp);
				itp = initialTrajectory.get(++ti);
			}
			
			LOGGER.fine("Road segment body done.");
		}
	}
	
	private Vector3f tunnelBodyEnd(List<Vector3f> initialTrajectory, PathPoint3D entrance, PathPoint3D exit,
			float minimalTunnelDepth, IHeightMap heightMap) {
		int entranceIndex = -1;
		int exitIndex = -1;
		for(int i = 0; i < initialTrajectory.size(); i++) {
			Vector3f itp = initialTrajectory.get(i);
			if(samePoint(itp, entrance, EPS)) {
				entranceIndex = i;
			}
			if(samePoint(itp, exit, EPS)) {
				exitIndex = i;
			}
		}

		if(exitIndex == -1) LOGGER.severe("Couldn't find tunnel exit in initial trajectory.");
		if(entranceIndex == -1) LOGGER.severe("Couldn't find tunnel entrance in initial trajectory.");
		if(entranceIndex == exitIndex) LOGGER.severe("Tunnel entrance and exit are the same trajectory point.");
		
		for(int i = exitIndex; i >= entranceIndex; i--) {
			Vector3f itp = initialTrajectory.get(i);
			
			float surfaceHeight = heightMap.getHeight(itp.x, itp.z);
			float depth = surfaceHeight - itp.y;
			
			if(depth + 1e-6 >= minimalTunnelDepth) {
				return itp;
			}
		}
		
		LOGGER.severe("Couldn't determine tunnel body end.");
		return initialTrajectory.get(exitIndex);
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

	private int nextExitIndex(int searchStart, List<PathPoint3D> pathPoints) {
		for(int i = searchStart; i < pathPoints.size(); i++) {
			PathPoint3D point = pathPoints.get(i);
			if(point.isExit()) return i;
		}
		
		LOGGER.severe("Tunnel exit not found.");
		return pathPoints.size() - 1;
	}
	
	private int nextEntranceOrEndIndex(int searchStart, List<PathPoint3D> pathPoints) {
		for(int i = searchStart; i < pathPoints.size(); i++) {
			PathPoint3D point = pathPoints.get(i);
			if(point.isEntrance()) return i;
		}

		return pathPoints.size() - 1;
	}

	private boolean samePoint(Vector3f tp, PathPoint3D pp, double eps) {
		Vector3f ppLocation = pp.getLocation();
		return samePoint(tp, ppLocation, eps);
	}
	
	private boolean samePoint(Vector3f tp1, Vector3f tp2, double eps) {
		return Vector3f.sub(tp1, tp2, null).lengthSquared() <= eps * eps;
	}

}
