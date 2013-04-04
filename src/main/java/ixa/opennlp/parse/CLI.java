package ixa.opennlp.parse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.List;

import org.jdom2.Element;
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
   * BufferedReader (from standard input) and BufferedWriter are opened. The module 
   * takes KAF and reads the header, and the text elements and uses Annotate class to 
   * provide constituent parsing of sentences, which are provided via standard output.  
   * 
   * @param args
   * @throws IOException
 * @throws JDOMException 
   */

  public static void main(String[] args) throws IOException, JDOMException {

	  KAFReader kafReader = new KAFReader();
	  Annotate annotator = new Annotate();
	  StringBuilder sb = new StringBuilder();
	  BufferedReader breader = null;
	  BufferedWriter bwriter = null;
	  KAF kaf = new KAF();
	  try {
	  breader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
      bwriter = new BufferedWriter(new OutputStreamWriter(System.out,"UTF-8"));
      String line;
      while ((line = breader.readLine()) != null) {
    	sb.append(line);
      }
      
      // read KAF from standard input
      InputStream kafIn = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
      Element rootNode = kafReader.getRootNode(kafIn);
  	  List<Element> lingProc = kafReader.getKafHeader(rootNode);
      List<Element> wfs = kafReader.getWfs(rootNode);
  	  LinkedHashMap<String, List<String>> sentencesMap = kafReader
            .getSentencesMap(wfs);
      LinkedHashMap<String, List<String>> sentences = kafReader
            .getSentsFromWfs(sentencesMap, wfs);
        
     // add already contained header plus this module linguistic
     // processor
      annotator.addKafHeader(lingProc, kaf);
      kaf.addlps("terms", "ehu-opennlp-pos-en", kaf.getTimestamp(), "1.0");  
      
      // get parsing  
  	 bwriter.write(annotator.getConstituentParse(sentences));
     bwriter.close();
    }
	  catch (IOException e){ 
		  e.printStackTrace();
	  }

  }
}
