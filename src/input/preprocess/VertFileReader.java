/* 
 * Copyright 2018 Behrang Qasemizadeh (zadeh@phil.hhu.de).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package input.preprocess;


import input.preprocess.objects.TerminalToken;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class VertFileReader {

    BufferedReader br;
    String nextLine;
    boolean hasNext;

    public VertFileReader(String file) throws FileNotFoundException, IOException {
        br = new BufferedReader(new FileReader(file));
        hasNext = true;
    }
   
    
    private static TerminalToken fromLineRawParse(String line) {
        //System.out.println("Line " + line);
        String[] split = line.split("\t");
        
        // 1	Jamaica	Jamaica	NNP	13	instructed	instruct	nsubj
        int position = Integer.parseInt(split[0])-1;
        String word = split[1];
        String lemma  = split[2];
        String pos = split[3];
        if (split[4].equals("_")||split[4].equals("-1")) {
            split[4] = "-1";
        }
        int gov = Integer.parseInt(split[4])-1;
        
        String depType = split[7].toLowerCase();
        TerminalToken terminalToken = new TerminalToken(position, word, lemma, pos, gov, depType);
        
        //System.out.println("-> " + terminalToken.toStringPWDep());
        return terminalToken ;
    }

    public List<TerminalToken> getNextSentence() throws IOException {
        if (!hasNext) {
            return null;
        }
        String line;
        List<TerminalToken> tokenList = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            if (line.trim().length() != 0) {
                tokenList.add(fromLineRawParse(line));
            } else {
                break;
            }
        }
        if (line != null) {
            hasNext = true;
        } else {
            br.close();
            hasNext = false;
        }
        if(tokenList.size()>0){
            return tokenList;
        }else{
            //try to read the next sentence if it exists
            return getNextSentence();
        }
        

    }

   public void close() throws IOException{
       this.br.close();
   }
    
}
