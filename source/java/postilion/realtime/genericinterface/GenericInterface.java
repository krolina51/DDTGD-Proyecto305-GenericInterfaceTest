package postilion.realtime.genericinterface;

import java.io.FileReader;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import postilion.realtime.date.CalendarDTO;
import postilion.realtime.date.CalendarLoader;
import postilion.realtime.genericinterface.channels.Super;
import postilion.realtime.genericinterface.eventrecorder.events.CannotProcessAcqReconRspFromRemote;
import postilion.realtime.genericinterface.eventrecorder.events.IncorrectLengthField_120KeyManagement;
import postilion.realtime.genericinterface.eventrecorder.events.IncorrectRuntimeParameters;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidAddressKey;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidDataField_120KeyManagement;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidDataField_123CryptoServiceMsg;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidLenghtCryptoKeyIdll;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidMacRdbnNtwrk;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidSinkKeyLoaded;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidSourceKeyLoaded;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidTypeKey;
import postilion.realtime.genericinterface.eventrecorder.events.KeyExchangeReqFailed;
import postilion.realtime.genericinterface.eventrecorder.events.MissingField053SecurityInfo;
import postilion.realtime.genericinterface.eventrecorder.events.MissingField_120KeyManagement;
import postilion.realtime.genericinterface.eventrecorder.events.MissingKeyOnField_123CryptoServiceMsg;
import postilion.realtime.genericinterface.eventrecorder.events.SignedOn;
import postilion.realtime.genericinterface.eventrecorder.events.SinkNodeKeyNotConfigured;
import postilion.realtime.genericinterface.eventrecorder.events.SinkNodeKeyReceivedByNonsinkNode;
import postilion.realtime.genericinterface.eventrecorder.events.SourceKeyExchangeInitiated;
import postilion.realtime.genericinterface.eventrecorder.events.SourceKeyGenerationFailed;
import postilion.realtime.genericinterface.eventrecorder.events.SourceNodeKeyNotConfigured;
import postilion.realtime.genericinterface.eventrecorder.events.SourceNodeKeyReceivedByNonsourceNode;
import postilion.realtime.genericinterface.eventrecorder.events.SucessfullSinkKeyLoad;
import postilion.realtime.genericinterface.eventrecorder.events.SucessfullSourceKeyLoad;
import postilion.realtime.genericinterface.eventrecorder.events.TryCatchException;
import postilion.realtime.genericinterface.eventrecorder.events.UnsupportedNmi;
import postilion.realtime.genericinterface.translate.ConstructFieldMessage;
import postilion.realtime.genericinterface.translate.MessageTranslator;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.bitmap.Base24AthCustom;
import postilion.realtime.genericinterface.translate.stream.Header;
import postilion.realtime.genericinterface.translate.util.BussinesRules;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.Constants.KeyExchange;
import postilion.realtime.genericinterface.translate.util.EventReporter;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.genericinterface.translate.validations.Validation;
import postilion.realtime.library.common.db.DBHandler;
import postilion.realtime.library.common.util.Logger;
import postilion.realtime.library.common.util.constants.General;
import postilion.realtime.library.common.util.constants.MsgTypeCsm;
import postilion.realtime.sdk.crypto.CryptoCfgManager;
import postilion.realtime.sdk.crypto.CryptoManager;
import postilion.realtime.sdk.crypto.DesKeyLength;
import postilion.realtime.sdk.crypto.DesKvc;
import postilion.realtime.sdk.crypto.DesKwa;
import postilion.realtime.sdk.crypto.DesKwp;
import postilion.realtime.sdk.crypto.XCrypto;
import postilion.realtime.sdk.eventrecorder.AContext;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.eventrecorder.contexts.ApplicationContext;
import postilion.realtime.sdk.message.IMessage;
import postilion.realtime.sdk.message.bitmap.BitmapMessage;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.Iso8583.MsgType;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.message.bitmap.StructuredData;
import postilion.realtime.sdk.node.AInterchangeDriver8583;
import postilion.realtime.sdk.node.AInterchangeDriverEnvironment;
import postilion.realtime.sdk.node.Action;
import postilion.realtime.sdk.node.ActiveActiveKeySyncMsgHandler;
import postilion.realtime.sdk.node.NodeDriverEnvAdapter;
import postilion.realtime.sdk.node.XNodeParameterValueInvalid;
import postilion.realtime.sdk.util.DateTime;
import postilion.realtime.sdk.util.TimedHashtable;
import postilion.realtime.sdk.util.XPostilion;
import postilion.realtime.sdk.util.convert.Pack;
import postilion.realtime.sdk.util.convert.Transform;
import postilion.realtime.validations.crypto.FactoryCommonRules;

/**
 *
 * Esta clase permite realizar procesar todos los mensajes entrantes de B24 y
 * transformalos a ISO8583Iso y viceversa en base a configuracion en memoria
 *
 * @author Mauricio Rodriguez Bello, Cesar Calderon, Andres Meneses.
 *
 */
public class GenericInterface extends AInterchangeDriver8583 {

	/** This is the key used to authenticate messages. */
	protected DesKwa kwa = null;

	protected long retentionPeriod = 0;

	/**
	 * Contiene los mensajes 0200 que llegan de la Interchange al NodoSource.
	 */
	public TimedHashtable sourceTranToTmHashtable = null;
	public TimedHashtable sourceTranToTmHashtableB24 = null;

	public boolean signed_on = false;
	public boolean encodeData = false;
	public boolean exeptionValidateExpiryDate = false;
	public boolean consultCovenants; // to validate consult of covenants
	public boolean businessValidation, create0220ToTM;
	protected boolean validateCvv = false;

	public String nameInterface = "";
	public String issuerId = null;
	public String urlCutWS = null;
	public String responseCodesVersion = null;
	public String ipCryptoValidation = "10.86.82.119";
	public String routingField100 = "";
	public String ipUdpServer = "0";
	public String portUdpServer = "0";
	public String portUdpClient = "0";
	public String ipServerAT = "0";
	public String portServerAT = "0";
	public String routingFilter = "";
	public String routingCreditPath = "";
	public String routingTransitoriasPath = "";
	public String routingFilterPath = "D:\\Apl\\postilion\\genericinterfacetest";
	public String routingLoadHashMap = "D:\\Apl\\postilion\\genericinterfacetest";
	public String termConsecutiveSection = "";
	public String sSignOn; // 0 - Don't Send SignOn on connection with
							// the remote entity. 1 - Send SignOn
							// on connection with the remote entity
	public static String exceptionMessage = null;

	public long delay = 0;
	public long period = 60_000;

	public int portCryptoValidation = 7000;
	/** Inicia la variable keyExchangeState en inactivo. */
	protected int keyExchangeState = Base24Ath.KeyExchangeState.IDLE;

	public Client udpClient = null;
	public Client udpClientAT = null;

	public Parameters params;

	public CalendarDTO calendarInfo;

	public FactoryCommonRules factory;

	protected DesKvc kvc = null;
	public boolean freeThreaded = false;

	static Header default_header;

	private static Logger logger = new Logger(Constants.Config.URL_LOG);

	public static HashMapBusinessLogic fillMaps = new HashMapBusinessLogic();

