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
import ixa.pipe.heads.AncoraHeadFinder;
import ixa.pipe.heads.AncoraSemanticHeadFinder;
import ixa.pipe.heads.CollinsHeadFinder;
import ixa.pipe.heads.EnglishSemanticHeadFinder;
import ixa.pipe.heads.HeadFinder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import org.jdom2.JDOMException;

/**
 * ixa-pipe-parse: Constituent Parsing:
 * 
 * <li>
 * <ol>
 * Using Apache OpenNLP Machine Learning API for training and deploying models.
 * <ol>
 * Providing extra support for Collins and Stanford Semantic Head Finders, the
 * latter useful for coreference resolution and dependency conversion.
 * <ol>
 * It takes the tokens of a gold parse file for evaluation (e.g., sec 23 of Penn
 * Treebank) and produces the test parse file ready to be evaluated with EVALB.
 * <ol>
 * Outputs KAF and penn treebank formats.</li>
 * 
 * @author ragerri
 * @version 1.0
 * 
 */

public class CLI {

  Namespace parsedArguments = null;

  // create Argument Parser
  ArgumentParser argParser = ArgumentParsers.newArgumentParser(
      "ixa-pipe-parse-1.0.jar").description(
      "ixa-pipe-parse-1.0 is a multilingual Constituent Parsing module "
          + "developed by IXA NLP Group.\n");
  /**
   * Sub parser instance.
   */
  private Subparsers subParsers = argParser.addSubparsers().help(
      "sub-command help");
  /**
   * The parser that manages the tagging sub-command.
   */
  private Subparser annotateParser;
  /**
   * The parser that manages the training sub-command.
   */
  private Subparser trainParser;
  /**
   * The parser that manages the evaluation sub-command.
   */
  private Subparser evalParser;

  /**
   * Construct a CLI object with the three sub-parsers to manage the command
   * line parameters.
   */
  public CLI() {
    annotateParser = subParsers.addParser("parse").help("Parsing CLI");
    loadAnnotateParameters();
    trainParser = subParsers.addParser("train").help("Training CLI");
    loadTrainingParameters();
    evalParser = subParsers.addParser("eval").help("Evaluation CLI");
    loadEvalParameters();
  }

  public static void main(String[] args) throws IOException, JDOMException {

    CLI cmdLine = new CLI();
    cmdLine.parseCLI(args);
  }

  /**
   * Parse the command interface parameters with the argParser.
   * 
   * @param args
   *          the arguments passed through the CLI
   * @throws IOException
   *           exception if problems with the incoming data
   */
  public final void parseCLI(final String[] args) throws IOException {
    try {
      parsedArguments = argParser.parseArgs(args);
      System.err.println("CLI options: " + parsedArguments);
      if (args[0].equals("parse")) {
        annotate(System.in, System.out);
      } else if (args[0].equals("eval")) {
        eval();
      } else if (args[0].equals("train")) {
        train();
      }
    } catch (ArgumentParserException e) {
      argParser.handleError(e);
      System.out
          .println("Run java -jar target/ixa-pipe-parse-1.0.jar (parse|train|eval) -help for details");
      System.exit(1);
    }
  }

