package it.polimi.wscol.assertions;

import it.polimi.wscol.dataobject.DataObject;
import it.polimi.wscol.services.WSColGrammarAccess.AssertionQuantifiedBooleanElements;
import it.polimi.wscol.services.WSColGrammarAccess.AssertionQuantifiedNumericElements;
import it.polimi.wscol.wscol.Assertion;
import it.polimi.wscol.wscol.AssertionQuantified;
import it.polimi.wscol.wscol.Assertions;
import it.polimi.wscol.wscol.Expression;
import it.polimi.wscol.wscol.Step;

import org.eclipse.emf.common.util.EList;

public interface AssertionService {

	/**
	 * Verifies the set of {@link Assertions}
	 * 
	 * @param assertions the {@link Assertions} to evaluate
	 * @return <code>true</code> if the whole {@link Assertions} is correct, <code>false</code> otherwise
	 * @throws Exception
	 */
	public boolean verifyAssertions(Assertions assertions) throws Exception;
	
	/**
	 * Resolves a XPath navigation query over the {@link DataObject} corresponding to the passed input file
	 * @param steps the {@link Step}s of the query
	 * @return a result {@link Object} 
	 * @throws Exception if some errors are find during the evaluation
	 */
	public Object resolveQuery(EList<Step> steps) throws Exception;
	
	/**
	 * Resolves numeric expression, containing numbers or navigation query (that will be resolved)
	 * @param assertion the {@link Expression} to resolve
	 * @return the result of the {@link Expression}
	 * @throws Exception if the arguments of the {@link Expression} is not numeric
	 */
	public double resolveExpression(Assertion assertion) throws Exception;
	
	/**
	 * Returns the result of an {@link AssertionQuantified}. {@link AssertionQuantified} are of two types: {@link AssertionQuantifiedNumericElements} if the result is of type {@link Double} and {@link AssertionQuantifiedBooleanElements} if the result is of type {@link Boolean}
	 * 
	 * @param assertion
	 *            the {@link AssertionQuantified} to evaluate
	 * @return a boolean if the assertion is a {@link AssertionQuantifiedBooleanElements}, otherwise a double in the case of {@link AssertionQuantifiedNumericElements}
	 * @throws Exception
	 *             if the selected variable is not of {@link DataObject} type
	 * @throws Exception
	 *             if the chosen variable alias is already used
	 * @throws Exception
	 *             if the variable is not defined
	 * @throws Exception
	 *             if the variable is not of the correct type regarding to the quantifier
	 */
	public Object doAssertionQuantified(Assertion assertion) throws Exception;
}
