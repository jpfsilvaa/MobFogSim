package org.fog.optimization;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.cloudbus.cloudsim.NetworkTopology;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.facade.DeviceFacade;
import org.fog.vmmigration.LatencyByDistance;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public final class ILPCalculationVCG {
	
	private static String TAG = ILPCalculationVCG.class.getName();
	
	private final String MODEL_NAME = "VCG_Allocation";
	private List<MobileDevice> smartThings;
	private List<FogDevice> availableCloudlets;
	private GRBEnv env;
    private GRBModel model;
    private DeviceFacade dfInstance;
    private GRBVar[][] allocate;
	
    public ILPCalculationVCG(List<MobileDevice> smartThings, List<FogDevice> availableCloudlets) {
		this.smartThings = smartThings;
		this.availableCloudlets = availableCloudlets;
		this.dfInstance = DeviceFacade.getInstance();
		createModelAndEnv();
    }

	private void createModelAndEnv(){	
		OptLogger.debug(TAG, "VCG - createModelAndEnv");
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
		OptLogger.debug(TAG, "VCG - createDecisionVariables");
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
	
	private void defineObjectiveFunction() {
		OptLogger.debug(TAG, "VCG - defineObjectiveFunction");
		GRBLinExpr obj = new GRBLinExpr();		
		
		for (int st = 0; st < smartThings.size(); st++) {
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
				MobileDevice user = smartThings.get(st);
				FogDevice cloudlet = availableCloudlets.get(cl);
				double latency = 99;
				try {
					latency = LatencyByDistance.latencyConnection(
							cloudlet, user);
				} catch (NullPointerException ex) { // In the case when the AP is null because the user did not connect to it yet
					OptLogger.debug(TAG, "Latency between st and cl is 99 because the user is still not connected in an AP");
				}
				
				double bidAndCost = user.getBid() - user.getMonetaryFactor() 
						* latency;
				
				obj.addTerm(bidAndCost, allocate[cl][st]);
				user.getRelativeBid().put(cloudlet.getMyId(), bidAndCost);
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
		OptLogger.debug(TAG, "VCG - storageConstr");
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			GRBLinExpr leftStorageConstr = new GRBLinExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				MobileDevice user = smartThings.get(st);
				long stStorage = dfInstance.getStorage(user);
				leftStorageConstr.addTerm(stStorage, allocate[cl][st]);
			}
			
			double clStorage = (double) dfInstance.getStorage(availableCloudlets.get(cl));
			try {
				model.addConstr(leftStorageConstr, GRB.LESS_EQUAL, clStorage, "storage[" 
									+ availableCloudlets.get(cl).getMyId() + "]");
			} catch (GRBException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void CPUConstr() {
		OptLogger.debug(TAG, "VCG - CPUConstr");
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			GRBLinExpr leftCPUConstr = new GRBLinExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				double stCPU = dfInstance.getMIPS(smartThings.get(st));
				leftCPUConstr.addTerm(stCPU, allocate[cl][st]);
			}
			
			double clCPU = dfInstance.getMIPS(availableCloudlets.get(cl));
			try {
				model.addConstr(leftCPUConstr, GRB.LESS_EQUAL, clCPU, "cpu[" 
									+ availableCloudlets.get(cl).getMyId() + "]");
			} catch (GRBException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void RAMConstr() {
		OptLogger.debug(TAG, "VCG - RAMConstr");
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			GRBLinExpr leftRAMConstr = new GRBLinExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				int stRAM = dfInstance.getRAM(smartThings.get(st));
				leftRAMConstr.addTerm(stRAM, allocate[cl][st]);
			}
			
			int clRAM = dfInstance.getRAM(availableCloudlets.get(cl));
			try {
				model.addConstr(leftRAMConstr, GRB.LESS_EQUAL, clRAM, "ram[" 
									+ availableCloudlets.get(cl).getMyId() + "]");
			} catch (GRBException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void latencyConstr() {
		OptLogger.debug(TAG, "VCG - latencyConstr");
		for (int st = 0; st < smartThings.size(); st++) {
			GRBLinExpr leftLatConstr = new GRBLinExpr();
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {				
				double latency = 99;
				try {
					latency = LatencyByDistance.latencyConnection(
							availableCloudlets.get(cl), smartThings.get(st));
				} catch (NullPointerException ex) { // In the case when the AP is null because the user did not connect to it yet
					OptLogger.debug(TAG, "Latency between st and cl is 99 because the user is still not connected in an AP");
				}
				
				leftLatConstr.addTerm(latency, allocate[cl][st]);
			}
			
			try {
				model.addConstr(leftLatConstr, GRB.LESS_EQUAL, 
						smartThings.get(st).getMaxLatency(), "latency[" 
								+ smartThings.get(st).getMyId() + "]");
			} catch (GRBException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void limitCloudletsConstr() {
		OptLogger.debug(TAG, "VCG - limitCloudletsConstr");
		for (int st = 0; st < smartThings.size(); st++) {
			GRBLinExpr limitCloudletConstr = new GRBLinExpr();
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
				limitCloudletConstr.addTerm(1, allocate[cl][st]);
			}
			
			try {
				model.addConstr(limitCloudletConstr, GRB.LESS_EQUAL, 1, "smartThing[" 
									+ smartThings.get(st).getMyId() + "]");
			} catch (GRBException e) {
				OptLogger.error(TAG, e.getMessage());
				e.printStackTrace();
			}
		}	
	}
	
	private void relativeBidConstr() {
		OptLogger.debug(TAG, "VCG - relativeBidConstr");
		for (int st = 0; st < smartThings.size(); st++) {
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
				if (smartThings.get(st).getRelativeBid().get(availableCloudlets.get(cl).getMyId()) <= 0) {
					GRBLinExpr limitCloudletConstr = new GRBLinExpr();
					limitCloudletConstr.addTerm(1, allocate[cl][st]);
					
					try {
						model.addConstr(limitCloudletConstr, GRB.EQUAL, 0, "smartThing[" 
											+ smartThings.get(st).getMyId() + "]_to_cloudlet[" 
											+ availableCloudlets.get(cl).getMyId() + "]");
					} catch (GRBException e) {
						OptLogger.error(TAG, e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}	
	}
	
	private void printResult() {
		OptLogger.debug(TAG, "VCG - printResult");
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			for (int st = 0; st < smartThings.size(); st++) {
				try {
					if (allocate[cl][st].get(GRB.DoubleAttr.X) > Math.pow(10, -6)) {
						OptLogger.debug(TAG, "" + allocate[cl][st].get(GRB.StringAttr.VarName)
								+ ": " + allocate[cl][st].get(GRB.DoubleAttr.X));
					}
				} catch (GRBException e) {
					OptLogger.error(TAG, e.getMessage());
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
	}
	
	private HashMap<MobileDevice, FogDevice> getResult() {	
		OptLogger.debug(TAG, "VCG - getResult");
		HashMap<MobileDevice, FogDevice> ILPResult = new HashMap<>();
		
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			for (int st = 0; st < smartThings.size(); st++) {	
				try {
					if (allocate[cl][st].get(GRB.DoubleAttr.X) > Math.pow(10, -6)) {
						ILPResult.put(smartThings.get(st), availableCloudlets.get(cl));
					}
				} catch (GRBException e) {
					OptLogger.error(TAG, e.getMessage());
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
		
		return ILPResult;
	}
	
	private void createConstraints() {
		OptLogger.debug(TAG, "VCG - createConstraints");
		storageConstr();
		CPUConstr();
		RAMConstr();
		latencyConstr();
		limitCloudletsConstr();
		relativeBidConstr();
	}
	
	private Boolean isUserAllocated(MobileDevice user, Set<MobileDevice> allocatedUsers) {
		Boolean result = false;
		for (MobileDevice u : allocatedUsers) {
			if (u.getMyId() == user.getMyId())
				result = true;
		}
		
		return result;
	}
	
	private void pricing(double optValue, HashMap<MobileDevice, FogDevice> mapResult) {
		OptLogger.debug(TAG, "\nVCG - pricing");
		for (MobileDevice user : smartThings) {
			if (isUserAllocated(user, mapResult.keySet())) {
				double socialWalfareValue = socialWelfareWithout(user, optValue, mapResult);
				OptLogger.debug("VCG - pricing", "USER[" + user.getMyId() + "] - socialWelfareWithout->" + socialWalfareValue);
				double clarkeValue = clarkePivotRule(user);
				OptLogger.debug("VCG - pricing", "USER[" + user.getMyId() + "] - clarkeValue->" + clarkeValue);
				user.setPriceToPay(clarkeValue - socialWalfareValue);
				OptLogger.debug("VCG - pricing", "USER[" + user.getMyId() + "] BID -> " + user.getBid() + " / USER REL BID -> " 
									+ user.getRelativeBid().get(mapResult.get(user).getMyId()) + " / USER PRICE -> " + user.getPriceToPay());
			} else {
				user.setPriceToPay(0);
			}
		}
	}
	
	private int findVariableIdByUser(MobileDevice v) {
		OptLogger.debug(TAG, "VCG - findVariableIdByUser");
		int stId = 0;
		for (int i = 0; i < smartThings.size(); i++) {
			if (smartThings.get(i).getMyId() == v.getMyId()) {
				stId = i;
			}
		}
		
		return stId;
	}
	
	private void changeUBAndRemoveConstr(int stIdFromV) {
		OptLogger.debug(TAG, "VCG - changeUBAndRemoveConstr");
		try {
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
					allocate[cl][stIdFromV].set(GRB.DoubleAttr.UB, 0.0);
					model.update();
			}
			
			model.remove(model.getConstrByName("smartThing[" 
					+ smartThings.get(stIdFromV).getMyId() + "]"));
			model.update();
		} catch (GRBException e) {
			OptLogger.debug(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void changeUBAndReaddConstr(int stIdFromV) {
		OptLogger.debug(TAG, "VCG - changeUBAndRemoveConstr");
		try {
			GRBLinExpr limitCloudletConstr = new GRBLinExpr();
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
					allocate[cl][stIdFromV].set(GRB.DoubleAttr.UB, 1.0);
					model.update();
					
					limitCloudletConstr.addTerm(1, allocate[cl][stIdFromV]);
			}
			
			model.addConstr(limitCloudletConstr, GRB.EQUAL, 1, "smartThing[" 
					+ smartThings.get(stIdFromV).getMyId() + "]");
			model.update();
		} catch (GRBException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private double clarkePivotRule(MobileDevice v) {
		OptLogger.debug(TAG, "VCG - clarkePivotRule");
		
		double clarkePivotResult = 0;
		int stIdFromV = findVariableIdByUser(v);
		
		changeUBAndRemoveConstr(stIdFromV);
		
		try {
			model.optimize();
			clarkePivotResult = model.get(GRB.DoubleAttr.ObjVal);
		} catch (GRBException e) {
			OptLogger.debug(TAG, e.getMessage());
			e.printStackTrace();
		}
		
		changeUBAndReaddConstr(stIdFromV);
		OptLogger.debug(TAG, "USER - CLARKE->" + clarkePivotResult);
		return clarkePivotResult;
	}
	
	private double socialWelfareWithout(MobileDevice v, double optValue, 
			HashMap<MobileDevice, FogDevice> mapResult) {
		OptLogger.debug(TAG, "VCG - socialWelfareWithout");
		
		double socialWelfare = optValue - v.getRelativeBid().get(mapResult.get(v).getMyId());
		return socialWelfare;
	}
	
	private void initializeModel() {
		OptLogger.debug(TAG, "VCG - initializeModel");
		createDecisionVariables();
		defineObjectiveFunction();
		createConstraints();
	}
	
	private HashMap<MobileDevice, FogDevice> optimizeAndPrice() {
		OptLogger.debug(TAG, "VCG - optimizeAndPrice");
		HashMap<MobileDevice, FogDevice> optResult = null;
		try {
			model.optimize();
			model.write("teste_lances.lp");
			optResult = getResult();
			printResult();
			pricing(model.get(GRB.DoubleAttr.ObjVal), optResult);
		} catch (GRBException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
		
		return optResult;
	}
	
	private void disposeModel() {
		OptLogger.debug(TAG, "VCG - disposeModel");
		model.dispose();
	    try {
			env.dispose();
		} catch (GRBException e) {
			OptLogger.error(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	public HashMap<MobileDevice, FogDevice> solveILP() {
		OptLogger.debug(TAG, "VCG - solveILP");
		
		initializeModel();
		HashMap<MobileDevice, FogDevice> optResult = optimizeAndPrice();
		disposeModel();
		
		return optResult;	
	}
	
}