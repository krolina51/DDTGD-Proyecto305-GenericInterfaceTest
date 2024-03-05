package postilion.realtime.genericinterface.translate;

import java.math.BigInteger;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import postilion.realtime.date.SettlementDate;
import postilion.realtime.genericinterface.GenericInterface;
import postilion.realtime.genericinterface.InvokeMethodByConfig;
import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.channels.Super;
import postilion.realtime.genericinterface.extract.Extract;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.database.DBHandler;
import postilion.realtime.genericinterface.translate.database.InfoRelatedToCard;
import postilion.realtime.genericinterface.translate.stream.Header;
import postilion.realtime.genericinterface.translate.util.BussinesRules;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.Constants.FormatDate;
import postilion.realtime.genericinterface.translate.util.Constants.General;
import postilion.realtime.genericinterface.translate.util.EventReporter;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.library.common.InitialLoadFilter;
import postilion.realtime.library.common.model.ConfigAllTransaction;
import postilion.realtime.library.common.model.ResponseCode;
import postilion.realtime.library.common.util.constants.TagNameStructuredData;
import postilion.realtime.sdk.crypto.DesKwa;
import postilion.realtime.sdk.env.calendar.BusinessCalendar;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.message.bitmap.ProcessingCode;
import postilion.realtime.sdk.message.bitmap.StructuredData;
import postilion.realtime.sdk.message.bitmap.XBitmapUnableToConstruct;
import postilion.realtime.sdk.message.bitmap.XFieldUnableToConstruct;
import postilion.realtime.sdk.util.DateTime;
import postilion.realtime.sdk.util.TimedHashtable;
import postilion.realtime.sdk.util.XPostilion;
import postilion.realtime.sdk.util.convert.Pack;

/**
 * Esta clase permite ser llamada por la clase GenericInterface para procesar
 * informacion y armar los mensjes B24 a ISO8583Post y viceversa
 *
 * @author Albert Medina y Fernando Casta�eda
 */

public class MessageTranslator {

	private DesKwa kwa;
	private TimedHashtable sourceTranToTmHashtable = null;
	private TimedHashtable sourceTranToTmHashtableB24 = null;
	private Map<String, ConfigAllTransaction> structureMap = new HashMap<>();
	private Map<String, ResponseCode> allCodesIsoToB24TM = new HashMap<>();
	private Client udpClient = null;
	private Client udpClientV2 = null;
	private String nameInterface = "";
	protected String responseCodesVersion = "1";
	private Parameters params;
	private BusinessCalendar objectBusinessCalendar = null;

	public MessageTranslator() {

	}

	public MessageTranslator(Parameters params) {
		this.kwa = params.getKwa();
		this.sourceTranToTmHashtable = params.getSourceTranToTmHashtable();
		this.sourceTranToTmHashtableB24 = params.getSourceTranToTmHashtableB24();
		this.udpClient = params.getUdpClient();
		this.udpClientV2 = params.getUdpClientV2();
		this.nameInterface = params.getNameInterface();
		this.responseCodesVersion = params.getResponseCodesVersion();
		this.params = params;
	}

	/**
	 * Method for be invoked in interface to use
	 *
	 * @param msg from Base24
	 * @return Iso8583Post Message
	 * @throws XBitmapUnableToConstruct
	 * @throws XPostilion
	 */
	static long tStart;

	/**
	 * Get the Hexadecimal BitMap from received Message.
	 *
	 * @param msg Message from Interchange in Base24
	 * @return BitMap Message in Hexadecimal
	 * @throws XBitmapUnableToConstruct
	 * @throws XPostilion
	 */
	public String getBitMap(Base24Ath msg) throws XPostilion {
		try {
			String trama = new String(msg.getBinaryData());

			StringBuilder bitMap = new StringBuilder().append(trama.substring(16, 32));

			BigInteger initial = new BigInteger(trama.substring(16, 17), 16);
			StringBuilder bitMapBinario = new StringBuilder();
			switch (initial.compareTo(BigInteger.valueOf(4))) {
			case -1:
				bitMapBinario.append("00");
				break;
			case 0:
			case 1:
				switch (initial.compareTo(BigInteger.valueOf(8))) {
				case -1:
					bitMapBinario.append("0");
					break;
				case 0:
				case 1:
					bitMap.append(trama.substring(32, 48));
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
			bitMapBinario.append(new BigInteger(bitMap.toString(), 16).toString(2));
			return bitMapBinario.toString();
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e,
					((Iso8583) msg).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "getBitMap", this.udpClient);
		}
		return null;
	}

	public Base24Ath constructBase24Request(Iso8583Post msg) {
		Base24Ath msgToRmto = new Base24Ath(this.kwa);
		String retRefNumber = "N/D";
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			InvokeMethodByConfig invoke = new InvokeMethodByConfig(params);
			String strTypeMsg = msg.getMessageType();

			msgToRmto.putHeader(constructAtmHeaderSinkNode(msg));
			msgToRmto.putMsgType(msg.getMsgType());

			StructuredData sd = new StructuredData();
			sd = msg.getStructuredData();
			String pCode126 = sd.get("B24_Field_126") != null ? sd.get("B24_Field_126").substring(22, 28) : null;

			Map<String, String> deleteFieldsRequest = null;
			Map<String, String> createFieldsRequest = null;

			switch (strTypeMsg) {
			case "0200":
				deleteFieldsRequest = GenericInterface.fillMaps.getDeleteFieldsRequest();
				createFieldsRequest = GenericInterface.fillMaps.getCreateFieldsRequest();
				break;
			case "0420":
			case "0421":
				
				switch (msg.getPrivField(Iso8583Post.PrivBit._006_AUTH_PROFILE)) {
				case "30":
					deleteFieldsRequest = GenericInterface.fillMaps.getDeleteFieldsRevAuto();
					createFieldsRequest = GenericInterface.fillMaps.getCreateFieldsRevAuto();
					break;
				default:
					deleteFieldsRequest = GenericInterface.fillMaps.getDeleteFieldsRevRequest();
					createFieldsRequest = GenericInterface.fillMaps.getCreateFieldsRevRequest();
					break;
				}
				
				break;

			default:

				break;
			}
			for (int i = 3; i <= 126; i++) {
				if (sd != null && sd.get("B24_Field_" + String.valueOf(i)) != null)
					msgToRmto.putField(i, sd.get("B24_Field_" + String.valueOf(i)));
				else if (msg.isFieldSet(i))
					msgToRmto.putField(i, msg.getField(i));
				
				if (sd != null && sd.get("B24_Field_REV_" + String.valueOf(i)) != null)
					msgToRmto.putField(i, sd.get("B24_Field_REV_" + String.valueOf(i)));
				else if (msg.isFieldSet(i))
					msgToRmto.putField(i, msg.getField(i));

				String key1 = String.valueOf(i) + "-" + msg.getField(Iso8583.Bit._003_PROCESSING_CODE) + "_" + pCode126;
				String key2 = String.valueOf(i) + "-" + msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
				String key3 = String.valueOf(i);

				String methodName = null;

				if (createFieldsRequest.containsKey(key1)) {
					methodName = createFieldsRequest.get(key1);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));

				} else if (createFieldsRequest.containsKey(key2)) {
					methodName = createFieldsRequest.get(key2);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));
				} else if (createFieldsRequest.containsKey(key3)) {
					methodName = createFieldsRequest.get(key3);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));
				}

			}

			String PCode = sd.get("CHANNEL_PCODE") != null
					? sd.get("CHANNEL_PCODE") + "_" + msg.getField(Iso8583.Bit._003_PROCESSING_CODE)
					: msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
			Set<String> set = deleteFieldsRequest.keySet().stream().filter(s -> s.length() <= 3)
					.collect(Collectors.toSet());

			if (set.size() > 0) {
				for (String item : set) {
					if (msgToRmto.isFieldSet(Integer.parseInt(item))) {
						msgToRmto.clearField(Integer.parseInt(item));
					}
				}
			}

			if (deleteFieldsRequest.containsKey(PCode)) {
				String[] parts = deleteFieldsRequest.get(PCode).split("-");
				for (String item : parts) {
					if (msgToRmto.isFieldSet(Integer.parseInt(item))) {
						msgToRmto.clearField(Integer.parseInt(item));
					}
				}
			}
			if (sd.get("NEXTDAY") != null && sd.get("NEXTDAY").equals("TRUE")) {
				objectBusinessCalendar = new BusinessCalendar("DefaultBusinessCalendar");
				Date businessCalendarDate = this.objectBusinessCalendar.getNextBusinessDate();
				String settlementDate = new SimpleDateFormat("MMdd").format(businessCalendarDate);
				msgToRmto.putField(Iso8583.Bit._017_DATE_CAPTURE, settlementDate);
			}
