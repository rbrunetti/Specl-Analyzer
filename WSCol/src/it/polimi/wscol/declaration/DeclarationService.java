package it.polimi.wscol.declaration;

import it.polimi.wscol.wscol.Constant;
import it.polimi.wscol.wscol.Declaration;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;

public interface DeclarationService {

	/**
	 * Set variables according to the {@link Declaration}. Evaluates the query, resolves the variables, translate {@link Values} into {@link DataObject}, extract correct value from a {@link Constant} and assigns the values to a key with the specified name The values are extracted and saved as simple type ({@link String}, {@link Double} and {@link Boolean}) if the results of an evaluation is a {@link DataObject} with a single value. Otherwise as a {@link DataObject}.
	 * 
	 * @param declarations
	 *            the list of {@link Declaration} rules parsed
	 * @throws Exception
	 *             if the variable is already in use
	 * @throws Exception
	 *             if a variable, used inside a declaration and on which that is based, is not defined
	 * @throws Exception
	 *             if the evaluation goes wrong, the cause will be specified
	 * @throws Exception
	 *             if the evaluation gives back an empty result
	 */
	public void setVariable(EObjectContainmentEList<Declaration> declarations) throws Exception;
}
