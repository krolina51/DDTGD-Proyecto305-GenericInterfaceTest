package postilion.realtime.genericinterface.translate.database;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import postilion.realtime.genericinterface.GenericInterface;
import postilion.realtime.genericinterface.Parameters;
import postilion.realtime.genericinterface.eventrecorder.events.SQLExceptionEvent;
import postilion.realtime.genericinterface.translate.util.Utils;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.library.common.util.constants.General;
import postilion.realtime.library.common.util.constants.TagNameStructuredData;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.jdbc.JdbcManager;
import postilion.realtime.sdk.message.bitmap.StructuredData;

/**
 * This class manage the connection and queries to the database. Mainly the
 * realtime database.
 *
 * @author Cristian Cardozo
 */
public class DBHandler {
	
	private Client udpClient = null;
	private String nameInterface = "";
	private Parameters params;
	
	public DBHandler(Parameters params) {
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
		this.params = params;
	}

	public String[] accounts(String bin, String pan, String typeAccount, String expiryDate, String p37) {
		CallableStatement stmt = null;
		Connection con = null;
		ResultSet rs = null;
		String seqNr = Account.NIL;
		int issuerNr;
		String[] result = new String[Account.NUMBER_RESULT_ACCOUNTS];
		result[Account.CUSTOMER_NAME] = Account.NO_CARD_RECORD;
		result[Account.TYPE] = Account.NO_ACCOUNT_LINKED;
		try {
			con = JdbcManager.getConnection(Account.POSTCARD_DATABASE);

			stmt = con.prepareCall(StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME);
			stmt.setString(1, pan);
			stmt.setString(2, expiryDate);
			stmt.registerOutParameter(3, java.sql.Types.VARCHAR);
			stmt.registerOutParameter(4, java.sql.Types.VARCHAR);
			stmt.registerOutParameter(5, java.sql.Types.VARCHAR);
			stmt.registerOutParameter(6, java.sql.Types.INTEGER);
			stmt.registerOutParameter(7, java.sql.Types.VARCHAR);
			stmt.registerOutParameter(8, java.sql.Types.VARCHAR);
			stmt.execute();

			result[Account.CUSTOMER_ID] = stmt.getString(3);
			String defaultAccount = stmt.getString(4);
			result[Account.CUSTOMER_NAME] = stmt.getString(5);
			issuerNr = stmt.getInt(6);
			seqNr = stmt.getString(8);
			StructuredData sd = new StructuredData();
			try {
				sd.fromMsgString(stmt.getString(7));
				result[Account.PROTECTED_CARD_CLASS] = sd.get(TagNameStructuredData.PROTECTED_CARD_CLASS);
			} catch (NullPointerException e) {
				result[Account.PROTECTED_CARD_CLASS] = Account.NO_PROTECTED_CARD_CLASS;
			}
			if (result[Account.CUSTOMER_ID] == null && result[Account.CUSTOMER_NAME] == null) {
				result[Account.CUSTOMER_NAME] = Account.NO_CARD_RECORD;
			} else if (result[Account.CUSTOMER_NAME] == null) {
				result[Account.CUSTOMER_NAME] = Account.CUSTOMER_NO_NAME;
			}
			stmt.close();
			stmt = con.prepareCall(StoreProcedures.CM_LOAD_CARD_ACCOUNTS);
			stmt.setInt(1, issuerNr);
			stmt.setString(2, pan);
			stmt.setString(3, seqNr);
			rs = stmt.executeQuery();
			boolean flagTypeAccount = true;
			boolean flagDefaultAccount = true;
			boolean flagOrderAccount = true;

			while (rs.next() && flagTypeAccount) {
				result[Account.TYPE] = rs.getString(ColumnNames.ACCOUNT_TYPE);
				result[Account.ID] = rs.getString(ColumnNames.ACCOUNT_ID);
				if (result[Account.TYPE].equals(typeAccount)) {
					flagTypeAccount = false;
					result[Account.CORRECT_PROCESSING_CODE] = Account.TRUE;
				} else if (result[Account.TYPE].equals(defaultAccount) && flagDefaultAccount) {
					flagDefaultAccount = false;
					result[Account.CORRECT_PROCESSING_CODE] = Account.FALSE;
				} else if (flagOrderAccount) {
					flagOrderAccount = false;
					result[Account.CORRECT_PROCESSING_CODE] = Account.FALSE;
				}
			}
			JdbcManager.commit(con, stmt, rs);
			return result;

		} catch (SQLException e) {
			e.printStackTrace();
			EventRecorder
					.recordEvent(
							new SQLExceptionEvent(new String[] {
									Account.POSTCARD_DATABASE, StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME
											+ General.VOIDSTRING + StoreProcedures.CM_LOAD_CARD_ACCOUNTS,
									e.getMessage() }));

			StringWriter outError = new StringWriter();
			e.printStackTrace(new PrintWriter(outError));
			this.udpClient
					.sendData(Client.getMsgKeyValue(p37, "catch sp accounts : " + outError.toString(), "LOG", this.nameInterface));

			return result;
		} finally {
			try {
				JdbcManager.cleanup(con, stmt, rs);
			} catch (SQLException e) {
				this.udpClient.sendData(Client.getMsgKeyValue(p37, e.getMessage(), "LOG", this.nameInterface));
				EventRecorder
						.recordEvent(new SQLExceptionEvent(new String[] {
								Account.POSTCARD_DATABASE, StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME
										+ General.VOIDSTRING + StoreProcedures.CM_LOAD_CARD_ACCOUNTS,
								e.getMessage() }));
			}
		}

	}

