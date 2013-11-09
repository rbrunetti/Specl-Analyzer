package it.polimi.wscol.assertions;

import it.polimi.wscol.WSCoLAnalyser;
import it.polimi.wscol.Helpers.FunctionHelper;
import it.polimi.wscol.Helpers.StringHelper;
import it.polimi.wscol.dataobject.DataObject;
import it.polimi.wscol.dataobject.DataObjectImpl;
import it.polimi.wscol.declaration.DeclarationServiceImpl;
import it.polimi.wscol.services.WSColGrammarAccess.AssertionQuantifiedBooleanElements;
import it.polimi.wscol.services.WSColGrammarAccess.AssertionQuantifiedNumericElements;
import it.polimi.wscol.wscol.Assertion;
import it.polimi.wscol.wscol.AssertionAnd;
import it.polimi.wscol.wscol.AssertionBraced;
import it.polimi.wscol.wscol.AssertionForm;
import it.polimi.wscol.wscol.AssertionNot;
import it.polimi.wscol.wscol.AssertionOr;
import it.polimi.wscol.wscol.AssertionQuantified;
import it.polimi.wscol.wscol.AssertionStdCmp;
import it.polimi.wscol.wscol.Assertions;
import it.polimi.wscol.wscol.Constant;
import it.polimi.wscol.wscol.Expression;
import it.polimi.wscol.wscol.Step;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import it.polimi.wscol.wscol.Div;
import it.polimi.wscol.wscol.Minus;
import it.polimi.wscol.wscol.Multi;
import it.polimi.wscol.wscol.Plus;
import it.polimi.wscol.wscol.Rest;

public class AssertionServiceImpl implements AssertionService {
	
	private static Logger logger = Logger.getRootLogger();
	
	
	public AssertionServiceImpl() {
//		logger.getLoggerRepository().resetConfiguration();
//		logger.addAppender(new ConsoleAppender(new PatternLayout("%5p - %m%n")));
	}
	
