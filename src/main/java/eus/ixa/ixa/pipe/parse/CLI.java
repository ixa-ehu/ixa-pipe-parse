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

package eus.ixa.ixa.pipe.parse;


import ixa.kaflib.KAFDocument;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import org.jdom2.JDOMException;

import com.google.common.io.Files;

/**
 * This is the main class of ixa-pipe-parse a constituent shift-reduce parser based on
 * on Apache OpenNLP project, which is based on (Ratnapharki 1999).
 * Provides extra support for Collins Head Finder useful for coreference resolution
 * and dependency conversion.
 * It also takes the tokens of a gold parse file for evaluation (e.g., sec 23 of Penn
 * Treebank) and produces the test parse file ready to be evaluated with EVALB.
 * 
 * @author ragerri
 * @version 2014-04-18
 */

public class CLI {

  /**
   * Get dynamically the version of ixa-pipe-parse by looking at the MANIFEST
   * file.
   */
  private final String version = CLI.class.getPackage()
      .getImplementationVersion();
  /**
   * Get the git commit of the ixa-pipe-parse compiled by looking at the MANIFEST
   * file.
   */
  private final String commit = CLI.class.getPackage().getSpecificationVersion();

  Namespace parsedArguments = null;

  // create Argument Parser
  ArgumentParser argParser = ArgumentParsers.newArgumentParser(
      "ixa-pipe-parse-" + version + ".jar").description(
      "ixa-pipe-parse is a multilingual Constituent Parsing module "
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
   * @throws JDOMException 
   */
  public final void parseCLI(final String[] args) throws IOException, JDOMException {
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
      System.out.println("Run java -jar target/ixa-pipe-parse-" + version
          + ".jar" +  " (parse|train|eval) -help for details");
      System.exit(1);
    }
  }

  public final void annotate(final InputStream inputStream,
      final OutputStream outputStream) throws IOException, JDOMException {
    
    BufferedReader breader = new BufferedReader(new InputStreamReader(
        inputStream, "UTF-8"));
    BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(
        outputStream, "UTF-8"));
    KAFDocument kaf = KAFDocument.createFromStream(breader);
    String model = parsedArguments.getString("model");
    String headFinderOption = parsedArguments.getString("headFinder");
    String outputFormat = parsedArguments.getString("outputFormat");
   // language parameter
    String lang = null;
    if (parsedArguments.getString("language") != null) {
      lang = parsedArguments.getString("language");
      if (!kaf.getLang().equalsIgnoreCase(lang)) {
        System.err
            .println("Language parameter in NAF and CLI do not match!!");
        System.exit(1);
      }
    } else {
      lang = kaf.getLang();
    }
    Properties properties = setAnnotateProperties(model, lang, headFinderOption);
    KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor(
        "constituency", "ixa-pipe-parse-" + Files.getNameWithoutExtension(model), version + "-" + commit);
    newLp.setBeginTimestamp();
    Annotate annotator = new Annotate(properties);
    String kafToString = null;
    if (outputFormat.equalsIgnoreCase("oneline")) {
      kafToString = annotator.parseToOneline(kaf);
    } else {
      annotator.parseToKAF(kaf);
      newLp.setEndTimestamp();
      kafToString = kaf.toString();
    }
    bwriter.write(kafToString);
    bwriter.close();
    breader.close();
  }

  public final void train() {
    System.err.println("Not yet ready!");
  }

  public final void eval() throws IOException {
    
    String lang = parsedArguments.getString("language");
    String model = parsedArguments.getString("model");
    String headFinderOption = parsedArguments.getString("headFinder");
    Properties properties = setEvaluateProperties(model, lang, headFinderOption);
    Annotate annotator = new Annotate(properties);
    // special option to process treebank files adding headword marks
    if (parsedArguments.getString("addHeads") != null) {
      File inputTree = new File(
          parsedArguments.getString("addHeads"));
      String ext = parsedArguments.getString("extension");
      annotator.processTreebankWithHeadWords(inputTree, ext);
    }
    else if (parsedArguments.get("test") != null) {
      File inputTree = new File(parsedArguments.getString("test"));
      annotator.parseForTesting(inputTree);
    }
  }

  public void loadAnnotateParameters() {
    annotateParser
        .addArgument("-m", "--model")
        .required(true)
        .help("Choose parsing model.\n");
    annotateParser
        .addArgument("-l", "--language")
         .choices("en", "es")
        .required(false)
        .help("Choose language; it defaults to the language value in incoming NAF file.\n");
    annotateParser
        .addArgument("-g", "--headFinder")
        .choices("collins", "sem", Flags.DEFAULT_HEADFINDER)
        .setDefault(Flags.DEFAULT_HEADFINDER)
        .required(false)
        .help("Choose between Collins or Semantic HeadFinder.\n");
    annotateParser
        .addArgument("-o", "--outputFormat")
        .choices("oneline", "naf")
        .setDefault(Flags.DEFAULT_OUTPUT_FORMAT)
        .required(false)
        .help(
            "Choose outputFormat; it defaults to NAF.\n");
  }

  /**
   * Create the main parameters available for training parse models.
   */
  private void loadTrainingParameters() {
  }

  private void loadEvalParameters() {
    
    evalParser
    .addArgument("-m", "--model")
    .required(true)
    .help("Choose parsing model.\n");
   evalParser
    .addArgument("-l", "--language")
     .choices("en", "es")
    .required(true)
    .help("Choose language.\n");
   evalParser
    .addArgument("-g", "--headFinder")
    .choices("collins", "sem", Flags.DEFAULT_HEADFINDER)
    .setDefault(Flags.DEFAULT_HEADFINDER)
    .required(false)
    .help("Choose between Collins or Semantic HeadFinder.\n");
    evalParser.addArgument("--addHeads")
        .help(
        "Takes a file or a directory as argument containing a parse tree in penn treebank (one line per sentence) format; this option requires --lang and --headFinder options.\n");
    evalParser
        .addArgument("--test")
        .help(
            "Takes a file as argument containing the tokenized text of a gold standard Penn Treebank file to process it; It produces a test file for its parseval evaluation with EVALB.\n");
  }
  
  private Properties setAnnotateProperties(String model, String language, String headFinder) {
    Properties annotateProperties = new Properties();
    annotateProperties.setProperty("model", model);
    annotateProperties.setProperty("language", language);
    annotateProperties.setProperty("headFinder", headFinder);
    return annotateProperties;
  }
  
  private Properties setEvaluateProperties(String model, String language, String headFinder) {
    Properties annotateProperties = new Properties();
    annotateProperties.setProperty("model", model);
    annotateProperties.setProperty("language", language);
    annotateProperties.setProperty("headFinder", headFinder);
    return annotateProperties;
  }

}
