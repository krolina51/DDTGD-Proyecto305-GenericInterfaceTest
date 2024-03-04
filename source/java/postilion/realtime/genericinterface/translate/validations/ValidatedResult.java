package postilion.realtime.genericinterface.translate.validations;

import java.util.HashMap;

import postilion.realtime.genericinterface.translate.util.Constants.General;

/**
 * DTO uses to move validation data of Additional ammounts
 * 
 * @author Cristian Cardozo
 *
 */
public class ValidatedResult {

	Boolean ammountStatus = true;
	String descriptionAmmountError = General.VOIDSTRING;
	String errorCode = General.VOIDSTRING;
	String errorCodeP = General.VOIDSTRING;
	HashMap<String, String> dataSFind = new HashMap<String, String>();
//	Object dataSFind = new Object();

	public ValidatedResult() {

	}

	public HashMap<String, String> getDataSFind() {
		return dataSFind;
	}

	public void setDataSFind(HashMap<String, String> dataSFind) {
		this.dataSFind = dataSFind;
	}

	public ValidatedResult(Boolean ammountStatus, String descriptionAmmountError, String errorCode, String errorCodeP) {
		this.ammountStatus = ammountStatus;
		this.descriptionAmmountError = descriptionAmmountError;
		this.errorCode = errorCode;
		this.errorCodeP = errorCodeP;
	}

	public ValidatedResult(Boolean ammountStatus, String descriptionAmmountError, String errorCode, String errorCodeP,
			HashMap<String, String> dataSFind) {
		this.ammountStatus = ammountStatus;
		this.descriptionAmmountError = descriptionAmmountError;
		this.errorCode = errorCode;
		this.errorCodeP = errorCodeP;
		this.dataSFind = dataSFind;
	}

	/**
	 * @return the ammountStatus
	 */
	public Boolean getAmmountStatus() {
		return ammountStatus;
	}

	/**
	 * @param ammountStatus the ammountStatus to set
	 */
	public void setAmmountStatus(Boolean ammountStatus) {
		this.ammountStatus = ammountStatus;
	}

	/**
	 * @return the descriptionAmmountError
	 */
	public String getDescriptionAmmountError() {
		return descriptionAmmountError;
	}

	/**
	 * @param descriptionAmmountError the descriptionAmmountError to set
	 */
	public void setDescriptionAmmountError(String descriptionAmmountError) {
		this.descriptionAmmountError = descriptionAmmountError;
	}

	/**
	 * @return the errorCode
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * @param errorCode the errorCode to set
	 */
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * @return the errorCodeP
	 */
	public String getErrorCodeP() {
		return errorCodeP;
	}

	/**
	 * @param errorCodeP the errorCodeP to set
	 */
	public void setErrorCodeP(String errorCodeP) {
		this.errorCodeP = errorCodeP;
	}

}
