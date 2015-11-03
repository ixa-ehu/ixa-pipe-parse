package eus.ixa.ixa.pipe.parse;

import ixa.kaflib.KAFDocument;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.jdom2.JDOMException;

import com.google.common.io.Files;

public class ConstituentParserServer {

  /**
   * Get dynamically the version of ixa-pipe-parse by looking at the MANIFEST
   * file.
   */
  private final String version = CLI.class.getPackage().getImplementationVersion();
  /**
   * Get the git commit of the ixa-pipe-parse compiled by looking at the MANIFEST
   * file.
   */
  private final String commit = CLI.class.getPackage().getSpecificationVersion();
  /**
   * The model.
   */
  private String model = null;
  /**
   * The annotation output format, one of NAF (default) and oneline penn treebank.
   */
  private String outputFormat = null;
  
  /**
   * Construct a NameFinder server.
   * 
   * @param properties
   *          the properties
   */
  public ConstituentParserServer(Properties properties) {

    Integer port = Integer.parseInt(properties.getProperty("port"));
    model = properties.getProperty("model");
    outputFormat = properties.getProperty("outputFormat");
    ServerSocket socketServer = null;

    try {
      Annotate annotator = new Annotate(properties);
      System.out.println("-> Trying to listen port... " + port);
      socketServer = new ServerSocket(port);
      System.out.println("-> Connected and listening to port " + port);
      while (true) {
        
        try (Socket activeSocket = socketServer.accept();
            DataInputStream inFromClient = new DataInputStream(
                activeSocket.getInputStream());
            DataOutputStream outToClient = new DataOutputStream(new BufferedOutputStream(
                activeSocket.getOutputStream()));) {
          //System.err.println("-> Received a  connection from: " + activeSocket);
          //get data from client
          String stringFromClient = getClientData(inFromClient);
          // annotate
          String kafToString = getAnnotations(annotator, stringFromClient);
          // send to server
          sendDataToServer(outToClient, kafToString);
        }
      }
    } catch (IOException | JDOMException e) {
      e.printStackTrace();
    } finally {
      System.out.println("closing tcp socket...");
      try {
        socketServer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  /**
   * Read data from the client and output to a String.
   * @param inFromClient the client inputstream
   * @return the string from the client
   */
  private String getClientData(DataInputStream inFromClient) {
    //get data from client and build a string with it
    StringBuilder stringFromClient = new StringBuilder();
    try {
      boolean endOfClientFile = inFromClient.readBoolean();
      String line;
      while (!endOfClientFile) {
        line = inFromClient.readUTF();
        stringFromClient.append(line).append("\n");
        endOfClientFile = inFromClient.readBoolean();
    }
    }catch (IOException e) {
      e.printStackTrace();
    }
    return stringFromClient.toString();
  }
  
  /**
   * Send data back to server after annotation.
   * @param outToClient the outputstream to the client
   * @param kafToString the string to be processed
   * @throws IOException if io error
   */
  private void sendDataToServer(DataOutputStream outToClient, String kafToString) throws IOException {
    
    byte[] kafByteArray = kafToString.getBytes("UTF-8");
    outToClient.write(kafByteArray);
  }
  
  /**
   * Named Entity annotator.
   * @param annotator the annotator
   * @param stringFromClient the string to be annotated
   * @return the annotation result
   * @throws IOException if io error
   * @throws JDOMException if xml error
   */
  private String getAnnotations(Annotate annotator, String stringFromClient) throws IOException, JDOMException {
  //get a breader from the string coming from the client
    BufferedReader clientReader = new BufferedReader(new StringReader(stringFromClient));
    KAFDocument kaf = KAFDocument.createFromStream(clientReader);
    final KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor(
        "constituency",
        "ixa-pipe-parse-" + Files.getNameWithoutExtension(model), this.version
            + "-" + this.commit);
    newLp.setBeginTimestamp();
    String kafToString = null;
    if (outputFormat.equalsIgnoreCase("oneline")) {
      kafToString = annotator.parseToOneline(kaf);
    } else {
      annotator.parseToKAF(kaf);
      newLp.setEndTimestamp();
      kafToString = kaf.toString();
    }
    return kafToString;
  }
}
