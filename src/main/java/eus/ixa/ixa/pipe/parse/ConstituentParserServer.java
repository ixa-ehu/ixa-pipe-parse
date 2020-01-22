/*
 *Copyright 2020 Rodrigo Agerri

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
import org.jdom2.JDOMException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 
 * @author ragerri
 *
 */
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
  private String model;
  /**
   * The annotation output format, one of NAF (default) and oneline penn treebank.
   */
  private String outputFormat;
  
  /**
   * Construct a NameFinder server.
   * 
   * @param properties
   *          the properties
   */
  @SuppressWarnings("InfiniteLoopStatement") public ConstituentParserServer(Properties properties) {

    int port = Integer.parseInt(properties.getProperty("port"));
    model = properties.getProperty("model");
    outputFormat = properties.getProperty("outputFormat");
    
    String kafToString;
    ServerSocket socketServer = null;
    Socket activeSocket;
    BufferedReader inFromClient;
    BufferedWriter outToClient = null;

    try {
      Annotate annotator = new Annotate(properties);
      System.out.println("-> Trying to listen port... " + port);
      socketServer = new ServerSocket(port);
      System.out.println("-> Connected and listening to port " + port);
      while (true) {
        try {
          activeSocket = socketServer.accept();
          inFromClient = new BufferedReader(new InputStreamReader(activeSocket.getInputStream(),
              StandardCharsets.UTF_8));
          outToClient = new BufferedWriter(new OutputStreamWriter(activeSocket.getOutputStream(),
              StandardCharsets.UTF_8));
          //get data from client
          String stringFromClient = getClientData(inFromClient);
          // annotate
          kafToString = getAnnotations(annotator, stringFromClient);
        } catch (JDOMException e) {
          kafToString = "\n-> ERROR: Badly formatted NAF document!!\n";
          sendDataToClient(outToClient, kafToString);
          continue;
        } catch (UnsupportedEncodingException e) {
          kafToString = "\n-> ERROR: UTF-8 not supported!!\n";
          assert outToClient != null;
          sendDataToClient(outToClient, kafToString);
          continue;
        } catch (IOException e) {
          kafToString = "\n -> ERROR: Input data not correct!!\n";
          assert outToClient != null;
          sendDataToClient(outToClient, kafToString);
          continue;
        }
        //send data to server after all exceptions and close the outToClient
        sendDataToClient(outToClient, kafToString);
        //close the resources
        inFromClient.close();
        activeSocket.close();
      } //end of processing block
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("-> IOException due to failing to create the TCP socket or to wrongly provided model path.");
    } finally {
      System.out.println("closing tcp socket...");
      try {
        assert socketServer != null;
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
  private String getClientData(BufferedReader inFromClient) {
    StringBuilder stringFromClient = new StringBuilder();
    try {
      String line;
      while ((line = inFromClient.readLine()) != null) {
        if (line.matches("<ENDOFDOCUMENT>")) {
          break;
        }
        stringFromClient.append(line).append("\n");
        if (line.matches("</NAF>")) {
          break;
        }
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
  private void sendDataToClient(BufferedWriter outToClient, String kafToString) throws IOException {
    outToClient.write(kafToString);
    outToClient.close();
  }

  /**
   * Named Entity annotator.
   * 
   * @param annotator
   *          the annotator
   * @param stringFromClient
   *          the string to be annotated
   * @return the annotation result
   * @throws IOException
   *           if io error
   * @throws JDOMException
   *           if xml error
   */
  private String getAnnotations(Annotate annotator, String stringFromClient)
      throws IOException, JDOMException {
    // get a breader from the string coming from the client
    BufferedReader clientReader = new BufferedReader(new StringReader(
        stringFromClient));
    KAFDocument kaf = KAFDocument.createFromStream(clientReader);
    final KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor(
        "constituency",
        "ixa-pipe-parse-" + Paths.get(model).getFileName(), this.version
            + "-" + this.commit);
    newLp.setBeginTimestamp();
    String kafToString;
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
