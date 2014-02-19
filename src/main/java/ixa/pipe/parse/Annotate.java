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

package ixa.pipe.parse;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;
import ixa.pipe.heads.HeadFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import opennlp.tools.parser.Parse;

/**
 * 
 * Class to provide parsing annotation in various forms: KAF, Penn style, and
 * with headWords marked. It also loads the right model for each language.
 * 
 * @author ragerri
 * 
 */
public class Annotate {

  private boolean MARKHEADS;
  private ConstituentParsing parser;
  private Models modelRetriever;
  private HeadFinder headFinder;

  /**
   * Constructor that takes into account lang options (en|es) loads the
   * corresponding parse model and decides whether to mark headWords or not.
   * 
   * @param lang
   */
  public Annotate(String lang) {
    modelRetriever = new Models();
    InputStream parseModel = modelRetriever.getParseModel(lang);
    parser = new ConstituentParsing(parseModel);
    MARKHEADS = false;
  }

  /**
   * Constructor that takes lang options (en|es) and a headFinder as parameters
   * and loads the corresponding parse model; it also states whether headWords
   * should be marked.
   * 
   * @param lang
   * @param headFinder
   */
  public Annotate(String lang, HeadFinder headFinder) {
    modelRetriever = new Models();
    InputStream parseModel = modelRetriever.getParseModel(lang);
    parser = new ConstituentParsing(parseModel);
    this.headFinder = headFinder;
    MARKHEADS = true;
  }

  /**
   * It takes an array of tokens and outputs a string with tokens joined by a
   * whitespace.
   * 
   * @param array
   *          of tokens
   * @return string representing one sentence for each array
   */
  private String getSentenceFromTokens(String[] tokens) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < tokens.length; i++) {
      sb.append(tokens[i]).append(" ");
    }
    String sentence = sb.toString();
    return sentence;
  }

  /**
   * @param kaf
   *          document containing <text> and <terms> elements
   * @return StringBuffer containing the Parse tree
   * @throws IOException
   */
  private StringBuffer getParse(KAFDocument kaf) throws IOException {
    StringBuffer parsingDoc = new StringBuffer();
    List<List<WF>> sentences = kaf.getSentences();
    for (List<WF> sentence : sentences) {
      // get array of token forms from a list of WF objects
      String[] tokens = new String[sentence.size()];
      for (int i = 0; i < sentence.size(); i++) {
        tokens[i] = sentence.get(i).getForm();
      }
      // Constituent Parsing
      String sent = getSentenceFromTokens(tokens);
      Parse parsedSentence[] = parser.parse(sent, 1);

      if (MARKHEADS) {
        for (Parse parse : parsedSentence) {
          headFinder.printHeads(parse);
        }
      }
      for (Parse parsedSent : parsedSentence) {
        parsedSent.show(parsingDoc);
        parsingDoc.append("\n");
      }
    }
    return parsingDoc;
  }

  /**
   * It takes a KAF document calls to getParse() and outputs the parse tree as
   * KAF constituents elements
   * 
   * @param KAF
   *          document containing <text> and <terms> elements
   * @return KAF <constituents> elements
   * @throws IOException
   */
  public String parseToKAF(KAFDocument kaf) throws IOException {
    StringBuffer parsingDoc = getParse(kaf);
    try {
      kaf.addConstituencyFromParentheses(parsingDoc.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return kaf.toString();
  }

  /**
   * @param KAF
   *          document containing <text> and <terms> elements
   * @return parse tree into plain text
   * @throws IOException
   */
  public String parse(KAFDocument kaf) throws IOException {
    StringBuffer parsingDoc = getParse(kaf);
    return parsingDoc.toString();
  }

  /**
   * Takes as input a list of parse strings, one for line, and annotates the
   * headwords
   * 
   * @param inputTrees
   * @return a list of parse trees with headwords annotated
   */
  private String addHeadWordsToTreebank(List<String> inputTrees) {
    StringBuffer parsedDoc = new StringBuffer();
    for (String parseSent : inputTrees) {
      Parse parsedSentence = Parse.parseParse(parseSent);
      headFinder.printHeads(parsedSentence);
      parsedSentence.show(parsedDoc);
      parsedDoc.append("\n");
    }
    return parsedDoc.toString();
  }

  /**
   * Takes a file containing Penn Treebank oneline annotation and annotates the
   * headwords, saving it to a file with the *.th extension. Optionally also
   * processes recursively an input directory adding heads only to the files
   * with the files with the specified extension
   * 
   * @param dir
   *          the input file or directory
   * @param ext
   *          the extension to look for in the directory
   * @throws IOException
   */
  public void processTreebankWithHeadWords(File dir, String ext)
      throws IOException {
    // process one file
    if (dir.isFile()) {
      List<String> inputTrees = FileUtils.readLines(
          new File(dir.getCanonicalPath()), "UTF-8");
      File outfile = new File(FilenameUtils.removeExtension(dir.getPath())
          + ".th");
      String outTree = addHeadWordsToTreebank(inputTrees);
      FileUtils.writeStringToFile(outfile, outTree, "UTF-8");
      System.err.println(">> Wrote headWords to Penn Treebank to " + outfile);
    } else {
      // recursively process directories
      File listFile[] = dir.listFiles();
      if (listFile != null) {
        if (ext == null) {
          System.out
              .println("For recursive directory processing of treebank files specify the extension of the files containing the syntactic trees.");
          System.exit(1);
        }
        for (int i = 0; i < listFile.length; i++) {
          if (listFile[i].isDirectory()) {
            processTreebankWithHeadWords(listFile[i], ext);
          } else {
            try {
              List<String> inputTrees = FileUtils.readLines(new File(
                  FilenameUtils.removeExtension(listFile[i].getCanonicalPath())
                      + ext), "UTF-8");
              File outfile = new File(FilenameUtils.removeExtension(listFile[i]
                  .getPath()) + ".th");
              String outTree = addHeadWordsToTreebank(inputTrees);
              FileUtils.writeStringToFile(outfile, outTree, "UTF-8");
              System.err.println(">> Wrote headWords to "
                  + outfile);
            } catch (FileNotFoundException noFile) {
              continue;
            }
          }
        }
      }
    }
  }
}
