package postilion.realtime.genericinterface.translate.stream;

import postilion.realtime.sdk.message.*;
import postilion.realtime.sdk.message.stream.*;

/**
 * Clase que define el formato del Additional Response Data (Campo 44) del
 * Base-24 ATM transaction.
 */
public class AthAtmAdditionalResponseData extends StreamMessage {
	/** Constructor. Crea un objeto AthAtmAdditionalResponseData. */
	public AthAtmAdditionalResponseData() {
		super(MAX_SIZE, stream);
	}

	/** Contenedor de Formatters. */
	private static StreamFormatterContainer stream = new StreamFormatterContainer();

	/** Campos. */
	public static class Field {
		public static final String USAGE_INDICATOR = "Usage";
		public static final String TOTAL_SIGN = "Sign";
		public static final String TOTAL_VALUE = "TotalValue";
		public static final String AVAILABLE_SIGN = "SignAvailable";
		public static final String AVAILABLE_VALUE = "AvailableValue";
	}

	/** Indicador de uso. */
	public static class UsageIndicator {
		public static final String TOTAL_VALUE_ONLY = "1";
		public static final String AVAILABLE_VALUE_ONLY = "2";
		public static final String BOTH_TOTAL_AVAILABLE = "3";

	}

	/** Signo. */
	public static class TotalSign {
		public static final String POSITIV = "0";
		public static final String NEGATIV = "-";

	}

	/** Signo. */
	public static class AvailableSign {
		public static final String POSITIV = "0";
		public static final String NEGATIV = "-";

	}

	/**
	 * Valida si es valor total.
	 * 
	 * @return True si es valor total.
	 */
	public boolean istTotalValueOnly() {
		return getField(Field.USAGE_INDICATOR).equals(
				UsageIndicator.TOTAL_VALUE_ONLY);
	}

	/**
	 * Valida si es valor disponible.
	 * 
	 * @return True si es valor disponible.
	 */
	public boolean isAvailableValueOnly() {
		return getField(Field.USAGE_INDICATOR).equals(
				UsageIndicator.AVAILABLE_VALUE_ONLY);
	}

	/**
	 * Valida si es valor total y disponible.
	 * 
	 * @return True si es valor total y disponible.
	 */
	public boolean isTotalAndAvailable() {
		return (getField(Field.USAGE_INDICATOR)
				.equals(UsageIndicator.BOTH_TOTAL_AVAILABLE));
	}

	/** Longitud máxima. */
	private static final int MAX_SIZE = 25;

	static {
		StreamFormatterFieldFixed usageIndicator = new StreamFormatterFieldFixed(
				Field.USAGE_INDICATOR, Validator.getAns(), 1, ' ', true, true);
		StreamFormatterFieldFixed totalSign = new StreamFormatterFieldFixed(
				Field.TOTAL_SIGN, Validator.getAns(), 1, ' ', true, true);
		StreamFormatterFieldFixed totalValue = new StreamFormatterFieldFixed(
				Field.TOTAL_VALUE, Validator.getAns(), 11, ' ', false, true);
		StreamFormatterFieldFixed availableSign = new StreamFormatterFieldFixed(
				Field.AVAILABLE_SIGN, Validator.getAns(), 1, ' ', true, true);
		StreamFormatterFieldFixed availableValue = new StreamFormatterFieldFixed(
				Field.AVAILABLE_VALUE, Validator.getAns(), 11, ' ', false, true);
		stream.add(usageIndicator);
		stream.add(totalSign);
		stream.add(totalValue);
		stream.add(availableSign);
		stream.add(availableValue);
	}
}