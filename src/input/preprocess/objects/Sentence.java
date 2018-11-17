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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class Sentence {

    private String wsjPSDID;
    private List<TerminalToken> sentenceTerminals;

    public Sentence(List<TerminalToken> sentenceTerminals) {
        this.sentenceTerminals = new ArrayList<>();
        for (TerminalToken tt : sentenceTerminals) {
            if (tt != null) {
                this.sentenceTerminals.add(tt);
            }
        }

    }

     public Sentence(List<TerminalToken> sentenceTerminals, String id) {
        this.sentenceTerminals = new ArrayList<>();
        for (TerminalToken tt : sentenceTerminals) {
            if (tt != null) {
                this.sentenceTerminals.add(tt);
            }
        }
        this.wsjPSDID=id;

    }
    public void setSentenceTerminals(List<TerminalToken> sentenceTerminals) {
        this.sentenceTerminals = sentenceTerminals;
    }

    public void setWsjPSDID(String wsjPSDID) {
        this.wsjPSDID = wsjPSDID;
    }

    public String getWsjPSDID() {
        return wsjPSDID;
    }

    public Sentence() {

        this.sentenceTerminals = new ArrayList<>();

        // depRel = new HashSet();
    }

    public void addNextTerminal(TerminalToken t) {
        sentenceTerminals.add(t);

        //depRel.add(t.getDepType());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.sentenceTerminals.size(); i++) {
            sb.append(sentenceTerminals.get(i).toString());
        }
        return sb.toString();
    }

    public String toStringAnnotated() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.sentenceTerminals.size(); i++) {
            sb.append(sentenceTerminals.get(i).toStringAnnotated()).append("\n");
        }
        return sb.toString();
    }

    public String toStringCoNLL() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.sentenceTerminals.size(); i++) {
            sb.append(sentenceTerminals.get(i).toStringConll()).append("\n");
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return sentenceTerminals.isEmpty();
    }

    public List<TerminalToken> getSentenceTerminals() {
        return sentenceTerminals;
    }

}
