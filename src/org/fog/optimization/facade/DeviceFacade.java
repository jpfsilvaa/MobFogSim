package org.fog.optimization.facade;

import java.util.HashMap;
import java.util.List;

import org.cloudbus.cloudsim.NetworkTopology;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.ILPCalculation;
import org.fog.optimization.ILPCalculationVCG;
import org.fog.optimization.OptLogger;
import org.fog.vmmigration.LatencyByDistance;
import org.fog.vmmobile.AppExample;

/**
 * 
 * @author jps
 *
 * Class for using the Facade Design Pattern. 
 * It will be useful to separate complex interactions from the simulator to
 * the optimization.
 */
public final class DeviceFacade {
	
	private static String TAG = DeviceFacade.class.getName();
	private static DeviceFacade INSTANCE = null;

	public HashMap<MobileDevice, FogDevice> calculatedCloudletsToSmartThings = new HashMap<>();
	
	private DeviceFacade() {}
	
	public static synchronized DeviceFacade getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new DeviceFacade();
		}
		
		return INSTANCE;
	}
	
	public long getStorage(FogDevice device) {
		if (device instanceof MobileDevice) {
			MobileDevice smartThing = (MobileDevice) device;
			return smartThing.getVmMobileDevice().getSize();
		} else {
			return device.getHost().getStorage();
		}
	}
	
	public double getMIPS(FogDevice device) {
		if (device instanceof MobileDevice) {
			MobileDevice smartThing = (MobileDevice) device;
			return smartThing.getVmMobileDevice().getMips();
		} else {
			return device.getHost().getMaxAvailableMips();
		}
	}
	
	public int getRAM(FogDevice device) {
		if (device instanceof MobileDevice) {
			MobileDevice smartThing = (MobileDevice) device;
			return smartThing.getVmMobileDevice().getRam();
		} else {
			return device.getHost().getRamProvisioner().getAvailableRam();
		}
	}
	
	public int getCalculatedCloudlet(MobileDevice smartThingKey) {
		OptLogger.debug(TAG, "getCalculatedCloudlet");
		
		return calculatedCloudletsToSmartThings.get(smartThingKey).getMyId();
	}
	
	/**
	 * Reset all the smartThings calculated that did not call for migration again,
	 * and then it did not uses the cloudlet result e must not stay as 'true' 
	 * for 'calculated cloudlet' attribute 
	 */
	private void resetCalculatedCloudlets() {
		OptLogger.debug(TAG, "resetCalculatedCloudlets");
		if (calculatedCloudletsToSmartThings.size() > 0) {
			for (MobileDevice st : calculatedCloudletsToSmartThings.keySet()) {
				st.setCloudletCalculated(false);
			}
			calculatedCloudletsToSmartThings.clear();
		}
	}
	
	public void bidGen(List<MobileDevice> devices, List<FogDevice> cloudlets) {
		OptLogger.debug(TAG, "bidGen");
		double minLatency = LatencyByDistance.getMinimumLatency(devices, cloudlets);
		double maxLatency = LatencyByDistance.getMaximumLatency(devices, cloudlets);
		OptLogger.debug(TAG, "MIN LATENCY-> " + minLatency);
		OptLogger.debug(TAG, "MAX LATENCY-> " + maxLatency);
		
		double minLatencyCost = minLatency * devices.get(0).getMonetaryFactor();
		double maxLatencyCost = maxLatency * devices.get(0).getMonetaryFactor();
		OptLogger.debug(TAG, "MIN LATENCY COST-> " + minLatencyCost);
		OptLogger.debug(TAG, "MAX LATENCY COST-> " + maxLatencyCost);

		for (MobileDevice md: devices) {
			double newBid = Math.abs(AppExample.getRand().nextGaussian()) + minLatencyCost;
			if (newBid > maxLatencyCost) 
				newBid = maxLatencyCost;
			OptLogger.debug(TAG, "BID (LATENCY)-> " + newBid);
			md.setBid(newBid);
			OptLogger.debug(TAG, "VCG - Bid value for the user " + md.getMyId() + ": " + md.getBid());
		}
	}
	
	public void calculate() {
		OptLogger.debug(TAG, "calculate");

		resetCalculatedCloudlets();
			
		ILPCalculationVCG calc = new ILPCalculationVCG(
				AppExample.getSmartThings(), AppExample.getServerCloudlets());
		calculatedCloudletsToSmartThings = calc.solveILP();
		calc = null;

		for (MobileDevice st : calculatedCloudletsToSmartThings.keySet()) {
			st.setCloudletCalculated(true);
		}
	}
	
}
