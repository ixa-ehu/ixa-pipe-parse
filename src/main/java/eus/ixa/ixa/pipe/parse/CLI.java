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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
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
 * This is the main class of ixa-pipe-parse a constituent shift-reduce parser
 * based on on Apache OpenNLP project, which is based on (Ratnapharki 1999).
 * Provides extra support for Collins Head Finder useful for coreference
 * resolution and dependency conversion. It also takes the tokens of a gold
 * parse file for evaluation (e.g., sec 23 of Penn Treebank) and produces the
 * test parse file ready to be evaluated with EVALB.
 * 
 * @author ragerri
 * @version 2020-01-22
 */

public class CLI {

  /**
   * Get dynamically the version of ixa-pipe-parse by looking at the MANIFEST
   * file.
   */
  private final String version = CLI.class.getPackage()
      .getImplementationVersion();
  /**
   * Get the git commit of the ixa-pipe-parse compiled by looking at the
   * MANIFEST file.
   */
  private final String commit = CLI.class.getPackage()
      .getSpecificationVersion();

  Namespace parsedArguments = null;

  // create Argument Parser
  ArgumentParser argParser = ArgumentParsers.newArgumentParser(
      "ixa-pipe-parse-" + this.version + ".jar").description(
      "ixa-pipe-parse is a multilingual Constituent Parsing module "
          + "developed by IXA NLP Group.\n");
  /**
   * Sub parser instance.
   */
  private final Subparsers subParsers = this.argParser.addSubparsers().help(
      "sub-command help");
  /**
   * The parser that manages the tagging sub-command.
   */
  private final Subparser annotateParser;
  /**
   * The parser that manages the training sub-command.
   */
  private final Subparser trainParser;
  /**
   * The parser that manages the evaluation sub-command.
   */
  private final Subparser evalParser;
  /**
   *  Parser to start TCP socket for server-client functionality.
   */
 private Subparser serverParser;
 /**
  * Sends queries to the serverParser for annotation.
  */
 private Subparser clientParser;

  /**
   * Construct a CLI object with the three sub-parsers to manage the command
   * line parameters.
   */
  public CLI() {
    this.annotateParser = this.subParsers.addParser("parse")
        .help("Parsing CLI");
    loadAnnotateParameters();
    this.trainParser = this.subParsers.addParser("train").help("Training CLI");
    loadTrainingParameters();
    this.evalParser = this.subParsers.addParser("eval").help("Evaluation CLI");
    loadEvalParameters();
    serverParser = subParsers.addParser("server").help("Start TCP socket server");
    loadServerParameters();
    clientParser = subParsers.addParser("client").help("Send queries to the TCP socket server");
    loadClientParameters();
  }

