package analysis;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import mapper.MapperEmbFile;
import mapper.Mappers;
import model.ConnectionInfo;
import model.CrewInfo;
import model.FlightInfo;

public class CrewRecoveryTest__ {
	public List<CrewInfo> crewList;
	public List<FlightInfo> flightList;
	public List<ConnectionInfo> connList;

	@Before
	public void setUp() throws Exception {
		crewList = Mappers.mapFromFileToCrewInfo(Paths.get("crewInfo.txt"));		
		connList = Mappers.mapFromFileToConnectionInfo(Paths.get("airplaneInfo.txt"));
		flightList = Mappers.mapFromFileToFlightInfo(Paths.get("FlightInfo.txt"));
		fillCrewInfo(crewList,flightList,1300);
		
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testCalculateRecovery51Flights() throws IloException {
		CrewRecovery cr = new CrewRecovery();
		
		//cr.setFlightDelay(4, 60*3);
		
		IloCplex cplex = cr.calculateRecovery(crewList, connList, flightList, 8);
		
		System.out.println(cplex.getCplexStatus());
		
	}
	
	//Ex1 da monografia
	@Test
	public void testCalculateRecovery5Flights() throws IloException {
		CrewRecovery cr = new CrewRecovery();
		
		IloCplex cplex = cr.calculateRecovery(genericCrewList(), generateConnectionInfo(), genericFlightList(), 3);
		
		System.out.println(cplex.getCplexStatus());
		
	}
	
	//Ex1 da monografia
	@Test
	public void testCalculateRecoveryReserveCrew() throws IloException {
		CrewRecovery cr = new CrewRecovery();
		cr.crewDelay(2, 1000);
		cr.setReserveCrew(2, 10*60);
		IloCplex cplex = cr.calculateRecovery(genericCrewList(), generateConnectionInfo(), genericFlightList(), 3);
		
		System.out.println(cplex.getCplexStatus());
		
	}
	
	@Test
	public void testSpecificCrewDelay() throws IloException {
		CrewRecovery cr = new CrewRecovery();
	
		cr.crewDelay(2, 120);
		
		IloCplex cplex = cr.calculateRecovery(genericCrewList(), generateConnectionInfo(), genericFlightList(), 3);
		
		System.out.println(cplex.getCplexStatus());
	}
	
	@Test
	public void testSpecificNoCrew() throws IloException {
		CrewRecovery cr = new CrewRecovery();
	
		cr.crewDelay(2, 1000);
		
		IloCplex cplex = cr.calculateRecovery(genericCrewList(), generateConnectionInfo(), genericFlightList(), 3);
		
		System.out.println(cplex.getCplexStatus());
	}

	@Test
	public void testSpecificDelay() throws IloException {
		CrewRecovery cr = new CrewRecovery();
		//cr.setFlightDelay(4, 60*2); // problema com conexao
		//cr.setFlightDelay(1, 60*3); //cancela o voo 0
		cr.setFlightDelay(4, 60*3); // reajusta tudo
		
		IloCplex cplex = cr.calculateRecovery(genericCrewList(), generateConnectionInfo(), genericFlightList(), 3);
		
		System.out.println(cplex.getCplexStatus());
	}

	
	@Test
	public void testExample2() throws IloException {
		List<CrewInfo> ex2crewList = Mappers.mapFromFileToCrewInfo(Paths.get("crewInfo_ex2.txt"));		
		List<ConnectionInfo> ex2connectionList = Mappers.mapFromFileToConnectionInfo(Paths.get("airplaneInfo_ex2.txt"));
		List<FlightInfo> ex2flightList = Mappers.mapFromFileToFlightInfo(Paths.get("FlightInfo_ex2.txt"));
		fillCrewInfo(ex2crewList,ex2flightList,570);
		CrewRecovery cr = new CrewRecovery();
		//cr.crewDelay(2, 90);
		IloCplex cplex = cr.calculateRecovery(ex2crewList, ex2connectionList, ex2flightList, 4);
		System.out.println(cplex.getCplexStatus());
	}

	
	@Test
	public void testExample3() throws IloException {
		List<CrewInfo> ex3crewList = Mappers.mapFromFileToCrewInfo(Paths.get("crewInfo_ex3.txt"));		
		List<ConnectionInfo> ex3connectionList = Mappers.mapFromFileToConnectionInfo(Paths.get("airplaneInfo_ex3.txt"));
		List<FlightInfo> ex3flightList = Mappers.mapFromFileToFlightInfo(Paths.get("FlightInfo_ex3.txt"));
		fillCrewInfo(ex3crewList,ex3flightList,1300);
		CrewRecovery cr = new CrewRecovery();
		IloCplex cplex = cr.calculateRecovery(ex3crewList, ex3connectionList, ex3flightList, 5);
		System.out.println(cplex.getCplexStatus());
	}
	
	@Test
	public void testEmb() throws IloException {
		
		MapperEmbFile mapper = new MapperEmbFile(Paths.get("example_emb.txt"), 64, 232);
		fillCrewInfo(mapper.getCrewList(),mapper.getFlightList(),1300);
		CrewRecovery cr = new CrewRecovery();
		IloCplex cplex = cr.calculateRecovery(mapper.getCrewList(), new ArrayList<>(), mapper.getFlightList(), 31);
		System.out.println(cplex.getCplexStatus());
	}
	
	@Test
	public void testGenerateDelayedFlights() {
		//List<FlightInfo> generateDelayedFlights(List<FlightInfo> flightList)
		//List<FlightInfo> originalFlightList = genericFlightList();
		
	}
	
	private void fillCrewInfo(List<CrewInfo> crewList, List<FlightInfo> flightList, int dutyLimit) {
		//atribui demais variáveis da tripulação com base nos voos:
		for (int k = 0; k < crewList.size(); k++) {
			int firstScheduleFlight = crewList.get(k).getCrewPath().get(0);
			int endScheduleFlight =  crewList.get(k).getCrewPath().get(crewList.get(k).getCrewPath().size()-1);
			int startAirport = flightList.get(firstScheduleFlight).getDepatureId();
			int startTime = flightList.get(firstScheduleFlight).getDepartureTime();
			int endAirport = flightList.get(endScheduleFlight).getArrivalId();
			//int endTime = flightList.get(endScheduleFlight).getArrivalTime();
			crewList.get(k).setStartAirport(startAirport);
			crewList.get(k).setStartTime(startTime);
			crewList.get(k).setEndAirport(endAirport);
			crewList.get(k).setEndTime(startTime+dutyLimit);
			
		}
	}

	private List<FlightInfo> genericFlightList(){
		List<FlightInfo> flightList = new ArrayList<FlightInfo>();
		
		FlightInfo f0 = new FlightInfo();
		f0.setFlightNumberId(0);
		f0.setDepatureId(0);
		f0.setDepartureTime(9*60);
		f0.setArrivalId(1);
		f0.setArrivalTime(12*60);
		f0.setBlockTime(3*60);
		flightList.add(f0);
		
		FlightInfo f1 = new FlightInfo();
		f1.setFlightNumberId(1);
		f1.setDepatureId(1);
		f1.setDepartureTime(9*60);
		f1.setArrivalId(2);
		f1.setArrivalTime(11*60);
		f1.setBlockTime(2*60);
		flightList.add(f1);
		
		FlightInfo f2 = new FlightInfo();
		f2.setFlightNumberId(2);
		f2.setDepatureId(2);
		f2.setDepartureTime(13*60);
		f2.setArrivalId(0);
		f2.setArrivalTime(14*60);
		f2.setBlockTime(1*60);
		flightList.add(f2);
		
		FlightInfo f3 = new FlightInfo();
		f3.setFlightNumberId(3);
		f3.setDepatureId(1);
		f3.setDepartureTime(13*60);
		f3.setArrivalId(0);
		f3.setArrivalTime(16*60);
		f3.setBlockTime(3*60);
		flightList.add(f3);

		FlightInfo f4 = new FlightInfo();
		f4.setFlightNumberId(4);
		f4.setDepatureId(2);
		f4.setDepartureTime(10*60);
		f4.setArrivalId(1);
		f4.setArrivalTime(12*60);
		f4.setBlockTime(2*60);
		flightList.add(f4);
		
		return flightList;
	}
	
	private List <CrewInfo> genericCrewList(){
		List <CrewInfo> crewList = new ArrayList<>();
		
		CrewInfo c0 = new CrewInfo();
		c0.setCrewNbr(0);
		List<Integer> list0 = new ArrayList<>();
		list0.add(0);
		c0.setCrewPath(list0);
		c0.setStartAirport(0);
		c0.setEndAirport(1);
		c0.setStartTime(9*60);
		c0.setEndTime(9*60+10*60-30);
		crewList.add(c0);
		
		CrewInfo c1 = new CrewInfo();
		c1.setCrewNbr(0);
		List<Integer> list1 = new ArrayList<>();
		list1.add(1);
		list1.add(2);
		c1.setStartAirport(1);
		c1.setEndAirport(0);
		c1.setStartTime(9*60);
		c1.setEndTime(9*60+10*60-30);
		c1.setCrewPath(list1);
		crewList.add(c1);

		CrewInfo c2 = new CrewInfo();
		c2.setCrewNbr(0);
		List<Integer> list2 = new ArrayList<>();
		list2.add(4);
		list2.add(3);
		c2.setStartAirport(2);
		c2.setEndAirport(0);
		c2.setStartTime(10*60);
		c2.setEndTime(10*60+10*60-30);
		c2.setCrewPath(list2);
		crewList.add(c2);
		
		return crewList;
	}
	
	private List<ConnectionInfo> generateConnectionInfo(){
		List<ConnectionInfo> connectionList = new ArrayList<>();
		
		ConnectionInfo c0 = new ConnectionInfo();
		c0.setDepartureFlightId(0);
		c0.setArrivalFlightId(3);
		connectionList.add(c0);
		
		ConnectionInfo c1 = new ConnectionInfo();
		c1.setDepartureFlightId(1);
		c1.setArrivalFlightId(2);
		connectionList.add(c1);
		
		return connectionList;
	}
}
