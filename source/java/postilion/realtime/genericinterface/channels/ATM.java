package postilion.realtime.genericinterface.channels;

import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import postilion.realtime.genericinterface.GenericInterface;
import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.eventrecorder.events.TryCatchException;
import postilion.realtime.genericinterface.extract.Extract;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.genericinterface.translate.validations.Validation.ErrorMessages;
import postilion.realtime.library.common.util.constants.General;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.ipc.XEncryptionKeyError;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.message.bitmap.PosEntryMode;
import postilion.realtime.sdk.message.bitmap.ProcessingCode;
import postilion.realtime.sdk.message.bitmap.XFieldUnableToConstruct;
import postilion.realtime.sdk.util.XPostilion;
import postilion.realtime.sdk.util.convert.Pack;

public class ATM extends Super {

	private Map<String, String> cutValues = new HashMap<>();
	private Client udpClient = null;
	private String nameInterface = "";
	private boolean encodeData = false;

	public ATM(Boolean validationResult, String descriptionError, String errorCodeAUTRA, String errorCodeISO,
			HashMap<String, String> inforCollectedForStructData, Parameters params) {
		super(validationResult, descriptionError, errorCodeAUTRA, errorCodeISO, inforCollectedForStructData, params);
		this.cutValues = params.getCutValues();
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
		this.encodeData = params.isEncodeData();
	}

	public void validations(Base24Ath msg, Super objectValidations) {

		try {

			objectValidations.putInforCollectedForStructData("CHANNEL", "ATM");
			objectValidations.putInforCollectedForStructData("Identificacion_Canal", "AT");
			objectValidations.putInforCollectedForStructData("Canal", "01");

			switch (msg.getMsgType()) {

			case Iso8583.MsgType._0200_TRAN_REQ:
			case Iso8583.MsgType._0420_ACQUIRER_REV_ADV:

				objectValidations.putInforCollectedForStructData("CARD_NUMBER", msg.getTrack2Data().getPan());
				Pattern pattern = null;
				Matcher matcher = null;
				String account2 = null;
				switch (msg.getProcessingCode().toString()) {

				case Constants.Channels.PCODE_RETIRO_ATM_A:// ***
				case Constants.Channels.PCODE_RETIRO_ATM_C:// ***

					String strCut = createfieldCut(msg);
					objectValidations.putInforCollectedForStructData("CUT_origen_de_la_transaccion", strCut);
					objectValidations.putInforCollectedForStructData("CUT_propio_de_la_transaccion", strCut);
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");

					switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
					case "051":
						objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_RETIRO");

						this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
								"Entro validacion ATM RETIRO", "LOG", this.nameInterface));

						accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
								msg.getProcessingCode().toString(), msg.getTrack2Data().getPan(),
								msg.getProcessingCode().getFromAccount(), msg.getTrack2Data().getExpiryDate(),
								(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
										? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
										: " 1234567",
								objectValidations);

						if (objectValidations.getValidationResult()) {
							account2 = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
							this.udpClient.sendData(Client.getMsgKeyValue(
									msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "cuenta trajo sp: " + account2
											+ " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
									"LOG", this.nameInterface));

							pattern = Pattern
									.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
							matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

							if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
									&& !account2.equals(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1))
									&& !matcher.matches()) {
								objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
							} else if (matcher.matches()) {
								msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
										(this.encodeData) ? new String(Base64.getDecoder().decode(account2))
												: account2);
							}
							GenericInterface.getLogger().logLine("Field 102 b24 " + msg.getField(102));
							GenericInterface.getLogger().logLine("CLIENT_ACCOUNT_NR "
									+ objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));

							if (objectValidations.getValidationResult()) {

								isValidAdditionalAmmount(msg, objectValidations);

								if (objectValidations.getValidationResult()
										&& (msg.getMsgType() == Iso8583.MsgType._0200_TRAN_REQ)) {

									validateField126(msg.getField(Constants.Fields.TOKEN126),
											msg.getField(Iso8583.Bit._003_PROCESSING_CODE), objectValidations);

								}
							}

							objectValidations.putInforCollectedForStructData(
									"Indicador_De_Aceptacion_O_De_No_Preaprobado",
									getIndicador_De_Aceptacion_O_De_No_Preaprobado(msg));
							objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
									(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE")
											.equals("10")) ? "05" : "04");
							objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
									(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE")
											.equals("10")) ? "AHO" : "CTE");
							objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "20");
							objectValidations.putInforCollectedForStructData("Entidad",
									(msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA))
											? msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(0, 4)
											: "0001");

							objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
									objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
							objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
									objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
							objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
									objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
							objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
									objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
						}

						break;
					default:

						switch (msg.getTrack2Data().getPan().substring(0, 6)) {
						case "777791":
							objectValidations.putInforCollectedForStructData(
									"Indicador_De_Aceptacion_O_De_No_Preaprobado",
									getIndicador_De_Aceptacion_O_De_No_Preaprobado(msg));
							objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_RETIRO_SIN_TD");
							objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "20");

							break;

						default:
							objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
									"ATM_COBRO_GIROS_TRADICIONAL_O_WEB_SERVICES");
							objectValidations.putInforCollectedForStructData("Identificador_Terminal", "N");

							break;
						}
						objectValidations.putInforCollectedForStructData("Indicador_De_Aceptacion_O_De_No_Preaprobado",
								getIndicador_De_Aceptacion_O_De_No_Preaprobado(msg));

						objectValidations.putInforCollectedForStructData("Identificador_Terminal",
								msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(12, 13));
						objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "20");
						objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
								(msg.getProcessingCode().getFromAccount().equals("10")) ? "05" : "04");
						objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
								(msg.getProcessingCode().getFromAccount().equals("10")) ? "AHO" : "CTE");
						objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR",
								msg.getTrack2Data().getPan());
						objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
								msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
						objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "00");
						objectValidations.putInforCollectedForStructData("CUSTOMER_ID",
								msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION));

						objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
								msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
						objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
								msg.getField(Iso8583.Bit._003_PROCESSING_CODE).substring(2, 4));

						objectValidations.putInforCollectedForStructData("P_CODE", "000000");
						break;
					}

					break;
				// CONSULTA DE COSTO ATM.
				case Constants.Channels.PCODE_CONSULTA_DE_COSTO_ATM:// 322000 - 321000 del 126 //***

					ProcessingCode pcode = new ProcessingCode(msg.getField(126).substring(22, 28));
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "ATM_CONSULTA_DE_COSTO");
					objectValidations.putInforCollectedForStructData("Migrada", "3");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
							(pcode.getFromAccount().equals("10")) ? "05" : "04");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
							(pcode.getFromAccount().equals("10")) ? "AHO" : "CTE");

					if (msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA)) {
						objectValidations.putInforCollectedForStructData("INCOCREDITO",
								msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(36));
						objectValidations.putInforCollectedForStructData("Terminal_Ampliada",
								msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(4, 12));
						objectValidations.putInforCollectedForStructData("Identificador_Terminal",
								msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(12, 13));
					}

