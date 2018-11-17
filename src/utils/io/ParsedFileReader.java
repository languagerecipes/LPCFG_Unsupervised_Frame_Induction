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
package utils.io;



import input.preprocess.objects.Sentence;
import input.preprocess.objects.TerminalToken;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class ParsedFileReader {

    private BufferedReader br;
    private String nextLine;
    private boolean hasNext;

    public ParsedFileReader(String file) throws FileNotFoundException, IOException {
        br = new BufferedReader(new FileReader(file));
        hasNext = true;
//       String l;
//        if ((l=br.readLine()) != null) {
//            hasNext = true;
//            System.err.println("Reading " + l);
//        } else {
//            hasNext = false;
//        }
    }

    public Sentence getNextSentence() throws IOException, Exception {
        if (!hasNext) {
            return null;
        }
        String line;
        if ((line = br.readLine()) != null) {
            Sentence sentence = new Sentence();
            if(line.startsWith("#")){
                sentence.setWsjPSDID(line.trim());
            }else{
                throw new Exception("Expected sentenceID");
            }
            while ((line = br.readLine()) != null) {
                if (line.trim().length()==0) {
                    hasNext = true;
                     return sentence;
                    //break;
                } else {
                    sentence.addNextTerminal(TerminalToken.fromLine(line));
                }
                
            }
             return sentence;
           
        } else {
            
            br.close();
            hasNext = false;
            return null;
        }

    }

    public void close() throws IOException {
        this.br.close();
    }

    @Override
    protected void finalize() throws Throwable {
        br.close();
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
    }

    
    

    
    
    
   
}
