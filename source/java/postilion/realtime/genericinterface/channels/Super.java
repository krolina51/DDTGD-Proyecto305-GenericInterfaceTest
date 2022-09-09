package postilion.realtime.genericinterface.channels;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;

import postilion.realtime.genericinterface.GenericInterface;
import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.eventrecorder.events.SQLExceptionEvent;
import postilion.realtime.genericinterface.translate.ConstructFieldMessage;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.database.DBHandler.Account;
import postilion.realtime.genericinterface.translate.database.DBHandler.ColumnNames;
import postilion.realtime.genericinterface.translate.database.DBHandler.StoreProcedures;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.EventReporter;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.genericinterface.translate.util.Constants.FormatDate;
import postilion.realtime.genericinterface.translate.util.Constants.General;
import postilion.realtime.library.common.util.constants.PosPinCaptureCode;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.ipc.SecurityManager;
import postilion.realtime.sdk.jdbc.JdbcManager;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.Iso8583.RspCode;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.message.bitmap.ProcessingCode;
import postilion.realtime.sdk.message.bitmap.StructuredData;
import postilion.realtime.sdk.message.bitmap.XBitmapUnableToConstruct;
import postilion.realtime.sdk.message.bitmap.XFieldUnableToConstruct;
import postilion.realtime.sdk.util.DateTime;
import postilion.realtime.sdk.util.XPostilion;
import postilion.realtime.sdk.util.convert.Pack;
import postilion.realtime.sdk.util.convert.Transform;

public abstract class Super {

	private Boolean validationResult = true;
	private String descriptionError = General.VOIDSTRING;
	private String errorCodeAUTRA = General.VOIDSTRING;
	private String errorCodeISO = General.VOIDSTRING;
	private HashMap<String, String> inforCollectedForStructData = new HashMap<String, String>();
	private String issuerId = null;
	private Client udpClient = null;
	private String nameInterface = "";
	private boolean encodeData = false;
	private boolean exeptionValidateExpiryDate = false;
	private Parameters params;

	private static SecurityManager securityManager = null;

	static {
		try {
			securityManager = new SecurityManager();
		} catch (Exception e) {
			GenericInterface.getLogger().logLine(e.getMessage());
			System.exit(1);
		}
	}

	public Super(Parameters params) {
		this.issuerId = params.getIssuerId();
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
		this.encodeData = params.isEncodeData();
		this.exeptionValidateExpiryDate = params.isExeptionValidateExpiryDate();
		this.params = params;
	}

	public Super(Boolean validationResult, String descriptionError, String errorCodeAUTRA, String errorCodeISO,
			HashMap<String, String> inforCollectedForStructData, Parameters params) {
		this.validationResult = validationResult;
		this.descriptionError = descriptionError;
		this.errorCodeAUTRA = errorCodeAUTRA;
		this.errorCodeISO = errorCodeISO;
		this.inforCollectedForStructData = inforCollectedForStructData;
		this.issuerId = params.getIssuerId();
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
		this.encodeData = params.isEncodeData();
		this.exeptionValidateExpiryDate = params.isExeptionValidateExpiryDate();
		this.params = params;
	}

	public void putInforCollectedForStructData(String key, String value) {
		inforCollectedForStructData.put(key, value);

	}

	public void modifyAttributes(Boolean validationResult, String descriptionError, String errorCodeAUTRA,
			String errorCodeISO) {

		setValidationResult(validationResult);
		setDescriptionError(descriptionError);
		setErrorCodeAUTRA(errorCodeAUTRA);
		setErrorCodeISO(errorCodeISO);
	}

	public Boolean getValidationResult() {
		return validationResult;
	}

	public void setValidationResult(Boolean validationResult) {
		this.validationResult = validationResult;
	}

	public String getDescriptionError() {
		return descriptionError;
	}

	public void setDescriptionError(String descriptionError) {
		this.descriptionError = descriptionError;
	}

	public String getErrorCodeAUTRA() {
		return errorCodeAUTRA;
	}

	public void setErrorCodeAUTRA(String errorCodeAUTRA) {
		this.errorCodeAUTRA = errorCodeAUTRA;
	}

	public String getErrorCodeISO() {
		return errorCodeISO;
	}

	public void setErrorCodeISO(String errorCodeISO) {
		this.errorCodeISO = errorCodeISO;
	}

	public HashMap<String, String> getInforCollectedForStructData() {
		return inforCollectedForStructData;
	}

	public void setInforCollectedForStructData(HashMap<String, String> inforCollectedForStructData) {
		this.inforCollectedForStructData = inforCollectedForStructData;
	}

	/**
	 * 
	 * Method to evaluate the business validations of the message
	 * 
	 * @param msg - Message received from Remote
	 * @return - Object with status and error message
	 * @throws Exception
	 */
	/*
	 * public Super businessValidation(Base24Ath msg, Super objectValidations)
	 * throws Exception {
	 * 
	 * switch (channelIdentifier(msg)) { case Constants.Channels.CNB:
	 * 
	 * this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit.
	 * _037_RETRIEVAL_REF_NR),
	 * "******mmmmmm*****mm*** Entro validacion CNB con este msgType: " +
	 * String.valueOf(msg.getMsgType()), "LOG", this.nameInterface));
	 * 
	 * objectValidations = new CNB(true, General.VOIDSTRING, General.VOIDSTRING,
	 * General.VOIDSTRING, new HashMap<String, String>(), this.params);
	 * objectValidations.validations(msg, objectValidations);
	 * 
	 * switch (msg.getProcessingCode().toString()) { case
	 * Constants.Channels.PCODE_CONSULTA_DE_COSTO_CNB: case
	 * Constants.Channels.PCODE_CONSULTA_DE_SALDO_Y_CUPO_CNB_A: case
	 * Constants.Channels.PCODE_CONSULTA_DE_SALDO_Y_CUPO_CNB_C: case
	 * Constants.Channels.PCODE_CONSULTA_DE_SALDO_Y_CUPO2_CNB_A: case
	 * Constants.Channels.PCODE_CONSULTA_DE_SALDO_Y_CUPO2_CNB_C:
	 * 
	 * objectValidations.putInforCollectedForStructData("A9B1", "I");
	 * 
	 * break;
	 * 
	 * default: objectValidations.putInforCollectedForStructData("A9B1", "M");
	 * break;
	 * 
	 * }
	 * 
	 * return objectValidations;
	 */

	/*
	 * case Constants.Channels.ATM:
	 * 
	 * this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit.
	 * _037_RETRIEVAL_REF_NR),
	 * "******mmmmmm*****mm*** Entro validacion ATM con este msgType: " +
	 * String.valueOf(msg.getMsgType()), "LOG", this.nameInterface));
	 * 
	 * objectValidations = new ATM(true, General.VOIDSTRING, General.VOIDSTRING,
	 * General.VOIDSTRING, new HashMap<String, String>(), this.params);
	 * objectValidations.validations(msg, objectValidations);
	 * 
	 * return objectValidations;
	 */

	/*
	 * case Constants.Channels.VTL:
	 * 
	 * objectValidations = new VTL(true, General.VOIDSTRING, General.VOIDSTRING,
	 * General.VOIDSTRING, new HashMap<String, String>(), this.params);
	 * objectValidations.validations(msg, objectValidations);
	 * 
	 * this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit.
	 * _037_RETRIEVAL_REF_NR),
	 * "******mmmmmm*****mm*** Entro validacion VTL con este msgType: " +
	 * String.valueOf(msg.getMsgType()), "LOG", this.nameInterface)); return
	 * objectValidations;
	 */

	/*
	 * default:
	 * this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit.
	 * _037_RETRIEVAL_REF_NR),
	 * "******mmmmmm*****mm*** Entro validacion CBCO con este msgType: " +
	 * String.valueOf(msg.getMsgType()), "LOG", this.nameInterface));
	 * 
	 * objectValidations = new CBCO(true, General.VOIDSTRING, General.VOIDSTRING,
	 * General.VOIDSTRING, new HashMap<String, String>(), this.params);
	 * objectValidations.validations(msg, objectValidations);
	 * 
	 * return objectValidations; }
	 * 
	 * }
	 */

