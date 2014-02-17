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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.parser.Parse;

public abstract class AbstractHeadFinder implements HeadFinder {

  private static final boolean DEBUG = false;
  protected Map<String, String[][]> headRules;
  protected final String headMark = "=H";
  protected Set<String> punctSet = new HashSet<String>(Arrays.asList(".",",","``","''",":"));


  /**
   * Default direction if no rule is found for category (the head/parent).
   * Subclasses can turn it on if they like. If they don't it is an error if no
   * rule is defined for a category (null is returned).
   */
  protected String[] defaultRule; // = null;

  /**
   * These are built automatically from categoriesToAvoid and used in a fairly
   * different fashion from defaultRule (above). These are used for categories
   * that do have defined rules but where none of them have matched. Rather than
   * picking the rightmost or leftmost child, we will use these to pick the the
   * rightmost or leftmost child which isn't in categoriesToAvoid.
   */
  protected String[] defaultLeftRule;
  protected String[] defaultRightRule;
  
  /**
   * Construct a HeadFinder.
   * @param categoriesToAvoid Constituent types to avoid as head
   */
  protected AbstractHeadFinder(String... categoriesToAvoid) {
    // automatically build defaultLeftRule, defaultRightRule
    defaultLeftRule = new String[categoriesToAvoid.length + 1];
    defaultRightRule = new String[categoriesToAvoid.length + 1];
    if (categoriesToAvoid.length > 0) {
      defaultLeftRule[0] = "leftexcept";
      defaultRightRule[0] = "rightexcept";
      System.arraycopy(categoriesToAvoid, 0, defaultLeftRule, 1, categoriesToAvoid.length);
      System.arraycopy(categoriesToAvoid, 0, defaultRightRule, 1, categoriesToAvoid.length);
    } else {
      defaultLeftRule[0] = "left";
      defaultRightRule[0] = "right";
    }
  }

  /**
   * A way for subclasses for corpora with explicit head markings to return the
   * explicitly marked head
   * 
   * @param parse
   *          a tree to find the head of
   * @return the marked head-- null if no marked head
   */
  // to be overridden in subclasses for corpora
  //
  protected Parse findMarkedHead(Parse parse) {
    return null;
  }

  /**
   * Finds which child of the current parse tree is the head.
   * 
   * @param parse
   *          The parse tree to examine the daughters of. If this is a leaf,
   *          <code>null</code> is returned
   * @return The daughter parse tree that is the head of <code>parse</code>
   * @see this#printHeads(parse) for a routine to call this and spread
   *      heads throughout a tree and print them 
   */
  public Parse getHead(Parse parse) {
    return getHead(parse, null);
  }

  /**
   * Find which child of the current parse tree is the head.
   * 
   * @param parse
   *          The parse tree to examine the daughters of. If this is a leaf,
   *          <code>null</code> is returned
   * @param parent
   *          The parent of parse
   * @return The child parse tree that is the head of <code>parse</code>. Returns
   *         null for leaf nodes.
   * @see this#printHeads(parse) for a routine to call this and spread
   *      heads throughout a tree and print them 
   */
  public Parse getHead(Parse parse, Parse parent) {
    if (headRules == null) {
      throw new IllegalStateException(
          "Classes derived from AbstractHeadFinder must create and fill HashMap headRules.");
    }
    if (parse == null || parse.getChildCount() == 0) {
      throw new IllegalArgumentException(
          "Can't return head of null or leaf Parse.");
    }
    if (DEBUG) {
      System.err.println("getHead for " + parse.getType() + " : " + parse.getCoveredText());
    }

    Parse[] children = parse.getChildren();

    Parse headChild;
    // first check if subclass found explicitly marked head
    if ((headChild = findMarkedHead(parse)) != null) {
      if (DEBUG) {
        System.err.println("Find marked head method returned "
            + headChild.getType() + " as head of " + parse.getType());
      }
      return headChild;
    }

    if (children.length == 1) {
      if (DEBUG) {
        System.err.println("Only one child determines " + children[0].getLabel()
            + " as head of " + parse.getType());
      }
      return children[0];
    }

    return getNonTrivialHead(parse, parent);
  }

  /**
   * Called by determineHead and may be overridden in subclasses if special
   * treatment is necessary for particular categories.
   * 
   * @param parse
   *          The parse tree from which to find the head child 
   * @param parent
   *          The parent of parse (or may be null)
   * @return The head daughter of t
   */
  
