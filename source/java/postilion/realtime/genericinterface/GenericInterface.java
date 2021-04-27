package postilion.realtime.genericinterface;

import postilion.realtime.library.common.model.ConfigAllTransaction;
import postilion.realtime.library.common.util.Logger;
import postilion.realtime.library.common.util.constants.MsgTypeCsm;
import postilion.realtime.sdk.crypto.*;

import postilion.realtime.sdk.eventrecorder.AContext;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.eventrecorder.contexts.ApplicationContext;
import postilion.realtime.library.common.util.constants.General;
import postilion.realtime.library.common.model.ResponseCode;
import postilion.realtime.genericinterface.channels.Super;
import postilion.realtime.genericinterface.eventrecorder.events.CannotProcessAcqReconRspFromRemote;
import postilion.realtime.genericinterface.eventrecorder.events.IncorrectLengthField_120KeyManagement;
import postilion.realtime.genericinterface.eventrecorder.events.IncorrectRuntimeParameters;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidAddressKey;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidDataField_120KeyManagement;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidDataField_123CryptoServiceMsg;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidLenghtCryptoKeyIdll;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidMacRdbnNtwrk;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidMessage;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidSinkKeyLoaded;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidSourceKeyLoaded;
import postilion.realtime.genericinterface.eventrecorder.events.InvalidTypeKey;
import postilion.realtime.genericinterface.eventrecorder.events.MissingField053SecurityInfo;
import postilion.realtime.genericinterface.eventrecorder.events.MissingField_120KeyManagement;
import postilion.realtime.genericinterface.eventrecorder.events.MissingKeyOnField_123CryptoServiceMsg;
import postilion.realtime.genericinterface.eventrecorder.events.SinkNodeKeyNotConfigured;
import postilion.realtime.genericinterface.eventrecorder.events.SinkNodeKeyReceivedByNonsinkNode;
import postilion.realtime.genericinterface.eventrecorder.events.TryCatchException;
import postilion.realtime.genericinterface.eventrecorder.events.SourceKeyExchangeInitiated;
import postilion.realtime.genericinterface.eventrecorder.events.SourceNodeKeyNotConfigured;
import postilion.realtime.genericinterface.eventrecorder.events.SourceNodeKeyReceivedByNonsourceNode;
import postilion.realtime.genericinterface.eventrecorder.events.SucessfullSinkKeyLoad;
import postilion.realtime.genericinterface.eventrecorder.events.SucessfullSourceKeyLoad;
import postilion.realtime.genericinterface.eventrecorder.events.SourceKeyGenerationFailed;
import postilion.realtime.genericinterface.eventrecorder.events.KeyExchangeReqFailed;
import postilion.realtime.genericinterface.eventrecorder.events.SignedOn;
import postilion.realtime.genericinterface.eventrecorder.events.UnsupportedNmi;

