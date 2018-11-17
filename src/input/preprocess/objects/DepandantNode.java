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

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class DepandantNode {

    
    private String positionInSent;
    private  String terminalString;
    private final TerminalType type;

    public DepandantNode(String symbol, TerminalType type, String positionInSent) {
        this.terminalString = symbol;
        this.type = type;
        this.positionInSent = positionInSent;
    }

    public String getTerminalString() {
        return terminalString;
    }

    public TerminalType getType() {
        return type;
    }

    public String getPositionInSent() {
//        try {
//            int val = Integer.parseInt(positionInSent);
//        } catch (Exception e) {
//            System.err.println(e);
//        }

        return positionInSent;
    }
    
    public void changeTerminalString(String newStr){
        this.terminalString= newStr;
    }

    @Override
    public String toString() {
        return terminalString+"-:-"+positionInSent+"-:-"+type;
    }

    void setPositionInSentence(String i) {
        this.positionInSent=i;
    }
    
    
    
    
}
