package postilion.realtime.genericinterface.extract;

import postilion.realtime.genericinterface.channels.Super;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.EventReporter;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.ProcessingCode;
import postilion.realtime.sdk.message.bitmap.XFieldUnableToConstruct;
import postilion.realtime.sdk.util.XPostilion;

public class Extract {

//**************************MODELOS TRANSFERENCIA****************************************************************
	// MIXTA
	public static void tagsModelTransferMixed(Super objectValidations, Base24Ath msg) throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_MIXTA");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "MIXTA");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "18");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "0");

		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT_CARD_NR")
				&& objectValidations.getInforCollectedForStructData().containsKey("CLIENT_ACCOUNT_NR")) {
			tagsAllDataCarsAccountsClienteCNB(objectValidations);
			tagsAllDataAccountsAccountsClienteCNBForMixed(objectValidations);
		} else {
			tagsAllDataCarsWithoutAccountsClienteCNB(objectValidations, msg);
			tagsAllDataAccountsWhitoutAccountsClienteCNBForMixed(objectValidations, msg);
		}

		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT2_ACCOUNT_NR"))
			tagsAllDataAccountsAccountsByNumberClientCNB(objectValidations);
		else
			tagsAllDataAccountsWhitoutAccountsByNumberClientCNB(objectValidations, msg);

	}

	// CREDITO
	public static void tagsModelTransferCredit(Super objectValidations, Base24Ath msg) throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_CREDITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "CREDITO");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "03");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");

		objectValidations.putInforCollectedForStructData("Ent_Adq",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4)
						: msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));

		tagsAllDataCarsWithoutCardBg(objectValidations, msg);

		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT2_ACCOUNT_NR"))
			tagsAllDataAccountsAccountsByNumberClientCNB(objectValidations);
		else
			tagsAllDataAccountsWhitoutAccountsByNumberClientCNB(objectValidations, msg);

		tagsAllDataAccountsWhitoutAccountsClienteCNBForMixed(objectValidations, msg);
	}

	// DEBITO
	public static void tagsModelTransferDebit(Super objectValidations, Base24Ath msg) throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);

		ProcessingCode procCode = getProcCode(msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_DEBITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "DEBITO");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "23");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSD");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
		objectValidations.putInforCollectedForStructData("Ent_Adq",
				msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2)
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7)
						: "0054");

		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT_CARD_NR")) {

			tagsAllDataCarsAccountsClienteCNB(objectValidations);
			tagsAllDataAccountsAccountsClienteCNB(objectValidations);
		} else {
			tagsAllDataCarsWithoutAccountsClienteCNB(objectValidations, msg);
			tagsAllDataAccountsWithoutAccountsClienteCNB(objectValidations, msg);
		}
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7).trim()
						: "00000000000000000");
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", procCode.getToAccount());

	}

