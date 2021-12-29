package postilion.realtime.genericinterface.translate.util;

import java.util.Arrays;
import java.util.List;

/**
 * Constantes propias de la interfaz y sus componentes.
 * 
 * @author Cristian Cardozo
 *
 */
public class Constants {

	/**
	 * Constantes que indican los indices de los campos.
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public static final class Indexes {
		public static final int RETENTION_PERIOD = 0;
		public static final int VALIDATE_MAC = 1;
		public static final int KWA_NAME = 2;
		public static final int SEND_SIGN_ON = 3;
		public static final int FIELD35_POSITION_0 = 0;
		public static final int FIELD35_POSITION_6 = 6;
		public static final int FIELD41_POSITION_0 = 0;
		public static final int FIELD41_POSITION_8 = 8;
		public static final int FIELD41_POSITION_12 = 12;
		public static final int FIELD41_POSITION_13 = 13;
		public static final int FIELD54_POSITION_0 = 0;
		public static final int FIELD54_POSITION_12 = 12;
		public static final int FIELD54_POSITION_24 = 24;
		public static final int FIELD54_POSITION_36 = 36;
//		public static final int BUSINESS_VALIDATION = 4;
//		public static final int CREATE_0220 = 5;
		public static final int ISSUERID=4;
//		public static final int IP_UDP_SERVER=7;
//		public static final int PORT_UDP_SERVER=8;
//		public static final int CONSULT_COVENANTS=9;
//		public static final int ENCODE_DATA=10;
//		public static final int ROUTING_FIELD_100=11;
//		public static final int EXEPTION_EXPIRY_DATE=12;
//		public static final int URL_CUT_WS=13;
//		public static final int IP_UDP_SERVER_TOKEN_ACTIVATION=14;
//		public static final int PORT_UDP_SERVER_TOKEN_ACTIVATION=15;
	}

	/** Parametros de ejecución de la interfaz. */
	public static final class RuntimeParm {
		public static final int NR_OF_PARAMETERS_EXPECTED = 1;
		public static final String RETENTION_PERIOD = "Retention Period";
		public static final String KWA_NAME = "KWA name";
		public static final String VALIDATE_MAC = "Validation of MAC";
		public static final String VALIDATE_BLOCK_CARD = "Validation of block card";
		public static final String VALIDATE_IP_UDP_SERVER = "Udp server's ip";
		public static final String VALIDATE_PORT_UDP_SERVER = "Udp server's port";
		public static final String KVC_NAME = "KVC name";
	}

