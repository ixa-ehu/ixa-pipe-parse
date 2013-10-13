/*
 *
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

import java.io.IOException;
import java.io.InputStream;


import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

/**
 * Class to load, initialize the parse model. It also creates the main 
 * parsing method.  
 * 
 *  
 * @author ragerri 2012/11/30
 * 
 */

public class ConstituentParsing {

  private ParserModel parserModel;
  private Parser parser;

  /**
   * It constructs an object Parser from the Parser class. First it loads a model,
   * then it initializes the nercModel and finally it creates a nercDetector
   * using such model.
   */
  public ConstituentParsing(InputStream trainedModel) {

    try {
      parserModel = new ParserModel(trainedModel);

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (trainedModel != null) {
        try {
          trainedModel.close();
        } catch (IOException e) {
        }
      }
    }
    parser = ParserFactory.create(parserModel);
  }

  /**
   * This method receives as an input a tokenized sentence and stores the 
   * parses in a Parse object array. The parses can then be visualized in the usual 
   * treebank format by using the Parse.show() function. 
   * 
   *  
   * @param tokenized sentence
   * @param int number of parsers 
   * @return an array Parse objects (as many as numParsers parameter) 
   * 
   *  */
  public Parse[] parse(String sentence, int numParsers) {
    Parse parsedSentence[] = ParserTool.parseLine(sentence, parser, numParsers);
    return parsedSentence;
  }
  
}