  public static void main(final String[] args) throws IOException,
      JDOMException {

    final CLI cmdLine = new CLI();
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
   *           if xml formatting exception
   */
  public final void parseCLI(final String[] args) throws IOException,
      JDOMException {
    try {
      this.parsedArguments = this.argParser.parseArgs(args);
      System.err.println("CLI options: " + this.parsedArguments);
      if (args[0].equals("parse")) {
        annotate(System.in, System.out);
      } else if (args[0].equals("eval")) {
        eval();
      } else if (args[0].equals("train")) {
        train();
      } else if (args[0].equals("server")) {
        server();
      } else if (args[0].equals("client")) {
        client(System.in, System.out);
      }
    } catch (final ArgumentParserException e) {
      this.argParser.handleError(e);
      System.out.println("Run java -jar target/ixa-pipe-parse-" + this.version
          + ".jar" + " (parse|train|eval|server|client) -help for details");
      System.exit(1);
    }
  }

  public final void annotate(final InputStream inputStream,
      final OutputStream outputStream) throws IOException, JDOMException {

    final BufferedReader breader = new BufferedReader(new InputStreamReader(
        inputStream, "UTF-8"));
    final BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(
        outputStream, "UTF-8"));
    final KAFDocument kaf = KAFDocument.createFromStream(breader);
    final String model = this.parsedArguments.getString("model");
    final String headFinderOption = this.parsedArguments
        .getString("headFinder");
    final String outputFormat = this.parsedArguments.getString("outputFormat");
    // language parameter
    String lang = null;
    if (this.parsedArguments.getString("language") != null) {
      lang = this.parsedArguments.getString("language");
      if (!kaf.getLang().equalsIgnoreCase(lang)) {
        System.err.println("Language parameter in NAF and CLI do not match!!");
        System.exit(1);
      }
    } else {
      lang = kaf.getLang();
    }
    final Properties properties = setAnnotateProperties(model, lang,
        headFinderOption);
    final KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor(
        "constituency",
        "ixa-pipe-parse-" + Files.getNameWithoutExtension(model), this.version
            + "-" + this.commit);
    newLp.setBeginTimestamp();
    final Annotate annotator = new Annotate(properties);
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

    final String lang = this.parsedArguments.getString("language");
    final String model = this.parsedArguments.getString("model");
    final String headFinderOption = this.parsedArguments
        .getString("headFinder");
    final Properties properties = setEvaluateProperties(model, lang,
        headFinderOption);
    final Annotate annotator = new Annotate(properties);
    // special option to process treebank files adding headword marks
    if (this.parsedArguments.getString("addHeads") != null) {
      final File inputTree = new File(
          this.parsedArguments.getString("addHeads"));
      annotator.processTreebankWithHeadWords(inputTree);
    } else if (this.parsedArguments.get("test") != null) {
      final File inputTree = new File(this.parsedArguments.getString("test"));
      annotator.parseForTesting(inputTree);
    }
  }
  
  /**
   * Set up the TCP socket for annotation.
   */
  public final void server() {

    // load parameters into a properties
    String port = parsedArguments.getString("port");
    String model = parsedArguments.getString("model");
    String headFinder = parsedArguments.getString("headFinder");
    String outputFormat = parsedArguments.getString("outputFormat");
    // language parameter
    String lang = parsedArguments.getString("language");
    Properties serverproperties = setServerProperties(port, model, lang, headFinder, outputFormat);
    new ConstituentParserServer(serverproperties);
  }
  
