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

/**
* This class is based on the SemanticHeadFinder of Stanford parser. It has been
* significantly modified to work with the Apache OpenNLP API and objects, 
* on which ixa-pipe-parse is based. Also several utility methods have been 
* added to the AbstractHeadFinder for this headFinder to work. 
* 
* The Stanford Semantic Head Finder is a variant of the the HeadFinder found in 
* Michael Collins' 1999 thesis. 
* 
* Stanford's semantic rules changes the order of preference for candidate constituents to be headnodes
* but also add extra rules based on the semantics of the verbs: 
* <quote>
* <p>
* For example. this head finder chooses the semantic head verb rather than the verb form
* for cases with verbs.  And it makes similar changes to other
* categories. <p/>
* <p>
* By default the SemanticHeadFinder uses a treatment of copula where the
* complement of the copula is taken as the head.  That is, a sentence like
* "Bill is big" the head will be "big". 
*
* This analysis is used for questions and declaratives for adjective
* complements and declarative nominal complements.  However Wh-sentences
* with nominal complements do not receive this treatment.
* "Who is the president?" is analyzed with "the president" as nsubj and "who"
* as "attr" of the copula, thus the head will be "is". 
* <p/>
* 
* Existential sentences are treated as follows:  <br/>
* "There is a man" <br/>
* "is" will be he head and "man" will be the head of "a man". 
* </quote>
* 
* @author ragerri
*/

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opennlp.tools.parser.Parse;

public class EnglishSemanticHeadFinder extends CollinsHeadFinderModified {
	
	 private static final boolean DEBUG = false;
	 /* A few times the apostrophe is missing on "'s", so we have "s" */
	  /* Tricky auxiliaries: "na" is from "gonna", "ve" from "Weve", etc.  "of" as non-standard for "have" */
	  private static final String[] auxiliaries = {"will", "wo", "shall", "sha", "may", "might", "should", "would", "can", "could", "ca", "must", "has", "have", "had", "having", "get", "gets", "getting", "got", "gotten", "do", "does", "did", "to", "'ve", "ve", "v", "'d", "d", "'ll", "ll", "na", "of", "hav", "hvae", "as" };
	  private static final String[] beGetVerbs = {"be", "being", "been", "am", "are", "r", "is", "ai", "was", "were", "'m", "m", "'re", "'s", "s", "art", "ar", "get", "getting", "gets", "got"};
	  private static final String[] copulaVerbs = {"be", "being", "been", "am", "are", "r", "is", "ai", "was", "were", "'m", "m", "'re", "'s", "s", "wase", "seem", "seems", "seemed", "appear", "appears", "appeared", "stay", "stays", "stayed", "remain", "remains", "remained", "resemble", "resembles", "resembled", "become", "becomes", "became"};

	  // include Charniak tags so can do BLLIP right
	  private static final String[] verbTags = {"TO", "MD", "VB", "VBD", "VBP", "VBZ", "VBG", "VBN", "AUX", "AUXG"};
	  // These ones are always auxiliaries, even if the word is "too", "my", or whatever else appears in web text.
	  private static final String[] unambiguousAuxTags = {"TO", "MD", "AUX", "AUXG"};


	  private final Set<String> verbalAuxiliaries;
	  private final Set<String> copulars;
	  private final Set<String> passiveAuxiliaries;
	  private final Set<String> verbalTags;
	  private final Set<String> unambiguousAuxiliaryTags;


	
	 public EnglishSemanticHeadFinder() { 
	   this(true);
	 }
	 
	  public EnglishSemanticHeadFinder(boolean copular) {
	    super();
	    // IMPORTANT: this modify some of the rules of the headRules map in CollinsHeadFinderModified,
	    // which in turn modifies CollinsHeadFinder
	    semanticHeadRules();
	    
	    // make a distinction between auxiliaries and copula verbs to
	    // get the NP has semantic head in sentences like "Bill is an honest man".
	    verbalAuxiliaries = new HashSet<String>(Arrays.asList(auxiliaries));
	    passiveAuxiliaries = new HashSet<String>(Arrays.asList(beGetVerbs));

	    //copula verbs having an NP complement
	    copulars = new HashSet<String>(Arrays.asList(copulaVerbs));
	    if (copular) {
	      copulars.addAll(Arrays.asList(copulaVerbs));
	    }
	    verbalTags = new HashSet<String>(Arrays.asList(verbTags));
	    unambiguousAuxiliaryTags = new HashSet<String>(Arrays.asList(unambiguousAuxTags));

	  } 
	  
