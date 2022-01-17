package org.fog.optimization;

import java.util.HashMap;
import java.util.List;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.facade.DeviceFacade;
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
	 * @return hashmap of smartThings with the respective calculated cloudlets
	 * 
	 */
	public HashMap<MobileDevice, FogDevice> calculateAllocations(List<FogDevice> cloudlets, List<MobileDevice> smartThings) {
		System.out.printf("%s: calculateAllocation%n", TAG);
		
		DeviceFacade facade = DeviceFacade.getInstance();
		HashMap<MobileDevice, FogDevice> cloudletResults = new HashMap<>();
		
		// TODO: pli
		for (int i = 0; i < smartThings.size(); i++) {
			System.out.printf("%s: smartThing %d for cloudlet %d%n", TAG, smartThings.get(i).getMyId(), cloudlets.get(i).getMyId());
			cloudletResults.put(smartThings.get(i), cloudlets.get(i));
		}
		
		return cloudletResults;
	}
}