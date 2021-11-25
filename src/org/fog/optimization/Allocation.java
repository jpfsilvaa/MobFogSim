package org.fog.optimization;

import java.util.List;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.facade.DeviceCharacsFacade;
import org.fog.vmmigration.Migration;

public class Allocation {
	
	private static String TAG = "-------JOAO " + Allocation.class.getName();

	public Allocation() {
		
	}
	
	/**
	 * @author jps
	 * 
	 * @param cloudlets available in distance
	 * @param smartThings requesting resources
	 * @return (WORKING) one cloudlet id 
	 * 
	 * (in the future, must return a set of cloudlet ids for all smartThing requesting)
	 */
	public int calculateAllocations(List<FogDevice> cloudlets, List<MobileDevice> smartThings) {
		System.out.printf("%s: calculateAllocation%n", TAG);
		
		DeviceCharacsFacade facade = DeviceCharacsFacade.getInstance();
		int cloudletResultId = 0;		
		
		if (facade.getRAM(cloudlets.get(0)) >= facade.getRAM(smartThings.get(0))) {
			System.out.printf("%s cloudlet %d allocated%n", TAG, cloudlets.get(0).getMyId());
			
			cloudletResultId = cloudlets.get(0).getMyId();
		}
		
		System.out.printf("%s: calculateAllocation - result: %d%n", TAG, cloudletResultId);
		return cloudletResultId;
	}
}