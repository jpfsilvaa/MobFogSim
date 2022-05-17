package org.fog.optimization.facade;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.optimization.OptLogger;

public final class SimClockFacade {
	
	private static String TAG = SimClockFacade.class.getName();
	private static SimClockFacade instance;
	
	private double lastCalculationTime;
	private int interval = 120 * 1000;
	private DeviceFacade dfInstance;
	
	private SimClockFacade() {
		dfInstance = DeviceFacade.getInstance();
	}
	
	public static synchronized SimClockFacade getInstance() {
		if (instance == null) {
			instance = new SimClockFacade();
		}
		
		return instance;
	}
	
	public boolean isIntervalReached() {
//		OptLogger.debug(TAG, "isIntervalReached");
//		OptLogger.debug(TAG, "current clock -> " + CloudSim.clock() + "// lastCalculationTime -> " + lastCalculationTime);
		if (CloudSim.clock() >= lastCalculationTime 
				+ interval) {
			return true;
		} else {
			return false;
		}
	}
	
	public void setLastCalculationTime(double lastCalculationTime) {
		OptLogger.debug(TAG, "setLastCalculationTime -: " + lastCalculationTime);
		this.lastCalculationTime = lastCalculationTime;
	}
	
	public int getInterval() {
		return this.interval;
	}
	
	private void callCalculation() {
//		OptLogger.debug(TAG, "callCalculation");
		
		dfInstance.calculate();
	}
	
	public void checkClock() {
//		OptLogger.debug(TAG, "checkClock");
		
		if (isIntervalReached()) {
			callCalculation();
			setLastCalculationTime(CloudSim.clock());
		}
	}
}
