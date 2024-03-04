package postilion.realtime.genericinterface.translate.bitmap;

import postilion.realtime.sdk.message.bitmap.*;
import postilion.realtime.genericinterface.translate.stream.AthAtmAdditionalResponseData;
import postilion.realtime.genericinterface.translate.stream.AthAtmTerminalData;
import postilion.realtime.genericinterface.translate.stream.AthErrorInterchange;
import postilion.realtime.genericinterface.translate.stream.AthOriginalDataElements;
import postilion.realtime.genericinterface.translate.stream.AthReplacementAmounts;
import postilion.realtime.genericinterface.translate.stream.Header;
import postilion.realtime.sdk.crypto.*;
import postilion.realtime.sdk.message.stream.*;
import postilion.realtime.sdk.message.*;
import postilion.realtime.sdk.util.*;
import postilion.realtime.sdk.util.convert.AmountFormat;
import postilion.realtime.sdk.util.convert.Transform;
import postilion.realtime.sdk.message.bitmap.Formatter;

public class Base24AthCustom extends Iso8583 {
	/** Variable para validaci�n del MAC. */
	private int failed_MAC = 0;
	/** Header de los mensajes. */
	private Header header = new Header();
	/** Llave usada para la autenticacion de los mensajes. */
	private DesKwa kwa = null;

	/** Constructor. Crea un nuevo objeto Iso8583Ath. */
	public Base24AthCustom(DesKwa mac_key) {
		super(iso_Ath_formatters);
		kwa = mac_key;
	}

	/** Constantes de bit de la mensajer�a ATH. */
	public static final class Bit {
		public static final int _048_ATM_ADDITIONAL_DATA = 48;
		public static final int ADDITIONAL_AMOUNTS = 54;
		public static final int TERMINAL_DATA = 60;
		public static final int CARD_ISSUER_DATA = 61;
		public static final int DATA_ADDTIONAL = 62;
		public static final int ENTITY_ERROR = 63;
		public static final int PSP_POS = 63;
		public static final int ID_ACCOUNT_CORRESP = 105;
		public static final int ID_CARD_CORRESP = 112;
		public static final int KEY_MANAGEMENT = 120;
		public static final int AUTH_INDICATORS = 121;
		public static final int POS_INVOICE_DATA = 123;
		public static final int DEPOSIT_CREDIT_AMOUNT = 123;
		public static final int CRYPTO_SERVICE_MSG = 123;
		public static final int POS_BATCH_DATA = 124;
		public static final int DEPOSITORY_TYPE = 124;
		public static final int POS_SETTLEMENT_DATA = 125;
		public static final int ATM_ACCOUNT_INDICATOR = 125;
		public static final int ATM_PREAUTH_DATA = 126;
		public static final int POS_PREAUTH_DATA = 126;
		public static final int _126_ATH_ADDITIONAL_DATA = 126;
	}

	/** Tipos de transacciones. */
	public static final class TranType {
		public static final String CASH_ADVANCE_ADJUSTMENT = new String("14");
		public static final String MAIL = new String("80");
		public static final String _80_MAIL = new String("80");
		public static final String _89_FEE_INQUIRY = new String("89");
	}

	/** Tipos de interchange. */
	public static final class InterchangeType {
		public static final int INTERCHANGE_IS_SOURCE = 1;
		public static final int INTERCHANGE_IS_SINK = 2;
		public static final int INTERCHANGE_IS_SOURCE_AND_SINK = 3;
	}

	/** C�digos de respuesta. */
	public static final class RspCode {
		public static final String _05_DENIED = new String("05");
		public static final String _12_BAD_CHECK_DIGITS = new String("12");
		public static final String _O5_PIN_REQUIRED = new String("O5");
		public static final String _68_RESPONSE_RECEIVED_LATE = new String("68");
		public static final String _21_NO_ACTION_TAKEN = new String("21");
		public static final int _17_CUSTOMER_CANCELLATION = 17;
		public static final String _17_CUSTOMER_CANCEL = new String("17");
		public static final int _22_SUSPECTED_MALFUNCTION = 22;
		public static final String _22_SUSPECTED_TRANSACTION = new String("22");
		public static final String DATABASE_ERROR = new String("89");
		public static final String ROUTING_ERROR = new String("88");
		public static final int _25_UNABLE_TO_LOCATE_RECORD = 25;
		public static final int _40_FUNCTION_NOT_SUPPORTED = 40;
		public static final int _60_CONTACT_ACQ = 60;
		public static final int _68_RESPONSE_RECEIVED_TOO_LATE = 68;
		public static final int _91_ISSUER_OR_SWITCH_INOPERATIVE = 91;
		public static final int _96_SYSTEM_MALFUNCTION = 96;
		public static final String _48_MISSING_TOKEN_FEE = "48";
	}