  protected Parse getNonTrivialHead(Parse parse, Parse parent) {
    Parse headChild = null;
    String nonTerminal = parse.getType().replace(headMark, "");
    if (DEBUG) {
      System.err.println("Looking for head of " + parse.getType() + " baseCat is |" + nonTerminal + '|');
    }
    String[][] headRule = headRules.get(nonTerminal);
    Parse[] children = parse.getChildren();
    if (headRule == null) {
      if (DEBUG) {
        System.err.println("Warning: No rule found for " + nonTerminal
            + " (first char: " + nonTerminal.charAt(0) + ')');
        System.err.println("Known nonterms are: " + headRules.keySet());
      }
      if (defaultRule != null) {
        if (DEBUG) {
          System.err.println(" Using defaultRule");
        }
        return traverseParse(children, defaultRule, true);
      } else {
        throw new IllegalArgumentException("No head rule defined for "
            + nonTerminal + " using " + this.getClass() + " in " + parse);
      }
    }
    for (int i = 0; i < headRule.length; i++) {
      boolean lastResort = (i == headRule.length - 1);
      headChild = traverseParse(children, headRule[i], lastResort);
      if (headChild != null) {
        break;
      }
    }
    if (DEBUG) {
      System.err.println(" Chose " + headChild.getType());
    }
    return headChild;
  }

  
  /**
   * Tries to find a child parse as head among the children. Walks the children 
   * looking for types contained in the headRule according to the traversal method
   * specified in headRule[0]. If no headNode is found, takes the leftmost or 
   * rightmost. If nothing is found, returns null. 
   * 
   * @param children
   * @param headRule
   * @param lastResort
   * @return parse headChild
   */
  protected Parse traverseParse(Parse[] children, String[] headRule,
      boolean lastResort) {
    int headIndex;
    if (headRule[0].equals("left")) {
      headIndex = findLeftHead(children, headRule);
    } else if (headRule[0].equals("leftdis")) {
      headIndex = findLeftDisHead(children, headRule);
    } else if (headRule[0].equals("leftexcept")) {
      headIndex = findLeftExceptHead(children, headRule);
    } else if (headRule[0].equals("right")) {
      headIndex = findRightHead(children, headRule);
    } else if (headRule[0].equals("rightdis")) {
      headIndex = findRightDisHead(children, headRule);
    } else if (headRule[0].equals("rightexcept")) {
      headIndex = findRightExceptHead(children, headRule);
    } else {
      throw new IllegalStateException("ERROR: invalid direction type " + headRule[0]
          + " to headRules map in AbstractHeadFinder.");
    }

    // what happens if our rule didn't match anything
    if (headIndex < 0) {
      if (lastResort) {
        String[] rule;
        if (headRule[0].startsWith("left")) {
          headIndex = 0;
          rule = defaultLeftRule;
        } else {
          headIndex = children.length - 1;
          rule = defaultRightRule;
        }
        Parse child = traverseParse(children, rule, false);
        if (child != null) {
          return child;
        } else {
          return children[headIndex];
        }
      } else {
        // if we're not the last resort, we can return null to let the next rule
        // try to match
        return null;
      }
    }

    headIndex = correctFoundHeads(headIndex, children);

    return children[headIndex];
  }

  private int findLeftHead(Parse[] children, String[] headRule) {
    for (int i = 1; i < headRule.length; i++) {
      for (int headIdx = 0; headIdx < children.length; headIdx++) {
        String childCat = children[headIdx].getType();
        if (headRule[i].equals(childCat)) {
          return headIdx;
        }
      }
    }
    return -1;
  }

  private int findLeftDisHead(Parse[] children, String[] headRule) {
    for (int headIdx = 0; headIdx < children.length; headIdx++) {
      String childCat = children[headIdx].getType();
      for (int i = 1; i < headRule.length; i++) {
        if (headRule[i].equals(childCat)) {
          return headIdx;
        }
      }
    }
    return -1;
  }

  private int findLeftExceptHead(Parse[] children, String[] headRule) {
    for (int headIdx = 0; headIdx < children.length; headIdx++) {
      String childCat = children[headIdx].getType();
      boolean found = true;
      for (int i = 1; i < headRule.length; i++) {
        if (headRule[i].equals(childCat)) {
          found = false;
        }
      }
      if (found) {
        return headIdx;
      }
    }
    return -1;
  }

  private int findRightHead(Parse[] children, String[] headRule) {
    for (int i = 1; i < headRule.length; i++) {
      for (int headIdx = children.length - 1; headIdx >= 0; headIdx--) {
        String childCat = children[headIdx].getType();
        if (headRule[i].equals(childCat)) {
          return headIdx;
        }
      }
    }
    return -1;
  }

