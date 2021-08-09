package postilion.realtime.genericinterface.channels;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidMessage;
import postilion.realtime.genericinterface.eventrecorder.events.SQLExceptionEvent;
import postilion.realtime.genericinterface.extract.Extract;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.database.DBHandler.Account;
import postilion.realtime.genericinterface.translate.database.DBHandler.ColumnNames;
import postilion.realtime.genericinterface.translate.database.DBHandler.StoreProcedures;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.library.common.util.constants.General;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.ipc.XEncryptionKeyError;
import postilion.realtime.sdk.jdbc.JdbcManager;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.ProcessingCode;
import postilion.realtime.sdk.message.bitmap.XFieldUnableToConstruct;
import postilion.realtime.sdk.util.XPostilion;

public class CNB extends Super {

	private Client udpClient = null;
	private String nameInterface = "";
	private boolean encodeData = false;

	public CNB(Boolean validationResult, String descriptionError, String errorCodeAUTRA, String errorCodeISO,
			HashMap<String, String> inforCollectedForStructData, Parameters params) {
		super(validationResult, descriptionError, errorCodeAUTRA, errorCodeISO, inforCollectedForStructData, params);
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
		this.encodeData = params.isEncodeData();
	}

	public void validations(Base24Ath msg, Super objectValidations) {

		try {

			switch (msg.getMsgType()) {

			case Iso8583.MsgType._0200_TRAN_REQ:// 512
			case Iso8583.MsgType._0420_ACQUIRER_REV_ADV:// 1056
				switch (msg.getProcessingCode().toString()) {

				// RETIRO CNB.
				case Constants.Channels.PCODE_RETIRO_CON_TARJETA_CNB_A:// ***
				case Constants.Channels.PCODE_RETIRO_CON_TARJETA_CNB_C:// ***

					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Entro validacion CNB RETIRO", "LOG", this.nameInterface));

					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_RETIRO");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "CNB_RETIRO");

					tagsExtractRetDepTransPOblig(objectValidations, msg);

					validationCardsAccountMixtaDebitCreditRetiroCNB(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

					break;

				// Retiro OTP Corresponsal
				case Constants.Channels.PCODE_RETIRO_SIN_TARJETA_CNB_DE_AHORROS_A_CNBAHORROS:
				case Constants.Channels.PCODE_RETIRO_SIN_TARJETA_CNB_DE_AHORROS_A_CNBCORRIENTE:
				case Constants.Channels.PCODE_RETIRO_SIN_TARJETA_CNB_DE_CORRIENTE_A_CNBAHORROS:
				case Constants.Channels.PCODE_RETIRO_SIN_TARJETA_CNB_DE_CORRIENTE_A_CNBCORRIENTE:

					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_RETIRO_OTP");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "CNB_RETIRO_OTP");

					validationMixtaDebitoRetiroWithoutCard(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

					objectValidations.putInforCollectedForStructData("P_CODE", "000000");

					break;
				// PAGO SERVICIOS PUBLICOS CNB.
				case Constants.Channels.PCODE_PAGO_SP_CNB_A:// ***
				case Constants.Channels.PCODE_PAGO_SP_CNB_C:// ***

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
							"CNB_PAGO_SERVICIOS_PUBLICOS");
					objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP", "CNB_PSP");

					Extract.tagsModelPspGeneral(objectValidations, msg);

//					objectValidations.putInforCollectedForStructData("Identificacion_Canal", "CB");

					switch (msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4)) {
					// CNB BANCO DE BOGOTA
					case "0001":

						switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {

						case "021":
						case "010":

							accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getTrack2Data().getPan(),
									objectValidations);

							// tags psp
							objectValidations.putInforCollectedForStructData("P_CODE", "000000");

							objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
									(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE")
											.equals("10")) ? "05" : "04");
							objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
									(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE")
											.equals("10")) ? "AHO" : "CTE");

							// TAGS ISC
							// ****************************************************************************************
							objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
									objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
							objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
									objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));
							objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
									objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
							objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID",
									objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
							objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
									objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
							// TAGS ISC
							// ****************************************************************************************

							validationCardsAccountMixtaDebitCreditPaymentPublicServicesWithoutCard(msg,
									objectValidations, msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(0, 1));

							break;
						case "051":

							validationCardsAccountMixtaDebitCreditPaymentPublicServicesWithCard(msg, objectValidations,
									msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(0, 1));

							break;
						default:
							break;
						}

						break;

					// CNB AVAL
					default:

						switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
						case "021":
						case "010":

							// Tags psp
							objectValidations.putInforCollectedForStructData("P_CODE", "000000");
							objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "CREDITO");
							objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "05");
							objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "AHO");
							tagsEncodeSensitiveData("DEBIT_CARD_NR", "0066010000000000", objectValidations);
							tagsEncodeSensitiveData("Tarjeta_Amparada", msg.getTrack2Data().getPan(),
									objectValidations);
							objectValidations.putInforCollectedForStructData("Vencimiento", "0000");
							objectValidations.putInforCollectedForStructData("Ind_4xmil", "0");
							objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID", "0000000000000");
							objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "2");
							objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS", "00");
							objectValidations.putInforCollectedForStructData("Identificacion_Canal", "C0");

							break;
						case "051":

							validationCardsAccountMixtaDebitCreditPaymentPublicServicesWithCard(msg, objectValidations,
									msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(0, 1));

							break;

						default:
							break;
						}

						break;
					}

					break;

				// TRANFERENCIAS CNB.
				case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_AHORROS:// ***
				case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_CORRIENTE:// ***
				case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_CORRIENTE:// ***
				case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_AHORROS:// ***

					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Entro validacion CNB TRANSFERENCIA", "LOG", this.nameInterface));
					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Indicador tipo TRANSFERENCIA:"
									+ msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3),
							"LOG", this.nameInterface));

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_TRANSFERENCIA");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "CNB_TRANSFERENCIA");

					tagsExtractRetDepTransPOblig(objectValidations, msg);
					validationCardsAccountMixtaDebitCreditTransferCNB(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

					break;
				// DEPOSITOS CNB.
				case Constants.Channels.PCODE_DEPOSITOS_AHORROS_A_CORRIENTE:// ***
				case Constants.Channels.PCODE_DEPOSITOS_AHORROS_A_AHORROS:// ***
				case Constants.Channels.PCODE_DEPOSITOS_CORRIENTE_A_AHORROS:// ***
				case Constants.Channels.PCODE_DEPOSITOS_CORRIENTE_A_CORRIENTE:// ***

					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"Entro validacion CNB DEPOSITO", "LOG", this.nameInterface));

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_DEPOSITO");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "CNB_DEPOSITO");
					tagsEncodeSensitiveData("ACCOUNT_DEST_DEPOSIT",
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7), objectValidations);
					objectValidations.putInforCollectedForStructData("ACCOUNT_DEST_DEPOSIT_TYPE",
							msg.getProcessingCode().getToAccount().substring(0, 1) + "0");
					objectValidations.putInforCollectedForStructData("P_CODE", "000000");

					tagsExtractRetDepTransPOblig(objectValidations, msg);

					validationCardsAccountMixtaDebitCreditDepositoCNB(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

					break;

				// PAGO DE OBLIGACIONES CNB.
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_EFECTIVO:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_CHEQUE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_EFECTIVO:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_CHEQUE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_EFECTIVO:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_CHEQUE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_EFECTIVO:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CHEQUE:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_AHORROS:
				case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_CORRIENTE:

					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_PAGO_OBLIGACIONES");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");