	/** Constantes. */
	public static final class Constant {
		public static final String FIELD_SEPARATOR = new String(" ");
		public static final String AUTHORISER_PRIMARY = new String("P");
		public static final String CATEGORY = new String("0");
		public static final String _06_SPACES = new String("      ");
		public static final String _06_ZEROS = new String("000000");
		public static final String _03_ZEROS = new String("000");
		public static final String _ZEROS = new String("0");
		public static final String ACQURING_INST_CODE = "0136";
		public static final String ACQUIRING_CONSTANT = "1000000";
		public static final String ISSUER_INST_CODE = "0036";
		public static final String INSTITUTION_ID = "99999900009";
		public static final String PSP_220_RED = "0000000000";
		public static final String PSPQUERY = "36";
		public static final String ESOCKET_QUERY = "36";
		public static final String PSPPAYMENT = "15";
		public static final String PSPTRANTYPE = "40";
		public static final String ADDITIONAL_DATA = "& 0000200058! P000036 ";
		public static final String ATM_FAILED = "4001";
		public static final String ATM_TIMEOUT = "4021";
		public static final int PSPPAY = 15;
		public static final int QUERY = 36;
		public static final String PSPTOACCOUNT = "32";
		public static final String ESOCKET = "Esocket";
		public static final String PSP_SOURCE = "PspSource";
		public static final String SP_ESOCKET = "SP";
		public static final String INTERNET_TRANS = "INTERNET";
		static final String F_SIXTEEN = "FFFFFFFFFFFFFFFF";
		public static final String MOBILEQUERY = "37";
		public static final String MOBILERELOAD = "38";
		public static final String MOBILETRANTYPE = "31";
		public static final String RECHARGETRANTYPE = "40";
		public static final String MOBILETOACCOUNT = "11";
	}

	/** Versi�n. */
	public static final class Version {
		public static final String REL_NR_34 = new String("34");
		public static final String REL_NR_40 = new String("40");
		public static final String REL_NR_60 = new String("60");
	}

	/** Estados. */
	public static final class Status {
		public static final String ON = new String("1");
		public static final String OFF = new String("0");
	}

	/** Comandos de mensaje. */
	public static final class CommandMsg {
		public static final String MANUAL_ECHO_TEST = new String("manual echo test");
		public static final String MANUAL_KEY_EXCHANGE_REQ = new String("manual key exchange req");
		public static final String AUTO_SIGN_ON = new String("auto sign on");
		public static final String MANUAL_SIGN_ON = new String("manual sign on");
		public static final String MANUAL_SIGN_OFF = new String("manual sign off");
		public static final String WAITING_FOR_0520_OR_0522_FROM_TM = new String("wait for recon msg from TM");
		public static final String SOURCE_NODE_KEY_EXCHANGE = new String("source node key exchange");
		public static final String SINK_NODE_KEY_EXCHANGE = new String("sink node key exchange");
	}

	/** Periodo de tiempo. */
	public static final class PeriodTime {
		public static final long REVERSAL_RETENTION_PERIOD = 1800000;
		public static final Integer NWRK_MNG_MSG_TIMEOUT_PERIOD = new Integer(30000);
		public static final Integer WAIT_TIME_FOR_0520_OR_0522_FROM_TM = new Integer(900000);
		public static final Integer KEY_EXCHANGE_INIT_WAIT_TIME = new Integer(3000);
		public static final Integer KEY_EXCHANGE_WAIT_TIME = new Integer(15000);
		public static final Integer SCHEDULE_KEY_EXCHANGE_TIME = new Integer(86400000);
	}

	/** Signo. */
	public static final class Sign {
		public static final String DEBIT = new String("-");
		public static final String CREDIT = new String("0");
	}