//					if (objectValidations.getValidationResult())
					consultaDeCostoAtm(objectValidations, msg, pattern, matcher, account2);

					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "CONSUL");
					objectValidations.putInforCollectedForStructData("Nombre_TransaccionX1", "CONSUL");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "65");
					objectValidations.putInforCollectedForStructData("Codigo_TransaccionX1", "65");
					objectValidations.putInforCollectedForStructData("FI_CREDITO", "0000");
					objectValidations.putInforCollectedForStructData("FI_DEBITO", "0000");

					break;
				case Constants.Channels.PCODE_CONSULTA_DE_SALDO_ATM_AHORROS:// ***
				case Constants.Channels.PCODE_CONSULTA_DE_SALDO_ATM_CORRIENTE:// ***
				case Constants.Channels.PCODE_CONSULTA_5ULTIMOS_MOVIMIENTOS_ATM_A:// ***
				case Constants.Channels.PCODE_CONSULTA_5ULTIMOS_MOVIMIENTOS_ATM_C:// ***

					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "CONSUL");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");
					if (msg.getProcessingCode().toString().equals("381000")
							|| msg.getProcessingCode().toString().equals("382000")) {

						objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
								"ATM_CONSULTA_5MOVIMIENTOS");
						objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "63");
					} else {
						objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_CONSULTA_DE_SALDO");
						objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "60");
					}
					consultaSaldoY5Mov(msg, objectValidations, msg.getProcessingCode());

					break;
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_AHORROS:// *
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_CORRIENTE:// *
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS:// *
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE:// *
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS:// *
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE:// *
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_EFECTIVO:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_CHEQUE:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_EFECTIVO:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_CHEQUE:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_EFECTIVO:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_CHEQUE:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_EFECTIVO:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CHEQUE:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_AHORROS:// ***
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_CORRIENTE:// ***

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_PAGO_OBLIGACIONES");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
							"ATM_POBLIG_" + tagTTypePOblig(msg, objectValidations));

					switch (msg.getProcessingCode().toString()) {
					case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS:// *
					case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE:// *

						pagoObligacionesAtmTC(msg, objectValidations);

						break;

					default:
						pagoObligacionesAtm(msg, objectValidations);
						break;
					}

					break;

				case Constants.Channels.PCODE_PAGO_DE_SERVICIOS_ATM_AHO:// ***
				case Constants.Channels.PCODE_PAGO_DE_SERVICIOS_ATM_COR:// ***

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"ATM_PAGO_SERVICIOS_PUBLICOS");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V3");
					objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP", "ATM_PSP");
					processingRequestPSP(objectValidations, msg);

					break;
				case Constants.Channels.PCODE_CONSULTA_CUPO_CREDITO_ROTATIVO_ATM:// ***

					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "60");
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "CONSUL");
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"ATM_CONSULTA_CUPO_CREDITO_ROTATIVO");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");
					processingRequestInquiryCreditLimit(objectValidations, msg);

					break;

				case Constants.Channels.PCODE_DEPOSITO_ATM_MULTIFUNCIONAL_AHO:// 27-21 //***
				case Constants.Channels.PCODE_DEPOSITO_ATM_MULTIFUNCIONAL_COR:// 27-21//***

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"ATM_MULTIFUNCIONAL_DEPOSITO");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
							"ATM_MULTIFUNCIONAL_DEPOSITO");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

					objectValidations.putInforCollectedForStructData("B24_Field_104",
							msg.isFieldSet(Iso8583.Bit._104_TRAN_DESCRIPTION)
									? msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION)
									: Constants.General.DEFAULT_104);

					tagsExtractDepositoAtmMultifuncional(objectValidations, msg);

					objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8, 23));
					break;
				case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_AHORROS:// ***
				case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_CORRIENTE:// ***ok
				case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_AHORROS:// ***
				case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_CORRIENTE:// ***

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_TRANSFERENCIA");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "ATM_TRANSFERENCIAS");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

					validationCardsAccountMixtaDebitCreditTransferATM(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

					break;
				case Constants.Channels.PCODE_PAGO_CREDITO_HIPOTECARIO_EFECTIVO_ATM_MULTIFUNCIONAL:// 27 - 51 //*** NO
																									// LANZARONnnnnnnnn
				case Constants.Channels.PCODE_PAGO_CREDITO_ROTATIVO_EFECTIVO_ATM_MULTIFUNCIONAL:// 27 - 51 //***
				case Constants.Channels.PCODE_PAGO_OTROS_CREDITOS_EFECTIVO_ATM_MULTIFUNCIONAL:// 27 - 51 //***

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"ATM_MULTIFUNCIONAL_PAGO_CREDITOS_EFECTIVO");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
							"ATM_MULTIFUNCIONAL_PAGO_CREDITOS_EFECTIVO");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

					tagsPagoCreditosEfectivoMultifuncional(objectValidations, msg);

					break;

				case Constants.Channels.PCODE_ACTIVACION_TOKEN:

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_ACTIVACION_TOKEN");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");
					objectValidations.putInforCollectedForStructData("P_CODE",
							msg.getField(Iso8583.Bit._003_PROCESSING_CODE));

					accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							msg.getProcessingCode().toString(), msg.getTrack2Data().getPan(),
							msg.getProcessingCode().getFromAccount(), msg.getTrack2Data().getExpiryDate(),
							(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
									? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									: " 1234567",
							objectValidations);

					objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
							msg.getTrack2Data().getPan().substring(6));
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "55");
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRNSAD");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "01");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "OTR");

					break;

				case Constants.Channels.PCODE_UTILIZACION_CREDITO_ROTATIVO_AHO:// ***
				case Constants.Channels.PCODE_UTILIZACION_CREDITO_ROTATIVO_COR:// ***
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"ATM_UTILIZACION_CREDITO_ROTATIVO");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
							"ATM_UTILIZACION_CREDITO_ROTATIVO");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

					atmUtilizacionCreditoRotativoTD(objectValidations, msg);

					break;
				default:
					break;
				}
				break;

			case Iso8583.MsgType._0220_TRAN_ADV:

				if (msg.getProcessingCode().toString().equals("321000")
						|| msg.getProcessingCode().toString().equals("322000")) {

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"ATM_0220_CONSULTA_CUENTAS_RELACIONADAS");
					tagsEncodeSensitiveData("CLIENT_CARD_NR", msg.getTrack2Data().getPan(), objectValidations);
					objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
							(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
									? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									: "000000000000000000");
					objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE",
							msg.getProcessingCode().getFromAccount());

				}

				if (msg.getProcessingCode().getTranType().equals("01")) {

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_0220");

					accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							msg.getProcessingCode().toString(), msg.getTrack2Data().getPan(),
							msg.getProcessingCode().getFromAccount(), msg.getTrack2Data().getExpiryDate(),
							msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1) ? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									: Constants.General.SIXTEEN_ZEROS,
							objectValidations, false);
//				}

					switch (msg.getProcessingCode().toString()) {
					case Constants.Channels.PCODE_CONSULTA_DE_COSTO_ATM:
						validateField126C(msg.getField(Constants.Fields.TOKEN126),
								msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), objectValidations);
						break;
					case Constants.Channels.PCODE_ACTIVACION_TOKEN:
						objectValidations.putInforCollectedForStructData("P_CODE",
								msg.getField(Iso8583.Bit._003_PROCESSING_CODE));
						break;
					default:
						validateField126(msg.getField(Constants.Fields.TOKEN126),
								msg.getField(Iso8583.Bit._003_PROCESSING_CODE), objectValidations);
						break;
					}

					if (!objectValidations.getValidationResult()) {

						tagsEncodeSensitiveData("CLIENT_CARD_NR", msg.getTrack2Data().getPan(), objectValidations);
						objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
								(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
										? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
										: "000000000000000000");
						objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE",
								msg.getProcessingCode().getFromAccount());
						objectValidations.putInforCollectedForStructData("CUSTOMER_NAME",
								"0220 ATH " + objectValidations.getDescriptionError());

					}
				}
				break;

			case Iso8583.MsgType._0430_ACQUIRER_REV_ADV_RSP:
				break;

			default:
				break;
			}

		} catch (XFieldUnableToConstruct e) {
			try {
				EventRecorder.recordEvent(new TryCatchException(
						new String[] { this.nameInterface, ATM.class.getName(), "Method: [validations]",
								Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"XFieldUnableToConstruct in Method: validations " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
						"Method: [validations]", Utils.getStringMessageException(e), "Unknown" }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
						"XFieldUnableToConstruct in Method: validations " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			}
		} catch (Exception e) {
			try {
				EventRecorder.recordEvent(new TryCatchException(
						new String[] { this.nameInterface, ATM.class.getName(), "Method: [validations]",
								Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: validations " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
						"Method: [validations]", Utils.getStringMessageException(e), "Unknown" }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
						"Exception in Method: validations " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			}
		}

	}

	private void pagoObligacionesAtmTC(Base24Ath msg, Super objectValidations) throws Exception {

		switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
		case "021":
		case "010":

			Extract.tagsModelPaymentOfObligationsCredit(objectValidations, msg);

			// ****************************************************************************
			objectValidations.putInforCollectedForStructData("P_CODE", "000000");
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
			objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS", "15CLASE12NB");
			// ****************************************************************************

			break;
		case "051":

			validationCardsAccountMixtaDebitCreditPaymentObligationsWithCardATMTC(msg, objectValidations,
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

//			validationCardsAccountMixtaDebitCreditPaymentObligationsWithCardATM(msg, objectValidations,
//					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

			break;

		default:
			break;
		}

		objectValidations.putInforCollectedForStructData("Tipo_de_Tarjeta", "1");
		objectValidations.putInforCollectedForStructData("Dispositivo", "0");
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "CRE");
		objectValidations.putInforCollectedForStructData("Entidad", "0000");

	}

	private void validationCardsAccountMixtaDebitCreditPaymentObligationsWithCardATMTC(Base24Ath msg,
			Super objectValidations, String indicatorMixCreditDebidP103) throws Exception {

		Pattern pattern = null;
		Matcher matcher = null;

		String procCode = null;
		ProcessingCode pc = null;

		if (msg.getProcessingCode().toString().equals("890000")) {
			procCode = msg.getField(126).substring(22, 28);
			pc = new ProcessingCode(procCode);
		} else {
			procCode = msg.getField(3);
			pc = new ProcessingCode(procCode);
		}

		if (indicatorMixCreditDebidP103.equals("2")) {// mixta tx
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion pago de O 2.MIXTA", "LOG", this.nameInterface));

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), pc.toString(),
					msg.getTrack2Data().getPan(), pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			Extract.tagsModelPaymentOfObligationsMixed(objectValidations, msg);

			objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "PAGOCB");
			objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "CRE");
			objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE", "CRE");
			objectValidations.putInforCollectedForStructData("BIN_Cuenta",
					(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
							? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8, 14)
							: "000000");

			tagTTypePOblig(msg, objectValidations);

			// TAGS UNICOS
			// ****************************************************************************
			// TAGS UNICOS
			// ****************************************************************************

			// TAGS ISC
			// ******************************************************************************
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
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
			// TAGS ISC
			// ******************************************************************************

			String account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos primera y segunda validacion: mixta "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"cuenta trajo sp: " + account + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
					"LOG", this.nameInterface));

			pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
			matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
					&& !account.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))
					&& !matcher.matches()) {
				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE",
						msg.getProcessingCode().getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			} else if (matcher.matches()) {
				msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
						(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
			}

		} else if (indicatorMixCreditDebidP103.equals("1")) {
			// viene solo cuenta corresponsal. CREDITO

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion pago de servicios publicos 1.CREDITO", "LOG", this.nameInterface));

			Extract.tagsModelPaymentOfObligationsCredit(objectValidations, msg);

			objectValidations.putInforCollectedForStructData("P_CODE", "000000");
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));

		} else if (indicatorMixCreditDebidP103.equals("0")) {

			// viene solo cuenta cliente. DEBITO

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro pago de servicios tipo 0.DEBITO", "LOG", this.nameInterface));

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), pc.toString(),
					msg.getTrack2Data().getPan(), pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			String account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos primera y segunda validacion: mixta "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"cuenta trajo sp: " + account + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
					"LOG", this.nameInterface));

			String field102 = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
					.substring(msg.getFieldLength(Iso8583.Bit._102_ACCOUNT_ID_1) - 17);
			pattern = Pattern.compile("0{" + field102.length() + "}");
			matcher = pattern.matcher(field102);

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1) && !account.equals("0" + field102)
					&& !matcher.matches()) {
				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE",
						msg.getProcessingCode().getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			}

