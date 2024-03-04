package postilion.realtime.genericinterface;

import java.util.Arrays;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.BussinesRules;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.EventReporter;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.XFieldUnableToConstruct;
import postilion.realtime.sdk.util.XPostilion;

public class ValidateAutra {

	public String ruta;
	public int rute;
	public String p125Accion;
	public String p125Valor;
	public String p100Valor;

	public ValidateAutra() {
		this.ruta = "";
		this.rute = 0;
		this.p125Accion = "";
		this.p125Valor = "";
		this.p100Valor = null;
	}

	/**
	 * @return the ruta
	 */
	public String getRuta() {
		return ruta;
	}

	/**
	 * @param ruta the ruta to set
	 */
	public void setRuta(String ruta) {
		this.ruta = ruta;
	}

	/**
	 * @return the rute
	 */
	public int getRute() {
		return rute;
	}

	/**
	 * @param rute the rute to set
	 */
	public void setRute(int rute) {
		this.rute = rute;
	}

	/**
	 * @return the p125Accion
	 */
	public String getP125Accion() {
		return p125Accion;
	}

	/**
	 * @param p125Accion the p125Accion to set
	 */
	public void setP125Accion(String p125Accion) {
		this.p125Accion = p125Accion;
	}

	/**
	 * @return the p125Valor
	 */
	public String getP125Valor() {
		return p125Valor;
	}

	/**
	 * @param p125Valor the p125Valor to set
	 */
	public void setP125Valor(String p125Valor) {
		this.p125Valor = p125Valor;
	}

	/**
	 * @return the p100Valor
	 */
	public String getP100Valor() {
		return p100Valor;
	}

	/**
	 * @param p100Valor the p100Valor to set
	 */
	public void setP100Valor(String p100Valor) {
		this.p100Valor = p100Valor;
	}

