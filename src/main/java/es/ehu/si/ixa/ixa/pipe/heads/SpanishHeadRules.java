/*
 *
 *Copyright 2013 Rodrigo Agerri and Apache Software Foundation (ASF)

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


package es.ehu.si.ixa.ixa.pipe.heads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import opennlp.tools.parser.Constituent;
import opennlp.tools.parser.GapLabeler;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.chunking.Parser;

/**
 * Class for storing the Spanish head rules associated with parsing. The headrules
 * are specified in $src/main/resources/es-head-rules
 *  
 * NOTE: This class has been adapted from opennlp.tools.parser.lang.en. 
 * 
 * Note also the change in the return of the getHead() method: In Apache OpenNLP
 * class: return constituents[ci].getHead(); Now: return constituents[ci];
 * 
 * Other changes include removal of deprecated methods we do not need to use. 
 * 
 */

public class SpanishHeadRules implements opennlp.tools.parser.HeadRules, GapLabeler {

  private static class HeadRule {
    public boolean leftToRight;
    public String[] tags;
    public HeadRule(boolean l2r, String[] tags) {
      leftToRight = l2r;

      for (String tag : tags) {
        if (tag == null)
            throw new IllegalArgumentException("tags must not contain null values!");
      }

      this.tags = tags;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      else if (obj instanceof HeadRule) {
        HeadRule rule = (HeadRule) obj;
        
        return (rule.leftToRight == leftToRight) && 
            Arrays.equals(rule.tags, tags);
      }
      else {
        return false;
      }
    }
  }

  private Map<String, HeadRule> headRules;
  private Set<String> punctSet;

  

  /**
   * Creates a new set of head rules based on the specified reader.
   *
   * @param rulesReader the head rules reader.
   *
   * @throws IOException if the head rules reader can not be read.
   */
  public SpanishHeadRules(Reader rulesReader) throws IOException {
    BufferedReader in = new BufferedReader(rulesReader);
    readHeadRules(in);

    punctSet = new HashSet<String>();
    punctSet.add(".");
    punctSet.add(",");
    punctSet.add("``");
    punctSet.add("''");
    //punctSet.add(":");
  }

  public Set<String> getPunctuationTags() {
    return punctSet;
  }

