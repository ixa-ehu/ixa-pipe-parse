package ixa.opennlp.parse;

import java.io.IOException;
import java.io.InputStream;


import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

/**
 * Simple Parse module based on Apache OpenNLP.
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
  public ConstituentParsing() {

    InputStream trainedModel = getClass().getResourceAsStream("/en-parser-chunking.bin");

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
