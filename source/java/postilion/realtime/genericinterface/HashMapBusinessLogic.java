package postilion.realtime.genericinterface;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import postilion.realtime.genericinterface.translate.util.EventReporter;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.library.common.model.ConfigAllTransaction;
import postilion.realtime.library.common.model.ResponseCode;
import postilion.realtime.sdk.crypto.DesKwa;

/**
 *
 * Esta clase permite realizar realizar el cargue a memoria de los HashMap para
 * tratamiento de campos
 *
 * @author Juan Carlos Rodriguez
 *
 */

public class HashMapBusinessLogic {

	private Map<String, String> createFields220ToTM = new HashMap<>();
	private Map<String, String> deleteFieldsRequest = new HashMap<>();
	private Map<String, String> createFieldsRequest = new HashMap<>();
	private Map<String, String> transformFieldsMultipleCases = new HashMap<>();
	private Map<String, String> transformFields = new HashMap<>();
	private Map<String, String> skipCopyFields = new HashMap<>();
	private Map<String, String> structuredDataFields = new HashMap<>();
	private Map<String, String> createFields = new HashMap<>();
	private Map<String, String> copyFieldsResponse = new HashMap<>();
	private Map<String, String> transformFieldsMultipleCasesResponse = new HashMap<>();
	private Map<String, String> transformFieldsResponse = new HashMap<>();
	private Map<String, String> createFieldsResponse = new HashMap<>();
	private Map<String, String> deleteFieldsResponse = new HashMap<>();
	private Map<String, String> copyFieldsResponseRev = new HashMap<>();
	private Map<String, String> createFieldsResponseRev = new HashMap<>();
	private Map<String, String> transformFieldsResponseRev = new HashMap<>();
	private Map<String, String> deleteFieldsResponseRev = new HashMap<>();
	private Map<String, String> createFieldsResponseAdv = new HashMap<>();
	private Map<String, String> copyFieldsResponseAdv = new HashMap<>();
	private Map<String, String> deleteFieldsResponseAdv = new HashMap<>();

	private HashMap<String, DesKwa> keys = new HashMap<>();

	private Map<String, String> institutionid = new HashMap<>();
	private Map<String, String> cutValues = new HashMap<>();
	private Map<String, String> covenantMap = new HashMap<>();
	private Map<String, HashMap<String, ConfigAllTransaction>> structureContent = new HashMap<>();
	private Map<String, ConfigAllTransaction> structureMap = new HashMap<>();
	private Map<String, ResponseCode> allCodesIsoToB24 = new HashMap<>();
	private Map<String, ResponseCode> allCodesIscToIso = new HashMap<>();
	private Map<String, ResponseCode> allCodesIsoToB24TM = new HashMap<>();
	private Map<String, ResponseCode> allCodesB24ToIso = new HashMap<>();
	private Map<String, String> migratedOpCodes = new HashMap<>();
	private Map<String, String> migratedCards = new HashMap<>();
	private Map<String, String> migratedBins = new HashMap<>();

	private Map<String, String> transformFieldsResponseAdv = new HashMap<>();

	private Map<String, String> primerFiltroTest1 = new HashMap<>();
	private Map<String, String> primerFiltroTest2 = new HashMap<>();
	private Map<String, String> segundoFiltroTest2 = new HashMap<>();
	private Map<String, String> segundoFiltro = new HashMap<>();
	private static Map<String, String> migratedOpCodesAtm = new HashMap<>();
	private static Map<String, String> createFieldsRev = new HashMap<>();
	private static Map<String, String> transformFieldsRev = new HashMap<>();
	private static Map<String, String> transformFieldsMultipleCasesRev = new HashMap<>();
	private static Map<String, String> skipCopyFieldsRev = new HashMap<>();
	private static Map<String, String> transformFieldsMultipleCasesResponseAdv = new HashMap<>();
	private static Map<String, String> transformFieldsMultipleCasesResponseRev = new HashMap<>();

	@SuppressWarnings("unchecked")
	public void loadHashMap(String filePath, String nameInterface, Client udpClient) {

		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(new FileReader(filePath));
			JSONObject jsonObject = (JSONObject) obj;
			JSONArray hash = (JSONArray) jsonObject.get("hash");
			for (int j = 0; j < hash.size(); j++) {
				JSONObject item = (JSONObject) hash.get(j);
				item.forEach((k, v) -> {
					JSONArray value = (JSONArray) v;
					putHashMapBusinessLogic(k, value);
				});
			}

		} catch (FileNotFoundException e) {
			EventReporter.reportGeneralEvent(nameInterface, GenericInterface.class.getName(), e, "N/D", "loadHashMap",
					udpClient, "of type FileNotFoundException");
		} catch (ParseException e) {
			EventReporter.reportGeneralEvent(nameInterface, GenericInterface.class.getName(), e, "N/D", "loadHashMap",
					udpClient, "of type ParseException");
		} catch (IOException e) {
			EventReporter.reportGeneralEvent(nameInterface, GenericInterface.class.getName(), e, "N/D", "loadHashMap",
					udpClient, "of type IOException");
		}

