from gurobipy import *
import sys
import json

def readJSONData(jsonFilePath):
    jsonFile = open(jsonFilePath)
    data = json.load(jsonFile)
    jsonFile.close()
    return data

def buildVMsDict(vmsJsonData):
    return multidict(
        (vm["VM_ID"], [vm["VM_CPU"], vm["CURR_CLOUDLET"], vm["VM_RAM"], vm["VM_STORAGE"]])
            for vm in vmsJsonData
    )

def buildCloudletsDict(cloudletsJsonData):
    return multidict(
        (cloudlet['CL_ID'], [cloudlet['CL_STORAGE'], cloudlet['CL_CPU'], cloudlet['CL_RAM']])
            for cloudlet in cloudletsJsonData
    )

def buildDelaysDict(delaysJsonData):
    return multidict(
        ((cloudlet['SOURCE'], cloudlet['DESTINY']), cloudlet['DELAY'])
            for cloudlet in delaysJsonData
    )

# Display optimal values of decision variables
def printSolution(modelOpt):
    for v in modelOpt.getVars():
        if (abs(v.x) > 1e-6):
            print(v.varName, v.x)

def build(pliOption, jsonFilePath, outFilePath):
    data = readJSONData(jsonFilePath)

    v_ids, v_CPU, v_curr_cl, v_RAM, v_storage = buildVMsDict(data['VMs'])
    c_ids, c_storage, c_CPU, c_RAM = buildCloudletsDict(data['Cloudlets'])
    combinations, delay = buildDelaysDict(data['DelayBetweenCloudlets'])

    m = Model('Cloudlet-VM Allocation')
    m.Params.LogToConsole = 0 # avoid to print optimizer info
    
    x = m.addVars(c_ids, v_ids, vtype=GRB.BINARY, name="allocate ")

    # storage constraint
    for n in c_ids:
        m.addConstr((
            quicksum(v_storage[v]*x[n,v] for v in v_ids) <= c_storage[n]
        ), name='storage[%s]'%n)

    # CPU constraint
    for n in c_ids:
        m.addConstr((
            quicksum(v_CPU[v]*x[n,v] for v in v_ids) <= c_CPU[n]
        ), name='CPU[%s]'%n)

    # RAM constraint
    for n in c_ids:
        m.addConstr((
            quicksum(v_RAM[v]*x[n,v] for v in v_ids) <= c_RAM[n]
        ), name='RAM[%s]'%n)

    # allocation constraint: a VM must be allocated (even in cloud), 
    # but only in one place (i.e., a VM must not be allocated in two places)
    for v in v_ids:
        m.addConstr((
            quicksum(x[n,v] for n in c_ids) == 1
        ), name='allocate[%s]'%v)

    if pliOption == "1":
        expr = (x.sum(c_ids[0], '*'))
    elif pliOption == "2":
        expr = (quicksum(delay[n, v_curr_cl[v]]*x[n,v] for n in c_ids for v in v_ids))

    m.setObjective(expr, GRB.MINIMIZE)

    fileName = "formulation.lp"
    m.write(fileName)

    m.optimize()
    printSolution(m)

def main():
    # python formulation_gurobi_json.py <1 for PLI 1 or 2 for PLI 2> <json file path> <out file path directory>
    args = sys.argv[1:]
    build(args[0], args[1], args[2])

if __name__ == "__main__":
    main()