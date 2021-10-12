package postilion.realtime.genericinterface.translate.util;

import postilion.realtime.genericinterface.translate.util.udp.Client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Esta clase permite hacer registro de mensajes en un archivo de texto, para
 * hacer trazabilidad dentro del código
 * 
 * @author Javier Flores
 *
 */
public class Logger {

	public static final String filePath = "C:\\temp\\logs\\IsoTranslateB24.txt";

	/**
	 * Escribe un mensaje en un archivo de texto.
	 * 
	 * @param msg a escribir en el archivo.
	 */
	public static void logLine(String msg, String nameInterface, Client udpClient) {

		StringBuilder sb = new StringBuilder();
		sb.append(Constants.OPEN_BRAKET).append(new Date().toString()).append(Constants.CLOSE_BRAKET);
		sb.append(msg);
		sb.append(Constants.LINE_BREAK);
		BufferedWriter bf = null;
		try {
			bf = new BufferedWriter(new FileWriter(filePath, true));
			bf.append(sb.toString());
		} catch (IOException e) {
			EventReporter.reportGeneralEvent(nameInterface, Logger.class.getName(), e, "N/D",
					"logLine", udpClient);
		} finally {
			if (bf != null)
				try {
					bf.close();
				} catch (IOException e) {
					EventReporter.reportGeneralEvent(nameInterface, Logger.class.getName(), e, "N/D",
							"logLine", udpClient);					
				}
		}

	}

	/**
	 * 
	 * Define constantes utiles para la clase
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public class Constants {

		public final static String OPEN_BRAKET = "[";
		public final static String CLOSE_BRAKET = "]";
		public final static String LINE_BREAK = "\n";

	}
}
