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
	public HashMap<MobileDevice, FogDevice> calculateAllocations(List<FogDevice> cloudlets, List<MobileDevice> smartThings) {
		System.out.printf("%s: calculateAllocation%n", TAG);
		
		try {
			ILPCalculation ILPcalculator = new ILPCalculation(smartThings, cloudlets);
			cloudletResults = ILPcalculator.solveILP();
		} catch (GRBException e) {
			System.out.printf("%s: calculateAllocations exception: %s%n", TAG, e.getMessage());
			e.printStackTrace();
		}
		
		return cloudletResults;
	}
}