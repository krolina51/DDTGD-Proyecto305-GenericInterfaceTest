package postilion.realtime.genericinterface.translate;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;


import postilion.realtime.genericinterface.InvokeMethodByConfig;
import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.channels.Super;
import postilion.realtime.genericinterface.eventrecorder.events.TryCatchException;
import postilion.realtime.genericinterface.GenericInterface;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.database.DBHandler;
import postilion.realtime.genericinterface.translate.stream.Header;
import postilion.realtime.genericinterface.translate.util.Constants;
import postilion.realtime.genericinterface.translate.util.Constants.FormatDate;
import postilion.realtime.genericinterface.translate.util.Constants.General;
import postilion.realtime.genericinterface.translate.util.Constants.StatusMsg;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.library.common.InitialLoadFilter;
import postilion.realtime.library.common.model.ConfigAllTransaction;
import postilion.realtime.library.common.model.ResponseCode;
import postilion.realtime.library.common.util.constants.TagNameStructuredData;
import postilion.realtime.sdk.crypto.DesKwa;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.message.bitmap.*;
import postilion.realtime.sdk.message.bitmap.Iso8583.MsgType;
import postilion.realtime.sdk.message.bitmap.Iso8583.TranType;
import postilion.realtime.sdk.message.xml.XMLMessage2;
import postilion.realtime.sdk.message.xml.XXMLMessageUnableToExtract;
import postilion.realtime.sdk.util.Convert;
import postilion.realtime.sdk.util.DateTime;
import postilion.realtime.sdk.util.TimedHashtable;
import postilion.realtime.sdk.util.XPostilion;
import postilion.realtime.sdk.util.convert.Pack;
import postilion.realtime.sdk.util.convert.Transform;

/**
 * Esta clase permite ser llamada por la clase GenericInterface para procesar
 * informacion y armar los mensjes B24 a ISO8583Post y viceversa
 *
 * @author Albert Medina y Fernando Castañeda
 */

public class MessageTranslator extends GenericInterface {

	private DesKwa kwa;
	private TimedHashtable sourceTranToTmHashtable = null;
	private TimedHashtable sourceTranToTmHashtableB24 = null;
	private Map<String, HashMap<String, ConfigAllTransaction>> structureContent = new HashMap<>();
	private Map<String, ConfigAllTransaction> structureMap = new HashMap<>();
	private Map<String, ResponseCode> allCodesIsoToB24TM = new HashMap<>();
	private Map<String, String> institutionid = new HashMap<>();
	private Client udpClient = null;
	private String nameInterface = "";
	private Parameters params;

	public MessageTranslator() {

	}

	public MessageTranslator(Parameters params) {
		this.kwa = params.getKwa();
		this.sourceTranToTmHashtable = params.getSourceTranToTmHashtable();
		this.sourceTranToTmHashtableB24 = params.getSourceTranToTmHashtableB24();
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
		this.params = params;
	}

	/**
	 * Method for be invoked in interface to use
	 *
	 * @param msg from Base24
	 * @return Iso8583Post Message
	 * @throws XBitmapUnableToConstruct
	 * @throws XPostilion
	 */
	static long tStart;