//					tagsExtractRetDepTransPOblig(objectValidations, msg);

					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
							"CNB_POBLIG_" + ATM.tagTTypePOblig(msg, objectValidations));

					switch (msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4)) {
					// CNB BOGOTA
					case "0001":

						switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
						case "021":
						case "010":

							objectValidations.putInforCollectedForStructData("P_CODE", "000000");
							accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getTrack2Data().getPan(),
									objectValidations);

							// tags ISC
							// *********************************************************************************
							objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
									objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
							objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
									objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));
							objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
									objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
							objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID",
									objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
							objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
									objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
							// TAGS ISC
							// ***********************************************************************************

							// TAGS EXTRACT
							// *****************************************************************************

							validationCardsAccountMixtaDebitCreditPaymentObligationsWithoutCard(msg, objectValidations,
									msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

							// TAGS EXTRACT
							// *****************************************************************************

							break;
						case "051":

							validationCardsAccountMixtaDebitCreditPaymentObligationsWithCard(msg, objectValidations,
									msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3),
									msg.getProcessingCode());

							break;

						default:
							break;
						}

						break;

					// CNB AVAL
					default:

						switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
						case "021":
						case "010":

							Extract.tagsModelPaymentOfObligationsCredit(objectValidations, msg);

							// TAGS ISC
							// ****************************************************************************
							objectValidations.putInforCollectedForStructData("P_CODE", "000000");
							objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
									msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
							objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
									msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
							objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS", "15CLASE12NB");
							// TAGS ISC
							// ****************************************************************************

							break;
						case "051":

							validationCardsAccountMixtaDebitCreditPaymentObligationsWithCard(msg, objectValidations,
									msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3),
									msg.getProcessingCode());

							break;

						default:
							break;
						}

					}

					break;

				case Constants.Channels.PCODE_CONSULTA_DE_SALDO_Y_CUPO_CNB_A:
				case Constants.Channels.PCODE_CONSULTA_DE_SALDO_Y_CUPO_CNB_C:
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "CONSUL");
					objectValidations.putInforCollectedForStructData("Canal", "CB");
					objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");

					Pattern pattern = null;
					Matcher matcher = null;

					accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							msg.getProcessingCode().toString(), msg.getTrack2Data().getPan(),
							msg.getProcessingCode().getFromAccount(), msg.getTrack2Data().getExpiryDate(),
							msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
									? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
											.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
									: Constants.General.SIXTEEN_ZEROS,
							objectValidations);

					if (msg.getProcessingCode().toString().equals("311000")
							|| msg.getProcessingCode().toString().equals("312000")) {

						objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_CONSULTA_DE_SALDO");
						objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "60");
					}

					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10"))
									? "05"
									: "04");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10"))
									? "AHO"
									: "CTE");

//					String account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
//					GenericInterface.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),"cuenta trajo sp: "+account + " cuenta p102: "+msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
//					if (!account.equals(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1))) {
//						objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0001", "14");
//						objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR", msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
//						objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE",msg.getProcessingCode().getFromAccount());
//						objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
//						objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
//					}

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

					String account = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"trajo datos primera y segunda validacion: mixta "
									+ objectValidations.getInforCollectedForStructData().toString(),
							"LOG", this.nameInterface));

					this.udpClient.sendData(Client.getMsgKeyValue(
							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "cuenta trajo sp: " + account
									+ " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
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

				case Constants.Channels.PCODE_CONSULTA_DE_COSTO_CNB:

					ProcessingCode pcode = new ProcessingCode(msg.getField(126).substring(22, 28));
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "CNB_CONSULTA_COSTO");
					objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_CONSULTA_COSTO");
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

					String procCode = msg.getField(126).substring(22, 28);
					ProcessingCode pc = new ProcessingCode(procCode);
					consultaDeConstoCnb(objectValidations, pc, msg, procCode);

					objectValidations.putInforCollectedForStructData("FI_CREDITO", "0000");
					objectValidations.putInforCollectedForStructData("FI_DEBITO", "0000");
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "CONSUL");
					objectValidations.putInforCollectedForStructData("Nombre_TransaccionX1", "CONSUL");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "65");
					objectValidations.putInforCollectedForStructData("Codigo_TransaccionX1", "65");

					break;

				default:
					break;
				}

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

	private void consultaDeConstoCnb(Super objectValidations, ProcessingCode pc, Base24Ath msg, String procCode)
			throws Exception {
		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"consultaDeConstoCnb", "LOG", this.nameInterface));
		switch (pc.toString()) {
		case Constants.Channels.PCODE_DEPOSITOS_AHORROS_A_CORRIENTE:
		case Constants.Channels.PCODE_DEPOSITOS_AHORROS_A_AHORROS:
		case Constants.Channels.PCODE_DEPOSITOS_CORRIENTE_A_AHORROS:
		case Constants.Channels.PCODE_DEPOSITOS_CORRIENTE_A_CORRIENTE:

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion CNB DEPOSITO", "LOG", this.nameInterface));

			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "CNB_CONSULTA_COSTO_DEPOSITO");
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

			tagsEncodeSensitiveData("ACCOUNT_DEST_DEPOSIT", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);
			objectValidations.putInforCollectedForStructData("ACCOUNT_DEST_DEPOSIT_TYPE",
					msg.getProcessingCode().getToAccount().substring(0, 1) + "0");
			objectValidations.putInforCollectedForStructData("P_CODE", "000000");
			tagsExtractRetDepTransPOblig(objectValidations, msg);
			validationCardsAccountMixtaDebitCreditDepositoCNB(msg, objectValidations,
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_CONSULTA_COSTO_DEPOSITO_"
					+ objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_CBN"));

			break;
		case Constants.Channels.PCODE_RETIRO_CON_TARJETA_CNB_A:// consultaDeConstoCnb ok
		case Constants.Channels.PCODE_RETIRO_CON_TARJETA_CNB_C:// consultaDeConstoCnb ok

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion CNB RETIRO", "LOG", this.nameInterface));
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "CNB_CONSULTA_COSTO_RETIRO");
			tagsExtractRetDepTransPOblig(objectValidations, msg);

			validationCardsAccountMixtaDebitCreditRetiroCNB(msg, objectValidations,
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_CONSULTA_COSTO_RETIRO_"
					+ objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_CBN"));

			break;

		// PAGO SERVICIOS PUBLICOS CNB.
		case Constants.Channels.PCODE_PAGO_SP_CNB_A:// Consulta de costo ok
		case Constants.Channels.PCODE_PAGO_SP_CNB_C:// ok

			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP", "CNB_CONSULTA_COSTO_PSP");
			Extract.tagsModelPspGeneral(objectValidations, msg);
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V3");
			objectValidations.putInforCollectedForStructData("Identificacion_Canal", "CB");
			objectValidations.putInforCollectedForStructData("B24_Field_104", Constants.General.DEFAULT_104);
			objectValidations.putInforCollectedForStructData("B24_Field_62", Constants.General.DEFAULT_P62);
			switch (msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4)) {
			// CNB BANCO DE BOGOTA
			case "0001":

				switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {

				case "021":
				case "010":

					accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getTrack2Data().getPan(),
							objectValidations);

					// tags psp
					objectValidations.putInforCollectedForStructData("P_CODE", "000000");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
							(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10"))
									? "05"
									: "04");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
							(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10"))
									? "AHO"
									: "CTE");

					// TAGS ISC
					// ****************************************************************************************
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
					objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID",
							objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
					// TAGS ISC
					// ****************************************************************************************

					validationCardsAccountMixtaDebitCreditPaymentPublicServicesWithoutCard(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(0, 1));
					break;
				case "051":

					validationCardsAccountMixtaDebitCreditPaymentPublicServicesWithCard(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(0, 1));

					break;
				default:
					break;
				}

				break;

			// CNB AVAL
			default:

				switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
				case "021":
				case "010":

					// Tags psp
					objectValidations.putInforCollectedForStructData("P_CODE", "000000");
					objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP",
							objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_PSP")
									+ "_CREDITO");
					objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "CREDITO");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "05");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "AHO");
					tagsEncodeSensitiveData("DEBIT_CARD_NR", "0066010000000000", objectValidations);
					tagsEncodeSensitiveData("Tarjeta_Amparada", msg.getTrack2Data().getPan(), objectValidations);
					objectValidations.putInforCollectedForStructData("Vencimiento", "0000");
					objectValidations.putInforCollectedForStructData("Ind_4xmil", "0");
					objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID", "0000000000000");
					objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "2");
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS", "00");
					objectValidations.putInforCollectedForStructData("Identificacion_Canal", "C0");

					break;
				case "051":

					validationCardsAccountMixtaDebitCreditPaymentPublicServicesWithCard(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(0, 1));

					break;

				default:
					break;
				}

				break;
			}

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
					"CNB_CONSULTA_COSTO_PAGO_SERVICIOS_PUBLICOS_"
							+ objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_CBN_PSP_S"));

			break;
		// TRANFERENCIAS CNB.
		case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_AHORROS:
		case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_CORRIENTE:// consulta de costo ok
		case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_CORRIENTE:
		case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_AHORROS:

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion CNB TRANSFERENCIA", "LOG", this.nameInterface));
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Indicador tipo TRANSFERENCIA:" + msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3),
					"LOG", this.nameInterface));

			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					"CNB_CONSULTA_COSTO_TRANSFERENCIA");
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

			tagsExtractRetDepTransPOblig(objectValidations, msg);
			validationCardsAccountMixtaDebitCreditTransferCNB(msg, objectValidations,
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_CONSULTA_COSTO_TRANSFERENCIA_"
					+ objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_CBN"));

			break;

		// PAGO DE OBLIGACIONES CNB.
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_CORRIENTE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE:// consulta
																											// costo ok
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_CORRIENTE:

			tagsExtractRetDepTransPOblig(objectValidations, msg);
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V2");

			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					"CNB_CONSULTA_COSTO_POBLIG_" + ATM.tagTTypePOblig(msg, objectValidations));

			switch (msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4)) {
			// CNB BOGOTA
			case "0001":

				switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
				case "021":
				case "010":
					// revizar casos.

					objectValidations.putInforCollectedForStructData("P_CODE", "000000");
					accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getTrack2Data().getPan(),
							objectValidations);

					// TAGS EXTRACT
					// *****************************************************************************