//			else if (matcher.matches()) {
//				msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
//						(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
//			}

			// TAGS EXTRACT
			// ****************************************************************************

//			switch ((msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3,7): "0001") {
			switch (msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7)) {

			case "0002":
			case "0052":
			case "0023":
				Extract.tagsModelPaymentOfObligationsDebit(objectValidations, msg);

				objectValidations.putInforCollectedForStructData("Dispositivo", "0");
				objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "LCR");
				objectValidations.putInforCollectedForStructData("Entidad", "0000");

				break;
			case "0001":

				Extract.tagsModelPaymentOfObligationsMixed(objectValidations, msg);
				objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "PAGOCB");
				objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "CRE");
				objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE", "CRE");
				objectValidations.putInforCollectedForStructData("BIN_Cuenta",
						(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
								? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8, 14)
								: "000000");

				tagTTypePOblig(msg, objectValidations);

				break;

			default:
				Extract.tagsModelPaymentOfObligationsDebit(objectValidations, msg);
				objectValidations.putInforCollectedForStructData("FI_CREDITO", "0014");
				objectValidations.putInforCollectedForStructData("Ent_Adq", "0054");
				objectValidations.putInforCollectedForStructData("Dispositivo", "0");
				objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "LCR");
				objectValidations.putInforCollectedForStructData("Entidad", "0000");

				break;
			}

			// TAGS EXTRACT
			// ****************************************************************************

			// TAGS ISC
			// *********************************************************************************
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
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
			// TAGS ISC
			// *********************************************************************************

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos cuentaCNB validacion: DEBITO  "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

		}

	}

	private void atmUtilizacionCreditoRotativoTD(Super objectValidations, Base24Ath msg)
			throws XFieldUnableToConstruct, XPostilion, Exception {
		ProcessingCode ps = null;
		String procCode = null;

		if (msg.getField(3).equals("890000")) {

			procCode = msg.getField(126).substring(22, 28);
			ps = new ProcessingCode(procCode);
		} else {
			procCode = msg.getField(3);
			ps = new ProcessingCode(procCode);
		}

		accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(), msg.getTrack2Data().getPan(),
				ps.getToAccount(), msg.getTrack2Data().getExpiryDate(),
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? "0" + msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2)
								.substring(msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).length() - 17)
						: " 1234567",
				objectValidations);

		// tener cuidado con los tipo de cuentas de servicio del credito *********

		Extract.tagsModelTransferDebit(objectValidations, msg);
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "38");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "06");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "OTR");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "0");
		objectValidations.putInforCollectedForStructData("Mod_Credito", "8");
		objectValidations.putInforCollectedForStructData("Entidad", "0000");
		objectValidations.putInforCollectedForStructData("Ent_Adq",
				msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
		objectValidations.putInforCollectedForStructData("Clase_Pago",
				msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(0, 1));

		objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
				msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
		objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
		objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
		objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));

		objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));

	}

	private void pagoObligacionesAtm(Base24Ath msg, Super objectValidations)
			throws XFieldUnableToConstruct, XEncryptionKeyError, XPostilion, Exception {

		switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
		case "021":
		case "010":

			Extract.tagsModelPaymentOfObligationsCredit(objectValidations, msg);

			// TAGS UNICOS
			// *********************************************************************************
//			objectValidations.putInforCollectedForStructData("Transacc_Ind", "C");
//			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C004");
			// *********************************************************************************

			// ****************************************************************************
			objectValidations.putInforCollectedForStructData("P_CODE", "000000");
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
			objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS", "15CLASE12NB");
			// ****************************************************************************

			break;
		case "051":

			validationCardsAccountMixtaDebitCreditPaymentObligationsWithCardATM(msg, objectValidations,
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

			break;

		default:
			break;
		}
	}

	public void consultaSaldoY5Mov(Base24Ath msg, Super objectValidations, ProcessingCode pCode) throws Exception {

		Pattern pattern = null;
		Matcher matcher = null;

		ProcessingCode pc = null;
		String procCode = null;

		if (msg.getField(3).equals("890000")) {

			procCode = msg.getField(126).substring(22, 28);
			pc = new ProcessingCode(procCode);
		} else {
			procCode = msg.getField(3);
			pc = new ProcessingCode(procCode);
		}

		accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), pc.toString(), msg.getTrack2Data().getPan(),
				pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
				msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1) ? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
						: Constants.General.SIXTEEN_ZEROS,
				objectValidations);

		objectValidations.putInforCollectedForStructData("Identificador_Terminal",
				msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(12, 13));

		String account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");

		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"cuenta trajo sp: " + account + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1), "LOG",
				this.nameInterface));

		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"cuenta trajo sp: " + account + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1), "LOG",
				this.nameInterface));

		pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
		matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

		if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
				&& !account.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
						.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))
				&& !matcher.matches()) {
			objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
			objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
			objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", pc.getFromAccount());
			objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
			objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
		} else if (matcher.matches()) {
			msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
					(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
		}

//		if (!account.equals(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1))) {
//			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//					"no son iguales", "LOG", this.nameInterface));
//			objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
//			objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
//					msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
//			objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE",
//					msg.getProcessingCode().getFromAccount());
//			objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
//			objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
//		}
		objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
		objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
		objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
	}

	private void consultaDeCostoAtm(Super objectValidations, Base24Ath msg, Pattern pattern, Matcher matcher,
			String account2) throws Exception {

		String procCode = msg.getField(126).substring(22, 28);
		ProcessingCode pc = new ProcessingCode(procCode);

		switch (procCode) {
		case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_AHORROS:// CONSULTA_DE_COSTO_ATM
		case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_CORRIENTE:
		case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_AHORROS:
		case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_CORRIENTE:// CONSULTA_DE_COSTO_ATM

			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					"ATM_CONSULTA_COSTO_TRANSFERENCIAS");

			validationCardsAccountMixtaDebitCreditTransferATM(msg, objectValidations,
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

			additionalTagsCostQuery(msg, objectValidations);

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_CONSULTA_COSTO_TRANSFERENCIAS_"
					+ objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_CBN"));

			break;
		case Constants.Channels.PCODE_PAGO_DE_SERVICIOS_ATM_AHO:// CONSULTA_DE_COSTO_ATM
		case Constants.Channels.PCODE_PAGO_DE_SERVICIOS_ATM_COR:// CONSULTA_DE_COSTO_ATM

			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP", "ATM_CONSULTA_COSTO_PSP");
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V3");
			objectValidations.putInforCollectedForStructData("P_CODE", msg.getField(126).substring(22, 28));
			objectValidations.putInforCollectedForStructData("B24_Field_104", "000000000000000000");
			objectValidations.putInforCollectedForStructData("B24_Field_62", Constants.General.DEFAULT_P62);
			processingRequestPSP(objectValidations, msg);
			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_CONSULTA_COSTO_PSP_"
					+ objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_CBN_PSP_S"));

			objectValidations.putInforCollectedForStructData("FI_CREDITO", "0000");
			objectValidations.putInforCollectedForStructData("FI_DEBITO", "0000");

			break;

		case Constants.Channels.PCODE_CONSULTA_DE_SALDO_ATM_AHORROS:// *** CONSULTA_DE_COSTO_ATM
		case Constants.Channels.PCODE_CONSULTA_DE_SALDO_ATM_CORRIENTE:// ***
		case Constants.Channels.PCODE_CONSULTA_5ULTIMOS_MOVIMIENTOS_ATM_A:// ***
		case Constants.Channels.PCODE_CONSULTA_5ULTIMOS_MOVIMIENTOS_ATM_C:// *** CONSULTA_DE_COSTO_ATM

			if (msg.getProcessingCode().toString().equals("381000")
					|| msg.getProcessingCode().toString().equals("382000")) {

				objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
						"ATM_CONSULTA_COSTO_CONSULTA_5MOVIMIENTOS");
			} else {
				objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
						"ATM_CONSULTA_COSTO_CONSULTA_DE_SALDO");
			}

			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");
			pc.putTranType(msg.getProcessingCode().getTranType());
			consultaSaldoY5Mov(msg, objectValidations, pc);
			objectValidations.putInforCollectedForStructData("FI_CREDITO", "0000");
			objectValidations.putInforCollectedForStructData("FI_DEBITO", "0000");

			break;
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_AHORROS:// **
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_CORRIENTE:// **
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS:// * CONSULTA
																											// DE COSTO
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE:// *
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS:// *
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE:// *
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_AHORROS:// *
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_CORRIENTE:// *

			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					"ATM_CONSULTA_COSTO_POBLIG_" + tagTTypePOblig(msg, objectValidations));
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

			switch (msg.getField(126).substring(22, 28).toString()) {
			case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS:// *
			case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE:// *

				pagoObligacionesAtmTC(msg, objectValidations);

				break;

			default:
				pagoObligacionesAtm(msg, objectValidations);
				break;
			}

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
					"ATM_CONSULTA_COSTO_POBLIG_" + tagTTypePOblig(msg, objectValidations) + "_"
							+ objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_CBN"));

			objectValidations.putInforCollectedForStructData("FI_CREDITO", "0000");
			objectValidations.putInforCollectedForStructData("FI_DEBITO", "0000");

			break;

		case Constants.Channels.PCODE_PAGO_CREDITO_HIPOTECARIO_EFECTIVO_ATM_MULTIFUNCIONAL:// 27 - 51 //***
		case Constants.Channels.PCODE_PAGO_CREDITO_ROTATIVO_EFECTIVO_ATM_MULTIFUNCIONAL:// 27 - 51 //*** CONSULTA DE
																						// COSTO
		case Constants.Channels.PCODE_PAGO_OTROS_CREDITOS_EFECTIVO_ATM_MULTIFUNCIONAL:// 27 - 51 //***

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
					"ATM_CONSULTA_COSTO_PAGOS_CREDITOS_EFECTIVO_MULTIFUNCIONAL");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					"ATM_CONSULTA_COSTO_PAGOS_CREDITOS_EFECTIVO_MULTIFUNCIONAL");
			objectValidations.putInforCollectedForStructData("P_CODE", msg.getField(126).substring(22, 28));

			tagsPagoCreditosEfectivoMultifuncional(objectValidations, msg);
			tagsConsultaCostoPagoCreditosEfectivoMultifuncional(objectValidations, msg);

			break;

		case Constants.Channels.PCODE_ACTIVACION_TOKEN:// consulta de costo

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
					"ATM_CONSULTA_COSTO_ACTIVACION_TOKEN");
			objectValidations.putInforCollectedForStructData("P_CODE", msg.getField(Iso8583.Bit._003_PROCESSING_CODE));
			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getProcessingCode().toString(),
					msg.getTrack2Data().getPan(), msg.getProcessingCode().getFromAccount(),
					msg.getTrack2Data().getExpiryDate(),
					(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)) ? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							: " 1234567",
					objectValidations);

			objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
					msg.getTrack2Data().getPan().substring(6));
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "55");
			objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRNSAD");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "01");
			objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "OTR");
			break;

		case Constants.Channels.PCODE_UTILIZACION_CREDITO_ROTATIVO_AHO:
		case Constants.Channels.PCODE_UTILIZACION_CREDITO_ROTATIVO_COR:

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
					"ATM_CONSULTA_COSTO_UTILIZACION_CREDITO_ROTATIVO");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					"ATM_CONSULTA_COSTO_UTILIZACION_CREDITO_ROTATIVO");
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

			atmUtilizacionCreditoRotativoTD(objectValidations, msg);

			objectValidations.putInforCollectedForStructData("FI_CREDITO", "0000");
			objectValidations.putInforCollectedForStructData("FI_DEBITO", "0000");
			objectValidations.putInforCollectedForStructData("Dispositivo", "0");

			break;

		case Constants.Channels.PCODE_CONSULTA_CUPO_CREDITO_ROTATIVO_ATM:// ***

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
					"ATM_CONSULTA_COSTO_CONSULTA_CREDITO_ROTATIVO");
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");
			processingRequestInquiryCreditLimit(objectValidations, msg);

			break;
		case Constants.Channels.PCODE_DEPOSITO_ATM_MULTIFUNCIONAL_AHO:// 27-21 //***
		case Constants.Channels.PCODE_DEPOSITO_ATM_MULTIFUNCIONAL_COR:// 27-21//*** CONSULTA_COSTO

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
					"ATM_CONSULTA_COSTO_DEPOSITO_ATM_MULTIFUNCIONAL");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					"ATM_CONSULTA_COSTO_DEPOSITO_ATM_MULTIFUNCIONAL");
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

			tagsExtractDepositoAtmMultifuncional(objectValidations, msg);

			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "   ");
			objectValidations.putInforCollectedForStructData("FI_CREDITO", "0000");
			objectValidations.putInforCollectedForStructData("Entidad_Origen", "0000");
			objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "0000000000000000000000000000");
			objectValidations.putInforCollectedForStructData("Ent_Adq",
					msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8, 23));
			objectValidations.putInforCollectedForStructData("B24_Field_104", "000000000000000000");
			objectValidations.putInforCollectedForStructData("PAN_Tarjeta", "                   ");

			break;

		case Constants.Channels.PCODE_RETIRO_ATM_A:// ***
		case Constants.Channels.PCODE_RETIRO_ATM_C:// ***

			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");

			switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
			case "051":
				String strCutRetiro = createfieldCut(msg);
				objectValidations.putInforCollectedForStructData("CUT_origen_de_la_transaccion", strCutRetiro);
				objectValidations.putInforCollectedForStructData("CUT_propio_de_la_transaccion", strCutRetiro);
				objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_CONSULTA_COSTO_RETIRO");

				pc.putTranType(Iso8583Post.TranType._32_GENERAL_INQUIRY);
				// String field102 = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1);
				String field102 = "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
						.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17);
				accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), procCode,
						msg.getTrack2Data().getPan(), pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
						msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
								? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
										.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
								: Constants.General.SIXTEEN_ZEROS,
						objectValidations);
				if (objectValidations.getValidationResult()) {
					account2 = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
					this.udpClient.sendData(Client.getMsgKeyValue(
							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "cuenta trajo sp: " + account2
									+ " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
							"LOG", this.nameInterface));

					pattern = Pattern.compile("0{" + field102.length() + "}");
					matcher = pattern.matcher(field102);

					if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1) && !account2.equals(field102)
							&& !matcher.matches()) {
						objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
					} else if (matcher.matches()) {
						msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1, account2);
					}

					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10"))
									? "05"
									: "04");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10"))
									? "AHO"
									: "CTE");

					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
				}
				break;

			default:

				switch (msg.getTrack2Data().getPan().substring(0, 6)) {
				case "777791":

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"ATM_CONSULTA_COSTO_RETIRO_SIN_TD");

					break;

				default:
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"ATM_CONSULTA_COSTO_COBRO_GIROS_TRADICIONAL_O_WEB_SERVICES");

					break;
				}

				objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
						(msg.getProcessingCode().getFromAccount().equals("10")) ? "05" : "04");
				objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
						(msg.getProcessingCode().getFromAccount().equals("10")) ? "AHO" : "CTE");
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR", msg.getTrack2Data().getPan());
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "00");

				objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
						msg.getField(Iso8583.Bit._003_PROCESSING_CODE).substring(2, 4));

				objectValidations.putInforCollectedForStructData("CUSTOMER_ID",
						msg.isFieldSet(Iso8583.Bit._104_TRAN_DESCRIPTION)
								? "PC00000".concat(msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION))
								: "PC00000".concat(Constants.Account.ACCOUNT_DEFAULT));
				objectValidations.putInforCollectedForStructData("P_CODE", "000000");
				break;
			}

			break;

		default:

			switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
			case "051":
				objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_CONSULTA_COSTO_DEFAULT_051");

				pc.putTranType(Iso8583Post.TranType._32_GENERAL_INQUIRY);
				// String field102 = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1);
				String field102 = "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
						.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17);
				accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), procCode,
						msg.getTrack2Data().getPan(), pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
						msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
								? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
										.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
								: Constants.General.SIXTEEN_ZEROS,
						objectValidations);
				if (objectValidations.getValidationResult()) {
					account2 = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
					this.udpClient.sendData(Client.getMsgKeyValue(
							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "cuenta trajo sp: " + account2
									+ " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
							"LOG", this.nameInterface));

					pattern = Pattern.compile("0{" + field102.length() + "}");
					matcher = pattern.matcher(field102);

					if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1) && !account2.equals(field102)
							&& !matcher.matches()) {
						objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
					} else if (matcher.matches()) {
						msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1, account2);
					}

					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10"))
									? "05"
									: "04");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10"))
									? "AHO"
									: "CTE");

					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
				}
				break;

			default:

				objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
						"ATM_CONSULTA_COSTO_DEFAULT_OTROS");

				objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
						(msg.getProcessingCode().getFromAccount().equals("10")) ? "05" : "04");
				objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
						(msg.getProcessingCode().getFromAccount().equals("10")) ? "AHO" : "CTE");
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR", msg.getTrack2Data().getPan());
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "00");

				objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
						msg.getField(Iso8583.Bit._003_PROCESSING_CODE).substring(2, 4));

				objectValidations.putInforCollectedForStructData("CUSTOMER_ID",
						msg.isFieldSet(Iso8583.Bit._104_TRAN_DESCRIPTION)
								? "PC00000".concat(msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION))
								: "PC00000".concat(Constants.Account.ACCOUNT_DEFAULT));
				objectValidations.putInforCollectedForStructData("P_CODE", "000000");
				break;
			}

			break;

		}

	}

	public static void tagsConsultaCostoPagoCreditosEfectivoMultifuncional(Super objectValidations, Base24Ath msg)
			throws XPostilion {

		objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
		objectValidations.putInforCollectedForStructData("FI_CREDITO", "0000");
		objectValidations.putInforCollectedForStructData("FI_DEBITO", "0000");
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "   ");
		objectValidations.putInforCollectedForStructData("PAN_Tarjeta", "                   ");
		objectValidations.putInforCollectedForStructData("Ent_Adq",
				msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
		objectValidations.putInforCollectedForStructData("Indicador_De_Aceptacion_O_De_No_Preaprobado", "0");

	}

	public static void additionalTagsCostQuery(Base24Ath msg, Super objectValidations) throws XPostilion {

		objectValidations.putInforCollectedForStructData("FI_CREDITO", "0000");
		objectValidations.putInforCollectedForStructData("FI_DEBITO", "0000");

		switch ((msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
				? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3)
				: "0") {
		case "0":// debito
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR", "000000000000000000");
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "   ");
			objectValidations.putInforCollectedForStructData("Ent_Adq",
					(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
							? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7)
							: msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));

			break;
		case "1":// credito
			objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1", msg.getTrack2Data().getPan());
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR", "000000000000000000");
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "   ");
			objectValidations.putInforCollectedForStructData("PAN_Tarjeta", msg.getTrack2Data().getPan());
			objectValidations.putInforCollectedForStructData("Ent_Adq",
					msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));

			break;
		case "2":// mixta

			break;

		default:
			break;
		}

	}

	public static String tagTTypePOblig(Base24Ath msg, Super objectValidations) throws XFieldUnableToConstruct {

		switch (msg.getProcessingCode().toString()) {
		case Constants.Channels.PCODE_PAGO_CREDITO_HIPOTECARIO_ATM_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_CORRIENTE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "1");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "1");

			return "PAGO_CREDITO_HIPOTECARIO";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "5");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "5");
			return "TARJETA_CREDITO";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "2");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "2");
			return "CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "3");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "3");
			return "OTROS_CREDITOS";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_EFECTIVO:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "1");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "1");
			return "HIPOTECARIO_EFECTIVO";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_CHEQUE:

			objectValidations.putInforCollectedForStructData("Mod_Credito", "1");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "1");
			return "HIPOTECARIO_CHEQUE";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_EFECTIVO:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "5");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "5");

			return "TC_EFECTIVO";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_CHEQUE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "5");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "5");

			return "TC_CHEQUE";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_EFECTIVO:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "2");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "2");

			return "ROTATIVO_EFECTIVO";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_CHEQUE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "2");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "2");

			return "ROTATIVO_CHEQUE";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_EFECTIVO:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "3");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "3");
			return "OTROS_EFECTIVO";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CHEQUE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "3");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "3");
			return "OTROS_CHEQUE";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_CORRIENTE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "4");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "4");
			return "VEHICULOS";

		default:
			return "OTROS";
		}

	}

	public static String getIndicador_De_Aceptacion_O_De_No_Preaprobado(Base24Ath msg) throws XPostilion {

		String p48 = msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA);
		switch (p48.substring(p48.length() - 1)) {
		case "7":

			return "D";
		case "6":

			return "N";
		case "5":

			return "S";

		default:
			return "0";
		}

	}

	public static void tagsPagoCreditosEfectivoMultifuncional(Super objectValidations, Base24Ath msg)
			throws XPostilion {

		Extract.tagsModelPaymentOfObligationsCredit(objectValidations, msg);

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "10");// preguntar si siempre es ahorros
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
		objectValidations.putInforCollectedForStructData("Dispositivo", "0");
		objectValidations.putInforCollectedForStructData("Entidad", "0000");
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR", "000000000000000000");
		objectValidations.putInforCollectedForStructData("Transaccion_Unica", "M002");
		objectValidations.putInforCollectedForStructData("Indicador_De_Aceptacion_O_De_No_Preaprobado", " ");

		String proCode = Extract.getProcCode(msg).toString();

		if (proCode.equals(Constants.Channels.PCODE_PAGO_CREDITO_HIPOTECARIO_EFECTIVO_ATM_MULTIFUNCIONAL))
			objectValidations.putInforCollectedForStructData("Mod_Credito", "1");
		else if (proCode.equals(Constants.Channels.PCODE_PAGO_CREDITO_ROTATIVO_EFECTIVO_ATM_MULTIFUNCIONAL))
			objectValidations.putInforCollectedForStructData("Mod_Credito", "2");
		else if (proCode.equals(Constants.Channels.PCODE_PAGO_OTROS_CREDITOS_EFECTIVO_ATM_MULTIFUNCIONAL))
			objectValidations.putInforCollectedForStructData("Mod_Credito", "3");

	}

	public static void transactionInputConsultaDeCosto(String text, String pcode, Super objectValidations) {
		switch (pcode) {
		case "011000":
		case "012000":
			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", text + "CONSULTA_COSTO_RETIRO");

			break;
		case "311000":
		case "312000":
			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
					text + "CONSULTA_COSTO_CONSULTA_DE_SALDO");

			break;
		case "381000":
		case "382000":
			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", text + "CONSULTA_COSTO_5MOVIMIENTOS");

			break;
		default:
			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", text + "CONSULTA_COSTO_OTRAS");
			break;
		}

	}

	public static void tagsExtractDepositoAtmMultifuncional(Super objectValidations, Base24Ath msg) throws XPostilion {

		Extract.tagsModelTransferCredit(objectValidations, msg);

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", msg.getProcessingCode().getToAccount());
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR", "00000000000000000");
		objectValidations.putInforCollectedForStructData("Dispositivo", "0");
		objectValidations.putInforCollectedForStructData("Transaccion_Unica", "M001");
		objectValidations.putInforCollectedForStructData("Entidad", "0000");
		objectValidations.putInforCollectedForStructData("FI_DEBITO", "0000");
//		objectValidations.putInforCollectedForStructData("Transacc_Ind", "C");

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

				insuranceAmmountsValidation(Integer.parseInt(montoTransaccion), Integer.parseInt(montoDonacion),
						Integer.parseInt(montoSeguro), Integer.parseInt(field4), objectValidations, msgFromRemote);
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
					"Method: [isValidAdditionalAmmount]", Utils.getStringMessageException(e),
					msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: isValidAdditionalAmmount " + e.getMessage(), "LOG", this.nameInterface));
		}