	public String[] infoAccount(String bin, String pan, String typeAccount, String expiryDate, String p37) {
		CallableStatement stmt = null;
		Connection con = null;
		String[] result = new String[Account.NUMBER_RESULT_ACCOUNTS];
		result[Account.CUSTOMER_NAME] = Account.NO_CARD_RECORD;
		result[Account.TYPE] = Account.NO_ACCOUNT_LINKED;
		try {
			con = JdbcManager.getConnection(Account.POSTCARD_DATABASE);

			stmt = con.prepareCall(StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME);
			stmt.setString(1, pan);
			stmt.setString(2, expiryDate);
			stmt.registerOutParameter(3, java.sql.Types.VARCHAR);
			stmt.registerOutParameter(4, java.sql.Types.VARCHAR);
			stmt.registerOutParameter(5, java.sql.Types.VARCHAR);
			stmt.registerOutParameter(6, java.sql.Types.INTEGER);
			stmt.registerOutParameter(7, java.sql.Types.VARCHAR);
			stmt.registerOutParameter(8, java.sql.Types.VARCHAR);
			stmt.execute();

			result[Account.CUSTOMER_ID] = stmt.getString(3);
//			String defaultAccount = stmt.getString(4);
			result[Account.CUSTOMER_NAME] = stmt.getString(5);
//			int issuerNr = stmt.getInt(6);
//			String seqNr = stmt.getString(8);
			StructuredData sd = new StructuredData();
			try {
				sd.fromMsgString(stmt.getString(7));
				result[Account.PROTECTED_CARD_CLASS] = sd.get(TagNameStructuredData.PROTECTED_CARD_CLASS);
			} catch (NullPointerException e) {
				result[Account.PROTECTED_CARD_CLASS] = Account.NO_PROTECTED_CARD_CLASS;
			}
			if (result[Account.CUSTOMER_ID] == null && result[Account.CUSTOMER_NAME] == null) {
				result[Account.CUSTOMER_NAME] = Account.NO_CARD_RECORD;
			} else if (result[Account.CUSTOMER_NAME] == null) {
				result[Account.CUSTOMER_NAME] = Account.CUSTOMER_NO_NAME;
			}
			JdbcManager.commit(con, stmt);
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			EventRecorder
					.recordEvent(
							new SQLExceptionEvent(new String[] {
									Account.POSTCARD_DATABASE, StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME
											+ General.VOIDSTRING + StoreProcedures.CM_LOAD_CARD_ACCOUNTS,
									e.getMessage() }));

			return result;
		} finally {
			try {
				JdbcManager.cleanup(con, stmt);
			} catch (SQLException e) {
				this.udpClient.sendData(Client.getMsgKeyValue(p37, e.getMessage(), "LOG", this.nameInterface));
				EventRecorder
						.recordEvent(new SQLExceptionEvent(new String[] {
								Account.POSTCARD_DATABASE, StoreProcedures.GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME
										+ General.VOIDSTRING + StoreProcedures.CM_LOAD_CARD_ACCOUNTS,
								e.getMessage() }));
			}
		}
	}

//	/*
//	 * Obtiene de la base de datos las descripciones de las redes adquirentes
//	 * 
//	 *
//	 * @return string con la información consultada
//	 */
//	public static HashMap<String, String> getAcquirerDesc() {
//		HashMap<String, String> AcquirerDesc = new HashMap<>();
//		Connection con = null;
//		ResultSet rs = null;
//		Statement st = null;
//		try {
//			con = JdbcManager.getDefaultConnection();
//
//			String query = "SELECT [key], netWorkDescription FROM acquirer_network_desc";
//			st = con.createStatement();
//
//			rs = st.executeQuery(query);
//
//			while (rs.next()) {
//
//				AcquirerDesc.put(rs.getString("key"), rs.getString("netWorkDescription"));
//			}
//
//			JdbcManager.commit(con, st, rs);
//
//		} catch (SQLException e) {
//			EventRecorder.recordEvent(new SQLExceptionEvent(new String[] { "Realtime",
//					"SELECT [key], netWorkDescription FROM acquirer_network_desc", e.getMessage() }));
//			return AcquirerDesc;
//		} finally {
//			try {
//				JdbcManager.cleanup(con, st, rs);
//			} catch (SQLException e) {
//				StringWriter outError = new StringWriter();
//				e.printStackTrace(new PrintWriter(outError));
//				GenericInterface.getLogger().logLine("catch getAcquirerDesc : " + outError.toString());
//				EventRecorder.recordEvent(new SQLExceptionEvent(new String[] { "Realtime",
//						"SELECT [key], netWorkDescription FROM acquirer_network_desc", e.getMessage() }));
//			}
//
//		}
//
//		return AcquirerDesc;
//	}

