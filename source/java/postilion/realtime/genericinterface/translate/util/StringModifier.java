package postilion.realtime.genericinterface.translate.util;

import postilion.realtime.genericinterface.GenericInterface;

/**
 * This class lets to modify strings
 * 
 * @author Cristian Cardozo
 *
 */
public class StringModifier {

	/**
	 * This method lets remove special characters that doesn't be spaces or letters
	 * among a-z and A-Z
	 * 
	 * @param value String to modify
	 * @return string without special characters
	 */
	public static String removeSpecialCharts(String value) {
		value = value.replaceAll("[^a-zA-Z\\s]", " ");
		return value;
	}

	/**
	 * Change the special characters for a normal characters
	 * 
	 * @param value string to modify
	 */
	public static String changeSpecialCharts(String value) {
		for (String i : StringAnalyzer.getSpecialCharaters(value)) {
			value = value.replaceAll(i, getReplaceCharacter(i));
		}
		return value;
	}

	/**
	 * Get the replace character for a special character
	 * 
	 * @param character to replace
	 * @return new character
	 */
	public static String getReplaceCharacter(String character) {
		int i = character.codePoints().sum();
		if (i > 223)
			i = getCodePointToReplace(i - 32) + 32;
		else
			i = getCodePointToReplace(i);
		GenericInterface.getLogger().logLine(i+"");
		return Character.toString(Character.toChars(i)[0]);
	}

	/**
	 * Get code point to replace a special codepoint's character
	 * 
	 * @param i special code point's character
	 * @return normal code point's character
	 */
	public static int getCodePointToReplace(int i) {
		if (192 <= i && i <= 198)
			i = 65;
		else if (i == 199)
			i = 67;
		else if (200 <= i & i <= 203)
			i = 69;
		else if (204 <= i & i <= 207)
			i = 73;
		else if (i == 208)
			i = 68;
		else if (i == 209)
			i = 78;
		else if (210 <= i & i <= 216)
			i = 79;
		else if (217 <= i & i <= 220)
			i = 85;
		else if (221 <= i & i <= 223)
			i = 89;
		return i;
	}
}