//		return new ValidatedResult(true, General.VOIDSTRING, General.VOIDSTRING, null);
	}

	/**
	 * El metodo realiza validaciones de cuentas para tres casos diferentes retiros
	 * CNB mixtos,debito y credito.
	 * 
	 * @param msg
	 * @param objectValidations
	 * @throws XFieldUnableToConstruct
	 * @throws XPostilion
	 * @throws Exception
	 */
	private void validationCardsAccountMixtaDebitCreditTransferATM(Base24Ath msg, Super objectValidations,
			String indicatorMixCreditDebidP103) throws XFieldUnableToConstruct, XPostilion, Exception {

		String account2 = null;
		objectValidations.putInforCollectedForStructData("P_CODE", "000000");

		Pattern pattern = null;
		Matcher matcher = null;

		ProcessingCode ps = null;
		if (msg.getField(3).equals("890000")) {
			ps = new ProcessingCode(msg.getField(126).substring(22, 28));
		} else {
			ps = new ProcessingCode(msg.getField(3));
		}

		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"Indicador tipo TRANSFERENCIA:" + indicatorMixCreditDebidP103, "LOG", this.nameInterface));
		switch (indicatorMixCreditDebidP103) {

		case "0":// debito

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ENTRO TRANSFERENCIA DEBITO", "LOG", this.nameInterface));

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(),
					msg.getTrack2Data().getPan(), ps.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			Extract.tagsModelTransferDebit(objectValidations, msg);

			// TAGS UNICOS
			// ***************************************************************************************************
			objectValidations.putInforCollectedForStructData("Entidad", "0000");
			objectValidations.putInforCollectedForStructData("Dispositivo", "0");
//			objectValidations.putInforCollectedForStructData("Transacc_Ind", "D");
//			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C002");
			// TAGS UNICOS
			// ***************************************************************************************************

			// TAGS ISC
			// **********************************************************************************************************
			tagsEncodeSensitiveData("CREDIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7).trim(), objectValidations);
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE", ps.getToAccount());
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
			// TAGS ISC
			// **********************************************************************************************************

//			account2 = objectValidations.getInforCollectedForStructData().get("DEBIT_ACCOUNT_NR");
//
//			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
//					&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
//							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))) {
//				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
//			}

			String account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos primera y segunda validacion: mixta "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"cuenta trajo sp: " + account + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
					"LOG", this.nameInterface));

			pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
			matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
					&& !account.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))
					&& !matcher.matches()) {
				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE",
						msg.getProcessingCode().getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			} else if (matcher.matches()) {
				msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
						(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
			}

			break;
		case "1":// credito

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ENTRO TRANSFERENCIA CREDITO", "LOG", this.nameInterface));

			accountsByNumberClientCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					Pack.resize(msg.getField(Iso8583Post.Bit._103_ACCOUNT_ID_2).substring(7).trim(), 18, '0', false),
					ps.getToAccount(), objectValidations);

			Extract.tagsModelTransferCredit(objectValidations, msg);

			// TAGS UNICOS
			// ***************************************************************************************************
			objectValidations.putInforCollectedForStructData("Entidad", "0000");
			objectValidations.putInforCollectedForStructData("Dispositivo", "0");
			// objectValidations.putInforCollectedForStructData("Transacc_Ind", "C");
			// objectValidations.putInforCollectedForStructData("Transaccion_Unica",
			// "C004");
			// TAGS UNICOS
			// ***************************************************************************************************

			// TAGS ISC
			// ****************************************************************************************************

			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_NR"));
			tagsEncodeSensitiveData("DEBIT_ACCOUNT_NR", msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
					.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 18), objectValidations);
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
					objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE"));
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE", ps.getFromAccount());

			// TAGS ISC
			// ****************************************************************************************************

			break;
		case "2":// mixta
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ENTRO TRANSFERENCIA MIXTA", "LOG", this.nameInterface));
			// TAGS UNICOS

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(),
					msg.getTrack2Data().getPan(), ps.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			// ************************************************************************************************
			objectValidations.putInforCollectedForStructData("Entidad", "0000");
			objectValidations.putInforCollectedForStructData("Dispositivo", "0");
