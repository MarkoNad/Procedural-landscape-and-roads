package roads;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector3f;

import terrains.IHeightGenerator;

public class TrajectoryPostprocessor {
	
	private static final Logger LOGGER = Logger.getLogger(TrajectoryPostprocessor.class.getName());
	private static final double EPS = 1e-6;
	
	private List<List<Vector3f>> modifierTrajectories = new ArrayList<>();
	private List<Vector3f> trajectory = new ArrayList<>();
	
	public TrajectoryPostprocessor(List<Vector3f> initialTrajectory, List<PathPoint3D> pathPoints, 
			IHeightGenerator heightMap, float minimalTunnelDepth) {
		modifierTrajectories = new ArrayList<>();
		trajectory = new ArrayList<>();
		
		process(initialTrajectory, pathPoints, heightMap, minimalTunnelDepth);
	}

	public List<Vector3f> getCorrectedTrajectory() {
		return trajectory;
	}
	
	public List<List<Vector3f>> getModifierTrajectories() {
		return modifierTrajectories;
	}
	
	private void process(List<Vector3f> initialTrajectory, List<PathPoint3D> pathPoints, 
			IHeightGenerator heightMap, float minimalTunnelDepth) {
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
				
				Vector3f itp = initialTrajectory.get(ti);
				
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
				
				while(!samePoint(itp, next, EPS)) {
					float surfaceHeight = heightMap.getHeight(itp.x, itp.z);
					float depth = surfaceHeight - curr.getLocation().y;
					
					trajectory.add(itp);
					
					// process entrance
					if(!entranceExcavationDone) {
						newModifier.add(itp);
						
						if(depth > minimalTunnelDepth) {
							entranceExcavationDone = true;
							LOGGER.finer("Excavation done.");

							modifierTrajectories.add(newModifier);
							LOGGER.finer("Added modifier.");
							
							newModifier = new ArrayList<>();
						}

						itp = initialTrajectory.get(++ti);
						continue;
					}
					
					// depth is enough here, no excavations - tunnel
					if(entranceExcavationDone && !tunnelBodyDone) {
						if(depth < minimalTunnelDepth) {
							tunnelBodyDone = true;
							LOGGER.fine("Tunnel body done.");
						}

						itp = initialTrajectory.get(++ti);
						continue;
					}
					
					// excavate exit
					newModifier.add(itp);
					
					itp = initialTrajectory.get(++ti);
				}
				
				continue;
			}
			
			// process road segment
			Vector3f itp = initialTrajectory.get(ti); // initial-trajectory point
			
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
