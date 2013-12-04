package it.polimi.specl.helpers;

import it.polimi.specl.specl.Assertion;
import it.polimi.specl.specl.AssertionAnd;
import it.polimi.specl.specl.AssertionBraced;
import it.polimi.specl.specl.AssertionForm;
import it.polimi.specl.specl.AssertionNot;
import it.polimi.specl.specl.AssertionOr;
import it.polimi.specl.specl.AssertionQuantified;
import it.polimi.specl.specl.AssertionStdCmp;
import it.polimi.specl.specl.Assertions;
import it.polimi.specl.specl.Constant;
import it.polimi.specl.specl.Declaration;
import it.polimi.specl.specl.Div;
import it.polimi.specl.specl.Expression;
import it.polimi.specl.specl.Function;
import it.polimi.specl.specl.Minus;
import it.polimi.specl.specl.Multi;
import it.polimi.specl.specl.Plus;
import it.polimi.specl.specl.Predicate;
import it.polimi.specl.specl.Rest;
import it.polimi.specl.specl.Step;
import it.polimi.specl.specl.Value;
import it.polimi.specl.specl.Values;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;

public class StringHelper {
	/**
	 * Generates the {@link String} representation of an {@link AssertionForm}
	 * 
	 * @param af the {@link AssertionForm} to represent
	 * @return a {@link String} representing the passed {@link AssertionForm}
	 */
	public static String assertionFormToString(AssertionForm af) {
		if (af instanceof AssertionStdCmp) {
			return assertionToString(((AssertionStdCmp) af).getLeftAssert()) + " " + ((AssertionStdCmp) af).getOp() + " " + assertionToString(((AssertionStdCmp) af).getRightAssert());
		} else if(af instanceof AssertionQuantified) {
			return assertionQuantifiedToString((AssertionQuantified) af);
		} else {
			return assertionToString((Assertion) af);
		}
	}

	/**
	 * Generates the {@link String} representation of a {@link Declaration}
	 * 
	 * @param d the {@link Declaration} to represent
	 * @return a {@link String} representing the passed {@link Declaration}
	 */
	public static String declarationToString(Declaration d) {
		return d.getVar() + " = " + assertionToString(d.getAssert());
	}

	/**
	 * Generates the {@link String} representation of a {@link Assertions}
	 * (parent of {@link AssertionOr}, {@link AssertionAnd}, {@link AssertionForm}, {@link AssertionNot} and {@link AssertionBraced})
	 * 
	 * @param a the {@link Assertions} to represent
	 * @return a {@link String} representing the passed {@link Assertions}
	 */
	public static String assertionsToString(Assertions a) {
		String res = "";
		if (a instanceof AssertionOr) {
			res = assertionsToString(((AssertionOr) a).getLeft()) + " || " + assertionsToString(((AssertionOr) a).getRight());
		} else if (a instanceof AssertionAnd) {
			res = assertionsToString(((AssertionAnd) a).getLeft()) + " && " + assertionsToString(((AssertionAnd) a).getRight());
		} else if (a instanceof AssertionNot) {
			res = "!(" + assertionsToString(((AssertionNot) a).getInnerFormula()) + ")";
		} else if (a instanceof AssertionBraced) {
			res = "(" + assertionsToString(((AssertionBraced) a).getInnerFormula()) + ")";
		} else if (a instanceof AssertionForm) {
			res = assertionFormToString((AssertionForm) a);
		}
		return res;
	}
	
	/**
	 * Generates the {@link String} representation of a {@link AssertionQuantified}
	 * 
	 * @param aq the {@link AssertionQuantified} to represent
	 * @return the {@link String} representing the passed {@link AssertionQuantified}
	 */
	public static String assertionQuantifiedToString(AssertionQuantified aq) {
		return aq.getQuantifier() + "(" + aq.getAlias() + " in " + aq.getVar() + ", " + assertionsToString(aq.getConditions()) + ")";
	}

	/**
	 * Generate the {@link String} representation of an {@link Assertion}
	 * 
	 * @param a the {@link Assertion} to represent
	 * @return the {@link String} representing the passed {@link Assertion}
	 */
	public static String assertionToString(Assertion a) {
		if(a == null) return null;
		String res = "";
		if (a instanceof AssertionQuantified) {
			res = assertionQuantifiedToString((AssertionQuantified) a);
		} else if (!a.getSteps().isEmpty()) {
			res = queryToString(a.getSteps());
		} else if (a instanceof Constant) {
			res = constantToString((Constant) a);
		} else if (a.getValues() != null) {
			res = valuesToList(a.getValues()).toString();
		} else if (a instanceof Expression) {
			res = expressionToString(a);
		} else {
			res = String.valueOf(a.isBoolean());
		}
		res += functionsToString(a.getFunctions());
		return res;
	}
	
