package org.fog.vmmigration;

import java.util.List;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Distances;
import org.fog.optimization.OptLogger;

public class LatencyByDistance {

	private static double latencyConnection(FogDevice sc1, FogDevice sc2) {
		double distance = Distances.checkDistance(sc1.getCoord(), sc2.getCoord());
		double latency = distance * 0.01;
		return latency;
	}

	public static double latencyConnection(FogDevice sc, MobileDevice st) {
		
		double distance = Distances.checkDistance(st.getCoord(), st.getSourceAp().getCoord());
		double latency1 = latencyConnection(st.getSourceAp().getServerCloudlet(),
			st.getVmLocalServerCloudlet());// bw source and vmLocal
		double latency2 = distance * 0.001; 
		return latency1 + latency2;
	}
	
	public static double cloudletsLatencyConnection(FogDevice src, FogDevice dst) {
		double distance = Distances.checkDistance(src.getCoord(), dst.getCoord());
		double latency1 = latencyConnection(src, dst);// bw source and vmLocal
		double latency2 = distance * 0.001; 
		return latency1 + latency2;
	}
	
	public static double getMaximumLatency(List<MobileDevice> devices, List<FogDevice> cloudlets) {
		double maxValue = Double.MIN_VALUE;
		for (MobileDevice d : devices) {
			for (FogDevice f : cloudlets) {
				if (latencyConnection(f, d) > maxValue) {
					maxValue = latencyConnection(f, d);
				}
			}
		}
		
		return maxValue;
	}
	
	public static double getMinimumLatency(List<MobileDevice> devices, List<FogDevice> cloudlets) {
		double minValue = Double.MAX_VALUE;
		for (MobileDevice d : devices) {
			for (FogDevice f : cloudlets) {
				if (latencyConnection(f, d) < minValue) {
					minValue = latencyConnection(f, d);
				}
			}
		}
		
		return minValue;
	}
}