import postilion.realtime.genericinterface.translate.ConstructFieldMessage;
import postilion.realtime.genericinterface.translate.MessageTranslator;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.stream.Header;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.Constants.KeyExchange;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.genericinterface.translate.validations.Validation;
import postilion.realtime.genericinterface.translate.validations.Validation.ErrorMessages;
import postilion.realtime.sdk.message.IMessage;
import postilion.realtime.sdk.message.bitmap.BitmapMessage;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.Iso8583.MsgType;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.message.bitmap.PosEntryMode;
import postilion.realtime.sdk.message.bitmap.ProcessingCode;
import postilion.realtime.sdk.message.bitmap.StructuredData;
import postilion.realtime.sdk.message.bitmap.XFieldUnableToConstruct;
import postilion.realtime.sdk.message.xml.XMLMessage2;
import postilion.realtime.sdk.node.AInterchangeDriver8583;
import postilion.realtime.sdk.node.AInterchangeDriverEnvironment;
import postilion.realtime.sdk.node.Action;
import postilion.realtime.sdk.node.XNodeParameterUnknown;
import postilion.realtime.sdk.node.XNodeParameterValueInvalid;
import postilion.realtime.sdk.util.DateTime;
import postilion.realtime.sdk.util.TimedHashtable;
import postilion.realtime.sdk.util.XPostilion;
import postilion.realtime.sdk.util.convert.Pack;
import postilion.realtime.sdk.util.convert.Transform;
import postilion.realtime.sdk.node.ActiveActiveKeySyncMsgHandler;
import postilion.realtime.sdk.node.NodeDriverEnvAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.JSONParser;



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

	/**
	 * Contiene los mensajes 0200 que llegan de la Interchange al NodoSource.
	 */
	public TimedHashtable sourceTranToTmHashtable = null;

	public TimedHashtable sourceTranToTmHashtableB24 = null;

	public Map<String, HashMap<String, ConfigAllTransaction>> structureContent = new HashMap<>();
	public Map<String, String> covenantMap = new HashMap<>();
	public Map<String, ConfigAllTransaction> structureMap = new HashMap<>();

	protected long retentionPeriod = 0;

	public Map<String, ResponseCode> allCodesIsoToB24 = new HashMap<>();
	public Map<String, ResponseCode> allCodesIscToIso = new HashMap<>();
	public Map<String, ResponseCode> allCodesIsoToB24TM = new HashMap<>();
	public Map<String, ResponseCode> allCodesB24ToIso = new HashMap<>();
	public boolean consultCovenants; // to validate consult of covenants
	public boolean businessValidation, create0220ToTM;
	static Header default_header;
	public String sSignOn; // 0 - Don't Send SignOn on connection with
							// the remote entity. 1 - Send SignOn
							// on connection with the remote entity

	public static String exceptionMessage=null;
	/** Inicia la variable keyExchangeState en inactivo. */
	protected int keyExchangeState = Base24Ath.KeyExchangeState.IDLE;
	public boolean signed_on = false;
	public Map<String, String> institutionid = new HashMap<>();

	public Map<String, String> cutValues = new HashMap<>();
	public static Map<String, String> migratedOpCodes = new HashMap<>();
	public static Map<String, String> migratedCards = new HashMap<>();
	public static Map<String, String> migratedBins = new HashMap<>();
	public static Map<String, String> migratedOpCodesAtm = new HashMap<>();

	public static Map<String, String> structuredDataFields = new HashMap<>();
	public static Map<String, String> createFields = new HashMap<>();
	public static Map<String, String> transformFields = new HashMap<>();
	public static Map<String, String> transformFieldsMultipleCases = new HashMap<>();
	public static Map<String, String> skipCopyFields = new HashMap<>();

	public static Map<String, String> createFieldsRev = new HashMap<>();
	public static Map<String, String> transformFieldsRev = new HashMap<>();
	public static Map<String, String> transformFieldsMultipleCasesRev = new HashMap<>();
	public static Map<String, String> skipCopyFieldsRev = new HashMap<>();

	public static Map<String, String> copyFieldsResponse = new HashMap<>();
	public static Map<String, String> deleteFieldsResponse = new HashMap<>();
	public static Map<String, String> createFieldsResponse = new HashMap<>();
	public static Map<String, String> transformFieldsResponse = new HashMap<>();
	public static Map<String, String> transformFieldsMultipleCasesResponse = new HashMap<>();

	public static Map<String, String> copyFieldsResponseAdv = new HashMap<>();
	public static Map<String, String> deleteFieldsResponseAdv = new HashMap<>();
	public static Map<String, String> createFieldsResponseAdv = new HashMap<>();
	public static Map<String, String> transformFieldsResponseAdv = new HashMap<>();
	public static Map<String, String> transformFieldsMultipleCasesResponseAdv = new HashMap<>();

	public static Map<String, String> copyFieldsResponseRev = new HashMap<>();
	public static Map<String, String> deleteFieldsResponseRev = new HashMap<>();
	public static Map<String, String> createFieldsResponseRev = new HashMap<>();
	public static Map<String, String> transformFieldsResponseRev = new HashMap<>();
	public static Map<String, String> transformFieldsMultipleCasesResponseRev = new HashMap<>();
	
	
	public static Map<String, String> primerFiltroTest1 = new HashMap<>();
	public static Map<String, String> primerFiltroTest2 = new HashMap<>();
	public static Map<String, String> segundoFiltroTest2 = new HashMap<>();
	public static Map<String, String> segundoFiltro = new HashMap<>();
	
	public static Map<String, String> createFields220ToTM = new HashMap<>();

	public String issuerId = null;

	public String ipUdpServer = "0";
	public String portUdpServer = "0";
	public String ipServerAT = "0";
	public String portServerAT = "0";

	public Client udpClient = null;
	public Client udpClientAT = null;
	private static Logger logger = new Logger(Constants.Config.URL_LOG);

	public String nameInterface = "";
	public boolean encodeData = false;
	public String routingField100 = "";
	public boolean exeptionValidateExpiryDate = false;
	public String urlCutWS = null;

	public Parameters params;

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
		
		
		createFields220ToTM.put("3", "3");
		createFields220ToTM.put("4", "4");
		createFields220ToTM.put("7", "7");
		createFields220ToTM.put("11", "11");
		createFields220ToTM.put("12", "12");
		createFields220ToTM.put("13", "13");
		createFields220ToTM.put("15", "15");
		createFields220ToTM.put("22", "22");
		createFields220ToTM.put("25", "25");
		createFields220ToTM.put("32", "32");
		createFields220ToTM.put("35", "35");
		createFields220ToTM.put("37", "37");
		createFields220ToTM.put("39", "39");
		createFields220ToTM.put("41", "41");
		createFields220ToTM.put("42", "42");
		createFields220ToTM.put("43", "43");
		createFields220ToTM.put("48", "48");
		createFields220ToTM.put("49", "49");
		createFields220ToTM.put("98", "98");
		createFields220ToTM.put("100", "100");
		createFields220ToTM.put("102", "102");
		createFields220ToTM.put("104", "104");
		createFields220ToTM.put("123", "123");

		
		
		
		
		

		structuredDataFields.put("17", "17");
		structuredDataFields.put("46", "46");
		structuredDataFields.put("60", "60");
		structuredDataFields.put("61", "61");
		structuredDataFields.put("62", "62");
		structuredDataFields.put("126", "126");
		structuredDataFields.put("128", "128");

		createFields.put("15", "compensationDateValidationP17ToP15");
		createFields.put("22", "constructField22");
		createFields.put("25", "constructPosConditionCodeToTranmgr");
		createFields.put("26", "constructDefaultField26");
		createFields.put("42", "constructDefaultCardAceptorCode");
		createFields.put("43", "constructField43");
		createFields.put("59", "constructEchoData");
		createFields.put("98", "constructField98");
		createFields.put("100", "constructField100");
		createFields.put("123", "constructPosDataCode");
		
		
		
		
		transformFieldsMultipleCasesResponse.put("3", "3");
		transformFieldsMultipleCasesResponse.put("44", "44");
		transformFieldsMultipleCasesResponse.put("48", "48");
		transformFieldsMultipleCasesResponse.put("63", "63");
		transformFieldsMultipleCasesResponse.put("102", "102");
		transformFieldsMultipleCasesResponse.put("103", "103");
		
		transformFieldsResponse.put("3", "N/A");
		transformFieldsResponse.put("3-210110", "transformField3ForDepositATM");
		transformFieldsResponse.put("3-210120", "transformField3ForDepositATM");
		transformFieldsResponse.put("3-320100_270100", "transformField3ForCreditPaymentATM");
		transformFieldsResponse.put("3-320100_270140", "transformField3ForCreditPaymentATM");
		transformFieldsResponse.put("3-320100_270141", "transformField3ForCreditPaymentATM");
		transformFieldsResponse.put("3-510100", "transformField3ForCreditPaymentATM");
		transformFieldsResponse.put("3-510140", "transformField3ForCreditPaymentATM");
		transformFieldsResponse.put("3-510141", "transformField3ForCreditPaymentATM");
		transformFieldsResponse.put("3-320100_270120","constructProcessingCode");
		transformFieldsResponse.put("3-320100_270130","constructProcessingCode");
		transformFieldsResponse.put("3-320000","constructProcessingCode");
		transformFieldsResponse.put("3-321000","constructProcessingCode");
		transformFieldsResponse.put("3-321000_011000","constructProcessingCode");
		transformFieldsResponse.put("3-321000_311000","constructProcessingCode");
		transformFieldsResponse.put("3-321000_321000","constructProcessingCode");
		transformFieldsResponse.put("3-321000_381000","constructProcessingCode");
		transformFieldsResponse.put("3-321000_401000","constructProcessingCode");
		transformFieldsResponse.put("3-321000_401010","constructProcessingCode");
		transformFieldsResponse.put("3-321000_401020","constructProcessingCode");
		transformFieldsResponse.put("3-321000_501000","constructProcessingCode");
		transformFieldsResponse.put("3-321000_501030","constructProcessingCode");
		transformFieldsResponse.put("3-321000_501040","constructProcessingCode");
		transformFieldsResponse.put("3-321000_501041","constructProcessingCode");
		transformFieldsResponse.put("3-321000_501042","constructProcessingCode");
		transformFieldsResponse.put("3-320100_270110","constructProcessingCode");
		transformFieldsResponse.put("3-322000","constructProcessingCode");
		transformFieldsResponse.put("3-322000_012000","constructProcessingCode");
		transformFieldsResponse.put("3-322000_312000","constructProcessingCode");
		transformFieldsResponse.put("3-322000_322000","constructProcessingCode");
		transformFieldsResponse.put("3-322000_382000","constructProcessingCode");
		transformFieldsResponse.put("3-322000_402000","constructProcessingCode");
		transformFieldsResponse.put("3-322000_402010","constructProcessingCode");
		transformFieldsResponse.put("3-322000_402020","constructProcessingCode");
		transformFieldsResponse.put("3-322000_502000","constructProcessingCode");
		transformFieldsResponse.put("3-322000_502030","constructProcessingCode");
		transformFieldsResponse.put("3-322000_502040","constructProcessingCode");
		transformFieldsResponse.put("3-322000_502041","constructProcessingCode");
		transformFieldsResponse.put("3-322000_502042","constructProcessingCode");
		transformFieldsResponse.put("3-324000_314000","constructProcessingCode");
		transformFieldsResponse.put("3-324000_404010","constructProcessingCode");
		transformFieldsResponse.put("3-324000_404020","constructProcessingCode");
		transformFieldsResponse.put("38-011000", "constructField38DefaultOrCopy");
		transformFieldsResponse.put("38-012000", "constructField38DefaultOrCopy");
		transformFieldsResponse.put("38-222222", "constructField38DefaultOrCopy");
		transformFieldsResponse.put("38-314000", "constructField38DefaultOrCopy");
		transformFieldsResponse.put("38-404010", "constructField38DefaultOrCopy");
		transformFieldsResponse.put("38-404020", "constructField38DefaultOrCopy");
		transformFieldsResponse.put("40-011000", "constructServiceRestrictionCode");
		transformFieldsResponse.put("40-012000", "constructServiceRestrictionCode");
		transformFieldsResponse.put("40-222222", "constructServiceRestrictionCode");
		transformFieldsResponse.put("40-321000_321000", "constructServiceRestrictionCode");
		transformFieldsResponse.put("40-322000_322000", "constructServiceRestrictionCode");
		transformFieldsResponse.put("44", "N/A");
		transformFieldsResponse.put("44-011000","constructField44");
		transformFieldsResponse.put("44-012000","constructField44");
		transformFieldsResponse.put("44-222222","constructField44");
		transformFieldsResponse.put("44-311000","constructField44");
		transformFieldsResponse.put("44-312000","constructField44");
		transformFieldsResponse.put("44-314000","constructField44");
		transformFieldsResponse.put("44-320100_270100","constructField44");
		transformFieldsResponse.put("44-320100_270140","constructField44");
		transformFieldsResponse.put("44-501000","constructField44");
		transformFieldsResponse.put("44-501030","constructField44");
		transformFieldsResponse.put("44-501040","constructField44");
		transformFieldsResponse.put("44-501041","constructField44");
		transformFieldsResponse.put("44-501042","constructField44");
		transformFieldsResponse.put("44-502000","constructField44");
		transformFieldsResponse.put("44-502030","constructField44");
		transformFieldsResponse.put("44-502040","constructField44");
		transformFieldsResponse.put("44-502041","constructField44");
		transformFieldsResponse.put("44-502042","constructField44");
		transformFieldsResponse.put("44-510100","constructField44");
		transformFieldsResponse.put("44-510140","constructField44");
		transformFieldsResponse.put("44-510141","constructField44");
		transformFieldsResponse.put("44-321000_321000","constuctServiceRspData");
		transformFieldsResponse.put("44-322000_012000","constuctServiceRspData");
		transformFieldsResponse.put("44-322000_322000","constuctServiceRspData");
		transformFieldsResponse.put("44-381000","constuctServiceRspData");
		transformFieldsResponse.put("44-382000","constuctServiceRspData");
		transformFieldsResponse.put("48", "N/A");
		transformFieldsResponse.put("48-321000_011000","constructField048InRspCostInquiry");
		transformFieldsResponse.put("48-321000_381000","constructField048InRspCostInquiry");
		transformFieldsResponse.put("48-322000_012000","constructField048InRspCostInquiry");
		transformFieldsResponse.put("48-322000_382000","constructField048InRspCostInquiry");
		transformFieldsResponse.put("48-011000","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-012000","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-210110","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-210120","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-222222","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-320100_270100","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-320100_270110","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-320100_270120","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-320100_270130","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-320100_270140","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-320100_270141","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-321000_321000","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-322000_322000","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-324000_314000","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-324000_404010","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-324000_404020","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-510100","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-510140","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("48-510141","constructFieldFromStructuredDataP48");
		transformFieldsResponse.put("54-011000", "constructField54");
		transformFieldsResponse.put("54-012000", "constructField54");
		transformFieldsResponse.put("63", "N/A");
		transformFieldsResponse.put("63-011000","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-012000","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-222222","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-321000_011000","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-321000_401000","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-321000_501030","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-322000_012000","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-322000_402000","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-322000_502030","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-401000","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-401010","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-401020","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-402000","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-402010","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-402020","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-501000","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-501030","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-501040","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-501041","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-501042","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-502030","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-502040","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-502041","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-502042","constuctCodeResponseInIsc");
		transformFieldsResponse.put("63-210110","constructResponseCodeField63");
		transformFieldsResponse.put("63-210120","constructResponseCodeField63");
		transformFieldsResponse.put("63-314000","constructResponseCodeField63");
		transformFieldsResponse.put("63-320000","constructResponseCodeField63");
		transformFieldsResponse.put("63-320100_270100","constructResponseCodeField63");
		transformFieldsResponse.put("63-320100_270110","constructResponseCodeField63");
		transformFieldsResponse.put("63-320100_270120","constructResponseCodeField63");
		transformFieldsResponse.put("63-320100_270130","constructResponseCodeField63");
		transformFieldsResponse.put("63-320100_270140","constructResponseCodeField63");
		transformFieldsResponse.put("63-320100_270141","constructResponseCodeField63");
		transformFieldsResponse.put("63-321000","constructResponseCodeField63");
		transformFieldsResponse.put("63-321000_321000","constructResponseCodeField63");
		transformFieldsResponse.put("63-321000_401010","constructResponseCodeField63");
		transformFieldsResponse.put("63-321000_401020","constructResponseCodeField63");
		transformFieldsResponse.put("63-321000_501000","constructResponseCodeField63");
		transformFieldsResponse.put("63-321000_501040","constructResponseCodeField63");
		transformFieldsResponse.put("63-321000_501041","constructResponseCodeField63");
		transformFieldsResponse.put("63-321000_501042","constructResponseCodeField63");
		transformFieldsResponse.put("63-322000","constructResponseCodeField63");
		transformFieldsResponse.put("63-322000_322000","constructResponseCodeField63");
		transformFieldsResponse.put("63-322000_402010","constructResponseCodeField63");
		transformFieldsResponse.put("63-322000_402020","constructResponseCodeField63");
		transformFieldsResponse.put("63-322000_502000","constructResponseCodeField63");
		transformFieldsResponse.put("63-322000_502040","constructResponseCodeField63");
		transformFieldsResponse.put("63-322000_502041","constructResponseCodeField63");
		transformFieldsResponse.put("63-322000_502042","constructResponseCodeField63");
		transformFieldsResponse.put("63-324000_314000","constructResponseCodeField63");
		transformFieldsResponse.put("63-324000_404010","constructResponseCodeField63");
		transformFieldsResponse.put("63-324000_404020","constructResponseCodeField63");
		transformFieldsResponse.put("63-404010","constructResponseCodeField63");
		transformFieldsResponse.put("63-404020","constructResponseCodeField63");
		transformFieldsResponse.put("63-502000","constructResponseCodeField63");
		transformFieldsResponse.put("63-510100","constructResponseCodeField63");
		transformFieldsResponse.put("63-510140","constructResponseCodeField63");
		transformFieldsResponse.put("63-510141","constructResponseCodeField63");
		transformFieldsResponse.put("100-321000_401010", "constructField100ACH");
		transformFieldsResponse.put("100-321000_401020", "constructField100ACH");
		transformFieldsResponse.put("100-322000_402010", "constructField100ACH");
		transformFieldsResponse.put("100-322000_402020", "constructField100ACH");
		transformFieldsResponse.put("102", "N/A");
		transformFieldsResponse.put("102-314000", "constructField102ConsultaCreditoRotativo");
		transformFieldsResponse.put("102-311000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-312000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-320000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-321000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-321000_311000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-321000_381000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-322000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-322000_312000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-322000_382000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-381000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-382000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("102-011000","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-012000","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-222222","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-321000_011000","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-321000_321000","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-321000_401000","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-322000_012000","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-322000_322000","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-322000_402000","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-324000_314000","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-324000_404010","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-324000_404020","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-401010","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-401020","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-402010","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-402020","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-404010","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-404020","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-501000","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-501030","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-501040","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-501041","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-501042","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-502030","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-502040","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-502041","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-502042","constructField102DefaultOrCopy");
		transformFieldsResponse.put("102-321000_401010","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-321000_401020","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-321000_501000","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-321000_501030","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-321000_501040","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-321000_501041","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-321000_501042","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-322000_402010","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-322000_402020","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-322000_502000","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-322000_502030","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-322000_502040","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-322000_502041","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("102-322000_502042","constructField102ConsultaCostoTransferencia");
		transformFieldsResponse.put("103", "N/A");
		transformFieldsResponse.put("103-311000","transformField103");
		transformFieldsResponse.put("103-312000","transformField103");
		transformFieldsResponse.put("103-314000","transformField103");
		transformFieldsResponse.put("103-322000_322000","transformField103");
		transformFieldsResponse.put("103-324000_314000","transformField103");
		transformFieldsResponse.put("103-381000","transformField103");
		transformFieldsResponse.put("103-382000","transformField103");
		transformFieldsResponse.put("103-320000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("103-321000_011000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("103-321000_311000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("103-321000_321000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("103-321000_381000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("103-322000_012000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("103-322000_312000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("103-322000_382000","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("103-322000_502030","constructField102_103ConsultaCosto");
		transformFieldsResponse.put("103-321000_401010","constructField104Deposito");
		transformFieldsResponse.put("103-321000_401020","constructField104Deposito");
		transformFieldsResponse.put("103-321000_501000","constructField104Deposito");
		transformFieldsResponse.put("103-321000_501030","constructField104Deposito");
		transformFieldsResponse.put("103-321000_501040","constructField104Deposito");
		transformFieldsResponse.put("103-321000_501041","constructField104Deposito");
		transformFieldsResponse.put("103-321000_501042","constructField104Deposito");
		transformFieldsResponse.put("103-322000_402010","constructField104Deposito");
		transformFieldsResponse.put("103-322000_402020","constructField104Deposito");
		transformFieldsResponse.put("103-322000_502000","constructField104Deposito");
		transformFieldsResponse.put("103-322000_502040","constructField104Deposito");
		transformFieldsResponse.put("103-322000_502041","constructField104Deposito");
		transformFieldsResponse.put("103-322000_502042","constructField104Deposito");
		transformFieldsResponse.put("105-321000_401000","constructField105DefaultOrCopy");
		transformFieldsResponse.put("105-322000_402000","constructField105DefaultOrCopy");
		transformFieldsResponse.put("126-011000", "constructField126IsoTranslate");
		transformFieldsResponse.put("126-012000", "constructField126IsoTranslate");
		transformFieldsResponse.put("126-320100_270100", "constructField126IsoTranslate");
		transformFieldsResponse.put("126-320100_270130", "constructField126IsoTranslate");
		transformFieldsResponse.put("126-320100_270140", "constructField126IsoTranslate");
		transformFieldsResponse.put("126-320100_270141", "constructField126IsoTranslate");
		transformFieldsResponse.put("126-321000_011000", "constructField126IsoTranslate");
		transformFieldsResponse.put("126-322000_012000", "constructField126IsoTranslate");
		transformFieldsResponse.put("126-510100", "constructField126IsoTranslate");
		transformFieldsResponse.put("126-510140", "constructField126IsoTranslate");
		transformFieldsResponse.put("126-510141", "constructField126IsoTranslate");
		
		
		transformFieldsResponseRev.put("3-510100", "transformField3ForCreditPaymentATM");
		transformFieldsResponseRev.put("3-510140", "transformField3ForCreditPaymentATM");
		transformFieldsResponseRev.put("3-510100", "transformField3ForDepositATM");
		transformFieldsResponseRev.put("3-510140", "transformField3ForDepositATM");
		transformFieldsResponseRev.put("3-210110", "transformField3ForDepositATM");
		transformFieldsResponseRev.put("3-210120", "transformField3ForDepositATM");
		transformFieldsResponseRev.put("3-510141", "transformField3ForDepositATM");
//		transformFieldsResponseRev.put("41-011000", "constructAdditionalRspData");
//		transformFieldsResponseRev.put("41-012000", "constructAdditionalRspData");
//		transformFieldsResponseRev.put("41-401000", "constructAdditionalRspData");
//		transformFieldsResponseRev.put("41-402000", "constructAdditionalRspData");
//		transformFieldsResponseRev.put("41-502000", "constructAdditionalRspData");
//		transformFieldsResponseRev.put("41-501000", "constructAdditionalRspData");
		transformFieldsResponseRev.put("41-501043", "constructAdditionalRspData");
		transformFieldsResponseRev.put("41-502043", "constructAdditionalRspData");
		transformFieldsResponseRev.put("95-012000", "constructReplacementAmountsZero");
		transformFieldsResponseRev.put("95-401000", "constructReplacementAmountsZero");
		transformFieldsResponseRev.put("95-402000", "constructReplacementAmountsZero");
		transformFieldsResponseRev.put("95-011000", "constructReplacementAmountsZero");
		transformFieldsResponseRev.put("95-501000", "constructReplacementAmountsZero");
		transformFieldsResponseRev.put("95-502000", "constructReplacementAmountsZero");
		
		
		
		
		
		
		
		
		createFieldsResponse.put("17-321000_321000", "constructDateCapture");
		createFieldsResponse.put("17-321000_401010", "constructDateCapture");
		createFieldsResponse.put("17-321000_401020", "constructDateCapture");
		createFieldsResponse.put("17-321000_501030", "constructDateCapture");
		createFieldsResponse.put("17-322000_322000", "constructDateCapture");
		createFieldsResponse.put("17-322000_402010", "constructDateCapture");
		createFieldsResponse.put("17-322000_402020", "constructDateCapture");
		createFieldsResponse.put("17-322000_502030", "constructDateCapture");
		createFieldsResponse.put("17-501000", "constructDateCapture");
		createFieldsResponse.put("17-502000", "constructDateCapture");
		createFieldsResponse.put("17-011000", "constructDateCapture");
		createFieldsResponse.put("17-012000", "constructDateCapture");
		createFieldsResponse.put("17-210110", "constructDateCapture");
		createFieldsResponse.put("17-210120", "constructDateCapture");
		createFieldsResponse.put("17-320100_270100", "constructDateCapture");
		createFieldsResponse.put("17-320100_270110", "constructDateCapture");
		createFieldsResponse.put("17-320100_270120", "constructDateCapture");
		createFieldsResponse.put("17-320100_270130", "constructDateCapture");
		createFieldsResponse.put("17-320100_270140", "constructDateCapture");
		createFieldsResponse.put("17-320100_270141", "constructDateCapture");
		createFieldsResponse.put("17-321000_011000", "constructDateCapture");
		createFieldsResponse.put("17-321000_501000", "constructDateCapture");
		createFieldsResponse.put("17-321000_501040", "constructDateCapture");
		createFieldsResponse.put("17-321000_501041", "constructDateCapture");
		createFieldsResponse.put("17-321000_501042", "constructDateCapture");
		createFieldsResponse.put("17-322000_012000", "constructDateCapture");
		createFieldsResponse.put("17-322000_502000", "constructDateCapture");
		createFieldsResponse.put("17-322000_502040", "constructDateCapture");
		createFieldsResponse.put("17-322000_502041", "constructDateCapture");
		createFieldsResponse.put("17-322000_502042", "constructDateCapture");
		createFieldsResponse.put("17-324000_314000", "constructDateCapture");
		createFieldsResponse.put("17-324000_404010", "constructDateCapture");
		createFieldsResponse.put("17-324000_404020", "constructDateCapture");
		createFieldsResponse.put("17-510100", "constructDateCapture");
		createFieldsResponse.put("17-510140", "constructDateCapture");
		createFieldsResponse.put("17-510141", "constructDateCapture");
		createFieldsResponse.put("38-011000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-012000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-210110", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-210120", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-314000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-320000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-320100_270130", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000_311000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000_381000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000_401010", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000_401020", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000_501000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000_501030", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000_501041", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-322000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-322000_312000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-322000_382000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-322000_402010", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-322000_402020", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-322000_502000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-322000_502030", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-324000_314000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-381000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-382000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-401000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-401010", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-401020", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-402000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-402010", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-402020", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-404010", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-404020", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-501000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-501030", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-501040", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-501041", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-501042", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-502000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-502030", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-502040", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-502041", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-502042", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-510141", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-320100_270100", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-320100_270110", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-320100_270120", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-320100_270140", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-320100_270141", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000_011000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000_321000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-321000_401000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-322000_012000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-322000_322000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-322000_402000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-324000_404010", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-324000_404020", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-510100", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-510140", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-311000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("38-312000", "constructField38DefaultOrCopy");
		createFieldsResponse.put("44-222222", "constructField44");
		createFieldsResponse.put("44-314000", "constructField44");
		createFieldsResponse.put("44-501030", "constructField44");
		createFieldsResponse.put("44-501040", "constructField44");
		createFieldsResponse.put("44-501041", "constructField44");
		createFieldsResponse.put("44-501042", "constructField44");
		createFieldsResponse.put("44-502030", "constructField44");
		createFieldsResponse.put("44-502040", "constructField44");
		createFieldsResponse.put("44-502041", "constructField44");
		createFieldsResponse.put("44-502042", "constructField44");
		createFieldsResponse.put("44-011000", "constuctServiceRspData");
		createFieldsResponse.put("44-311000", "constructField44");
		createFieldsResponse.put("44-312000", "constructField44");
		createFieldsResponse.put("44-320100_270100", "constructField44");
		createFieldsResponse.put("44-320100_270140", "constructField44");
		createFieldsResponse.put("44-502000", "constructField44");
		createFieldsResponse.put("44-510100", "constructField44");
		createFieldsResponse.put("44-510140", "constructField44");
		createFieldsResponse.put("44-510141", "constructField44");
		createFieldsResponse.put("44-501000", "constructField44");
		createFieldsResponse.put("52-210110", "constructPinDataToBase24Response");
		createFieldsResponse.put("52-210120", "constructPinDataToBase24Response");
		createFieldsResponse.put("52-320100_270100", "constructPinDataToBase24Response");
		createFieldsResponse.put("52-320100_270140", "constructPinDataToBase24Response");
		createFieldsResponse.put("52-510100", "constructPinDataToBase24Response");
		createFieldsResponse.put("52-510140", "constructPinDataToBase24Response");
		createFieldsResponse.put("52-510141", "constructPinDataToBase24Response");
		createFieldsResponse.put("54-321000_501030", "constructField54");
		createFieldsResponse.put("54-322000_502030", "constructField54");
//		createFieldsResponse.put("54-401000", "constructField54");
//		createFieldsResponse.put("54-402000", "constructField54");
		createFieldsResponse.put("61-321000_381000", "constructField61");
		createFieldsResponse.put("61-322000_382000", "constructField61");
		createFieldsResponse.put("61-381000", "constructField61");
		createFieldsResponse.put("61-382000", "constructField61");
		createFieldsResponse.put("61-210110", "constructField61ByDefault");
		createFieldsResponse.put("61-210120", "constructField61ByDefault");
		createFieldsResponse.put("61-320100_270100", "constructField61ByDefault");
		createFieldsResponse.put("61-320100_270110", "constructField61ByDefault");
		createFieldsResponse.put("61-320100_270120", "constructField61ByDefault");
		createFieldsResponse.put("61-320100_270130", "constructField61ByDefault");
		createFieldsResponse.put("61-320100_270140", "constructField61ByDefault");
		createFieldsResponse.put("61-320100_270141", "constructField61ByDefault");
		createFieldsResponse.put("61-321000_401010", "constructField61ByDefault");
		createFieldsResponse.put("61-321000_401020", "constructField61ByDefault");
		createFieldsResponse.put("61-321000_401000", "constructField61ByDefault");
		createFieldsResponse.put("61-322000_402000", "constructField61ByDefault");
		createFieldsResponse.put("61-321000_501000", "constructField61ByDefault");
		createFieldsResponse.put("61-321000_501030", "constructField61ByDefault");
		createFieldsResponse.put("61-321000_501040", "constructField61ByDefault");
		createFieldsResponse.put("61-321000_501041", "constructField61ByDefault");
		createFieldsResponse.put("61-321000_501042", "constructField61ByDefault");
		createFieldsResponse.put("61-322000_402010", "constructField61ByDefault");
		createFieldsResponse.put("61-322000_402020", "constructField61ByDefault");
		createFieldsResponse.put("61-322000_502000", "constructField61ByDefault");
		createFieldsResponse.put("61-322000_502030", "constructField61ByDefault");
		createFieldsResponse.put("61-322000_502040", "constructField61ByDefault");
		createFieldsResponse.put("61-322000_502041", "constructField61ByDefault");
		createFieldsResponse.put("61-322000_502042", "constructField61ByDefault");
		createFieldsResponse.put("61-324000_404010", "constructField61ByDefault");
		createFieldsResponse.put("61-324000_404020", "constructField61ByDefault");
		createFieldsResponse.put("61-401000", "constructField61ByDefault");
		createFieldsResponse.put("61-401010", "constructField61ByDefault");
		createFieldsResponse.put("61-401020", "constructField61ByDefault");
		createFieldsResponse.put("61-402000", "constructField61ByDefault");
		createFieldsResponse.put("61-402010", "constructField61ByDefault");
		createFieldsResponse.put("61-402020", "constructField61ByDefault");
		createFieldsResponse.put("61-404010", "constructField61ByDefault");
		createFieldsResponse.put("61-404020", "constructField61ByDefault");
		createFieldsResponse.put("61-501030", "constructField61ByDefault");
		createFieldsResponse.put("61-501040", "constructField61ByDefault");
		createFieldsResponse.put("61-501041", "constructField61ByDefault");
		createFieldsResponse.put("61-501042", "constructField61ByDefault");
		createFieldsResponse.put("61-502000", "constructField61ByDefault");
		createFieldsResponse.put("61-502030", "constructField61ByDefault");
		createFieldsResponse.put("61-502040", "constructField61ByDefault");
		createFieldsResponse.put("61-502041", "constructField61ByDefault");
		createFieldsResponse.put("61-502042", "constructField61ByDefault");
		createFieldsResponse.put("61-510100", "constructField61ByDefault");
		createFieldsResponse.put("61-510140", "constructField61ByDefault");
		createFieldsResponse.put("61-510141", "constructField61ByDefault");
		createFieldsResponse.put("61-501000", "constructField61ByDefault");
		createFieldsResponse.put("61-321000_401000", "constructField61ByDefault");
		createFieldsResponse.put("61-322000_402000", "constructField61ByDefault");
//		createFieldsResponse.put("62-401000", "constructFieldFromStructuredData");
//		createFieldsResponse.put("62-402000", "constructFieldFromStructuredData");
		createFieldsResponse.put("62-210110", "constructField62InDeclinedResponse");
		createFieldsResponse.put("62-210120", "constructField62InDeclinedResponse");
		createFieldsResponse.put("62-320100_270100", "constructField62InDeclinedResponse");
		createFieldsResponse.put("62-320100_270140", "constructField62InDeclinedResponse");
		createFieldsResponse.put("62-510100", "constructField62InDeclinedResponse");
		createFieldsResponse.put("62-510140", "constructField62InDeclinedResponse");
		createFieldsResponse.put("62-510141", "constructField62InDeclinedResponse");
		createFieldsResponse.put("62-381000", "constructField62Last5Mov");
		createFieldsResponse.put("62-382000", "constructField62Last5Mov");
		createFieldsResponse.put("63-011000", "constructResponseCodeField63");
		createFieldsResponse.put("63-321000", "constructResponseCodeField63");
		createFieldsResponse.put("63-321000_011000", "constructResponseCodeField63");
		createFieldsResponse.put("63-321000_321000", "constructResponseCodeField63");
		createFieldsResponse.put("63-321000_501030", "constructResponseCodeField63");
		createFieldsResponse.put("63-322000", "constructResponseCodeField63");
		createFieldsResponse.put("63-322000_012000", "constructResponseCodeField63");
		createFieldsResponse.put("63-322000_322000", "constructResponseCodeField63");
		createFieldsResponse.put("63-322000_502030", "constructResponseCodeField63");
		createFieldsResponse.put("63-321000_401010", "constructResponseCodeField63");
		createFieldsResponse.put("63-321000_401020", "constructResponseCodeField63");
		createFieldsResponse.put("63-321000_501040", "constructResponseCodeField63");
		createFieldsResponse.put("63-321000_501042", "constructResponseCodeField63");
		createFieldsResponse.put("63-322000_402010", "constructResponseCodeField63");
		createFieldsResponse.put("63-322000_402020", "constructResponseCodeField63");
		createFieldsResponse.put("63-322000_502040", "constructResponseCodeField63");
		createFieldsResponse.put("63-322000_502041", "constructResponseCodeField63");
		createFieldsResponse.put("63-322000_502042", "constructResponseCodeField63");
		createFieldsResponse.put("63-501000", "constructResponseCodeField63");
		createFieldsResponse.put("63-502000", "constructResponseCodeField63");
		createFieldsResponse.put("63-321000_501000", "constructResponseCodeField63");
		createFieldsResponse.put("63-321000_501041", "constructResponseCodeField63");
		createFieldsResponse.put("63-322000_502000", "constructResponseCodeField63");
		createFieldsResponse.put("100-401010", "constructField100ACH");
		createFieldsResponse.put("100-401020", "constructField100ACH");
		createFieldsResponse.put("100-402010", "constructField100ACH");
		createFieldsResponse.put("100-402020", "constructField100ACH");
		createFieldsResponse.put("100-210110", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-210120", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-320100_270100", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-320100_270110", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-320100_270120", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-320100_270130", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-320100_270140", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-320100_270141", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-321000_501000", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-321000_501030", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-321000_501040", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-321000_501041", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-321000_501042", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-322000_502000", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-322000_502030", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-322000_502040", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-322000_502041", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-322000_502042", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-324000_404010", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-324000_404020", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-401000", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-402000", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-404010", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-404020", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-501030", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-501040", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-501041", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-501042", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-502000", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-502030", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-502040", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-502041", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-502042", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-510100", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-510140", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-510141", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-501000", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-321000_401000", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("100-322000_402000", "constructField100DefaultInstitutionID");
		createFieldsResponse.put("102-321000_321000", "constructField102DefaultOrCopy");
		createFieldsResponse.put("102-322000_322000", "constructField102DefaultOrCopy");
		createFieldsResponse.put("102-320000", "constructField102_103ConsultaCosto");
		createFieldsResponse.put("102-321000", "constructField102_103ConsultaCosto");
		createFieldsResponse.put("102-322000", "constructField102_103ConsultaCosto");
		createFieldsResponse.put("102-321000_311000", "constructField102_103ConsultaCosto");
		createFieldsResponse.put("102-322000_312000", "constructField102_103ConsultaCosto");
		createFieldsResponse.put("102-322000_402000", "constructField102DefaultOrCopy");
		createFieldsResponse.put("102-321000_401000", "constructField102DefaultOrCopy");
		createFieldsResponse.put("102-401000", "constructField102toB24");
		createFieldsResponse.put("102-402000", "constructField102toB24");
		createFieldsResponse.put("104-314000", "constructDefaultField104");
		createFieldsResponse.put("104-320000", "constructDefaultField104");
		createFieldsResponse.put("104-404010", "constructDefaultField104");
		createFieldsResponse.put("104-404020", "constructDefaultField104");
		createFieldsResponse.put("104-314000", "constructField104Credit");
		createFieldsResponse.put("104-404010", "constructField104Credit");
		createFieldsResponse.put("104-404020", "constructField104Credit");
		createFieldsResponse.put("104-321000_401010", "construct0210ErrorFields");
		createFieldsResponse.put("104-321000_401020", "construct0210ErrorFields");
		createFieldsResponse.put("104-322000_402010", "construct0210ErrorFields");
		createFieldsResponse.put("104-322000_402020", "construct0210ErrorFields");
		createFieldsResponse.put("104-324000_314000", "constructDefaultField104");
		createFieldsResponse.put("104-324000_404010", "constructDefaultField104");
		createFieldsResponse.put("104-324000_404020", "constructDefaultField104");
		createFieldsResponse.put("104-320100_270130", "constructField104Credit");
		createFieldsResponse.put("105-321000_501000", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-321000_501030", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-321000_501040", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-321000_501041", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-321000_501042", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-322000_502000", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-322000_502030", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-322000_502040", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-322000_502041", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-322000_502042", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-401000", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-402000", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-501030", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-501040", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-501041", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-501042", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-502000", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-502030", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-502040", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-502041", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-502042", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-501000", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-321000_401000", "constructField105DefaultOrCopy");
		createFieldsResponse.put("105-322000_402000", "constructField105DefaultOrCopy");
		
		
		
		createFieldsResponseAdv.put("17", "constructOriginalFieldMsg");
		createFieldsResponseAdv.put("38", "constructAuthorizationIdResponse");
		createFieldsResponseAdv.put("48", "constructOriginalFieldMsg");
		createFieldsResponseAdv.put("100", "construct0210ErrorFields");
		
		
//		createFieldsResponseRev.put("48-401000", "constructFieldFromTimedHashTable");
//		createFieldsResponseRev.put("48-402000", "constructFieldFromTimedHashTable");
//		createFieldsResponseRev.put("54-401000", "constructDefaultField54");
//		createFieldsResponseRev.put("54-402000", "constructDefaultField54");
//		createFieldsResponseRev.put("95-510100", "constructReplacementAmounts");
//		createFieldsResponseRev.put("95-510140", "constructReplacementAmounts");
//		createFieldsResponseRev.put("95-510141", "constructReplacementAmounts");
		createFieldsResponseRev.put("112-501043", "constructField112");
		createFieldsResponseRev.put("112-502043", "constructField112");

		
		
		
		
		transformFieldsMultipleCases.put("3", "3");
		
		transformFields.put("3", "N/A");
		transformFields.put("3-270110", "transformField3ForDepositATM");
		transformFields.put("3-270120", "transformField3ForDepositATM");
		transformFields.put("3-270100", "transformField3ForCreditPaymentATM");
		transformFields.put("3-270140", "transformField3ForCreditPaymentATM");
		transformFields.put("3-270141", "transformField3ForCreditPaymentATM");
		transformFields.put("3-890000", "constructProcessingCode");
		transformFields.put("15", "compensationDateValidationP17ToP15");
		transformFields.put("32", "constructAcquiringInstitutionIDCodeToTM");
		transformFields.put("41", "constructCardAcceptorTermIdToTranmgr");
		transformFields.put("52", "constructPinDataToTranmgr");
		transformFields.put("54", "constructAdditionalAmounts");
		transformFields.put("28", "transformAmountFeeFields");
		transformFields.put("30", "transformAmountFeeFields");
		transformFields.put("95", "constructReplacementAmounts");
		transformFields.put("100", "constructField100");
		
		
		
		
		skipCopyFields.put("48", "48");
		skipCopyFields.put("102", "102");
		skipCopyFields.put("103", "103");
		skipCopyFields.put("104", "104");
		skipCopyFields.put("105", "105");
		
		copyFieldsResponse.put("3", "3");
		copyFieldsResponse.put("4", "4");
		copyFieldsResponse.put("7", "7");
		copyFieldsResponse.put("11", "11");
		copyFieldsResponse.put("12", "12");
		copyFieldsResponse.put("13", "13");
		copyFieldsResponse.put("15", "15");
		copyFieldsResponse.put("22", "22");
		copyFieldsResponse.put("32", "32");
		copyFieldsResponse.put("35", "35");
		copyFieldsResponse.put("37", "37");
		copyFieldsResponse.put("38", "38");
		copyFieldsResponse.put("39", "39");
		copyFieldsResponse.put("48", "48");
		copyFieldsResponse.put("49", "49");
		copyFieldsResponse.put("103", "103");
		
		
		copyFieldsResponseAdv.put("3", "3");
		copyFieldsResponseAdv.put("4", "4");
		copyFieldsResponseAdv.put("7", "7");
		copyFieldsResponseAdv.put("11", "11");
		copyFieldsResponseAdv.put("12", "12");
		copyFieldsResponseAdv.put("13", "13");
		copyFieldsResponseAdv.put("15", "15");
		copyFieldsResponseAdv.put("22", "22");
		copyFieldsResponseAdv.put("32", "32");
		copyFieldsResponseAdv.put("35", "35");
		copyFieldsResponseAdv.put("37", "37");
		copyFieldsResponseAdv.put("39", "39");
		copyFieldsResponseAdv.put("49", "49");
		copyFieldsResponseAdv.put("90", "90");
		copyFieldsResponseAdv.put("102", "102");
		
		copyFieldsResponseRev.put("3", "3");
		copyFieldsResponseRev.put("4", "4");
		copyFieldsResponseRev.put("7", "7");
		copyFieldsResponseRev.put("11", "11");
		copyFieldsResponseRev.put("22", "22");
		copyFieldsResponseRev.put("32", "32");
		copyFieldsResponseRev.put("35", "35");
		copyFieldsResponseRev.put("37", "37");
		copyFieldsResponseRev.put("39", "39");
		//copyFieldsResponseRev.put("41", "41");
		copyFieldsResponseRev.put("48", "48");
		copyFieldsResponseRev.put("49", "49");
		copyFieldsResponseRev.put("54", "54");
		copyFieldsResponseRev.put("90", "90");
		copyFieldsResponseRev.put("95", "95");
		
		
		
		
		
		deleteFieldsResponse.put("011000", "104");
		deleteFieldsResponse.put("012000", "104");
		deleteFieldsResponse.put("401000", "15-40-44");
		deleteFieldsResponse.put("402000", "15-40-44");
		deleteFieldsResponse.put("321000", "44-54-62-100-104-105");
		deleteFieldsResponse.put("322000", "44-54-62-100-104-105");
		deleteFieldsResponse.put("501041", "15");
		
		
		
		
		deleteFieldsResponseAdv.put("28", "28");
		deleteFieldsResponseAdv.put("30", "30");
		
		deleteFieldsResponseRev.put("17", "17");
		deleteFieldsResponseRev.put("102", "102");
		deleteFieldsResponseRev.put("103", "103");
		deleteFieldsResponseRev.put("104", "104");
		
		
		deleteFieldsResponseRev.put("401000", "100-61-60");
		deleteFieldsResponseRev.put("402000", "100-61-60");
		deleteFieldsResponseRev.put("401010", "61");
		deleteFieldsResponseRev.put("401020", "61");
		deleteFieldsResponseRev.put("402010", "61");
		deleteFieldsResponseRev.put("402020", "61");
		

		

		


		
		

		migratedOpCodes.put("201000", "201000");
		migratedOpCodes.put("202000", "202000");

		migratedOpCodes.put("001000", "001000");
		migratedOpCodes.put("002000", "002000");
		migratedOpCodes.put("401010", "401010");
		migratedOpCodes.put("401020", "401020");
		migratedOpCodes.put("402010", "402010");
		migratedOpCodes.put("402020", "402020");

		migratedOpCodes.put("322000", "322000");
		migratedOpCodes.put("321000", "321000");

		migratedOpCodes.put("381000", "381000");
		migratedOpCodes.put("382000", "382000");
		migratedOpCodes.put("890000", "890000");
		migratedOpCodes.put("402000", "402000");
		migratedOpCodes.put("401000", "401000");
		migratedOpCodes.put("011000", "011000");
		migratedOpCodes.put("012000", "012000");
		migratedOpCodes.put("314000", "314000");
		migratedOpCodes.put("311000", "311000");
		migratedOpCodes.put("312000", "312000");

		migratedOpCodes.put("401024", "401024");
		migratedOpCodes.put("401014", "401014");
		migratedOpCodes.put("402024", "402024");
		migratedOpCodes.put("402014", "402014");
		migratedOpCodes.put("404010", "404010");
		migratedOpCodes.put("404020", "404020");
		migratedOpCodes.put("501043", "501043");
		migratedOpCodes.put("502043", "502043");
		
		migratedOpCodes.put("401410", "401410");
		migratedOpCodes.put("402410", "402410");
		migratedOpCodes.put("401420", "401420");
		migratedOpCodes.put("402420", "402420");


		migratedOpCodes.put("501030", "501030");
		migratedOpCodes.put("502030", "502030");

		migratedOpCodes.put("270110", "270110");
		migratedOpCodes.put("270120", "270120");

		migratedOpCodes.put("270100", "270100");
		migratedOpCodes.put("270140", "270140");
		//migratedOpCodes.put("270141", "270141");

		migratedOpCodes.put("501000", "501000");
		migratedOpCodes.put("502000", "502000");

		migratedOpCodes.put("501040", "501040");
		migratedOpCodes.put("502040", "502040");
		migratedOpCodes.put("501041", "501041");
		migratedOpCodes.put("502041", "502041");
		migratedOpCodes.put("500100", "500100");
		migratedOpCodes.put("500200", "500200");
		migratedOpCodes.put("500130", "500130");
		migratedOpCodes.put("500230", "500230");
		migratedOpCodes.put("500140", "500140");
		migratedOpCodes.put("500240", "500240");

		migratedOpCodes.put("500141", "500141");
		migratedOpCodes.put("500241", "500241");
		migratedOpCodes.put("501042", "501042");
		migratedOpCodes.put("502042", "502042");
		migratedOpCodes.put("270130", "270130");

		migratedOpCodes.put("950000", "950000");

		migratedBins.put("008823", "008823");
		migratedBins.put("008802", "008802");
		migratedBins.put("008852", "008852");
		migratedBins.put("530710", "530710");
//		migratedBins.put("777790", "777790");
//		migratedBins.put("777760", "777760");
//		migratedBins.put("777791", "777791");

		migratedCards.put("4576030000030265", "4576030000030265");
		migratedCards.put("4509420010043782", "4509420010043782");
		migratedCards.put("4915110205551966", "4915110205551966");
		migratedCards.put("6013679017727259", "6013679017727259");
		migratedCards.put("5307100090056620", "5307100090056620");
		migratedCards.put("4546167323769315", "4546167323769315");
		migratedCards.put("4000020000003129", "4000020000003129");
		migratedCards.put("7425300031761634", "7425300031761634");
		migratedCards.put("4915110200100637", "4915110200100637");
		migratedCards.put("6013679000029721", "6013679000029721");

		migratedCards.put("4546167323765636", "4546167323765636");
		migratedCards.put("5307100090056646", "5307100090056646");
		migratedCards.put("5185030000004575", "5185030000004575");

		migratedCards.put("4215440000106477", "4215440000106477");
		migratedCards.put("4509420000958882", "4509420000958882");
		migratedCards.put("4573200000164587", "4573200000164587");
		migratedCards.put("4573200000164595", "4573200000164595");
		migratedCards.put("4573210000093389", "4573210000093389");
		migratedCards.put("4573210000096234", "4573210000096234");
		migratedCards.put("4573210000141873", "4573210000141873");
		migratedCards.put("4576020000066930", "4576020000066930");
		migratedCards.put("4576030000158140", "4576030000158140");
		migratedCards.put("4576030000160633", "4576030000160633");
		migratedCards.put("4576040000143455", "4576040000143455");
		migratedCards.put("4576050000041252", "4576050000041252");
		migratedCards.put("4646200115001685", "4646200115001685");
		migratedCards.put("4915110205374526", "4915110205374526");
		migratedCards.put("4915110205386504", "4915110205386504");
		migratedCards.put("4915110205386520", "4915110205386520");

		migratedCards.put("4915110205552006", "4915110205552006");
		migratedCards.put("4915110205552014", "4915110205552014");
		migratedCards.put("4915110205553905", "4915110205553905");
		migratedCards.put("4915110205552220", "4915110205552220");

		migratedCards.put("4576020000066922", "4576020000066922");

		migratedCards.put("5454541234567890", "5454541234567890");
		migratedCards.put("4473990520002999", "4473990520002999");
		migratedCards.put("4573210000096242", "4573210000096242");
		migratedCards.put("4915110200103953", "4915110200103953");
		migratedCards.put("4576020000066914", "4576020000066914");
		
		migratedCards.put("4915110205553442", "4915110205553442");
		migratedCards.put("4915110205553392", "4915110205553392");
		migratedCards.put("4915110205553426", "4915110205553426");
		migratedCards.put("4915110205553467", "4915110205553467");
		migratedCards.put("4915110205553418", "4915110205553418");
		migratedCards.put("4915110200114703", "4915110200114703");
		migratedCards.put("4000020000003129", "4000020000003129");
		
		


		migratedOpCodes.put("500100", "500100");
		migratedOpCodes.put("890000", "890000");

		migratedOpCodes.put("011000", "011000");
		migratedOpCodes.put("012000", "012000");
		migratedOpCodes.put("311000", "311000");
		migratedOpCodes.put("312000", "312000");
		migratedOpCodes.put("381000", "381000");
		migratedOpCodes.put("382000", "382000");

		migratedOpCodesAtm.put("011000", "011000");
		migratedOpCodesAtm.put("012000", "012000");
		migratedOpCodesAtm.put("311000", "311000");
		migratedOpCodesAtm.put("312000", "312000");
		migratedOpCodesAtm.put("381000", "381000");
		migratedOpCodesAtm.put("382000", "382000");
		migratedOpCodesAtm.put("890000", "890000");

		migratedBins.put("457602", "457602");
		migratedBins.put("450942", "450942");
		migratedBins.put("421544", "421544");
		migratedBins.put("491614", "491614");
		migratedBins.put("457320", "457320");
		migratedBins.put("457605", "457605");
		migratedBins.put("457603", "457603");
		migratedBins.put("464620", "464620");
		migratedBins.put("457321", "457321");
		migratedBins.put("457604", "457604");



		migratedCards.put("4915110114372017", "4915110114372017");
		migratedCards.put("4915110099198007", "4915110099198007");
		migratedCards.put("4915110111298280", "4915110111298280");
		migratedCards.put("4576030009659056", "4576030009659056");
		migratedCards.put("4509420001964350", "4509420001964350");
		migratedCards.put("4576030003416800", "4576030003416800");

		getLogger().logLine("#=== Enter to [Init] Method Interchange " + interchange.getName() + " ===#");

		//String[] userParams = Pack.splitParams(interchange.getUserParameter());
//		acquirersNetwork = postilion.realtime.genericinterface.translate.database.DBHandler.getAcquirerDesc();
		this.nameInterface = interchange.getName();
//		String[] logParms = { nameInterface, Integer.toString(Constants.RuntimeParm.NR_OF_PARAMETERS_EXPECTED),
//				Integer.toString(userParams.length), interchange.getUserParameter() };
//
//		if (userParams.length != Constants.RuntimeParm.NR_OF_PARAMETERS_EXPECTED) {
//			EventRecorder.recordEvent(new IncorrectRuntimeParameters(new String[] { interchange.getName(),
//					Integer.toString(Constants.RuntimeParm.NR_OF_PARAMETERS_EXPECTED),
//					Integer.toString(userParams.length), interchange.getUserParameter() }));
//			throw new XPostilion(new IncorrectRuntimeParameters(
//					new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
//		}
		getParameters();
		//validateParameters(userParams);
		udpClient = new Client(ipUdpServer, portUdpServer);


//		structureContent = DBHandler.getConfigTypeMsg();
//
//		if (structureContent.size() == 0) {
//			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
//					GenericInterface.class.getName(), "Method: [init]",
//					"Error to load the database configuration. The interface not working without the messages configuration. Map structureContent",
//					"N/A" }));
//			throw new XPostilion(
//					new TryCatchException(new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
//		}
//
//		allCodesIsoToB24 = DBHandler.getResponseCodes(false, "0");
//
//		if (allCodesIsoToB24.size() == 0) {
//			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
//					GenericInterface.class.getName(), "Method: [init]",
//					"Error to load the database configuration. The interface not working without the messages configuration. Map allCodesIsoToB24",
//					"N/A" }));
//			throw new XPostilion(
//					new TryCatchException(new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
//		}
//
//		allCodesIscToIso = DBHandler.getResponseCodes(true, "0");
//		if (allCodesIscToIso.size() == 0) {
//			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
//					GenericInterface.class.getName(), "Method: [init]",
//					"Error to load the database configuration. The interface not working without the messages configuration. Map allCodesIscToIso",
//					"N/A" }));
//			throw new XPostilion(
//					new TryCatchException(new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
//		}
//
//		institutionid = postilion.realtime.genericinterface.translate.database.DBHandler.setInstitutionIds();
//		if (institutionid.size() == 0) {
//			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
//					GenericInterface.class.getName(), "Method: [init]",
//					"Error to load the database configuration. The interface not working without the messages configuration. Map institutionid",
//					"N/A" }));
//			throw new XPostilion(
//					new TryCatchException(new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
//		}
//
////		validator = Validation.getInstance();
//
//		allCodesIsoToB24TM = DBHandler.getResponseCodes(false, "1");
//		if (allCodesIsoToB24TM.size() == 0) {
//			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
//					GenericInterface.class.getName(), "Method: [init]",
//					"Error to load the database configuration. The interface not working without the messages configuration. Map allCodesIsoToB24TM",
//					"N/A" }));
//			throw new XPostilion(
//					new TryCatchException(new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
//		}
//
//		allCodesB24ToIso = DBHandler.getResponseCodesBase24("1");
//		if (allCodesB24ToIso.size() == 0) {
//			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
//					GenericInterface.class.getName(), "Method: [init]",
//					"Error to load the database configuration. The interface not working without the messages configuration. Map allCodesB24ToIso",
//					"N/A" }));
//			throw new XPostilion(
//					new TryCatchException(new AContext[] { new ApplicationContext(interchange.getName()) }, logParms));
//		}

		params = new Parameters(kwa, sourceTranToTmHashtable, sourceTranToTmHashtableB24, issuerId,udpClient,
				nameInterface);

	}

	/************************************************************************************
	 * Metodo que valida los parametros de usuario de la interchange
	 *
	 * @param userParameters arreglo con los parametros de usuario
	 * @throws XNodeParameterValueInvalid
	 *
	 ************************************************************************************/
	public void validateParameters(String[] userParameters) throws XNodeParameterValueInvalid {
		try {

			String cfgRetentionPeriod = userParameters[Constants.Indexes.RETENTION_PERIOD];
			String cfgValidateMAC = userParameters[Constants.Indexes.VALIDATE_MAC];
			String cfgKwaName = userParameters[Constants.Indexes.KWA_NAME];
			sSignOn = userParameters[Constants.Indexes.SEND_SIGN_ON];
//			businessValidation = userParameters[Constants.Indexes.BUSINESS_VALIDATION].equals(General.TRUE);
//			create0220ToTM = userParameters[Constants.Indexes.CREATE_0220].equals(General.TRUE);
			issuerId = userParameters[Constants.Indexes.ISSUERID];
			//String cfgIpUdpServer = userParameters[Constants.Indexes.IP_UDP_SERVER];
			//String cfgPortUdpServer = userParameters[Constants.Indexes.PORT_UDP_SERVER];
			//consultCovenants = userParameters[Constants.Indexes.CONSULT_COVENANTS].equals(General.TRUE);
			//encodeData = userParameters[Constants.Indexes.ENCODE_DATA].equalsIgnoreCase(General.TRUE);
			//routingField100 = userParameters[Constants.Indexes.ROUTING_FIELD_100];
			//exeptionValidateExpiryDate = false;
			//urlCutWS = userParameters[Constants.Indexes.URL_CUT_WS];

			//exeptionValidateExpiryDate = userParameters[Constants.Indexes.EXEPTION_EXPIRY_DATE].equals(General.TRUE);


			// exeptionValidateExpiryDate =
			// userParameters[Constants.Indexes.EXEPTION_EXPIRY_DATE].equals(General.TRUE);

			//String cfgIpServer = userParameters[Constants.Indexes.IP_UDP_SERVER_TOKEN_ACTIVATION];
			//String cfgPortServer = userParameters[Constants.Indexes.PORT_UDP_SERVER_TOKEN_ACTIVATION];

			if (cfgRetentionPeriod != null) {
				try {
					retentionPeriod = Long.parseLong(cfgRetentionPeriod);
				} catch (NumberFormatException e) {
					EventRecorder.recordEvent(
							new XNodeParameterValueInvalid(Constants.RuntimeParm.RETENTION_PERIOD, cfgRetentionPeriod));
					throw new XNodeParameterValueInvalid(Constants.RuntimeParm.RETENTION_PERIOD, cfgRetentionPeriod);

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
						} catch (XCrypto e) {
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

		

		} catch (Exception e) {
			EventRecorder.recordEvent(new InvalidMessage(
					new String[] { Constants.Config.NAME, "Method: [validateParameters]", e.getMessage() }));
			EventRecorder.recordEvent(e);
			getLogger().logLine("Exception in validateParameters: " + e.getMessage());
		}
	}
	
	public void getParameters() {
		
		
		try
		{
		JSONParser parser=new JSONParser();
		
		org.json.simple.JSONObject jsonObject=(org.json.simple.JSONObject) parser.parse(new InputStreamReader(getClass().getResourceAsStream("parameters.json")));
		
		
		
		String cfgRetentionPeriod = jsonObject.get("cfgRetentionPeriod").toString();
		String cfgValidateMAC = jsonObject.get("cfgValidateMAC").toString();
		String cfgKwaName = jsonObject.get("cfgKwaName").toString();
		sSignOn = jsonObject.get("sSignOn").toString();

		issuerId = jsonObject.get("issuerId").toString();
		String cfgIpUdpServer = jsonObject.get("cfgIpUdpServer").toString();
		String cfgPortUdpServer = jsonObject.get("cfgPortUdpServer").toString();
		boolean create0220ToTM = (boolean)jsonObject.get("create0220ToTM");
		
		
		if (cfgRetentionPeriod != null) {
			try {
				retentionPeriod = Long.parseLong(cfgRetentionPeriod);
			} catch (NumberFormatException e) {
				EventRecorder.recordEvent(
						new XNodeParameterValueInvalid(Constants.RuntimeParm.RETENTION_PERIOD, cfgRetentionPeriod));
				throw new XNodeParameterValueInvalid(Constants.RuntimeParm.RETENTION_PERIOD, cfgRetentionPeriod);

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
					} catch (XCrypto e) {
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
		
		this.ipUdpServer = validateIpUdpServerParameter(cfgIpUdpServer);
		this.portUdpServer = validatePortUdpServerParameter(cfgPortUdpServer);
		this.create0220ToTM=create0220ToTM;
		
		}
		catch(Exception e)
		{
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, GenericInterface.class.getName(),
							"getParameters:", Utils.getStringMessageException(e), "Unknown" }));
			EventRecorder.recordEvent(e);
		}
	
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
			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, GenericInterface.class.getName(),
							"Method: [processResyncCommand]", Utils.getStringMessageException(e), "N/A" }));
			EventRecorder.recordEvent(e);
		}
		return action;
	}
	
	/**
	 * 
	 * Validate parameter for connection to udp server
	 * 
	 * @param cfgIpUdpServer server's ip
	 * @throws XNodeParameterValueInvalid if parameter is invalid
	 */
	public String validateIpUdpServerParameter(String cfgIpUdpServer) throws XNodeParameterValueInvalid {
		String ip = null;
		if (cfgIpUdpServer != null && !cfgIpUdpServer.equals("0")) {
			if (Client.validateIp(cfgIpUdpServer)) {
				ip = cfgIpUdpServer;
			} else {
				EventRecorder.recordEvent(
						new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_IP_UDP_SERVER, cfgIpUdpServer));
				throw new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_IP_UDP_SERVER, cfgIpUdpServer);
			}
		} else {
			EventRecorder.recordEvent(
					new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_IP_UDP_SERVER, General.NULLSTRING));
			throw new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_IP_UDP_SERVER, General.NULLSTRING);
		}
		return ip;
	}

	/**
	 * 
	 * Validate parameter for connection to udp server
	 * 
	 * @param cfgPortUdpServer server's port
	 * @throws XNodeParameterValueInvalid if parameter is invalid
	 */
	public String validatePortUdpServerParameter(String cfgPortUdpServer) throws XNodeParameterValueInvalid {
		String port = null;
		if (cfgPortUdpServer != null && !cfgPortUdpServer.equals("0")) {
			if (Client.validatePort(cfgPortUdpServer)) {
				port = cfgPortUdpServer;
			} else {
				EventRecorder.recordEvent(new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_PORT_UDP_SERVER,
						cfgPortUdpServer));
				throw new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_PORT_UDP_SERVER, cfgPortUdpServer);
			}
		} else {
			EventRecorder.recordEvent(
					new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_PORT_UDP_SERVER, General.NULLSTRING));
			throw new XNodeParameterValueInvalid(Constants.RuntimeParm.VALIDATE_PORT_UDP_SERVER, General.NULLSTRING);
		}
		return port;
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
			String msgType = new String(data, 0, 3);
			BitmapMessage inMsg = null;
			if (msgType.equals("ISO")) {
				inMsg = new Base24Ath(kwa);
				inMsg.fromMsg(data);
				msg = inMsg;
			} else {
				inMsg = new Iso8583Post();
				inMsg.fromMsg(data);
				msg = inMsg;
			}

			getLogger().logLine("**MENSAJE**\n" + msg);
			exceptionMessage=Transform.fromBinToHex(Transform.getString(data));
			return msg;

		} catch (Exception e) {
			
			exceptionMessage=Transform.fromBinToHex(Transform.getString(data));
			udpClient.sendData(
					Client.getMsgKeyValue("N/A", "ERRISO30 Exception en Mensaje: " + exceptionMessage, "LOG", nameInterface));

			EventRecorder.recordEvent(
					new TryCatchException(new String[] { this.nameInterface, GenericInterface.class.getName(),
							"Method: [newMsg]", Utils.getStringMessageException(e), "Unknown" }));
			EventRecorder.recordEvent(e);
			udpClient.sendData(
					Client.getMsgKeyValue("N/A", "Exception in newMsg: " + e.getMessage(), "LOG", nameInterface));

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
			udpClient.sendData(Client.getMsgKeyValue("N/A",
					"RuntimeException in loadSourceNodeKey:\n" + e.getMessage() + "\n" + e.getLocalizedMessage(), "LOG",
					nameInterface));
			throw e;
		} catch (Exception e) {
			StringWriter outError = new StringWriter();
			e.printStackTrace(new PrintWriter(outError));
			String errorString = outError.toString();
			udpClient.sendData(Client.getMsgKeyValue("N/A", "Exception in loadSourceNodeKey:\n" + errorString, "LOG",
					nameInterface));
			return false;
		}
	}

	/**
	 * Valida si el String es una cadena Hexadecimal.
	 *
	 * @param hexString Cadena a validar.
	 * @return True si la cadena es una cadena hexadecimal.
	 */
	public static boolean isHexadecimal(String hexString) {
		if ((hexString.length() & 2) == 1)
			return false;
		char c;
		for (int i = 0; i < hexString.length(); i++) {
			c = hexString.charAt(i);
			if (!('0' <= c && c <= '9' || 'a' <= c && c <= 'f' || 'A' <= c && c <= 'F'))
				return false;
		}
		return true;
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
			
			putRecordIntoSourceToTmHashtableB24(
					msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), msgFromRemote);	
			
			Base24Ath msgFromRemoteT = new Base24Ath(kwa);
			Iso8583Post msgToTm = new Iso8583Post();
			Base24Ath msgToRemote = new Base24Ath(kwa);
			MessageTranslator translator = new MessageTranslator(params);

			Utils tool = new Utils(params);
			try {

				// Validación MAC
				int errMac = msgFromRemote.failedMAC();

				if (errMac == Base24Ath.MACError.INVALID_MAC_ERROR) {
					action = new Action(null, constructEchoMsgIndicatorFailedMAC(msgFromRemote, errMac), null, null);
				} else {

					if (!msg.isFieldSet(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID)) {
						msg.putField(Iso8583.Bit._041_CARD_ACCEPTOR_TERM_ID, Constants.General.DEFAULT_P41);
					}
					
					
					

					 Iso8583Post Isomsg =
					 translator.constructIso8583(msgFromRemote);
					 
			
					
					this.getLogger().logLine("MENSAJEIso8583Post:"+Isomsg.toString());
					
					action.putMsgToTranmgr(Isomsg);

					putRecordIntoSourceToTmHashtable(
							Isomsg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), Isomsg);
		

				}
			} catch (Exception e) {
				
				Iso8583Post msg220=translator.construct0220ToTm(msg, nameInterface);
				action.putMsgToTranmgr(msg220);

				e.printStackTrace();
				udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: processTranReqFromInterchange " + Utils.getStringMessageException(e),
						"LOG", nameInterface));
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						GenericInterface.class.getName(), "Method: [processTranReqFromInterchange]",
						Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));

				EventRecorder.recordEvent(e);
				
				udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in message: " + exceptionMessage,
						"LOG", nameInterface));

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
		udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"LA RESPUESTA 0210 TRAE ESTOS VALORES EN EL 102:" + msg.getField("102") + " y estos en el 103: "
						+ msg.getField("103"),
				"LOG", nameInterface));

		udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				Transform.fromBinToHex(Transform.getString(msg.toMsg())), "ISO", nameInterface));

		if (!msg.getField(Iso8583.Bit._039_RSP_CODE).equals("00"))
			udpClient
					.sendData(
							Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
									"DECISO" + msg.getField(Iso8583.Bit._039_RSP_CODE)
											+ Transform.fromBinToHex(Transform.getString(msg.toMsg())),
									"ISO", nameInterface));


		try {

			
				MessageTranslator translator = new MessageTranslator(params);
				
				

				
				Base24Ath msgToRemote2=translator.constructBase24((Iso8583Post) msg);
				udpClient.sendData(Client.getMsgKeyValue(msgToRemote2.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						Transform.fromBinToHex(Transform.getString(msgToRemote2.toMsg(false))), "B24", nameInterface));
				this.getLogger().logLine("210CONSTRUCTISO8583:"+msgToRemote2);


				action.putMsgToRemote(msgToRemote2);
			

		} catch (Exception e) {

			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30" + Transform.fromBinToHex(Transform.getString(msg.toMsg())), "ISO", nameInterface));
			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30 Exception en Mensaje: " + msg.toString(), "LOG", nameInterface));

			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					GenericInterface.class.getName(), "Method: [processTranReqRspFromTranmgr]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
		}
		return action;
	}
	
	@Override

	public Action processTranReqFromTranmgr(AInterchangeDriverEnvironment interchange, Iso8583Post msg)
			throws Exception {
		
	
		putRecordIntoSourceToTmHashtable(
				msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)+msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR), msg);
		
		MessageTranslator translator = new MessageTranslator(params);
		Base24Ath msgToRemote = translator.constructBase24Request((Iso8583Post) msg);
		

		Action action = new Action();
		
		action.putMsgToRemote(msgToRemote);
		return action;
	}
	
	@Override
	public Action processAcquirerRevAdvFromTranmgr(AInterchangeDriverEnvironment interchange, Iso8583Post msg)
			throws Exception {
		MessageTranslator translator = new MessageTranslator(params);
		Base24Ath msgToRemote = translator.constructBase24Request((Iso8583Post) msg);
		
		putRecordIntoSourceToTmHashtable(
				msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)+msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR), msg);
		Action action = new Action();
		
		action.putMsgToRemote(msgToRemote);
		return action;
		
	}
	
	@Override
	public Action processTranReqRspFromInterchange(AInterchangeDriverEnvironment interchange, Iso8583 msgFromRemote)
			throws Exception {
		
		Iso8583Post originalMsg = (Iso8583Post) sourceTranToTmHashtable
				.get(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)+msgFromRemote.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR));
		
		originalMsg.putMsgType(msgFromRemote.getMsgType());
		originalMsg.putField(Iso8583.Bit._038_AUTH_ID_RSP, msgFromRemote.getField((Iso8583.Bit._038_AUTH_ID_RSP)));
		originalMsg.putField(Iso8583.Bit._039_RSP_CODE, msgFromRemote.getField((Iso8583.Bit._039_RSP_CODE)));
		
		Action action = new Action();
		action.putMsgToTranmgr(originalMsg);
		
		return action;

		// return super.processTranReqRspFromInterchange(interchange, msg);
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
			if (originalMsg != null) {
				if (originalMsg.getMessageType().equals(Iso8583.MsgType.toString(MsgType._0220_TRAN_ADV))) {
					
	
					Base24Ath msgToRemote2=translator.constructBase24(msg);
	


					action.putMsgToRemote(msgToRemote2);
				}
				else if(originalMsg.getMessageType().equals(Iso8583.MsgType.toString(MsgType._0200_TRAN_REQ)))
				{
					originalMsg.putMsgType(MsgType._0210_TRAN_REQ_RSP);
					originalMsg.putField(Iso8583.Bit._039_RSP_CODE, "06");
					originalMsg.putField(Iso8583.Bit._038_AUTH_ID_RSP, "000000");
					
					action.putMsgToRemote(originalMsg);
				}
				else if(originalMsg.getMessageType().equals(Iso8583.MsgType.toString(MsgType._0420_ACQUIRER_REV_ADV)))
				{
					originalMsg.putMsgType(MsgType._0430_ACQUIRER_REV_ADV_RSP);
					originalMsg.putField(Iso8583.Bit._039_RSP_CODE, "06");
					originalMsg.putField(Iso8583.Bit._038_AUTH_ID_RSP, "000000");
					
					action.putMsgToRemote(originalMsg);
				}
			}
		} catch (Exception e) {
			
			
			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30" + Transform.fromBinToHex(Transform.getString(msg.toMsg())), "ISO", nameInterface));
			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30 Exception en Mensaje: " + msg.toString(), "LOG", nameInterface));
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					GenericInterface.class.getName(), "Method: [processTranAdvRspFromTranmgr]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
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
		Base24Ath msgToRemote = new Base24Ath(kwa);
		MessageTranslator translator = new MessageTranslator(params);
		try {
			Utils tool = new Utils(params);
			
			int errMac = msgFromRemote.failedMAC();
			if (errMac == Base24Ath.MACError.INVALID_MAC_ERROR) {
				action = new Action(null, constructEchoMsgIndicatorFailedMAC(msgFromRemote, errMac), null, null);
			} else {


				 Iso8583Post Isomsg =
				 translator.constructIso8583(msgFromRemote);
				 
				
				
				this.getLogger().logLine("MENSAJEIso8583Post:"+Isomsg.toString());
				
				action.putMsgToTranmgr(Isomsg);

				putRecordIntoSourceToTmHashtable(
						Isomsg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), Isomsg);
				
				
				
				
				

			}
		} catch (Exception e) {
			
			Iso8583Post msg220=translator.construct0220ToTm(msg, nameInterface);
			action.putMsgToTranmgr(msg220);

			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30 Exception en Mensaje: " + msg.toString(), "LOG", nameInterface));
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					GenericInterface.class.getName(), "Method: [processAcquirerRevAdvFromInterchange]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: processAcquirerRevAdvFromInterchange " + e.getMessage(), "LOG", nameInterface));
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

			MessageTranslator translator = new MessageTranslator(params);
			
			

			
			Base24Ath msgToRemote2=translator.constructBase24((Iso8583Post) msg);
			udpClient.sendData(Client.getMsgKeyValue(msgToRemote2.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					Transform.fromBinToHex(Transform.getString(msgToRemote2.toMsg(false))), "B24", nameInterface));
			this.getLogger().logLine("430CONSTRUCTISO8583:"+msgToRemote2);


			action.putMsgToRemote(msgToRemote2);
			

		} catch (Exception e) {

			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30 Exception en Mensaje: " + msg.toString(), "LOG", nameInterface));
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					GenericInterface.class.getName(), "Method: [processAcquirerRevAdvRspFromTranmgr]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
		}
		return action;
	}
	
	@Override
	public Action processAcquirerRevAdvRspFromInterchange(AInterchangeDriverEnvironment interchange,
			Iso8583 msgFromRemote) throws Exception {
		Iso8583Post originalMsg = (Iso8583Post) sourceTranToTmHashtable
				.get(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR)+msgFromRemote.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR));
		
		originalMsg.putMsgType(msgFromRemote.getMsgType());
		originalMsg.putField(Iso8583.Bit._038_AUTH_ID_RSP, msgFromRemote.getField((Iso8583.Bit._038_AUTH_ID_RSP)));
		originalMsg.putField(Iso8583.Bit._039_RSP_CODE, msgFromRemote.getField((Iso8583.Bit._039_RSP_CODE)));
		
		Action action = new Action();
		action.putMsgToTranmgr(originalMsg);
		
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
		Iso8583Post msgToTm = new Iso8583Post();
		Base24Ath msgToRemote = new Base24Ath(kwa);
		MessageTranslator translator = new MessageTranslator(params);
		try {
			
			int errMac = msgFromRemote.failedMAC();
			if (errMac == Base24Ath.MACError.INVALID_MAC_ERROR) {
				action = new Action(null, constructEchoMsgIndicatorFailedMAC(msgFromRemote, errMac), null, null);
			} else {

				

				 Iso8583Post Isomsg =
				 translator.constructIso8583(msgFromRemote);
				 
			
				
				this.getLogger().logLine("MENSAJEIso8583Post:"+Isomsg.toString());
				
				action.putMsgToTranmgr(Isomsg);

				putRecordIntoSourceToTmHashtable(
						Isomsg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), Isomsg);
				
				
				}

			

		} catch (Exception e) {
			
			Iso8583Post msg220=translator.construct0220ToTm(msg, nameInterface);
			action.putMsgToTranmgr(msg220);

			udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"ERRISO30 Exception en Mensaje: " + msg.toString(), "LOG", nameInterface));
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					GenericInterface.class.getName(), "Method: [processTranAdvFromInterchange]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
		}
		return action;
	}

	
	

	public static String hexToAscci(String hexString) {
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < hexString.length(); i += 2) {
			String str = hexString.substring(i, i + 2);
			output.append((char) Integer.parseInt(str, 16));
		}
		return output.toString();
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
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					GenericInterface.class.getName(), "Method: [constructEchoMsgIndicatorFailedMAC]",
					Utils.getStringMessageException(e), rsp.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
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

}