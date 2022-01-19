package org.fog.optimization;

import java.util.HashMap;
import java.util.List;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.optimization.facade.DeviceFacade;
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
	
    // TODO: COLOCAR TRY/CATCH NO METODO QUE CHAMA TODOS OS METODOS
    
	public ILPCalculation(List<MobileDevice> smartThings, List<FogDevice> availableCloudlets) throws GRBException {
		System.out.printf("%s: ILPCalculation%n", TAG);
		this.smartThings = smartThings;
		this.availableCloudlets = availableCloudlets;
		this.dfInstance = DeviceFacade.getInstance();
		createModelAndEnv();
		
		availableCloudlets.add(0, AppExample.getServerCloudlets().get(0));
	}

	private void createModelAndEnv(){
		System.out.printf("%s: createModelAndEnv%n", TAG);
		try {
			env = new GRBEnv();
			model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, MODEL_NAME);
		} catch (GRBException e) {
			System.out.printf("%s: createModel exception: %s%n", TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void createDecisionVariables() {
		System.out.printf("%s: createDecisionVariables%n", TAG);
		allocate = new GRBVar[availableCloudlets.size()][smartThings.size()];
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			for (int st = 0; st < smartThings.size(); st++) {
				try {
					allocate[cl][st] = model.addVar(0, 1, 1, GRB.BINARY, "allocate[" 
							+ availableCloudlets.get(cl).getMyId() + "][" + smartThings.get(st).getMyId() + "]");
				} catch (GRBException e) {
					System.out.printf("%s: createDecisionVariables exception: %s%n", TAG, e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
	
	private void defineObjectiveFunction() {
		System.out.printf("%s: defineObjectiveFunction%n", TAG);
		GRBLinExpr obj = new GRBLinExpr();
		for (int st = 0; st < smartThings.size(); st++) {
			obj.addTerm(1, allocate[0][st]);
		}
		
		try {
			model.setObjective(obj, GRB.MINIMIZE);
		} catch (GRBException e) {
			System.out.printf("%s: defineObjectiveFunction exception: %s%n", TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void storageConstr() {
		System.out.printf("%s: storageConstr%n", TAG);
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			GRBLinExpr leftStorageConstr = new GRBLinExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				long stStorage = dfInstance.getStorage(smartThings.get(st));
				leftStorageConstr.addTerm(stStorage, allocate[cl][st]);
			}
			
			double clStorage = (double) dfInstance.getStorage(availableCloudlets.get(cl));
			try {
				if (cl == 0) clStorage *= 100; // caso seja fakeCloud
				model.addConstr(leftStorageConstr, GRB.LESS_EQUAL, clStorage, "storage[" + availableCloudlets.get(cl).getMyId() + "]");
			} catch (GRBException e) {
				System.out.printf("%s: createConstraints exception: %s%n", TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void CPUConstr() {
		System.out.printf("%s: CPUConstr%n", TAG);
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			GRBLinExpr leftCPUConstr = new GRBLinExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				double stCPU = dfInstance.getMIPS(smartThings.get(st));
				leftCPUConstr.addTerm(stCPU, allocate[cl][st]);
			}
			
			double clCPU = dfInstance.getMIPS(availableCloudlets.get(cl));
			try {
				if (cl == 0) clCPU *= 100; // caso seja fakeCloud
				model.addConstr(leftCPUConstr, GRB.LESS_EQUAL, clCPU, "cpu[" + availableCloudlets.get(cl).getMyId() + "]");
			} catch (GRBException e) {
				System.out.printf("%s: createConstraints exception: %s%n", TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void RAMConstr() {
		System.out.printf("%s: RAMConstr%n", TAG);
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			GRBLinExpr leftRAMConstr = new GRBLinExpr();
			for (int st = 0; st < smartThings.size(); st++) {
				int stRAM = dfInstance.getRAM(smartThings.get(st));
				leftRAMConstr.addTerm(stRAM, allocate[cl][st]);
			}
			
			int clRAM = dfInstance.getRAM(availableCloudlets.get(cl));
			try {
				if (cl == 0) clRAM *= 100; // caso seja fakeCloud
				model.addConstr(leftRAMConstr, GRB.LESS_EQUAL, clRAM, "ram[" + availableCloudlets.get(cl).getMyId() + "]");
			} catch (GRBException e) {
				System.out.printf("%s: createConstraints exception: %s%n", TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void limitCloudletsConstr() {
		System.out.printf("%s: limitCloudletsConstr%n", TAG);
		for (int st = 0; st < smartThings.size(); st++) {
			GRBLinExpr limitCloudletConstr = new GRBLinExpr();
			for (int cl = 0; cl < availableCloudlets.size(); cl++) {
				int stRAM = dfInstance.getRAM(smartThings.get(st));
				limitCloudletConstr.addTerm(1, allocate[cl][st]);
			}
			
			try {
				model.addConstr(limitCloudletConstr, GRB.EQUAL, 1, "smartThing[" + smartThings.get(st).getMyId() + "]");
			} catch (GRBException e) {
				System.out.printf("%s: createConstraints exception: %s%n", TAG, e.getMessage());
				e.printStackTrace();
			}
		}	
	}
	
	private void printResult() {
		System.out.printf("%s: printResult%n", TAG);
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			for (int st = 0; st < smartThings.size(); st++) {
				try {
					if (allocate[cl][st].get(GRB.DoubleAttr.X) > 0) {
						System.out.println("" + allocate[cl][st].get(GRB.StringAttr.VarName)
								+ ": " + allocate[cl][st].get(GRB.DoubleAttr.X));
					}
				} catch (GRBException e) {
					System.out.printf("%s: printResult exception: %s%n", TAG, e.getMessage());
					e.printStackTrace();
				}
			}
		}

	}
	
	private HashMap<MobileDevice, FogDevice> getResult() {
		System.out.printf("%s: printResult%n", TAG);
		
		HashMap<MobileDevice, FogDevice> ILPResult = new HashMap<>();
		
		for (int cl = 0; cl < availableCloudlets.size(); cl++) {
			for (int st = 0; st < smartThings.size(); st++) {
				try {
					if (allocate[cl][st].get(GRB.DoubleAttr.X) > 0) {
						ILPResult.put(smartThings.get(st), availableCloudlets.get(cl));
					}
				} catch (GRBException e) {
					System.out.printf("%s: getResult exception: %s%n", TAG, e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		return ILPResult;
	}
	
	private void createConstraints() {
		System.out.printf("%s: createConstraints%n", TAG);
		storageConstr();
		CPUConstr();
		RAMConstr();
		limitCloudletsConstr();
	}
	
	public HashMap<MobileDevice, FogDevice> solveILP() {
		System.out.printf("%s: solveILP%n", TAG);
		createDecisionVariables();
		defineObjectiveFunction();
		createConstraints();
		
		try {
			model.write("teste_pli.lp");
			model.optimize();
		} catch (GRBException e) {
			System.out.printf("%s: solveILP exception: %s%n", TAG, e.getMessage());
			e.printStackTrace();
		}
		
		printResult();
		return getResult();
		
	}
	
}