  /**
   * The client to query the TCP server for annotation.
   * 
   * @param inputStream
   *          the stdin
   * @param outputStream
   *          stdout
   */
  public final void client(final InputStream inputStream,
      final OutputStream outputStream) {

    String host = parsedArguments.getString("host");
    String port = parsedArguments.getString("port");
    try (Socket socketClient = new Socket(host, Integer.parseInt(port));
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(
            System.in, "UTF-8"));
        BufferedWriter outToUser = new BufferedWriter(new OutputStreamWriter(
            System.out, "UTF-8"));
        BufferedWriter outToServer = new BufferedWriter(new OutputStreamWriter(
            socketClient.getOutputStream(), "UTF-8"));
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
            socketClient.getInputStream(), "UTF-8"));) {

      // send data to server socket
      StringBuilder inText = new StringBuilder();
      String line;
      while ((line = inFromUser.readLine()) != null) {
        inText.append(line).append("\n");
      }
      inText.append("<ENDOFDOCUMENT>").append("\n");
      outToServer.write(inText.toString());
      outToServer.flush();
      
      // get data from server
      StringBuilder sb = new StringBuilder();
      String kafString;
      while ((kafString = inFromServer.readLine()) != null) {
        sb.append(kafString).append("\n");
      }
      outToUser.write(sb.toString());
    } catch (UnsupportedEncodingException e) {
      //this cannot happen but...
      throw new AssertionError("UTF-8 not supported");
    } catch (UnknownHostException e) {
      System.err.println("ERROR: Unknown hostname or IP address!");
      System.exit(1);
    } catch (NumberFormatException e) {
      System.err.println("Port number not correct!");
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void loadAnnotateParameters() {
    this.annotateParser.addArgument("-m", "--model").required(true)
        .help("Choose parsing model.\n");
    this.annotateParser
        .addArgument("-l", "--language")
        .choices("ca", "en", "es", "it")
        .required(false)
        .help(
            "Choose language; it defaults to the language value in incoming NAF file.\n");
    this.annotateParser.addArgument("-g", "--headFinder")
        .choices("collins", "sem", Flags.DEFAULT_HEADFINDER)
        .setDefault(Flags.DEFAULT_HEADFINDER).required(false)
        .help("Choose between Collins or Semantic HeadFinder.\n");
    this.annotateParser.addArgument("-o", "--outputFormat")
        .choices("oneline", "naf").setDefault(Flags.DEFAULT_OUTPUT_FORMAT)
        .required(false).help("Choose outputFormat; it defaults to NAF.\n");
  }

  /**
   * Create the main parameters available for training parse models.
   */
  private void loadTrainingParameters() {
  }

  private void loadEvalParameters() {

    this.evalParser.addArgument("-m", "--model").required(true)
        .help("Choose parsing model.\n");
    this.evalParser.addArgument("-l", "--language").choices("ca", "en", "es", "it")
        .required(true).help("Choose language.\n");
    this.evalParser.addArgument("-g", "--headFinder")
        .choices("collins", "sem", Flags.DEFAULT_HEADFINDER)
        .setDefault(Flags.DEFAULT_HEADFINDER).required(false)
        .help("Choose between Collins or Semantic HeadFinder.\n");
    this.evalParser
        .addArgument("--addHeads")
        .help(
            "Takes a file or a directory as argument containing a parse tree in penn treebank (one line per sentence) format; this option requires --lang and --headFinder options.\n");
    this.evalParser
        .addArgument("--test")
        .help(
            "Takes a file as argument containing the tokenized text of a gold standard Penn Treebank file to process it; It produces a test file for its parseval evaluation with EVALB.\n");
  }
  

  /**
   * Create the available parameters for NER tagging.
   */
  private void loadServerParameters() {
    
    serverParser.addArgument("-p", "--port")
        .required(true)
         .help("Port to be assigned to the server.\n");
    serverParser.addArgument("-m", "--model").required(true)
        .help("Choose parsing model.\n");
    serverParser
        .addArgument("-l", "--language")
        .choices("ca", "en", "es", "it")
        .required(true)
        .help(
            "Choose language.\n");
    serverParser.addArgument("-g", "--headFinder")
        .choices("collins", "sem", Flags.DEFAULT_HEADFINDER)
        .setDefault(Flags.DEFAULT_HEADFINDER).required(false)
        .help("Choose between Collins or Semantic HeadFinder.\n");
    serverParser.addArgument("-o", "--outputFormat").choices("oneline", "naf")
        .setDefault(Flags.DEFAULT_OUTPUT_FORMAT).required(false)
        .help("Choose outputFormat; it defaults to NAF.\n");
  }
  
  private void loadClientParameters() {
    
    clientParser.addArgument("-p", "--port")
        .required(true)
        .help("Port of the TCP server.\n");
    clientParser.addArgument("--host")
        .required(false)
        .setDefault(Flags.DEFAULT_HOSTNAME)
        .help("Hostname or IP where the TCP server is running.\n");
  }

  private Properties setAnnotateProperties(final String model,
      final String language, final String headFinder) {
    final Properties annotateProperties = new Properties();
    annotateProperties.setProperty("model", model);
    annotateProperties.setProperty("language", language);
    annotateProperties.setProperty("headFinder", headFinder);
    return annotateProperties;
  }

  private Properties setEvaluateProperties(final String model,
      final String language, final String headFinder) {
    final Properties annotateProperties = new Properties();
    annotateProperties.setProperty("model", model);
    annotateProperties.setProperty("language", language);
    annotateProperties.setProperty("headFinder", headFinder);
    return annotateProperties;
  }
  
  private Properties setServerProperties(String port, String model, String language, String headFinder, String outputFormat) {
    Properties serverProperties = new Properties();
    serverProperties.setProperty("port", port);
    serverProperties.setProperty("model", model);
    serverProperties.setProperty("language", language);
    serverProperties.setProperty("headFinder", headFinder);
    serverProperties.setProperty("outputFormat", outputFormat);
    return serverProperties;
  }

}
