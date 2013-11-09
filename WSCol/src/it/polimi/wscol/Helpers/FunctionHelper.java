package it.polimi.wscol.Helpers;

import it.polimi.wscol.assertions.AssertionServiceImpl;
import it.polimi.wscol.dataobject.DataObject;
import it.polimi.wscol.wscol.Assertion;
import it.polimi.wscol.wscol.Constant;
import it.polimi.wscol.wscol.Function;
import it.polimi.wscol.wscol.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FunctionHelper {

	public static Object applyFunctions(Object result, List<Function> functions) throws Exception {
		if (functions != null) {
			for (Function f : functions) {
				List<Object> params = getFunctionParams(f);
				if (result instanceof DataObject) {
					result = applyDataObjectFunctions(result, f.getName(), params);
				} else if (result instanceof String) {
					result = applyStringFunctions(result, f.getName(), params);
				} else if (result instanceof Double) {
					result = applyDoubleFunctions(result, f.getName(), params);
				} else if (result instanceof ArrayList) {
					result = applyArrayFunctions(result, f);
				}
			}
		}
		return result;
	}

	/**
	 * List of functions for the String results from an {@link Assertion} evaluation
	 * 
	 * @param object
	 *            the String the elaborate
	 * @param function
	 *            the {@link Function} to apply
	 * @return the result of the function
	 * @throws Exception
	 *             if the number of parameter is wrong, according to the function, and prints out the expected number
	 * @throws Exception
	 *             if the type of parameter is wrong, according to the function, and prints out the value and the expected type
	 */
	@SuppressWarnings("null")
	private static Object applyStringFunctions(Object object, String name, List<Object> params) throws Exception {
		switch (name) {
		case "uppercase":
			if (params == null) {
				return ((String) object).toUpperCase();
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 0)");
			}
		case "lowercase":
			if (params == null) {
				return ((String) object).toLowerCase();
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 0)");
			}
		case "length":
			if (params == null) {
				return (double) ((String) object).length();
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 0)");
			}
		case "startsWith":
			if (params != null && params.size() == 1) {
				if (params.get(0) instanceof String) {
					return ((String) object).startsWith((String) params.get(0));
				} else {
					throw new Exception("Wrong type of parameter (" + params.get(0).getClass().getSimpleName() + " instead of a string)");
				}
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 1)");
			}
		case "endsWith":
			if (params != null && params.size() == 1) {
				if (params.get(0) instanceof String) {
					return ((String) object).endsWith((String) params.get(0));
				} else {
					throw new Exception("Wrong type of parameter (" + params.get(0).getClass().getSimpleName() + " instead of a string)");
				}
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 1)");
			}
		case "substring":
			if (params != null && params.size() == 2) {
				if (params.get(0) instanceof Double && params.get(1) instanceof Double) {
					int beginIndex, endIndex;
					if (Math.rint((double) params.get(0)) == (double) params.get(0)) {
						beginIndex = (int) ((double) params.get(0));
					} else {
						throw new Exception("The first parameter in function '" + name + "' is not of type Int.");
					}
					if (Math.rint((double) params.get(1)) == (double) params.get(1)) {
						endIndex = (int) ((double) params.get(1));
					} else {
						throw new Exception("The second parameter in function '" + name + "' is not of type Int.");
					}
					return ((String) object).substring(beginIndex, endIndex);
				} else {
					throw new Exception("Wrong type of parameter (" + params.get(0).getClass().getSimpleName() + " and " + params.get(1).getClass().getSimpleName() + " instead of two numbers)");
				}
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 2)");
			}
		case "replace":
			if (params != null && params.size() == 2) {
				if (params.get(0) instanceof String && params.get(1) instanceof String) {
					return ((String) object).replace((String) params.get(0), (String) params.get(1));
				} else {
					throw new Exception("Wrong type of parameter (" + params.get(0).getClass().getSimpleName() + " and " + params.get(1).getClass().getSimpleName() + " instead of two String)");
				}
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 2)");
			}
		case "contains":
			if(params.size() == 1){
				return ((String) object).contains((String)params.get(1));
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 2)");
			}
		case "concat":
			if(params != null){
				for(int j=0; j<params.size(); j++){
					((String) object).concat((String) params.get(j));
				}
				return object;
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 2)");
			}
		default:
			throw new Exception("Unsupported function '" + name + "' for a " + object.getClass().getSimpleName() + " (value: \"" + object + "\")");
		}
	}

	/**
	 * List of functions for the numeric results from an {@link Assertion} evaluation
	 * 
	 * @param object
	 *            the number (type {@link Double}) the elaborate
	 * @param function
	 *            the {@link Function} to apply
	 * @return the result of the function
	 * @throws Exception
	 *             if the number of parameter is wrong, according to the function, and prints out the expected number
	 */
	private static Object applyDoubleFunctions(Object object, String name, List<Object> params) throws Exception {
		switch (name) {
		case "abs":
			if (params == null) {
				return Math.abs((double) object);
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 2)");
			}
		case "ceiling":
			if(params == null) {
				return Math.ceil((double) object);
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 2)");
			}
		case "floor":
			if (params == null) {
				return Math.floor((double) object);
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 2)");
			}
		case "round":
			if (params == null) {
				return Math.round((double) object);
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 2)");
			}
		case "round-half-to-even":
			if (params == null) {
				return BigDecimal.valueOf((double) object).setScale(0, BigDecimal.ROUND_HALF_EVEN).doubleValue();
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 2)");
			}
		default:
			throw new Exception("Unsupported function '" + name + "' for a " + object.getClass().getSimpleName() + " (value: \"" + object + "\")");
		}
	}

	/**
	 * List of functions for the {@link DataObject} results from an {@link Assertion} evaluation
	 * 
	 * @param object
	 *            the {@link DataObject} the elaborate
	 * @param function
	 *            the {@link Function} to apply
	 * @return the result of the function
	 * @throws Exception
	 *             if the number of parameter is wrong, according to the function, and prints out the expected number
	 */
	private static Object applyDataObjectFunctions(Object object, String name, List<Object> params) throws Exception {
		switch (name) {
		case "contains":
			if (((DataObject) object).isEmpty()) {
				throw new Exception("Function '" + name + "' could not be applied because the DataObject is empty");
			}
			if (params.size() == 1) {
				return ((DataObject) object).contains(params.get(0));
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 1)");
			}
		case "get":
			if (((DataObject) object).isEmpty()) {
				throw new Exception("Function '" + name + "' could not be applied because the DataObject is empty");
			}
			if (params.size() == 1) {
				if (params.get(0) instanceof String) {
					Set<Object> results = ((DataObject) object).get((String) params.get(0));
					if (results.size() == 0) {
						throw new Exception("The property '" + (String) params.get(0) + "' is not contained in '" + ((DataObject) object).toString() + "'");
					} else if (results.size() > 1) {
						throw new Exception("Improper use of 'get' function, you should use the slash navigation instead.");
					}
					return results.iterator().next();
				} else if (params.get(0) instanceof Double) {
					return ((DataObject) object).get((int) (double) params.get(0));
				}
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 1)");
			}
		case "cardinality":
			if (params == null) {
				return (double) ((DataObject) object).size();
			} else {
				throw new Exception("Wrong number of parameters for function '" + name + "' (" + params.size() + " instead of 0)");
			}
		default:
			throw new Exception("Unsupported function '" + name + "' for a " + object.getClass().getSimpleName() + " (value: \"" + object + "\")");
		}
	}
	
	/**
	 * List of functions for the {@link ArrayList} resulting from an {@link Assertion} evaluation
	 * 
	 * @param object
	 *            the {@link ArrayList} the elaborate
	 * @param function
	 *            the {@link Function} to apply
	 * @return the result of the function
	 * @throws Exception
	 *             if the number of parameter is wrong, according to the function, and prints out the expected number
	 */
	@SuppressWarnings("unchecked")
	private static Object applyArrayFunctions(Object object, Function function) throws Exception {
		List<Object> params = getFunctionParams(function);
		switch (function.getName()) {
		case "contains":
			if (((ArrayList<Object>) object).isEmpty()) {
				throw new Exception("Function '" + function.getName() + "' could not be applied because the Array is empty");
			}
			if (params.size() == 1) {
				return ((ArrayList<Object>) object).contains(params.get(0));
			} else {
				throw new Exception("Wrong number of parameters for function '" + function.getName() + "' (" + params.size() + " instead of 1)");
			}
		case "get":
			if (((ArrayList<Object>) object).isEmpty()) {
				throw new Exception("Function '" + function.getName() + "' could not be applied because the Array is empty");
			}
			if (params.size() == 1) {
				if (params.get(0) instanceof Double) {
					return ((ArrayList<Object>) object).get((int) (double) params.get(0));
				} else {
					throw new Exception("Function 'get(key)' could not be applied on a " + object.getClass().getSimpleName() + " (value: \"" + object + "\")");
				}
			} else {
				throw new Exception("Wrong number of parameters for function '" + function.getName() + "' (" + params.size() + " instead of 1)");
			}
		case "cardinality":
			if (params == null) {
				return (double) ((ArrayList<Object>) object).size();
			} else {
				throw new Exception("Wrong number of parameters for function '" + function.getName() + "' (" + params.size() + " instead of 0)");
			}
		default:
			throw new Exception("Unsupported function '" + function.getName() + "' for a " + object.getClass().getSimpleName() + " (value: \"" + object + "\")");
		}
	}
	
	/**
	 * Given the function extract the parameters (resolving the ones related to a variable) and returns a list of value (of different type: {@link String}, {@link Double}, {@link Boolean} or {@link DataObject})
	 * 
	 * @param function
	 *            the {@link Function} to elaborate
	 * @return the list of value
	 * @throws Exception
	 *             if the variable (if present) it's not defined
	 */
	private static List<Object> getFunctionParams(Function function) throws Exception {
		if (function.getParams() != null) {
			List<Object> params = new ArrayList<Object>();
			for (Value v : function.getParams().getValue()) {
				if (!v.getSteps().isEmpty()) {
					params.add(applyFunctions(new AssertionServiceImpl().resolveQuery(v.getSteps()), v.getFunctions()));
				} else if (v instanceof Constant) {
					if (((Constant) v).getString() != null) {
						params.add(((Constant) v).getString());
					} else {
						params.add(((Constant) v).getNumber());
					}
				} else {
					params.add(v);
				}
			}
			return params;
		} else {
			return null;
		}
	}
}