	/**
	 * Generate the {@link String} representation of an {@link Expression}.
	 * The method check and add eventual brackets.
	 * 
	 * @param exp the {@link Expression} to represent
	 * @return the {@link String} representing the passed {@link Expression}
	 */
	private static String expressionToString(Assertion exp) {
		String res = "";
		if(exp instanceof Plus) {
			res = expressionToString(((Plus) exp).getLeft()) + " + " + expressionToString(((Plus) exp).getRight());
		} else if(exp instanceof Minus) {
			res = expressionToString(((Minus) exp).getLeft()) + " - " + expressionToString(((Minus) exp).getRight());
		} else if(exp instanceof Multi) {
			if(((Multi) exp).getLeft() instanceof Plus || ((Multi) exp).getLeft() instanceof Minus) {
				res = "( " + expressionToString(((Multi) exp).getLeft()) + " ) * ";
			} else {
				res = expressionToString(((Multi) exp).getLeft()) + " * ";
			}
			if (((Multi) exp).getRight() instanceof Plus || ((Multi) exp).getRight() instanceof Minus){
				res += "( " + expressionToString(((Multi) exp).getRight()) + " )";
			} else {
				 res += expressionToString(((Multi) exp).getRight());
			}
		} else if(exp instanceof Div) {
			if(((Div) exp).getLeft() instanceof Plus || ((Div) exp).getLeft() instanceof Minus){
				res = "( " + expressionToString(((Div) exp).getLeft()) + " ) / ";
			} else {
				res = expressionToString(((Div) exp).getLeft()) + " / ";
			}
			if (((Div) exp).getRight() instanceof Plus || ((Div) exp).getRight() instanceof Minus ){
				res += "( " + expressionToString(((Div) exp).getRight()) + " )";
			} else {
				 res += expressionToString(((Div) exp).getRight());
			}
		} else if(exp instanceof Rest) {
			if (((Rest) exp).getLeft() instanceof Plus || ((Rest) exp).getLeft() instanceof Minus) {
				res = "( " + expressionToString(((Rest) exp).getLeft()) + " ) % ";
			} else {
				res = expressionToString(((Rest) exp).getLeft()) + " % ";
			}
			if (((Rest) exp).getRight() instanceof Plus || ((Rest) exp).getRight() instanceof Minus){
				res += "( " + expressionToString(((Rest) exp).getRight()) + " )";
			} else {
				res +=  expressionToString(((Rest) exp).getRight());
			}
		} else if(exp instanceof Constant) {
			return constantToString((Constant) exp);
		} else if(!exp.getSteps().isEmpty()) {
			return assertionToString(exp);
		}
		return res;
	}
	
	private static String functionsToString(EList<Function> functions) {
		String res = "";
		if (functions != null) {
			for(Function f:functions){
				String params = "";
				if (f.getParams() != null) {
					for (Value v : f.getParams().getValue()) {
						params += ((v instanceof Constant) 
								? constantToString((Constant) v) 
								: (queryToString(v.getSteps()) + functionsToString(v.getFunctions()))) + ", ";
					}
					params = params.substring(0, params.length() - 2); //delete the last ','
				}
				res += '.' + f.getName() + '(' + params + ')';
			}
		}
		return res;
	}

	/**
	 * Generate the {@link String} representation of an {@link Query}
	 * 
	 * @param q the {@link Query} to represent
	 * @return the {@link String} representing the passed {@link Query}
	 */
	public static String queryToString(EList<Step> steps) {
		String res = "";
		for (int i = 0; i < steps.size(); i++) {
			res += stepToString(steps.get(i));
		}
		return res;
	}

	/**
	 * Generate the {@link String} representation of a {@link Step}
	 * 
	 * @param s the {@link Step} to represent
	 * @return the {@link String} representing the passed {@link Step}
	 */
	public static String stepToString(Step s) {
		// if it is a variable
		if (s.getPlaceholder() != null) {
			return s.getPlaceholder();
		}
		
		String res = '/' + s.getName();
		Predicate attribute = s.getPredicate();
		
		// check if it contains an attribute
		if (attribute != null) {
			String property = attribute.getProperty();
			String operation = attribute.getOp();
			double value = attribute.getNumber();
			double numericValue = attribute.getNumberValue();
			res += '[';
			if (property != null && operation != null) {
				res += property + operation;
				if (attribute.getStrValue() != null) {
					res += '"' + attribute.getStrValue() + '"' + ']';
				} else if(attribute.getVarValue() != null) {
					res += attribute.getVarValue() + ']';
				} else {
					res += String.valueOf(numericValue) + ']';
				}
			} else { // it contains a variable or it's just the i-th selection
				res += ((attribute.getVar()!=null) ? attribute.getVar() : String.valueOf(value)) + ']';
			}
		}
		return res;
	}

	/**
	 * Generate the {@link String} representation of a {@link Values}
	 * 
	 * @param values the {@link Values} to represent
	 * @return the {@link String} representing the passed {@link Values}
	 */
	 //TODO trovagli un'altra posizione...
	public static List<Object> valuesToList(Values values) {
		List<Object> result = new ArrayList<>();
		for (Value c : values.getValue()) {
			if (c instanceof Constant) {
				if (((Constant) c).getString() != null) {
					result.add(((Constant) c).getString());
				} else {
					result.add(((Constant) c).getNumber());
				}
			} else if (c.getSteps() != null) {
				result.add(queryToString(c.getSteps()));
			} else {
				result.add(c);
			}
		}
		return result;
	}

	/**
	 * Returns the {@link String} representation of the inner value of a {@link Constant}
	 * @param constant the {@link Constant} to represent
	 * @return the {@link String} representing the value of the passed {@link Constant}
	 */
	private static String constantToString(Constant constant) {
		if (constant.getString() != null) {
			return constant.getString();
		} else {
			return String.valueOf(constant.getNumber());
		}
	}
}