//					FGHFH
					tagsAccountsCNBCard(objectValidations);
					tagsAccountsCNBAccount(objectValidations);

					tagsPObligSecAccounts(objectValidations, msg);
					objectValidations.putInforCollectedForStructData("ID_CLIENT",
							objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));

					// TAGS EXTRACT
					// *****************************************************************************

					// tags ISC
					// *********************************************************************************
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
					objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID",
							objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
					// TAGS ISC
					// ***********************************************************************************

					validationCardsAccountMixtaDebitCreditPaymentObligationsWithoutCard(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3));

					break;
				case "051":

					validationCardsAccountMixtaDebitCreditPaymentObligationsWithCard(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3), pc);

					break;

				default:
					break;
				}

				break;

			// CNB AVAL
			default:

				switch (msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)) {
				case "021":
				case "010":

					// TAGS UNICOS
					// *********************************************************************************
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
							objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE")
									+ "_CREDITO");
					objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "CREDITO");
					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "29");
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "PAGOLC");
					objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
//					objectValidations.putInforCollectedForStructData("Transacc_Ind", "C");
//					objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C004");
					// TAGS UNICOS
					// *********************************************************************************

					tagsEncodeSensitiveData("CLIENT_CARD_NR_1",
							"00" + msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4) + "0000000000",
							objectValidations);
					objectValidations.putInforCollectedForStructData("CARD_CLASS", "00");
					tagsEncodeSensitiveData("Tarjeta_Amparada", "0000000000000000", objectValidations);

					objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2)
									.substring(msg.getFieldLength(Iso8583.Bit._103_ACCOUNT_ID_2) - 17));

					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
							(msg.getProcessingCode().getFromAccount().equals("10")) ? "05" : "04");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
							(msg.getProcessingCode().getFromAccount().equals("10")) ? "AHO" : "CTE");

					tagsEncodeSensitiveData("SEC_ACCOUNT_NR", msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 18), objectValidations);
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE",
							msg.getProcessingCode().getFromAccount());// ??????????

					// TAGS ISC
					// ****************************************************************************
					objectValidations.putInforCollectedForStructData("P_CODE", "000000");
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
					objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS", "15CLASE12NB");
					// TAGS ISC
					// ****************************************************************************

					break;
				case "051":

					validationCardsAccountMixtaDebitCreditPaymentObligationsWithCard(msg, objectValidations,
							msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3), pc);

					break;

				default:
					break;
				}

			}

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
					"CNB_CONSULTA_COSTO_PAGO_OBLIGACIONES_"
							+ objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_CBN"));

			break;

		case Constants.Channels.PCODE_CONSULTA_DE_SALDO_Y_CUPO_CNB_A:
		case Constants.Channels.PCODE_CONSULTA_DE_SALDO_Y_CUPO_CNB_C:
		case Constants.Channels.PCODE_CONSULTA_DE_SALDO_Y_CUPO2_CNB_C:// consulta costo
		case Constants.Channels.PCODE_CONSULTA_DE_SALDO_Y_CUPO2_CNB_A:

			Pattern pattern = null;
			Matcher matcher = null;

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getProcessingCode().toString(),
					msg.getTrack2Data().getPan(), pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			if (msg.getProcessingCode().toString().equals("311000")
					|| msg.getProcessingCode().toString().equals("312000"))

				objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
						"CNB_CONSULTA_COSTO_CONSULTA_DE_SALDO");

			else
				objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT",
						"CNB_CONSULTA_COSTO_CONSULTA_DE_CUPO");

			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");

			objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
					(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
							: "04");
			objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
					(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "AHO"
							: "CTE");

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

//**********************************************************************************************************************************							

//**********************************************************************************************************************************
		default:

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), procCode, msg.getTrack2Data().getPan(),
					pc.getFromAccount(), msg.getTrack2Data().getExpiryDate(), Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			String account2 = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"cuenta trajo sp: " + account2 + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
					"LOG", this.nameInterface));

