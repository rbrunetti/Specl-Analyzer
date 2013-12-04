package it.polimi.specl.dataobject;

import java.util.Collection;
import java.util.Set;

import it.polimi.specl.specl.Step;

import org.eclipse.emf.common.util.EList;

import com.google.common.collect.Multimap;

/**
 * Implementation of the used SDO
 * @author Riccardo Brunetti
 *
 */
public interface DataObject {

	/**
	 * Method for the evaluation of a Query
	 * 
	 * @param query the {@link Query} to evaluate
	 * @return a {@link DataObject} containing the results of the query
	 * @throws Exception if the evaluation goes wrong (see the called method for the specific exception)
	 */
	public Object evaluate(EList<Step> steps) throws Exception;
	
	/**
	 * Returns a collection view of all values associated with a key.
	 * 
	 * @param key key to search for in the {@link DataObject}
	 * @return the collection of values that the key maps to
	 * @see com.google.common.collect.AbstractSetMultimap#get(Object)
	 */
	public Set<Object> get(String property);
	
	/**
	 * Returns the i-th element of the {@link DataObject}
	 *  
	 * @param index the i-th element to extract
	 * @return the i-th element if present
	 * @throws Exception if the index is out of bound
	 */
	public Object get(int index) throws Exception;
	
	/**
	 * Returns the size of the {@link DataObject} (the number of pairs key-value)
	 */
	public int size();
	
	/**
	 * Return the value of the first entry of the {@link DataObject}
	 * 
	 * @return value corresponding to the first key inserted in the {@link DataObject}
	 */
	public Object getFirstValue();

	/**
	 * Return <code>true</code> if there is only a value in the {@link DataObject}
	 */
	public boolean isSingleValue();
	
	/**
	 * Returns <code>true</code> if the {@link Multimap} contains no key-value pairs. 
	 */
	public boolean isEmpty();
	
	/**
	 * Search the passed target through the values of the {@link DataObject}.
	 * The search is deep, so every nested {@link DataObject} value is inspected.
	 * 
	 * @param target the {@link Object} to search (possible types are {@link String}, {@link Double} and {@link DataObject})
	 * @return <code>true</code> if the target is found, <code>false</code> otherwise
	 */
	public boolean contains(Object target);
	
	/**
	 * Returns the set of all keys, each appearing once in the returned set.
	 * 
	 * @return the collection of distinct keys
	 * @see com.google.common.collect.AbstractSetMultimap#keySet()
	 */
	public Set<String> keySet();
	
	/**
	 * Returns a collection of all values in the multimap.
	 * 
	 * @return collection of values, which may include the same value multiple times if it occurs in multiple mappings
	 * @see com.google.common.collect.AbstractSetMultimap#values()
	 */
	public Collection<Object> values();
	
}

/*
 
{\"inventory\":{\"book\": [{\"year\":2000, \"title\":\"Snow Crash\", \"author\":\"Neal Stephenson\", \"publisher\":\"Spectra\", \"isbn\":\"i0553380958\", \"price\":15}, {\"year\":2005, \"title\":\"Burning Tower\", \"author\":[\"Larry Niven\", \"Jerry Pournelle\"], \"publisher\":\"Pocket\", \"isbn\":\"i0743416910\", \"price\":6}, {\"year\":1995, \"title\":\"Zodiac\", \"author\":\"Neal Stephenson\", \"publisher\":\"Spectra\", \"isbn\":{}, \"price\":7.5}]}}"  
  
 */