	/**
	 * Define algunas propiedades de una cuenta
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public static class Account {
		public static final int NUMBER_RESULT_ACCOUNTS = 6;
		public static final int ID = 0;
		public static final int TYPE = 1;
		public static final int CUSTOMER_ID = 2;
		public static final int CUSTOMER_NAME = 3;
		public static final int CORRECT_PROCESSING_CODE = 4;
		public static final int NUM_FOUR = 4;
		public static final int NUM_SEVENTEEN = 17;
		public static final int PROTECTED_CARD_CLASS = 5;
		public static final int FULL_LENGHT_FIELD_102 = 18;
		public static final String TRUE = "true";
		public static final String FALSE = "false";
		public static final String NIL = "NIL";
		public static final String POSTCARD_DATABASE = "postcard";
		public static final String DEFAULT_PROCESSING_CODE = "000000";
		public static final String CUSTOMER_NO_NAME = "**CLIENTE NO ENCONTRADO**";
		public static final String NO_CARD_RECORD = "**ESTA TARJETA NO EXISTE**";
		public static final String VALIDATE_FIELD_52 = "ERROR FORMATO P52 CON ENTRY_MODE 010";
		public static final String VALIDATE_FIELD_112 = "ERROR TX NO PERMITIDA CON ESTA TARJETA";
		public static final String NO_PROTECTED_CARD_CLASS = "**NO HAY CLASE ASOCIADA A ESTA TARJETA**";
		public static final char ZERO_FILLER = '0';
		public static final String ACCOUNT_DEFAULT = "000000000000000000";
	}

	public static class RoutingAccount {
		public static final String ACCOUNT_SAVINGS_10 = "Savings";
		public static final String ACCOUNT_CHECK_20 = "Check";
	}

	public static class Config {
		public static final int NR_OF_PARAMETERS_EXPECTED = 5;
		public static final String NAME = "GenericInterface";
		public static final String URL_LOG = "D:\\Temp\\log\\GenericInterfaceTest.txt";
		public static final int STARTBITMAPPRIMARY = 16; // Indicates the position where the primary bitmap starts
		public static final int ENDBITMAPPRIMARY = 32; // Indicates the position where the primary bitmap ends
		public static final int STARTBITMAPSECONDARY = 32; // Indicates the position where the secondary bitmap starts
		public static final int ENDBITMAPSECONDARY = 48; // Indicates the position where the secondary bitmap ends
		public static final String IND_BITMAP_SEC = "B"; // First Bit to indicate if it contains secondary BitMap
		public static final int BASE16 = 16; // Parameter to decode in Base 16
		public static final int BASE2 = 2; // Parameter to decode in Base 2
		public static final String FIELDON = "1"; // To validate which fields come in the message
		public static final int STARTBIT1 = 16; // Indicates the initial position of the first bit of the primary bitmap
		public static final int ENDBIT1 = 17; // Indicates the final position of the first bit of the primary bitmap
		public static final String SLASH = "/"; // Indicates the skip action of the field
		public static final int VAL_LENGTH_BITMAPPRIMARY = 62; // Indicates the final position of the first bit of the
																// primary bitmap
		public static final String ADDPOSITIONINITIALBITMAP = "10"; // First and Second Bit in BitMap If not exist
																	// BitMap
																	// Secondary
		public static final String UNDERSCORE = "_"; // Underscore character to build key to hash
		public static final String SPACEREG = "\\s+"; // Regular Expression Space
		public static final String SPACE = ""; // Space
		public static final String WORD_FIELD = "field"; // Space
		public static final String WORD_VALIDATION = "validation"; // Space

		protected static final List<Integer> FIELDSEXCEPTION = Arrays.asList(17, 41, 52, 54, 64, 126, 128); // List of
																											// Fields
																											// with
																											// Format
																											// Exception
		public static final String TAGNAMESD = "B24_Field_"; // Tag name for field 127.22 for field exceptions
		public static final String CARDNUMBER = "CARD_NUMBER";

		public static final String COPY = "C"; // Space
		public static final String BUILD = "B"; // Space
		public static final String TRANS = "T"; // Space
		public static final String SKIP = "S"; // Space
		public static final String SKIP_TRNS = "S-T"; // Space
		public static final String SKIP_BUILD = "S-B"; // Space
		public static final String CREATE = "X"; // Space
		public static final String COPY_TRNS = "C-T"; // Space
		public static final String SKIP_COPY = "S-C"; // Space
		public static final String COPY_TRNS_PRIV = "CTP"; // Space
		public static final String DELETE = "D"; // Space
		public static final String DEFAULT = "DFL"; // Space
		public static final String REQUIRED_FIELD = "CAMPO_REQUERIDO";

		public static final String AMOUNTS_ZERO = "000000000000000000000000000000000000000000";

		public static final String ID_BBOGOTA = "0001";
		public static final String CODE_ERROR_12 = "12";
		public static final String CODE_ERROR_51 = "51";
		public static final String CODE_ERROR_30 = "30";
		public static final String FIELD_B24_63 = "B24_Field_63";
	}

	public static class StatusMsg {
		public static final String SUCCESS = "00";
		public static final String ERROR_PRE_FIELDS = "97";
		public static final String ERROR_PRE_TM = "98";
		public static final String ERROR_POST_TM = "99";

	}

	public final class General {
		public static final String VOIDSTRING = "";
		public static final String NULLSTRING = "null";
		public static final String TRUE = "true";
		public static final String FALSE = "false";
		public static final int UNO = 1;
		public static final String STRING_CERO = "0";
		public static final String STRING_UNO = "1";
		public static final String STRING_DOS = "2";
		public static final String STRING_010 = "010";
		public static final String STRING_054 = "054";
		public static final String POSDATACODE = "911201513344002";
		public static final String RED_ENTRADA = "RED_ENTRADA";
		public static final String ERROR_CODE = "ERROR";
		public static final String COMMISION="COMISION";
		public static final int LENGTH_0 = 0;
		public static final int LENGTH_4 = 4;
		public static final int LENGTH_8 = 8;
		public static final int LENGTH_32 = 32;
		public static final int LENGTH_44 = 44;
		public static final int NUMBER_112 = 112;
		public static final int NUMBER_125 = 125;
		public static final int NUMBER_60 = 60;
        public static final int NUMBER_61 = 61;
        public static final int NUMBER_128 = 128;
		public static final char SPACE = ' ';
		public static final char ZERO = '0';
		public static final String FOUR = "4";
		public static final String SEVEN = "7";
		public static final String TWO_ZEROS = "00";
		public static final String THREE_ZEROS = "000";
		public static final String FOUR_ZEROS = "0000";
		public static final String SIX_ZEROS = "000000";
		public static final String TEN_ZEROS = "0000000000";
		public static final String ELEVEN_ZEROS = "00000000000";
		public static final String TWELVE_ZEROS = "000000000000";
		public static final String SIXTEEN_ZEROS = "0000000000000000";
		public static final String FIFTEEN_ZEROS = "000000000000000";
		public static final String EIGHT_ZEROS = "00000000";
		public static final String TWENTYONE_ZEROS = "000000000000000000000";
		public static final String TWENTYFOUR_ZEROS = "000000000000000000000000";
		public static final String P = "P";
		public static final String AVAL = "Red Aval";
		public static final String TM = "Transaction Manager";
		public static final int ISSUER = 1;
		public static final String COSTINQUIRY89 = "89";
		public static final String NUM_NINETYONE = "91";
		public static final String CASHWITHDRAWAL01 = "01";
		public static final String TAG_ERROR = "ERROR";
		public static final String SIGN_ON = "SIGN-ON";
		public static final String KEYEXCHANGE = "KEYEXCHANGE";
		public static final String ECHO_TEST = "ECHO-TEST";
		public static final String PCODE_DEFAULT_ERROR = "000000";
		public static final String DEFAULT_ERROR_022 = "051";
		public static final String DEFAULT_ERROR_049 = "170";
		public static final String DEFAULT_P22 = "021";
		public static final String DEFAULT_P43 = "ATH  B.POP  TECCARIBE 1300102C/GEN BOLCO";
		public static final String DEFAULT_P41 = "0054000000001   ";
		public static final String DEFAULT_P32 = "10000000054";
		public static final String DEFAULT_P35 = "0000000000000000=00000000000000000000";
		public static final String DEFAULT_P37 = "[005400000000]";
		public static final String DEFAULT_P44 = "0000000000000000000000000";
		public static final String DEFAULT_P48 = "00520000000000000000000000000000000000000000";
		public static final String DEFAULT_P54 = "000005000000000000000000000000150000";
		public static final String DEFAULT_P54_CONSULTA_COSTO = "000000000000000000000000000000000000";
		public static final String DEFAULT_P54_CONSULTA_COSTO_X60 = "000000000000000000000000000000000000000000000000000000000000";
		public static final String DEFAULT_P60 = "0054BOG 0000";
		public static final String DEFAULT_P61 = "0901BBOG0000P";
		public static final String DEFAULT_P62 = "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
		public static final String DEFAULT_P95 = "000000000000000000000000000000000000000000";
		public static final String DEFAULT_100 = "00";
		public static final String DEFAULT_104 = "000000000000000000";
		public static final String DEFAULT_P105 = "0000000000000000000000000";
		public static final String DEFAULT_P105_MIXTA = "1000000000000000000000000";
		public static final String DEFAULT_P128 = "FFFFFFFF00000000";
		public static final String DEFAULT_P125 = "00000000000000000000000000000000000000000000000000000000000";
		public static final String DEFAULT_126 = "& 0000000000! QT00000 0000000000000000000000000000000";
		public static final String DEFAULT_126_TC = "& 0000200054! QT00032 5010300000000000000000000000000 ";
		public static final String NUM_75 = "75";
		public static final String DEFAULT_ORIGINAL_DATA_ELEMENTS = "000000000000000000000000000000000000000000";
		public static final String DEFAULT_RECEIVING_INST_ID_CODE = "00000000000";
		public static final String DEFAULT_INST_ID_CODE = "00000000001";
		public static final String DEFAULT_INST_ID_CODE_MASIVA = "10000000001";
		public static final String NUM_170 = "170";
		public static final String PDC_DEFAULT_FROM_TM = "000000000000002";
		public static final String NUMBER_99 = "99";
		public static final String SPACE_25 = "                         ";
		public static final String _0230 = "0230";
		public static final String _0210 = "0210";
		public static final String _0430 = "0430";
		public static final String NETWORKID_ATH = "0901";
		public static final String NETWORKNAME_ATH = "BBOG";
		public static final String CARD_ISSUER_AUTH = "P";
		public static final String _010 = "010";
		public static final String PIN_DATA_VALIDATION = "FFFFFFFFFFFFFFFF";
		public static final String STRING_TRES = "3";
		public static final String EXCEPTION_BIN_CODTX = "450942";
		public static final String DEFAULT_TRACK2_MASIVA ="5454541234567890D20122211388313500000";
	}

	public static class RouteField100 {
		public static final String F = "F"; // Prefix for fixed balancing
		public static final String B = "B"; // Prefix for load balancing
	}

	public static class Fields {
		public static final int TOKEN126 = 126; // Token
	}

	public static class KeyExchange {
		public static final String KEY_REQUEST = "161";
		public static final String NEW_KEY = "162";
		public static final String SOURCE_KEY_IND = "1";
		public static final String SINK_KEY_IND = "2";
		public static final String KEY_EXCHANGE_FAILED = "ESM";
		public static final String SUCCESSFUL_KEY_EXCHANGE = "RSM";
		public static final String TIPO_LLAVE_PIN = "00";
		public static final String KEY_EXCHANGE_ADDN_DATA = "01M00";
		public static final String CSM_DATA = "CSM(MCL/KSM RCV/BOG              ORG/ATH              KD/";
		public static final String CSM_RSM = "CSM(MCL/RSM RCV/RATH ORG/ATH )";
		public static final String CSM_RSM_161_SRC = "CSM(MCL/RSI RCV/ATH              ORG/BOG              SVR/ )                                                                                          ";
		public static final String CSM_RSM_161_SNK = "CSM(MCL/RSI RCV/RATH ORG/ATH SVR/ )";
		public static final String CSM_ERROR_GRAL_SRC = "CSM(MCL/ESM RCV/RATH ORG/ATH ERF/C )";
		public static final String CSM_ERROR_GRAL_SNK = "CSM(MCL/ESM RCV/RATH ORG/ATH ERF/C )";
		public static final String CSM_ERROR_CTP = "CSM(MCL/ESM RCV/RATH ORG/ATH ERF/P )";
		public static final String CSM_SOURCE_KEY_DATA = " CTP/00000000002B3D )                                        ";
		public static final String CSM_SINK_KEY_DATA = " CTP/1 )";
	}

	public class FormatDate {
		public static final String MMDDHHMMSS = "MMddHHmmss";
		public static final String HHMMSS = "HHmmss";
		public static final String MMDD = "MMdd";
	}

	public static class ChannelID {
		public static final String IATH = "ATM";
		public static final String IATHPb = "ATMPb";
		public static final String ICRD = "Credibanco";
		public static final String ICRDPb = "CredibancoPb";
		public static final String ICOR = "Corresponsal";
		public static final String ICORPb = "CorresponsalPb3";
	}
	
	public static class Standards {
		public static final String B24 = "Base24Ath";
		public static final String ISO = "Iso8583Post";
	}
	
	public static final class ValidationTypes
	{
		public static final String KEY_BUSSINESVALIDATION = "01";
		public static final String KEY_REGULAREXPRESSIONVALIDATION = "02";
		public static final String KEY_CONFIGCONSOLEVALIDATION = "30";
		public static final String KEY_BITMAPVALIDATION = "04";
	}
	
	public static final class Channels
	{
		public static final String ATM = "ATM";
		public static final String CNB = "CNB";
		public static final String CBCO= "CBCO";
		public static final String VTL= "VTL";
		public static final String IVR= "IVR";
		public static final String OFC= "OFC";
		public static final String OFCAVAL= "OFCAVAL";
		//CNB
		//Retiro con tarjeta CNB
		public static final String PCODE_RETIRO_CON_TARJETA_CNB_A = "501043";
		public static final String PCODE_RETIRO_CON_TARJETA_CNB_C = "502043";
		//Pago de servicios CNB
		public static final String PCODE_PAGO_SP_CNB_A = "401000";
		public static final String PCODE_PAGO_SP_CNB_C = "402000";
		//Consulta de Costo CNB
		public static final String PCODE_CONSULTA_DE_COSTO_CNB = "890000";
		//Consulta de saldo y cupo CNB
		public static final String PCODE_CONSULTA_DE_SALDO_Y_CUPO_CNB_A = "311000";
		public static final String PCODE_CONSULTA_DE_SALDO_Y_CUPO_CNB_C = "312000";
		public static final String PCODE_CONSULTA_DE_SALDO_Y_CUPO2_CNB_A = "321000";
		public static final String PCODE_CONSULTA_DE_SALDO_Y_CUPO2_CNB_C = "322000";
		public static final String PCODE_CONSULTA_DE_SALDO_Y_CUPO_CNB_CUPO = "313000";
		//Transferencias CNB
		public static final String PCODE_TRANSFERENCIAS_AHORROS_A_AHORROS = "401010";
		public static final String PCODE_TRANSFERENCIAS_AHORROS_A_CORRIENTE = "401020";
		public static final String PCODE_TRANSFERENCIAS_CORRIENTE_A_AHORROS = "402010";
		public static final String PCODE_TRANSFERENCIAS_CORRIENTE_A_CORRIENTE = "402020";
		//Depositos CNB
		public static final String PCODE_DEPOSITOS_CORRIENTE_A_AHORROS = "402014";
		public static final String PCODE_DEPOSITOS_CORRIENTE_A_CORRIENTE = "402024";
		public static final String PCODE_DEPOSITOS_AHORROS_A_CORRIENTE = "401024";
		public static final String PCODE_DEPOSITOS_AHORROS_A_AHORROS = "401014";
		//Retiro sin tarjeta CNB
		public static final String PCODE_RETIRO_SIN_TARJETA_CNB_DE_AHORROS_A_CNBAHORROS = "401410";
		public static final String PCODE_RETIRO_SIN_TARJETA_CNB_DE_AHORROS_A_CNBCORRIENTE = "401420";
		public static final String PCODE_RETIRO_SIN_TARJETA_CNB_DE_CORRIENTE_A_CNBAHORROS = "402410";
		public static final String PCODE_RETIRO_SIN_TARJETA_CNB_DE_CORRIENTE_A_CNBCORRIENTE = "402420";
		//AVANCES CNB
		public static final String PCODE_AVANCES_TC = "403043";
		//PAGO DE OBLIGACIONES
		public static final String PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_AHORROS = "501000";
		public static final String PCODE_PAGO_OBLIGACIONES_CREDITO_HIPOTECARIO_CORRIENTE = "502000";
		public static final String PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_AHORROS = "501030";
		public static final String PCODE_PAGO_OBLIGACIONES_TARJETA_CREDITO_CORRIENTE = "502030";
		public static final String PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_AHORROS = "501040";
		public static final String PCODE_PAGO_OBLIGACIONES_CREDITOROTATIVO_CREDISERVICES_DINEROEXTRA_CORRIENTE = "502040";
		public static final String PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_AHORROS= "501041";
		public static final String PCODE_PAGO_OBLIGACIONES_OTROS_CREDITOS_CORRIENTE= "502041";
		public static final String PCODE_PAGO_OBLIGACIONES_VEHICULOS_AHORROS= "501042";
		public static final String PCODE_PAGO_OBLIGACIONES_VEHICULOS_CORRIENTE= "502042";
		
		public static final String PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_EFECTIVO= "500100";
		public static final String PCODE_PAGO_OBLIGACIONES_HIPOTECARIO_CHEQUE= "500200";
		
		public static final String PCODE_PAGO_OBLIGACIONES_TC_EFECTIVO= "500130";
		public static final String PCODE_PAGO_OBLIGACIONES_TC_CHEQUE= "500230";
		
		public static final String PCODE_PAGO_OBLIGACIONES_ROTATIVO_EFECTIVO= "500140";
		public static final String PCODE_PAGO_OBLIGACIONES_ROTATIVO_CHEQUE= "500240";
		
		public static final String PCODE_PAGO_OBLIGACIONES_OTROS_EFECTIVO= "500141";
		public static final String PCODE_PAGO_OBLIGACIONES_OTROS_CHEQUE= "500241";
		public static final String PCODE_PAGO_OBLIGACIONES_PAGO_MOTOS_Y_VEHICULOS_EFECTIVO= "500142";
		public static final String PCODE_PAGO_OBLIGACIONES_PAGO_MOTOS_Y_VEHICULOS_CHEQUE= "500242";
		
		//ATM
		public static final String PCODE_RETIRO_ATM_A = "011000";
		public static final String PCODE_RETIRO_ATM_C = "012000";
		public static final String PCODE_CONSULTA_CUENTAS_RELACIONADAS_ATM_A = "891000";
		public static final String PCODE_CONSULTA_CUENTAS_RELACIONADAS_ATM_C = "892000";
		public static final String PCODE_CONSULTA_DE_COSTO_ATM = "890000";		
		public static final String PCODE_CONSULTA_DE_SALDO_ATM_AHORROS = "311000";
		public static final String PCODE_CONSULTA_DE_SALDO_ATM_CORRIENTE = "312000";
		public static final String PCODE_PAGO_CREDITO_HIPOTECARIO_ATM_AHORROS= "501000";
		public static final String PCODE_PAGO_CREDITO_HIPOTECARIO_ATM_CORRIENTE = "502000";
		public static final String PCODE_PAGO_DE_SERVICIOS_ATM_AHO = "401000";
		public static final String PCODE_PAGO_DE_SERVICIOS_ATM_COR = "402000";
		public static final String PCODE_PAGO_DE_TARJETA_CREDITO_ATM_AHO = "501030";
		public static final String PCODE_PAGO_DE_TARJETA_CREDITO_ATM_COR = "502030";
		
		public static final String PCODE_UTILIZACION_CREDITO_ROTATIVO_AHO = "404010";
		public static final String PCODE_UTILIZACION_CREDITO_ROTATIVO_COR = "404020";
		
		public static final String PCODE_CONSULTA_5ULTIMOS_MOVIMIENTOS_ATM_A = "381000";
		public static final String PCODE_CONSULTA_5ULTIMOS_MOVIMIENTOS_ATM_C = "382000";
		
		public static final String PCODE_CONSULTA_CUPO_CREDITO_ROTATIVO_ATM = "314000";
		
		public static final String PCODE_DEPOSITO_ATM_MULTIFUNCIONAL_AHO = "270110";
		public static final String PCODE_DEPOSITO_ATM_MULTIFUNCIONAL_COR = "270120";
		
		public static final String PCODE_PAGO_CREDITO_HIPOTECARIO_EFECTIVO_ATM_MULTIFUNCIONAL = "270100";
		public static final String PCODE_PAGO_CREDITO_ROTATIVO_EFECTIVO_ATM_MULTIFUNCIONAL = "270140";
		public static final String PCODE_PAGO_OTROS_CREDITOS_EFECTIVO_ATM_MULTIFUNCIONAL = "270141";
		
		public static final String PCODE_ACTIVACION_TOKEN = "950000";
		
		//Credibanco
		public static final String PCODE_COMPRA_CREDIBANCO_A = "001000";
		public static final String PCODE_COMPRA_CREDIBANCO_C = "002000";
		
		public static final String PCODE_ANULACION_COMPRA_CREDIBANCO_A = "201000";
		public static final String PCODE_ANULACION_COMPRA_CREDIBANCO_C = "202000";
		
		//Oficina Aval		
		public static final String PCODE_ANULACION_PAGO_CREDITO_HIPOTECARIO_EFECTIVO = "200100";		
		public static final String PCODE_ANULACION_PAGO_TC_EFECTIVO = "200130";		
		public static final String PCODE_ANULACION_PAGO_CREDITO_CUPO_ROTATIVO_EFECTIVO = "200140";		
		public static final String PCODE_ANULACION_PAGO_OTROS_CREDITOS_EFECTIVO = "200141";		
		public static final String PCODE_ANULACION_PAGO_MOTOS_Y_VEHICULOS_EFECTIVO = "200142";		
		public static final String PCODE_ANULACION_PAGO_CREDITO_HIPOTECARIO_CHEQUE = "200200";
		public static final String PCODE_ANULACION_PAGO_TC_CHEQUE = "200230";
		public static final String PCODE_ANULACION_PAGO_CREDITO_CUPO_ROTATIVO_CHEQUE = "200240";
		public static final String PCODE_ANULACION_PAGO_OTROS_CREDITOS_CHEQUE = "200241";
		public static final String PCODE_ANULACION_PAGO_MOTOS_Y_VEHICULOS_CHEQUE = "200242";		
		
		public static final String PCODE_DEVOLUCION_CANJE_PAGO_A_CREDIBANCO_HIPOTECARIO = "230000";
		public static final String PCODE_DEVOLUCION_CANJE_PAGO_A_TARJETA_CREDITO = "233000";
		public static final String PCODE_DEVOLUCION_CANJE_PAGO_A_CUPO_ROTATIVO= "234000";
		public static final String PCODE_DEVOLUCION_CANJE_PAGO_A_OTROS_CREDITOS = "234100";
		public static final String PCODE_DEVOLUCION_CANJE_PAGO_A_CREDITO_MOTO_Y_VEHICULO = "234200";
		
		public static final String PCODE_CONSULTA_TITULARIDAD_CREDITO_HIPOTECARIO ="330000";
		public static final String PCODE_CONSULTA_TITULARIDAD_TARJETA_CREDITO ="333000";
		public static final String PCODE_CONSULTA_TITULARIDAD_CREDITO_ROTATIVO ="334000";
		public static final String PCODE_CONSULTA_TITULARIDAD_OTROS_CREDITOS ="334100";
		public static final String PCODE_CONSULTA_TITULARIDAD_CREDITO_MOTO_VEHICULO ="334200";
		
	}
	public static final class TransactionTypes
	{
		public static final String TT_WITHDRAWAL = "01_512";
	    public static final String TT_REVERSE = "01_1056";
	    public static final String TT_REP_REVERSE = "01_1057";
	    public static final String TT_PAYMENT_CB_MIXT = "40_512_0";
	    public static final String TT_PAYMENT_CB_DEBIT = "40_512_1";
	    public static final String TT_PAYMENT_CB_CREDIT = "40_512_2";
	    public static final String TT_REV_PAYMENT_CB_MIXT = "40_1056_0";
	    public static final String TT_REV_PAYMENT_CB_DEBIT = "40_1056_1";
	    public static final String TT_REV_PAYMENT_CB_CREDIT = "40_1056_2";
	    public static final String TT_REV_REP_PAYMENT_CB_MIXT = "40_1057_0";
	    public static final String TT_REV_REP_PAYMENT_CB_DEBIT = "40_1057_1";
	    public static final String TT_REV_REP_PAYMENT_CB_CREDIT = "40_1057_2";
	    public static final String TT_WITHDRAWAL_CB_ATTD = "50_512_0";
	    public static final String TT_WITHDRAWAL_CB_ATTC = "50_512_1";
	    public static final String TT_WITHDRAWAL_CB_ATTF = "50_512_2";
	    public static final String TT_PAYMENT_OBLIG_CB_MIXT = "50_512_97_0";
	    public static final String TT_PAYMENT_OBLIG_DEBIT = "50_512_97_1";
	    public static final String TT_PAYMENT_OBLIG_CB_CREDIT = "50_512_97_2";
	    public static final String TT_REVERSE_CB_ATTD = "50_1056_0";
	    public static final String TT_REVERSE_CB_ATTC = "50_1056_1";
	    public static final String TT_REVERSE_CB_ATTF = "50_1056_2";
	    public static final String TT_REP_REVERSE_CB_ATTD = "50_1057_0";
	    public static final String TT_REP_REVERSE_CB_ATTC = "50_1057_1";
	    public static final String TT_REP_REVERSE_CB_ATTF = "50_1057_2";
	    public static final String TT_REVERSE_GNS = "00_1056";
	    public static final String TT_REP_REVERSE_GNS = "00_1057";
	    public static final String TT_COST_INQUIRY = "32_512";
	    public static final String TT_BALANCE_INQUIRY_CB = "31_512";
	    public static final String TT_GOOD_N_SERVICES = "00_512";
	    public static final String TT_TRANSFER_CB_ATTD = "40_512_0_99";
	    public static final String TT_TRANSFER_CB_ATTC = "40_512_1_99";
	    public static final String TT_TRANSFER_CB_ATTF = "40_512_2_99";
	    public static final String TT_REV_TRANSFER_CB_ATTD = "40_1056_0_99";
	    public static final String TT_REV_TRANSFER_CB_ATTC = "40_1056_1_99";
	    public static final String TT_REV_TRANSFER_CB_ATTF = "40_1056_2_99";
	    public static final String TT_REV_REP_TRANSFER_CB_ATTD = "40_1057_0_99";
	    public static final String TT_REV_REP_TRANSFER_CB_ATTC = "40_1057_1_99";
	    public static final String TT_REV_REP_TRANSFER_CB_ATTF = "40_1057_2_99";
	    public static final String TT_DEPOSIT_CB_ATTD = "40_512_0_98";
	    public static final String TT_DEPOSIT_CB_ATTC = "40_512_1_98";
	    public static final String TT_DEPOSIT_CB_ATTF = "40_512_2_98";
	    public static final String TT_REV_DEPOSIT_CB_ATTD = "40_1056_0_98";
	    public static final String TT_REV_DEPOSIT_CB_ATTC = "40_1056_1_98";
	    public static final String TT_REV_DEPOSIT_CB_ATTF = "40_1056_2_98";
	    public static final String TT_REV_REP_DEPOSIT_CB_ATTD = "40_1057_0_98";
	    public static final String TT_REV_REP_DEPOSIT_CB_ATTC = "40_1057_1_98";
	    public static final String TT_REV_REP_DEPOSIT_CB_ATTF = "40_1057_2_98";
	    public static final String TT_CARD_PAYMENT = "50_512_96";
	    public static final String TT_MORTGAGE_PAYMENT = "50_512_95";
	    public static final String ATWV="ATWV";
	    public static final String ATWI="ATWI";
	    public static final String ATTF="ATTF";
	    public static final String ATTC="ATTC";
	    public static final String ATTD="ATTD";
	    public static final String POWV="POWV";
	    public static final String POWI="POWI";
	    public static final String ATCO="ATCO";
	    public static final String ATPS="ATPS";
	    public static final String ATPF="ATPF";
	    public static final String ATPG="ATPG";
	    public static final String ATPA="ATPA";
	}
	
	public static final class ErrorTypeCsm {
		public static final int INT_CSM_DATA = 1;
		public static final int INT_CSM_RSM_SRC = 2;
		public static final int INT_CSM_ERROR_GRAL_SRC = 3;
		public static final int INT_CSM_ERROR_CTP = 4;
		public static final int INT_CSM_RSM_SNK = 5;
		public static final int INT_CSM_ERROR_GRAL_SNK = 6;
	}
	
	public static final class KeyExchangeMethod {
		public static final String KEY_REQUEST = new String("161");
		public static String NEW_KEY = new String("162");
		public static String SOURCE_KEY_IND = new String("1");
		public static String SINK_KEY_IND = new String("2");
		public static String KEY_EXCHANGE_FAILED = new String("ESM");
		public static String SUCCESSFUL_KEY_EXCHANGE = new String("RSM");
		public static String TIPO_LLAVE_PIN = new String("00");
		public static String KEY_EXCHANGE_ADDN_DATA = new String("01M00");
		public static String CSM_DATA = new String("CSM(MCL/KSM RCV/0000 ORG/1111 KD/");
		public static String CSM_DATA_ATH = new String("CSM(MCL/KSM RCV/BOG              ORG/ATH              KD/");
		public static String CSM_RSM = new String("CSM(MCL/RSM RCV/RATH ORG/ATH )");
		public static String CSM_RSM_161_SRC = new String("CSM(MCL/RSI RCV/RATH ORG/ATH SVR/ )");
		public static String CSM_RSM_161_SNK = new String("CSM(MCL/RSI RCV/RATH ORG/ATH SVR/ )");
		public static String CSM_ERROR_GRAL_SRC = new String("CSM(MCL/ESM RCV/RATH ORG/ATH ERF/C )");
		public static String CSM_ERROR_GRAL_SNK = new String("CSM(MCL/ESM RCV/RATH ORG/ATH ERF/C )");

		public static String CSM_ERROR_CTP = new String("CSM(MCL/ESM RCV/RATH ORG/ATH ERF/P )");
		public static String CSM_SOURCE_KEY_DATA = new String(" CTP/0 )");
		public static String CSM_SINK_KEY_DATA = new String(" CTP/1 )");
		public static String CSM_SOURCE_KEY_DATA_ATH = new String(" CTP/00000000002B3D )                                        ");
	}
	
	public static final class TransactionRouting {
		public static final int INT_CAPA_DE_INTEGRACION = 0;
		public static final int INT_AUTRA = 1;
	}
	
	public static final class DefaultATM {
		public static String ATM_CHANNEL_NAME= new String("ATM");
		public static String ATM_ID_CHANNEL = new String("AT");
		public static String ATM_CHANNEL = new String("01");
		public static String ATM_VIEW_ROUTER = new String("V1");
		public static String ATM_TRANSACTION_INPUT = new String("ATM_RETIRO");
		public static String ATM_COD_TRANSACTION = new String("20");	
	}
	
	public static final class DefaultOficinasAVAL {
		public static String OFC_TRANSACTION_INPUT= new String("CNB_PAGO_OBLIGACIONES");
		public static String OFC_VIEW_ROUTER= new String("V2");
	
	}
}