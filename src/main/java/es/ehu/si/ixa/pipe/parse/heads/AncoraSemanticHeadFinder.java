package es.ehu.si.ixa.pipe.parse.heads;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opennlp.tools.parser.Parse;

/**
 * Extends the AncoraHeadFinder by adding specific rules based on lexical information. 
 * 
 * @author ragerri
 * @version 2014-03-12
 */
public class AncoraSemanticHeadFinder extends AncoraHeadFinder {


  private static final boolean DEBUG = false;
  
   private static final String[] copulars = {"pareceres","parecer","pareced","parecéis","parecemos","parecen","parece","parece","parecerán",
     "parecerá","parecerás","pareceréis","pareceremos","pareceré","pareceríais","pareceríamos","parecerían","parecería","parecería","parecerías",
     "parecer","pareces","parecíais","parecíamos","parecían","parecía","parecía","parecías","parecida","parecidas","parecido","parecidos",
     "pareciendo","parecierais","pareciéramos","parecieran","pareciera","pareciera","parecieras","pareciereis","pareciéremos","parecieren",
     "pareciere","pareciere","parecieres","parecieron","parecieseis","pareciésemos","pareciesen","pareciese","pareciese","parecieses","parecimos",
     "pareció","parecí","parecisteis","pareciste","parezcáis","parezcamos","parezcamos","parezcan","parezcan","parezca","parezca","parezca",
     "parezcas","parezco","estaba","estaba","estabais","estábamos","estaban","estabas","estad","estado","está","está","estáis","estamos",
     "estando","están","estará","estarán","estarás","estaré","estaréis","estaremos","estar","estaría","estaría","estaríais","estaríamos",
     "estarían","estarías","estás","esté","esté","esté","estéis","estemos","estemos","estén","estén","estés","estoy","estuve","estuviera",
     "estuviera","estuvierais","estuviéramos","estuvieran","estuvieras","estuviere","estuviere","estuviereis","estuviéremos","estuvieren",
     "estuvieres","estuvieron","estuviese","estuviese","estuvieseis","estuviésemos","estuviesen","estuvieses","estuvimos","estuviste","estuvisteis",
     "estuvo", "erais","éramos","eran","era","era","eras","eres","es","fuerais","fuéramos","fueran","fuera","fuera","fueras","fuereis","fuéremos",
     "fueren","fuere","fuere","fueres","fueron","fueseis","fuésemos","fuesen","fue","fuese","fuese","fueses","fuimos","fui","fuisteis","fuiste",
     "seáis","seamos","seamos","sean","sean","sea","sea","sea","seas","sed","serán","será","serás","seréis","seremos","seré","seríais",
     "seríamos","serían","sería","sería","serías","ser","sé","sido","siendo","sois","somos","son","soy"};


 
   private final Set<String> copularVerbs;
  
  public AncoraSemanticHeadFinder() { 
    this(true);
  }
    
    public AncoraSemanticHeadFinder(boolean copular) {
      super();
      // IMPORTANT: this modifies some of the rules in the AncoraHeadFinder map
      semanticHeadRules();

      //copula verbs having an NP complement
      copularVerbs = new HashSet<String>(Arrays.asList(copulars));
      if (copular) {
        copularVerbs.addAll(Arrays.asList(copulars));
      }

    }
    