//			if(msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA)
//					&& msg.getStructuredData().get("ANULACION") != null
//					&& msg.getStructuredData().get("ANULACION").equals("TRUE")
//					&& msg.isFieldSet(Iso8583Post.Bit._059_ECHO_DATA)) {
//				String[] dataP59 = msg.getField(Iso8583Post.Bit._059_ECHO_DATA).split("\\|");
//				if(dataP59.length>=1)
//					msgToRmto.putField(Iso8583.Bit._038_AUTH_ID_RSP, dataP59[0]);
//			}

		} catch (XPostilion e) {
			msgToRmto = null;
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"constructBase24Request", this.udpClient, "of type XPostilion");
		} catch (Exception e1) {
			msgToRmto = null;
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e1, retRefNumber,
					"constructBase24Request", this.udpClient, "of type Exception");
		}

		return msgToRmto;
	}

	public Base24Ath constructBase24(Iso8583Post msg) {
		Base24Ath msgToRmto = new Base24Ath(this.kwa);
		String strTypeMsg = msg.getMessageType();
		String retRefNumber = "N/D";
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)
					+ msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR);
			msgToRmto.putHeader(constructAtmHeaderSourceNode(msgToRmto));
			msgToRmto.putMsgType(msg.getMsgType());
			Iso8583Post msgOriginal = null;
			Base24Ath msgB24Orig = (Base24Ath) this.sourceTranToTmHashtableB24.get(retRefNumber);
			StructuredData sd = new StructuredData();

			InvokeMethodByConfig invoke = new InvokeMethodByConfig(params);

			if (msg.getResponseCode().equals(Iso8583.RspCode._00_SUCCESSFUL)) {// si la respuesta es exitosa
				sd = msg.getStructuredData();
				msgOriginal = (Iso8583Post) this.sourceTranToTmHashtable.get(retRefNumber);

				this.udpClient.sendData(Client.getMsgKeyValue(retRefNumber,
						"VA A ENTRAR .... A SACAR EL SD DEL MENSAJE ORIGINAL", "LOG", this.nameInterface));
				if ((sd == null && msgOriginal != null)) {
//				if (msgOriginal != null) {
					this.udpClient.sendData(Client.getMsgKeyValue(retRefNumber,
							"ENTRO A SACAR EL SD DEL MENSAJE ORIGINAL", "LOG", this.nameInterface));
					sd = msgOriginal.getStructuredData();
				}

			} else {
//				if (!msg.getMessageType().equals(Iso8583.MsgTypeStr._0430_ACQUIRER_REV_ADV_RSP)) {//respuesta no exitosa !=430
				msgOriginal = (Iso8583Post) this.sourceTranToTmHashtable.get(retRefNumber);
				if (msgOriginal != null) {
					sd = msgOriginal.getStructuredData();
				}
//				}
			}

			Map<String, String> copyFieldsResponse = null;
			Map<String, String> deleteFieldsResponse = null;
			Map<String, String> createFieldsResponse = null;
			Map<String, String> transformFieldsResponse = null;
			String pCode126 = null;

			switch (strTypeMsg) {
			case "0210":
				copyFieldsResponse = GenericInterface.fillMaps.getCopyFieldsResponse();
				deleteFieldsResponse = GenericInterface.fillMaps.getDeleteFieldsResponse();
				createFieldsResponse = GenericInterface.fillMaps.getCreateFieldsResponse();
				transformFieldsResponse = GenericInterface.fillMaps.getTransformFieldsResponse();
				pCode126 = sd.get("B24_Field_126") != null ? sd.get("B24_Field_126").substring(22, 28) : null;
				break;
			case "0230":
				copyFieldsResponse = GenericInterface.fillMaps.getCopyFieldsResponseAdv();
				deleteFieldsResponse = GenericInterface.fillMaps.getDeleteFieldsResponseAdv();
				createFieldsResponse = GenericInterface.fillMaps.getCreateFieldsResponseAdv();
				transformFieldsResponse = GenericInterface.fillMaps.getTransformFieldsResponseAdv();
				break;
			case "0430":
				copyFieldsResponse = GenericInterface.fillMaps.getCopyFieldsResponseRev();
				deleteFieldsResponse = GenericInterface.fillMaps.getDeleteFieldsResponseRev();
				createFieldsResponse = GenericInterface.fillMaps.getCreateFieldsResponseRev();
				transformFieldsResponse = GenericInterface.fillMaps.getTransformFieldsResponseRev();
				break;
			default:

				break;
			}

			// Copia los campos en el mensaje B24
			for (String key : copyFieldsResponse.keySet()) {

				int intKey = Integer.parseInt(key);
				if (msg.isFieldSet(intKey)) {
					msgToRmto.putField(intKey, msg.getField(intKey));
				}

			}

			if (sd != null) {
				msgToRmto = constructFieldsFromSd(msgToRmto, sd);
			}
			sd = msg.getStructuredData();

			if (sd != null) {
				msgToRmto = constructFieldsFromSd(msgToRmto, sd);
			}

			if (!msgToRmto.getField(Iso8583.Bit._039_RSP_CODE).equals(Iso8583.RspCode._00_SUCCESSFUL)
					&& msgToRmto.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)) {

				msgToRmto.putField(Iso8583.Bit._102_ACCOUNT_ID_1, Pack.resize(Constants.Account.ACCOUNT_DEFAULT,
						msgToRmto.getFieldLength(Iso8583.Bit._102_ACCOUNT_ID_1), '0', false));
			}
			if (!msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA)
					|| msg.getStructuredData().get("B24_Field_41") == null) {
				msgToRmto.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID,
						msgB24Orig != null && msgB24Orig.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)
								? msgB24Orig.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)
								: Pack.resize(msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID), 16, ' ', true));
			}

			// SKIP-TRANSFORM y TRANSFORM

			for (int i = 3; i < 127; i++) {

				String key1 = String.valueOf(i) + "-" + msg.getField(Iso8583.Bit._003_PROCESSING_CODE) + "_" + pCode126;
				String key2 = String.valueOf(i) + "-" + msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
				String key3 = String.valueOf(i);

				String methodName = null;

				if (createFieldsResponse.containsKey(key1)) {
					methodName = createFieldsResponse.get(key1);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));

				} else if (createFieldsResponse.containsKey(key2)) {
					methodName = createFieldsResponse.get(key2);

					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));
				} else if (createFieldsResponse.containsKey(key3)) {
					methodName = createFieldsResponse.get(key3);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));
				}
				if (transformFieldsResponse.containsKey(key1)) {
					methodName = transformFieldsResponse.get(key1);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));

				} else if (transformFieldsResponse.containsKey(key2)) {
					methodName = transformFieldsResponse.get(key2);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));
				} else if (transformFieldsResponse.containsKey(key3)) {
					methodName = transformFieldsResponse.get(key3);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));
				}
			}

			if (msgB24Orig != null && msg.getMessageType().equals("0430")) {
				if (msgB24Orig.isFieldSet(Iso8583.Bit._095_REPLACEMENT_AMOUNTS))
					msgToRmto.putField(Iso8583.Bit._095_REPLACEMENT_AMOUNTS,
							msgB24Orig.getField(Iso8583.Bit._095_REPLACEMENT_AMOUNTS));
				if (msgB24Orig.isFieldSet(125))
					msgToRmto.putField(125, msgB24Orig.getField(125));
			}

			// Busca si hay que eliminar campos dado el processingCode

			String PCode = msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
			Set<String> set = deleteFieldsResponse.keySet().stream().filter(s -> s.length() <= 3)
					.collect(Collectors.toSet());

			if (set.size() > 0) {
				for (String item : set) {
					if (msgToRmto.isFieldSet(Integer.parseInt(item))) {
						msgToRmto.clearField(Integer.parseInt(item));
					}
				}
			}

			if (deleteFieldsResponse.containsKey(PCode)) {
				String[] parts = deleteFieldsResponse.get(PCode).split("-");
				for (String item : parts) {
					if (msgToRmto.isFieldSet(Integer.parseInt(item))) {
						msgToRmto.clearField(Integer.parseInt(item));
					}
				}
			}

			if (msgB24Orig != null) {
				msgToRmto.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION,
						msgB24Orig.getField(Iso8583.Bit._004_AMOUNT_TRANSACTION));
				msgToRmto.putField(Iso8583.Bit._035_TRACK_2_DATA, msgB24Orig.getField(Iso8583.Bit._035_TRACK_2_DATA));
			}

		} catch (XPostilion e) {
			msgToRmto = null;
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"constructBase24", this.udpClient, "of type XPostilion");
		} catch (Exception e1) {
			msgToRmto = null;
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e1, retRefNumber,
					"constructBase24", this.udpClient, "of type Exception");
		}

		return msgToRmto;
	}

	// public Iso8583Post constructIso8583(Base24Ath msgFromRemote, HashMap<String,
	// String> returnInfoValidations) {

	public Iso8583Post constructIso8583(Base24Ath msgFromRemote, Super objectValidations) {
		Iso8583Post Iso = new Iso8583Post();
		tStart = System.currentTimeMillis();
		String strTypeMsg = msgFromRemote.getMessageType();
		String retRefNumber = "N/D";
		String channel;

		try {

			Iso.setMessageType(msgFromRemote.getMessageType());
			String strBitmap = this.getBitMap(msgFromRemote);
			StructuredData sd = new StructuredData();
			retRefNumber = msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);

			char[] chars = strBitmap.toCharArray();

			for (int i = 0; i < chars.length; i++) {
				if (chars[i] == '1') {
					processField(msgFromRemote, Iso, i + 1, sd);
				} else {
					processCreateField(msgFromRemote, Iso, i + 1);
				}
			}

			InvokeMethodByConfig invoke = new InvokeMethodByConfig(params);
			ConstructFieldMessage constructor = new ConstructFieldMessage(this.params);
			Iso.putField(Iso8583Post.Bit._123_POS_DATA_CODE,
					invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
							"constructPosDataCode", msgFromRemote, Iso8583Post.Bit._123_POS_DATA_CODE));

			if (msgFromRemote.getMessageType().equals(Iso8583.MsgTypeStr._0220_TRAN_ADV)) {
				Iso.putField(Iso8583Post.Bit._039_RSP_CODE,
						constructor.constructP39For0220NotiBloq(msgFromRemote, Iso8583Post.Bit._039_RSP_CODE));
				Iso.putField(63, constructor.constructResponseCodeField63(msgFromRemote, 63));
			} else {
				Iso.putField(Iso8583Post.Bit._026_POS_PIN_CAPTURE_CODE,
						constructor.constructPosPinCaptureCode(Iso, Iso8583Post.Bit._026_POS_PIN_CAPTURE_CODE));
			}

			Iso.putPrivField(Iso8583Post.PrivBit._002_SWITCH_KEY,
					constructor.constructSwitchKey(msgFromRemote, this.nameInterface));

			sd.put(TagNameStructuredData.REQ_TIME, String.valueOf(System.currentTimeMillis() - tStart));

			if (strTypeMsg.equals("0420")) {
				Iso.putPrivField(Iso8583Post.PrivBit._011_ORIGINAL_KEY,
						msgFromRemote.getField(Iso8583Post.Bit._090_ORIGINAL_DATA_ELEMENTS).substring(0, 32));

				// Iso.putField(Iso8583Post.Bit._090_ORIGINAL_DATA_ELEMENTS,
				// msgFromRemote.getField(Iso8583Post.Bit._090_ORIGINAL_DATA_ELEMENTS));
			}

			if (msgFromRemote.getMessageType().equals(Iso8583.MsgTypeStr._0200_TRAN_REQ)) {
				// Se invoca al metodo getTransactionConsecutive a fin de obtener el consecutivo
				// para la transaaci�n
				String cons = constructor.getTransactionConsecutive(
						msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR).substring(5, 9), "00",
						this.params.getTermConsecutiveSection());
				sd.put("REFERENCE_KEY", msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR).concat("|")
						.concat(cons.split(",")[0].trim().concat(cons.split(",")[1].trim())));
			}

			Super account = new Super(true, General.VOIDSTRING, General.VOIDSTRING, General.VOIDSTRING,
					new HashMap<String, String>(), params) {

				@Override
				public void validations(Base24Ath msg, Super objectValidations) {

				}
			};

			String p41 = msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID);
			channel = BussinesRules.channelIdentifier(msgFromRemote, this.nameInterface, this.udpClient);

			switch (channel) {

			case Constants.Channels.OFC:
				objectValidations.putInforCollectedForStructData("CHANNEL", "4");
				objectValidations.putInforCollectedForStructData("Identificacion_Canal", "OF");
				switch (msgFromRemote.getProcessingCode().toString()) {

				// RETIRO OFIAVAL
				case Constants.Channels.PCODE_RETIRO_ATM_A:
				case Constants.Channels.PCODE_RETIRO_ATM_C:

					sd.put("CHANNEL_TX", channel);
					SettlementDate date = new SettlementDate(this.params.getCalendarInfo().getCalendar());
					date.calculateDate((Iso8583) msgFromRemote);
					sd.put("CURRENT_TX", !date.isNextDay() ? "S" : "N");

					// sd.put("CHANNEL", Constants.DefaultATM.ATM_CHANNEL_NAME);
					// sd.put("Identificacion_Canal", Constants.DefaultATM.ATM_ID_CHANNEL);
					// sd.put("Canal", Constants.DefaultATM.ATM_CHANNEL);
					// se realiza comentario a la siguiente linea por solicitud de Andres Meneces
					// sd.put("VIEW_ROUTER", Constants.DefaultATM.ATM_VIEW_ROUTER);
					// sd.put("VIEW_ROUTER", Constants.DefaultOficinasAVAL.OFC_VIEW_ROUTER);
					// sd.put("TRANSACTION_INPUT", Constants.DefaultATM.ATM_TRANSACTION_INPUT);
					// sd.put("Codigo_Transaccion", Constants.DefaultATM.ATM_COD_TRANSACTION);

					sd.put("CARD_NUMBER", msgFromRemote.getTrack2Data().getPan());

					String strCut = ConstructFieldMessage.createfieldCut(msgFromRemote, this.nameInterface,
							GenericInterface.fillMaps.getCutValues(), this.udpClient);
					sd.put("CUT_origen_de_la_transaccion", strCut);
					sd.put("CUT_propio_de_la_transaccion", strCut);

					sd.put("Indicador_De_Aceptacion_O_De_No_Preaprobado",
							BussinesRules.getIndicador_De_Aceptacion_O_De_No_Preaprobado(msgFromRemote));

					sd.put("Entidad",
							msgFromRemote.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA)
									? msgFromRemote.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(0, 4)
									: "0001");

					fillAccount(msgFromRemote, objectValidations, new DBHandler(this.params), retRefNumber);

					sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append("103").toString(),
							Constants.Account.ACCOUNT_DEFAULT);

					if (strTypeMsg.equals("0420")) {
						sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append("104").toString(),
								Constants.Account.ACCOUNT_DEFAULT);
					}

					sd.put("Codigo_Transaccion_Producto",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")
									? "05"
									: "04");
					sd.put("Tipo_de_Cuenta_Debitada",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")
									? "AHO"
									: "CTE");
					sd.put("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
					sd.put("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
					sd.put("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
					sd.put("DEBIT_CARD_NR", objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));

					break;
				case Constants.Channels.PCODE_ANULACION_PAGO_CREDITO_HIPOTECARIO_EFECTIVO:
				case Constants.Channels.PCODE_ANULACION_PAGO_CREDITO_HIPOTECARIO_CHEQUE:
				case Constants.Channels.PCODE_ANULACION_PAGO_TC_EFECTIVO:
				case Constants.Channels.PCODE_ANULACION_PAGO_TC_CHEQUE:
				case Constants.Channels.PCODE_ANULACION_PAGO_CREDITO_CUPO_ROTATIVO_EFECTIVO:
				case Constants.Channels.PCODE_ANULACION_PAGO_CREDITO_CUPO_ROTATIVO_CHEQUE:
				case Constants.Channels.PCODE_ANULACION_PAGO_OTROS_CREDITOS_EFECTIVO:
				case Constants.Channels.PCODE_ANULACION_PAGO_OTROS_CREDITOS_CHEQUE:
				case Constants.Channels.PCODE_ANULACION_PAGO_MOTOS_Y_VEHICULOS_EFECTIVO:
				case Constants.Channels.PCODE_ANULACION_PAGO_MOTOS_Y_VEHICULOS_CHEQUE:

					objectValidations.putInforCollectedForStructData("Tipo_de_Tarjeta", "0");
					objectValidations.putInforCollectedForStructData("Dispositivo", "0");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "CRE");
					objectValidations.putInforCollectedForStructData("Entidad", "0000");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "PO_OFICINAS");

					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "32");
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "DEBCOR");

					objectValidations.putInforCollectedForStructData("Mod_Credito", "9");
					objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");

					// tarjetas y cuentas por cuadrar segun modelo en el pcode no aparece el segundo
					// tipo de cuenta revisar
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "06");

					Iso.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION,
							msgFromRemote.getField(Iso8583.Bit._054_ADDITIONAL_AMOUNTS).substring(30));
					objectValidations.putInforCollectedForStructData("B24_Field_35",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA));
					Iso.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);
					objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(6, 24));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(6, 24));
					objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(6, 24));
					// *

					break;
				// DEVOLUCION CANJE
				case Constants.Channels.PCODE_DEVOLUCION_CANJE_PAGO_A_CREDIBANCO_HIPOTECARIO:
				case Constants.Channels.PCODE_DEVOLUCION_CANJE_PAGO_A_TARJETA_CREDITO:
				case Constants.Channels.PCODE_DEVOLUCION_CANJE_PAGO_A_CUPO_ROTATIVO:
				case Constants.Channels.PCODE_DEVOLUCION_CANJE_PAGO_A_OTROS_CREDITOS:
				case Constants.Channels.PCODE_DEVOLUCION_CANJE_PAGO_A_CREDITO_MOTO_Y_VEHICULO:

					objectValidations.putInforCollectedForStructData("Tipo_de_Tarjeta", "0");
					objectValidations.putInforCollectedForStructData("Dispositivo", "0");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "CRE");
					objectValidations.putInforCollectedForStructData("Entidad", "0000");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "PO_OFICINAS");

					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "32");
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "DEBCOR");

					objectValidations.putInforCollectedForStructData("Mod_Credito", "9");
					objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");

					// tarjetas y cuentas por cuadrar segun modelo en el pcode no aparece el segundo
					// tipo de cuenta revisar
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "06");

					objectValidations.putInforCollectedForStructData("B24_Field_17",
							msgFromRemote.getField(Iso8583.Bit._015_DATE_SETTLE));
					objectValidations.putInforCollectedForStructData("B24_Field_35",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA));
					objectValidations.putInforCollectedForStructData("B24_Field_43",
							msgFromRemote.getField(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC));
					Iso.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION,
							msgFromRemote.getField(Iso8583.Bit._054_ADDITIONAL_AMOUNTS).substring(30));
					Iso.putField(Iso8583.Bit._017_DATE_CAPTURE, msgFromRemote.getField(Iso8583.Bit._015_DATE_SETTLE));
					Iso.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);
					fillAccount(msgFromRemote, account, new DBHandler(this.params), retRefNumber);
					objectValidations.putInforCollectedForStructData("P_CODE",
							account.getInforCollectedForStructData().get("P_CODE"));
					// Se envia el CHANNEL en 4 para que ISC identifique que son tx sin tarjeta
					// presente
					objectValidations.putInforCollectedForStructData("CHANNEL", "4");
					objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(6, 24));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(6, 24));
					objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(6, 24));

					break;

				case Constants.Channels.PCODE_CONSULTA_TITULARIDAD_CREDITO_HIPOTECARIO:
				case Constants.Channels.PCODE_CONSULTA_TITULARIDAD_TARJETA_CREDITO:
				case Constants.Channels.PCODE_CONSULTA_TITULARIDAD_CREDITO_ROTATIVO:
				case Constants.Channels.PCODE_CONSULTA_TITULARIDAD_OTROS_CREDITOS:
				case Constants.Channels.PCODE_CONSULTA_TITULARIDAD_CREDITO_MOTO_VEHICULO:

					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "PO_OFICINAS");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "06");

					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "62");
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "CONSUL");

					objectValidations.putInforCollectedForStructData("Codigo_FI_Origen", "1011");
					objectValidations.putInforCollectedForStructData("Nombre_FI_Origen", "OFI");
					objectValidations.putInforCollectedForStructData("Codigo_de_Red",
							(msgFromRemote.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
									? msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4)
									: msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "   ");
					objectValidations.putInforCollectedForStructData("PAN_Tarjeta", "                   ");
					objectValidations.putInforCollectedForStructData("Vencimiento", "0000");
					objectValidations.putInforCollectedForStructData("Ent_Adq",
							(msgFromRemote.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
									? msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4)
									: msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
					objectValidations.putInforCollectedForStructData("Canal", "01");
					objectValidations.putInforCollectedForStructData("pos_entry_mode", "000");
					objectValidations.putInforCollectedForStructData("service_restriction_code", "000");
					objectValidations.putInforCollectedForStructData("Identificador_Terminal", "0");
					objectValidations.putInforCollectedForStructData("Dispositivo", "0");
					objectValidations.putInforCollectedForStructData("FI_CREDITO_REV", "0000");
					objectValidations.putInforCollectedForStructData("FI_DEBITO_REV", "0000");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR_REV", "000000000000000000");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE_REV", "   ");
					objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE_REV", "   ");
					objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_NR_REV", "000000000000000000");

					objectValidations.putInforCollectedForStructData("B24_Field_3",
							msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE));

					Iso.putField(Iso8583.Bit._003_PROCESSING_CODE,
							Iso8583Post.TranType._32_GENERAL_INQUIRY
									.concat(new ProcessingCode(msgFromRemote.getField(3)).getFromAccount())
									.concat(msgFromRemote.getProcessingCode().getToAccount()).toString());

					objectValidations.putInforCollectedForStructData("B24_Field_17",
							msgFromRemote.getField(Iso8583.Bit._015_DATE_SETTLE));
					objectValidations.putInforCollectedForStructData("B24_Field_35",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA));
					Iso.putField(Iso8583.Bit._017_DATE_CAPTURE, msgFromRemote.getField(Iso8583.Bit._015_DATE_SETTLE));
					Iso.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);
					objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(6, 24));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(6, 24));
					objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(6, 24));

					Iso.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION, Constants.General.TWELVE_ZEROS);
					Iso.putField(Iso8583.Bit._049_CURRENCY_CODE_TRAN, Constants.General.DEFAULT_ERROR_049);

					Iso.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE,
							invoke.invokeMethodConfig(
									"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
									"constructField100", msgFromRemote, 100));

					// Se envia el CHANNEL en 4 para que ISC identifique que son tx sin tarjeta
					// presente
					objectValidations.putInforCollectedForStructData("CHANNEL", "4");

					break;

				// TRANSFERENCIAS OFIAVAL
				case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_AHORROS:
					switch (msgFromRemote.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {

					// CHIP-TARJETA
					// CON 051 - 071 SE CONSULTA LA TARJETA
					// CON 021 SE ENVIA DE LA MENSAJERIA BANDA
					case "051":
						// DEBITO
						if (msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3).equals("0")) {

							// TAGS ISC
							// **********************************************************************************************************
							fillAccount(msgFromRemote, objectValidations, new DBHandler(this.params), retRefNumber);
							fillSDtagsInfoAccount(msgFromRemote, objectValidations, retRefNumber);
							// TAGS ISC
							// **********************************************************************************************************

						}

						// CREDITO
						if (msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3).equals("1")) {

							// TAGS ISC
							// ****************************************************************************************************
							fillInfoSDToTransactionByAccount(msgFromRemote, objectValidations,
									new DBHandler(this.params), retRefNumber);
							objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
									objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_NR"));
							objectValidations.tagsEncodeSensitiveData("DEBIT_ACCOUNT_NR",
									msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(
											msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 18),
									objectValidations);
							objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
									objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE"));
							objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
									new ProcessingCode(msgFromRemote.getField(3)).getFromAccount());
							fillAccount(msgFromRemote, account, new DBHandler(this.params), retRefNumber);
							objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
									account.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
							objectValidations.putInforCollectedForStructData("P_CODE",
									account.getInforCollectedForStructData().get("P_CODE"));
							// TAGS ISC
							// ****************************************************************************************************

						}

						// MIXTA
						if (msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3).equals("2")) {

							// TAGS ISC
							// ****************************************************************************************************
							fillAccount(msgFromRemote, objectValidations, new DBHandler(this.params), retRefNumber);
							objectValidations.tagsEncodeSensitiveData("CLIENT2_ACCOUNT_NR",
									msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
									objectValidations);
							objectValidations.putInforCollectedForStructData("CLIENT2_ACCOUNT_TYPE",
									new ProcessingCode(msgFromRemote.getField(3)).getToAccount());
							fillSDtagsInfoAccount(msgFromRemote, objectValidations, retRefNumber);
							// TAGS ISC
							// ****************************************************************************************************

						}

						break;

					default:
						break;
					}

					break;

				// PAGO OBLIGACIONES OFIAVAL EFECTIVO Y CHEQUE

				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_EFECTIVO:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_CHEQUE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_EFECTIVO:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_CHEQUE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_EFECTIVO:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CHEQUE:

					objectValidations.putInforCollectedForStructData("Tipo_de_Tarjeta", "0");
					objectValidations.putInforCollectedForStructData("Dispositivo", "0");
					objectValidations.putInforCollectedForStructData("Entidad", "0000");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "20");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "PO_OFICINAS");
					objectValidations.putInforCollectedForStructData("Codigo_FI_Origen", "1011");
					objectValidations.putInforCollectedForStructData("Nombre_FI_Origen", "OFI");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
							"OF_POBLIG_" + Extract.tagTTypePOblig(msgFromRemote, objectValidations));

					Extract.tagsModelPaymentOfObligationsCredit(objectValidations, msgFromRemote);

					objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1", "0077"
							+ msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(2, 4) + "0000000000000");
					objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1_REV", "0077010000000000000");

					objectValidations.putInforCollectedForStructData("PAN_Tarjeta", "0077"
							+ msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(2, 4) + "0000000000000");
					objectValidations.putInforCollectedForStructData("PAN_Tarjeta_REV", "0077010000000000000");

					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "OTR");
					objectValidations.putInforCollectedForStructData("Ofi_Adqui", "9999");
					objectValidations.putInforCollectedForStructData("Ofi_Adqui_REV", "0000");
					objectValidations.putInforCollectedForStructData("Canal", "01");
					objectValidations.putInforCollectedForStructData("pos_entry_mode", "000");
					objectValidations.putInforCollectedForStructData("service_restriction_code", "000");
					objectValidations.putInforCollectedForStructData("Identificador_Terminal", "0");
					objectValidations.putInforCollectedForStructData("Numero_Cedula",
							msgFromRemote.isFieldSet(125)
									? msgFromRemote.getField(125).substring(msgFromRemote.getField(125).length() - 11)
									: "00000000000");

					objectValidations.putInforCollectedForStructData("B24_Field_35",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA));
					// Se llena campo 52 para reversos
					if (!msgFromRemote.isFieldSet(Iso8583.Bit._052_PIN_DATA))
						objectValidations.putInforCollectedForStructData("B24_Field_52",
								Constants.General.SIXTEEN_ZEROS);
					if (msgFromRemote.isFieldSet(60))
						objectValidations.putInforCollectedForStructData("B24_Field_60", msgFromRemote.getField(60));
					objectValidations.putInforCollectedForStructData("B24_Field_61", "0901BBOG0000P");
					objectValidations.putInforCollectedForStructData("B24_Field_125", msgFromRemote.getField(125));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
					objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
					Iso.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);
					objectValidations.putInforCollectedForStructData("P_CODE",
							msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE));

					// Se envia el CHANNEL en 4 para que ISC identifique que son tx sin tarjeta
					// presente
					objectValidations.putInforCollectedForStructData("CHANNEL", "4");
					objectValidations.putInforCollectedForStructData("FI_CREDITO_REV", "0000");
					objectValidations.putInforCollectedForStructData("FI_DEBITO_REV", "0000");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR_REV", "000000000000000000");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE_REV", "   ");
					objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE_REV", "   ");
					objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_NR_REV", "000000000000000000");
					objectValidations.putInforCollectedForStructData("Codigo_Establecimiento", "          ");
					objectValidations.putInforCollectedForStructData("indicador_efectivo_cheque", "1");
					objectValidations.putInforCollectedForStructData("indicador_efectivo_cheque_REV", "0");
					objectValidations.putInforCollectedForStructData("TIPO_TX", "EFECTIVO");
					objectValidations.putInforCollectedForStructData("TAG_924A",
							msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
					if (channel.equals(Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_CHEQUE)) {
						objectValidations.putInforCollectedForStructData("indicador_efectivo_cheque", "2");
						objectValidations.putInforCollectedForStructData("Mod_Credito", "8");
					} else if (channel.equals(Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CHEQUE)) {
						objectValidations.putInforCollectedForStructData("indicador_efectivo_cheque", "2");
						objectValidations.putInforCollectedForStructData("Mod_Credito", "9");
					}

					objectValidations.putInforCollectedForStructData("Numero_Cheques", "01");

					break;

				// PAGO OBLIGACIONES OFIAVAL EFECTIVO Y CHEQUE

				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_EFECTIVO:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_CHEQUE:

					objectValidations.putInforCollectedForStructData("Tipo_de_Tarjeta", "0");
					objectValidations.putInforCollectedForStructData("Dispositivo", "0");
					objectValidations.putInforCollectedForStructData("Entidad", "0000");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "20");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "PO_OFICINAS");
					objectValidations.putInforCollectedForStructData("Codigo_FI_Origen", "1011");
					objectValidations.putInforCollectedForStructData("Nombre_FI_Origen", "OFI");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
							"OF_PAGOTC_" + Extract.tagTTypePOblig(msgFromRemote, objectValidations));

					Extract.tagsModelPaymentOfObligationsCredit(objectValidations, msgFromRemote);

					objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6) + "0000000000000");

					objectValidations.putInforCollectedForStructData("PAN_Tarjeta",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6) + "0000000000000");

					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "OTR");
					objectValidations.putInforCollectedForStructData("Ofi_Adqui", "9999");
					objectValidations.putInforCollectedForStructData("Canal", "01");
					objectValidations.putInforCollectedForStructData("pos_entry_mode", "000");
					objectValidations.putInforCollectedForStructData("service_restriction_code", "000");
					objectValidations.putInforCollectedForStructData("Identificador_Terminal", "0");
					objectValidations.putInforCollectedForStructData("Numero_Cedula",
							msgFromRemote.isFieldSet(125)
									? msgFromRemote.getField(125).substring(msgFromRemote.getField(125).length() - 11)
									: "00000000000");

					objectValidations.putInforCollectedForStructData("B24_Field_35",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA));
					// Se llena campo 52 para reversos
					if (!msgFromRemote.isFieldSet(Iso8583.Bit._052_PIN_DATA))
						objectValidations.putInforCollectedForStructData("B24_Field_52",
								Constants.General.SIXTEEN_ZEROS);
					if (msgFromRemote.isFieldSet(60))
						objectValidations.putInforCollectedForStructData("B24_Field_60", msgFromRemote.getField(60));
					objectValidations.putInforCollectedForStructData("B24_Field_61", "0901BBOG0000P");
					objectValidations.putInforCollectedForStructData("B24_Field_125", msgFromRemote.getField(125));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
					objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
					Iso.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);
					objectValidations.putInforCollectedForStructData("P_CODE",
							msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE));

					// Se envia el CHANNEL en 4 para que ISC identifique que son tx sin tarjeta
					// presente
					objectValidations.putInforCollectedForStructData("CHANNEL", "4");
					objectValidations.putInforCollectedForStructData("FI_CREDITO_REV", "0000");
					objectValidations.putInforCollectedForStructData("FI_DEBITO_REV", "0000");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR_REV", "000000000000000000");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE_REV", "   ");
					objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE_REV", "   ");
					objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_NR_REV", "000000000000000000");

					Client udpClientValidation = new Client(params.getIpUdpServerValidation(),
							params.getPortUdpServerValidation());

					String msgFromValidationTC = udpClientValidation.sendMsgForValidation(msgFromRemote, nameInterface);

					objectValidations.putInforCollectedForStructData("TC_BOGOTA", msgFromValidationTC);

					if (msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7).equals("0001")
							&& !msgFromValidationTC.equals("SI")) {
						objectValidations.modifyAttributes(false, "TIPO APPL NO RELACIONADO A TARJETA", "2003", "85");
					}
					break;

				// PAGO OBLIGACIONES OFIAVAL DESDE AHORRO Y CTE

				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_PAGO_MOTOS_Y_VEHICULOS_EFECTIVO:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_PAGO_MOTOS_Y_VEHICULOS_CHEQUE:

					switch (msgFromRemote.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {

					// CHIP-TARJETA
					case "051":
						// DEBITO
						if (msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3).equals("0")) {

							fillBasicSDtagsByPayment(msgFromRemote, sd, account, retRefNumber, true);

							// viene solo cuenta corresponsal. CREDITO
							// TAGS UNICOS
							// *********************************************************************************

//							Extract.tagsModelPaymentOfObligationsCredit(objectValidations, msgFromRemote);
							// TAGS UNICOS
							// **********************************************************************************

//							sd.put("DEBIT_ACCOUNT_NR", msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1));							

						}

						// CREDITO
						if (msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3).equals("1")) {

							fillBasicSDtagsByPayment(msgFromRemote, sd, account, retRefNumber, false);
							sd.put("DEBIT_ACCOUNT_NR", "000000000000");
						}

						// MIXTA
						if (msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3).equals("2")) {

							fillBasicSDtagsByPayment(msgFromRemote, sd, account, retRefNumber, true);
							sd.put("CREDIT_ACCOUNT_TYPE", "2");
							sd.put("CREDIT_ACCOUNT_NR",
									msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
						}

						break;

					default:

						break;

					}

					break;

				default:
					Iso.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION,
							msgFromRemote.isFieldSet(Iso8583.Bit._004_AMOUNT_TRANSACTION)
									? msgFromRemote.getField(Iso8583.Bit._004_AMOUNT_TRANSACTION)
									: Constants.General.TWELVE_ZEROS);
					objectValidations.modifyAttributes(false, "TRANSACCION NO CONFIGURADA", "0001", "30");
					break;
				}

				break;

			case Constants.Channels.INTERNET:

				objectValidations.putInforCollectedForStructData("CHANNEL", "8");

				switch (msgFromRemote.getProcessingCode().toString()) {
				// PAGO CONVENIOS
				case Constants.Channels.PCODE_PAGO_SP_CNB_A:// ***
				case Constants.Channels.PCODE_PAGO_SP_CNB_C:// ***

					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR", "0000000000000");

					// TAGS ISC
					if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0088")
							&& msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(25, 26).equals("1")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "5000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "B");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "C2");

					} else if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0088")
							&& msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(25, 26).equals("2")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "6000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "W");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "C6");
					} else if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0099")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "9000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "V");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "C5");
					} else {
						objectValidations.putInforCollectedForStructData("TAG_D140", "8000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "T");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "IT");
					}
					objectValidations.putInforCollectedForStructData("P_CODE",
							msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE));

					objectValidations.putInforCollectedForStructData("B24_Field_35",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA));
					Iso.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);

					// TAGS EXTRACT
					// ***************************************************************************************************
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"INTERNET_PAGO_SERVICIOS_PUBLICOS");
					objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP", "INTERNET_PSP");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V3");
					Extract.tagsModelPspGeneral(objectValidations, msgFromRemote, udpClient, nameInterface);
					objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP", "CREDITO");
					objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP",
							objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_PSP")
									+ "_CREDITO");
					objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "CREDITO");
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS", "15CLASE12001");
					objectValidations.putInforCollectedForStructData("pos_entry_mode", "000");
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR_PSP", "0066010000000000");
					objectValidations.putInforCollectedForStructData("Vencimiento", "0000");
					objectValidations.putInforCollectedForStructData("Ind_4xmil", "0");
					objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID", "0000000000000");
					objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "2");
					objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio_REV", "0");
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
					objectValidations.putInforCollectedForStructData("Tarjeta_Amparada",
							msgFromRemote.getTrack2Data().getPan());
					sd.put("Codigo_Transaccion_Producto",
							msgFromRemote.getProcessingCode().toString().substring(2, 4).equals("10") ? "05" : "04");
					sd.put("Tipo_de_Cuenta_Debitada",
							msgFromRemote.getProcessingCode().toString().substring(2, 4).equals("10") ? "AHO" : "CTE");

					objectValidations.putInforCollectedForStructData("Codigo_Establecimiento", "0000      ");
					objectValidations.putInforCollectedForStructData("Identificacion_Canal_REV", "AT");
					objectValidations.putInforCollectedForStructData("PRIM_COV_NR_REV", "0000"); // CODIGO SERVICIO
					objectValidations.putInforCollectedForStructData("FI__Debito", "0001");
					objectValidations.putInforCollectedForStructData("FI__Debito_REV", "0000");
					objectValidations.putInforCollectedForStructData("Entidad", "0000");
					objectValidations.putInforCollectedForStructData("service_restriction_code", "000");
					objectValidations.putInforCollectedForStructData("Canal", "01");
					objectValidations.putInforCollectedForStructData("Dispositivo", "0");
					objectValidations.putInforCollectedForStructData("Codigo_FI_Origen", "1005");
					objectValidations.putInforCollectedForStructData("Numero_Terminal", "0000");
					objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1", "0000000000000");
					objectValidations.putInforCollectedForStructData("PAN_TARJETA", "0088020000000000001");
					objectValidations.putInforCollectedForStructData("Identificador_Terminal", "0");
					objectValidations.putInforCollectedForStructData("Ent_Adq",
							msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));

					break;

				// PAGO DE OBLIGACIONES CNB internet.
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_CORRIENTE:
					// TAGS ISC
					if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0088")
							&& msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(25, 26).equals("1")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "5000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "B");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "BS");

					} else if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0088")
							&& msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(25, 26).equals("2")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "6000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "W");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "WP");
					} else if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0099")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "9000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "V");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "IV");
					} else {
						objectValidations.putInforCollectedForStructData("TAG_D140", "8000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "T");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "IT");
					}

					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));

					objectValidations.putInforCollectedForStructData("P_CODE",
							msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE));

					objectValidations.putInforCollectedForStructData("B24_Field_35",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA));
					Iso.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);

					// TAGS EXTRACT
					// ***************************************************************************************************
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "INTERNET_PAGOOBLIGACION");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "INTERNET_PAGOOBLIGACION");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
					objectValidations.putInforCollectedForStructData("Codigo_FI_Origen", "1005");
					objectValidations.putInforCollectedForStructData("Canal", "01");
					Extract.tagsModelPaymentOfObligationsCredit(objectValidations, msgFromRemote);
					objectValidations.putInforCollectedForStructData("Dispositivo", "0");

					objectValidations.putInforCollectedForStructData("Entidad", "0000");

					objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1", msgFromRemote
							.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6).concat("0000000000000"));
					objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1_REV", "0099010000000000000");
					objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
					objectValidations.putInforCollectedForStructData("PAN_Tarjeta", msgFromRemote
							.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6).concat("0000000000000"));
					objectValidations.putInforCollectedForStructData("Indicador_Aval", "1");
					objectValidations.putInforCollectedForStructData("pos_entry_mode", "000");
					objectValidations.putInforCollectedForStructData("service_restriction_code", "000");
					objectValidations.putInforCollectedForStructData("Identificador_Terminal", "0");
					objectValidations.putInforCollectedForStructData("FI_DEBITO", "0002");
					objectValidations.putInforCollectedForStructData("Codigo_de_Red",
							msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(4, 8));
					objectValidations.putInforCollectedForStructData("Numero_Terminal", "0000");

					if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0088")
							&& msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(25, 26).equals("1")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "5000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "B");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "BS");

					} else if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0088")
							&& msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(25, 26).equals("2")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "6000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "W");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "WP");
					} else if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0099")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "9000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "V");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "IV");
					} else {
						objectValidations.putInforCollectedForStructData("TAG_D140", "8000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "T");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "IT");
					}
					Extract.tagTTypePObligInternet(msgFromRemote, objectValidations);
					objectValidations.putInforCollectedForStructData("Identificacion_Canal_REV", "AT");
					objectValidations.putInforCollectedForStructData("FI_CREDITO_REV", "0000");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR_REV", "000000000000000000");
					objectValidations.putInforCollectedForStructData("FI_DEBITO_REV", "0000");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE_REV", "   ");
					objectValidations.putInforCollectedForStructData("PAN_Tarjeta_REV", "0099010000000000   ");

					objectValidations.putInforCollectedForStructData("Codigo_Establecimiento", "0000      ");

					break;

				// Transferencias Internet
				case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_AHORROS:// ***
				case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_CORRIENTE:// ***
				case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_CORRIENTE:// ***
				case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_AHORROS:// ***

					// TAGS EXTRACT
					// ***************************************************************************************************
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "INTERNET_TRANSFERENCIA");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "INTERNET_TRANSFERENCIAS");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
					objectValidations.putInforCollectedForStructData("Codigo_FI_Origen", "1005");
					objectValidations.putInforCollectedForStructData("Canal", "01");
					Extract.tagsModelTransferCredit(objectValidations, msgFromRemote);
					objectValidations.putInforCollectedForStructData("Dispositivo", "0");
					objectValidations.putInforCollectedForStructData("Codigo_de_Red", "9601");
					objectValidations.putInforCollectedForStructData("Entidad", "0000");

					////

					// TAGS ISC
					if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0088")
							&& msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(25, 26).equals("1")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "5000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "B");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "BS");

					} else if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0088")
							&& msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(25, 26).equals("2")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "6000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "W");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "WP");
					} else if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 4).equals("0099")) {
						objectValidations.putInforCollectedForStructData("TAG_D140", "9000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "V");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "IV");
					} else {
						objectValidations.putInforCollectedForStructData("TAG_D140", "8000");
						objectValidations.putInforCollectedForStructData("TAG_D139", "T");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "IT");
					}

					objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1", msgFromRemote
							.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6).concat("0000000000000"));
					objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
					objectValidations.putInforCollectedForStructData("PAN_Tarjeta", msgFromRemote
							.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6).concat("0000000000000"));
					objectValidations.putInforCollectedForStructData("Indicador_Aval", "1");
					objectValidations.putInforCollectedForStructData("pos_entry_mode", "000");
					objectValidations.putInforCollectedForStructData("service_restriction_code", "000");
					objectValidations.putInforCollectedForStructData("Identificador_Terminal", "0");

					String[] terminalsID = { "8590", "8591", "8593", "8594" };
					String terminalId = msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(4, 8);

					// ES TRANSFERENCIA CEL2CEL
					if (Arrays.stream(terminalsID).anyMatch(terminalId::equals)) {
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "IT");
						objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C202");
						if (msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(25, 26).equals("0")
								&& msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13)
										.equals("2")) {
							objectValidations.putInforCollectedForStructData("Identificacion_Canal", "PB");
						}
					}

					objectValidations.putInforCollectedForStructData("B24_Field_35",
							msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA));
					Iso.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);
					objectValidations.putInforCollectedForStructData("P_CODE",
							msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE));
					objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
					////

					if (msgFromRemote.isFieldSet(125)
							&& (msgFromRemote.getField(125).length() > 90
									&& msgFromRemote.getField(125).length() <= 150)
							&& (msgFromRemote.getField(125).substring(138, 139).equals("1")
									|| msgFromRemote.getField(125).substring(138, 139).equals("2"))
							&& !msgFromRemote.getField(125).substring(139, 140).equals(" ")) {
						objectValidations.putInforCollectedForStructData("TX_QR", "TRUE");

						// Thread.sleep(1000);

						String indicadorTransferencia = msgFromRemote.getField(125).substring(138, 139);
						String indicadorDevolucion = msgFromRemote.getField(125).substring(139, 140);
						objectValidations.putInforCollectedForStructData("TAG_D139", "_");
						objectValidations.putInforCollectedForStructData("Identificacion_Canal", "IT");
						objectValidations.putInforCollectedForStructData("Dispositivo", "_");
						objectValidations.putInforCollectedForStructData("Nombre_Establecimiento_QR",
								msgFromRemote.getField(125).substring(0, 20));
						objectValidations.putInforCollectedForStructData("PAN_Tarjeta",
								msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6).concat(
										msgFromRemote.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(11, 24)));

						switch (indicadorTransferencia) {
						case "0":
							objectValidations.putInforCollectedForStructData("INDICATIVO_TRANSFERENCIA", "QR_NO");
							break;
						case "1":
							objectValidations.putInforCollectedForStructData("INDICATIVO_TRANSFERENCIA", "QR_PERSONA");
							break;
						case "2":
							objectValidations.putInforCollectedForStructData("INDICATIVO_TRANSFERENCIA", "QR_COMERCIO");
							break;

						default:
							break;
						}

						switch (indicadorDevolucion) {
						case "0":
							objectValidations.putInforCollectedForStructData("INDICATIVO_TX", "ORIGINAL");
							objectValidations.putInforCollectedForStructData("TAG_E0E2", "Q003");
							objectValidations.putInforCollectedForStructData("Transaccion_Unica", "Q003");
							objectValidations.putInforCollectedForStructData("TAG_A9B2", "0");
							objectValidations.putInforCollectedForStructData("TAG_9197", "0000");
							objectValidations.putInforCollectedForStructData("TAG_4043", "000000");
							break;
						case "1":
							objectValidations.putInforCollectedForStructData("INDICATIVO_TX", "DEVOLUCION");
							objectValidations.putInforCollectedForStructData("TAG_E0E2", "Q002");
							objectValidations.putInforCollectedForStructData("Transaccion_Unica", "Q002");
							objectValidations.putInforCollectedForStructData("TAG_A9B2", "1");
							objectValidations.putInforCollectedForStructData("TAG_9197",
									msgFromRemote.getField(125).substring(48, 52));
							objectValidations.putInforCollectedForStructData("TAG_4043",
									msgFromRemote.getField(125).substring(52, 58));
							break;

						default:
							break;
						}
					}
					objectValidations.putInforCollectedForStructData("Codigo_Establecimiento", "0000      ");

					break;

				}

				break;

			default:
				objectValidations.modifyAttributes(false, "CANAL NO CONFIGURADO", "0001", "30");
				break;
			}

			if (msgFromRemote.getMessageType().equals(Iso8583.MsgTypeStr._0220_TRAN_ADV)) {
				Iso.putField(Iso8583Post.Bit._100_RECEIVING_INST_ID_CODE,
						constructor.constructField100(msgFromRemote, Iso8583Post.Bit._100_RECEIVING_INST_ID_CODE));
			}
			sd.put("B24_Field_4", msgFromRemote.getField(Iso8583.Bit._004_AMOUNT_TRANSACTION));
			if (msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE)
					.equals(Constants.Channels.PCODE_CONSULTA_DE_COSTO_CNB)) {
				Iso.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION, "000000000000");
			}

			fillStructuredData(objectValidations, sd, retRefNumber);

			Iso.putStructuredData(sd);

		} catch (XPostilion e) {
			Iso = null;
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"constructIso8583", this.udpClient, "of type XPostilion");
		} catch (Exception e1) {
			Iso = null;
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e1, retRefNumber,
					"constructIso8583", this.udpClient, "of type Exception");
		}

		return Iso;
	}

	/**
	 * llena informacion de cuenta relacionada en el track2 de cuenta
	 *
	 * @param Base24Ath Message from Interchange in Base24
	 * @param Super     objectValidations to filling SD information
	 * @param DBHandler handler to call accountsByNumberClientCNB method
	 * 
	 */
	public void fillInfoSDToTransactionByAccount(Base24Ath msgFromRemote, Super objectValidations, DBHandler handler,
			String retRefNumber) {

		try {
			InfoRelatedToCard infoCard = handler.accountsByNumberClientCNB(
					msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"0" + msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					new ProcessingCode(msgFromRemote.getField(3)).getToAccount(), objectValidations);
			infoCard.loadInfoToTransactionByAccount(objectValidations);
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"fillInfoSDToTransactionByAccount", this.udpClient, "of type Exception");
		}

	}

	/**
	 * llena informacion de cuenta debito, credito apartir de la informacion del
	 * cliente
	 *
	 * @param Base24Ath Message from Interchange in Base24
	 * @param Super     objectValidations to filling SD information
	 * @param String    retRefNumber in case of exception
	 * 
	 */
	public void fillSDtagsInfoAccount(Base24Ath msgFromRemote, Super objectValidations, String retRefNumber) {
		try {
			objectValidations.tagsEncodeSensitiveData("CREDIT_ACCOUNT_NR",
					msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7), objectValidations);
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
					new ProcessingCode(msgFromRemote.getField(3)).getToAccount());
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
					objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
			objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
			objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
					objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
			objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID",
					objectValidations.getInforCollectedForStructData().get("CUSTOMER_ID"));
			objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_NAME",
					objectValidations.getInforCollectedForStructData().get("CUSTOMER_NAME"));
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"fillSDtagsInfoAccount", this.udpClient, "of type XPostilion");
		}

	}

	/**
	 * llena informacion basica en tags para pago de obligaciones
	 *
	 * @param Base24Ath      Message from Interchange in Base24
	 * @param StructuredData sd to filling information
	 * @param Super          account to filling SD information
	 * @param String         retRefNumber in case of exception
	 * 
	 */
	public void fillBasicSDtagsByPayment(Base24Ath msgFromRemote, StructuredData sd, Super account, String retRefNumber,
			boolean getDebitAccount) {
		sd.put("TRANSACTION_INPUT", Constants.DefaultOficinasAVAL.OFC_TRANSACTION_INPUT);
		// se realiza comentario a la siguiente linea por solicitud de Andres Meneces
		// sd.put("VIEW_ROUTER", Constants.DefaultOficinasAVAL.OFC_VIEW_ROUTER);
		sd.put("Mod_Credito", "3");
		sd.put("Mod_CreditoX1", "3");
		sd.put("TRANSACTION_CNB_TYPE", "OFC_POBLIG_" + "OTROS_CREDITOS");
		try {
			sd.put("B24_Field_41", msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID));
			sd.put("B24_Field_102", msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
			sd.put("B24_Field_103", msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2));
			sd.put("P_CODE", msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE));
			if (getDebitAccount) {
				fillAccount(msgFromRemote, account, new DBHandler(this.params), retRefNumber);
				sd.put("DEBIT_ACCOUNT_NR", account.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
				sd.put("DEBIT_ACCOUNT_TYPE", account.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
				sd.put("DEBIT_CARD_CLASS", account.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
			}
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"fillBasicSDtagsByPayment", this.udpClient, "of type XPostilion");
		}
	}

	/**
	 * llena informacion de la cuenta realacionada a la tarjeta
	 *
	 * @param Base24Ath Message from Interchange in Base24
	 * @param Super     objectValidations to filling information
	 * @param DBHandler handler to call accountsClienteCNB method
	 * @param String    retRefNumber in case of exception
	 * 
	 */
	public void fillAccount(Base24Ath msgFromRemote, Super objectValidations, DBHandler handler, String retRefNumber) {
		try {

			InfoRelatedToCard infoCard = handler.accountsClienteCNB(
					msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msgFromRemote.getTrack2Data().getPan(),
					msgFromRemote.getTrack2Data().getExpiryDate(), objectValidations);
			infoCard.chooseAccountWithDefault(
					msgFromRemote.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					new ProcessingCode(msgFromRemote.getField(3)).getFromAccount());
			infoCard.loadInfoToTransaction(objectValidations, new ProcessingCode(msgFromRemote.getField(3)).toString());

//			account.accountsClienteCNB(retRefNumber, msgFromRemote.getTrack2Data().getPan(),
//					msgFromRemote.getTrack2Data().getExpiryDate(), objectValidations);

		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"fillAccount", this.udpClient, "of type Exception");
		}
	}

	/**
	 * llena informacion que trae hash map de la clase super al Structured Data
	 *
	 * @param Super          objectValidations to write information
	 * @param StructuredData sd to fill information
	 * @param String         retRefNumber in case of exception
	 * 
	 */
	public void fillStructuredData(Super objectValidations, StructuredData sd, String retRefNumber) {

		HashMap<String, String> inforCollectedForStructData = objectValidations.getInforCollectedForStructData();

		if (!(inforCollectedForStructData == null)) {
			this.udpClient.sendData(Client.getMsgKeyValue(retRefNumber,
					"Entro poner hashmap si trae algo el map" + objectValidations.toString(), "LOG",
					this.nameInterface));
			this.udpClientV2.sendData(Client.getMsgKeyValue(retRefNumber,
					"Entro poner hashmap si trae algo el map" + objectValidations.toString(), "LOG",
					this.nameInterface));
			for (Map.Entry<String, String> info : inforCollectedForStructData.entrySet()) {
				sd.put(info.getKey(), info.getValue());
				this.udpClient.sendData(Client.getMsgKeyValue(retRefNumber,
						"Entro poner hashmap si trae algo el map Key: " + info.getKey() + " Value: " + info.getValue(),
						"LOG", this.nameInterface));
				this.udpClientV2.sendData(Client.getMsgKeyValue(retRefNumber,
						"Entro poner hashmap si trae algo el map Key: " + info.getKey() + " Value: " + info.getValue(),
						"LOG", this.nameInterface));
			}
		}
	}

	public void processField(Base24Ath msg, Iso8583Post isoMsg, int numField, StructuredData sd) {
		InvokeMethodByConfig invoke = new InvokeMethodByConfig(params);
		try {
			if (GenericInterface.fillMaps.getStructuredDataFields().containsKey(String.valueOf(numField))) {
				sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
						msg.getField(numField));
			} else if (GenericInterface.fillMaps.getTransformFields().containsKey(String.valueOf(numField))) {
				if (GenericInterface.fillMaps.getTransformFieldsMultipleCases().containsKey(String.valueOf(numField))) {
					sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
							msg.getField(numField));

					String key = String.valueOf(numField) + "-" + msg.getField(3);

					// si el methodName es null, significa que no tiene m�todo para transformar por
					// lo tanto se copia el campo

					String methodName = GenericInterface.fillMaps.getTransformFields().get(key);

					String fieldValue = null;

					if (methodName != null && !methodName.equals("N/A")) {

						fieldValue = invoke.invokeMethodConfig(
								"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg,
								numField);

					} else {
						fieldValue = msg.getField(numField);
					}

					isoMsg.putField(numField, fieldValue);

				} else {
					String methodName = GenericInterface.fillMaps.getTransformFields().get(String.valueOf(numField));
					if (!methodName.equals("N/A")) {

						String fieldValue = invoke.invokeMethodConfig(
								"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg,
								numField);

						isoMsg.putField(numField, fieldValue);
						sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
								msg.getField(numField));
					}
				}

			} else if (GenericInterface.fillMaps.getSkipCopyFields().containsKey(String.valueOf(numField))) {
				isoMsg.putField(numField, msg.getField(numField));
				sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
						msg.getField(numField));
			} else {
				isoMsg.putField(numField, msg.getField(numField));
			}

		} catch (XPostilion e) {
			e.printStackTrace();
		}
	}

	public void processFieldResponse(Base24Ath msg, Iso8583Post isoMsg, int numField, StructuredData sd)
			throws XPostilion {
		InvokeMethodByConfig invoke = new InvokeMethodByConfig(params);
		String retRefNumber = "N/D";
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			if (GenericInterface.fillMaps.getStructuredDataFields().containsKey(String.valueOf(numField))) {
				sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
						msg.getField(numField));
			} else if (GenericInterface.fillMaps.getTransformFields().containsKey(String.valueOf(numField))) {
				if (GenericInterface.fillMaps.getTransformFieldsMultipleCases().containsKey(String.valueOf(numField))) {
					sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
							msg.getField(numField));

					String key = String.valueOf(numField) + "-" + msg.getField(3);

					// si el methodName es null, significa que no tiene m�todo para transformar por
					// lo tanto se copia el campo
					String methodName = GenericInterface.fillMaps.getTransformFields().get(key);

					String fieldValue = null;

					if (methodName != null && !methodName.equals("N/A")) {
						fieldValue = invoke.invokeMethodConfig(
								"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg,
								numField);
					} else {
						fieldValue = msg.getField(numField);
					}

					isoMsg.putField(numField, fieldValue);

				} else {
					String methodName = GenericInterface.fillMaps.getTransformFields().get(String.valueOf(numField));

					if (!methodName.equals("N/A")) {
						String fieldValue = invoke.invokeMethodConfig(
								"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg,
								numField);

						isoMsg.putField(numField, fieldValue);
						sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
								msg.getField(numField));
					}
				}

			} else if (GenericInterface.fillMaps.getSkipCopyFields().containsKey(String.valueOf(numField))) {
				isoMsg.putField(numField, msg.getField(numField));
				sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
						msg.getField(numField));
			} else {
				isoMsg.putField(numField, msg.getField(numField));
			}

		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"processFieldResponse", this.udpClient);
		}
	}

	public void processCreateField(Base24Ath msg, Iso8583Post isoMsg, int numField) throws XPostilion {
		InvokeMethodByConfig invoke = new InvokeMethodByConfig(params);
		String retRefNumber = "N/D";
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);

			if (GenericInterface.fillMaps.getCreateFields().containsKey(String.valueOf(numField))) {
				String methodName = GenericInterface.fillMaps.getCreateFields().get(String.valueOf(numField));

				if (!methodName.equals("N/A")) {

					String fieldValue = invoke.invokeMethodConfig(
							"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg,
							numField);
					isoMsg.putField(numField, fieldValue);

				}

			}

		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"processCreateField", this.udpClient);

		}
	}

	private Base24Ath constructFieldsFromSd(Base24Ath msg, StructuredData sd) {
		Base24Ath msgToRemote = msg;
		String retRefNumber = "N/D";
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			if (sd != null) {
				Enumeration<?> sdFields = sd.getTypeNames();
				while (sdFields.hasMoreElements()) {
					String element = sdFields.nextElement().toString();
					String[] fieldNum = element.split(Constants.Config.UNDERSCORE);
					if (element.contains(Constants.Config.TAGNAMESD)
							&& !element.contains(Constants.Config.TAGNAMESD + Constants.General.NUMBER_128)) {
						msgToRemote.putField(Integer.parseInt(fieldNum[2]), sd.get(element));
					}
				}
			}
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"constructFieldsFromSd", this.udpClient, "of type XPostilion");
		} catch (Exception e1) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e1, retRefNumber,
					"constructFieldsFromSd", this.udpClient, "of type Exception");
		}

		return msgToRemote;
	}

	/**
	 * Construye el Header de un mensaje que va a ser enviado a la Interchange desde
	 * el Nodo Source.
	 *
	 * @param msgFromRemote Mensaje desde ATH.
	 * @return Objeto Header.
	 */
	private Header constructAtmHeaderSourceNode(Base24Ath msgFromRemote) {
		String retRefNumber = "N/D";
		try {
			retRefNumber = msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			Header atmHeader = new Header(msgFromRemote.getHeader());
			atmHeader.putField(Header.Field.ISO_LITERAL, Header.Iso.ISO);
			atmHeader.putField(Header.Field.RESPONDER_CODE, Header.SystemCode.HOST);
			atmHeader.putField(Header.Field.PRODUCT_INDICATOR, Header.ProductIndicator.ATM);
			atmHeader.putField(Header.Field.RELEASE_NUMBER, Base24Ath.Version.REL_NR_40);
			atmHeader.putField(Header.Field.STATUS, Header.Status.OK);
			atmHeader.putField(Header.Field.ORIGINATOR_CODE, Header.OriginatorCode.CINCO);
			return atmHeader;
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"constructAtmHeaderSourceNode", this.udpClient, "of type XPostilion");
		} catch (Exception e1) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e1, retRefNumber,
					"constructAtmHeaderSourceNode", this.udpClient, "of type Exception");
		}
		return null;
	}

	/**
	 * Construye el Header de un mensaje que va a ser enviado a la Interchange desde
	 * el Nodo Source.
	 *
	 * @param msgFromRemote Mensaje desde ATH.
	 * @return Objeto Header.
	 */
	private Header constructAtmHeaderSinkNode(Iso8583Post msg) {
		String retRefNumber = "N/D";
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			Header atmHeader = new Header();
			atmHeader.putField(Header.Field.ISO_LITERAL, Header.Iso.ISO);
			atmHeader.putField(Header.Field.RESPONDER_CODE, Header.SystemCode.UNDETERMINED);
			if (msg.getStructuredData().get("indicator_product") != null
					&& msg.getStructuredData().get("indicator_product").equals("1"))
				atmHeader.putField(Header.Field.PRODUCT_INDICATOR, Header.ProductIndicator.ATM);
			else if (msg.getStructuredData().get("indicator_product") != null
					&& msg.getStructuredData().get("indicator_product").equals("2"))
				atmHeader.putField(Header.Field.PRODUCT_INDICATOR, Header.ProductIndicator.POS);
			else
				atmHeader.putField(Header.Field.PRODUCT_INDICATOR, Header.ProductIndicator.POS);
			atmHeader.putField(Header.Field.RELEASE_NUMBER, Base24Ath.Version.REL_NR_34);
			atmHeader.putField(Header.Field.STATUS, Header.Status.OK);
			atmHeader.putField(Header.Field.ORIGINATOR_CODE, Header.OriginatorCode.CINCO);
			return atmHeader;
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"constructAtmHeaderSourceNode", this.udpClient, "of type XPostilion");
		} catch (Exception e1) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e1, retRefNumber,
					"constructAtmHeaderSourceNode", this.udpClient, "of type Exception");
		}
		return null;
	}

	/**
	 * Method to construct Iso8583Post error message for to send to TM
	 * 
	 * @param msg     - Message from Remote in Base24
	 * @param keyHash - Configuration key for get message configuration
	 * @param error   - Error information
	 * @return - 0220 Message in format Iso8583Post
	 * @throws XPostilion
	 */
	public Iso8583Post construct0220ToTm(Iso8583 msg, String nameInterchange) throws XPostilion {

		tStart = System.currentTimeMillis();

		StructuredData sd = new StructuredData();
		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"ENTRO A  construct0220ToTm", "LOG", this.nameInterface));

		Iso8583Post msgToTm = new Iso8583Post();
		msgToTm.putMsgType(Iso8583.MsgType._0220_TRAN_ADV);
		// this.structureMap = this.structureContent.get(keyHash.toString());
		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"structureMap:" + this.structureMap, "LOG", this.nameInterface));
		try {

			ConstructFieldMessage cfm = new ConstructFieldMessage(params);

			// Crea los campos
			for (String key : GenericInterface.fillMaps.getCreateFields220ToTM().keySet()) {

				int intKey = Integer.parseInt(key);
				switch (intKey) {
				case 15:
					msgToTm.putField(intKey, cfm.compensationDateValidationP17ToP15(msg, intKey));
					break;
				case 98:
					msgToTm.putField(intKey, cfm.constructField98(msg, intKey));
					break;
				default:
					msgToTm.putField(intKey, cfm.construct0220ErrorFields(msg, intKey));
					break;
				}

			}

			msgToTm.putPrivField(Iso8583Post.PrivBit._002_SWITCH_KEY,
					new ConstructFieldMessage(this.params).constructSwitchKey(msgToTm, nameInterchange));
			GenericInterface.getLogger().logLine("Exception message:" + GenericInterface.exceptionMessage);

			sd.put("ERROR_MESSAGE:", GenericInterface.exceptionMessage);
			msgToTm.putStructuredData(sd);

			// msg.putField(Iso8583.Bit._039_RSP_CODE, error.getErrorCodeISO());

		} catch (XPostilion e1) {
			e1.printStackTrace();
		}

		return msgToTm;
	}

	public void constructAutra0800Message(Base24Ath msg, Iso8583Post msgToTM) throws XPostilion {
		msgToTM.putMsgType(Iso8583.MsgType._0800_NWRK_MNG_REQ);

		if (msg.isFieldSet(Iso8583.Bit._007_TRANSMISSION_DATE_TIME))
			msgToTM.putField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME,
					msg.getField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME).toString());

		if (msg.isFieldSet(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR))
			msgToTM.putField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR,
					msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR).toString());

		if (msg.isFieldSet(Iso8583.Bit._012_TIME_LOCAL)) {
			msgToTM.putField(Iso8583.Bit._012_TIME_LOCAL, msg.getField(Iso8583.Bit._012_TIME_LOCAL).toString());
		} else {
			msgToTM.putField(Iso8583.Bit._012_TIME_LOCAL, new DateTime().get(FormatDate.HHMMSS));
		}

		if (msg.isFieldSet(Iso8583.Bit._013_DATE_LOCAL)) {
			msgToTM.putField(Iso8583.Bit._013_DATE_LOCAL, msg.getField(Iso8583.Bit._013_DATE_LOCAL).toString());
		} else {
			msgToTM.putField(Iso8583.Bit._013_DATE_LOCAL, new DateTime().get(FormatDate.MMDD));

		}

		if (msg.isFieldSet(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE))
			msgToTM.putField(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE,
					msg.getField(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE).toString());

		msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "40");

