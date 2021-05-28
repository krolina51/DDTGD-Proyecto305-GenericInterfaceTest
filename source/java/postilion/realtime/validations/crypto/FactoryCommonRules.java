package postilion.realtime.validations.crypto;

import java.security.Key;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;

import postilion.realtime.genericinterface.GenericInterface;
import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.sdk.crypto.CryptoCfgManager;
import postilion.realtime.sdk.crypto.CryptoManager;
import postilion.realtime.sdk.crypto.DesKvc;
import postilion.realtime.sdk.crypto.DesKwa;
import postilion.realtime.sdk.crypto.impl.atalla.AtallaMsg;
import postilion.realtime.sdk.jdbc.JdbcManager;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.message.bitmap.StructuredData;
import postilion.realtime.sdk.util.Convert;
import postilion.realtime.sdk.util.XPostilion;

@SuppressWarnings("deprecation")
public class FactoryCommonRules {

	private static final String _PCI_DATA_EXTENDED = "PCI_DATA_EXTENDED";
	private static final String _VALIDACION_PIN = "01";
	private static final String _CAMBIO_PIN = "02";
	private static final String _CREACION_PIN = "03";
	private static final String _TRADUCCION_PIN = "04";
	private static final String _TRASLATE_PIN = "06";
	private static final String _ERROR_HSM = "00000000ER99";
	private Parameters params;
	private String ip =  "10.86.82.119";
	private int port = 7000;

	public CryptoCfgManager crypcfgman = CryptoManager.getStaticConfiguration();
	private static postilion.realtime.sdk.ipc.SecurityManager SEC_MANAGER;

