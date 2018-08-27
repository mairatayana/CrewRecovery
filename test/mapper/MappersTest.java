package mapper;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import model.ConnectionInfo;
import model.CrewInfo;
import model.FlightInfo;

public class MappersTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMapFromFileToCrewInfo() {
		List<CrewInfo> crew = Mappers.mapFromFileToCrewInfo(Paths.get("resource"+File.separatorChar+"crewInfo.txt"));
		assertEquals(20, crew.size());
		assertEquals(2, crew.get(2).getCrewNbr());
		assertEquals(0, (int)crew.get(2).getCrewPath().get(0));
		assertEquals(12, (int)crew.get(2).getCrewPath().get(1));
		assertEquals(18, (int)crew.get(2).getCrewPath().get(2));
	}
	
	@Test
	public void testMapFromFileToConnectionInfo() {
		List<ConnectionInfo> connections = Mappers.mapFromFileToConnectionInfo(Paths.get("resource"+File.separatorChar+"airplaneInfo.txt"));
		assertEquals(37, connections.size());
		assertEquals(8, connections.get(2).getDepartureFlightId());
		assertEquals(14, connections.get(2).getArrivalFlightId());
	}
	
	@Test
	public void testMapFromFileToFlightInfo() {
		List<FlightInfo> flights = Mappers.mapFromFileToFlightInfo(Paths.get("resource"+File.separatorChar+"FlightInfo.txt"));
		assertEquals(51, flights.size());
		assertEquals(2, flights.get(2).getFlightNumberId());
		assertEquals(2, flights.get(2).getDepatureId());
		assertEquals(1, flights.get(2).getArrivalId());
		assertEquals(Mappers.getTimeInMinutes("8:00"), flights.get(2).getDepartureTime());
		assertEquals(Mappers.getTimeInMinutes("9:00"), flights.get(2).getArrivalTime());
		assertEquals(60, flights.get(2).getBlockTime());
	}

	@Test
	public void testGetTimeInMinutes() {
		assertEquals(0, Mappers.getTimeInMinutes("00:00"));
		assertEquals(810, Mappers.getTimeInMinutes("13:30"));
		assertEquals(1439, Mappers.getTimeInMinutes("23:59"));
	}

}