	/**
	 * Get the Hexadecimal BitMap from received Message.
	 *
	 * @param msg Message from Interchange in Base24
	 * @return BitMap Message in Hexadecimal
	 * @throws XBitmapUnableToConstruct
	 * @throws XPostilion
	 */
	public String getBitMap(Base24Ath msg) throws XPostilion {
		try {
			String trama = new String(msg.getBinaryData());

			StringBuilder bitMap = new StringBuilder().append(trama.substring(16, 32));

			BigInteger initial = new BigInteger(trama.substring(16, 17), 16);
			StringBuilder bitMapBinario = new StringBuilder();
			switch (initial.compareTo(BigInteger.valueOf(4))) {
			case -1:
				bitMapBinario.append("00");
				break;
			case 0:
			case 1:
				switch (initial.compareTo(BigInteger.valueOf(8))) {
				case -1:
					bitMapBinario.append("0");
					break;
				case 0:
				case 1:
					bitMap.append(trama.substring(32, 48));
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
			bitMapBinario.append(new BigInteger(bitMap.toString(), 16).toString(2));
			return bitMapBinario.toString();
		} catch (Exception e) {
			EventRecorder.recordEvent(new TryCatchException(
					new String[] { this.nameInterface, MessageTranslator.class.getName(), "Method: [getBitMap]",
							Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: getBitMapHexMsg " + e.getMessage(), "LOG", this.nameInterface));
		}
		return null;
	}
	
	
	public Base24Ath constructBase24Request(Iso8583Post msg)
	{
		Base24Ath msgToRmto = new Base24Ath(this.kwa);
		try {
			InvokeMethodByConfig invoke = new InvokeMethodByConfig(params);

			msgToRmto.putHeader(constructAtmHeaderSourceNode(msgToRmto));
			msgToRmto.putMsgType(msg.getMsgType());
			
			StructuredData sd = new StructuredData();
			sd = msg.getStructuredData();
			String pCode126=sd.get("B24_Field_126")!=null? sd.get("B24_Field_126").substring(22, 28):null;
			for(int i=3;i<=126;i++)
			{
				if(sd!=null && sd.get("B24_Field_"+String.valueOf(i))!=null)
					msgToRmto.putField(i, sd.get("B24_Field_"+String.valueOf(i)));
				else if(msg.isFieldSet(i))
					msgToRmto.putField(i, msg.getField(i));
				
				String key1=String.valueOf(i)+"-"+msg.getField(Iso8583.Bit._003_PROCESSING_CODE)+"_"+ pCode126;
				String key2=String.valueOf(i)+"-"+msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
				String key3=String.valueOf(i);
				
				String methodName=null;
				
				if(createFieldsRequest.containsKey(key1))
				{
					methodName=createFieldsRequest.get(key1);
					if(!methodName.equals("N/A"))
						msgToRmto.putField(i, invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, i));
					
				}
				else if(createFieldsRequest.containsKey(key2))
				{
					methodName=createFieldsRequest.get(key2);
					if(!methodName.equals("N/A"))
						msgToRmto.putField(i, invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, i));
				}
				else if(createFieldsRequest.containsKey(key3))
				{
					methodName=createFieldsRequest.get(key3);
					if(!methodName.equals("N/A"))
						msgToRmto.putField(i, invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, i));
				}
				
			}
			
			String PCode=msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
			Set<String> set=deleteFieldsRequest.keySet().stream().filter(s->s.length()<=3).collect(Collectors.toSet());
			
			if(set.size()>0)
			{
				for(String item:set)
				{
					if(msgToRmto.isFieldSet(Integer.parseInt(item)))
					{
						msgToRmto.clearField(Integer.parseInt(item));
					}
				}
			}
			
			if(deleteFieldsRequest.containsKey(PCode))
			{
				String[] parts=deleteFieldsRequest.get(PCode).split("-");
				for(String item:parts)
				{
					if(msgToRmto.isFieldSet(Integer.parseInt(item)))
					{
						msgToRmto.clearField(Integer.parseInt(item));
					}
				}
			}

			
		}
		catch (Exception e) {
			try {
				msgToRmto=null;
				EventRecorder.recordEvent(new TryCatchException(
						new String[] { this.nameInterface, MessageTranslator.class.getName(), "Method: [constructBase24Request]",
								Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: constructBase24Request " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, MessageTranslator.class.getName(),
						"Method: [constructBase24Request]", Utils.getStringMessageException(e), "Unknown" }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
						"Exception in Method: constructBase24Request " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			}
		}
		
		return msgToRmto;
	}
	
	public Base24Ath constructBase24(Iso8583Post msg)
	{
		Base24Ath msgToRmto=new Base24Ath(this.kwa);
		String strTypeMsg=msg.getMessageType();
		
		try
		{
			msgToRmto.putHeader(constructAtmHeaderSourceNode(msgToRmto));
			msgToRmto.putMsgType(msg.getMsgType());
			Iso8583Post msgOriginal = null;
			StructuredData sd = new StructuredData();
			
			InvokeMethodByConfig invoke=new InvokeMethodByConfig(params);
			ConstructFieldMessage constructor = new ConstructFieldMessage(this.params);
			

			if (msg.getResponseCode().equals(Iso8583.RspCode._00_SUCCESSFUL)) {// si la respuesta es exitosa
				sd = msg.getStructuredData();
				msgOriginal = (Iso8583Post) this.sourceTranToTmHashtable
						.get(msg.getField(Iso8583Post.Bit._037_RETRIEVAL_REF_NR));
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"VA A ENTRAR .... A SACAR EL SD DEL MENSAJE ORIGINAL", "LOG", this.nameInterface));
				if ((sd == null && msgOriginal != null)) {
//				if (msgOriginal != null) {
					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"ENTRO A SACAR EL SD DEL MENSAJE ORIGINAL", "LOG", this.nameInterface));
					sd = msgOriginal.getStructuredData();
				}

			} else {
//				if (!msg.getMessageType().equals(Iso8583.MsgTypeStr._0430_ACQUIRER_REV_ADV_RSP)) {//respuesta no exitosa !=430
				msgOriginal = (Iso8583Post) this.sourceTranToTmHashtable
						.get(msg.getField(Iso8583Post.Bit._037_RETRIEVAL_REF_NR));
				if (msgOriginal != null) {
					sd = msgOriginal.getStructuredData();
				}
//				}
			}
			
			
			Map<String, String> copyFieldsResponse = null;
			Map<String, String> deleteFieldsResponse = null;
			Map<String, String> createFieldsResponse = null;
			Map<String, String> transformFieldsResponse = null;
			String pCode126=null;
			
			switch(strTypeMsg)
			{
				case "0210":
					
					copyFieldsResponse=GenericInterface.copyFieldsResponse;
					deleteFieldsResponse=GenericInterface.deleteFieldsResponse;
					createFieldsResponse=GenericInterface.createFieldsResponse;
					transformFieldsResponse=GenericInterface.transformFieldsResponse;
					pCode126=sd.get("B24_Field_126")!=null? sd.get("B24_Field_126").substring(22, 28):null;
					
					break;
				case "0230":
					
					copyFieldsResponse=GenericInterface.copyFieldsResponseAdv;
					deleteFieldsResponse=GenericInterface.deleteFieldsResponseAdv;
					createFieldsResponse=GenericInterface.createFieldsResponseAdv;
					transformFieldsResponse=GenericInterface.transformFieldsResponseAdv;
					
					break;
					
				case "0430":
					
					copyFieldsResponse=GenericInterface.copyFieldsResponseRev;
					deleteFieldsResponse=GenericInterface.deleteFieldsResponseRev;
					createFieldsResponse=GenericInterface.createFieldsResponseRev;
					transformFieldsResponse=GenericInterface.transformFieldsResponseRev;
					
					break;
				default:
					
					break;
			}
			
			
			//Copia los campos en el mensaje B24
			for(String key : copyFieldsResponse.keySet())
			{
				
				int intKey=Integer.parseInt(key);
				if(msg.isFieldSet(intKey))
				{
					msgToRmto.putField(intKey, msg.getField(intKey));
				}
				
			}
			
			if (sd != null) {
				msgToRmto = constructFieldsFromSd(msgToRmto, sd);
			}
			sd = msg.getStructuredData();
			
			if (sd != null) {
				msgToRmto = constructFieldsFromSd(msgToRmto, sd);
			}
			
			if (!msgToRmto.getField(Iso8583.Bit._039_RSP_CODE).equals(Iso8583.RspCode._00_SUCCESSFUL)
					&& msgToRmto.isFieldSet(Iso8583.Bit._102_ACCOUNT_ID_1)) {

				msgToRmto.putField(Iso8583.Bit._102_ACCOUNT_ID_1, Pack.resize(Constants.Account.ACCOUNT_DEFAULT,
						msgToRmto.getFieldLength(Iso8583.Bit._102_ACCOUNT_ID_1), '0', false));
			}
			
			//SKIP-TRANSFORM y TRANSFORM
			 
			
			for(int i=3;i<127;i++)
			{
				
				String key1=String.valueOf(i)+"-"+msg.getField(Iso8583.Bit._003_PROCESSING_CODE)+"_"+ pCode126;
				String key2=String.valueOf(i)+"-"+msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
				String key3=String.valueOf(i);
				

				
				String methodName=null;
				
				if(createFieldsResponse.containsKey(key1))
				{
					methodName=createFieldsResponse.get(key1);
					if(!methodName.equals("N/A"))
						msgToRmto.putField(i, invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, i));
					
				}
				else if(createFieldsResponse.containsKey(key2))
				{
					methodName=createFieldsResponse.get(key2);
					if(!methodName.equals("N/A"))
						msgToRmto.putField(i, invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, i));
				}
				else if(createFieldsResponse.containsKey(key3))
				{
					methodName=createFieldsResponse.get(key3);
					if(!methodName.equals("N/A"))
						msgToRmto.putField(i, invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, i));
				}
				if(transformFieldsResponse.containsKey(key1))
				{
					methodName=transformFieldsResponse.get(key1);
					if(!methodName.equals("N/A"))
						msgToRmto.putField(i, invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, i));
					
				}
				else if(transformFieldsResponse.containsKey(key2))
				{
					methodName=transformFieldsResponse.get(key2);
					if(!methodName.equals("N/A"))
						msgToRmto.putField(i, invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, i));
				}
				else if(transformFieldsResponse.containsKey(key3))
				{
					methodName=transformFieldsResponse.get(key3);
					if(!methodName.equals("N/A"))
						msgToRmto.putField(i, invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, i));
				}
			}
			
			//Busca si hay que eliminar campos dado el processingCode
			
			String PCode=msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
			Set<String> set=deleteFieldsResponse.keySet().stream().filter(s->s.length()<=3).collect(Collectors.toSet());
			
			if(set.size()>0)
			{
				for(String item:set)
				{
					if(msgToRmto.isFieldSet(Integer.parseInt(item)))
					{
						msgToRmto.clearField(Integer.parseInt(item));
					}
				}
			}
			
			if(deleteFieldsResponse.containsKey(PCode))
			{
				String[] parts=deleteFieldsResponse.get(PCode).split("-");
				for(String item:parts)
				{
					if(msgToRmto.isFieldSet(Integer.parseInt(item)))
					{
						msgToRmto.clearField(Integer.parseInt(item));
					}
				}
			}
			
			
			
		}
		catch (Exception e) {
			try {
				msgToRmto=null;
				EventRecorder.recordEvent(new TryCatchException(
						new String[] { this.nameInterface, MessageTranslator.class.getName(), "Method: [constructBase24]",
								Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: constructBase24 " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, MessageTranslator.class.getName(),
						"Method: [constructBase24]", Utils.getStringMessageException(e), "Unknown" }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
						"Exception in Method: constructBase24 " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			}
		}
		
		
		return msgToRmto;
	}

	public Iso8583Post constructIso8583(Base24Ath msgFromRemote) 
	{
		Iso8583Post Iso=new Iso8583Post();
		tStart = System.currentTimeMillis();
		String strTypeMsg=msgFromRemote.getMessageType();
		
		try {
			
			Iso.setMessageType(msgFromRemote.getMessageType());
			String strBitmap=this.getBitMap(msgFromRemote);
			StructuredData sd=new StructuredData();
			
			char[] chars=strBitmap.toCharArray();
			
			for(int i=0;i<chars.length;i++)
			{
				if(chars[i]=='1')
				{
					processField(msgFromRemote,Iso,i+1,sd);				
				}
				else
				{
					processCreateField(msgFromRemote,Iso,i+1);
				}
			}
			
			
			
			
			InvokeMethodByConfig invoke=new InvokeMethodByConfig(params);
			ConstructFieldMessage constructor = new ConstructFieldMessage(this.params);
			Iso.putField(Iso8583Post.Bit._123_POS_DATA_CODE, invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
					"constructPosDataCode", msgFromRemote, Iso8583Post.Bit._123_POS_DATA_CODE));
			
			if(msgFromRemote.getMessageType().equals(Iso8583.MsgTypeStr._0220_TRAN_ADV))
			{
				Iso.putField(Iso8583Post.Bit._039_RSP_CODE, constructor.constructP39For0220NotiBloq(msgFromRemote, Iso8583Post.Bit._039_RSP_CODE));
				Iso.putField(63, constructor.constructResponseCodeField63(msgFromRemote, 63));
			}
			else
			{
				Iso.putField(Iso8583Post.Bit._026_POS_PIN_CAPTURE_CODE, constructor.constructPosPinCaptureCode(Iso, Iso8583Post.Bit._026_POS_PIN_CAPTURE_CODE));
				
			}
			

			
			Iso.putPrivField(Iso8583Post.PrivBit._002_SWITCH_KEY,
								constructor.constructSwitchKey(msgFromRemote, this.nameInterface));
			
			
			sd.put(TagNameStructuredData.REQ_TIME, String.valueOf(System.currentTimeMillis() - tStart));
			
			if(strTypeMsg.equals("0420"))
			{
				Iso.putPrivField(Iso8583Post.PrivBit._011_ORIGINAL_KEY, msgFromRemote.getField(Iso8583Post.Bit._090_ORIGINAL_DATA_ELEMENTS).substring(0,32));
				//Iso.putField(Iso8583Post.Bit._090_ORIGINAL_DATA_ELEMENTS, msgFromRemote.getField(Iso8583Post.Bit._090_ORIGINAL_DATA_ELEMENTS));
			}
						
			
			
			Iso.putStructuredData(sd);
					
			
		} catch (Exception e) {
			try {
				Iso=null;
				EventRecorder.recordEvent(new TryCatchException(
						new String[] { this.nameInterface, MessageTranslator.class.getName(), "Method: [constructIso8583]",
								Utils.getStringMessageException(e), msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: constructIso8583 " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			} catch (XPostilion e1) {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface, MessageTranslator.class.getName(),
						"Method: [constructIso8583]", Utils.getStringMessageException(e), "Unknown" }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
						"Exception in Method: constructIso8583 " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			}
		}
		
		return Iso;
	}
	
	public void processField(Base24Ath msg,Iso8583Post isoMsg,int numField,StructuredData sd)
	{
		InvokeMethodByConfig invoke=new InvokeMethodByConfig(params);
		try {
			
			if(GenericInterface.structuredDataFields.containsKey(String.valueOf(numField)))
			{
				sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
						msg.getField(numField));
			}
			else if(GenericInterface.transformFields.containsKey(String.valueOf(numField)))
			{
				if(GenericInterface.transformFieldsMultipleCases.containsKey(String.valueOf(numField)))
				{
					sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
							msg.getField(numField));
					
					String key = String.valueOf(numField)+"-"+msg.getField(3);
					
					//si el methodName es null, significa que no tiene método para transformar por lo tanto se copia el campo
					
					String methodName=GenericInterface.transformFields.get(key);
					
					
					String fieldValue=null;
					
					
					if(methodName!=null && !methodName.equals("N/A"))
					{
						fieldValue=invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, numField);
					}
					else
					{
						fieldValue= msg.getField(numField);
					}
					
					
					isoMsg.putField(numField, fieldValue);
					
				}
				else
				{
					String methodName=GenericInterface.transformFields.get(String.valueOf(numField));
					if(!methodName.equals("N/A"))
					{
					String fieldValue=invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
							methodName, msg, numField);
					
					
					isoMsg.putField(numField, fieldValue);
					sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
							msg.getField(numField));
					}
				}
				
			}
			else if(GenericInterface.skipCopyFields.containsKey(String.valueOf(numField)))
			{
				isoMsg.putField(numField, msg.getField(numField));
				sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
						msg.getField(numField));
			}
			else
			{
				isoMsg.putField(numField, msg.getField(numField));
			}
			
			
		} catch (XPostilion e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	
	
	public void processFieldResponse(Base24Ath msg,Iso8583Post isoMsg,int numField,StructuredData sd) throws XPostilion
	{
		InvokeMethodByConfig invoke=new InvokeMethodByConfig(params);
		try {
			
			if(GenericInterface.structuredDataFields.containsKey(String.valueOf(numField)))
			{
				sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
						msg.getField(numField));
			}
			else if(GenericInterface.transformFields.containsKey(String.valueOf(numField)))
			{
				if(GenericInterface.transformFieldsMultipleCases.containsKey(String.valueOf(numField)))
				{
					sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
							msg.getField(numField));
					
					String key = String.valueOf(numField)+"-"+msg.getField(3);
					
					//si el methodName es null, significa que no tiene método para transformar por lo tanto se copia el campo
					
					String methodName=GenericInterface.transformFields.get(key);
					
					
					String fieldValue=null;
					
					
					if(methodName!=null && !methodName.equals("N/A"))
					{
						fieldValue=invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
								methodName, msg, numField);
					}
					else
					{
						fieldValue= msg.getField(numField);
					}
					
					
					isoMsg.putField(numField, fieldValue);
					
				}
				else
				{
					String methodName=GenericInterface.transformFields.get(String.valueOf(numField));
					
					if(!methodName.equals("N/A"))
					{
					String fieldValue=invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
							methodName, msg, numField);
					
					
					isoMsg.putField(numField, fieldValue);
					sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
							msg.getField(numField));
					}
				}
				
			}
			else if(GenericInterface.skipCopyFields.containsKey(String.valueOf(numField)))
			{
				isoMsg.putField(numField, msg.getField(numField));
				sd.put(new StringBuilder().append(Constants.Config.TAGNAMESD).append(numField).toString(),
						msg.getField(numField));
			}
			else
			{
				isoMsg.putField(numField, msg.getField(numField));
			}
			
			
		} catch (XPostilion e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					MessageTranslator.class.getName(), "Method: [processFieldResponse]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
		}
	}
	
	
	
	
	
	
	
	public void processCreateField(Base24Ath msg,Iso8583Post isoMsg,int numField) throws XPostilion
	{
		InvokeMethodByConfig invoke=new InvokeMethodByConfig(params);
		
		try {
			
			
			if(GenericInterface.createFields.containsKey(String.valueOf(numField)))
			{
				String methodName=GenericInterface.createFields.get(String.valueOf(numField));
				GenericInterface.getLogger().logLine("methodName:"+methodName);
				if(!methodName.equals("N/A"))
				{
				
				String fieldValue=invoke.invokeMethodConfig("postilion.realtime.genericinterface.translate.ConstructFieldMessage",
						methodName, msg, numField);
				GenericInterface.getLogger().logLine("fieldValue:"+fieldValue);
				
				isoMsg.putField(numField, fieldValue);
				}
			}
			
			
		} catch (XPostilion e) {
			// TODO Auto-generated catch block
		
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					MessageTranslator.class.getName(), "Method: [processCreateField]",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
		}
	}

	/**
	 * Place the fields in the original message that come from structured data
	 * 
	 * @param msg
	 * @param sd
	 * @return
	 */
	private Base24Ath constructFieldsFromSd(Base24Ath msg, StructuredData sd,
			Map<String, ConfigAllTransaction> structureMap) {
		Base24Ath msgToRemote = msg;
		try {
			if (sd != null) {
				Enumeration<?> sdFields = sd.getTypeNames();
				while (sdFields.hasMoreElements()) {
					String element = sdFields.nextElement().toString();
					String[] fieldNum = element.split(Constants.Config.UNDERSCORE);
					if (element.contains(Constants.Config.TAGNAMESD) && structureMap.get(fieldNum[2]) != null) {
						msgToRemote.putField(Integer.parseInt(fieldNum[2]), sd.get(element));
					}
				}
			}
		} catch (Exception e) {
			try {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						MessageTranslator.class.getName(), "Method: [constructFieldsFromSd]",
						Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: constructFieldsFromSd " + e.getMessage(), "LOG", this.nameInterface));
			} catch (XPostilion e1) {
				this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
						"Exception in Method:  constructFieldsFromSd: " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			}
		}
		return msgToRemote;
	}
	
	private Base24Ath constructFieldsFromSd(Base24Ath msg, StructuredData sd) {
		Base24Ath msgToRemote = msg;
		try {
			if (sd != null) {
				Enumeration<?> sdFields = sd.getTypeNames();
				while (sdFields.hasMoreElements()) {
					String element = sdFields.nextElement().toString();
					String[] fieldNum = element.split(Constants.Config.UNDERSCORE);
					if (element.contains(Constants.Config.TAGNAMESD)) {
						msgToRemote.putField(Integer.parseInt(fieldNum[2]), sd.get(element));
					}
				}
			}
		} catch (Exception e) {
			try {
				EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
						MessageTranslator.class.getName(), "Method: [constructFieldsFromSd]",
						Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: constructFieldsFromSd " + e.getMessage(), "LOG", this.nameInterface));
			} catch (XPostilion e1) {
				this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
						"Exception in Method:  constructFieldsFromSd: " + Utils.getStringMessageException(e), "LOG",
						this.nameInterface));
			}
		}
		return msgToRemote;
	}

	/**
	 * Construye el Header de un mensaje que va a ser enviado a la Interchange desde
	 * el Nodo Source.
	 *
	 * @param msgFromRemote Mensaje desde ATH.
	 * @return Objeto Header.
	 */
	private Header constructAtmHeaderSourceNode(Base24Ath msgFromRemote) {
		try {
			Header atmHeader = new Header(msgFromRemote.getHeader());
			atmHeader.putField(Header.Field.ISO_LITERAL, Header.Iso.ISO);
			atmHeader.putField(Header.Field.RESPONDER_CODE, Header.SystemCode.HOST);
			atmHeader.putField(Header.Field.PRODUCT_INDICATOR, Header.ProductIndicator.ATM);
			atmHeader.putField(Header.Field.RELEASE_NUMBER, Base24Ath.Version.REL_NR_40);
			atmHeader.putField(Header.Field.STATUS, Header.Status.OK);
			atmHeader.putField(Header.Field.ORIGINATOR_CODE, Header.OriginatorCode.CINCO);
			return atmHeader;
		} catch (Exception e) {
			try {
				EventRecorder.recordEvent(
						new TryCatchException(new String[] { this.nameInterface, MessageTranslator.class.getName(),
								"Method: [constructAtmHeaderSourceNode]", Utils.getStringMessageException(e),
								msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient.sendData(Client.getMsgKeyValue(msgFromRemote.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"Exception in Method: constructAtmHeaderSourceNode " + e.getMessage(), "LOG",
						this.nameInterface));
			} catch (XPostilion e1) {
				this.udpClient.sendData(Client.getMsgKeyValue("Unknown",
						"Exception in Method:  constructAtmHeaderSourceNode: " + Utils.getStringMessageException(e),
						"LOG", this.nameInterface));
			}
		}
		return null;
	}


	/**
	 * Method to construct Iso8583Post error message for to send to TM
	 * 
	 * @param msg     - Message from Remote in Base24
	 * @param keyHash - Configuration key for get message configuration
	 * @param error   - Error information
	 * @return - 0220 Message in format Iso8583Post
	 * @throws XPostilion
	 */
	public Iso8583Post construct0220ToTm(Iso8583 msg, String nameInterchange) throws XPostilion {

		long tStart, tEnd, resultTime;
		tStart = System.currentTimeMillis();

		StructuredData sd = new StructuredData();
		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"ENTRO A  construct0220ToTm", "LOG", this.nameInterface));
		

		Iso8583Post msgToTm = new Iso8583Post();
		msgToTm.putMsgType(Iso8583.MsgType._0220_TRAN_ADV);
		//this.structureMap = this.structureContent.get(keyHash.toString());
		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"structureMap:" + this.structureMap, "LOG", this.nameInterface));
		try {
			
			ConstructFieldMessage cfm=new ConstructFieldMessage(params);
			//Crea los campos 
			for(String key : GenericInterface.createFields220ToTM.keySet())
			{
				
				int intKey=Integer.parseInt(key);
				switch(intKey)
				{
				case 15:
					msgToTm.putField(intKey, cfm.compensationDateValidationP17ToP15(msg, intKey));
					break;
				case 98:
					msgToTm.putField(intKey, cfm.constructField98(msg, intKey));
					break;
				default:
					msgToTm.putField(intKey, cfm.construct0220ErrorFields(msg, intKey));
					break;
				}
				
				
				
			}
			
			msgToTm.putPrivField(Iso8583Post.PrivBit._002_SWITCH_KEY,
					new ConstructFieldMessage(this.params).constructSwitchKey(msgToTm, nameInterchange));
			GenericInterface.getLogger().logLine("EXception message:"+GenericInterface.exceptionMessage);
			
			
			sd.put("ERROR_MESSAGE:", GenericInterface.exceptionMessage);
			msgToTm.putStructuredData(sd);
			

			//msg.putField(Iso8583.Bit._039_RSP_CODE, error.getErrorCodeISO());

		} catch (XPostilion e1) {
			e1.printStackTrace();
		}

		
		return msgToTm;
	}
	
	
	public void constructAutra0800Message(Base24Ath msg, Iso8583Post msgToTM) throws XPostilion {
		msgToTM.putMsgType(Iso8583.MsgType._0800_NWRK_MNG_REQ);

		if (msg.isFieldSet(Iso8583.Bit._007_TRANSMISSION_DATE_TIME))
			msgToTM.putField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME,
					msg.getField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME).toString());

		if (msg.isFieldSet(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR))
			msgToTM.putField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR,
					msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR).toString());

		if (msg.isFieldSet(Iso8583.Bit._012_TIME_LOCAL)) {
			msgToTM.putField(Iso8583.Bit._012_TIME_LOCAL, msg.getField(Iso8583.Bit._012_TIME_LOCAL).toString());
		} else {
			msgToTM.putField(Iso8583.Bit._012_TIME_LOCAL, new DateTime().get(FormatDate.HHMMSS));
		}

		if (msg.isFieldSet(Iso8583.Bit._013_DATE_LOCAL)) {
			msgToTM.putField(Iso8583.Bit._013_DATE_LOCAL, msg.getField(Iso8583.Bit._013_DATE_LOCAL).toString());
		} else {
			msgToTM.putField(Iso8583.Bit._013_DATE_LOCAL, new DateTime().get(FormatDate.MMDD));

		}

		if (msg.isFieldSet(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE))
			msgToTM.putField(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE,
					msg.getField(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE).toString());

		msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "40");

