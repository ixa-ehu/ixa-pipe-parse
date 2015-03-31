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

package es.ehu.si.ixa.ixa.pipe.parse;

import java.io.InputStream;

public class Models {

  private InputStream parseModel;
  private InputStream headsFile; 
  
  public InputStream getParseModel(String cmdOption) {

    if (cmdOption.equals("en")) {
      parseModel = getClass().getResourceAsStream("/en-parser-chunking.bin");
    }

    if (cmdOption.equals("es")) {
      parseModel = getClass().getResourceAsStream("/es-parser-chunking.bin");
    }
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
