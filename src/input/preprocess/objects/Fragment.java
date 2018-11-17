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

import frameinduction.grammar.SymbolFractory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class Fragment {

    public static final int  DEP_NODE_PER_WORD =4; // this can be changed, if new features are added
    //String head;
    private List<DepandantNode> terminals;
    // private int sentenceID;
    private String fragmentOriginID;

//    public String getHead() {
//        return head;
//    }
    public Fragment(List<DepandantNode> terminals, String fragmentOriginSentID) {
        this.terminals = Collections.synchronizedList(terminals);
        this.fragmentOriginID = fragmentOriginSentID;
    }

    public Fragment(List<DepandantNode> terminals) {
        this.terminals = Collections.synchronizedList(terminals);
    }

    public Fragment(String fragmentOriginSentID) {
        this.fragmentOriginID = fragmentOriginSentID;
        this.terminals = Collections.synchronizedList(new ArrayList<DepandantNode>());
    }

    /**
     * Used in processing semeval data ... the string keys are converted to
     * longs and set here
     *
     * @param fragmentOriginID
     */
    public void setFragmentOriginID(String fragmentOriginID) {
        this.fragmentOriginID = fragmentOriginID;
    }

    public String getEvaluationID() {
        return this.fragmentOriginID + " " + terminals.get(0).getPositionInSent();
    }

    public long getLongID() {
        if(true){
        return UUID.nameUUIDFromBytes((terminals.get(0).getPositionInSent()+fragmentOriginID).getBytes()).getMostSignificantBits();
        }
        String formattedHead = String.format("%02d", Long.parseLong(
                terminals.get(0).getPositionInSent()
        )
        );
        String substring = fragmentOriginID.substring(1);
        //try{
        long uniqueIntID = Long.parseLong(substring + formattedHead);
        return uniqueIntID;
    }

    public List<DepandantNode> getTerminals() {
        return terminals;
    }

    public String getSentID() {
        return fragmentOriginID;
    }

    public String getHead() {
        return this.terminals.get(0).getTerminalString();
    }

    public String getHeadPosition() {
        return this.terminals.get(0).getPositionInSent();
    }

    public void addEOSNode() {
        terminals.add(new DepandantNode(SymbolFractory.getEOSTerminalSymb(), TerminalType.EOS, -1+""));
    }

    public String toStringTrype() {
        StringBuilder sb = new StringBuilder();
        for (DepandantNode tn : terminals) {
            sb.append(tn.getTerminalString()).append(" ").append(tn.getType()).append(" ");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DepandantNode tn : terminals) {
            sb.append(tn.getTerminalString()).append(" ");
        }
        return sb.toString();
    }

//    public int getPositionInSent() {
//        return sentenceID;
//    }
    public static Fragment fromTextLine(String line) {
        //#20001001 8 join.UNKNOWN Vinken-:-1-:-nsubj board-:-10-:-dobj director-:-14-:-nmod:as Nov.-:-15-:-nmod:tmod
        String[] split = line.split(" ");
        String idOrignin = split[0];
        List<DepandantNode> fragmentNodes = new ArrayList<>();
        
        DepandantNode head = new DepandantNode(split[2].split("\\.")[0], TerminalType.HEAD, split[1]);
        DepandantNode headRootRel = new DepandantNode("root", TerminalType.DependancyRelationHead, "-1");
        fragmentNodes.add(head);
        fragmentNodes.add(headRootRel);
        for (int i = 3; i < split.length; i++) {
            String[] bits = split[i].split("-:-");
            if (bits.length != 3) {
                System.out.println("HERE!!!!! DEBUG " + line);
            } else {
                    //int pos=Integer.parseInt(bits[1].split("_")[0]);
                fragmentNodes.add(new DepandantNode(bits[0], TerminalType.Argument,bits[1]));// Integer.parseInt(bits[1]) ));
                fragmentNodes.add(new DepandantNode(bits[2], TerminalType.DependancyRelation, "-1"));

            }
        }

        Fragment f = new Fragment(fragmentNodes, idOrignin);
        f.addEOSNode();
        return f;

    }

    public void modifyHeadRelationDepency(int integerToEndAtTheEndofSymbol) {
        this.terminals.forEach(d -> {
            if (d.getType().equals(TerminalType.DependancyRelationHead)) {
                d.changeTerminalString(d.getTerminalString() + integerToEndAtTheEndofSymbol);
            }
        });
    }

    /**
     * We have to make sure that the input fragment does not contain symbols
     * that are used in the rules the problem occurs when ParseFrame (the parsed
     * output of a fragment) is used as an input, to remedy the problem I have
     * added the follwing pattern checkings... quiet messy solution
     *
     * @param line
     * @return
     */
    public static Fragment fromTextLineOfParsedFrame(String line) {
        //#20001001 8 join.UNKNOWN Vinken-:-1-:-nsubj board-:-10-:-dobj director-:-14-:-nmod:as Nov.-:-15-:-nmod:tmod

        String[] split = line.split(" ");
        String idOrignin = split[0];
        List<DepandantNode> fragmentNodes = new ArrayList<>();
        DepandantNode head = new DepandantNode(split[2].split("\\.")[0], TerminalType.HEAD,split[1]);// Integer.parseInt(split[1]));
        DepandantNode headRootRel = new DepandantNode("root", TerminalType.DependancyRelationHead, -1+"");
        fragmentNodes.add(head);
        fragmentNodes.add(headRootRel);
        for (int i = 3; i < split.length; i++) {
            String[] bits = split[i].split("-:-");
            if (bits.length != 3) {
                System.out.println("HERE!!!!! DEBUG " + line);
            } else {
                fragmentNodes.add(new DepandantNode(bits[0], TerminalType.Argument,bits[1]));// Integer.parseInt(bits[1])));
                if (bits[2].contains("_") && bits[2].contains("^")) {
                    bits[2] = bits[2].split("\\_")[1].split("\\^")[0];
                }
                fragmentNodes.add(new DepandantNode(bits[2], TerminalType.DependancyRelation, -1+""));

            }
        }

        Fragment f = new Fragment(fragmentNodes, idOrignin);
        f.addEOSNode();
        return f;

    }

    public String toStringPosition() {
        return toStringPosition("UNKNOWN");
    }

    /**
     * Returns a map view of gramatical relations, i.e., argPosition as key and
     * value being gramatical relation
     *
     * @return
     */
    public Map<String, String> toPositionSyntaxMap() {
        Map<String, String> grMap = new HashMap<>();
        for (int i = 2; i < terminals.size(); i++) {
            DepandantNode tn = terminals.get(i);

            if (tn.getType() == TerminalType.Argument //|| tn.getType() == TerminalType.HEAD
                    ) {
                String position = tn.getPositionInSent();
                DepandantNode get = terminals.get(++i); // the gr
                String grRelation = get.getTerminalString();
                grMap.put(position, grRelation);

            }

        }
        return grMap;
    }
    public String toStringPosition(String frameType) {
        StringBuilder sb = new StringBuilder();
        DepandantNode getHead = terminals.get(0);
        sb.append(this.fragmentOriginID).append(" ").append(getHead.getPositionInSent())
                .append(" ").append(getHead.getTerminalString()).append("." + frameType);
        for (int i = 2; i < terminals.size(); i++) {
            DepandantNode tn = terminals.get(i);

            if (tn.getType() == TerminalType.Argument //|| tn.getType() == TerminalType.HEAD
                    ) {
                sb.append(" ").append(tn.getTerminalString()).append("-:-").append(tn.getPositionInSent());
            } else if (tn.getType() == TerminalType.DependancyRelation //|| tn.getType() == TerminalType.Preposition
                    ) {
                sb.append("-:-").append(tn.getTerminalString());

            } else {
                sb.append(" ");
            }

        }
        return sb.toString();
    }

    public String toLexicalizedString() {
        StringBuilder sb = new StringBuilder();
        DepandantNode getHead = terminals.get(0);
        sb.append(this.fragmentOriginID).append("\t")
                .append(getHead.getTerminalString());
        for (int i = 2; i < terminals.size(); i++) {
            DepandantNode tn = terminals.get(i);

            if (tn.getType() == TerminalType.Argument //|| tn.getType() == TerminalType.HEAD
                    ) {
                sb.append(" ").append(tn.getTerminalString());
            }

        }
        return sb.toString();
    }

    public void addNode(DepandantNode dn) {
        this.terminals.add(dn);
    }

    public List<String> getLexicalizedSequence() {
        List<String> strList = new ArrayList<>();

        DepandantNode getHead = terminals.get(0);
        strList.add(getHead.getTerminalString());
       
        for (int i = 2; i < terminals.size(); i++) {
            DepandantNode tn = terminals.get(i);

            if (tn.getType() == TerminalType.Argument //|| tn.getType() == TerminalType.HEAD
                    ) {
                strList.add(tn.getTerminalString());
            }

        }
        return strList;
    }

    public String getDependencyString() {
        StringBuilder sb = new StringBuilder();
        DepandantNode getHead = terminals.get(0);
//        sb.append(this.fragmentOriginID).append("\t")
//                .append(getHead.getTerminalString());
        for (int i = 2; i < terminals.size(); i++) {
            DepandantNode tn = terminals.get(i);

            if (tn.getType() == TerminalType.DependancyRelation //|| tn.getType() == TerminalType.HEAD
                    ) {
                sb.append(" ").append(tn.getTerminalString());
            }
            
        }
        return sb.toString();
    }
    
    public List<String> getDependencyStringList(boolean endOfString) {
        List<String> list = new ArrayList<>();
        for (int i = 2; i < terminals.size(); i++) {
            DepandantNode tn = terminals.get(i);

            if (tn.getType() == TerminalType.DependancyRelation //|| tn.getType() == TerminalType.HEAD
                    ) {
                String terminalString = tn.getTerminalString();
                if(terminalString==null){
                    System.err.println("------------------------->");
                }
                
                list.add(terminalString);
            }

        }
        if (endOfString) {
            list.add(TerminalType.EOS.name());
        }
        return list;
    }

    
    public void setHeadPosition(String i) {
            this.terminals.get(0).setPositionInSentence(i);
    }

    public void addHeadAsDEP() {
        // remember positions are fixed to 0 and 1 for the head!
         terminals.add(2, new DepandantNode(this.getHead(), TerminalType.Argument, this.getHeadPosition()+"Fake"));
         terminals.add(3, new DepandantNode("HEAD-2-Head-Hack", TerminalType.DependancyRelation, this.getHeadPosition()+"Fake"));
    }
}
