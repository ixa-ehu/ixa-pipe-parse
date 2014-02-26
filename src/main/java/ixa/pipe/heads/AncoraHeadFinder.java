package ixa.pipe.heads;

import java.util.HashMap;

import opennlp.tools.parser.Parse;

public class AncoraHeadFinder extends AbstractHeadFinder {
  public AncoraHeadFinder(String... categoriesToAvoid) {
    super(categoriesToAvoid);
    
    headRules = new HashMap<String, String[][]>();
    headRules.put("NP", new String[][]{{"rightdis", "NN", "NNP", "NNPS", "NNS", "NX", "POS", "JJR"}, {"left", "NP"}, {"rightdis", "$", "ADJP", "PRN"}, {"right", "CD"}, {"rightdis", "JJ", "JJS", "RB", "QP"}});
    headRules.put("ADJP", new String[][]{{"left", "NNS", "QP", "NN", "$", "ADVP", "JJ", "VBN", "VBG", "ADJP", "JJR", "NP", "JJS", "DT", "FW", "RBR", "RBS", "SBAR", "RB"}} );
    headRules.put("ADVP", new String[][]{{"right", "RB", "RBR", "RBS", "FW", "ADVP", "TO", "CD", "JJR", "JJ", "IN", "NP", "JJS", "NN"}});
    headRules.put("CONJP", new String[][]{{"right", "CC", "RB", "IN"}});
    headRules.put("FRAG", new String[][]{{"right"}});
    headRules.put("INTJ", new String[][]{{"left"}});
    headRules.put("LST", new String[][]{{"right", "LS", ":"}});
    headRules.put("NAC", new String[][]{{"left", "NN", "NNS", "NNP", "NNPS", "NP", "NAC", "EX", "$", "CD", "QP", "PRP", "VBG", "JJ", "JJS", "JJR", "ADJP", "FW"}});
    headRules.put("NX", new String[][]{{"left"}}); 
    headRules.put("PP", new String[][]{{"right", "IN", "TO", "VBG", "VBN", "RP", "FW"}});
    headRules.put("PRN", new String[][]{{"left"}});
    headRules.put("PRT", new String[][]{{"right", "RP"}});
    headRules.put("QP", new String[][]{{"left", "$", "IN", "NNS", "NN", "JJ", "RB", "DT", "CD", "NCD", "QP", "JJR", "JJS"}});
    headRules.put("RRC", new String[][]{{"right", "VP", "NP", "ADVP", "ADJP", "PP"}});
    headRules.put("S", new String[][]{{"left", "TO", "IN", "VP", "S", "SBAR", "ADJP", "UCP", "NP"}});
    headRules.put("SBAR", new String[][]{{"left", "WHNP", "WHPP", "WHADVP", "WHADJP", "IN", "DT", "S", "SQ", "SINV", "SBAR", "FRAG"}});
    headRules.put("SBARQ", new String[][]{{"left", "SQ", "S", "SINV", "SBARQ", "FRAG"}});
    headRules.put("SINV", new String[][]{{"left", "VBZ", "VBD", "VBP", "VB", "MD", "VP", "S", "SINV", "ADJP", "NP"}});
    headRules.put("SQ", new String[][]{{"left", "VBZ", "VBD", "VBP", "VB", "MD", "VP", "SQ"}});
    headRules.put("UCP", new String[][]{{"right"}});
    headRules.put("VP", new String[][]{{"left", "TO", "VBD", "VBN", "MD", "VBZ", "VB", "VBG", "VBP", "AUX", "AUXG", "VP", "ADJP", "NN", "NNS", "NP"}});
    headRules.put("WHADJP", new String[][]{{"left", "CC", "WRB", "JJ", "ADJP"}});
    headRules.put("WHADVP", new String[][]{{"right", "CC", "WRB"}});
    headRules.put("WHNP", new String[][]{{"left", "WDT", "WP", "WP$", "WHADJP", "WHPP", "WHNP"}});
    headRules.put("WHPP", new String[][]{{"right", "IN", "TO", "FW"}});
    headRules.put("X", new String[][]{{"right"}});
    // these last three added by Stanford parser
    headRules.put("TYPO", new String[][] {{"left"}});
    headRules.put("EDITED", new String[][] {{"left"}});
    headRules.put("XS", new String[][] {{"right", "IN"}});
  }
    
  /* (non-Javadoc)
   * @see ixa.pipe.heads.AbstractHeadFinder#postOperationFix(int, opennlp.tools.parser.Parse[])
   * 
   * This fixes the headWord found. If its previous node is a conjunction, it keeps going up 
   * the tree looking for a head until a node is found that is not punctuation or a leaf. 
   * 
   */
  @Override
  protected int correctFoundHeads(int headIndex, Parse[] children) {
    if (headIndex >= 2) {
      String prevLab = children[headIndex - 1].getType();
      if (prevLab.equals("CC") || prevLab.equals("CONJP")) {
        int newHeadIndex = headIndex - 2;
        Parse t = children[newHeadIndex];
        while (newHeadIndex >= 0 && (t.getChildCount() ==  1 && t.getChildren()[0].getChildCount() == 0) &&
            punctSet.contains(t.getType())) {
          newHeadIndex--;
        }
        if (newHeadIndex >= 0) {
          headIndex = newHeadIndex;
        }
      }
    }
    return headIndex;
    
  }


}
