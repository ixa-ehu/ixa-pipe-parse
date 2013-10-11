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
 *
 * A base class for a HeadFinder similar to the one described in
 * Michael Collins' 1999 thesis.  For a given constituent we perform operations
 * try to search in a parse node children for a constituent which, if it is equal to a 
 * tag, will be chosen as headWord. with a final default that goes with the direction (leftToRight or 
 * rightToLeft). 
 * 
 * For most constituents, there will be only one category in the list,
 * the exception being, in Collins' original version, NP, as seen in CollinsHeadFinder.java HeadRule. 
 * </p>
 * <p>
 * It is up to the overriding base class to initialize the map headRules<String,HeadRule> 
 * from non-terminal constituent type to HeadRule in its constructor.
 
 * Each HeadRule consists of a String[][].  Each String[] is a list of
 * candidate tags, except for the first entry, which specifies direction of
 * traversal and must be one of the following:
 * </p>
 * <ul>
 * <li> "left" means search left-to-right by category and then by position
 * <li> "leftDis" means search left-to-right by position and then by category
 * <li> "right" means search right-to-left by category and then by position
 * <li> "rightDis" means search right-to-left by position and then by category
 * <li> "leftExc" means to take the first thing from the left that isn't in the list
 * <li> "rightExc" means to take the first thing from the right that isn't on the list
 * </ul>
 * <p>
 * Changes:
 * </p>
 * <ul>
 * <li> 2013/10: Originally this class was partially based in reading the HeadRule from a file 
 * $src/main/resources/en-head-rules, hard-coding every traversal which was not left or right with heavily 
 * duplicated and ad-hoc code. The code has been refactored and re-implemented to read now the new 
 * HeadRule 2d arrays. 
 * <li> 2013/10: In the getHead() method whenever the return constituents[ci].getHead() appeared it was 
 * changed to return constituents[ci]. Other changes include removal of deprecated methods we do 
 * not need to use in this new version. 
 * </ul>
 * 
 * @author ragerri
 * 
 */
public abstract class OpennNLPHeadFinder implements HeadFinder, GapLabeler {

  private static final boolean DEBUG = false;	
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
  public OpennNLPHeadFinder() {

    punctSet = new HashSet<String>();
    punctSet.add(".");
    punctSet.add(",");
    punctSet.add("``");
    punctSet.add("''");
    punctSet.add(":");

  }

  protected Set<String> getPunctuationTags() {
    return punctSet;
  }

  protected Parse getHead(Parse[] constituents, String type) {
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
          for (int ci = 0; ci < constituents.length; ci++) {
            for (int ti = 0;  ti < tags.length; ti++) {
              if (constituents[ci].getType().equals(tags[ti])) {
                return constituents[ci];
              }
            }
          }
          return constituents[0].getHead();
        }

        else if (tags[0].equals("right")) {
          for (int ti = 0; ti < tags.length; ti++) {
            for (int ci = constituents.length - 1; ci >= 0; ci--) {
              if (constituents[ci].getType().equals(tags[ti])) {
                return constituents[ci];
              }
            }
          }
          constituents[constituents.length -1].getHead();
        }

        else if (tags[0].equals("rightdis")) {
          for (int ci = constituents.length - 1; ci >= 0; ci--) {
            for (int ti = tags.length - 1; ti >= 0; ti--) {
              if (constituents[ci].getType().equals(tags[ti])) {
                return constituents[ci];
              }
            }
          }
          return constituents[constituents.length -1].getHead();
        }
        else if (tags[0].equals("leftdis")) {
          for (int ci = 0; ci < constituents.length; ci++) {
            for (int ti = 0;  ti < tags.length; ti++) {
              if (constituents[ci].getType().equals(tags[ti])) {
                return constituents[ci];
              }
            }
          }
          return constituents[constituents.length -1].getHead();
        }

        else {
          for (int ti = 0; ti < tags.length; ti++) {
            for (int ci = constituents.length - 1; ci >= 0; ci--) {
              if (constituents[ci].getType().equals(tags[ti])) {
                return constituents[ci];
              }
            }
          }
          return constituents[constituents.length - 1].getHead();
        }
      }
    }
    postOperationFix(constituents[constituents.length -1],constituents);
    return constituents[constituents.length -1].getHead();  
    
  }

  private Parse leftTraversal(Parse[] constituents, String[] tags) {
    for (int ti = 0; ti < tags.length; ti++) {
      for (int ci = 0; ci < constituents.length; ci++) {
        if (constituents[ci].getType().equals(tags[ti])) {
          return constituents[ci];
        }
      }
    }
    return constituents[0].getHead();
  }

  private Parse rightTraversal(Parse[] constituents, String[] tags) {
    for (int ti = 0; ti < tags.length; ti++) {
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        if (constituents[ci].getType().equals(tags[ti])) {
          return constituents[ci];
        }
      }
    }
    return constituents[constituents.length -1].getHead();
  }

  private Parse rightDisTraversal(Parse[] constituents, String[] tags) {
    for (int ci = constituents.length - 1; ci >= 0; ci--) {
      for (int ti = tags.length - 1; ti >= 0; ti--) {
        if (constituents[ci].getType().equals(tags[ti])) {
          return constituents[ci];
        }
      }
    }
    return constituents[constituents.length -1].getHead();
  }
  
  private Parse leftDisTraversal(Parse[] constituents, String[] tags) {
    for (int ci = 0; ci < constituents.length; ci++) {
      for (int ti = 0;  ti < tags.length; ti++) {
        if (constituents[ci].getType().equals(tags[ti])) {
          return constituents[ci];
        }
      }
    }
    return constituents[constituents.length -1].getHead();
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
    } else if (obj instanceof OpennNLPHeadFinder) {
      OpennNLPHeadFinder rules = (OpennNLPHeadFinder) obj;

      return rules.headRules.equals(headRules)
          && rules.punctSet.equals(punctSet);
    } else {
      return false;
    }
  }

  protected boolean isPreTerminal(Parse parse) { 
	  return (parse.getChildCount() == 1 && parse.getChildren()[0].getChildCount() == 0);
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
  protected void postOperationFix(Parse headNode, Parse[] constituents) {
  }
}