//**************************MODELOS TRANSFERENCIA****************************************************************	
//**************************MODELOS PAGO DE OBLIGACIONES****************************************************************

	// MIXTA EN EFECTIVO CNB
	public static void tagsModelPaymentOfObligationsMixedWithoutCard(Super objectValidations, Base24Ath msg)
			throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "MIXTA");
		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_MIXTA");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "29");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccionx1", "29");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "PAGOLC");
		objectValidations.putInforCollectedForStructData("Nombre_TransaccionX1", "ONESID");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "0");
		objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "0");
		objectValidations.putInforCollectedForStructData("Mod_Credito", "9");
		objectValidations.putInforCollectedForStructData("Entidad", "0000");

		if (objectValidations.getInforCollectedForStructData().containsKey("CORRES_CARD_NR")
				&& objectValidations.getInforCollectedForStructData().containsKey("CORRES_ACCOUNT_NR")) {
			tagsAllDataCarsAccountsClienteCNBWithoutCard(objectValidations);
			tagsAllDataAccountsAccountsClienteCNBForMixedPObligWithoutCard(objectValidations);
		} else {
			tagsAllDataCarsWithoutAccountsClienteCNB(objectValidations, msg);// corroborar que tarjeta cb viene en el 35
			tagsAllDataAccountsWhitoutAccountsClienteCNBForMixedPObligWithoutCard(objectValidations, msg);
		}
		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT2_ACCOUNT_NR"))
			tagsAllDataAccountsAccountsByNumberClientCNBPoblig(objectValidations);
		else
			tagsAllDataAccountsWhitoutAccountsByNumberClientCNBPOblig(objectValidations, msg);

	}

	// DEBITO EN EFECTIVO CNB
	public static void tagsModelPaymentOfObligationsDebitWithoutCard(Super objectValidations, Base24Ath msg)
			throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "DEBITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_DEBITO");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "29");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "ONESID");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
		objectValidations.putInforCollectedForStructData("Entidad", "0000");

		if (msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2)) {
			switch (msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7)) {
			case "0002":
			case "0052":
			case "0023":
				objectValidations.putInforCollectedForStructData("Ent_Adq",
						msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7));

				break;

			default:
				objectValidations.putInforCollectedForStructData("Ent_Adq", "0054");
				break;
			}
		}

		if (objectValidations.getInforCollectedForStructData().containsKey("CORRES_CARD_NR")
				&& objectValidations.getInforCollectedForStructData().containsKey("CORRES_ACCOUNT_NR")) {

			tagsAllDataCarsAccountsClienteCNBWithoutCard(objectValidations);
			tagsAllDataAccountsAccountsClienteCNBWithoutCard(objectValidations);

		} else {
			tagsAllDataCarsWithoutAccountsClienteCNB(objectValidations, msg);
			tagsAllDataAccountsWithoutAccountsClienteCNBWithoutCard(objectValidations, msg);
		}

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7)
						: "00000000000000000");
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "LCR");

	}

	// CREDITO EN EFECTIVO CNB EL MISMO DE ATM.

	// MIXTA
	public static void tagsModelPaymentOfObligationsMixed(Super objectValidations, Base24Ath msg) throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "MIXTA");
		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_MIXTA");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "29");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccionx1", "29");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "PAGOLC");
		objectValidations.putInforCollectedForStructData("Nombre_TransaccionX1", "ONESID");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "0");
		objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "0");
		objectValidations.putInforCollectedForStructData("Mod_Credito", "9");
		objectValidations.putInforCollectedForStructData("Entidad", "0000");

		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT_CARD_NR")
				&& objectValidations.getInforCollectedForStructData().containsKey("CLIENT_ACCOUNT_NR")) {
			tagsAllDataCarsAccountsClienteCNB(objectValidations);
			tagsAllDataAccountsAccountsClienteCNBForMixedPOblig(objectValidations);
		} else {
			tagsAllDataCarsWithoutAccountsClienteCNB(objectValidations, msg);
			tagsAllDataAccountsWhitoutAccountsClienteCNBForMixedPOblig(objectValidations, msg);
		}

		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT2_ACCOUNT_NR"))
			tagsAllDataAccountsAccountsByNumberClientCNBPoblig(objectValidations);
		else
			tagsAllDataAccountsWhitoutAccountsByNumberClientCNBPOblig(objectValidations, msg);

	}

	// CREDITO
	public static void tagsModelPaymentOfObligationsCredit(Super objectValidations, Base24Ath msg) throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_CREDITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "CREDITO");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "03");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "PAGOLC");

		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
		objectValidations.putInforCollectedForStructData("Ent_Adq",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4)
						: msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
		objectValidations.putInforCollectedForStructData("Codigo_de_Red",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4)
						: msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));

		// tarjetas y cuentas por cuadrar segun modelo en el pcode no aparece el segundo
		// tipo de cuenta revisar
		tagsAllDataCarsWithoutCardBg(objectValidations, msg);
		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT2_ACCOUNT_NR"))
			tagsAllDataAccountsAccountsByNumberClientCNBPoblig(objectValidations);
		else
			tagsAllDataAccountsWhitoutAccountsByNumberClientCNBPOblig(objectValidations, msg);

		tagsAllDataAccountsWhitoutAccountsClienteCNBForMixed(objectValidations, msg);
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "06");

	}

	// DEBITO
	public static void tagsModelPaymentOfObligationsDebit(Super objectValidations, Base24Ath msg) throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "DEBITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_DEBITO");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "29");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "ONESID");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
		objectValidations.putInforCollectedForStructData("Entidad", "0000");

		if (msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2)) {
			switch (msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7)) {
			case "0002":
			case "0052":
			case "0023":
				objectValidations.putInforCollectedForStructData("Ent_Adq",
						msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7));

				break;

			default:
				objectValidations.putInforCollectedForStructData("Ent_Adq", "0054");
				break;
			}
		}

		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT_CARD_NR")) {
			tagsAllDataCarsAccountsClienteCNB(objectValidations);
			tagsAllDataAccountsAccountsClienteCNB(objectValidations);
		} else {
			tagsAllDataCarsWithoutAccountsClienteCNB(objectValidations, msg);
			tagsAllDataAccountsWithoutAccountsClienteCNB(objectValidations, msg);
		}

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7)
						: "00000000000000000");
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "LCR");

	}

