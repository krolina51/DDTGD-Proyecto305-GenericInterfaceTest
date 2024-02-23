package postilion.realtime.genericinterface.translate.stream;

import postilion.realtime.sdk.message.*;
import postilion.realtime.sdk.message.stream.*;
import postilion.realtime.sdk.util.convert.Pack;

/**
 * Clase que define el formato del original data elements (bit 90) del Base-24
 * ATM transaction.
 */
public class AthOriginalDataElements extends StreamMessage {
	/** Constructor. Crea un objeto AthOriginalDataElements. */
	public AthOriginalDataElements() {
		super(MAX_SIZE, stream);
		putField(Field.FILLER, Pack.resize("", 10, '0', true));
	}

	/** Contenedor de Formatters. */
	private static StreamFormatterContainer stream = new StreamFormatterContainer();

	/** Campos. */
	public static class Field {
		public static final String ORIG_TRAN_TYPE = "Tran Type";
		public static final String ORIG_SEQ_NR = "Seq Nr";
		public static final String TRAN_DATE = "Tran Date";
		public static final String TRAN_TIME = "Tran Time";
		public static final String ORIG_DATE_CAPTURE = "Date Capture";
		public static final String FILLER = "Filler";
	}

	/** Longitud m�xima. */
	private static final int MAX_SIZE = 42;

	static {
		// StreamFormatterFields for the fields in Original Data Elements
		StreamFormatterFieldFixed origTranType = new StreamFormatterFieldFixed(Field.ORIG_TRAN_TYPE, Validator.getN(),
				4, ' ', false, true);
		StreamFormatterFieldFixed origSeqNr = new StreamFormatterFieldFixed(Field.ORIG_SEQ_NR, Validator.getN(), 12,
				' ', false, true);
		StreamFormatterFieldFixed tranDate = new StreamFormatterFieldFixed(Field.TRAN_DATE, Validator.getN(), 4, ' ',
				false, true);
		StreamFormatterFieldFixed tranTime = new StreamFormatterFieldFixed(Field.TRAN_TIME, Validator.getN(), 8, ' ',
				false, true);
		StreamFormatterFieldFixed dateCapture = new StreamFormatterFieldFixed(Field.ORIG_DATE_CAPTURE, Validator.getN(),
				4, ' ', false, true);
		StreamFormatterFieldFixed filler = new StreamFormatterFieldFixed(Field.FILLER, Validator.getN(), 10);
		stream.add(origTranType);
		stream.add(origSeqNr);
		stream.add(tranDate);
		stream.add(tranTime);
		stream.add(dateCapture);
		stream.add(filler);
	}
}