  public final void annotate(final InputStream inputStream,
      final OutputStream outputStream) throws IOException {
    String headFinderOption;
    if (parsedArguments.get("heads") == null) {
      headFinderOption = "";
    } else {
      headFinderOption = parsedArguments.getString("heads");
    }
    HeadFinder headFinder = null;
    BufferedReader breader = new BufferedReader(new InputStreamReader(
        inputStream, "UTF-8"));
    BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(
        outputStream, "UTF-8"));
    // read KAF document from inputstream
    KAFDocument kaf = KAFDocument.createFromStream(breader);
    String lang;
    if (parsedArguments.get("lang") == null) {
      lang = kaf.getLang();
    } else {
      lang = parsedArguments.getString("lang");
    }
    KAFDocument.LinguisticProcessor newLp =
        kaf.addLinguisticProcessor("constituency","ixa-pipe-parse-" + lang,"1.0");
    if (!headFinderOption.isEmpty()) {
      if (lang.equalsIgnoreCase("en")) {

        if (headFinderOption.equalsIgnoreCase("collins")) {
          headFinder = new CollinsHeadFinder();
        } else {
          headFinder = new EnglishSemanticHeadFinder();
        }
      }
      if (lang.equalsIgnoreCase("es")) {
        if (headFinderOption.equalsIgnoreCase("collins")) {
          headFinder = new AncoraHeadFinder();
        } else {
          headFinder = new AncoraSemanticHeadFinder();
        }
      }
      // parse with heads
      newLp.setBeginTimestamp();
      Annotate annotator = new Annotate(lang, headFinder);
      if (parsedArguments.getBoolean("nokaf")) {
        annotator.parseToKAF(kaf);
        newLp.setEndTimestamp();
        bwriter.write(kaf.toString());
      } else {
        bwriter.write(annotator.parse(kaf));
      }
    }
    // parse without heads
    else {
      newLp.setBeginTimestamp();
      Annotate annotator = new Annotate(lang);
      if (parsedArguments.getBoolean("nokaf")) {
        annotator.parseToKAF(kaf);
        newLp.setEndTimestamp();
        bwriter.write(kaf.toString());
      } else {
        bwriter.write(annotator.parse(kaf));
      }
    }
    bwriter.close();
  }

  public final void train() {
    System.err.println("Not yet ready!");
  }

  public final void eval() throws IOException {
    String headFinderOption;
    if (parsedArguments.get("heads") == null) {
      headFinderOption = "";
    } else {
      headFinderOption = parsedArguments.getString("heads");
    }
    HeadFinder headFinder = null;

    // special option to process treebank files adding headword marks
    if (parsedArguments.getString("processTreebankWithHeadWords") != null) {
      File inputTree = new File(
          parsedArguments.getString("processTreebankWithHeadWords"));
      String lang = parsedArguments.getString("lang");
      String ext = parsedArguments.getString("extension");
      if (!headFinderOption.isEmpty()) {
        if (lang.equalsIgnoreCase("en")) {

          if (headFinderOption.equalsIgnoreCase("collins")) {
            headFinder = new CollinsHeadFinder();
          } else {
            headFinder = new EnglishSemanticHeadFinder();
          }
        }
        if (lang.equalsIgnoreCase("es")) {
          if (headFinderOption.equalsIgnoreCase("collins")) {
            headFinder = new AncoraHeadFinder();
          } else {
            headFinder = new AncoraHeadFinder();
          }
        }
      }
      Annotate annotator = new Annotate(lang, headFinder);
      annotator.processTreebankWithHeadWords(inputTree, ext);
    }

    else if (parsedArguments.get("test") != null) {
      File inputTree = new File(parsedArguments.getString("test"));
      String lang = parsedArguments.getString("lang");
      Annotate annotator = new Annotate(lang);
      annotator.parseForTesting(inputTree);
    }
  }

  public void loadAnnotateParameters() {
    annotateParser.addArgument("-k", "--nokaf").action(Arguments.storeFalse())
        .help("Do not print parse in KAF format, but plain text.\n");
    annotateParser
        .addArgument("-o", "--outputFormat")
        .choices("penn", "oneline")
        .setDefault("oneline")
        .required(false)
        .help(
            "Choose between Penn style or oneline LISP style tree output; this option only works if '--nokaf' is also on.\n");
    annotateParser
        .addArgument("-g", "--heads")
        .choices("collins", "sem")
        .required(false)
        .help("Choose between Collins-based or Stanford Semantic HeadFinder.\n");
    annotateParser.addArgument("-l", "--lang").choices("en", "es")
        .required(false)
        .help("Choose a language to perform annotation with ixa-pipe-parse.\n");

  }

  /**
   * Create the main parameters available for training parse models.
   */
  private void loadTrainingParameters() {
  }

  private void loadEvalParameters() {
    evalParser.addArgument("--processTreebankWithHeadWords").help(
        "Takes a file as argument containing a parse tree in penn treebank "
            + "(one line per sentence) format; "
            + "this option requires --lang, --heads and --nokaf options.\n");
    evalParser
        .addArgument("--extension")
        .help(
            "Specify extension of files, e.g. '.txt' or '' for every file, "
                + "to be processed by the --processTreebankWithHeadWords directory option.\n");
    evalParser
        .addArgument("--test")
        .help(
            "Takes a file as argument containing the tokenized text "
                + "of a gold standard Penn Treebank file to process it; It produces a test file for its "
                + "parseval evaluation with EVALB.\n");
  }

}
