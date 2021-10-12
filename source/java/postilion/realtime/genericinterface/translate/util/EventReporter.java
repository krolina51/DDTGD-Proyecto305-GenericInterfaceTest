package postilion.realtime.genericinterface.translate.util;

import postilion.realtime.genericinterface.eventrecorder.events.TryCatchException;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.sdk.eventrecorder.EventRecorder;

public class EventReporter {

	/**
	 * This method report an event to the framework, OS and monitor UDP
	 * 
	 * @param nameInterface name of Interface where the event occurs
	 * @param className     name of class where the event occurs
	 * @param e             exception throws by the incident
	 * @param field37       value of field 37 of message that present the event
	 * @param method        name of method where the event occurs
	 * @param udpClient     Client object to report to monitor UDP.
	 */
	public static void reportGeneralEvent(String nameInterface, String className, Exception e, String field37,
			String method, Client udpClient) {
		reportGeneralEvent(nameInterface, className, e, field37, method, udpClient, "");
	}

	/**
	 * This method report an event to the framework, OS and monitor UDP with an
	 * additional message
	 * 
	 * @param nameInterface name of Interface where the event occurs
	 * @param className     name of class where the event occurs
	 * @param eexception    throws by the incident
	 * @param field37       value of field 37 of message that present the event
	 * @param method        name of method where the event occurs
	 * @param udpClient     Client object to report to monitor UDP.
	 * @param addMsg        message to attach
	 */
	public static void reportGeneralEvent(String nameInterface, String className, Exception e, String field37,
			String method, Client udpClient, String addMsg) {
		EventRecorder.recordEvent(new TryCatchException(
				new String[] { nameInterface, className, "Exception in Method :[" + method + "]\n" + addMsg + "\n",
						Utils.getStringMessageException(e), field37 }));
		EventRecorder.recordEvent(e);
		if (udpClient != null) {
			udpClient.sendData(Client.getMsgKeyValue(field37,
					"Exception in Method: " + method + "\n" + addMsg + "\n" + Utils.getStringMessageException(e), "ERR",
					nameInterface));
		}
	}

}
