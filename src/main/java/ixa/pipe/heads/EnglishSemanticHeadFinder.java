package ixa.pipe.heads;

import java.util.Arrays;
import java.util.HashSet;
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


	
	 

	  public EnglishSemanticHeadFinder(boolean copular) {
	    super(); // avoids punctuation as head in final default rule of getHead()
	    // IMPORTANT: modify some of the rules of the headRules map in CollinsHeadFinderModified
	    semanticHeadRules();
	    
	    // make a distinction between auxiliaries and copula verbs to
	    // get the NP has semantic head in sentences like "Bill is an honest man".  (Added "sha" for "shan't" May 2009
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
	  // These rules provided by Stanford parser
	  // ragerri: I am doing all this "extremely funny" work because I want to produce "semantic heads" for our 
	  // coreference system
	  
	  private void semanticHeadRules() { 
		//  NP: don't want a POS to be the head
		    headRules.put("NP", new HeadRule(new String[][]{{"rightdis", "NN", "NNP", "NNPS", "NNS", "NX", "NML", "JJR", "WP" }, {"left", "NP", "PRP"}, {"rightdis", "$", "ADJP", "FW"}, {"right", "CD"}, {"rightdis", "JJ", "JJS", "QP", "DT", "WDT", "NML", "PRN", "RB", "RBR", "ADVP"}, {"left", "POS"}}));
		    // WHNP clauses should have the same sort of head as an NP
		    // but it a WHNP has a NP and a WHNP under it, the WHNP should be the head.  E.g.,  (WHNP (WHNP (WP$ whose) (JJ chief) (JJ executive) (NN officer))(, ,) (NP (NNP James) (NNP Gatward))(, ,))
		    headRules.put("WHNP", new HeadRule(new String[][]{{"rightdis", "NN", "NNP", "NNPS", "NNS", "NX", "NML", "JJR", "WP"}, {"left", "WHNP", "NP"}, {"rightdis", "$", "ADJP", "PRN", "FW"}, {"right", "CD"}, {"rightdis", "JJ", "JJS", "RB", "QP"}, {"left", "WHPP", "WHADJP", "WP$", "WDT"}}));
		    //WHADJP
		    headRules.put("WHADJP", new HeadRule(new String[][]{{"left", "ADJP", "JJ", "JJR", "WP"}, {"right", "RB"}, {"right"}}));
		    //WHADJP
		    headRules.put("WHADVP", new HeadRule(new String[][]{{"rightdis", "WRB", "WHADVP", "RB", "JJ"}})); // if not WRB or WHADVP, probably has flat NP structure, allow JJ for "how long" constructions
		    // QP: we don't want the first CD to be the semantic head (e.g., "three billion": head should be "billion"), so we go from right to left
		    headRules.put("QP", new HeadRule(new String[][]{{"right", "$", "NNS", "NN", "CD", "JJ", "PDT", "DT", "IN", "RB", "NCD", "QP", "JJR", "JJS"}}));
		    // S, SBAR and SQ clauses should prefer the main verb as the head
		    // S: "He considered him a friend" -> we want a friend to be the head
		    headRules.put("S", new HeadRule(new String[][]{{"left", "VP", "S", "FRAG", "SBAR", "ADJP", "UCP", "TO"}, {"right", "NP"}}));
		    headRules.put("SBAR", new HeadRule(new String[][]{{"left", "S", "SQ", "SINV", "SBAR", "FRAG", "VP", "WHNP", "WHPP", "WHADVP", "WHADJP", "IN", "DT"}}));
		    // VP shouldn't be needed in SBAR, but occurs in one buggy tree in PTB3 wsj_1457 and otherwise does no harm
		    headRules.put("SQ", new HeadRule(new String[][]{{"left", "VP", "SQ", "ADJP", "VB", "VBZ", "VBD", "VBP", "MD", "AUX", "AUXG"}}));
		    // UCP take the first element as head
		    headRules.put("UCP", new HeadRule(new String[][]{{"left"}}));
		    // CONJP: we want different heads for "but also" and "but not" and we don't want "not" to be the head in "not to mention"; now make "mention" head of "not to mention"
		    headRules.put("CONJP", new HeadRule(new String[][]{{"right", "VB", "JJ", "RB", "IN", "CC"}}));
		    // FRAG: crap rule needs to be change if you want to parse glosses; but it is correct to have ADJP and ADVP before S because of weird parses of reduced sentences.
		    headRules.put("FRAG", new HeadRule(new String[][]{{"left", "IN"}, {"right", "RB"}, {"left", "NP"}, {"left", "ADJP", "ADVP", "FRAG", "S", "SBAR", "VP"}}));
		    // PRN: sentence first
		    headRules.put("PRN", new HeadRule(new String[][]{{"left", "VP", "SQ", "S", "SINV", "SBAR", "NP", "ADJP", "PP", "ADVP", "INTJ", "WHNP", "NAC", "VBP", "JJ", "NN", "NNP"}}));
		    // add the constituent XS (special node to add a layer in a QP tree introduced in our QPTreeTransformer)
		    headRules.put("XS", new HeadRule(new String[][]{{"right", "IN"}}));
		    // add a rule to deal with the CoNLL data
		    headRules.put("EMBED", new HeadRule(new String[][]{{"right", "INTJ"}}));
	  }

	  
	  private boolean shouldSkip(Parse parse, boolean origInterjection) { 
		  return isPreTerminal(parse) && punctSet.contains(parse.getType()) || 
				  !origInterjection && "UH".equals(parse.getType()) || 
				  "INTJ".equals(parse.getType()) && !origInterjection;
	  }
	  
	  private int findPreviousHead(Parse headNode, Parse[] constituents, boolean origInterjection) { 
		  boolean seenSeparator = false;
		  int newHeadIdx = headNode.getHeadIndex();
		  if (DEBUG) { 
			  System.err.println("head index is " + headNode.getHeadIndex());
		  }
		  while (newHeadIdx >= 0) {
		      newHeadIdx = newHeadIdx - 1;
		      if (newHeadIdx < 0) {
		        return newHeadIdx;
		      }
		      
		  String label = constituents[newHeadIdx].getType();
		  if (",".equals(label) || ":".equals(label)) { 
			  seenSeparator = true;
		  }
		  else if (isPreTerminal(constituents[newHeadIdx]) && punctSet.contains(label) || 
				  !origInterjection && "UH".equals(label) || 
				  "INTJ".equals(label) && ! origInterjection) { 
			  // keep going
		  }
		  else { 
			  if (!seenSeparator) { 
				  newHeadIdx = -1;
			  }
			  break;
		  }
		  }
		  return newHeadIdx;
	  }


}
