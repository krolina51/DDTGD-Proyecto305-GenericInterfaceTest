package postilion.realtime.genericinterface.translate;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Base64;
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
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.database.DBHandler;
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
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
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
 * @author Albert Medina y Fernando Castañeda
 */

public class MessageTranslator {

	private DesKwa kwa;
	private TimedHashtable sourceTranToTmHashtable = null;
	private Map<String, ConfigAllTransaction> structureMap = new HashMap<>();
	private Map<String, ResponseCode> allCodesIsoToB24TM = new HashMap<>();
	private Client udpClient = null;
	private String nameInterface = "";
	protected String responseCodesVersion = "1";
	private Parameters params;

	public MessageTranslator() {

	}

	public MessageTranslator(Parameters params) {
		this.kwa = params.getKwa();
		this.sourceTranToTmHashtable = params.getSourceTranToTmHashtable();
		this.udpClient = params.getUdpClient();
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

			msgToRmto.putHeader(constructAtmHeaderSourceNode(msgToRmto));
			msgToRmto.putMsgType(msg.getMsgType());

			StructuredData sd = new StructuredData();
			sd = msg.getStructuredData();
			String pCode126 = sd.get("B24_Field_126") != null ? sd.get("B24_Field_126").substring(22, 28) : null;
			for (int i = 3; i <= 126; i++) {
				if (sd != null && sd.get("B24_Field_" + String.valueOf(i)) != null)
					msgToRmto.putField(i, sd.get("B24_Field_" + String.valueOf(i)));
				else if (msg.isFieldSet(i))
					msgToRmto.putField(i, msg.getField(i));

				String key1 = String.valueOf(i) + "-" + msg.getField(Iso8583.Bit._003_PROCESSING_CODE) + "_" + pCode126;
				String key2 = String.valueOf(i) + "-" + msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
				String key3 = String.valueOf(i);

				String methodName = null;

				if (GenericInterface.fillMaps.getCreateFieldsRequest().containsKey(key1)) {
					methodName = GenericInterface.fillMaps.getCreateFieldsRequest().get(key1);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));

				} else if (GenericInterface.fillMaps.getCreateFieldsRequest().containsKey(key2)) {
					methodName = GenericInterface.fillMaps.getCreateFieldsRequest().get(key2);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));
				} else if (GenericInterface.fillMaps.getCreateFieldsRequest().containsKey(key3)) {
					methodName = GenericInterface.fillMaps.getCreateFieldsRequest().get(key3);
					if (!methodName.equals("N/A"))
						msgToRmto.putField(i,
								invoke.invokeMethodConfig(
										"postilion.realtime.genericinterface.translate.ConstructFieldMessage",
										methodName, msg, i));
				}

			}

			String PCode = msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
			Set<String> set = GenericInterface.fillMaps.getDeleteFieldsRequest().keySet().stream()
					.filter(s -> s.length() <= 3).collect(Collectors.toSet());

			if (set.size() > 0) {
				for (String item : set) {
					if (msgToRmto.isFieldSet(Integer.parseInt(item))) {
						msgToRmto.clearField(Integer.parseInt(item));
					}
				}
			}

			if (GenericInterface.fillMaps.getDeleteFieldsRequest().containsKey(PCode)) {
				String[] parts = GenericInterface.fillMaps.getDeleteFieldsRequest().get(PCode).split("-");
				for (String item : parts) {
					if (msgToRmto.isFieldSet(Integer.parseInt(item))) {
						msgToRmto.clearField(Integer.parseInt(item));
					}
				}
			}

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
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			msgToRmto.putHeader(constructAtmHeaderSourceNode(msgToRmto));
			msgToRmto.putMsgType(msg.getMsgType());
			Iso8583Post msgOriginal = null;
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

		} catch (XPostilion e) {
			msgToRmto = null;
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"constructBase24", this.udpClient, "of type XPostilion");
		} catch (Exception e1) {
			msgToRmto = null;
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e1, retRefNumber,
					"constructBase24", this.udpClient, "of type Exception");
		}

		GenericInterface.getLogger().logLine("constructBase24:  return " + msgToRmto);

		return msgToRmto;
	}

	// public Iso8583Post constructIso8583(Base24Ath msgFromRemote, HashMap<String,
	// String> returnInfoValidations) {

	public Iso8583Post constructIso8583(Base24Ath msgFromRemote, Super objectValidations) {
		Iso8583Post Iso = new Iso8583Post();
		tStart = System.currentTimeMillis();
		String strTypeMsg = msgFromRemote.getMessageType();
		String retRefNumber = "N/D";

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

			String channel = BussinesRules.channelIdentifier(msgFromRemote, this.nameInterface, this.udpClient);

			switch (channel) {

			case Constants.Channels.OFCAVAL:

				switch (msgFromRemote.getProcessingCode().toString()) {

				case Constants.Channels.PCODE_RETIRO_ATM_A:
				case Constants.Channels.PCODE_RETIRO_ATM_C:

					sd.put("CHANNEL_TX", channel);
					SettlementDate date = new SettlementDate(this.params.getCalendarInfo().getCalendar());
					date.calculateDate((Iso8583) msgFromRemote);
					sd.put("CURRENT_TX", !date.isNextDay() ? "S" : "N");

					// sd.put("CHANNEL", Constants.DefaultATM.ATM_CHANNEL_NAME);
					// sd.put("Identificacion_Canal", Constants.DefaultATM.ATM_ID_CHANNEL);
					// sd.put("Canal", Constants.DefaultATM.ATM_CHANNEL);
					// sd.put("VIEW_ROUTER", Constants.DefaultATM.ATM_VIEW_ROUTER);
					sd.put("VIEW_ROUTER", Constants.DefaultOficinasAVAL.OFC_VIEW_ROUTER);
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

					fillAccount(retRefNumber, msgFromRemote, objectValidations, new DBHandler(this.params));

					sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append("103").toString(),
							Constants.Account.ACCOUNT_DEFAULT);

					if (strTypeMsg.equals("0420")) {
						sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append("104").toString(),
								Constants.Account.ACCOUNT_DEFAULT);
					}

					if (msgFromRemote.getMessageType().equals(Iso8583.MsgTypeStr._0200_TRAN_REQ)) {
						// Se invoca al metodo getTransactionConsecutive a fin de obtener el consecutivo
						// para la transaación
						String cons = constructor.getTransactionConsecutive(
								msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR).substring(5, 9), "00",
								this.params.getTermConsecutiveSection());
						sd.put("REFERENCE_KEY", msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR).concat("|")
								.concat(cons.split(",")[0].trim().concat(cons.split(",")[1].trim())));
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

				default:
					break;

				}
			case Constants.Channels.OFC:

				Super account = new Super(true, General.VOIDSTRING, General.VOIDSTRING, General.VOIDSTRING,
						new HashMap<String, String>(), params) {

					@Override
					public void validations(Base24Ath msg, Super objectValidations) {

					}
				};

				switch (msgFromRemote.getProcessingCode().toString()) {

				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_CORRIENTE:

					switch (msgFromRemote.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {

					case "051":
						// DEBITO
						if (msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3).equals("0")) {

							fillBasicSDtagsByPayment(sd, msgFromRemote, retRefNumber, account, true);

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
							fillBasicSDtagsByPayment(sd, msgFromRemote, retRefNumber, account, false);
							sd.put("DEBIT_ACCOUNT_NR", "000000000000");
						}

						// MIXTA
						if (msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3).equals("2")) {
							fillBasicSDtagsByPayment(sd, msgFromRemote, retRefNumber, account, true);
							sd.put("CREDIT_ACCOUNT_TYPE", "2");
							sd.put("CREDIT_ACCOUNT_NR",
									msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
						}

						break;

					default:
						break;

					}

				default:
					break;
				}

			default:
				break;
			}

			if (msgFromRemote.getMessageType().equals(Iso8583.MsgTypeStr._0220_TRAN_ADV)) {
				Iso.putField(Iso8583Post.Bit._100_RECEIVING_INST_ID_CODE,
						constructor.constructField100(msgFromRemote, Iso8583Post.Bit._100_RECEIVING_INST_ID_CODE));
			}

			fillStructuredData(retRefNumber, sd, objectValidations);

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

	public void fillBasicSDtagsByPayment(StructuredData sd, Base24Ath msgFromRemote, String retRefNumber, Super account,
			boolean getAccount) {
		sd.put("TRANSACTION_INPUT", Constants.DefaultOficinasAVAL.OFC_TRANSACTION_INPUT);
		sd.put("VIEW_ROUTER", Constants.DefaultOficinasAVAL.OFC_VIEW_ROUTER);
		sd.put("Mod_Credito", "3");
		sd.put("Mod_CreditoX1", "3");
		sd.put("TRANSACTION_CNB_TYPE", "OFC_POBLIG_" + "OTROS_CREDITOS");
		try {
			sd.put("B24_Field_41", msgFromRemote.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID));
			sd.put("B24_Field_102", msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
			sd.put("B24_Field_103", msgFromRemote.getField(Iso8583.Bit._103_ACCOUNT_ID_2));
			sd.put("P_CODE", msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE));
			if (getAccount) {
				fillAccount(retRefNumber, msgFromRemote, account, new DBHandler(this.params));
				sd.put("DEBIT_ACCOUNT_NR",
						account.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
				sd.put("DEBIT_ACCOUNT_TYPE",
						account.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));							
				sd.put("DEBIT_CARD_CLASS",
						account.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
			}
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"getAccount", this.udpClient, "of type XPostilion");
		}
	}

	public void fillAccount(String retRefNumber, Base24Ath msgFromRemote, Super objectValidations, DBHandler account) {
		try {
			account.accountsClienteCNB(retRefNumber, msgFromRemote.getProcessingCode().toString(),
					msgFromRemote.getTrack2Data().getPan(), msgFromRemote.getProcessingCode().getFromAccount(),
					msgFromRemote.getTrack2Data().getExpiryDate(),
					msgFromRemote.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? msgFromRemote.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							: "0000000",
					objectValidations);

		} catch (XFieldUnableToConstruct e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"getAccount", this.udpClient, "of type XFieldUnableToConstruct");
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"getAccount", this.udpClient, "of type XPostilion");
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e, retRefNumber,
					"getAccount", this.udpClient, "of type Exception");
		}
	}

	public void fillStructuredData(String retRefNumber, StructuredData sd, Super objectValidations) {

		HashMap<String, String> inforCollectedForStructData = objectValidations.getInforCollectedForStructData();

		if (!(inforCollectedForStructData == null)) {
			this.udpClient.sendData(Client.getMsgKeyValue(retRefNumber,
					"Entro poner hashmap si trae algo el map" + objectValidations.toString(), "LOG",
					this.nameInterface));
			for (Map.Entry<String, String> info : inforCollectedForStructData.entrySet()) {
				sd.put(info.getKey(), info.getValue());
				this.udpClient.sendData(Client.getMsgKeyValue(retRefNumber,
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

					// si el methodName es null, significa que no tiene método para transformar por
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

					// si el methodName es null, significa que no tiene método para transformar por
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
			GenericInterface.getLogger().logLine("EXception message:" + GenericInterface.exceptionMessage);

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
					EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e1,
							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructBase24", this.udpClient);
				}
				responseCode = InitialLoadFilter.getFilterCodeIsoToB24(error.getErrorCodeISO(), allCodesIsoToB24TM);
			} else {
				responseCode = new ResponseCode("10002", "Error Code could not extracted from message",
						error.getErrorCodeISO(), error.getErrorCodeISO(), "10002");
				EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e,
						msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructBase24", this.udpClient);
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
					EventReporter.reportGeneralEvent(this.nameInterface, MessageTranslator.class.getName(), e,
							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructBase24", this.udpClient);
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