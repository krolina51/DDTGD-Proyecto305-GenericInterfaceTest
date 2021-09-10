package postilion.realtime.genericinterface;

import java.util.HashMap;
import java.util.Map;

import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.library.common.model.ConfigAllTransaction;
import postilion.realtime.library.common.model.ResponseCode;
import postilion.realtime.sdk.crypto.DesKwa;
import postilion.realtime.sdk.util.TimedHashtable;

public class Parameters {

	private DesKwa kwa = null;

	private TimedHashtable sourceTranToTmHashtable = null;
	private TimedHashtable sourceTranToTmHashtableB24 = null;

	private Map<String, HashMap<String, ConfigAllTransaction>> structureContent = new HashMap<>();

	private Map<String, String> covenantMap = new HashMap<>();
	private Map<String, ConfigAllTransaction> structureMap = new HashMap<>();

	private Map<String, ResponseCode> allCodesIsoToB24 = new HashMap<>();
	private Map<String, ResponseCode> allCodesIscToIso = new HashMap<>();
	private Map<String, ResponseCode> allCodesIsoToB24TM = new HashMap<>();
	private Map<String, ResponseCode> allCodesB24ToIso = new HashMap<>();

	private boolean consultCovenants;

	private Map<String, String> institutionid = new HashMap<>();

	private Map<String, String> cutValues = new HashMap<>();

	private String issuerId = null;

	private String ipUdpServer = "0";
	private String portUdpServer = "0";

	private Client udpClient = null;

	private String nameInterface = "";
	private boolean encodeData = false;
	private String routingField100 = "";
	private boolean exeptionValidateExpiryDate = false;
	private String urlCutWS = null;
	private String ipCryptoValidation = "10.86.82.119";
	private int portCryptoValidation = 7000;
	private HashMap<String, DesKwa> keys = new HashMap<>();

	public DesKwa getKwa() {
		return kwa;
	}

	public void setKwa(DesKwa kwa) {
		this.kwa = kwa;
	}

	public TimedHashtable getSourceTranToTmHashtable() {
		return sourceTranToTmHashtable;
	}

	public void setSourceTranToTmHashtable(TimedHashtable sourceTranToTmHashtable) {
		this.sourceTranToTmHashtable = sourceTranToTmHashtable;
	}

	public TimedHashtable getSourceTranToTmHashtableB24() {
		return sourceTranToTmHashtableB24;
	}

	public void setSourceTranToTmHashtableB24(TimedHashtable sourceTranToTmHashtableB24) {
		this.sourceTranToTmHashtableB24 = sourceTranToTmHashtableB24;
	}

	public Map<String, HashMap<String, ConfigAllTransaction>> getStructureContent() {
		return structureContent;
	}

	public void setStructureContent(Map<String, HashMap<String, ConfigAllTransaction>> structureContent) {
		this.structureContent = structureContent;
	}

	public Map<String, String> getCovenantMap() {
		return covenantMap;
	}

	public void setCovenantMap(Map<String, String> covenantMap) {
		this.covenantMap = covenantMap;
	}

	public Map<String, ConfigAllTransaction> getStructureMap() {
		return structureMap;
	}

	public void setStructureMap(Map<String, ConfigAllTransaction> structureMap) {
		this.structureMap = structureMap;
	}

	public Map<String, ResponseCode> getAllCodesIsoToB24() {
		return allCodesIsoToB24;
	}

	public void setAllCodesIsoToB24(Map<String, ResponseCode> allCodesIsoToB24) {
		this.allCodesIsoToB24 = allCodesIsoToB24;
	}

	public Map<String, ResponseCode> getAllCodesIscToIso() {
		return allCodesIscToIso;
	}

	public void setAllCodesIscToIso(Map<String, ResponseCode> allCodesIscToIso) {
		this.allCodesIscToIso = allCodesIscToIso;
	}

	public Map<String, ResponseCode> getAllCodesIsoToB24TM() {
		return allCodesIsoToB24TM;
	}

