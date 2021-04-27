package postilion.realtime.genericinterface.translate.stream;

import postilion.realtime.sdk.message.*;
import postilion.realtime.sdk.message.stream.*;

/**
 * Clase que define el formato del terminal data field format (bit 60) del
 * Base-24 ATM transaction.
 */
public class AthAtmTerminalData extends StreamMessage {
	/** Constructor. Crea un objeto AthAtmTerminalData. */
	public AthAtmTerminalData() {
		super(MAX_SIZE, stream);
	}

	/** Contenedor de Formatters. */
	private static StreamFormatterContainer stream = new StreamFormatterContainer();

	/** Campos. */
	public static class Field {
		public static final String TERM_OWNER_FIID = "ATM FIID";
		public static final String TERM_LOGICAL_NWK = "ATM Network";
		public static final String TERM_TIME_OFFSET = "ATM Time Offset";
	}

	/** Longitud máxima. */
	private static final int MAX_SIZE = 12;

	static {
		StreamFormatterFieldFixed termOwnerFiid = new StreamFormatterFieldFixed(
				Field.TERM_OWNER_FIID, Validator.getAns(), 4, ' ', true, true);
		StreamFormatterFieldFixed termLogicalNwk = new StreamFormatterFieldFixed(
				Field.TERM_LOGICAL_NWK, Validator.getAns(), 4, ' ', true, true);
		StreamFormatterFieldFixed termTimeOffset = new StreamFormatterFieldFixed(
				Field.TERM_TIME_OFFSET, Validator.getAns(), 4, ' ', true, true);
		stream.add(termOwnerFiid);
		stream.add(termLogicalNwk);
		stream.add(termTimeOffset);
	}
}