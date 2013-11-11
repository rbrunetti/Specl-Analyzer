package it.polimi.wscol.helpers;

import java.util.HashMap;
import java.util.Map;

public class VariablesHelper {
	
	private static Map<String, Object> variables;
	
	public VariablesHelper() {
		variables = new HashMap<String, Object>();
	}

	public static Object getVariable(String key) {
		return variables.get(key);
	}

	public static void putVariable(String key, Object value) {
		variables.put(key, value);
	}
	
	public static void removeVariable(String key) {
		variables.remove(key);
	}

}
