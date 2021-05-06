package postilion.realtime.genericinterface.channels;

import java.util.HashMap;

import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.eventrecorder.events.TryCatchException;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.genericinterface.translate.validations.Validation.ErrorMessages;
import postilion.realtime.library.common.util.constants.General;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.XFieldUnableToConstruct;
import postilion.realtime.sdk.util.XPostilion;

public class CBCO extends Super {

	private Client udpClient = null;
	private String nameInterface = "";

	public CBCO(Boolean validationResult, String descriptionError, String errorCodeAUTRA, String errorCodeISO,
			HashMap<String, String> inforCollectedForStructData, Parameters params) {
		super(validationResult, descriptionError, errorCodeAUTRA, errorCodeISO, inforCollectedForStructData, params);
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
	}

	public void validations(Base24Ath msg, Super objectValidations) {

		try {
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validations Credibanco: " , "LOG", this.nameInterface));
			objectValidations.putInforCollectedForStructData("CHANNEL",
					"3");
			switch (msg.getMsgType()) {
			

			case Iso8583.MsgType._0200_TRAN_REQ://512
			case Iso8583.MsgType._0420_ACQUIRER_REV_ADV://1056

				objectValidations.putInforCollectedForStructData("CARD_NUMBER", msg.getTrack2Data().getPan());
				switch (msg.getProcessingCode().toString()) {
				
				case Constants.Channels.PCODE_RETIRO_ATM_A:// ***
				case Constants.Channels.PCODE_RETIRO_ATM_C:// ***
					
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CBCO_RETIRO");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "20");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");
					
					
					accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							msg.getProcessingCode().toString(), msg.getTrack2Data().getPan(),
							msg.getProcessingCode().getFromAccount(), msg.getTrack2Data().getExpiryDate(),
							(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
									? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									: " 1234567",
							objectValidations);
					
					
//					objectValidations.putInforCollectedForStructData(
//							"Indicador_De_Aceptacion_O_De_No_Preaprobado",
//							getIndicador_De_Aceptacion_O_De_No_Preaprobado(msg));
					
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE")
									.equals("10")) ? "05" : "04");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE")
									.equals("10")) ? "AHO" : "CTE");
					
//					if (objectValidations.getValidationResult()) {
//						validateField126(msg.getField(Constants.Fields.TOKEN126),
//								msg.getField(Iso8583.Bit._003_PROCESSING_CODE), objectValidations);
//					}
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
					objectValidations.putInforCollectedForStructData("CREDIBANCO",
							"CREDIBANCO");
					
					
					break;
					
				case Constants.Channels.PCODE_COMPRA_CREDIBANCO_A:// ***
				case Constants.Channels.PCODE_COMPRA_CREDIBANCO_C:// ***
					
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CBCO_COMPRA");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "25");
					objectValidations.putInforCollectedForStructData("Codigo_FI_Origen", "1006");
					objectValidations.putInforCollectedForStructData("Nombre_FI_Origen", "ASC");
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "COMPRA");
					
					
					accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							msg.getProcessingCode().toString(), msg.getTrack2Data().getPan(),
							msg.getProcessingCode().getFromAccount(), msg.getTrack2Data().getExpiryDate(),
							(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
									? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									: " 1234567",
							objectValidations);
					
					
					
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE")
									.equals("10")) ? "05" : "04");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE")
									.equals("10")) ? "AHO" : "CTE");
					
//					if (objectValidations.getValidationResult()) {
//						validateField126(msg.getField(Constants.Fields.TOKEN126),
//								msg.getField(Iso8583.Bit._003_PROCESSING_CODE), objectValidations);
//					}
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
					objectValidations.putInforCollectedForStructData("CREDIBANCO",
							"CREDIBANCO");
					
					break;
					
				case Constants.Channels.PCODE_ANULACION_COMPRA_CREDIBANCO_A:// ***
				case Constants.Channels.PCODE_ANULACION_COMPRA_CREDIBANCO_C:// ***
					
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CBCO_ANULACION_COMPRA");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "10");
					objectValidations.putInforCollectedForStructData("Codigo_FI_Origen", "1006");
					objectValidations.putInforCollectedForStructData("Nombre_FI_Origen", "ASC");
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "REFUND");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");
					accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							msg.getProcessingCode().toString(), msg.getTrack2Data().getPan(),
							msg.getProcessingCode().getFromAccount(), msg.getTrack2Data().getExpiryDate(),
							(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
									? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									: " 1234567",
							objectValidations);
