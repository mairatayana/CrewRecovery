package analysis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import model.ActivityNode;
import model.ActivityNode.FlightType;
import model.ActivityNode.GroundType;
import model.ConnectionInfo;
import model.CrewInfo;
import model.FlightInfo;
import model.GroundArc;

public class CrewRecovery {

	// Variáveis de Custo
	private static int maximumDelayTime = 90;//90;
	private static int nGenFlight = 6; //6;
	private static int delayCost = 40;//40;
	private static int groundCostPerMin = 0;
	//private static int crewDeadHeadCost = 500;
	//private static int crewDestinationInconsistencyCost = 3000;
	private static int crewSwapCost = 300; //100
	private static int flightCancelationCost = 25000;//25000;
	private static int minimumCrewConnectionTime = 30;
	private static int flightLimit = 1000;//480;//1000
	private static int dutyLimit = 1300;//570;//1300
	private static int landingOff = 6;//4
	
	// Variáveis de atraso
	private int delayType = 0; // 0-> roda escala normal 1 -> atraso de um voo 2-> atraso de uma tripulação
	private int delayFlightNbr;
	private int deltaDelay;
	private int delayCrewNbr;
	private int reserveCrewAirport = -1;
	private int reserveCrewStartTime = -1;
	
	public void setReserveCrew(int airport, int startTime) {
		this.reserveCrewAirport = airport;
		this.reserveCrewStartTime = startTime;
	}
	
	public void setFlightDelay(int flightNbr, int deltaDelay) {
		this.delayType = 1;
		this.delayFlightNbr = flightNbr;
		this.deltaDelay = deltaDelay;
	}
	
