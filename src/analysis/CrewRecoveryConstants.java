package analysis;

public class CrewRecoveryConstants {
	
	public  int maximumDelayTime;
	public int nGenFlight;
	public int delayCost;
	public int groundCostPerMin;
	public int crewDeadHeadCost;
	public int crewDestinationInconsistencyCost;
	public int crewSwapCost;
	public int flightCancelationCost;
	public int minimumCrewConnectionTime;
	public int flightLimit;
	public int dutyLimit;
	public int landingOff;

	public CrewRecoveryConstants(int maximumDelayTime, int nGenFlight, int delayCost, int groundCostPerMin,
			int crewDeadHeadCost, int crewDestinationInconsistencyCost, int crewSwapCost, int flightCancelationCost,
			int minimumCrewConnectionTime, int flightLimit, int dutyLimit, int landingOff) {
		this.maximumDelayTime = maximumDelayTime;
		this.nGenFlight = nGenFlight;
		this.delayCost = delayCost;
		this.groundCostPerMin = groundCostPerMin;
		this.crewDeadHeadCost = crewDeadHeadCost;
		this.crewDestinationInconsistencyCost = crewDestinationInconsistencyCost;
		this.crewSwapCost = crewSwapCost;
		this.flightCancelationCost = flightCancelationCost;
		this.minimumCrewConnectionTime = minimumCrewConnectionTime;
		this.flightLimit = flightLimit;
		this.dutyLimit = dutyLimit;
		this.landingOff = landingOff;
	}
}