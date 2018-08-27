package mapper;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import model.ConnectionInfo;
import model.CrewInfo;
import model.FlightInfo;

public class Mappers {

	public static List<CrewInfo> mapFromFileToCrewInfo(Path path){
		List<CrewInfo> crew = new ArrayList<>();
		try {
			InputStream inCrew = Files.newInputStream(path);
		    BufferedReader readerCrew = new BufferedReader(new InputStreamReader(inCrew));
		    String line = readerCrew.readLine();
		    while ((line = readerCrew.readLine()) != null) {
		        String[] strArray = line.split(",");
		        String[] strAux = strArray[1].split("–");
	        	CrewInfo crew1 = new CrewInfo();
	        	crew1.setCrewNbr(Integer.parseInt(strArray[0].toLowerCase().replace("k", ""))-1);
	        	List<Integer> crewPath = new ArrayList<>();
			    for (int i = 0; i < strAux.length; i++) {
		        	int flight = Integer.parseInt(strAux[i].toLowerCase().replace("f", ""))-1;
		        	crewPath.add(flight);
		        }
			    crew1.setCrewPath(crewPath);
			    crew.add(crew1);
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return crew;
	}
	
	public static List<ConnectionInfo> mapFromFileToConnectionInfo(Path path){
		List<ConnectionInfo> connections = new ArrayList<>();
		try {
			InputStream inAirplane = Files.newInputStream(path);
		    BufferedReader readerAirplane = new BufferedReader(new InputStreamReader(inAirplane));
		    String line = readerAirplane.readLine();
		    while ((line = readerAirplane.readLine()) != null) {
		        String[] strArray = line.split(",");
		        String[] strAux = strArray[1].split("–");
		        for (int i = 0; i < strAux.length-1; i++) {
		        	ConnectionInfo conn = new ConnectionInfo();
		        	conn.setDepartureFlightId(Integer.parseInt(strAux[i].toLowerCase().replace("f", ""))-1);
		        	conn.setArrivalFlightId(Integer.parseInt(strAux[i+1].toLowerCase().replace("f", ""))-1);
		        	connections.add(conn);
		        }
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return connections;
	}
	
	public static List<FlightInfo> mapFromFileToFlightInfo(Path path){
		List<FlightInfo> flightsInfo = new ArrayList<FlightInfo>();
		
		try {			
			InputStream inFlight = Files.newInputStream(path);
		
		    BufferedReader readerFlight = new BufferedReader(new InputStreamReader(inFlight));
		    String line = readerFlight.readLine();
		    while ((line = readerFlight.readLine()) != null) {
		        String[] strArray = line.split(",");
		        FlightInfo flight = new FlightInfo();
		        flight.setFlightNumberId(Integer.parseInt(strArray[0].toLowerCase().replace("f", ""))-1);
		        flight.setDepatureId(Integer.parseInt(strArray[1].toLowerCase().replace("a", ""))-1);
		        flight.setArrivalId(Integer.parseInt(strArray[2].toLowerCase().replace("a", ""))-1);
		        flight.setDepartureTime(getTimeInMinutes(strArray[3]));
		        flight.setArrivalTime(getTimeInMinutes(strArray[4]));
		        flight.setBlockTime(Integer.parseInt(strArray[5]));
		        if (getTimeInMinutes(strArray[3]) + Integer.parseInt(strArray[5]) !=  getTimeInMinutes(strArray[4]))
		        	flight.setArrivalTime(getTimeInMinutes(strArray[3]) + Integer.parseInt(strArray[5]));
		        flightsInfo.add(flight);
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return flightsInfo;
	}

	protected static int getTimeInMinutes(String string) {
		String[] aux = string.split(":");
		int hour = Integer.parseInt(aux[0]);
		int minutes = Integer.parseInt(aux[1]);
		return hour*60+minutes;
	}
	
}