	public void crewDelay(int crewNbr, int deltaDelay) {
		this.delayType = 2;
		this.delayCrewNbr = crewNbr;
		this.deltaDelay = deltaDelay;
	}
	
	
	public IloCplex calculateRecovery(List<CrewInfo> originalCrewList, List<ConnectionInfo> connectionList,
			List<FlightInfo> originalFlightList, int numberAirports) throws IloException {
		
		// inicia criação de input para CPLEX
		List<FlightInfo> flightList = new ArrayList<>();
		List<CrewInfo> crewList = new ArrayList<>();
		if (this.delayType == 1) {
			flightList = changeDelayFlight(originalFlightList);
			crewList = copyCrewList(originalCrewList);
		}else if(this.delayType == 2) {
			crewList = changeCrewSchedule(originalCrewList);
			for (FlightInfo f : originalFlightList) {
				flightList.add(copyFlight(f));
			}
		}else {
			for (FlightInfo f : originalFlightList) {
				flightList.add(copyFlight(f));
			}
			crewList = copyCrewList(originalCrewList);
		}
		
		if (this.reserveCrewAirport != -1) {
			CrewInfo cr = new CrewInfo();
			cr.setCrewNbr(crewList.size());
			cr.setCrewPath(new ArrayList<>());
			cr.setStartAirport(this.reserveCrewAirport);
			cr.setStartTime(this.reserveCrewStartTime);
			cr.setEndTime(this.reserveCrewStartTime+CrewRecovery.dutyLimit);
			crewList.add(cr);
		}
		
		List<FlightInfo> listDelayFlight = generateDelayedFlights(flightList, crewList);
		
		float[][] flightCost = calculateFlightCost(listDelayFlight, crewList);//[tripulação][flightNb]
		
		List<LinkedList<ActivityNode>> activityNodes = generateActivityNodes(listDelayFlight, numberAirports);//[aeroporto][actNode]
		
		if (this.delayType == 1) {
			includeCrewDelayFlightStartNode(activityNodes,originalFlightList,this.delayFlightNbr);
		}else if (this.delayType == 2) {
			includeCrewDelaedStartNode(activityNodes, crewList, this.delayCrewNbr);
		}
		
		
		includeAllCrewFinishNode(activityNodes,crewList);
		
		List<List<GroundArc>> groundArcs = generateGroundArcs(activityNodes);//[aeroporto][arcoSolo]
		
		float[][] groundCost = calculateGroundCost(groundArcs); //[aeroporto][arcoSolo]
		
		float[] cancelationCost = generateCancelationCost(flightList);
		// fim criação input		
		
		// inicia modelo
		IloCplex cplex = new IloCplex();
		
		IloIntVar[][] x = new IloIntVar[crewList.size()][listDelayFlight.size()];// tripulação por arco de voo [tripulação][flightNb]
		IloIntVar[][][] z = new IloIntVar[crewList.size()][numberAirports][];// tripulação por arco de solo [tripulação][aeroporto][arcoSolo]
        IloIntVar[] y = new IloIntVar[listDelayFlight.size()];
		
		// cria variável de decisão para cada atribuir tripulação ao voo
		for(int i=0; i<crewList.size(); i++){
			for(int j=0; j<listDelayFlight.size(); j++){
				x[i][j] = cplex.intVar(0, 1, "x"+i+","+j);
			}
		}
		
		for(int i=0; i<flightList.size(); i++) {
			y[i] = cplex.intVar(0, 1, "y"+i);
		}
		
		// cria variável de decisão para definir os arcos de solo de cada tripulação
		for (int i=0; i<crewList.size(); i++) {
			for(int j=0; j<numberAirports; j++){
				if (groundArcs.get(j)!= null) {
					z[i][j] = new IloIntVar[groundArcs.get(j).size()];
					for (int k=0; k<groundArcs.get(j).size(); k++) {
						z[i][j][k] = cplex.intVar(0, 1, "z"+i+","+j+","+k);
					}
				}else {
					z[i][j] = new IloIntVar[1];
					z[i][j][0] = cplex.intVar(0, 1, "z"+i+","+j+","+0);
				}
			}
		}
		
		IloLinearNumExpr objective = cplex.linearNumExpr();
		for(int i=0; i<crewList.size(); i++){
			for(int j=0; j<listDelayFlight.size(); j++){
				objective.addTerm(flightCost[i][j], x[i][j]);
			}
		}
		for (int i=0; i< flightList.size(); i++) {
			objective.addTerm(cancelationCost[i], y[i]);
		}
		for (int i=0; i<crewList.size(); i++) {
			for(int j=0; j<numberAirports; j++){
				if (groundArcs.get(j)!= null) {
					for (int k=0; k<groundArcs.get(j).size(); k++) {
						objective.addTerm(groundCost[j][k],z[i][j][k]);
					}
				}
			}
		}
		cplex.addMinimize(objective);
		
		// restricao que garante que cada voo tem uma e somente uma tripulaçao atribuida e que não é atribuida em voo cancelado
		IloLinearNumExpr xkj = cplex.linearNumExpr();
		for(int i=0; i<flightList.size(); i++){
			xkj.clear();
			int originalFlightNbr = flightList.get(i).getFlightNumberId();
			for(int j=0; j<listDelayFlight.size(); j++){
				if (originalFlightNbr == listDelayFlight.get(j).getFlightNumberId()) {
					for (int k=0; k<crewList.size();k++) {
						xkj.addTerm(1, x[k][j]); 
					}
				}
			}
			xkj.addTerm(1, y[i]);
			cplex.addEq(1, xkj);
		}
		
		// restrição que inicia tripulação em algum nó, no caso, a base da tripulação e no nó de atividade que a tripulação iniciaria
		// para isso, ou a tripulação faz o arco de voo que a ela está escalada ou faz o arco de solo que está saindo desse nó
		IloLinearNumExpr crewStartInput = cplex.linearNumExpr();
		IloLinearNumExpr crewStartOutput = cplex.linearNumExpr();
		for (int k=0; k<crewList.size();k++) {
			crewStartOutput.clear();
			crewStartInput.clear();
			//int firstScheduleFlight = crewList.get(k).getCrewPath().get(0);
			int startAirport = crewList.get(k).getStartAirport();//originalFlightList.get(firstScheduleFlight).getDepatureId();
			int startTime = crewList.get(k).getStartTime();//originalFlightList.get(firstScheduleFlight).getDepartureTime();
			int idNodeStart = 0;
			for(int node=0; node<activityNodes.get(startAirport).size(); node++) {
				int nodeTime = activityNodes.get(startAirport).get(node).getTime();
				if (nodeTime == startTime)
					idNodeStart = node;
			}
			ActivityNode node = activityNodes.get(startAirport).get(idNodeStart);
			List<FlightType> nodeFlights = node.getListFlightArc();
			if (nodeFlights!= null) {
				for (FlightType flightType : nodeFlights) {
					if (flightType.isOutput()) {
						crewStartOutput.addTerm(1, x[k][flightType.getFlightNumberId()]);
					}else {
						crewStartInput.addTerm(1, x[k][flightType.getFlightNumberId()]);
					}
				}
			}
			List<GroundType> nodeGroundArcs = node.getListGroundArc();
			if (nodeGroundArcs!= null) {
				for (GroundType groundType : nodeGroundArcs) {
					if (groundType.isOutput()) {
						crewStartOutput.addTerm(1, z[k][startAirport][groundType.getGroundArcId()]);
					}else {
						crewStartInput.addTerm(1, z[k][startAirport][groundType.getGroundArcId()]);
					}
				}
			}
			cplex.addEq(0, crewStartInput);
			cplex.addEq(1, crewStartOutput);
		}
		
		// restrição que garante fluxo no nó, uma tripulação só pode ter uma atividade de input em um nó e uma atividade de output neste nó
		// não é avaliado o primeiro nó da tripulação e o último nó dos aeroportos, o primeiro nó da tripulação é na restrição anterior 
		// e no último não precisa ser avaliado (para não forçar nenhuma tripulação a estar em determinado ponto de output)
		IloLinearNumExpr nodeCrewInput = cplex.linearNumExpr();
		IloLinearNumExpr nodeCrewOutput = cplex.linearNumExpr();
		IloLinearNumExpr nodeEndCrewInput = cplex.linearNumExpr();
		IloLinearNumExpr nodeEndCrewOutput = cplex.linearNumExpr();
		for (int k=0; k<crewList.size();k++) {
			int startAirport = crewList.get(k).getStartAirport();
			int crewStartTime = crewList.get(k).getStartTime();
			int crewEndTime = crewList.get(k).getEndTime();
			nodeEndCrewInput.clear();
			nodeEndCrewOutput.clear();
			for(int a=0; a<numberAirports; a++){
				for (int node=0; node < activityNodes.get(a).size(); node++) {
					
					int nodeTime = activityNodes.get(a).get(node).getTime();
					// não faz nada se for o primeiro nó da tripulação, essa regra já foi atribuída
					if (!(nodeTime == crewStartTime && a == startAirport)){
					// se o nó for antes de começar o turno da tripulação, ou depois de acabar o turno máximo da tripulação, não deve ter fluxo para aquela posição
						if (nodeTime < crewStartTime || nodeTime > crewEndTime) {
							nodeCrewInput.clear();
							nodeCrewOutput.clear();					
							List<FlightType> flyList = activityNodes.get(a).get(node).getListFlightArc();
							
							if (flyList != null) {
								for (FlightType flightType : flyList) {
									if (flightType.isInput()) {
										nodeCrewInput.addTerm(1, x[k][flightType.getFlightNumberId()]);
									}else {
										nodeCrewOutput.addTerm(1, x[k][flightType.getFlightNumberId()]);
									}
								}
							}
							List<GroundType> groundList = activityNodes.get(a).get(node).getListGroundArc();
							if (groundList != null) {
								for (GroundType groundType : groundList) {
									if (groundType.isInput()) {
										nodeCrewInput.addTerm(1, z[k][a][groundType.getGroundArcId()]);
									}else {
										nodeCrewOutput.addTerm(1, z[k][a][groundType.getGroundArcId()]);
									}
								}
							}
							
							cplex.addEq(0, nodeCrewInput);
							cplex.addEq(0, nodeCrewOutput);
						
						}else if (nodeTime == crewEndTime) {
							List<FlightType> flyList = activityNodes.get(a).get(node).getListFlightArc();
							if (flyList != null) {
								for (FlightType flightType : flyList) {
									if (flightType.isInput()) {
										nodeEndCrewInput.addTerm(1, x[k][flightType.getFlightNumberId()]);
									}else {
										nodeEndCrewOutput.addTerm(1, x[k][flightType.getFlightNumberId()]);
									}
								}
							}
							List<GroundType> groundList = activityNodes.get(a).get(node).getListGroundArc();
							if (groundList != null) {
								for (GroundType groundType : groundList) {
									if (groundType.isInput()) {
										nodeEndCrewInput.addTerm(1, z[k][a][groundType.getGroundArcId()]);
									}else {
										nodeEndCrewOutput.addTerm(1, z[k][a][groundType.getGroundArcId()]);
									}
								}
							}
							
						} else if (node != activityNodes.get(a).size()-1) {
							nodeCrewInput.clear();
							nodeCrewOutput.clear();					
							List<FlightType> flyList = activityNodes.get(a).get(node).getListFlightArc();
							
							if (flyList != null) {
								for (FlightType flightType : flyList) {
									if (flightType.isInput()) {
										nodeCrewInput.addTerm(1, x[k][flightType.getFlightNumberId()]);
									}else {
										nodeCrewOutput.addTerm(1, x[k][flightType.getFlightNumberId()]);
									}
								}
							}
							List<GroundType> groundList = activityNodes.get(a).get(node).getListGroundArc();
							if (groundList != null) {
								for (GroundType groundType : groundList) {
									if (groundType.isInput()) {
										nodeCrewInput.addTerm(1, z[k][a][groundType.getGroundArcId()]);
									}else {
										nodeCrewOutput.addTerm(1, z[k][a][groundType.getGroundArcId()]);
									}
								}
							}
							cplex.addEq(nodeCrewInput, nodeCrewOutput);
						}
					}
				}
			}
			
			
			cplex.addEq(1, nodeEndCrewInput);
			cplex.addEq(0, nodeEndCrewOutput);
		}

		// restrição de tempo de conexão entre os voos
		for (int k = 0; k < crewList.size(); k++) {
			for (int a=0; a < numberAirports; a++) {
				LinkedList<ActivityNode> airportNodes = activityNodes.get(a);
				for (int n = 0; n < airportNodes.size()-1; n++) {
					List<FlightType> flightsA = airportNodes.get(n).getListFlightArc();
					for (FlightType flightA : flightsA) {
						if (flightA.isInput()) {
							int timeArrival = listDelayFlight.get(flightA.getFlightNumberId()).getArrivalTime();
							for (int n_prox = n; n_prox < airportNodes.size(); n_prox++) {
								if (timeArrival + CrewRecovery.minimumCrewConnectionTime > airportNodes.get(n_prox).getTime()) {
									List<FlightType> flightsB = airportNodes.get(n_prox).getListFlightArc();
									for (FlightType flightB : flightsB) {
										if (flightB.isOutput()) {
											//cplex.addLe(cplex.prod(x[k][flightA.getFlightNumberId()],x[k][flightB.getFlightNumberId()],listDelayFlight.get(flightA.getFlightNumberId()).getArrivalTime()+CrewRecovery.minimumCrewConnectionTime-listDelayFlight.get(flightB.getFlightNumberId()).getDepartureTime()), 0);
											cplex.addLe(cplex.prod(x[k][flightA.getFlightNumberId()],x[k][flightB.getFlightNumberId()]), 0);
										}
									}
								}
							}
						}
					}
				}
			}	
		}
		
		// restrição fly limit
		IloLinearNumExpr limit = cplex.linearNumExpr();
		for (int k=0; k<crewList.size();k++) {
			limit.clear();
			for(int f=0; f<listDelayFlight.size(); f++){
				limit.addTerm(listDelayFlight.get(f).getBlockTime(), x[k][f]);
			}
			cplex.addLe(limit, CrewRecovery.flightLimit);
		}
		
		// restrição block time, que é o tempo de voo e ground arc, do block time eu retiro o tempo de apresentacao (60min) e o tempo de finalizacao (30min)
		IloLinearNumExpr duty = cplex.linearNumExpr();
		for (int k=0; k<crewList.size();k++) {
			duty.clear();
			for(int f=0; f<listDelayFlight.size(); f++){
				duty.addTerm(listDelayFlight.get(f).getBlockTime(), x[k][f]);
			}
			for(int a=0; a<numberAirports; a++){
				for(int g=0; g< groundArcs.get(a).size(); g++) {
					duty.addTerm(groundArcs.get(a).get(g).getBlockTime(), z[k][a][g]);
				}
			}
			cplex.addLe(duty, CrewRecovery.dutyLimit);
		}
		
		// restrição da quantidade máxima de pousos
		IloLinearNumExpr ld = cplex.linearNumExpr();
		for (int k=0; k<crewList.size();k++) {
			ld.clear();
			for(int f=0; f<listDelayFlight.size(); f++){
				ld.addTerm(1, x[k][f]);
			}
			cplex.addLe(ld, CrewRecovery.landingOff);
		}
		
		cplex.exportModel("teste.lp");
		
		long startRunTime = System.currentTimeMillis();
		
		if (cplex.solve()) {
			System.out.println("Funcionou");;
			int count = 0;
			for(int i=0; i<crewList.size(); i++){
				for(int j=0; j<listDelayFlight.size(); j++){
					if (Math.abs(cplex.getValue(x[i][j]) - 1) < 0.001) {
						
						cplex.output().println(""+x[i][j].getName() + ": FlightNbr: "+ listDelayFlight.get(j).getFlightNumberId()+
							" ,DepartureTime: "	+ listDelayFlight.get(j).getDepartureTime()/60. +
                            " ,Value= " + cplex.getValue(x[i][j]));
						count++;
					}
				}
			}
			System.out.println("Count FLights: "+count);
			count=0;
			for(int i=0; i< flightList.size(); i++) {
				if (Math.abs(cplex.getValue(y[i]) - 1) < 0.001) {
					cplex.output().println(""+y[i].getName() + ": FlightNbr: "+ flightList.get(i).getFlightNumberId()+
                            " ,Value= " + cplex.getValue(y[i]));
					count++;
				}
			}
			System.out.println("Count Canceled FLights: "+count);
			
		}else {
			
			
			System.out.println("nao funcionou");
		}
		
		long endRunTime = System.currentTimeMillis();
		
		System.out.println("Tempo total para rodar o CPLEX: "+ (endRunTime - startRunTime) + " segundos.");
		
		cplex.exportModel("teste.lp");
		
		return cplex;
	}

