package model;

/**
 * This class holds information regarding one specific flight arc
 * @author MaíraTayana
 *
 */
public class FlightInfo {

	/**
	 * Flight Number Id, designated to one flight.
	 */
	int flightNumberId;
	/**
	 * Flight Number Id, designated to one flight.
	 */
	int delayFlightNumberId;
	
	boolean originalFlight;
	
	int amountOfDelay;

	/**
	 * Departure airport id
	 */
	int depatureId;
	/**
	 * Arival airport id
	 */
	int arrivalId;
	/**
	 * Departure time, in minutes, consider that the day starts at 00:00 with 0min and finish at 23:59 with 1439min
	 * If your flight starts at 13:30, your departure time will be 13*60+30 = 810
	 */
	int departureTime;
	/**
	 * Arrival time, in minutes, consider that the day starts at 00:00 with 0min and finish at 24:00 with 1440min
	 * If your flight finishes at 13:30, your departure time will be 13*60+30 = 810
	 */
	int arrivalTime;
	/**
	 * Difference between arrivalTime and departureTime, in minutes
	 */
	int blockTime;
	public int getFlightNumberId() {
		return flightNumberId;
	}
	public void setFlightNumberId(int flightNumberId) {
		this.flightNumberId = flightNumberId;
	}
	public int getDelayFlightNumberId() {
		return delayFlightNumberId;
	}
	public void setDelayFlightNumberId(int delayFlightNumberId) {
		this.delayFlightNumberId = delayFlightNumberId;
	}
	public int getDepatureId() {
		return depatureId;
	}
	public void setDepatureId(int depatureId) {
		this.depatureId = depatureId;
	}
	public int getArrivalId() {
		return arrivalId;
	}
	public void setArrivalId(int arrivalId) {
		this.arrivalId = arrivalId;
	}
	public int getDepartureTime() {
		return departureTime;
	}
	public void setDepartureTime(int departureTime) {
		this.departureTime = departureTime;
	}
	public int getArrivalTime() {
		return arrivalTime;
	}
	public void setArrivalTime(int arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	public int getBlockTime() {
		return blockTime;
	}
	public void setBlockTime(int blockTime) {
		this.blockTime = blockTime;
	}
	public boolean isOriginalFlight() {
		return originalFlight;
	}
	public void setOriginalFlight(boolean originalFlight) {
		this.originalFlight = originalFlight;
	}
	public int getAmountOfDelay() {
		return amountOfDelay;
	}
	public void setAmountOfDelay(int amountOfDelay) {
		this.amountOfDelay = amountOfDelay;
	}
	
	
}
