package org.fog.optimization;

import java.util.HashMap;
import java.util.List;

import org.cloudbus.cloudsim.NetworkTopology;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.facade.DeviceFacade;
import org.fog.optimization.facade.OptLogger;
import org.fog.vmmobile.AppExample;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class ILPCalculation {
	
	private static String TAG = "-------JOAO " + ILPCalculation.class.getName();
	
	private final String MODEL_NAME = "Cloudlet-SmartThing_Allocation";
	private List<MobileDevice> smartThings;
	private List<FogDevice> availableCloudlets;
	private GRBEnv env;
    private GRBModel model;
    private DeviceFacade dfInstance;
    private GRBVar[][] allocate;
    private int ilpMode = 1; // 1 for pli-1 and 2 for pli-2
	
	public ILPCalculation(List<MobileDevice> smartThings, List<FogDevice> availableCloudlets, int ilpMode) {
		this.smartThings = smartThings;
		this.availableCloudlets = availableCloudlets;
		this.dfInstance = DeviceFacade.getInstance();
		this.ilpMode = ilpMode;
		createModelAndEnv();
		
		availableCloudlets.add(0, AppExample.getServerCloudlets().get(0));
	}

	private void createModelAndEnv(){
		try {
			env = new GRBEnv();
			model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, MODEL_NAME);
		} catch (GRBException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void createDecisionVariables() {
		allocate = new GRBVar[availableCloudlets.size()][smartThings.size()];
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			for (int st = 0; st < smartThings.size(); st++) {
				try {
					allocate[cl][st] = model.addVar(0, 1, 1, GRB.BINARY, "allocate[" 
							+ availableCloudlets.get(cl).getMyId() + "][" + smartThings.get(st).getMyId() + "]");
				} catch (GRBException e) {
					OptLogger.error(TAG, e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
	
	private double getDelay(FogDevice destinyCloudlet, FogDevice sourceCloudlet) {
		return NetworkTopology.getDelay(destinyCloudlet.getId(), sourceCloudlet.getId());
	}
	
	private void defineObjectiveFunction() {
		GRBLinExpr obj = new GRBLinExpr();
		
		if (ilpMode == 1) {
			for (int st = 0; st < smartThings.size(); st++) {
				obj.addTerm(1, allocate[0][st]);
			}
		} else if (ilpMode == 2) {
			for (int st = 0; st < smartThings.size(); st++) {
				for (int cl = 0; cl < availableCloudlets.size(); cl++) {
					obj.addTerm(
							getDelay(
									availableCloudlets.get(cl), 
									smartThings.get(st).getVmLocalServerCloudlet())
							, allocate[cl][st]);
				}
			}
		}		
		
		try {
			model.setObjective(obj, GRB.MINIMIZE);
		} catch (GRBException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void storageConstr() {
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			GRBLinExpr leftStorageConstr = new GRBLinExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				long stStorage = dfInstance.getStorage(smartThings.get(st));
				leftStorageConstr.addTerm(stStorage, allocate[cl][st]);
			}
			
			double clStorage = (double) dfInstance.getStorage(availableCloudlets.get(cl));
			try {
				model.addConstr(leftStorageConstr, GRB.LESS_EQUAL, clStorage, "storage[" + availableCloudlets.get(cl).getMyId() + "]");
			} catch (GRBException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void CPUConstr() {
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			GRBLinExpr leftCPUConstr = new GRBLinExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				double stCPU = dfInstance.getMIPS(smartThings.get(st));
				leftCPUConstr.addTerm(stCPU, allocate[cl][st]);
			}
			
			double clCPU = dfInstance.getMIPS(availableCloudlets.get(cl));
			try {
				model.addConstr(leftCPUConstr, GRB.LESS_EQUAL, clCPU, "cpu[" + availableCloudlets.get(cl).getMyId() + "]");
			} catch (GRBException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void RAMConstr() {
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			GRBLinExpr leftRAMConstr = new GRBLinExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				int stRAM = dfInstance.getRAM(smartThings.get(st));
				leftRAMConstr.addTerm(stRAM, allocate[cl][st]);
			}
			
			int clRAM = dfInstance.getRAM(availableCloudlets.get(cl));
			try {
				model.addConstr(leftRAMConstr, GRB.LESS_EQUAL, clRAM, "ram[" + availableCloudlets.get(cl).getMyId() + "]");
			} catch (GRBException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void limitCloudletsConstr() {
		for (int st = 0; st < smartThings.size(); st++) {
			GRBLinExpr limitCloudletConstr = new GRBLinExpr();
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
				limitCloudletConstr.addTerm(1, allocate[cl][st]);
			}
			
			try {
				model.addConstr(limitCloudletConstr, GRB.EQUAL, 1, "smartThing[" + smartThings.get(st).getMyId() + "]");
			} catch (GRBException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}	
	}
	
	private void printResult() {
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			for (int st = 0; st < smartThings.size(); st++) {
				try {
					if (allocate[cl][st].get(GRB.DoubleAttr.X) > 0) {
						OptLogger.debug(TAG, "" + allocate[cl][st].get(GRB.StringAttr.VarName)
								+ ": " + allocate[cl][st].get(GRB.DoubleAttr.X));
					}
				} catch (GRBException e) {
					OptLogger.error(TAG, e.getMessage());
					e.printStackTrace();
				}
			}
		}

	}
	
	private HashMap<MobileDevice, FogDevice> getResult() {		
		HashMap<MobileDevice, FogDevice> ILPResult = new HashMap<>();
		
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			for (int st = 0; st < smartThings.size(); st++) {
				try {
					if (allocate[cl][st].get(GRB.DoubleAttr.X) > 0) {
						ILPResult.put(smartThings.get(st), availableCloudlets.get(cl));
					}
				} catch (GRBException e) {
					OptLogger.error(TAG, e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		return ILPResult;
	}
	
	private void createConstraints() {
		storageConstr();
		CPUConstr();
		RAMConstr();
		limitCloudletsConstr();
	}
	
	public HashMap<MobileDevice, FogDevice> solveILP() {
		createDecisionVariables();
		defineObjectiveFunction();
		createConstraints();
		
		try {
			model.write("teste_pli.lp");
			model.optimize();
		} catch (GRBException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
		
		printResult();
		return getResult();	
	}
	
}