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
package input.preprocess.objects;

import frameinduction.grammar.RuleMaps;
import java.util.List;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class FragmentCompact {

    int[] terminals;
    String[] realPositionsInSentence; // this added to generate CoNLL like output, the cost is storing the array
    public FragmentCompact(Fragment f, RuleMaps rm) throws Exception {
        List<DepandantNode> terminalsNodes = f.getTerminals();
        terminals = new int[terminalsNodes.size()];
        realPositionsInSentence = new String[terminalsNodes.size()];
        for (int i = 0; i < terminalsNodes.size(); i++) {
            DepandantNode depNode = terminalsNodes.get(i);
            terminals[i] = rm.getIDFromSymbol(depNode.getTerminalString());
            realPositionsInSentence[i]= depNode.getPositionInSent();
        }
    }

    public FragmentCompact(int[] terminals) {
        this.terminals = terminals;
    }

    public String[] getPositionsInSentence() {
        return realPositionsInSentence;
    }

  

    public int[] getTerminals() {
        return terminals;
    }

    public String toHumanReadable(RuleMaps rm) throws Exception{
        StringBuilder str = new StringBuilder();
         for (int terminal :terminals) {
         str.append(rm.getSymbolFromID(terminal)).append(" ");
            
        }
        return str.toString();
    }
//    public String toStringTrype() {
//        StringBuilder sb = new StringBuilder();
//        for(DepandantNode tn :  terminals){
//            sb.append(tn.getTerminalString()).append(" ").append(tn.getType()).append(" ");
//        }
//        return sb.toString();
//    }
//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        for(DepandantNode tn :  terminals){
//            sb.append(tn.getTerminalString()).append(" ");
//        }
//        return sb.toString();
//    }
}
