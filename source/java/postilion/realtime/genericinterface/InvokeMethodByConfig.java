package postilion.realtime.genericinterface;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import postilion.realtime.genericinterface.translate.MessageTranslator;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.EventReporter;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.message.xml.XMLMessage2;
import postilion.realtime.sdk.util.XPostilion;

public class InvokeMethodByConfig extends MessageTranslator {

	private Client udpClient = null;
	private String nameInterface = "";
	private Parameters params;

	public InvokeMethodByConfig(Parameters params) {
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
		this.params = params;
	}

	/**************************************************************************************
	 * This method of this class executes the class and the message configuration
	 * method
	 * 
	 * @param classReq - Class Name to load
	 * @param method   - Method Name to execute
	 * @param object   - new instance to invoke
	 * @return - the string information result of that was executed
	 * @throws XPostilion
	 *************************************************************************************/
	public String invokeMethodConfig(String classReq, String method, Object object, Integer numfield)
			throws XPostilion {
		String fieldValue = null;

		try {
			Class<?> classRequest = Class.forName(classReq);
			Class<?>[] args = { Parameters.class };
			Constructor<?> constructor = classRequest.getConstructor(args);
			Object obj = constructor.newInstance(this.params);
			Method methodExec = classRequest.getDeclaredMethod(method, Object.class, Integer.class);
			Object result = methodExec.invoke(obj, object, numfield);
			fieldValue = (String) result;

		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException | InstantiationException e) {

			this.udpClient.sendData(Client.getMsgKeyValue("9999", "CLASSREQ:" + classReq, "LOG", nameInterface));
			EventReporter.reportGeneralEvent(this.nameInterface, InvokeMethodByConfig.class.getName(), e,
					((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "invokeMethodConfig",
					this.udpClient);

			try {
				if (object instanceof Iso8583Post) {
					Iso8583Post msg = (Iso8583Post) object;
					EventReporter.reportGeneralEvent(this.nameInterface, InvokeMethodByConfig.class.getName(), e,
							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "invokeMethodConfig", this.udpClient);
				} else if (object instanceof Base24Ath) {
					Base24Ath msg = (Base24Ath) object;
					EventReporter.reportGeneralEvent(this.nameInterface, InvokeMethodByConfig.class.getName(), e,
							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "invokeMethodConfig", this.udpClient);
				}
			} catch (XPostilion e1) {
				EventReporter.reportGeneralEvent(this.nameInterface, InvokeMethodByConfig.class.getName(), e1,
						((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "invokeMethodConfig",
						this.udpClient);
			}

		}
		return fieldValue;
	}

	/**************************************************************************************
	 * This method of this class executes the class and the message configuration
	 * method
	 * 
	 * @param classReq - Class Name to load
	 * @param method   - Method Name to execute
	 * @param object   - new instance to invoke
	 * @return - the object information result of that was executed
	 * @throws XPostilion
	 *************************************************************************************/
	public Object invokeMethod2Config(String classReq, String method, Object object, Integer numfield)
			throws XPostilion {
		Object result = new Object();
		try {
			Class<?> classRequest = Class.forName(classReq);
			Class<?>[] args = { Parameters.class };
			Constructor<?> constructor = classRequest.getConstructor(args);
			Object obj = constructor.newInstance(this.params);
			Method methodExec = classRequest.getDeclaredMethod(method, Object.class, Integer.class);
			result = methodExec.invoke(obj, object, numfield);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
			try {
				if (object instanceof Iso8583Post) {
					Iso8583Post msg = (Iso8583Post) object;
					EventReporter.reportGeneralEvent(this.nameInterface, InvokeMethodByConfig.class.getName(), e,
							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "invokeMethod2Config", this.udpClient);
				} else if (object instanceof Base24Ath) {
					Base24Ath msg = (Base24Ath) object;
					EventReporter.reportGeneralEvent(this.nameInterface, InvokeMethodByConfig.class.getName(), e,
							msg.getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "invokeMethod2Config", this.udpClient);
				}

			} catch (XPostilion e1) {
				EventReporter.reportGeneralEvent(this.nameInterface, InvokeMethodByConfig.class.getName(), e1,
						((Iso8583) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "invokeMethod2Config",
						this.udpClient);
			}
		}
		return result;
	}

	/**************************************************************************************
	 * This method of this class executes the class and the message configuration
	 * method to construct xml message
	 * 
	 * @param classReq  class to execute
	 * @param method    method to execute
	 * @param object    message Base24Ath. An argument for method to execute
	 * @param xmlObject message XMlMessage2. An argument for method to execute
	 * @throws XPostilion
	 * 
	 *************************************************************************************/
	public Object invokeXmlMethodConfig(String classReq, String method, Base24Ath object, Object xmlObject)
			throws XPostilion {
		Object result = new Object();
		try {
			Class<?> classRequest = Class.forName(classReq);
			Class<?>[] args = { Parameters.class };
			Constructor<?> constructor = classRequest.getConstructor(args);
			Object obj = constructor.newInstance(this.params);
			Method methodExec = classRequest.getDeclaredMethod(method, Base24Ath.class, XMLMessage2.class);
			result = methodExec.invoke(obj, object, xmlObject);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
			EventReporter.reportGeneralEvent(this.nameInterface, InvokeMethodByConfig.class.getName(), e,
					((Base24Ath) object).getField(Iso8583.Bit._037_RETRIEVAL_REF_NR), "invokeXmlMethodConfig",
					this.udpClient);
		}
		return result;
	}
}
