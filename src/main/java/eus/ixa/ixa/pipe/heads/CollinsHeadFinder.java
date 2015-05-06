/*
 *Copyright 2015 Rodrigo Agerri

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
  private final HeadRules headRules;
  private static boolean DEBUG = false;
  public static final String HEADMARK = "=H";

  public CollinsHeadFinder(final Properties properties) {
    final String lang = properties.getProperty("language");
    this.headRules = loadHeadRules(lang);
  }

  private HeadRules loadHeadRules(final String lang) {
    try {
      if (headRulesMap.get("lang") == null) {
        final InputStream is = getHeadRulesFile(lang);
        if (lang.equalsIgnoreCase("en")) {
          headRulesMap.put(lang,
              new EnglishHeadRules(new InputStreamReader(is)));
        } else if (lang.equalsIgnoreCase("es")) {
          headRulesMap.put(lang,
              new SpanishHeadRules(new InputStreamReader(is)));
        }
        is.close();
      }
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return headRulesMap.get(lang);
  }

  private InputStream getHeadRulesFile(final String lang) {

    InputStream headsFileInputStream = null;
    if (lang.equals("en")) {
      headsFileInputStream = getClass().getResourceAsStream("/en-head-rules");
    } else if (lang.equals("es")) {
      headsFileInputStream = getClass().getResourceAsStream("/es-head-rules");
    }
    return headsFileInputStream;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * eus.ixa.ixa.pipe.heads.HeadFinder#printHeads(opennlp.tools.parser.Parse)
   */
  public void printHeads(final Parse parse) {
    if (parse == null || parse.getChildCount() == 0) {
      throw new IllegalArgumentException(
          "Can't return head of null or leaf Parse.");
    }
    final LinkedList<Parse> nodes = new LinkedList<Parse>();
    nodes.add(parse);
    // This is a recursive iteration over the whole parse tree
    while (!nodes.isEmpty()) {
      final Parse currentNode = nodes.removeFirst();
      // When a node is here its '=H' annotation has already happened
      // so it '=H' has to be removed to match with the head rules
      final String type = currentNode.getType().replace(HEADMARK, "");
      if (DEBUG) {
        System.err.println("-> Current Node: " + type + " "
            + currentNode.toString());
      }
      final Parse[] children = currentNode.getChildren();
      Parse headChild = null;
      if (children.length > 0) {
        headChild = this.headRules.getHead(children, type);
      }
      // For every child, if it is the head, annotate with '=H'
      // and also add to the queue for the recursive processing
      for (final Parse child : currentNode.getChildren()) {
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
