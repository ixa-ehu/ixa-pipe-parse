package ixa.pipe.heads;

import java.util.LinkedList;
import java.util.Map;

import opennlp.tools.parser.Parse;

public class AbstractHeadFinder implements HeadFinder {
	
  private static final boolean DEBUG = true;
  protected Map<String, String[][]> headRules;
  
  

  /** Default direction if no rule is found for category (the head/parent).
   *  Subclasses can turn it on if they like.
   *  If they don't it is an error if no rule is defined for a category
   *  (null is returned).
   */
  protected String[] defaultRule; // = null;

  /** These are built automatically from categoriesToAvoid and used in a fairly
   *  different fashion from defaultRule (above).  These are used for categories
   *  that do have defined rules but where none of them have matched.  Rather
   *  than picking the rightmost or leftmost child, we will use these to pick
   *  the the rightmost or leftmost child which isn't in categoriesToAvoid.
   */
  protected String[] defaultLeftRule;
  protected String[] defaultRightRule;

  /**
   * A way for subclasses for corpora with explicit head markings
   * to return the explicitly marked head
   *
   * @param t a tree to find the head of
   * @return the marked head-- null if no marked head
   */
  // to be overridden in subclasses for corpora
  //
  protected Parse findMarkedHead(Parse t) {
    return null;
  }

  /**
   * Determine which daughter of the current parse tree is the head.
   *
   * @param t The parse tree to examine the daughters of.
   *          If this is a leaf, <code>null</code> is returned
   * @return The daughter parse tree that is the head of <code>t</code>
   * @see Tree#percolateHeads(HeadFinder)
   *      for a routine to call this and spread heads throughout a tree
   */
  
  public Parse determineHead(Parse t) {
    return determineHead(t, null);
  }
  
  /**
   * Determine which daughter of the current parse tree is the head.
   *
   * @param t The parse tree to examine the daughters of.
   *          If this is a leaf, <code>null</code> is returned
   * @param parent The parent of t
   * @return The daughter parse tree that is the head of <code>t</code>.
   *   Returns null for leaf nodes.
   * @see Tree#percolateHeads(HeadFinder)
   *      for a routine to call this and spread heads throughout a tree
   */
 
  public Parse determineHead(Parse t, Parse parent) {
    if (headRules == null) {
      throw new IllegalStateException("Classes derived from AbstractCollinsHeadFinder must create and fill HashMap nonTerminalInfo.");
    }
    if (t == null || t.getChildCount() == 0) {
      throw new IllegalArgumentException("Can't return head of null or leaf Tree.");
    }
    if (DEBUG) {
      System.err.println("determineHead for " + t.getType());
    }

    Parse[] kids = t.getChildren();

    Parse theHead;
    // first check if subclass found explicitly marked head
    if ((theHead = findMarkedHead(t)) != null) {
      if (DEBUG) {
        System.err.println("Find marked head method returned " +
                           theHead.getType() + " as head of " + t.getType());
      }
      return theHead;
    }

    // if the node is a unary, then that kid must be the head
    // it used to special case preterminal and ROOT/TOP case
    // but that seemed bad (especially hardcoding string "ROOT")
    if (kids.length == 1) {
      if (DEBUG) {
        System.err.println("Only one child determines " +
                           kids[0].getLabel() + " as head of " + t.getType());
      }
      return kids[0];
    }

    return determineNonTrivialHead(t, parent);
  }
  
  /** Called by determineHead and may be overridden in subclasses
   *  if special treatment is necessary for particular categories.
   *
   *  @param t The tre to determine the head daughter of
   *  @param parent The parent of t (or may be null)
   *  @return The head daughter of t
   */
  protected Parse determineNonTrivialHead(Parse t, Parse parent) {
    Parse theHead = null;
    String motherCat = t.getType();
    if (DEBUG) {
      System.err.println("Looking for head of " + t.getType() +
                         "; value is |" + t.getType() + "|, " +
                         " baseCat is |" + motherCat + '|');
    }
    String[][] how = headRules.get(motherCat);
    Parse[] kids = t.getChildren();
    if (how == null) {
      if (DEBUG) {
        System.err.println("Warning: No rule found for " + motherCat +
                           " (first char: " + motherCat.charAt(0) + ')');
        System.err.println("Known nonterms are: " + headRules.keySet());
      }
      if (defaultRule != null) {
        if (DEBUG) {
          System.err.println("  Using defaultRule");
        }
        return traverseLocate(kids, defaultRule, true);
      } else {
        throw new IllegalArgumentException("No head rule defined for " + motherCat + " using " + this.getClass() + " in " + t);
      }
    }
    for (int i = 0; i < how.length; i++) {
      boolean lastResort = (i == how.length - 1);
      theHead = traverseLocate(kids, how[i], lastResort);
      if (theHead != null) {
        break;
      }
    }
    if (DEBUG) {
      System.err.println("  Chose " + theHead.getType());
    }
    return theHead;
  }
  
