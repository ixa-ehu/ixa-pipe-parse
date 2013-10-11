/*
 * Copyright 2013 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package ixa.pipe.parse;


import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;
import ixa.pipe.heads.HeadFinder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import opennlp.tools.parser.Parse;

/**
 * 
 * Class to provide parsing annotation in various forms: KAF, penn style,
 * and with headWords marked. It also loads the right model for each language. 
 * 
 * @author ragerri
 *
 */
public class Annotate {

  private boolean MARKHEADS;
  private ConstituentParsing parser;
  private Models modelRetriever;
  private HeadFinder headFinder;
  StringBuffer parsingDoc;
  
  
  /**
   * Constructor that takes into account lang options (en|es) loads the corresponding
   * parse model and decides whether to mark headWords or not. 
   * 
 * @param lang
 */
public Annotate(String lang) {
	modelRetriever = new Models();
	InputStream parseModel = modelRetriever.getParseModel(lang);
    parser = new ConstituentParsing(parseModel);
    MARKHEADS = false;
    parsingDoc = new StringBuffer();
  }
  
  /**
   * Constructor that takes lang options (en|es) and a headFinder as parameters and 
   * loads the corresponding parse model; it also states whether headWords should be marked.  
   * 
 * @param lang
 * @param headFinder
 */
public Annotate(String lang, HeadFinder headFinder) {
    modelRetriever = new Models();
    InputStream parseModel = modelRetriever.getParseModel(lang);
    parser = new ConstituentParsing(parseModel);
    this.headFinder = headFinder;
    MARKHEADS = true;
    parsingDoc = new StringBuffer();
  }

  /**
   * It takes an array of tokens and outputs a string with 
   * tokens joined by a whitespace. 
   * 
 * @param array of tokens
 * @return string representing one sentence for each array
 */
private String getSentenceFromTokens(String[] tokens) {
	  StringBuilder sb = new StringBuilder();
	  for (int i=0; i<tokens.length; i++) {
		  sb.append(tokens[i]).append(" ");
	  }
	  String sentence = sb.toString();
	  return sentence;
  }
  
  /**
 * @param kaf document containing <text> and <terms> elements
 * @return StringBuffer containing the Parse tree
 * @throws IOException
 */
private StringBuffer getParse(KAFDocument kaf) throws IOException {
	    
	    List<List<WF>> sentences = kaf.getSentences();
	    for (List<WF> sentence: sentences) {
	    //get array of token forms from a list of WF objects
	      String[] tokens = new String[sentence.size()];
	      for (int i=0; i < sentence.size(); i++) {
	        tokens[i] = sentence.get(i).getForm();
	      }
	      // Constituent Parsing
	     String sent = this.getSentenceFromTokens(tokens);
	     Parse parsedSentence[] = parser.parse(sent,1);
	     
	     if (MARKHEADS) { 
	     for(Parse parse:parsedSentence){
	       headFinder.printHeads(parse);
	       }
	     }
	     for (Parse parsedSent : parsedSentence) {
	       parsedSent.show(parsingDoc);
	       parsingDoc.append("\n");
	     }
	    }
	    return parsingDoc;
	    }
  
  

  /**
   * It takes a KAF document calls to getParse() and outputs the 
   * parse tree as KAF constituents elements 
   * 
 * @param KAF document containing <text> and <terms> elements
 * @return KAF <constituents> elements
 * @throws IOException
 */
public String parseToKAF(KAFDocument kaf) throws IOException {
    parsingDoc = getParse(kaf);
    try {
	 kaf.addConstituencyFromParentheses(parsingDoc.toString());
     } catch (Exception e) {
	 e.printStackTrace();
     }
     return kaf.toString();
    }
  
  /**
 * @param KAF document containing <text> and <terms> elements
 * @return parse tree into plain text 
 * @throws IOException
 */
public String parse(KAFDocument kaf) throws IOException {
	 parsingDoc = getParse(kaf);
     return parsingDoc.toString();
    }


}