	public boolean validationexpiryDate(String expDate, Super objectValidations) throws XFieldUnableToConstruct {
		int year = Integer.parseInt(expDate.substring(0, 2));
		int cYear = Integer.parseInt(String.valueOf(LocalDate.now().getYear()).substring(2));
		int month = Integer.parseInt(expDate.substring(2));

		if (!this.exeptionValidateExpiryDate) {

			GenericInterface.getLogger().logLine("year: " + year);
			GenericInterface.getLogger().logLine("cYear: " + cYear);
			GenericInterface.getLogger().logLine("month: " + month);

			if ((year > cYear) || (month >= LocalDate.now().getMonthValue() && year == cYear)
					|| (month == 12 && (year == 20 || year == 21 || year == 22 || year == 23)))

			{
				return true;
			} else {
				objectValidations.modifyAttributes(false, "TARJETA EXPIRADA", "8054", "54");
				return false;
			}

		}

		else {

			if ((year > cYear) || (month >= LocalDate.now().getMonthValue() && year == cYear)
					|| (month == 12 && (year == 20 || year == 21 || year == 22 || year == 23))
					|| ((month == 10 || month == 11) && year == 20))

			{

				return true;
			} else {
				objectValidations.modifyAttributes(false, "TARJETA EXPIRADA", "8054", "54");
				return false;
			}

		}
	}

	public void constructAutraMessage(Base24Ath msg, Iso8583Post msgToTM) throws XPostilion {
		String procCode = msg.getProcessingCode().toString();
		if (procCode.equals("890000")) {
			procCode = msg.getField(126).substring(22, 28);
		}
		msgToTM.putMsgType(Iso8583.MsgType._0200_TRAN_REQ);

		msgToTM.putField(Iso8583.Bit._003_PROCESSING_CODE, this.transformProcessingCodeForAutra(msg));
		if (msg.isFieldSet(Iso8583.Bit._004_AMOUNT_TRANSACTION))
			msgToTM.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION,
					msg.getField(Iso8583.Bit._004_AMOUNT_TRANSACTION).toString());
		else
			msgToTM.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION, Constants.General.TWELVE_ZEROS);

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

		if (msg.isFieldSet(Iso8583.Bit._015_DATE_SETTLE))
			msgToTM.putField(Iso8583.Bit._015_DATE_SETTLE, msg.getField(Iso8583.Bit._015_DATE_SETTLE).toString());

		if (msg.isFieldSet(Iso8583.Bit._017_DATE_CAPTURE))
			msgToTM.putField(Iso8583.Bit._017_DATE_CAPTURE, msg.getField(Iso8583.Bit._017_DATE_CAPTURE).toString());

		if (msg.isFieldSet(Iso8583.Bit._022_POS_ENTRY_MODE))
			msgToTM.putField(Iso8583.Bit._022_POS_ENTRY_MODE, msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE).toString());
		else
			msgToTM.putField(Iso8583.Bit._022_POS_ENTRY_MODE, "021");

		if (msg.isFieldSet(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE))
			msgToTM.putField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE,
					msg.getField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE).toString());