	private void includeCrewDelaedStartNode(List<LinkedList<ActivityNode>> activityNodes, List<CrewInfo> crewList,
			int delayCrewNbr2) {
		int startTime = crewList.get(delayCrewNbr2).getStartTime();
		int startAirport = crewList.get(delayCrewNbr2).getStartAirport();
		
		LinkedList<ActivityNode> nodes = activityNodes.get(startAirport);
		int nodePos = 0;
		while(nodePos < nodes.size() &&nodes.get(nodePos).getTime() <= startTime) {
			nodePos++;
		}
		if (nodePos == nodes.size()) {
			ActivityNode actNode = new ActivityNode();
			actNode.setAirportId(startAirport);
			actNode.setTime(startTime);
			nodes.add(actNode);
		}else if (nodes.get(nodePos).getTime() != startTime) {
			ActivityNode actNode = new ActivityNode();
			actNode.setAirportId(startAirport);
			actNode.setTime(startTime);
			nodes.add(nodePos, actNode);
		}
		
	}

	private List<CrewInfo> changeCrewSchedule(List<CrewInfo> originalCrewList) {
		List<CrewInfo> newCrewList = copyCrewList(originalCrewList);
		
		int originalStartTime = newCrewList.get(this.delayCrewNbr).getStartTime();
		int originalEndTime = newCrewList.get(this.delayCrewNbr).getEndTime();
		
		newCrewList.get(this.delayCrewNbr).setStartTime(originalStartTime+this.deltaDelay);
		newCrewList.get(this.delayCrewNbr).setEndTime(originalEndTime+this.deltaDelay);
		
		return newCrewList;
	}
	
