package postilion.realtime.genericinterface;

import java.util.Arrays;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.EventReporter;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.XFieldUnableToConstruct;
import postilion.realtime.sdk.util.XPostilion;

public class ValidateAutra {

	/************************************************************************************
	 *
	 * @param Base24Ath
	 * @return int 1, routing to AUTRA, 0 routing to Capa Integracion
	 * @throws Exception
	 ************************************************************************************/
	public static int getRouting(Base24Ath msg, Client udpClient, String nameInterface, String filtro)
			throws XFieldUnableToConstruct {
		// deuda tecnica
		String retRefNumber = "N/D";
		String channel = "N/D";
		String pan = msg.getTrack2Data().getPan();
		String procCode = msg.getProcessingCode().toString();
		String bin = pan.substring(0, 6);
		int routingTo = 0;
		StringBuilder keyCuenta = new StringBuilder();
		try {
			channel = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13).equals(" ") ? "E"
					: msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13);
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			String[] terminalsID = { "8354", "8110", "9631", "9632" };
			String terminalId = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(4, 8);

			if (Arrays.stream(terminalsID).anyMatch(terminalId::equals)) {
				routingTo = Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION;
			} else {
				if (procCode.equals("890000"))
					procCode = msg.getField(126).substring(22, 28);
				String keyTarjeta = channel + procCode + pan;
				String keyBin = channel + procCode + bin;
				keyCuenta.append(channel);
				keyCuenta.append(procCode);
				keyCuenta.append(construyeCtasValidacionAutra(procCode, msg));

				switch (filtro) {
				case "Test1":
				case "Prod1":
					if (GenericInterface.primerFiltroTest1.containsKey(keyBin)
							|| GenericInterface.primerFiltroTest1.containsKey(keyTarjeta)
							|| GenericInterface.primerFiltroTest1.containsKey(keyCuenta.toString()))
						routingTo = Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION;
					else
						routingTo = Constants.TransactionRouting.INT_AUTRA;

					break;
				}

				udpClient.sendData(Client.getMsgKeyValue(retRefNumber,
						routingTo == Constants.TransactionRouting.INT_AUTRA ? "Enrutamiento Autra Key: "
								: "Enrutamiento CI Key: " + "PAN:" + pan + "ProcCode:" + procCode + "Chanel:" + channel,
						"LOG", nameInterface));
			}

		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(nameInterface, ValidateAutra.class.getName(), e, retRefNumber,
					"getRouting", udpClient, routingTo == Constants.TransactionRouting.INT_AUTRA ? "Enrutamiento Autra Key: "
							: "Enrutamiento CI Key: " + "PAN:" + pan + "ProcCode:" + procCode + "Chanel:" + channel);
		}

		return routingTo;
	}

	public static int validateAutra(Base24Ath msg, Client udpClient, String nameInterface, String filtro)
			throws XFieldUnableToConstruct {
		String pan = msg.getTrack2Data().getPan();
		String procCode = msg.getProcessingCode().toString();
		String bin = pan.substring(0, 6);
		String retRefNumber = "N/D";
		String channel = "N/D";
		int routingTo = 0;
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			channel = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13);
			if (channel.equals("7") || channel.equals("1")) {
				if ((GenericInterface.migratedCards.containsKey(pan) || GenericInterface.migratedBins.containsKey(bin))
						&& GenericInterface.migratedOpCodes.containsKey(procCode)) {

					String opCode = GenericInterface.migratedOpCodes.get(procCode);
					switch (opCode) {

					case "890000":// Consultas de costo, se valida si el codigo de procesamiento de la consulta se
									// encuentra en la tabla de codigos migrados

						String opCode126 = msg.getField(126).substring(22, 28);
						if (GenericInterface.migratedOpCodes.containsKey(opCode126)) {
							routingTo = Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION;
						} else {
							routingTo = Constants.TransactionRouting.INT_AUTRA;
						}
						break;

					default:
						routingTo = Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION;
						break;
					}
				} else {
					routingTo = Constants.TransactionRouting.INT_AUTRA;
				}
			} else {
				routingTo = Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION;
			}

			udpClient.sendData(Client.getMsgKeyValue("N/A",
					routingTo == Constants.TransactionRouting.INT_AUTRA ? "Enrutamiento Autra Key: "
							: "Enrutamiento CI Key: " + "PAN:" + pan + "ProcCode:" + procCode + "Chanel:"
									+ msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13),
					"LOG", nameInterface));

		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(nameInterface, ValidateAutra.class.getName(), e, retRefNumber,
					"validateAutra", udpClient, routingTo == Constants.TransactionRouting.INT_AUTRA ? "Enrutamiento Autra Key: "
							: "Enrutamiento CI Key: " + "PAN:" + pan + "ProcCode:" + procCode + "Chanel:" + channel);
		}

		return routingTo;
	}

	/************************************************************************************
	 *
	 * @param Base24Ath
	 * @return int 1, routing to AUTRA, 0 routing to Capa Integracion
	 * @throws Exception
	 ************************************************************************************/
	public static int validateAutra2(Base24Ath msg, Client udpClient, String nameInterface, String filtro)
			throws XFieldUnableToConstruct {

		int routingTo = 0;
		String key1 = null;
		String key2 = null;
		String retRefNumber = "N/D";
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			String pan = msg.getTrack2Data().getPan();
			String procCode = msg.getProcessingCode().toString();
			String bin = pan.substring(0, 6);
			String entryMode = msg.isFieldSet(Iso8583.Bit._022_POS_ENTRY_MODE)
					? msg.getField(Iso8583.Bit._022_POS_ENTRY_MODE)
					: "";

			String channel = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13);

			if (procCode.equals("890000")) {
				procCode = msg.getField(126).substring(22, 28);
			}

			key1 = channel + procCode + entryMode;

			if (GenericInterface.primerFiltroTest2.containsKey(key1)) {

				StringBuilder sb = new StringBuilder();
				sb.append(channel);
				sb.append(procCode);

				switch (channel) {
				case "1":
					sb.append(bin);
					sb.append(construyeCtasValidacionAutra(procCode, msg));
					break;
				case "7":
					sb.append(construyeCtasValidacionAutra(procCode, msg));
					break;
				default:

					break;
				}

				key2 = sb.toString();

				if (GenericInterface.segundoFiltroTest2.containsKey(key2)) {
					routingTo = Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION;
				} else {
					routingTo = Constants.TransactionRouting.INT_AUTRA;
				}

			} else {
				routingTo = Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION;
			}

			udpClient.sendData(Client.getMsgKeyValue("N/A",
					routingTo == Constants.TransactionRouting.INT_AUTRA ? "Enrutamiento Autra Key: "
							: "Enrutamiento CI Key: " + "PAN:" + pan + "ProcCode:" + procCode + "Chanel:"
									+ msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13),
					"LOG", nameInterface));
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(nameInterface, ValidateAutra.class.getName(), e, retRefNumber,
					"validateAutra2", udpClient);
		}

		return routingTo;
	}

	public static String construyeCtasValidacionAutra(String pCode, Base24Ath msg) throws XPostilion {
		String subKey = null;
		switch (pCode) {
		case Constants.Channels.PCODE_PAGO_CREDITO_HIPOTECARIO_EFECTIVO_ATM_MULTIFUNCIONAL:
		case Constants.Channels.PCODE_PAGO_CREDITO_ROTATIVO_EFECTIVO_ATM_MULTIFUNCIONAL:
		case Constants.Channels.PCODE_PAGO_OTROS_CREDITOS_EFECTIVO_ATM_MULTIFUNCIONAL:
		case Constants.Channels.PCODE_PAGO_CREDITO_HIPOTECARIO_ATM_AHORROS:
		case Constants.Channels.PCODE_PAGO_CREDITO_HIPOTECARIO_ATM_CORRIENTE:
		case Constants.Channels.PCODE_PAGO_DE_SERVICIOS_ATM_AHO:
		case Constants.Channels.PCODE_PAGO_DE_SERVICIOS_ATM_COR:
		case Constants.Channels.PCODE_PAGO_DE_TARJETA_CREDITO_ATM_AHO:
		case Constants.Channels.PCODE_PAGO_DE_TARJETA_CREDITO_ATM_COR:
		case Constants.Channels.PCODE_UTILIZACION_CREDITO_ROTATIVO_AHO:
		case Constants.Channels.PCODE_UTILIZACION_CREDITO_ROTATIVO_COR:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_AHORROS:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_VEHICULOS_CORRIENTE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_TC_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_ROTATIVO_CHEQUE:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_EFECTIVO:
		case Constants.Channels.PCODE_PAGO_OBLIGACIONES_OTROS_CHEQUE:

			subKey = msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2)
					? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).substring(10).trim()
					: Constants.General.SIXTEEN_ZEROS;

			break;
		default:

			subKey = msg.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)
					? "0" + msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1)
							.substring(msg.getField(Iso8583.Bit._102_ACCOUNT_ID_1).length() - 17)
					: Constants.General.SIXTEEN_ZEROS;

			break;

		}

		return subKey;
	}

}