	/************************************************************************************
	 *
	 * @param Base24Ath
	 * @return int 1, routing to AUTRA, 0 routing to Capa Integracion
	 * @throws XPostilion if field 37 is not present
	 * @throws Exception
	 ************************************************************************************/
	public static ValidateAutra getRoutingData(Base24Ath msg, Client udpClient, String nameInterface, String filtro,
			boolean applyV2Filter) throws XPostilion {
		String pan = msg.getTrack2Data().getPan();
		String procCode = msg.getProcessingCode().toString();
		String bin = pan.substring(0, 6);
		StringBuilder keyCuenta = new StringBuilder();
		ValidateAutra validateAutra = new ValidateAutra();
		String cuenta = construyeCtasValidacionAutra(procCode, msg);
		String channel = null;
		String keyTerminal = null;
		String keyCuentaFv2 = null;
		String keyBin = null;
		String keyTarjeta = null;
		String keyAll = null;
		String value[] = null;
		String excepcion = null;
		try {

			// **********************************************************************************
			// Verifica transferencias masivas, enruta Capa
			String[] terminalsID = { "8354", "8110", "9631", "9632" };
			String terminalId = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(4, 8);

			if (Arrays.stream(terminalsID).anyMatch(terminalId::equals)) {
				validateAutra.setRute(Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION);
				return validateAutra;
			}
			// **********************************************************************************

			// **********************************************************************************
			// VERIFICA TERMINALES CEL2CEL
			String[] terminalsIDCel2Cel = { "8590", "8591", "8593", "8594" };
			String terminalIdCel2Cel = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(4, 8);

			// **********************************************************************************
			// VERIFICAR EXCEPCIONES TRANSFERENCIAS
			if ((procCode.equals("401010") || procCode.equals("401020") || procCode.equals("402010")
					|| procCode.equals("402020")) && msg.isFieldSet(125)
					&& (msg.getField(125).length() > 90 && msg.getField(125).length() <= 150)
					&& (msg.getField(125).substring(138, 139).equals("1")
							|| msg.getField(125).substring(138, 139).equals("2"))) {
				// ES QR
				// excepcion = "QR";
				validateAutra.setRute(Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION);
				return validateAutra;

			} else if ((procCode.equals("401010") || procCode.equals("401020") || procCode.equals("402010")
					|| procCode.equals("402020"))
					&& (Arrays.stream(terminalsIDCel2Cel).anyMatch(terminalIdCel2Cel::equals))) {
				// ES TRANSFERENCIA CEL2CEL
				excepcion = "CEL2CEL";
			} else if ((procCode.equals("401010") || procCode.equals("401020") || procCode.equals("402010")
					|| procCode.equals("402020"))) {
				// ES TRANSFERENCIA NORMAL
				excepcion = "NORMAL";
			}
			// **********************************************************************************

			/*
			 * if(nameInterface.toLowerCase().equals("genericinternet") &&
			 * (procCode.equals("401010") || procCode.equals("401020") ||
			 * procCode.equals("402010") || procCode.equals("402020")) &&
			 * msg.isFieldSet(125) && (msg.getField(125).length()>90 &&
			 * msg.getField(125).length()<=150) &&
			 * (!msg.getField(125).substring(138,139).equals("1") &&
			 * !msg.getField(125).substring(138,139).equals("2"))) {
			 * validateAutra.setRute(Constants.TransactionRouting.INT_AUTRA);
			 * validateAutra.setP125Accion("REDUCIR"); return validateAutra; }
			 * if(nameInterface.toLowerCase().equals("genericinternet") &&
			 * (procCode.equals("401010") || procCode.equals("401020") ||
			 * procCode.equals("402010") || procCode.equals("402020")) &&
			 * msg.isFieldSet(125) && msg.getField(125).length()<=90) {
			 * validateAutra.setRute(Constants.TransactionRouting.INT_AUTRA); return
			 * validateAutra; } if(nameInterface.toLowerCase().equals("genericinternet") &&
			 * (procCode.equals("401010") || procCode.equals("401020") ||
			 * procCode.equals("402010") || procCode.equals("402020")) &&
			 * !msg.isFieldSet(125)) {
			 * validateAutra.setRute(Constants.TransactionRouting.INT_AUTRA); return
			 * validateAutra; }
			 */

			if (applyV2Filter) {
				GenericInterface.getLogger().logLine("APLICA FILTRO V2");
				channel = BussinesRules.channelIdentifier(msg, nameInterface, udpClient);
				keyTerminal = excepcion != null
						? nameInterface + "_" + channel + "_" + procCode + "_" + excepcion + "_" + terminalId
						: nameInterface + "_" + channel + "_" + procCode + "_" + terminalId;
				keyBin = excepcion != null
						? nameInterface + "_" + channel + "_" + procCode + "_" + excepcion + "_" + bin
						: nameInterface + "_" + channel + "_" + procCode + "_" + bin;
				keyCuentaFv2 = excepcion != null
						? nameInterface + "_" + channel + "_" + procCode + "_" + excepcion + "_" + cuenta
						: nameInterface + "_" + channel + "_" + procCode + "_" + cuenta;
				keyAll = excepcion != null ? nameInterface + "_" + channel + "_" + procCode + "_" + excepcion + "_"
						: nameInterface + "_" + channel + "_" + procCode + "_";

				GenericInterface.getLogger().logLine("keyTerminal " + keyTerminal);
				GenericInterface.getLogger().logLine("keybin " + keyBin);

				GenericInterface.fillMaps.getFiltrosV2().forEach((k, v) -> {
					GenericInterface.getLogger().logLine("key: " + k + " with value: " + v);
				});

				// Verifica terminales
				if (GenericInterface.fillMaps.getFiltrosV2().containsKey(keyTerminal)) {
					value = GenericInterface.fillMaps.getFiltrosV2().get(keyTerminal).split("_");
					validateAutra.setRuta(value[0]);
					validateAutra.setRute(
							value[0].toLowerCase().equals("capa") ? Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION
									: Constants.TransactionRouting.INT_AUTRA);
					validateAutra.setP100Valor(value[1]);
					validateAutra.setP125Accion(value[2]);
					validateAutra.setP125Valor(value[3]);

					GenericInterface.getLogger().logLine("validateAutra Ruta:" + validateAutra.getRuta());
					GenericInterface.getLogger().logLine("validateAutra Rute:" + validateAutra.getRute());
					GenericInterface.getLogger().logLine("validateAutra p100:" + validateAutra.getP100Valor());
					GenericInterface.getLogger().logLine("validateAutra p125 valor:" + validateAutra.getP125Valor());
					GenericInterface.getLogger().logLine("validateAutra p125 accion:" + validateAutra.getP125Accion());
					return validateAutra;
				}

				// Verifica bines
				if (GenericInterface.fillMaps.getFiltrosV2().containsKey(keyBin)) {
					value = GenericInterface.fillMaps.getFiltrosV2().get(keyBin).split("_");
					validateAutra.setRuta(value[0]);
					validateAutra.setRute(
							value[0].toLowerCase().equals("capa") ? Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION
									: Constants.TransactionRouting.INT_AUTRA);
					validateAutra.setP100Valor(value[1]);
					validateAutra.setP125Accion(value[2]);
					validateAutra.setP125Valor(value[3]);

					GenericInterface.getLogger().logLine("validateAutra Ruta:" + validateAutra.getRuta());
					GenericInterface.getLogger().logLine("validateAutra Rute:" + validateAutra.getRute());
					GenericInterface.getLogger().logLine("validateAutra p100:" + validateAutra.getP100Valor());
					GenericInterface.getLogger().logLine("validateAutra p125 valor:" + validateAutra.getP125Valor());
					GenericInterface.getLogger().logLine("validateAutra p125 accion:" + validateAutra.getP125Accion());
					return validateAutra;
				}

				// Verifica cuentas
				if (GenericInterface.fillMaps.getFiltrosV2().containsKey(keyCuentaFv2)) {
					value = GenericInterface.fillMaps.getFiltrosV2().get(keyCuentaFv2).split("_");
					validateAutra.setRuta(value[0]);
					validateAutra.setRute(
							value[0].toLowerCase().equals("capa") ? Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION
									: Constants.TransactionRouting.INT_AUTRA);
					validateAutra.setP100Valor(value[1]);
					validateAutra.setP125Accion(value[2]);
					validateAutra.setP125Valor(value[3]);

					GenericInterface.getLogger().logLine("validateAutra Ruta:" + validateAutra.getRuta());
					GenericInterface.getLogger().logLine("validateAutra Rute:" + validateAutra.getRute());
					GenericInterface.getLogger().logLine("validateAutra p100:" + validateAutra.getP100Valor());
					GenericInterface.getLogger().logLine("validateAutra p125 valor:" + validateAutra.getP125Valor());
					GenericInterface.getLogger().logLine("validateAutra p125 accion:" + validateAutra.getP125Accion());
					return validateAutra;
				}

				// Verifica solo pcode
				if (GenericInterface.fillMaps.getFiltrosV2().containsKey(keyAll)) {
					value = GenericInterface.fillMaps.getFiltrosV2().get(keyAll).split("_");
					validateAutra.setRuta(value[0]);
					validateAutra.setRute(
							value[0].toLowerCase().equals("capa") ? Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION
									: Constants.TransactionRouting.INT_AUTRA);
					validateAutra.setP100Valor(value[1]);
					validateAutra.setP125Accion(value[2]);
					validateAutra.setP125Valor(value[3]);

					GenericInterface.getLogger().logLine("validateAutra Ruta:" + validateAutra.getRuta());
					GenericInterface.getLogger().logLine("validateAutra Rute:" + validateAutra.getRute());
					GenericInterface.getLogger().logLine("validateAutra p100:" + validateAutra.getP100Valor());
					GenericInterface.getLogger().logLine("validateAutra p125 valor:" + validateAutra.getP125Valor());
					GenericInterface.getLogger().logLine("validateAutra p125 accion:" + validateAutra.getP125Accion());
					return validateAutra;
				}
			}

			channel = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13).equals(" ") ? "3"
					: msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13);