  public Parse getHead(Parse[] constituents, String type) {
    if (constituents[0].getType() == Parser.TOK_NODE) {
      return null;
    }
    HeadRule hr;
    //if (type.equals("SN") || type.equals("GRUP.NOM")) {
    if (type.startsWith("SN") || type.startsWith("GRUP.NOM")) {
      String[] tags1 = { "NCMS000", "NCFS000", "NCCS000", "NCMS00D", "NCMS00A", "NCFS00D","NCFS00A", "NCCS00A", "NCCS00D", 
    		  "NP0000", "NCMP000", "NCFP000", "NCCP000", "NCMP00D", "NCMP00A", "NCFP00D","NCFP00A", "NCCP00A", "NCCP00D", 
    		  "GRUP.NOM", "AQAMS0","AQAFS0","AQACS0", "AQAMN0", "AQAFN0", "AQACN0", "AQAMP0","AQAFP0","AQACP0",
    		  "AQCMS0","AQCFS0","AQCCS0", "AQCMN0", "AQCFN0", "AQCCN0", "AQCMP0","AQCFP0","AQCCP0" };
      
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        for (int ti = tags1.length - 1; ti >= 0; ti--) {
          if (constituents[ci].getType().equals(tags1[ti])) {
            return constituents[ci];
          }
        }
      }
      for (int ci = 0; ci < constituents.length; ci++) {
        //if (constituents[ci].getType().equals("SN")) {
          if (constituents[ci].getType().startsWith("SN") || constituents[ci].getType().startsWith("GRUP.NOM")) {        
            return constituents[ci];
        }
      }
      String[] tags2 = { "$", "SA","S.A","GRUP.A" };
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        for (int ti = tags2.length - 1; ti >= 0; ti--) {
          if (constituents[ci].getType().equals(tags2[ti])) {
            return constituents[ci];
          }
        }
      }
      String[] tags3 = { "AQ0MS0","AQ0FS0","AQ0CS0","AQ0MSP","AQ0FSP","AQ0CSP","AQ0CNP",
    		  "AQ0MP0","AQ0FP0","AQ0CP0","AQ0MPP","AQ0FPP","AQ0CPP",
    		  "AQ0MN0","AQ0FN0","AQ0CN0","AQ0MNP","AQ0FNP","AQ0CNP",
    		  "AQSMS0","AQSFS0","AQSCS0", "AQSMN0", "AQSFN0", "AQSCN0", "AQSMP0","AQSFP0","AQSCP0", 
    		   "RG","RN", "GRUP.NOM" };
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        for (int ti = tags3.length - 1; ti >= 0; ti--) {
          if (constituents[ci].getType().equals(tags3[ti])) {
            return constituents[ci];
          }
        }
      }
      return constituents[constituents.length - 1].getHead();
    }
    else if ((hr = headRules.get(type)) != null) {
      String[] tags = hr.tags;
      int cl = constituents.length;
      int tl = tags.length;
      if (hr.leftToRight) {
        for (int ti = 0; ti < tl; ti++) {
          for (int ci = 0; ci < cl; ci++) {
        	  // TODO: Examine this function closely are we infra-heading or over-heading?
            if (constituents[ci].getType().equals(tags[ti]) || constituents[ci].getType().startsWith(tags[ti])) {
        	// if (constituents[ci].getType().equals(tags[ti])) {
              return constituents[ci];
            }
          }
        }
        return constituents[0].getHead();
      }
      else {
        for (int ti = 0; ti < tl; ti++) {
          for (int ci = cl - 1; ci >= 0; ci--) {
            if (constituents[ci].getType().equals(tags[ti])) {
              return constituents[ci];
            }
          }
        }
        return constituents[cl - 1].getHead();
      }
    }
    return constituents[constituents.length - 1].getHead();
  }

  private void readHeadRules(BufferedReader str) throws IOException {
    String line;
    headRules = new HashMap<String, HeadRule>(60);
    while ((line = str.readLine()) != null) {
      StringTokenizer st = new StringTokenizer(line);
      String num = st.nextToken();
      String type = st.nextToken();
      String dir = st.nextToken();
      String[] tags = new String[Integer.parseInt(num) - 2];
      int ti = 0;
      while (st.hasMoreTokens()) {
        tags[ti] = st.nextToken();
        ti++;
      }
      headRules.put(type, new HeadRule(dir.equals("1"), tags));
    }
  }

  public void labelGaps(Stack<Constituent> stack) {
    if (stack.size() > 4) {
      //Constituent con0 = (Constituent) stack.get(stack.size()-1);
      Constituent con1 = stack.get(stack.size()-2);
      Constituent con2 = stack.get(stack.size()-3);
      Constituent con3 = stack.get(stack.size()-4);
      Constituent con4 = stack.get(stack.size()-5);
      //System.err.println("con0="+con0.label+" con1="+con1.label+" con2="+con2.label+" con3="+con3.label+" con4="+con4.label);
      //subject extraction
      if (con1.getLabel().equals("SN") && con2.getLabel().equals("S") && con3.getLabel().equals("GRUP.NOM")) {
        con1.setLabel(con1.getLabel()+"-G");
        con2.setLabel(con2.getLabel()+"-G");
        con3.setLabel(con3.getLabel()+"-G");
      }
      //object extraction
      else if (con1.getLabel().equals("SN") && con2.getLabel().equals("GRUP.VERB") && con3.getLabel().equals("S") && con4.getLabel().equals("GRUP.NOM")) {
        con1.setLabel(con1.getLabel()+"-G");
        con2.setLabel(con2.getLabel()+"-G");
        con3.setLabel(con3.getLabel()+"-G");
        con4.setLabel(con4.getLabel()+"-G");
      }
    }
  }

  /**
   * Writes the head rules to the writer in a format suitable for loading
   * the head rules again with the constructor. The encoding must be
   * taken into account while working with the writer and reader.
   * <p> 
   * After the entries have been written, the writer is flushed.
   * The writer remains open after this method returns.
   * 
   * @param writer
   * @throws IOException
   */
  public void serialize(Writer writer) throws IOException {

    for (String type : headRules.keySet()) {

      HeadRule headRule = headRules.get(type);

      // write num of tags
      writer.write(Integer.toString(headRule.tags.length + 2));
      writer.write(' ');

      // write type
      writer.write(type);
      writer.write(' ');

      // write l2r true == 1
      if (headRule.leftToRight)
        writer.write("1");
      else
        writer.write("0");

      // write tags
      for (String tag : headRule.tags) {
        writer.write(' ');
        writer.write(tag);
      }

      writer.write('\n');
    }
    
    writer.flush();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    else if (obj instanceof SpanishHeadRules) {
      SpanishHeadRules rules = (SpanishHeadRules) obj;
      
      return rules.headRules.equals(headRules) &&
          rules.punctSet.equals(punctSet);
    }
    else {
      return false;
    }
  }
}