	/************************************************************************************
	 * SDK Postilion method implementation, which serves to initialize interchange
	 *
	 * @param interchange interchage del producto
	 * @throws XPostilion
	 * @throws SQLException
	 ************************************************************************************/
	@Override
	public void init(AInterchangeDriverEnvironment interchange) throws XPostilion, SQLException {

//		Thread grpcServer = new Thread(new Runnable() {
//			@Override
//			public void run() {
//						GRPCServer.runServer();
//			}});
//		grpcServer.start();

		getLogger().logLine("#=== Enter to [Init] Method Interchange " + interchange.getName() + " ===#");
		this.calendarInfo = new CalendarDTO();
//		acquirersNetwork = postilion.realtime.genericinterface.translate.database.DBHandler.getAcquirerDesc();

		this.nameInterface = interchange.getName();
		String[] userParams = Pack.splitParams(interchange.getUserParameter());
//		acquirersNetwork = postilion.realtime.genericinterface.translate.database.DBHandler.getAcquirerDesc();
		this.nameInterface = interchange.getName();
		String[] logParms = { nameInterface, Integer.toString(Constants.RuntimeParm.NR_OF_PARAMETERS_EXPECTED),
				Integer.toString(userParams.length), interchange.getUserParameter() };

		if (userParams.length != Constants.RuntimeParm.NR_OF_PARAMETERS_EXPECTED) {
			EventRecorder.recordEvent(new IncorrectRuntimeParameters(new String[] { interchange.getName(),
					Integer.toString(Constants.RuntimeParm.NR_OF_PARAMETERS_EXPECTED),
					Integer.toString(userParams.length), interchange.getUserParameter() }));
			throw new XPostilion(new IncorrectRuntimeParameters(
					new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
		}

		getParameters(userParams[0]);

		Timer timer = new Timer();
		TimerTask task = new CalendarLoader(this.calendarInfo, this.nameInterface);
		timer.schedule(task, this.delay, this.period);

		udpClient = new Client(ipUdpServer, portUdpServer, portUdpClient, nameInterface);

		fillMaps.loadHashMap(routingLoadHashMap, this.nameInterface, this.udpClient);

		switch (routingFilter) {
		case "Test2":
		case "Prod2":
			fillMaps.fillFilters2(routingFilterPath, this.nameInterface, this.udpClient);
			break;
		default:
			fillMaps.fillFilters(routingFilterPath, this.nameInterface, this.udpClient);
			break;
		}

		fillMaps.setAllCodesIsoToB24(DBHandler.getResponseCodes(false, "0", responseCodesVersion));
		if (fillMaps.getAllCodesIsoToB24().size() == 0) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					GenericInterface.class.getName(), "Method: [init]",
					"Error to load the database configuration. The interface not working without the messages configuration. Map allCodesIsoToB24",
					"N/A" }));
			throw new XPostilion(
					new TryCatchException(new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
		}

		fillMaps.setAllCodesIscToIso(DBHandler.getResponseCodes(true, "0", responseCodesVersion));
		if (fillMaps.getAllCodesIscToIso().size() == 0) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					GenericInterface.class.getName(), "Method: [init]",
					"Error to load the database configuration. The interface not working without the messages configuration. Map allCodesIscToIso",
					"N/A" }));
			throw new XPostilion(
					new TryCatchException(new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
		}

		fillMaps.setAllCodesIsoToB24TM(DBHandler.getResponseCodes(false, "1", responseCodesVersion));
		if (fillMaps.getAllCodesIsoToB24TM().size() == 0) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					GenericInterface.class.getName(), "Method: [init]",
					"Error to load the database configuration. The interface not working without the messages configuration. Map allCodesIsoToB24TM",
					"N/A" }));
			throw new XPostilion(
					new TryCatchException(new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
		}

		fillMaps.setAllCodesB24ToIso(DBHandler.getResponseCodesBase24("1", responseCodesVersion));
		if (fillMaps.getAllCodesB24ToIso().size() == 0) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					GenericInterface.class.getName(), "Method: [init]",
					"Error to load the database configuration. The interface not working without the messages configuration. Map allCodesB24ToIso",
					"N/A" }));
			throw new XPostilion(
					new TryCatchException(new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
		}

		// udpClient = new Client(ipUdpServer, portUdpServer);
		params = new Parameters(kwa, sourceTranToTmHashtable, sourceTranToTmHashtableB24, issuerId, udpClient,
				nameInterface, ipCryptoValidation, portCryptoValidation, fillMaps.getKeys(), routingField100,
				fillMaps.getAllCodesIsoToB24(), fillMaps.getAllCodesIscToIso(), fillMaps.getAllCodesIsoToB24TM(),
				fillMaps.getAllCodesB24ToIso(), this.calendarInfo, this.termConsecutiveSection,
				this.responseCodesVersion);
		factory = new FactoryCommonRules(params);

	}

	/**
	 * Inicializacion de variables segun datos que llegan en los parametros
	 */
	@SuppressWarnings("unchecked")
	public void getParameters(String path) {

		try {

			JSONParser parser = new JSONParser();
			org.json.simple.JSONObject jsonObjects = (org.json.simple.JSONObject) parser.parse(new FileReader(path));
			org.json.simple.JSONObject parameters = (JSONObject) jsonObjects.get(this.nameInterface);
			if (parameters != null) {
				String cfgRetentionPeriod = parameters.get("cfgRetentionPeriod").toString();
				String cfgValidateMAC = parameters.get("cfgValidateMAC").toString();
				String cfgKwaName = parameters.get("cfgKwaName").toString();
				sSignOn = parameters.get("sSignOn").toString();
				responseCodesVersion = parameters.get("responseCodesVersion").toString();
				issuerId = parameters.get("issuerId").toString();
				String cfgIpUdpServer = parameters.get("cfgIpUdpServer").toString();
				String cfgPortUdpServer = parameters.get("cfgPortUdpServer").toString();
				String cfgPortUdpClient = parameters.get("cfgPortUdpClient").toString();
				boolean create0220ToTM = (boolean) parameters.get("create0220ToTM");
				String cfgIpCryptoValidation = parameters.get("ipCryptoValidation").toString();
				String cfgPortCryptoValidation = parameters.get("portCryptoValidation").toString();
				JSONArray channelsIds = (JSONArray) parameters.get("channelIds");
				this.routingFilter = parameters.get("routing_filter").toString();
				this.routingFilterPath = parameters.get("routing_filter_path").toString();
				this.routingLoadHashMap = parameters.get("routing_load_hashMap").toString();
				this.routingField100 = parameters.get("routing_field_100").toString();
				businessValidation = (boolean) parameters.get("bussinessValidation");
				try {
					this.delay = (long) parameters.get("delay_timer");
					this.period = (long) parameters.get("period_timer");
					this.calendarInfo.setThreshold((long) parameters.get("threshold_timer"));
				} catch (ClassCastException e) { // PARA MEJORAR
					EventRecorder.recordEvent(new XNodeParameterValueInvalid("Delay, period, or threshold timer ",
							"Incorrect data type"));
					throw new XNodeParameterValueInvalid("Delay, period, or threshold timer", "Incorrect data type");
				}
				if (cfgRetentionPeriod != null) {
					try {
						retentionPeriod = Long.parseLong(cfgRetentionPeriod);
					} catch (NumberFormatException e) { // PARA MEJORAR
						EventRecorder.recordEvent(new XNodeParameterValueInvalid(Constants.RuntimeParm.RETENTION_PERIOD,
								cfgRetentionPeriod));
						throw new XNodeParameterValueInvalid(Constants.RuntimeParm.RETENTION_PERIOD,
								cfgRetentionPeriod);

					}
					sourceTranToTmHashtable = new TimedHashtable(retentionPeriod);
					sourceTranToTmHashtableB24 = new TimedHashtable(retentionPeriod);
				} else {
					EventRecorder.recordEvent(
							new XNodeParameterValueInvalid(Constants.RuntimeParm.RETENTION_PERIOD, General.NULLSTRING));
					throw new XNodeParameterValueInvalid(Constants.RuntimeParm.RETENTION_PERIOD, General.NULLSTRING);
				}

				if (cfgValidateMAC != null) {
					boolean validateMac = cfgValidateMAC.equals(General.TRUE);
					if (cfgKwaName != null) {
						if (validateMac) {
							try {
								CryptoCfgManager crypcfgman = CryptoManager.getStaticConfiguration();
								kwa = crypcfgman.getKwa(cfgKwaName);
							} catch (XCrypto e) { // PARA MEJORAR
								EventRecorder.recordEvent(
										new XNodeParameterValueInvalid(Constants.RuntimeParm.KWA_NAME, cfgKwaName));
								throw new XNodeParameterValueInvalid(Constants.RuntimeParm.KWA_NAME, cfgKwaName);
							}
						}
					} else {
						EventRecorder.recordEvent(
								new XNodeParameterValueInvalid(Constants.RuntimeParm.KWA_NAME, General.NULLSTRING));
						throw new XNodeParameterValueInvalid(Constants.RuntimeParm.KWA_NAME, General.NULLSTRING);
					}
				} else {
					EventRecorder.recordEvent(
							new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_MAC, General.NULLSTRING));
					throw new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_MAC, General.NULLSTRING);
				}

				this.ipUdpServer = BussinesRules.validateIpUdpServerParameter(cfgIpUdpServer);
				this.portUdpServer = BussinesRules.validatePortUdpServerParameter(cfgPortUdpServer);
				this.portUdpClient = BussinesRules.validatePortUdpServerParameter(cfgPortUdpClient);
				this.create0220ToTM = create0220ToTM;
				this.ipCryptoValidation = BussinesRules.validateIpUdpServerParameter(cfgIpCryptoValidation);
				this.portCryptoValidation = Integer
						.valueOf(BussinesRules.validatePortUdpServerParameter(cfgPortCryptoValidation));
				this.termConsecutiveSection = parameters.get("terminal_consecutive_section").toString();
				this.freeThreaded = (boolean) parameters.get("FREE_THREADED");

				if (channelsIds.size() != 0) {
					CryptoCfgManager crypcfgman = CryptoManager.getStaticConfiguration();
					fillMaps.putKeys("VBK", crypcfgman.getKwa(this.nameInterface + "_VBK"));
					channelsIds.stream().forEach(s -> {
						try {
							GenericInterface.getLogger()
									.logLine("Looking pbk " + this.nameInterface + "_" + s.toString() + "_PBK");
							fillMaps.putKeys(s.toString(),
									crypcfgman.getKwa(this.nameInterface + "_" + s.toString() + "_PBK"));
						} catch (XCrypto e) { // PARA MEJORAR
							GenericInterface.getLogger().logLine(Utils.getStringMessageException(e));
							EventRecorder.recordEvent(new XNodeParameterValueInvalid(
									this.nameInterface + "_" + s.toString() + "_PBK", "Not present"));
						}
					});
				}
			} else {
				EventRecorder.recordEvent(new XNodeParameterValueInvalid(
						"Parameters for interchange" + this.nameInterface, General.NULLSTRING));
				throw new XNodeParameterValueInvalid("Parameters for interchange" + this.nameInterface,
						General.NULLSTRING);
			}
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"getParameters", this.udpClient);
		}

	}
	
	

	@Override
	public boolean isFreeThreaded() {
		return this.freeThreaded;
	}

	/************************************************************************************
	 * This method is invoked by the Node Interface whenever a RESYNC command is
	 * received.
	 *
	 * @param interchange
	 * @return action a ejecutar con el comando
	 * @throws Exception
	 ************************************************************************************/
	@Override
	public Action processResyncCommand(AInterchangeDriverEnvironment interchange) throws Exception {
		Action action = new Action();
		try {
			kwa = null;
			init(interchange);
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"processResyncCommand", this.udpClient);
		}
		return action;
	}

	/**************************************************************************************
	 * This method returns an IMessage object constructed from the data received
	 * from the interchange.
	 *
	 * @param data
	 * @throws Exception
	 *************************************************************************************/
	@Override
	public IMessage newMsg(byte[] data) throws Exception {
		try {

			IMessage msg = null;

			udpClient.sendData(Client.getMsgKeyValue("N/A",
					"**Recibiendo nuevo mensaje de respuesta**\n" + Transform.getString(data, Transform.Encoding.ASCII),
					"LOG", nameInterface));

//			long tStart = System.currentTimeMillis();
			
			//borrar****
			String dataAux = new String (data);
			getLogger().logLine("data impresion " + dataAux);
			String rawMsgType = dataAux.substring(12, 16);					
			////******
			String msgType = new String(data, 0, 3);
			BitmapMessage inMsg = null;
			if (msgType.equals("ISO")) {
				if (rawMsgType.equals("0210") && dataAux.contains("333000")) {
					getLogger().logLine("Data Respuesta " + dataAux);
					inMsg = new Base24Ath(kwa, 1);
					inMsg.fromMsg(data);
				} else {
					getLogger().logLine("Data Solicitud " + dataAux);
					inMsg = new Base24Ath(kwa);
					inMsg.fromMsg(data);
				}		
//				inMsg = new Base24Ath(kwa);
//				inMsg.fromMsg(data);
				msg = inMsg;
			} else {
				getLogger().logLine("Respuesta else " + dataAux);
				inMsg = new Iso8583Post();
				inMsg.fromMsg(data);
				msg = inMsg;
			}
			getLogger().logLine("Respuesta despues del else " + dataAux);
			getLogger().logLine("**MENSAJE**\n" + msg);
			exceptionMessage = Transform.fromBinToHex(Transform.getString(data));
			return msg;

		} catch (Exception e) {
			GenericInterface.getLogger().logLine(Utils.getStringMessageException(e));
			exceptionMessage = Transform.fromBinToHex(Transform.getString(data));
			udpClient.sendData(Client.getMsgKeyValue("N/A", "ERRISO30 Exception en Mensaje: " + exceptionMessage, "ERR",
					nameInterface));

			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D", "newMsg",
					this.udpClient);

		}
		return null;
	}

	static {
		default_header = new Header();
		default_header.putField(Header.Field.ISO_LITERAL, Header.Iso.ISO);
		default_header.putField(Header.Field.PRODUCT_INDICATOR, Header.ProductIndicator.POS);
		default_header.putField(Header.Field.STATUS, Header.Status.OK);
		default_header.putField(Header.Field.ORIGINATOR_CODE, Header.OriginatorCode.CINCO);
		default_header.putField(Header.Field.RESPONDER_CODE, Header.ResponderCode.CERO);
	}

	/**
	 * Method to call with remote disconnects from interface Sets the signed_on flag
	 * to false
	 * 
	 * @param interchange The interchange involved
	 * @return An empty Action
	 * @throws Exception
	 * @see postilion.realtime.sdk.node.AInterchangeDriver#processInterchangeDisconnected(postilion.realtime.sdk.node.AInterchangeDriverEnvironment)
	 */
	@Override
	public Action processInterchangeDisconnected(AInterchangeDriverEnvironment interchange) throws Exception {
		this.signed_on = false;
		return new Action();
	}

	/**
	 * Clase para reportardo el error en el mensaje 0800.
	 * 
	 * @param eventId     nÃºmero identificador del evento a reportar
	 * @param interchange Interchange que reporta el error.
	 * @param nodeKey     Llave de nodo.
	 */
	public void reportEvent(int eventId, AInterchangeDriverEnvironment interchange, DesKwp nodeKey) {
		switch (eventId) {
		case EventId.MISSING_FIELD053_SECURITY_INFO:
			EventRecorder.recordEvent(new MissingField053SecurityInfo(new String[] { interchange.getName() }));
			break;

		case EventId.MISSING_KEY_ON_FIELD_123_CRYPTO_SERVICE_MSG:
			EventRecorder
					.recordEvent(new MissingKeyOnField_123CryptoServiceMsg(new String[] { interchange.getName() }));
			break;

		case EventId.INVALID_DATA_FIELD_123_CRYPTO_SERVICE_MSG:
			EventRecorder.recordEvent(new InvalidDataField_123CryptoServiceMsg(new String[] { nodeKey.getName() }));

			break;

		case EventId.INVALID_SOURCE_KEY_LOADED:
			EventRecorder.recordEvent(new InvalidSourceKeyLoaded(new String[] { interchange.getName() }));
			break;

		case EventId.MISSING_FIELD_120_KEY_MANAGEMENT:

			EventRecorder.recordEvent(new MissingField_120KeyManagement(new String[] { nodeKey.getName() }));
			break;

		case EventId.INCORRECT_LENGTH_FIELD_120_KEY_MANAGEMENT:
			EventRecorder
					.recordEvent(new IncorrectLengthField_120KeyManagement(new String[] { interchange.getName() }));
			break;

		case EventId.INVALID_DATA_FIELD_120_KEY_MANAGEMENT:
			EventRecorder.recordEvent(new InvalidDataField_120KeyManagement(new String[] { interchange.getName() }));
			break;

		case EventId.SUCESSFULL_SOURCE_KEY_LOAD:
			EventRecorder.recordEvent(new SucessfullSourceKeyLoad(new String[] { interchange.getName() }));
			break;

		case EventId.INVALID_SINK_KEY_LOADED:
			EventRecorder.recordEvent(new InvalidSinkKeyLoaded(new String[] { interchange.getName() }));
			break;

		case EventId.SUCESSFULL_SINK_KEY_LOAD:
			EventRecorder.recordEvent(new SucessfullSinkKeyLoad(new String[] { interchange.getName() }));
			break;

		case EventId.SOURCE_NODE_KEY_RECEIVED_BY_NONSOURCE_NODE:
			EventRecorder.recordEvent(new SourceNodeKeyReceivedByNonsourceNode(new String[] { interchange.getName() }));
			break;

		case EventId.SINK_NODE_KEY_RECEIVED_BY_NONSINK_NODE:
			EventRecorder.recordEvent(new SinkNodeKeyReceivedByNonsinkNode(new String[] { interchange.getName() }));
			break;

		case EventId.INVALID_ADDRESS_KEY:
			EventRecorder.recordEvent(new InvalidAddressKey(new String[] { interchange.getName() }));
			break;

		case EventId.SINK_NODE_KEY_NOT_CONFIGURED:
			EventRecorder.recordEvent(new SinkNodeKeyNotConfigured(new String[] { interchange.getName() }));
			break;

		case EventId.SOURCE_NODE_KEY_NOT_CONFIGURED:
			EventRecorder.recordEvent(new SourceNodeKeyNotConfigured(new String[] { interchange.getName() }));
			break;

		default:
			EventRecorder.recordEvent(new CannotProcessAcqReconRspFromRemote(new String[] { interchange.getName() }));
			break;

		}

	}

	/**
	 * Carga la llave del nodo source.
	 * 
	 * @param keyUnderParent Llave cifrada con la llave padre.
	 * @param checkDigits    DÃ­gitos de chequeo.
	 * @param interchange    InfomraciÃ³n de la interchange en Postilion.
	 * @return True si la carga fue exitosa.
	 */
	public final boolean loadSourceNodeKey(String keyUnderParent, String checkDigits,
			AInterchangeDriverEnvironment interchange) {
		try {
			DesKwp sourceNodeKwp = interchange.getSourceNodeKwp();
			sourceNodeKwp.loadEncrypted(keyUnderParent);

			String newCheckDigits = sourceNodeKwp.getCheckDigits();

			if (checkDigits.length() > 4) {
				checkDigits = checkDigits.substring(0, 4);
			}
			if (newCheckDigits.length() > 4) {
				newCheckDigits = newCheckDigits.substring(0, 4);
			}
			if (checkDigits.length() > 4) {
				checkDigits = checkDigits.substring(0, 4);
			}
			if (newCheckDigits.length() > 4) {
				newCheckDigits = newCheckDigits.substring(0, 4);
			}
			if (newCheckDigits.length() <= checkDigits.length()) {
				return checkDigits.startsWith(newCheckDigits);
			} else {
				return newCheckDigits.startsWith(checkDigits);
			}
		} catch (RuntimeException e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"loadSourceNodeKey", this.udpClient,
					e.getMessage() + "\n" + e.getLocalizedMessage() + "\n" + " of type RuntimeException");
			throw e;
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"loadSourceNodeKey", this.udpClient, "of type Exception");
			return false;
		}
	}

	/************************************************************************************
	 * Processes a Transaction Request (0200/0201) from a remote interchange.
	 * Drivers capable of handling this message should implement this method.
	 * Otherwise, null should be returned.
	 *
	 * @param interchange
	 * @param msg
	 * @return action a ejecutar con el comando
	 * @throws XPostilion
	 ************************************************************************************/
	@Override
	public Action processTranReqFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws XPostilion {
		Action action = new Action();
		String retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);

		if (msg instanceof Iso8583Post) {

			msg.putField(Iso8583Post.Bit._052_PIN_DATA, Transform.fromHexToBin("FB8BDD3488A84D1D"));

			StructuredData sd = new StructuredData();
			sd.put("CARD_NUMBER", "4573210000096242");
			sd.put("B24_Field_41", "0054232100001   ");
			sd.put("CUSTOMER_NAME", "TERESA MIGRACION VALOIS");
			sd.put("P_CODE", "012000");
			sd.put("CLASE", "EE");
			sd.put("CUSTOMER_ID", "PC00000000000000000123457");
			sd.put("B24_Field_54", "000001000000000000000000000000000000");
			sd.put("B24_Field_48", "00520000000030000000000000000000000000000000");

			((Iso8583Post) msg).putStructuredData(sd);

			((Iso8583Post) msg).putPrivField(Iso8583Post.PrivBit._002_SWITCH_KEY,
					new ConstructFieldMessage(params).constructSwitchKey(msg, interchange.getName()));

			action.putMsgToTranmgr((Iso8583Post) msg);
		} else if (msg instanceof Base24Ath) {

			Base24Ath msgFromRemote = (Base24Ath) msg;
			Base24Ath msgToRemote = new Base24Ath(kwa);

			putRecordIntoSourceToTmHashtableB24(retRefNumber, msgFromRemote);

			udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					Transform.fromBinToHex(Transform.getString(msgFromRemote.getBinaryData())), "B24", nameInterface));