			if (nameInterface.toLowerCase().startsWith("credibanco"))
				channel = "C";

			keyTarjeta = channel + procCode + pan;
			keyBin = channel + procCode + bin;
			keyCuenta.append(channel);
			keyCuenta.append(procCode);
			keyCuenta.append(construyeCtasValidacionAutra(procCode, msg));

			switch (filtro) {
			case "Test1":
			case "Prod1":
				if (GenericInterface.fillMaps.getPrimerFiltroTest1().containsKey(keyBin)
						|| GenericInterface.fillMaps.getPrimerFiltroTest1().containsKey(keyTarjeta)
						|| GenericInterface.fillMaps.getPrimerFiltroTest1().containsKey(keyCuenta.toString()))
					validateAutra.setRute(Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION);
				else
					validateAutra.setRute(Constants.TransactionRouting.INT_AUTRA);

				break;
			}

		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(nameInterface, ValidateAutra.class.getName(), e,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "validateAutraNuevo", udpClient);
		}

		return validateAutra;
	}

	/************************************************************************************
	 *
	 * @param Base24Ath
	 * @return int 1, routing to AUTRA, 0 routing to Capa Integracion
	 * @throws Exception
	 ************************************************************************************/
	public static int getRouting(Base24Ath msg, Client udpClient, String nameInterface, String filtro)
			throws XFieldUnableToConstruct {

		int routingTo = 0;

		if (filtro.equals("Capa"))
			// Tarjetas de credito son administradas por FirstData
			return msg.getProcessingCode().toString().equals("333000") ? Constants.TransactionRouting.INT_AUTRA
					: Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION;

		// deuda tecnica
		String retRefNumber = "N/D";
		String channel = "N/D";
		String pan = msg.getTrack2Data().getPan();
		String procCode = msg.getProcessingCode().toString();
		String bin = pan.substring(0, 6);
		StringBuilder keyCuenta = new StringBuilder();
		try {
			channel = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13).equals(" ") ? "E"
					: msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(12, 13);
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			String[] terminalsID = { "8354", "8110", "9631", "9632" };
			String terminalId = msg.getField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID).substring(4, 8);
			if (nameInterface.toLowerCase().startsWith("generictestdes2")) {
				routingTo = Constants.TransactionRouting.INT_AUTRA;
			} else if (Arrays.stream(terminalsID).anyMatch(terminalId::equals)) {
				routingTo = Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION;
			} else {

				if (procCode.equals("890000"))
					procCode = msg.getField(126).substring(22, 28);
				String keyTarjeta = channel + procCode + pan;
				String keyBin = channel + procCode + bin;
				keyCuenta.append(channel);
				keyCuenta.append(procCode);
				keyCuenta.append(construyeCtasValidacionAutra(procCode, msg));

				GenericInterface.getLogger().logLine("keybin " + keyBin);
				GenericInterface.getLogger().logLine("keyTarjeta " + keyBin);
				GenericInterface.getLogger().logLine("keyCuenta " + keyBin);

				switch (filtro) {
				case "Test1":
				case "Prod1":
					if (GenericInterface.fillMaps.getPrimerFiltroTest1().containsKey(keyBin)
							|| GenericInterface.fillMaps.getPrimerFiltroTest1().containsKey(keyTarjeta)
							|| GenericInterface.fillMaps.getPrimerFiltroTest1().containsKey(keyCuenta.toString()))
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
					"getRouting", udpClient,
					routingTo == Constants.TransactionRouting.INT_AUTRA ? "Enrutamiento Autra Key: "
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
				if ((GenericInterface.fillMaps.getMigratedCards().containsKey(pan)
						|| GenericInterface.fillMaps.getMigratedBins().containsKey(bin))
						&& GenericInterface.fillMaps.getMigratedOpCodes().containsKey(procCode)) {

					String opCode = GenericInterface.fillMaps.getMigratedOpCodes().get(procCode);
					switch (opCode) {

					case "890000":// Consultas de costo, se valida si el codigo de procesamiento de la consulta se
									// encuentra en la tabla de codigos migrados

						String opCode126 = msg.getField(126).substring(22, 28);
						if (GenericInterface.fillMaps.getMigratedOpCodes().containsKey(opCode126)) {
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
					"validateAutra", udpClient,
					routingTo == Constants.TransactionRouting.INT_AUTRA ? "Enrutamiento Autra Key: "
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

			if (GenericInterface.fillMaps.getPrimerFiltroTest2().containsKey(key1)) {

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

				if (GenericInterface.fillMaps.getSegundoFiltroTest2().containsKey(key2)) {
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
		case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_AHORROS:
		case Constants.Channels.PCODE_TRANSFERENCIAS_AHORROS_A_CORRIENTE:
		case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_AHORROS:
		case Constants.Channels.PCODE_TRANSFERENCIAS_CORRIENTE_A_CORRIENTE:

			subKey = msg.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2)
					? msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2)
							.substring(msg.getField(Iso8583.Bit._103_ACCOUNT_ID_2).length() - 16).trim()
					: Constants.General.SIXTEEN_ZEROS;

			break;
		case Constants.Channels.PCODE_RETIRO_SIN_TARJETA_CNB_DE_AHORROS_A_CNBAHORROS:
		case Constants.Channels.PCODE_RETIRO_SIN_TARJETA_CNB_DE_AHORROS_A_CNBCORRIENTE:
		case Constants.Channels.PCODE_RETIRO_SIN_TARJETA_CNB_DE_CORRIENTE_A_CNBAHORROS:
		case Constants.Channels.PCODE_RETIRO_SIN_TARJETA_CNB_DE_CORRIENTE_A_CNBCORRIENTE:

			subKey = msg.isFieldSet(Iso8583.Bit._104_TRAN_DESCRIPTION)
					? msg.getField(Iso8583.Bit._104_TRAN_DESCRIPTION).substring(2, 12).trim()
					: Constants.General.TEN_ZEROS;

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
