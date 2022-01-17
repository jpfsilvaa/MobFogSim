package org.fog.optimization.facade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.Allocation;
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
	
	private static String TAG = "-------JOAO " + DeviceFacade.class.getName();
	
	private static DeviceFacade instance;
	
	//	Wait list for devices which wants to migrate do another Cloudlet
	public final HashMap<MobileDevice, List<FogDevice>> devicesWaitList = new HashMap<>();
	public HashMap<MobileDevice, FogDevice> calculatedCloudletsToSmartThings = new HashMap<>();
	private Allocation allocator = new Allocation();
//	public boolean cloudletsCalculated = false;
	
	public static DeviceFacade getInstance() {
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
			return device.getHost().getTotalMips();
		}
	}
	
	public int getRAM(FogDevice device) {
		if (device instanceof MobileDevice) {
			MobileDevice smartThing = (MobileDevice) device;
			return smartThing.getVmMobileDevice().getRam();
		} else {
			return device.getHost().getRam();
		}
	}
	
	public int getCalculatedCloudlet(MobileDevice smartThingKey) {
		System.out.printf("%s: getCalculatedCloudlet%n", TAG);
		
		return calculatedCloudletsToSmartThings.get(smartThingKey).getMyId();
	}
	
	/**
	 * @author jps
	 * 
	 * @param devices waiting to be allocated
	 * @return a merged list of cloudlets available for all devices in the hashmap
	 */
	private List<FogDevice> mergeAvailableCloudlets(HashMap<MobileDevice, List<FogDevice>> devices){
		System.out.printf("%s: mergeAvailableCloudlets%n", TAG);
		HashSet<FogDevice> totalCloudlets = new HashSet<>();
		
		for (MobileDevice md : devices.keySet()) {
			totalCloudlets.addAll(Migration.serverClouletsAvailableList(devices.get(md), md));
		}
		
		return new ArrayList<FogDevice>(totalCloudlets);
	}
	
	public void addSmartThingInWaitList(MobileDevice st, List<FogDevice> cloudlets) {
		System.out.printf("%s: addSmartThingInWaitList%n", TAG);
		
		// Accumulate requisitions. When it gets REQUEST_LIMITS, then the method for calculating will be called.
		int REQUESTS_LIMIT = 2;
		
		if (devicesWaitList.size() < REQUESTS_LIMIT) {
			if (!devicesWaitList.containsKey(st)) {
				devicesWaitList.put(st, cloudlets);
				System.out.printf("%s: added to the list - size: %d %n", TAG, devicesWaitList.size());
			} else {
				System.out.printf("%s: smartThing %d is already on the list waiting for migration%n", TAG, st.getMyId());
			}
		} else {
			System.out.printf("%s: filled list%n", TAG);
			
			List<FogDevice> allAvailableCloudlets = mergeAvailableCloudlets(devicesWaitList);
			List<MobileDevice> reqSmartThings = new ArrayList<> (devicesWaitList.keySet());
			
			if (!allAvailableCloudlets.isEmpty()) {
				calculatedCloudletsToSmartThings = allocator.calculateAllocations(allAvailableCloudlets, reqSmartThings);
				DeviceFacade.getInstance().devicesWaitList.clear();
			}
			
			for (MobileDevice reqSmartThing : reqSmartThings) {
				reqSmartThing.setCloudletCalculated(true);
				System.out.printf("%s: cloudlet calculated for smart thing : %d%n", TAG, reqSmartThing.getMyId());
			}
			
		}
	}
	
}