//		msgToTM.putField(Iso8583.Bit._025_POS_CONDITION_CODE, Iso8583.PosCondCode._00_NORMAL_PRESENTMENT);
//		msgToTM.putField(Iso8583.Bit._026_POS_PIN_CAPTURE_CODE, PosPinCaptureCode.FOUR);
//		msgToTM.putField(Iso8583Post.Bit._123_POS_DATA_CODE, General.POSDATACODE);
//		msgToTM.putField(Iso8583.Bit._098_PAYEE, "0054150070650000000000000");

		String OriginalInput = new String(msg.toMsg(false));
		String encodedString = Base64.getEncoder().encodeToString(OriginalInput.getBytes());

		GenericInterface.getLogger().logLine("Original Input B24 : " + OriginalInput);
		GenericInterface.getLogger().logLine("Encoded Input B24 : " + encodedString);

		StructuredData sd = new StructuredData();
		sd.put("B24_Message", encodedString);
		msgToTM.putStructuredData(sd);
		msgToTM.putPrivField(Iso8583Post.PrivBit._002_SWITCH_KEY,
				new ConstructFieldMessage(this.params).constructSwitchKey(msg, "ATM"));
	}

	public void constructAutra0810ResponseMessage(Base24Ath msg, Iso8583Post msgToTM) throws XPostilion {

		msgToTM.putMsgType(Iso8583.MsgType._0810_NWRK_MNG_REQ_RSP);

		if (msg.isFieldSet(Iso8583.Bit._007_TRANSMISSION_DATE_TIME))
			msgToTM.putField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME,
					msg.getField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME).toString());

		if (msg.isFieldSet(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR))
			msgToTM.putField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR,
					msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR).toString());

		if (msg.isFieldSet(Iso8583.Bit._012_TIME_LOCAL))
			msgToTM.putField(Iso8583.Bit._012_TIME_LOCAL, msg.getField(Iso8583.Bit._012_TIME_LOCAL).toString());

		if (msg.isFieldSet(Iso8583.Bit._013_DATE_LOCAL))
			msgToTM.putField(Iso8583.Bit._013_DATE_LOCAL, msg.getField(Iso8583.Bit._013_DATE_LOCAL).toString());

		if (msg.isFieldSet(Iso8583.Bit._039_RSP_CODE))
			msgToTM.putField(Iso8583.Bit._039_RSP_CODE, msg.getField(Iso8583.Bit._039_RSP_CODE).toString());

		if (msg.isFieldSet(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE))
			msgToTM.putField(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE,
					msg.getField(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE).toString());

		msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "40");

