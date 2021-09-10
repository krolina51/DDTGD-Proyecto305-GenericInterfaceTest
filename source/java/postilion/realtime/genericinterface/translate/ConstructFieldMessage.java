package postilion.realtime.genericinterface.translate;

import postilion.realtime.library.common.InitialLoadFilter;
import postilion.realtime.library.common.model.ResponseCode;
import postilion.realtime.library.common.util.constants.AdditionalRspData;
import postilion.realtime.library.common.util.constants.AmmountType;
import postilion.realtime.library.common.util.constants.AuthorizationIdRsp;
import postilion.realtime.library.common.util.constants.General;
import postilion.realtime.library.common.util.constants.PosPinCaptureCode;
import postilion.realtime.library.common.util.constants.ServiceRestrictionCode;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.eventrecorder.events.TryCatchException;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath.TranType;
import postilion.realtime.genericinterface.translate.database.DBHandler;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.genericinterface.translate.util.Constants.Account;
import postilion.realtime.genericinterface.translate.util.Constants.FormatDate;
import postilion.realtime.genericinterface.translate.util.Constants.RoutingAccount;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.sdk.env.calendar.BusinessCalendar;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.message.bitmap.*;
import postilion.realtime.sdk.message.bitmap.Iso8583.AccountType;
import postilion.realtime.sdk.message.bitmap.Iso8583.MsgType;
import postilion.realtime.sdk.util.DateTime;
import postilion.realtime.sdk.util.TimedHashtable;
import postilion.realtime.sdk.util.XPostilion;
import postilion.realtime.sdk.util.convert.Pack;
import postilion.realtime.sdk.util.convert.Transform;

/**
 * Construye los campos que son excepción en el mensaje hacia Transaction
 * Manager
 */
public class ConstructFieldMessage extends MessageTranslator {

	private TimedHashtable sourceTranToTmHashtable = null;
	private TimedHashtable sourceTranToTmHashtableB24 = null;
	private Map<String, ResponseCode> allCodesIsoToB24 = new HashMap<>();
	private Map<String, ResponseCode> allCodesIscToIso = new HashMap<>();
	private Map<String, ResponseCode> allCodesIsoToB24TM = new HashMap<>();
	private Map<String, ResponseCode> allCodesB24ToIso = new HashMap<>();
	private Map<String, String> institutionid = new HashMap<>();
	private Client udpClient = null;
	private String nameInterface = "";
	private String routingField100 = "";
	private Parameters params;

	public ConstructFieldMessage(Parameters params) {
		this.sourceTranToTmHashtable = params.getSourceTranToTmHashtable();
		this.sourceTranToTmHashtableB24 = params.getSourceTranToTmHashtableB24();
		this.allCodesIsoToB24 = params.getAllCodesIsoToB24();
		this.allCodesIscToIso = params.getAllCodesIscToIso();
		this.allCodesIsoToB24TM = params.getAllCodesIsoToB24TM();
		this.allCodesB24ToIso = params.getAllCodesB24ToIso();
		this.institutionid = params.getInstitutionid();
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
		this.routingField100 = params.getRoutingField100();
		this.params = params;
	}

	private static String respCode = null;
	private static String statusDesc = null;
	private static String correctTypeAccount = null;
	private static int lengthField102 = 0;