//		msgToTM.putField(Iso8583.Bit._025_POS_CONDITION_CODE, Iso8583.PosCondCode._00_NORMAL_PRESENTMENT);
//		msgToTM.putField(Iso8583.Bit._026_POS_PIN_CAPTURE_CODE, PosPinCaptureCode.FOUR);
//		msgToTM.putField(Iso8583Post.Bit._123_POS_DATA_CODE, General.POSDATACODE);
//		msgToTM.putField(Iso8583.Bit._098_PAYEE, "0054150070650000000000000");

		String OriginalInput = new String(msg.toMsg(false));
		String encodedString = Base64.getEncoder().encodeToString(OriginalInput.getBytes());

		GenericInterface.getLogger().logLine("Original Input B24 : " + OriginalInput);
		GenericInterface.getLogger().logLine("Encoded Input B24 : " + encodedString);

		StructuredData sd = new StructuredData();
		sd.put("B24_Message", encodedString);
		msgToTM.putStructuredData(sd);
		msgToTM.putPrivField(Iso8583Post.PrivBit._002_SWITCH_KEY,
				new ConstructFieldMessage(this.params).constructSwitchKey(msg, "ATM"));
	}
	
	public void constructAutra0810ResponseMessage(Base24Ath msg, Iso8583Post msgToTM) throws XPostilion {

		msgToTM.putMsgType(Iso8583.MsgType._0810_NWRK_MNG_REQ_RSP);

		if (msg.isFieldSet(Iso8583.Bit._007_TRANSMISSION_DATE_TIME))
			msgToTM.putField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME,
					msg.getField(Iso8583.Bit._007_TRANSMISSION_DATE_TIME).toString());

		if (msg.isFieldSet(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR))
			msgToTM.putField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR,
					msg.getField(Iso8583.Bit._011_SYSTEMS_TRACE_AUDIT_NR).toString());

		if (msg.isFieldSet(Iso8583.Bit._012_TIME_LOCAL))
			msgToTM.putField(Iso8583.Bit._012_TIME_LOCAL, msg.getField(Iso8583.Bit._012_TIME_LOCAL).toString());

		if (msg.isFieldSet(Iso8583.Bit._013_DATE_LOCAL))
			msgToTM.putField(Iso8583.Bit._013_DATE_LOCAL, msg.getField(Iso8583.Bit._013_DATE_LOCAL).toString());

		if (msg.isFieldSet(Iso8583.Bit._039_RSP_CODE))
			msgToTM.putField(Iso8583.Bit._039_RSP_CODE, msg.getField(Iso8583.Bit._039_RSP_CODE).toString());

		if (msg.isFieldSet(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE))
			msgToTM.putField(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE,
					msg.getField(Iso8583.Bit._070_NETWORK_MNG_INFO_CODE).toString());

		msgToTM.putField(Iso8583.Bit._100_RECEIVING_INST_ID_CODE, "40");