	/**
	 * Method for the evaluation of the {@link Assertions}, considering the operation (NOT, AND, OR) and the corresponding priority (NOT>AND>OR)
	 * 
	 * @param assertions
	 *            the list of {@link Assertions} to evaluate
	 * @return <code>true</code> if the {@link Assertions} are respected, <code>false</code> otherwise
	 * @throws Exception
	 *             if there are exception (caused by runtime errors) from the single {@link Assertions}
	 */
	@Override
	public boolean verifyAssertions(Assertions assertions) throws Exception {
		EList<EObject> a = assertions.eContents();
		if (assertions instanceof AssertionAnd) {
			return (verifyAssertions((Assertions) a.get(0)) & verifyAssertions((Assertions) a.get(1)));
		} else if (assertions instanceof AssertionOr) {
			return (verifyAssertions((Assertions) a.get(0)) | verifyAssertions((Assertions) a.get(1)));
		} else if (assertions instanceof AssertionNot) {
			boolean res = !verifyAssertions(((AssertionNot) assertions).getInnerFormula());
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + StringHelper.assertionsToString(assertions) + "' is " + ((res) ? "verified." : "wrong."));
			}
			return res;
		} else if (assertions instanceof AssertionBraced) {
			boolean res = verifyAssertions(((AssertionBraced) assertions).getInnerFormula());
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + StringHelper.assertionsToString(assertions) + "' is " + ((res) ? "verified." : "wrong."));
			}
			return res;
		} else if (assertions instanceof AssertionForm) {
			return verifyAssertionForm((AssertionForm) assertions);
		}
		return false;
	}

	/**
	 * Check an {@link AssertionForm}, in its various forms
	 * 
	 * @param af
	 *            the {@link AssertionForm} to check
	 * @return <code>true</code> if the {@link AssertionForm} is verified, <code>false</code> otherwise
	 * @throws Exception
	 *             if there is data types conflicts, specifying the cause of the error and the erroneous values
	 * @throws Exception
	 *             if there is a generic runtime error, specified with a proper message
	 */
	@SuppressWarnings("unchecked")
	private boolean verifyAssertionForm(AssertionForm af) throws Exception {
		Object laObj, raObj;
		String operation;

		String assertionRepr = StringHelper.assertionFormToString(af);
		String leftToken, rightToken;
		
		if(af instanceof AssertionStdCmp) {
			laObj = doAssertion(((AssertionStdCmp) af).getLeftAssert());
			operation = ((AssertionStdCmp) af).getOp();
			raObj = doAssertion(((AssertionStdCmp) af).getRightAssert());
			leftToken = StringHelper.assertionToString(((AssertionStdCmp) af).getLeftAssert());
			rightToken = StringHelper.assertionToString(((AssertionStdCmp) af).getRightAssert());
		} else {  // so: (af instanceof Assertion | af instanceof AssertionQuantified)
			laObj = doAssertion((Assertion) af);
			operation = "==";
			raObj = true;
			leftToken = StringHelper.assertionToString((Assertion) af);
			rightToken = "";
		}

		// check the objects class and evaluate the corresponding assertion
		if (laObj instanceof Double && raObj instanceof Double) {
			return numericAssertion((double) laObj, (double) raObj, operation, assertionRepr);
		} else if (laObj instanceof String && raObj instanceof String) {
			return stringAssertion((String) laObj, (String) raObj, operation, assertionRepr);
		} else if (laObj instanceof DataObject && raObj instanceof DataObject) {
			return dataobjectAssertion((DataObject) laObj, (DataObject) raObj, operation, assertionRepr);
		} else if (laObj instanceof ArrayList && raObj instanceof ArrayList) {
			return arraylistAssertion((ArrayList<Object>) laObj, (ArrayList<Object>) raObj, operation, assertionRepr);
		} else if (laObj instanceof Boolean && raObj instanceof Boolean) {
			return booleanAssertion((boolean) laObj, (boolean) raObj, operation, assertionRepr);
		} else if (laObj != null && raObj != null) {
			String msg = "Assertion could not be evaluated due to data types conflicts [token: '" + assertionRepr + "']\n";
			// just push the string 10 characters to the right
			msg += String.format("%10s %s", " ", "Left assertion [token: '" + leftToken + "'] = " + laObj + " (Class: " + laObj.getClass().getSimpleName() + ")\n");
			msg += String.format("%10s %s", " ", "Right assertion [token: '" + rightToken + "'] = " + raObj + " (Class: " + raObj.getClass().getSimpleName() + ")\n");
			throw new Exception(msg);
		} else {
			throw new Exception("Unable to evaluate the assertion, due to erroneous variables declaration [token: '" + assertionRepr + "']");
		}
	}

	/**
	 * Method for the evaluation of numeric {@link AssertionForm}
	 * 
	 * @param left
	 *            the result of the left part of the {@link AssertionForm}
	 * @param right
	 *            the result of the right part of the {@link AssertionForm}
	 * @param operation
	 *            a {@link String} containing the operation to evaluate
	 * @param condition
	 *            a {@link String} representation of the {@link AssertionForm}
	 * @return <code>true</code> if the {@link AssertionForm} is verified, <code>false</code> otherwise
	 * @throws Exception
	 *             if the operation is not supported
	 */
	private boolean numericAssertion(double left, double right, String operation, String condition) throws Exception {
		boolean result;
		switch (operation) {
		case ">":
			if (left > right) {
				result = true;
			} else {
				result = false;
			}
			break;
		case ">=":
			if (left >= right) {
				result = true;
			} else {
				result = false;
			}
			break;
		case "<":
			if (left < right) {
				result = true;
			} else {
				result = false;
			}
			break;
		case "<=":
			if (left <= right) {
				result = true;
			} else {
				result = false;
			}
			break;
		case "==":
			if (left == right) {
				result = true;
			} else {
				result = false;
			}
			break;
		case "!=":
			if (left != right) {
				result = true;
			} else {
				result = false;
			}
			break;
		default:
			String msg = "Unsopported operation '" + operation + "' for the assertion between two String [token: '" + condition + "']\n";
			msg += String.format("%10s %s", " ", "Left assertion = '" + left + "'\n");
			msg += String.format("%10s %s", " ", "Right assertion = '" + right + "'\n");
			throw new Exception(msg);
		}

		if (result) {
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + condition + "' is verified.");
			}
		} else {
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + condition + "' is wrong.");
			}
		}

		return result;
	}

	/**
	 * Method for the evaluation of string {@link AssertionForm}
	 * 
	 * @param left
	 *            the result of the left part of the {@link AssertionForm}
	 * @param right
	 *            the result of the right part of the {@link AssertionForm}
	 * @param operation
	 *            a {@link String} containing the operation to evaluate
	 * @param condition
	 *            a {@link String} representation of the {@link AssertionForm}
	 * @return <code>true</code> if the {@link AssertionForm} is verified, <code>false</code> otherwise
	 * @throws Exception
	 *             if the operation is not supported
	 */
	private boolean stringAssertion(String left, String right, String operation, String condition) throws Exception {
		boolean result;
		switch (operation) {
		case "==":
			if (left.equals(right)) {
				result = true;
			} else {
				result = false;
			}
			break;
		case "!=":
			if (!left.equals(right)) {
				result = true;
			} else {
				result = false;
			}
			break;
		default:
			String msg = "Unsopported operation '" + operation + "' for the assertion between two String [token: '" + condition + "']\n";
			msg += String.format("%10d", "\n Left assertion = '" + left + "'\n");
			msg += String.format("%10d", "\n Right assertion = '" + right + "'\n");
			throw new Exception(msg);
		}

		if (result) {
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + condition + "' is verified.");
			}
		} else {
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + condition + "' is wrong.");
			}
		}

		return result;
	}

	/**
	 * Method for the evaluation of boolean {@link AssertionForm}
	 * 
	 * @param left
	 *            the result of the left part of the {@link AssertionForm}
	 * @param right
	 *            the result of the right part of the {@link AssertionForm}
	 * @param operation
	 *            a {@link String} containing the operation to evaluate
	 * @param condition
	 *            a {@link String} representation of the {@link AssertionForm}
	 * @return <code>true</code> if the {@link AssertionForm} is verified, <code>false</code> otherwise
	 * @throws Exception
	 *             if the operation is not supported
	 */
	private boolean booleanAssertion(boolean left, boolean right, String operation, String condition) throws Exception {
		boolean result;
		switch (operation) {
		case "==":
			result = left == right;
			break;
		case "!=":
			result = left != right;
			break;
		default:
			String msg = "Unsopported operation '" + operation + "' for the assertion between two Boolean [token: '" + condition + "']\n";
			msg += String.format("%10s %s", " ", "Left assertion = '" + left + "'\n");
			msg += String.format("%10s %s", " ", "Right assertion = '" + right + "'\n");
			throw new Exception(msg);
		}

		if (result) {
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + condition + "' is verified.");
			}
		} else {
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + condition + "' is wrong.");
			}
		}

		return result;
	}

	/**
	 * Method for the evaluation of DataObject {@link AssertionForm}
	 * 
	 * @param left
	 *            the result of the left part of the {@link AssertionForm}
	 * @param right
	 *            the result of the right part of the {@link AssertionForm}
	 * @param operation
	 *            a {@link String} containing the operation to evaluate
	 * @param condition
	 *            a {@link String} representation of the {@link AssertionForm}
	 * @return <code>true</code> if the {@link AssertionForm} is verified, <code>false</code> otherwise
	 * @throws Exception
	 *             if the operation is not supported
	 */
	private boolean dataobjectAssertion(DataObject left, DataObject right, String operation, String condition) throws Exception {
		boolean result;
		switch (operation) {
		case "==":
			if (left.equals(right)) {
				result = true;
			} else {
				result = false;
			}
			break;
		case "!=":
			if (!left.equals(right)) {
				result = true;
			} else {
				result = false;
			}
			break;
		default:
			String msg = "Unsopported operation '" + operation + "' for the assertion between two DataObject [token: '" + condition + "']\n";
			msg += String.format("%10s %s", " ", "Left assertion = '" + left + "'\n");
			msg += String.format("%10s %s", " ", "Right assertion = '" + right + "'\n");
			throw new Exception(msg);
		}

		if (result) {
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + condition + "' is verified.");
			}
		} else {
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + condition + "' is wrong.");
			}
		}

		return result;
	}

	/**
	 * Method for the evaluation of ArrayList {@link AssertionForm}
	 * 
	 * @param left
	 *            the result of the left part of the {@link AssertionForm}
	 * @param right
	 *            the result of the right part of the {@link AssertionForm}
	 * @param operation
	 *            a {@link String} containing the operation to evaluate
	 * @param condition
	 *            a {@link String} representation of the {@link AssertionForm}
	 * @return <code>true</code> if the {@link AssertionForm} is verified, <code>false</code> otherwise
	 * @throws Exception
	 *             if the operation is not supported
	 */
	private boolean arraylistAssertion(ArrayList<Object> left, ArrayList<Object> right, String operation, String condition) throws Exception {
		boolean result;
		
		// switch to HashSet for use the equal function and check elements (ignoring its order)
		Set<Object> leftSet = new HashSet<Object>();
		Set<Object> rightSet = new HashSet<Object>();
		leftSet.addAll(left);
		rightSet.addAll(right);
		
		switch (operation) {
		case "==":
			if (leftSet.equals(rightSet)) {
				result = true;
			} else {
				result = false;
			}
			break;
		case "!=":
			if (!leftSet.equals(rightSet)) {
				result = true;
			} else {
				result = false;
			}
			break;
		default:
			String msg = "Unsopported operation '" + operation + "' for the assertion between two ArrayList [token: '" + condition + "']\n";
			msg += String.format("%10s %s", " ", "Left assertion = '" + left + "'\n");
			msg += String.format("%10s %s", " ", "Right assertion = '" + right + "'\n");
			throw new Exception(msg);
		}

		if (result) {
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + condition + "' is verified.");
			}
		} else {
			if(logger.isInfoEnabled()){
				logger.info("Assertion '" + condition + "' is wrong.");
			}
		}

		return result;
	}

	/**
	 * Returns the result of a single {@link Assertion}, considering also the applied functions
	 * 
	 * @param assertion
	 *            the {@link Assertion} to evaluate
	 * @return an {@link Object} corresponding to the result of the evaluation
	 * @throws Exception
	 *             if is used an undefined variable for the {@link Assertion} specification
	 * @throws Exception
	 *             if the evaluation goes wrong, the cause will be specified with a message
	 */
	private Object doAssertion(Assertion assertion) throws Exception {
		Object result;
		String assertionRepr = " [token: '" + StringHelper.assertionToString(assertion) + "']";
		
		if (assertion instanceof AssertionQuantified) {
			return doAssertionQuantified(assertion);
		} else if (assertion instanceof Constant) {
			return ((((Constant) assertion).getString() == null) ? ((Constant) assertion).getNumber() : ((Constant) assertion).getString());
		} else 	if (!assertion.getSteps().isEmpty()) {
			// look if there's a placeholder, if any substitute it with its values (note: the placeholder is always on the first step!)
			try {
				result = resolveQuery(assertion.getSteps());
			} catch (Exception e) {
				throw new Exception(e.getMessage() + assertionRepr);
			}
		} else if(assertion.getValues() != null) {
			result = StringHelper.valuesToList(assertion.getValues());
		} else if(assertion instanceof Expression) {
			try {
				result = resolveExpression(assertion);
			} catch (Exception e) {
				throw new Exception(e.getMessage() + assertionRepr);
			}
		} else {
			result = assertion.isBoolean();
		}

		// functions evaluation, according to the corresponding type
		try {
			result = FunctionHelper.applyFunctions(result, assertion.getFunctions());
		} catch (Exception e) {
			throw new Exception(e.getMessage() + assertionRepr);
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolveQuery(EList<Step> steps) throws Exception {
		Object result;
		String placeholder = steps.get(0).getPlaceholder();

		if (placeholder != null) {
			Object value = WSCoLAnalyser.getVariable(placeholder);
			if (value == null) {
				throw new Exception("Variable '" + placeholder + "' is not defined");
			}
			if (value instanceof DataObject) {
				if (steps.size() > 1) { // if there query goes deeper
					result = ((DataObject) value).evaluate(steps);
				} else {
					result = value;
				}
			} else if (value instanceof ArrayList) {
				if (steps.size() > 1) {
					result = DataObjectImpl.evaluateArray((ArrayList<Object>) value, steps); //TODO non il massimo
				} else {
					result = (ArrayList<Object>) value;
				}

				// if (result instanceof ArrayList && ((ArrayList<Object>) result).size() == 1) {
				// result = ((ArrayList<Object>) result).get(0);
				// }
			} else {
				if (steps.size() > 1) {
					throw new Exception("The property '" + steps.get(1).getName() + "' could not be retrieved from the element '" + value + "' of type '" + value.getClass().getSimpleName() + "'");
				}
				result = value;
			}
		} else {
			result = WSCoLAnalyser.getInput().evaluate(steps);
		}
		return result;
	}

	@Override
	public double resolveExpression(Assertion exp) throws Exception {
		double result = 0;
		if(exp instanceof Plus) {
			result = resolveExpression(((Plus) exp).getLeft()) + resolveExpression(((Plus) exp).getRight());
		} else if(exp instanceof Minus) {
			result = resolveExpression(((Minus) exp).getLeft()) - resolveExpression(((Minus) exp).getRight());
		} else if(exp instanceof Multi) {
			result = resolveExpression(((Multi) exp).getLeft()) * resolveExpression(((Multi) exp).getRight());
		} else if(exp instanceof Div) {
			result = resolveExpression(((Div) exp).getLeft()) / resolveExpression(((Div) exp).getRight());
		} else if(exp instanceof Rest) {
			result = resolveExpression(((Rest) exp).getLeft()) % resolveExpression(((Rest) exp).getRight());
		} else if(exp instanceof Constant) {
			if(((Constant) exp).getString() != null){
				throw new Exception("Could not use a String inside an expression '" + ((Constant) exp).getString() + "'");
			}
			return ((Constant) exp).getNumber();
		} else if(!exp.getSteps().isEmpty()) {
			Object query = resolveQuery(exp.getSteps());
			if(!(query instanceof Double)){
				throw new Exception("Could not use a '" + query.getClass().getSimpleName() + "' inside an expression (value: '" + query + "')");
			}
			return (double) query;
		}
		return result;
	}

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
	@Override
	@SuppressWarnings("unchecked")
	public Object doAssertionQuantified(Assertion assertion) throws Exception {
		AssertionQuantified aq = (AssertionQuantified) assertion;
		String assertionRepr = "[token: '" + StringHelper.assertionQuantifiedToString(aq) + "']";

		Object variable = WSCoLAnalyser.getVariable(aq.getVar());
		if (variable == null) {
			throw new Exception("The variable '" + aq.getVar() + "' is not defined. " + assertionRepr);
		}
		
		boolean isDataObject;
		if (variable instanceof DataObject) {
			isDataObject = true;
		} else if(variable instanceof ArrayList) {
			isDataObject = false;
		} else {
			throw new Exception("Could not iterate over a " + variable.getClass().getSimpleName() + " (" + aq.getVar() + "). A DataObject type was expected " + assertionRepr);
		}

		String alias = aq.getAlias();
		boolean result;
		double count, sum;

		if (WSCoLAnalyser.getVariable(alias) != null) {
			throw new Exception("The variable '" + alias + "' is already used. Choose another. " + assertionRepr);
		}

		Iterator<Object> iter = ((isDataObject) ? ((DataObject) variable).values().iterator() : ((ArrayList<Object>) variable).iterator());

		switch (aq.getQuantifier()) {
		case "forall":
			while (iter.hasNext()) {
				WSCoLAnalyser.putVariable(alias, iter.next());
				result = verifyAssertions(aq.getConditions());
				if (!result) {
					WSCoLAnalyser.removeVariable(alias);
					return false;
				}
			}
			return true;
		case "exists":
			result = false;
			while (iter.hasNext()) {
				WSCoLAnalyser.putVariable(alias, iter.next());
				result = result | verifyAssertions(aq.getConditions());
			}
			WSCoLAnalyser.removeVariable(alias);
			return result;
		case "numOf":
			count = 0;
			while (iter.hasNext()) {
				WSCoLAnalyser.putVariable(alias, iter.next());
				if (verifyAssertions(aq.getConditions())) {
					count = count + 1;
				}
			}
			WSCoLAnalyser.removeVariable(alias);
			return count;
		case "sum":
			sum = 0;
			while (iter.hasNext()) {
				Object next = iter.next();
				if (!(next instanceof Double)) {
					throw new Exception("The variable '" + aq.getVar() + "' contains an element of class '" + next.getClass().getSimpleName() + "'. Only Doubles are accepted by '" + aq.getQuantifier() + "' function " + assertionRepr);
				}
				WSCoLAnalyser.putVariable(alias, next);
				if (verifyAssertions(aq.getConditions())) {
					sum += (double) next;
				}
			}
			WSCoLAnalyser.removeVariable(alias);
			return sum;
		case "avg":
			sum = 0;
			count = 0;
			while (iter.hasNext()) {
				Object next = iter.next();
				if (!(next instanceof Double)) {
					throw new Exception("The variable '" + aq.getVar() + "' contains an element of class '" + next.getClass().getSimpleName() + "'. Only Doubles are accepted by '" + aq.getQuantifier() + "' function " + assertionRepr);
				}
				WSCoLAnalyser.putVariable(alias, next);
				if (verifyAssertions(aq.getConditions())) {
					sum += (double) next;
					count += 1;
				}
			}
			WSCoLAnalyser.removeVariable(alias);
			return sum / count;
		case "product":
			double product = 1;
			while (iter.hasNext()) {
				Object next = iter.next();
				if (!(next instanceof Double)) {
					throw new Exception("The variable '" + aq.getVar() + "' contains an element of class '" + next.getClass().getSimpleName() + "'. Only Doubles are accepted by '" + aq.getQuantifier() + "' function " + assertionRepr);
				}
				WSCoLAnalyser.putVariable(alias, next);
				if (verifyAssertions(aq.getConditions())) {
					product *= (double) next;
				}
			}
			WSCoLAnalyser.removeVariable(alias);
			return product;
		case "max":
			double max = Double.NEGATIVE_INFINITY;
			while (iter.hasNext()) {
				Object next = iter.next();
				if (!(next instanceof Double)) {
					throw new Exception("The variable '" + aq.getVar() + "' contains an element of class '" + next.getClass().getSimpleName() + "'. Only Doubles are accepted by '" + aq.getQuantifier() + "' function " + assertionRepr);
				}
				WSCoLAnalyser.putVariable(alias, next);
				if (verifyAssertions(aq.getConditions())) {
					if ((double) next > max) {
						max = (double) next;
					}
				}
			}
			WSCoLAnalyser.removeVariable(alias);
			return max;
		case "min":
			double min = Double.POSITIVE_INFINITY;
			while (iter.hasNext()) {
				Object next = iter.next();
				if (!(next instanceof Double)) {
					throw new Exception("The variable '" + aq.getVar() + "' contains an element of class '" + next.getClass().getSimpleName() + "'. Only Doubles are accepted by '" + aq.getQuantifier() + "' function " + assertionRepr);
				}
				WSCoLAnalyser.putVariable(alias, next);
				if (verifyAssertions(aq.getConditions())) {
					if ((double) next < min) {
						min = (double) next;
					}
				}
			}
			WSCoLAnalyser.removeVariable(alias);
			return min;
		default:
			return null; // never reached: other cases are blocked by the grammar parser as errors
		}
	}

}