//		if (msg.getProcessingCode().toString().equals("333000")) { 
//
//			msgToTM.putField(Iso8583.Bit._003_PROCESSING_CODE,
//					Iso8583Post.TranType._32_GENERAL_INQUIRY
//							.concat(new ProcessingCode(msg.getField(3)).getFromAccount())
//							.concat(msg.getProcessingCode().getToAccount()).toString());
//			msgToTM.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);
//
//		} else 
		if (msg.isFieldSet(Iso8583.Bit._035_TRACK_2_DATA)) {
			switch (msg.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6)) {
			case "008823":
			case "008802":
			case "008852":
			case "007701":	

				msgToTM.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);

				break;
			default:
				msgToTM.putField(Iso8583.Bit._035_TRACK_2_DATA, msg.getField(Iso8583.Bit._035_TRACK_2_DATA).toString());
				break;
			}

		}

		if (msg.isFieldSet(Iso8583.Bit._037_RETRIEVAL_REF_NR))
			msgToTM.putField(Iso8583.Bit._037_RETRIEVAL_REF_NR,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR).toString());

		if (msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)) {
			msgToTM.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID,
					msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(Constants.Indexes.FIELD41_POSITION_0,
							Constants.Indexes.FIELD41_POSITION_8));
		} else {
			msgToTM.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID,
					Pack.resize(General.VOIDSTRING, General.LENGTH_8, General.SPACE, true));
		}

		msgToTM.putField(Iso8583.Bit._042_CARD_ACCEPTOR_ID_CODE, "               ");

		if (msg.isFieldSet(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC))
			msgToTM.putField(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC,
					msg.getField(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC).toString());

		if (msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA))
			msgToTM.putField(Iso8583.Bit._048_ADDITIONAL_DATA,
					msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).toString());

		if (msg.isFieldSet(Iso8583.Bit._049_CURRENCY_CODE_TRAN))
			msgToTM.putField(Iso8583.Bit._049_CURRENCY_CODE_TRAN,
					msg.getField(Iso8583.Bit._049_CURRENCY_CODE_TRAN).toString());
		else
			msgToTM.putField(Iso8583.Bit._049_CURRENCY_CODE_TRAN, Constants.General.DEFAULT_ERROR_049);

		if (msg.isFieldSet(Iso8583.Bit._052_PIN_DATA))
			msgToTM.putField(Iso8583.Bit._052_PIN_DATA,
					Transform.fromHexToBin(msg.getField(Iso8583.Bit._052_PIN_DATA)));

		if (this.nameInterface.toLowerCase().startsWith("autra"))
			msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "50");
		else
			msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "40");
		

		if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
			msgToTM.putField(Iso8583.Bit._102_ACCOUNT_ID_1, msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).toString());

		if (msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
			msgToTM.putField(Iso8583.Bit._103_ACCOUNT_ID_2, msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).toString());

		msgToTM.putField(Iso8583.Bit._025_POS_CONDITION_CODE, Iso8583.PosCondCode._00_NORMAL_PRESENTMENT);
		msgToTM.putField(Iso8583.Bit._026_POS_PIN_CAPTURE_CODE, PosPinCaptureCode.FOUR);
		msgToTM.putField(Iso8583Post.Bit._123_POS_DATA_CODE, General.POSDATACODE);
		msgToTM.putField(Iso8583.Bit._098_PAYEE, "0054150070650000000000000");

		StructuredData sd = new StructuredData();
		if(procCode.equals("330000") || procCode.equals("334000")
				|| procCode.equals("333000") || procCode.equals("334100")
				|| procCode.equals("334200")){
			sd.put("CONSUL_TITUL", "TRUE");
				if(!msg.isFieldSet(Iso8583.Bit._022_POS_ENTRY_MODE)) {
					sd.put("PROCCESS_FIELD_22", "TRUE");
					msg.putField(Iso8583.Bit._022_POS_ENTRY_MODE, "021");
					msg.clearField(64);
				}				
		}

		String OriginalInput = new String(msg.toMsg(false));
		String encodedString = Base64.getEncoder().encodeToString(OriginalInput.getBytes());

		GenericInterface.getLogger().logLine("Original Input B24 : " + OriginalInput);
		GenericInterface.getLogger().logLine("Encoded Input B24 : " + encodedString);

		
		
		sd.put("B24_Message", encodedString);
		msgToTM.putStructuredData(sd);
		msgToTM.putPrivField(Iso8583Post.PrivBit._002_SWITCH_KEY,
				new ConstructFieldMessage(this.params).constructSwitchKey(msg, "ATM"));

	}

	public void constructAutraRevMessage(Base24Ath msg, Iso8583Post msgToTM) throws XPostilion {
		msgToTM.putMsgType(Iso8583.MsgType._0420_ACQUIRER_REV_ADV);
		msgToTM.putField(Iso8583.Bit._003_PROCESSING_CODE, this.transformProcessingCodeForAutra(msg));

		if (msg.isFieldSet(Iso8583.Bit._004_AMOUNT_TRANSACTION))
			msgToTM.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION,
					msg.getField(Iso8583.Bit._004_AMOUNT_TRANSACTION).toString());

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

		if (msg.isFieldSet(Iso8583.Bit._015_DATE_SETTLE))
			msgToTM.putField(Iso8583.Bit._015_DATE_SETTLE, msg.getField(Iso8583.Bit._015_DATE_SETTLE).toString());

		if (msg.isFieldSet(Iso8583.Bit._017_DATE_CAPTURE))
			msgToTM.putField(Iso8583.Bit._017_DATE_CAPTURE, msg.getField(Iso8583.Bit._017_DATE_CAPTURE).toString());

		if (msg.isFieldSet(Iso8583.Bit._022_POS_ENTRY_MODE))
			msgToTM.putField(Iso8583.Bit._022_POS_ENTRY_MODE, msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE).toString());
		else
			msgToTM.putField(Iso8583.Bit._022_POS_ENTRY_MODE, "021");

		if (msg.isFieldSet(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE))
			msgToTM.putField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE,
					msg.getField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE).toString());

		if (msg.isFieldSet(Iso8583.Bit._035_TRACK_2_DATA)) {
			switch (msg.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6)) {
			case "008823":
			case "008802":
			case "008852":
			case "007701":	

				msgToTM.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);

				break;
			default:
				msgToTM.putField(Iso8583.Bit._035_TRACK_2_DATA, msg.getField(Iso8583.Bit._035_TRACK_2_DATA).toString());
				break;
			}

		}

		if (msg.isFieldSet(Iso8583.Bit._037_RETRIEVAL_REF_NR))
			msgToTM.putField(Iso8583.Bit._037_RETRIEVAL_REF_NR,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR).toString());

		if (msg.isFieldSet(Iso8583.Bit._038_AUTH_ID_RSP))
			msgToTM.putField(Iso8583.Bit._038_AUTH_ID_RSP, msg.getField(Iso8583.Bit._038_AUTH_ID_RSP).toString());

		if (msg.isFieldSet(Iso8583.Bit._039_RSP_CODE))
			msgToTM.putField(Iso8583.Bit._039_RSP_CODE, msg.getField(Iso8583.Bit._039_RSP_CODE).toString());

		if (msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)) {
			msgToTM.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID,
					msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(Constants.Indexes.FIELD41_POSITION_0,
							Constants.Indexes.FIELD41_POSITION_8));
		} else {
			msgToTM.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID,
					Pack.resize(General.VOIDSTRING, General.LENGTH_8, General.SPACE, true));
		}

		msgToTM.putField(Iso8583.Bit._042_CARD_ACCEPTOR_ID_CODE, "               ");

		if (msg.isFieldSet(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC))
			msgToTM.putField(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC,
					msg.getField(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC).toString());

		if (msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA))
			msgToTM.putField(Iso8583.Bit._048_ADDITIONAL_DATA,
					msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).toString());

		if (msg.isFieldSet(Iso8583.Bit._049_CURRENCY_CODE_TRAN))
			msgToTM.putField(Iso8583.Bit._049_CURRENCY_CODE_TRAN,
					msg.getField(Iso8583.Bit._049_CURRENCY_CODE_TRAN).toString());

		if (msg.isFieldSet(Iso8583.Bit._090_ORIGINAL_DATA_ELEMENTS))
			msgToTM.putField(Iso8583.Bit._090_ORIGINAL_DATA_ELEMENTS,
					msg.getField(Iso8583.Bit._090_ORIGINAL_DATA_ELEMENTS).toString());

		if (this.nameInterface.toLowerCase().startsWith("autra"))
			msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "50");
		else
			msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "40");

		if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
			msgToTM.putField(Iso8583.Bit._102_ACCOUNT_ID_1, msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).toString());

		if (msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
			msgToTM.putField(Iso8583.Bit._103_ACCOUNT_ID_2, msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).toString());

		msgToTM.putField(Iso8583.Bit._025_POS_CONDITION_CODE, Iso8583.PosCondCode._00_NORMAL_PRESENTMENT);
		msgToTM.putField(Iso8583.Bit._026_POS_PIN_CAPTURE_CODE, PosPinCaptureCode.FOUR);
		msgToTM.putField(Iso8583Post.Bit._123_POS_DATA_CODE, General.POSDATACODE);
		msgToTM.putField(Iso8583.Bit._098_PAYEE, "0054150070650000000000000");

		String OriginalInput = new String(msg.toMsg(false));
		String encodedString = Base64.getEncoder().encodeToString(OriginalInput.getBytes());

		GenericInterface.getLogger().logLine("Original Input B24 : " + OriginalInput);
		GenericInterface.getLogger().logLine("Encoded Input B24 : " + encodedString);

		StructuredData sd = new StructuredData();
		sd.put("B24_Message", encodedString);
		msgToTM.putStructuredData(sd);
		msgToTM.putPrivField(Iso8583Post.PrivBit._002_SWITCH_KEY,
				new ConstructFieldMessage(this.params).constructSwitchKey(msg, "ATM"));

		msgToTM.putPrivField(Iso8583Post.PrivBit._011_ORIGINAL_KEY,
				msg.getField(Iso8583.Bit._090_ORIGINAL_DATA_ELEMENTS).substring(General.LENGTH_0, General.LENGTH_32));

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

		if (this.nameInterface.toLowerCase().startsWith("autra"))
			msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "50");
		else
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

	public void constructAutraResponseMessage(Base24Ath msg, Iso8583Post msgToTM) throws XPostilion {
		msgToTM.putMsgType(Iso8583.MsgType._0210_TRAN_REQ_RSP);
		msgToTM.putField(Iso8583.Bit._003_PROCESSING_CODE, this.transformProcessingCodeForAutra(msg));

		if (msg.isFieldSet(Iso8583.Bit._004_AMOUNT_TRANSACTION))
			msgToTM.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION,
					msg.getField(Iso8583.Bit._004_AMOUNT_TRANSACTION).toString());

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

		if (msg.isFieldSet(Iso8583.Bit._015_DATE_SETTLE))
			msgToTM.putField(Iso8583.Bit._015_DATE_SETTLE, msg.getField(Iso8583.Bit._015_DATE_SETTLE).toString());

		if (msg.isFieldSet(Iso8583.Bit._017_DATE_CAPTURE))
			msgToTM.putField(Iso8583.Bit._017_DATE_CAPTURE, msg.getField(Iso8583.Bit._017_DATE_CAPTURE).toString());

		if (msg.isFieldSet(Iso8583.Bit._022_POS_ENTRY_MODE))
			msgToTM.putField(Iso8583.Bit._022_POS_ENTRY_MODE, msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE).toString());
		else
			msgToTM.putField(Iso8583.Bit._022_POS_ENTRY_MODE, "021");

		if (msg.isFieldSet(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE))
			msgToTM.putField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE,
					msg.getField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE).toString());

		if (msg.isFieldSet(Iso8583.Bit._035_TRACK_2_DATA)) {
			switch (msg.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6)) {
			case "008823":
			case "008802":
			case "008852":
			case "007701":	

				msgToTM.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);

				break;
			default:
				msgToTM.putField(Iso8583.Bit._035_TRACK_2_DATA, msg.getField(Iso8583.Bit._035_TRACK_2_DATA).toString());
				break;
			}

		}

		if (msg.isFieldSet(Iso8583.Bit._037_RETRIEVAL_REF_NR))
			msgToTM.putField(Iso8583.Bit._037_RETRIEVAL_REF_NR,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR).toString());

		if (msg.isFieldSet(Iso8583.Bit._038_AUTH_ID_RSP))
			msgToTM.putField(Iso8583.Bit._038_AUTH_ID_RSP, msg.getField(Iso8583.Bit._038_AUTH_ID_RSP).toString());

		if (msg.isFieldSet(Iso8583.Bit._039_RSP_CODE))
			msgToTM.putField(Iso8583.Bit._039_RSP_CODE, msg.getField(Iso8583.Bit._039_RSP_CODE).toString());

		if (msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)) {
			msgToTM.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID,
					msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(Constants.Indexes.FIELD41_POSITION_0,
							Constants.Indexes.FIELD41_POSITION_8));
		} else {
			msgToTM.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID,
					Pack.resize(General.VOIDSTRING, General.LENGTH_8, General.SPACE, true));
		}

		msgToTM.putField(Iso8583.Bit._042_CARD_ACCEPTOR_ID_CODE, "               ");

		if (msg.isFieldSet(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC))
			msgToTM.putField(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC,
					msg.getField(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC).toString());

		if (msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA))
			msgToTM.putField(Iso8583.Bit._048_ADDITIONAL_DATA,
					msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).toString());

		if (msg.isFieldSet(Iso8583.Bit._049_CURRENCY_CODE_TRAN))
			msgToTM.putField(Iso8583.Bit._049_CURRENCY_CODE_TRAN,
					msg.getField(Iso8583.Bit._049_CURRENCY_CODE_TRAN).toString());

		if (msg.isFieldSet(62)) {

			msg.putField(62, Pack.resize(Normalizer.normalize(msg.getField(62), Normalizer.Form.NFD)
					.replaceAll("\\p{InCombiningDiacriticalMarks}+", ""), 150, ' ', false));
		}

		if (this.nameInterface.toLowerCase().startsWith("autra"))
			msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "50");
		else
			msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "40");

		if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
			msgToTM.putField(Iso8583.Bit._102_ACCOUNT_ID_1, msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).toString());

		if (msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
			msgToTM.putField(Iso8583.Bit._103_ACCOUNT_ID_2, msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).toString());

		if (msg.isFieldSet(Iso8583.Bit._104_TRAN_DESCRIPTION))
			msgToTM.putField(Iso8583.Bit._104_TRAN_DESCRIPTION,
					msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION).toString());

