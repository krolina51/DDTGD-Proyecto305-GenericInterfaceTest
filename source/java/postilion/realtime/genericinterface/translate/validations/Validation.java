package postilion.realtime.genericinterface.translate.validations;

import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import postilion.realtime.sdk.message.bitmap.Iso8583.MsgType;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.util.XPostilion;

/**
 *
 * Esta clase permite realizar validaciones a los mensajes recibidos desde la
 * Red AVAL
 *
 * @author Cristian Cardozo
 *
 */
public class Validation {

	private static Validation validator;

	/**
	 * Constructor singelton
	 */
	private Validation() {
		if (validator != null) {
			throw new IllegalStateException("Singleton already initialized");
		}
	}

	/**
	 * Obtiene intancia de la clase. Para que sea singleton.
	 * 
	 * @return instancia de la clase.
	 */
	public static Validation getInstance() {
		if (validator == null) {
			validator = new Validation();
		}
		return validator;
	}

	SimpleDateFormat dateFormat = new SimpleDateFormat(FormatDate.MMDDhhmmss);

	/**
	 * Valida los valores suministrados contra un expresión regular.
	 * 
	 * @param regex
	 *            expresión regular
	 * @param fieldText
	 *            valor a comparar.
	 * @return true si el valor cumple la expresión regular.
	 */
	public boolean validateByRegex(String regex, String fieldText) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(fieldText);
		return matcher.matches();
	}

	/**
	 * Contruye el campo privado SwitchKey (Bit 2) hacia el TM.
	 * 
	 * @param msg
	 *            Mensaje.
	 * @return Retorna un objeto String data con el campo construido.
	 * @throws Exception
	 *             En caso de error.
	 */
	public String constructSwitchKey(Iso8583Post msg) throws XPostilion {
		return new StringBuilder().append(MsgType.toString(msg.getMsgType()))
				.append(msg.getField(Iso8583Post.Bit._011_SYSTEMS_TRACE_AUDIT_NR))
				.append(msg.getField(Iso8583Post.Bit._037_RETRIEVAL_REF_NR))
				.append(msg.getField(Iso8583Post.Bit._007_TRANSMISSION_DATE_TIME)).toString();
	}