//				if (!account2.equals(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1))) {
//					objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0001", "14");
			// }

			objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "CNB_CONSULTA_COSTO_DEFAULT");
			objectValidations.putInforCollectedForStructData("VIEW_ROUTER", "V1");

			objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
					(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
							: "04");
			objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
					(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "AHO"
							: "CTE");

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
			break;

		}
	}

	private void tagsPObligSecAccounts(Super objectValidations, Base24Ath msg) throws XPostilion {

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2)
				.substring(msg.getFieldLength(Iso8583.Bit._103_ACCOUNT_ID_2) - 17));
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", msg.getProcessingCode().getFromAccount());
	}

	public static void tagsExtractRetDepTransPOblig(Super objectValidations, Base24Ath msg) {

		try {

			if (msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2)) {
				objectValidations.putInforCollectedForStructData("FI_CREDITO",
						msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7));
			}

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)) {

				objectValidations.putInforCollectedForStructData("FI_DEBITO",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4));
			}

			if (msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)) {

				objectValidations.putInforCollectedForStructData("Ent_Adq",
						msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
				objectValidations.putInforCollectedForStructData("Entidad",
						msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
			}

			if (msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA)) {
				objectValidations.putInforCollectedForStructData("INCOCREDITO",
						msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(36));
				objectValidations.putInforCollectedForStructData("Terminal_Ampliada",
						msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(4, 12));
				objectValidations.putInforCollectedForStructData("Identificador_Terminal",
						msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(12, 13));
			}

		} catch (XPostilion e) {
			EventRecorder.recordEvent(new InvalidMessage(new String[] { Constants.Config.NAME,
					"Method: [tagsExtractRetDepTransPOblig()]", e.getMessage() }));
			EventRecorder.recordEvent(e);
		}
	}

	private void validationCardsAccountMixtaDebitCreditPaymentPublicServicesWithoutCard(Base24Ath msg,
			Super objectValidations, String indicatorMixCreditDebidP103)
			throws XEncryptionKeyError, XFieldUnableToConstruct, XPostilion, Exception {

		if (indicatorMixCreditDebidP103.equals("0")) {// mixta tx
			// si convenio y tarjeta corresponsal

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion pago de servicios publicos 1.BB_EF_MIXTA", "LOG", this.nameInterface));

			// tags psp

			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_PSP") + "_MIXTA");
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "MIXTA");
			objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "0");
			objectValidations.putInforCollectedForStructData("Identificacion_Canal", "CB");

		} else if (indicatorMixCreditDebidP103.equals("2")) {// CREDITO
			// no aplica

		} else if (indicatorMixCreditDebidP103.equals("1")) {// DEBITO
			// solo tarjeta CNB

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion pago de servicios publicos 1.BB_EF_DEBITO", "LOG", this.nameInterface));

			// tags psp
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_PSP") + "_DEBITO");
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "DEBITO");
			objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "1");
			objectValidations.putInforCollectedForStructData("PRIM_COV_ABO", "2");
			objectValidations.putInforCollectedForStructData("Identificacion_Canal", "00");

		}

	}

	private void validationMixtaDebitoRetiroWithoutCard(Base24Ath msg, Super objectValidations,
			String indicatorMixCreditDebidP103)
			throws XEncryptionKeyError, XFieldUnableToConstruct, XPostilion, Exception {

		ProcessingCode ps = null;
		if (msg.getField(3).equals("890000")) {
			ps = new ProcessingCode(msg.getField(126).substring(22, 28));
		} else {
			ps = new ProcessingCode(msg.getField(3));
		}

		if (indicatorMixCreditDebidP103.equals("2")) {// mixta tx
			// si convenio y tarjeta corresponsal

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion Retiro OTP", "LOG", this.nameInterface));

			// tags retiro
			accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getTrack2Data().getPan(),
					objectValidations);

			Extract.tagsModelWithdrawalOtpMixed(objectValidations, msg);

			// TAGS ISC
			// -------------------------------------------------------------------------------------------------------

			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
					objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));
			objectValidations.putInforCollectedForStructData("CREDIT_CARD_CLASS",
					objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
			objectValidations.putInforCollectedForStructData("CREDIT_CUSTOMER_ID",
					objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
			objectValidations.putInforCollectedForStructData("CREDIT_CARD_NR",
					objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));

		} else if (indicatorMixCreditDebidP103.equals("1")) {// CREDITO
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion Retiro OTP", "LOG", this.nameInterface));

			// tags retiro
			accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getTrack2Data().getPan(),
					objectValidations);
			
			Extract.tagsModelWithdrawalOtpCredit(objectValidations, msg);
			objectValidations.putInforCollectedForStructData("FI_Tarjeta","000");

			// TAGS ISC
			// -------------------------------------------------------------------------------------------------------

			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
					objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));
			objectValidations.putInforCollectedForStructData("CREDIT_CARD_CLASS",
					objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
			objectValidations.putInforCollectedForStructData("CREDIT_CUSTOMER_ID",
					objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
			objectValidations.putInforCollectedForStructData("CREDIT_CARD_NR",
					objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));

		} else if (indicatorMixCreditDebidP103.equals("0")) {// DEBITO
			// solo tarjeta CNB

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion Retiro OTP", "LOG", this.nameInterface));

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Validaciones cnb credito, entrando a accountsByNumberClientCNB", "LOG", this.nameInterface));

			accountsByNumberClientCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4),
					ps.getFromAccount().substring(0, 1) + "0", objectValidations);
			
			Extract.tagsModelTransferOtpDebit(objectValidations, msg);
			objectValidations.putInforCollectedForStructData("FI_Tarjeta","000");
			objectValidations.putInforCollectedForStructData("Tarjeta_Amparada","0000000000000000");
