package es.ehu.si.ixa.ixa.pipe.heads;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

import es.ehu.si.ixa.ixa.pipe.parse.Models;


import opennlp.tools.parser.HeadRules;
import opennlp.tools.parser.Parse;

/**
 * HeadFinder for constituent parse using Collins rules. These rules and the 
 * getHead() method in the language specific HeadRules classes (adapted from 
 * Collins' original head rules).   
 * 
 * 
 * @author ragerri
 *
 */
public class CollinsHeadFinder implements HeadFinder {
	
	private static HeadRules headRules;
	/**
	 * Constructor which reads the file $lang-head-rules at the root of classpath
	 * The file is read only the first time an instance is loaded.
	 * Every instance shares the same internal rules representation
	 * 
	 */
	public CollinsHeadFinder(String lang) {
		try{
			if(headRules==null){
				Models headFileRetriever = new Models();
				//InputStream is=getClass().getResourceAsStream("/en-head-rules");
				InputStream is = headFileRetriever.getHeadRulesFile(lang);
				//headRules=new EnglishHeadRules(new InputStreamReader(is));
				
				if (lang.equalsIgnoreCase("en")) {
					headRules=new EnglishHeadRules(new InputStreamReader(is));
				}
				if (lang.equalsIgnoreCase("es")) { 
					headRules=new SpanishHeadRules(new InputStreamReader(is));
				}
				is.close();
			}
		}
		catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public void printHeads(Parse parse){
		LinkedList<Parse> nodes=new LinkedList<Parse>();
		nodes.add(parse);
		//This is a recursive iteration over the whole parse tree
		while(!nodes.isEmpty()){
			Parse currentNode=nodes.removeFirst();
			//When a node is here its '=H' annotation has already happened
			//so it '=H' has to be removed to match with the head rules
			String type=currentNode.getType().replace("=H","");
			Parse[] children=currentNode.getChildren();
			Parse headChild=null;
			if(children.length>0){
				headChild=headRules.getHead(children, type);
			}
			//For every child, if it is the head, annotate with '=H'
			//and also add to the queue for the recursive processing
			for(Parse child:currentNode.getChildren()){
				if(child==headChild){
					child.setType(child.getType()+"=H");
				}
				nodes.addLast(child);
			}
		}
	}
	
}
