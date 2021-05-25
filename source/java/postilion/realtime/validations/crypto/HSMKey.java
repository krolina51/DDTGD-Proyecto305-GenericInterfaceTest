package postilion.realtime.validations.crypto;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

import postilion.realtime.sdk.jdbc.JdbcManager;
import postilion.realtime.sdk.util.XPostilion;

public class HSMKey {

	public static Hashtable keys_info = new Hashtable();
	public static Hashtable bin_info = new Hashtable();
	public static String ip_hsm = "localhost";
	public static int port_hsm = 0;

	/**
	 * Carga la informacion desde la base de datos al hashtable
	 * 
	 * @throws XPostilion
	 * @throws SQLException
	 */

	public static void loadKeysInformation() throws XPostilion, SQLException {

		Connection cn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;
		try {
			cn = JdbcManager.getDefaultConnection();

			// cn = JdbcUtils.getConnection("jdbc:odbc:realtime", "", "");
			stmt = cn.prepareCall("{call ci_get_keys_info }");
			rs = stmt.executeQuery();

			while (rs.next()) {
				HSMKeyInfo key_info = new HSMKeyInfo(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
						rs.getString(5));
				keys_info.put(key_info.getNameKey(), key_info);

			}

			JdbcManager.commit(cn, stmt, rs);
		}

		finally {

			JdbcManager.cleanup(cn, stmt, rs);
		}
		// carga la configuracion por defecto o seleccionada en Postilion en el HSM
		// Module
		loadHSMDefault();

		loadBinConfig();
	}

	public static HSMKeyInfo getKeyADQ(String nameKey) {

		HSMKeyInfo h = new HSMKeyInfo();

		String d = "CIK_" + nameKey + "_EMV";

		h = (HSMKeyInfo) keys_info.get(d);

		return h;

	}

	public static String getFranquicia(String nameBin, boolean token) {

		String h = (String) bin_info.get(nameBin.substring(0, 6));
		if (h == null) {
			return "N";
		}
		if (token) {
			if (h.equals("V"))
				return "0";
			else
				return "1";
		}
		return h;

	}

	public static void loadHSMDefault() throws XPostilion, SQLException {
		Connection cn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;
		try {
			cn = JdbcManager.getDefaultConnection();
			// cn = JdbcUtils.getConnection("jdbc:odbc:realtime", "", "");
			stmt = cn.prepareCall("{call ci_get_hsms_data }");
			rs = stmt.executeQuery();

			while (rs.next()) {
				ip_hsm = rs.getString(1);
				port_hsm = rs.getInt(2);
			}
			JdbcManager.commit(cn, stmt, rs);

		}

		finally {

			JdbcManager.cleanup(cn, stmt, rs);
		}
	}

	public static void loadBinConfig() throws XPostilion, SQLException {
		Connection cn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;
		try {
			cn = JdbcManager.getDefaultConnection();

			// cn = JdbcUtils.getConnection("jdbc:odbc:realtime", "", "");
			stmt = cn.prepareCall("{call ci_get_bin_config }");
			rs = stmt.executeQuery();

			while (rs.next()) {
				bin_info.put(rs.getString(1), rs.getString(2));
			}

			JdbcManager.commit(cn, stmt, rs);
		}

		finally {

			JdbcManager.cleanup(cn, stmt, rs);
		}

	}

	public static Hashtable loadKeysInformationADQ() throws XPostilion, SQLException {
		Hashtable data = new Hashtable();
		Connection cn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;
		try {

			cn = JdbcManager.getDefaultConnection();

			// cn = JdbcUtils.getConnection("jdbc:odbc:realtime", "", "");
			stmt = cn.prepareCall("{call ci_get_keys_info }");
			rs = stmt.executeQuery();

			while (rs.next()) {
				HSMKeyInfo key_info = new HSMKeyInfo(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
						rs.getString(5));
				data.put(key_info.getNameKey(), key_info);

			}
			JdbcManager.commit(cn, stmt, rs);
		}

		finally {
			JdbcManager.cleanup(cn, stmt, rs);
		}
		return data;
	}
}
