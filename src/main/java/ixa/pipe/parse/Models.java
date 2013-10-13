/*
 * Copyright 2013 Rodrigo Agerri

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

import java.io.InputStream;
import java.util.Date;

/**
 * Class to load the language-dependent resources. Mainly models but also head rules files if 
 * required. Resources are located according to maven project structure: $src/main/resources/
 * 
 * @author ragerri
 *
 */
public class Models {

  private InputStream parseModel;
  private InputStream headsFile; 
  
  public InputStream getParseModel(String cmdOption) {
    long lStartTime = new Date().getTime();

    if (cmdOption.equals("en")) {
      parseModel = getClass().getResourceAsStream("/en-parser-chunking.bin");
    }

    if (cmdOption.equals("es")) {
      parseModel = getClass().getResourceAsStream("/es-parser-chunking.bin");
    }
    
    long lEndTime = new Date().getTime();
    long difference = lEndTime - lStartTime;
    System.err.println("Parse model loaded in: " + difference + " seconds ... [DONE]");
    return parseModel;
  }
  
  public InputStream getHeadRulesFile(String cmdOption) {

	    if (cmdOption.equals("en")) {
	      headsFile = getClass().getResourceAsStream("/en-head-rules");
	    }

	    if (cmdOption.equals("es")) {
	      headsFile = getClass().getResourceAsStream("/es-head-rules");
	    }
	    return headsFile;
	  }

}