//		msgToTM.putField(Iso8583.Bit._025_POS_CONDITION_CODE, Iso8583.PosCondCode._00_NORMAL_PRESENTMENT);
//		msgToTM.putField(Iso8583.Bit._026_POS_PIN_CAPTURE_CODE, PosPinCaptureCode.FOUR);
//		msgToTM.putField(Iso8583Post.Bit._123_POS_DATA_CODE, General.POSDATACODE);
//		msgToTM.putField(Iso8583.Bit._098_PAYEE, "0054150070650000000000000");

		String OriginalInput = new String(msg.toMsg(false));
		String encodedString = Base64.getEncoder().encodeToString(OriginalInput.getBytes());

		StructuredData sd = msgToTM.getStructuredData();
		sd.put("B24_MessageRsp", encodedString);
		msgToTM.putStructuredData(sd);
	}

	public Base24Ath constructBase24(Base24Ath msg, Super error) throws XPostilion {
		this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
				"ENTRO AL constructMsgRspToRem0210DeclinedRegExBussines ESTA COLOCANDO ESTO EN EL sWITCH "
						+ msg.getResponseMessageType(),
				"LOG", this.nameInterface));
		Base24Ath msgToRem = new Base24Ath(this.kwa);
		String resposeMessageType = msg.getResponseMessageType();

		ResponseCode responseCode;
		try {
			responseCode = InitialLoadFilter.getFilterCodeIsoToB24(error.getErrorCodeISO(), allCodesIsoToB24TM);
		} catch (NoSuchElementException e) {
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"NoSuchElementException in Method: constructMsgRspToRem0210DeclinedRegExBussines "
							+ Utils.getStringMessageException(e),
					"LOG", this.nameInterface));
			if (new DBHandler(this.params).updateResgistry(error.getErrorCodeISO(), "1")) {
				try {
					allCodesIsoToB24TM = postilion.realtime.library.common.db.DBHandler.getResponseCodes(false, "1",responseCodesVersion);
				} catch (SQLException e1) {
					EventRecorder.recordEvent(new TryCatchException(new String[] { nameInterface,
							ConstructFieldMessage.class.getName(),
							"Method: [constructMsgRspToRem0210DeclinedRegExBussines]",
							Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
					EventRecorder.recordEvent(e);
				}
				responseCode = InitialLoadFilter.getFilterCodeIsoToB24(error.getErrorCodeISO(), allCodesIsoToB24TM);
			} else {
				responseCode = new ResponseCode("10002", "Error Code could not extracted from message",
						error.getErrorCodeISO(), error.getErrorCodeISO());
				EventRecorder.recordEvent(
						new TryCatchException(new String[] { nameInterface, ConstructFieldMessage.class.getName(),
								"Method: [constructMsgRspToRem0210DeclinedRegExBussines]",
								Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
				EventRecorder.recordEvent(e);
				this.udpClient
						.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
								"NoSuchElementException in Method: constructMsgRspToRem0210DeclinedRegExBussines value"
										+ error.getErrorCodeISO() + " is not in the table.",
								"LOG", this.nameInterface));
			}
		}

		Map<String, String> copyFieldsResponse = null;
		Map<String, String> deleteFieldsResponse = null;
		Map<String, String> createFieldsResponse = null;
		Map<String, String> transformFieldsResponse = null;

		switch (resposeMessageType) {
		case "0210":

			copyFieldsResponse = GenericInterface.copyFieldsResponse;
			deleteFieldsResponse = GenericInterface.deleteFieldsResponse;
			createFieldsResponse = GenericInterface.createFieldsResponse;
			transformFieldsResponse = GenericInterface.transformFieldsResponse;

			msgToRem.putMsgType(Iso8583.MsgType._0210_TRAN_REQ_RSP);

			msgToRem.putField(Base24Ath.Bit.ENTITY_ERROR,
					Pack.resize(new StringBuilder().append(responseCode.getKeyIsc())
							// .append(responseCode.getDescriptionIsc().trim())
							.append(error.getDescriptionError()).toString(), General.LENGTH_44, General.SPACE, true));

			break;
		case "0230":

			copyFieldsResponse = GenericInterface.copyFieldsResponseAdv;
			deleteFieldsResponse = GenericInterface.deleteFieldsResponseAdv;
			createFieldsResponse = GenericInterface.createFieldsResponseAdv;
			transformFieldsResponse = GenericInterface.transformFieldsResponseAdv;

			msgToRem.putMsgType(Iso8583.MsgType._0230_TRAN_ADV_RSP);

			msgToRem.putField(Base24Ath.Bit.ENTITY_ERROR,
					Pack.resize(new StringBuilder().append(responseCode.getKeyIsc())
							// .append(responseCode.getDescriptionIsc().trim())
							.append(error.getDescriptionError()).toString(), General.LENGTH_44, General.SPACE, true));

			break;

		case "0430":

			copyFieldsResponse = GenericInterface.copyFieldsResponseRev;
			deleteFieldsResponse = GenericInterface.deleteFieldsResponseRev;
			createFieldsResponse = GenericInterface.createFieldsResponseRev;
			transformFieldsResponse = GenericInterface.transformFieldsResponseRev;

			msgToRem.putMsgType(Iso8583.MsgType._0430_ACQUIRER_REV_ADV_RSP);

			break;
		default:

			break;
		}

		// Copia los campos en el mensaje B24
		for (String key : copyFieldsResponse.keySet()) {

			int intKey = Integer.parseInt(key);
			if (msg.isFieldSet(intKey)) {
				msgToRem.putField(intKey, msg.getField(intKey));
			}

		}
		// Busca si hay que eliminar campos dado el processingCode

		String PCode = msg.getField(Iso8583.Bit._003_PROCESSING_CODE);
		Set<String> set = deleteFieldsResponse.keySet().stream().filter(s -> s.length() <= 3)
				.collect(Collectors.toSet());

		if (set.size() > 0) {
			for (String item : set) {
				if (msgToRem.isFieldSet(Integer.parseInt(item))) {
					msgToRem.clearField(Integer.parseInt(item));
				}
			}
		}

		if (deleteFieldsResponse.containsKey(PCode)) {
			String[] parts = deleteFieldsResponse.get(PCode).split("-");
			for (String item : parts) {
				if (msgToRem.isFieldSet(Integer.parseInt(item))) {
					msgToRem.clearField(Integer.parseInt(item));
				}
			}
		}

		String ProcCode = null;
		String keyHash = null;

		try {
			msgToRem.putHeader(constructAtmHeaderSourceNode(msg));

			try {
				responseCode = InitialLoadFilter.getFilterCodeIsoToB24(error.getErrorCodeISO(),
						this.allCodesIsoToB24TM);
			} catch (NoSuchElementException e) {
				this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
						"NoSuchElementException in Method: constructMsgRspToRem0210DeclinedRegExBussines "
								+ Utils.getStringMessageException(e),
						"LOG", this.nameInterface));
				if (new DBHandler(this.params).updateResgistry(error.getErrorCodeISO(), "1")) {
					this.allCodesIsoToB24TM = postilion.realtime.library.common.db.DBHandler.getResponseCodes(false,
							"1",responseCodesVersion);
					responseCode = InitialLoadFilter.getFilterCodeIsoToB24(error.getErrorCodeISO(),
							this.allCodesIsoToB24TM);
				} else {
					responseCode = new ResponseCode("10002", "Error Code could not extracted from message",
							error.getErrorCodeISO(), error.getErrorCodeISO());
					EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
							ConstructFieldMessage.class.getName(),
							"Method: [constructMsgRspToRem0210DeclinedRegExBussines]",
							Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
					EventRecorder.recordEvent(e);
					this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
							"NoSuchElementException in Method: constructMsgRspToRem0210DeclinedRegExBussines value"
									+ error.getErrorCodeISO() + " is not in the table.",
							"LOG", this.nameInterface));
				}
			}
			msgToRem.putField(Base24Ath.Bit.ENTITY_ERROR,
					Pack.resize(new StringBuilder().append(responseCode.getKeyIsc())
							// .append(responseCode.getDescriptionIsc().trim())
							.append(error.getDescriptionError()).toString(), General.LENGTH_44, General.SPACE, true));

			msgToRem.putField(Iso8583.Bit._039_RSP_CODE, error.getErrorCodeISO());
		} catch (Exception e) {
			msgToRem.putField(Base24Ath.Bit.ENTITY_ERROR, Pack.resize(
					"10002" + "Error Code could not extracted from message", General.LENGTH_44, General.SPACE, true));
			EventRecorder.recordEvent(new TryCatchException(new String[] { this.nameInterface,
					MessageTranslator.class.getName(), "constructMsgRspToRem0210DeclinedRegExBussines",
					Utils.getStringMessageException(e), msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR) }));
			EventRecorder.recordEvent(e);
			this.udpClient.sendData(Client.getMsgKeyValue(msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR),
					"Exception in Method: constructMsgRspToRem0210DeclinedRegExBussines "
							+ Utils.getStringMessageException(e),
					"LOG", this.nameInterface));
		}
		return msgToRem;
	}
	
}