//			Base24Ath msgFromRemoteT = new Base24Ath(kwa);
			Iso8583Post msgToTm = new Iso8583Post();
//			Base24Ath msgToRemote = new Base24Ath(kwa);
			MessageTranslator translator = new MessageTranslator(params);

//			Utils tool = new Utils(params);
			try {

				// Validacion MAC
				int errMac = msgFromRemote.failedMAC();
				if (errMac == Base24Ath.MACError.INVALID_MAC_ERROR) {
					action = new Action(null, constructEchoMsgIndicatorFailedMAC(msgFromRemote, errMac), null, null);
				} else {

					if (!msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)) {
						msg.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID, Constants.General.DEFAULT_P41);
					}

					// Validacion Enrutamiento Interfaz2 o Autra
					int routingto = 0;

					switch (routingFilter) {
					case "Test1":
					case "Prod1":
					case "Capa":

						routingto = ValidateAutra.getRouting(msgFromRemote, udpClient, nameInterface, routingFilter);

						break;
					case "Autra":

						routingto = Constants.TransactionRouting.INT_AUTRA;

						break;

					default:

						break;

					}

					switch (routingto) {
					case Constants.TransactionRouting.INT_CAPA_DE_INTEGRACION:

						Super objectValidations = new Super(true, General.VOIDSTRING, General.VOIDSTRING,
								General.VOIDSTRING, new HashMap<String, String>(), params) {

							@Override
							public void validations(Base24Ath msg, Super objectValidations) {

							}
						};

						// Iso8583Post Isomsg = translator.constructIso8583(msgFromRemote,
						// objectValidations.getInforCollectedForStructData());

						Iso8583Post Isomsg = translator.constructIso8583(msgFromRemote, objectValidations);

						if (!objectValidations.getValidationResult()) {
							udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
									"Error de formato", "LOG", nameInterface));
							action.putMsgToTranmgr(translator.construct0220ToTm(msg, interchange.getName()));
							msgToRemote = translator.constructBase24(msgFromRemote, objectValidations);
							udpClient.sendData(
									Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
											Transform.fromBinToHex(Transform.getString(msgToRemote.toMsg(false))),
											"B24", nameInterface));
							action.putMsgToRemote(msgToRemote);
						} else {

							action.putMsgToTranmgr(Isomsg);
							putRecordIntoSourceToTmHashtable(retRefNumber, Isomsg);

						}

						break;

					default:
						Super objectSuper = new Super(true, General.VOIDSTRING, General.VOIDSTRING, General.VOIDSTRING,
								null, params) {

							@Override
							public void validations(Base24Ath msg, Super objectValidations) {

							}
						};

						objectSuper.constructAutraMessage(msgFromRemote, msgToTm);

						if (msgFromRemote.getField(Iso8583.Bit._003_PROCESSING_CODE).equals("910000")) {
							msgToTm.putMsgType(Iso8583.MsgType._0600_ADMIN_REQ);
						}

						action.putMsgToTranmgr(msgToTm);

						putRecordIntoSourceToTmHashtable(
								retRefNumber + msgToTm.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR), msgToTm);
						putRecordIntoSourceToTmHashtableB24(
								retRefNumber + msgToTm.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR),
								msgFromRemote);

						break;
					}
				}
			} catch (Exception e) {

				// Iso8583Post msg220=translator.construct0220ToTm(msg, nameInterface);
				// action.putMsgToTranmgr( );
				e.printStackTrace();
				EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, retRefNumber,
						"processTranReqFromInterchange", this.udpClient);
				udpClient.sendData(Client.getMsgKeyValue(retRefNumber, "Exception in message: " + exceptionMessage,
						"ERR", nameInterface));

			}
		}
		return action;
	}

	/**************************************************************************************
	 * Processes a Transaction Request Response (0210) from a remote interchange.
	 * Drivers capable of handling this message should implement this method.
	 * Otherwise, null should be returned.
	 *
	 * @param interchange
	 * @param msg
	 *************************************************************************************/
	@Override
	public Action processTranReqRspFromTranmgr(AInterchangeDriverEnvironment interchange, Iso8583Post msg)
			throws Exception {
		Action action = new Action();
		String retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);

		udpClient.sendData(Client.getMsgKeyValue(retRefNumber, "LA RESPUESTA 0210 TRAE ESTOS VALORES EN EL 102:"
				+ msg.getField("102") + " y estos en el 103: " + msg.getField("103"), "LOG", nameInterface));

		udpClient.sendData(Client.getMsgKeyValue(retRefNumber, Transform.fromBinToHex(Transform.getString(msg.toMsg())),
				"ISO", nameInterface));

		if (!msg.getField(Iso8583.Bit._039_RSP_CODE).equals("00"))
			udpClient.sendData(Client.getMsgKeyValue(retRefNumber, "DECISO" + msg.getField(Iso8583.Bit._039_RSP_CODE)
					+ Transform.fromBinToHex(Transform.getString(msg.toMsg())), "ISO", nameInterface));

		try {
			if (msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA)
					&& msg.getStructuredData().get("B24_MessageRsp") != null) {
				String sdData = msg.getStructuredData().get("B24_MessageRsp");
				byte[] decodedBytes = Base64.getDecoder().decode(sdData);
				String decodedString = new String(decodedBytes, Charset.forName("US-ASCII"));
				Base24Ath msgDecoded = new Base24Ath(null);

				msgDecoded.fromMsg(decodedString);

				GenericInterface.getLogger().logLine("Respuesta 0210 Desencapsulada:" + msgDecoded.toString());
				udpClient.sendData(Client.getMsgKeyValue(msgDecoded.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						Transform.fromBinToHex(Transform.getString(msgDecoded.getBinaryData())), "B24", nameInterface));
				if (!nameInterface.toLowerCase().startsWith("credibanco"))
					action.putMsgToRemote(msgDecoded);

			} else {
				MessageTranslator translator = new MessageTranslator(params);
				Base24Ath msgToRemote2 = translator.constructBase24((Iso8583Post) msg);
				udpClient.sendData(Client.getMsgKeyValue(msgToRemote2.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						Transform.fromBinToHex(Transform.getString(msgToRemote2.toMsg(false))), "B24", nameInterface));
				GenericInterface.getLogger().logLine("210CONSTRUCTISO8583:" + msgToRemote2);
				action.putMsgToRemote(msgToRemote2);
			}

		} catch (Exception e) {
			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30" + Transform.fromBinToHex(Transform.getString(msg.toMsg())), "ERR", nameInterface));
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, retRefNumber,
					"processTranReqRspFromTranmgr", this.udpClient);
		}
		return action;
	}

	@Override

	public Action processTranReqFromTranmgr(AInterchangeDriverEnvironment interchange, Iso8583Post msg)
			throws Exception {

		putRecordIntoSourceToTmHashtable(
				msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) + msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR),
				msg);
		Action action = new Action();
		if (msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA)
				&& msg.getStructuredData().get("B24_Message") != null) {
			String sdData = msg.getStructuredData().get("B24_Message");
			byte[] decodedBytes = Base64.getDecoder().decode(sdData);
			String decodedString = new String(decodedBytes);
			GenericInterface.getLogger().logLine("B24_Message: " + decodedString);
			Base24Ath msgDecoded = new Base24Ath(null);
			msgDecoded.fromMsg(decodedString);
			action.putMsgToRemote(msgDecoded);
		} else {
			msg.clearField(28);
			msg.clearField(30);
			MessageTranslator translator = new MessageTranslator(params);
			Base24Ath msgToRemote = translator.constructBase24Request((Iso8583Post) msg);
			action.putMsgToRemote(msgToRemote);
		}
		return action;
	}

	@Override
	public Action processAcquirerRevAdvFromTranmgr(AInterchangeDriverEnvironment interchange, Iso8583Post msg)
			throws Exception {
		GenericInterface.getLogger().logLine("Entro processAcquirerRevAdvFromTranmgr");
		GenericInterface.getLogger().logLine("Iso8583Post Rev: " + msg.toString());
		String keyMsg = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
		Action action = new Action();
		if (msg.isPrivFieldSet(Iso8583Post.PrivBit._006_AUTH_PROFILE)) {
			switch (msg.getPrivField(Iso8583Post.PrivBit._006_AUTH_PROFILE)) {
			case "30":
				if (msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA)
						&& msg.getStructuredData().get("B24_Message") != null) {
					action.putMsgToRemote(constructRevAdvToRemote(msg));
				} else {
					MessageTranslator translator = new MessageTranslator(params);
					Base24Ath msgToRemote = translator.constructBase24Request((Iso8583Post) msg);

					putRecordIntoSourceToTmHashtable(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)
							+ msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR), msg);
					action.putMsgToRemote(msgToRemote);
				}
				return action;
			default:
				break;
			}
		}

		String sdData = msg.getStructuredData().get("B24_Message");
		putRecordIntoSourceToTmHashtable(keyMsg, msg);
		if (sdData != null) {
			byte[] decodedBytes = Base64.getDecoder().decode(sdData);
			String decodedString = new String(decodedBytes);
			GenericInterface.getLogger().logLine("B24_Message: " + decodedString);
			Base24Ath msgDecoded = new Base24Ath(null);
			msgDecoded.fromMsg(decodedString);
			action.putMsgToRemote(msgDecoded);
		}
		return action;
	}

	public Base24Ath constructRevAdvToRemote(Iso8583Post msg) throws XPostilion {
		Base24Ath msg0200ToRev = new Base24Ath(null);
		String retRefNumber = "N/D";
		try {
			retRefNumber = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			String sdData0200 = msg.getStructuredData().get("B24_Message");
			byte[] decodedBytes = Base64.getDecoder().decode(sdData0200);
			String decodedString = new String(decodedBytes);

			String keyMsg = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR);
			putRecordIntoSourceToTmHashtable(keyMsg, msg);

			String keyMsgFromRemote210 = msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)
					+ msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR);
			GenericInterface.getLogger().logLine("keyMsgFromRemote210: " + keyMsgFromRemote210);
			Iso8583Post msgIsoPost210 = (Iso8583Post) sourceTranToTmHashtable.get(keyMsgFromRemote210);

			GenericInterface.getLogger().logLine("msgIsoPost210: " + msgIsoPost210.toString());

			msg0200ToRev.fromMsg(decodedString);
			msg0200ToRev.putMsgType(Iso8583.MsgType._0420_ACQUIRER_REV_ADV);

			msg0200ToRev.putField(Iso8583.Bit._038_AUTH_ID_RSP, msgIsoPost210.getField(Iso8583.Bit._038_AUTH_ID_RSP));

			msg0200ToRev.putField(Iso8583.Bit._090_ORIGINAL_DATA_ELEMENTS,
					new ConstructFieldMessage(params).constructField90AutraRevResponse(msgIsoPost210, msg));

			msg0200ToRev.putField(Iso8583.Bit._039_RSP_CODE, msg.getField(Iso8583.Bit._039_RSP_CODE));

			if (msgIsoPost210.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1))
				msg0200ToRev.putField(Iso8583.Bit._102_ACCOUNT_ID_1,
						msgIsoPost210.getField(Iso8583.Bit._102_ACCOUNT_ID_1));
			if (msgIsoPost210.isFieldSet(Iso8583.Bit._103_ACCOUNT_ID_2))
				msg0200ToRev.putField(Iso8583.Bit._103_ACCOUNT_ID_2,
						msgIsoPost210.getField(Iso8583.Bit._103_ACCOUNT_ID_2));

			msg0200ToRev.clearField(Iso8583.Bit._052_PIN_DATA);
			msg0200ToRev.clearField(105);
			msg0200ToRev.clearField(112);
			msg0200ToRev.clearField(126);

			GenericInterface.getLogger().logLine("Base24Ath Rev: " + msg0200ToRev.toString());
		} catch (XPostilion e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, retRefNumber,
					"constructRevAdvToRemote", this.udpClient);

		}
		return msg0200ToRev;
	}

	@Override
	public Action processTranAdvFromTranmgr(AInterchangeDriverEnvironment interchange, Iso8583Post msg)
			throws Exception {
		MessageTranslator translator = new MessageTranslator(params);
		Base24Ath msgToRemote = translator.constructBase24Request((Iso8583Post) msg);

		putRecordIntoSourceToTmHashtable(
				msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) + msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR),
				msg);
		Action action = new Action();

		action.putMsgToRemote(msgToRemote);
		return action;
	}

	@Override
	public Action processTranAdvRspFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msgFromRemote)
			throws Exception {
		Iso8583Post originalMsg = (Iso8583Post) sourceTranToTmHashtable
				.get(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)
						+ msgFromRemote.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR));

		originalMsg.putMsgType(msgFromRemote.getMsgType());
		originalMsg.putField(Iso8583.Bit._038_AUTH_ID_RSP, msgFromRemote.getField((Iso8583.Bit._038_AUTH_ID_RSP)));
		originalMsg.putField(Iso8583.Bit._039_RSP_CODE, msgFromRemote.getField((Iso8583.Bit._039_RSP_CODE)));

		Action action = new Action();
		action.putMsgToTranmgr(originalMsg);

		return action;
	}

	@Override
	public Action processTranReqRspFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msgFromRemote)
			throws Exception {

		Action action = new Action();
		Iso8583Post originalMsg = (Iso8583Post) sourceTranToTmHashtable
				.get(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)
						+ msgFromRemote.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR));

		if (originalMsg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA)
				&& originalMsg.getStructuredData().get("B24_Message") != null) {
			Base24Ath msg = (Base24Ath) msgFromRemote;

			Super objectSuper = new Super(true, General.VOIDSTRING, General.VOIDSTRING, General.VOIDSTRING, null,
					params) {

				@Override
				public void validations(Base24Ath msg, Super objectValidations) {

				}
			};

			objectSuper.constructAutraResponseMessage(msg, originalMsg);

			if (msg.getField(Iso8583.Bit._003_PROCESSING_CODE).equals("910000")) {
				originalMsg.putMsgType(Iso8583.MsgType._0610_ADMIN_REQ_RSP);
			}
			action.putMsgToTranmgr(originalMsg);
		} else {

			originalMsg.putMsgType(msgFromRemote.getMsgType());
			originalMsg.putField(Iso8583.Bit._038_AUTH_ID_RSP, msgFromRemote.getField((Iso8583.Bit._038_AUTH_ID_RSP)));
			originalMsg.putField(Iso8583.Bit._039_RSP_CODE, msgFromRemote.getField((Iso8583.Bit._039_RSP_CODE)));
			action.putMsgToTranmgr(originalMsg);

		}
		sourceTranToTmHashtable.remove(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)
				+ msgFromRemote.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR));
		return action;
	}

	/**************************************************************************************
	 * Processes a Transaction Advice Response (0230) from Transaction Manager.
	 * Drivers capable of handling this message should implement this method.
	 * Otherwise, null should be returned.
	 *************************************************************************************/
	@Override
	public Action processTranAdvRspFromTranmgr(AInterchangeDriverEnvironment interchange, Iso8583Post msg)
			throws Exception {
		Action action = new Action();
		try {
			MessageTranslator translator = new MessageTranslator(params);
			Base24Ath originalMsg = (Base24Ath) sourceTranToTmHashtableB24
					.get(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR));

			getLogger().logLine("processTranAdvRspFromTranmgr : 0");
			if (originalMsg != null) {
				if (originalMsg.getMessageType().equals(Iso8583.MsgType.toString(MsgType._0220_TRAN_ADV))) {

					getLogger().logLine("processTranAdvRspFromTranmgr: 1  ");

					Base24Ath msgToRemote2 = translator.constructBase24(msg);

					getLogger().logLine("processTranAdvRspFromTranmgr : 2" + msgToRemote2);

					action.putMsgToRemote(msgToRemote2);
				}
//				else if(originalMsg.getMessageType().equals(Iso8583.MsgType.toString(MsgType._0200_TRAN_REQ)))
//				{
//					originalMsg.putMsgType(MsgType._0210_TRAN_REQ_RSP);
//					originalMsg.putField(Iso8583.Bit._039_RSP_CODE, "06");
//					originalMsg.putField(Iso8583.Bit._038_AUTH_ID_RSP, "000000");
//					
//					action.putMsgToRemote(originalMsg);
//				}
				else if (originalMsg.getMessageType()
						.equals(Iso8583.MsgType.toString(MsgType._0420_ACQUIRER_REV_ADV))) {
					originalMsg.putMsgType(MsgType._0430_ACQUIRER_REV_ADV_RSP);
					originalMsg.putField(Iso8583.Bit._039_RSP_CODE, "06");
					originalMsg.putField(Iso8583.Bit._038_AUTH_ID_RSP, "000000");

					getLogger().logLine("processTranAdvRspFromTranmgr : 3" + originalMsg);

					action.putMsgToRemote(originalMsg);
				}
			}
		} catch (Exception e) {
			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30" + Transform.fromBinToHex(Transform.getString(msg.toMsg())), "ERR", nameInterface));
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "processTranAdvRspFromTranmgr", this.udpClient);
		}
		return action;
	}

	/**************************************************************************************
	 * Processes an Acquire Reversal Advice (0420/0421) from a remote interchange.
	 * Drivers capable of handling this message should implement this method.
	 * Otherwise, null should be returned.
	 *************************************************************************************/
	@Override
	public Action processAcquirerRevAdvFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws Exception {
		Action action = new Action();
		Base24Ath msgFromRemote = (Base24Ath) msg;
		udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				Transform.fromBinToHex(Transform.getString(msgFromRemote.getBinaryData())), "B24", nameInterface));
		Iso8583Post msgToTm = new Iso8583Post();
		putRecordIntoSourceToTmHashtableB24(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msgFromRemote);
