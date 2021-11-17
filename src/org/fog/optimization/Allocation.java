package org.fog.optimization;

import java.util.List;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.vmmigration.Migration;

public class Allocation {
	
	private static String TAG = "-------JOAO " + Allocation.class.getName();

	public Allocation() {
		
	}
	
	/**
	 * @author jps
	 * 
	 * @param (WORKING) cloudlets available in distance
	 * @param (WORKING) smartThings requesting resources
	 * @return (WORKING) one cloudlet id 
	 * 
	 * (in the future, must return a set of cloudlet ids for all smartThing requesting)
	 */
	public int calculateAllocations(List<FogDevice> cloudlets, List<MobileDevice> smartThings) {
		System.out.printf("%s: calculateAllocation%n", TAG);
		
		int cloudletResultId = 0;
		
		// smartThings.get(0).getCharacteristics()
		
		if (cloudlets.get(0).getRatePerMips() >= smartThings.get(0).getRatePerMips()) {
			cloudletResultId = cloudlets.get(0).getId();
		}
		
		System.out.printf("%s: calculateAllocation - result: %d%n", TAG, cloudletResultId);
		return cloudletResultId;
	}
}