	static {
		try {
			SEC_MANAGER = new postilion.realtime.sdk.ipc.SecurityManager();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public FactoryCommonRules(Parameters params, String ip, int port) {
		this.params = params;
		this.ip = ip;
		this.port = port;
		GenericInterface.getLogger().logLine("Constructor --- ip "+ip+" port "+port);
	}

	/**
	 * Evalua la Mac
	 * 
	 * @author camayo
	 * @param MsgIn Mensaje en Base 24 , Mensaje en Iso
	 * @return Valor del Campo
	 * @throws XPostilion
	 */
	public Object responderMensaje0800(Base24Ath MsgIn, Iso8583Post MsgOld) throws XPostilion {

		int msg_type = MsgOld.getMsgType();
		Object r[] = { null, null };
		if (msg_type == Iso8583.MsgType._0800_NWRK_MNG_REQ) {
			if (MsgOld != null) {
				r[0] = "CFG_WS_01";
				MsgOld.putMsgType(Iso8583.MsgType._0810_NWRK_MNG_REQ_RSP);
				MsgOld.putField(Iso8583.Bit._039_RSP_CODE, "00");
				r[1] = MsgOld;
			}
			return r;
		}
		return r;
	}

	
	public Object desencryptPan(Base24Ath MsgIn, Iso8583Post MsgOld) throws XPostilion {

		int msg_type = MsgOld.getMsgType();
		StructuredData std_data = new StructuredData();
		StructuredData stData = null;
		String tranType = "";
		Object r[] = { null, null };

		// 900000 Desencriptar
		// 910000 encriptar
		if (msg_type == Iso8583.MsgType._0600_ADMIN_REQ) {
			if (MsgOld != null) {
				tranType = MsgOld.getField(Iso8583.Bit._003_PROCESSING_CODE);
				stData = MsgOld.getStructuredData();
				r[0] = "CFG_WS_02";
				DesKwa current_in_card = null;
				String node = params.getNameInterface();
				MsgOld.putMsgType(Iso8583.MsgType._0610_ADMIN_REQ_RSP);
				MsgOld.putField(Iso8583.Bit._039_RSP_CODE, Iso8583.RspCode._00_SUCCESSFUL);
				// postilion.realtime.sdk.ipc.SecurityManager SEC_MANAGER; // Performance -
				// 2017/01/04
				try {
					// SEC_MANAGER = new postilion.realtime.sdk.ipc.SecurityManager(); //
					// Performance - 2017/01/04

					// Código de Procesamiento agregado para retornar los dos criptogramas -
					// 2017/01/27
					if (tranType.equals("910200")) {
						std_data.put(GeneralConstant._PCI_DATA,
								SEC_MANAGER.encryptPan(stData.get(GeneralConstant._PCI_DATA)));
						std_data.put(_PCI_DATA_EXTENDED, SEC_MANAGER.encrypt(stData.get(GeneralConstant._PCI_DATA)));
					}

					if (tranType.equals("920000")) { // Para procesar el PINBLOCK
						String pin_clear = MsgOld.getField(Iso8583.Bit._052_PIN_DATA);
						current_in_card = crypcfgman.getKwa(node + "_CARD");
						String pin_block = calcPinBlock(pin_clear, current_in_card);

						MsgOld.putField(Iso8583.Bit._052_PIN_DATA, Convert.fromHexToBin(pin_block));
						std_data.put("PIN_BLOCK", Iso8583.RspCode._00_SUCCESSFUL);
					}

					if (tranType.equals("900000")) {
						std_data.put(GeneralConstant._PCI_DATA,
								SEC_MANAGER.decryptPan(stData.get(GeneralConstant._PCI_DATA)));
					} else {
						if (tranType.equals("910000")) {
							std_data.put(GeneralConstant._PCI_DATA,
									SEC_MANAGER.encryptPan(stData.get(GeneralConstant._PCI_DATA)));
						} else {
							if (tranType.equals("900100")) {
								std_data.put(GeneralConstant._PCI_DATA,
										SEC_MANAGER.decryptToString(stData.get(GeneralConstant._PCI_DATA)));
							} else {
								if (tranType.equals("910100")) {
									std_data.put(GeneralConstant._PCI_DATA,
											SEC_MANAGER.encrypt(stData.get(GeneralConstant._PCI_DATA)));
								}
							}
						}
					}
				} catch (Exception e) {
					MsgOld.putField(Iso8583.Bit._039_RSP_CODE, Iso8583.RspCode._30_FORMAT_ERROR);
					std_data.put(GeneralConstant._PCI_DATA, "");
					e.printStackTrace();
				}

				MsgOld.putStructuredData(std_data);

				r[1] = MsgOld;
			}
			return r;
		}
		return r;
	}

	public String calcPinBlock(String pinClear, DesKwa kvp) {
		String keyPin = postilion.realtime.sdk.util.convert.Transform.fromBinToHex(pinClear);
		// System.out.println("Pin enviado " + keyPin);
		return keyPin;
	}

	@SuppressWarnings("unused")
	private static byte[] tripleDES(byte[] value, Key key, int mode) {
		try {
			Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
			cipher.init(mode, key);
			return cipher.doFinal(value);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String encrypt(String value, Key key) {
		return "";/// Hexa.toHex(tripleDES(Hexa.toBytes(value), key, Cipher.ENCRYPT_MODE));
	}

	/*
	 * public static String decrypt(String value, Key key) { return
	 * Hexa.toHex(tripleDES(Hexa.toBytes(value), key, Cipher.DECRYPT_MODE)); }
	 */
	/**
	 * Regla de Verificacion del PIN de una cuenta.
	 * 
	 * @param MsgIn
	 * @param MsgOld
	 * @param messageConverterFramework
	 * @return
	 * @throws XPostilion
	 */

	@SuppressWarnings("resource")
	private String executeQuery(String Pan, String Perfil) throws SQLException {
		Connection cn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			// Para Postilion 4.3
			cn = JdbcManager.getDefaultConnection();
			String key = "";
			String Offset = "";
			stmt = cn.prepareStatement("SELECT concat(issuer_nr,'_',curr_table_cards)n "
					+ " from postcard.[dbo].[pc_issuers] (NOLOCK) " + " where issuer_name='" + Perfil + "';");
			rs = stmt.executeQuery();
			while (rs.next() != false) {
				key = rs.getString(1);
			}

			if (key != null) {
				// Obtiene el pvvoffset
				stmt = cn.prepareStatement("select pvv_or_pin_offset " + " from [postcard].[dbo].[pc_cards_" + key
						+ "] with (nolock) " + " where pan_encrypted='" + Pan + "' and date_deleted is null ");
				rs = stmt.executeQuery();
				while (rs.next() != false) {
					Offset = rs.getString(1);
				}
			} else {
				GenericInterface.getLogger().logLine("Issuer no encontrado");
			}
			JdbcManager.commit(cn, stmt, rs);
			return Offset;
		} finally {
			JdbcManager.cleanup(cn, stmt, rs);
		}
	}

	public Object[] commandProcess(Base24Ath MsgIn, Iso8583Post MsgOld) throws XPostilion {
		Object r[] = { "", null };
		String resultado = "";
		GenericInterface.getLogger().logLine("commandProcess");
//		String rutaLog = messageConverterFramework.runtime_params[4];
//		String nombreLog = this.getClass().getSimpleName().toString() + _EXT;
//		String permisoLog = messageConverterFramework.runtime_params[6];
//
//		logger.createFileLog(rutaLog, nombreLog, permisoLog);
		try {
			// Obtiene la informacion del StructuredData para procesar la informacion
			// dependiendo la validacion que se requiera
			StructuredData std_data = null;

			String accion = null;

			try {
				// Obtiene el valor del tipo de comando a procesar
				std_data = MsgOld.getStructuredData();
				accion = std_data.get("_PROCESS_TYPE");
				GenericInterface.getLogger().logLine("accion == _PROCESS_TYPE: "+accion);
			} catch (Exception e) {
				// Genera mensaje de respuest si ocurre alguna excepcion con el structureddata
				MsgOld.setMessageType(MsgOld.getResponseMessageType());
				MsgOld.putField(Iso8583Post.Bit._039_RSP_CODE, Iso8583Post.RspCode._12_INVALID_TRAN);
				MsgOld.putField(Iso8583Post.Bit._059_ECHO_DATA, "NO SE ENCUENTRA EL STRUCTUREDDATA");
				System.out.println("ERROR _PROCCESS_TYPE ");
				r[1] = MsgOld;
				return r;
			}

			// Valida que se tenga el comando a procesar con la informacion requerida
			if (accion != null && std_data != null) {
				/*
				 * Comando de Validacion del PIN _PROCCESS_TYPE=01
				 */
				if (accion.equals(_VALIDACION_PIN)) {
					GenericInterface.getLogger().logLine("VALIDACION_PIN " + _VALIDACION_PIN);
					resultado = verificarPin_HSM(MsgIn, MsgOld);
					MsgOld.putField(Iso8583Post.Bit._039_RSP_CODE, resultado);
					GenericInterface.getLogger()
							.logLine("Fin VALIDACION_PIN " + _VALIDACION_PIN + " codigo del resultado " + resultado);
				}
				/*
				 * Comando de cambio de PIN _PROCCESS_TYPE=02
				 */
				if (accion.equals(_CAMBIO_PIN)) {
					resultado = CambioPin_HSM(MsgIn, MsgOld);
					String[] respuesta = resultado.split(",");
					StructuredData sd = MsgOld.getStructuredData();

					String of_k = null;
					if (respuesta[0].equals(Iso8583Post.RspCode._00_SUCCESSFUL)) {
						// of_k=base64Encoder(of_k,"E");
						of_k = respuesta[1];
						sd.put("_OFFSET_PVV", of_k);
					}

					MsgOld.putStructuredData(sd);
					resultado = respuesta[0];
					MsgOld.putField(Iso8583Post.Bit._039_RSP_CODE, resultado);
				}

				// Traduccion del PIN = 04
				if (accion.equals(_TRADUCCION_PIN)) {
					resultado = traslatePin(MsgIn, MsgOld);
					String[] h = resultado.split(",");
					StructuredData sd = MsgOld.getStructuredData();
					String pb_value = h[1];
					if (h[0].equals(Iso8583Post.RspCode._00_SUCCESSFUL)) {
						pb_value = base64Encoder(sd.get("_PIN_B64"), "E");
					} else {
						h[0] = GeneralConstant._CLAVEINVALIDA;
					}
					sd.put("_PIN_NEW_B64", pb_value);
					MsgOld.putStructuredData(sd);
					resultado = h[0];
					MsgOld.putField(Iso8583Post.Bit._039_RSP_CODE, resultado);
				}

				// Crear del PIN = 03
				if (accion.equals(_CREACION_PIN)) {
					// Envia la información para ejecutar el comando
					resultado = CreacionPin_HSM(MsgIn, MsgOld);
					// Recibe la respuesta del comando
					String[] respuesta = resultado.split(",");
					// Obtiene la información proveniente del mensaje para adicionar el resultado en
					// el SD
					StructuredData sd = MsgOld.getStructuredData();
					// Obtiene el valor a retornale al SD

					// Evalua que la resuesta sea exitosa para asignar la respuesta en el mensaje
					if (respuesta[0].equals(Iso8583Post.RspCode._00_SUCCESSFUL)) {
						// pb_value=base64Encoder(pb_value,"E");
						sd.put("_OFFSET", respuesta[1]);
						MsgOld.putStructuredData(sd);
					}
					MsgOld.putField(Iso8583Post.Bit._039_RSP_CODE, respuesta[0]);
				}

				if (accion.equals(_TRASLATE_PIN)) {

					resultado = traslatePin(MsgIn, MsgOld);
					String[] respuesta = resultado.split("#");
					StructuredData sd = MsgOld.getStructuredData();
					sd.put("_RESPUESTA", respuesta[1]);
					MsgOld.setMessageType(MsgOld.getResponseMessageType());
					if ("N".equals(respuesta[2]))
						MsgOld.putField(Iso8583Post.Bit._039_RSP_CODE, GeneralConstant._ERRORGENERICO);
					else
						MsgOld.putField(Iso8583Post.Bit._039_RSP_CODE, Iso8583Post.RspCode._00_SUCCESSFUL);
					MsgOld.putStructuredData(sd);
					r[1] = MsgOld;
					return r;
				}

			} else {
				MsgOld.setMessageType(MsgOld.getResponseMessageType());
				MsgOld.putField(Iso8583Post.Bit._039_RSP_CODE, Iso8583Post.RspCode._12_INVALID_TRAN);
				MsgOld.putField(Iso8583Post.Bit._059_ECHO_DATA,
						"NO SE ENCUENTRA EL TIPO DE COMANDO A PROCESAR EN EL STRUCTUREDDATA");
				System.out.println("ERROR _PROCCESS_TYPE 1");
				r[1] = MsgOld;
				return r;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		MsgOld.setMessageType(MsgOld.getResponseMessageType());
		r[1] = MsgOld;
		return r;
	}

	public String verificarPin_HSM(Base24Ath MsgIn, Iso8583Post MsgOld) throws XPostilion {
		/*
		 * Se usara el COMANDO de la caja = 32 Requiere creacion de llaves que contiene
		 * los criptogramas por canal Por tal razon y revisando la caracteristicas del
		 * negocio se tiene una llave por cada canal PBK y VBK
		 * 
		 * 
		 */
		// System.out.println("<<<Comando 32 Verificacion de Pin 01>>>");
		GenericInterface.getLogger().logLine("<<<Comando 32 Verificacion de Pin 01>>>");
		// Se asignara la respuesta que se realice despues del comando
		String respuesta = "";
		String resultado33 = null;
		String pb_new = null;
		// Carga las
		DesKwa current_in_pbk = null;
		DesKwa current_in_vbk = null;
		StructuredData sd = MsgOld.getStructuredData();
		String canal = sd.get("_CHANNEL");
		String commandHSM = null;
		// Valores que se asig
		String key1 = "";
		String key2 = "";
		String campo11 = "";
		String campo112 = "";

		// Obtiene el nombre de la interchange para buscar el valor de la llaves
		String node = params.getNameInterface();
		campo11 = MsgOld.getField(Iso8583Post.Bit._011_SYSTEMS_TRACE_AUDIT_NR) + ""
				+ AtallaMsg.Command._33_TRANSLATE_PIN;
		campo112 = MsgOld.getField(Iso8583Post.Bit._011_SYSTEMS_TRACE_AUDIT_NR) + "" + AtallaMsg.Command._32_VERIFY_PIN;

		try {
			// Obtiene los nombre de los criptogramas por cada canal
			current_in_pbk = crypcfgman.getKwa(node + "_" + canal + "_PBK");
			current_in_vbk = crypcfgman.getKwa(node + "_VBK");

			// Asigna los valores de los criptogramas
			key1 = current_in_pbk.getContents().getAdditionalData();
			key2 = current_in_vbk.getContents().getAdditionalData();
			GenericInterface.getLogger().logLine("Criptogramas cargados correctamente");
		} catch (Exception e) {
			GenericInterface.getLogger().logLine("Criptogramas no fueron cargados " + e.getMessage());
			e.printStackTrace();
		}

		// Se ejecuta la oepracion de traduccion de pin
		resultado33 = traslatePin(MsgIn, MsgOld);
		// Validacion de la ejecucion del comando existoso
		if (resultado33 == null || _ERROR_HSM.equals(resultado33) || "".equals(resultado33)) {
			return respuesta = Iso8583Post.RspCode._91_ISSUER_OR_SWITCH_INOPERATIVE;
		} else {
			if (resultado33.indexOf("#^" + campo11 + "#") > 0) {
				if (resultado33.indexOf("#Y#") > 0) {
					int inicio = resultado33.indexOf("#", 0);
					int fin = resultado33.indexOf("#", 5);
					pb_new = resultado33.substring(inicio, fin);
					GenericInterface.getLogger().logLine("Comando 33 Se genero exitoso ");
				} else {
					GenericInterface.getLogger().logLine("Comando 33 rechazado no se genero el nuevo PinBlock");
					return respuesta = GeneralConstant._ERRORGENERICO;
				}
			} else {
				respuesta = Iso8583Post.RspCode._12_INVALID_TRAN;
				System.out.println("Respuesta no corresponde con la solicitud");
				new HSMDirectorBuild().resetConecction(this.ip, this.port);
				return respuesta;
			}
		}
		// Obiene el valor del pan para luego ser encriptado
		String Pan = MsgOld.getField(Iso8583Post.Bit._002_PAN);
		// Encripta el pan para ser consultado en la base de datos y verificar que
		// exista la tarjeta
		String EncPan = SEC_MANAGER.encrypt(Pan);

		// Extrae el offset, sino lo encuentra declina con codigo 25 para autra
		String offset = "";
		try {
			offset = executeQuery(EncPan, "BBogota");
			if ("".equals(offset) || offset == null) {
				GenericInterface.getLogger().logLine("PVV Offset no encontrado ");
				respuesta = GeneralConstant._ESTNOPERMITETX;
			} else {

				commandHSM = "<32#2#3" + pb_new + "#" + key1 + "#0123456789012345#" + offset + "#" + Pan.substring(4)
						+ "#F#4#" + key2 + "#F#" + Pan.substring(3, Pan.length() - 1) + "#^" + campo112 + "#>";
				HSMDirectorBuild hsmComm = new HSMDirectorBuild();
				hsmComm.openConnectHSM(ip, port);
				String resultado = hsmComm.sendCommand(commandHSM, this.ip, this.port);
				if (resultado == null || _ERROR_HSM.equals(resultado) || "".equals(resultado)) {
					respuesta = Iso8583Post.RspCode._91_ISSUER_OR_SWITCH_INOPERATIVE;
				} else {
					if (resultado.indexOf("#^" + campo112 + "#") > 0) {
						respuesta = GeneralConstant._CLAVEINVALIDA;
						if (resultado.indexOf("#Y#") > 0) {
							respuesta = Iso8583Post.RspCode._00_SUCCESSFUL;
							GenericInterface.getLogger().logLine("Comando 32 Se genero exitoso ");
						} else {
							GenericInterface.getLogger().logLine("Comando 32 rechazado no se genero correctamente");
						}
					} else {
						respuesta = Iso8583Post.RspCode._12_INVALID_TRAN;
						System.out.println("Respuesta no corresponde con la solicitud");
						return respuesta;
					}
				}
			}
		} catch (SQLException e) {
			GenericInterface.getLogger().logLine("<<<SQLException>> "+Utils.getStringMessageException(e));
		}
		GenericInterface.getLogger().logLine("<<<Fin Comando 32 Verificacion de Pin 01>>>");

		return respuesta;
	}

	public String CreacionPin_HSM(Base24Ath MsgIn, Iso8583Post MsgOld) throws XPostilion {
		/*
		 * Se usara el COMANDO de la caja = 37 Requiere creacion de llaves que contiene
		 * los criptogramas Por tal razon y revisando la caracteristicas del negocio se
		 * tiene una llave por cada canal PBK y VBK
		 * 
		 * 
		 */
		GenericInterface.getLogger().logLine("<<<Inicio Comando 37 Creacion de Pin 03>>>");
		// Se asignara la respuesta que se realice despues del comando
		String respuesta = "";
		String resultado33 = null;
		String pb_new = null;
		String campo11 = null;
		String campo112 = null;
		// Carga las
		DesKwa current_in_pbk = null;
		DesKwa current_in_vbk = null;
		StructuredData sd = MsgOld.getStructuredData();
		String canal = sd.get("_CHANNEL");

		// Valores que se asig
		String key1 = "";
		String key2 = "";
		String offset = "0000";

		// Obiene el valor del pan
		String Pan = MsgOld.getField(Iso8583Post.Bit._002_PAN);

		// Obtiene el nombre de la interchange para buscar el valor de la llaves
		String node = params.getNameInterface();
		campo11 = MsgOld.getField(Iso8583Post.Bit._011_SYSTEMS_TRACE_AUDIT_NR) + ""
				+ AtallaMsg.Command._33_TRANSLATE_PIN;
		campo112 = MsgOld.getField(Iso8583Post.Bit._011_SYSTEMS_TRACE_AUDIT_NR) + "" + AtallaMsg.Command._37_PIN_CHANGE;

		try {
			// Obtiene los nombre de los criptogramas por cada canal
			current_in_pbk = crypcfgman.getKwa(node + "_" + canal + "_PBK");
			current_in_vbk = crypcfgman.getKwa(node + "_VBK");
			// Asigna los valores de los criptogramas
			key1 = current_in_pbk.getContents().getAdditionalData();
			key2 = current_in_vbk.getContents().getAdditionalData();
			GenericInterface.getLogger().logLine("Criptogramas cargados correctamente");
		} catch (Exception e) {
			GenericInterface.getLogger().logLine("Criptogramas no fueron cargados " + e.getMessage());
			e.printStackTrace();
		}

		resultado33 = traslatePin(MsgIn, MsgOld);
		// Validacion de la ejecucion del comando existoso
		if (resultado33 == null || _ERROR_HSM.equals(resultado33) || "".equals(resultado33)) {
			return respuesta = Iso8583Post.RspCode._91_ISSUER_OR_SWITCH_INOPERATIVE;
		} else {
			if (resultado33.indexOf("#^" + campo11 + "#") > 0) {
				if (resultado33.indexOf("#Y#") > 0) {
					int inicio = resultado33.indexOf("#", 0);
					int fin = resultado33.indexOf("#", 5);
					pb_new = resultado33.substring(inicio, fin);
					GenericInterface.getLogger().logLine("Comando 33 creacion de pin se genero exitoso ");
				} else {
					GenericInterface.getLogger()
							.logLine("Comando 33 creacion de pin rechazado no se genero el nuevo PinBlock");
					return respuesta = GeneralConstant._CLAVEINVALIDA;
				}
			} else {
				respuesta = Iso8583Post.RspCode._12_INVALID_TRAN;
				System.out.println("Respuesta no corresponde con la solicitud");
				new HSMDirectorBuild().resetConecction("", 1);
				return respuesta;
			}
		}

		String commandHSM = "<37#2#3##" + key1 + "#0123456789012345#" + offset + "#" + Pan.substring(4) + "#F#4#" + key2
				+ pb_new + "#F#" + Pan.substring(3, Pan.length() - 1) + "#^" + campo112 + "#>";
		HSMDirectorBuild hsmComm = new HSMDirectorBuild();
		hsmComm.openConnectHSM(ip, port);
		String resultado = hsmComm.sendCommand(commandHSM, this.ip, this.port);

		if (resultado == null || _ERROR_HSM.equals(resultado) || "".equals(resultado)) {
			respuesta = Iso8583Post.RspCode._91_ISSUER_OR_SWITCH_INOPERATIVE;
		} else {
			if (resultado.indexOf("#^" + campo112 + "#") > 0) {
				respuesta = GeneralConstant._CLAVEINVALIDA;
				if (resultado.indexOf("#NO#") > 0) {
					int ll_i = resultado.indexOf("#", 4);
					int ll_f = resultado.indexOf("#", ll_i + 1);
					String of_k = resultado.substring(ll_i + 1, ll_f);
					// System.out.println("offset_new "+of_k);
					respuesta = Iso8583Post.RspCode._00_SUCCESSFUL + "," + of_k;
					GenericInterface.getLogger().logLine("Comando 37 creacion de pin se genero exitoso ");
				} else {
					GenericInterface.getLogger().logLine("Comando 37 creacion pin no se genero correctamente");
				}
			} else {
				respuesta = Iso8583Post.RspCode._12_INVALID_TRAN;
				System.out.println("Respuesta no corresponde con la solicitud");
				GenericInterface.getLogger().logLine("creacion de pin no se genero correctamente");
				return respuesta;
			}
		}
		GenericInterface.getLogger().logLine("<<<Fin Comando 37 Creacion de Pin 03>>>");
		return respuesta;
	}

	public String CambioPin_HSM(Base24Ath MsgIn, Iso8583Post MsgOld) throws XPostilion {
		/*
		 * Se usara el COMANDO de la caja = 37 Requiere creacion de llaves que contiene
		 * los criptogramas Por tal razon y revisando la caracteristicas del negocio se
		 * tiene una llave por cada canal PBK y VBK
		 * 
		 * 
		 */
		GenericInterface.getLogger().logLine("<<<Inicio Comando 37 Cambio de Pin 02>>>");
		// Se asignara la respuesta que se realice despues del comando
		String respuesta = "";
		String resultado33 = null;
		String pb_old = null;
		String pb_new = null;
		String offset = null;
		DesKwa current_in_pbk = null;
		DesKwa current_in_vbk = null;
		StructuredData sd = MsgOld.getStructuredData();
		String canal = sd.get("_CHANNEL");
		String campo11 = "";
		String campo112 = "";

		// Valores que se asigna para los criptogramas
		String key1 = "";
		String key2 = "";
		// Obiene el valor del pan
		String Pan = MsgOld.getField(Iso8583Post.Bit._002_PAN);

		// Obtiene el nombre de la interchange para buscar el valor de la llaves
		// (criptogramas)
		String node = params.getNameInterface();
		campo11 = MsgOld.getField(Iso8583Post.Bit._011_SYSTEMS_TRACE_AUDIT_NR) + ""
				+ AtallaMsg.Command._33_TRANSLATE_PIN;
		campo112 = MsgOld.getField(Iso8583Post.Bit._011_SYSTEMS_TRACE_AUDIT_NR) + "" + AtallaMsg.Command._37_PIN_CHANGE;

		try {
			// Obtiene los nombre de los criptogramas por cada canal
			current_in_pbk = crypcfgman.getKwa(node + "_" + canal + "_PBK");
			current_in_vbk = crypcfgman.getKwa(node + "_VBK");

			// Asigna los valores de los criptogramas
			key1 = current_in_pbk.getContents().getAdditionalData();
			key2 = current_in_vbk.getContents().getAdditionalData();
			GenericInterface.getLogger().logLine("Criptogramas cargados correctamente");
		} catch (Exception e) {
			GenericInterface.getLogger().logLine("Criptogramas no fueron cargados " + e.getMessage());
			e.printStackTrace();
		}

		String EncPan = SEC_MANAGER.encrypt(Pan);
		System.out.println(EncPan);
		resultado33 = traslatePin(MsgIn, MsgOld);
		if (resultado33 == null || _ERROR_HSM.equals(resultado33) || "".equals(resultado33)) {
			return respuesta = Iso8583Post.RspCode._91_ISSUER_OR_SWITCH_INOPERATIVE;
		} else {
			if (resultado33.indexOf("#^" + campo11 + "#") > 0) {
				if (resultado33.indexOf("#Y#") > 0) {
					int inicio = resultado33.indexOf("#", 0);
					int fin = resultado33.indexOf("#", 5);
					pb_new = resultado33.substring(inicio, fin);
					GenericInterface.getLogger().logLine("Comando 33 cambio de PIN, Se genero exitoso PIN_B64");

					sd.put("_PIN_B64", sd.get("_PIN_B64_OLD"));
					sd.put("_PIN_B64_OLD", null);
					MsgOld.putStructuredData(sd);

					resultado33 = traslatePin(MsgIn, MsgOld);
					if (resultado33 == null || _ERROR_HSM.equals(resultado33) || "".equals(resultado33)) {
						return respuesta = Iso8583Post.RspCode._91_ISSUER_OR_SWITCH_INOPERATIVE;
					} else {
						if (resultado33.indexOf("#^" + campo11 + "#") > 0) {
							if (resultado33.indexOf("#Y#") > 0) {
								inicio = resultado33.indexOf("#", 0);
								fin = resultado33.indexOf("#", 5);
								pb_old = resultado33.substring(inicio, fin);
								GenericInterface.getLogger()
										.logLine("Comando 33 cambio de PIN, Se genero exitoso PIN_B64_OLD ");
								// Extrae el offset
								try {
									offset = executeQuery(EncPan, "BBFAVIRTUAL");
									if ("".equals(offset) || offset == null) {
										GenericInterface.getLogger().logLine("PVV Offset no encontrado ");
										respuesta = GeneralConstant._ESTNOPERMITETX;
									} else {

										String commandHSM = "<37#2#3" + pb_old + "#" + key1 + "#0123456789012345#"
												+ offset + "#" + Pan.substring(4) + "#F#4#" + key2 + pb_new + "#F#"
												+ Pan.substring(3, Pan.length() - 1) + "#^" + campo112 + "#>";
										HSMDirectorBuild hsmComm = new HSMDirectorBuild();
										hsmComm.openConnectHSM(ip, port);
										String resultado = hsmComm.sendCommand(commandHSM, this.ip, this.port);

										respuesta = GeneralConstant._CLAVEINVALIDA;
										if (resultado == null || _ERROR_HSM.equals(resultado) || "".equals(resultado)) {
											respuesta = Iso8583Post.RspCode._91_ISSUER_OR_SWITCH_INOPERATIVE;
										} else {
											if (resultado.indexOf("#^" + campo112 + "#") > 0) {
												if (resultado.indexOf("#Y#") > 0) {
													int ll_i = resultado.indexOf("#", 4);
													int ll_f = resultado.indexOf("#", ll_i + 1);
													String of_k = resultado.substring(ll_i + 1, ll_f);
													respuesta = Iso8583Post.RspCode._00_SUCCESSFUL + "," + of_k;
													GenericInterface.getLogger().logLine(
															"Comando 37 cambio de PIN, rechazado se genero correctamente");
												} else {
													GenericInterface.getLogger().logLine(
															"Comando 37 cambio de PIN, rechazado no se genero correctamente");
												}
											} else {
												respuesta = Iso8583Post.RspCode._12_INVALID_TRAN;
												System.out.println("Respuesta no corresponde con la solicitud");
												return respuesta;
											}
										}
									}
								} catch (SQLException e) {
									e.printStackTrace();
								}
							} else {
								GenericInterface.getLogger().logLine(
										"Comando 33 cambio de PIN, rechazado no se genero el nuevo PinBlock PIN_B64_OLD");
								return respuesta = GeneralConstant._ERRORGENERICO;
							}
						} else {
							respuesta = Iso8583Post.RspCode._12_INVALID_TRAN;
							System.out.println("Respuesta no corresponde con la solicitud");
							new HSMDirectorBuild().resetConecction(this.ip, this.port);
							return respuesta;
						}
					}
				} else {
					GenericInterface.getLogger()
							.logLine("Comando 33 cambio de PIN rechazado, no se genero el nuevo PinBlock PIN_B64");
					return respuesta = GeneralConstant._ERRORGENERICO;
				}
			} else {
				respuesta = Iso8583Post.RspCode._12_INVALID_TRAN;
				System.out.println("Respuesta no corresponde con la solicitud");
				new HSMDirectorBuild().resetConecction("", 1);
				return respuesta;
			}
		}
		GenericInterface.getLogger().logLine("<<<Fin Comando 37 Cambio de Pin 02>>>");
		return respuesta;
	}

	public String traslatePin(Base24Ath MsgIn, Iso8583Post MsgOld) throws XPostilion {
		GenericInterface.getLogger().logLine("traslatePin");
		String respuesta = "";
		DesKwa current_in_pbk = null;
		String key1 = "";
		StructuredData sd = MsgOld.getStructuredData();
		String canal = sd.get("_CHANNEL");
		String tipDoc = sd.get("_TIPDOC");
		String numDoc = sd.get("_NUMDOC");
		String pb = base64Encoder(sd.get("_PIN_B64"), "D");
		String padded = "0000000000".substring(numDoc.length()) + numDoc;
		String node = params.getNameInterface();
		String campo_11 = MsgOld.getField(Iso8583Post.Bit._011_SYSTEMS_TRACE_AUDIT_NR) + ""
				+ AtallaMsg.Command._33_TRANSLATE_PIN;

		if (pb == null) {
			// return GeneralConstant._ERRORGENERICO;
			System.out.println("Entro");
			return "12";
		}

		try {
			// Obtiene la llave origen y destino, esta informacion la trae de la llave que
			// se configuro en la HSM
			current_in_pbk = crypcfgman.getKwa(node + "_" + canal + "_PBK");
			key1 = current_in_pbk.getContents().getAdditionalData();
			GenericInterface.getLogger().logLine("Criptogramas cargados correctamente");
		} catch (Exception e) {
			e.printStackTrace();
			GenericInterface.getLogger().logLine("Criptogramas no fueron cargados " + e.getMessage());
			return respuesta = Iso8583Post.RspCode._69_ADVICE_RECEIVED_TOO_LATE;
		}
		String commandHSM = "<" + AtallaMsg.Command._33_TRANSLATE_PIN + "#13#" + key1 + "#" + key1 + "#" + pb + "#F"
				+ "#" + tipDoc + padded + "#^" + campo_11 + "#>";
		// String
		// commandHSM="<"+AtallaMsg.Command._33_TRANSLATE_PIN+"#13#"+key1+"#"+key1+"#"+pb+"#F"+"#"+tipDoc+padded+"#>";
		HSMDirectorBuild hsmComm = new HSMDirectorBuild();
		hsmComm.openConnectHSM(ip, port);
		respuesta = hsmComm.sendCommand(commandHSM, this.ip, this.port);
		return respuesta;
	}

	public String base64Encoder(String str, String accion) {
		if (accion.equals("E")) {
			// encode data on your side using BASE64
			String encoded = DatatypeConverter.printBase64Binary(str.getBytes());
			return encoded;
		} else {
			// Decode data on other side, by processing encoded data
			String decoded = new String(DatatypeConverter.parseBase64Binary(str));
			return decoded;
		}
	}

	public Object calc_CVV(Base24Ath MsgIn, Iso8583Post MsgOld, String puerto, String ip) throws XPostilion {
		// Datos conexion caja criptografica
		final String _COMMANDHSM = "00000000CW";
		final String _LLAVEHSM = "MEGABANCO_CVV";
		final String _CODIGOSERVICIO = "999";
		final String _SEPARADOR = ";";

		// Arreglo validacion mensajes
		Object r[] = { null, null };

		// LLave del criptograma
		DesKvc current_in_pbk = null;
		String key1 = "";
		try {
			// Valida el calculo del cvv solo para mensaje 0200
			if (MsgOld.getMsgType() == Iso8583Post.MsgType._0200_TRAN_REQ) {
				// Se trae la llave para el calculo
				current_in_pbk = crypcfgman.getKvc(_LLAVEHSM);
				key1 = current_in_pbk.getContents().getValueUnderKsk();
				// Obtiene el pan de la transaccion
				String pan = MsgOld.getField(Iso8583Post.Bit._002_PAN);
				// Obtiene la fecha de expiracion
				String exp_date = MsgOld.getField(Iso8583Post.Bit._014_DATE_EXPIRATION);
				// Construccion del comando
				String commandHSM = _COMMANDHSM + key1 + pan + _SEPARADOR + exp_date + _CODIGOSERVICIO;

				// Verifica la conexion con la HSM
				HSMDirectorBuild hsmConn = new HSMDirectorBuild();
				if (hsmConn.getSocket() == null) {
					System.out.println("Conexion nula para calculo calc_CVV");
					hsmConn.openConnectHSM(ip, Integer.parseInt(puerto));
				}

				String resultado = hsmConn.sendCommand(commandHSM, "", 1);
				String cvv = "";
				int k = resultado.trim().indexOf("CX00");

				if (k > 0) {
					cvv = resultado.trim().substring(k + 4, k + 7);
					String track_2 = pan + "D" + exp_date + "221" + "00000" + cvv;
					MsgOld.putField(Iso8583Post.Bit._035_TRACK_2_DATA, track_2);
					r[1] = MsgOld;
					r[0] = GeneralConstant._FOLLOW_MESSAGE_ISO;
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return r;
	}
}