//		Base24Ath msgToRemote = new Base24Ath(kwa);
		MessageTranslator translator = new MessageTranslator(params);
		try {
//			Utils tool = new Utils(params);

			int errMac = msgFromRemote.failedMAC();
			if (errMac == Base24Ath.MACError.INVALID_MAC_ERROR) {
				action = new Action(null, constructEchoMsgIndicatorFailedMAC(msgFromRemote, errMac), null, null);
			} else {

				int intAutraAnt = ValidateAutra.validateAutra(msgFromRemote, udpClient, nameInterface, routingFilter);

				int intAutraNvo = 0;

				if (intAutraAnt != intAutraNvo) {
					getLogger().logLine("**********************************************************************");
					getLogger().logLine("DIFERENCIA ValidacionAutra");
					getLogger().logLine(msgFromRemote.toString());
					getLogger().logLine("**********************************************************************");
					getLogger().logLine("**********************************************************************");
				}

				switch (intAutraNvo) {
				case 0:

					Super objectValidations = new Super(true, General.VOIDSTRING, General.VOIDSTRING,
							General.VOIDSTRING, new HashMap<String, String>(), params) {

						@Override
						public void validations(Base24Ath msg, Super objectValidations) {

						}
					};

					// Iso8583Post Isomsg = translator.constructIso8583(msgFromRemote,
					// objectValidations.getInforCollectedForStructData());

					Iso8583Post Isomsg = translator.constructIso8583(msgFromRemote, objectValidations);

					if (!objectValidations.getValidationResult()) {
						udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
								"Error de formato", "LOG", nameInterface));
						action.putMsgToTranmgr(translator.construct0220ToTm(msg, interchange.getName()));
						msgFromRemote = translator.constructBase24(msgFromRemote, objectValidations);
						udpClient.sendData(
								Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
										Transform.fromBinToHex(Transform.getString(msgFromRemote.toMsg(false))), "B24",
										nameInterface));
						action.putMsgToRemote(msgFromRemote);
					} else {
						GenericInterface.getLogger().logLine("MENSAJEIso8583Post:" + Isomsg.toString());
						action.putMsgToTranmgr(Isomsg);
						putRecordIntoSourceToTmHashtable(Isomsg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), Isomsg);
					}

					break;

				default:

					Super objectSuper = new Super(true, General.VOIDSTRING, General.VOIDSTRING, General.VOIDSTRING,
							null, params) {

						@Override
						public void validations(Base24Ath msg, Super objectValidations) {

						}
					};

					GenericInterface.getLogger().logLine("msgToTm AUTRA:" + msgToTm.toString());
					objectSuper.constructAutraRevMessage(msgFromRemote, msgToTm);
					GenericInterface.getLogger().logLine("msgToTm AUTRA:" + msgToTm.toString());

					action.putMsgToTranmgr(msgToTm);
					putRecordIntoSourceToTmHashtable(msgToTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msgToTm);
					putRecordIntoSourceToTmHashtableB24(msgToTm.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							msgFromRemote);
					break;

				}
			}
		} catch (Exception e) {
			Iso8583Post msg220 = translator.construct0220ToTm(msg, nameInterface);
			action.putMsgToTranmgr(msg220);
			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30 Exception en Mensaje: " + msg.toString(), "ERR", nameInterface));
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "processAcquirerRevAdvFromInterchange",
					this.udpClient);
		}
		return action;
	}

	/**************************************************************************************
	 * Processes an Acquire Reversal Advice Response (0430) from Transaction
	 * Manager. Drivers capable of handling this message should implement this
	 * method. Otherwise, null should be returned.
	 *************************************************************************************/
	@Override
	public Action processAcquirerRevAdvRspFromTranmgr(AInterchangeDriverEnvironment interchange, Iso8583Post msg)
			throws Exception {
		Action action = new Action();
		try {
			if (msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA)
					&& msg.getStructuredData().get("B24_MessageRsp") != null) {
				String sdData = msg.getStructuredData().get("B24_MessageRsp");
				byte[] decodedBytes = Base64.getDecoder().decode(sdData);
				String decodedString = new String(decodedBytes);
				Base24Ath msgDecoded = new Base24Ath(null);

				msgDecoded.fromMsg(decodedString);

				GenericInterface.getLogger().logLine("Respuesta 0430 Desencapsulada:" + msgDecoded.toString());
				udpClient.sendData(Client.getMsgKeyValue(msgDecoded.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						Transform.fromBinToHex(Transform.getString(msgDecoded.getBinaryData())), "B24", nameInterface));
				action.putMsgToRemote(msgDecoded);

			} else {
				MessageTranslator translator = new MessageTranslator(params);

				Base24Ath msgToRemote2 = translator.constructBase24((Iso8583Post) msg);
				udpClient.sendData(Client.getMsgKeyValue(msgToRemote2.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						Transform.fromBinToHex(Transform.getString(msgToRemote2.toMsg(false))), "B24", nameInterface));
				GenericInterface.getLogger().logLine("430CONSTRUCTISO8583:" + msgToRemote2);

				action.putMsgToRemote(msgToRemote2);
			}
		} catch (Exception e) {
			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30 Exception en Mensaje: " + msg.toString(), "ERR", nameInterface));
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "processAcquirerRevAdvRspFromTranmgr",
					this.udpClient);
		}
		return action;
	}

	@Override
	public Action processAcquirerRevAdvRspFromInterchange(AInterchangeDriverEnvironment interchange,
			Iso8583 msgFromRemote) throws Exception {
		GenericInterface.getLogger().logLine("Entro processAcquirerRevAdvRspFromInterchange");
		Base24Ath msg = (Base24Ath) msgFromRemote;
		Action action = new Action();
		String keyMsgFromRemote = msg
				.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR + msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR));
		Iso8583Post msgToTM = new Iso8583Post();
		msgToTM = (Iso8583Post) sourceTranToTmHashtable.get(keyMsgFromRemote);

		sourceTranToTmHashtable.remove(keyMsgFromRemote);

		if (msgToTM.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA)
				&& msgToTM.getStructuredData().get("B24_Message") != null) {
			Super objectSuper = new Super(true, General.VOIDSTRING, General.VOIDSTRING, General.VOIDSTRING, null,
					params) {

				@Override
				public void validations(Base24Ath msg, Super objectValidations) {

				}
			};
			objectSuper.constructAutraRevResponseMessage(msg, msgToTM);
			action.putMsgToTranmgr(msgToTM);
		} else {
			msgToTM.putMsgType(msgFromRemote.getMsgType());
			msgToTM.putField(Iso8583.Bit._038_AUTH_ID_RSP, msgFromRemote.getField((Iso8583.Bit._038_AUTH_ID_RSP)));
			msgToTM.putField(Iso8583.Bit._039_RSP_CODE, msgFromRemote.getField((Iso8583.Bit._039_RSP_CODE)));
			action.putMsgToTranmgr(msgToTM);
		}
		return action;
	}

	/**
	 * Processes a Transaction Advice (0220/0221) from a remote interchange. Drivers
	 * capable of handling this message should implement this method. Otherwise,
	 * null should be returned.
	 * 
	 */
	@Override
	public Action processTranAdvFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws java.lang.Exception {
		Action action = new Action();
		Base24Ath msgFromRemote = (Base24Ath) msg;
		udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				Transform.fromBinToHex(Transform.getString(msgFromRemote.getBinaryData())), "B24", nameInterface));