	private List<CrewInfo> copyCrewList(List<CrewInfo> originalCrewList){
		List<CrewInfo> newCrewList = new ArrayList<>();
		for (int i=0; i < originalCrewList.size(); i++) {
			CrewInfo crew = copyCrew(originalCrewList.get(i));
			newCrewList.add(crew);
		}
		return newCrewList;
	}

	private CrewInfo copyCrew(CrewInfo crewInfo) {
		CrewInfo cr = new CrewInfo();
		cr.setCrewNbr(crewInfo.getCrewNbr());
		cr.setCrewPath(crewInfo.getCrewPath());
		cr.setEndAirport(crewInfo.getEndAirport());
		cr.setEndTime(crewInfo.getEndTime());
		cr.setStartAirport(crewInfo.getStartAirport());
		cr.setStartTime(crewInfo.getStartTime());
		return cr;
	}

	private void includeAllCrewFinishNode(List<LinkedList<ActivityNode>> activityNodes, List<CrewInfo> crewList) {
		for (int k = 0; k < crewList.size(); k++) {
			int finishTime = crewList.get(k).getEndTime();
			for (int a = 0; a < activityNodes.size(); a++) {
				int n = 0;
				if (activityNodes.get(a) != null) {
					while (n < activityNodes.get(a).size() && activityNodes.get(a).get(n).getTime() < finishTime) {
						n++;
					}
					if (n == activityNodes.get(a).size()) {
						ActivityNode node = new ActivityNode();
						node.setAirportId(a);
						node.setTime(finishTime);
						activityNodes.get(a).add(node);
					}else if (activityNodes.get(a).get(n).getTime() != finishTime) {
						ActivityNode node = new ActivityNode();
						node.setAirportId(a);
						node.setTime(finishTime);
						activityNodes.get(a).add(n,node);
					}
				}
			}
		}
		
	}

