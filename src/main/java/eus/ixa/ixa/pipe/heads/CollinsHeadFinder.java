package eus.ixa.ixa.pipe.heads;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import opennlp.tools.parser.HeadRules;
import opennlp.tools.parser.Parse;

/**
 * HeadFinder for constituent parse using Collins rules. These rules and the
 * getHead() method in the language specific HeadRules classes (adapted from
 * Collins' original head rules).
 *
 * @author ragerri
 * @version 2015-04-30
 * 
 */
public class CollinsHeadFinder implements HeadFinder {

  private static Map<String, HeadRules> headRulesMap = new HashMap<String, HeadRules>();
  private HeadRules headRules;
  private static boolean DEBUG = false;
  public static final String HEADMARK = "=H";

  public CollinsHeadFinder(Properties properties) {
    String lang = properties.getProperty("language");
    headRules = loadHeadRules(lang);
  }
  
  private HeadRules loadHeadRules(String lang) {
    try {
      if (headRulesMap.get("lang") == null) {
        InputStream is = getHeadRulesFile(lang);
        if (lang.equalsIgnoreCase("en")) {
          headRulesMap.put(lang, new EnglishHeadRules(new InputStreamReader(is)));
        } else if (lang.equalsIgnoreCase("es")) {
          headRulesMap.put(lang, new SpanishHeadRules(new InputStreamReader(is)));
        }
        is.close();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return headRulesMap.get(lang);
  }
  
  private InputStream getHeadRulesFile(String lang) {
    
    InputStream headsFileInputStream = null;
    if (lang.equals("en")) {
      headsFileInputStream = getClass().getResourceAsStream("/en-head-rules");
    } else if (lang.equals("es")) {
      headsFileInputStream = getClass().getResourceAsStream("/es-head-rules");
    }
    return headsFileInputStream;
  }

  public void printHeads(Parse parse) {
    LinkedList<Parse> nodes = new LinkedList<Parse>();
    nodes.add(parse);
    // This is a recursive iteration over the whole parse tree
    while (!nodes.isEmpty()) {
      Parse currentNode = nodes.removeFirst();
      // When a node is here its '=H' annotation has already happened
      // so it '=H' has to be removed to match with the head rules
      String type = currentNode.getType().replace(HEADMARK, "");
      /*if (currentNode.getType().equals(opennlp.tools.parser.AbstractBottomUpParser.TOP_NODE)) { 
      currentNode.getChildren()[0].setType(currentNode.getChildren()[0].getType() + HEADMARK);
      }*/
      if (DEBUG) {
        System.err.println(currentNode.toString());
      }
      Parse[] children = currentNode.getChildren();
      Parse headChild = null;
      if (children.length > 0) {
        headChild = headRules.getHead(children, type);
      }
      // For every child, if it is the head, annotate with '=H'
      // and also add to the queue for the recursive processing
      for (Parse child : currentNode.getChildren()) {
        if (child == headChild) {
          child.setType(child.getType() + HEADMARK);
        }
        // add child to last of LinkedList which will 
        // now take as currentNode the child of this child
        nodes.addLast(child);
      }
    }
  }

}