//		Iso8583Post msgToTm = new Iso8583Post();
//		Base24Ath msgToRemote = new Base24Ath(kwa);
		MessageTranslator translator = new MessageTranslator(params);
		try {

			int errMac = msgFromRemote.failedMAC();
			if (errMac == Base24Ath.MACError.INVALID_MAC_ERROR) {
				action = new Action(null, constructEchoMsgIndicatorFailedMAC(msgFromRemote, errMac), null, null);
			} else {

				Super objectValidations = new Super(true, General.VOIDSTRING, General.VOIDSTRING, General.VOIDSTRING,
						new HashMap<String, String>(), params) {

					@Override
					public void validations(Base24Ath msg, Super objectValidations) {

					}
				};

				// Iso8583Post Isomsg = translator.constructIso8583(msgFromRemote,
				// objectValidations.getInforCollectedForStructData());

				Iso8583Post Isomsg = translator.constructIso8583(msgFromRemote, objectValidations);

				GenericInterface.getLogger().logLine("MENSAJEIso8583Post:" + Isomsg.toString());

				action.putMsgToTranmgr(Isomsg);

				putRecordIntoSourceToTmHashtableB24(Isomsg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msgFromRemote);
				putRecordIntoSourceToTmHashtable(Isomsg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), Isomsg);

			}

		} catch (Exception e) {
			Iso8583Post msg220 = translator.construct0220ToTm(msg, nameInterface);
			action.putMsgToTranmgr(msg220);
			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30 Exception en Mensaje: " + msg.toString(), "ERR", nameInterface));
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "processTranAdvFromInterchange", this.udpClient);
		}
		return action;
	}

	/**************************************************************************************
	 * Hace un echo message del mensaje recibido de ATH. En el header se devuelve el
	 * tipo de la transaccion anteponiendole un '9' y el STATUS con el codigo de
	 * error correspondiente a la MAC.
	 *
	 * @param rsp       Mensaje desde ATH.
	 * @param codeError Codigo de error.
	 * @return Mensaje Base24Ath hacia ATH.
	 * @throws XPostilion En caso de error.
	 *************************************************************************************/
	Base24Ath constructEchoMsgIndicatorFailedMAC(Base24Ath rsp, int codeError) throws XPostilion {
		Header hdrInic = rsp.getHeader();

		try {
			hdrInic.putField(Header.Field.STATUS, String.valueOf(codeError));
			rsp.putHeader(hdrInic);
			rsp.putMsgType(0x9000 + rsp.getMsgType());
			EventRecorder.recordEvent(
					new InvalidMacRdbnNtwrk(new String[] { rsp.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR) }));
			rsp.putField(Base24Ath.Bit.CRYPTO_SERVICE_MSG, // 123
					Constants.KeyExchange.CSM_ERROR_GRAL_SRC);
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e,
					rsp.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "constructEchoMsgIndicatorFailedMAC",
					this.udpClient);
		}
		return rsp;
	}

	/**************************************************************************************
	 * Sube a memoria los mensajes llegan a la interfaz de tipo ISO8583Post.
	 *
	 * @param key Llave del Hashtable.
	 * @param msg Mensaje Base24Ath.
	 * @throws Exception En caso de error.
	 *************************************************************************************/
	public void putRecordIntoSourceToTmHashtable(String key, Iso8583Post msg) {
		sourceTranToTmHashtable.put(key, msg);
	}

	public void putRecordIntoSourceToTmHashtableB24(String key, Base24Ath msg) {
		sourceTranToTmHashtableB24.put(key, msg);
	}

	public static Logger getLogger() {
		return logger;
	}

	public static void setLogger(Logger logger) {
		GenericInterface.logger = logger;
	}

	/**************************************************************************************
	 * Processes a Network Management Request (0800/0801) from a remote interchange.
	 * Drivers capable of handling this message should implement this method.
	 * Otherwise, null should be returned.
	 *************************************************************************************/
	@Override
	public Action processNwrkMngReqFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws Exception {
		Action action = new Action();
		try {
			switch (Integer.parseInt(msg.getField(Iso8583.Bit.NETWORK_MNG_INFO_CODE))) {
			case Base24Ath.InfoCode.SIGN_ON:// 001
				action = processSignOnReqFromInterchange(interchange, msg);
				break;
			case Base24Ath.InfoCode.SIGN_OFF:// 002
				action = processSignOffReqFromInterchange(interchange, msg);
				break;
			case Base24Ath.InfoCode.ECHO_TEST:// 301
				action = processEchoTestReqFromInterchange(interchange, msg);
				break;
			case Base24Ath.InfoCode.CHANGE_KEY:// 161
				action = processKeyRequestReqFromInterchange(interchange, msg);
				break;
			case Base24Ath.InfoCode.NEW_KEY: // 162
				action = processKeyLoadKeyReqFromInterchange(interchange, msg);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "processNwrkMngReqFromInterchange",
					this.udpClient);
		}
		return action;
	}

	/**
	 * Construye un mensaje de respuesta de una Solicitud de Mensaje Administrativo.
	 * 
	 * @param interchange InformaciÃ³n de la interchange en Postilion.
	 * @param msg         Mensaje desde ATH.
	 * @return Action con el mensaje a enviar.
	 * @throws XPostilion Al obtener o poner un campo en el mensaje
	 */
	public Action processKeyLoadKeyReqFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws XPostilion {
		Base24Ath msgFromAth = (Base24Ath) msg;
		Action action = new Action();

		int errMac = msgFromAth.failedMAC();
		if (errMac == Base24Ath.MACError.INVALID_MAC_ERROR) {
			action.putMsgToRemote(constructEchoMsgIndicatorFailedMAC(msgFromAth, errMac));
		} else {

			Base24Ath msgToRemote = new Base24Ath(kwa);
			msgToRemote = constructNwrkMngMsgRspToRemote(msg, msgToRemote);

			if (!msg.isFieldSet(Base24Ath.Bit.CRYPTO_SERVICE_MSG)) {
				action.putMsgToRemote(constructRspMsgExchangePIN(msgToRemote,
						Constants.ErrorTypeCsm.INT_CSM_ERROR_GRAL_SRC, Base24Ath.RspCode._17_CUSTOMER_CANCEL, null));
				reportEvent(EventId.MISSING_FIELD053_SECURITY_INFO, interchange, null);
				return action;
			}
			String cryptoServiceMsg = msg.getField(Base24Ath.Bit.CRYPTO_SERVICE_MSG);
			if (!msg.isFieldSet(Iso8583.Bit._053_SECURITY_INFO)) {
				action.putMsgToRemote(constructRspMsgExchangePIN(msgToRemote,
						Constants.ErrorTypeCsm.INT_CSM_ERROR_GRAL_SRC, Base24Ath.RspCode._21_NO_ACTION_TAKEN, null));
				reportEvent(EventId.MISSING_FIELD053_SECURITY_INFO, interchange, null);
				return action;
			}

			String secInfo = msg.getField(Iso8583.Bit._053_SECURITY_INFO);
			String tipoLlave = secInfo.substring(0, 2);

			if (tipoLlave.equals(Constants.KeyExchangeMethod.TIPO_LLAVE_PIN)) {
				if (interchange.isSourceNode()) {
					return processKeyLoadKeyReqFromInterchangeToSourceNode(cryptoServiceMsg, msgToRemote, interchange,
							msgFromAth);
				}

			} else // Si no es una llave de PIN
			{
				EventRecorder.recordEvent(new InvalidTypeKey(new String[] { interchange.getName() }));
				return (new Action(null, constructEchoMsgIndicatorFailedMAC(msgFromAth,
						Base24Ath.MACError.KEY_SYNCRONIZATION_ERROR, interchange), null, null));
			}
		}
		return action;
	}

	/**
	 * Hace un echo message del mensaje recibido de ATH. En el header se devuelve el
	 * tipo de la transacciÃ³n anteponiendole un '9' y el STATUS con el cÃ³digo
	 * de error correspondiente a la MAC.
	 * 
	 * @param rsp       Mensaje desde ATH.
	 * @param codeError CÃ³digo de error.
	 * @return Mensaje Base24Ath hacia ATH.
	 * @throws XPostilion En caso de error.
	 */
	Base24Ath constructEchoMsgIndicatorFailedMAC(Base24Ath rsp, int codeError,
			AInterchangeDriverEnvironment interchange) throws XPostilion {
		Header hdrInic = rsp.getHeader();

		switch (codeError) {
		case Base24Ath.MACError.KEY_SYNCRONIZATION_ERROR:
			hdrInic.putField(Header.Field.STATUS, General.VOIDSTRING + Base24Ath.MACError.KEY_SYNCRONIZATION_ERROR);
			break;

		case Base24Ath.MACError.INVALID_MAC_ERROR:
			hdrInic.putField(Header.Field.STATUS, General.VOIDSTRING + Base24Ath.MACError.INVALID_MAC_ERROR);
			break;

		case Base24Ath.MACError.SECURITY_OPERATION_FAIL:
			hdrInic.putField(Header.Field.STATUS, General.VOIDSTRING + Base24Ath.MACError.SECURITY_OPERATION_FAIL);
			break;

		case Base24Ath.MACError.SECURITY_DEVICE_FAILURE:
			hdrInic.putField(Header.Field.STATUS, General.VOIDSTRING + Base24Ath.MACError.SECURITY_DEVICE_FAILURE);
			break;

		default:
			break;
		}
		rsp.putHeader(hdrInic);
		rsp.putMsgType(0x9000 + rsp.getMsgType());

		EventRecorder.recordEvent(new InvalidMacRdbnNtwrk(
				new String[] { rsp.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR), interchange.getName() }));
		rsp.putField(Base24Ath.Bit.CRYPTO_SERVICE_MSG, Constants.KeyExchangeMethod.CSM_ERROR_GRAL_SRC);

		return rsp;
	}

	public Action processKeyLoadKeyReqFromInterchangeToSourceNode(String cryptoServiceMsg, Base24Ath msgToRemote,
			AInterchangeDriverEnvironment interchange, Base24Ath msg) throws XPostilion {

		Action action = new Action();
		int indexKey = cryptoServiceMsg.indexOf("KD/");
		if (indexKey == -1) {
			action.putMsgToRemote(constructRspMsgExchangePIN(msgToRemote, Constants.ErrorTypeCsm.INT_CSM_ERROR_GRAL_SRC,
					Base24Ath.RspCode._68_RESPONSE_RECEIVED_LATE, null));
			reportEvent(EventId.MISSING_KEY_ON_FIELD_123_CRYPTO_SERVICE_MSG, interchange, null);
			return action;
		}

		String key = null;
		String isDES = null;

		isDES = cryptoServiceMsg.substring(indexKey + 19, indexKey + 20);

		if (isDES.equals(" ")) {
			key = cryptoServiceMsg.substring(indexKey + 3, indexKey + 19);
		} else {
			key = cryptoServiceMsg.substring(indexKey + 3, indexKey + 35);
		}

		if (!isLoadableKeyLength(key, interchange)) {
			EventRecorder.recordEvent(new InvalidLenghtCryptoKeyIdll(new String[] { interchange.getName() }));
		}

		String checkDigits = msgToRemote.getField(Base24Ath.Bit.KEY_MANAGEMENT);
		if (checkDigits.length() != Base24Ath.Length.CHECK_DIGIT_6) {
			action.putMsgToRemote(constructRspMsgExchangePIN(msgToRemote, Constants.ErrorTypeCsm.INT_CSM_ERROR_GRAL_SRC,
					Base24Ath.RspCode._17_CUSTOMER_CANCEL, checkDigits));
			reportEvent(EventId.INCORRECT_LENGTH_FIELD_120_KEY_MANAGEMENT, interchange, null);
			return action;
		}

		checkDigits = checkDigits.substring(0, Base24Ath.Length.CHECK_DIGIT);
		if (!Utils.isHexadecimal(checkDigits)) {
			action.putMsgToRemote(constructRspMsgExchangePIN(msgToRemote, Constants.ErrorTypeCsm.INT_CSM_ERROR_GRAL_SRC,
					Base24Ath.RspCode._17_CUSTOMER_CANCEL, checkDigits));
			reportEvent(EventId.INVALID_DATA_FIELD_120_KEY_MANAGEMENT, interchange, null);
			return action;
		}

		udpClient.sendData(
				Client.getMsgKeyValue("N/A", "key " + key + " checkdigits " + checkDigits, "LOG", nameInterface));

		try {

			if (interchange.loadSourceNodeKwp(key, checkDigits)) {

				if (interchange.relayKeySyncToActiveActivePartner()) {

					try {

						action.putMsgToActiveActivePartner(
								new ActiveActiveKeySyncMsgHandler(this, new NodeDriverEnvAdapter(interchange))
										.constructKeySyncMsgToPartner(interchange.getName(),
												interchange.getSourceNodeKwp().getContents()));
					} catch (Exception e) {

						action.putMsgToRemote(constructRspMsgExchangePIN(msgToRemote,
								Constants.ErrorTypeCsm.INT_CSM_RSM_SRC, Iso8583.RspCode._06_ERROR, null));
						EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e,
								msgToRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
								"processKeyLoadKeyReqFromInterchangeToSourceNode", this.udpClient);

					}
				}
				action.putMsgToRemote(constructRspMsgExchangePIN(msgToRemote, Constants.ErrorTypeCsm.INT_CSM_RSM_SRC,
						Iso8583.RspCode._00_SUCCESSFUL, null));
				reportEvent(EventId.SUCESSFULL_SOURCE_KEY_LOAD, interchange, null);

				Iso8583Post msgToTM = new Iso8583Post();

				MessageTranslator translator = new MessageTranslator(params);

				translator.constructAutra0800Message(msg, msgToTM);
				udpClient.sendData(Client.getMsgKeyValue("N/A",
						interchange.getSourceNodeKwp().getContents().getValueUnderKsk(), "KWP", interchange.getName()));
				if (kwa != null)
					udpClient.sendData(
							Client.getMsgKeyValue("N/A", kwa.getValueUnderKsk(), "KWA", interchange.getName()));

				action.putMsgToTranmgr(msgToTM);

				return action;
			} else // Si los digitos de chequeo no coinciden
			{
				action.putMsgToRemote(constructRspMsgExchangePIN(msgToRemote,
						Constants.ErrorTypeCsm.INT_CSM_ERROR_GRAL_SRC, Base24Ath.RspCode._12_BAD_CHECK_DIGITS, null));
				reportEvent(EventId.INVALID_SOURCE_KEY_LOADED, interchange, null);
				return action;
			}
		} catch (Exception e) {

			action.putMsgToRemote(constructRspMsgExchangePIN(msgToRemote, Constants.ErrorTypeCsm.INT_CSM_RSM_SRC,
					Iso8583.RspCode._06_ERROR, null));
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"processKeyLoadKeyReqFromInterchangeToSourceNode", this.udpClient);
			return action;

		}
	}

	/**
	 * Compara el tamano de la llave actual con el tamano de la llave que estÃ¡
	 * entrando.
	 * 
	 * @param keyUnderParent Llave cifrada con la llave padre.
	 * @param interchange    Infomracion de la interchange en Postilion.
	 * @return True si la llave es vÃ¡lida.
	 */
	public final boolean isLoadableKeyLength(String keyUnderParent, AInterchangeDriverEnvironment interchange) {
		boolean result = false;

		try {
			DesKwp nodeKwp = null;
			if (interchange.isSourceNode()) {
				nodeKwp = interchange.getSourceNodeKwp();
			} else if (interchange.isSinkNode()) {
				nodeKwp = interchange.getSinkNodeKwp();
			}

			DesKeyLength keyInL = DesKeyLength.getFromCryptogram(keyUnderParent);
			if (nodeKwp != null) {
				DesKeyLength actualKeyL = nodeKwp.getKeyLength();
				if (keyInL.equals(actualKeyL)) {
					result = true;
				} else {
					result = false;
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	/**
	 * Construye el eco de un mensaje 800 de intercambio de llave de PIN, retornando
	 * el indicador del campo del Mensaje Criptografico (123)
	 * 
	 * @param rsp         Mensaje desde ATH.
	 * @param csmType     Tipo de CSM.
	 * @param rspCode     CÃ³digo de respuesta.
	 * @param checkDigits DÃ­gitos de chequeo.
	 * @return Mensaje Base24Ath hacia ATH.
	 * @throws XPostilion En caso de error.
	 */
	public Base24Ath constructRspMsgExchangePIN(Base24Ath rsp, int csmType, String rspCode, String checkDigits)
			throws XPostilion {
		switch (csmType) {
		case Constants.ErrorTypeCsm.INT_CSM_ERROR_GRAL_SRC:
			rsp.putField(Base24Ath.Bit.CRYPTO_SERVICE_MSG, Constants.KeyExchangeMethod.CSM_ERROR_GRAL_SRC);
			break;
		case Constants.ErrorTypeCsm.INT_CSM_ERROR_CTP:
			rsp.putField(Base24Ath.Bit.CRYPTO_SERVICE_MSG, Constants.KeyExchangeMethod.CSM_ERROR_CTP);
			break;
		case Constants.ErrorTypeCsm.INT_CSM_DATA:
			rsp.putField(Base24Ath.Bit.CRYPTO_SERVICE_MSG, Constants.KeyExchangeMethod.CSM_DATA);
			break;
		case Constants.ErrorTypeCsm.INT_CSM_RSM_SRC:
			rsp.putField(Base24Ath.Bit.CRYPTO_SERVICE_MSG, Constants.KeyExchangeMethod.CSM_RSM);
			break;
		case Constants.ErrorTypeCsm.INT_CSM_RSM_SNK:
			rsp.putField(Base24Ath.Bit.CRYPTO_SERVICE_MSG, Constants.KeyExchangeMethod.CSM_RSM);
			break;
		default:
			rsp.putField(Base24Ath.Bit.CRYPTO_SERVICE_MSG, Constants.KeyExchangeMethod.CSM_ERROR_GRAL_SRC);
			break;
		}

		rsp.putField(Iso8583.Bit._039_RSP_CODE, rspCode);
		return rsp;
	}

	/**
	 * Construye un mensaje de respuesta hacia Ath cuando se recibe una solicitud de
	 * intercambio de llaves. Este método es llamado por POSTILION sólo si se
	 * define como MAESTRO.
	 *
	 * @param interchange Información de la interchange en Postilion.
	 * @param msg         Mensaje desde ATH.
	 * @return Action con el mensaje a enviar.
	 * @throws Exception En caso de error.
	 *************************************************************************************/
	public Action processKeyRequestReqFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws Exception {
		Base24Ath msgFromAth = (Base24Ath) msg;
		Action action = new Action();
		try {

			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"entro metodo processKeyRequestReqFromInterchange ********** ", "LOG", nameInterface));

			Base24Ath msgToRemote = constructNwrkMngMsgRspToRemote(msg);
			msgToRemote.copyFieldFrom(Iso8583.Bit.SECURITY_INFO, msgFromAth); // 053
			msgToRemote.copyFieldFrom(Iso8583.Bit.MAC_NORMAL, msgFromAth); // 064
			msgToRemote.copyFieldFrom(Base24Ath.Bit.KEY_MANAGEMENT, msgFromAth); // 120
			msgToRemote.copyFieldFrom(Base24Ath.Bit.CRYPTO_SERVICE_MSG, msgFromAth); // 123
			msgToRemote.copyFieldFrom(Iso8583.Bit.MAC_EXTENDED, msgFromAth); // 128
			action.putMsgToRemote(msgToRemote);

			if (interchange.isSourceNode()) {
				udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"entro al if metodo processKeyRequestReqFromInterchange ", "LOG", nameInterface));

				Base24Ath sourceMsg = null;

				sourceMsg = constructSourceNodeExchangeKwpMsgToRemote(interchange);
				action.putMsgToRemote(sourceMsg);
				action.putTimerAction(new Action.Timer(Base24Ath.CommandMsg.SOURCE_NODE_KEY_EXCHANGE,
						Long.valueOf(Base24Ath.PeriodTime.KEY_EXCHANGE_WAIT_TIME),
						new StringBuilder().append(sourceMsg.getField(Base24Ath.Bit.KEY_MANAGEMENT))
								.append(sourceMsg.getField(Base24Ath.Bit.CRYPTO_SERVICE_MSG)).toString()));
				EventRecorder.recordEvent(new SourceKeyExchangeInitiated(new String[] { interchange.getName() }));
			}
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e,
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "processKeyRequestReqFromInterchange",
					this.udpClient);
		}
		return action;
	}

	/**************************************************************************************
	 * Construye un mensaje de petición de intercambio de llave (0800).
	 *
	 * @param interchange Información de la interchange en Postilion.
	 * @return Mensaje Base24Ath hacia ATH.
	 * @throws Exception En caso de error.
	 *************************************************************************************/
	public Base24Ath constructSourceNodeExchangeKwpMsgToRemote(AInterchangeDriverEnvironment interchange)
			throws Exception {
		Base24Ath msgToRemote = null;
		try {
			if (interchange.getSourceNodeKwp() == null) {
				throw new XPostilion(
						new SourceNodeKeyNotConfigured(new AContext[] { new ApplicationContext(interchange.getName()) },
								new String[] { interchange.getName() }));
			}
			interchange.generateSourceNodeKwp();
			if (interchange.getSourceNodeKwp() != null) {
				DesKwp sourceNodeKey = interchange.getSourceNodeKwp();
				DateTime dateTimeLocal = new DateTime();
				msgToRemote = constructKeyExchangeNwrkMsgToRemote(interchange);
				msgToRemote.putField(Iso8583.Bit.SYSTEMS_TRACE_AUDIT_NR,
						new StringBuilder().append(Constants.KeyExchange.SOURCE_KEY_IND)
								.append(dateTimeLocal.get(Constants.FormatDate.HHMMSS).substring(1)).toString());
				msgToRemote.putField(Base24Ath.Bit.KEY_MANAGEMENT, sourceNodeKey.getCheckDigits().substring(0, 6));
				msgToRemote.putField(Base24Ath.Bit.CRYPTO_SERVICE_MSG,
						new StringBuilder().append(Constants.KeyExchange.CSM_DATA)
								.append(sourceNodeKey.getValueUnderParent())
								.append(Constants.KeyExchange.CSM_SOURCE_KEY_DATA).toString());
			} else {
				EventRecorder.recordEvent(new SourceKeyGenerationFailed(new String[] { interchange.getName() }));
				msgToRemote = null;
			}
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e,
					msgToRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"constructSourceNodeExchangeKwpMsgToRemote", this.udpClient);
		}
		return msgToRemote;
	}

	/**
	 * Method to process an echo test request received from remote
	 * 
	 * @param interchange The interchange involved
	 * @param msg
	 * @return The Action with the response message
	 * @throws Exception
	 */
	public Action processEchoTestReqFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws Exception {
		Action action = new Action();
		try {
			Base24Ath msg_to_remote = constructNwrkMngMsgRspToRemote(msg);
			return new Action(null, msg_to_remote, null, null);
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"processEchoTestReqFromInterchange", this.udpClient);
		}
		return action;
	}

	/**
	 * Method to process a sign off request received from remote
	 * 
	 * @param interchange The interchange involved
	 * @param msg         The message with the sign off request
	 * @return An action with the message containing the response
	 * @throws Exception
	 */
	public Action processSignOffReqFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws Exception {
		Base24Ath msg_to_remote = constructNwrkMngMsgRspToRemote(msg);
		this.signed_on = false;
		return new Action(null, msg_to_remote, null, null);
	}

	@Override
	public Action processNwrkMngReqFromTranmgr(AInterchangeDriverEnvironment interchange, Iso8583Post msg)
			throws Exception {
		GenericInterface.getLogger().logLine("Entro processNwrkMngReqFromTranmgr");
		String sdData = msg.getStructuredData().get("B24_Message");
		String keyMsg = msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR);

		putRecordIntoSourceToTmHashtable(keyMsg, msg);

		Action action = new Action();
		if (sdData != null) {
			byte[] decodedBytes = Base64.getDecoder().decode(sdData);

			String decodedString = new String(decodedBytes);

			Base24Ath msgDecoded = new Base24Ath(null);

			msgDecoded.fromMsg(decodedString);
			action.putMsgToRemote(msgDecoded);

			GenericInterface.getLogger().logLine("B24_Message: " + decodedString);
		}

		return action;
	}

	/**
	 * Method to construct an 0800 message to send to the remote entity
	 * 
	 * @param info_code The information code to put in field 70
	 * @return The 0800 message
	 * @throws Exception
	 */
	public Base24Ath constructNwrkMngReqToRemote(AInterchangeDriverEnvironment interchange, String info_code)
			throws Exception {
		Base24Ath msgToRemote = new Base24Ath(kwa);
		DateTime dateTimeGmt = new DateTime(0);
		DateTime date_time_local = new DateTime();
		try {
			switch (Integer.parseInt(info_code)) {
			case Base24Ath.InfoCode.SIGN_ON:
			case Base24Ath.InfoCode.ECHO_TEST:
				msgToRemote.putHeader((Header) constructNewNetworkHeader());
				msgToRemote.putMsgType(Iso8583Post.MsgType._0800_NWRK_MNG_REQ);
				msgToRemote.putField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME, dateTimeGmt.get("MMddHHmmss"));
				msgToRemote.putField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR, date_time_local.get("HHmmss"));
				msgToRemote.putField(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE, info_code);
				break;
			case Base24Ath.InfoCode.CHANGE_KEY:
				msgToRemote.putHeader(constructNewNetworkHeader());
				msgToRemote.putMsgType(Iso8583Post.MsgType._0800_NWRK_MNG_REQ);
				msgToRemote.putField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME, dateTimeGmt.get("MMddHHmmss"));
				msgToRemote.putField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR, date_time_local.get("HHmmss"));
				msgToRemote.putField(Iso8583.Bit._053_SECURITY_INFO, MsgTypeCsm.KEY_EXCHANGE_INBOUND_OUTBOUND_MSG);

				msgToRemote.putField(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE, Constants.KeyExchange.KEY_REQUEST);
				boolean is_source_node = interchange.isSourceNode();
				if (is_source_node) {
					msgToRemote.putField(Iso8583Post.Bit._123_POS_DATA_CODE, Constants.KeyExchange.CSM_RSM_161_SRC);
				} else {
					msgToRemote.putField(Iso8583Post.Bit._123_POS_DATA_CODE, Constants.KeyExchange.CSM_RSM_161_SNK);
				}
				EventRecorder.recordEvent(new SourceKeyExchangeInitiated(new String[] { interchange.getName() }));
				break;
			case Base24Ath.InfoCode.NEW_KEY:

				getLogger()
						.logLine("entro a case Base24Ath.InfoCode.NEW_KEY: en el metodo  constructNwrkMngReqToRemote");

				DesKwp source_node_key = interchange.getSourceNodeKwp();
				msgToRemote = constructKeyExchangeNwrkMsgToRemote(interchange);
				msgToRemote.putField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR,
						Constants.KeyExchange.SOURCE_KEY_IND + date_time_local.get("hhmmss").substring(1));
				msgToRemote.putField(Base24Ath.Bit.KEY_MANAGEMENT, source_node_key.getCheckDigits());
				msgToRemote.putField(Iso8583Post.Bit._123_POS_DATA_CODE,
						KeyExchange.CSM_DATA + source_node_key.getValueUnderParent() + KeyExchange.CSM_SOURCE_KEY_DATA);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"constructNwrkMngReqToRemote", this.udpClient);
		}
		return msgToRemote;
	}

	/**
	 * Method to build header for 0800 messages
	 * 
	 * @return The header
	 */
	public Header constructNewNetworkHeader() {
		Header network_header = new Header(default_header);
		try {
			network_header.putField(Header.Field.PRODUCT_INDICATOR, Header.ProductIndicator.POS);
			network_header.putField(Header.Field.RELEASE_NUMBER, Base24Ath.Version.REL_NR_34);
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"constructNewNetworkHeader", this.udpClient);
		}
		return network_header;
	}

	/**
	 * Processes a Network Management Request Response (0810) from a remote
	 * interchange. Drivers capable of handling this message should implement this
	 * method. Otherwise, null should be returned.
	 * 
	 * @param interchange
	 * @param msg
	 */
	public Action processNwrkMngReqRspFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws java.lang.Exception {
		Action action = new Action();
		try {
			switch (Integer.parseInt(msg.getField(Iso8583.Bit.NETWORK_MNG_INFO_CODE))) {
			case Base24Ath.InfoCode.SIGN_ON: // 001
			{
				action = processSignOnReqRspFromInterchange(interchange, msg);
				break;
			}

			case Base24Ath.InfoCode.CHANGE_KEY: // 161
			{
				action = processNewKeyExchangeReqRspFromInterchange(interchange, msg);
				break;
			}

			case Base24Ath.InfoCode.NEW_KEY: // 162
			{
				GenericInterface.getLogger().logLine("Entro processTranReqRspFromInterchange");
				Base24Ath msgFromRemote = (Base24Ath) msg;

				Iso8583Post msgToTM = (Iso8583Post) sourceTranToTmHashtable
						.get(msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR));

				sourceTranToTmHashtable.remove(msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR));
				// Iso8583Post msgToTM = new Iso8583Post();

				MessageTranslator translator = new MessageTranslator(params);

				translator.constructAutra0810ResponseMessage(msgFromRemote, msgToTM);

				action.putMsgToTranmgr(msgToTM);

				// constructAutra0810ResponseMessage
				// action = processNewKeyExchangeReqRspFromInterchange(interchange, msg);
				break;
			}

			default: {
				EventRecorder.recordEvent(new UnsupportedNmi(
						new String[] { interchange.getName(), msg.getField(Iso8583.Bit.NETWORK_MNG_INFO_CODE) }));
				break;
			}
			}
			return action;

		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"processNwrkMngReqRspFromInterchange", this.udpClient);
		}
		return action;
	}

	/**
	 * Procesa la respuesta de solicitud de intercambio de Llaves "161".
	 * 
	 * @param interchange Información de la interchange en Postilion.
	 * @param msg         Mensaje desde ATH.
	 * @return Action con el mensaje a enviar.
	 * @throws XPostilion al obtener un campo del mensaje
	 * @throws Exception  En caso de error.
	 */
	public Action processNewKeyExchangeReqRspFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws XPostilion {
		Base24Ath msgFromAth = (Base24Ath) msg;
		Action action = new Action();
		int errMac = msgFromAth.failedMAC();
		if (errMac == Base24Ath.MACError.INVALID_MAC_ERROR) {
			action.putMsgToRemote(constructEchoMsgIndicatorFailedMAC(msgFromAth, errMac));
		} else {

			action.putTimerAction(
					new Action.Timer(Base24Ath.CommandMsg.MANUAL_KEY_EXCHANGE_REQ, Action.Timer.STOP, null));

			keyExchangeState = Base24Ath.KeyExchangeState.IDLE;

			if (!msg.getField(Iso8583.Bit._039_RSP_CODE).equals(Iso8583.RspCode._00_SUCCESSFUL)) {
				EventRecorder.recordEvent(new KeyExchangeReqFailed(new String[] { interchange.getName() }));
			}
		}
		return action;
	}

	/**
	 * Procesa un mensaje de respuesta sign_on de la Interchange.
	 * 
	 * @param interchange Informaci�n de la interchange en Postilion.
	 * @param msg         Mensaje desde ATH.
	 * @return Action con el mensaje a enviar.
	 * @throws Exception En caso de error.
	 */
	public Action processSignOnReqRspFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws Exception {
		Action action = new Action();

		action.putTimerAction(new Action.Timer(Base24Ath.CommandMsg.AUTO_SIGN_ON, Action.Timer.STOP, null));

		action.putTimerAction(new Action.Timer(Base24Ath.CommandMsg.MANUAL_SIGN_ON, Action.Timer.STOP, null));

		signed_on = true;

//		signon_state = Base24Ath.SignOnState.IDLE;

		EventRecorder.recordEvent(new SignedOn(new String[] { interchange.getName() }));

		return action;
	}

	@Override
	public Action processNwrkMngReqRspFromTranmgr(AInterchangeDriverEnvironment interchange, Iso8583Post msg)
			throws Exception {

		Action action = new Action();
		return action;
	}

	/**
	 * Construye un eco del mensaje que llega.
	 * 
	 * @param msg Mensaje desde ATH.
	 * @return Mensaje Base24Ath hacia ATH.
	 * @throws XPostilion al copiar o poner un campo en el mensaje
	 * @throws Exception  En caso de error.
	 */

	public Base24Ath constructNwrkMngMsgRspToRemote(Iso8583 msg, Base24Ath msgToRemote) throws XPostilion {

		msgToRemote.putHeader(constructNetworkHeader(((Base24Ath) msg).getHeader()));

		msgToRemote.putMsgType(Iso8583.MsgType.NWRK_MNG_REQ_RSP);

		msgToRemote.copyFieldFrom(Iso8583.Bit.TRANSMISSION_DATE_TIME, msg);
		msgToRemote.copyFieldFrom(Iso8583.Bit.SYSTEMS_TRACE_AUDIT_NR, msg);
		msgToRemote.putField(Iso8583.Bit.RSP_CODE, Iso8583.RspCode.SUCCESSFUL);
		msgToRemote.copyFieldFrom(Iso8583.Bit.NETWORK_MNG_INFO_CODE, msg);
		msgToRemote.copyFieldFrom(Iso8583.Bit._053_SECURITY_INFO, msg);
		if (msg.isFieldSet(Base24Ath.Bit.KEY_MANAGEMENT))
			msgToRemote.copyFieldFrom(Base24Ath.Bit.KEY_MANAGEMENT, msg);
		msgToRemote.copyFieldFrom(Base24Ath.Bit.CRYPTO_SERVICE_MSG, msg);
		return msgToRemote;
	}

	/**************************************************************************************
	 * Construye el header del mensaje hacia ATH para un mensaje administrativo
	 * (0800).
	 *
	 * @param msgFromRemoteHeader Encabezado de mensaje desde ATH.
	 * @return Objeto Header.
	 *************************************************************************************/
	public Header constructNetworkHeader(Header msgFromRemoteHeader) {
		Header networkHeader = new Header(msgFromRemoteHeader);
		try {
			networkHeader.putField(Header.Field.PRODUCT_INDICATOR, Header.ProductIndicator.NETWORK);
			networkHeader.putField(Header.Field.RELEASE_NUMBER, Base24Ath.Version.REL_NR_60);
			networkHeader.putField(Header.Field.RESPONDER_CODE, Header.SystemCode.HOST);
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"constructNetworkHeader", this.udpClient);
		}
		return networkHeader;
	}

	/**************************************************************************************
	 * Construye un mensaje de intercambio 0800. Campo 70 = 162.
	 *
	 * @param interchange Información de la interchange en Postilion.
	 * @return Mensaje Base24Ath hacia ATH.
	 * @throws Exception En caso de error.
	 *************************************************************************************/
	public Base24Ath constructKeyExchangeNwrkMsgToRemote(AInterchangeDriverEnvironment interchange) throws Exception {
		DateTime date_time_gmt = new DateTime(0);
		Base24Ath msgToRemote = new Base24Ath(kwa);
		try {
			msgToRemote.putHeader(constructNewNetworkHeader());
			msgToRemote.putMsgType(Iso8583.MsgType.NWRK_MNG_REQ);
			msgToRemote.putField(Iso8583.Bit.TRANSMISSION_DATE_TIME,
					date_time_gmt.get(Validation.FormatDate.MMDDhhmmss));
			msgToRemote.putField(Iso8583.Bit.SECURITY_INFO, MsgTypeCsm.KEY_EXCHANGE_INBOUND_OUTBOUND_MSG);
			msgToRemote.putField(Iso8583.Bit.NETWORK_MNG_INFO_CODE, Constants.KeyExchange.NEW_KEY);
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"constructKeyExchangeNwrkMsgToRemote", this.udpClient);
		}
		return msgToRemote;
	}

	/**************************************************************************************
	 * Construye un eco del mensaje que llega.
	 *
	 * @param msg Mensaje desde ATH.
	 * @return Mensaje Base24Ath hacia ATH.
	 * @throws Exception En caso de error.
	 *************************************************************************************/
	public Base24Ath constructNwrkMngMsgRspToRemote(Iso8583 msg) throws Exception {
		Base24Ath msgToRemote = new Base24Ath(kwa);
		try {
			msgToRemote.putHeader(constructNetworkHeader(((Base24Ath) msg).getHeader()));
			msgToRemote.putMsgType(Iso8583.MsgType.NWRK_MNG_REQ_RSP); // 810
			msgToRemote.copyFieldFrom(Iso8583.Bit.TRANSMISSION_DATE_TIME, msg); // 007
			msgToRemote.copyFieldFrom(Iso8583.Bit.SYSTEMS_TRACE_AUDIT_NR, msg); // 011
			msgToRemote.putField(Iso8583.Bit.RSP_CODE, Iso8583.RspCode.SUCCESSFUL); // 039
			msgToRemote.copyFieldFrom(Iso8583.Bit.NETWORK_MNG_INFO_CODE, msg); // 070
			msgToRemote.copyFieldFrom(Iso8583.Bit._053_SECURITY_INFO, msg); // 053
			msgToRemote.copyFieldFrom(Base24Ath.Bit.KEY_MANAGEMENT, msg); // 120
			msgToRemote.copyFieldFrom(Base24Ath.Bit.CRYPTO_SERVICE_MSG, msg); // 123
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"constructNwrkMngMsgRspToRemote", this.udpClient);
		}
		return msgToRemote;
	}

	/**
	 * Method to process an sign request received from remote
	 * 
	 * @param interchange The interchange involved
	 * @param msg
	 * @return The Action with the response message
	 * @throws Exception
	 */
	public Action processSignOnReqFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws Exception {
		Action action = new Action();
		try {
			Base24Ath response = constructNwrkMngMsgRspToRemote(msg);
			response.copyFieldFrom(Iso8583.Bit._048_ADDITIONAL_DATA, msg);
			action.putMsgToRemote(response);
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(this.nameInterface, GenericInterface.class.getName(), e, "N/D",
					"processSignOnReqFromInterchange", this.udpClient);
		}
		return action;
	}

	@Override
	public Action processAcquirerFileUpdateAdvFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msg)
			throws Exception {
		Action action = new Action();
		Object response[] = this.factory.commandProcess(new Base24Ath(kwa), (Iso8583Post) msg);
		action.putMsgToRemote((Iso8583Post) response[1]);
		return action;
	}

}