	private void includeCrewDelayFlightStartNode(List<LinkedList<ActivityNode>> activityNodes,
			List<FlightInfo> originalFlightList, int delayFlightNbr) {
		int flightPos = 0;
		while (originalFlightList.get(flightPos).getFlightNumberId()!= delayFlightNbr) {
			flightPos++;
		}
		LinkedList<ActivityNode> nodes = activityNodes.get(originalFlightList.get(flightPos).getDepatureId());
		int nodePos = 0;
		while(nodes.get(nodePos).getTime() <= originalFlightList.get(flightPos).getDepartureTime()) {
			nodePos++;
		}
		if (nodes.get(nodePos).getTime() != originalFlightList.get(flightPos).getDepartureTime()) {
			ActivityNode actNode = new ActivityNode();
			actNode.setAirportId(originalFlightList.get(flightPos).getDepatureId());
			actNode.setTime(originalFlightList.get(flightPos).getDepartureTime());
			nodes.add(nodePos, actNode);
		}
	}

	private List<FlightInfo> changeDelayFlight(List<FlightInfo> flightList) {
		List<FlightInfo> modFlightList = new ArrayList<>();
		int flightNbrPos = 0;
		for (int i=0; i<flightList.size(); i++) {
			FlightInfo f = flightList.get(i);
			modFlightList.add(copyFlight(f));
			if (flightList.get(i).getFlightNumberId() == this.delayFlightNbr){
				flightNbrPos=i;
			}
		}
		modFlightList.get(flightNbrPos).setDepartureTime(flightList.get(flightNbrPos).getDepartureTime()+this.deltaDelay);
		modFlightList.get(flightNbrPos).setArrivalTime(flightList.get(flightNbrPos).getArrivalTime()+this.deltaDelay);
		
		return modFlightList;
	}

