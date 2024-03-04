package postilion.realtime.genericinterface.translate.util;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import postilion.realtime.genericinterface.GenericInterface;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.library.common.util.constants.General;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.jdbc.JdbcManager;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.node.XNodeParameterValueInvalid;
import postilion.realtime.sdk.util.XPostilion;

/**
 * @author Juan Carlos Rodriguez
 *
 */
public class BussinesRules {
	/**
	 * Identifies the source channel of a transaction with a message Base24
	 * 
	 * @param msg           message in base24
	 * @param nameInterface
	 * @param udpClient
	 * @return CBC for Credibanco, CNB for "Corresponsal no bancario" , VTL
	 *         "Transferencia masivas", ATM for ATMs
	 * @throws XPostilion
	 *
	 */
	public static String channelIdentifier(Base24Ath msg, String nameInterface, Client udpClient) throws XPostilion {
		String channel;
		try {
			String p41 = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID);
			String red = p41.substring(4, 8);

			String[] terminalsID = { "8354", "8110", "9631", "9632" };
			String terminalId = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(4, 8);
			if (Arrays.stream(terminalsID).anyMatch(terminalId::equals)) {
				channel = Constants.Channels.VTL;
			} else if (nameInterface.toLowerCase().startsWith("credibanco")) {
				return Constants.Channels.CBCO;
			} else if (nameInterface.toLowerCase().contains("internet")) {
				return Constants.Channels.INTERNET;
			} else if (p41.substring(12, 13).equals("3") || p41.substring(12, 13).equals(" ")) {
				return Constants.Channels.OFC;
			} else if (p41.substring(12, 13).equals(Constants.General.SEVEN))
				return Constants.Channels.CNB;

			else if (p41.subSequence(0, 4).equals("0001") && p41.substring(12, 13).equals("5")) {
				channel = Constants.Channels.IVR;
				GenericInterface.getLogger().logLine("IVR");
			} else if ((p41.substring(0, 4).equals("0054") && p41.substring(12, 13).equals("1"))
					|| (red.equals("1004") || red.equals("1005") || red.equals("1006")))
				return Constants.Channels.ATM;
//			else if (p41.substring(12, 13).equals(Constants.General.FOUR))
//				channel = Constants.Channels.OFCAVAL;
			else
				channel = Constants.Channels.ATM;

		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(nameInterface, BussinesRules.class.getName(), e, "N/D",
					"channelIdentifier", udpClient);
			channel = null;
		}

		return channel;
	}

	/**
	 * 
	 * @param Base24Ath message in base24
	 * @return Indicador de aceptacion o de no preaprobado
	 * @throws XPostilion
	 * 
	 */
	public static String getIndicador_De_Aceptacion_O_De_No_Preaprobado(Base24Ath msg) throws XPostilion {

		String p48 = msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA);
		switch (p48.substring(p48.length() - 1)) {
		case "7":

			return "D";
		case "6":

			return "N";
		case "5":

			return "S";

		default:
			return "0";
		}
	}

	/**
	 * This method return consecutive and it is use for reverse
	 *
	 * @param atmId
	 * @param termId      id terminañ
	 * @param consSection configured in parameters
	 * @return String with the consecutive for reverse
	 */

	public static String getCalculateConsecutive(String atmId, String termId, String consSection, String nameInterface,
			Client udpClient) {

		String consecutive = null;
		Connection cn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;
		try {
			cn = JdbcManager.getDefaultConnection();
			stmt = cn.prepareCall("{call Get_Consecutive(?, ?, ?, ?)}");
			// stmt = cn.prepareCall ("{call Get_Consecutivo(?, ?, ?)}");
			stmt.setString(1, atmId);
			stmt.setString(2, termId);
			stmt.setString(3, consSection);
			stmt.registerOutParameter(4, java.sql.Types.VARCHAR);
			// stmt.registerOutParameter(3, java.sql.Types.VARCHAR);
			stmt.execute();
			// consecutive = stmt.getString(3);
			consecutive = stmt.getString(4);
			JdbcManager.commit(cn, stmt, rs);
		}

		catch (Exception e) {
			EventReporter.reportGeneralEvent(nameInterface, BussinesRules.class.getName(), e, "N/D",
					"getCalculateConsecutive", udpClient);
		} finally {
			try {
				JdbcManager.cleanup(cn, stmt, rs);
			} catch (SQLException e) {
				EventReporter.reportGeneralEvent(nameInterface, BussinesRules.class.getName(), e, "N/D",
						"getCalculateConsecutive", udpClient);
			}
		}

		return consecutive;
	}

	/**
	 * 
	 * Validate parameter for connection to udp server
	 * 
	 * @param cfgIpUdpServer server's ip
	 * @throws XNodeParameterValueInvalid if parameter is invalid
	 */
	public static String validateIpUdpServerParameter(String cfgIpUdpServer) throws XNodeParameterValueInvalid {
		String ip = null;
		if (cfgIpUdpServer != null && !cfgIpUdpServer.equals("0")) {
			if (Client.validateIp(cfgIpUdpServer)) {
				ip = cfgIpUdpServer;
			} else {
				EventRecorder.recordEvent(
						new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_IP_UDP_SERVER, cfgIpUdpServer));
				throw new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_IP_UDP_SERVER, cfgIpUdpServer);
			}
		} else {
			EventRecorder.recordEvent(
					new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_IP_UDP_SERVER, General.NULLSTRING));
			throw new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_IP_UDP_SERVER, General.NULLSTRING);
		}
		return ip;
	}

	/**
	 * 
	 * Validate parameter for connection to udp server
	 * 
	 * @param cfgPortUdpServer server's port
	 * @throws XNodeParameterValueInvalid if parameter is invalid
	 */
	public static String validatePortUdpServerParameter(String cfgPortUdpServer) throws XNodeParameterValueInvalid {
		String port = null;
		if (cfgPortUdpServer != null && !cfgPortUdpServer.equals("0")) {
			if (Client.validatePort(cfgPortUdpServer)) {
				port = cfgPortUdpServer;
			} else {
				EventRecorder.recordEvent(new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_PORT_UDP_SERVER,
						cfgPortUdpServer));
				throw new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_PORT_UDP_SERVER, cfgPortUdpServer);
			}
		} else {
			EventRecorder.recordEvent(
					new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_PORT_UDP_SERVER, General.NULLSTRING));
			throw new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_PORT_UDP_SERVER, General.NULLSTRING);
		}
		return port;
	}

}