//			Extract.tagsModelWithdrawalOtpDebit(objectValidations, msg);


			// TAGS ISC
			// -------------------------------------------------------------------------------------------------------
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_NR"));
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
					objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE"));
			objectValidations.putInforCollectedForStructData("CORRES_CARD_NR",
					msg.getField(112));
			

		}

	}

	private void validationCardsAccountMixtaDebitCreditPaymentPublicServicesWithCard(Base24Ath msg,
			Super objectValidations, String indicatorMixCreditDebidP103)
			throws XFieldUnableToConstruct, XEncryptionKeyError, XPostilion, Exception {

		Pattern pattern = null;
		Matcher matcher = null;

		ProcessingCode ps = null;
		if (msg.getField(3).equals("890000")) {
			ps = new ProcessingCode(msg.getField(126).substring(22, 28));
		} else {
			ps = new ProcessingCode(msg.getField(3));
		}

		if (indicatorMixCreditDebidP103.equals("0")) {// mixta tx

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion pago de servicios 0.MIXTA", "LOG", this.nameInterface));

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(),
					msg.getTrack2Data().getPan(), ps.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,

					objectValidations);

			// tags psp
			// *****************************************************************************************************
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_PSP") + "_MIXTA");
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "MIXTA");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
					(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
							: "04");
			objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
					(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "AHO"
							: "CTE");
			objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "0");
			objectValidations.putInforCollectedForStructData("Identificacion_Canal", "CB");

			// tags psp
			// *****************************************************************************************************

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos primera y segunda validacion: mixta "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			// TAGS ISC -
			// Extract*****************************************************************************************************
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
			// TAGS ISC - Extract
			// *****************************************************************************************************

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
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", ps.getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			} else if (matcher.matches()) {
				msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
						(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
			}

		} else if (indicatorMixCreditDebidP103.equals("2")) {// CREDITO

			// viene solo cuenta corresponsal.

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion pago de servicios publicos 2.CREDITO", "LOG", this.nameInterface));

			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP", "CREDITO");
			objectValidations.putInforCollectedForStructData("P_CODE", "000000");

			if (msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(5, 9).equals("0001")) {
				accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getTrack2Data().getPan(),
						objectValidations);
				objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
						objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
				objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID",
						objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
				objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
						objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));

			}

			// tags psp
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_PSP") + "_CREDITO");
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "CREDITO");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "05");
			objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "AHO");
			tagsEncodeSensitiveData("DEBIT_CARD_NR", "0066010000000000", objectValidations);
			tagsEncodeSensitiveData("Tarjeta_Amparada", msg.getTrack2Data().getPan(), objectValidations);
			objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS", "00");
			objectValidations.putInforCollectedForStructData("Vencimiento", "0000");
			objectValidations.putInforCollectedForStructData("Ind_4xmil", "0");
			objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID", "0000000000000");
			objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "2");
			objectValidations.putInforCollectedForStructData("Identificacion_Canal", "C0");

		} else if (indicatorMixCreditDebidP103.equals("1")) {// DEBITO

			// viene solo cuenta cliente.

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro pago de servicios tipo 1.DEBITO", "LOG", this.nameInterface));

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(),
					msg.getTrack2Data().getPan(), ps.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos cuentaCNB validacion: DEBITO  "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			// tags psp
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_PSP") + "_DEBITO");
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "DEBITO");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
					(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
							: "04");
			objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
					(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "AHO"
							: "CTE");
			objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "1");
			objectValidations.putInforCollectedForStructData("PRIM_COV_ABO", "2");
			objectValidations.putInforCollectedForStructData("Identificacion_Canal", "00");

			// tags isc

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
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", ps.getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			} else if (matcher.matches()) {
				msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
						(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
			}

		}

	}

	private void validationCardsAccountMixtaDebitCreditPaymentObligationsWithCard(Base24Ath msg,
			Super objectValidations, String indicatorMixCreditDebidP103, ProcessingCode pc)
			throws XFieldUnableToConstruct, XEncryptionKeyError, XPostilion, Exception {

		Pattern pattern = null;
		Matcher matcher = null;

		ProcessingCode ps = null;
		if (msg.getField(3).equals("890000")) {
			ps = new ProcessingCode(msg.getField(126).substring(22, 28));
		} else {
			ps = new ProcessingCode(msg.getField(3));
		}

		if (indicatorMixCreditDebidP103.equals("2")) {// mixta tx
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion pago de O 2.MIXTA", "LOG", this.nameInterface));

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(),
					msg.getTrack2Data().getPan(), ps.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			// TAGS EXTRACT
			// ***************************************************************************
			Extract.tagsModelPaymentOfObligationsMixed(objectValidations, msg);

			if (ps.toString().equals(Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS)
					|| ps.toString().equals(Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE)) {
				
				objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "PAGOCB");
				objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "CRE");
				objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE", "CRE");
				objectValidations.putInforCollectedForStructData("BIN_Cuenta",
						(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
								? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8, 14)
								: "000000");
				ATM.tagTTypePOblig(msg, objectValidations);

			}

			// TAGS EXTRACT
			// ***************************************************************************

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

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos primera y segunda validacion: mixta "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));
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
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", ps.getFromAccount());
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

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(),
					msg.getTrack2Data().getPan(), ps.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

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
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", ps.getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			}

			// TAGS EXTRACT
			// ****************************************************************************

			if (ps.toString().equals(Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS)
					|| ps.toString().equals(Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE)) {
				
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
					
					ATM.tagTTypePOblig(msg, objectValidations);

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
				
			}else {
				
				Extract.tagsModelPaymentOfObligationsDebit(objectValidations, msg);
				
				objectValidations.putInforCollectedForStructData("Dispositivo", "0");
				objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "LCR");
				objectValidations.putInforCollectedForStructData("Entidad", "0000");
				
			}
			

			// TAGS EXTRACT
			// ****************************************************************************

		}

	}

	public static void validationCardsAccountMixtaDebitCreditPaymentObligationsWithoutCard(Base24Ath msg,
			Super objectValidations, String indicatorMixCreditDebidP103)
			throws XEncryptionKeyError, XFieldUnableToConstruct, XPostilion, Exception {
		
		ProcessingCode ps = null;
		if (msg.getField(3).equals("890000")) ps = new ProcessingCode(msg.getField(126).substring(22, 28));
		else ps = new ProcessingCode(msg.getField(3));
		

		if (indicatorMixCreditDebidP103.equals("2")) {
			Extract.tagsModelPaymentOfObligationsMixedWithoutCard(objectValidations, msg);
			
			if (ps.toString().equals(Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS)
					|| ps.toString().equals(Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE)) {
				
				objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "PAGOCB");
				objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "CRE");
				objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE", "CRE");
				objectValidations.putInforCollectedForStructData("BIN_Cuenta",
						(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
								? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8, 14)
								: "000000");
				ATM.tagTTypePOblig(msg, objectValidations);

			}

		} else if (indicatorMixCreditDebidP103.equals("0")) {
			
			
			if (ps.toString().equals(Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS)
					|| ps.toString().equals(Constants.Channels.PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE)) {
				
				switch (msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7)) {

				case "0002":
				case "0052":
				case "0023":
					Extract.tagsModelPaymentOfObligationsDebitWithoutCard(objectValidations, msg);
					
					objectValidations.putInforCollectedForStructData("Dispositivo", "0");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "LCR");
					objectValidations.putInforCollectedForStructData("Entidad", "0000");

					break;
				case "0001":

					Extract.tagsModelPaymentOfObligationsMixedWithoutCard(objectValidations, msg);
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "PAGOCB");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "CRE");
					objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE", "CRE");
					objectValidations.putInforCollectedForStructData("BIN_Cuenta",
							(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
									? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8, 14)
									: "000000");
					
					ATM.tagTTypePOblig(msg, objectValidations);

					break;

				default:
					Extract.tagsModelPaymentOfObligationsDebitWithoutCard(objectValidations, msg);
					objectValidations.putInforCollectedForStructData("FI_CREDITO", "0014");
					objectValidations.putInforCollectedForStructData("Ent_Adq", "0054");
					objectValidations.putInforCollectedForStructData("Dispositivo", "0");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "LCR");
					objectValidations.putInforCollectedForStructData("Entidad", "0000");
					
					break;
				}
				
			}else {
				
				Extract.tagsModelPaymentOfObligationsDebitWithoutCard(objectValidations, msg);
				
				objectValidations.putInforCollectedForStructData("Dispositivo", "0");
				objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "LCR");
				objectValidations.putInforCollectedForStructData("Entidad", "0000");
				
			}

		}

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
	private void validationCardsAccountMixtaDebitCreditRetiroCNB(Base24Ath msg, Super objectValidations,
			String indicatorMixCreditDebidP103) throws XFieldUnableToConstruct, XPostilion, Exception {

		String account2 = null;
		Pattern pattern = null;
		Matcher matcher = null;

		ProcessingCode ps = null;
		if (msg.getField(3).equals("890000")) {
			ps = new ProcessingCode(msg.getField(126).substring(22, 28));
		} else {
			ps = new ProcessingCode(msg.getField(3));
		}

		if (indicatorMixCreditDebidP103.equals("2")) {// mixta tx
			// en este caso de retiro con corresponsal mirar las otras opciones.

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion donde el 103 trae el 2 en la 3 poscicion 2.MIXTA", "LOG", this.nameInterface));

			if (msg.getTrack2Data().getPan().equals(msg.getField("112"))) {

				objectValidations.modifyAttributes(false, " MISMA TARJETA CNB Y CLIENTE", "0001", "14");// ERROR 12

			} else {

				accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getField("112"), objectValidations);

				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"trajo datos primera validacion: mixta  "
								+ objectValidations.getInforCollectedForStructData().toString(),
						"LOG", this.nameInterface));

				if (objectValidations.getValidationResult()) {

					accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(),
							msg.getTrack2Data().getPan(), ps.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
							msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
									? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
											.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
									: Constants.General.SIXTEEN_ZEROS,
							objectValidations);
					// TAGS UNICOS
					// **********************************************************************************************
					objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "MIXTA");
					objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
							objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_MIXTA");

					objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "18");
					objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
					objectValidations.putInforCollectedForStructData("Indicador_AVAL", "0");
					objectValidations.putInforCollectedForStructData("Indicador_AVALX2", "1");
					objectValidations.putInforCollectedForStructData("Transacc_Ind", "T");
					objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C003");

					// TAGS UNICOS
					// **********************************************************************************************

					// TAGS accountsCNB Y accountsClienteCBN
					// *********************************************************************

					// datos tarjeta
					tagsAccountsClienteCNBCard(objectValidations);
					// datos cuentas
					tagsAccountsCNBAccount(objectValidations);
					// TAGS accountsCNB Y accountsClienteCBN

					// ********************************************************************

					objectValidations.putInforCollectedForStructData("Tarjeta_Amparada",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR_1"));
					objectValidations.putInforCollectedForStructData("ID_CLIENT",
							objectValidations.getInforCollectedForStructData().get("CUSTOMER_ID"));

					objectValidations.putInforCollectedForStructData("Codigo_Transaccion_ProductoX1",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10"))
									? "05"
									: "04");
					objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_DebitadaX1",
							(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10"))
									? "AHO"
									: "CTE");
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));

					tagsEncodeSensitiveData("MIX_ACCOUNT_NR", "000000000000000000", objectValidations);
					objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE", "00");

					// TAGS ISC
					// ***************************************************************************************************
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
							objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
					objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));

					objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
							objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
					objectValidations.putInforCollectedForStructData("CREDIT_CARD_CLASS",
							objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
					objectValidations.putInforCollectedForStructData("CREDIT_CUSTOMER_ID",
							objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
					objectValidations.putInforCollectedForStructData("CREDIT_CARD_NR",
							objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
					// TAGS ISC
					// ***************************************************************************************************

					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"trajo datos primera y segunda validacion: mixta "
									+ objectValidations.getInforCollectedForStructData().toString(),
							"LOG", this.nameInterface));

					account2 = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
//					pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
//					matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

//					if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
//							&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
//									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))) {
//						objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
//					}

					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"trajo datos primera y segunda validacion: mixta "
									+ objectValidations.getInforCollectedForStructData().toString(),
							"LOG", this.nameInterface));

					this.udpClient.sendData(Client.getMsgKeyValue(
							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "cuenta trajo sp: " + account2
									+ " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
							"LOG", this.nameInterface));

					pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
					matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

					if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))
							&& !matcher.matches()) {
						objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
						objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
								msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
						objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", ps.getFromAccount());
						objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
						objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
					} else if (matcher.matches()) {
						msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
								(this.encodeData) ? new String(Base64.getDecoder().decode(account2)) : account2);
					}

				}
			}

		} else if (indicatorMixCreditDebidP103.equals("1")) {
			// viene solo cuenta corresponsal. CREDITO
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos cuentaCNB validacion: credito  "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion donde el 103 trae el 2 en la 3 poscicion 1.CREDITO", "LOG", this.nameInterface));

			accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getField("112"), objectValidations);

			// TAGS UNICOS
			// ************************************************************************************

			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "CREDITO");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_CREDITO");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "03");
			objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
			objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
			objectValidations.putInforCollectedForStructData("Transacc_Ind", "Q");
			objectValidations.putInforCollectedForStructData("Vencimiento", "9912");
			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C001");

			// TAGS UNICOS
			// *************************************************************************************

			tagsEncodeSensitiveData("Tarjeta_Amparada", "000000000000000000", objectValidations);
			objectValidations.putInforCollectedForStructData("ID_CLIENT",
					objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
			tagsEncodeSensitiveData("SEC_ACCOUNT_NR", msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4),
					objectValidations);
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", ps.getFromAccount());

			// TAGS accountsCNN
			// ***************************************************************************************

			tagsAccountsCNBCard(objectValidations);
			tagsAccountsCNBAccount(objectValidations);
			objectValidations.putInforCollectedForStructData("FI_Tarjeta", "000000");