//	/**************************************************************************************
//	 * Method that validates the customer account
//	 *
//	 * @param msgFromRemote
//	 * @return flatValidate Position 0 true or false Position 1 Error
//	 *         description
//	 *************************************************************************************/
//	@SuppressWarnings("unchecked")
//	public static ValidatedResult validateAccount(Base24Ath msgFromRemote) throws XPostilion {
//
//		HashMap<String, String> mapInfoAccount = null;
//
//		try {
//			
//			GenericInterface.udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),"ENTRO A validateAccount", "LOG"));
//			
//			mapInfoAccount = (HashMap<String, String>) new ConstructFieldMessage(params).constructAccount(msgFromRemote, null);
//			GenericInterface.sP_CODE = mapInfoAccount.get(TagNameStructuredData.PROCESSING_CODE) != null
//					? mapInfoAccount.get(TagNameStructuredData.PROCESSING_CODE)
//					: msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE);
//					
//			GenericInterface.udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),"GenericInterface.sP_CODE :" + GenericInterface.sP_CODE, "LOG"));
//
//			if (mapInfoAccount.size() > 0 && mapInfoAccount.get(TagNameStructuredData.CUSTOMER_NAME)
//
//					.equals(postilion.realtime.genericinterface.translate.database.DBHandler.Account.NO_CARD_RECORD)) {
//				return new ValidatedResult(false, mapInfoAccount.get(TagNameStructuredData.CUSTOMER_NAME),
//						ErrorMessages.FORMAT_ERROR_CODE,
//						Utils.getRspCodeToCardWithoutAccount(GenericInterface.sP_CODE));
//
//			} else if (mapInfoAccount.size() > 0 && mapInfoAccount.get("validation").equals(
//					postilion.realtime.genericinterface.translate.database.DBHandler.Account.NO_ACCOUNT_LINKED)) {
//
//				return new ValidatedResult(false, mapInfoAccount.get("validation"), ErrorMessages.FORMAT_ERROR_CODE,
//						Utils.getRspCodeToCardWithoutAccount(GenericInterface.sP_CODE));
//
//			} 
////			else if (msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13)
////					.equals(Constants.SEVEN) && msgFromRemote.getPosEntryMode().equals(Constants._010)) {
////				if (!mapInfoAccount.get(TagNameStructuredData.PROTECTED_CARD_CLASS).equals(Constants.NB)) {
////					return new ValidatedResult(false, ErrorMessages.INVALID_CLASS, ErrorMessages.PROHIBID_DEBITS,
////							ErrorMessages.INVALID_TRANSACTION);
////				}
////			}
//
//		} catch (Exception e) {
//			EventRecorder.recordEvent(new TryCatchException(
//					new String[] { "GenericInterface", "Method: ["
//							+ "]", e.getMessage() }));
//			EventRecorder.recordEvent(e);
//		}
//
//		return new ValidatedResult(true, General.VOIDSTRING, General.VOIDSTRING, null);
//	}

	/**
	 * Define los tipos de validación de la clase.
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public class ValidationType {
		public static final int CONSTANTS = 1;
		public static final int REGULAR = 2;
		public static final int VARIABLES = 3;
	}

	/**
	 * Define los tipos de formato de fecha que pueden tener los campos de
	 * fecha.
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public class FormatDate {
		public static final String MMDDhhmmss = "MMddHHmmss";
		public static final String hhmmss = "HHmmss";
		public static final String MMDD = "MMdd";
	}

	/**
	 * Define los canales de los cuales puede provenir un mensaje.
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public class Channel {
		public static final int ATM = 1;
		public static final int INTERNET = 2;
		public static final int OFICINAS = 3;
		public static final int POS = 4;
		public static final int IVR = 5;
		public static final int CNB = 7;
	}





	/**
	 * Define los mensaje de error, para la validaciones
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public static class ErrorMessages {
		public static final String[] CONSTANT_FIELD_ERROR = { " has an unexpected value. It should be [", "]" };
		public static final String REGULAR_FIELD_ERROR = " Error E/R Field";
		public static final String REGULAR_FIELD_ERROR_ER = " REVIZAR EN CONSOLA E/R O CONF p";
		public static final String FIELD_126_TOKENS_ERROR = " should contain at least 2 tokens.";
		public static final String FIELD_126_LENGTH_TOKEN_QT_ERROR = " should be greater than 32 in positions 16 - 21";
		public static final String FIELD_126_PROCESSING_CODE_ERROR = " Field 3 error in token ";
		public static final String FIELD_126_RETRIEVAL_REFERENCE_ERROR = " Field 37 error in token ";
		public static final String DATE_ERROR = " hasn't a format date";
		public static final String FIELD_054_ERROR = " have a mistake. Positions 1 - 10 should be greater than 0";
		public static final String FIELD_041_ERROR = " can not be 00 in positions 5 and 6 and atm code should be the same that is in field 37";
		public static final String FIELD_041_CHANNEL_ERROR = " Channel value in field 41 is not valid";
		public static final String FIELD_041_TERMINAL_ERROR = " Terminal value in field 41 is not valid";
		public static final String FIELD_103_TYPE_ERROR = " Type value in field 103, position 3 is not valid";
		public static final String FIELD_037_ERROR = " should have atm Code and  transaction's sequence number different of 0000";
		public static final String FIELD_035_ERROR = " the number of card is incorrect or expirity date is invalid.";
		public static final String FIELD_004_ERROR = " should be greater than 0";
		public static final String INVALID_CLASS = "ERROR se espera que la tarjeta sea clase CB.";
		public static final String IS_NOT_PRESENT_FIELD = " not present field ";
		public static final String END_STRING = ". ";
		public static final String FORMAT_ERROR_CODE = "0001";
		public static final String NO_CARD_RECORD = "8014";
		public static final String NO_CFG_MESSAGE = " Conf Not Found: ";
		public static final String F4_MUST_ZERO = " Field 4 must be 0 ";
		public static final String INVALID_AMOUNTS = "1994";
		public static final String PROHIBID_DEBITS = "3050";
		public static final String INVALID_TRANSACTION = "12";
		public static final String ERROR_AMOUNTS = "Ammounts in fields 4 and 54 first part should be the same and parts 2 and 3 of field 54 should be zero";
		public static final String ERROR_INSURANCE_AMOUNTS = "Ammount of fields 4 should be the same that field 54 part 1.  Also field 54 part 2 should be zero and part 3 should not be zero";
		public static final String ERROR_DONATION_AMOUNTS = "Ammount of fields 4 should be the same that field 54 part 1 and 2 added.  Also field 54 part 3 should be different of zero";
		public static final String ERROR_TERMINAL_VTL = " Terminal is not valid";
	}
}