    private void semanticHeadRules() { 
      headRules.put("SN", new String[][]{{"rightdis", "NC.*S.*", "NP.*","NC.*P.*", "SN","GRUP\\.NOM", "AQA.*","AQC.*","GRUP\\.A","S\\.A"}, {"left", "SN","GRUP\\.NOM","P.*"}, {"rightdis", "\\$","SA", "S\\.A","GRUP\\.A"}, {"right", "Z.*"}, {"rightdis", "AQ0.*", "AQ[AC].*","AO.*","GRUP\\.A","S\\.A","GRUP\\.NOM", "D.*","RG","RN","SADV","GRUP\\.ADV"}});
      headRules.put("GRUP.NOM",new String[][]{{"rightdis", "NC.*S.*", "NP.*","NC.*P.*", "GRUP\\.NOM", "AQA.*","AQC.*","GRUP\\.A","S\\.A"}, {"left", "GRUP\\.NOM","P.*"}, {"rightdis", "\\$","SA", "S\\.A","GRUP\\.A"}, {"right", "Z.*"}, {"rightdis", "AQ0.*", "AQ[AC].*","AO.*","GRUP\\.A","S\\.A","GRUP\\.NOM", "D.*","RG","RN","SADV","GRUP\\.ADV"}});
      headRules.put("SENTENCE", new String[][] {{"left","GRUP\\.VERB","S","SA","S\\.A","GRUP\\.A","COORD","CONJ","PREP","SP[CS].*"}, {"right","SN","GRUP\\.NOM"}});
      headRules.put("S", new String[][] {{"left","GRUP\\.VERB","S","SA","S\\.A","GRUP\\.A","COORD","CONJ","PREP","SP[CS].*"}, {"right","SN","GRUP\\.NOM"}});
      headRules.put("COORD", new String[][]{{"right", "GRUP\\.VERB", "A[QO][AC].*","GRUP\\.A","S\\.A","RB","RN","PREP","SP[CS].*","CC"}});
      headRules.put("CONJ", new String[][]{{"right", "GRUP\\.VERB", "A[QO][AC].*","GRUP\\.A","S\\.A","RB","RN","PREP","SP[CS].*","CC"}});   
    }
    
    
    /**
     * skip a parse as a head if is a leaf, or punctuation or interjection 
     * 
     * @param parse
     * @param origHeadInterjection
     * @return
     */
    private boolean skipHeads(Parse parse, boolean origHeadInterjection) {
      return (parse.getChildCount() == 1 && parse.getChildren()[0].getChildCount() == 0) && (punctSet.contains(parse.getType()) 
          || ! origHeadInterjection && "I".equals(parse.getType())) ||
             "INTERJECCIO".equals(parse.getType()) && ! origHeadInterjection;
    }
    
    /**
     * Finds previous head ignoring punctuation and interjections
     * 
     * @param headIndex
     * @param children
     * @param origHeadInterjection
     * @return newHeadIndex
     */
    private int findPreviousHead(int headIndex, Parse[] children, boolean origHeadInterjection) {
      boolean seenSeparator = false;
      int newHeadIndex = headIndex;
      while (newHeadIndex >= 0) {
        newHeadIndex = newHeadIndex - 1;
        if (newHeadIndex < 0) {
          return newHeadIndex;
        }
        String type = children[newHeadIndex].getType();
        if (",".equals(type) || ":".equals(type)) {
          seenSeparator = true;
        } else if ((children[newHeadIndex].getChildCount() == 1 && 
            children[newHeadIndex].getChildren()[0].getChildCount() == 1) 
            && (punctSet.contains(type) || ! origHeadInterjection && "I".equals(type)) ||
                 "INTERJECCIO".equals(type) && ! origHeadInterjection) {
          // keep looping
        } else {
          if ( ! seenSeparator) {
            newHeadIndex = -1;
          }
          break;
        }
      }
      return newHeadIndex;
    }
    
    
    /* (non-Javadoc)
     * @see ixa.pipe.heads.AbstractHeadFinder#correctFoundHeads(int, opennlp.tools.parser.Parse[])
     * 
     * Overwrite the correctFoundHeads method.  For "a, b and c" or similar: "a" is the head.
     */
    @Override
    protected int correctFoundHeads(int headIndex, Parse[] children) {
      if (headIndex >= 2) {
        String prevType = children[headIndex - 1].getType();
        if (prevType.equals("CC") || prevType.equals("CONJ")) {
          boolean origHeadInterjection = "I".equals(children[headIndex].getType());
          int newHeadIndex = headIndex - 2;
          // newHeadIdx is now left of conjunction.  Now try going back over commas, etc. for 3+ conjuncts
          // Don't allow INTJ unless conjoined with INTJ - important in informal genres "Oh and don't forget to call!"
          while (newHeadIndex >= 0 && skipHeads(children[newHeadIndex], origHeadInterjection)) {
            newHeadIndex--;
          }
          // We're now at newHeadIdx < 0 or have found a left head
          // Now consider going back some number of punct that includes a , or : tagged thing and then find non-punct
          while (newHeadIndex >= 2) {
            int nextHead = findPreviousHead(newHeadIndex, children, origHeadInterjection);
            if (nextHead < 0) {
              break;
            }
            newHeadIndex = nextHead;
          }
          if (newHeadIndex >= 0) {
            headIndex = newHeadIndex;
          }
        }
      }
      return headIndex;
    }
    

