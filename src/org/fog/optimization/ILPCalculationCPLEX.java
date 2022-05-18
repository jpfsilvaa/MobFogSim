package org.fog.optimization;

import java.util.HashMap;
import java.util.List;

import org.cloudbus.cloudsim.NetworkTopology;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.facade.DeviceFacade;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

public class ILPCalculationCPLEX {
	
	private static String TAG = ILPCalculationCPLEX.class.getName();
	
//	private final String MODEL_NAME = "Cloudlet-SmartThing-Allocation";
	private List<MobileDevice> smartThings;
	private List<FogDevice> availableCloudlets;
    private IloCplex model;
    private DeviceFacade dfInstance;
    private IloIntVar[][] allocate;
//    private int ilpMode = 1; // 1 for pli-1 and 2 for pli-2
	
	public ILPCalculationCPLEX(List<MobileDevice> smartThings, List<FogDevice> availableCloudlets, int ilpMode) {
		this.smartThings = smartThings;
		this.availableCloudlets = availableCloudlets;
		this.dfInstance = DeviceFacade.getInstance();
//		this.ilpMode = ilpMode;
		createModelAndEnv();
	}
	
	private void createModelAndEnv() {
		try {
			model = new IloCplex();
		} catch (IloException e) {
			 OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void createDecisionVariables() {
		allocate = new IloIntVar[availableCloudlets.size()][smartThings.size()];
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			for (int st = 0; st < smartThings.size(); st++) {
				try {
					allocate[cl][st] = model.boolVar("allocate[" 
							+ cl + "][" + st + "]");
				} catch (IloException e) {
					OptLogger.error(TAG, e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
	
	private void storageConstr() {
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			IloLinearIntExpr leftStorageConstr;
			try {
				leftStorageConstr = model.linearIntExpr();
				for (int st = 0; st < smartThings.size(); st++) {
					MobileDevice user = smartThings.get(st);
					long stStorage = dfInstance.getStorage(user);
					leftStorageConstr.addTerm(Math.toIntExact(stStorage), allocate[cl][st]);
				}
				
				double clStorage = (double) dfInstance.getStorage(availableCloudlets.get(cl));
				model.addLe(leftStorageConstr, clStorage, "storage[" + cl + "]");
			} catch (IloException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void CPUConstr() {
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			IloLinearNumExpr leftCPUConstr;
			try {
				leftCPUConstr = model.linearNumExpr();
				for (int st = 0; st < smartThings.size(); st++) {
					MobileDevice user = smartThings.get(st);
					double stStorage = dfInstance.getMIPS(user);
					leftCPUConstr.addTerm(stStorage, allocate[cl][st]);
				}
				
				double clCPU = (double) dfInstance.getMIPS(availableCloudlets.get(cl));
				model.addLe(leftCPUConstr, clCPU, "cpu[" + cl + "]");
			} catch (IloException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void RAMConstr() {
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			IloLinearIntExpr leftRAMConstr;
			try {
				leftRAMConstr = model.linearIntExpr();
				for (int st = 0; st < smartThings.size(); st++) {
					MobileDevice user = smartThings.get(st);
					int stStorage = dfInstance.getRAM(user);
					leftRAMConstr.addTerm(stStorage, allocate[cl][st]);
				}
				
				double clRAM = (double) dfInstance.getRAM(availableCloudlets.get(cl));
				model.addLe(leftRAMConstr, clRAM, "ram[" + cl+ "]");
			} catch (IloException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void limitCloudletsConstr() {
		for (int st = 0; st < smartThings.size(); st++) {
			IloLinearIntExpr limitCloudletConstr;
			
			try {
				limitCloudletConstr = model.linearIntExpr();
				for (int cl = 0; cl < availableCloudlets.size(); cl++) {
					limitCloudletConstr.addTerm(1, allocate[cl][st]);
				}
			
				model.addEq(limitCloudletConstr, 1, "smartThing[" + st + "]");
			} catch (IloException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}	
	}
	
	private void defineObjFunction() {
		IloLinearNumExpr obj;
		try {
			obj = model.linearNumExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				for (int cl = 0; cl < availableCloudlets.size(); cl++) {
					obj.addTerm(
							getDelay(
									availableCloudlets.get(cl), 
									smartThings.get(st).getVmLocalServerCloudlet())
							, allocate[cl][st]);
				}
			}
			
			model.addMinimize(obj);
		} catch (IloException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private double getDelay(FogDevice destinyCloudlet, FogDevice sourceCloudlet) {
		return NetworkTopology.getDelay(destinyCloudlet.getId(), sourceCloudlet.getId());
	}
	
	public void createConstrs() {
		storageConstr();
		CPUConstr();
		RAMConstr();
//		latencyConstr();
		limitCloudletsConstr();
	}
	
	private void printResult() {
		try {
			OptLogger.debug(TAG, "Solution status: " + model.getStatus());
			
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
				for (int st = 0; st < smartThings.size(); st++) {
						if (model.getValue(allocate[cl][st]) > 0) {
							OptLogger.debug(TAG, "" + allocate[cl][st].getName()
									+ ": " + model.getValue(allocate[cl][st]));
						}
				}
			}
		} catch (IloException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private HashMap<MobileDevice, FogDevice> getResult() {
		HashMap<MobileDevice, FogDevice> ILPResult = new HashMap<>();
		
		try {
			OptLogger.debug(TAG, "Solution status: " + model.getStatus());
			
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
				for (int st = 0; st < smartThings.size(); st++) {
						if (model.getValue(allocate[cl][st]) > 0) {
							ILPResult.put(smartThings.get(st), availableCloudlets.get(cl));
						}
				}
			}
		} catch (IloException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
		
		for (MobileDevice name: ILPResult.keySet()) {
		    String key = name.toString();
		    String value = ILPResult.get(name).toString();
		    OptLogger.debug(TAG, key + " " + value);
		}
		
		model.end();
		
		return ILPResult;
		
	}
	
	public HashMap<MobileDevice, FogDevice>  solveILP() {
		createDecisionVariables();
		defineObjFunction();
		createConstrs();
		
		try {
			if (!model.solve()) {
				throw new IloException("Failed to optimize");
			}
			
			model.exportModel("project.java.lp");
		} catch (IloException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
		
		printResult();
		return getResult();
	}
	
}
