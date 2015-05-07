/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eus.ixa.ixa.pipe.heads;

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

import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Constituent;
import opennlp.tools.parser.GapLabeler;
import opennlp.tools.parser.Parse;

/**
* Class for storing the Spanish head rules associated with parsing. The
* headrules are specified in $src/main/resources/es-head-rules
* 
* NOTE: This is the very same class than the one inside
* opennlp.tools.parser.lang.es package. The main change is the return of the getHead()
* method: Every return constituents[i].getHead() has been replaced by the
* same return statement without the .getHead() method call.
* 
* Other changes include removal of deprecated methods we do not need to use and
* adding some other methods for debugging.
* @author ragerri
* @version 2015-05-06
*/
public class SpanishHeadRules implements opennlp.tools.parser.HeadRules,
    GapLabeler {

  private static class HeadRule {
    public boolean leftToRight;
    public String[] tags;

    public HeadRule(final boolean l2r, final String[] tags) {
      this.leftToRight = l2r;

      for (final String tag : tags) {
        if (tag == null) {
          throw new IllegalArgumentException(
              "tags must not contain null values!");
        }
      }

      this.tags = tags;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof HeadRule) {
        final HeadRule rule = (HeadRule) obj;

        return rule.leftToRight == this.leftToRight
            && Arrays.equals(rule.tags, this.tags);
      } else {
        return false;
      }
    }
  }

  private Map<String, HeadRule> headRules;
  private final Set<String> punctSet;

  /**
   * Creates a new set of head rules based on the specified reader.
   * 
   * @param rulesReader
   *          the head rules reader.
   * 
   * @throws IOException
   *           if the head rules reader can not be read.
   */
  public SpanishHeadRules(final Reader rulesReader) throws IOException {
    final BufferedReader in = new BufferedReader(rulesReader);
    readHeadRules(in);

    this.punctSet = new HashSet<String>();
    this.punctSet.add(".");
    this.punctSet.add(",");
    this.punctSet.add("``");
    this.punctSet.add("''");
    // punctSet.add(":");
  }

  public Set<String> getPunctuationTags() {
    return this.punctSet;
  }

  public Parse getHead(final Parse[] constituents, final String type) {
    if (constituents[0].getType() == AbstractBottomUpParser.TOK_NODE) {
      return null;
    }
    HeadRule hr;
    // if (type.equals("SN") || type.equals("GRUP.NOM")) {
    if (type.startsWith("SN") || type.startsWith("GRUP.NOM")) {
      final String[] tags1 = { "NCMS000", "NCFS000", "NCCS000", "NCMS00D",
          "NCMS00A", "NCFS00D", "NCFS00A", "NCCS00A", "NCCS00D", "NP0000",
          "NCMP000", "NCFP000", "NCCP000", "NCMP00D", "NCMP00A", "NCFP00D",
          "NCFP00A", "NCCP00A", "NCCP00D", "GRUP.NOM", "AQAMS0", "AQAFS0",
          "AQACS0", "AQAMN0", "AQAFN0", "AQACN0", "AQAMP0", "AQAFP0", "AQACP0",
          "AQCMS0", "AQCFS0", "AQCCS0", "AQCMN0", "AQCFN0", "AQCCN0", "AQCMP0",
          "AQCFP0", "AQCCP0" };

      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        for (int ti = tags1.length - 1; ti >= 0; ti--) {
          if (constituents[ci].getType().equals(tags1[ti])) {
            return constituents[ci];
          }
        }
      }
      for (final Parse constituent : constituents) {
        // if (constituents[ci].getType().equals("SN")) {
        if (constituent.getType().startsWith("SN")
            || constituent.getType().startsWith("GRUP.NOM")) {
          return constituent;
        }
      }
      final String[] tags2 = { "$", "SA", "S.A", "GRUP.A" };
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        for (int ti = tags2.length - 1; ti >= 0; ti--) {
          if (constituents[ci].getType().equals(tags2[ti])) {
            return constituents[ci];
          }
        }
      }
      final String[] tags4 = { "COORD", "CONJ", "CS" };
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        for (int ti = tags2.length - 1; ti >= 0; ti--) {
          if (constituents[ci].getType().equals(tags4[ti])) {
            return constituents[ci];
          }
        }
      }
      final String[] tags3 = { "AQ0MS0", "AQ0FS0", "AQ0CS0", "AQ0MSP",
          "AQ0FSP", "AQ0CSP", "AQ0CNP", "AQ0MP0", "AQ0FP0", "AQ0CP0", "AQ0MPP",
          "AQ0FPP", "AQ0CPP", "AQ0MN0", "AQ0FN0", "AQ0CN0", "AQ0MNP", "AQ0FNP",
          "AQ0CNP", "AQSMS0", "AQSFS0", "AQSCS0", "AQSMN0", "AQSFN0", "AQSCN0",
          "AQSMP0", "AQSFP0", "AQSCP0", "RG", "RN", "GRUP.NOM" };
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        for (int ti = tags3.length - 1; ti >= 0; ti--) {
          if (constituents[ci].getType().equals(tags3[ti])) {
            return constituents[ci];
          }
        }
      }
      return constituents[constituents.length - 1];
    } else if ((hr = this.headRules.get(type)) != null) {
      final String[] tags = hr.tags;
      final int cl = constituents.length;
      final int tl = tags.length;
      if (hr.leftToRight) {
        for (int ti = 0; ti < tl; ti++) {
          for (int ci = 0; ci < cl; ci++) {
            // TODO: Examine this function closely are we infra-heading or
            // over-heading?
            if (constituents[ci].getType().equals(tags[ti])
                || constituents[ci].getType().startsWith(tags[ti])) {
              // if (constituents[ci].getType().equals(tags[ti])) {
              return constituents[ci];
            }
          }
        }
        return constituents[0];
      } else {
        for (int ti = 0; ti < tl; ti++) {
          for (int ci = cl - 1; ci >= 0; ci--) {
            if (constituents[ci].getType().equals(tags[ti])) {
              return constituents[ci];
            }
          }
        }
        return constituents[cl - 1];
      }
    }
    return constituents[constituents.length - 1];
  }

  private void readHeadRules(final BufferedReader str) throws IOException {
    String line;
    this.headRules = new HashMap<String, HeadRule>(60);
    while ((line = str.readLine()) != null) {
      final StringTokenizer st = new StringTokenizer(line);
      final String num = st.nextToken();
      final String type = st.nextToken();
      final String dir = st.nextToken();
      final String[] tags = new String[Integer.parseInt(num) - 2];
      int ti = 0;
      while (st.hasMoreTokens()) {
        tags[ti] = st.nextToken();
        ti++;
      }
      this.headRules.put(type, new HeadRule(dir.equals("1"), tags));
    }
  }

  public void labelGaps(final Stack<Constituent> stack) {
    if (stack.size() > 4) {
      // Constituent con0 = (Constituent) stack.get(stack.size()-1);
      final Constituent con1 = stack.get(stack.size() - 2);
      final Constituent con2 = stack.get(stack.size() - 3);
      final Constituent con3 = stack.get(stack.size() - 4);
      final Constituent con4 = stack.get(stack.size() - 5);
      // System.err.println("con0="+con0.label+" con1="+con1.label+" con2="+con2.label+" con3="+con3.label+" con4="+con4.label);
      // subject extraction
      if (con1.getLabel().equals("SN") && con2.getLabel().equals("S")
          && con3.getLabel().equals("GRUP.NOM")) {
        con1.setLabel(con1.getLabel() + "-G");
        con2.setLabel(con2.getLabel() + "-G");
        con3.setLabel(con3.getLabel() + "-G");
      }
      // object extraction
      else if (con1.getLabel().equals("SN")
          && con2.getLabel().equals("GRUP.VERB") && con3.getLabel().equals("S")
          && con4.getLabel().equals("GRUP.NOM")) {
        con1.setLabel(con1.getLabel() + "-G");
        con2.setLabel(con2.getLabel() + "-G");
        con3.setLabel(con3.getLabel() + "-G");
        con4.setLabel(con4.getLabel() + "-G");
      }
    }
  }

  /**
   * Writes the head rules to the writer in a format suitable for loading the
   * head rules again with the constructor. The encoding must be taken into
   * account while working with the writer and reader.
   * <p>
   * After the entries have been written, the writer is flushed. The writer
   * remains open after this method returns.
   * 
   * @param writer
   *          the writer
   * @throws IOException
   *           if io exception
   */
  public void serialize(final Writer writer) throws IOException {

    for (final String type : this.headRules.keySet()) {

      final HeadRule headRule = this.headRules.get(type);
      // write num of tags
      writer.write(Integer.toString(headRule.tags.length + 2));
      writer.write(' ');
      // write type
      writer.write(type);
      writer.write(' ');
      // write l2r true == 1
      if (headRule.leftToRight) {
        writer.write("1");
      } else {
        writer.write("0");
      }
      // write tags
      for (final String tag : headRule.tags) {
        writer.write(' ');
        writer.write(tag);
      }
      writer.write('\n');
    }
    writer.flush();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof SpanishHeadRules) {
      final SpanishHeadRules rules = (SpanishHeadRules) obj;

      return rules.headRules.equals(this.headRules)
          && rules.punctSet.equals(this.punctSet);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    assert false : "hashCode not designed";
    return 42; // any arbitrary constant will do
  }
}
