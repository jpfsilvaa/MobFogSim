package org.fog.optimization.facade;

import org.cloudbus.cloudsim.core.CloudSim;

public final class SimulationClockFacade {
	
	private static String TAG = "-------JOAO " + SimulationClockFacade.class.getName();
	private static SimulationClockFacade instance;
	
	private double lastCalculationTime;
	private final int CALCULATION_TIME_INTERVAL = 15;
	
	private SimulationClockFacade() {}
	
	public static synchronized SimulationClockFacade getInstance() {
		if (instance == null) {
			instance = new SimulationClockFacade();
		}
		
		return instance;
	}
	
	public boolean isCalculationReleased() {
		if (CloudSim.clock() >= lastCalculationTime 
				+ CALCULATION_TIME_INTERVAL) {
			return true;
		} else {
			return false;
		}
	}
	
	public void setLastCalculationTime(double lastCalculationTime) {
		this.lastCalculationTime = lastCalculationTime;
	}
	
}