//**************************MODELOS PAGO DE OBLIGACIONES****************************************************************	
//**************************MODELOS RETIRO OTP CNB****************************************************************	

	// MIXTA
	public static void tagsModelWithdrawalOtpMixed(Super objectValidations, Base24Ath msg) throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);
		ProcessingCode procCode = getProcCode(msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_MIXTA");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "MIXTA");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "18");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "0");
		objectValidations.putInforCollectedForStructData("Transacc_Ind", "Z");
		objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C006");

		if (objectValidations.getInforCollectedForStructData().containsKey("CORRES_CARD_NR")
				&& objectValidations.getInforCollectedForStructData().containsKey("CORRES_ACCOUNT_NR")) {

			tagsAllDataCarsAccountsClienteCNBWithoutCard(objectValidations);
			tagsAllDataAccountsAccountsClienteCNBWithoutCard(objectValidations);
		} else {

			tagsAllDataCarsWithoutAccountsClienteCNB(objectValidations, msg);
			tagsAllDataAccountsWithoutAccountsClienteCNBWithoutCard(objectValidations, msg);
		}

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4)
						: "00000000000000000"));
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE",
				(procCode.getFromAccount().toString().equals("14") ? "10" : "20"));
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_ProductoX1",
				(procCode.getFromAccount().toString().equals("14") ? "05" : "04"));
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_DebitadaX1",
				(procCode.getFromAccount().toString().equals("14") ? "AHO" : "CTE"));

	}

//// DEBITO

	public static void tagsModelTransferOtpDebit(Super objectValidations, Base24Ath msg) throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);
		ProcessingCode procCode = getProcCode(msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_DEBITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "DEBITO");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "23");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSD");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
		objectValidations.putInforCollectedForStructData("Transacc_Ind", "Y");
		objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C005");
		objectValidations.putInforCollectedForStructData("Ent_Adq",
				msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2)
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7)
						: "0054");

		tagsAllDataCarsWithoutAccountsClienteCNB(objectValidations, msg);
		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT2_CARD_NR")) {

//			tagsAllDataCarsAccountsClienteCNBWithdrawalOtp(objectValidations);
			tagsAllDataAccountsAccountsClienteCNBOtp(objectValidations);

		} else {
			tagsAllDataAccountsWithoutAccountsClienteCNBOtp(objectValidations, msg);
		}
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(7).trim()
						: "00000000000000000");
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", procCode.getToAccount());

	}

