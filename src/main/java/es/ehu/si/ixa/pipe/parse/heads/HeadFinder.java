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

package es.ehu.si.ixa.pipe.parse.heads;

import opennlp.tools.parser.Parse;

/**
 * Interface to provide head finder methods. These methods are implemented in the AbstractHeadFinder 
 * class. 
 * 
 * @author ragerri
 * 
 */

  public interface HeadFinder {

    /**
     * It finds the head of the parse constituent according to some head rules 
     * 
     * @param parse
     * @return Parse head constituent
     */
    public Parse getHead(Parse parse);

    /**
     * It finds the head of the parse constituent according to some head rules
     * 
     * @param parse
     * @param parent
     * @return Parse head constituent 
     */
    public Parse getHead(Parse parse, Parse parent);
    
    /**
     * It reads a Parse object and adds the head "=H" symbol for each constituent's 
     * head Node applying some head rules.
     * 
     * @param parse
     */
      public void printHeads(Parse parse);


}
