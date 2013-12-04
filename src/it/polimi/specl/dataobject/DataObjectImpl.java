package it.polimi.specl.dataobject;

import it.polimi.specl.helpers.VariablesHelper;
import it.polimi.specl.specl.Predicate;
import it.polimi.specl.specl.Step;
import it.polimi.specl.specl.Values;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.Query;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public class DataObjectImpl implements DataObject {

	/**
	 * {@link LinkedHashMultimap} containing the data
	 */
	private LinkedHashMultimap<String, Object> data;
	
	private static Logger logger = Logger.getLogger(DataObjectImpl.class);

	// ************************
	// ***** CONSTRUCTORS *****
	// ************************

	/**
	 * Default constructor, build an empty {@link LinkedHashMultimap}
	 */
	public DataObjectImpl() {
		data = LinkedHashMultimap.create();
	}

	/**
	 * Builds the {@link DataObject} starting from a passed {@link LinkedHashMultimap}
	 * 
	 * @param data
	 *            the {@link LinkedHashMultimap} from which data are copied
	 */
	public DataObjectImpl(LinkedHashMultimap<String, Object> data) {
		this.data = data;
	}

	/**
	 * Builds the {@link DataObject} as a single pair key-value, where the value is a list of {@link Object} (used for the representation of {@link Values} elements)
	 * 
	 * @param name
	 *            the key name
	 * @param values
	 *            the list of values to associate with the key
	 */
	public DataObjectImpl(String name, List<Object> values) {
		data = LinkedHashMultimap.create();
		for (Object o : values) {
			data.put(name, o); // in this way there's values associated to a key ('data.put(name,values)' would associate a list to a key)
		}
	}

	/**
	 * Builds the {@link DataObject} from the parsing of JSON code or an XML file
	 * 
	 * @param string
	 *            the JSON code or the path of the XML file to parse
	 * @param isJson
	 *            if <code>true</code> the string param is intendes as JSON, otherwise as the path of the XML file to read
	 */
	public DataObjectImpl(String jsonString) {
		data = parseJSON(jsonString);
	}
	
	public DataObjectImpl(Document xml) {
		data = parseXML(xml);
	}

	// ****************************
	// ***** XPATH NAVIGATION *****
	// ****************************

	/**
	 * Method for the evaluation of a Query
	 * 
	 * @param query
	 *            the {@link Query} to evaluate
	 * @return a {@link DataObject} containing the results of the query
	 * @throws Exception
	 *             if the evaluation goes wrong (see the called method for the specific exception)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object evaluate(EList<Step> steps) throws Exception {
		Object current = new DataObjectImpl(data);
		for (Step s : steps) {
			if (s.getPlaceholder() == null) {
				current = getSubmap(current, s.getName(), s.getPredicate());
			}
		}
		if (current instanceof ArrayList && ((ArrayList<Object>) current).size() == 1) {
			current = ((ArrayList<Object>) current).get(0);
		}
		return current;
	}

	/**
	 * Static method for the evaluation of a Query on an {@link ArrayList}
	 * 
	 * @param arrayList
	 *            the {@link ArrayList} containing the objects to evaluate
	 * @param steps
	 *            the {@link Step}s for the navigation
	 * @return the result of the query
	 * @throws Exception
	 *             if the evaluation goes wrong (see the called method for the specific exception)
	 */
	@SuppressWarnings("unchecked")
	public static Object evaluateArray(ArrayList<Object> arrayList, EList<Step> steps) throws Exception {
		Object current = arrayList;
		for (Step s : steps) {
			if (s.getPlaceholder() == null) {
				current = getSubmap(current, s.getName(), s.getPredicate());
			}
		}
		if (current instanceof ArrayList && ((ArrayList<Object>) current).size() == 1) {
			current = ((ArrayList<Object>) current).get(0);
		}
		return current;
	}

	/**
	 * Search the specified key in the DataObject and return the corresponding DataObject value
	 * 
	 * @param current
	 *            the {@link DataObject} in which the search is done
	 * @param property
	 *            the searched key/property in the {@link DataObject}
	 * @return a {@link DataObject} corresponding to the searched value (if found)
	 * @throws Exception
	 *             if the searched property is not found
	 */
	@SuppressWarnings("unchecked")
	private static Object getSubmap(Object current, String property, Predicate predicate) throws Exception {
		if (current instanceof ArrayList) {
			List<Object> list = new ArrayList<>();
			int index = -1;
			if (predicate != null) {
				index = evaluateNumericPredicate(predicate) - 1;
				if (index >= 0) {
					predicate = null;
				}
			}
			for (Object obj : ((ArrayList<Object>) current)) {
				Object subMap = getSubmap(obj, property, predicate);
				if (subMap instanceof ArrayList) {
					for (Object elem : (ArrayList<Object>) subMap) {
//						if (!list.contains(elem)) {
							list.add(elem);
//						}
					}
				} else {
//					if (!list.contains(subMap)) {
						list.add(subMap);
//					}
				}
			}

			if (index >= 0) {
				if (index >= list.size()) {
					throw new Exception("Index out of bound (index = " + index + ", actual size = " + list.size() + ")");
				} else {
					return list.get(index);
				}
			} else {
				if (list.size() == 1) {
					return list.get(0);
				}
				return list;
			}
		} else if (current instanceof DataObject) {
			if (((DataObject) current).keySet().contains(property)) {
				Set<Object> values = ((DataObject) current).get(property);
				// if (values.size() > 1) {
				List<Object> list = new ArrayList<>(values);
				if (predicate != null) {
					int numericPredicate = evaluateNumericPredicate(predicate);
					if (numericPredicate > 0) {
						if (numericPredicate > list.size()) {
							throw new Exception("Index out of bound (index = " + numericPredicate + ", actual size = " + list.size() + ")");
						} else {
							return list.get(numericPredicate - 1);
						}
					} else {
						list.clear();
						Iterator<Object> iter = values.iterator();
						while (iter.hasNext()) {
							Object next = iter.next();
							if (next instanceof DataObject) {
								if (checkPredicate((DataObjectImpl) next, predicate)) {
									list.add(next);
								}
							} else {
								throw new Exception("Could not apply that predicate to element '" + property + "' of type '" + next.getClass().getSimpleName() + "'");
							}
						}
					}
				}

				if (list.size() == 1) {
					return list.get(0);
				}
				return list;
				// } else {
				// return values.iterator().next();
				// }
			}
			throw new Exception("The property '" + property + "' is not contained in '" + current.toString() + "'");
		} else {
			throw new Exception("The property '" + property + "' could not be retrieved from the element '" + current + "' of type '" + current.getClass().getSimpleName() + "'");
		}
	}

	private static int evaluateNumericPredicate(Predicate predicate) throws Exception {
		// check the case in which there's a variable instead of a String or a Double
		if (predicate.getVar() != null) {
			Object num = VariablesHelper.getVariable(predicate.getVar());
			if (num instanceof Double) {
				return (int) (double) num;
			} else if (num == null) {
				throw new Exception("The variable '" + predicate.getVar() + "' is not defined.");
			} else {
				throw new Exception("The variable '" + predicate.getVar() + "' it's not of a numeric type (Value: " + num + ". Class: " + num.getClass().getSimpleName() + ").");
			}
		} else {
			return (int) predicate.getNumber();
		}
	}

	/**
	 * Evaluate the predicates on a passed {@link DataObject}
	 * 
	 * @param current
	 *            {@link DataObject} on which the predicate is verified
	 * @param predicate
	 *            {@link Predicate} to check
	 * @return {@link DataObject} selected from 'current' with the corresponding predicate, <code>null</code> if the predicate is not respected
	 * @throws Exception
	 *             if the searched property is not found
	 */
	private static boolean checkPredicate(DataObjectImpl current, Predicate predicate) throws Exception {
		String key = predicate.getProperty();
		String strValue = predicate.getStrValue();
		String op = predicate.getOp();
		double numericValue = predicate.getNumberValue();

		if (current.containsKey(key)) {
			if (op != null) { // check if it's a complete predicate (eg. /book[title="..."]), other types of predicate (by i-th selection and by varible are already checked)

				// check if the comparison is done with a variable; if necessary retrieve the value
				if (predicate.getVarValue() != null) {
					Object value = VariablesHelper.getVariable(predicate.getVarValue());
					if (value != null) {
						if (value instanceof String) {
							strValue = (String) value;
						} else if (value instanceof Double) {
							numericValue = (double) value;
						} else {
							throw new Exception("The variable '" + predicate.getVarValue() + "' contains a DataObject, unacceptable in predicates.");
						}
					} else {
						throw new Exception("The variable '" + predicate.getVarValue() + "' is not defined.");
					}
				}

				if (strValue != null) { // check if it's a string predicate
					if (op.equals("==") && current.containsEntry(key, strValue)) {
						return true;
					} else if (op.equals("!=") && !current.containsEntry(key, strValue)) {
						return true;
					} else if (!(op.equals("!=") || op.equals("=="))) { // runtime check in the case that the strValue is obtained by a variable (so there's not static analysis of tokens)
						throw new Exception("Unsupported operation '" + op + "' for a String.");
					} else {
						return false;
					}
				} else {
					Set<Object> set = current.get(key);
					Iterator<Object> iter = set.iterator();
					while (iter.hasNext()) {
						switch (op) {
						case "==":
							if ((double) iter.next() == numericValue) {
								return true;
							}
							break;
						case "!=":
							if ((double) iter.next() != numericValue) {
								return true;
							}
							break;
						case ">":
							if ((double) iter.next() > numericValue) {
								return true;
							}
							break;
						case ">=":
							if ((double) iter.next() >= numericValue) {
								return true;
							}
							break;
						case "<":
							if ((double) iter.next() < numericValue) {
								return true;
							}
							break;
						case "<=":
							if ((double) iter.next() <= numericValue) {
								return true;
							}
							break;
						}
					}
					return false;
				}
			}
		}
		throw new Exception("The property '" + key + "' is not contained in '" + current.toString() + "'");
	}

	// ***************************
	// ***** SUPPORT METHODS *****
	// ***************************

	/**
	 * Returns a collection view of all values associated with a key.
	 * 
	 * @param key
	 *            key to search for in the {@link DataObject}
	 * @return the collection of values that the key maps to
	 * @see com.google.common.collect.AbstractSetMultimap#get(Object)
	 */
	@Override
	public Set<Object> get(String property) {
		return data.get(property);

	}

	/**
	 * Returns the i-th element of the {@link DataObject}
	 * 
	 * @param index
	 *            the i-th element to extract
	 * @return the i-th element if present
	 * @throws Exception
	 *             if the index is out of bound
	 */
	@Override
	public Object get(int index) throws Exception {
		Object[] valuesArray = values().toArray();
		if (index >= valuesArray.length) {
			throw new Exception("Index out of bound (index = " + index + ", actual size = " + valuesArray.length + ")");
		}
		return valuesArray[index];
	}

	/**
	 * Returns the size of the {@link DataObject} (the number of pairs key-value)
	 */
	@Override
	public int size() {
		return data.size();
	}

	/**
	 * Return the value of the first entry of the {@link DataObject}
	 * 
	 * @return value corresponding to the first key inserted in the {@link DataObject}
	 */
	@Override
	public Object getFirstValue() {
		return data.values().iterator().next();
	}

	/**
	 * Return <code>true</code> if there is only a value in the {@link DataObject}
	 */
	@Override
	public boolean isSingleValue() {
		// a single value and not a DataObject (potentially containing other values)
		return data.values().size() == 1 && !(this.getFirstValue() instanceof DataObject);
	}

	/**
	 * Returns <code>true</code> if the {@link Multimap} contains no key-value pairs.
	 */
	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	/**
	 * Search the passed target through the values of the {@link DataObject}. The search is deep, so every nested {@link DataObject} value is inspected.
	 * 
	 * @param target
	 *            the {@link Object} to search (possible types are {@link String}, {@link Double} and {@link DataObject})
	 * @return <code>true</code> if the target is found, <code>false</code> otherwise
	 */
	@Override
	public boolean contains(Object target) {

		// if target is a DataObject, for each value contained in the target we try to find it in the current DataObject
		if (target instanceof DataObject) {
			Collection<Object> values = ((DataObject) target).values();
			boolean res = true;
			for (Object o : values) {
				if (!data.containsValue(o)) {
					res = res & false;
					break;
				}
			}
			if (res) {
				return true;
			}
		} else if (data.containsValue(target)) { // if the target is a simple object (String or Double)
			return true;
		}

		Iterator<Object> iter = this.values().iterator();
		while (iter.hasNext()) {
			Object elem = iter.next();
			if (elem instanceof DataObject) {
				if (target instanceof DataObject && elem.equals(target)) { // try a comparison between DataObjects
					return true;
				}
				boolean res = ((DataObject) elem).contains(target);
				if (res) {
					return res;
				}
			}
		}
		return false;
	}

	/**
	 * Returns a collection, which may contain duplicates, of all keys.
	 * 
	 * @return a multiset with keys corresponding to the distinct keys of the multimap and frequencies corresponding to the number of values that each key maps to
	 * @see com.google.common.collect.AbstractSetMultimap#keys()
	 */
	public Multiset<String> keys() {
		return data.keys();
	}

	/**
	 * Returns the set of all keys, each appearing once in the returned set.
	 * 
	 * @return the collection of distinct keys
	 * @see com.google.common.collect.AbstractSetMultimap#keySet()
	 */
	@Override
	public Set<String> keySet() {
		return data.keySet();
	}

	/**
	 * Returns true if the multimap contains any values for the specified key.
	 * 
	 * @param key
	 *            key to search for in multimap
	 * @see com.google.common.collect.AbstractSetMultimap#containsKey(Object)
	 */
	public boolean containsKey(Object key) {
		return data.containsKey(key);
	}

	/**
	 * Returns true if the multimap contains the specified key-value pair.
	 * 
	 * @param key
	 *            key to search for in multimap
	 * @param value
	 *            value to search for in multimap
	 * @see com.google.common.collect.AbstractSetMultimap#containsEntry(Object, Object)
	 */
	public boolean containsEntry(Object key, Object value) {
		return data.containsEntry(key, value);
	}

	/**
	 * Returns a collection of all values in the multimap.
	 * 
	 * @return collection of values, which may include the same value multiple times if it occurs in multiple mappings
	 * @see com.google.common.collect.AbstractSetMultimap#values()
	 */
	@Override
	public Collection<Object> values() {
		return data.values();
	}

	/**
	 * Stores a key-value pair in the multimap.
	 * 
	 * @param key
	 *            key to store in the multimap
	 * @param value
	 *            value to store in the multimap
	 * @return true if the method increased the size of the multimap, or false if the multimap already contained the key-value pair
	 * @see com.google.common.collect.AbstractSetMultimap#put(Object, Object)
	 */
	public boolean put(String key, Object value) {
		return data.put(key, value);
	}

	/**
	 * Copies all of another multimap's key-value pairs into this multimap. The order in which the mappings are added is determined by multimap.entries().
	 * 
	 * @param dataObject
	 *            mappings to store in this multimap
	 * @return true if the multimap changed
	 */
	public boolean putAll(DataObjectImpl dataObject) {
		return data.putAll(dataObject.data);
	}

	/**
	 * Returns <code>true</code> if the passed object is a number, <code>false</code> othewise
	 * 
	 * @param elem
	 *            the object to check
	 */
	private boolean isNumeric(Object elem) {
		if (elem instanceof Double)
			return true;
		try {
			Double.valueOf(elem.toString());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// *******************************
	// ***** XML PARSING METHODS *****
	// *******************************

	/**
	 * Returns the {@link LinkedHashMultimap} corresponding to the input XML file
	 * 
	 * @param xmlPath
	 *            the path of the XML file to parse
	 * @return the corresponding {@link LinkedHashMultimap} parsed from the XML
	 */
//	private LinkedHashMultimap<String, Object> parseXML(String xmlPath) {
//		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//		DocumentBuilder builder;
//		try {
//			builder = factory.newDocumentBuilder();
//			Document doc = builder.parse(new File(xmlPath));
//			doc.getDocumentElement().normalize();
//			Node root = doc.getFirstChild();
//			return stepThroughXML(root).data;
//		} catch (ParserConfigurationException e2) {
//			e2.printStackTrace();
//		} catch (SAXException | IOException e1) {
//			e1.printStackTrace();
//		}
//		return null;
//	}
	
	private LinkedHashMultimap<String, Object> parseXML(Document xml) {
		xml.getDocumentElement().normalize();
		Node root = xml.getFirstChild();
		return stepThroughXML(root).data;
	}

	/**
	 * Navigate recursively through the XML nodes and translate them to a {@link DataObject}
	 * 
	 * @param root
	 *            the root {@link Node} of an XML portion of file
	 * @return a {@link DataObject} conversion of XML
	 */
	private DataObjectImpl stepThroughXML(Node root) {
		String name = ((root.getNodeName().split(":").length == 2) ? root.getNodeName().split(":")[1] : root.getNodeName());
		DataObjectImpl father = new DataObjectImpl();
		DataObjectImpl sons = new DataObjectImpl();

		// clear all the useless nodes
//		for (int i = 0; i < root.getChildNodes().getLength(); i++) {
//			Node s = root.getChildNodes().item(i);
//			if (s.getNodeName().equals("#text") || s.getNodeName().equals("#comment"))
//				root.removeChild(s);
//		}

		for (int i = 0; i < root.getChildNodes().getLength(); i++) {
			if(!(root.getChildNodes().item(i) instanceof Text || root.getChildNodes().item(i) instanceof Comment)){
					
				Node son = root.getChildNodes().item(i);
				String sn = ((son.getNodeName().split(":").length == 2) ? son.getNodeName().split(":")[1] : son.getNodeName());
				// String sv = son.getNodeValue();
				String st = son.getTextContent().trim();
				if (son.getChildNodes().getLength() == 0 || (son.getChildNodes().getLength() == 1 && son.getFirstChild() instanceof Text)) {
					if (!st.equals("")) {
						try {
							if(st.startsWith("0") && !st.startsWith("0.")) { // it's a string
								sons.put(sn, st);
							} else {
								double num = Double.parseDouble(st);
								sons.put(sn, num);
							}
						} catch (NumberFormatException e) {
							sons.put(sn, st);
						}
					} else {
						sons.put(sn, new DataObjectImpl());
					}
				} else {
					sons.putAll(stepThroughXML(son));
				}
			}
		}
		father.put(name, sons);
		return father;
	}

	// ********************************
	// ***** JSON PARSING METHODS *****
	// ********************************

	/**
	 * Parse JSON to DataObject
	 * 
	 * @param json
	 *            the json object to parse
	 * @return a {@link LinkedHashMultimap} corresponding to the JSON input
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private LinkedHashMultimap<String, Object> parseJSON(String json) {
		LinkedHashMultimap<String, Object> dataMap = LinkedHashMultimap.create();
		;
		JSONParser jsonParser = new JSONParser();
		try {
			Map map;
			Object parsedObj = jsonParser.parse(json);
			if(parsedObj instanceof JSONArray) {
				String key = "root";
				Object value = parsedObj;
				map = new JSONObject();
				map.put(key, value);
			} else {
				map = (Map) parsedObj;
			}
			Iterator iter = map.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry next = (Map.Entry) iter.next();
				Object value = next.getValue();
				String key = clean_key((String) next.getKey());
				if (value instanceof JSONObject) {
					dataMap.put(key, stepThroughJSON((JSONObject) value));
				} else if (value instanceof JSONArray) {
					for (int i = 0; i < ((JSONArray) value).size(); i++) {
						Object elem = ((JSONArray) value).get(i);
						if (elem instanceof JSONObject) {
							dataMap.put(key, stepThroughJSON((JSONObject) elem));
						} else {
							if (isNumeric(elem)) {
								dataMap.put(key, Double.valueOf(elem.toString())); // numbers are parsed as Long, so it's converted to Double
							} else {
								dataMap.put(key, elem);
							}
						}
					}
				} else {
					if (isNumeric(value)) {
						dataMap.put(key, Double.valueOf(value.toString())); // numbers are parsed as Long, so it's converted to Double
					} else {
						dataMap.put(key, value);
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dataMap;
	}

	/**
	 * Navigate recursively through the JSON object and converts elements to DataObject
	 * 
	 * @param obj
	 *            the {@link JSONObject} to convert
	 * @return a {@link DataObject} corresponding to the passed {@link JSONObject}
	 */
	@SuppressWarnings("rawtypes")
	private DataObject stepThroughJSON(JSONObject obj) {
		LinkedHashMultimap<String, Object> dataMap = LinkedHashMultimap.create();
		;
		Iterator iterator = obj.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry next = (Map.Entry) iterator.next();
			Object value = next.getValue();
			String key = clean_key((String) next.getKey());
			if (value instanceof JSONObject) {
				dataMap.put(key, stepThroughJSON((JSONObject) value));
			} else if (value instanceof JSONArray) {
				for (int i = 0; i < ((JSONArray) value).size(); i++) {
					Object elem = ((JSONArray) value).get(i);
					if (elem instanceof JSONObject) {
						dataMap.put(key, stepThroughJSON((JSONObject) elem));
					} else {
						if (isNumeric(elem)) {
							dataMap.put(key, Double.valueOf(elem.toString())); // numbers are parsed as Long, so it's converted to Double
						} else {
							dataMap.put(key, elem);
						}
					}
				}
			} else {
				if (isNumeric(value)) {
					dataMap.put(key, Double.valueOf(value.toString())); // numbers are parsed as Long, so it's converted to Double
				} else {
					dataMap.put(key, value);
				}
			}
		}
		return new DataObjectImpl(dataMap);
	}

	/**
	 * Clean a string replacing whitespace with '_' and removing non-alphanumeric characters 
	 * 
	 * @param key the string to clean
	 * @return a string without whitespaces or non-alphanumeric char
	 */
	private String clean_key(String key) {
		Pattern NONLATIN = Pattern.compile("[^\\w-]");  
		Pattern WHITESPACE = Pattern.compile("[\\s]"); 
		String nowhitespace = WHITESPACE.matcher(key).replaceAll("_");  
	    String result = Normalizer.normalize(nowhitespace, Form.NFD);  
	    result = NONLATIN.matcher(result).replaceAll("");  
	    result = result.toLowerCase(Locale.ENGLISH);
	    if(logger.isInfoEnabled() && !key.equals(result)) {
	    	logger.info("JSON PARSING: the key '" + key + "' has been replaced with '" + result + "'");
	    }
	    return result;
	}

	// **************************
	// ***** OBJECT METHODS *****
	// **************************

	@Override
	public String toString() {
		String result = "{";
		for (String key : data.keySet()) {
			result += key + "=" + data.get(key) + ", ";
		}
		if (result.length() > 1) {
			result = result.substring(0, result.length() - 2);
		}
		result += "}";
		return result;

	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DataObject)) {
			return false;
		}

		// check if the maps has same values for the same keys
		return deepEqual(this, (DataObject) o);
	}

	/**
	 * Search and compare every single key-value pair
	 * 
	 * @param a
	 *            the {@link DataObject} to compare
	 * @param b
	 *            the {@link DataObject} to compare
	 * @return <code>true</code> if the {@link DataObject} are deeply equals, <code>false</code> otherwise
	 */
	private boolean deepEqual(DataObject a, DataObject b) {
		Set<String> keysA = a.keySet();
		Set<String> keysB = b.keySet();
		if (keysA.size() != keysB.size() || a.values().size() != b.values().size()) {
			return false;
		}
		for (String keyA : keysA) {
			if (!keysB.contains(keyA)) {
				return false;
			}
			Iterator<Object> iterA = a.get(keyA).iterator();
			Iterator<Object> iterB = b.get(keyA).iterator();
			while (iterA.hasNext() && iterB.hasNext()) {
				Object elemA = iterA.next();
				Object elemB = iterB.next();
				if (elemA instanceof DataObject && elemB instanceof DataObject) {
					boolean res = deepEqual((DataObject) elemA, (DataObject) elemB);
					if (!res) {
						return false;
					}
				} else if (!elemA.equals(elemB)) {
					return false;
				}
			}
		}
		return true;
	}
}
