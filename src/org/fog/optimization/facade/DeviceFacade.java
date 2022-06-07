package org.fog.optimization.facade;

import java.util.HashMap;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.ILPCalculation;
import org.fog.optimization.ILPCalculationVCG;
import org.fog.optimization.OptLogger;
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
	private static DeviceFacade INSTANCE;

	public HashMap<MobileDevice, FogDevice> calculatedCloudletsToSmartThings = new HashMap<>();
	private int ilpMode = 1;
//	private Process pythonProcess;
	
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
	
	private MobileDevice getSmartThingById(int myId) {
		for (MobileDevice user : AppExample.getSmartThings()) {
			if (user.getMyId() == myId) {
				return user;
			}
		}
		return null;
	}
	
	private FogDevice getCloudletById(int myId) {
		for (FogDevice cloudlet : AppExample.getServerCloudlets()) {
			if (cloudlet.getMyId() == myId) {
				return cloudlet;
			}
		}
		return null;
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