    /**
     * Find child of the current parse tree is the
     * head.  It assumes that the children already have had their
     * heads determined.  Uses special rule for VP heads
     *
     * @param parse The parse tree to examine the children of.
     *          This is assumed to never be a leaf
     * @param parent the parse parent of the parse tree
     * @return The parse tree that is the head
     */
    @Override
    protected Parse getNonTrivialHead(Parse parse, Parse parent) {
      String nonTerminal = parse.getType().replace(headMark, "");

      if (DEBUG) {
        System.err.println("At " + nonTerminal + ", my parent is " + parent);
      }

      // do VPs with auxiliary as special case
      if (nonTerminal.equals("GRUP.VERB")) {
        Parse[] children = parse.getChildren();

        if (DEBUG) {
          System.err.println("Semantic head finder: at GRUP.VERB");
          System.err.println("Class is " + parse.getClass().getName());
          System.err.println("hasVerbalAuxiliary = " + hasVerbalAuxiliary(children, true));
        }

        // looks for auxiliaries
        if (hasVerbalAuxiliary(children, true) || hasPassiveProgressiveAuxiliary(children)) {
          String[] headRule = { "left", "GRUP\\.VERB", "SA","S\\.A","GRUP\\.A"};
          Parse auxHeadNode = traverseParse(children, headRule, false);
          if (DEBUG) {
            System.err.println("Determined head (case 1) for " + parse.getType() + " is: " + auxHeadNode);
          }
          if (auxHeadNode != null) {
            return auxHeadNode;
          }
        }

        // looks for copular verbs
        if (hasCopular(children) && ! isExistential(parse, parent) && ! isWHQ(parse, parent)) {
          String[] headRule;
          if (nonTerminal.equals("SQ")) {
            headRule = new String[]{"right", "VP", "ADJP", "NP", "WHADJP", "WHNP"};
          } else {
            headRule = new String[]{"left", "GRUP\\.VERB", "SA", "S\\.A","GRUP\\.A","SN","GRUP\\.NOM"};
          }
          Parse auxHeadNode = traverseParse(children, headRule, false);
          // don't allow a temporal to become head
          if (auxHeadNode != null && auxHeadNode.getType() != null && auxHeadNode.getType().contains("-TMP")) {
            auxHeadNode = null;
          }
          // In SQ, only allow an NP to become head if there is another one to the left (then it's probably predicative)
          if (nonTerminal.equals("SQ") && auxHeadNode != null && auxHeadNode.getType() != null && auxHeadNode.getType().startsWith("SN")) {
              boolean foundAnotherNP = false;
              for (Parse child : children) {
                if (child == auxHeadNode) {
                  break;
                } else if (child.getType() != null && child.getType().startsWith("NP")) {
                  foundAnotherNP = true;
                  break;
                }
              }
            if ( ! foundAnotherNP) {
              auxHeadNode = null;
            }
          }

          if (DEBUG) {
            System.err.println("Determined head (case 2) for " + parse.getType() + " is: " + auxHeadNode);
          }
          if (auxHeadNode != null) {
            return auxHeadNode;
          } else {
            if (DEBUG) {
              System.err.println("------");
              System.err.println("SemanticHeadFinder failed to reassign head for");
              System.err.println("------");
            }
          }
        }
      }

      Parse hd = super.getNonTrivialHead(parse, parent);

      if (DEBUG) {
        System.err.println("Determined head (case 3) for " + parse.getType() + " is: " + hd);
      }
      return hd;
    }
    
    /** This looks to see whether any of the children is a preterminal headed by a word
     *  which matches the Ancora tags for auxiliary verbs. 
     *
     * @param children The child trees
     * @param allowTagOnlyMatch If true, it's sufficient to match on an unambiguous auxiliary tag.
     *                          Make true iff verbalSet is "all auxiliaries"
     * @return Returns true if one of the child trees is a preterminal verb headed
     *      by an auxiliary Verb
     */
    private boolean hasVerbalAuxiliary(Parse[] children, boolean allowTagOnlyMatch) {
      if (DEBUG) {
        System.err.println("Checking for verbal auxiliary");
      }
      for (Parse child : children) {
        if (DEBUG) {
          System.err.println("  checking in " + child);
        }
        if (isVerbalAuxiliary(child, allowTagOnlyMatch)) {
          return true;
        }
      }
      if (DEBUG) {
        System.err.println("hasVerbalAuxiliary returns false");
      }
      return false;
    }

    
    /**
     * Checks if a parse tree is a verbal auxiliary. Parse needs to be a preterminal which 
     * is matched against the tags for Auxiliary verbs in Ancora tagset
     * 
     * @param preterminal
     * @param allowJustTagMatch
     * @return
     */
    private boolean isVerbalAuxiliary(Parse preterminal, boolean allowJustTagMatch) {
      if (preterminal.isPosTag()) {
        String tag = preterminal.getType();
        if (tag == null) {
          tag = preterminal.getType();
        }
        String word = null;
        if (word == null) {
          word = preterminal.getChildren()[0].getCoveredText();
        }
        if (DEBUG) {
          System.err.println("Checking " + preterminal.getType() + " head is " + word + '/' + tag);
        }
        if (allowJustTagMatch && tag.matches("V[A][IS][FC].*") || tag.matches("V[A].*")) {
          if (DEBUG) {
            System.err.println("isAuxiliary found desired type of aux");
          }
          return true;
        }
      }
      return false;
    }

