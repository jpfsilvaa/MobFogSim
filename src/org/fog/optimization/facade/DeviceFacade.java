package org.fog.optimization.facade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.MobileDevice;
import org.fog.optimization.Allocation;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.vmmigration.Migration;

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
	private static DeviceFacade instance;

	public final HashMap<MobileDevice, List<FogDevice>> devicesWaitList = new HashMap<>();
	public HashMap<MobileDevice, FogDevice> calculatedCloudletsToSmartThings = new HashMap<>();
	private Allocation allocator = new Allocation();
	private int ilpMode = 1;
	private SimulationClockFacade simClock;
	
	private DeviceFacade() {
		simClock = SimulationClockFacade.getInstance();
	}
	
	public static synchronized DeviceFacade getInstance() {
		if (instance == null) {
			instance = new DeviceFacade();
		}
		
		return instance;
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
	 * @author jps
	 * 
	 * @param devices waiting to be allocated
	 * @return a merged list of cloudlets available for all devices in the hashmap
	 */
	private List<FogDevice> mergeAvailableCloudlets(){
		OptLogger.debug(TAG, "mergeAvailableCloudlets");
		HashSet<FogDevice> totalCloudlets = new HashSet<>();
		
		for (MobileDevice md : devicesWaitList.keySet()) {
			// it could happens if: the smartThis was disconnected (by distance, migration, or event)
			if (md.getSourceServerCloudlet() != null) {
				totalCloudlets.addAll(Migration.serverClouletsAvailableList(devicesWaitList.get(md), md));
			}
		}
		
		return new ArrayList<FogDevice>(totalCloudlets);
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
		}
	}
	
	public void addSmartThingInWaitList(MobileDevice st, List<FogDevice> cloudlets) {
		OptLogger.debug(TAG, "addSmartThingInWaitList");
		
		if (simClock.isCalculationReleased()) {
			OptLogger.debug(TAG, "calc released");
			
			List<FogDevice> allAvailableCloudlets = mergeAvailableCloudlets();
			List<MobileDevice> reqSmartThings = new ArrayList<> (devicesWaitList.keySet());
			
			simClock.setLastCalculationTime(CloudSim.clock());
			
			if (!allAvailableCloudlets.isEmpty()) {
				ilpMode = 2;
				resetCalculatedCloudlets();
				calculatedCloudletsToSmartThings = allocator.calculateAllocations(allAvailableCloudlets, reqSmartThings, ilpMode);
				DeviceFacade.getInstance().devicesWaitList.clear();
			}
			
			for (MobileDevice reqSmartThing : reqSmartThings) {
				reqSmartThing.setCloudletCalculated(true);
			}
		} else {
			if (!devicesWaitList.containsKey(st)) {
				devicesWaitList.put(st, cloudlets);
				OptLogger.debug(TAG, "added to the list - size:" + devicesWaitList.size());
			} else {
				OptLogger.debug(TAG, "smartThing " + st.getMyId() 
					+ " is already on the list waiting for migration" + devicesWaitList.size());
			}			
		}
	}
	
}
