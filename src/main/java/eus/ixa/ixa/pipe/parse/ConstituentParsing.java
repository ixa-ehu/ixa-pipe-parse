/*
 * Copyright 2015 Rodrigo Agerri

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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

/**
 * Probabilistic Constituent Parser based on Apache OpenNLP shift-reduced parser
 * (Ratnapharki 1999).
 * 
 * @author ragerri
 * @version 2015-04-30
 */

public class ConstituentParsing {

  /**
   * The models to use for every language. The keys of the hash are the language
   * codes, the values the models.
   */
  private static ConcurrentHashMap<String, ParserModel> parseModels = new ConcurrentHashMap<String, ParserModel>();
  /**
   * The parser.
   */
  private final Parser parser;

  public ConstituentParsing(final Properties properties) {
    final String lang = properties.getProperty("language");
    final String model = properties.getProperty("model");
    final ParserModel parserModel = loadModel(lang, model);
    this.parser = ParserFactory.create(parserModel);
  }

  private final ParserModel loadModel(final String lang, final String model) {
    final long lStartTime = new Date().getTime();
    try {
      parseModels
          .putIfAbsent(lang, new ParserModel(new FileInputStream(model)));
    } catch (final IOException e) {
      e.printStackTrace();
    }
    final long lEndTime = new Date().getTime();
    final long difference = lEndTime - lStartTime;
    System.err.println("ixa-pipe-parse model loaded in: " + difference
        + " miliseconds ... [DONE]");
    return parseModels.get(lang);
  }

  /**
   * This method receives as an input a tokenized sentence and stores the parses
   * in a Parse object array. The parses can then be visualized in the usual
   * treebank format by using the Parse.show() function.
   * 
   * 
   * @param sentence
   *          tokenized sentence
   * @param numParses
   *          number of parses
   * @return an array Parse objects (as many as numParses parameter)
   * 
   * */
  public Parse[] parse(final String sentence, final int numParses) {
    final Parse[] parsedSentence = ParserTool.parseLine(sentence, this.parser,
        numParses);
    return parsedSentence;
  }

}