    /**
     * Returns true if this parse is a preterminal that is a verbal auxiliary.
     *
     * @param preterminal A tree to examine for being an auxiliary.
     * @return Whether it is a verbal auxiliary (be, do, have, get)
     */
    public boolean isVerbalAuxiliary(Parse preterminal) {
      return isVerbalAuxiliary(preterminal, true);
    }

    /**
     * Checks if children's parse has passive progressive auxiliary 
     * 
     * @param children
     * @return
     */
    private boolean hasPassiveProgressiveAuxiliary(Parse[] children) {
      if (DEBUG) {
        System.err.println("Checking for passive/progressive auxiliary");
      }
      boolean foundPassiveVP = false;
      boolean foundPassiveAux = false;
      for (Parse child : children) {
        if (DEBUG) {
          System.err.println("  checking in " + child);
        }
        if (child.getType().matches("V[S].*")) {
            foundPassiveAux = true;
        } else if (isPhrasal(child)) {
          String type = null;
          if (type == null) {
            type = child.getType();
          }
          if (!type.startsWith("GRUP.VERB")) {
            continue;
          }
          if (DEBUG) {
            System.err.println("hasPassiveProgressiveAuxiliary found VP");
          }
          Parse[] grandchildren = child.getChildren();
          boolean foundParticipleInVp = false;
          for (Parse grandchild : grandchildren) {
            if (DEBUG) {
              System.err.println("  hasPassiveProgressiveAuxiliary examining " + grandchild);
            }
            if (grandchild.isPosTag()) {
              String tag = grandchild.getType();
              
              if (tag == null) {
                tag = grandchild.getType();
              }
              // allow in the third option because of frequent tagging mistakes
              if (tag.matches("V[MAS]P.*") || tag.matches("V[MAS]G.*") || tag.matches("V[MAS][ISMN][IS].*")) {
                foundPassiveVP = true;
                if (DEBUG) {
                  System.err.println("hasPassiveAuxiliary found VBN/VBG/VBD VP");
                }
                break;
              } else if ("CC".equals(tag) && foundParticipleInVp) {
                foundPassiveVP = true;
                if (DEBUG) {
                  System.err.println("hasPassiveAuxiliary [coordination] found (VP (VP[VBN/VBG/VBD] CC");
                }
                break;
              }
            } else if (isPhrasal(grandchild)) {
              String catcat = null;
              if (catcat == null) {
                catcat = child.getType();
              }
              if ("GRUP.VERB".equals(catcat)) {
                if (DEBUG) {
                  System.err.println("hasPassiveAuxiliary found (VP (VP)), recursing");
                }
                foundParticipleInVp = vpContainsParticiple(grandchild);
              } else if (("CONJ".equals(catcat) || foundParticipleInVp)) { // occasionally get PRN in CONJ-like structures
                foundPassiveVP = true;
                if (DEBUG) {
                  System.err.println("hasPassiveAuxiliary [coordination] found (VP (VP[VBN/VBG/VBD] CONJP");
                }
                break;
              }
            }
          }
        }
        if (foundPassiveAux && foundPassiveVP) {
          break;
        }
      } // end for (Tree child : children)
      if (DEBUG) {
        System.err.println("hasPassiveProgressiveAuxiliary returns " + (foundPassiveAux && foundPassiveVP));
      }
      return foundPassiveAux && foundPassiveVP;
    }
    
    /**
     * Check if parse contains a participle verb
     * 
     * @param parse
     * @return
     */
    private static boolean vpContainsParticiple(Parse parse) {
      for (Parse child : parse.getChildren()) {
        if (DEBUG) {
          System.err.println("vpContainsParticiple examining " + child);
        }
        if (child.isPosTag()) {
          String tag = child.getType();
          
          if (tag == null) {
            tag = child.getType();
          }
          if (tag.matches("V[MAS]P.*") || tag.matches("V[MAS]G.*") || tag.matches("V[MAS][ISMN][IS].*")) {
            if (DEBUG) {
              System.err.println("vpContainsParticiple found VBN/VBG/VBD VP");
            }
            return true;
          }
        }
      }
      return false;
    }
    
