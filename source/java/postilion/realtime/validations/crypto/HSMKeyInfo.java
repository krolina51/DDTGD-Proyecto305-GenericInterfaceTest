package postilion.realtime.validations.crypto;

public class HSMKeyInfo {

	private String nameKey = new String();
	private String typeKey = new String();
	private String pinBlockFormat = new String();
	private String ksk_1 = new String();
	private String ksk_2 = new String();
	private String ksk_3 = new String();
	private String checkDigits = new String();

	public HSMKeyInfo(String nk, String tk, String pb, String k1, String cd) {
		this.nameKey = nk;
		this.typeKey = tk;
		this.pinBlockFormat = pb;
		String[] data = parseKeys(k1);
		this.ksk_1 = data[0];
		this.ksk_2 = data[1];
		this.ksk_3 = data[2];
		this.checkDigits = cd;
	}

	public HSMKeyInfo() {
	}

	/**
	 * Parsea las llaves en bloques de 16 digitos
	 * 
	 * @param data
	 * @return
	 */
	public String[] parseKeys(String data) {
		String[] kp = { "", "", "" };
		final int max_key = 16;
		int c = 0;
		// Si no tiene las llaves fijadas
		if (data == null) {
			return kp;
		}

		int tam = data.length();
		int k = 0;
		int pos = 0;
		String tdata = "";

		while (c < tam) {
			k++;
			tdata += data.charAt(c);
			if (k == max_key) {
				kp[pos] = tdata;
				tdata = "";
				k = 0;
				pos++;
			}
			c++;
		}
		return kp;
	}

	public String getNameKey() {
		return nameKey;
	}

	public void setNameKey(String nameKey) {
		this.nameKey = nameKey;
	}

	public String getTypeKey() {
		return typeKey;
	}

	public void setTypeKey(String typeKey) {
		this.typeKey = typeKey;
	}

	public String getPinBlockFormat() {
		return pinBlockFormat;
	}

	public void setPinBlockFormat(String pinBlockFormat) {
		this.pinBlockFormat = pinBlockFormat;
	}

	public String getKsk_1() {
		return ksk_1;
	}

	public void setKsk_1(String ksk_1) {
		this.ksk_1 = ksk_1;
	}

	public String getKsk_2() {
		return ksk_2;
	}

	public void setKsk_2(String ksk_2) {
		this.ksk_2 = ksk_2;
	}

	public String getKsk_3() {
		return ksk_3;
	}

	public void setKsk_3(String ksk_3) {
		this.ksk_3 = ksk_3;
	}

	public String getCheckDigits() {
		return checkDigits;
	}

	public void setCheckDigits(String checkDigits) {
		this.checkDigits = checkDigits;
	}

	public static void main(String[] ar) {
		HSMKeyInfo h = new HSMKeyInfo("A", "B", "C", "123456789123456700000000000000001111111111111111", "E");
		System.out.println("name=" + h.getNameKey());
		System.out.println("tk=" + h.getTypeKey());
		System.out.println("cd=" + h.getCheckDigits());
		System.out.println("pb=" + h.getPinBlockFormat());
		System.out.println("k1=" + h.getKsk_1());
		System.out.println("k2=" + h.getKsk_2());
		System.out.println("k3=" + h.getKsk_3());
	}

}
