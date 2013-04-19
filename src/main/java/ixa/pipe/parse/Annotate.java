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

import ixa.pipe.heads.HeadFinder;
import ixa.pipe.kaf.KAFUtils;
import ixa.pipe.parse.Models;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.parser.Parse;

import org.jdom2.Element;
import org.jdom2.JDOMException;

/**
 * @author ragerri
 * 
 */
public class Annotate {

  private ConstituentParsing parser;
  private KAFUtils kafUtils;
  private Models modelRetriever;

  public Annotate(String lang) {
	modelRetriever = new Models();
	InputStream parseModel = modelRetriever.getParseModel(lang);
    parser = new ConstituentParsing(parseModel);
    kafUtils = new KAFUtils();
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

  public String getConstituentParse(
      List<Element> wfs, HeadFinder headFinder) throws IOException, JDOMException {
	  
	  LinkedHashMap<String, List<String>> sentencesMap = kafUtils
	            .getSentencesMap(wfs);
	      LinkedHashMap<String, List<String>> sentTokensMap = kafUtils
	            .getSentsFromWfs(sentencesMap, wfs);

    StringBuffer parsingDoc = new StringBuffer();
	for (Map.Entry<String, List<String>> sentence : sentTokensMap.entrySet()) {
      String[] tokens = sentence.getValue().toArray(
          new String[sentence.getValue().size()]);
      
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
	return parsingDoc.toString();

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

  public String getConstituentParse(
      List<Element> wfs) throws IOException, JDOMException {
      
      LinkedHashMap<String, List<String>> sentencesMap = kafUtils
                .getSentencesMap(wfs);
          LinkedHashMap<String, List<String>> sentTokensMap = kafUtils
                .getSentsFromWfs(sentencesMap, wfs);

    StringBuffer parsingDoc = new StringBuffer();
    for (Map.Entry<String, List<String>> sentence : sentTokensMap.entrySet()) {
      String[] tokens = sentence.getValue().toArray(
          new String[sentence.getValue().size()]);
      
      // Constituent Parsing
      String sent = this.getSentenceFromTokens(tokens);
      Parse parsedSentence[] = parser.parse(sent,1);
      for (Parse parsedSent : parsedSentence) {
          parsedSent.show(parsingDoc);
          parsingDoc.append("\n");
      }
      }
    return parsingDoc.toString();

    }
  

}
