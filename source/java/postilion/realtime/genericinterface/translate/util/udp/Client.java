package postilion.realtime.genericinterface.translate.util.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import postilion.realtime.genericinterface.GenericInterface;
import postilion.realtime.genericinterface.eventrecorder.events.TryCatchException;
import postilion.realtime.genericinterface.translate.util.EventReporter;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.genericinterface.translate.validations.Validation;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.Iso8583.MsgType;
import postilion.realtime.sdk.message.bitmap.Iso8583.RspCode;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.util.XPostilion;
import postilion.realtime.sdk.util.convert.Transform;

/**
 * This class deifines a basic UDP client
 * 
 * @author Cristian Cardozo
 *
 */
public class Client {

	InetAddress ipAddress;
	int port;
	DatagramSocket socket;

	public Client() {

	}

	public Client(String ipAddress, String port, String portOut) {

		if (!ipAddress.equals("0") && !port.equals("0")) {
			try {

				if (validateIp(ipAddress))
					this.ipAddress = InetAddress.getByName(ipAddress);
				else
					throw new Exception("IP parameter for server UDP, is not a IP valid");

				if (validatePort(port))
					this.port = Integer.valueOf(port);
				else
					throw new Exception("Port parameter for server UDP, is not a Port valid");

			} catch (Exception e) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { "Unknown", Client.class.getName(),
						"Constructor: [Client]", Utils.getStringMessageException(e), "N/A" }));
				EventRecorder.recordEvent(e);
				GenericInterface.getLogger()
						.logLine("Exception in Constructor:  Client: " + Utils.getStringMessageException(e));
			}
		}
	}

	public Client(String ipAddress, String port, String portOut, String nameInterface) {
		if (!ipAddress.equals("0") && !port.equals("0")) {
			try {

				if (validateIp(ipAddress))
					this.ipAddress = InetAddress.getByName(ipAddress);
				else
					throw new Exception("IP parameter for server UDP, is not a IP valid");

				if (validatePort(port))
					this.port = Integer.valueOf(port);
				else
					throw new Exception("Port parameter for server UDP, is not a Port valid");

				if (validatePort(portOut))
					this.socket = new DatagramSocket(Integer.valueOf(portOut));
				else
					throw new Exception("Port Out parameter for server UDP, is not a Port valid");

			} catch (Exception e) {
				EventReporter.reportGeneralEvent(nameInterface, Client.class.getName(), e, "N/D", "Client", null);
				GenericInterface.getLogger()
						.logLine("Exception in Constructor:  Client: " + Utils.getStringMessageException(e));
				GenericInterface.getLogger()
						.logLine("Exception in Constructor:  Client: " + Utils.getStringMessageException(e));
			}
		}
	}
	
	public void close(){
		if(this.socket != null)
			this.socket.close();
		this.socket = null;
	}

	public void setIpAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public InetAddress getIpAddress() {
		return this.ipAddress;
	}

	public int getPort() {
		return this.port;
	}

	/**
	 * Valida si la información en el archivo es una ip.
	 * 
	 * @param ip
	 * @return true si es un ip
	 */
	static public boolean validateIp(String ip) {
		boolean ipIsOk = false;
		Validation validator = Validation.getInstance();
		if (validator.validateByRegex(IP_REGEX, ip)) {
			ipIsOk = true;
		} else {
			GenericInterface.getLogger().logLine("IP parameter for server UDP, is not a IP valid");
		}
		return ipIsOk;
	}

	/**
	 * Valida si la información en el archivo es un puerto.
	 * 
	 * @param port
	 * @return true si es un puerto
	 */
	static public boolean validatePort(String port) {
		boolean portIsOk = false;
		try {
			int configPort = Integer.parseInt(port);
			if (0 < configPort && configPort < 65536) {
				portIsOk = true;
			} else {
				GenericInterface.getLogger().logLine("PORT parameter is not a value valid for a port.");
			}
		} catch (NumberFormatException e) {
			GenericInterface.getLogger().logLine("PORT parameter is not a value valid for a port.");
		}
		return portIsOk;
	}

	/**
	 * Open a socket to send data over UDP protocol
	 * 
	 * @param data to send
	 */
	public void sendData(byte[] data) {
		try {
			DatagramPacket request = new DatagramPacket(data, data.length, ipAddress, port);
//			DatagramPacket request = new DatagramPacket(data, data.length, ipAddress,
//					port + (int) (Math.random() * 10));
			this.socket.send(request);

		} catch (IOException e) {
			GenericInterface.getLogger()
					.logLine("Exception in Method:  sendData: " + Utils.getStringMessageException(e));
		} 
	}

	/**
	 * Open a socket to send data over UDP protocol
	 * 
	 * @param data            to send
	 * @param waitForResponse
	 * @throws XPostilion
	 */
	public Iso8583Post sendMsg(Iso8583Post msg, String interchangeName) throws XPostilion {
		Iso8583Post msgResponse = new Iso8583Post();
		String msgIncoming = new String();
		DatagramSocket socket = null;

		try {

			byte[] data = Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					Transform.fromBinToHex(Transform.getString(msg.toMsg())), "ISO", interchangeName);

			try {
				socket = new DatagramSocket();
				socket.setSoTimeout(5000);
				String p11 = msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR);
//				DatagramPacket request = new DatagramPacket(data, data.length, ipAddress, port);
				DatagramPacket request = new DatagramPacket(data, data.length, ipAddress,
						50000 + Integer.parseInt(p11.substring(p11.length() - 1)));
				socket.send(request);
				byte[] bufer = new byte[4172];// 4072
				DatagramPacket respuesta = new DatagramPacket(bufer, bufer.length);
				socket.receive(respuesta);
				String dataB64 = new String(respuesta.getData()).trim();
				GenericInterface.getLogger().logLine("data incoming: " + dataB64);
				msgIncoming = new String(Base64.getDecoder().decode(dataB64));
				socket.close();
			} catch (SocketTimeoutException e) {
				msgIncoming = "TIMEOUT";
			} catch (IOException e) {
				EventReporter.reportGeneralEvent(interchangeName, Client.class.getName(), e,
						msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "sendMsg", null);
				GenericInterface.getLogger()
						.logLine("Exception in Method:  sendMsg: " + Utils.getStringMessageException(e));
			} finally {
				if (socket != null)
					socket.close();
			}
			if (msgIncoming.equals("TIMEOUT")) {
				msgResponse = msg;
				msgResponse.putMsgType(MsgType.getResponse(msg.getMsgType()));
				msgResponse.putField(Iso8583Post.Bit._038_AUTH_ID_RSP, "000000");
				msgResponse.putField(Iso8583Post.Bit._039_RSP_CODE, RspCode._91_ISSUER_OR_SWITCH_INOPERATIVE);
			} else {
				GenericInterface.getLogger().logLine("Msg incoming: " + msgIncoming);
				msgResponse.fromMsg(Transform.fromHexToBin(msgIncoming));
				GenericInterface.getLogger().logLine("Msg response: " + msgResponse);
			}
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(interchangeName, Client.class.getName(), e,
					"N/D", "sendMsg", null);
			GenericInterface.getLogger()
					.logLine("Exception in Method:  sendMsg: " + Utils.getStringMessageException(e));
		} catch (IllegalArgumentException e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { "Unknown", Client.class.getName(),
					"Method: [sendMsg]", Utils.getStringMessageException(e), "N/A" }));
			EventRecorder.recordEvent(e);
			GenericInterface.getLogger()
					.logLine("Exception in Method:  sendMsg: " + Utils.getStringMessageException(e));
			msgResponse = msg;
			msgResponse.putMsgType(MsgType.getResponse(msg.getMsgType()));
			msgResponse.putField(Iso8583Post.Bit._038_AUTH_ID_RSP, "000000");
			msgResponse.putField(Iso8583Post.Bit._039_RSP_CODE, RspCode._06_ERROR);
		}
		return msgResponse;
	}

	
	static final String IP_REGEX = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

	public static byte[] getMsgKeyValue(String p37, String value, String type, String nameInterface) {

//		boolean msg2 = false;
		String key = new String();
//		if (type.equals("B24")) {
//			Base24Ath msgb24Ath = new Base24Ath(null);
//			try {
//				msgb24Ath.fromMsg(Transform.fromHexToBin(value).getBytes());
//				key = "V2" + type + msgb24Ath.getField(Iso8583.Bit._003_PROCESSING_CODE)
//						+ msgb24Ath.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR)
//						+ msgb24Ath.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) + nameInterface;
//				String msglog = msgb24Ath.toString();
//				String msghex = value;
//				key = key + "|" + msglog + "|" + msghex;
//				msg2 = true;
//			} catch (XPostilion e) {
//				GenericInterface.getLogger().logLine(Utils.getStringMessageException(e));
//			}
//
//		}
//		if (type.equals("ISO")) {
//			Iso8583Post msgIso = new Iso8583Post();
//			try {
//				msgIso.fromMsg(Transform.fromHexToBin(value).getBytes());
//				key = "V2" + type + msgIso.getField(Iso8583.Bit._003_PROCESSING_CODE)
//						+ msgIso.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR)
//						+ msgIso.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) + nameInterface;
//				String msglog = msgIso.toString();
//				String msghex = value;
//				key = key + "|" + msglog + "|" + msghex;
//				msg2 = true;
//				GenericInterface.getLogger().logLine("msg2 "+msg2);
//			} catch (XPostilion e) {
//				GenericInterface.getLogger().logLine(Utils.getStringMessageException(e));
//			}
//
//		}
//		GenericInterface.getLogger().logLine(key);
//		if (msg2) {
//			sendDataFixed(key.getBytes());
//		}
		key = type + ":" + nameInterface + "_" + p37 + "_"
				+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ":";
		String lKey = String.valueOf(key.length());
		String llKey = String.valueOf(lKey.length());

		String lValue = String.valueOf(value.length());
		String llValue = String.valueOf(lValue.length());

		String msg = llKey + lKey + key + llValue + lValue + value;
		return (Base64.getEncoder().encodeToString(msg.getBytes())).getBytes();
	}

}
