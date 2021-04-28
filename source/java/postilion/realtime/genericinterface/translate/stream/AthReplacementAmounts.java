package postilion.realtime.genericinterface.translate.stream;

import postilion.realtime.sdk.message.*;
import postilion.realtime.sdk.message.stream.*;
import postilion.realtime.sdk.util.convert.Pack;

/**
 * Clase que define el formato del replacement amounts field format (bit 95) del
 * Base-24 ATM transaction.
 */
public final class AthReplacementAmounts extends StreamMessage {
	/** Constructor. Crea un objeto AthOriginalDataElements. */
	public AthReplacementAmounts() {
		super(MAX_SIZE, stream);
		putField(Field.FILLER, Pack.resize("", 30, '0', true));
	}

	/** Contenedor de Formatters. */
	private static StreamFormatterContainer stream = new StreamFormatterContainer();

	/** Campos. */
	public static final class Field {
		public static final String ACTUAL_TRAN_AMOUNT = "actual amt";
		public static final String FILLER = "filler";
	}

	/** Longitud máxima. */
	private static final int MAX_SIZE = 42;

	static {
		StreamFormatterFieldFixed actualTranAmount = new StreamFormatterFieldFixed(
				Field.ACTUAL_TRAN_AMOUNT, Validator.getN(), 12, ' ', true, true);
		StreamFormatterFieldFixed filler = new StreamFormatterFieldFixed(
				Field.FILLER, Validator.getAn(), 30);
		stream.add(actualTranAmount);
		stream.add(filler);
	}
}