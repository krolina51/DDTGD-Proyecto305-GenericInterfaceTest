package postilion.realtime.validations.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import postilion.realtime.genericinterface.GenericInterface;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.sdk.crypto.CryptoCfgManager;
import postilion.realtime.sdk.crypto.CryptoManager;
import postilion.realtime.sdk.crypto.ICryptoConnection;
import postilion.realtime.sdk.crypto.XCryptoCommsFailure;

public class HSMDirectorBuild {
	private String command = null;
	private Socket socket = null;
	private DataOutputStream out = null;
	private DataInputStream in = null;
	public CryptoCfgManager crypto_cfg_man = null;
	public static ICryptoConnection connection = null;
	static String errorHsm = "00000000ER99";

	public Socket getSocket() {
		return socket;
	}

	public void resetConecction(String ip, int puerto) {
		try {
			socket.close();
			openConnectHSM(ip, puerto);
			System.out.println("Reconexion reset socket <<<" + ip + ":" + puerto + ">>>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String sendCommand(String commandHSM, String ip, int puerto) {
		String data = null;
		GenericInterface.getLogger().logLine("sendCommand");
		try {
			data = processMessage(commandHSM);
			if (data == null || errorHsm.equals(data)) {
				socket.close();
				openConnectHSM(ip, puerto);
				data = processMessage(commandHSM);
				System.out.println("Reconexion socket <<<" + ip + ":" + puerto + ">>>");
			}
		} catch (SocketException e) {
			GenericInterface.getLogger().logLine("Exception "+Utils.getStringMessageException(e));
		} catch (IOException e) {
			GenericInterface.getLogger().logLine("Exception "+Utils.getStringMessageException(e));
		}

		return data;
	}

	public String processMessage(String msgIn) {
		GenericInterface.getLogger().logLine("processMessage");
		try {
			GenericInterface.getLogger().logLine("socket "+socket);
			if (socket != null) {
				in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				String response = "";
				command = msgIn;
				// socket.shutdownInput();
				GenericInterface.getLogger().logLine("envia " + command);
				out.writeShort(command.getBytes().length);
				out.write(command.getBytes());
				out.flush();

				try {
					byte[] fr = new byte[255];
					in.read(fr);
					response = new String(fr);
					GenericInterface.getLogger().logLine("salida hsm " + response);
					
				} catch (SocketException e) {
					GenericInterface.getLogger().logLine("Error en la conexion con el socket");
				} catch (Exception e) {
					GenericInterface.getLogger().logLine("Exception "+Utils.getStringMessageException(e));
					e.printStackTrace();
				}
				GenericInterface.getLogger().logLine("response "+response);
				return response;
			}
		} catch (Exception e) {
			GenericInterface.getLogger().logLine("Exception "+Utils.getStringMessageException(e));
		}
		return errorHsm; // No hay Conexion con la HSM
	}

	public void closeConnectHSM() {
		try {
			socket.close();
			if (out != null)
				out.close();
			if (in != null)
				in.close();
			socket = null;
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public void openConnectHSM(String ipAddress, int port) {
		// hsm OFF
		if (ipAddress.equals("-1") && port == -1) {
		} else {
			if (ipAddress.equals("0") && port == 0) {
				ipAddress = HSMKey.ip_hsm;
				port = HSMKey.port_hsm;
			}

			try {
				socket = new Socket(ipAddress, port);
				socket.setSoTimeout(6000);
				System.out.println("<<< Sockt s >>> :" + socket);
			} catch (Exception ex) {
				try {
					socket.close();
				} catch (Exception e) {
				}
				ex.printStackTrace();
			}
		}
	}

	public void initConn() {
		crypto_cfg_man = CryptoManager.getStaticConfiguration();
		try {
			connection = crypto_cfg_man.getConnectionToHsmService();
		} catch (XCryptoCommsFailure e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