//			objectValidations.putInforCollectedForStructData("CARD_CLASS","");

			// TAGS accountsCBN
			// ***************************************************************************************

			// TAGS ISC
			// ************************************************************************************************
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
					objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));

			tagsEncodeSensitiveData("DEBIT_ACCOUNT_NR", msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4),
					objectValidations);
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE", ps.getFromAccount());
			// TAGS ISC
			// ************************************************************************************************

		} else if (indicatorMixCreditDebidP103.equals("0")) {
			// viene solo cuenta cliente. DEBITO

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Entro validacion donde el 103 trae el 2 en la 3 poscicion 0.DEBITO", "LOG", this.nameInterface));

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(),
					msg.getTrack2Data().getPan(), msg.getProcessingCode().getFromAccount(),
					msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos cuentaCNB validacion: DEBITO  "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			// TAGS UNICOS
			// *************************************************************************************
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "DEBITO");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_DEBITO");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "23");
			objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSD");
			objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
			objectValidations.putInforCollectedForStructData("Transacc_Ind", "S");

			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C002");

			// TAGS UNICOS
			// *************************************************************************************

			// TAGS accountsClienteCBN
			// ***************************************************************************

			tagsAccountsClienteCNBCard(objectValidations);
			tagsAccountsClienteCNBAccount(objectValidations);

			// TAGS accountsClienteCBN
			// ****************************************************************************

			objectValidations.putInforCollectedForStructData("Tarjeta_Amparada",
					objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR_1"));
			objectValidations.putInforCollectedForStructData("ID_CLIENT",
					objectValidations.getInforCollectedForStructData().get("CUSTOMER_ID"));
			tagsEncodeSensitiveData("SEC_ACCOUNT_NR", "000000000000000000", objectValidations);
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "20");

			// TAGS ISC
			// ************************************************************************************************
			tagsEncodeSensitiveData("CREDIT_ACCOUNT_NR", "000000000000000000", objectValidations);
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE", "20");

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
			// ************************************************************************************************

//			account2 = objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR");
////			pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
////			matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
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
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", ps.getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			} else if (matcher.matches()) {
				msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
						(this.encodeData) ? new String(Base64.getDecoder().decode(account)) : account);
			}

		}

	}

	private static void tagsAccountsClienteCNBAccount(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");

	}

	private static void tagsAccountsClienteCNBCard(Super objectValidations) {
		objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
		objectValidations.putInforCollectedForStructData("PAN_Tarjeta",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
		objectValidations.putInforCollectedForStructData("FI_Tarjeta",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR").substring(0, 6));
		objectValidations.putInforCollectedForStructData("CARD_CLASS",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
	}

	private static void tagsAccountsCNBAccount(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");

	}

	private static void tagsAccountsCNBCard(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1",
				objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
		objectValidations.putInforCollectedForStructData("FI_Tarjeta",
				objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR").substring(0, 6));
		objectValidations.putInforCollectedForStructData("PAN_Tarjeta",
				objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
		objectValidations.putInforCollectedForStructData("CARD_CLASS",
				objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
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
	private void validationCardsAccountMixtaDebitCreditDepositoCNB(Base24Ath msg, Super objectValidations,
			String indicatorMixCreditDebidP103) throws XFieldUnableToConstruct, XPostilion, Exception {

		String account2 = null;
		ProcessingCode ps = null;
		if (msg.getField(3).equals("890000")) {
			ps = new ProcessingCode(msg.getField(126).substring(22, 28));
		} else {
			ps = new ProcessingCode(msg.getField(3));
		}

		switch (indicatorMixCreditDebidP103) {

		case "0":// DEBITO

			accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getTrack2Data().getPan(),
					objectValidations);

			// TAGS UNICOS
			// ************************************************************************************************
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "DEBITO");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_DEBITO");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "23");
			objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSD");
			objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
			objectValidations.putInforCollectedForStructData("Transacc_Ind", "D");
			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "0000");
			// TAGS UNICOS
			// ************************************************************************************************

			// TAGS CUENTAS Y TARJETA
			// ************************************************************************************

			tagsAccountsCNBCard(objectValidations);
			tagsAccountsCNBAccount(objectValidations);

			objectValidations.putInforCollectedForStructData("Tarjeta_Amparada",
					objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR_1"));
			objectValidations.putInforCollectedForStructData("ID_CLIENT",
					objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
			tagsEncodeSensitiveData("SEC_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);

			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "OTR");

			// TAGS CUENTAS Y TARJETA
			// ************************************************************************************

			// TAGS ISC
			// ***********************************************************************************************
			tagsEncodeSensitiveData("CREDIT_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
					ps.getToAccount().substring(0, 1) + "0");
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
					objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));
			objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
					objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
			objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID",
					objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
			objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
					objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
			// TAGS ISC
			// ***********************************************************************************************
			account2 = objectValidations.getInforCollectedForStructData().get("DEBIT_ACCOUNT_NR");

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
					&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))) {
				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
			}
			break;
		case "2":// MIXTA

			// TAGS UNICOS
			// ********************************************************************************************
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "MIXTA");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_MIXTO");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "18");
			objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
			objectValidations.putInforCollectedForStructData("Indicador_AVAL", "0");
			objectValidations.putInforCollectedForStructData("Transacc_Ind", "F");
			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "0000");
			// TAGS UNICOS
			// ********************************************************************************************

			accountsCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msg.getTrack2Data().getPan(),
					objectValidations);

			// TAGS CUENTAS Y TARJETAS
			// *********************************************************************************************

			tagsAccountsCNBCard(objectValidations);

			objectValidations.putInforCollectedForStructData("Tarjeta_Amparada",
					objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR_1"));
			objectValidations.putInforCollectedForStructData("ID_CLIENT",
					objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
			tagsEncodeSensitiveData("PRIM_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
					((msg.getProcessingCode().getToAccount().substring(0, 1) + "0").equals("10")) ? "05" : "04");
			objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
					((msg.getProcessingCode().getToAccount().substring(0, 1) + "0").equals("10")) ? "AHO" : "CTE");

			tagsAccountsCNBAccountX2(objectValidations);
			tagsAccountsMix(objectValidations, msg);

			// TAGS CUENTAS Y TARJETAS
			// ********************************************************************************

			// TAGS ISC
			// ********************************************************************************************
			tagsEncodeSensitiveData("CREDIT_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_NR",
					objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
					ps.getToAccount().substring(0, 1) + "0");
			objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
					objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));
			objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS",
					objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
			objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID",
					objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
			objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR",
					objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
			// TAGS ISC
			// ******************************************************************************************************
			account2 = objectValidations.getInforCollectedForStructData().get("DEBIT_ACCOUNT_NR");

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
					&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))) {
				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
			}
			break;

		case "1":// CREDITO
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Validaciones cnb credito", "LOG", this.nameInterface));
			// TAGS UNICOS
			// *******************************************************************************************
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "CREDITO");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_CREDITO");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "03");
			objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
			objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
			objectValidations.putInforCollectedForStructData("Transacc_Ind", "C");
			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "0000");
			// TAGS UNICOS
			// *******************************************************************************************
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Validaciones cnb credito, entrando a accountsByNumberClientCNB", "LOG", this.nameInterface));
			accountsByNumberClientCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"0" + msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					ps.getToAccount().substring(0, 1) + "0", objectValidations);

			// TAGS CUENTA Y TARJETA
			// ********************************************************************************

			tagsEncodeSensitiveData("CLIENT_CARD_NR_1",
					"00" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4) + "0000000000",
					objectValidations);
			tagsEncodeSensitiveData("PAN_Tarjeta",
					"00" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4) + "0000000000000",
					objectValidations);
			objectValidations.putInforCollectedForStructData("CARD_CLASS", "00");
			objectValidations.putInforCollectedForStructData("Vencimiento", "0000");
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
					msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4));
			objectValidations.putInforCollectedForStructData("ID_CLIENT", "0000000000000");
			tagsEncodeSensitiveData("Tarjeta_Amparada", "0000000000000000", objectValidations);
			tagsAccountsByNumberClientCNBAccount(objectValidations);
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", ps.getFromAccount());

			// TAGS CUENTA Y TARJETA
			// *********************************************************

			// TAGS ISC
			// ***********************************************************************
			tagsEncodeSensitiveData("CREDIT_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);
			objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
					ps.getToAccount().substring(0, 1) + "0");
			// TAGS ISC
			// ***********************************************************************

			break;

		default:

			break;

		}

	}

	private static void tagsAccountsCNBAccountX2(Super objectValidations) {
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE",
				objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE"));
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_ProductoX1",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_DebitadaX1",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");
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
	private void validationCardsAccountMixtaDebitCreditTransferCNB(Base24Ath msg, Super objectValidations,
			String indicatorMixCreditDebidP103) throws XFieldUnableToConstruct, XPostilion, Exception {

		String account2 = null;
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
					"ENTRO TRANSFERENCIA CREDITO", "LOG", this.nameInterface));
			// TAGS UNICOS
			// ***************************************************************************************************
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "DEBITO");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_DEBITO");

			objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "23");
			objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSD");
			objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