	private FlightInfo copyFlight(FlightInfo f) {
		FlightInfo fCopy = createFlight(f.getFlightNumberId(), f.getDelayFlightNumberId(), f.isOriginalFlight(), f.getDepatureId(), 
				f.getDepartureTime(), f.getArrivalId(), f.getArrivalTime(),f.getBlockTime());
		return fCopy;
	}
	
	private FlightInfo createFlight(int flightNumberId, int delayFlightNumberId, boolean originalFlight, 
			int depatureId, int departureTime, int arrivalId, int arrivalTime, int blockTime) {
		FlightInfo f = new FlightInfo();
		f.setFlightNumberId(flightNumberId);
		f.setDelayFlightNumberId(delayFlightNumberId);
		f.setOriginalFlight(originalFlight);
		f.setDepatureId(depatureId);
		f.setDepartureTime(departureTime);
		f.setArrivalId(arrivalId);
		f.setArrivalTime(arrivalTime);
		f.setBlockTime(blockTime);
		return f;
	}
	
	private float[] generateCancelationCost(List<FlightInfo> listFlight) {
		float[] cancelationCost = new float[listFlight.size()];
		
		for (int i=0; i<listFlight.size(); i++) {
			cancelationCost[i] = CrewRecovery.flightCancelationCost;
		}
		
		
		return cancelationCost;
	}

	private float[][] calculateGroundCost(List<List<GroundArc>> groundArcs) {
		float[][] cost = new float[groundArcs.size()][];
		for (int i = 0; i < groundArcs.size(); i++) {
			if (groundArcs.get(i) != null) {
				int a = groundArcs.get(i).size();
				cost[i] = new float[a];
				for (int j = 0; j < groundArcs.get(i).size(); j++)
					cost[i][j] = groundArcs.get(i).get(j).getBlockTime()*CrewRecovery.groundCostPerMin;
			}else {
				cost[i] = new float[1];
				cost[i][0] = Float.POSITIVE_INFINITY;
			}
		}
		return cost;
	}

	// gera lista de ground arcs e adiciona nos nós de atividade tbm.
	protected List<List<GroundArc>> generateGroundArcs(List<LinkedList<ActivityNode>> activityNodes) {
		List<List<GroundArc>> totalGroundArcs = new ArrayList<>();
		for (int i = 0; i < activityNodes.size(); i++) {// para cada aeroporto i
			List<GroundArc> groundArcs = new ArrayList<>();
			if (activityNodes.get(i)!= null && activityNodes.get(i).size() > 1) { // se existem nos de atividade no aeroporto i
				for (int j = 0; j < activityNodes.get(i).size()-1; j++) {// para cada no de atividade no aeroporto i
					GroundArc arc = new GroundArc();
					arc.setAirportId(i);
					arc.setGroundArcId(j);
					arc.setStartTime(activityNodes.get(i).get(j).getTime());
					arc.setFinishTime(activityNodes.get(i).get(j+1).getTime());
					arc.setBlockTime(arc.getFinishTime()-arc.getStartTime());
					groundArcs.add(arc);
					activityNodes.get(i).get(j).addGroundArc(j, 1);
					activityNodes.get(i).get(j+1).addGroundArc(j, 0);
				}
			}
			totalGroundArcs.add(groundArcs);
		}
		return totalGroundArcs;
	}

