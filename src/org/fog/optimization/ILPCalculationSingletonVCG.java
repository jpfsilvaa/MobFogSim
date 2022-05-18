package org.fog.optimization;

import java.util.HashMap;
import java.util.List;

import org.cloudbus.cloudsim.NetworkTopology;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.facade.DeviceFacade;
import org.fog.vmmigration.LatencyByDistance;
import org.fog.vmmobile.AppExample;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public final class ILPCalculationSingletonVCG {
	
	private static String TAG = ILPCalculationSingletonVCG.class.getName();
	private static ILPCalculationSingletonVCG INSTANCE;
	
	private final String MODEL_NAME = "Cloudlet-SmartThing_Allocation";
	private List<MobileDevice> smartThings;
	private List<FogDevice> availableCloudlets;
	private GRBEnv env;
    private GRBModel model;
    private DeviceFacade dfInstance;
    private GRBVar[][] allocate;
    private int ilpMode = 1; // 1 for pli-1 and 2 for pli-2    
	
	private ILPCalculationSingletonVCG() {
		createModelAndEnv();
		createDecisionVariables();
		defineObjectiveFunction();
		createConstraints();
	}
	
	public static synchronized ILPCalculationSingletonVCG getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ILPCalculationSingletonVCG();
		}
		
		return INSTANCE;
	}

	private void createModelAndEnv(){
		this.smartThings = AppExample.getSmartThings();
		this.availableCloudlets = AppExample.getServerCloudlets();
		this.dfInstance = DeviceFacade.getInstance();
//		this.ilpMode = ilpMode;
		
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
				int currUserId = smartThings.get(st).getMyId();
				int currCloudletId = availableCloudlets.get(cl).getMyId();
				try {
					allocate[currCloudletId][currUserId] = model.addVar(0, 1, 1, GRB.BINARY, "allocate[" 
							+ currCloudletId + "][" + currUserId + "]");
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
		
		for (int st = 0; st < smartThings.size(); st++) {
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
				MobileDevice user = smartThings.get(st);
				int currUserId = user.getMyId();
				int currCloudletId = availableCloudlets.get(cl).getMyId();
				obj.addTerm(
						(user.getBid() - user.getMonetaryFactor() * user.getMigCost())
						, allocate[currCloudletId][currUserId]);
			}
		}
		
		try {
			model.setObjective(obj, GRB.MAXIMIZE);
		} catch (GRBException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void storageConstr() {
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			GRBLinExpr leftStorageConstr = new GRBLinExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				MobileDevice user = smartThings.get(st);
				int currUserId = user.getMyId();
				int currCloudletId = availableCloudlets.get(cl).getMyId();
				long stStorage = dfInstance.getStorage(user);
				leftStorageConstr.addTerm(stStorage, allocate[currCloudletId][currUserId]);
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
				int currUserId = smartThings.get(st).getMyId();
				int currCloudletId = availableCloudlets.get(cl).getMyId();
				leftCPUConstr.addTerm(stCPU, allocate[currCloudletId][currUserId]);
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
				int currUserId = smartThings.get(st).getMyId();
				int currCloudletId = availableCloudlets.get(cl).getMyId();
				leftRAMConstr.addTerm(stRAM, allocate[currCloudletId][currUserId]);
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
	
	private void latencyConstr() {
		for (int st = 0; st < smartThings.size(); st++) {
			GRBLinExpr leftLatConstr = new GRBLinExpr();
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
				double latency = LatencyByDistance.latencyConnection(availableCloudlets.get(cl), smartThings.get(st));
				int currUserId = smartThings.get(st).getMyId();
				int currCloudletId = availableCloudlets.get(cl).getMyId();
				leftLatConstr.addTerm(latency, allocate[currCloudletId][currUserId]);
			}
			
			try {
				model.addConstr(leftLatConstr, GRB.LESS_EQUAL, smartThings.get(st).getMaxLatency(), "latency[" + smartThings.get(st).getMyId() + "]");
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
				int currUserId = smartThings.get(st).getMyId();
				int currCloudletId = availableCloudlets.get(cl).getMyId();
				limitCloudletConstr.addTerm(1, allocate[currCloudletId][currUserId]);
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
					int currUserId = smartThings.get(st).getMyId();
					int currCloudletId = availableCloudlets.get(cl).getMyId();
					if (allocate[currCloudletId][currUserId].get(GRB.DoubleAttr.X) > 0) {
						OptLogger.debug(TAG, "" + allocate[currCloudletId][currUserId].get(GRB.StringAttr.VarName)
								+ ": " + allocate[currCloudletId][currUserId].get(GRB.DoubleAttr.X));
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
					int currUserId = smartThings.get(st).getMyId();
					int currCloudletId = availableCloudlets.get(cl).getMyId();
					if (allocate[currCloudletId][currUserId].get(GRB.DoubleAttr.X) > 0) {
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
		latencyConstr();
		limitCloudletsConstr();
	}
	
	private void pricing() {
		for (MobileDevice user : smartThings) {
//			user.setPriceToPay(clarke/PivotRule(user) - socialWelfareWithout(user));
		}
	}
	
//	private float clarkePivotRule(MobileDevice user) {
//		
//	}
	
	private float socialWelfareWithout(MobileDevice v) {
		float socialWelfare = 0;
		for (int st = 0; st < smartThings.size(); st++) {
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
				MobileDevice user = smartThings.get(st);
				int currUserId = user.getMyId();
				int currCloudletId = availableCloudlets.get(cl).getMyId();
				if (currUserId != v.getMyId()) {
					try {
						if (allocate[currCloudletId][currUserId].get(GRB.DoubleAttr.X) > 0) {
							socialWelfare += user.getBid() - user.getMonetaryFactor() * user.getMigCost();
						}
					} catch (GRBException e) {
						OptLogger.error(TAG, e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
		
		return socialWelfare;
	}
	
	
	public HashMap<MobileDevice, FogDevice> solveILP() {

		try {
//			model.write("teste_pli.lp");
			model.optimize();
			pricing();
		} catch (GRBException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
		
		printResult();
		return getResult();	
	}
	
}