	/** Estado de corte. */
	public static final class CutoffState {
		public static final int IDLE = 0;
		public static final int WF_0810 = 1;
		public static final int WF_0510 = 2;
		public static final int WF_0512 = 3;
		public static final int WF_0510_AND_0512 = 4;
	}

	/** Estado de Sign On. */
	public static final class SignOnState {
		public static final int IDLE = 0;
		public static final int PENDING = 1;
	}

	/** Estado de Sign Off. */
	public static final class SignOffState {
		public static final int IDLE = 0;
		public static final int PENDING = 1;
	}

	/** Estado de echo test. */
	public static final class EchoTestState {
		public static final int IDLE = 0;
		public static final int PENDING = 1;
	}

	/** Estado de intercambio de llaves. */
	public static final class KeyExchangeState {
		public static final int IDLE = 0;
		public static final int PENDING = 1;
	}

	/** Indicador de partici�n. */
	public static final class IndParticion {
		public static final int IND_PART_PRIMARIA = 31;
		public static final int IND_PART_SECUNDARIA = 47;
	}

	/** Comandos. */
	public static final class Command {
		public static final String ECHO = new String("ECHO");
		public static final String KEYEXCHANGE = new String("KEYEXCHANGE");
	}

	/** Posibles valores del campo 70 en los mensajes 0800. */
	public static final class InfoCode {
		public static final int SIGN_ON = 1;
		public static final int SIGN_OFF = 2;
		public static final int CHANGE_KEY = 161;
		public static final int NEW_KEY = 162;
		public static final int VERIFY_KEY = 163;
		public static final int INITIATE_CUTOFF = 201;
		public static final int ECHO_TEST = 301;
	}

	/** Longitudes constantes. */
	public static final class Length {
		public static final int SHARING_GROUP_ID_LEN = 24;
		public static final int CARD_FIID_LEN = 4;
		public static final int CARD_NWK_LEN = 4;
		public static final int TERM_FIID_LEN = 4;
		public static final int TERM_NWK_LEN = 4;
		public static final int SETTLE_CURRENCY_LEN = 3;
		public static final int MAC_LEN = 16;
		public static final int IN_MAC_LEN = 16;
		public static final int CHECK_DIGIT = 4;
		public static final int CHECK_DIGIT_6 = 6;
	}

	/** Error de Mac. */
	public static final class MACError {
		public static final int KEY_SYNCRONIZATION_ERROR = 196;
		public static final int INVALID_MAC_ERROR = 197;
		public static final int SECURITY_OPERATION_FAIL = 198;
		public static final int SECURITY_DEVICE_FAILURE = 199;
	}