	protected List<LinkedList<ActivityNode>> generateActivityNodes(List<FlightInfo> listDelayFlight,
			int numberAirports) {
		List<LinkedList<ActivityNode>> activityNodes = new ArrayList<>();
		// cria todos os aeroportos, para evitar null pointer exception, a posicao da lista eh o id do aeroporto
		for (int i = 0; i < numberAirports; i++) {
			LinkedList<ActivityNode> nodes = new LinkedList<ActivityNode>();
			activityNodes.add(nodes);
		}
		
		for (FlightInfo flight : listDelayFlight) {
			// verifica a origem e o tempo, busca se tem algum no de atividade jah criado nesse tempo
			// se nao tiver, cria no de atividade no aeroporto de origem e adiciona o arco de voo de output
			// se tiver, adiciona o arco de voo na lista de arcos de voo daquele no.
			
			int fltNbrId = flight.getDelayFlightNumberId();
			int originalFltNbrId = flight.getFlightNumberId();
			
			int airportDepartId = flight.getDepatureId();
			int timeDepart = flight.getDepartureTime();
			int typeDepart = 1;// if 0 is input, if 1 is output
			addActivityNode(activityNodes, fltNbrId, originalFltNbrId, airportDepartId, timeDepart, typeDepart);
			
			int airportArrivalId = flight.getArrivalId();
			int timeArrival = flight.getArrivalTime();
			int typeArrival = 0;// if 0 is input, if 1 is output
			addActivityNode(activityNodes, fltNbrId, originalFltNbrId, airportArrivalId, timeArrival, typeArrival);
		}
		
		return activityNodes;
	}

	protected void addActivityNode(List<LinkedList<ActivityNode>> activityNodes, int fltNbrId, int originalFltNbrId, int airportDepartId,
			int timeDepart, int type) {
		if (activityNodes.get(airportDepartId).size() > 0) {
			int i = -1;
			for (ActivityNode node : activityNodes.get(airportDepartId)) {
				if (node.getTime() <= timeDepart) {
					i++;
				}
			}
			if (i != -1 && activityNodes.get(airportDepartId).get(i).getTime() == timeDepart) { // adiciona novo voo ao no existente
				activityNodes.get(airportDepartId).get(i).addFlightArc(fltNbrId, originalFltNbrId, type);
			}else {// cria no e adiciona voo
				ActivityNode actDeparture = new ActivityNode();
				actDeparture.setAirportId(airportDepartId);
				actDeparture.setTime(timeDepart);
				actDeparture.addFlightArc(fltNbrId, originalFltNbrId, type);
				activityNodes.get(airportDepartId).add(i+1,actDeparture);// adiciona na posicao i, linkedlist empurra para frente
			}
		}else { // se nao tem nenhum no, adiciona o primeiro
			ActivityNode actDeparture = new ActivityNode();
			actDeparture.setAirportId(airportDepartId);
			actDeparture.setTime(timeDepart);
			actDeparture.addFlightArc(fltNbrId, originalFltNbrId, type);
			activityNodes.get(airportDepartId).add(actDeparture);
		}
	}

	protected float[][] calculateFlightCost(List<FlightInfo> listDelayFlight, List<CrewInfo> crewList) {
		float[][] flightCost = new float[crewList.size()][listDelayFlight.size()];
		for (int i = 0; i < crewList.size(); i++) {
			for (int j = 0; j < listDelayFlight.size(); j++) {
				flightCost[i][j] = 0;
				if (!listDelayFlight.get(j).isOriginalFlight())
					//flightCost[i][j] += CrewRecovery.delayCost;
					flightCost[i][j] += CrewRecovery.delayCost*listDelayFlight.get(j).getAmountOfDelay();
				if (!isOriginalSchedule(listDelayFlight.get(j).getFlightNumberId(),crewList.get(i).getCrewPath()))
					flightCost[i][j] += CrewRecovery.crewSwapCost;
			}
		}
		return flightCost;
	}

	protected boolean isOriginalSchedule(int flightNumberId, List<Integer> crewPath) {
		for (Integer flight : crewPath) {
			if (flight == flightNumberId) return true;
		}
		return false;
	}