//					if (objectValidations.getValidationResult()) {
//						validateField126(msg.getField(Constants.Fields.TOKEN126),
//								msg.getField(Iso8583.Bit._003_PROCESSING_CODE), objectValidations);
//					}
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
					objectValidations.putInforCollectedForStructData("CREDIBANCO",
							"CREDIBANCO");
					
					break;
				

				case Constants.Channels.PCODE_CONSULTA_DE_SALDO_ATM_AHORROS:
				case Constants.Channels.PCODE_CONSULTA_DE_SALDO_ATM_CORRIENTE:
				case Constants.Channels.PCODE_CONSULTA_5ULTIMOS_MOVIMIENTOS_ATM_A:
				case Constants.Channels.PCODE_CONSULTA_5ULTIMOS_MOVIMIENTOS_ATM_C:
					
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");
					
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "CONSUL");
					if (msg.getProcessingCode().toString().equals("381000")
							|| msg.getProcessingCode().toString().equals("382000")) {

						objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
								"CBCO_CONSULTA_5MOVIMIENTOS");
						objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "63");
					} else {
						objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CBCO_CONSULTA_DE_SALDO");
						objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "60");
					}
					
					
					accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							msg.getProcessingCode().toString(), msg.getTrack2Data().getPan(),
							msg.getProcessingCode().getFromAccount(), msg.getTrack2Data().getExpiryDate(),
							msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1) ? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									: Constants.General.SIXTEEN_ZEROS,
							objectValidations);
					
					
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
									: "04");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "AHO"
									: "CTE");
					

					String account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"cuenta trajo sp: " + account, "LOG", this.nameInterface));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
					objectValidations.putInforCollectedForStructData("CREDIBANCO",
							"CREDIBANCO");
					break;
	
					
					
				default:
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"CBCO_DEFAULT");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");
					accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							msg.getProcessingCode().toString(), msg.getTrack2Data().getPan(),
							msg.getProcessingCode().getFromAccount(), msg.getTrack2Data().getExpiryDate(),
							(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
									? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									: " 1234567",
							objectValidations);
