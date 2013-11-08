package it.polimi.wscol;

import it.polimi.wscol.Helpers.StringHelper;
import it.polimi.wscol.assertions.AssertionService;
import it.polimi.wscol.assertions.AssertionServiceImpl;
import it.polimi.wscol.dataobject.DataObject;
import it.polimi.wscol.dataobject.DataObjectImpl;
import it.polimi.wscol.declaration.DeclarationService;
import it.polimi.wscol.declaration.DeclarationServiceImpl;
import it.polimi.wscol.wscol.Assertion;
import it.polimi.wscol.wscol.AssertionForm;
import it.polimi.wscol.wscol.Assertions;
import it.polimi.wscol.wscol.Declaration;
import it.polimi.wscol.wscol.Model;
import it.polimi.wscol.wscol.Step;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.SyntaxErrorMessage;
import org.eclipse.xtext.resource.XtextResource;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

public class WSCoL {
	
	@Inject
	private Provider<ResourceSet> resourceSetProvider;

	private static Logger logger = Logger.getLogger(WSCoL.class);;
	
	private static String wscolFilePath;
	private static String inputFilePath;
	private static Map<String, Object> variables;
	private static DataObject input;
	
	private static AssertionService assertionService;
	private static DeclarationService declarationService;
	
	public static void main(String[] args) {
		new WSCoL().evaluate(args);
	}
	
	public boolean evaluate(String[] args) {
		Injector injector = new it.polimi.wscol.WSColStandaloneSetupGenerated().createInjectorAndDoEMFRegistration();
		WSCoL main = injector.getInstance(WSCoL.class);
		
		variablesSetUp(args);
		
		File f = new File(wscolFilePath);
		String string = f.toURI().toString();

		return main.runGenerator(string);
	}
	
	private static void variablesSetUp(String[] args){
		
		if(args.length != 3) {
			logger.error("WSCol Usage: wscol [wscol assertions file] [-json|-xml] [input file]\n'-json' and '-xml' for type of input type.");
			logger.error("Examples:\n wscol rules.wscol -xml input.xml\n");
			System.exit(0);
		}
		
		if(logger.isInfoEnabled()){
			logger.info("*** WSCoL Analyzer ***");
		}
		
		wscolFilePath = args[0];
		boolean isJson = (args[1].equals("-json") ? true : false);
		inputFilePath = args[2];
		
		if(logger.isInfoEnabled()){
			logger.info("Readed " + args[0] + " and " + args[2]);
		}
		
		assertionService = new AssertionServiceImpl();
		declarationService = new DeclarationServiceImpl();
		
		variables = new HashMap<>();
		input = new DataObjectImpl(inputFilePath, isJson);
		
	}
	