//		msgToTM.putField(Iso8583.Bit._025_POS_CONDITION_CODE, Iso8583.PosCondCode._00_NORMAL_PRESENTMENT);
//		msgToTM.putField(Iso8583.Bit._026_POS_PIN_CAPTURE_CODE, PosPinCaptureCode.FOUR);
//		msgToTM.putField(Iso8583Post.Bit._123_POS_DATA_CODE, General.POSDATACODE);
//		msgToTM.putField(Iso8583.Bit._098_PAYEE, "0054150070650000000000000");
		String OriginalInput;
		try {
			OriginalInput = new String(msg.toMsg(false), "US-ASCII");
		} catch (XBitmapUnableToConstruct e) {
			EventReporter.reportGeneralEvent(this.nameInterface, Super.class.getName(), e,
					((Iso8583) msg).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructAutraResponseMessage",
					this.udpClient);
			OriginalInput = "";
		} catch (UnsupportedEncodingException e) {
			EventReporter.reportGeneralEvent(this.nameInterface, Super.class.getName(), e,
					((Iso8583) msg).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructAutraResponseMessage",
					this.udpClient);
			OriginalInput = "";
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, Super.class.getName(), e,
					((Iso8583) msg).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructAutraResponseMessage",
					this.udpClient);
			OriginalInput = "";
		}
		GenericInterface.getLogger().logLine("Original Response B24 : " + OriginalInput);
		String encodedString = Base64.getEncoder().encodeToString(OriginalInput.getBytes());

		StructuredData sd = msgToTM.getStructuredData();
		sd.put("B24_MessageRsp", encodedString);
		msgToTM.putStructuredData(sd);

	}

	public void constructAutraRevResponseMessage(Base24Ath msg, Iso8583Post msgToTM) throws XPostilion {
		msgToTM.putMsgType(Iso8583.MsgType._0430_ACQUIRER_REV_ADV_RSP);
		msgToTM.putField(Iso8583.Bit._003_PROCESSING_CODE, this.transformProcessingCodeForAutra(msg));

		if (msg.isFieldSet(Iso8583.Bit._004_AMOUNT_TRANSACTION))
			msgToTM.putField(Iso8583.Bit._004_AMOUNT_TRANSACTION,
					msg.getField(Iso8583.Bit._004_AMOUNT_TRANSACTION).toString());

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

		if (msg.isFieldSet(Iso8583.Bit._015_DATE_SETTLE))
			msgToTM.putField(Iso8583.Bit._015_DATE_SETTLE, msg.getField(Iso8583.Bit._015_DATE_SETTLE).toString());

		if (msg.isFieldSet(Iso8583.Bit._017_DATE_CAPTURE))
			msgToTM.putField(Iso8583.Bit._017_DATE_CAPTURE, msg.getField(Iso8583.Bit._017_DATE_CAPTURE).toString());

		if (msg.isFieldSet(Iso8583.Bit._022_POS_ENTRY_MODE))
			msgToTM.putField(Iso8583.Bit._022_POS_ENTRY_MODE, msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE).toString());
		else
			msgToTM.putField(Iso8583.Bit._022_POS_ENTRY_MODE, "021");

		if (msg.isFieldSet(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE))
			msgToTM.putField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE,
					msg.getField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE).toString());

		if (msg.isFieldSet(Iso8583.Bit._035_TRACK_2_DATA)) {
			switch (msg.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0, 6)) {
			case "008823":
			case "008802":
			case "008852":
			case "007701":	

				msgToTM.putField(Iso8583.Bit._035_TRACK_2_DATA, Constants.General.DEFAULT_TRACK2_MASIVA);

				break;
			default:
				msgToTM.putField(Iso8583.Bit._035_TRACK_2_DATA, msg.getField(Iso8583.Bit._035_TRACK_2_DATA).toString());
				break;
			}

		}

		if (msg.isFieldSet(Iso8583.Bit._037_RETRIEVAL_REF_NR))
			msgToTM.putField(Iso8583.Bit._037_RETRIEVAL_REF_NR,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR).toString());

		if (msg.isFieldSet(Iso8583.Bit._038_AUTH_ID_RSP))
			msgToTM.putField(Iso8583.Bit._038_AUTH_ID_RSP, msg.getField(Iso8583.Bit._038_AUTH_ID_RSP).toString());

		if (msg.isFieldSet(Iso8583.Bit._039_RSP_CODE))
			msgToTM.putField(Iso8583.Bit._039_RSP_CODE, msg.getField(Iso8583.Bit._039_RSP_CODE).toString());

		if (msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)) {
			msgToTM.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID,
					msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(Constants.Indexes.FIELD41_POSITION_0,
							Constants.Indexes.FIELD41_POSITION_8));
		} else {
			msgToTM.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID,
					Pack.resize(General.VOIDSTRING, General.LENGTH_8, General.SPACE, true));
		}

		msgToTM.putField(Iso8583.Bit._042_CARD_ACCEPTOR_ID_CODE, "               ");

		if (msg.isFieldSet(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC))
			msgToTM.putField(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC,
					msg.getField(Iso8583.Bit._043_CARD_ACCEPTOR_NAME_LOC).toString());

		if (msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA))
			msgToTM.putField(Iso8583.Bit._048_ADDITIONAL_DATA,
					msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).toString());

		if (msg.isFieldSet(Iso8583.Bit._049_CURRENCY_CODE_TRAN))
			msgToTM.putField(Iso8583.Bit._049_CURRENCY_CODE_TRAN,
					msg.getField(Iso8583.Bit._049_CURRENCY_CODE_TRAN).toString());

		if (this.nameInterface.toLowerCase().startsWith("autra"))
			msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "50");
		else
			msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "40");

		if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
			msgToTM.putField(Iso8583.Bit._102_ACCOUNT_ID_1, msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).toString());

		if (msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
			msgToTM.putField(Iso8583.Bit._103_ACCOUNT_ID_2, msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).toString());

		if (msg.isFieldSet(Iso8583.Bit._104_TRAN_DESCRIPTION))
			msgToTM.putField(Iso8583.Bit._104_TRAN_DESCRIPTION,
					msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION).toString());

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

		if (this.nameInterface.toLowerCase().startsWith("autra"))
			msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "50");
		else
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

	/**
	 * Identifies the source channel of a transaction with a message ISO8583
	 * 
	 * @param msg
	 * @return CBC for Credibanco, CNB for "Corresponsal no bancario" , VTL
	 *         "Transferencia masivas", ATM for ATMs
	 * @throws XPostilion if msg doesn't have field 37
	 *
	 */
	public String channelIdentifier(Iso8583 msg) throws XPostilion {
		String channel = Constants.Channels.ATM;
		if (msg instanceof Base24Ath) {
			channel = channelIdentifier((Base24Ath) msg);
		} else if (msg instanceof Iso8583Post) {
			try {
				channel = channelIdentifier((Iso8583Post) msg);
			} catch (XPostilion e) {
				EventReporter.reportGeneralEvent(this.nameInterface, Super.class.getName(), e,
						msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "channelIdentifier", this.udpClient);
			}
		}
		return channel;
	}

	/**
	 * Identifies the source channel of a transaction with a message Base24
	 * 
	 * @param msg
	 * @return CBC for Credibanco, CNB for "Corresponsal no bancario" , VTL
	 *         "Transferencia masivas", ATM for ATMs
	 *
	 */
	public String channelIdentifier(Base24Ath msg) {
		String channel;
		try {
			this.udpClient.sendData(Client.getMsgKeyValue("999999",
					"channelIdentifier: nameInterface:" + this.nameInterface, "LOG", this.nameInterface));
			String p41 = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID);
			String red = p41.substring(4, 8);

			if (this.nameInterface.equals("Credibanco"))
				return Constants.Channels.CBCO;
			else if (p41.substring(12, 13).equals(Constants.General.SEVEN))
				return Constants.Channels.CNB;
			else if (p41.substring(12, 13).equals("2") || p41.substring(12, 13).equals("5")) {
				if (p41.subSequence(0, 4).equals("0001") && p41.substring(12, 13).equals("5")) {
					channel = Constants.Channels.IVR;
					GenericInterface.getLogger().logLine("IVR");
				} else {
					channel = Constants.Channels.VTL;
				}
			} else if ((p41.substring(0, 4).equals("0054") && p41.substring(12, 13).equals("1"))
					|| (red.equals("1004") || red.equals("1005") || red.equals("1006")))
				return Constants.Channels.ATM;
			else
				channel = Constants.Channels.ATM;

		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, Super.class.getName(), e, "N/D", "channelIdentifier",
					this.udpClient);
			channel = null;
		}

		return channel;
	}

	/**
	 * Identifies the source channel of a transaction with a message ISO8583Post
	 * 
	 * @param msg
	 * @return CBC for Credibanco, CNB for "Corresponsal no bancario" , VTL
	 *         "Transferencia masivas", ATM for ATMs
	 * @throws XPostilion if fiel 37 is not present in msg
	 *
	 */
	public String channelIdentifier(Iso8583Post msg) throws XPostilion {
		String channel = Constants.Channels.IVR;
		try {
			postilion.realtime.sdk.message.bitmap.PosDataCode posDataField = msg.getPosDataCode();
			switch (posDataField.getTerminalType()) {
			case "20":
				channel = Constants.Channels.IVR;
				break;
			default:
				channel = Constants.Channels.IVR;
				break;
			}
		} catch (XFieldUnableToConstruct e) {
			EventReporter.reportGeneralEvent(this.nameInterface, Super.class.getName(), e,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "channelIdentifier", this.udpClient);
		}
		return channel;

	}

	/**
	 * El metodo se encarga de validar que la tarjeta del cliente del corresponsal
	 * exista. Si la tarjeta existe trae informacion de base de datos relacionada a
	 * la tarjeta y a su cuenta. Modifica al objeto Super almacenando la informacion
	 * encontrada en base. de lo contrario genera una declinacion.
	 * 
	 * @param pCode
	 * @param pan
	 * @param typeAccountInput
	 * @param expiryDate
	 * @param accountInput
	 * @param objectValidations
	 * @throws Exception
	 */
	protected void accountsByNumberClientCNB(String p37, String accountNumber, String typeAccountInput,
			Super objectValidations) throws Exception {
		this.udpClient.sendData(Client.getMsgKeyValue(
				p37, "accountNumber:" + accountNumber + " typeAccountInput:" + typeAccountInput
						+ " encriptedaccountNumber:" + securityManager.encrypt(accountNumber),
				"LOG", this.nameInterface));

		CallableStatement stmt = null;
		ResultSet rs = null;
		Connection con = null;
		try {

			con = JdbcManager.getConnection(Account.POSTCARD_DATABASE);
			stmt = con.prepareCall(StoreProcedures.GET_ACCOUNT_ADITIONAL_INFO_CNB);

			stmt.setString(1, this.issuerId);
			stmt.setString(2, securityManager.encrypt(accountNumber));
			stmt.setString(3, typeAccountInput);
//			stmt.registerOutParameter(4, java.sql.Types.VARCHAR);//account_type
//			stmt.registerOutParameter(5, java.sql.Types.VARCHAR);//currency_code
//			stmt.registerOutParameter(6, java.sql.Types.VARCHAR);//last_updated_date
//			stmt.registerOutParameter(7, java.sql.Types.VARCHAR);//last_updated_user
//			stmt.registerOutParameter(8, java.sql.Types.VARCHAR);//hold_rsp_code
//			stmt.registerOutParameter(9, java.sql.Types.VARCHAR);//account_product
//			stmt.registerOutParameter(10, java.sql.Types.VARCHAR);//extended_fields
//			stmt.registerOutParameter(11, java.sql.Types.VARCHAR);//overdraft_limit
			rs = stmt.executeQuery();
			this.udpClient.sendData(Client.getMsgKeyValue(p37, "Termino ejecucion GET_ACCOUNT_ADITIONAL_INFO_CNB",
					"LOG", this.nameInterface));
			rs.next();

			if (rs.getString(1) != null) {
				this.udpClient.sendData(Client.getMsgKeyValue(p37, "Encontro datos cuenta", "LOG", this.nameInterface));
				String customer_id = rs.getString(1);// customer_id
				String extended_fields = rs.getString(2) == null ? "" : rs.getString(2);// extended_fields
				String pan_encrypted = rs.getString(3) == null ? "" : rs.getString(3);// pan_encrypted
				String c1_first_name = rs.getString(4) == null ? "" : rs.getString(4);// c1_first_name

				this.udpClient
						.sendData(Client.getMsgKeyValue(p37, "customer_id:" + customer_id, "LOG", this.nameInterface));
				this.udpClient.sendData(
						Client.getMsgKeyValue(p37, "extended_fields:" + extended_fields, "LOG", this.nameInterface));
				this.udpClient.sendData(
						Client.getMsgKeyValue(p37, "pan_encrypted:" + pan_encrypted, "LOG", this.nameInterface));
				this.udpClient.sendData(
						Client.getMsgKeyValue(p37, "c1_first_name:" + c1_first_name, "LOG", this.nameInterface));
				this.udpClient.sendData(Client.getMsgKeyValue(p37,
						"pan:" + securityManager.decryptToString(pan_encrypted), "LOG", this.nameInterface));

				tagsEncodeSensitiveData("CLIENT2_ACCOUNT_NR", accountNumber, objectValidations);
				objectValidations.putInforCollectedForStructData("CLIENT2_ACCOUNT_TYPE", typeAccountInput);
				objectValidations.putInforCollectedForStructData("CUSTOMER2_ID", customer_id);
				objectValidations.putInforCollectedForStructData("CLIENT2_CARD_CLASS", extended_fields);
				objectValidations.putInforCollectedForStructData("CUSTOMER2_NAME", c1_first_name);
				tagsEncodeSensitiveData("CLIENT2_CARD_NR", securityManager.decryptToString(pan_encrypted),
						objectValidations);

				this.udpClient
						.sendData(Client.getMsgKeyValue(p37, "Puso datos en structure", "LOG", this.nameInterface));

				JdbcManager.commit(con, stmt, rs);
			} else {

				objectValidations.modifyAttributes(false, " NO EXITE CUENTA", "0001", "14");// Error 12

			}

			objectValidations.putInforCollectedForStructData("P_CODE", "000000");
			this.udpClient.sendData(
					Client.getMsgKeyValue(p37, "SALIENDO DE accountsByNumberClientCNB", "LOG", this.nameInterface));

		} catch (SQLException e) {
			objectValidations.modifyAttributes(false, "Database Connection Failure.", "1006", "06");
			e.printStackTrace();
			EventRecorder.recordEvent(new SQLExceptionEvent(new String[] { Account.POSTCARD_DATABASE,
					StoreProcedures.GET_ACCOUNT_ADITIONAL_INFO_CNB, e.getMessage() }));

			StringWriter outError = new StringWriter();
			e.printStackTrace(new PrintWriter(outError));
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"catch metodo accountsByNumberClientCNB : " + outError.toString(), "ERR", this.nameInterface));

		} finally {
			try {
				JdbcManager.cleanup(con, stmt, rs);
			} catch (SQLException e) {
				this.udpClient.sendData(Client.getMsgKeyValue(p37, e.getMessage(), "ERR", this.nameInterface));
				EventRecorder.recordEvent(new SQLExceptionEvent(new String[] { Account.POSTCARD_DATABASE,
						StoreProcedures.GET_ACCOUNT_ADITIONAL_INFO_CNB, e.getMessage() }));
			}
		}

	}

	/**
	 * El metodo se encarga de validar que la tarjeta del cliente del corresponsal
	 * exista. Si la tarjeta existe trae informacion de base de datos relacionada a
	 * la tarjeta y a su cuenta. Modifica al objeto Super almacenando la informacion
	 * encontrada en base. de lo contrario genera una declinacion.
	 * 
	 * @param pCode
	 * @param pan
	 * @param typeAccountInput
	 * @param expiryDate
	 * @param accountInput
	 * @param objectValidations
	 * @throws Exception
	 */
	protected void accountsClienteCNB(String p37, String pCode, String pan, String typeAccountInput, String expiryDate,
			String accountInput, Super objectValidations) throws Exception {

		if (validationexpiryDate(expiryDate, objectValidations)) {

			String panHash = Utils.getHashPanCNB(pan);

			CallableStatement cst = null;
			CallableStatement stmt = null;
			ResultSet rs = null;
			Connection con = null;
			try {
				this.udpClient.sendData(Client.getMsgKeyValue(p37, "entro al primer cp cuando se coge del p35  pan: "
						+ panHash + " fecha de expiracion: " + expiryDate, "LOG", this.nameInterface));

				con = JdbcManager.getConnection(Account.POSTCARD_DATABASE);
				stmt = con.prepareCall(StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME);
				stmt.setString(1, panHash);
				stmt.setString(2, expiryDate);
				stmt.registerOutParameter(3, java.sql.Types.VARCHAR);// customer id
				stmt.registerOutParameter(4, java.sql.Types.VARCHAR);// default account type
				stmt.registerOutParameter(5, java.sql.Types.VARCHAR);// name
				stmt.registerOutParameter(6, java.sql.Types.INTEGER);// issure
				stmt.registerOutParameter(7, java.sql.Types.VARCHAR);// extended field
				stmt.registerOutParameter(8, java.sql.Types.VARCHAR);// sequence nr
				stmt.registerOutParameter(9, java.sql.Types.CHAR);// id type
				stmt.execute();

				String customerId = stmt.getString(3);// customer id
				String defaultAccountType = stmt.getString(4);// default account type
				String customerName = stmt.getString(5);// name
				int issuerNr = stmt.getInt(6);// issuer number
				String extendedField = stmt.getString(7);// extended field
				String sequenceNr = stmt.getString(8);// sequence number
				String idType = stmt.getString(9);// id type
				String accountTypeClient = null;// account Type
				String accountNumber = null;// account Number
				String processingCode = null;// p Code

				this.udpClient.sendData(Client.getMsgKeyValue(p37,
						"validacion CNBCliente. primer SP customerID " + customerId + " ceuntaDefecto: "
								+ defaultAccountType + " name: " + customerName + " issure: " + issuerNr
								+ " extendedfield: " + extendedField + " seq_nr: " + sequenceNr,
						"LOG", this.nameInterface));
				GenericInterface.getLogger()
						.logLine("validacion CNBCliente. primer SP customerID " + customerId + " ceuntaDefecto: "
								+ defaultAccountType + " name: " + customerName + " issure: " + issuerNr
								+ " extendedfield: " + extendedField + " seq_nr: " + sequenceNr);

				if (!(issuerNr == 0)) {

					this.udpClient.sendData(Client.getMsgKeyValue(p37, "Esto entra al segundo sp: issuerNr:" + issuerNr
							+ " hashPanCNB: " + panHash + " seqNr: " + sequenceNr, "LOG", this.nameInterface));

					GenericInterface.getLogger().logLine("Esto entra al segundo sp: issuerNr:" + issuerNr
							+ " hashPanCNB: " + panHash + " seqNr: " + sequenceNr);
					cst = con.prepareCall(StoreProcedures.CM_LOAD_CARD_ACCOUNTS);
					cst.setInt(1, issuerNr);
					cst.setString(2, panHash);
					cst.setString(3, sequenceNr);
					rs = cst.executeQuery();

					String accountTypeDefault = null, accountIdDefault = null;
					String accountTypeDefaultTypeAccount = null, accountIdDefaultTypeAccount = null;

					boolean flagDefaultaccount = true;
					boolean flagDefaultaccountTypeAccount = true;
					boolean doWhile = true;
					boolean accountConflict = false;
					while (rs.next()) {
						doWhile = false;
						String accountType = rs.getString(ColumnNames.ACCOUNT_TYPE);// account type
						String accountId = rs.getString(ColumnNames.ACCOUNT_ID);// account type

						accountInput = ("000000000000000000" + accountInput)
								.substring(("000000000000000000" + accountInput).length() - 18);
						this.udpClient.sendData(Client.getMsgKeyValue(p37,
								"cuenta que viene 102: con subString : " + accountInput + " cuentas que tra el sp2: "
										+ Utils.getClearAccount(accountId) + " tipo de cuenta sp " + accountType,
								"LOG", this.nameInterface));

						GenericInterface.getLogger()
								.logLine("cuenta que viene 102: con subString : " + accountInput
										+ " cuentas que tra el sp2: " + Utils.getClearAccount(accountId)
										+ " tipo de cuenta sp " + accountType);

						if (accountType.equals(typeAccountInput)) {
							if (accountInput.equals(Utils.getClearAccount(accountId))) {

								accountTypeClient = accountType;// account type
								accountNumber = accountId;// cuenta
								processingCode = "000000";
								break;
							} else if (flagDefaultaccountTypeAccount) {
								accountTypeDefaultTypeAccount = accountType;
								accountIdDefaultTypeAccount = accountId;
								flagDefaultaccountTypeAccount = false;
							}

						} else if (accountType.equals(defaultAccountType) && flagDefaultaccount) {
							accountTypeDefault = accountType;
							accountIdDefault = rs.getString(ColumnNames.ACCOUNT_ID);
							flagDefaultaccount = false;
						}

					}
					if (doWhile) {
						chooseCodeForNoAccount(pCode, objectValidations);
					} else {
						if (accountTypeClient == null || accountNumber == null || processingCode == null) {
							if (accountTypeDefaultTypeAccount == null || accountIdDefaultTypeAccount == null) {
								if (accountIdDefault != null) {
									accountTypeClient = accountTypeDefault;// account type
									accountNumber = accountIdDefault;// cuenta
									processingCode = pCode.substring(0, 2) + accountTypeDefault + pCode.substring(4, 6);
								} else {
									accountConflict = true;
									this.udpClient.sendData(Client.getMsgKeyValue(p37,
											">>>>> There is an account conflict <<<<<<<", "LOG", this.nameInterface));
								}
							} else {
								accountTypeClient = accountTypeDefaultTypeAccount;// account type
								accountNumber = accountIdDefaultTypeAccount;// cuenta
								processingCode = "000000";
							}
						}
						if (!accountConflict) {
							objectValidations.putInforCollectedForStructData("P_CODE", processingCode);

							tagsEncodeSensitiveData("CLIENT_ACCOUNT_NR", Utils.getClearAccount(accountNumber),
									objectValidations);
							tagsEncodeSensitiveData("CLIENT_CARD_NR", pan, objectValidations);

							objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", accountTypeClient);
							objectValidations.putInforCollectedForStructData("CUSTOMER_ID", customerId);
							objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", extendedField);
							objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", customerName);
							objectValidations.putInforCollectedForStructData("CUSTOMER_ID_TYPE", idType);

							this.udpClient.sendData(Client.getMsgKeyValue(p37,
									"final del metodo accountsClienteCBN: cuando mira p35 " + customerId
											+ " ceuntaDefecto: " + defaultAccountType + " name: " + customerName
											+ " issure: " + issuerNr + " extendedfield: " + extendedField + " seq_nr: "
											+ sequenceNr + " accountTypeClient: " + accountTypeClient
											+ " accountNumber: " + accountNumber + " processingCode :" + processingCode,
									"LOG", this.nameInterface));

						} else {
							objectValidations.modifyAttributes(false, "CUENTA NO ASOCIADA", "0001", "14");
						}
					}
				} else {
					objectValidations.modifyAttributes(false, " NO EXITE TARJETA CLIENTE", "8014",
							RspCode._56_NO_CARD_RECORD);
				}

			} catch (SQLException e) {
				objectValidations.modifyAttributes(false, "Database Connection Failure.", "1006", "06");
				e.printStackTrace();
				EventRecorder
						.recordEvent(new SQLExceptionEvent(new String[] {
								Account.POSTCARD_DATABASE, StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME
										+ General.VOIDSTRING + StoreProcedures.CM_LOAD_CARD_ACCOUNTS,
								e.getMessage() }));

				StringWriter outError = new StringWriter();
				e.printStackTrace(new PrintWriter(outError));
				this.udpClient.sendData(Client.getMsgKeyValue(p37,
						"catch metodo accountsClienteCBN : " + outError.toString(), "ERR", this.nameInterface));

			} finally {
				JdbcManager.cleanup(con, stmt, rs);
				JdbcManager.cleanup(con, cst, rs);
			}
		}

	}

	protected void accountsClienteCNB(String p37, String pCode, String pan, String typeAccountInput, String expiryDate,
			String accountInput, Super objectValidations, boolean expiryDateValidation) throws Exception {

		if (!expiryDateValidation) {

			String panHash = Utils.getHashPanCNB(pan);

			CallableStatement cst = null;
			CallableStatement stmt = null;
			ResultSet rs = null;
			Connection con = null;
			try {
				this.udpClient.sendData(Client.getMsgKeyValue(p37, "entro al primer cp cuando se coge del p35  pan: "
						+ panHash + " fecha de expiracion: " + expiryDate, "LOG", this.nameInterface));

				con = JdbcManager.getConnection(Account.POSTCARD_DATABASE);
				stmt = con.prepareCall(StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME);
				stmt.setString(1, panHash);
				stmt.setString(2, expiryDate);
				stmt.registerOutParameter(3, java.sql.Types.VARCHAR);// customer id
				stmt.registerOutParameter(4, java.sql.Types.VARCHAR);// default account type
				stmt.registerOutParameter(5, java.sql.Types.VARCHAR);// name
				stmt.registerOutParameter(6, java.sql.Types.INTEGER);// issure
				stmt.registerOutParameter(7, java.sql.Types.VARCHAR);// extended field
				stmt.registerOutParameter(8, java.sql.Types.VARCHAR);// sequence nr
				stmt.execute();

				String customerId = stmt.getString(3);// customer id
				String defaultAccountType = stmt.getString(4);// default account type
				String customerName = stmt.getString(5);// name
				int issuerNr = stmt.getInt(6);// issuer number
				String extendedField = stmt.getString(7);// extended field
				String sequenceNr = stmt.getString(8);// sequence number
				String accountTypeClient = null;// account Type
				String accountNumber = null;// account Number
				String processingCode = null;// p Code

				this.udpClient.sendData(Client.getMsgKeyValue(p37,
						"validacion CNBCliente. primer SP customerID " + customerId + " ceuntaDefecto: "
								+ defaultAccountType + " name: " + customerName + " issure: " + issuerNr
								+ " extendedfield: " + extendedField + " seq_nr: " + sequenceNr,
						"LOG", this.nameInterface));

				if (!(issuerNr == 0)) {

					this.udpClient.sendData(Client.getMsgKeyValue(p37, "Esto entra al segundo sp: issuerNr:" + issuerNr
							+ " hashPanCNB: " + panHash + " seqNr: " + sequenceNr, "LOG", this.nameInterface));
					cst = con.prepareCall(StoreProcedures.CM_LOAD_CARD_ACCOUNTS);
					cst.setInt(1, issuerNr);
					cst.setString(2, panHash);
					cst.setString(3, sequenceNr);
					rs = cst.executeQuery();

					String accountTypeDefault = null, accountIdDefault = null;
					String accountTypeDefaultTypeAccount = null, accountIdDefaultTypeAccount = null;

					boolean flagDefaultaccount = true;
					boolean flagDefaultaccountTypeAccount = true;
					boolean doWhile = true;
					while (rs.next()) {

						doWhile = false;
						String accountType = rs.getString(ColumnNames.ACCOUNT_TYPE);// account type
						String accountId = rs.getString(ColumnNames.ACCOUNT_ID);// account type

						accountInput = ("000000000000000000" + accountInput)
								.substring(("000000000000000000" + accountInput).length() - 18);
						this.udpClient.sendData(Client.getMsgKeyValue(
								p37, "cuenta que viene 102: con subString : " + accountInput
										+ " cuentas que tra el sp2: " + Utils.getClearAccount(accountId),
								"LOG", this.nameInterface));

						if (accountType.equals(typeAccountInput)) {

							if (accountInput.equals(Utils.getClearAccount(accountId))) {

								accountTypeClient = accountType;// account type
								accountNumber = accountId;// cuenta
								processingCode = "000000";
								break;
							} else if (flagDefaultaccountTypeAccount) {
								accountTypeDefaultTypeAccount = accountType;
								accountIdDefaultTypeAccount = accountId;
								flagDefaultaccountTypeAccount = false;
							}

						} else if (accountType.equals(defaultAccountType) && flagDefaultaccount) {
							accountTypeDefault = accountType;
							accountIdDefault = rs.getString(ColumnNames.ACCOUNT_ID);
							flagDefaultaccount = false;
						}
					}
					if (doWhile) {
						chooseCodeForNoAccount(pCode, objectValidations);
					} else {

						if (accountTypeClient == null || accountNumber == null || processingCode == null) {

							if (accountTypeDefaultTypeAccount == null || accountIdDefaultTypeAccount == null) {
								accountTypeClient = accountTypeDefault;// account type
								accountNumber = accountIdDefault;// cuenta
								processingCode = pCode.substring(0, 2) + accountTypeDefault + pCode.substring(4, 6);
							} else {
								accountTypeClient = accountTypeDefaultTypeAccount;// account type
								accountNumber = accountIdDefaultTypeAccount;// cuenta
								processingCode = "000000";
							}
						}

						objectValidations.putInforCollectedForStructData("P_CODE", processingCode);

						tagsEncodeSensitiveData("CLIENT_ACCOUNT_NR", Utils.getClearAccount(accountNumber),
								objectValidations);
						tagsEncodeSensitiveData("CLIENT_CARD_NR", pan, objectValidations);

						objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", accountTypeClient);
						objectValidations.putInforCollectedForStructData("CUSTOMER_ID", customerId);
						objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", extendedField);
						objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", customerName);
					}
					this.udpClient.sendData(Client.getMsgKeyValue(p37,
							"final del metodo accountsClienteCBN: cuando mira p35 " + customerId + " ceuntaDefecto: "
									+ defaultAccountType + " name: " + customerName + " issure: " + issuerNr
									+ " extendedfield: " + extendedField + " seq_nr: " + sequenceNr
									+ " accountTypeClient: " + accountTypeClient + " accountNumber: " + accountNumber
									+ " processingCode :" + processingCode,
							"LOG", this.nameInterface));

				} else {
					objectValidations.modifyAttributes(false, " NO EXITE TARJETA CLIENTE", "8014",
							RspCode._56_NO_CARD_RECORD);
				}

			} catch (SQLException e) {
				objectValidations.modifyAttributes(false, "Database Connection Failure.", "1006", "06");
				e.printStackTrace();
				EventRecorder
						.recordEvent(new SQLExceptionEvent(new String[] {
								Account.POSTCARD_DATABASE, StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME
										+ General.VOIDSTRING + StoreProcedures.CM_LOAD_CARD_ACCOUNTS,
								e.getMessage() }));

				StringWriter outError = new StringWriter();
				e.printStackTrace(new PrintWriter(outError));
				this.udpClient.sendData(Client.getMsgKeyValue(p37,
						"catch metodo accountsClienteCBN : " + outError.toString(), "LOG", this.nameInterface));

			} finally {
				JdbcManager.cleanup(con, stmt, rs);
				JdbcManager.cleanup(con, cst, rs);
			}
		}

	}

	public void tagsEncodeSensitiveData(String tag, String data, Super objectValidations) {

		if (this.encodeData) {
			objectValidations.putInforCollectedForStructData(tag, Base64.getEncoder().encodeToString(data.getBytes()));
		} else {
			objectValidations.putInforCollectedForStructData(tag, data);
		}

	}

	public String transformProcessingCodeForAutra(Base24Ath msg) throws XPostilion {
		String field3 = null;
		if (msg.isFieldSet(Iso8583.Bit._003_PROCESSING_CODE)) {

			try {
				ProcessingCode procCode = msg.getProcessingCode();
				String fromAccount = procCode.getFromAccount();
				String toAccount = procCode.getToAccount();
				switch (procCode.toString()) {
				case Constants.Channels.PCODE_CONSULTA_DE_COSTO_ATM:
					field3 = "32" + msg.getField(Base24Ath.Bit._126_ATH_ADDITIONAL_DATA).substring(24, 26) + "00";
					break;
				case Constants.Channels.PCODE_DEPOSITO_ATM_MULTIFUNCIONAL_AHO:
				case Constants.Channels.PCODE_DEPOSITO_ATM_MULTIFUNCIONAL_COR:
					field3 = postilion.realtime.sdk.message.bitmap.Iso8583.TranType._21_DEPOSITS + fromAccount
							+ toAccount;
					break;
				case Constants.Channels.PCODE_PAGO_CREDITO_HIPOTECARIO_EFECTIVO_ATM_MULTIFUNCIONAL:
				case Constants.Channels.PCODE_PAGO_CREDITO_ROTATIVO_EFECTIVO_ATM_MULTIFUNCIONAL:
				case Constants.Channels.PCODE_PAGO_OTROS_CREDITOS_EFECTIVO_ATM_MULTIFUNCIONAL:
					field3 = postilion.realtime.sdk.message.bitmap.Iso8583Post.TranType._51_PAYMENT_BY_DEPOSIT
							+ fromAccount + toAccount;
					break;
				case Constants.Channels.PCODE_CONSULTA_TITULARIDAD_CREDITO_HIPOTECARIO:
				case Constants.Channels.PCODE_CONSULTA_TITULARIDAD_TARJETA_CREDITO:
				case Constants.Channels.PCODE_CONSULTA_TITULARIDAD_CREDITO_ROTATIVO:
				case Constants.Channels.PCODE_CONSULTA_TITULARIDAD_OTROS_CREDITOS:
				case Constants.Channels.PCODE_CONSULTA_TITULARIDAD_CREDITO_MOTO_VEHICULO:
					field3 = Iso8583Post.TranType._32_GENERAL_INQUIRY + fromAccount + toAccount;
					break;
				default:
					field3 = procCode.toString();
					break;
				}
			} catch (XPostilion e) {
				EventReporter.reportGeneralEvent(this.nameInterface, Super.class.getName(), e,
						msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "transformProcessingCodeForAutra",
						this.udpClient);
			}
		}
		return field3;
	}

	protected static void disconnect(CallableStatement cst, CallableStatement stmt, Connection con, ResultSet rs) {
		try {
			if (con != null) {
				con.close();
			}
			if (stmt != null) {
				stmt.close();
			}
			if (cst != null) {
				cst.close();
			}
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			EventRecorder.recordEvent(new SQLExceptionEvent(
					new String[] { "Unknown", "Closing objects to database Connection", e.getMessage() }));

		}
	}

	public static void chooseCodeForNoAccount(String pCode, Super objectValidations) {

		switch (pCode.substring(2, 4)) {
		case "20":
			objectValidations.modifyAttributes(false, "NO CHECK ACCOUNT", "0852", RspCode._52_NO_CHEQUING_ACCOUNT);
			break;
		case "10":
			objectValidations.modifyAttributes(false, "NO SAVINGS ACCOUNT", "9956", RspCode._53_NO_SAVINGS_ACCOUNT);
			break;
		default:
			objectValidations.modifyAttributes(false, "Cuenta No Inscrita", "9956", RspCode._53_NO_SAVINGS_ACCOUNT);
			break;
		}
	}

	public abstract void validations(Base24Ath msg, Super objectValidations);