//					if (objectValidations.getValidationResult()) {
//						validateField126(msg.getField(Constants.Fields.TOKEN126),
//								msg.getField(Iso8583.Bit._003_PROCESSING_CODE), objectValidations);
//					}
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
					objectValidations.putInforCollectedForStructData("CREDIBANCO",
							"CREDIBANCO");
					break;
				}
				break;

			case Iso8583.MsgType._0220_TRAN_ADV://544
				validateField126(msg.getField(Constants.Fields.TOKEN126),
						msg.getField(Iso8583.Bit._003_PROCESSING_CODE), objectValidations);
				break;
			case Iso8583.MsgType._0430_ACQUIRER_REV_ADV_RSP:
				break;

			default:
				break;
			}

		} catch (XFieldUnableToConstruct e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Valida el monto que viene en campo 54
	 *
	 * @param msgFromRemote mensaje recibido desde el remoote
	 * @return Arreglo de String indicando si el monto es valido y si no lo es una
	 *         descripción
	 * @throws XPostilion al intentar obtner los valores de los campos del mensaje.
	 */
	public void isValidAdditionalAmmount(Base24Ath msgFromRemote, Super objectValidations) throws XPostilion {
		try {
			String field4 = msgFromRemote.getField(Iso8583.Bit._004_AMOUNT_TRANSACTION);
			String field48 = msgFromRemote.getField(Iso8583.Bit._048_ADDITIONAL_DATA);
			String field54 = msgFromRemote.getField(Iso8583.Bit._054_ADDITIONAL_AMOUNTS);
			String indicadorMontos = field48.substring(field48.length() - General.UNO, field48.length());
			String montoTransaccion = field54.substring(Indexes.FIELD54_POSITION_0, Indexes.FIELD54_POSITION_12);
			String montoDonacion = field54.substring(Indexes.FIELD54_POSITION_12, Indexes.FIELD54_POSITION_24);
			String montoSeguro = field54.substring(Indexes.FIELD54_POSITION_24, Indexes.FIELD54_POSITION_36);

			if ((indicadorMontos.equals(Indexes.STRING_DOS) || indicadorMontos.equals(Indexes.STRING_CINCO)
					|| indicadorMontos.equals(Indexes.STRING_SEIS) || indicadorMontos.equals(Indexes.STRING_SIETE))
					&& (!field4.equals(montoTransaccion) || !montoDonacion.equals(Constants.General.TWELVE_ZEROS)
							|| !montoSeguro.equals(Constants.General.TWELVE_ZEROS))) {

				objectValidations.modifyAttributes(false, ErrorMessages.ERROR_AMOUNTS, ErrorMessages.INVALID_AMOUNTS,
						Constants.Config.CODE_ERROR_12);

			} else if (indicadorMontos.equals(Constants.General.STRING_UNO)) {

				insuranceAmmountsValidation(msgFromRemote, Integer.parseInt(montoTransaccion),
						Integer.parseInt(montoDonacion), Integer.parseInt(montoSeguro), Integer.parseInt(field4),
						objectValidations);
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, CBCO.class.getName(),
					"Method: [isValidAdditionalAmmount]", Utils.getStringMessageException(e),
					msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: isValidAdditionalAmmount " + e.getMessage(), "LOG", this.nameInterface));
		}
//		return new ValidatedResult(true, General.VOIDSTRING, General.VOIDSTRING, null);
	}

	/**
	 * Validate the field 54 ammounts if transaction indicate that client could be
	 * bought an insurance
	 *
	 * @param transactionAmmount ammount that client want use indeed
	 * @param donationAmmount    ammount that client has chose to donate
	 * @param insuranceAmmount   ammount that indicate the price of insurance
	 * @param field004Ammount    ammount specified in field 004
	 * @return DOT object AdditionalAmmountValidated
	 */
	public void insuranceAmmountsValidation(Base24Ath msg, int transactionAmmount, int donationAmmount,
			int insuranceAmmount, int field004Ammount, Super objectValidations) {
		try {
			int total = transactionAmmount + donationAmmount;
			boolean montoTotal = field004Ammount != total;
			boolean montoseguro = (insuranceAmmount == 0);
			if (field004Ammount % 1000000 == 0) {
				if (field004Ammount != transactionAmmount || (donationAmmount != 0) || montoseguro) {

					objectValidations.modifyAttributes(false, ErrorMessages.ERROR_INSURANCE_AMOUNTS,
							ErrorMessages.INVALID_AMOUNTS, Constants.Config.CODE_ERROR_12);

				}
			} else if (montoTotal || (donationAmmount == 0) || montoseguro) {

				objectValidations.modifyAttributes(false, ErrorMessages.ERROR_DONATION_AMOUNTS,
						ErrorMessages.INVALID_AMOUNTS, Constants.Config.CODE_ERROR_30);
			}
		} catch (Exception e) {
			try {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, CBCO.class.getName(),
						"Method: [insuranceAmmountsValidation]", Utils.getStringMessageException(e),
						msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: insuranceAmmountsValidation " + e.getMessage(), "LOG",
						this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, CBCO.class.getName(),
						"Method: [insuranceAmmountsValidation]", Utils.getStringMessageException(e), "Unknown" }));

			}
		}
	}

	/**
	 * Valida los valores del campo 126.
	 *
	 * @param value valor del campo 126.
	 * 
	 */
	public static void validateField126(String value, String field3, Super objectValidations) {
		int counter = Integer.parseInt(value.substring(Indexes.FIELD126_POSITION_2, Indexes.FIELD126_POSITION_7));
		if (counter < Indexes.MINIMUM_NUMBER_TOKENS) {

			objectValidations.modifyAttributes(false, ErrorMessages.FIELD_126_TOKENS_ERROR,
					ErrorMessages.FORMAT_ERROR_CODE,
					postilion.realtime.genericinterface.translate.util.Constants.Config.CODE_ERROR_30);

		}
		counter = Integer.parseInt(value.substring(Indexes.FIELD126_POSITION_16, Indexes.FIELD126_POSITION_21));
		if (counter < Indexes.MINIMUM_LENGTH_TOKEN_QT) {

			objectValidations.modifyAttributes(false, ErrorMessages.FIELD_126_LENGTH_TOKEN_QT_ERROR,
					ErrorMessages.FORMAT_ERROR_CODE,
					postilion.realtime.genericinterface.translate.util.Constants.Config.CODE_ERROR_30);
		}

		if (!field3.equals(value.substring(Indexes.FIELD126_POSITION_22, Indexes.FIELD126_POSITION_28))) {

			objectValidations.modifyAttributes(false, ErrorMessages.FIELD_126_PROCESSING_CODE_ERROR,
					ErrorMessages.FORMAT_ERROR_CODE,
					postilion.realtime.genericinterface.translate.util.Constants.Config.CODE_ERROR_30);
		}
	}

	/**
	 * Valida los valores del campo 126.
	 *
	 * @param value valor del campo 126.
	 * @return Hashmap indicando true si la validación fue existosa, false de lo
	 *         contrario y agrega descripción del error.
	 */
	public static void validateField126C(String value, String field37, Super objectValidations) {

		int counter = Integer.parseInt(value.substring(Indexes.FIELD126_POSITION_2, Indexes.FIELD126_POSITION_7));

		if (counter < Indexes.MINIMUM_NUMBER_TOKENS) {

			objectValidations.modifyAttributes(false, ErrorMessages.FIELD_126_TOKENS_ERROR,
					ErrorMessages.FORMAT_ERROR_CODE,
					postilion.realtime.genericinterface.translate.util.Constants.Config.CODE_ERROR_30);

		}

		counter = Integer.parseInt(value.substring(Indexes.FIELD126_POSITION_16, Indexes.FIELD126_POSITION_21));

		if (counter < Indexes.MINIMUM_LENGTH_TOKEN_QT) {

			objectValidations.modifyAttributes(false, ErrorMessages.FIELD_126_LENGTH_TOKEN_QT_ERROR,
					ErrorMessages.FORMAT_ERROR_CODE,
					postilion.realtime.genericinterface.translate.util.Constants.Config.CODE_ERROR_30);
		}

		if (!field37.equals(value.substring(Indexes.FIELD126_POSITION_28, Indexes.FIELD126_POSITION_40))) {

			objectValidations.modifyAttributes(false, ErrorMessages.FIELD_126_RETRIEVAL_REFERENCE_ERROR,
					ErrorMessages.FORMAT_ERROR_CODE,
					postilion.realtime.genericinterface.translate.util.Constants.Config.CODE_ERROR_30);
		}
	}

	/**
	 * 
	 * @param p4
	 * @param p48
	 * @return
	 */
	public void validatefield4CostInquiry(String p4, String p48, String p54, Super objectValidation) {
		try {

			String indicadorMontos = p48.substring(p48.length() - General.UNO, p48.length());
			String montoTransaccion = p54.substring(Indexes.FIELD54_POSITION_0, Indexes.FIELD54_POSITION_12);
			String montoDonacion = p54.substring(Indexes.FIELD54_POSITION_12, Indexes.FIELD54_POSITION_24);
			String montoSeguro = p54.substring(Indexes.FIELD54_POSITION_24, Indexes.FIELD54_POSITION_36);

			if (indicadorMontos.equals(postilion.realtime.genericinterface.translate.util.Constants.General.STRING_CERO)
					|| indicadorMontos
							.equals(postilion.realtime.genericinterface.translate.util.Constants.General.STRING_DOS)) {
				int field4Ammount = Integer.parseInt(p4);
				if (field4Ammount % 1000000 == 0) {
					if (!p4.equals(montoTransaccion)
							|| !montoDonacion.equals(
									postilion.realtime.genericinterface.translate.util.Constants.General.TWELVE_ZEROS)
							|| !montoSeguro.equals(
									postilion.realtime.genericinterface.translate.util.Constants.General.TWELVE_ZEROS)) {

						objectValidation.modifyAttributes(false,
								"Ammounts in fields 4 and 54 first part should be the same and parts 2 and 3 of field 54 should be zero",
								"1994", "30");
					}
				} else {

					objectValidation.modifyAttributes(false, "Ammount in field 4 should be multiple of 10000", "1994",
							"30");
				}
			} else if (indicadorMontos
					.equals(postilion.realtime.genericinterface.translate.util.Constants.General.STRING_UNO)) {

				insuranceAmmountsValidationForCostInquiry(Integer.parseInt(montoTransaccion),
						Integer.parseInt(montoDonacion), Integer.parseInt(montoSeguro), Integer.parseInt(p4),
						objectValidation);
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, CBCO.class.getName(),
					"Method: [validatefield4CostInquiry]", Utils.getStringMessageException(e),
					"Unknown" }));
			EventRecorder.recordEvent(e);
		}
	}

	/**
	 * Validate the field 54 ammounts if cost inquiry transaction indicate that
	 * client could be bought an insurance
	 * 
	 * @param transactionAmmount ammount that client want use indeed
	 * @param donationAmmount    ammount that client has chose to donate
	 * @param insuranceAmmount   ammount that indicate the price of insurance
	 * @param field004Ammount    ammount specified in field 004
	 * @return DOT object AdditionalAmmountValidated
	 */
	public static void insuranceAmmountsValidationForCostInquiry(int transactionAmmount, int donationAmmount,
			int insuranceAmmount, int field004Ammount, Super objectValidation) {

		boolean montoTotal = field004Ammount != transactionAmmount;
		boolean montoseguro = (insuranceAmmount == 0);
		if (field004Ammount % 1000000 == 0) {
			if (montoTotal || (donationAmmount != 0) || montoseguro) {

				objectValidation.modifyAttributes(false,
						"Ammount of field 4 should be the same that field 54 part 1.  Also field 54 part 2 should be zero and part 3 should not be zero",
						"1994", "30");

			}
		} else {
			if (montoTotal || (donationAmmount != 0) || montoseguro) {

				objectValidation.modifyAttributes(false,
						"Ammount of fields 4 should be the same that field 54 part 1. Field 54 part 2 should be zero. Also field 54 part 3 should be different of zero",
						"1994", "30");
			} else {

				objectValidation.modifyAttributes(false, "Ammount of field 4 should be multiple of 10000", "1994",
						"30");
			}
		}
	}

	/**
	 * Constantes que indican indices de campos.
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public static final class Indexes {
		public static final int POSITION_0 = 0;
		public static final int POSITION_1 = 1;
		public static final int FIELD4_POSITION_0 = 0;
		public static final int FIELD4_POSITION_10 = 10;
		public static final int FIELD35_POSITION_6 = 6;
		public static final int FIELD35_POSITION_16 = 16;
		public static final int FIELD35_POSITION_17 = 17;
		public static final int FIELD35_POSITION_19 = 19;
		public static final int FIELD37_POSITION_4 = 4;
		public static final int FIELD37_POSITION_8 = 8;
		public static final int FIELD37_POSITION_12 = 12;
		public static final int FIELD41_POSITION_4 = 4;
		public static final int FIELD41_POSITION_6 = 6;
		public static final int FIELD41_POSITION_10 = 10;
		public static final int FIELD54_POSITION_0 = 0;
		public static final int FIELD54_POSITION_10 = 10;
		public static final int FIELD54_POSITION_12 = 12;
		public static final int FIELD54_POSITION_24 = 24;
		public static final int FIELD54_POSITION_36 = 36;
		public static final int FIELD126_POSITION_2 = 2;
		public static final int FIELD126_POSITION_7 = 7;
		public static final int FIELD126_POSITION_16 = 16;
		public static final int FIELD126_POSITION_21 = 21;
		public static final int FIELD126_POSITION_22 = 22;
		public static final int FIELD126_POSITION_28 = 28;
		public static final int FIELD126_POSITION_40 = 40;
		public static final String SEPARADOR = "_";
		public static final String VOID = "";
		public static final String FIELD = "Field ";
		public static final String ELEVEN_ZEROS = "00000000000";
		public static final String FOUR_ZEROS = "0000";
		public static final String TWO_ZEROS = "00";
		public static final String NB = "NB";
		public static final String _010 = "010";
		public static final String SEVEN = "7";
		public static final int ZERO_AMMOUNT = 0;
		public static final int FIELD_POSITION = 0;
		public static final int VALIDATION_TYPE = 0;
		public static final int RULE = 1;
		public static final int MINIMUM_EXPIRY_YEAR = 19;
		public static final int MINIMUM_NUMBER_TOKENS = 2;
		public static final int MINIMUM_LENGTH_TOKEN_QT = 32;
		public static final int FIELD_126 = 126;
		public static final int LENGTH_ZERO = 0;
		public static final int LENGTH_ONE = 1;
		public static final String STRING_UNO = "1";
		public static final String STRING_DOS = "2";
		public static final String STRING_CINCO = "5";
		public static final String STRING_SEIS = "6";
		public static final String STRING_SIETE = "7";

	}

}