//			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C003");
			// TAGS UNICOS
			// ************************************************************************************************

			// TAGS ISC
			// ****************************************************************************************************
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE", ps.getToAccount());
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
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7).trim());

			Extract.tagsModelTransferMixed(objectValidations, msg);
			// TAGS ISC
			// ****************************************************************************************************
//			account2 = objectValidations.getInforCollectedForStructData().get("DEBIT_ACCOUNT_NR");
//
//			this.udpClient
//					.sendData(Client.getMsgKeyValue("999999", "account2 : " + account2, "LOG", this.nameInterface));
//			this.udpClient.sendData(Client.getMsgKeyValue("999999",
//					"102 substring sin 0 : " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
//							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17),
//					"LOG", this.nameInterface));
//
//			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
//					&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
//							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))) {
//				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
//			}

			account2 = objectValidations.getInforCollectedForStructData().get("DEBIT_ACCOUNT_NR");
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos primera y segunda validacion: mixta "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"cuenta trajo sp: " + account2 + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
					"LOG", this.nameInterface));

			GenericInterface.getLogger().logLine(
					"cuenta trajo sp: " + account2 + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
			GenericInterface.getLogger()
					.logLine("cuenta p102 substring: " + "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17));

			pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
			matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
					&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))
					&& !matcher.matches()) {
				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE",
						msg.getProcessingCode().getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			} else if (matcher.matches()) {
				msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
						(this.encodeData) ? new String(Base64.getDecoder().decode(account2)) : account2);
			}

			break;

		default:

			break;

		}

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
	public void insuranceAmmountsValidation(int transactionAmmount, int donationAmmount, int insuranceAmmount,
			int field004Ammount, Super objectValidations, Base24Ath msg) {
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
				EventRecorder.recordEvent(new TryCatchException(
						new String[] { this.nameInterface, ATM.class.getName(), "Method: [insuranceAmmountsValidation]",
								Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: insuranceAmmountsValidation " + e.getMessage(), "LOG",
						this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
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
	 * Validate field four of incoming message all transaction types
	 * 
	 * @param p4               field 4 of message
	 * @param p48              field 48 of message
	 * @param p54              field 54 of message
	 * @param objectValidation object that contains the result of validations
	 * 
	 */
	public void validatefield4CostInquiry(String p37, String p4, String p48, String p54, String processingCode,
			Super objectValidation) {
		try {
			ProcessingCode pcode = new ProcessingCode(processingCode);
			switch (pcode.getTranType()) {
			case postilion.realtime.sdk.message.bitmap.Iso8583.TranType._01_CASH:
				validatefield4CostInquiryForWithdrawal(p37, p4, p48, p54, objectValidation);
				break;
			case postilion.realtime.sdk.message.bitmap.Iso8583Post.TranType._50_PAYMENT_SERVICES:
				// validatefield4CostInquiryForPayment(p37, p4, objectValidation);
				break;
			case postilion.realtime.sdk.message.bitmap.Iso8583.TranType._40_CARDHOLDER_ACCOUNTS_TRANSFER:
				// validatefield4CostInquiryForPSP(p37, p4, p54, objectValidation);
				break;
			case "38":

				break;
			case "31":

				break;
			case "32":

				break;
			default:
				objectValidation.modifyAttributes(false, "This tran type " + pcode.getTranType()
						+ " is not configured to validatefield4CostInquiry method.", "1994", "30");
				break;

			}

		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
					"Method: [validatefield4CostInquiry]", Utils.getStringMessageException(e), p37 }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: validatefield4CostInquiry " + e.getMessage(), "LOG", this.nameInterface));
			objectValidation.modifyAttributes(false, "Exception in validatefield4CostInquiry method.", "1994", "30");
		}
	}

	/**
	 * Validate field four of incoming message all transaction types
	 * 
	 * @param p4               field 4 of message
	 * @param p48              field 48 of message
	 * @param p54              field 54 of message
	 * @param objectValidation object that contains the result of validations
	 * @throws XPostilion
	 * 
	 */
	public String createfieldCut(Base24Ath msg) throws XPostilion {
		String strCut = null;
		try {
			// Método retiro y consulta ATM
			String p41 = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID);
			String red = p41.substring(4, 8);
			String channel = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13);
			String terminalOwn = null;
			String key = "";
			if ((p41.substring(0, 4).equals("0054"))) {
				terminalOwn = msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(0, 4);
			}

			else {
				channel = "1";
				terminalOwn = red;
			}

			StringBuilder ksb = new StringBuilder();

			if (msg.getProcessingCode().toString().equals("890000")) {
				key = msg.getField(126).substring(22, 28);
			} else {
				key = msg.getProcessingCode().toString();
			}

			ksb.append(key);
			ksb.append(channel == "99" ? channel : "0" + channel);
			ksb.append(terminalOwn);

			String valueCut = this.cutValues.get(ksb.toString());

			StringBuilder sb = new StringBuilder();

//			sb.append("ATWI ");
//			sb.append("00001");
//			sb.append("0000");
//			sb.append("IPP772");

			if (valueCut == null) {
				valueCut = "ATWI 000010000IPP772";
			}

			sb.append(valueCut);
			sb.append(String.valueOf(LocalDate.now().getYear()));
			sb.append(msg.getField(Iso8583.Bit._013_DATE_LOCAL));
			sb.append(msg.getField(Iso8583.Bit._012_TIME_LOCAL));
			sb.append(msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR).substring(1));

			strCut = sb.toString();

			return strCut;

		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(
					new String[] { this.nameInterface, ATM.class.getName(), "Method: [createfieldCut]",
							Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: createfieldCut " + e.getMessage(), "LOG", this.nameInterface));
		}

		return strCut;
	}

	/**
	 * Validate field four of incoming message for a withdrawal
	 * 
	 * @param p4               field 4 of message
	 * @param p48              field 48 of message
	 * @param p54              field 54 of message
	 * @param objectValidation object that contains the result of validations
	 * 
	 */
	public void validatefield4CostInquiryForWithdrawal(String p37, String p4, String p48, String p54,
			Super objectValidation) {
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
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
					"Method: [validatefield4CostInquiryForWithdrawal]", Utils.getStringMessageException(e), p37 }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: validatefield4CostInquiryForWithdrawal " + e.getMessage(), "LOG",
					this.nameInterface));
		}
	}

	/**
	 * Validate field four of incoming message for a payment
	 * 
	 * @param p4               field 4 of message
	 * @param p48              field 48 of message
	 * @param objectValidation object that contains the result of validations
	 * 
	 */
	public void validatefield4CostInquiryForPayment(String p37, String p4, Super objectValidation) {
		try {

			int field4Ammount = Integer.parseInt(p4);
			if (field4Ammount % 1000000 != 0) {
				objectValidation.modifyAttributes(false, "Ammount in field 4 should be multiple of 10.000", "1994",
						"30");
			}

		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
					"Method: [validatefield4CostInquiryForPayment]", Utils.getStringMessageException(e), p37 }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"Exception in Method: validatefield4CostInquiryForPayment " + e.getMessage(), "LOG",
					this.nameInterface));
		}
	}

	/**
	 * Validate field four of incoming message for a payment of service
	 * 
	 * @param p4               field 4 of message
	 * @param p48              field 48 of message
	 * @param p54              field 54 of message
	 * @param objectValidation object that contains the result of validations
	 * 
	 */
	public void validatefield4CostInquiryForPSP(String p37, String p4, String p54, Super objectValidation) {
		try {

			String montoTransaccion = p54.substring(Indexes.FIELD54_POSITION_0, Indexes.FIELD54_POSITION_12);
			String montoDonacion = p54.substring(Indexes.FIELD54_POSITION_12, Indexes.FIELD54_POSITION_24);
			String montoSeguro = p54.substring(Indexes.FIELD54_POSITION_24, Indexes.FIELD54_POSITION_36);

			if (!p4.equals(montoTransaccion)
					|| !montoDonacion
							.equals(postilion.realtime.genericinterface.translate.util.Constants.General.TWELVE_ZEROS)
					|| !montoSeguro.equals(
							postilion.realtime.genericinterface.translate.util.Constants.General.TWELVE_ZEROS)) {

				objectValidation.modifyAttributes(false,
						"Ammounts in fields 4 and 54 first part should be the same and parts 2 and 3 of field 54 should be zero",
						"1994", "30");
			}
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
					"Method: [validatefield4CostInquiryForPSP]", Utils.getStringMessageException(e), p37 }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(
					Client.getMsgKeyValue(p37, "Exception in Method: validatefield4CostInquiryForPSP " + e.getMessage(),
							"LOG", this.nameInterface));
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
	 * Search and put data to construct a message for a inquiry for credit limit
	 * transaction.
	 * 
	 * @param objectValidations
	 * @param msg               to process
	 */
	public void processingRequestInquiryCreditLimit(Super objectValidations, Base24Ath msg) {
//		objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "ATM_CONSULTA_CUPO_CREDITO_ROTATIVO");

		try {
			String procCode = null;

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), procCode, msg.getTrack2Data().getPan(),
					"10", msg.getTrack2Data().getExpiryDate(),
					(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)) ? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							: " 1234567",
					objectValidations);

			objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "OTR");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "06");
			objectValidations.putInforCollectedForStructData("Ent_Adq",
					msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
			objectValidations.putInforCollectedForStructData("Entidad", "0000");
			objectValidations.putInforCollectedForStructData("Identificador_Terminal", "0");
			objectValidations.putInforCollectedForStructData("Indicador_AVAL", "0");

		} catch (Exception e) {
			try {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
						"Method: [processingRequestInquiryCreditLimitAtm]", Utils.getStringMessageException(e),
						msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"XFieldUnableToConstruct in Method: processingRequestInquiryCreditLimitAtm "
								+ Utils.getStringMessageException(e),
						"LOG", this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
						"Method: [processingRequestInquiryCreditLimitAtm]", Utils.getStringMessageException(e1),
						"Unknown" }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
						"XFieldUnableToConstruct in Method: processingRequestInquiryCreditLimitAtm "
								+ Utils.getStringMessageException(e1),
						"LOG", this.nameInterface));
			}
		}
	}

	/**
	 * Search and put data to construct a message for a psp transaction.
	 * 
	 * @param objectValidations
	 * @param msg               to process
	 */
	public void processingRequestPSP(Super objectValidations, Base24Ath msg) {
		try {
			if (isMultifuctionalATM(msg)) {
				Extract.tagsModelPaymentOfServicesCredit(objectValidations, msg);
				objectValidations.putInforCollectedForStructData("Identificacion_Canal", "C1");
			} else {
				processingRequestPSPATM(objectValidations, msg);
			}
		} catch (Exception e) {
			try {
				EventRecorder.recordEvent(new TryCatchException(
						new String[] { this.nameInterface, ATM.class.getName(), "Method: [processingRequestPSP]",
								Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"XFieldUnableToConstruct in Method: processingRequestPSP " + Utils.getStringMessageException(e),
						"LOG", this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
						"Method: [processingRequestPSP]", Utils.getStringMessageException(e1), "Unknown" }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(
						Client.getMsgKeyValue("Unknown", "XFieldUnableToConstruct in Method: processingRequestPSP "
								+ Utils.getStringMessageException(e1), "LOG", this.nameInterface));
			}
		}
	}

	/**
	 * Validate if a message is from a multifuctional atm
	 * 
	 * @param msg received from acquirer institution
	 * @return true for a message from a multifuctional atm
	 */
	public boolean isMultifuctionalATM(Base24Ath msg) {
		boolean multifunctional = false;
		try {
			String bin = (String) msg.getTrack2Data().getPan().substring(0, 6);
			if (msg.getPosEntryMode().toString()
					.equals(PosEntryMode.PanEntryMode._02_MAGNETIC_STRIPE_CVV_UNRELIABLE
							+ PosEntryMode.PinEntryCapability._1_YES)
					&& (bin.equals("777760") || bin.equals("777790") || bin.equals("777791"))) {
				multifunctional = true;
			}
		} catch (XFieldUnableToConstruct e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
					"Method: [isMultifuctionalATM]", Utils.getStringMessageException(e), "Unknown" }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
					"XFieldUnableToConstruct in Method: isMultifuctionalATM " + Utils.getStringMessageException(e),
					"LOG", this.nameInterface));
		}
		return multifunctional;
	}

	/**
	 * Search and put data to construct a message for a psp transaction that comes
	 * from ATM.
	 * 
	 * @param objectValidations
	 * @param msg               to process
	 */
	public void processingRequestPSPATM(Super objectValidations, Base24Ath msg) {
		Pattern pattern = null;
		Matcher matcher = null;

		String procCode = null;
		ProcessingCode pc = null;

		try {

			if (msg.getProcessingCode().toString().equals("890000")) {
				procCode = msg.getField(126).substring(22, 28);
				pc = new ProcessingCode(procCode);
			} else {
				procCode = msg.getField(3);
				pc = new ProcessingCode(procCode);
			}

			switch (msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(0, 1)) {
			case PSP.MIXTA:
				accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), pc.toString(),
						msg.getTrack2Data().getPan(), pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
						(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)) ? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
								: " 1234567",
						objectValidations);
				Extract.tagsModelPaymentOfServicesMixed(objectValidations, msg);
				objectValidations.putInforCollectedForStructData("Identificacion_Canal", "AT");
				tagsForPSPDebitMixATM(objectValidations);

				String account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"trajo datos primera y segunda validacion: mixta "
								+ objectValidations.getInforCollectedForStructData().toString(),
						"LOG", this.nameInterface));

				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"cuenta trajo sp: " + account + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
						"LOG", this.nameInterface));

				if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)) {
					pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
					matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

					if (!account.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))
							&& !matcher.matches()) {
						objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
						objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
								msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
						objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", pc.getFromAccount());
						objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
						objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
					} else if (matcher.matches()) {
						msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
								(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
					}
				}

				break;
			case PSP.DEBITO:
				accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), pc.toString(),
						msg.getTrack2Data().getPan(), pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
						(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)) ? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
								: " 1234567",
						objectValidations);
				Extract.tagsModelPaymentOfServicesDebit(objectValidations, msg);
				objectValidations.putInforCollectedForStructData("Identificacion_Canal", "01");
				tagsForPSPDebitMixATM(objectValidations);

				account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"trajo datos primera y segunda validacion: mixta "
								+ objectValidations.getInforCollectedForStructData().toString(),
						"LOG", this.nameInterface));

				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"cuenta trajo sp: " + account + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
						"LOG", this.nameInterface));

				pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
				matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

				if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						&& !account.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
								.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))
						&& !matcher.matches()) {
					objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
					objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
							msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
					objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", pc.getFromAccount());
					objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
					objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
				} else if (matcher.matches()) {
					msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
							(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
				}

				break;
			case PSP.CREDITO:
				Extract.tagsModelPaymentOfServicesCredit(objectValidations, msg);
				objectValidations.putInforCollectedForStructData("Identificacion_Canal", "C1");
				break;
			}
		} catch (Exception e) {
			try {
				EventRecorder.recordEvent(new TryCatchException(
						new String[] { this.nameInterface, ATM.class.getName(), "Method: [processingRequestPSPATM]",
								Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"XFieldUnableToConstruct in Method: processingRequestPSPATM "
								+ Utils.getStringMessageException(e),
						"LOG", this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, ATM.class.getName(),
						"Method: [processingRequestPSPATM]", Utils.getStringMessageException(e1), "Unknown" }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(
						Client.getMsgKeyValue("Unknown", "XFieldUnableToConstruct in Method: processingRequestPSPATM "
								+ Utils.getStringMessageException(e1), "LOG", this.nameInterface));
			}
		}
	}

	public void tagsForPSPDebitMixATM(Super objectValidations) {
		if (!objectValidations.getValidationResult())
			return;

		objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
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
	}

	private void validationCardsAccountMixtaDebitCreditPaymentObligationsWithCardATM(Base24Ath msg,
			Super objectValidations, String indicatorMixCreditDebidP103)
			throws XFieldUnableToConstruct, XEncryptionKeyError, XPostilion, Exception {

		Pattern pattern = null;
		Matcher matcher = null;

		String procCode = null;
		ProcessingCode pc = null;

		if (msg.getProcessingCode().toString().equals("890000")) {
			procCode = msg.getField(126).substring(22, 28);
			pc = new ProcessingCode(procCode);
		} else {
			procCode = msg.getField(3);
			pc = new ProcessingCode(procCode);
		}

		if (indicatorMixCreditDebidP103.equals("2")) {// mixta tx
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion pago de O 2.MIXTA", "LOG", this.nameInterface));

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), pc.toString(),
					msg.getTrack2Data().getPan(), pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			Extract.tagsModelPaymentOfObligationsMixed(objectValidations, msg);

			// TAGS UNICOS
			// ****************************************************************************
			// TAGS UNICOS
			// ****************************************************************************

			// TAGS ISC
			// ******************************************************************************
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
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
			// TAGS ISC
			// ******************************************************************************

			String account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos primera y segunda validacion: mixta "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"cuenta trajo sp: " + account + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
					"LOG", this.nameInterface));

			pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
			matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

			GenericInterface.getLogger().logLine("FIELD102:[" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1) + "]");
			GenericInterface.getLogger().logLine("ACCOUNT:[" + account + "]");

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1) && account != null
					&& !account.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))
					&& !matcher.matches()) {
				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE",
						msg.getProcessingCode().getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			} else if (matcher.matches()) {
				msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
						(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
			}

		} else if (indicatorMixCreditDebidP103.equals("1")) {

			// viene solo cuenta corresponsal. CREDITO
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion pago de servicios publicos 1.CREDITO", "LOG", this.nameInterface));

			// TAGS UNICOS
			// *********************************************************************************
			Extract.tagsModelPaymentOfObligationsCredit(objectValidations, msg);
			// TAGS UNICOS
			// **********************************************************************************

			objectValidations.putInforCollectedForStructData("P_CODE", "000000");

			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));

		} else if (indicatorMixCreditDebidP103.equals("0")) {

			// viene solo cuenta cliente. DEBITO

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro pago de servicios tipo 0.DEBITO", "LOG", this.nameInterface));

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), pc.toString(),
					msg.getTrack2Data().getPan(), pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			String account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos primera y segunda validacion: mixta "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"cuenta trajo sp: " + account + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
					"LOG", this.nameInterface));

			String field102 = msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
					.substring(msg.getFieldLength(Iso8583.Bit._102_ACCOUNT_ID_1) - 17);
			pattern = Pattern.compile("0{" + field102.length() + "}");
			matcher = pattern.matcher(field102);

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1) && !account.equals("0" + field102)
					&& !matcher.matches()) {
				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE",
						msg.getProcessingCode().getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			}
//			else if (matcher.matches()) {
//				msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
//						(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
//			}

			// TAGS ISC
			// *********************************************************************************
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
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
			// TAGS ISC
			// *********************************************************************************

			// TAGS EXTRACT
			// ****************************************************************************

			Extract.tagsModelPaymentOfObligationsDebit(objectValidations, msg);

			objectValidations.putInforCollectedForStructData("Dispositivo", "0");
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "LCR");
			objectValidations.putInforCollectedForStructData("Entidad", "0000");
//			objectValidations.putInforCollectedForStructData("Indicador_De_Aceptacion_O_De_No_Preaprobado", " ");

			// TAGS EXTRACT
			// ****************************************************************************

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos cuentaCNB validacion: DEBITO  "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

		}

	}

	public static final class PSP {
		public static final String MIXTA = "0";
		public static final String DEBITO = "1";
		public static final String CREDITO = "2";
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