  /**
   * Attempt to locate head daughter tree from among daughters.
   * Go through daughterTrees looking for things from or not in a set given by
   * the contents of the array how, and if
   * you do not find one, take leftmost or rightmost perhaps matching thing iff
   * lastResort is true, otherwise return <code>null</code>.
   */
  protected Parse traverseLocate(Parse[] daughterTrees, String[] how, boolean lastResort) {
    int headIdx;
    if (how[0].equals("left")) {
      headIdx = findLeftHead(daughterTrees, how);
    } else if (how[0].equals("leftdis")) {
      headIdx = findLeftDisHead(daughterTrees, how);
    } else if (how[0].equals("leftexcept")) {
      headIdx = findLeftExceptHead(daughterTrees, how);
    } else if (how[0].equals("right")) {
      headIdx = findRightHead(daughterTrees, how);
    } else if (how[0].equals("rightdis")) {
      headIdx = findRightDisHead(daughterTrees, how);
    } else if (how[0].equals("rightexcept")) {
      headIdx = findRightExceptHead(daughterTrees, how);
    } else {
      throw new IllegalStateException("ERROR: invalid direction type " + how[0] + " to nonTerminalInfo map in AbstractCollinsHeadFinder.");
    }

    // what happens if our rule didn't match anything
    if (headIdx < 0) {
      if (lastResort) {
        // use the default rule to try to match anything except categoriesToAvoid
        // if that doesn't match, we'll return the left or rightmost child (by
        // setting headIdx).  We want to be careful to ensure that postOperationFix
        // runs exactly once.
        String[] rule;
        if (how[0].startsWith("left")) {
          headIdx = 0;
          rule = defaultLeftRule;
        } else {
          headIdx = daughterTrees.length - 1;
          rule = defaultRightRule;
        }
        Parse child = traverseLocate(daughterTrees, rule, false);
        if (child != null) {
          return child;
        } else {
          return daughterTrees[headIdx];
        }
      } else {
        // if we're not the last resort, we can return null to let the next rule try to match
        return null;
      }
    }

    headIdx = postOperationFix(headIdx, daughterTrees);

    return daughterTrees[headIdx];
  }

  private int findLeftHead(Parse[] daughterTrees, String[] how) {
    for (int i = 1; i < how.length; i++) {
      for (int headIdx = 0; headIdx < daughterTrees.length; headIdx++) {
        String childCat = daughterTrees[headIdx].getType();
        if (how[i].equals(childCat)) {
          return headIdx;
        }
      }
    }
    return -1;
  }

  private int findLeftDisHead(Parse[] daughterTrees, String[] how) {
    for (int headIdx = 0; headIdx < daughterTrees.length; headIdx++) {
      String childCat = daughterTrees[headIdx].getType();
      for (int i = 1; i < how.length; i++) {
        if (how[i].equals(childCat)) {
          return headIdx;
        }
      }
    }
    return -1;
  }

  private int findLeftExceptHead(Parse[] daughterTrees, String[] how) {
    for (int headIdx = 0; headIdx < daughterTrees.length; headIdx++) {
      String childCat = daughterTrees[headIdx].getType();
      boolean found = true;
      for (int i = 1; i < how.length; i++) {
        if (how[i].equals(childCat)) {
          found = false;
        }
      }
      if (found) {
        return headIdx;
      }
    }
    return -1;
  }

  private int findRightHead(Parse[] daughterTrees, String[] how) {
    for (int i = 1; i < how.length; i++) {
      for (int headIdx = daughterTrees.length - 1; headIdx >= 0; headIdx--) {
        String childCat = daughterTrees[headIdx].getType();
        if (how[i].equals(childCat)) {
          return headIdx;
        }
      }
    }
    return -1;
  }

  // from right, but search for any of the categories, not by category in turn
  private int findRightDisHead(Parse[] daughterTrees, String[] how) {
    for (int headIdx = daughterTrees.length - 1; headIdx >= 0; headIdx--) {
      String childCat = daughterTrees[headIdx].getType();
      for (int i = 1; i < how.length; i++) {
        if (how[i].equals(childCat)) {
          return headIdx;
        }
      }
    }
    return -1;
  }

  private int findRightExceptHead(Parse[] daughterTrees, String[] how) {
    for (int headIdx = daughterTrees.length - 1; headIdx >= 0; headIdx--) {
      String childCat = daughterTrees[headIdx].getType();
      boolean found = true;
      for (int i = 1; i < how.length; i++) {
        if (how[i].equals(childCat)) {
          found = false;
        }
      }
      if (found) {
        return headIdx;
      }
    }
    return -1;
  }

  public void printHeads(Parse parse) {
	  LinkedList<Parse> nodes = new LinkedList<Parse>();
	    nodes.add(parse);
	    // This is a recursive iteration over the whole parse tree
	    while (!nodes.isEmpty()) {
	      Parse currentNode = nodes.removeFirst();
	      // When a node is here its '=H' annotation has already happened
	      // so it '=H' has to be removed to match with the head rules
	      Parse headChild = null;
	      // For every child, if it is the head, annotate with '=H'
	      // and also add to the queue for the recursive processing
	      Parse[] children = currentNode.getChildren();
	      for (int i = 0; i < children.length; i++) {
	        if (children[i].getChildCount() > 1) {
	    	  headChild = determineHead(children[i]);
	    	  if (DEBUG) { 
	    		  System.err.println("headChild of " + children[i].getType() + " is " + headChild.getType());  
	    	  }
	    	  
	        if (children[i].getChildren()[children[i].indexOf(headChild)] != null) {
	          children[children[i].indexOf(headChild)].setType(headChild.getType() + "=H");
	        }
	        nodes.addLast(children[i]);
	      }
	      
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
  protected int postOperationFix(int headIdx, Parse[] daughterTrees) {
    return headIdx;
  }
}
