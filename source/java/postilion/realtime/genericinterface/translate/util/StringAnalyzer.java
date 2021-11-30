package postilion.realtime.genericinterface.translate.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class lets to analyze strings to identify it
 * 
 * @author Cristian Cardozo
 *
 */
public class StringAnalyzer {

	/**
	 * This method gets special characters that doesn't be spaces or letters among
	 * a-z and A-Z
	 * 
	 * @param value String to analyze
	 * @return ArrayList with the special characters
	 */
	public static ArrayList<String> getSpecialCharaters(String value) {
		Pattern pattern = Pattern.compile("[^a-zA-Z\\s]");
		Matcher matcher = pattern.matcher(value);
		ArrayList<String> specials = new ArrayList<>();
		while (matcher.find()) {
			specials.add(matcher.group());
		}
		return specials;
	}

}
