package postilion.realtime.genericinterface.translate.stream;

import postilion.realtime.sdk.message.*;
import postilion.realtime.sdk.message.stream.*;

/**
 * Esta clase implementa el Header del mensaje usado en todos los mensajes de
 * la interfaz con conexión ha la red Aval.
 */
public final class Header extends StreamMessage {
	/** Constructor. Crea un objeto Header. */
	public Header() {
		super(MAX_SIZE, stream);
	}

	/**
	 * Constructor. Crea un objeto Header.
	 * 
	 * @param hdr
	 *            Encabezado del mensaje.
	 */
	public Header(Header hdr) {
		this();
		this.copyFieldFrom(Field.ISO_LITERAL, hdr, Field.ISO_LITERAL);
		this.copyFieldFrom(Field.RELEASE_NUMBER, hdr, Field.RELEASE_NUMBER);
		this.copyFieldFrom(Field.STATUS, hdr, Field.STATUS);
		this.copyFieldFrom(Field.ORIGINATOR_CODE, hdr, Field.ORIGINATOR_CODE);
		this.copyFieldFrom(Field.RESPONDER_CODE, hdr, Field.RESPONDER_CODE);
	}

	/** Contenedor de Formatters. */
	private static StreamFormatterContainer stream = new StreamFormatterContainer();

	/** Longitud máxima. */
	protected static final int MAX_SIZE = 42;

	/** Campos. */
	public static class Field {
		public static final String ISO_LITERAL = "Iso";
		public static final String PRODUCT_INDICATOR = "Product";
		public static final String RELEASE_NUMBER = "Release";
		public static final String STATUS = "Status";
		public static final String ORIGINATOR_CODE = "Originator";
		public static final String RESPONDER_CODE = "Responder";
	}

	/** Indicador de producto. */
	public static class ProductIndicator {
		public static final String NETWORK = "00";
		public static final String ATM = "01";
		public static final String POS = "02";
	}

	/** Código de sistema. */
	public static class SystemCode {
		public static final String UNDETERMINED = "0";
		public static final String ATHHOST = "1";
		public static final String HOST = "5";
		public static final String INTERCHANGE_INTERFACE_PROCESS = "6";
		public static final String INTERCHANGE = "7";
	}
	

	/** Estado. */
	public static class Status {
		public static final String OK = "000";
	}
	
	/** Iso literal. */
	public static class Iso {
		public static final String ISO = "ISO";
	}
	
	/** Orriginator Code. */
	public static class OriginatorCode {
		public static final String UNO = "1";
		public static final String CINCO = "5";
	}
	
	/** Responder Code. */
	public static class ResponderCode {
		public static final String CERO = "0";
		public static final String UNO = "1";
		public static final String CINCO = "5";
	}

	/**
	 * Copia el encabezado.
	 * 
	 * @param hdr
	 *            Encabezado.
	 * @return Copia del encabezado.
	 */
	public Header copyHeader(Header hdr) {
		Header header = new Header();
		header.copyFieldFrom(Field.ISO_LITERAL, hdr, Field.ISO_LITERAL);
		header.copyFieldFrom(Field.RELEASE_NUMBER, hdr, Field.RELEASE_NUMBER);
		header.copyFieldFrom(Field.STATUS, hdr, Field.STATUS);
		header.copyFieldFrom(Field.ORIGINATOR_CODE, hdr, Field.ORIGINATOR_CODE);
		header.copyFieldFrom(Field.RESPONDER_CODE, hdr, Field.RESPONDER_CODE);
		return header;
	}

	static {
		StreamFormatterFieldFixed isoLiteral = new StreamFormatterFieldFixed(
				Field.ISO_LITERAL, Validator.getAns(), 3, ' ', true, true);
		StreamFormatterFieldFixed productIndicator = new StreamFormatterFieldFixed(
				Field.PRODUCT_INDICATOR, Validator.getN(), 2, ' ', false, true);
		StreamFormatterFieldFixed releaseNumber = new StreamFormatterFieldFixed(
				Field.RELEASE_NUMBER, Validator.getN(), 2, ' ', false, true);
		StreamFormatterFieldFixed status = new StreamFormatterFieldFixed(
				Field.STATUS, Validator.getN(), 3, ' ', false, true);
		StreamFormatterFieldFixed originatorCode = new StreamFormatterFieldFixed(
				Field.ORIGINATOR_CODE, Validator.getN(), 1, ' ', true, true);
		StreamFormatterFieldFixed responderCode = new StreamFormatterFieldFixed(
				Field.RESPONDER_CODE, Validator.getN(), 1, ' ', true, true);
		stream.add(isoLiteral);
		stream.add(productIndicator);
		stream.add(releaseNumber);
		stream.add(status);
		stream.add(originatorCode);
		stream.add(responderCode);
	}
}