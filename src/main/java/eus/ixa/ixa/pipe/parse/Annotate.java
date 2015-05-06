package eus.ixa.ixa.pipe.parse;


import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import opennlp.tools.parser.Parse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

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
 private ConstituentParsing parser;
 private HeadFinder headFinder;

 /**
  * Constructor that takes into account lang options (en|es) loads the
  * corresponding parse model and decides whether to mark headWords or not.
  * 
  * @param lang
  */
 public Annotate(Properties properties) {
   parser = new ConstituentParsing(properties);
   annotateOptions(properties);
 }
 
 private void annotateOptions(Properties properties) {
   
   if (!properties.getProperty("headFinder").equalsIgnoreCase(Flags.DEFAULT_HEADFINDER)) {
     markHeads = true;
     loadHeadFinder(properties);
   } else {
     markHeads = false;
   }
 }
 
 private void loadHeadFinder(Properties properties) {
   String headFinderOption = properties.getProperty("headFinder");
   if (headFinderOption.equalsIgnoreCase("collins")) {
     headFinder = new CollinsHeadFinder(properties);
   } else if (headFinderOption.equalsIgnoreCase("sem")) {
     headFinder = new CollinsHeadFinder(properties);
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
     Parse[] parsedSentence = parser.parse(sent, 1);

     if (markHeads) {
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
 public void parseToKAF(KAFDocument kaf) throws IOException {
   StringBuffer parsingDoc = getParse(kaf);
   try {
     kaf.addConstituencyFromParentheses(parsingDoc.toString());
   } catch (Exception e) {
     e.printStackTrace();
   }
 }

 /**
  * @param KAF
  *          document containing <text> and <terms> elements
  * @return parse tree into plain text
  * @throws IOException
  */
 public String parseToOneline(KAFDocument kaf) throws IOException {
   StringBuffer parsingDoc = getParse(kaf);
   return parsingDoc.toString();
 }
 
 public void parseForTesting(File inputText) throws IOException {
   StringBuffer parsingDoc = new StringBuffer();
   if (inputText.isFile()) { 
     List<String> inputTrees = FileUtils.readLines(inputText,"UTF-8");
     for (String sentence : inputTrees) { 
       Parse parsedSentence = parser.parse(sentence,1)[0];
       parsedSentence.show(parsingDoc);
       parsingDoc.append("\n");
       }
     File outfile = new File(FilenameUtils.removeExtension(inputText.getPath()) + ".test");
     System.err.println("Writing test parse file to " + outfile);
     FileUtils.writeStringToFile(outfile, parsingDoc.toString(), "UTF-8");  
     }  
   else { 
     System.out.println("Choose a correct file!");
     System.exit(1);
   }
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
}