//  compensationDateValidationP17ToP15
//	public static void compensationDateValidation(int field, String fecha, Super objectValidations) {
//		try {
//
//			BusinessCalendar objectBusinessCalendar = new BusinessCalendar(ConstantsSuper.BUSINESS_CALENDAR);
//			
//			if (((!objectBusinessCalendar.isBusinessDay(new Date())) || (objectBusinessCalendar.isHoliday(new Date())))
//					&& (!fecha.equals(new SimpleDateFormat(ConstantsSuper.DATE_FORMAT_MMDD)
//							.format(objectBusinessCalendar.getCurrentBusinessDate())))) {
//
//				objectValidations.modifyAttributes(false, "ERROR P" + field + " FECHA INADECUADA", "0001", "30");
//			}
//			else if(objectBusinessCalendar.isBusinessDay(new Date()) &&  (!fecha.equals(new SimpleDateFormat(ConstantsSuper.DATE_FORMAT_MMDD)
//					.format(objectBusinessCalendar.getCurrentBusinessDate()))) && (!fecha.equals(new SimpleDateFormat(ConstantsSuper.DATE_FORMAT_MMDD)
//							.format(objectBusinessCalendar.getNextBusinessDate())))
//					 )	{	
//				
//				objectValidations.modifyAttributes(false, "ERROR P" + field + " FECHA INADECUADA", "0001", "30");
//					
//			}
//				
//		} catch (Exception e) {
//
//			EventRecorder.recordEvent(new TryCatchException(
//					new String[] { Constants.Config.NAME, "Method: compensationDateValidation", e.getMessage() }));
//			EventRecorder.recordEvent(e);
//
//		}
//
//	}

	public static final class ConstantsSuper {

		public static final String BUSINESS_CALENDAR = "DefaultBusinessCalendar";
		public static final String DATE_FORMAT_MMDD = "MMdd";

	}

}