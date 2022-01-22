package org.fog.optimization;

import java.util.HashMap;
import java.util.List;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;

import gurobi.GRBException;

public class Allocation {
	
	private static String TAG = "-------JOAO " + Allocation.class.getName();
	private HashMap<MobileDevice, FogDevice> cloudletResults = new HashMap<>();
	
	/**
	 * @author jps
	 * 
	 * @param cloudlets available in distance
	 * @param smartThings requesting resources
	 * @return hashmap of smartThings with the respective calculated cloudlets
	 * 
	 */
	public HashMap<MobileDevice, FogDevice> calculateAllocations(
			List<FogDevice> cloudlets, 
			List<MobileDevice> smartThings,
			int ilpMode) {
		System.out.printf("%s: ccalculateAllocation%n", TAG);
		
		ILPCalculation ILPcalculator = new ILPCalculation(smartThings, cloudlets, ilpMode);
		cloudletResults = ILPcalculator.solveILP();
		
		return cloudletResults;
	}
}