//			objectValidations.putInforCollectedForStructData("Transacc_Ind", "D");
//			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C002");
			// TAGS UNICOS
			// ***************************************************************************************************

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(),
					msg.getTrack2Data().getPan(), ps.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

			// TAGS accountsClienteCNB
			// *****************************************************************************

			tagsAccountsClienteCNBCard(objectValidations);
			tagsAccountsClienteCNBAccount(objectValidations);

			// TAGS accountsClienteCNB
			// *****************************************************************************

			tagsEncodeSensitiveData("SEC_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", ps.getToAccount());
			objectValidations.putInforCollectedForStructData("CLIENT2_ACCOUNT_TYPE", ps.getToAccount());
			objectValidations.putInforCollectedForStructData("Tarjeta_Amparada",
					objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR_1"));
			objectValidations.putInforCollectedForStructData("ID_CLIENT",
					objectValidations.getInforCollectedForStructData().get("CUSTOMER_ID"));

			// TAGS ISC
			// **********************************************************************************************************
			tagsEncodeSensitiveData("CREDIT_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);
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

			account2 = objectValidations.getInforCollectedForStructData().get("DEBIT_ACCOUNT_NR");

//			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
//					&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
//							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))) {
//				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
//			}

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos primera y segunda validacion: mixta "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"cuenta trajo sp: " + account2 + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
					"LOG", this.nameInterface));

			pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
			matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
					&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))
					&& !matcher.matches()) {
				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", ps.getFromAccount());
				objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "MM");
				objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", "");
			} else if (matcher.matches()) {
				msg.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
						(this.encodeData) ? new String(Base64.getDecoder().decode(account2)) : account2);
			}

			break;
		case "1":// credito

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ENTRO TRANSFERENCIA CREDITO", "LOG", this.nameInterface));

			// TAGS UNICOS
			// ***************************************************************************************************
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "CREDITO");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_CREDITO");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "03");
			objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
			objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
//			objectValidations.putInforCollectedForStructData("Transacc_Ind", "C");
//			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C004");
			// TAGS UNICOS
			// ***************************************************************************************************

			accountsByNumberClientCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"0" + msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7), ps.getToAccount(),
					objectValidations);

			tagsEncodeSensitiveData("CLIENT_CARD_NR_1",
					"00" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4) + "0000000000",
					objectValidations);
			tagsEncodeSensitiveData("PAN_Tarjeta",
					"00" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4) + "0000000000000",
					objectValidations);
			tagsEncodeSensitiveData("Tarjeta_Amparada", "0000000000000000", objectValidations);
//			objectValidations.putInforCollectedForStructData("Tarjeta_Amparada",
//			"00" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4) + "0000000000");

			objectValidations.putInforCollectedForStructData("ID_CLIENT", "0000000000000");
			objectValidations.putInforCollectedForStructData("Vencimiento", "0000");

			tagsEncodeSensitiveData("SEC_ACCOUNT_NR", msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
					.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17), objectValidations);
			objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", ps.getFromAccount());

			// tags accountsByNumberClientCNB
			// *****************************************************************************************

			tagsAccountsByNumberClientCNBAccount(objectValidations);

			// tags accountsByNumberClientCNB
			// *****************************************************************************************

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
			// ************************************************************************************************
			objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "MIXTA");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE", "TRANSFERENCIA_MIXTA");
			objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
					objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_MIXTA");
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "18");
			objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
			objectValidations.putInforCollectedForStructData("Indicador_AVAL", "0");
//			objectValidations.putInforCollectedForStructData("Transacc_Ind", "F");
//			objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C003");
			// TAGS UNICOS
			// ************************************************************************************************

			accountsClienteCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), ps.toString(),
					msg.getTrack2Data().getPan(), ps.getFromAccount(), msg.getTrack2Data().getExpiryDate(),
					msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
							? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
							: Constants.General.SIXTEEN_ZEROS,
					objectValidations);

