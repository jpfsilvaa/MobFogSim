package org.fog.optimization;

import java.text.DecimalFormat;

import org.cloudbus.cloudsim.core.CloudSim;

public class OptLogger {
	
	private static DecimalFormat df = new DecimalFormat("#.00");
	private static String TAG = "OPT_LOGGER";

	public static boolean ENABLED = true;

	public static void debug(String className, String msg) {
		if (!ENABLED)
			return;
		
		String msgFormat = ("%s -- %s: %s: %s%n");
		System.out.printf(msgFormat, 
				df.format(CloudSim.clock()), TAG, className, msg);
	}
	
	public static void error(String className, String msg) {
		if (!ENABLED)
			return;
		
		String msgFormat = ("%s -- %s: %s: %s%n");
		System.err.printf(msgFormat, 
				df.format(CloudSim.clock()), TAG, className, msg);
	}
	
}
