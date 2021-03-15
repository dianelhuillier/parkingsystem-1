package com.parkit.parkingsystem.integration;

import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import junit.framework.Assert;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

	private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
	private static ParkingSpotDAO parkingSpotDAO;
	private static TicketDAO ticketDAO;
	private static DataBasePrepareService dataBasePrepareService;

	@Mock
	private static InputReaderUtil inputReaderUtil;

	@BeforeAll
	private static void setUp() throws Exception {
		parkingSpotDAO = new ParkingSpotDAO();
		parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
		ticketDAO = new TicketDAO();
		ticketDAO.dataBaseConfig = dataBaseTestConfig;
		dataBasePrepareService = new DataBasePrepareService();
	}

	@BeforeEach
	private void setUpPerTest() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
		dataBasePrepareService.clearDataBaseEntries();

	}

	@AfterAll
	private static void tearDown() {
	}

	@Test
	public void testParkingACar() throws Exception {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();
		// TODO: check that a ticket is actually saved in DB and Parking table is
		// updated with availability

		int parkingNumber = 0;

		Connection con = null;
		try {
			con = dataBaseTestConfig.getConnection();

			PreparedStatement ps = con.prepareStatement(DBConstants.TEST_TICKET);
			ps.setString(1, "ABCDEF");
			ResultSet rs = ps.executeQuery();
			Assert.assertTrue(rs.next());
			if (rs.next()) {
				parkingNumber = rs.getInt(1);
			}
			ps = con.prepareStatement(DBConstants.TEST_SPOT);
			ps.setInt(1, parkingNumber);
			ResultSet rsparking = ps.executeQuery();
			if (rsparking.next()) {
				Assert.assertEquals(rsparking.getInt(2), 0);
			}
		} finally {
			con.close();
		}
	}

	@Test
	public void testParkingLotExit() throws Exception {
		testParkingACar();
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

		Timestamp tsBefore = new Timestamp(System.currentTimeMillis() - 20000);
		parkingService.processExitingVehicle();
		Timestamp tsAfter = new Timestamp(System.currentTimeMillis() + 20000);
		// TODO: check that the fare generated and out time are populated correctly in
		// the database
		Connection con = null;
		try {
			con = dataBaseTestConfig.getConnection();
			PreparedStatement ps = con.prepareStatement(DBConstants.TEST_TIME);
			ps.setString(1, "ABCDEF");
			ResultSet rs = ps.executeQuery();

			// check out time
			Assert.assertTrue(rs.next());
			Assert.assertTrue(tsBefore.before(rs.getTimestamp(5)));
			Assert.assertTrue(tsAfter.after(rs.getTimestamp(5)));
			// check fare
			Assert.assertEquals(0.0, rs.getDouble(rs.findColumn("PRICE")));

		} finally {
			con.close();
		}
	}

}