  // from right, but search for any of the categories, not by category in turn
  private int findRightDisHead(Parse[] children, String[] headRule) {
    for (int headIdx = children.length - 1; headIdx >= 0; headIdx--) {
      String childCat = children[headIdx].getType();
      for (int i = 1; i < headRule.length; i++) {
        if (headRule[i].equals(childCat)) {
          return headIdx;
        }
      }
    }
    return -1;
  }

  private int findRightExceptHead(Parse[] children, String[] headRule) {
    for (int headIdx = children.length - 1; headIdx >= 0; headIdx--) {
      String childCat = children[headIdx].getType();
      boolean found = true;
      for (int i = 1; i < headRule.length; i++) {
        if (headRule[i].equals(childCat)) {
          found = false;
        }
      }
      if (found) {
        return headIdx;
      }
    }
    return -1;
  }

  /* (non-Javadoc)
   * @see ixa.pipe.heads.HeadFinder#printHeads(opennlp.tools.parser.Parse)
   */
  public void printHeads(Parse parse) {
    LinkedList<Parse> nodes = new LinkedList<Parse>();
    //insert Parse into LinkedList
    nodes.add(parse);
    while (!nodes.isEmpty()) {
      Parse currentNode = nodes.removeFirst();
      Parse headChild = null;
      // if firstNode is TOP then mark its only child (usually S) as head
      if (currentNode.getType().equals(opennlp.tools.parser.AbstractBottomUpParser.TOP_NODE)) { 
        currentNode.getChildren()[0].setType(currentNode.getChildren()[0].getType() + headMark);
      }
      // recursive for every child of currentNode
      for (Parse child : currentNode.getChildren()) {
        // check it has more than one child and that is not a posTag child (means leaf here)
        if (child.getChildCount()  > 0 && !child.isPosTag()) {
          headChild = getHead(child);
          if (DEBUG) {
            System.err.println("headChild of " + child.getType() + " is "
                + headChild.getType() + " :" + headChild.getCoveredText());
          }
          // change type of child node which is identified as head by taking the 
          // index of headChild as match and adding a headMark =H to it 
            child.getChildren()[child.indexOf(headChild)].setType(headChild
                .getType() + headMark); 
        }
        // add child to last of LinkedList which will 
        // now take as currentNode the child of this child
        nodes.addLast(child);
      }
    }
  }

  /**
   * A way for subclasses to fix any heads under special conditions. The default
   * does nothing.
   * 
   * @param headIndex
   *          The index of the proposed head
   * @param children
   *          The array of daughter trees
   * @return The new headIndex
   */
  protected int correctFoundHeads(int headIndex, Parse[] children) {
    return headIndex;
  }
  
  /**
   * Gets the preterminal yield (i.e., tags) of the tree.  All data in
   * preterminal nodes is returned as a list ordered by the natural left to
   * right order of the tree. 
   * @param Parse parse to yield 
   * @return a {@code List} of the data in the tree's pre-leaves.
   */
  public List<String> preTerminalYield(Parse parse) {
    return preTerminalYield(parse, new ArrayList<String>());
  }
  
  /**
  * Gets the preterminal yield (i.e., tags) of the tree.  All data in
  * preleaf nodes is returned as a list ordered by the natural left to
  * right order of the tree. 
  *
  * @ param parse the parse tree
  * @param y The list in which the preterminals of the tree will be
  *          placed. Normally, this will be empty when the routine is called,
  *          but if not, the new yield is added to the end of the list.
  * @return a <code>List</code> of the data in the tree's pre-leaves.
  */
 public List<String> preTerminalYield(Parse parse, List<String> y) {
   if (parse.getChildCount() == 1 && parse.getChildren()[0].getChildCount() == 0) {
     y.add(parse.getType());
   } else {
     Parse[] children = parse.getChildren();
     for (int i = 0; i < children.length; i++) {
       this.preTerminalYield(children[i], y);
     }
   }
   return y;
 }
 
 /**
  * Return whether this node is a phrasal node or not.  A phrasal node
  * is defined to be a node which is not a leaf or a preterminal.
  * Worded positively, this means that it must have two or more children,
  * or one child that is not a leaf.
  *
  * @param parse the parse tree for which to determine whether is phrasal or not
  * @return <code>true</code> if the node is phrasal;
  *         <code>false</code> otherwise
  */
 public boolean isPhrasal(Parse parse) {
   Parse[] children = parse.getChildren();
   return !(children == null || children.length == 0 || (children.length == 1 && children[0].getChildCount() == 0));
 }

 
}
