package mapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import model.CrewInfo;
import model.FlightInfo;

public class MapperEmbFile {
	private List<CrewInfo> crewList;
	private List<FlightInfo> flightList;

	public MapperEmbFile(Path path, int i, int j) {
		mapEmbFile(path, i, j);
	}

	public List<CrewInfo> getCrewList() {
		return crewList;
	}

	public void setCrewList(List<CrewInfo> crewList) {
		this.crewList = crewList;
	}

	public List<FlightInfo> getFlightList() {
		return flightList;
	}

	public void setFlightList(List<FlightInfo> flightList) {
		this.flightList = flightList;
	}

	private void mapEmbFile(Path path, int crNbr, int fltNbr) {
		crewList = new ArrayList<>(crNbr);
		for (int i=0; i < crNbr; i++) {
			CrewInfo cr = new CrewInfo();
			cr.setCrewNbr(i);
			crewList.add(cr);
		}
		flightList = new ArrayList<>(fltNbr);
		try {
			InputStream inFile = Files.newInputStream(path);
		    BufferedReader readerCrew = new BufferedReader(new InputStreamReader(inFile));
		    String line = readerCrew.readLine();
		    String crewAnterior = "";
		    while ((line = readerCrew.readLine()) != null) {
		    	String[] strArray = line.split(",");
		    	FlightInfo flt = new FlightInfo();
		    	flt.setOriginalFlight(true);
		    	flt.setFlightNumberId(Integer.parseInt(strArray[0])-1);
		    	flt.setDepatureId(Integer.parseInt(strArray[1].toLowerCase().replace("a", "")) - 1);
		    	flt.setArrivalId(Integer.parseInt(strArray[2].toLowerCase().replace("a", "")) - 1);
		    	flt.setDepartureTime(getTimeInMinutes(strArray[3]));
		    	flt.setArrivalTime(getTimeInMinutes(strArray[4]));
		    	if (flt.getArrivalTime() < flt.getDepartureTime())
		    		flt.setArrivalTime(flt.getArrivalTime()+24*60);
		    	flt.setBlockTime(flt.getArrivalTime() - flt.getDepartureTime());
		    	flightList.add(flt);
		        
		    	if (strArray[5].toLowerCase().contentEquals(crewAnterior)) { // se tripulação atual igual a linha anterior, add path
		    		int crewId = Integer.parseInt(strArray[5].toLowerCase().replace("k", ""))-1;
		    		crewList.get(crewId).getCrewPath().add(flt.getFlightNumberId());
		    	}else { // se não, cria tripulação e path inicial.
		    		List<Integer> fltPath = new ArrayList<>();
		    		fltPath.add(flt.getFlightNumberId());
		    		crewList.get(Integer.parseInt(strArray[5].toLowerCase().replace("k", ""))-1).setCrewPath(fltPath);
		    	}
		    	crewAnterior = strArray[5].toLowerCase();
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static int getTimeInMinutes(String string) {
		String[] aux = string.split(":");
		int hour = Integer.parseInt(aux[0]);
		int minutes = Integer.parseInt(aux[1]);
		return hour*60+minutes;
	}
	
}