/*
 *Copyright 2013 Rodrigo Agerri

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
import ixa.pipe.heads.CollinsHeadFinder;
import ixa.pipe.heads.HeadFinder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.jdom2.JDOMException;

/**
 * EHU-OpenNLP Constituent Parsing using Apache OpenNLP.
 * 
 * @author ragerri
 * @version 1.0
 * 
 */

public class CLI {

  /**
   * 
   * 
   * BufferedReader (from standard input) and BufferedWriter are opened. The
   * module takes KAF and reads the header, and the text elements and uses
   * Annotate class to provide constituent parsing of sentences, which are
   * provided via standard output.
   * 
   * @param args
   * @throws IOException
   * @throws JDOMException
   */

  public static void main(String[] args) throws IOException, JDOMException {

    Namespace parsedArguments = null;

    // create Argument Parser
    ArgumentParser parser = ArgumentParsers.newArgumentParser(
        "ixa-pipe-parse-1.0.jar").description(
        "ixa-pipe-parse-1.0 is a multilingual Constituent Parsing module "
            + "developed by IXA NLP Group based on Apache OpenNLP API.\n");

    // specify language
    parser
        .addArgument("-l", "--lang")
        .choices("en","es")
        .required(true)
        .help(
            "It is REQUIRED to choose a language to perform annotation with ixa-pipe-parse");

    parser
        .addArgument("-g", "--heads")
        .choices("synt", "sem")
        .required(false)
        .help(
            "It is REQUIRED to choose a language to perform annotation with ixa-pipe-parse");

    /*
     * Parse the command line arguments
     */

    // catch errors and print help
    try {
      parsedArguments = parser.parseArgs(args);
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      System.out
          .println("Run java -jar target/ixa-pipe-parse-1.0.jar -help for details");
      System.exit(1);
    }

    /*
     * Load language and headFinder parameters
     */

    String lang = parsedArguments.getString("lang");
    String headFinderOption;
    if (parsedArguments.get("heads") == null) {
      headFinderOption = "";
    } else {
      headFinderOption = parsedArguments.getString("heads");
    }

    // construct kaf Reader and read from standard input
    Annotate annotator = new Annotate(lang);
    BufferedReader breader = null;
    BufferedWriter bwriter = null;
    
    try {
      breader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
      bwriter = new BufferedWriter(new OutputStreamWriter(System.out, "UTF-8"));
      KAFDocument kaf = KAFDocument.createFromStream(breader);
      
      // choosing HeadFinder: (Collins rules for English and derivations of it
      // for other languages; sem (Semantic headFinder re-implemented from
      // Stanford CoreNLP).
      // Default: sem (semantic head finder).

      HeadFinder headFinder = null;

      if (!headFinderOption.isEmpty()) {
        if (lang.equalsIgnoreCase("en")) {

          if (headFinderOption.equalsIgnoreCase("synt")) {
            headFinder = new CollinsHeadFinder(lang);
          } else {
        	  // headFinder = new SemanticHeadFinder(lang);
            System.err
                .println("English Semantic Head Finder not yet available. Using Collins instead.");
            headFinder = new CollinsHeadFinder(lang);
          }
        }
        if (lang.equalsIgnoreCase("es")) {
          if (headFinderOption.equalsIgnoreCase("synt")) {
            headFinder = new CollinsHeadFinder(lang);
          } else {
            // headFinder = new SemanticHeadFinder(lang);
            System.err
                .println("Spanish Semantic Head Finder not available. Using Collins instead");
            headFinder = new CollinsHeadFinder(lang);
          }
        }

        // parse with heads
        bwriter.write(annotator.getConstituentParseWithHeads(kaf, headFinder));
      }
        // parse without heads
      else {
        bwriter.write(annotator.getConstituentParse(kaf));
      }

      bwriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
