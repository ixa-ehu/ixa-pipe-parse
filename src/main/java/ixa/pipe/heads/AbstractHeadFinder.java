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

package ixa.pipe.heads;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import opennlp.tools.parser.Constituent;
import opennlp.tools.parser.GapLabeler;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.chunking.Parser;

/**
 * Class for storing the English head rules associated with parsing. The
 * headrules are specified in $src/main/resources/en-head-rules
 * 
 * NOTE: This is the very same class than the one inside
 * opennlp.tools.parser.lang.en. The only change is the return of the getHead()
 * method:
 * 
 * Before: return constituents[ci].getHead(); Now: return constituents[ci];
 * 
 * Other changes include removal of deprecated methods we do not need to use.
 * 
 */
public abstract class AbstractHeadFinder implements HeadFinder, GapLabeler {

  protected Map<String, HeadRule> headRules;
  protected Set<String> punctSet;

  protected static class HeadRule {

    protected boolean leftToRight;
    protected Set<String> how;
    public String[][] howTags;

    public HeadRule(String[][] howTags) {

      for (String[] tags : howTags) {
        for (String tag : tags) {
          if (tag == null)
            throw new IllegalArgumentException(
                "tags must not contain null values!");
        }
      }
      this.howTags = howTags;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof HeadRule) {
        HeadRule rule = (HeadRule) obj;

        return (rule.leftToRight == leftToRight)
            && Arrays.equals(rule.howTags, howTags);
      } else {
        return false;
      }
    }
  }

  /**
   * Creates a new set of head rules based on the specified reader.
   * 
   * @param rulesReader
   *          the head rules reader.
   * 
   * @throws IOException
   *           if the head rules reader can not be read.
   */
  public AbstractHeadFinder() {

    punctSet = new HashSet<String>();
    punctSet.add(".");
    punctSet.add(",");
    punctSet.add("``");
    punctSet.add("''");
    // punctSet.add(":");

  }

  public Set<String> getPunctuationTags() {
    return punctSet;
  }

  public Parse getHead(Parse[] constituents, String type) {
    int headChild;
    HeadRule hr;

    if (constituents[0].getType() == Parser.TOK_NODE) {
      return null;
    }
    // check that our rules match the non-terminals we obtain 
    // in the parse
    if ((hr = headRules.get(type)) != null) {
      //iterate over the headRule 2 dimensional String[][] 
      for (int i = 0; i < hr.howTags.length; i++) {
        // for every nonTerminal the first elem is the traversal method 
        // and the rest the tags 
        String[] tags = hr.howTags[i];

        if (tags[0].equals("left")) {
          headChild = leftTraversal(constituents, tags);
        }

        else if (tags[0].equals("right")) {
          headChild = rightTraversal(constituents, tags);
        }

        else if (tags[0].equals("rightdis")) {
          headChild = rightDisTraversal(constituents, tags);
        }
        else if (tags[0].equals("leftdis")) { 
          headChild = leftDisTraversal(constituents, tags);
        }

        else {
          throw new IllegalStateException(
              "ERROR: invalid direction type to headRules map in AbstractHeadFinder.");
        }
      }
    }
    return constituents[headChild];
  }

  private int leftTraversal(Parse[] constituents, String[] tags) {
    for (int ti = 0; ti < tags.length; ti++) {
      for (int ci = 0; ci < constituents.length; ci++) {
        if (constituents[ci].getType().equals(tags[ti])) {
          return ci;
        }
      }
    }
    return -1;
  }

  private int rightTraversal(Parse[] constituents, String[] tags) {
    for (int ti = 0; ti < tags.length; ti++) {
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        if (constituents[ci].getType().equals(tags[ti])) {
          return ci;
        }
      }
    }
    return -1;
  }

  private int rightDisTraversal(Parse[] constituents, String[] tags) {
    for (int ci = constituents.length - 1; ci >= 0; ci--) {
      for (int ti = tags.length - 1; ti >= 0; ti--) {
        if (constituents[ci].getType().equals(tags[ti])) {
          return ci;
        }
      }
    }
    return -1;
  }
  
  private int leftDisTraversal(Parse[] constituents, String[] tags) {
    for (int ci = 0; ci < constituents.length; ci++) {
      for (int ti = 0;  ti < tags.length; ti++) {
        if (constituents[ci].getType().equals(tags[ti])) {
          return ci;
        }
      }
    }
    return -1;
  }


  public void labelGaps(Stack<Constituent> stack) {
    if (stack.size() > 4) {
      // Constituent con0 = (Constituent) stack.get(stack.size()-1);
      Constituent con1 = stack.get(stack.size() - 2);
      Constituent con2 = stack.get(stack.size() - 3);
      Constituent con3 = stack.get(stack.size() - 4);
      Constituent con4 = stack.get(stack.size() - 5);
      // System.err.println("con0="+con0.label+" con1="+con1.label+" con2="+con2.label+" con3="+con3.label+" con4="+con4.label);
      // subject extraction
      if (con1.getLabel().equals("NP") && con2.getLabel().equals("S")
          && con3.getLabel().equals("SBAR")) {
        con1.setLabel(con1.getLabel() + "-G");
        con2.setLabel(con2.getLabel() + "-G");
        con3.setLabel(con3.getLabel() + "-G");
      }
      // object extraction
      else if (con1.getLabel().equals("NP") && con2.getLabel().equals("VP")
          && con3.getLabel().equals("S") && con4.getLabel().equals("SBAR")) {
        con1.setLabel(con1.getLabel() + "-G");
        con2.setLabel(con2.getLabel() + "-G");
        con3.setLabel(con3.getLabel() + "-G");
        con4.setLabel(con4.getLabel() + "-G");
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof AbstractHeadFinder) {
      AbstractHeadFinder rules = (AbstractHeadFinder) obj;

      return rules.headRules.equals(headRules)
          && rules.punctSet.equals(punctSet);
    } else {
      return false;
    }
  }

  /**
   * Modifies the input Parse tree annotating the heads with '=H' according to
   * every language specific HeadRules class.
   * 
   * 
   * 
   * @param parse
   */
  public void printHeads(Parse parse) {
    LinkedList<Parse> nodes = new LinkedList<Parse>();
    nodes.add(parse);
    // This is a recursive iteration over the whole parse tree
    while (!nodes.isEmpty()) {
      Parse currentNode = nodes.removeFirst();
      // When a node is here its '=H' annotation has already happened
      // so it '=H' has to be removed to match with the head rules
      String type = currentNode.getType().replace("=H", "");
      Parse[] children = currentNode.getChildren();
      Parse headChild = null;
      if (children.length > 0) {
        headChild = getHead(children, type);
      }
      // For every child, if it is the head, annotate with '=H'
      // and also add to the queue for the recursive processing
      for (Parse child : currentNode.getChildren()) {
        if (child == headChild) {
          child.setType(child.getType() + "=H");
        }
        nodes.addLast(child);
      }
    }
  }
  
  /**
   * A way for subclasses to fix any heads under special conditions.
   * The default does nothing.
   *
   * @param headIdx       The index of the proposed head
   * @param daughterTrees The array of daughter trees
   * @return The new headIndex
   */
  protected void postOperationFix(Parse headNode, Parse[] daughterTrees) {
  }
}