// CREDITO
	public static void tagsModelWithdrawalOtpCredit(Super objectValidations, Base24Ath msg) throws XPostilion {

		tagsModelTransferView2(objectValidations, msg);

		objectValidations.putInforCollectedForStructData("TRANSACTION_CNB_TYPE",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_CNB_TYPE") + "_CREDITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "CREDITO");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion", "03");
		objectValidations.putInforCollectedForStructData("Nombre_Transaccion", "TRANSC");
		objectValidations.putInforCollectedForStructData("Indicador_AVAL", "1");
		objectValidations.putInforCollectedForStructData("Transacc_Ind", "X");
		objectValidations.putInforCollectedForStructData("Transaccion_Unica", "C004");

		objectValidations.putInforCollectedForStructData("Ent_Adq",
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7)
						: msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));

		if (objectValidations.getInforCollectedForStructData().containsKey("CORRES_CARD_NR")
				&& objectValidations.getInforCollectedForStructData().containsKey("CORRES_ACCOUNT_NR")) {

			tagsAllDataCarsAccountsClienteCNBWithoutCard(objectValidations);
			tagsAllDataAccountsAccountsClienteCNBWithoutCardOtpCnb(objectValidations);

		} else {
			tagsAllDataCarsWithoutAccountsClienteCNB(objectValidations, msg);
			tagsAllDataAccountsWithoutAccountsClienteCNBWithoutCardOtpCnb(objectValidations, msg);
		}

	}

