package model;

import java.util.ArrayList;
import java.util.List;

public class ActivityNode {

	public class FlightType{
		int originalFlightNumber;
		int flightNumberId;
		int type; // if 0 is input, if 1 is output
		public void setFlightNumberId(int id) {
			this.flightNumberId = id;
		}
		public int getFlightNumberId() {
			return this.flightNumberId;
		}
		/**
		 * input if the arc arrives at activity node or output id the arc leaves activity node
		 * @param type - if 0 is input, if 1 is output
		 */
		public void setFlightArcType(int type) {
			this.type = type;
		}
		public boolean isInput() {
			if (type == 0)
				return true;
			return false;
		}
		public boolean isOutput() {
			if (type == 1)
				return true;
			return false;
		}
		public int getOriginalFlightNumber() {
			return originalFlightNumber;
		}
		public void setOriginalFlightNumber(int originalFlightNumber) {
			this.originalFlightNumber = originalFlightNumber;
		}
	}
	public class GroundType{
		int groundArcId;
		int type; // if 0 is input, if 1 is output
		public void setGroundArcId(int id) {
			this.groundArcId = id;
		}
		public int getGroundArcId() {
			return this.groundArcId;
		}
		public void setGroundArcType(int type) {
			this.type = type;
		}
		public boolean isInput() {
			if (type == 0)
				return true;
			return false;
		}
		public boolean isOutput() {
			if (type == 1)
				return true;
			return false;
		}
	}
	
	int time;
	int airportId;
	List<FlightType> listFlightArc = new ArrayList<>();
	List<GroundType> listGroundArc = new ArrayList<>();

	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public int getAirportId() {
		return airportId;
	}
	public void setAirportId(int airportId) {
		this.airportId = airportId;
	}
	public void addFlightArc(int flightNumberId, int originalFlgNbr, int type) {
		FlightType flt = new FlightType();
		flt.setFlightNumberId(flightNumberId);
		flt.setOriginalFlightNumber(originalFlgNbr);
		flt.setFlightArcType(type);
		this.listFlightArc.add(flt);
	}
	public void addGroundArc(int groundArcId, int type) {
		GroundType grd = new GroundType();
		grd.setGroundArcId(groundArcId);
		grd.setGroundArcType(type);
		this.listGroundArc.add(grd);
	}
	public List<FlightType> getListFlightArc(){
		return this.listFlightArc;
	}
	public List<GroundType> getListGroundArc(){
		return this.listGroundArc;
	}
}