    /** This looks to see whether any of the children is a preterminal headed by a word
     *  which is within the set copularVerbs (ser, estar y parecer).
     *  It only returns true if it's a preterminal head. 
     *
     * @param children The child trees
     * @param copularVerbs The set of verbs
     * @return Returns true if one of the child trees is a preterminal verb headed
     *      by a word in copularVerbs
     */
    private boolean hasCopular(Parse[] children) {
      if (DEBUG) {
        System.err.println("Checking for verbal auxiliary");
      }
      for (Parse child : children) {
        if (DEBUG) {
          System.err.println("  checking in " + child);
        }
        if (isCopular(child)) {
          return true;
        }
      }
      if (DEBUG) {
        System.err.println("hasVerbalAuxiliary returns false");
      }
      return false;
    }

    
    /**
     * Checks if a parse tree contains a copular verb: ser, estar o parecer. 
     * Parse needs to be a preterminal which 
     * is matched against various list of verbs contained in this class. 
     * 
     * @param preterminal
     * @return
     */
    private boolean isCopular(Parse preterminal) {
      if (preterminal.isPosTag()) {
        String tag = preterminal.getType();
        if (tag == null) {
          tag = preterminal.getType();
        }
        String word = null;
        if (word == null) {
          word = preterminal.getChildren()[0].getCoveredText();
        }
        if (DEBUG) {
          System.err.println("Checking " + preterminal.getType() + " head is " + word + '/' + tag);
        }
        String lcWord = word.toLowerCase();
        if (tag.matches("VS.*") || copularVerbs.contains(lcWord)) {
          if (DEBUG) {
            System.err.println("isAuxiliary found desired type of aux");
          }
          return true;
        }
      }
      return false;
    }
    
    /**
     * Checks whether the parse is existential: 
     *   + affirmative sentences in which "there" is a left sister of the VP
     *   + questions in which "there" is a child of the SQ
     *    
     * @param parse
     * @param parent
     * @return
     * 
     */
    //TODO how to do this in Ancora with different tagset?
    private boolean isExistential(Parse parse, Parse parent) {
      if (DEBUG) {
        System.err.println("isExistential: " + parse + ' ' + parent);
      }
      boolean toReturn = false;
      String nonTerminal = parse.getType().replace(headMark,"");
      // affirmative case
      if (nonTerminal.equals("GRUP.VERB") && parent != null) {
        //take t and the sisters
        Parse[] children = parent.getChildren();
        // iterate over the sisters before t and checks if existential
        for (Parse child : children) {
          if (!child.getType().equals("GRUP.VERB")) {
            List<String> tags = preTerminalYield(child);
            for (String tag : tags) {
              if (tag.equals("EX")) {
                toReturn = true;
              }
            }
          } else {
            break;
          }
        }
      }
      // question case
      else if (nonTerminal.startsWith("SQ") && parent != null) {
        //take the daughters
        Parse[] children = parent.getChildren();
        // iterate over the daughters and checks if existential
        for (Parse child : children) {
          if (!child.getType().startsWith("VB")) {//not necessary to look into the verb
            List<String> tags = preTerminalYield(child);
            for (String tag : tags) {
              if (tag.equals("EX")) {
                toReturn = true;
              }
            }
          }
        }
      }

      if (DEBUG) {
        System.err.println("decision " + toReturn);
      }

      return toReturn;
    }
    
    /**
     * Checks whether the parse tree is a WH-question. True if parse is a SQ 
     * which has a WH.* as sister and headed by SBARQ. 
     * 
     * @param parse
     * @param parent
     * @return
     */
    //TODO how to do this in Ancora with different tagset?
    private static boolean isWHQ(Parse parse, Parse parent) {
      if (parse == null) {
        return false;
      }
      boolean toReturn = false;
      if (parse.getType().startsWith("SQ")) {
        if (parent != null && parent.getType().equals("SBARQ")) {
          Parse[] children = parent.getChildren();
          for (Parse child : children) {
            // looks for a WH.*
            if (child.getType().startsWith("WH")) {
              toReturn = true;
            }
          }
        }
      }

      if (DEBUG) {
        System.err.println("in isWH, decision: " + toReturn + " for node " + parse);
      }

      return toReturn;
    }
  
}