//			TAGS CUENTA Y TARJETA *************************************************************************************

			tagsAccountsClienteCNBCard(objectValidations);

			tagsEncodeSensitiveData("PRIM_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);
			objectValidations.putInforCollectedForStructData("ID_CLIENT",
					objectValidations.getInforCollectedForStructData().get("CUSTOMER_ID"));
			objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
					(ps.getToAccount().equals("10")) ? "05" : "04");
			objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
					(ps.getToAccount().equals("10")) ? "AHO" : "CTE");
			objectValidations.putInforCollectedForStructData("Tarjeta_Amparada",
					objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR_1"));
			tagsAccountsClienteCNBAccountX2(objectValidations);
			tagsAccountsMix(objectValidations, msg);

//			TAGS CUENTA Y TARJETA *************************************************************************************
//			***************************************************************************************************************

			// TAGS ISC
			// ****************************************************************************************************

			tagsEncodeSensitiveData("CLIENT2_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);
			objectValidations.putInforCollectedForStructData("CLIENT2_ACCOUNT_TYPE", ps.getToAccount());
			tagsEncodeSensitiveData("CREDIT_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);
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
			// ****************************************************************************************************
			account2 = objectValidations.getInforCollectedForStructData().get("DEBIT_ACCOUNT_NR");

//			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
//					&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
//							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))) {
//				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
//			}

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"trajo datos primera y segunda validacion: mixta "
							+ objectValidations.getInforCollectedForStructData().toString(),
					"LOG", this.nameInterface));

			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"cuenta trajo sp: " + account2 + " cuenta p102: " + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1),
					"LOG", this.nameInterface));

			pattern = Pattern.compile("0{" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() + "}");
			matcher = pattern.matcher(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));

			if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
					&& !account2.equals("0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17))
					&& !matcher.matches()) {
				objectValidations.modifyAttributes(false, "CUENTA NO EXISTENTE", "0014", "14");
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
						msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
				objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_TYPE", ps.getFromAccount());
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

	private void tagsAccountsMix(Super objectValidations, Base24Ath msg) throws XPostilion {

		try {

			tagsEncodeSensitiveData("MIX_ACCOUNT_NR", msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
					objectValidations);

			objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE",
					msg.getProcessingCode().getToAccount().substring(0, 1) + "0");

		} catch (XPostilion e) {

			EventRecorder.recordEvent(new InvalidMessage(
					new String[] { Constants.Config.NAME, "Method: [tagsAccountsMix()]", e.getMessage() }));
			EventRecorder.recordEvent(e);
			StringWriter outError = new StringWriter();
			e.printStackTrace(new PrintWriter(outError));
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"catch metodo tagsAccountsMix (): " + outError.toString(), "LOG", this.nameInterface));
		}
	}

	private static void tagsAccountsClienteCNBAccountX2(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE"));
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_ProductoX1",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_DebitadaX1",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");

	}

	public static void tagsAccountsByNumberClientCNBAccount(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");

	}

	/**
	 * El metodo se encarga de validar que la tarjeta del corresponsal exista. Si la
	 * tarjeta existe trae informacion de base de datos relacionada a la tarjeta y a
	 * su cuenta. Modifica al objeto Super almacenando la informacion encontrada en
	 * base. de lo contrario genera una declinacion.
	 * 
	 * @param hashPanCNB
	 * @param objectValidations
	 * @throws Exception
	 */
	public void accountsCNB(String p37, String pan, Super objectValidations) throws Exception {

		String hashPanCNB = Utils.getHashPanCNB(pan);

		CallableStatement stmt = null;
		ResultSet rs = null;
		Connection con = null;

		this.udpClient
				.sendData(Client.getMsgKeyValue(p37, "Pan hasheado entrada: " + hashPanCNB, "LOG", this.nameInterface));

		try {
			con = JdbcManager.getConnection(Account.POSTCARD_DATABASE);
			stmt = con.prepareCall(StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME_CNB);
			stmt.setString(1, hashPanCNB);
			stmt.registerOutParameter(2, java.sql.Types.VARCHAR);// customer id
			stmt.registerOutParameter(3, java.sql.Types.VARCHAR);// default account type
			stmt.registerOutParameter(4, java.sql.Types.VARCHAR);// name
			stmt.registerOutParameter(5, java.sql.Types.INTEGER);// issure
			stmt.registerOutParameter(6, java.sql.Types.VARCHAR);// extended field
			stmt.registerOutParameter(7, java.sql.Types.VARCHAR);// sequence nr
			stmt.execute();

			String customerId = stmt.getString(2);// customer id
			String defaultAccountType = stmt.getString(3);// default account type
			String customerName = stmt.getString(4);// name
			int issuerNr = stmt.getInt(5);
			String extendedField = stmt.getString(6);// extended field
			String sequenceNr = stmt.getString(7);

			this.udpClient.sendData(Client.getMsgKeyValue(p37,
					"validacion CNB. primer SP customerID " + customerId + " ceuntaDefecto: " + defaultAccountType
							+ " name: " + customerName + " issure: " + issuerNr + " extendedfield: " + extendedField
							+ " seq_nr: " + sequenceNr,
					"LOG", this.nameInterface));
			stmt.close();
			if (!(issuerNr == 0)) {

				if (!extendedField.equals("15CLASE12NB")) {
					objectValidations.modifyAttributes(false, " TRN FALLBACK NO PERMITIDA", "U8", "83");
				} else {

					this.udpClient.sendData(Client.getMsgKeyValue(p37, "Esto entra al segundo sp: issuerNr:" + issuerNr
							+ " hashPanCNB: " + hashPanCNB + " seqNr: " + sequenceNr, "LOG", this.nameInterface));

					stmt = con.prepareCall(StoreProcedures.CM_LOAD_CARD_ACCOUNTS);
					stmt.setInt(1, issuerNr);
					stmt.setString(2, hashPanCNB);
					stmt.setString(3, sequenceNr);
					rs = stmt.executeQuery();
					rs.next();
					String accountType = rs.getString(ColumnNames.ACCOUNT_TYPE);// account type
					String accountNumber = rs.getString(ColumnNames.ACCOUNT_ID);// numero de cuenta

					objectValidations.putInforCollectedForStructData("CORRES_CARD_CLASS", extendedField);
					objectValidations.putInforCollectedForStructData("CORRES_CUSTOMER_ID", customerId);

					tagsEncodeSensitiveData("CORRES_ACCOUNT_NR", Utils.getClearAccount(accountNumber),
							objectValidations);
					tagsEncodeSensitiveData("CORRES_CARD_NR", pan, objectValidations);

					objectValidations.putInforCollectedForStructData("CUSTOMER_NAME", customerName);
					objectValidations.putInforCollectedForStructData("CORRES_ACCOUNT_TYPE", accountType);
					objectValidations.putInforCollectedForStructData("P_CODE", "000000");

					JdbcManager.commit(con, stmt, rs);
					this.udpClient.sendData(Client.getMsgKeyValue(p37,
							"validacion CNB. segundo SP salida : customerID " + customerId + " ceuntaDefecto: "
									+ defaultAccountType + " name: " + customerName + " issure: " + issuerNr
									+ " extendedfield: " + extendedField + " seq_nr: " + sequenceNr + " account type "
									+ accountType + " accountNumbe: " + accountNumber,
							"LOG", this.nameInterface));

				}

			} else {
				objectValidations.modifyAttributes(false, " NO EXITE TARJETA CNB", "0001", "14");// Error 12

			}

		} catch (SQLException | XEncryptionKeyError e) {
			e.printStackTrace();
			EventRecorder
					.recordEvent(
							new SQLExceptionEvent(new String[] {
									Account.POSTCARD_DATABASE, StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME
											+ General.VOIDSTRING + StoreProcedures.CM_LOAD_CARD_ACCOUNTS,
									e.getMessage() }));

			StringWriter outError = new StringWriter();
			e.printStackTrace(new PrintWriter(outError));
			this.udpClient.sendData(Client.getMsgKeyValue(p37, "catch BUSQUEDA CUENTA CNB : " + outError.toString(),
					"LOG", this.nameInterface));

		} finally {
			try {
				JdbcManager.cleanup(con, stmt, rs);
			} catch (SQLException e) {
				this.udpClient.sendData(Client.getMsgKeyValue(p37, e.getMessage(), "LOG", this.nameInterface));
				EventRecorder
						.recordEvent(new SQLExceptionEvent(new String[] {
								Account.POSTCARD_DATABASE, StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME
										+ General.VOIDSTRING + StoreProcedures.CM_LOAD_CARD_ACCOUNTS,
								e.getMessage() }));
			}
		}

	}

	
	
}