	/*
	 * Obtiene de la base de datos los identificadores de institución y sus
	 * respectivos nombres
	 *
	 * @return hashMap con la información consultada
	 */
	public static HashMap<String, String> setInstitutionIds() {
		HashMap<String, String> institutionIds = new HashMap<>();
		Statement st = null;
		ResultSet rs = null;
		Connection con = null;
		try {
			con = JdbcManager.getDefaultConnection();
			String query = "SELECT institution_name, institution FROM institution_ids WITH (NOLOCK)";
			st = con.createStatement();
			rs = st.executeQuery(query);
			while (rs.next()) {
				institutionIds.put(rs.getString("institution_name"), rs.getString("institution"));
			}
			JdbcManager.commit(con, st, rs);
			return institutionIds;
		} catch (SQLException e) {
			EventRecorder.recordEvent(new SQLExceptionEvent(new String[] { "Realtime",
					"SELECT institution_name, institution FROM institution_ids", e.getMessage() }));
			return institutionIds;
		} finally {
			try {
				JdbcManager.cleanup(con, st, rs);
			} catch (SQLException e) {
				StringWriter outError = new StringWriter();
				e.printStackTrace(new PrintWriter(outError));
				GenericInterface.getLogger().logLine("catch setInstitutionIds : " + outError.toString());
				EventRecorder.recordEvent(new SQLExceptionEvent(new String[] { "Realtime",
						"SELECT institution_name, institution FROM institution_ids", e.getMessage() }));
			}

		}
	}

	/**
	 * Activa codigos de respuesta qeu se reciben y no estaban activos en base de
	 * datos
	 * 
	 * @param code    a consultar
	 * @param process 1
	 * 
	 * @return update true si activo false de lo contrario
	 */
	public boolean updateResgistry(String code, String process) {
		boolean update = false;
		Statement st = null;
		ResultSet rs = null;
		Connection con = null;
		int rows = 0;
		try {
			con = JdbcManager.getDefaultConnection();
			st = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = st.executeQuery(String.format(Queries.SELECT_ALL_FROM_CUST_EQUIVALENT_RESPONSE_CODES, code, process));
			rows = new Utils(this.params).countRows(rs);
			boolean go = true;
			if (rows > 0)
				while (go) {
					if (rs.next()) {
						if (!rs.getString(ColumnNames.ACTIVE).equals("1")) {
							st.executeUpdate(String.format(Queries.UPDATE_ACTIVE_CUST_EQUIVALENT_RESPONSE_CODES,
									rs.getString("id")));
							con.commit();
							update = true;
							go = false;
						}
					} else {
						go = false;
					}
				}
			else {
				rs = st.executeQuery(String.format(Queries.SELECT_ALL_FROM_CUST_EQUIVALENT_RESPONSE_CODES, code,
						(process.equals("0") ? "1" : "0")));
				rows = new Utils(this.params).countRows(rs);
				go = true;
				if (rows > 0)
					while (go) {
						if (rs.next()) {
							if (!rs.getString(ColumnNames.ACTIVE).equals("1")) {
								st.executeUpdate(
										String.format(Queries.UPDATE_ACTIVE_AND_PROCESS_CUST_EQUIVALENT_RESPONSE_CODES,
												process, rs.getString("id")));
								update = true;
								con.commit();
								go = false;
							}
						} else {
							go = false;
						}
					}
			}
			JdbcManager.commit(con, st, rs);
		} catch (SQLException e) {
			EventRecorder.recordEvent(new SQLExceptionEvent(new String[] { "Realtime",
					Queries.SELECT_ALL_FROM_CUST_EQUIVALENT_RESPONSE_CODES, Utils.getStringMessageException(e) }));
		} finally {
			try {
				JdbcManager.cleanup(con, st, rs);
			} catch (SQLException e) {
				this.udpClient.sendData(
						Client.getMsgKeyValue("N/A", "Error while cleaning up connection at Method updateResgistry: "
								+ Utils.getStringMessageException(e), "LOG", this.nameInterface));
				EventRecorder.recordEvent(new SQLExceptionEvent(new String[] { "Realtime",
						Queries.SELECT_ALL_FROM_CUST_EQUIVALENT_RESPONSE_CODES, Utils.getStringMessageException(e) }));
			}

		}
		return update;
	}

