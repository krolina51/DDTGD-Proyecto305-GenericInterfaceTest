package postilion.realtime.genericinterface.translate.validations;

import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.EventReporter;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.genericinterface.translate.validations.Validation.ErrorMessages;
import postilion.realtime.library.common.util.constants.General;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.StructuredData;
import postilion.realtime.sdk.util.XPostilion;

public class ValidateCorrespondentMsg {

	private static String info = "";
	private static boolean approved = true;
	private static String codeError = Constants.Config.CODE_ERROR_30;

	private Client udpClient = null;
	private String nameInterface = "";

	public ValidateCorrespondentMsg(Parameters params) {
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
	}

	public ValidatedResult validationsCNB(Base24Ath msg) {
		String retRefNumber = "N/D";
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			if (!validationField22(msg))
				return new ValidatedResult(approved, info, ErrorMessages.INVALID_AMOUNTS, codeError);

			if (msg.isFieldSet(Constants.General.NUMBER_112)
					&& !msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE).equals(Constants.General.STRING_010)) {

				if (!validationCorrespondenCard(msg.getTrack2Data().getPan(),
						msg.getField(Constants.General.NUMBER_112)))
					return new ValidatedResult(approved, info, ErrorMessages.INVALID_AMOUNTS, codeError);
			}

		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, ValidateCorrespondentMsg.class.getName(), e,
					retRefNumber, "validationsCNB", this.udpClient);
		}

		return new ValidatedResult(true, General.VOIDSTRING, General.VOIDSTRING, null);
	}

	private static boolean validationCorrespondenCard(String pan, String p112) {

		if (pan.equals(p112)) {

			approved = false;
			info = Constants.Account.VALIDATE_FIELD_112;
			codeError = Constants.Config.CODE_ERROR_12;
			return false;
		}

		return true;
	}

	public static boolean validationField22(Base24Ath msg) throws XPostilion {

		if (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE).equals(Constants.General._010)) {
			if (!msg.getField(Iso8583.Bit._052_PIN_DATA).equals(Constants.General.PIN_DATA_VALIDATION)) {
				approved = false;
				info = Constants.Account.VALIDATE_FIELD_52;
				codeError = Constants.Config.CODE_ERROR_30;
				return false;
			}
		}
		return true;

	}

	public void getSDCorresponsalValidate(Iso8583 msg, StructuredData sd) {
		String retRefNumber = "N/D";
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			sd.put("DISPOSITIVO", "A");
			sd.put("T_TRANSACCION", "CORRESPONSAL");
			String AcquiringInst = msg.getField(Iso8583.Bit._032_ACQUIRING_INST_ID_CODE).substring(7);
			if (AcquiringInst.equals(Constants.General.STRING_054))
				sd.put(Constants.General.RED_ENTRADA, Constants.General.STRING_DOS);
			else
				sd.put(Constants.General.RED_ENTRADA, Constants.General.STRING_TRES);

			switch (AcquiringInst) {
			case "0001":
				sd.put("RED_ADQUIRIENTE", "01");
				break;
			case "0023":
				sd.put("RED_ADQUIRIENTE", "03");
				break;
			case "0056":
				sd.put("RED_ADQUIRIENTE", "02");
				break;
			case "0052":
				sd.put("RED_ADQUIRIENTE", "04");
				break;
			case "0055":
				sd.put("RED_ADQUIRIENTE", "07");
				break;
			case "0002":
				sd.put("RED_ADQUIRIENTE", "09");
				break;

			default:
				sd.put("RED_ADQUIRIENTE", "99");
				break;
			}

			if (msg.getProcessingCode().getTranType().equals("89"))
				sd.put("INDICADOR_TRANSACCION", "I");
			else
				sd.put("INDICADOR_TRANSACCION", "M");

		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, ValidateCorrespondentMsg.class.getName(), e,
					retRefNumber, "getSDCorresponsalValidate", this.udpClient);
		}

	}

}