//**************************MODELOS RETIRO OTP CNB*******************************************************************	
//**************************MODELOS PAGO DE SERVICIOS****************************************************************	
	// MIXTA
	public static void tagsModelPaymentOfServicesMixed(Super objectValidations, Base24Ath msg, Client udpClient,
			String nameInterface) throws XPostilion {

		tagsModelPspGeneral(objectValidations, msg, udpClient, nameInterface);

		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_PSP") + "_MIXTA");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "MIXTA");
		objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "0");
		objectValidations.putInforCollectedForStructData("Identificacion_Canal", "CB");// revizar cananl para agrupacion
																						// totalizados

		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT_ACCOUNT_TYPE"))
			tagsAllDataAccountsAccountsClienteCNB(objectValidations);
		else
			tagsAllDataAccountsWithoutAccountsClienteCNB(objectValidations, msg);

		// faltan tarjeta id y demas con tags de javier

	}

	// CREDITO
	public static void tagsModelPaymentOfServicesCredit(Super objectValidations, Base24Ath msg, Client udpClient,
			String nameInterface) throws XPostilion {

		tagsModelPspGeneral(objectValidations, msg, udpClient, nameInterface);

		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP", "CREDITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN", "CREDITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "CREDITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_PSP") + "_CREDITO");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "05");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "AHO");
		objectValidations.putInforCollectedForStructData("DEBIT_CARD_NR", "0066010000000000");
		objectValidations.putInforCollectedForStructData("Tarjeta_Amparada", msg.getTrack2Data().getPan());
		objectValidations.putInforCollectedForStructData("DEBIT_CARD_CLASS", "00");
		objectValidations.putInforCollectedForStructData("Vencimiento", "0000");
		objectValidations.putInforCollectedForStructData("Ind_4xmil", "0");
		objectValidations.putInforCollectedForStructData("DEBIT_CUSTOMER_ID", "0000000000000");
		objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "2");
		objectValidations.putInforCollectedForStructData("Identificacion_Canal", "C0");
	}

	// DEBITO
	public static void tagsModelPaymentOfServicesDebit(Super objectValidations, Base24Ath msg, Client udpClient,
			String nameInterface) throws XPostilion {

		tagsModelPspGeneral(objectValidations, msg, udpClient, nameInterface);

		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_PSP",
				objectValidations.getInforCollectedForStructData().get("TRANSACTION_TYPE_PSP") + "_DEBITO");
		objectValidations.putInforCollectedForStructData("TRANSACTION_TYPE_CBN_PSP_S", "DEBITO");
		objectValidations.putInforCollectedForStructData("Indicador_Tipo_Servicio", "1");
		objectValidations.putInforCollectedForStructData("PRIM_COV_ABO", "2");
		objectValidations.putInforCollectedForStructData("Identificacion_Canal", "00");

		if (objectValidations.getInforCollectedForStructData().containsKey("CLIENT_ACCOUNT_TYPE"))
			tagsAllDataAccountsAccountsClienteCNB(objectValidations);
		else
			tagsAllDataAccountsWithoutAccountsClienteCNB(objectValidations, msg);

		// faltan tarjeta id y demas con tags de javier

	}

	public static void tagsModelPspGeneral(Super objectValidations, Base24Ath msg, Client udpClient,
			String nameInterface) throws XPostilion {
		try {
			if (msg.isFieldSet(Base24Ath.Bit.DATA_ADDTIONAL)) {
				objectValidations.putInforCollectedForStructData("Numero_de_Recibo_de_Terminal",
						msg.getField(Base24Ath.Bit.DATA_ADDTIONAL).substring(24, 30));
			} else {
				objectValidations.putInforCollectedForStructData("Numero_de_Recibo_de_Terminal",
						Constants.General.SIX_ZEROS);
			}
			if (msg.isFieldSet(Iso8583.Bit._104_TRAN_DESCRIPTION)) {
				objectValidations.putInforCollectedForStructData("ADDITIONAL_SERVICE_CODE",
						msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION)
								.substring(msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION).length() - 2));
			} else {
				objectValidations.putInforCollectedForStructData("ADDITIONAL_SERVICE_CODE",
						Constants.General.SIXTEEN_ZEROS);
			}

			objectValidations.putInforCollectedForStructData("Entidad_Origen",
					(msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID))
							? msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4)
							: "0001");

			if (msg.isFieldSet(Iso8583.Bit._054_ADDITIONAL_AMOUNTS)) {

				objectValidations.putInforCollectedForStructData("Valor_total",
						msg.getField(Iso8583.Bit._054_ADDITIONAL_AMOUNTS).substring(0, 12));
				objectValidations.putInforCollectedForStructData("Valor_total_subconvenio",
						msg.getField(Iso8583.Bit._054_ADDITIONAL_AMOUNTS).substring(12, 24));
				objectValidations.putInforCollectedForStructData("subconv_ind",
						((msg.getField(Iso8583.Bit._054_ADDITIONAL_AMOUNTS).substring(12, 24)).equals("000000000000"))
								? "0"
								: "1");
			}

			if (msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2)) {

				// reisarrrr *********************
				objectValidations.putInforCollectedForStructData("FI_Credito",
						msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(1, 5));
				objectValidations.putInforCollectedForStructData("FI__Debito",
						msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(5, 9));
				// reisarrrr *********************
				objectValidations.putInforCollectedForStructData("PRIM_COV_NR",
						msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(10));
				objectValidations.putInforCollectedForStructData("NURA_CODE",
						msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(10));
			}

			if (msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA)) {
				Extract.putTagIncocredito(objectValidations, msg);
				objectValidations.putInforCollectedForStructData("Terminal_Ampliada",
						msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(4, 12));
				objectValidations.putInforCollectedForStructData("Identificador_Terminal",
						msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(12, 13));
			}
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(nameInterface, Extract.class.getName(), e,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "tagsModelPspGeneral", udpClient);
		}
	}

	private static void tagsModelTransferView2(Super objectValidations, Base24Ath msg) throws XPostilion {

		objectValidations.putInforCollectedForStructData("FI_DEBITO",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4)
						: "0000");

		if (msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2)) {

			objectValidations.putInforCollectedForStructData("FI_CREDITO",
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(3, 7));

			objectValidations.putInforCollectedForStructData("Clase_Pago",
					msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(0, 1));
		}

		if (msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)) {
			objectValidations.putInforCollectedForStructData("Ent_Adq",
					msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
			objectValidations.putInforCollectedForStructData("Entidad",
					msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(0, 4));
		}

		if (msg.isFieldSet(Iso8583.Bit._048_ADDITIONAL_DATA)) {

			Extract.putTagIncocredito(objectValidations, msg);
			objectValidations.putInforCollectedForStructData("Terminal_Ampliada",
					msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(4, 12));
			objectValidations.putInforCollectedForStructData("Identificador_Terminal",
					msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(12, 13));
		}
	}

	public static ProcessingCode getProcCode(Base24Ath msg) throws XFieldUnableToConstruct, XPostilion {

		if (msg.getField(Iso8583.Bit._003_PROCESSING_CODE).equals("890000"))
			return new ProcessingCode(msg.getField(126).substring(22, 28));
		else
			return msg.getProcessingCode();

	}

	private static void tagsAllDataAccountsWhitoutAccountsClienteCNBForMixed(Super objectValidations, Base24Ath msg)
			throws XPostilion {

		ProcessingCode procCode = getProcCode(msg);

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4)
						: "00000000000000000"));
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", procCode.getFromAccount());

		// solo mixta

		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_ProductoX1",
				(procCode.getFromAccount().equals("10")) ? "05" : "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_DebitadaX1",
				(procCode.getFromAccount().equals("10")) ? "AHO" : "CTE");

	}

	private static void tagsAllDataCarsAccountsClienteCNBWithoutCard(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("FI_Tarjeta",
				objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR").substring(0, 6));
		objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1",
				objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
		objectValidations.putInforCollectedForStructData("CARD_CLASS",
				objectValidations.getInforCollectedForStructData().get("CORRES_CARD_CLASS"));
		objectValidations.putInforCollectedForStructData("ID_CLIENT",
				objectValidations.getInforCollectedForStructData().get("CORRES_CUSTOMER_ID"));
		objectValidations.putInforCollectedForStructData("Tarjeta_Amparada",
				objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));
		objectValidations.putInforCollectedForStructData("PAN_Tarjeta",
				objectValidations.getInforCollectedForStructData().get("CORRES_CARD_NR"));

	}

	private static void tagsAllDataAccountsAccountsClienteCNBForMixed(Super objectValidations) {
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

	private static void tagsAllDataAccountsAccountsClienteCNBWithoutCard(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");

		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "10"
						: "20");
	}

	private static void tagsAllDataAccountsAccountsClienteCNBWithoutCardOtpCnb(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "10"
						: "20");
	}

	private static void tagsAllDataAccountsWhitoutAccountsClienteCNBForMixedPObligWithoutCard(Super objectValidations,
			Base24Ath msg) throws XPostilion {

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4)
						: "00000000000000000"));
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "OTR");

		// solo mixta
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "04");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_ProductoX1", "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_DebitadaX1", "CTE");
	}

	private static void tagsAllDataAccountsWhitoutAccountsByNumberClientCNB(Super objectValidations, Base24Ath msg)
			throws XPostilion {

		ProcessingCode procCode = getProcCode(msg);

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8).trim()
						: "00000000000000000");
		objectValidations.putInforCollectedForStructData("CLIENT_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8).trim()
						: "00000000000000000");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(procCode.getToAccount().equals("10")) ? "05" : "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(procCode.getToAccount().equals("10")) ? "AHO" : "CTE");

		// solo mixtas
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8).trim()
						: "00000000000000000");
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE", procCode.getToAccount());
	}

	private static void tagsAllDataAccountsAccountsByNumberClientCNB(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_NR"));

		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");

		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");

		// ---- solo mixta
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE",
				objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE"));
	}

	private static void tagsAllDataAccountsWithoutAccountsClienteCNBWithoutCard(Super objectValidations, Base24Ath msg)
			throws XPostilion {
		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4)
						: "00000000000000000"));

		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "CTE");
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4)
						: "00000000000000000"));
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE", "20");

	}

	private static void tagsAllDataAccountsWithoutAccountsClienteCNBWithoutCardOtpCnb(Super objectValidations,
			Base24Ath msg) throws XPostilion {
		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4)
						: "00000000000000000"));

		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto", "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "CTE");

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4)
						: "00000000000000000"));

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "20");

	}

	private static void tagsAllDataCarsWithoutCardBg(Super objectValidations, Base24Ath msg) throws XPostilion {

		objectValidations.putInforCollectedForStructData("CARD_CLASS", "00");
		objectValidations.putInforCollectedForStructData("CLIENT_CARD_CLASS", "00");

		objectValidations.putInforCollectedForStructData("Entidad_Origen", "00");
		putTagPAN(objectValidations, msg);
		objectValidations.putInforCollectedForStructData("Vencimiento", "0000");
		putTagClientCardNr(objectValidations, msg);
		objectValidations.putInforCollectedForStructData("Entidad", "0000");
	}

	private static void tagsAllDataAccountsWithoutAccountsClienteCNB(Super objectValidations, Base24Ath msg)
			throws XPostilion {
		ProcessingCode procCode = getProcCode(msg);

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4)
						: "00000000000000000"));

		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(procCode.getFromAccount().equals("10")) ? "05" : "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(procCode.getFromAccount().equals("10")) ? "AHO" : "CTE");
	}

	private static void tagsAllDataAccountsWithoutAccountsClienteCNBOtp(Super objectValidations, Base24Ath msg)
			throws XPostilion {
		ProcessingCode procCode = getProcCode(msg);

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4)
						: "00000000000000000"));

		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(procCode.getFromAccount().equals("14")) ? "05" : "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(procCode.getFromAccount().equals("14")) ? "AHO" : "CTE");
	}

	private static void tagsAllDataAccountsAccountsClienteCNBForMixedPObligWithoutCard(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "OTR");

		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_ProductoX1",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");

		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_DebitadaX1",
				(objectValidations.getInforCollectedForStructData().get("CORRES_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");
	}

	private static void tagsAllDataAccountsWhitoutAccountsByNumberClientCNBPOblig(Super objectValidations,
			Base24Ath msg) throws XPostilion {

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8)
						: "00000000000000000");