		/*
		 * createFields220ToTM.put("3", "3"); createFields220ToTM.put("4", "4");
		 * createFields220ToTM.put("7", "7"); createFields220ToTM.put("11", "11");
		 * createFields220ToTM.put("12", "12"); createFields220ToTM.put("13", "13");
		 * createFields220ToTM.put("15", "15"); createFields220ToTM.put("22", "22");
		 * createFields220ToTM.put("25", "25"); createFields220ToTM.put("32", "32");
		 * createFields220ToTM.put("35", "35"); createFields220ToTM.put("37", "37");
		 * createFields220ToTM.put("39", "39"); createFields220ToTM.put("41", "41");
		 * createFields220ToTM.put("42", "42"); createFields220ToTM.put("43", "43");
		 * createFields220ToTM.put("48", "48"); createFields220ToTM.put("49", "49");
		 * createFields220ToTM.put("98", "98"); createFields220ToTM.put("100", "100");
		 * createFields220ToTM.put("102", "102"); createFields220ToTM.put("104", "104");
		 * createFields220ToTM.put("123", "123");
		 * 
		 * deleteFieldsRequest.put("501030", "14-15-22-25-26-40-42-56-98-100-123");
		 * deleteFieldsRequest.put("401010", "14-15-22-25-26-40-42-56-100-123");
		 * deleteFieldsRequest.put("013000", "14-15-25-26-40-52-56-102-103-104-123");
		 * 
		 * createFieldsRequest.put("3-270110", "compensationDateValidationP17ToP15");
		 * 
		 * transformFieldsMultipleCases.put("3", "3");
		 * 
		 * transformFields.put("3", "N/A"); transformFields.put("3-270110",
		 * "transformField3ForDepositATM"); transformFields.put("3-270120",
		 * "transformField3ForDepositATM"); transformFields.put("3-270100",
		 * "transformField3ForCreditPaymentATM"); transformFields.put("3-270140",
		 * "transformField3ForCreditPaymentATM"); transformFields.put("3-270141",
		 * "transformField3ForCreditPaymentATM"); transformFields.put("3-890000",
		 * "constructProcessingCode"); transformFields.put("15",
		 * "compensationDateValidationP17ToP15"); transformFields.put("32",
		 * "constructAcquiringInstitutionIDCodeToTM"); transformFields.put("41",
		 * "constructCardAcceptorTermIdToTranmgr"); transformFields.put("52",
		 * "constructPinDataToTranmgr"); transformFields.put("54",
		 * "constructAdditionalAmounts"); transformFields.put("28",
		 * "transformAmountFeeFields"); transformFields.put("30",
		 * "transformAmountFeeFields"); // transformFields.put("35",
		 * "constructField35Oficinas"); transformFields.put("95",
		 * "constructReplacementAmounts"); transformFields.put("100",
		 * "constructField100");
		 * 
		 * skipCopyFields.put("48", "48"); skipCopyFields.put("102", "102");
		 * skipCopyFields.put("103", "103"); skipCopyFields.put("104", "104");
		 * skipCopyFields.put("105", "105");
		 * 
		 * structuredDataFields.put("17", "17"); structuredDataFields.put("46", "46");
		 * structuredDataFields.put("60", "60"); structuredDataFields.put("61", "61");
		 * structuredDataFields.put("62", "62"); structuredDataFields.put("126", "126");
		 * structuredDataFields.put("128", "128");
		 * 
		 * createFields.put("15", "compensationDateValidationP17ToP15");
		 * createFields.put("22", "constructField22"); createFields.put("25",
		 * "constructPosConditionCodeToTranmgr"); createFields.put("42",
		 * "constructDefaultCardAceptorCode"); createFields.put("43",
		 * "constructField43"); createFields.put("59", "constructEchoData");
		 * createFields.put("98", "constructField98"); createFields.put("100",
		 * "constructField100"); createFields.put("123", "constructPosDataCode");
		 * 
		 * copyFieldsResponse.put("3", "3"); copyFieldsResponse.put("4", "4");
		 * copyFieldsResponse.put("7", "7"); copyFieldsResponse.put("11", "11");
		 * copyFieldsResponse.put("12", "12"); copyFieldsResponse.put("13", "13");
		 * copyFieldsResponse.put("15", "15"); copyFieldsResponse.put("22", "22");
		 * copyFieldsResponse.put("32", "32"); copyFieldsResponse.put("35", "35");
		 * copyFieldsResponse.put("37", "37"); copyFieldsResponse.put("38", "38");
		 * copyFieldsResponse.put("39", "39"); copyFieldsResponse.put("48", "48");
		 * copyFieldsResponse.put("49", "49"); copyFieldsResponse.put("103", "103");
		 * 
		 * transformFieldsMultipleCasesResponse.put("3", "3");
		 * transformFieldsMultipleCasesResponse.put("44", "44");
		 * transformFieldsMultipleCasesResponse.put("48", "48");
		 * transformFieldsMultipleCasesResponse.put("63", "63");
		 * transformFieldsMultipleCasesResponse.put("102", "102");
		 * transformFieldsMultipleCasesResponse.put("103", "103");
		 * 
		 * transformFieldsResponse.put("3", "N/A");
		 * transformFieldsResponse.put("3-210110", "transformField3ForDepositATM");
		 * transformFieldsResponse.put("3-210120", "transformField3ForDepositATM");
		 * transformFieldsResponse.put("3-320100_270100",
		 * "transformField3ForCreditPaymentATM");
		 * transformFieldsResponse.put("3-320100_270140",
		 * "transformField3ForCreditPaymentATM");
		 * transformFieldsResponse.put("3-320100_270141",
		 * "transformField3ForCreditPaymentATM");
		 * transformFieldsResponse.put("3-510100",
		 * "transformField3ForCreditPaymentATM");
		 * transformFieldsResponse.put("3-510140",
		 * "transformField3ForCreditPaymentATM");
		 * transformFieldsResponse.put("3-510141",
		 * "transformField3ForCreditPaymentATM");
		 * transformFieldsResponse.put("3-320100_270120", "constructProcessingCode");
		 * transformFieldsResponse.put("3-320100_270130", "constructProcessingCode");
		 * transformFieldsResponse.put("3-320000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_011000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_311000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_321000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_381000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_401000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_401010", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_401020", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_501000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_501030", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_501040", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_501041", "constructProcessingCode");
		 * transformFieldsResponse.put("3-321000_501042", "constructProcessingCode");
		 * transformFieldsResponse.put("3-320100_270110", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_012000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_312000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_322000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_382000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_402000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_402010", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_402020", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_502000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_502030", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_502040", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_502041", "constructProcessingCode");
		 * transformFieldsResponse.put("3-322000_502042", "constructProcessingCode");
		 * transformFieldsResponse.put("3-324000_314000", "constructProcessingCode");
		 * transformFieldsResponse.put("3-324000_404010", "constructProcessingCode");
		 * transformFieldsResponse.put("3-324000_404020", "constructProcessingCode");
		 * transformFieldsResponse.put("38-011000", "constructField38DefaultOrCopy");
		 * transformFieldsResponse.put("38-012000", "constructField38DefaultOrCopy");
		 * transformFieldsResponse.put("38-222222", "constructField38DefaultOrCopy");
		 * transformFieldsResponse.put("38-314000", "constructField38DefaultOrCopy");
		 * transformFieldsResponse.put("38-404010", "constructField38DefaultOrCopy");
		 * transformFieldsResponse.put("38-404020", "constructField38DefaultOrCopy");
		 * transformFieldsResponse.put("40-011000", "constructServiceRestrictionCode");
		 * transformFieldsResponse.put("40-012000", "constructServiceRestrictionCode");
		 * transformFieldsResponse.put("40-222222", "constructServiceRestrictionCode");
		 * transformFieldsResponse.put("40-321000_321000",
		 * "constructServiceRestrictionCode");
		 * transformFieldsResponse.put("40-322000_322000",
		 * "constructServiceRestrictionCode"); transformFieldsResponse.put("44", "N/A");
		 * transformFieldsResponse.put("44-011000", "constructField44");
		 * transformFieldsResponse.put("44-012000", "constructField44");
		 * transformFieldsResponse.put("44-222222", "constructField44");
		 * transformFieldsResponse.put("44-311000", "constructField44");
		 * transformFieldsResponse.put("44-312000", "constructField44");
		 * transformFieldsResponse.put("44-314000", "constructField44");
		 * transformFieldsResponse.put("44-320100_270100", "constructField44");
		 * transformFieldsResponse.put("44-320100_270140", "constructField44");
		 * transformFieldsResponse.put("44-501000", "constructField44");
		 * transformFieldsResponse.put("44-501030", "constructField44");
		 * transformFieldsResponse.put("44-501040", "constructField44");
		 * transformFieldsResponse.put("44-501041", "constructField44");
		 * transformFieldsResponse.put("44-501042", "constructField44");
		 * transformFieldsResponse.put("44-502000", "constructField44");
		 * transformFieldsResponse.put("44-502030", "constructField44");
		 * transformFieldsResponse.put("44-502040", "constructField44");
		 * transformFieldsResponse.put("44-502041", "constructField44");
		 * transformFieldsResponse.put("44-502042", "constructField44");
		 * transformFieldsResponse.put("44-510100", "constructField44");
		 * transformFieldsResponse.put("44-510140", "constructField44");
		 * transformFieldsResponse.put("44-510141", "constructField44");
		 * transformFieldsResponse.put("44-321000_321000", "constuctServiceRspData");
		 * transformFieldsResponse.put("44-322000_012000", "constuctServiceRspData");
		 * transformFieldsResponse.put("44-322000_322000", "constuctServiceRspData");
		 * transformFieldsResponse.put("44-381000", "constuctServiceRspData");
		 * transformFieldsResponse.put("44-382000", "constuctServiceRspData");
		 * transformFieldsResponse.put("48", "N/A");
		 * transformFieldsResponse.put("48-321000_011000",
		 * "constructField048InRspCostInquiry");
		 * transformFieldsResponse.put("48-321000_381000",
		 * "constructField048InRspCostInquiry");
		 * transformFieldsResponse.put("48-322000_012000",
		 * "constructField048InRspCostInquiry");
		 * transformFieldsResponse.put("48-322000_382000",
		 * "constructField048InRspCostInquiry");
		 * transformFieldsResponse.put("48-011000",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-012000",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-210110",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-210120",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-222222",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-320100_270100",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-320100_270110",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-320100_270120",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-320100_270130",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-320100_270140",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-320100_270141",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-321000_321000",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-322000_322000",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-324000_314000",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-324000_404010",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-324000_404020",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-510100",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-510140",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("48-510141",
		 * "constructFieldFromStructuredDataP48");
		 * transformFieldsResponse.put("54-011000", "constructField54");
		 * transformFieldsResponse.put("54-012000", "constructField54");
		 * transformFieldsResponse.put("63", "N/A");
		 * transformFieldsResponse.put("63-011000", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-012000", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-222222", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-321000_011000", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-321000_401000", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-321000_501030", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-322000_012000", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-322000_402000", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-322000_502030", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-401000", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-401010", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-401020", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-402000", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-402010", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-402020", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-501000", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-501030", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-501040", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-501041", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-501042", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-502030", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-502040", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-502041", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-502042", "constuctCodeResponseInIsc");
		 * transformFieldsResponse.put("63-210110", "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-210120", "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-314000", "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-320000", "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-320100_270100",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-320100_270110",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-320100_270120",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-320100_270130",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-320100_270140",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-320100_270141",
		 * "constructResponseCodeField63"); transformFieldsResponse.put("63-321000",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-321000_321000",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-321000_401010",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-321000_401020",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-321000_501000",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-321000_501040",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-321000_501041",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-321000_501042",
		 * "constructResponseCodeField63"); transformFieldsResponse.put("63-322000",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-322000_322000",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-322000_402010",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-322000_402020",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-322000_502000",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-322000_502040",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-322000_502041",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-322000_502042",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-324000_314000",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-324000_404010",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("63-324000_404020",
		 * "constructResponseCodeField63"); transformFieldsResponse.put("63-404010",
		 * "constructResponseCodeField63"); transformFieldsResponse.put("63-404020",
		 * "constructResponseCodeField63"); transformFieldsResponse.put("63-502000",
		 * "constructResponseCodeField63"); transformFieldsResponse.put("63-510100",
		 * "constructResponseCodeField63"); transformFieldsResponse.put("63-510140",
		 * "constructResponseCodeField63"); transformFieldsResponse.put("63-510141",
		 * "constructResponseCodeField63");
		 * transformFieldsResponse.put("100-321000_401010", "constructField100ACH");
		 * transformFieldsResponse.put("100-321000_401020", "constructField100ACH");
		 * transformFieldsResponse.put("100-322000_402010", "constructField100ACH");
		 * transformFieldsResponse.put("100-322000_402020", "constructField100ACH");
		 * transformFieldsResponse.put("102", "N/A");
		 * transformFieldsResponse.put("102-314000",
		 * "constructField102ConsultaCreditoRotativo");
		 * transformFieldsResponse.put("102-311000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-312000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-320000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-321000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-321000_311000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-321000_381000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-322000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-322000_312000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-322000_382000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-381000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-382000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("102-011000", "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-012000", "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-222222", "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-321000_011000",
		 * "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-321000_321000",
		 * "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-321000_401000",
		 * "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-322000_012000",
		 * "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-322000_322000",
		 * "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-322000_402000",
		 * "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-324000_314000",
		 * "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-324000_404010",
		 * "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-324000_404020",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-401010",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-401020",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-402010",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-402020",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-404010",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-404020",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-501000",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-501030",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-501040",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-501041",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-501042",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-502030",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-502040",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-502041",
		 * "constructField102DefaultOrCopy"); transformFieldsResponse.put("102-502042",
		 * "constructField102DefaultOrCopy");
		 * transformFieldsResponse.put("102-321000_401010",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-321000_401020",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-321000_501000",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-321000_501030",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-321000_501040",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-321000_501041",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-321000_501042",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-322000_402010",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-322000_402020",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-322000_502000",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-322000_502030",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-322000_502040",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-322000_502041",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("102-322000_502042",
		 * "constructField102ConsultaCostoTransferencia");
		 * transformFieldsResponse.put("103", "N/A");
		 * transformFieldsResponse.put("103-311000", "transformField103");
		 * transformFieldsResponse.put("103-312000", "transformField103");
		 * transformFieldsResponse.put("103-314000", "transformField103");
		 * transformFieldsResponse.put("103-322000_322000", "transformField103");
		 * transformFieldsResponse.put("103-324000_314000", "transformField103");
		 * transformFieldsResponse.put("103-381000", "transformField103");
		 * transformFieldsResponse.put("103-382000", "transformField103");
		 * transformFieldsResponse.put("103-320000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("103-321000_011000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("103-321000_311000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("103-321000_321000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("103-321000_381000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("103-322000_012000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("103-322000_312000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("103-322000_382000",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("103-322000_502030",
		 * "constructField102_103ConsultaCosto");
		 * transformFieldsResponse.put("103-321000_401010",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-321000_401020",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-321000_501000",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-321000_501030",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-321000_501040",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-321000_501041",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-321000_501042",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-322000_402010",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-322000_402020",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-322000_502000",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-322000_502040",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-322000_502041",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("103-322000_502042",
		 * "constructField104Deposito");
		 * transformFieldsResponse.put("105-321000_401000",
		 * "constructField105DefaultOrCopy");
		 * transformFieldsResponse.put("105-322000_402000",
		 * "constructField105DefaultOrCopy"); transformFieldsResponse.put("126-011000",
		 * "constructField126IsoTranslate"); transformFieldsResponse.put("126-012000",
		 * "constructField126IsoTranslate");
		 * transformFieldsResponse.put("126-320100_270100",
		 * "constructField126IsoTranslate");
		 * transformFieldsResponse.put("126-320100_270130",
		 * "constructField126IsoTranslate");
		 * transformFieldsResponse.put("126-320100_270140",
		 * "constructField126IsoTranslate");
		 * transformFieldsResponse.put("126-320100_270141",
		 * "constructField126IsoTranslate");
		 * transformFieldsResponse.put("126-321000_011000",
		 * "constructField126IsoTranslate");
		 * transformFieldsResponse.put("126-322000_012000",
		 * "constructField126IsoTranslate"); transformFieldsResponse.put("126-510100",
		 * "constructField126IsoTranslate"); transformFieldsResponse.put("126-510140",
		 * "constructField126IsoTranslate"); transformFieldsResponse.put("126-510141",
		 * "constructField126IsoTranslate"); transformFieldsResponse.put("128-401010",
		 * "constructField128");
		 * 
		 * createFieldsResponse.put("17-321000_321000", "constructDateCapture");
		 * createFieldsResponse.put("17-321000_401010", "constructDateCapture");
		 * createFieldsResponse.put("17-321000_401020", "constructDateCapture");
		 * createFieldsResponse.put("17-321000_501030", "constructDateCapture");
		 * createFieldsResponse.put("17-322000_322000", "constructDateCapture");
		 * createFieldsResponse.put("17-322000_402010", "constructDateCapture");
		 * createFieldsResponse.put("17-322000_402020", "constructDateCapture");
		 * createFieldsResponse.put("17-322000_502030", "constructDateCapture");
		 * createFieldsResponse.put("17-501000", "constructDateCapture");
		 * createFieldsResponse.put("17-502000", "constructDateCapture");
		 * createFieldsResponse.put("17-011000", "constructDateCapture");
		 * createFieldsResponse.put("17-012000", "constructDateCapture");
		 * createFieldsResponse.put("17-210110", "constructDateCapture");
		 * createFieldsResponse.put("17-210120", "constructDateCapture");
		 * createFieldsResponse.put("17-320100_270100", "constructDateCapture");
		 * createFieldsResponse.put("17-320100_270110", "constructDateCapture");
		 * createFieldsResponse.put("17-320100_270120", "constructDateCapture");
		 * createFieldsResponse.put("17-320100_270130", "constructDateCapture");
		 * createFieldsResponse.put("17-320100_270140", "constructDateCapture");
		 * createFieldsResponse.put("17-320100_270141", "constructDateCapture");
		 * createFieldsResponse.put("17-321000_011000", "constructDateCapture");
		 * createFieldsResponse.put("17-321000_501000", "constructDateCapture");
		 * createFieldsResponse.put("17-321000_501040", "constructDateCapture");
		 * createFieldsResponse.put("17-321000_501041", "constructDateCapture");
		 * createFieldsResponse.put("17-321000_501042", "constructDateCapture");
		 * createFieldsResponse.put("17-322000_012000", "constructDateCapture");
		 * createFieldsResponse.put("17-322000_502000", "constructDateCapture");
		 * createFieldsResponse.put("17-322000_502040", "constructDateCapture");
		 * createFieldsResponse.put("17-322000_502041", "constructDateCapture");
		 * createFieldsResponse.put("17-322000_502042", "constructDateCapture");
		 * createFieldsResponse.put("17-324000_314000", "constructDateCapture");
		 * createFieldsResponse.put("17-324000_404010", "constructDateCapture");
		 * createFieldsResponse.put("17-324000_404020", "constructDateCapture");
		 * createFieldsResponse.put("17-510100", "constructDateCapture");
		 * createFieldsResponse.put("17-510140", "constructDateCapture");
		 * createFieldsResponse.put("17-510141", "constructDateCapture");
		 * createFieldsResponse.put("38-011000", "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-012000", "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-210110", "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-210120", "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-314000", "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-320000", "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-320100_270130",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-321000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-321000_311000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-321000_381000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-321000_401010",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-321000_401020",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-321000_501000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-321000_501030",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-321000_501041",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-322000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-322000_312000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-322000_382000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-322000_402010",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-322000_402020",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-322000_502000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-322000_502030",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-324000_314000",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-381000",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-382000",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-401000",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-401010",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-401020",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-402000",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-402010",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-402020",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-404010",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-404020",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-501000",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-501030",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-501040",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-501041",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-501042",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-502000",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-502030",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-502040",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-502041",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-502042",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-510141",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-320100_270100",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-320100_270110",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-320100_270120",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-320100_270140",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-320100_270141",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-321000_011000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-321000_321000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-321000_401000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-322000_012000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-322000_322000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-322000_402000",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-324000_404010",
		 * "constructField38DefaultOrCopy");
		 * createFieldsResponse.put("38-324000_404020",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-510100",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-510140",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-311000",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("38-312000",
		 * "constructField38DefaultOrCopy"); createFieldsResponse.put("44-222222",
		 * "constructField44"); createFieldsResponse.put("44-314000",
		 * "constructField44"); createFieldsResponse.put("44-501030",
		 * "constructField44"); createFieldsResponse.put("44-501040",
		 * "constructField44"); createFieldsResponse.put("44-501041",
		 * "constructField44"); createFieldsResponse.put("44-501042",
		 * "constructField44"); createFieldsResponse.put("44-502030",
		 * "constructField44"); createFieldsResponse.put("44-502040",
		 * "constructField44"); createFieldsResponse.put("44-502041",
		 * "constructField44"); createFieldsResponse.put("44-502042",
		 * "constructField44"); createFieldsResponse.put("44-011000",
		 * "constuctServiceRspData"); createFieldsResponse.put("44-311000",
		 * "constructField44"); createFieldsResponse.put("44-312000",
		 * "constructField44"); createFieldsResponse.put("44-320100_270100",
		 * "constructField44"); createFieldsResponse.put("44-320100_270140",
		 * "constructField44"); createFieldsResponse.put("44-502000",
		 * "constructField44"); createFieldsResponse.put("44-510100",
		 * "constructField44"); createFieldsResponse.put("44-510140",
		 * "constructField44"); createFieldsResponse.put("44-510141",
		 * "constructField44"); createFieldsResponse.put("44-501000",
		 * "constructField44"); createFieldsResponse.put("52-210110",
		 * "constructPinDataToBase24Response"); createFieldsResponse.put("52-210120",
		 * "constructPinDataToBase24Response");
		 * createFieldsResponse.put("52-320100_270100",
		 * "constructPinDataToBase24Response");
		 * createFieldsResponse.put("52-320100_270140",
		 * "constructPinDataToBase24Response"); createFieldsResponse.put("52-510100",
		 * "constructPinDataToBase24Response"); createFieldsResponse.put("52-510140",
		 * "constructPinDataToBase24Response"); createFieldsResponse.put("52-510141",
		 * "constructPinDataToBase24Response");
		 * createFieldsResponse.put("54-321000_501030", "constructField54");
		 * createFieldsResponse.put("54-322000_502030", "constructField54"); //
		 * createFieldsResponse.put("54-401000", "constructField54"); //
		 * createFieldsResponse.put("54-402000", "constructField54");
		 * createFieldsResponse.put("61-321000_381000", "constructField61");
		 * createFieldsResponse.put("61-322000_382000", "constructField61");
		 * createFieldsResponse.put("61-381000", "constructField61");
		 * createFieldsResponse.put("61-382000", "constructField61");
		 * createFieldsResponse.put("61-210110", "constructField61ByDefault");
		 * createFieldsResponse.put("61-210120", "constructField61ByDefault");
		 * createFieldsResponse.put("61-320100_270100", "constructField61ByDefault");
		 * createFieldsResponse.put("61-320100_270110", "constructField61ByDefault");
		 * createFieldsResponse.put("61-320100_270120", "constructField61ByDefault");
		 * createFieldsResponse.put("61-320100_270130", "constructField61ByDefault");
		 * createFieldsResponse.put("61-320100_270140", "constructField61ByDefault");
		 * createFieldsResponse.put("61-320100_270141", "constructField61ByDefault");
		 * createFieldsResponse.put("61-321000_401010", "constructField61ByDefault");
		 * createFieldsResponse.put("61-321000_401020", "constructField61ByDefault");
		 * createFieldsResponse.put("61-321000_401000", "constructField61ByDefault");
		 * createFieldsResponse.put("61-322000_402000", "constructField61ByDefault");
		 * createFieldsResponse.put("61-321000_501000", "constructField61ByDefault");
		 * createFieldsResponse.put("61-321000_501030", "constructField61ByDefault");
		 * createFieldsResponse.put("61-321000_501040", "constructField61ByDefault");
		 * createFieldsResponse.put("61-321000_501041", "constructField61ByDefault");
		 * createFieldsResponse.put("61-321000_501042", "constructField61ByDefault");
		 * createFieldsResponse.put("61-322000_402010", "constructField61ByDefault");
		 * createFieldsResponse.put("61-322000_402020", "constructField61ByDefault");
		 * createFieldsResponse.put("61-322000_502000", "constructField61ByDefault");
		 * createFieldsResponse.put("61-322000_502030", "constructField61ByDefault");
		 * createFieldsResponse.put("61-322000_502040", "constructField61ByDefault");
		 * createFieldsResponse.put("61-322000_502041", "constructField61ByDefault");
		 * createFieldsResponse.put("61-322000_502042", "constructField61ByDefault");
		 * createFieldsResponse.put("61-324000_404010", "constructField61ByDefault");
		 * createFieldsResponse.put("61-324000_404020", "constructField61ByDefault");
		 * createFieldsResponse.put("61-401000", "constructField61ByDefault");
		 * createFieldsResponse.put("61-401010", "constructField61ByDefault");
		 * createFieldsResponse.put("61-401020", "constructField61ByDefault");
		 * createFieldsResponse.put("61-402000", "constructField61ByDefault");
		 * createFieldsResponse.put("61-402010", "constructField61ByDefault");
		 * createFieldsResponse.put("61-402020", "constructField61ByDefault");
		 * createFieldsResponse.put("61-404010", "constructField61ByDefault");
		 * createFieldsResponse.put("61-404020", "constructField61ByDefault");
		 * createFieldsResponse.put("61-501030", "constructField61ByDefault");
		 * createFieldsResponse.put("61-501040", "constructField61ByDefault");
		 * createFieldsResponse.put("61-501041", "constructField61ByDefault");
		 * createFieldsResponse.put("61-501042", "constructField61ByDefault");
		 * createFieldsResponse.put("61-502000", "constructField61ByDefault");
		 * createFieldsResponse.put("61-502030", "constructField61ByDefault");
		 * createFieldsResponse.put("61-502040", "constructField61ByDefault");
		 * createFieldsResponse.put("61-502041", "constructField61ByDefault");
		 * createFieldsResponse.put("61-502042", "constructField61ByDefault");
		 * createFieldsResponse.put("61-510100", "constructField61ByDefault");
		 * createFieldsResponse.put("61-510140", "constructField61ByDefault");
		 * createFieldsResponse.put("61-510141", "constructField61ByDefault");
		 * createFieldsResponse.put("61-501000", "constructField61ByDefault");
		 * createFieldsResponse.put("61-321000_401000", "constructField61ByDefault");
		 * createFieldsResponse.put("61-322000_402000", "constructField61ByDefault"); //
		 * createFieldsResponse.put("62-401000", "constructFieldFromStructuredData"); //
		 * createFieldsResponse.put("62-402000", "constructFieldFromStructuredData");
		 * createFieldsResponse.put("62-210110", "constructField62InDeclinedResponse");
		 * createFieldsResponse.put("62-210120", "constructField62InDeclinedResponse");
		 * createFieldsResponse.put("62-320100_270100",
		 * "constructField62InDeclinedResponse");
		 * createFieldsResponse.put("62-320100_270140",
		 * "constructField62InDeclinedResponse"); createFieldsResponse.put("62-510100",
		 * "constructField62InDeclinedResponse"); createFieldsResponse.put("62-510140",
		 * "constructField62InDeclinedResponse"); createFieldsResponse.put("62-510141",
		 * "constructField62InDeclinedResponse"); createFieldsResponse.put("62-381000",
		 * "constructField62Last5Mov"); createFieldsResponse.put("62-382000",
		 * "constructField62Last5Mov"); createFieldsResponse.put("63-011000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-321000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-321000_011000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-321000_321000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-321000_501030",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-322000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-322000_012000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-322000_322000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-322000_502030",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-321000_401010",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-321000_401020",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-321000_501040",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-321000_501042",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-322000_402010",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-322000_402020",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-322000_502040",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-322000_502041",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-322000_502042",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-501000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-502000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-321000_501000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-321000_501041",
		 * "constructResponseCodeField63"); createFieldsResponse.put("63-322000_502000",
		 * "constructResponseCodeField63"); createFieldsResponse.put("100-401010",
		 * "constructField100ACH"); createFieldsResponse.put("100-401020",
		 * "constructField100ACH"); createFieldsResponse.put("100-402010",
		 * "constructField100ACH"); createFieldsResponse.put("100-402020",
		 * "constructField100ACH"); createFieldsResponse.put("100-210110",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-210120",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-320100_270100",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-320100_270110",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-320100_270120",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-320100_270130",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-320100_270140",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-320100_270141",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-321000_501000",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-321000_501030",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-321000_501040",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-321000_501041",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-321000_501042",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-322000_502000",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-322000_502030",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-322000_502040",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-322000_502041",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-322000_502042",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-324000_404010",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-324000_404020",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-401000",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-401010",
		 * "constructField100DefaultInstitutionIDMasiva");
		 * createFieldsResponse.put("100-402000",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-404010",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-404020",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-501030",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-501040",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-501041",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-501042",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-502000",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-502030",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-502040",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-502041",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-502042",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-510100",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-510140",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-510141",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-501000",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-321000_401000",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("100-322000_402000",
		 * "constructField100DefaultInstitutionID");
		 * createFieldsResponse.put("102-321000_321000",
		 * "constructField102DefaultOrCopy");
		 * createFieldsResponse.put("102-322000_322000",
		 * "constructField102DefaultOrCopy"); createFieldsResponse.put("102-320000",
		 * "constructField102_103ConsultaCosto"); createFieldsResponse.put("102-321000",
		 * "constructField102_103ConsultaCosto"); createFieldsResponse.put("102-322000",
		 * "constructField102_103ConsultaCosto");
		 * createFieldsResponse.put("102-321000_311000",
		 * "constructField102_103ConsultaCosto");
		 * createFieldsResponse.put("102-322000_312000",
		 * "constructField102_103ConsultaCosto");
		 * createFieldsResponse.put("102-322000_402000",
		 * "constructField102DefaultOrCopy");
		 * createFieldsResponse.put("102-321000_401000",
		 * "constructField102DefaultOrCopy"); createFieldsResponse.put("102-401000",
		 * "constructField102toB24"); createFieldsResponse.put("102-402000",
		 * "constructField102toB24"); createFieldsResponse.put("104-314000",
		 * "constructDefaultField104"); createFieldsResponse.put("104-320000",
		 * "constructDefaultField104"); createFieldsResponse.put("104-404010",
		 * "constructDefaultField104"); createFieldsResponse.put("104-404020",
		 * "constructDefaultField104"); createFieldsResponse.put("104-314000",
		 * "constructField104Credit"); createFieldsResponse.put("104-404010",
		 * "constructField104Credit"); createFieldsResponse.put("104-404020",
		 * "constructField104Credit"); createFieldsResponse.put("104-321000_401010",
		 * "construct0210ErrorFields"); createFieldsResponse.put("104-321000_401020",
		 * "construct0210ErrorFields"); createFieldsResponse.put("104-322000_402010",
		 * "construct0210ErrorFields"); createFieldsResponse.put("104-322000_402020",
		 * "construct0210ErrorFields"); createFieldsResponse.put("104-324000_314000",
		 * "constructDefaultField104"); createFieldsResponse.put("104-324000_404010",
		 * "constructDefaultField104"); createFieldsResponse.put("104-324000_404020",
		 * "constructDefaultField104"); createFieldsResponse.put("104-320100_270130",
		 * "constructField104Credit"); createFieldsResponse.put("105-321000_501000",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-321000_501030",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-321000_501040",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-321000_501041",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-321000_501042",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-322000_502000",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-322000_502030",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-322000_502040",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-322000_502041",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-322000_502042",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-401000",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-402000",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-501030",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-501040",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-501041",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-501042",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-502000",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-502030",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-502040",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-502041",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-502042",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("105-501000",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-321000_401000",
		 * "constructField105DefaultOrCopy");
		 * createFieldsResponse.put("105-322000_402000",
		 * "constructField105DefaultOrCopy"); createFieldsResponse.put("128-401010",
		 * "constructField128");
		 * 
		 * deleteFieldsResponse.put("011000", "104-49-52");
		 * deleteFieldsResponse.put("012000", "104-49-52");
		 * deleteFieldsResponse.put("401000", "15-40-44");
		 * deleteFieldsResponse.put("402000", "15-40-44");
		 * deleteFieldsResponse.put("321000", "44-54-62-100-104-105");
		 * deleteFieldsResponse.put("322000", "44-54-62-100-104-105");
		 * deleteFieldsResponse.put("501041", "15"); deleteFieldsResponse.put("401010",
		 * "15-22-52"); deleteFieldsResponse.put("501030", "15-22-44-52-105");
		 * 
		 * copyFieldsResponseRev.put("3", "3"); copyFieldsResponseRev.put("4", "4");
		 * copyFieldsResponseRev.put("7", "7"); copyFieldsResponseRev.put("11", "11");
		 * copyFieldsResponseRev.put("22", "22"); copyFieldsResponseRev.put("32", "32");
		 * copyFieldsResponseRev.put("35", "35"); copyFieldsResponseRev.put("37", "37");
		 * copyFieldsResponseRev.put("39", "39"); // copyFieldsResponseRev.put("41",
		 * "41"); copyFieldsResponseRev.put("48", "48"); copyFieldsResponseRev.put("49",
		 * "49"); copyFieldsResponseRev.put("54", "54"); copyFieldsResponseRev.put("90",
		 * "90"); copyFieldsResponseRev.put("95", "95");
		 * 
		 * // createFieldsResponseRev.put("48-401000",
		 * "constructFieldFromTimedHashTable"); //
		 * createFieldsResponseRev.put("48-402000", "constructFieldFromTimedHashTable");
		 * // createFieldsResponseRev.put("54-401000", "constructDefaultField54"); //
		 * createFieldsResponseRev.put("54-402000", "constructDefaultField54"); //
		 * createFieldsResponseRev.put("95-510100", "constructReplacementAmounts"); //
		 * createFieldsResponseRev.put("95-510140", "constructReplacementAmounts"); //
		 * createFieldsResponseRev.put("95-510141", "constructReplacementAmounts");
		 * createFieldsResponseRev.put("112-501043", "constructField112");
		 * createFieldsResponseRev.put("112-502043", "constructField112");
		 * createFieldsResponseRev.put("17-401010", "constructField17");
		 * createFieldsResponseRev.put("104-401010", "constructDefaultField104");
		 * 
		 * transformFieldsResponseRev.put("3-510100",
		 * "transformField3ForCreditPaymentATM");
		 * transformFieldsResponseRev.put("3-510140",
		 * "transformField3ForCreditPaymentATM");
		 * transformFieldsResponseRev.put("3-510100", "transformField3ForDepositATM");
		 * transformFieldsResponseRev.put("3-510140", "transformField3ForDepositATM");
		 * transformFieldsResponseRev.put("3-210110", "transformField3ForDepositATM");
		 * transformFieldsResponseRev.put("3-210120", "transformField3ForDepositATM");
		 * transformFieldsResponseRev.put("3-510141", "transformField3ForDepositATM");
		 * // transformFieldsResponseRev.put("41-011000", "constructAdditionalRspData");
		 * // transformFieldsResponseRev.put("41-012000", "constructAdditionalRspData");
		 * // transformFieldsResponseRev.put("41-401000", "constructAdditionalRspData");
		 * // transformFieldsResponseRev.put("41-402000", "constructAdditionalRspData");
		 * // transformFieldsResponseRev.put("41-502000", "constructAdditionalRspData");
		 * // transformFieldsResponseRev.put("41-501000", "constructAdditionalRspData");
		 * transformFieldsResponseRev.put("41-501043", "constructAdditionalRspData");
		 * transformFieldsResponseRev.put("41-502043", "constructAdditionalRspData");
		 * transformFieldsResponseRev.put("95-012000",
		 * "constructReplacementAmountsZero");
		 * transformFieldsResponseRev.put("95-401000",
		 * "constructReplacementAmountsZero");
		 * transformFieldsResponseRev.put("95-402000",
		 * "constructReplacementAmountsZero");
		 * transformFieldsResponseRev.put("95-011000",
		 * "constructReplacementAmountsZero");
		 * transformFieldsResponseRev.put("95-501000",
		 * "constructReplacementAmountsZero");
		 * transformFieldsResponseRev.put("95-502000",
		 * "constructReplacementAmountsZero");
		 * 
		 * // deleteFieldsResponseRev.put("17", "17"); //
		 * deleteFieldsResponseRev.put("102", "102"); //
		 * deleteFieldsResponseRev.put("103", "103"); //
		 * deleteFieldsResponseRev.put("104", "104");
		 * 
		 * deleteFieldsResponseRev.put("011000", "15-17-49-102");
		 * deleteFieldsResponseRev.put("401000", "100-61-60");
		 * deleteFieldsResponseRev.put("402000", "100-61-60");
		 * deleteFieldsResponseRev.put("401010", "22-61-95");
		 * deleteFieldsResponseRev.put("401020", "61");
		 * deleteFieldsResponseRev.put("402010", "61");
		 * deleteFieldsResponseRev.put("402020", "61");
		 * 
		 * createFieldsResponseAdv.put("17", "constructOriginalFieldMsg");
		 * createFieldsResponseAdv.put("38", "constructAuthorizationIdResponse");
		 * createFieldsResponseAdv.put("48", "constructOriginalFieldMsg");
		 * createFieldsResponseAdv.put("100", "construct0210ErrorFields");
		 * 
		 * copyFieldsResponseAdv.put("3", "3"); copyFieldsResponseAdv.put("4", "4");
		 * copyFieldsResponseAdv.put("7", "7"); copyFieldsResponseAdv.put("11", "11");
		 * copyFieldsResponseAdv.put("12", "12"); copyFieldsResponseAdv.put("13", "13");
		 * copyFieldsResponseAdv.put("15", "15"); copyFieldsResponseAdv.put("22", "22");
		 * copyFieldsResponseAdv.put("32", "32"); copyFieldsResponseAdv.put("35", "35");
		 * copyFieldsResponseAdv.put("37", "37"); copyFieldsResponseAdv.put("39", "39");
		 * copyFieldsResponseAdv.put("49", "49"); copyFieldsResponseAdv.put("90", "90");
		 * copyFieldsResponseAdv.put("102", "102");
		 * 
		 * deleteFieldsResponseAdv.put("28", "28"); deleteFieldsResponseAdv.put("30",
		 * "30");
		 */
	}

	public Map<String, ResponseCode> getAllCodesIsoToB24() {
		return allCodesIsoToB24;
	}

	public void setAllCodesIsoToB24(Map<String, ResponseCode> allCodesIsoToB24) {
		this.allCodesIsoToB24 = allCodesIsoToB24;
	}

	public Map<String, ResponseCode> getAllCodesIsoToB24TM() {
		return allCodesIsoToB24TM;
	}

	public void setAllCodesIsoToB24TM(Map<String, ResponseCode> allCodesIsoToB24TM) {
		this.allCodesIsoToB24TM = allCodesIsoToB24TM;
	}

	public Map<String, ResponseCode> getAllCodesIscToIso() {
		return allCodesIscToIso;
	}

	public void setAllCodesIscToIso(Map<String, ResponseCode> allCodesIscToIso) {
		this.allCodesIscToIso = allCodesIscToIso;
	}

	public Map<String, ResponseCode> getAllCodesB24ToIso() {
		return allCodesB24ToIso;
	}

	public void setAllCodesB24ToIso(Map<String, ResponseCode> allCodesB24ToIso) {
		this.allCodesB24ToIso = allCodesB24ToIso;
	}

	public Map<String, String> getCreateFieldsResponseAdv() {
		return createFieldsResponseAdv;
	}

	public void setCreateFieldsResponseAdv(Map<String, String> createFieldsResponseAdv) {
		this.createFieldsResponseAdv = createFieldsResponseAdv;
	}

	public Map<String, String> getCopyFieldsResponseAdv() {
		return copyFieldsResponseAdv;
	}

	public void setCopyFieldsResponseAdv(Map<String, String> copyFieldsResponseAdv) {
		this.copyFieldsResponseAdv = copyFieldsResponseAdv;
	}

	public Map<String, String> getDeleteFieldsResponseAdv() {
		return deleteFieldsResponseAdv;
	}

	public void setDeleteFieldsResponseAdv(Map<String, String> deleteFieldsResponseAdv) {
		this.deleteFieldsResponseAdv = deleteFieldsResponseAdv;
	}

	public Map<String, String> getTransformFieldsResponseAdv() {
		return transformFieldsResponseAdv;
	}

	public void setTransformFieldsResponseAdv(Map<String, String> transformFieldsResponseAdv) {
		this.transformFieldsResponseAdv = transformFieldsResponseAdv;
	}

	public Map<String, String> getCopyFieldsResponse() {
		return copyFieldsResponse;
	}

	public void setCopyFieldsResponse(Map<String, String> copyFieldsResponse) {
		this.copyFieldsResponse = copyFieldsResponse;
	}

	public Map<String, String> getTransformFieldsResponse() {
		return transformFieldsResponse;
	}

	public void setTransformFieldsResponse(Map<String, String> transformFieldsResponse) {
		this.transformFieldsResponse = transformFieldsResponse;
	}

	public Map<String, String> getCreateFieldsResponse() {
		return createFieldsResponse;
	}

	public void setCreateFieldsResponse(Map<String, String> createFieldsResponse) {
		this.createFieldsResponse = createFieldsResponse;
	}

	public Map<String, String> getDeleteFieldsResponse() {
		return deleteFieldsResponse;
	}

	public void setDeleteFieldsResponse(Map<String, String> deleteFieldsResponse) {
		this.deleteFieldsResponse = deleteFieldsResponse;
	}

	public Map<String, String> getCopyFieldsResponseRev() {
		return copyFieldsResponseRev;
	}

	public void setCopyFieldsResponseRev(Map<String, String> copyFieldsResponseRev) {
		this.copyFieldsResponseRev = copyFieldsResponseRev;
	}

	public Map<String, String> getCreateFieldsResponseRev() {
		return createFieldsResponseRev;
	}

	public void setCreateFieldsResponseRev(Map<String, String> createFieldsResponseRev) {
		this.createFieldsResponseRev = createFieldsResponseRev;
	}

	public Map<String, String> getTransformFieldsResponseRev() {
		return transformFieldsResponseRev;
	}

	public void setTransformFieldsResponseRev(Map<String, String> transformFieldsResponseRev) {
		this.transformFieldsResponseRev = transformFieldsResponseRev;
	}

	public Map<String, String> getDeleteFieldsResponseRev() {
		return deleteFieldsResponseRev;
	}

	public void setDeleteFieldsResponseRev(Map<String, String> deleteFieldsResponseRev) {
		this.deleteFieldsResponseRev = deleteFieldsResponseRev;
	}

	public Map<String, String> getCreateFields220ToTM() {
		return createFields220ToTM;
	}

	public void setCreateFields220ToTM(Map<String, String> createFields220ToTM) {
		this.createFields220ToTM = createFields220ToTM;
	}

	public Map<String, String> getCreateFields() {
		return createFields;
	}

	public void setCreateFields(Map<String, String> createFields) {
		this.createFields = createFields;
	}

	public Map<String, String> getCreateFieldsRequest() {
		return createFieldsRequest;
	}

	public void setCreateFieldsRequest(Map<String, String> createFieldsRequest) {
		this.createFieldsRequest = createFieldsRequest;
	}

	public Map<String, String> getDeleteFieldsRequest() {
		return deleteFieldsRequest;
	}

	public void setDeleteFieldsRequest(Map<String, String> deleteFieldsRequest) {
		this.deleteFieldsRequest = deleteFieldsRequest;
	}

	public Map<String, String> getTransformFieldsMultipleCases() {
		return transformFieldsMultipleCases;
	}

	public void setTransformFieldsMultipleCases(Map<String, String> transformFieldsMultipleCases) {
		this.transformFieldsMultipleCases = transformFieldsMultipleCases;
	}

	public Map<String, String> getTransformFields() {
		return transformFields;
	}

	public void setTransformFields(Map<String, String> transformFields) {
		this.transformFields = transformFields;
	}

	public Map<String, String> getSkipCopyFields() {
		return skipCopyFields;
	}

	public void setSkipCopyFields(Map<String, String> skipCopyFields) {
		this.skipCopyFields = skipCopyFields;
	}

	public Map<String, String> getStructuredDataFields() {
		return structuredDataFields;
	}

	public void setStructuredDataFields(Map<String, String> structuredDataFields) {
		this.structuredDataFields = structuredDataFields;
	}

	public Map<String, String> getMigratedCards() {
		return migratedCards;
	}

	public void setMigratedCards(Map<String, String> migratedCards) {
		this.migratedCards = migratedCards;
	}

	public Map<String, String> getMigratedOpCodes() {
		return migratedOpCodes;
	}

	public void setMigratedOpCodes(Map<String, String> migratedOpCodes) {
		this.migratedOpCodes = migratedOpCodes;
	}

	public Map<String, String> getMigratedBins() {
		return migratedBins;
	}

	public void setMigratedBins(Map<String, String> migratedBins) {
		this.migratedBins = migratedBins;
	}

	public Map<String, String> getPrimerFiltroTest1() {
		return primerFiltroTest1;
	}

	public void setPrimerFiltroTest1(Map<String, String> primerFiltroTest1) {
		this.primerFiltroTest1 = primerFiltroTest1;
	}

	public Map<String, String> getSegundoFiltroTest2() {
		return segundoFiltroTest2;
	}

	public void setSegundoFiltroTest2(Map<String, String> segundoFiltroTest2) {
		this.segundoFiltroTest2 = segundoFiltroTest2;
	}

	public Map<String, String> getPrimerFiltroTest2() {
		return primerFiltroTest2;
	}

	public void setPrimerFiltroTest2(Map<String, String> primerFiltroTest2) {
		this.primerFiltroTest2 = primerFiltroTest2;
	}

	public HashMap<String, DesKwa> getKeys() {
		return keys;
	}

	public void setKeys(HashMap<String, DesKwa> keys) {
		this.keys = keys;
	}

	public Map<String, String> getCutValues() {
		return cutValues;
	}

	public void setCutValues(Map<String, String> cutValues) {
		this.cutValues = cutValues;
	}

	public void putPrimerFiltroTest1(String key, String value) {
		this.primerFiltroTest1.put(key, value);
	}

	public void putPrimerFiltroTest2(String key, String value) {
		this.primerFiltroTest2.put(key, value);
	}

	public void putSegundoFiltroTest2(String key, String value) {
		this.segundoFiltroTest2.put(key, value);
	}

	public void putKeys(String keyKwa, DesKwa valueKwa) {
		this.keys.put(keyKwa, valueKwa);
	}

	/**
	 * Llena HashMap deacuerdo a lo encontrado en el archivo Json ubicado en la ruta
	 * routingFilterPath
	 */
	public void fillFilters2(String routingFilterPath, String nameInterface, Client udpClient) {

		try (FileReader fr = new FileReader(routingFilterPath)) {
			JSONParser parser = new JSONParser();
			org.json.simple.JSONArray jsonArray = (org.json.simple.JSONArray) parser.parse(fr);
			for (Object object : jsonArray) {
				StringBuilder sbKey = new StringBuilder();
				StringBuilder sbKey2 = new StringBuilder();
				org.json.simple.JSONObject canal = (org.json.simple.JSONObject) object;
				String strCanal = (String) canal.get("Canal");
				String strCodProc = (String) canal.get("Codigo_Proceso");
				String strEntryMode = (String) canal.get("Modo_Entrada");
				sbKey.append(strCanal);
				sbKey.append(strCodProc);
				sbKey.append(strEntryMode);
				sbKey2.append(strCanal);
				sbKey2.append(strCodProc);

				if (!getPrimerFiltroTest2().containsKey(sbKey.toString()))
					putPrimerFiltroTest2(sbKey.toString(), sbKey.toString());

				String strBin = (String) canal.get("BIN");
				String strCuenta = (String) canal.get("Cuenta");

				// iteracion sobre bines
				if (!strBin.equals("0")) {
					String[] strBines = strBin.split(",");
					for (int i = 0; i < strBines.length; i++) {
						if (!getSegundoFiltroTest2().containsKey(sbKey2.toString() + strBines[i]))
							putSegundoFiltroTest2(sbKey2.toString() + strBines[i], sbKey2.toString() + strBines[i]);
					}
				}

				// iteracion sobre tarjetas
				if (!strCuenta.equals("0")) {
					String[] strCuentas = strCuenta.split(",");
					for (int i = 0; i < strCuentas.length; i++) {
						if (!getSegundoFiltroTest2().containsKey(sbKey2.toString() + strCuentas[i]))
							putSegundoFiltroTest2(sbKey2.toString() + strCuentas[i], sbKey2.toString() + strCuentas[i]);
					}
				}

			}
			fr.close();
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(nameInterface, GenericInterface.class.getName(), e, "N/D", "fillFilters2",
					udpClient);
		}

	}
   
	/**
	 * Cargue HashMap deacuerdo a lo encontrado en el archivo Json ubicado en la ruta
	 * routingLoadHashMap
	 */
	private void putHashMapBusinessLogic(Object key, JSONArray value) {
		String[] parts;
		for (int i = 0; i < value.size(); i++) {
			value.get(i);
			parts = value.get(i).toString().split(",");
			parts[0] = parts[0].replace("[", "").replace("\"", "");
			parts[1] = parts[1].replace("]", "").replace("\"", "").replace("\\", "");
			switch ((String) key) {
			case "createFields220ToTM":
				createFields220ToTM.put(parts[0], parts[1]);
				break;
			case "deleteFieldsRequest":
				deleteFieldsRequest.put(parts[0], parts[1]);
				break;
			case "createFieldsRequest":
				createFieldsRequest.put(parts[0], parts[1]);
				break;
			case "transformFieldsMultipleCases":
				transformFieldsMultipleCases.put(parts[0], parts[1]);
				break;
			case "transformFields":
				transformFields.put(parts[0], parts[1]);
				break;
			case "skipCopyFields":
				skipCopyFields.put(parts[0], parts[1]);
				break;
			case "structuredDataFields":
				structuredDataFields.put(parts[0], parts[1]);
				break;
			case "createFields":
				createFields.put(parts[0], parts[1]);
				break;
			case "copyFieldsResponse":
				copyFieldsResponse.put(parts[0], parts[1]);
				break;
			case "transformFieldsMultipleCasesResponse":
				transformFieldsMultipleCasesResponse.put(parts[0], parts[1]);
				break;
			case "transformFieldsResponse":
				transformFieldsResponse.put(parts[0], parts[1]);
				break;
			case "createFieldsResponse":
				createFieldsResponse.put(parts[0], parts[1]);
				break;
			case "deleteFieldsResponse":
				deleteFieldsResponse.put(parts[0], parts[1]);
				break;
			case "copyFieldsResponseRev":
				copyFieldsResponseRev.put(parts[0], parts[1]);
				break;
			case "createFieldsResponseRev":
				createFieldsResponseRev.put(parts[0], parts[1]);
				break;
			case "transformFieldsResponseRev":
				transformFieldsResponseRev.put(parts[0], parts[1]);
				break;
			case "deleteFieldsResponseRev":
				deleteFieldsResponseRev.put(parts[0], parts[1]);
				break;
			case "createFieldsResponseAdv":
				createFieldsResponseAdv.put(parts[0], parts[1]);
				break;
			case "copyFieldsResponseAdv":
				copyFieldsResponseAdv.put(parts[0], parts[1]);
				break;
			case "deleteFieldsResponseAdv":
				deleteFieldsResponseAdv.put(parts[0], parts[1]);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Llena HashMap deacuerdo a lo encontrado en el archivo Json ubicado en la ruta
	 * routingFilterPath
	 */
	public void fillFilters(String routingFilterPath, String nameInterface, Client udpClient) {

		try (FileReader fr = new FileReader(routingFilterPath)) {
			JSONParser parser = new JSONParser();
			org.json.simple.JSONArray jsonArray = (org.json.simple.JSONArray) parser.parse(fr);
			for (Object object : jsonArray) {
				StringBuilder sbKey = new StringBuilder();
				org.json.simple.JSONObject canal = (org.json.simple.JSONObject) object;
				String strCanal = (String) canal.get("Canal");
				String strCodProc = (String) canal.get("Codigo_Proceso");
				sbKey.append(strCanal);
				sbKey.append(strCodProc);

				String strBin = (String) canal.get("BIN");
				String strTarjeta = (String) canal.get("Numero_Tarjeta");
				String strCuenta = (String) canal.get("Cuenta");

				// iteracion sobre bines
				if (!strBin.equals("0")) {
					String[] strBines = strBin.split(",");
					for (int i = 0; i < strBines.length; i++) {
						if (!getPrimerFiltroTest1().containsKey(sbKey.toString() + strBines[i]))
							putPrimerFiltroTest1(sbKey.toString() + strBines[i], sbKey.toString() + strBines[i]);
					}
				}

				// iteracion sobre tarjetas
				if (!strTarjeta.equals("0")) {
					String[] strTarjetas = strTarjeta.split(",");
					for (int i = 0; i < strTarjetas.length; i++) {
						if (!getPrimerFiltroTest1().containsKey(sbKey.toString() + strTarjetas[i]))
							putPrimerFiltroTest1(sbKey.toString() + strTarjetas[i], sbKey.toString() + strTarjetas[i]);
					}
				}

				// iteracion sobre cuentas
				if (!strCuenta.equals("0")) {
					String[] strCuentas = strCuenta.split(",");
					for (int i = 0; i < strCuentas.length; i++) {
						if (!getPrimerFiltroTest1().containsKey(sbKey.toString() + strCuentas[i]))
							putPrimerFiltroTest1(sbKey.toString() + strCuentas[i], sbKey.toString() + strCuentas[i]);

					}
				}

			}
			fr.close();
		} catch (Exception e) {
			EventReporter.reportGeneralEvent(nameInterface, GenericInterface.class.getName(), e, "N/D", "fillFilters",
					udpClient);
		}

	}

}