	protected List<FlightInfo> generateDelayedFlights(List<FlightInfo> flightList, List<CrewInfo> crewList) {
		List<FlightInfo> delayedFlights = new ArrayList<>();
		
		
		
		if (this.delayType == 0) {
			int delayNbr = 0;
			for (FlightInfo originalFlight : flightList) {
				for (int i = 0; i < CrewRecovery.nGenFlight; i++) {
					FlightInfo flight = new FlightInfo();
					flight.setFlightNumberId(originalFlight.getFlightNumberId());
					flight.setDelayFlightNumberId(delayNbr);
					flight.setArrivalId(originalFlight.getArrivalId());
					flight.setDepatureId(originalFlight.getDepatureId());
					flight.setDepartureTime(originalFlight.getDepartureTime()+(CrewRecovery.maximumDelayTime/CrewRecovery.nGenFlight)*i);
					flight.setArrivalTime(originalFlight.getArrivalTime()+(CrewRecovery.maximumDelayTime/CrewRecovery.nGenFlight)*i);
					flight.setAmountOfDelay((CrewRecovery.maximumDelayTime/CrewRecovery.nGenFlight)*i);
					flight.setBlockTime(originalFlight.getBlockTime());
					if (i == 0) flight.setOriginalFlight(true);
					else flight.setOriginalFlight(false);
					delayedFlights.add(flight);
					delayNbr++;
				}
			}
		}else if (this.delayType == 1 || this.delayType == 2) {
			int startDelayTime;
			if (this.delayType == 1)
				startDelayTime = flightList.get(this.delayFlightNbr).getDepartureTime();
			else
				startDelayTime = crewList.get(this.delayCrewNbr).getStartTime() - this.deltaDelay; 
				// como a tripulação já teve ser horário ajustado, os voos precisam começar a serem duplicados no horário original da tripulação
			
			
			int delayNbr = 0;
			for (FlightInfo originalFlight : flightList) {
				if (originalFlight.getDepartureTime() >= startDelayTime) {
					for (int i = 0; i < CrewRecovery.nGenFlight; i++) {
						FlightInfo flight = new FlightInfo();
						flight.setFlightNumberId(originalFlight.getFlightNumberId());
						flight.setDelayFlightNumberId(delayNbr);
						flight.setArrivalId(originalFlight.getArrivalId());
						flight.setDepatureId(originalFlight.getDepatureId());
						flight.setDepartureTime(originalFlight.getDepartureTime()+(CrewRecovery.maximumDelayTime/CrewRecovery.nGenFlight)*i);
						flight.setArrivalTime(originalFlight.getArrivalTime()+(CrewRecovery.maximumDelayTime/CrewRecovery.nGenFlight)*i);
						flight.setAmountOfDelay((CrewRecovery.maximumDelayTime/CrewRecovery.nGenFlight)*i);
						flight.setBlockTime(originalFlight.getBlockTime());
						if (i == 0 && originalFlight.getFlightNumberId() != this.delayFlightNbr) flight.setOriginalFlight(true);
						else flight.setOriginalFlight(false);
						delayedFlights.add(flight);
						delayNbr++;
					}
				}else {
					FlightInfo flight = new FlightInfo();
					flight.setFlightNumberId(originalFlight.getFlightNumberId());
					flight.setDelayFlightNumberId(delayNbr);
					flight.setArrivalId(originalFlight.getArrivalId());
					flight.setDepatureId(originalFlight.getDepatureId());
					flight.setDepartureTime(originalFlight.getDepartureTime());
					flight.setArrivalTime(originalFlight.getArrivalTime());
					flight.setBlockTime(originalFlight.getBlockTime());
					flight.setOriginalFlight(true);
					delayedFlights.add(flight);
					delayNbr++;
				}
			}
		}
		
/*		for (FlightInfo flightInfo : delayedFlights) {
			printFlight(flightInfo);
		}
*/	
		return delayedFlights;
	}
	
/*	private void printFlight(FlightInfo flight) {
		System.out.println("OriginalFltNbr: "+flight.getFlightNumberId()+", DelayFltNbr: "+ flight.getDelayFlightNumberId()+
				", DepAirport: "+ flight.getDepatureId()+ ", DepTime: "+flight.getDepartureTime()/60.+", ArrAirport: "+ flight.getArrivalId()+
				", ArrTime: "+ flight.getArrivalTime()/60.+ ", blockTime: "+flight.getBlockTime()/60.+", Original: "+ flight.isOriginalFlight());
	}
*/
}
