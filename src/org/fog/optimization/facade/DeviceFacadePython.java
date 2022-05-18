package org.fog.optimization.facade;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

import org.cloudbus.cloudsim.NetworkTopology;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.OptLogger;
import org.fog.vmmobile.AppExample;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 
 * @author jps
 *
 * Class for using the Facade Design Pattern. 
 * It will be useful to separate complex interactions from the simulator to
 * the optimization.
 */
public final class DeviceFacadePython {
	
	private static String TAG = DeviceFacadePython.class.getName();
	private static DeviceFacadePython INSTANCE;

	private HashMap<MobileDevice, FogDevice> calculatedCloudletsToSmartThings = new HashMap<>();
	private Process pythonProcess;
	
	private DeviceFacadePython() {}
	
	public static synchronized DeviceFacadePython getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new DeviceFacadePython();
		}
		
		return INSTANCE;
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
		
		OptLogger.debug(TAG, "1- ACCESSING HASHMAP - SIZE: " + calculatedCloudletsToSmartThings.size());
		return calculatedCloudletsToSmartThings.get(smartThingKey).getMyId();
	}

	
	/**
	 * Reset all the smartThings calculated that did not call for migration again,
	 * and then it did not uses the cloudlet result e must not stay as 'true' 
	 * for 'calculated cloudlet' attribute 
	 */
	private void resetCalculatedCloudlets() {
		OptLogger.debug(TAG, "resetCalculatedCloudlets");
		OptLogger.debug(TAG, "2- ACCESSING HASHMAP - SIZE: " + calculatedCloudletsToSmartThings.size());
		if (calculatedCloudletsToSmartThings.size() > 0) {
			for (MobileDevice st : calculatedCloudletsToSmartThings.keySet()) {
				st.setCloudletCalculated(false);
			}
			calculatedCloudletsToSmartThings.clear();
		}
	}
	
	private MobileDevice getSmartThingById(int myId) {
		for (MobileDevice user : AppExample.getSmartThings()) {
			if (user.getMyId() == myId) {
				return user;
			}
		}
		return null;
	}
	
	private FogDevice getCloudletById(int myId) {
		for (FogDevice cloudlet : AppExample.getServerCloudlets()) {
			if (cloudlet.getMyId() == myId) {
				return cloudlet;
			}
		}
		return null;
	}
	
	private void getProcessResult(InputStream stdout) {
		OptLogger.debug(TAG, "GETTING PROCESS RESULTS");
		
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stdout, StandardCharsets.UTF_8));
		String line;
		
		try {
			while((line = reader.readLine()) != null) {
				OptLogger.debug(TAG, line);
				if (line.contains("allocate")) {
					String[] result = line.split(" ");
					String resultFinal = result[1].replace("[", "");
					resultFinal = resultFinal.replace("]", "");
					String[] allocation = resultFinal.split(",");
					
					OptLogger.debug(TAG, "ALLOCATE VALUE" + Arrays.toString(allocation));
					
					if (getCloudletById(Integer.valueOf(allocation[0])) != null 
							&& getSmartThingById(Integer.valueOf(allocation[1])) != null) {
						OptLogger.debug(TAG, "3- ACCESSING HASHMAP - SIZE: " + calculatedCloudletsToSmartThings.size());
						calculatedCloudletsToSmartThings.put(
								getSmartThingById(Integer.valueOf(allocation[1])), 
								getCloudletById(Integer.valueOf(allocation[0]))
						);
					}
					
				}
			}
		} catch (IOException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void runScript() {
		OptLogger.error(TAG, "CALLING PYTHON PROCESS");
		
		Process process;
		String modelPath = "/home/jps/eclipse-workspace/mestrado"
				+ "/MobFogSim/src/org/fog/optimization/python/";
		String modelCode = "formulation_gurobi_json.py";
		String modelInput = "jsonInput.json";
		int ILPMODE = 1;
		
		try {
			String command = "python3 " + modelPath + modelCode + " " + ILPMODE + " " 
					+ modelPath + modelInput + " " + modelPath;
			OptLogger.debug(TAG, "COMMAND - " + command);
			OptLogger.error(TAG, "TESTE- INICIO");
//			 process = Runtime.getRuntime().exec(command);
//			 pythonProcess = process;
		} catch (Exception e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
		
//		 InputStream stdout = pythonProcess.getInputStream();
		OptLogger.error(TAG, "TESTE- FIM");
//		 getProcessResult(stdout);
	}
	
	private void fillJsonInput() {
		JSONArray vms = new JSONArray();
		for (MobileDevice vm : AppExample.getSmartThings()) {
			vms.put(new JSONObject()
	                  .put("VM_ID", vm.getMyId())
	                  .put("VM_STORAGE", getStorage(vm))
	                  .put("VM_CPU", getMIPS(vm))
	                  .put("VM_RAM", getRAM(vm))
	                  .put("CURR_CLOUDLET", vm.getVmLocalServerCloudlet().getMyId())
			);
		}
		
		JSONArray cloudlets = new JSONArray();
		for (FogDevice cloudlet : AppExample.getServerCloudlets()) {
			cloudlets.put(new JSONObject()
	                  .put("CL_ID", cloudlet.getMyId())
	                  .put("CL_STORAGE", getStorage(cloudlet))
	                  .put("CL_CPU", getMIPS(cloudlet))
	                  .put("CL_RAM", getRAM(cloudlet))
			);
		}
		
		JSONArray delays = new JSONArray();
		// TODO: otimizar essa combinação pra evitar 2 loops
		for (FogDevice srcCloudlet : AppExample.getServerCloudlets()) {
			for (FogDevice destCloudlet : AppExample.getServerCloudlets()) {
				delays.put(new JSONObject()
						.put("SOURCE", srcCloudlet.getMyId())
						.put("DESTINY", destCloudlet.getMyId())
						.put("DELAY", NetworkTopology.getDelay(destCloudlet.getId(), srcCloudlet.getId()))
				);
			}
		}
		
		JSONObject finalJson = new JSONObject()
				.put("VMs", vms)
				.put("Cloudlets", cloudlets)
				.put("DelayBetweenCloudlets", delays);
		
		try {
			String modelPath = "/home/jps/eclipse-workspace/mestrado"
					+ "/MobFogSim/src/org/fog/optimization/python/";
			FileWriter jsonFile = new FileWriter(modelPath + "jsonInput.json");			
			jsonFile.write(finalJson.toString());
			jsonFile.close();
		} catch (IOException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void calculate() {
		OptLogger.debug(TAG, "calculate");

		resetCalculatedCloudlets();	
		fillJsonInput();
		runScript();

		OptLogger.debug(TAG, "4- ACCESSING HASHMAP - SIZE: " + calculatedCloudletsToSmartThings.size());
		for (MobileDevice st : calculatedCloudletsToSmartThings.keySet()) {
			st.setCloudletCalculated(true);
		}
	}
	
}