//		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
//				(msg.getProcessingCode().getToAccount().equals("10")) ? "05" : "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "OTR");

		// solo mixtas
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
						? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(8)
						: "00000000000000000");
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE", "LCR");

	}

	private static void tagsAllDataAccountsAccountsByNumberClientCNBPoblig(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_NR"));

//				objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
//						(objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE").equals("10")) ? "05"
//								: "04");

		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada", "OTR");

		// ---- solo mixta
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("MIX_ACCOUNT_TYPE", "LCR");

	}

	private static void tagsAllDataAccountsWhitoutAccountsClienteCNBForMixedPOblig(Super objectValidations,
			Base24Ath msg) throws XPostilion {

		ProcessingCode procCode = getProcCode(msg);

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				(msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
						? msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(4)
						: "00000000000000000"));
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "OTR");

		// solo mixta

		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(procCode.getFromAccount().equals("10")) ? "05" : "04");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_ProductoX1",
				(procCode.getFromAccount().equals("10")) ? "05" : "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_DebitadaX1",
				(procCode.getFromAccount().equals("10")) ? "AHO" : "CTE");

	}

	private static void tagsAllDataAccountsAccountsClienteCNBForMixedPOblig(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("SEC_ACCOUNT_TYPE", "OTR");

		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_ProductoX1",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");

		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_DebitadaX1",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");
	}

	private static void tagsAllDataAccountsAccountsClienteCNB(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(objectValidations.getInforCollectedForStructData().get("CLIENT_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");
	}

	private static void tagsAllDataAccountsAccountsClienteCNBOtp(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("PRIM_ACCOUNT_NR",
				objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_NR"));
		objectValidations.putInforCollectedForStructData("Codigo_Transaccion_Producto",
				(objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE").equals("10")) ? "05"
						: "04");
		objectValidations.putInforCollectedForStructData("Tipo_de_Cuenta_Debitada",
				(objectValidations.getInforCollectedForStructData().get("CLIENT2_ACCOUNT_TYPE").equals("10")) ? "AHO"
						: "CTE");
	}

	private static void tagsAllDataCarsWithoutAccountsClienteCNB(Super objectValidations, Base24Ath msg)
			throws XFieldUnableToConstruct {

		objectValidations.putInforCollectedForStructData("FI_Tarjeta", msg.getTrack2Data().getPan());
		objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1", msg.getTrack2Data().getPan());
		objectValidations.putInforCollectedForStructData("Tarjeta_Amparada", msg.getTrack2Data().getPan());
		objectValidations.putInforCollectedForStructData("PAN_Tarjeta", msg.getTrack2Data().getPan());
		objectValidations.putInforCollectedForStructData("CARD_CLASS", "00");
	}

	private static void tagsAllDataCarsAccountsClienteCNB(Super objectValidations) {

		objectValidations.putInforCollectedForStructData("FI_Tarjeta",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR").substring(0, 6));
		objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
		objectValidations.putInforCollectedForStructData("CARD_CLASS",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_CLASS"));
		objectValidations.putInforCollectedForStructData("ID_CLIENT",
				objectValidations.getInforCollectedForStructData().get("CUSTOMER_ID"));
		objectValidations.putInforCollectedForStructData("Tarjeta_Amparada",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
		objectValidations.putInforCollectedForStructData("PAN_Tarjeta",
				objectValidations.getInforCollectedForStructData().get("CLIENT_CARD_NR"));
	}

	/**
	 * Coloca el tag INCOCREDITO
	 * 
	 * @param objectValidations
	 * @param msg
	 * @throws XPostilion
	 */
	public static void putTagIncocredito(Super objectValidations, Base24Ath msg) throws XPostilion {
		if (msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).length() < 36)
			objectValidations.putInforCollectedForStructData("INCOCREDITO", "00000000");
		else
			objectValidations.putInforCollectedForStructData("INCOCREDITO",
					msg.getField(Iso8583.Bit._048_ADDITIONAL_DATA).substring(36));
	}

	/**
	 * Coloca el tag PAN_Tarjeta
	 * 
	 * @param objectValidations
	 * @param msg
	 * @throws XPostilion
	 */
	public static void putTagPAN(Super objectValidations, Base24Ath msg) throws XPostilion {
		if (msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
			objectValidations.putInforCollectedForStructData("PAN_Tarjeta",
					"00" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).substring(0, 4) + "0000000000000");
		else
			objectValidations.putInforCollectedForStructData("PAN_Tarjeta", "0000000000000000000");

	}

	/**
	 * Coloca el tag CLIENT_CARD_NR_1
	 * 
	 * @param objectValidations
	 * @param msg
	 * @throws XPostilion
	 */
	public static void putTagClientCardNr(Super objectValidations, Base24Ath msg) throws XPostilion {
		if (msg.isFieldSet(Iso8583.Bit._035_TRACK_2_DATA))
			objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1", msg.getTrack2Data().getPan());
		else
			objectValidations.putInforCollectedForStructData("CLIENT_CARD_NR_1", "0000000000000000000");

	}

	public static String tagTTypePObligInternet(Base24Ath msg, Super objectValidations) throws XFieldUnableToConstruct {

		switch (msg.getProcessingCode().toString()) {

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "8");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "8");
			objectValidations.putInforCollectedForStructData("Mod_Credito_REV", "3");
			return "CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "9");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "9");
			objectValidations.putInforCollectedForStructData("Mod_Credito_REV", "3");
			return "OTROS_CREDITOS";

		default:
			return "OTROS";
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
			objectValidations.putInforCollectedForStructData("Mod_Credito", "8");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "8");
			objectValidations.putInforCollectedForStructData("Mod_Credito_REV", "2");

			return "ROTATIVO_EFECTIVO";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_CHEQUE:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "2");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "2");

			return "ROTATIVO_CHEQUE";

		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_EFECTIVO:
			objectValidations.putInforCollectedForStructData("Mod_Credito", "9");
			objectValidations.putInforCollectedForStructData("Mod_CreditoX1", "9");
			objectValidations.putInforCollectedForStructData("Mod_Credito_REV", "3");
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
}
