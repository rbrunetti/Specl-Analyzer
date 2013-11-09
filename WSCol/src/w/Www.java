package w;

import it.polimi.wscol.WSCoLAnalyzer;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Www {

	public static void main(String[] args) {
		WSCoLAnalyzer wsca = new WSCoLAnalyzer();
		
		wsca.shutdownLogger();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document doc = null;
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(new File("/home/ricky/Desktop/book.xml"));
			wsca.setXMLInput(doc);
		} catch (ParserConfigurationException e2) {
			e2.printStackTrace();
		} catch (SAXException | IOException e1) {
			e1.printStackTrace();
		}
		
		String wscol = "let $a = /inventory/book[1]/title;"
					 + "let $b = /inventory/book;"
					 + "forall($bb in $b, $bb/title != $a || $bb/title.length() > 5);";
		try {
			if(wsca.evaluate(wscol))
				System.out.println("Fuck yeah, it's right!\n $a = " + wsca.getVariable("$a"));
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		wsca = new WSCoLAnalyzer();
		wsca.setXMLInput(doc);
		wscol = "let $a = /inventory/book[2]/title;"
			 + "let $ba = /inventory/book;"
			 + "forall($bb in $ba, $bb/title != $a || $bb/title.length() > 5);";
		
		try {
			if(wsca.evaluate(wscol))
				System.out.println("Fuck yeah, even the second it's right!\n $b = " + wsca.getVariable("$b"));
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		
		
		
	}

}