	public void setAllCodesIsoToB24TM(Map<String, ResponseCode> allCodesIsoToB24TM) {
		this.allCodesIsoToB24TM = allCodesIsoToB24TM;
	}

	public Map<String, ResponseCode> getAllCodesB24ToIso() {
		return allCodesB24ToIso;
	}

	public void setAllCodesB24ToIso(Map<String, ResponseCode> allCodesB24ToIso) {
		this.allCodesB24ToIso = allCodesB24ToIso;
	}

	public boolean isConsultCovenants() {
		return consultCovenants;
	}

	public void setConsultCovenants(boolean consultCovenants) {
		this.consultCovenants = consultCovenants;
	}

	public Map<String, String> getInstitutionid() {
		return institutionid;
	}

	public void setInstitutionid(Map<String, String> institutionid) {
		this.institutionid = institutionid;
	}

	public Map<String, String> getCutValues() {
		return cutValues;
	}

	public void setCutValues(Map<String, String> cutValues) {
		this.cutValues = cutValues;
	}

	public String getIssuerId() {
		return issuerId;
	}

	public void setIssuerId(String issuerId) {
		this.issuerId = issuerId;
	}

	public String getIpUdpServer() {
		return ipUdpServer;
	}

	public void setIpUdpServer(String ipUdpServer) {
		this.ipUdpServer = ipUdpServer;
	}

	public String getPortUdpServer() {
		return portUdpServer;
	}

	public void setPortUdpServer(String portUdpServer) {
		this.portUdpServer = portUdpServer;
	}

	public Client getUdpClient() {
		return udpClient;
	}

	public void setUdpClient(Client udpClient) {
		this.udpClient = udpClient;
	}

	public String getNameInterface() {
		return nameInterface;
	}

	public void setNameInterface(String nameInterface) {
		this.nameInterface = nameInterface;
	}

	public boolean isEncodeData() {
		return encodeData;
	}

	public void setEncodeData(boolean encodeData) {
		this.encodeData = encodeData;
	}

	public String getRoutingField100() {
		return routingField100;
	}

	public void setRoutingField100(String routingField100) {
		this.routingField100 = routingField100;
	}

	public boolean isExeptionValidateExpiryDate() {
		return exeptionValidateExpiryDate;
	}

	public void setExeptionValidateExpiryDate(boolean exeptionValidateExpiryDate) {
		this.exeptionValidateExpiryDate = exeptionValidateExpiryDate;
	}

	public String getUrlCutWS() {
		return urlCutWS;
	}

	public void setUrlCutWS(String urlCutWS) {
		this.urlCutWS = urlCutWS;
	}

	public String getIpCryptoValidation() {
		return ipCryptoValidation;
	}

	public void setIpCryptoValidation(String ipCryptoValidation) {
		this.ipCryptoValidation = ipCryptoValidation;
	}

	public int getPortCryptoValidation() {
		return portCryptoValidation;
	}

	public void setPortCryptoValidation(int portCryptoValidation) {
		this.portCryptoValidation = portCryptoValidation;
	}
	public HashMap<String, DesKwa> getKeys() {
		return keys;
	}

	public void setKeys(HashMap<String, DesKwa> keys) {
		this.keys = keys;
	}

	public Parameters(DesKwa kwa, TimedHashtable sourceTranToTmHashtable, TimedHashtable sourceTranToTmHashtableB24,
			String issuerId, Client udpClient, String nameInterface, String ipCryptoValidation,
			int portCryptoValidation, HashMap<String, DesKwa> keys, String routingField100) {
		this.kwa = kwa;
		this.sourceTranToTmHashtable = sourceTranToTmHashtable;
		this.sourceTranToTmHashtableB24 = sourceTranToTmHashtableB24;
		this.issuerId = issuerId;
		this.udpClient = udpClient;
		this.nameInterface = nameInterface;
		this.ipCryptoValidation = ipCryptoValidation;
		this.portCryptoValidation = portCryptoValidation;
		this.keys = keys;
		this.routingField100 = routingField100;
	}

}
