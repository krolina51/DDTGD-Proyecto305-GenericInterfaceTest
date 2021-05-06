package postilion.realtime.genericinterface.channels;

import java.util.Arrays;
import java.util.HashMap;

import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.genericinterface.translate.validations.Validation.ErrorMessages;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.XFieldUnableToConstruct;
import postilion.realtime.sdk.util.XPostilion;

public class VTL extends Super {
	private Client udpClient = null;
	private String nameInterface = "";

	public VTL(Boolean validationResult, String descriptionError, String errorCodeAUTRA, String errorCodeISO,
			HashMap<String, String> inforCollectedForStructData, Parameters params) {
		super(validationResult, descriptionError, errorCodeAUTRA, errorCodeISO, inforCollectedForStructData, params);
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
	}

	public void validations(Base24Ath msg, Super objectValidations) {

		try {

			switch (msg.getMsgType()) {

			case Iso8583.MsgType._0200_TRAN_REQ:
			case Iso8583.MsgType._0420_ACQUIRER_REV_ADV:

				objectValidations.putInforCollectedForStructData("CARD_NUMBER", msg.getTrack2Data().getPan());
				switch (msg.getProcessingCode().toString()) {

				// TRANFERENCIAS VTL.
				case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_AHORROS:
				case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_CORRIENTE:
				case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_CORRIENTE:
				case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_AHORROS:

					String[] terminalsID = { "8354", "8110", "9631", "9632" };
					String terminalId = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(4, 8);
					
					if (Arrays.stream(terminalsID).anyMatch(terminalId::equals)) {
						
						this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
								"Entro validacion CNB TRANSFERENCIA Masiva", "LOG", this.nameInterface));
						this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
								"Indicador tipo TRANSFERENCIA:"
										+ msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3),
								"LOG", this.nameInterface));
						
						
						objectValidations.putInforCollectedForStructData("TRANSACTION_INPUT", "VTL_TRANSFERENCIA");
						
						switch (msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(2, 3)) {
						case "1":
							
							this.udpClient
									.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
											"ENTRO TRANSFERENCIA VTL CREDITO", "LOG", this.nameInterface));
							
							objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "CREDITO");
							objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
									"TRANSFERENCIA_CREDITO");

							constructTagsby125field(msg, objectValidations);
							
//							accountsByNumberClientCNB(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
//									"0" + msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7),
//									msg.getProcessingCode().getToAccount(), objectValidations);
							
							objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "03");
							objectValidations.putInforCollectedForStructData("Ent_Adq", msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0,4));
							objectValidations.putInforCollectedForStructData("FI_DEBITO", msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0,4));
							objectValidations.putInforCollectedForStructData("Numero_Cuenta", "0" + msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7));
							objectValidations.putInforCollectedForStructData("BIN_Tarjeta", msg.getField(Iso8583.Bit._035_TRACK_2_DATA).substring(0,6));
							objectValidations.putInforCollectedForStructData("Numero_Tarjeta", msg.getTrack2Data().getPan());
							tagsEncodeSensitiveData("SEC_ACCOUNT_NR", msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
									.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17), objectValidations);
							objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE",
									(msg.getProcessingCode().getFromAccount().equals("10"))? "AHO" : "CTE");								
//							objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
							
							switch (msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(16,17)) {
							case "5":
								
								objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto","06");
								objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "PAGOLC");
								objectValidations.putInforCollectedForStructData("Mod_Credito", "8");
								objectValidations.putInforCollectedForStructData("Clase_Pago", "2");
							
							
							break;
							default:
								
//								CNB.tagsAccountsByNumberClientCNBAccount(objectValidations);
								
								
								// TAGS UNICOS
								// ***************************************************************************************************
								
								objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
								objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
										(msg.getProcessingCode().getToAccount().equals("10")) ? "05"
												: "04");
								objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
										(msg.getProcessingCode().getToAccount().equals("10")) ? "AHO"
												: "CTE");
//								objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C004");
//								objectValidations.putInforCollectedForStructData("Transacc_Ind", "C");
								
								// ***************************************************************************************************
								// TAGS UNICOS
								
								//TAGS ISC *******************************************************************************************************
								
								
								objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_NR",
										objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_NR"));
								
								tagsEncodeSensitiveData("DEBIT_ACCOUNT_NR", msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
										.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 18), objectValidations);
								
								objectValidations.putInforCollectedForStructData("CREDIT_ACCOUNT_TYPE",
										objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE"));
								
								objectValidations.putInforCollectedForStructData("DEBIT_ACCOUNT_TYPE",
										msg.getProcessingCode().getFromAccount());
								
								
								
								
								break;
							}
							
							
							
							
							

							break;
						default:
							objectValidations.modifyAttributes(false, "TIPO DE TRANSACCION INVALIDO EN VTL",
									ErrorMessages.FIELD_103_TYPE_ERROR, Constants.General.NUM_NINETYONE);// ERROR 91
							break;
						}
					}
//					
					else
					{
						objectValidations.modifyAttributes(false, "TERMINAL INVALIDO EN VTL",
								ErrorMessages.ERROR_TERMINAL_VTL, Constants.General.NUM_NINETYONE);// ERROR 91
					}

					break;

				}

			default:
				break;
			}

		} catch (XFieldUnableToConstruct e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void constructTagsby125field(Base24Ath msg, Super objectValidations) throws XPostilion {

		String field125 = msg.getField(125);

		String ADENDA_REF_1 = field125.substring(0, 24);
		String ADENDA_REF_2 = field125.substring(24, 48);
		String ADENDA_REF_3 = field125.substring(48, 73);
		String ID_REGISTRO = field125.substring(73, 81);
		String TIPO_DE_IDENTIFICACION_ORIGEN = field125.substring(81, 83);
		String NUMERO_IDENTIFICACION_ORIGEN = field125.substring(83, 98);
		String VALOR_PAGO = field125.substring(98, 115);
		String TIPO_IDENTIFICACION_DESTINO = field125.substring(115, 117);
		String NUMERO_IDENTIFICACION_DESTINO = field125.substring(117, 132);
		String VALIDACION_TITULARIDAD = field125.substring(132, 133);
		String TERMINAL_DESTINO = field125.substring(133, 138);

		objectValidations.putInforCollectedForStructData("ADENDA REF 1", ADENDA_REF_1);
		objectValidations.putInforCollectedForStructData("ADENDA REF 2", ADENDA_REF_2);
		objectValidations.putInforCollectedForStructData("ADENDA REF 3", ADENDA_REF_3);
		objectValidations.putInforCollectedForStructData("ID REGISTRO", ID_REGISTRO);
		objectValidations.putInforCollectedForStructData("TIPO DE IDENTIFICACION ORIGEN",
				TIPO_DE_IDENTIFICACION_ORIGEN);
		objectValidations.putInforCollectedForStructData("NUMERO IDENTIFICACION ORIGEN", NUMERO_IDENTIFICACION_ORIGEN);
		objectValidations.putInforCollectedForStructData("VALOR PAGO", VALOR_PAGO);
		objectValidations.putInforCollectedForStructData("TIPO IDENTIFICACION DESTINO", TIPO_IDENTIFICACION_DESTINO);
		objectValidations.putInforCollectedForStructData("NUMERO IDENTIFICACION DESTINO",
				NUMERO_IDENTIFICACION_DESTINO);
		objectValidations.putInforCollectedForStructData("VALIDACION TITULARIDAD", VALIDACION_TITULARIDAD);
		objectValidations.putInforCollectedForStructData("TERMINAL DESTINO", TERMINAL_DESTINO);
		
		

	}

}