//		msgToTM.putField(Iso8583.Bit._025_POS_CONDITION_CODE, Iso8583.PosCondCode._00_NORMAL_PRESENTMENT);
//		msgToTM.putField(Iso8583.Bit._026_POS_PIN_CAPTURE_CODE, PosPinCaptureCode.FOUR);
//		msgToTM.putField(Iso8583Post.Bit._123_POS_DATA_CODE, General.POSDATACODE);
//		msgToTM.putField(Iso8583.Bit._098_PAYEE, "0054150070650000000000000");

		String OriginalInput = new String(msg.toMsg(false));
		String encodedString = Base64.getEncoder().encodeToString(OriginalInput.getBytes());

		StructuredData sd = msgToTM.getStructuredData();
		sd.put("B24_MessageRsp", encodedString);
		msgToTM.putStructuredData(sd);
	}

	public Base24Ath constructBase24(Base24Ath msg, Super error) throws XPostilion {
		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"ENTRO AL constructMsgRspToRem0210DeclinedRegExBussines ESTA COLOCANDO ESTO EN EL sWITCH "
						+ msg.getResponseMessageType(),
				"LOG", this.nameInterface));
		Base24Ath msgToRem = new Base24Ath(this.kwa);
		String resposeMessageType = msg.getResponseMessageType();

		ResponseCode responseCode;
		try {
			responseCode = InitialLoadFilter.getFilterCodeIsoToB24(error.getErrorCodeISO(), allCodesIsoToB24TM);
		} catch (NoSuchElementException e) {
			if (new DBHandler(this.params).updateResgistry(error.getErrorCodeISO(), "1", responseCodesVersion)) {
				try {
					allCodesIsoToB24TM = postilion.realtime.library.common.db.DBHandler.getResponseCodes(false, "1",
							responseCodesVersion);
				} catch (SQLException e1) {
//					EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e1,
//							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructBase24", this.udpClient);
				}
				responseCode = InitialLoadFilter.getFilterCodeIsoToB24(error.getErrorCodeISO(), allCodesIsoToB24TM);
			} else {
				responseCode = new ResponseCode("10002", "Error Code could not extracted from message",
						error.getErrorCodeISO(), error.getErrorCodeISO(), "10002");
//				EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e,
//						msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructBase24", this.udpClient);
			}
		}

		Map<String, String> copyFieldsResponse = null;
		Map<String, String> deleteFieldsResponse = null;
		Map<String, String> createFieldsResponse = null;
		Map<String, String> transformFieldsResponse = null;
		String pCode126 = null;
		InvokeMethodByConfig invoke = new InvokeMethodByConfig(params);
		switch (resposeMessageType) {
		case "0210":
			copyFieldsResponse = GenericInterface.fillMaps.getCopyFieldsResponse();
			deleteFieldsResponse = GenericInterface.fillMaps.getDeleteFieldsResponse();
			createFieldsResponse = GenericInterface.fillMaps.getCreateFieldsResponse();
			transformFieldsResponse = GenericInterface.fillMaps.getTransformFieldsResponse();
			pCode126 = msg.getField(126) != null ? msg.getField(126).substring(22, 28) : null;

			msgToRem.putMsgType(Iso8583.MsgType._0210_TRAN_REQ_RSP);

			msgToRem.putField(Base24Ath.Bit.ENTITY_ERROR,
					Pack.resize(new StringBuilder().append(responseCode.getKeyIsc())
							// .append(responseCode.getDescriptionIsc().trim())
							.append(error.getDescriptionError()).toString(), General.LENGTH_44, General.SPACE, true));

			break;
		case "0230":
			copyFieldsResponse = GenericInterface.fillMaps.getCopyFieldsResponseAdv();
			deleteFieldsResponse = GenericInterface.fillMaps.getDeleteFieldsResponseAdv();
			createFieldsResponse = GenericInterface.fillMaps.getCreateFieldsResponseAdv();
			transformFieldsResponse = GenericInterface.fillMaps.getTransformFieldsResponseAdv();

			msgToRem.putMsgType(Iso8583.MsgType._0230_TRAN_ADV_RSP);

			msgToRem.putField(Base24Ath.Bit.ENTITY_ERROR,
					Pack.resize(new StringBuilder().append(responseCode.getKeyIsc())
							// .append(responseCode.getDescriptionIsc().trim())
							.append(error.getDescriptionError()).toString(), General.LENGTH_44, General.SPACE, true));

			break;

		case "0430":
			copyFieldsResponse = GenericInterface.fillMaps.getCopyFieldsResponseRev();
			deleteFieldsResponse = GenericInterface.fillMaps.getDeleteFieldsResponseRev();
			createFieldsResponse = GenericInterface.fillMaps.getCreateFieldsResponseRev();
			transformFieldsResponse = GenericInterface.fillMaps.getTransformFieldsResponseRev();
			msgToRem.putMsgType(Iso8583.MsgType._0430_ACQUIRER_REV_ADV_RSP);
			break;
		default:

			break;
		}

		// Copia los campos en el mensaje B24
		for (String key : copyFieldsResponse.keySet()) {

			int intKey = Integer.parseInt(key);
			if (msg.isFieldSet(intKey)) {
				msgToRem.putField(intKey, msg.getField(intKey));
			}

		}

		// SKIP-TRANSFORM y TRANSFORM

		for (int i = 3; i < 127; i++) {

			String key1 = String.valueOf(i) + "-" + msg.getField(Iso8583.Bit._003_PROCESSING_CODE) + "_" + pCode126;
			String key2 = String.valueOf(i) + "-" + msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
			String key3 = String.valueOf(i);

			String methodName = null;

			if (createFieldsResponse.containsKey(key1)) {
				methodName = createFieldsResponse.get(key1);
				if (!methodName.equals("N/A"))
					msgToRem.putField(i, invoke.invokeMethodConfig(
							"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg, i));

			} else if (createFieldsResponse.containsKey(key2)) {
				methodName = createFieldsResponse.get(key2);
				if (!methodName.equals("N/A"))
					msgToRem.putField(i, invoke.invokeMethodConfig(
							"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg, i));
			} else if (createFieldsResponse.containsKey(key3)) {
				methodName = createFieldsResponse.get(key3);
				if (!methodName.equals("N/A"))
					msgToRem.putField(i, invoke.invokeMethodConfig(
							"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg, i));
			}
			if (transformFieldsResponse.containsKey(key1)) {
				methodName = transformFieldsResponse.get(key1);
				if (!methodName.equals("N/A"))
					msgToRem.putField(i, invoke.invokeMethodConfig(
							"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg, i));

			} else if (transformFieldsResponse.containsKey(key2)) {
				methodName = transformFieldsResponse.get(key2);
				if (!methodName.equals("N/A"))
					msgToRem.putField(i, invoke.invokeMethodConfig(
							"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg, i));
			} else if (transformFieldsResponse.containsKey(key3)) {
				methodName = transformFieldsResponse.get(key3);
				if (!methodName.equals("N/A"))
					msgToRem.putField(i, invoke.invokeMethodConfig(
							"postilion.realtime.genericinterface.translate.ConstructFieldMessage", methodName, msg, i));
			}
		}

		// Busca si hay que eliminar campos dado el processingCode

		String PCode = msg.getField(Iso8583.Bit._003_PROCESSING_CODE);

		Set<String> set = deleteFieldsResponse.keySet().stream().filter(s -> s.length() <= 3)
				.collect(Collectors.toSet());

		if (set.size() > 0) {
			for (String item : set) {
				if (msgToRem.isFieldSet(Integer.parseInt(item))) {
					msgToRem.clearField(Integer.parseInt(item));
				}
			}
		}

		if (deleteFieldsResponse.containsKey(PCode)) {
			String[] parts = deleteFieldsResponse.get(PCode).split("-");
			for (String item : parts) {
				if (msgToRem.isFieldSet(Integer.parseInt(item))) {
					msgToRem.clearField(Integer.parseInt(item));
				}
			}
		}

		try {
			msgToRem.putHeader(constructAtmHeaderSourceNode(msg));

			try {
				responseCode = InitialLoadFilter.getFilterCodeIsoToB24(error.getErrorCodeISO(),
						this.allCodesIsoToB24TM);
			} catch (NoSuchElementException e) {
				if (new DBHandler(this.params).updateResgistry(error.getErrorCodeISO(), "1", responseCodesVersion)) {
					this.allCodesIsoToB24TM = postilion.realtime.library.common.db.DBHandler.getResponseCodes(false,
							"1", responseCodesVersion);
					responseCode = InitialLoadFilter.getFilterCodeIsoToB24(error.getErrorCodeISO(),
							this.allCodesIsoToB24TM);
				} else {
					responseCode = new ResponseCode("10002", "Error Code could not extracted from message",
							error.getErrorCodeISO(), error.getErrorCodeISO(), "10002");
//					EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e,
//							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructBase24", this.udpClient);
				}
			}
			msgToRem.putField(Base24Ath.Bit.ENTITY_ERROR,
					Pack.resize(new StringBuilder().append(responseCode.getKeyIsc())
							// .append(responseCode.getDescriptionIsc().trim())
							.append(error.getDescriptionError()).toString(), General.LENGTH_44, General.SPACE, true));

			msgToRem.putField(Iso8583.Bit._039_RSP_CODE, error.getErrorCodeISO());
			msgToRem.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID,
					msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID));
		} catch (Exception e) {
			msgToRem.putField(Base24Ath.Bit.ENTITY_ERROR, Pack.resize(
					"10002" + "Error Code could not extracted from message", General.LENGTH_44, General.SPACE, true));
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructBase24", this.udpClient);

		}
		return msgToRem;
	}

}