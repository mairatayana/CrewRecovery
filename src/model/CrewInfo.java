package model;

import java.util.List;

public class CrewInfo {

	/**
	 * Id for a specific crew
	 */
	int crewNbr;
	/**
	 * List with the airport schedule for the crewId
	 * For example, if the crew goes from airport 2, to 5 and then to 3, the list will be
	 * crewPath = {2,5,3}
	 */
	List<Integer> crewPath;
	
	int startAirport;
	int startTime;
	int endAirport;
	int endTime;
	
	public int getCrewNbr() {
		return crewNbr;
	}
	public void setCrewNbr(int crewNbr) {
		this.crewNbr = crewNbr;
	}
	public List<Integer> getCrewPath() {
		return crewPath;
	}
	public void setCrewPath(List<Integer> crewPath) {
		this.crewPath = crewPath;
	}
	public int getStartAirport() {
		return startAirport;
	}
	public void setStartAirport(int startAirport) {
		this.startAirport = startAirport;
	}
	public int getStartTime() {
		return startTime;
	}
	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}
	public int getEndAirport() {
		return endAirport;
	}
	public void setEndAirport(int endAirport) {
		this.endAirport = endAirport;
	}
	public int getEndTime() {
		return endTime;
	}
	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}
	
}