	  // modifies the CollinsHeadFinderModified rules according to semantic notions
	  // These rules provided by Stanford parser; adapted to work with Apache OpenNLP parser API
	  // ragerri: I am doing all this "funny" work because I want to produce "semantic heads" for our 
	  // coreference system, not only for NP headWord detection but also for sieves such as strict head 
	  // matching
	  
	  private void semanticHeadRules() { 
		//  NP: don't want a POS to be the head
		    headRules.put("NP", new String[][]{{"rightdis", "NN", "NNP", "NNPS", "NNS", "NX", "NML", "JJR", "WP" }, {"left", "NP", "PRP"}, {"rightdis", "$", "ADJP", "FW"}, {"right", "CD"}, {"rightdis", "JJ", "JJS", "QP", "DT", "WDT", "NML", "PRN", "RB", "RBR", "ADVP"}, {"left", "POS"}});
		    // WHNP clauses should have the same sort of head as an NP
		    // but it a WHNP has a NP and a WHNP under it, the WHNP should be the head.  E.g.,  (WHNP (WHNP (WP$ whose) (JJ chief) (JJ executive) (NN officer))(, ,) (NP (NNP James) (NNP Gatward))(, ,))
		    headRules.put("WHNP", new String[][]{{"rightdis", "NN", "NNP", "NNPS", "NNS", "NX", "NML", "JJR", "WP"}, {"left", "WHNP", "NP"}, {"rightdis", "$", "ADJP", "PRN", "FW"}, {"right", "CD"}, {"rightdis", "JJ", "JJS", "RB", "QP"}, {"left", "WHPP", "WHADJP", "WP$", "WDT"}});
		    //WHADJP
		    headRules.put("WHADJP", new String[][]{{"left", "ADJP", "JJ", "JJR", "WP"}, {"right", "RB"}, {"right"}});
		    //WHADJP
		    headRules.put("WHADVP", new String[][]{{"rightdis", "WRB", "WHADVP", "RB", "JJ"}}); // if not WRB or WHADVP, probably has flat NP structure, allow JJ for "how long" constructions
		    // QP: we don't want the first CD to be the semantic head (e.g., "three billion": head should be "billion"), so we go from right to left
		    headRules.put("QP", new String[][]{{"right", "$", "NNS", "NN", "CD", "JJ", "PDT", "DT", "IN", "RB", "NCD", "QP", "JJR", "JJS"}});
		    // S, SBAR and SQ clauses should prefer the main verb as the head
		    // S: "He considered him a friend" -> we want a friend to be the head
		    headRules.put("S", new String[][]{{"left", "VP", "S", "FRAG", "SBAR", "ADJP", "UCP", "TO"}, {"right", "NP"}});
		    headRules.put("SBAR", new String[][]{{"left", "S", "SQ", "SINV", "SBAR", "FRAG", "VP", "WHNP", "WHPP", "WHADVP", "WHADJP", "IN", "DT"}});
		    // VP shouldn't be needed in SBAR, but occurs in one buggy tree in PTB3 wsj_1457 and otherwise does no harm
		    headRules.put("SQ", new String[][]{{"left", "VP", "SQ", "ADJP", "VB", "VBZ", "VBD", "VBP", "MD", "AUX", "AUXG"}});
		    // UCP take the first element as head
		    headRules.put("UCP", new String[][]{{"left"}});
		    // CONJP: we want different heads for "but also" and "but not" and we don't want "not" to be the head in "not to mention"; now make "mention" head of "not to mention"
		    headRules.put("CONJP", new String[][]{{"right", "VB", "JJ", "RB", "IN", "CC"}});
		    // FRAG: crap rule needs to be change if you want to parse glosses; but it is correct to have ADJP and ADVP before S because of weird parses of reduced sentences.
		    headRules.put("FRAG", new String[][]{{"left", "IN"}, {"right", "RB"}, {"left", "NP"}, {"left", "ADJP", "ADVP", "FRAG", "S", "SBAR", "VP"}});
		    // PRN: sentence first
		    headRules.put("PRN", new String[][]{{"left", "VP", "SQ", "S", "SINV", "SBAR", "NP", "ADJP", "PP", "ADVP", "INTJ", "WHNP", "NAC", "VBP", "JJ", "NN", "NNP"}});
		    headRules.put("XS", new String[][]{{"right", "IN"}});
		    // add a rule to deal with the CoNLL data
		    headRules.put("EMBED", new String[][]{{"right", "INTJ"}});
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
	        || ! origHeadInterjection && "UH".equals(parse.getType())) ||
	           "INTJ".equals(parse.getType()) && ! origHeadInterjection;
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
	          && (punctSet.contains(type) || ! origHeadInterjection && "UH".equals(type)) ||
	               "INTJ".equals(type) && ! origHeadInterjection) {
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
	      if (prevType.equals("CC") || prevType.equals("CONJP")) {
	        boolean origHeadInterjection = "UH".equals(children[headIndex].getType());
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
	  //TODO Lot of hard coding here. As ixa-pipe-parse is multilingual,
	  //find out if we can remove hard-coded categories for code reuse for 
	  //Semantic Head Finders for other languages. Or maybe the rules
	  //will be so different that it is not worth the effort
	  @Override
	  protected Parse getNonTrivialHead(Parse parse, Parse parent) {
	    String nonTerminal = parse.getType().replace(headMark, "");

	    if (DEBUG) {
	      System.err.println("At " + nonTerminal + ", my parent is " + parent);
	    }

	    // do VPs with auxiliary as special case
	    if ((nonTerminal.equals("VP") || nonTerminal.equals("SQ") || nonTerminal.equals("SINV"))) {
	      Parse[] children = parse.getChildren();

	      if (DEBUG) {
	        System.err.println("Semantic head finder: at VP");
	        System.err.println("Class is " + parse.getClass().getName());
	        System.err.println("hasVerbalAuxiliary = " + hasVerbalAuxiliary(children, verbalAuxiliaries, true));
	      }

	      // looks for auxiliaries
	      if (hasVerbalAuxiliary(children, verbalAuxiliaries, true) || hasPassiveProgressiveAuxiliary(children)) {
	        String[] headRule = { "left", "VP", "ADJP" };
	        Parse auxHeadNode = traverseParse(children, headRule, false);
	        if (DEBUG) {
	          System.err.println("Determined head (case 1) for " + parse.getType() + " is: " + auxHeadNode);
	        }
	        if (auxHeadNode != null) {
	          return auxHeadNode;
	        }
	      }

	      // looks for copular verbs
	      if (hasVerbalAuxiliary(children, copulars, false) && ! isExistential(parse, parent) && ! isWHQ(parse, parent)) {
	        String[] headRule;
	        if (nonTerminal.equals("SQ")) {
	          headRule = new String[]{"right", "VP", "ADJP", "NP", "WHADJP", "WHNP"};
	        } else {
	          headRule = new String[]{"left", "VP", "ADJP", "NP", "WHADJP", "WHNP"};
	        }
	        Parse auxHeadNode = traverseParse(children, headRule, false);
	        // don't allow a temporal to become head
	        if (auxHeadNode != null && auxHeadNode.getType() != null && auxHeadNode.getType().contains("-TMP")) {
	          auxHeadNode = null;
	        }
	        // In SQ, only allow an NP to become head if there is another one to the left (then it's probably predicative)
	        if (nonTerminal.equals("SQ") && auxHeadNode != null && auxHeadNode.getType() != null && auxHeadNode.getType().startsWith("NP")) {
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
	  
	  /**
	   * Checks whether the parse is existential: 
	   *   + affirmative sentences in which "there" is a left sister of the VP
	   *   + questions in which "there" is a child of the SQ
	   *    
	   * @param parse
	   * @param parent
	   * @return
	   */
	  private boolean isExistential(Parse parse, Parse parent) {
	    if (DEBUG) {
	      System.err.println("isExistential: " + parse + ' ' + parent);
	    }
	    boolean toReturn = false;
	    String nonTerminal = parse.getType().replace(headMark,"");
	    // affirmative case
	    if (nonTerminal.equals("VP") && parent != null) {
	      //take t and the sisters
	      Parse[] children = parent.getChildren();
	      // iterate over the sisters before t and checks if existential
	      for (Parse child : children) {
	        if (!child.getType().equals("VP")) {
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


	  /**
	   * Checks if a parse tree is a verbal auxiliary. Parse needs to be a preterminal which 
	   * is matched against various list of verbs contained in this class. 
	   * 
	   * @param preterminal
	   * @param verbalSet
	   * @param allowJustTagMatch
	   * @return
	   */
	  private boolean isVerbalAuxiliary(Parse preterminal, Set<String> verbalSet, boolean allowJustTagMatch) {
	    if (preterminal.getChildCount() == 1 && preterminal.getChildren()[0].getChildCount() == 0) {
	      String tag = null;
	      if (preterminal.isPosTag()) {
	        tag = preterminal.getType();
	      }
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
	      if (allowJustTagMatch && unambiguousAuxiliaryTags.contains(tag) || verbalTags.contains(tag) && verbalSet.contains(lcWord)) {
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
	    return isVerbalAuxiliary(preterminal, verbalAuxiliaries, true);
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
	      if (isVerbalAuxiliary(child, passiveAuxiliaries, false)) {
	          foundPassiveAux = true;
	      } else if (isPhrasal(child)) {
	        String type = null;
	        if (type == null) {
	          type = child.getType();
	        }
	        if ( ! type.startsWith("VP")) {
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
	          if (grandchild.getChildCount() == 1 && grandchild.getChildren()[0].getChildCount() == 0) {
	            String tag = null;
	            if (grandchild.isPosTag()) {
	              tag = grandchild.getType();
	            }
	            if (tag == null) {
	              tag = grandchild.getType();
	            }
	            // allow in VBD because of frequent tagging mistakes
	            if ("VBN".equals(tag) || "VBG".equals(tag) || "VBD".equals(tag)) {
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
	            if ("VP".equals(catcat)) {
	              if (DEBUG) {
	                System.err.println("hasPassiveAuxiliary found (VP (VP)), recursing");
	              }
	              foundParticipleInVp = vpContainsParticiple(grandchild);
	            } else if (("CONJP".equals(catcat) || "PRN".equals(catcat)) && foundParticipleInVp) { // occasionally get PRN in CONJ-like structures
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
	      if (child.getChildCount() == 1 && child.getChildren()[0].getChildCount() == 0) {
	        String tag = null;
	        if (child.isPosTag()) {
	          tag = child.getType();
	        }
	        if (tag == null) {
	          tag = child.getType();
	        }
	        if ("VBN".equals(tag) || "VBG".equals(tag) || "VBD".equals(tag)) {
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
	   *  which is within the set verbalSet (which in practice is either
	   *  auxiliary or copula verbs).  It only returns true if it's a preterminal head, since
	   *  you don't want to pick things up in non terminal children. 
	   *
	   * @param children The child trees
	   * @param verbalSet The set of words
	   * @param allowTagOnlyMatch If true, it's sufficient to match on an unambiguous auxiliary tag.
	   *                          Make true iff verbalSet is "all auxiliaries"
	   * @return Returns true if one of the child trees is a preterminal verb headed
	   *      by a word in verbalSet
	   */
	  private boolean hasVerbalAuxiliary(Parse[] children, Set<String> verbalSet, boolean allowTagOnlyMatch) {
	    if (DEBUG) {
	      System.err.println("Checking for verbal auxiliary");
	    }
	    for (Parse child : children) {
	      if (DEBUG) {
	        System.err.println("  checking in " + child);
	      }
	      if (isVerbalAuxiliary(child, verbalSet, allowTagOnlyMatch)) {
	        return true;
	      }
	    }
	    if (DEBUG) {
	      System.err.println("hasVerbalAuxiliary returns false");
	    }
	    return false;
	  }



}