	/**
	 * Define algunas propiedades de una cuenta
	 *
	 * @author Cristian Cardozo
	 */
	public static class Account {
		public static final int NUMBER_RESULT_ACCOUNTS = 6;
		public static final int ID = 0;
		public static final int TYPE = 1;
		public static final int CUSTOMER_ID = 2;
		public static final int CUSTOMER_NAME = 3;
		public static final int CUSTOMER_NAME_CNB = 2;
		public static final int CORRECT_PROCESSING_CODE = 4;
		public static final int PROTECTED_CARD_CLASS = 5;
		public static final int FULL_LENGHT_FIELD_102 = 18;
		public static final String TRUE = "true";
		public static final String FALSE = "false";
		public static final String NIL = "NIL";
		public static final String POSTCARD_DATABASE = "postcard";
		public static final String DEFAULT_PROCESSING_CODE = "000000";
		public static final String CUSTOMER_NO_NAME = "**CLIENTE NO ENCONTRADO**";
		public static final String NO_CARD_RECORD = "**ESTA TARJETA NO EXISTE**";
		public static final String NO_ACCOUNT_LINKED = "**ESTA TARJETA NO TIENE UNA CUENTA ASOCIADA**";
		public static final String NO_PROTECTED_CARD_CLASS = "**NO HAY CLASE ASOCIADA A ESTA TARJETA**";
		public static final char ZERO_FILLER = '0';
	}

	/**
	 * Almacena los Store Procedures utilizados en la clase
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public static class StoreProcedures {
		public static final String GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME = "{call GET_customer_id_default_account_type_NAME(?,?,?,?,?,?,?,?,?)}";
		public static final String GET_CUSTOMES_ID_DEFAULT_ACCOUNT_TYPE_NAME_CNB = "{call GET_customer_id_default_account_type_NAME_CNB(?,?,?,?,?,?,?)}";
		public static final String GET_ACCOUNTS_CNB = "{call b_get_account(?,?,?)}";
		public static final String GET_ACCOUNT_ADITIONAL_INFO_CNB = "{call pc_get_account_aditional_data(?,?,?)}";

		public static final String CM_LOAD_CARD_ACCOUNTS = "{call cm_load_card_accounts(?,?,?)}";
	}

	/**
	 * Define sentencias a ejecutar en base de datos.
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public static class Queries {
		public static final String SELECT_ALL_FROM_CUST_EQUIVALENT_RESPONSE_CODES = "SELECT * FROM cust_equivalent_response_codes WHERE code_iso = '%s' and process='%s'";
		public static final String UPDATE_ACTIVE_CUST_EQUIVALENT_RESPONSE_CODES = "UPDATE cust_equivalent_response_codes SET active = '1' WHERE id='%s'";
		public static final String UPDATE_ACTIVE_AND_PROCESS_CUST_EQUIVALENT_RESPONSE_CODES = "UPDATE cust_equivalent_response_codes SET active = '1', process = '%s' WHERE id='%s'";
	}

	/**
	 * Define el nombre de las columnas
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public static class ColumnNames {
		public static final String ACCOUNT_TYPE = "account_type";
		public static final String ACCOUNT_ID = "account_id_encrypted";
		public static final String ACTIVE = "active";
	}

}
