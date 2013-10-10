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

import java.util.HashMap;

/**
 * Implements a variant on the HeadFinder rules found in Michael Collins' 1999
* thesis. These rules are provided by Stanford Parser and added in this 
* particular implementation to work with our AbstractHeadFinder.java. 
*
* The changes made by (Christopher Manning Michel Galley) of Stanford 
* on the initial Collins' rules are, and I quote: 
* "
* <ol>
* <li>The PRN rule used to just take the leftmost thing, not anymore. 
* <li>Remove IN as a possible head of S, and add FRAG (low priority)
* <li>Place NN before QP in ADJP head rules 
* * <li>Place PDT before RB and after CD in QP rules.  Also prefer CD to
* DT or RB.  And DT to RB.
* <li>Add DT, WDT as low priority choice for head of NP. Add PRP before PRN
* Add RBR as low priority choice of head for NP.
* <li>Prefer NP or NX as head of NX, and otherwise default to rightmost not
* leftmost (NP-like headedness)
* <li>VP: add JJ and NNP as low priority heads (many tagging errors)
*   Place JJ above NP in priority, as it is to be preferred to NP object.
* <li>PP: add PP as a possible head (rare conjunctions)
* <li>Added rule for POSSP (can be introduced by parser)
* <li>Added a sensible-ish rule for X.
* <li>Added NML head rules, which are the same as for NP.
* <li>NP head rule: NP and NML are treated almost identically (NP has precedence)
* <li>NAC head rule: NML comes after NN/NNS but after NNP/NNPS
* <li>PP head rule: JJ added
* <li>Added JJP (appearing in David Vadas's annotation), which seems to play
* the same role as ADJP.
* </ol>
* These rules are suitable for the Penn Treebank.
* <p/>
* A case that you apparently just can't handle well in this framework is
* (NP (NP ... NP)).  If this is a conjunction, apposition or similar, then
* the leftmost NP is the head, but if the first is a measure phrase like
* (NP $ 38) (NP a share) then the second should probably be the head.
* ". 
*
* @author ragerri
* 
**/


public class CollinsHeadFinderModified extends CollinsHeadFinder {
	

