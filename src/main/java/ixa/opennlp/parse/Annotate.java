/**
 * 
 */
package ixa.opennlp.parse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.parser.Parse;

import org.jdom2.Element;

/**
 * @author ragerri
 * 
 */
public class Annotate {

  private ConstituentParsing parser;

  public Annotate() {
    parser = new ConstituentParsing();
  }

  
  /**
   * It reads the linguisticProcessor elements and adds them to
   * the KAF document.
   * 
   * @param lingProc
   * @param kaf
   */
  public void addKafHeader(List<Element> lingProc, KAF kaf) {
    String layer = null;
    for (int i = 0; i < lingProc.size(); i++) {
      layer = lingProc.get(i).getAttributeValue("layer");
      List<Element> lps = lingProc.get(i).getChildren("lp");
      for (Element lp : lps) {
        kaf.addlps(layer, lp.getAttributeValue("name"),
            lp.getAttributeValue("timestamp"), lp.getAttributeValue("version"));
      }
    }
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
   * This method uses the Apache OpenNLP to perform POS tagging.
   * 
   * It gets a Map<SentenceId, tokens> from the input KAF document and iterates
   * over the tokens of each sentence to annotated POS tags.
   * 
   * It also reads <wf>, elements from the input KAF document and fills
   * the KAF object with those elements plus the annotated POS tags in the <term>
   * elements.
   * 
   * @param LinkedHashMap
   *          <String,List<String>
   * @param List
   *          <Element> termList
   * @param KAF
   *          object. This object is used to take the output data and convert it
   *          to KAF.
   * 
   * @return JDOM KAF document containing <wf>, and <terms> elements.
   */

  public String getConstituentParse(
      LinkedHashMap<String, List<String>> sentTokensMap) throws IOException {

    StringBuffer parsingDoc = new StringBuffer();
	for (Map.Entry<String, List<String>> sentence : sentTokensMap.entrySet()) {
      String[] tokens = sentence.getValue().toArray(
          new String[sentence.getValue().size()]);
      
      // Constituent Parsing
      String sent = getSentenceFromTokens(tokens);
      Parse parsedSentence[] = parser.parse(sent,1);
      for (Parse parsedSent : parsedSentence) {
    	  parsedSent.show(parsingDoc);
    	  parsingDoc.append("\n");
      }
      }
	return parsingDoc.toString();

    }
  

}
