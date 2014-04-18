/*
 *
 *Copyright 2014 Rodrigo Agerri

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

package es.ehu.si.ixa.pipe.parse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

/**
 * Class to load, initialize the parse model. It also creates the main parsing
 * method.
 * 
 * 
 * @author ragerri
 * @version 2014-01-10
 * 
 */

public class ConstituentParsing {

  private static ParserModel parserModel;
  private Parser parser;

  /**
   * It constructs an object Parser from the Parser class. First it loads a
   * model, then it initializes the nercModel and finally it creates a
   * nercDetector using such model.
   */
  public ConstituentParsing(String lang, String model) {

    ParserModel parserModel = loadModel(lang, model);
    parser = ParserFactory.create(parserModel);
  }

  /**
   * This method receives as an input a tokenized sentence and stores the parses
   * in a Parse object array. The parses can then be visualized in the usual
   * treebank format by using the Parse.show() function.
   * 
   * 
   * @param tokenized
   *          sentence
   * @param int number of parsers
   * @return an array Parse objects (as many as numParsers parameter)
   * 
   * */
  public Parse[] parse(String sentence, int numParsers) {
    Parse parsedSentence[] = ParserTool.parseLine(sentence, parser, numParsers);
    return parsedSentence;
  }

  private ParserModel loadModel(String lang, String model) {
    InputStream trainedModelInputStream = null;
    try {
      if (parserModel == null) {
        if (model.equalsIgnoreCase("baseline")) {
          trainedModelInputStream = getParserModelInputStream(lang);
        } else {
          trainedModelInputStream = new FileInputStream(model);
        }
        parserModel = new ParserModel(trainedModelInputStream);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (trainedModelInputStream != null) {
        try {
          trainedModelInputStream.close();
        } catch (IOException e) {
          System.err.println("Could not load model!");
        }
      }
    }
    return parserModel;
  }

  public InputStream getParserModelInputStream(String lang) {
    InputStream parseModelInputStream = null;
    long lStartTime = new Date().getTime();

    if (lang.equals("en")) {
      parseModelInputStream = getClass().getResourceAsStream(
          "/en-parser-chunking.bin");
    }

    if (lang.equals("es")) {
      parseModelInputStream = getClass().getResourceAsStream(
          "/es-parser-chunking.bin");
    }

    long lEndTime = new Date().getTime();
    long difference = lEndTime - lStartTime;
    System.err.println("Parse model loaded in: " + difference
        + " seconds ... [DONE]");
    return parseModelInputStream;
  }

}
