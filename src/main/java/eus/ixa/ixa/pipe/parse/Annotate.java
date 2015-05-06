/*
 *Copyright 2015 Rodrigo Agerri

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
package eus.ixa.ixa.pipe.parse;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import opennlp.tools.parser.Parse;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import eus.ixa.ixa.pipe.heads.CollinsHeadFinder;
import eus.ixa.ixa.pipe.heads.HeadFinder;

/**
 * 
 * Class to provide parsing annotation in various forms: KAF, Penn style, and
 * with or without headWords marked.
 * 
 * @author ragerri
 * @version 2014-02-03
 * 
 */
public class Annotate {

  private boolean markHeads;
  private final ConstituentParsing parser;
  private HeadFinder headFinder;

  /**
   * Constructor that takes into account lang options (en|es) loads the
   * corresponding parse model and decides whether to mark headWords or not.
   * 
   * @param properties
   *          the configuration properties
   */
  public Annotate(final Properties properties) {
    this.parser = new ConstituentParsing(properties);
    annotateOptions(properties);
  }

  private void annotateOptions(final Properties properties) {

    if (!properties.getProperty("headFinder").equalsIgnoreCase(
        Flags.DEFAULT_HEADFINDER)) {
      this.markHeads = true;
      loadHeadFinder(properties);
    } else {
      this.markHeads = false;
    }
  }

  private void loadHeadFinder(final Properties properties) {
    final String headFinderOption = properties.getProperty("headFinder");
    if (headFinderOption.equalsIgnoreCase("collins")) {
      this.headFinder = new CollinsHeadFinder(properties);
    } else if (headFinderOption.equalsIgnoreCase("sem")) {
      this.headFinder = new CollinsHeadFinder(properties);
    } else {
      System.err.println("HeadFinder option not recognized!");
      System.exit(1);
    }
  }

  /**
   * It takes an array of tokens and outputs a string with tokens joined by a
   * whitespace.
   * 
   * @param array
   *          of tokens
   * @return string representing one sentence for each array
   */
  private String getSentenceFromTokens(final String[] tokens) {
    final StringBuilder sb = new StringBuilder();
    for (final String token : tokens) {
      sb.append(token).append(" ");
    }
    final String sentence = sb.toString();
    return sentence;
  }

  /**
   * @param kaf
   *          document containing WF and Term elements
   * @return StringBuffer containing the Parse tree
   * @throws IOException
   */
  private StringBuffer getParse(final KAFDocument kaf) throws IOException {
    final StringBuffer parsingDoc = new StringBuffer();
    final List<List<WF>> sentences = kaf.getSentences();
    for (final List<WF> sentence : sentences) {
      // get array of token forms from a list of WF objects
      final String[] tokens = new String[sentence.size()];
      for (int i = 0; i < sentence.size(); i++) {
        tokens[i] = sentence.get(i).getForm();
      }
      // Constituent Parsing
      final String sent = getSentenceFromTokens(tokens);
      final Parse[] parsedSentence = this.parser.parse(sent, 1);
      if (this.markHeads) {
        for (final Parse parse : parsedSentence) {
          this.headFinder.printHeads(parse);
        }
      }
      for (final Parse parsedSent : parsedSentence) {
        parsedSent.show(parsingDoc);
        parsingDoc.append("\n");
      }
    }
    return parsingDoc;
  }

  /**
   * It takes a KAF document calls to getParse() and outputs the parse tree as
   * KAF constituents elements.
   * 
   * @param kaf
   *          document containing WF and Term elements
   * @throws IOException
   *           if io error
   */
  public void parseToKAF(final KAFDocument kaf) throws IOException {
    final StringBuffer parsingDoc = getParse(kaf);
    try {
      kaf.addConstituencyFromParentheses(parsingDoc.toString());
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @param kaf
   *          document containing WF and Term elements
   * @return parse tree into plain text
   * @throws IOException
   *           if io error
   */
  public String parseToOneline(final KAFDocument kaf) throws IOException {
    final StringBuffer parsingDoc = getParse(kaf);
    return parsingDoc.toString();
  }

  public void parseForTesting(final File inputText) throws IOException {
    final StringBuffer parsingDoc = new StringBuffer();
    if (inputText.isFile()) {
      final List<String> inputTrees = Files
          .readLines(inputText, Charsets.UTF_8);
      for (final String sentence : inputTrees) {
        final Parse parsedSentence = this.parser.parse(sentence, 1)[0];
        parsedSentence.show(parsingDoc);
        parsingDoc.append("\n");
      }
      final File outfile = new File(Files.getNameWithoutExtension(inputText
          .getPath()) + ".test");
      System.err.println("Writing test parse file to " + outfile);
      Files.write(parsingDoc.toString(), outfile, Charsets.UTF_8);
    } else {
      System.out.println("Choose a correct file!");
      System.exit(1);
    }
  }

  /**
   * Takes a file containing Penn Treebank oneline annotation and annotates the
   * headwords, saving it to a file with the *.head extension. Optionally also
   * processes recursively an input directory adding heads only to the files
   * with the files with the specified extension.
   * 
   * @param dir
   *          the input file or directory
   * @throws IOException
   *           if io error
   */
  public void processTreebankWithHeadWords(final File dir) throws IOException {
    // process one file
    if (dir.isFile()) {
      final List<String> inputTrees = Files.readLines(
          new File(dir.getCanonicalPath()), Charsets.UTF_8);
      final File outfile = new File(
          Files.getNameWithoutExtension(dir.getPath()) + ".head");
      final String outTree = addHeadWordsToTreebank(inputTrees);
      Files.write(outTree, outfile, Charsets.UTF_8);
      System.err.println(">> Wrote headWords to " + outfile);
    } else {
      // recursively process directories
      final File listFile[] = dir.listFiles();
      if (listFile != null) {
        for (final File element : listFile) {
          if (element.isDirectory()) {
            processTreebankWithHeadWords(element);
          } else {
            try {
              final List<String> inputTrees = Files.readLines(
                  new File(Files.getNameWithoutExtension(element
                      .getCanonicalPath())), Charsets.UTF_8);
              final File outfile = new File(
                  Files.getNameWithoutExtension(element.getPath()) + ".head");
              final String outTree = addHeadWordsToTreebank(inputTrees);
              Files.write(outTree, outfile, Charsets.UTF_8);
              System.err.println(">> Wrote headWords to " + outfile);
            } catch (final FileNotFoundException noFile) {
              continue;
            }
          }
        }
      }
    }
  }

  /**
   * Takes as input a list of parse strings, one for line, and annotates the
   * headwords
   * 
   * @param inputTrees
   * @return a list of parse trees with headwords annotated
   */
  private String addHeadWordsToTreebank(final List<String> inputTrees) {
    final StringBuffer parsedDoc = new StringBuffer();
    for (final String parseSent : inputTrees) {
      final Parse parsedSentence = Parse.parseParse(parseSent);
      this.headFinder.printHeads(parsedSentence);
      parsedSentence.show(parsedDoc);
      parsedDoc.append("\n");
    }
    return parsedDoc.toString();
  }
}
