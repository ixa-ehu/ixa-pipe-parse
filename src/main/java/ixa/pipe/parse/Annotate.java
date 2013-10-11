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

import org.jdom2.JDOMException;

/**
 * @author ragerri
 *
 */
public class Annotate {

  private ConstituentParsing parser;
  private Models modelRetriever;
  private HeadFinder headFinder; 
  
  public Annotate(String lang) {
	modelRetriever = new Models();
	InputStream parseModel = modelRetriever.getParseModel(lang);
    parser = new ConstituentParsing(parseModel);
  }
  
  public Annotate(String lang, HeadFinder headFinder) {
    modelRetriever = new Models();
    InputStream parseModel = modelRetriever.getParseModel(lang);
    parser = new ConstituentParsing(parseModel);
    this.headFinder = headFinder;
  }

  private String getSentenceFromTokens(String[] tokens) {
	  StringBuilder sb = new StringBuilder();
	  for (int i=0; i<tokens.length; i++) {
		  sb.append(tokens[i]).append(" ");
	  }
	  String sentence = sb.toString();
	  return sentence;
  }
  
  /**
   * This method uses the Apache OpenNLP to perform Constituent parsing.
   *
   * It gets a Map<SentenceId, tokens> from the input KAF document and iterates
   * over the tokens of each sentence.
   * @param List<Element> wfs
   * @return String parsed document
 * @throws JDOMException
   */

  public String parseToKAFHeadFinder(KAFDocument kaf) throws IOException {
    
    StringBuffer parsingDoc = new StringBuffer();
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
     
     for(Parse parse:parsedSentence){
       headFinder.printHeads(parse);
     } 
     for (Parse parsedSent : parsedSentence) {
       parsedSent.show(parsingDoc);
       parsingDoc.append("\n");
     }
    }
    try {
	 kaf.addConstituencyFromParentheses(parsingDoc.toString());
     } catch (Exception e) {
	 e.printStackTrace();
     }
     return kaf.toString();
    }
  
public String parseHeadFinder(KAFDocument kaf) throws IOException {
    
    StringBuffer parsingDoc = new StringBuffer();
    List<List<WF>> sentences = kaf.getSentences();
    for (List<WF> sentence: sentences) {
    //get array of token forms from a list of WF objects
      String[] tokens = new String[sentence.size()];
      for (int i=0; i < sentence.size(); i++) {
        tokens[i] = sentence.get(i).getForm();
      }
      // Constituent Parsing
     String sent = this.getSentenceFromTokens(tokens);
     Parse[] parsedSentence = parser.parse(sent,1);
     
     for(Parse parse: parsedSentence){
       headFinder.printHeads(parse);
     } 
     for (Parse parsedSent : parsedSentence) {
       parsedSent.show(parsingDoc);
       parsingDoc.append("\n");
     }
    }
     return parsingDoc.toString();
    }


}
