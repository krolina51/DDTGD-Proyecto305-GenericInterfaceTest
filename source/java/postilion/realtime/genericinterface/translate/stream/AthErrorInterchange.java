package postilion.realtime.genericinterface.translate.stream;

import postilion.realtime.sdk.message.*;
import postilion.realtime.sdk.message.stream.*;

/**
 * Clase que define el formato del AthErrorInterchange.
 */
public class AthErrorInterchange extends StreamMessage {
	/**Constructor. Formato del AthErrorInterchange. */
	public AthErrorInterchange() {
		super(MAX_SIZE, stream);
	}

	/** Contenedor de Formatters. */
	private static StreamFormatterContainer stream = new StreamFormatterContainer();

	/** Campos. */
	public static final class Field {
		public static final String CODE_ERROR = "error";
		public static final String DESCRIPTION = "description";
	}

	/** Longitud máxima. */
	private static final int MAX_SIZE = 44;

	static {
		StreamFormatterFieldFixed codeError = new StreamFormatterFieldFixed(
				Field.CODE_ERROR, Validator.getAns(), 4, ' ', true, true);
		StreamFormatterFieldFixed description = new StreamFormatterFieldFixed(
				Field.DESCRIPTION, Validator.getAns(), 40, ' ', true, true);
		stream.add(codeError);
		stream.add(description);
	}
}