	/**
	 * Este metodo convierte un arreglo de bytes que llega en una instancia de clase
	 * Iso8583Ath. Despues de esto, los metodos fromMsg() y getField() pueden ser
	 * usados para traer el valor de cada campo.
	 * 
	 * @see postilion.realtime.sdk.message.bitmap.Iso8583#fromMsg(byte[], int)
	 */
	@Override
	public int fromMsg(byte[] msg, int offset) throws XBitmapUnableToExtract, XStreamBase, XPostilion {
		offset += header.fromMsg(msg, 0);
		offset = super.fromMsg(msg, offset);
		String info_code = getField(Iso8583.Bit.NETWORK_MNG_INFO_CODE);

		if (kwa != null && (info_code == null || (!info_code.equals(Iso8583.NwrkMngInfoCode._001_SIGN_ON)
				&& !info_code.equals(Iso8583.NwrkMngInfoCode._002_SIGN_OFF)
				&& !info_code.equals(Iso8583.NwrkMngInfoCode._301_ECHO_TEST)))) {
			// Obtiene el codigo de autenticacion que trae el mensaje
			int mac_field = Iso8583.Bit._064_MAC_NORMAL;

			if (!isFieldSet(mac_field)) {
				mac_field = Iso8583.Bit._128_MAC_EXTENDED;
			}
			if (isFieldSet(mac_field)) {
				String mac_from_rdbn = getField(mac_field).substring(0, 8);
//				this.clearField(mac_field);
				// Calcula el codigo de autenticacion de mensajes como un Hexadecimal de 8
				// digitos
				byte[] msg_nvo = new byte[msg.length - Length.IN_MAC_LEN];

				System.arraycopy(msg, 0, msg_nvo, 0, msg.length - Length.IN_MAC_LEN);
				String mac_generated = kwa.authenticate(msg_nvo);
				// La autenticacion del mensaje se determina comparando el MAC que viene de Ath
				// con el generado por Postilion
				if (mac_generated.equals(mac_from_rdbn)) {
					failed_MAC = 0;
				} else {
					failed_MAC = 197;
				}
			} else {
				failed_MAC = 197;
			}
		}
		// --------------------------------------------------------------------------------
		// Es el fin de la implementacion de MAC
		// --------------------------------------------------------------------------------

		// Se recibe un mensaje "9XXX" que no es soportado por versiones Postilion 3.5.
		// Esta clase de mensajes se reciben cuando Ath rechaza el mensaje por MAC
		// invalido.
		if (msg[0] == '9') {
//			EventRecorder.recordEvent( new RspMacInvalid(
//												new String[] 
//													{getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR)}));
		}

		// ----------------------------------------------------------------------------------
		// Field 100
		// ----------------------------------------------------------------------------------/

		// Ath podria enviar nulls o caracteres especiales en el campo 100 Rec_Id_Code,
		// si esto pasa se deben reemplazar por ceros
		String RecInst_Id = getField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE);
		if (isFieldSet(Iso8583.Bit._100_RECEIVING_INST_ID_CODE) && !Validator.isValidN(RecInst_Id)) {
			int i, fin;
			byte RecInst_Id_byte[] = Transform.getData(RecInst_Id);

			for (fin = RecInst_Id_byte.length, i = 0; i < fin; i++) {
				if (!Validator.isValidN(RecInst_Id_byte, i, 1)) {
					RecInst_Id_byte[i] = (byte) '0';
				}
			}
			String valid_RecInst_Id = Transform.getString(RecInst_Id_byte);
			clearField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE);

			putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, valid_RecInst_Id);
		}
		return offset;
	}

	/**
	 * Retorna el resultado de la validaci�n del MAC.
	 * 
	 * @return True si el MAC es inv�lido.
	 */
	public int failedMAC() {
		return failed_MAC;
	}

	/**
	 * Convierte el mensaje en un array de bytes.
	 * 
	 * @see postilion.realtime.sdk.message.bitmap.Iso8583#toMsg()
	 */
	public byte[] toMsg() throws XBitmapUnableToConstruct, XPostilion {
		return toMsg(true);
	}

	/**
	 * Convierte el mensaje en un array de bytes.
	 * 
	 * @param insert_mac Indica si debe insertar el MAC.
	 * @return Array de bytes con el mensaje.
	 * @throws XBitmapUnableToConstruct En caso de error.
	 * @throws XPostilion               En caso de error.
	 */
	public byte[] toMsg(boolean insert_mac) throws XBitmapUnableToConstruct, XPostilion {
		// get the message packed into a byte array
		byte[] msg = super.toMsg();

		// get the header packed into a byte array
		byte[] hdr = header.toMsg();

		// create the byte array which will store the final message w/o MAC
		byte[] final_msg = new byte[msg.length + hdr.length];

		// create the byte array which will store the final message with MAC
		byte[] msg_to_rdbn = new byte[msg.length + hdr.length + Length.MAC_LEN];

		// copy the header into the final message array
		System.arraycopy(hdr, 0, final_msg, 0, hdr.length);

		// copy the message into the final message array
		System.arraycopy(msg, 0, final_msg, hdr.length, msg.length);

		String info_code = getField(Iso8583.Bit.NETWORK_MNG_INFO_CODE);
		if (kwa != null && insert_mac
				&& (info_code == null || (!info_code.equals(Iso8583.NwrkMngInfoCode._001_SIGN_ON)
						&& !info_code.equals(Iso8583.NwrkMngInfoCode._002_SIGN_OFF)
						&& !info_code.equals(Iso8583.NwrkMngInfoCode._301_ECHO_TEST)))) {
			// Antes de generar el MAC debe indicarse la particion correspondiente que ira
			// (primera si es hasta 64bytes o segunda si es de 128bytes).
			if (isPrimaryBytes()) {
				final_msg[IndParticion.IND_PART_PRIMARIA] = ++final_msg[IndParticion.IND_PART_PRIMARIA];
			} else {
				final_msg[IndParticion.IND_PART_SECUNDARIA] = ++final_msg[IndParticion.IND_PART_SECUNDARIA];
			}

			// Calcula el codigo de autenticacion del mensaje como un Hexadecimal de 8
			// digitos
			String mac = kwa.authenticate(final_msg);

			// get the message packed into a byte array
			msg = super.toMsg();

			// copy the message without MAC into the final message array (include MAC code)
			System.arraycopy(final_msg, 0, msg_to_rdbn, 0, final_msg.length);

			byte[] byte_mac = new byte[Base24AthCustom.Length.MAC_LEN];

			byte_mac = Transform.getData(mac + "00000000"); // 8 zeros

			if (isPrimaryBytes()) {
				this.putField(64, mac + "00000000");
			} else {
				this.putField(128, mac + "00000000");
			}

			// copy the MAC authentication code into the final message array
			System.arraycopy(byte_mac, 0, msg_to_rdbn, final_msg.length, Base24AthCustom.Length.MAC_LEN);

			this.clear_binary_data = msg_to_rdbn;
			return msg_to_rdbn;

		}
		return final_msg;
	}

	/**
	 * Este m�todo retorna true si el mensaje corresponde a "Primary Bytes" o false
	 * si el mensaje corresponde a "Secundary Bytes"
	 * 
	 * @return True si esta presente si solo esta presente el primary byte.
	 */
	boolean isPrimaryBytes() {
		int i;
		for (i = 65; !isFieldSet(i) && i < 128; i++)
			;
		return i == 128;
	}

	/**
	 * Establece el valor del encabezado.
	 * 
	 * @param header Encabezado.
	 */
	public void putHeader(Header header) {
		this.header = header;
	}

	/**
	 * Obtiene el valor del encabezado.
	 * 
	 * @return Encabezado.
	 */
	public Header getHeader() {
		return header;
	}

	/**
	 * Retorna el mensaje en un String.
	 * 
	 * @see postilion.realtime.sdk.message.bitmap.BitmapMessage#toString(int)
	 */
	public String toString(int field_nr) {

		if (field_nr == 57) {
			if ((fields[field_nr] != null) && (formatters[field_nr] != null)) {
				return "   [" + formatters[field_nr].describeType() + " "
						+ AmountFormat.toString(fields[field_nr].data.length, 4) + "] " + _bit_nr_prefix
						+ AmountFormat.toString(field_nr, 3) + "  [" + Transform.getString(fields[field_nr].data)
						+ "] ";
			} else {
				return "";
			}
		} else {

			if ((fields[field_nr] != null) && (formatters[field_nr] != null)) {
				return "   [" + formatters[field_nr].describeType() + " "
						+ AmountFormat.toString(fields[field_nr].data.length, 3) + "] " + _bit_nr_prefix
						+ AmountFormat.toString(field_nr, 3) + "  [" + Transform.getString(fields[field_nr].data)
						+ "] ";
			} else {
				return "";
			}
		}
	}

	/**
	 * Sobreescribe el toString del Stream Message para que muestre el Header.
	 * 
	 * @see postilion.realtime.sdk.message.bitmap.Iso8583#toString()
	 */
	public String toString() {
		String sMessage = "";
		sMessage = this.getHeader().toString() + super.toString();
		return sMessage;
	}

	/**
	 * Retorna el valor del Atm Additional Response Data (Ath ISO8583 field 44).
	 *
	 * @return El valor del campo. Si es vac�o retorna null.
	 * @throws XPostilion En caso de error.
	 * @see AthAtmAdditionalResponseData
	 */
	public final AthAtmAdditionalResponseData getAthAtmAdditionalResponseData() throws XPostilion {
		String data = getField(Iso8583.Bit.ADDITIONAL_RSP_DATA);
		if (data == null) {
			return null;
		}
		AthAtmAdditionalResponseData addn_data = new AthAtmAdditionalResponseData();
		addn_data.fromMsg(data);
		return addn_data;
	}

	/**
	 * Establece el valor del ATM Additional Response Data (Ath ISO8583 field 44).
	 * 
	 * @param value Establece el valor del ATM Additional Response Data. Si es null,
	 *              el campo se limp�a.
	 * @see AthAtmAdditionalResponseData
	 */
	public final void putAthAtmAdditionalResponseData(AthAtmAdditionalResponseData value) throws XPostilion {
		if (value != null) {
			putField(Iso8583.Bit.ADDITIONAL_RSP_DATA, Transform.getString(value.toMsg()));
		} else {
			clearField(Iso8583.Bit.ADDITIONAL_RSP_DATA);
		}
	}

	/**
	 * Obtiene el valor del ATM Terminal Data (Ath ISO8583 field 60).
	 * 
	 * @return El valor del ATM Terminal Data (Ath ISO8583 field 60).
	 * @throws XPostilion En caso de error.
	 * @see AthAtmTerminalData
	 */
	public final AthAtmTerminalData getAthAtmTerminalData() throws XPostilion {
		String data = getField(Base24AthCustom.Bit.TERMINAL_DATA);
		if (data == null) {
			return null;
		}
		AthAtmTerminalData term_data = new AthAtmTerminalData();
		term_data.fromMsg(data);
		return term_data;
	}

	/**
	 * Establece el valor del Atm Terminal Data (Ath ISO8583 field 60).
	 * 
	 * @param value Valor del Atm Terminal Data (Ath ISO8583 field 60).
	 * @see AthAtmTerminalData
	 */
	public final void putAthAtmTerminalData(AthAtmTerminalData value) throws XPostilion {
		if (value != null) {
			putField(Base24AthCustom.Bit.TERMINAL_DATA, Transform.getString(value.toMsg()));
		} else {
			clearField(Base24AthCustom.Bit.TERMINAL_DATA);
		}
	}

	/**
	 * Obtiene el objeto AthErrorInterchange.
	 * 
	 * @return Objeto error de interchange.
	 * @throws XPostilion En caso de error.
	 */
	public final AthErrorInterchange getAthErrorInterchange() throws XPostilion {
		String data = getField(Base24AthCustom.Bit.ENTITY_ERROR);
		if (data == null) {
			return null;
		}
		AthErrorInterchange entity_error = new AthErrorInterchange();
		entity_error.fromMsg(data);
		return entity_error;
	}

	/**
	 * Establece el valor del AthErrorInterchange.
	 * 
	 * @param value Objeto error de interchange.
	 * @throws XPostilion En caso de error.
	 */
	public final void putAthErrorInterchange(AthErrorInterchange value) throws XPostilion {
		if (value != null) {
			StringBuffer field = new StringBuffer();
			field.append(value.getField("error"));
			field.append(value.getField("description"));
			putField(Base24AthCustom.Bit.ENTITY_ERROR, field.toString());
		} else {
			clearField(Base24AthCustom.Bit.ENTITY_ERROR);
		}
	}

	/**
	 * Obtiene el Original Data Elements (Ath ISO8583 field 90).
	 * 
	 * @return Obtiene el valor Original Data Elements (Ath ISO8583 field 90).
	 * @throws XPostilion En caso de error.
	 * @see AthOriginalDataElements
	 */
	public final AthOriginalDataElements getAthOriginalDataElements() throws XPostilion {
		String data = getField(Iso8583.Bit._090_ORIGINAL_DATA_ELEMENTS);
		if (data == null) {
			return null;
		}

		AthOriginalDataElements orig_data = new AthOriginalDataElements();
		orig_data.fromMsg(data);

		return orig_data;
	}

	/**
	 * Establece el valor del campo Original Data Elements </i> (Ath ISO8583 field
	 * 90).
	 * 
	 * @param value Valor del campo Original Data Elements </i> (Ath ISO8583 field
	 *              90).
	 * @throws XPostilion En caso de error.
	 * @see AthOriginalDataElements
	 */
	public final void putAthOriginalDataElements(AthOriginalDataElements value) throws XPostilion {
		if (value != null) {
			putField(Iso8583.Bit._090_ORIGINAL_DATA_ELEMENTS, Transform.getString(value.toMsg()));
		} else {
			clearField(Iso8583.Bit._090_ORIGINAL_DATA_ELEMENTS);
		}
	}

	/**
	 * Obtiene el valor del campo Replacement Amounts (Ath ISO8583 field 95).
	 * 
	 * @return Valor del campo Replacement Amounts (Ath ISO8583 field 95).
	 * @throws XPostilion En caso de error.
	 * @see AthReplacementAmounts
	 */
	public final AthReplacementAmounts getAthReplacementAmounts() throws XPostilion {
		String data = getField(Iso8583.Bit.REPLACEMENT_AMOUNTS);
		if (data == null) {
			return null;
		}

		AthReplacementAmounts replacement_amts = new AthReplacementAmounts();
		replacement_amts.fromMsg(data);

		return replacement_amts;
	}

	/**
	 * Establece el valor Replacement Amounts (Ath ISO8583 field 95).
	 * 
	 * @param value El valor del Replacement Amounts (Ath ISO8583 field 95).
	 * @throws XPostilion En caso de error.
	 * @see AthReplacementAmounts
	 */
	public final void putAthReplacementAmounts(AthReplacementAmounts value) throws XPostilion {
		if (value != null) {
			putField(Iso8583.Bit.REPLACEMENT_AMOUNTS, Transform.getString(value.toMsg()));
		} else {
			clearField(Iso8583.Bit.REPLACEMENT_AMOUNTS);
		}
	}

	/** Instancia del array est�tico Iso8583. */
	protected static Iso8583.Template iso_Ath_formatters = null;

	/**
	 * Inicializa el array de formatters.
	 */

	private static void init() {

		try {
			iso_Ath_formatters = new Iso8583.Template(Iso8583.Template.Packing.NONE, Iso8583.Template.Packing.NONE,
					Iso8583.Template.Packing.NONE, Iso8583.Template.Packing.HEX, Iso8583.Template.Packing.NONE,
					Iso8583.Template.Packing.NONE, Iso8583.Template.Packing.NONE, Iso8583.Template.Packing.NONE,
					Iso8583.Template.Packing.HEX, true);
		} catch (XInputParameterError e) {
			System.exit(1);
		}

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit.AMOUNT_TRANSACTION,
				new Formatter(LengthFormatter.getFixed(12), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit._028_AMOUNT_TRAN_FEE,
				new Formatter(LengthFormatter.getFixed(8), FieldFormatter.getNone(), Validator.getN()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit._030_AMOUNT_TRAN_PROC_FEE,
				new Formatter(LengthFormatter.getFixed(8), FieldFormatter.getNone(), Validator.getN()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit.ACQUIRING_INST_ID_CODE,
				new Formatter(LengthFormatter.getVar(2, 11), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit._035_TRACK_2_DATA,
				new Formatter(LengthFormatter.getVar(2, 45), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit.CARD_ACCEPTOR_TERM_ID,
				new Formatter(LengthFormatter.getFixed(16), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit.PIN_DATA,
				new Formatter(LengthFormatter.getFixed(16), FieldFormatter.getNone(), Validator.getAn()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit.SECURITY_INFO,
				new Formatter(LengthFormatter.getFixed(16), FieldFormatter.getNone(), Validator.getAn()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit.ADDITIONAL_AMOUNTS,
				new Formatter(LengthFormatter.getVar(3, 120), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Iso8583Post.Bit._057_AUTH_LIFE_CYCLE,
				new Formatter(LengthFormatter.getVar(4, 9999), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Base24AthCustom.Bit.CARD_ISSUER_DATA,
				new Formatter(LengthFormatter.getVar(3, 22), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Base24AthCustom.Bit.DATA_ADDTIONAL,
				new Formatter(LengthFormatter.getVar(3, 150), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit.MAC_NORMAL,
				new Formatter(LengthFormatter.getFixed(16), FieldFormatter.getNone(), Validator.getAn()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit.REPLACEMENT_AMOUNTS,
				new Formatter(LengthFormatter.getFixed(42), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit._100_RECEIVING_INST_ID_CODE,
				new Formatter(LengthFormatter.getVar(2, 11), FieldFormatter.getNone(), Validator.getNs()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit._104_TRAN_DESCRIPTION,
				new Formatter(LengthFormatter.getVar(2, 24), FieldFormatter.getNone(), Validator.getNs()));

		iso_Ath_formatters.putFieldFormatter(Base24AthCustom.Bit.ID_ACCOUNT_CORRESP,
				new Formatter(LengthFormatter.getVar(3, 25), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Base24AthCustom.Bit.KEY_MANAGEMENT,
				new Formatter(LengthFormatter.getVar(3, 6), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Base24AthCustom.Bit._126_ATH_ADDITIONAL_DATA,
				new Formatter(LengthFormatter.getVar(3, 999), FieldFormatter.getNone(), Validator.getAns()));

		iso_Ath_formatters.putFieldFormatter(Iso8583.Bit.MAC_EXTENDED,
				new Formatter(LengthFormatter.getFixed(16), FieldFormatter.getNone(), Validator.getAn()));
	}

//	private static void init() {
//		try {
//			
//			iso_Ath_formatters = new Iso8583.Template(
//					Iso8583.Template.Packing.NONE,
//					Iso8583.Template.Packing.NONE,
//					Iso8583.Template.Packing.NONE,
//					Iso8583.Template.Packing.HEX,
//					Iso8583.Template.Packing.NONE,
//					Iso8583.Template.Packing.NONE,
//					Iso8583.Template.Packing.NONE,
//					Iso8583.Template.Packing.NONE,
//					Iso8583.Template.Packing.HEX, true);
//			
//		} catch (XInputParameterError e) {
//			System.exit(1);
//		}
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit.AMOUNT_TRANSACTION,
//				new Formatter(LengthFormatter.getFixed(12), FieldFormatter
//						.getNone(), Validator.getAns()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit.ACQUIRING_INST_ID_CODE, new Formatter(
//						LengthFormatter.getVar(2, 11),
//						FieldFormatter.getNone(), Validator.getAns()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit.CARD_ACCEPTOR_TERM_ID,
//				new Formatter(LengthFormatter.getFixed(16), FieldFormatter
//						.getNone(), Validator.getAns()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit.PIN_DATA,
//				new Formatter(LengthFormatter.getFixed(16), FieldFormatter
//						.getNone(), Validator.getAn()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit.SECURITY_INFO,
//				new Formatter(LengthFormatter.getFixed(16), FieldFormatter
//						.getNone(), Validator.getAn()));// Marisol Leon
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit.ADDITIONAL_AMOUNTS,
//				new Formatter(LengthFormatter.getVar(3, 120), FieldFormatter
//						.getNone(), Validator.getAns()));
//
////		iso_Ath_formatters.putFieldFormatter(
////				Base24Ath.Bit.CARD_ISSUER_DATA,
////				new Formatter(LengthFormatter.getFixed(19), FieldFormatter
////						.getNone(), Validator.getAns()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Base24Ath.Bit.DATA_ADDTIONAL,
//				new Formatter(LengthFormatter.getVar(3, 150), FieldFormatter
//						.getNone(), Validator.getAns()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit.MAC_NORMAL,
//				new Formatter(LengthFormatter.getFixed(16), FieldFormatter
//						.getNone(), Validator.getAn()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit.REPLACEMENT_AMOUNTS,
//				new Formatter(LengthFormatter.getFixed(42), FieldFormatter
//						.getNone(), Validator.getAns()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit._100_RECEIVING_INST_ID_CODE, new Formatter(
//						LengthFormatter.getVar(2, 11),
//						FieldFormatter.getNone(), Validator.getNs()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit._104_TRAN_DESCRIPTION,
//				new Formatter(LengthFormatter.getVar(2, 24), FieldFormatter
//						.getNone(), Validator.getNs()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Base24Ath.Bit.ID_ACCOUNT_CORRESP,
//				new Formatter(LengthFormatter.getVar(3, 25), FieldFormatter
//						.getNone(), Validator.getAns()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Base24Ath.Bit.KEY_MANAGEMENT,
//				new Formatter(LengthFormatter.getVar(3, 6), FieldFormatter
//						.getNone(), Validator.getAns()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Base24Ath.Bit._126_ATH_ADDITIONAL_DATA,
//				new Formatter(LengthFormatter.getVar(3, 100), FieldFormatter
//						.getNone(), Validator.getAns()));
//
//		iso_Ath_formatters.putFieldFormatter(
//				Iso8583.Bit.MAC_EXTENDED,
//				new Formatter(LengthFormatter.getFixed(16), FieldFormatter
//						.getNone(), Validator.getAn()));
//	}

	static {
		init();
	}
}