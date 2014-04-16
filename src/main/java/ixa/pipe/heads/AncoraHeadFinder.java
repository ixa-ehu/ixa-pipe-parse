package ixa.pipe.heads;

import java.util.HashMap;

import opennlp.tools.parser.Parse;

/**
 * Defines head finding rules for the Ancora corpus. 
 * 
 * @author ragerri
 * @version 2014-03-09
 */

public class AncoraHeadFinder extends AbstractHeadFinder {
  public AncoraHeadFinder(String... categoriesToAvoid) {
    super(categoriesToAvoid);
    
    headRules = new HashMap<String, String[][]>();
    headRules.put("SN", new String[][]{{"rightdis","AQA.*","AQC.*","GRUP\\.A","S\\.A","NC.*S.*", "NP.*","NC.*P.*", "GRUP\\.NOM"}, {"left", "SN","GRUP\\.NOM"}, {"rightdis", "\\$","GRUP\\.A","S\\.A","SA"}, {"right", "Z.*"}, {"rightdis", "AQ0.*","AQ[AC].*","AO.*","GRUP\\.A","S\\.A","RG","RN","GRUP\\.NOM"}});
    headRules.put("GRUP.NOM", new String[][]{{"rightdis","AQA.*","AQC.*","GRUP\\.A","S\\.A","NC.*S.*", "NP.*","NC.*P.*", "GRUP\\.NOM"}, {"left", "SN","GRUP\\.NOM"}, {"rightdis", "\\$","GRUP\\.A","S\\.A","SA"}, {"right", "Z.*"}, {"rightdis", "AQ0.*","AQ[AC].*","AO.*","GRUP\\.A","S\\.A","RG","RN","GRUP\\.NOM"}});
    headRules.put("SENTENCE", new String[][] {{"left","PREP","SP[CS].*","CS.*","GRUP\\.VERB","S","SA","COORD","CONJ","GRUP\\.NOM","SN","S"}});
    headRules.put("S", new String[][]{{"left","PREP","SP[CS].*","COORD","CONJ","CS.*","GRUP\\.VERB","S","SA","COORD","GRUP\\.NOM","SN"}});
    headRules.put("SA", new String[][]{{"left", "NC.*P.*", "GRUP\\.NOM","\\$","NC.*S.*","SADV","GRUP\\.ADV","AQA.*", "AQC.*","V[MAS]P.*", "V[MAS]G.*", "SA","S\\.A","GRUP\\.A","AQS.*", "SN", "GRUP\\.NOM", "D.*", "S", "RG", "RN"}} );
    headRules.put("S.A", new String[][]{{"left", "NC.*P.*", "GRUP\\.NOM","\\$","NC.*S.*","SADV","GRUP\\.ADV","AQA.*", "AQC.*","V[MAS]P.*", "V[MAS]G.*", "S\\.A","GRUP\\.A","AQS.*", "SN", "GRUP\\.NOM", "D.*", "S", "RG", "RN"}} );
    headRules.put("SADV", new String[][]{{"right","S","RG", "RN", "SADV", "GRUP\\.ADV", "SP[CS].*","PREP", "Z.*", "AQA.*", "AQC.*","S\\.A","GRUP\\.A","CONJ","CS.*", "SN", "GRUP\\.NOM", "AQS.*", "NC.*S.*"}});
    headRules.put("SP", new String[][]{{"right", "SP[CS].*", "PREP", "CS.*", "CONJ", "V[MAS]G.*", "V[MAS]P.*"}});
    headRules.put("GRUP.A",new String[][]{{"left","NC.*P.*", "GRUP\\.NOM","\\$","NC.*S.*","SADV","GRUP\\.ADV","AQA.*", "AQC.*","V[MAS]P.*", "V[MAS]G.*", "GRUP\\.A","AQS.*", "SN", "GRUP\\.NOM", "D.*", "S", "RG", "RN"}} );
    headRules.put("GRUP.ADV",new String[][]{{"right", "RG", "RN", "GRUP\\.ADV", "PREP", "SP.*", "Z.*", "AQA.*", "AQC.*", "GRUP\\.A","S\\.A","CS.*", "CONJ","SN", "GRUP\\.NOM", "AQS.*", "NC.*S.*"}});
    headRules.put("GRUP.VERB", new String[][]{{"left","INFINITIU","GERUNDI","PARTICIPI","PREP","SP[CS].*", "V[MAS].*[IS].*", "V[MAS]P.*", "V.*C.*", "V[MAS]IP3S.*", "V.*", "V[MAS]G.*", "V[MAS]IP[12]S.*","GRUP\\.VERB", "SA","S\\.A","GRUP\\.A", "NC.*S.*", "NC.*P.*", "GRUP\\.NOM","SN","S"}});
    headRules.put("INFINITIU", new String[][]{{"left", "VMN.*","V[MAS]N.*","V.*"}});
    headRules.put("GERUNDI", new String[][]{{"left", "VMG.*","V[MAS]G.*","V.*"}});
    headRules.put("PARTICIPI", new String[][]{{"left", "VMP.*","V[MAS]P.*","V.*"}});
    headRules.put("MORFEMA.PRONOMINAL", new String[][]{{"left", "P.*","SN.*","GRUP\\.NOM.*","GRUP\\.VERB"}});
    headRules.put("MORFEMA.VERBAL", new String[][]{{"left", "GRUP\\.VERB","P.*","SN.*","GRUP\\.NOM.*","S"}});
    headRules.put("COORD", new String[][]{{"right"}});
    headRules.put("CONJ", new String[][]{{"right", "CONJ","CC.*", "RB", "RN","SP[CS].*","PREP","CS"}});
    headRules.put("INC",new String[][]{{"left","S","SN","GRUP\\.NOM","GRUP\\.VERB","SADV","GRUP.ADV","SA","S\\.A","GRUP\\.A","PREP","SP[CS].*","CONJ","CS","D.*"}});
    headRules.put("INTERJECCIO", new String[][]{{"left","I"}});
    headRules.put("NEG", new String[][]{{"left","RN"}}); 
    headRules.put("PREP", new String[][]{{"left","PREP","SP[CS].*","CONJ","CS"}});
    headRules.put("RELATIU", new String[][]{{"left", "P.*","SN","GRUP\\.NOM","S", "GRUP\\.VERB"}}); 
    headRules.put("SPEC", new String[][]{{"left"}});
    headRules.put("X", new String[][]{{"right"}});
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
      if (prevLab.equals("CC") || prevLab.equals("COORD") || prevLab.equals("CONJ")) {
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