	protected boolean runGenerator(String string) {
		// load the resource and parse it
		ResourceSet set = resourceSetProvider.get();
		Resource resource = set.getResource(URI.createURI(string), true);

		// check for syntax errors
		if (syntaxErrors(resource)) {
			System.exit(0);
		}

		// get contents
		Model model = (Model) resource.getContents().get(0);
		EObjectContainmentEList<Declaration> declarations = (EObjectContainmentEList<Declaration>) model.getDeclarations();
		Assertions assertionSet = model.getAssertionSet();

		if(logger.isInfoEnabled()){
			// print out the DataObject conversion of the XML file
			logger.info("INPUT: " + input);
	
			logger.info(declarations.size() + " declarations");
			logger.info("? assertions"); //TODO
		}

		// get variables declaration and sets the hashmap
		try {
			declarationService.setVariable(declarations);
		} catch (Exception e) {
			logger.error(e.getMessage());
			System.exit(0);
		}

		// verify the assertions
		boolean result = false;
		try {
			if(logger.isInfoEnabled()){
				logger.info("ASSERTION EVALUATION");
			}
			result = assertionService.verifyAssertions(assertionSet);
			if(logger.isInfoEnabled()){
				logger.info("RESULT: " + result);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			System.exit(0);
		}
		return result;
	}

	/**
	 * Syntax error checking, with information about the error (an error message, the line number of the error and wrong token)
	 * 
	 * @param resource
	 *            the result of the parsed rules
	 * @return <code>true</code> if there is errors, <code>false</code> otherwise
	 */
	private boolean syntaxErrors(Resource resource) {
		// syntax errors checking
		Iterable<INode> errors = ((XtextResource) resource).getParseResult().getSyntaxErrors();
		Iterator<INode> iter = errors.iterator();
		int number = resource.getErrors().size();

		if (number == 0) { // no errors, go away!
			return false;
		}

		logger.error("SYNTAX ERRORS: " + number + " found");

		INode errorNode = null;
		while (iter.hasNext()) {
			errorNode = iter.next();
			ICompositeNode parent = errorNode.getParent();
			EObject semanticElement = errorNode.getSemanticElement();
			int sl = errorNode.getStartLine();
			SyntaxErrorMessage errm = errorNode.getSyntaxErrorMessage();
			String erroneousToken = "";
			if (semanticElement instanceof AssertionForm) {
				erroneousToken = StringHelper.assertionFormToString((AssertionForm) semanticElement);
			} else if (semanticElement instanceof Assertion) {
				erroneousToken = StringHelper.assertionToString((Assertion) semanticElement);
			} else if (semanticElement instanceof Declaration) {
				erroneousToken = StringHelper.declarationToString((Declaration) semanticElement);
			} else if (semanticElement instanceof Step) {
				erroneousToken = StringHelper.stepToString((Step) semanticElement);
			} else {
				EObject parentSemanticElement = parent.getSemanticElement();
				while (erroneousToken.equals("")) {
					if (parentSemanticElement instanceof AssertionForm) {
						erroneousToken = StringHelper.assertionFormToString((AssertionForm) parentSemanticElement);
					} else if (parentSemanticElement instanceof Assertion) {
						erroneousToken = StringHelper.assertionToString((Assertion) parentSemanticElement);
					} else if (parentSemanticElement instanceof Declaration) {
						erroneousToken = StringHelper.declarationToString((Declaration) parentSemanticElement);
					} else if (parentSemanticElement instanceof Step) {
						erroneousToken = StringHelper.stepToString((Step) parentSemanticElement);
					} else {
						if (parentSemanticElement.eContainer() != null) {
							parentSemanticElement = parentSemanticElement.eContainer();
						} else { // we are in the root node
							erroneousToken = "ROOT";
						}
					}
				}
			}
			
			logger.error("MSG: " + errm.getMessage() + " - LINE: " + sl + " - TOKEN: '" + erroneousToken + "'");

			// "table-formatted" output
			// System.err.printf("%-50s - %-8s - %-60s %n", "ERROR: " + errm.getMessage(), "LINE: " + sl, "TOKEN: " + erroneousToken);
		}
		return true;
	}
	
	/**
	 * Returns the value corresponding to the passed key
	 * 
	 * @param key
	 *            the name (and the key) of the variable to retrieve
	 * @return the corresponding value if the key is found, null otherwise
	 * @see Map#get(Object)
	 */
	public static Object getVariable(String key) {
		return variables.get(key);
	}
	
	/**
	 * Puts a key-value pair variable
	 * 
	 * @param key the name of the variable
	 * @param value the value of the variable
	 * @return the previous value associated with key, or null if there was no mapping for key
	 * @see Map#put(Object)
	 */
	public static Object putVariable(String key, Object value){
		return variables.put(key, value);
	}
	
	/**
	 * Removes the variable
	 * 
	 * @param key the name of the variable to delete
	 * @return the previous value associated with key, or null if there was no mapping for key
	 * @see Map#remove(Object)
	 */
	public static Object removeVariable(String key) {
		return variables.remove(key);
	}
	
	/**
	 * Returns the input
	 * 
	 * @return the input
	 */
	public static DataObject getInput() {
		return input;
	}
	
	
}