	  public CollinsHeadFinderModified() {
	    super(); // avoids punctuation as head in final default rule of getHead()
	    
	    headRules = new HashMap<String, HeadRule>();
	    // This version of HeadRule from Stanford's CoreNLP ModCollinsHeadFinder.java 
	    headRules.put("ADJP", new HeadRule(new String[][]{{"left", "$"}, {"rightdis", "NNS", "NN", "JJ", "QP", "VBN", "VBG"}, {"left", "ADJP"}, {"rightdis", "JJP", "JJR", "JJS", "DT", "RB", "RBR", "CD", "IN", "VBD"}, {"left", "ADVP", "NP"}}));
	    // from David Vadas, James R. Curran: Parsing Noun Phrases in the Penn Treebank. Computational Linguistics 37(4): 753-809 (2011)
	    headRules.put("JJP", new HeadRule(new String[][]{{"left", "NNS", "NN", "$", "QP", "JJ", "VBN", "VBG", "ADJP", "JJP", "JJR", "NP", "JJS", "DT", "FW", "RBR", "RBS", "SBAR", "RB"}}));
	    // head rule rewritten by C. Manning: JJ.* is often head and rightmost.
	    headRules.put("ADVP", new HeadRule(new String[][]{{"left", "ADVP", "IN"},{"rightdis", "RB", "RBR", "RBS", "JJ", "JJR", "JJS"},{"rightdis", "RP", "DT", "NN", "CD", "NP", "VBN", "NNP", "CC", "FW", "NNS", "ADJP", "NML"}}));
	    headRules.put("CONJP", new HeadRule(new String[][]{{"right", "CC", "RB", "IN"}}));
	    headRules.put("FRAG", new HeadRule(new String[][]{{"right"}})); // crap
	    headRules.put("INTJ", new HeadRule(new String[][]{{"left"}}));
	    headRules.put("LST", new HeadRule(new String[][]{{"right", "LS", ":"}}));
	    headRules.put("NAC", new HeadRule(new String[][]{{"left", "NN", "NNS", "NML", "NNP", "NNPS", "NP", "NAC", "EX", "$", "CD", "QP", "PRP", "VBG", "JJ", "JJS", "JJR", "ADJP", "JJP", "FW"}}));
	    headRules.put("NX", new HeadRule(new String[][]{{"right","NP","NX"}}));
	    //Changed PP search to left -- for conjunction (and consistent with SemanticHeadFinder)
	    // added JJ as head
	    headRules.put("PP", new HeadRule(new String[][]{{"right", "IN", "TO", "VBG", "VBN", "RP", "FW", "JJ", "SYM"}, {"left", "PP"}}));
	    headRules.put("PRN", new HeadRule(new String[][]{{"left", "VP", "NP", "PP", "SQ", "S", "SINV", "SBAR", "ADJP", "JJP", "ADVP", "INTJ", "WHNP", "NAC", "VBP", "JJ", "NN", "NNP"}}));
	    headRules.put("PRT", new HeadRule(new String[][]{{"right", "RP"}}));
	    headRules.put("QP", new HeadRule(new String[][]{{"left", "$", "IN", "NNS", "NN", "JJ", "CD", "PDT", "DT", "RB", "NCD", "QP", "JJR", "JJS"}}));
	    // reduced relative clause can be any predicate VP, ADJP, NP, PP.
	    // For choosing between NP and PP, really need to know which one is temporal to discard it. 
	    // ADVP?
	    headRules.put("RRC", new HeadRule(new String[][]{{"left", "RRC"}, {"right", "VP", "ADJP", "JJP", "NP", "PP", "ADVP"}}));
	    // removed IN -- go for main part of sentence; add FRAG
	    headRules.put("S", new HeadRule(new String[][]{{"left", "TO", "VP", "S", "FRAG", "SBAR", "ADJP", "JJP", "UCP", "NP"}}));
	    headRules.put("SBAR", new HeadRule(new String[][]{{"left", "WHNP", "WHPP", "WHADVP", "WHADJP", "IN", "DT", "S", "SQ", "SINV", "SBAR", "FRAG"}}));
	    headRules.put("SBARQ", new HeadRule(new String[][]{{"left", "SQ", "S", "SINV", "SBARQ", "FRAG", "SBAR"}}));
	    // Stanford CoreNLP: if you have 2 VP under a SINV, take the 2nd as syntactic head, because the 
	    // first is a topicalized VP complement of the second, but for now didn't change this, 
	    // since it didn't help parsing.  (If it were changed, it'd need to be also changed to the opposite 
	    // in SemanticHeadFinder.)
	    headRules.put("SINV", new HeadRule(new String[][]{{"left", "VBZ", "VBD", "VBP", "VB", "MD", "VBN", "VP", "S", "SINV", "ADJP", "JJP", "NP"}}));
	    // Should maybe put S before SQ for tag questions. Check.
	    headRules.put("SQ", new HeadRule(new String[][]{{"left", "VBZ", "VBD", "VBP", "VB", "MD", "AUX", "AUXG", "VP", "SQ"}}));  
	    headRules.put("UCP", new HeadRule(new String[][]{{"right"}}));
	    headRules.put("VP", new HeadRule(new String[][]{{"left", "TO", "VBD", "VBN", "MD", "VBZ", "VB", "VBG", "VBP", "VP", "AUX", "AUXG", "ADJP", "JJP", "NN", "NNS", "JJ", "NP", "NNP"}}));
	    headRules.put("WHADJP", new HeadRule(new String[][]{{"left", "WRB", "WHADVP", "RB", "JJ", "ADJP", "JJP", "JJR"}}));
	    headRules.put("WHADVP", new HeadRule(new String[][]{{"right", "WRB", "WHADVP"}}));
	    headRules.put("WHNP", new HeadRule(new String[][]{{"left", "WDT", "WP", "WP$", "WHADJP", "WHPP", "WHNP"}}));
	    headRules.put("WHPP", new HeadRule(new String[][]{{"right", "IN", "TO", "FW"}}));
	    headRules.put("X", new HeadRule(new String[][]{{"right", "S", "VP", "ADJP", "JJP", "NP", "SBAR", "PP", "X"}}));
	    headRules.put("NP", new HeadRule(new String[][]{{"rightdis", "NN", "NNP", "NNPS", "NNS", "NML", "NX", "POS", "JJR"}, {"left", "NP", "PRP"}, {"rightdis", "$", "ADJP", "JJP", "PRN", "FW"}, {"right", "CD"}, {"rightdis", "JJ", "JJS", "RB", "QP", "DT", "WDT", "RBR", "ADVP"}}));
	    headRules.put("NML", new HeadRule(new String[][]{{"rightdis", "NN", "NNP", "NNPS", "NNS", "NX", "NML", "POS", "JJR"}, {"left", "NP", "PRP"}, {"rightdis", "$", "ADJP", "JJP", "PRN"}, {"right", "CD"}, {"rightdis", "JJ", "JJS", "RB", "QP", "DT", "WDT", "RBR", "ADVP"}}));
	    headRules.put("TYPO", new HeadRule(new String[][]{{"left", "NN", "NP", "NML", "NNP", "NNPS", "TO","VBD", "VBN", "MD", "VBZ", "VB", "VBG", "VBP", "VP", "ADJP", "JJP", "FRAG"}})); 
	    headRules.put("EDITED", new HeadRule(new String[][] {{"left"}}));
	    headRules.put("XS", new HeadRule(new String[][] {{"right", "IN"}}));
	    headRules.put("VB", new HeadRule(new String[][]{{"left", "TO", "VBD", "VBN", "MD", "VBZ", "VB", "VBG", "VBP", "VP", "AUX", "AUXG", "ADJP", "JJP", "NN", "NNS", "JJ", "NP", "NNP"}}));
	  }



}