	/**************************************************************************************
	 * Construye el campo POS Condition Code (Bit 25) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que contiene el campo construido.
	 * @throws XPostilion error al obtener el campo
	 *************************************************************************************/
	public String constructPosConditionCodeToTranmgr(Object object, Integer num) throws XPostilion {
		Iso8583 msg = (Iso8583) object;
		String posConditionCode = General.VOIDSTRING;
		try {
			if (msg.isFieldSet(Iso8583.Bit._025_POS_CONDITION_CODE)) {
				posConditionCode = msg.getField(Iso8583.Bit._025_POS_CONDITION_CODE);
			} else {
				posConditionCode = Iso8583.PosCondCode.NORMAL_PRESENTMENT;
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructPosConditionCodeToTranmgr]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructPosConditionCodeToTranmgr: " + e.getMessage(), "LOG",
					this.nameInterface));
		}
		return posConditionCode;
	}

	/**************************************************************************************
	 * Contruye el campo 26 hacia Transaction Manager
	 * 
	 * @param msgToTm mensaje hacia Transaction Manger
	 * @return String con el valor del campo 26
	 * @throws XFieldUnableToConstruct Si lo puede obtener el campo 123 del mensaje.
	 *************************************************************************************/
	public String constructPosPinCaptureCode(Iso8583Post msgToTm, Integer num) throws XFieldUnableToConstruct {
		String posPinCaptureCode = null;
		try {
			if (msgToTm.isFieldSet(Iso8583Post.Bit._123_POS_DATA_CODE) && msgToTm.getPosDataCode()
					.getPinCaptureCapability().equals(PosDataCode.PinCaptureCapability._4_FOUR)) {
				posPinCaptureCode = PosPinCaptureCode.FOUR;
			}else {
				posPinCaptureCode = "00";
			}
		} catch (Exception e) {
			try {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						ConstructFieldMessage.class.getName(), "Method: [constructPosPinCaptureCode]",
						Utils.getStringMessageException(e), msgToTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msgToTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method:  constructPosPinCaptureCode: " + e.getMessage(), "LOG",
						this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						ConstructFieldMessage.class.getName(), "Method: [constructPosPinCaptureCode]",
						Utils.getStringMessageException(e1), "Unknown" }));

			}
		}
		return posPinCaptureCode;
	}

	/**************************************************************************************
	 * Contruye el campo 26 hacia Transaction Manager
	 * 
	 * @param msgToTm mensaje hacia Transaction Manger
	 * @return String con el valor del campo 26
	 * @throws XPostilion Si no puede retornar el valor.
	 *************************************************************************************/
	public static String constructDefaultField26(Object object, Integer num) throws XPostilion {

		return "04";
	}

	/**************************************************************************************
	 * Construye el campo Authorization ID Response aleatorio (Bit 38) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que devuelve el campo construido.
	 * @throws XPostilion
	 *************************************************************************************/
	public static String constructRandomField38(Object object, Integer num) throws XPostilion {

		String time = new SimpleDateFormat("mssSSS").format(new Date());
		return time.substring(time.length() - 6, time.length());
	}

	/**************************************************************************************
	 * Construye el campo Additional Response Data (Bit 44) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que devuelve el campo construido.
	 * @throws XPostilion
	 *************************************************************************************/
	public static String constructEntryMode010(Object object, Integer num) throws XPostilion {
		Base24Ath msg = (Base24Ath) object;
		String bin = msg.getTrack2Data().getPan().substring(0, 6);

		if (bin.equals("491511") && msg.getField(num).equals("021")) {
			return "010";
		}

		return msg.getField(num);
	}

	/**************************************************************************************
	 * Construye el campo entry mode con valor 010.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que devuelve el campo construido.
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructDefaultField44(Object object, Integer num) throws XPostilion {

		return postilion.realtime.genericinterface.translate.util.Constants.General.DEFAULT_P44;
	}

	/**************************************************************************************
	 * Construye el campo ACCOUNT_ID (Bit 102) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que devuelve el campo construido.
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField102AccountId(Object object, Integer num) throws XPostilion {
		String accountId = null;
		Base24Ath msg = (Base24Ath) object;
		try {

			String pan = msg.getTrack2Data().getPan();

			accountId = pan.substring(pan.length() - 6, pan.length());

		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructField102AccountId]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructField102AccountId " + e.getMessage(), "LOG", this.nameInterface));
		}
		return accountId;
	}

	/**************************************************************************************
	 * Construye el campo 61 (Bit 61) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que devuelve el campo construido.
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField61ByDefault(Object object, Integer num) throws XPostilion {

		Iso8583 msg = (Iso8583) object;
		StringBuilder sbField = new StringBuilder();
		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"getFromAccount " + msg.getProcessingCode(), "LOG", this.nameInterface));
		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"getToAccount " + msg.getProcessingCode().getToAccount(), "LOG", this.nameInterface));

		sbField.append(Constants.General.NETWORKID_ATH).append(Constants.General.NETWORKNAME_ATH)
				.append(msg.getProcessingCode().getFromAccount()).append(msg.getProcessingCode().getToAccount())
				.append(Constants.General.CARD_ISSUER_AUTH);

		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"constructField61 " + sbField.toString(), "LOG", this.nameInterface));

		return sbField.toString();
		// return "0901BBOG0000P";
	}

	/**************************************************************************************
	 * Construye el campo 105 (Bit 105) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que devuelve el campo construido.
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField105(Object object, Integer num) throws XPostilion {
		// String field41 = msg.getStructuredData().get("B24_Field_41");
		String field105 = Constants.General.DEFAULT_P105;
		if (object instanceof Iso8583Post) {
			Iso8583Post msg = (Iso8583Post) object;
			if (msg.getStructuredData() == null) {
				return Constants.General.DEFAULT_P105;
			}

			String field41 = msg.getStructuredData().get("B24_Field_41");

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"constructField105 CAMPO41: " + field41, "LOG", this.nameInterface));
			if (field41.substring(12, 13).equals("7")) {
				switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
				case "021":
				case "010":

					switch (msg.getField(Iso8583.Bit._003_PROCESSING_CODE)) {
					case Constants.Channels.PCODE_PAGO_SP_CNB_A:
					case Constants.Channels.PCODE_PAGO_SP_CNB_C:

						if (!msg.getStructuredData().get("B24_Field_103").substring(0, 1).equals("2"))
							field105 = Constants.General.DEFAULT_P105_MIXTA;
						break;
					default:
						if (!msg.getStructuredData().get("B24_Field_103").substring(2, 3).equals("1"))
							field105 = Constants.General.DEFAULT_P105_MIXTA;
						break;
					}

					break;

				default:

					break;
				}
			}
		}

		return field105;
	}

	/**************************************************************************************
	 * Construye el campo 98 (Bit 98) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que devuelve el campo construido.
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField98(Object object, Integer num) throws XPostilion {

		return "0054150070650000000000000";
	}

	/**************************************************************************************
	 * Construye el campo 128 default (Bit 128) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que devuelve el campo construido.
	 * @throws XPostilion
	 *************************************************************************************/
	public static String constructField128(Object object, Integer num) throws XPostilion {

		return Constants.General.DEFAULT_P128;
	}

	/**************************************************************************************
	 * Construye el campo Card Acceptor Terminal ID (Bit 41) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que devuelve el campo construido.
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructCardAcceptorTermIdToTranmgr(Object object, Integer num) throws XPostilion {
		String cardAcceptortermId = null;
		Base24Ath msg = (Base24Ath) object;
		try {
			if (msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)) {
				cardAcceptortermId = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)
						.substring(Constants.Indexes.FIELD41_POSITION_0, Constants.Indexes.FIELD41_POSITION_8);
			} else {
				cardAcceptortermId = Pack.resize(General.VOIDSTRING, General.LENGTH_8, General.SPACE, true);
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructCardAcceptorTermIdToTranmgr]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructCardAcceptorTermIdToTranmgr " + e.getMessage(), "LOG",
					this.nameInterface));
		}
		return cardAcceptortermId;
	}

//	/**************************************************************************************
//	 * Construye el campo response code (Bit 39) hacia el TM consumiendo el
//	 * microservicio para consulta de cuentas relacionadas
//	 * 
//	 * @param object Mensaje desde ATH.
//	 * @return Retorna un String que devuelve el campo construido.
//	 * @throws XPostilion
//	 *************************************************************************************/
//	public String constructResponseCodeByRest(Object object, Integer num) throws XPostilion {
//		respCode = null;
//		statusDesc = null;
//		String responseCode = postilion.realtime.genericinterface.translate.util.Constants.General.NUM_NINETYONE;
//		Base24Ath msg = (Base24Ath) object;
//
//		try {
//			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//					"restParams :  " + this.restParams, "LOG", this.nameInterface));
//
//			AccountsInfo JsonObjects = ConnectRest.getRelatedAccounts(msg.getProcessingCode().getFromAccount(),
//					msg.getTrack2Data().getPan(), this.restParams);
//			responseCode = JsonObjects.getStatus().getstatusCode();
//			String relatedAccount = (responseCode.equals("00")) ? constructField125(JsonObjects.getAccounts())
//					: postilion.realtime.genericinterface.translate.util.Constants.General.DEFAULT_P125;
//			msg.putField(postilion.realtime.genericinterface.translate.util.Constants.General.NUMBER_125,
//					relatedAccount);
//
//			ResponseCode responseCodeStatus;
//			try {
//				responseCodeStatus = InitialLoadFilter.getFilterCodeIsoToB24(responseCode, this.allCodesIsoToB24);
//			} catch (NoSuchElementException e) {
//				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//						"NoSuchElementException in Method: constructResponseCodeByRest "
//								+ Utils.getStringMessageException(e),
//						"LOG", this.nameInterface));
//				if (new DBHandler(this.params).updateResgistry(responseCode, "0")) {
//					this.allCodesIsoToB24 = postilion.realtime.library.common.db.DBHandler.getResponseCodes(false, "0");
//					responseCodeStatus = InitialLoadFilter.getFilterCodeIsoToB24(responseCode, this.allCodesIsoToB24);
//				} else {
//					responseCodeStatus = new ResponseCode("10002", "Error Code could not extracted from message",
//							responseCode, responseCode);
//					EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
//							ConstructFieldMessage.class.getName(), "Method: [constructResponseCodeByRest]",
//							Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
//					EventRecorder.recordEvent(e);
//					this.udpClient
//							.sendData(
//									Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//											"NoSuchElementException in Method: constructResponseCodeByRest value "
//													+ responseCode + " is not in the table",
//											"LOG", this.nameInterface));
//				}
//			}
//			statusDesc = Pack.resize(responseCodeStatus.getKeyIsc() + responseCodeStatus.getDescriptionIsc(),
//					General.LENGTH_44, General.SPACE, true);
//			msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1, constructField102AccountId(object, 102));
//
//		} catch (Exception e) {
//			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
//					ConstructFieldMessage.class.getName(), "Method: [constructResponseCodeByRest]",
//					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
//			EventRecorder.recordEvent(e);
//			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//					"Exception in Method: constructResponseCodeByRest " + e.getMessage(), "LOG", this.nameInterface));
//		}
//
//		respCode = responseCode;
//
//		return responseCode;
//	}

	/**
	 * Construye el campo 125 en base 24
	 * 
	 * @param accounts arreglo de String con las cuentas a cargar en el campo
	 * @return String con todas las cuentas
	 */
	public static String constructField125(String[] accounts) {
		StringBuilder field125 = new StringBuilder();
		field125.append("0").append(accounts.length);
		for (int i = 0; i < accounts.length; i++) {
			field125.append(Pack.resize(accounts[i], 13, '0', false));
		}
		return Pack.resize(field125.toString(), 54, '0', true);
	}

	/**
	 * Construye el campo 39 para mensaje 0220 de consulta de cuentas relacionadas.
	 * 
	 * @return String codigo de respuesta devuelto por el servicio.
	 */
	public static String constructField39Msg0220RelatedAccounts(Object object, Integer num) throws XPostilion {
		return respCode;
	}

	/**
	 * Construye el campo 63 para mensaje 0220 de consulta de cuentas relacionadas.
	 * 
	 * @return String valor de la descripcion devuelto por el servicio.
	 */
	public static String constructField63Msg0220RelatedAccounts(Object object, Integer num) throws XPostilion {

//		msgToTm.putField(Iso8583Ath.Bit.ENTITY_ERROR, Pack.resize(rspCode.getKeyIsc() + rspCode.getDescriptionIsc(),
//				General.LENGTH_44, General.SPACE, true));

		return statusDesc;
	}

	/**************************************************************************************
	 * Contruye el campo PIN Data (Bit 52) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String con el campo construido o null.
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructPinDataToTranmgr(Object object, Integer num) throws XPostilion {

		String pinBlock = null;
		Base24Ath msg = (Base24Ath) object;
		try {
			if (msg.isFieldSet(Iso8583.Bit._052_PIN_DATA)) {
				pinBlock = Transform.fromHexToBin(msg.getField(Iso8583.Bit._052_PIN_DATA));
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructPinDataToTranmgr]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructPinDataToTranmgr " + e.getMessage(), "LOG", this.nameInterface));
		}
		return pinBlock;
	}

	/**************************************************************************************
	 * Contruye el campo AcquiringInstitutionID (Bit 32) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String con el campo construido o null.
	 * @throws XPostilion
	 *************************************************************************************/
	public static String constructAcquiringInstitutionIDCodeToTM(Object object, Integer num) throws XPostilion {
		Base24Ath msg = (Base24Ath) object;
		return msg.getField(num).trim();

	}

	/**************************************************************************************
	 * construye el campo 54 con el formato de postilion
	 * 
	 * @param object mensaje recibido desde Red AVAL
	 * @return objeto AdditionalAmounts
	 * @throws XPostilion al obtener un campo del mensaje
	 *************************************************************************************/
	public String constructAdditionalAmounts(Object object, Integer num) throws XPostilion {
		AdditionalAmounts amounts = new AdditionalAmounts();
		Base24Ath msgFromRemote = (Base24Ath) object;
		String field54 = null;
		try {
			if (msgFromRemote.isFieldSet(Iso8583.Bit._054_ADDITIONAL_AMOUNTS)) {

				field54 = msgFromRemote.getField(Iso8583.Bit._054_ADDITIONAL_AMOUNTS);
			} else {
				field54 = Constants.General.DEFAULT_P54_CONSULTA_COSTO;

			}
			String[] values = {
					field54.substring(Constants.Indexes.FIELD54_POSITION_0, Constants.Indexes.FIELD54_POSITION_12),
					field54.substring(Constants.Indexes.FIELD54_POSITION_12, Constants.Indexes.FIELD54_POSITION_24),
					field54.substring(Constants.Indexes.FIELD54_POSITION_24, Constants.Indexes.FIELD54_POSITION_36) };
			String accountType = msgFromRemote.getProcessingCode().getFromAccount();
			String amountType = AmmountType.CASH;
			String currencyCode = msgFromRemote.getField(Iso8583.Bit._049_CURRENCY_CODE_TRAN);
			for (int i = 0; i < values.length; i++) {
				AdditionalAmount amount = new AdditionalAmount(accountType, amountType, currencyCode,
						Double.parseDouble(values[i]));
				amounts.addAdditionalAmount(amount);
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructAdditionalAmounts]",
					Utils.getStringMessageException(e), msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructAdditionalAmounts " + e.getMessage(), "LOG", this.nameInterface));
		}
		return amounts.toString();
	}

	/**************************************************************************************
	 * Contruye el campo 059 hacia Transaction Manager
	 * 
	 * @param object mensaje a enviar
	 * @return String con el valor del campo 059.
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructEchoData(Object object, Integer num) throws XPostilion {
		Base24Ath msgFromRmto = (Base24Ath) object;
		String echoData = null;
		try {
			echoData = msgFromRmto.getTrack2Data().getServiceRestrictionCode();
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructEchoData]",
					Utils.getStringMessageException(e), msgFromRmto.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msgFromRmto.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructEchoData " + e.getMessage(), "LOG", this.nameInterface));
		}
		return echoData;
	}

	/**************************************************************************************
	 * Construye el campo 95 con valores en cero.
	 * 
	 * @param object mensaje recibido desde Red AVAL
	 * @return objeto ReplacementAmounts que defien el campo
	 *         95constructAdditionalRspData
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructReplacementAmounts(Object object, Integer num) throws XPostilion {
		try {
			ReplacementAmounts field95 = ReplacementAmounts.ZERO_AMOUNTS;
			return field95.toString();
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructReplacementAmounts]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(
					((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructReplacementAmounts " + e.getMessage(), "LOG", this.nameInterface));

		}
		return null;
	}

	/**************************************************************************************
	 * Construye el campo 95 con valores en cero.
	 *
	 * @param object mensaje recibido desde Red AVAL
	 * @return objeto ReplacementAmounts que defien el campo
	 *         95constructAdditionalRspData
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructReplacementAmountsZero(Object object, Integer num) throws XPostilion {
		try {
			return Constants.Config.AMOUNTS_ZERO;
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructReplacementAmountsZero]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient
					.sendData(Client.getMsgKeyValue(((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Exception in Method: constructReplacementAmountsZero " + e.getMessage(), "LOG",
							this.nameInterface));

		}
		return null;
	}

	/**
	 * contruye el campo 123 hacia Transaction Manager.
	 *
	 * @param msgFromRemote mensaje recibido desde red AVAL
	 * @return objeto {@link PosDataCode}
	 * @throws XPostilion
	 */
	public String constructPosDataCode(Object object, Integer num) throws XPostilion {
		Base24Ath msgFromRemote = (Base24Ath) object;
		PosDataCode posDataCode = new PosDataCode(General.POSDATACODE);
		String posEntryMode;
		try {
			if (msgFromRemote.isFieldSet(Iso8583.Bit._022_POS_ENTRY_MODE)) {
				posEntryMode = msgFromRemote.getPosEntryMode().toString();
			} else {
				posEntryMode = "000";
			}
			switch (posEntryMode) {
			case (PosEntryMode.PanEntryMode._05_INTEGRATED_CIRCUIT_CARD + PosEntryMode.PinEntryCapability._1_YES):
				posDataCode.putCardDataInputCapability(PosDataCode.CardDataInputCapability._9_ICC);
				posDataCode.putCardholderAuthCapability(PosDataCode.CardholderAuthCapability._1_PIN);
				posDataCode.putCardCaptureCapability(PosDataCode.CardCaptureCapability._1_CAPTURE);
				posDataCode
						.putOperatingEnvironment(PosDataCode.OperatingEnvironment._2_UNATTENDED_ON_ACCEPTOR_PREMISES);
				posDataCode.putCardholderPresent(PosDataCode.CardholderPresent._0_PRESENT);
				posDataCode.putCardPresent(PosDataCode.CardPresent._1_PRESENT);
				posDataCode.putCardDataInputMode(PosDataCode.CardDataInputMode._5_ICC);
				posDataCode.putCardholderAuthMethod(PosDataCode.CardholderAuthMethod._1_PIN);
				posDataCode.putCardholderAuthEntity(PosDataCode.CardholderAuthEntity._0_NOT_AUTHENTICATED);
				posDataCode.putCardDataOutputCapability(PosDataCode.CardDataOutputCapability._1_NONE);
				posDataCode.putTerminalOutputCapability(PosDataCode.TerminalOutputCapability._4_PRINT_DISPLAY);
				posDataCode.putPinCaptureCapability(PosDataCode.PinCaptureCapability._4_FOUR);
				posDataCode.putTerminalOperator(PosDataCode.TerminalOperator._0_CUSTOMER);
				posDataCode.putTerminalType(PosDataCode.TerminalType._02_ATM);
				break;
			case (PosEntryMode.PanEntryMode._00_UNSPECIFIED + PosEntryMode.PinEntryCapability._0_UNSPECIFIED):
				posDataCode.putCardDataInputCapability(PosDataCode.CardDataInputCapability._0_UNKNOWN);
				posDataCode.putCardholderAuthCapability(PosDataCode.CardholderAuthCapability._0_NONE);
				posDataCode.putCardCaptureCapability(PosDataCode.CardCaptureCapability._0_NONE);
				posDataCode.putOperatingEnvironment(PosDataCode.OperatingEnvironment._0_NO_TERMINAL);
				posDataCode.putCardholderPresent(PosDataCode.CardholderPresent._1_NOT_PRESENT);
				posDataCode.putCardPresent(PosDataCode.CardPresent._0_NOT_PRESENT);
				posDataCode.putCardDataInputMode(PosDataCode.CardDataInputMode._0_UNKNOWN);
				posDataCode.putCardholderAuthMethod(PosDataCode.CardholderAuthMethod._0_NONE);
				posDataCode.putCardholderAuthEntity(PosDataCode.CardholderAuthEntity._0_NOT_AUTHENTICATED);
				posDataCode.putCardDataOutputCapability(PosDataCode.CardDataOutputCapability._0_UNKNOWN);
				posDataCode.putTerminalOutputCapability(PosDataCode.TerminalOutputCapability._0_UNKNOWN);
				posDataCode.putPinCaptureCapability(PosDataCode.PinCaptureCapability._1_UNKNOWN);
				posDataCode.putTerminalOperator(PosDataCode.TerminalOperator._0_CUSTOMER);
				posDataCode.putTerminalType(PosDataCode.TerminalType._00_ADMIN);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructPosDataCode]",
					Utils.getStringMessageException(e), msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructPosDataCode " + e.getMessage(), "LOG", this.nameInterface));
		}
		return posDataCode.toString();
	}

	/**************************************************************************************
	 * Contruye el campo privado SwitchKey (Bit 2) hacia el TM.
	 * 
	 * @param msg Mensaje.
	 * @return Retorna un objeto String data con el campo construido.
	 * @throws Exception En caso de error.
	 *************************************************************************************/
	public String constructSwitchKey(Iso8583 msg, String NameInterface) throws XPostilion {
		StringBuilder switchKey = new StringBuilder();
		try {

			switchKey.append(MsgType.toString(msg.getMsgType()))
					.append(msg.isFieldSet(Iso8583Post.Bit._037_RETRIEVAL_REF_NR)
							? msg.getField(Iso8583Post.Bit._037_RETRIEVAL_REF_NR)
							: String.valueOf(System.currentTimeMillis())
									.substring(String.valueOf(System.currentTimeMillis()).length() - 12))
					.append(msg.getField(Iso8583.Bit._013_DATE_LOCAL))
					.append(msg.getField(Iso8583.Bit._012_TIME_LOCAL));

			switch (NameInterface) {
			case Constants.ChannelID.IATH:
				switchKey.append(General.TWO_ZEROS);
				break;
			case Constants.ChannelID.ICRD:
				switchKey.append(Constants.General.NUMBER_99);
				break;
			default:
				switchKey.append(General.TWO_ZEROS);
				break;
			}

			switchKey.append(msg.getField(Iso8583Post.Bit._017_DATE_CAPTURE));

			// switchKey.append(MsgType.toString(msg.getMsgType()))
			// .append(msg.isFieldSet(Iso8583Post.Bit._037_RETRIEVAL_REF_NR)
			// ? msg.getField(Iso8583Post.Bit._037_RETRIEVAL_REF_NR)
			// : String.valueOf(System.currentTimeMillis())
			// .substring(String.valueOf(System.currentTimeMillis()).length() -
			// 12))
			// .append(msg.getField(Iso8583Post.Bit._007_TRANSMISSION_DATE_TIME)).append(General.TWO_ZEROS)
			// .append(msg.getField(Iso8583Post.Bit._013_DATE_LOCAL));
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructSwitchKey]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructSwitchKey " + e.getMessage(), "LOG", this.nameInterface));
		}
		return switchKey.toString();
	}

	/**************************************************************************************
	 * Build and return the Data Capture is get from structure data.
	 * 
	 * @param - Receive the object null
	 * @return - The value from structure data of tag B24_Field_17
	 *************************************************************************************/
	public String constructDateCapture(Object object, Integer num) {
		try {
			if (object instanceof Base24Ath) {
				Base24Ath msg = (Base24Ath) object;
				if (msg.isFieldSet(num))
					return msg.getField(num);
				else
					return Constants.General.FOUR_ZEROS;

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				if (msg.isFieldSet(num))
					return msg.getStructuredData().get("B24_Field_17");
				else
					return Constants.General.FOUR_ZEROS;
			}

		} catch (XPostilion e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructDateCapture]", Utils.getStringMessageException(e), "Unknown" }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
					"Exception in Method: constructDateCapture " + e.getMessage(), "LOG", this.nameInterface));
		}

		return null;
	}

	/**************************************************************************************
	 * Build and return field 44 is get from structure data.
	 * 
	 * @param - Receive the object null
	 * @return - The value from structure data of tag B24_Field_44
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField44(Object object, Integer num) throws XPostilion {
		String field = Constants.General.DEFAULT_P44;
		try {
			if (object instanceof Base24Ath) {
				field = Constants.General.DEFAULT_P44;
			} else if (object instanceof Iso8583Post) {
				Iso8583Post msgFromTm = (Iso8583Post) object;
				if ((msgFromTm.getField(Iso8583.Bit._003_PROCESSING_CODE).equals("011000")
						|| msgFromTm.getField(Iso8583.Bit._003_PROCESSING_CODE).equals("012000"))
						&& !msgFromTm.getField(Iso8583.Bit._022_POS_ENTRY_MODE).equals("051")) {
					field = Constants.General.DEFAULT_P44;
				} else if (msgFromTm.isPrivFieldSet(22)) {

					field = msgFromTm.getStructuredData().get("B24_Field_44") != null
							? msgFromTm.getStructuredData().get("B24_Field_44")
							: Constants.General.DEFAULT_P44;
				}
			}

		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructField44]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient
					.sendData(Client.getMsgKeyValue(((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Exception in Method: constructField44 " + e.getMessage(), "LOG", this.nameInterface));
		}
		return field;
	}

	/**************************************************************************************
	 * Build and return field 44 is get from structure data.
	 * 
	 * @param - Receive the object null
	 * @return - The value from structure data of tag B24_Field_44
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField61(Object object, Integer num) throws XPostilion {

		String field = Constants.General.DEFAULT_P44;
		try {
			if (object instanceof Base24Ath) {
				field = "";
			} else if (object instanceof Iso8583Post) {
				field = "";
			}

		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructField61]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient
					.sendData(Client.getMsgKeyValue(((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Exception in Method: constructField61 " + e.getMessage(), "LOG", this.nameInterface));
		}
		return field;
	}

	/**************************************************************************************
	 * Build and return field 48 is get from structure data.
	 * 
	 * @param - Receive the object null
	 * @return - The value from structure data of tag B24_Field_48
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField48(Object object, Integer num) throws XPostilion {

		Iso8583Post msgFromTm = (Iso8583Post) object;
		String field48 = "";
		String field37 = "Unknown";
		try {
			field37 = msgFromTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			return msgFromTm.getStructuredData().get("B24_Field_48");
		} catch (XPostilion e) {
			field48 = construct0210ErrorFields(object, num);
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructField48]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(field37,
					"Exception in Method: constructField48 " + Utils.getStringMessageException(e), "LOG",
					this.nameInterface));
		}
		return field48;
	}

	/**************************************************************************************
	 * Build and return field 22 Default.
	 * 
	 * @param - Receive the object null
	 * @return - The value 22 default
	 *************************************************************************************/
	public static String constructField22(Object object, Integer num) {

		try {
			return Constants.General.DEFAULT_P22;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**************************************************************************************
	 * Build and return field 43 Default.
	 * 
	 * @param - Receive the object null
	 * @return - The value 43 default
	 *************************************************************************************/
	public static String constructField43(Object object, Integer num) {

		try {
			return Constants.General.DEFAULT_P43;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**************************************************************************************
	 * Build and return field 126 is get from structure data.
	 * 
	 * @param - Receive the object null
	 * @return - The value from structure data of tag B24_Field_48
	 *************************************************************************************/
	public static String constructField126(Object object, Integer num) {

		Iso8583Post msgFromTm = (Iso8583Post) object;

		try {
			return msgFromTm.getStructuredData().get("B24_Field_126");
		} catch (XPostilion e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Valida si es necesario poner el valor de la comisión en el campo 126 hacia el
	 * remoto
	 * 
	 * @param msg_to_remote mensaje a enviar a el remoto
	 * @return variable booleana true indica que es necesario poner la comisión
	 * @throws XPostilion al intentar obtener el código de respuesta y el campo 126
	 */

	public String constructField126IsoTranslate(Object object, Integer num) {
		String p37 = null;
		try {
			String field126 = "& 0000000000! QT00032 0000000000000000000000000000000 ";
			if (object instanceof Base24Ath) {
				Base24Ath msg = (Base24Ath) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num))
					field126 = msg.getField(num);

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				int initialLength = field126.length();
				String field39 = msg.getField(Iso8583Post.Bit._039_RSP_CODE);
				if (msg.isApprovedResponse()) {
					field126 = constructField126IsoTranslateApprove(msg, field39);

				} else {
					if (msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA)) {
						if (msg.getStructuredData().get(
								"B24_Field_126") != null) {
							field126 = msg.getStructuredData()
									.get("B24_Field_126");
							initialLength = field126.length();
						}
					}
					ResponseCode responseCode;
					try {
						responseCode = InitialLoadFilter.getFilterCodeIsoToB24(field39, this.allCodesIsoToB24);
					} catch (NoSuchElementException e) {
						
						
						udpClient.sendData(
								Client.getMsgKeyValue("N/A", "Exception en Mensaje: " + msg.toString(), "ERR", nameInterface));

						this.udpClient
								.sendData(
										Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
												"NoSuchElementException in Method: constructField126IsoTranslate "
														+ Utils.getStringMessageException(e),
												"LOG", this.nameInterface));
						if (new DBHandler(this.params).updateResgistry(field39, "0",responseCodesVersion)) {
							this.allCodesIsoToB24 = postilion.realtime.library.common.db.DBHandler
									.getResponseCodes(false, "0",responseCodesVersion);
							responseCode = InitialLoadFilter.getFilterCodeIsoToB24(field39, this.allCodesIsoToB24);
						} else {
							responseCode = new ResponseCode("10002", "Error Code could not extracted from message",
									field39, field39);
							EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
									ConstructFieldMessage.class.getName(), "Method: [constructField126IsoTranslate]",
									Utils.getStringMessageException(e),
									msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
							EventRecorder.recordEvent(e);
							this.udpClient
									.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
											"NoSuchElementException in Method: constructField126IsoTranslate value"
													+ field39 + "is not in the table",
											"LOG", this.nameInterface));
						}
					}
					field126 = field126.substring(0, initialLength - 14)
							.concat(Pack.resize(responseCode.getCommision() + General.VOIDSTRING, 12,
									Constants.General.ZERO, false))
							.concat(field126.substring(initialLength - 2, initialLength));
					if ((msg.getProcessingCode().toString().equals(Constants.Channels.PCODE_RETIRO_ATM_A)
							|| msg.getProcessingCode().toString().equals(Constants.Channels.PCODE_RETIRO_ATM_C)))
						field126 = putToken13(msg.getStructuredData(), field126, false);
				}
			}
			if (field126.length() > 54) {
				String[] parts = field126.split("!");
				int initialLength = field126.length();
				String inlength = String.valueOf(initialLength);

				field126 = "& 0000" + parts.length + "00" + inlength.concat(field126.substring(12, initialLength));
			}

			return field126;

		} catch (XPostilion | NullPointerException | SQLException e) {
			
			String strMsg=null;
			
			if (object instanceof Base24Ath) {
				Base24Ath msg = (Base24Ath) object;
				strMsg=msg.toString();
				
			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				strMsg=msg.toString();
			}
			
			udpClient.sendData(
					Client.getMsgKeyValue("N/A", "Exception en Mensaje: " + strMsg , "ERR", nameInterface));
			
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructField126IsoTranslate]",
					Utils.getStringMessageException(e), "Unknown" }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: constructField126IsoTranslate " + e.getMessage(), "LOG", this.nameInterface));
		}

		return Constants.General.DEFAULT_126;// devolver uno por defecto

	}

	public String constructField126IsoTranslateApprove(Iso8583Post msg, String field39) throws XPostilion {
		String field126 = "";
		try {
			StructuredData sdFromTm = msg.getStructuredData();
			field126 = sdFromTm.get("B24_Field_126");
			int initialLength = field126.length();
			boolean isClientFailure = true;
			if (sdFromTm != null) {
				if (sdFromTm.get(Constants.General.ERROR_CODE) != null
						&& sdFromTm.get(
								Constants.General.COMMISION) == null) {
					ResponseCode responseCode;
					try {
						responseCode = InitialLoadFilter.getFilterCodeISCToIso(
								sdFromTm.get(
										Constants.General.ERROR_CODE),
								this.allCodesIscToIso);
					} catch (NoSuchElementException e) {
						this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
								"NoSuchElementException in Method: constructField126IsoTranslateApprove "
										+ Utils.getStringMessageException(e),
								"LOG", this.nameInterface));
						if (new DBHandler(this.params).updateResgistry(
								sdFromTm.get(
										Constants.General.ERROR_CODE),
								"0",responseCodesVersion)) {
							this.allCodesIscToIso = postilion.realtime.library.common.db.DBHandler
									.getResponseCodes(true, "0",responseCodesVersion);
							responseCode = InitialLoadFilter.getFilterCodeIsoToB24(sdFromTm.get(
									Constants.General.ERROR_CODE),
									this.allCodesIscToIso);
						} else {
							responseCode = new ResponseCode("10002", "Error Code could not extracted from message",
									sdFromTm.get(
											Constants.General.ERROR_CODE),
									sdFromTm.get(
											Constants.General.ERROR_CODE));
							EventRecorder.recordEvent(new TryCatchException(
									new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
											"Method: [constructField126IsoTranslateApprove]",
											Utils.getStringMessageException(e),
											msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
							EventRecorder.recordEvent(e);
							this.udpClient.sendData(Client.getMsgKeyValue(
									msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
									"NoSuchElementException in Method: constructField126IsoTranslateApprove value"
											+ sdFromTm.get(
													Constants.General.ERROR_CODE)
											+ " is not in the table.",
									"LOG", this.nameInterface));
						}
					}
					field126 = field126.substring(0, initialLength - 14)
							.concat(Pack.resize(responseCode.getCommision() + General.VOIDSTRING, 12,
									Constants.General.ZERO, false))
							.concat(field126.substring(initialLength - 2, initialLength));
					isClientFailure = false;
				} else if (sdFromTm
						.get(Constants.General.ERROR_CODE) == null
						&& sdFromTm.get(
								Constants.General.COMMISION) != null) {
					String tranType = sdFromTm.get("B24_Field_3");
					if (tranType != null)
						field126 = tranType.equals(TranType._89_FEE_INQUIRY)
								? addCommision(field126, initialLength, sdFromTm.get(
										Constants.General.COMMISION))
								: field126;
					isClientFailure = false;
				}
				if ((msg.getProcessingCode().toString().equals(Constants.Channels.PCODE_RETIRO_ATM_A)
						|| msg.getProcessingCode().toString().equals(Constants.Channels.PCODE_RETIRO_ATM_C)))
					field126 = putToken13(sdFromTm, field126, true);
			}
			if (isClientFailure) {
				ResponseCode responseCode;
				try {
					responseCode = InitialLoadFilter.getFilterCodeIsoToB24(field39, this.allCodesIsoToB24);
				} catch (NoSuchElementException e) {
					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"NoSuchElementException in Method: constructField126IsoTranslateApprove "
									+ Utils.getStringMessageException(e),
							"LOG", this.nameInterface));
					if (new DBHandler(this.params).updateResgistry(field39, "0",responseCodesVersion)) {

						this.allCodesIsoToB24 = postilion.realtime.library.common.db.DBHandler.getResponseCodes(false,
								"0",responseCodesVersion);
						responseCode = InitialLoadFilter.getFilterCodeIsoToB24(field39, this.allCodesIsoToB24);
					} else {
						responseCode = new ResponseCode("10002", "Error Code could not extracted from message", field39,
								field39);
						EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
								ConstructFieldMessage.class.getName(), "Method: [constructField126IsoTranslateApprove]",
								Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
						EventRecorder.recordEvent(e);
						this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
								"NoSuchElementException in Method: constructField126IsoTranslateApprove value "
										+ field39 + " is not in the table.",
								"LOG", this.nameInterface));
					}
				}
				field126 = field126.substring(0, initialLength - 14)
						.concat(Pack.resize(responseCode.getCommision() + General.VOIDSTRING, 12,
								Constants.General.ZERO, false))
						.concat(field126.substring(initialLength - 2, initialLength));
			}
		} catch (SQLException e1) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructField126IsoTranslateApprove]",
					Utils.getStringMessageException(e1), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e1);
		}
		return field126;

	}

	/**
	 * It puts token13 in field 126 with data of "Preaporbado"
	 * 
	 * @param sdFromTm campo 127.22
	 * @param field126 a modificar
	 * @return field126
	 */
	public String putToken13(StructuredData sdFromTm, String field126, boolean fromSd) {
		String tagPaMonto = null;
		String tagPaCuota = null;
		String tagPaTasa = null;
		String tagPaPlazo = null;
		if (fromSd) {
			tagPaMonto = sdFromTm.get("PA_MONTO");
			tagPaCuota = sdFromTm.get("PA_CUOTA");
			tagPaTasa = sdFromTm.get("PA_TASA");
			tagPaPlazo = sdFromTm.get("PA_PLAZO");
		}
		if (tagPaMonto != null && tagPaCuota != null && tagPaTasa != null && tagPaPlazo != null && fromSd) {
			field126 = field126.concat("! 1300042 ").concat(Pack.resize(tagPaMonto, 12, '0', false))
					.concat(Pack.resize(tagPaCuota, 12, '0', false)).concat(Pack.resize(tagPaTasa, 10, '0', false))
					.concat(Pack.resize(tagPaPlazo, 8, '0', false));
		} else {
			field126 = field126.concat("! 1300042 ").concat(Pack.resize("0", 12, '0', false))
					.concat(Pack.resize("0", 12, '0', false)).concat(Pack.resize("0", 10, '0', false))
					.concat(Pack.resize("0", 8, '0', false));
		}
		return field126;
	}

	/**************************************************************************************
	 * Build and return the Service Restriction Code
	 * 
	 * @param - Receive the object null
	 * @return - The value 000 by default
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructServiceRestrictionCode(Object object, Integer num) throws XPostilion {
		try {
			return ServiceRestrictionCode.THREE_ZEROS;
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructServiceRestrictionCode]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient
					.sendData(Client.getMsgKeyValue(((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Exception in Method: constructServiceRestrictionCode " + e.getMessage(), "LOG",
							this.nameInterface));

		}
		return null;
	}

	/**************************************************************************************
	 * Build and return the Service Response Data
	 * 
	 * @param - Receive the object null
	 * @return - The value 0000000000000000000000000 length 25
	 * @throws XPostilion
	 *************************************************************************************/
	public String constuctServiceRspData(Object object, Integer num) throws XPostilion {
		try {
			return AdditionalRspData.DEFAULT;
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constuctServiceRspData]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(
					((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constuctServiceRspData " + e.getMessage(), "LOG", this.nameInterface));
		}
		return null;
	}

	/**************************************************************************************
	 * Construye el campo 102 hacia la interchange
	 * 
	 * @param object mensaje que va hacia la interchange.
	 * @return String con el campo 102 lleno de ceros para completar la longitud de
	 *         18
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructAccountIdentification(Object object, Integer num) throws XPostilion {
		String field102 = null;
		String p37 = null;
		try {
			if (object instanceof Base24Ath) {
				Base24Ath msg = (Base24Ath) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num))
					field102 = msg.getField(num);

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;

				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				boolean lengthCondition = lengthField102 == 18;
				if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)) {
					field102 = ((lengthCondition) ? "" : Constants.Config.ID_BBOGOTA) + Pack.resize(
							msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
							((lengthCondition) ? lengthField102 : (lengthField102 - Constants.Account.NUM_FOUR)),
							Account.ZERO_FILLER, false);
				}
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructAccountIdentification]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(
					Client.getMsgKeyValue(p37, "Exception in Method: constructAccountIdentification " + e.getMessage(),
							"LOG", this.nameInterface));
		}
		return field102;
	}

	/**************************************************************************************
	 * Construye el campo 54 hacia la interchange
	 * 
	 * @param object mensaje que va hacia la interchange.
	 * @return String con el campo 54 obtenido del 200
	 * 
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField54(Object object, Integer num) throws XPostilion {
		String field = Constants.General.DEFAULT_P54;

		String p37 = null;
		try {
			if (object instanceof Base24Ath) {
				Base24Ath msg = (Base24Ath) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num))
					field = msg.getField(num);

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msgFromTm = (Iso8583Post) object;
				p37 = msgFromTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msgFromTm.isPrivFieldSet(22)) {
					field = msgFromTm.getStructuredData().get("B24_Field_54") != null
							? msgFromTm.getStructuredData().get("B24_Field_54")
							: Constants.General.DEFAULT_P54;
				}
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructField54]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: constructField54 " + e.getMessage(), "LOG", this.nameInterface));
		}
		return field;
	}

	/**************************************************************************************
	 * Construye el campo 102 hacia la interchange
	 * 
	 * @param object mensaje que va hacia la interchange.
	 * @return String con el campo 102 obtenido del 200
	 * 
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField102fromStructureData(Object object, Integer num) throws XPostilion {
		Iso8583Post msg = null;
		try {
			msg = (Iso8583Post) object;

			return msg.getStructuredData().get("CLIENT_ACCOUNT_NR") == null
					? msg.getStructuredData().get("CORRES_ACCOUNT_NR")
					: msg.getStructuredData().get("CLIENT_ACCOUNT_NR");

		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructField102fromStructureData]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructField102fromStructureData " + e.getMessage(), "LOG",
					this.nameInterface));
		}
		return null;
	}

	/**************************************************************************************
	 * Construye el campo 54 hacia la interchange
	 * 
	 * @param object mensaje que va hacia la interchange.
	 * @return String con el campo 54 obtenido del 200
	 * 
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructDefaultField54(Object object, Integer num) throws XPostilion {
		try {
			return Constants.General.DEFAULT_P54_CONSULTA_COSTO_X60;
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructDefaultField54]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient
					.sendData(Client.getMsgKeyValue(((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Exception in Method: constructField54 " + e.getMessage(), "LOG", this.nameInterface));
		}
		return null;
	}

	/**************************************************************************************
	 * Construye el campo 62 hacia la interchange
	 * 
	 * @param object mensaje que va hacia la interchange.
	 * @return String con el campo 62 obtenido del 200
	 * 
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructFieldFromStructuredData(Object object, Integer num) throws XPostilion {

		String field37 = null;
		String field62 = "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
		try {
			this.udpClient.sendData(Client.getMsgKeyValue("9999", "ENTRO A constructFieldFromStructuredData", "LOG",
					this.nameInterface));
			this.udpClient.sendData(Client.getMsgKeyValue("9999", "CAMPO:" + num, "LOG", this.nameInterface));

			if (object instanceof Base24Ath) {
				Base24Ath msg = (Base24Ath) object;
				field37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);

				field62 = Constants.General.DEFAULT_ORIGINAL_DATA_ELEMENTS;

			} else if (object instanceof Iso8583Post) {

				Iso8583Post msg = (Iso8583Post) object;
				field37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				this.udpClient
						.sendData(Client.getMsgKeyValue("9999", "Convirtio a Iso8583Post", "LOG", this.nameInterface));
				if (msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA))
					field62 = msg.getStructuredData().get("B24_Field_" + num.toString());

			}

		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructFieldFromStructuredData]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(field37,
					"Exception in Method: constructFieldFromStructuredData" + e.getMessage(), "LOG",
					this.nameInterface));

		}
		return field62;

	}

	/**************************************************************************************
	 * Construye el campo 48 hacia la interchange
	 * 
	 * @param object mensaje que va hacia la interchange.
	 * @return String con el campo 48 obtenido del 0210
	 * 
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructFieldFromStructuredDataP48(Object object, Integer num) throws XPostilion {
		StringBuilder sbFields = new StringBuilder();
		if (object instanceof Base24Ath) {
			Base24Ath msg = (Base24Ath) object;
			if (msg.isFieldSet(num))
				sbFields.append(msg.getField(num));

		} else if (object instanceof Iso8583Post) {
			Iso8583Post msgFromTm = (Iso8583Post) object;
			if (msgFromTm.isPrivFieldSet(22)) {
				sbFields.append(msgFromTm.getStructuredData().get("B24_Field_" + num.toString()));
				if (sbFields.substring(sbFields.length() - 1).equals("2")
						|| sbFields.substring(sbFields.length() - 1).equals("6")
						|| sbFields.substring(sbFields.length() - 1).equals("7")) {

					sbFields.setCharAt(sbFields.length() - 1, '2');

				}
			} else {

				Base24Ath msgOriginalB24 = (Base24Ath) this.sourceTranToTmHashtableB24
						.get(msgFromTm.getField(Iso8583Post.Bit._037_RETRIEVAL_REF_NR));
				sbFields.append(msgOriginalB24.getField(Iso8583.Bit._048_ADDITIONAL_DATA));
				// sbFields.append(construct0210ErrorFields(object, num));
			}
		}
		return sbFields.toString();

	}

//	/**************************************************************************************
//	 * Build field 35 to TM
//	 * 
//	 * @param reveived the object by default
//	 * @return - The map object with the information the account transaction
//	 * @throws XPostilion
//	 *************************************************************************************/
//	public Object constructAccount(Object object, Integer num) throws XPostilion {
//		Base24Ath msg = (Base24Ath) object;
//
//		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//				"ENTRO A constructAccount", "LOG", this.nameInterface));
//
//		HashMap<String, String> map = new HashMap<>();
//
//		String account = null;
//		String customerId;
//		String customerName;
//		String cardClas;
//		String pCodeStr;
//		String accountType;
//		lengthField102 = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1) != null
//				? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length()
//				: 18;
//
//		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//				"lengthField102 :" + lengthField102, "LOG", this.nameInterface));
//
//		if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)) {
//
//			this.consultInfoDefault = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
//					.equals(Constants.Account.ACCOUNT_DEFAULT)
//					|| Integer.parseInt((msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
//							.substring(lengthField102 - Constants.Account.NUM_SEVENTEEN))) == 0;
//
//		} else {
//			this.consultInfoDefault = true;
//		}
//
//		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//				"consultInfoDefault :" + this.consultInfoDefault, "LOG", this.nameInterface));
//		try {
//			String[] accountInfo = new String[6];
//
//			if (this.consultInfoDefault) {
//				accountInfo = new DBHandler(this.params).accounts(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//						msg.getTrack2Data().getPan().substring(Constants.Indexes.FIELD35_POSITION_0,
//								Constants.Indexes.FIELD35_POSITION_6),
//						Utils.getHashPan(msg.getTrack2Data()), msg.getProcessingCode().getFromAccount(),
//						msg.getTrack2Data().getExpiryDate());
//
//				if (accountInfo[Account.ID] != null) {
//					account = Utils.getClearAccount(accountInfo[Account.ID]);
//				}
//			} else {
//
//				accountInfo = new DBHandler(this.params).infoAccount(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//						msg.getTrack2Data().getPan().substring(Constants.Indexes.FIELD35_POSITION_0,
//								Constants.Indexes.FIELD35_POSITION_6),
//						Utils.getHashPan(msg.getTrack2Data()), msg.getProcessingCode().getFromAccount(),
//						msg.getTrack2Data().getExpiryDate());
//
//				account = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1);
//				accountInfo[Account.CORRECT_PROCESSING_CODE] = Account.TRUE;
//			}
//
//			customerId = accountInfo[Account.CUSTOMER_ID];
//			customerName = accountInfo[Account.CUSTOMER_NAME];
//
////			 if (msg.getProcessingCode().toString().equals("890000") &&
//			if (!this.consultInfoDefault) {
//
//				accountType = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1);
//				account = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1);
//
//			} else {
//				accountType = accountInfo[Account.TYPE];
//			}
//
////			accountType = accountInfo[Account.TYPE];
//
//			cardClas = accountInfo[Account.PROTECTED_CARD_CLASS];
//			pCodeStr = null;
//			correctTypeAccount = accountType;
//
//			if (accountInfo[Account.CORRECT_PROCESSING_CODE] != null) {
//
//				if (accountInfo[Account.CORRECT_PROCESSING_CODE].equals(Account.TRUE)) {
//
//					pCodeStr = Account.DEFAULT_PROCESSING_CODE;
//
//				} else if (accountInfo[Account.CORRECT_PROCESSING_CODE].equals(Account.FALSE)) {
//					ProcessingCode pCode = msg.getProcessingCode();
//					pCodeStr = new StringBuilder().append(pCode.getTranType()).append(accountInfo[Account.TYPE])
//							.append(pCode.getToAccount()).toString();
//				}
//			}
//			map.put(Constants.Config.WORD_FIELD, account);
//			map.put(Constants.Config.WORD_VALIDATION, accountType);
//			map.put(TagNameStructuredData.CUSTOMER_ID, customerId);
//			map.put(TagNameStructuredData.CUSTOMER_NAME, customerName);
//			map.put(TagNameStructuredData.PROCESSING_CODE, pCodeStr);
//			map.put(TagNameStructuredData.PROTECTED_CARD_CLASS, cardClas);
//
//			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//					"map: " + map, "LOG", this.nameInterface));
//
//			return map;
//		} catch (Exception e) {
//			EventRecorder.recordEvent(new TryCatchException(new String[] { ConstructFieldMessage.class.getName(),
//					"Method: [constructAccount]", e.getMessage() }));
//			EventRecorder.recordEvent(e);
//			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//					"Exception in Method: constructAccount " + e.getMessage(), "LOG", this.nameInterface));
//		}
//		return map;
//	}

	/**************************************************************************************
	 * Transform the field 41 to TM
	 * 
	 * @param - reveived the object by default
	 * @return - The string with information by field 41
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructAdditionalRspData(Object object, Integer num) throws XPostilion {
		String p37 = null;
		try {

			if (object instanceof Base24Ath) {
				Base24Ath msg = (Base24Ath) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				return Pack.resize(msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID), 16, ' ', true);

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				return Pack.resize(msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID), 16, ' ', true);
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructAdditionalRspData]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: constructAdditionalRspData " + e.getMessage(), "LOG", this.nameInterface));
		}
		return null;
	}

	/**************************************************************************************
	 * Transform the field 38 to TM
	 * 
	 * @param - reveived the object by default
	 * @return - The string with information by field 38
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField38DefaultOrCopy(Object object, Integer num) throws XPostilion {
		String p37 = null;
		try {
			if (object instanceof Base24Ath) {
				Base24Ath msg = (Base24Ath) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num))
					return msg.getField(num);
				else
					return Constants.General.SIX_ZEROS;

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num))
					return msg.getField(num);
				else
					return Constants.General.SIX_ZEROS;
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructField38DefaultOrCopy]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);

			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: constructField38DefaultOrCopy " + Utils.getStringMessageException(e), "LOG",
					this.nameInterface));
		}
		return Constants.General.SIX_ZEROS;
	}

	public String constructFields6061DefaultOrCopy(Object object, Integer num) throws XPostilion {
		String p37 = null;
		try {
			if (object instanceof Base24Ath) {
				Base24Ath msg = (Base24Ath) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num))
					return msg.getField(num);
				else
					return Constants.General.SIX_ZEROS;

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num))
					return msg.getField(num);
				else {
					StructuredData sd = msg.getStructuredData();
					String field;
					if (sd != null)
						field = sd.get("B24_Field_" + num.toString());
					else
						field = Constants.General.SIX_ZEROS;
					return field;
				}

			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { ConstructFieldMessage.class.getName(),
					"Method: [constructFields6061DefaultOrCopy]", Utils.getStringMessageException(e) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: constructFields6061DefaultOrCopy " + Utils.getStringMessageException(e),
					"LOG", this.nameInterface));

		}
		return Constants.General.SIX_ZEROS;
	}

	public String constructField105DefaultOrCopy(Object object, Integer num) throws XPostilion {
		String p37 = null;
		String field105 = Constants.General.DEFAULT_P105;
		try {
			if (object instanceof Base24Ath) {
				return Constants.General.DEFAULT_P105;

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;

				if (msg.getStructuredData() == null) {
					return Constants.General.DEFAULT_P105;
				}

				String field41 = msg.getStructuredData().get("B24_Field_41");

				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"constructField105 CAMPO41: " + field41, "LOG", this.nameInterface));
				if (field41.substring(12, 13).equals("7")) {
					switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
					case "021":
					case "010":

						switch (msg.getField(Iso8583.Bit._003_PROCESSING_CODE)) {
						case Constants.Channels.PCODE_PAGO_SP_CNB_A:
						case Constants.Channels.PCODE_PAGO_SP_CNB_C:

							if (!msg.getStructuredData().get("B24_Field_103").substring(0, 1).equals("2")) {

								return Constants.General.DEFAULT_P105_MIXTA;

							}

							break;
						default:

							if (!msg.getStructuredData().get("B24_Field_103").substring(2, 3).equals("1")) {

								return Constants.General.DEFAULT_P105_MIXTA;
							}

							break;
						}

						break;

					default:

						break;
					}

				}

			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"constructField105DefaultOrCopy", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);

			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: constructField38DefaultOrCopy " + e.getMessage(), "LOG", this.nameInterface));
		}
		return field105;
	}

	/**************************************************************************************
	 * Transform the field 38 to TM
	 * 
	 * @param - reveived the object by default
	 * @return - The string with information by field 38
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField102_103ConsultaCosto(Object object, Integer num) throws XPostilion {
		String p37 = null;
		this.udpClient.sendData(Client.getMsgKeyValue("999999", "Entro Method: constructField102_103ConsultaCosto ",
				"LOG", this.nameInterface));
		try {
			String field = null;

			if (object instanceof Base24Ath) {

				Base24Ath msg = (Base24Ath) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num)) {

					field = Pack.resize(msg.getField(num), 28, Account.ZERO_FILLER, false);

					return field;

				} else
					return Constants.General.TWENTYONE_ZEROS;

			} else if (object instanceof Iso8583Post) {
				this.udpClient
						.sendData(Client.getMsgKeyValue("999999", "instanceof Iso8583Post", "LOG", this.nameInterface));
				Iso8583Post msg = (Iso8583Post) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num)) {
					this.udpClient
							.sendData(Client.getMsgKeyValue("999999", "campo seteado", "LOG", this.nameInterface));
					field = Pack.resize(msg.getField(num), 28, Account.ZERO_FILLER, false);
					this.udpClient.sendData(Client.getMsgKeyValue("999999", "campo " + num.toString() + ": " + field,
							"LOG", this.nameInterface));
					return field;

				} else
					return Constants.General.TWENTYONE_ZEROS;
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"constructField105DefaultOrCopy", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: constructField102_103ConsultaCosto " + e.getMessage(), "LOG",
					this.nameInterface));
		}
		return null;
	}

	public String constructField102ConsultaCostoTransferencia(Object object, Integer num) throws XPostilion {
		String p37 = null;
		this.udpClient.sendData(Client.getMsgKeyValue("999999",
				"Entro Method: constructField102ConsultaCostoTransferencia ", "LOG", this.nameInterface));
		try {
			String field = null;

			if (object instanceof Base24Ath) {

				Base24Ath msg = (Base24Ath) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num)) {

					field = Pack.resize(msg.getField(num), 21, Account.ZERO_FILLER, false);

					return field;

				} else
					return Constants.General.TWENTYONE_ZEROS;

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num)) {
					field = Pack.resize(msg.getField(num), 21, Account.ZERO_FILLER, false);
					return field;

				} else
					return Constants.General.TWENTYONE_ZEROS;
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"constructField102ConsultaCostoTransferencia", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: constructField102ConsultaCostoTransferencia " + e.getMessage(), "LOG",
					this.nameInterface));
		}
		return null;
	}

	/**************************************************************************************
	 * Transform the field 38 to TM
	 * 
	 * @param - reveived the object by default
	 * @return - The string with information by field 38
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField104Deposito(Object object, Integer num) throws XPostilion {
		String p37 = null;
		try {
			String field = null;

			if (object instanceof Base24Ath) {

				Base24Ath msg = (Base24Ath) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num)) {

					field = Pack.resize(msg.getField(num), 24, Account.ZERO_FILLER, false);

					return field;

				} else
					return Constants.General.TWENTYFOUR_ZEROS;

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num)) {
					field = Pack.resize(msg.getField(num), 24, Account.ZERO_FILLER, false);

					return field;

				} else
					return Constants.General.TWENTYFOUR_ZEROS;
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructField104Deposito]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: constructField104Deposito " + e.getMessage(), "LOG", this.nameInterface));
		}
		return null;
	}

	/**************************************************************************************
	 * Transform the field 38 to TM
	 * 
	 * @param - reveived the object by default
	 * @return - The string with information by field 38
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField102DefaultOrCopy(Object object, Integer num) throws XPostilion {
		String p37 = null;
		try {
			String field102 = null;
			boolean lengthCondition = false;
			if (object instanceof Base24Ath) {

				Base24Ath msg = (Base24Ath) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num)) {
//					lengthField102 = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1) != null
//							? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length()
//							: 18;
//					lengthCondition = lengthField102 == 18;
//					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//							"msg.getField:" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1), "LOG", this.nameInterface));
//					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//							"lengthField102:" + lengthField102, "LOG", this.nameInterface));
//
//					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//							"DENTRO DEL IF", "LOG", this.nameInterface));
//					field102 = ((lengthCondition) ? "" : Constants.Config.ID_BBOGOTA) + Pack.resize(
//							msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
//							((lengthCondition) ? lengthField102 : (lengthField102 - Constants.Account.NUM_FOUR)),
//							Account.ZERO_FILLER, false);
					field102 = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1);
					return field102;

				} else
					return Constants.General.TWENTYONE_ZEROS;

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				p37 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
				if (msg.isFieldSet(num)) {
					lengthField102 = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1) != null
							? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length()
							: 18;
					lengthCondition = lengthField102 == 18;
					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"msg.getField:" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1), "LOG", this.nameInterface));
					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"lengthField102:" + lengthField102, "LOG", this.nameInterface));

					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"DENTRO DEL IF", "LOG", this.nameInterface));
					field102 = ((lengthCondition) ? "" : Constants.Config.ID_BBOGOTA) + Pack.resize(
							msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
							((lengthCondition) ? lengthField102 : (lengthField102 - Constants.Account.NUM_FOUR)),
							Account.ZERO_FILLER, false);

					return field102;

				} else
					return Constants.General.TWENTYONE_ZEROS;
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructField102DefaultOrCopy]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(
					Client.getMsgKeyValue(p37, "Exception in Method: constructField102DefaultOrCopy " + e.getMessage(),
							"LOG", this.nameInterface));
		}
		return null;
	}

	/**************************************************************************************
	 * Build the code error for field 39
	 * 
	 * @param - the object boolean for validate code error
	 * @return - String with the code error
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructResponseCode(Object object, Integer num) throws XPostilion {
		Boolean error = false;
		String codeRsp = null;
		try {
			if (object instanceof Boolean) {
				error = (Boolean) object;
				if (error) {
					codeRsp = Constants.Config.CODE_ERROR_30;
				}
			} else if (object instanceof Base24Ath) {
				codeRsp = Constants.Config.CODE_ERROR_30;
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructResponseCode]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient
					.sendData(Client.getMsgKeyValue(((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Exception in Method: constructResponseCode " + e.getMessage(), "LOG", this.nameInterface));
		}
		return codeRsp;
	}

	/**************************************************************************************
	 * Build field 63 Response Code
	 * 
	 * @param - Received the object message B24 or ISO
	 * @return - information of field 63 Code
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructResponseCodeField63(Object object, Integer num) throws XPostilion {
		String code = null;
		try {
			if (object instanceof Base24Ath) {
				Base24Ath msg = (Base24Ath) object;
				if (msg.isFieldSet(Iso8583.Bit._039_RSP_CODE)) {
					ResponseCode responseCode;
					try {
						responseCode = InitialLoadFilter.getFilterCodeIsoToB24(
								msg.getField(Iso8583Post.Bit._039_RSP_CODE), this.allCodesB24ToIso);
					} catch (NoSuchElementException e) {
						this.udpClient
								.sendData(
										Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
												"NoSuchElementException in Method: constructResponseCodeField63 "
														+ Utils.getStringMessageException(e),
												"LOG", this.nameInterface));
						if (new DBHandler(this.params).updateResgistry(msg.getField(Iso8583Post.Bit._039_RSP_CODE),
								"1",responseCodesVersion)) {
							this.allCodesB24ToIso = postilion.realtime.library.common.db.DBHandler
									.getResponseCodesBase24("1",responseCodesVersion);
							responseCode = InitialLoadFilter.getFilterCodeIsoToB24(
									msg.getField(Iso8583Post.Bit._039_RSP_CODE), this.allCodesB24ToIso);
						} else {
							responseCode = new ResponseCode("10002", "Error Code could not extracted from message",
									msg.getField(Iso8583Post.Bit._039_RSP_CODE),
									msg.getField(Iso8583Post.Bit._039_RSP_CODE));
							EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
									ConstructFieldMessage.class.getName(), "Method: [constructResponseCodeField63]",
									Utils.getStringMessageException(e),
									msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
							EventRecorder.recordEvent(e);
							this.udpClient.sendData(Client.getMsgKeyValue(
									msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
									"NoSuchElementException in Method: constructResponseCodeField63 value"
											+ msg.getField(Iso8583Post.Bit._039_RSP_CODE) + " is not in the table.",
									"LOG", this.nameInterface));
						}
					}
					code = Pack.resize(responseCode.getKeyIsc() + responseCode.getDescriptionIsc(), General.LENGTH_44,
							General.SPACE, true);

				}
			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				StructuredData sd = new StructuredData();
				if (msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA)) {
					sd = msg.getStructuredData();
				}
				if (!sd.isEmpty() && sd.get(Constants.Config.FIELD_B24_63) != null) {
					code = sd.get(Constants.Config.FIELD_B24_63).length() == 44 ? sd.get(Constants.Config.FIELD_B24_63)
							: sd.get(Constants.Config.FIELD_B24_63).substring(0, 44);
				} else {
					if (msg.isFieldSet(Iso8583.Bit._039_RSP_CODE)) {
						ResponseCode responseCode;
						try {
							responseCode = InitialLoadFilter.getFilterCodeIsoToB24(
									msg.getField(Iso8583Post.Bit._039_RSP_CODE), this.allCodesIsoToB24TM);
						} catch (NoSuchElementException e) {
							this.udpClient
									.sendData(
											Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
													"NoSuchElementException in Method: constructResponseCodeField63 "
															+ Utils.getStringMessageException(e),
													"LOG", this.nameInterface));
							if (new DBHandler(this.params).updateResgistry(msg.getField(Iso8583Post.Bit._039_RSP_CODE),
									"1",responseCodesVersion)) {
								this.allCodesIsoToB24TM = postilion.realtime.library.common.db.DBHandler
										.getResponseCodes(false, "1",responseCodesVersion);
								responseCode = InitialLoadFilter.getFilterCodeIsoToB24(
										msg.getField(Iso8583Post.Bit._039_RSP_CODE), this.allCodesIsoToB24TM);
							} else {
								responseCode = new ResponseCode("10002", "Error Code could not extracted from message",
										msg.getField(Iso8583Post.Bit._039_RSP_CODE),
										msg.getField(Iso8583Post.Bit._039_RSP_CODE));
								EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
										ConstructFieldMessage.class.getName(), "Method: [constructResponseCodeField63]",
										Utils.getStringMessageException(e),
										msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
								EventRecorder.recordEvent(e);
								this.udpClient.sendData(Client.getMsgKeyValue(
										msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
										"NoSuchElementException in Method: constructResponseCodeField63 value"
												+ msg.getField(Iso8583Post.Bit._039_RSP_CODE) + " is not in the table.",
										"LOG", this.nameInterface));
							}
						}
						code = Pack.resize(responseCode.getKeyIsc() + responseCode.getDescriptionIsc(),
								General.LENGTH_44, General.SPACE, true);
						this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
								"constructResponseCodeField63 responseCode.getKeyIsc() =[" + responseCode.getKeyIsc()
										+ "] responseCode.getDescriptionIsc() [" + responseCode.getDescriptionIsc()
										+ "]",
								"LOG", this.nameInterface));
					}
				}
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"constructResponseCodeField63", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(
					((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructResponseCodeField63 " + e.getMessage(), "LOG", this.nameInterface));
			code = Pack.resize("10002" + "Error Code could not extracted from message", General.LENGTH_44,
					General.SPACE, true);
		}
		return code;
	}

	/**************************************************************************************
	 * Build field 90 Original Data Elements
	 * 
	 * @param - Received the object B24 or Iso
	 * @return - the string response to field 90
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructOriginalKey(Object object, Integer num) throws XPostilion {
		Base24Ath msg = (Base24Ath) object;
		String originalKey = null;
		try {
			if (msg.isFieldSet(Iso8583Post.Bit._090_ORIGINAL_DATA_ELEMENTS)) {
				originalKey = msg.getField(Iso8583Post.Bit._090_ORIGINAL_DATA_ELEMENTS).substring(0, 32);
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructOriginalKey]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructOriginalKey " + e.getMessage(), "LOG", this.nameInterface));
		}
		return originalKey;
	}

	/**************************************************************************************
	 * Build the field 38 Authorization Id response in 0210 message when the
	 * transaction is failed by TM
	 * 
	 * @param - Received the object null
	 * @return - The value string "000000"
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructAuthorizationIdResponse(Object object, Integer num) throws XPostilion {
		try {
			return AuthorizationIdRsp.SIX_ZEROS;
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructAuthorizationIdResponse]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient
					.sendData(Client.getMsgKeyValue(((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Exception in Method: constructAuthorizationIdResponse " + e.getMessage(), "LOG",
							this.nameInterface));
		}
		return null;
	}

	/**************************************************************************************
	 * Transform the field 63 in 0210 successful to remote
	 * 
	 * @param - Received the object B24 or Iso
	 * @return - String to response code ISC equivalent
	 * @throws XPostilion
	 *************************************************************************************/
	public String constuctCodeResponseInIsc(Object object, Integer num) throws XPostilion {

		String codeResponse = null;
		if (object instanceof Base24Ath) {
			codeResponse = "30";
		} else if (object instanceof Iso8583Post) {
			Iso8583Post msgFromTm = (Iso8583Post) object;
			ResponseCode cResponse = new ResponseCode();
			try {
				if (msgFromTm.isFieldSet(Iso8583Post.Bit._039_RSP_CODE)) {
					if (this.allCodesIsoToB24TM.containsKey(msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE))) {
						try {
							cResponse = InitialLoadFilter.getFilterCodeIsoToB24(
									msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE), this.allCodesIsoToB24TM);
						} catch (NoSuchElementException e) {
							this.udpClient
									.sendData(
											Client.getMsgKeyValue(msgFromTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
													"NoSuchElementException in Method: constuctCodeResponseInIsc "
															+ Utils.getStringMessageException(e),
													"LOG", this.nameInterface));
							if (new DBHandler(this.params)
									.updateResgistry(msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE), "1",responseCodesVersion)) {
								this.allCodesIsoToB24TM = postilion.realtime.library.common.db.DBHandler
										.getResponseCodes(false, "1",responseCodesVersion);
								cResponse = InitialLoadFilter.getFilterCodeIsoToB24(
										msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE), this.allCodesIsoToB24TM);
							} else {
								cResponse = new ResponseCode("10002", "Error Code could not extracted from message",
										msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE),
										msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE));
								EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
										ConstructFieldMessage.class.getName(), "Method: [constuctCodeResponseInIsc]",
										Utils.getStringMessageException(e),
										msgFromTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
								EventRecorder.recordEvent(e);
								this.udpClient.sendData(
										Client.getMsgKeyValue(msgFromTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
												"NoSuchElementException in Method: constuctCodeResponseInIsc value"
														+ msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE)
														+ " is not in the table.",
												"LOG", this.nameInterface));
							}
						}
					} else {
						try {
							cResponse = InitialLoadFilter.getFilterCodeIsoToB24(
									msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE), this.allCodesIsoToB24);
						} catch (NoSuchElementException e) {
							this.udpClient
									.sendData(
											Client.getMsgKeyValue(msgFromTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
													"NoSuchElementException in Method: constuctCodeResponseInIsc "
															+ Utils.getStringMessageException(e),
													"LOG", this.nameInterface));
							if (new DBHandler(this.params)
									.updateResgistry(msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE), "0",responseCodesVersion)) {
								this.allCodesIsoToB24 = postilion.realtime.library.common.db.DBHandler
										.getResponseCodes(false, "0",responseCodesVersion);
								cResponse = InitialLoadFilter.getFilterCodeIsoToB24(
										msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE), this.allCodesIsoToB24);
							} else {
								cResponse = new ResponseCode("10002", "Error Code could not extracted from message",
										msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE),
										msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE));
								EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
										ConstructFieldMessage.class.getName(), "Method: [constuctCodeResponseInIsc]",
										Utils.getStringMessageException(e),
										msgFromTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
								EventRecorder.recordEvent(e);
								this.udpClient.sendData(
										Client.getMsgKeyValue(msgFromTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
												"NoSuchElementException in Method: constuctCodeResponseInIsc value "
														+ msgFromTm.getField(Iso8583Post.Bit._039_RSP_CODE)
														+ " is not in the table",
												"LOG", this.nameInterface));
							}
						}
					}
					codeResponse = Pack.resize(cResponse.getKeyIsc() + cResponse.getDescriptionIsc(), General.LENGTH_44,
							General.SPACE, true);
				}
			} catch (Exception e) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						ConstructFieldMessage.class.getName(), "Method: [constuctCodeResponseInIsc]",
						Utils.getStringMessageException(e), msgFromTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(
						((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: constuctCodeResponseInIsc " + e.getMessage(), "LOG", this.nameInterface));
			}
		}
		return codeResponse;

	}

	/**************************************************************************************
	 * Build field 42 Card Acceptor ID Code
	 * 
	 * @param - The object default null
	 * @return - String with the information of field 42
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructDefaultCardAceptorCode(Object object, Integer num) throws XPostilion {
		Base24Ath msg = (Base24Ath) object;
		String defaultCardAccepto = null;
		try {

			if (msg.isFieldSet(Iso8583Post.Bit._042_CARD_ACCEPTOR_ID_CODE)) {

				defaultCardAccepto = msg.getField(Iso8583Post.Bit._042_CARD_ACCEPTOR_ID_CODE);

			} else {
				defaultCardAccepto = General.DEFAULT_CARD_ACCEPTOR_ID_CODE;
			}
		} catch (XPostilion e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructDefaultCardAceptorCode]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient
					.sendData(Client.getMsgKeyValue(((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Exception in Method: constructDefaultCardAceptorCode " + e.getMessage(), "LOG",
							this.nameInterface));
		}
		return defaultCardAccepto;
	}

	/**************************************************************************************
	 * Build field 43 for 0220 messages when the message failed for regular
	 * expression to TM notified
	 * 
	 * @param - Message to construct to TM
	 * @return - String value for field 43
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructNameCardAcceptor(Object object, Integer num) throws XPostilion {

		Base24Ath msg = (Base24Ath) object;
		String nameCardAcceptor = null;
		try {
			if (msg.isFieldSet(Iso8583Post.Bit._043_CARD_ACCEPTOR_NAME_LOC)) {
				nameCardAcceptor = msg.getField(Iso8583Post.Bit._043_CARD_ACCEPTOR_NAME_LOC);
			} else {
				nameCardAcceptor = Pack.resize(msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID), 40, ' ', true);
			}
		} catch (XPostilion e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructNameCardAcceptor]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(
					((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructNameCardAcceptor " + e.getMessage(), "LOG", this.nameInterface));
		}
		return nameCardAcceptor;
	}

	/**
	 * Transform the field 3 in Cost inquiry message in 0210 message
	 * 
	 * @param - Received the object default null
	 * @return - String the new processing code
	 * @throws XPostilion
	 */
	public String constructProcessingCode(Object object, Integer num) throws XPostilion {
		String processingCode = null;
		try {
			if (object instanceof Base24Ath) {

				Base24Ath msg = (Base24Ath) object;

				if (msg.getMsgType() == Iso8583.MsgType._0200_TRAN_REQ
						|| msg.getMsgType() == Iso8583.MsgType._0220_TRAN_ADV) {

					ProcessingCode pc = new ProcessingCode(msg.getField(126).substring(22, 28));
					processingCode = new ProcessingCode(Iso8583Post.TranType._32_GENERAL_INQUIRY
							.concat(pc.getFromAccount()).concat(msg.getProcessingCode().getToAccount())).toString();
				} else {
					processingCode = msg.getProcessingCode().toString();
				}

			} else if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;

				Base24Ath msgOriginal = (Base24Ath) this.sourceTranToTmHashtableB24
						.get(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR));
				processingCode = new ProcessingCode("89".concat(msgOriginal.getProcessingCode().getFromAccount())
						.concat(msg.getProcessingCode().getToAccount())).toString();
			}
		} catch (XPostilion e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructProcessingCode]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(
					((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructProcessingCode " + e.getMessage(), "LOG", this.nameInterface));
		}
		return processingCode;
	}

	/**
	 * Construye el campo Card Acceptor Terminal ID (Bit 42) hacia el TM.
	 * 
	 * @param object Mensaje desde ATH.
	 * @return Retorna un String que devuelve el campo construido.
	 * @throws XPostilion
	 */
	public String constructCardAcceptorTermIdToTranmgrResize(Object object, Integer num) throws XPostilion {
		String cardAcceptortermId = null;
		Base24Ath msg = (Base24Ath) object;
		try {
			if (msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)) {
				cardAcceptortermId = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)
						.substring(Constants.Indexes.FIELD41_POSITION_0, Constants.Indexes.FIELD41_POSITION_8);
			} else {
				cardAcceptortermId = Pack.resize(General.VOIDSTRING, General.LENGTH_8, General.SPACE, true);
			}
			cardAcceptortermId = Pack.resize(constructCardAcceptorTermIdToTranmgr(msg, num), 15, '0', false);
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructCardAcceptorTermIdToTranmgrResize]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructCardAcceptorTermIdToTranmgrResize " + e.getMessage(), "LOG",
					this.nameInterface));
		}
		return cardAcceptortermId;
	}

	/**
	 * Transform of field 28.
	 * 
	 * @param object
	 * @return
	 */
	public static String transformField28And30ToIso8583Post(Object object, Integer num) {

		Base24Ath msgFromRemote = (Base24Ath) object;
		try {
			return "D" + msgFromRemote.getField(num);
		} catch (XPostilion e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Routing by account type
	 * 
	 * @param object
	 * @return
	 * @throws XPostilion
	 */
	public String constructField100(Object object, Integer num) throws XPostilion {
		Base24Ath msg = (Base24Ath) object;
		try {
			switch (this.routingField100) {

			case "10":

				return "10";

			case "20":

				return "20";

			default:
				String secondsfield12 = msg.getField(Iso8583.Bit._012_TIME_LOCAL).substring(5, 6);
				int fieldValue = Integer.parseInt(secondsfield12);

				switch (fieldValue % 2) {
				case 0:
					return "20";
				default:
					return "10";
				}
			}

		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "constructField100", Utils.getStringMessageException(e),
					((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));

			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructField100 " + e.getMessage(), "LOG", this.nameInterface));
		}
		return "10";
	}

	public String constructField62Last5Mov(Object object, Integer num) throws XPostilion {
		StringBuilder sb = new StringBuilder("");
		//String strField62=null;
		try {
			if (object instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) object;
				String Mov1 = null;
				String Mov2 = null;
				String Mov3 = null;
				String Mov4 = null;
				String Mov5 = null;

				StructuredData sd = msg.getStructuredData();
				if (sd != null) {
					Mov1 = sd.get("MOVIMIEN_1") != null ? sd.get("MOVIMIEN_1") : "";
					Mov2 = sd.get("MOVIMIEN_2") != null ? sd.get("MOVIMIEN_2") : "";
					Mov3 = sd.get("MOVIMIEN_3") != null ? sd.get("MOVIMIEN_3") : "";
					Mov4 = sd.get("MOVIMIEN_4") != null ? sd.get("MOVIMIEN_4") : "";
					Mov5 = sd.get("MOVIMIEN_5") != null ? sd.get("MOVIMIEN_5") : "";

					sb.append(Mov1);
					sb.append(Mov2);
					sb.append(Mov3);
					sb.append(Mov4);
					sb.append(Mov5);
				} else {
					sb.append("   ");
				}
				
				//strField62= Pack.resize(sb.toString(),150, ' ', true);
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructField62Last5Mov]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(
					((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructField62Last5Mov " + e.getMessage(), "LOG", this.nameInterface));
		}
		return sb.toString();
	}

	/**
	 * Routing by account type
	 * 
	 * @param object
	 * @return
	 * @throws XPostilion
	 */
	/**
	 * @param object
	 * @param num
	 * @return
	 * @throws XPostilion
	 */
	public static String constructField100ACH(Object object, Integer num) throws XPostilion {
		String rta = Constants.General.DEFAULT_INST_ID_CODE;
		String field103 = Constants.General.DEFAULT_INST_ID_CODE;
		if (object instanceof Iso8583Post) {
			Iso8583Post msg = (Iso8583Post) object;
			field103 = msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2);
		} else if (object instanceof Base24Ath) {
			Base24Ath msg = (Base24Ath) object;
			field103 = msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2);
		}
		rta = "0000000" + field103.substring(3, 7);
		return rta;
	}

	public static String constructField100DefaultInstitutionID(Object object, Integer num) throws XPostilion {

		return Constants.General.DEFAULT_INST_ID_CODE;
	}

	public static String constructField100DefaultInstitutionIDMasiva(Object object, Integer num) throws XPostilion {
		return Constants.General.DEFAULT_INST_ID_CODE_MASIVA;
	}

	public static String constructField35Virtual(Object object, Integer num) throws XPostilion {
		return Constants.General.DEFAULT_TRACK2_MASIVA;
	}
	
	public static String constructField35Oficinas(Object object, Integer num) throws XPostilion {
		return "0099010000000000000D991200000001";
	}
	

	/**
	 * Routing by account type
	 * 
	 * @param object
	 * @return
	 * @throws XPostilion
	 */
	/**
	 * @param object
	 * @param num
	 * @return
	 * @throws XPostilion
	 */
	public static String constructField112(Object object, Integer num) throws XPostilion {
		return Constants.General.SIXTEEN_ZEROS;
	}

	/**
	 * Routing by account type
	 * 
	 * @param object
	 * @return
	 * @throws XPostilion
	 */
	/**
	 * @param object
	 * @param num
	 * @return
	 * @throws XPostilion
	 */
	public static String constructDefaultField104(Object object, Integer num) throws XPostilion {
		return Constants.Account.ACCOUNT_DEFAULT;
	}

	/**
	 * Routing by account type
	 * 
	 * @param object
	 * @return
	 * @throws XPostilion
	 */
	/**
	 * @param object
	 * @param num
	 * @return
	 * @throws XPostilion
	 */
	public static String constructField100DefaultAccountType(Object object, Integer num) throws XPostilion {
		return correctTypeAccount;
	}

	/**
	 * Method to return el field value default of message
	 * 
	 * @param object - Message from Remote in Base 24
	 * @param num    - Field Number to create
	 * @return - Field value to message Iso8583Post
	 * @throws XPostilion
	 */
	public String construct0220ErrorFields(Object object, Integer num) throws XPostilion {
		String valueField = null;
		Iso8583 msg = null;
		if (object instanceof Base24Ath) {
			msg = (Base24Ath) object;
		} else {
			msg = (Iso8583Post) object;
		}

		try {
			switch (num) {
			case 2:
				if (msg.isFieldSet(Iso8583.Bit._002_PAN)) {
					valueField = msg.getField(Iso8583.Bit._002_PAN);
				} else if (msg.isFieldSet(Iso8583.Bit._035_TRACK_2_DATA)) {
					valueField = msg.getTrack2Data().getPan();
				} else {
					valueField = Constants.General.SIXTEEN_ZEROS;
				}
				break;
			case 3:
				valueField = constructField3to0220ToTM(msg);
				break;
			case 4:
				valueField = msg.isFieldSet(Iso8583.Bit._004_AMOUNT_TRANSACTION)
						? msg.getField(Iso8583.Bit._004_AMOUNT_TRANSACTION)
						: Constants.General.TWELVE_ZEROS;
				break;
			case 7:
				valueField = msg.isFieldSet(Iso8583.Bit._007_TRANSMISSION_DATE_TIME)
						? msg.getField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME)
						: new DateTime().get(FormatDate.MMDDHHMMSS);
				break;
			case 11:
				valueField = msg.isFieldSet(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR)
						? msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR)
						: Constants.General.SIX_ZEROS;
				break;
			case 12:
				valueField = msg.isFieldSet(Iso8583.Bit._012_TIME_LOCAL) ? msg.getField(Iso8583.Bit._012_TIME_LOCAL)
						: new DateTime().get(FormatDate.HHMMSS);
				break;
			case 13:
			case 15:
				valueField = msg.isFieldSet(Iso8583.Bit._013_DATE_LOCAL) ? msg.getField(Iso8583.Bit._013_DATE_LOCAL)
						: new DateTime().get(FormatDate.MMDD);
				break;
			case 22:
				valueField = msg.isFieldSet(Iso8583.Bit._022_POS_ENTRY_MODE)
						? msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)
						: Constants.General.DEFAULT_ERROR_022;
				break;
			case 25:
				valueField = msg.isFieldSet(Iso8583.Bit._025_POS_CONDITION_CODE)
						? msg.getField(Iso8583.Bit._025_POS_CONDITION_CODE)
						: Iso8583.PosCondCode.NORMAL_PRESENTMENT;
				break;
			case 32:
				valueField = msg.isFieldSet(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE)
						? msg.getField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE).trim()
						: Constants.General.ELEVEN_ZEROS;
				break;
			case 35:
				valueField = constructField35To0220Error(msg);
				break;
			case 37:
				valueField = msg.isFieldSet(Iso8583.Bit._037_RETRIEVAL_REF_NR)
						? msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)
						: Constants.General.DEFAULT_P37;
				break;
			case 38:
				valueField = msg.isFieldSet(Iso8583.Bit._038_AUTH_ID_RSP) ? msg.getField(Iso8583.Bit._038_AUTH_ID_RSP)
						: Constants.General.SIX_ZEROS;
				break;
			case 39:
				valueField = msg.isFieldSet(Iso8583.Bit._039_RSP_CODE) ? msg.getField(Iso8583.Bit._039_RSP_CODE)
						: Constants.Config.CODE_ERROR_30;
				break;
			case 41:
				valueField = msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)
						? constructCardAcceptorTermIdToTranmgr(object, 41)
						: Pack.resize(General.VOIDSTRING, General.LENGTH_8, General.SPACE, true);
				break;
			case 42:
				valueField = msg.isFieldSet(Iso8583.Bit._042_CARD_ACCEPTOR_ID_CODE)
						? msg.getField(Iso8583.Bit._042_CARD_ACCEPTOR_ID_CODE)
						: General.DEFAULT_CARD_ACCEPTOR_ID_CODE;
				break;
			case 43:
				valueField = msg.isFieldSet(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC)
						? msg.getField(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC)
						: Pack.resize(General.VOIDSTRING, 40, General.SPACE, true);
				break;
			case 48:

				valueField = msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA)
						? msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA)
						: Constants.General.DEFAULT_P48;
				break;
			case 49:
				valueField = msg.isFieldSet(Iso8583.Bit._049_CURRENCY_CODE_TRAN)
						? msg.getField(Iso8583.Bit._049_CURRENCY_CODE_TRAN)
						: Constants.General.DEFAULT_ERROR_049;
				break;
			case 100:
				valueField = msg.isFieldSet(Iso8583.Bit._100_RECEIVING_INST_ID_CODE)
						? msg.getField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE)
						: constructField100(msg, num);
				break;
			case 102:
				valueField = msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1) ? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
						: Constants.Account.ACCOUNT_DEFAULT;
				break;
			case 104:
				valueField = msg.isFieldSet(Iso8583.Bit._104_TRAN_DESCRIPTION)
						? msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION)
						: Constants.Account.ACCOUNT_DEFAULT;
				break;
			case 123:
				valueField = constructPosDataCode(msg, 123);
				break;
			default:
				valueField = General.VOIDSTRING;
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [construct0220ErrorFields]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: construct0220ErrorFields " + e.getMessage(), "LOG", this.nameInterface));
		}
		return valueField;
	}

	/**
	 * Method to return el field value default of message
	 * 
	 * @param object - Message from Remote in Base 24
	 * @param num    - Field Number to create
	 * @return - Field value to message Base24
	 * @throws XPostilion
	 */

	public String construct0210ErrorFields(Object object, Integer num) throws XPostilion {
		String valueField = null;
		Iso8583 msg = null;
		if (object instanceof Base24Ath) {
			msg = (Base24Ath) object;
		} else {
			msg = (Iso8583Post) object;
		}
		try {
			switch (num) {
			case 3:
				valueField = msg.isFieldSet(Iso8583.Bit._003_PROCESSING_CODE)
						? msg.getField(Iso8583.Bit._003_PROCESSING_CODE)
						: Constants.General.PCODE_DEFAULT_ERROR;
				break;
			case 4:
				valueField = msg.isFieldSet(Iso8583.Bit._004_AMOUNT_TRANSACTION)
						? msg.getField(Iso8583.Bit._004_AMOUNT_TRANSACTION)
						: Constants.General.TWELVE_ZEROS;
				break;
			case 7:
				valueField = msg.isFieldSet(Iso8583.Bit._007_TRANSMISSION_DATE_TIME)
						? msg.getField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME)
						: new DateTime().get(FormatDate.MMDDHHMMSS);
				break;
			case 11:
				valueField = msg.isFieldSet(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR)
						? msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR)
						: Constants.General.SIX_ZEROS;
				break;
			case 12:
				valueField = msg.isFieldSet(Iso8583.Bit._012_TIME_LOCAL) ? msg.getField(Iso8583.Bit._012_TIME_LOCAL)
						: new DateTime().get(FormatDate.HHMMSS);
				break;
			case 13:
			case 15:
			case 17:
				valueField = msg.isFieldSet(Iso8583.Bit._013_DATE_LOCAL) ? msg.getField(Iso8583.Bit._013_DATE_LOCAL)
						: new DateTime().get(FormatDate.MMDD);
				break;
			case 22:
				valueField = msg.isFieldSet(Iso8583.Bit._022_POS_ENTRY_MODE)
						? msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)
						: Constants.General.DEFAULT_ERROR_022;
				break;
			case 32:
				valueField = msg.isFieldSet(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE)
						? msg.getField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE)
						: Constants.General.DEFAULT_P32;
				break;
			case 35:
				valueField = msg.isFieldSet(Iso8583.Bit._035_TRACK_2_DATA) ? msg.getField(Iso8583.Bit._035_TRACK_2_DATA)
						: Constants.General.DEFAULT_P35;
				break;
			case 37:
				valueField = msg.isFieldSet(Iso8583.Bit._037_RETRIEVAL_REF_NR)
						? msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)
						: Constants.General.DEFAULT_P37;
				break;
			case 38:
				valueField = msg.isFieldSet(Iso8583.Bit._038_AUTH_ID_RSP) ? msg.getField(Iso8583.Bit._038_AUTH_ID_RSP)
						: Constants.General.SIX_ZEROS;
				break;
			case 39:
				valueField = Constants.Config.CODE_ERROR_30;
				break;
			case 40:
				valueField = msg.isFieldSet(Iso8583.Bit._040_SERVICE_RESTRICTION_CODE)
						? msg.getField(Iso8583.Bit._040_SERVICE_RESTRICTION_CODE)
						: Constants.General.THREE_ZEROS;
				break;
			case 41:
				valueField = msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)
						? msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)
						: Constants.General.DEFAULT_P41;
				break;
			case 44:
				valueField = Pack.resize(General.VOIDSTRING, 25, '0', true);
				break;
			case 48:
				valueField = msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA)
						? msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA)
						: Constants.General.DEFAULT_P48;
				break;
			case 49:
				valueField = msg.isFieldSet(Iso8583.Bit._049_CURRENCY_CODE_TRAN)
						? msg.getField(Iso8583.Bit._049_CURRENCY_CODE_TRAN)
						: Constants.General.NUM_170;
				break;
			case 54:
				valueField = msg.isFieldSet(Iso8583.Bit._054_ADDITIONAL_AMOUNTS)
						? msg.getField(Iso8583.Bit._054_ADDITIONAL_AMOUNTS)
						: Constants.General.DEFAULT_P54;
				break;
			case 60:
				valueField = msg.isFieldSet(Constants.General.NUMBER_60) ? msg.getField(Constants.General.NUMBER_60)
						: Constants.General.DEFAULT_P60;
				break;
			case 61:
				valueField = msg.isFieldSet(Constants.General.NUMBER_61) ? msg.getField(Constants.General.NUMBER_61)
						: Constants.General.DEFAULT_P61;
				break;
			case 90:
				valueField = msg.isFieldSet(Iso8583.Bit._090_ORIGINAL_DATA_ELEMENTS)
						? msg.getField(Iso8583.Bit._090_ORIGINAL_DATA_ELEMENTS)
						: Constants.General.DEFAULT_ORIGINAL_DATA_ELEMENTS;
				break;
			case 95:
				valueField = msg.isFieldSet(Iso8583.Bit._095_REPLACEMENT_AMOUNTS)
						? msg.getField(Iso8583.Bit._095_REPLACEMENT_AMOUNTS)
						: Constants.General.DEFAULT_P95;
				break;
			case 100:

				valueField = Constants.General.DEFAULT_RECEIVING_INST_ID_CODE;

				break;
			case 102:
				valueField = msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1) ? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
						: Account.ACCOUNT_DEFAULT;
				break;
			case 103:
				valueField = msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2) ? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2)
						: Account.ACCOUNT_DEFAULT;
				break;
			case 104:
				valueField = msg.isFieldSet(Iso8583.Bit._104_TRAN_DESCRIPTION)
						? msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION)
						: Account.ACCOUNT_DEFAULT;
				break;
			case 112:
				valueField = msg.isFieldSet(Constants.General.NUMBER_112) ? msg.getField(Constants.General.NUMBER_112)
						: Constants.General.SIXTEEN_ZEROS;
				break;
			case 126:
				valueField = msg.isFieldSet(Base24Ath.Bit._126_ATH_ADDITIONAL_DATA)
						? msg.getField(Base24Ath.Bit._126_ATH_ADDITIONAL_DATA)
						: Constants.General.DEFAULT_P48;
				break;
			case 128:
				valueField = msg.isFieldSet(Constants.General.NUMBER_128) ? msg.getField(Constants.General.NUMBER_128)
						: Constants.General.DEFAULT_P128;
				break;
			default:
				valueField = General.VOIDSTRING;
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [construct0210ErrorFields]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: construct0210ErrorFields " + e.getMessage(), "LOG", this.nameInterface));
		}
		return valueField;
	}

	/**
	 * Method to construct field 129 when message is 0220 successful
	 * 
	 * @param obj - message from remote
	 * @return - value field 39
	 * @throws XPostilion
	 */
	public String constructP39For0220NotiBloq(Object obj, Integer num) throws XPostilion {
		String valueField = null;
		try {
			Base24Ath msg = (Base24Ath) obj;
			ResponseCode responseCode;
			try {
				responseCode = InitialLoadFilter.getFilterCodeIsoToB24(msg.getField(Iso8583Post.Bit._039_RSP_CODE),
						this.allCodesB24ToIso);
			} catch (NoSuchElementException e) {
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"NoSuchElementException in Method: constructP39For0220NotiBloq "
								+ Utils.getStringMessageException(e),
						"LOG", this.nameInterface));
				if (new DBHandler(this.params).updateResgistry(msg.getField(Iso8583Post.Bit._039_RSP_CODE), "1",responseCodesVersion)) {
					this.allCodesB24ToIso = postilion.realtime.library.common.db.DBHandler.getResponseCodesBase24("1",responseCodesVersion);
					responseCode = InitialLoadFilter.getFilterCodeIsoToB24(msg.getField(Iso8583Post.Bit._039_RSP_CODE),
							this.allCodesB24ToIso);
				} else {
					responseCode = new ResponseCode("10002", "Error Code could not extracted from message",
							msg.getField(Iso8583Post.Bit._039_RSP_CODE), msg.getField(Iso8583Post.Bit._039_RSP_CODE));
					EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
							ConstructFieldMessage.class.getName(), "Method: [constructP39For0220NotiBloq]",
							Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
					EventRecorder.recordEvent(e);
					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"NoSuchElementException in Method: constructP39For0220NotiBloq value"
									+ msg.getField(Iso8583Post.Bit._039_RSP_CODE) + " is not in the table.",
							"LOG", this.nameInterface));
				}
			}
			valueField = responseCode.getKeyIso();
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "constructP39For0220NotiBloq",
					Utils.getStringMessageException(e), ((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructP39For0220NotiBloq " + e.getMessage(), "LOG", this.nameInterface));
			Base24Ath msg = (Base24Ath) obj;
			valueField = msg.getField(Iso8583Post.Bit._039_RSP_CODE);
		}
		return valueField;
	}

	public String constructOriginalFieldMsg(Object obj, Integer num) throws XPostilion {
		String valueField = null;
		Iso8583Post msg = (Iso8583Post) obj;
		Base24Ath msgOriginalB24 = (Base24Ath) this.sourceTranToTmHashtableB24
				.get(msg.getField(Iso8583Post.Bit._037_RETRIEVAL_REF_NR));
		try {
			if (num == Iso8583Post.Bit._017_DATE_CAPTURE) {
				valueField = msgOriginalB24.getField(Iso8583Post.Bit._017_DATE_CAPTURE);
			}
			if (num == Iso8583Post.Bit._048_ADDITIONAL_DATA) {
				valueField = msgOriginalB24.getField(Iso8583Post.Bit._048_ADDITIONAL_DATA);
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructOriginalFieldMsg]",
					Utils.getStringMessageException(e), ((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructOriginalFieldMsg " + e.getMessage(), "LOG", this.nameInterface));
		}
		return valueField;
	}

	public String constructCardAcceptorIDCode(Object obj, Integer num) throws XPostilion {
		try {
			return Pack.resize(General.VOIDSTRING, 15, General.SPACE, true);
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructCardAcceptorIDCode]",
					Utils.getStringMessageException(e), ((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructCardAcceptorIDCode " + e.getMessage(), "LOG", this.nameInterface));
		}
		return null;
	}

	/**
	 * Method to construct field 59 to B24
	 * 
	 * @param obj - message from remote
	 * @return - value field 59
	 * @throws XPostilion
	 */
	public String constructP59toB24(Object obj, Integer num) throws XPostilion {
		String valueField = null;
		try {
			valueField = Constants.General.SPACE_25;
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructP59toB24]",
					Utils.getStringMessageException(e), ((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructP39For0220NotiBloq " + e.getMessage(), "LOG", this.nameInterface));
		}
		return valueField;
	}

	/**
	 * Construye el campo 102 del mesnaje de respuesta base 24
	 * 
	 * @param obj mensaje from transaction manager
	 * @param num numero del campo 102
	 * @return camppoo 102
	 * @throws XPostilion
	 */
	public String constructField102toB24(Object obj, Integer num) throws XPostilion {
		String valueField = "000000000000000000";
		try {
			if (obj instanceof Base24Ath)
				return valueField;

			else if (obj instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) obj;
				if (msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA))
					if (msg.getStructuredData().get("DEBIT_ACCOUNT_NR") != null) {
						valueField = msg.getStructuredData().get("DEBIT_ACCOUNT_NR");
					}
			}
		} catch (Exception e) {

			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructField102toB24]",
					Utils.getStringMessageException(e), ((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructField102toB24 " + e.getMessage(), "LOG", this.nameInterface));

		}

		return valueField;
	}

	/**
	 * Construye el campo 102 del mesnaje de respuesta base 24
	 * 
	 * @param obj mensaje from transaction manager
	 * @param num numero del campo 102
	 * @return camppoo 102
	 * @throws XPostilion
	 */
	public String constructField102ConsultaCreditoRotativo(Object obj, Integer num) throws XPostilion {
		String valueField = "000000000000000000";
		try {
			if (obj instanceof Base24Ath)
				return valueField;

			else if (obj instanceof Iso8583Post) {
				Iso8583Post msg = (Iso8583Post) obj;
				if (msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA))
					if (msg.getStructuredData().get("P102_1") != null
							&& msg.getStructuredData().get("P102_2") != null) {
						valueField = msg.getStructuredData().get("P102_1") + msg.getStructuredData().get("P102_2");
					}
			}
		} catch (Exception e) {

			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructField102ConsultaCreditoRotativo]",
					Utils.getStringMessageException(e), ((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructField102ConsultaCreditoRotativo " + e.getMessage(), "LOG",
					this.nameInterface));

		}

		return valueField;
	}

	/**
	 * Ajusta la respuesta del campo 48 para una consulta de consto de retiro
	 * 
	 * @param field48 campo 48 que se obtiene del tag del sturtured data
	 * @return String con el nuevo campo 48
	 * @throws XPostilion
	 */
	public String constructField048InRspCostInquiry(Object obj, Integer num) throws XPostilion {
		Iso8583Post msg = (Iso8583Post) obj;
		StringBuilder newField48 = new StringBuilder("");
		try {
			if (msg.getStructuredData() != null) {
				newField48.append(msg.getStructuredData().get("B24_Field_" + num.toString()));
				newField48.setCharAt(newField48.length() - 2, '0');
			} else {
				Base24Ath msgOriginalB24 = (Base24Ath) this.sourceTranToTmHashtableB24
						.get(msg.getField(Iso8583Post.Bit._037_RETRIEVAL_REF_NR));
				newField48.append(msgOriginalB24.getField(Iso8583.Bit._048_ADDITIONAL_DATA));
			}
			newField48.setCharAt(newField48.length() - 2, '0');
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					ConstructFieldMessage.class.getName(), "Method: [constructField048InRspCostInquiry]",
					Utils.getStringMessageException(e), ((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructField048InRspCostInquiry " + e.getMessage(), "LOG",
					this.nameInterface));
			try {
				Base24Ath msgOriginalB24 = (Base24Ath) this.sourceTranToTmHashtable
						.get(msg.getField(Iso8583Post.Bit._037_RETRIEVAL_REF_NR));
				newField48.append(msgOriginalB24.getField(Iso8583.Bit._048_ADDITIONAL_DATA));
				newField48.setCharAt(newField48.length() - 2, '0');
			} catch (XPostilion e1) {
				newField48.append(Constants.General.DEFAULT_P48);
				EventRecorder.recordEvent(
						new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
								"Method: [constructField048InRspCostInquiry]", Utils.getStringMessageException(e),
								((Iso8583) obj).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: constructField048InRspCostInquiry " + e.getMessage(), "LOG",
						this.nameInterface));
			}
		}
		return newField48.toString();
	}

	/**
	 * Agrega el valor de comision en el token QT
	 * 
	 * @param field126      campo 126 al cualse le va agregar la comision
	 * @param initialLength longitud incial del campo 126
	 * @param commision     valor de la comision
	 * @return campo 126 con el valor de la comision en el toekn QT.
	 */
	public static String addCommision(String field126, int initialLength, String commision) {
		field126 = field126.substring(0, initialLength - 14)
				.concat(Pack.resize(commision + General.VOIDSTRING, 12,
						Constants.General.ZERO, false))
				.concat(field126.substring(initialLength - 2, initialLength));
		return field126;
	}

	/**
	 * Construye campo 100 para consulta de costo
	 * 
	 * @param object mensaje base 24
	 * @param num    campo 100
	 * @return campo 100
	 * @throws XPostilion
	 */
	public String constructField100ForCostInquiry(Object object, Integer num) throws XPostilion {
		Base24Ath msg = (Base24Ath) object;
		ProcessingCode pc = new ProcessingCode(msg.getField(126).substring(22, 28));
		try {
			switch (pc.getFromAccount()) {
			case AccountType._20_CHECK:
				return this.institutionid.get(RoutingAccount.ACCOUNT_CHECK_20);
			case AccountType._10_SAVINGS:
			default:
				return this.institutionid.get(RoutingAccount.ACCOUNT_SAVINGS_10);
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructField100ForCostInquiry]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructField100ForCostInquiry " + e.getMessage(), "LOG",
					this.nameInterface));
		}
		return "10";
	}

	/**
	 * Adecua los campo 28, 29, 30, 31 para el mensaje Iso8583Post
	 * 
	 * @param object mensaje en base24
	 * @param num    número del campo a transformar
	 * @return String del campo adecuado
	 * @throws XPostilion
	 */
	public String transformAmountFeeFields(Object object, Integer num) throws XPostilion {
		Base24Ath msg = (Base24Ath) object;
		StringBuilder field = new StringBuilder("D");
		try {
			field.append(msg.getField(num));
		} catch (XPostilion e) {
			field.append("00000000");
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [transformAmountFeeFields]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: transformAmountFeeFields " + e.getMessage(), "LOG", this.nameInterface));
		}

		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), field.toString(),
				"LOG", this.nameInterface));
		return field.toString();
	}

	/**
	 * Transforma el campo 103 a la longitud de 28 para mensajes 0210 en base24
	 * 
	 * @param object mensaje en Iso8583Post
	 * @param num    número del campo a transformar (103)
	 * @return String del campo adecuado
	 * @throws XPostilion
	 */
	public String transformField103(Object object, Integer num) throws XPostilion {
		String field = null;
		Iso8583 msg;
		if (object instanceof Base24Ath) {
			msg = (Base24Ath) object;
		} else {
			msg = (Iso8583Post) object;
		}
		try {
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"in Method: transformField103 " + msg.getField(3), "LOG", this.nameInterface));
			if (msg.isFieldSet(Iso8583Post.Bit._103_ACCOUNT_ID_2)) {
				field = Pack.resize(msg.getField(Iso8583Post.Bit._103_ACCOUNT_ID_2), 28, '0', false);
			} else {
				field = "0000000000000000000000000000";
			}
		} catch (XPostilion e) {
			field = "0000000000000000000000000000";
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [transformField103]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: transformField103 " + e.getMessage(), "LOG", this.nameInterface));
		}
		return field;
	}

	/**
	 * Construye el campo 53 a enviar a TM para la tx de cambio de pin.
	 * 
	 * @param msg Mensaje desde ATH.
	 * @return Retorna un String con el campo construido o null.
	 * @throws Exception En caso de error.
	 */
	public String constructNewPinDataToTranmgr(Object object, Integer num) throws Exception {
		Base24Ath msg = (Base24Ath) object;
		try {
			if (!msg.isFieldSet(num)) {
				return null;
			}
			String newField53 = Transform.fromHexToBin("01")
					+ Transform.fromHexToBin(msg.getField(Iso8583.Bit._053_SECURITY_INFO));
			return Pack.resize(newField53, 48, Transform.fromHexToBin("00").charAt(0), true);
		} catch (XPostilion e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [constructNewPinDataToTranmgr]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructNewPinDataToTranmgr " + e.getMessage(), "LOG", this.nameInterface));
		}
		return null;
	}

	/**
	 * TRansforma el campo 3 para notificacionde cambio de clave
	 */
	public String transformProcessingToTranmgr(Object object, Integer num) throws Exception {
		String field = null;
		if (object instanceof Base24Ath) {
			field = "320000";
		} else {
			Iso8583Post msg = (Iso8583Post) object;
			if (msg.getStructuredData().get("B24_Field_" + num.toString()) != null) {
				field = msg.getStructuredData().get("B24_Field_" + num.toString());
			} else {
				field = "910000";
			}
		}

		return field;
	}

	/**************************************************************************************
	 * Construye el campo 15 hacia Transaction manager
	 * 
	 * @param object mensaje recibido desde la interchange.
	 * @return String con el campo 15 obtenido del 200 campo 17
	 * 
	 * @throws XPostilion
	 *************************************************************************************/

	public String compensationDateValidationP17ToP15(Object object, Integer num) throws XPostilion {

		Base24Ath msg = (Base24Ath) object;
		BusinessCalendar objectBusinessCalendar = null;

		try {

			objectBusinessCalendar = new BusinessCalendar("DefaultBusinessCalendar");

			LocalDate currentBusinessDate = LocalDate
					.parse(new SimpleDateFormat("yyyy-MM-dd").format(objectBusinessCalendar.getCurrentBusinessDate()));

			String dateCapture = msg.getField(Iso8583.Bit._017_DATE_CAPTURE);
			LocalDate DateCapture = LocalDate.parse(currentBusinessDate.getYear() + "-" + dateCapture.substring(0, 2)
					+ "-" + dateCapture.substring(2, 4));

			if (currentBusinessDate.getMonthValue() == 12 && DateCapture.getMonthValue() == 1) {

				DateCapture = DateCapture.plusYears(1);

			} else if (DateCapture.getMonthValue() == 12 && currentBusinessDate.getMonthValue() == 1) {

				DateCapture = DateCapture.plusYears(-1);

			}

			return (DateCapture.isBefore(currentBusinessDate) || DateCapture.isEqual(currentBusinessDate))
					? new SimpleDateFormat("MMdd").format(objectBusinessCalendar.getCurrentBusinessDate())
					: new SimpleDateFormat("MMdd").format(objectBusinessCalendar.getNextBusinessDate());

		} catch (Exception e) {
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
							"Method: [compensationDateValidationP17ToP15]", Utils.getStringMessageException(e),
							((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: compensationDateValidationP17ToP15 " + e.getMessage(), "LOG",
					this.nameInterface));

		}

		return new SimpleDateFormat("MMdd").format(objectBusinessCalendar.getCurrentBusinessDate());

	}

	public String constructField3to0220ToTM(Iso8583 msg) throws XPostilion {
		String valueField = "";
		if (msg.isFieldSet(Iso8583.Bit._003_PROCESSING_CODE)) {
			try {
				if (msg.getProcessingCode().getTranType().equals("89")) {
					valueField = "320000";
				} else {
					valueField = msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
				}
			} catch (XFieldUnableToConstruct e) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						ConstructFieldMessage.class.getName(), "Method: [constructField3to0220ToTM]",
						Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			} catch (XPostilion e) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						ConstructFieldMessage.class.getName(), "Method: [constructField3to0220ToTM]",
						Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			}
		} else {
			valueField = Constants.General.PCODE_DEFAULT_ERROR;
		}
		return valueField;
	}

	public String constructField62InDeclinedResponse(Object object, Integer num) {
		return "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

	}

	public String transformField3ForDepositATM(Object object, Integer num) throws XPostilion {
		ProcessingCode pCode;
		if (object instanceof Base24Ath) {
			Base24Ath msg = (Base24Ath) object;
			pCode = new ProcessingCode("210110");
			try {
				pCode = msg.getProcessingCode();
				pCode.putTranType(postilion.realtime.sdk.message.bitmap.Iso8583.TranType._21_DEPOSITS);
			} catch (XFieldUnableToConstruct | XFieldUnableToPutSubfield e) {
				EventRecorder.recordEvent(
						new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
								"Method: [transformField3ForDepositATMToTranMgr]", Utils.getStringMessageException(e),
								((Base24Ath) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			}
		} else if (object instanceof Iso8583Post) {
			Iso8583Post msg = (Iso8583Post) object;
			pCode = new ProcessingCode("270110");
			try {
				pCode = msg.getProcessingCode();
				pCode.putTranType("27");
			} catch (XFieldUnableToConstruct | XFieldUnableToPutSubfield e) {
				EventRecorder.recordEvent(
						new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
								"Method: [transformField3ForDepositATMToTranMgr]", Utils.getStringMessageException(e),
								((Base24Ath) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			}
		} else {
			pCode = new ProcessingCode("270110");
		}
		return pCode.toString();
	}

	/**************************************************************************************
	 * Contruye el campo PIN Data (Bit 52) hacia el Base24.
	 * 
	 * @param object Mensaje desde TM.
	 * @return Retorna un String con el campo construido por defecto
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructPinDataToBase24Response(Object object, Integer num) throws XPostilion {
		String pinBlock = "0000000000000000";
		return pinBlock;
	}

	/**************************************************************************************
	 * Contruye el campo PIN Data (Bit 52) hacia el Base24.
	 * 
	 * @param object Mensaje desde TM.
	 * @return Retorna un String con el campo construido por defecto
	 * @throws XPostilion
	 *************************************************************************************/
	public String constructField104Credit(Object object, Integer num) throws XPostilion {
		Iso8583Post msg = (Iso8583Post) object;
		String field;
		if (msg.isPrivFieldSet(22)) {
			StructuredData sd = msg.getStructuredData();
			field = ((sd.get("CUSTOMER_ID_TYPE") != null) ? sd.get("CUSTOMER_ID_TYPE") : "0").concat(
					(sd.get("CUSTOMER_ID") != null) ? sd.get("CUSTOMER_ID").substring(8, 25) : "00000000000000000");
		} else {
			field = "000000000000000000";
		}
		return field;
	}

	public String transformField3ForCreditPaymentATM(Object object, Integer num) throws XPostilion {
		ProcessingCode pCode;
		if (object instanceof Base24Ath) {
			Base24Ath msg = (Base24Ath) object;
			pCode = new ProcessingCode("510100");
			try {
				pCode = msg.getProcessingCode();
				pCode.putTranType(postilion.realtime.sdk.message.bitmap.Iso8583Post.TranType._51_PAYMENT_BY_DEPOSIT);
			} catch (XFieldUnableToConstruct | XFieldUnableToPutSubfield e) {
				EventRecorder.recordEvent(
						new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
								"Method: [transformField3ForCreditPaymentATM]", Utils.getStringMessageException(e),
								((Base24Ath) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			}
		} else if (object instanceof Iso8583Post) {
			Iso8583Post msg = (Iso8583Post) object;
			pCode = new ProcessingCode("270100");
			try {
				pCode = msg.getProcessingCode();
				pCode.putTranType("27");
			} catch (XFieldUnableToConstruct | XFieldUnableToPutSubfield e) {
				EventRecorder.recordEvent(
						new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
								"Method: [transformField3ForCreditPaymentATM]", Utils.getStringMessageException(e),
								((Base24Ath) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			}
		} else {
			pCode = new ProcessingCode("270100");
		}
		return pCode.toString();
	}

	public String transformField3ForTokenActivationATM(Object object, Integer num) throws XPostilion {
		ProcessingCode pCode;
		if (object instanceof Base24Ath) {
			Base24Ath msg = (Base24Ath) object;
			pCode = new ProcessingCode("950000");
			try {
				pCode = msg.getProcessingCode();
				pCode.putTranType(postilion.realtime.sdk.message.bitmap.Iso8583Post.TranType._91_GENERAL_ADMIN);
			} catch (XFieldUnableToConstruct | XFieldUnableToPutSubfield e) {
				EventRecorder.recordEvent(
						new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
								"Method: [transformField3ForTokenActivationATM]", Utils.getStringMessageException(e),
								((Base24Ath) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			}
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"salida Method: transformField3ForTokenActivationATM processing code" + pCode.toString(), "LOG",
					this.nameInterface));

		} else if (object instanceof Iso8583Post) {
			Iso8583Post msg = (Iso8583Post) object;
			pCode = new ProcessingCode("910000");
			try {
				pCode = msg.getProcessingCode();
				pCode.putTranType("95");
			} catch (XFieldUnableToConstruct | XFieldUnableToPutSubfield e) {
				EventRecorder.recordEvent(
						new TryCatchException(new String[] { this.nameInterface, ConstructFieldMessage.class.getName(),
								"Method: [transformField3ForTokenActivationATM]", Utils.getStringMessageException(e),
								((Base24Ath) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			}
		} else {
			pCode = new ProcessingCode("950000");
		}
		return pCode.toString();
	}

	public String transformField3ForRelatedAccountsInquiryATM(Object object, Integer num) throws XPostilion {
		String pCode = "";
		if (object instanceof Base24Ath) {
			Base24Ath msg = (Base24Ath) object;

			try {
				pCode = msg.getProcessingCode().toString();
				switch (pCode) {
				case "321000":
					pCode = new ProcessingCode("391000").toString();
					break;
				case "322000":
					pCode = new ProcessingCode("392000").toString();
					break;
				default:
					break;
				}

			} catch (Exception e) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						ConstructFieldMessage.class.getName(), "Method: [transformField3ForRelatedAccountsInquiryATM]",
						Utils.getStringMessageException(e),
						((Base24Ath) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			}
		} else if (object instanceof Iso8583Post) {
			Iso8583Post msg = (Iso8583Post) object;

			try {
				pCode = msg.getProcessingCode().toString();
				switch (pCode) {
				case "391000":
					pCode = new ProcessingCode("321000").toString();
					break;
				case "392000":
					pCode = new ProcessingCode("322000").toString();
					break;
				default:
					break;
				}
			} catch (Exception e) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						ConstructFieldMessage.class.getName(), "Method: [transformField3ForRelatedAccountsInquiryATM]",
						Utils.getStringMessageException(e),
						((Base24Ath) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			}
		}
		return pCode;
	}

	public String constructField35To0220Error(Iso8583 msg) throws XPostilion {
		String field35 = Constants.General.DEFAULT_P35;
		if (msg.isFieldSet(Iso8583.Bit._035_TRACK_2_DATA)) {
			try {
				if (msg.getTrack2Data().getPan().length() < 19) {
					field35 = msg.getField(Iso8583.Bit._035_TRACK_2_DATA);
				}
			} catch (XFieldUnableToConstruct e) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						ConstructFieldMessage.class.getName(), "Method: [constructField35To0220Error]",
						Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			}
		}
		return field35;
	}
	
	public String constructField17(Object object, Integer num) throws XPostilion {
		Iso8583Post msg = (Iso8583Post) object;
		String field;
		if (msg.isFieldSet(17)) {
			field = msg.getField(17);
		} else {
			LocalDate local = LocalDate.now();
			String date = local.format(DateTimeFormatter.ofPattern("MMdd"));
			field = date;
		}
		return field;
	}
	
}