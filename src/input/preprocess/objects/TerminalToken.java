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
public class TerminalToken {

    private final int position;
    private String word;
    private String lemma;
    private final String pos;
    private final int itsHead;
    private String depType;
    private List<String> compound = null;
    

    public void replaceWordByCompond() {
        if (this.compound != null) {
            if (this.pos.startsWith("V")) {
                String sx = "";
                for (String s : compound) {
                    sx += ":" + s;
                }
                this.word += sx;
            } else {
                this.word += ":hmc";
            }
        }
    }

    
    public static TerminalToken fromLineOld(String line) {
        //System.out.println("Line " + line);
        String[] split = line.split("\t");

        // 2	understood	understand	VBD	0	_	understand	ROOT
        int position = Integer.parseInt(split[0]);
        String word = split[1];
        String lemma  = split[2];
        String pos = split[3];
        if (split[4].equals("_")) {
            split[4] = "-1";
        }
        int gov = Integer.parseInt(split[4]);
      
        String depType = split[5].toLowerCase();
        TerminalToken terminalToken = new TerminalToken(position, word, lemma, pos, gov, depType);
        
        //System.out.println("-> " + terminalToken.toStringPWDep());
        return terminalToken ;
    }

    public static TerminalToken fromLine(String line) {
        //System.out.println("Line " + line);
        String[] split = line.split("\t");

        //1	Pierre	Pierre	PROPN	NNP	_	2	compound	_	_
        int position = Integer.parseInt(split[0])-1;
        String word = split[1];
        String lemma  = split[2];
        String pos = split[4];
        if (split[6].equals("_")||split[6].equals("0")) {
            split[6] = "-1";
        }
        int gov = Integer.parseInt(split[6])-1;
      
        String depType = split[7].toLowerCase();
        TerminalToken terminalToken = new TerminalToken(position, word, lemma, pos, gov, depType);
        
        //System.out.println("-> " + terminalToken.toStringPWDep());
        return terminalToken ;
    }

    
    /**
     * Method used for the French file
     * @param line
     * @return 
     */
     public static TerminalToken fromLineDeepSyntax(String line) {
        //System.out.println("Line " + line);
        String[] split = line.split("\t");

        //1	Pierre	Pierre	PROPN	NNP	_	2	compound	_	_
        int position = Integer.parseInt(split[0])-1;
        String word = split[1];
        String lemma  = split[2];
        String pos = split[4];
        if (split[6].equals("_")||split[6].equals("0")) {
            split[6] = "-1";
        }
        
        if(split[6].contains("|")){
           
            split[6]=split[6].split("\\|")[1];
        }
        int gov = Integer.parseInt(split[6])-1
                ;
      
        String depType = split[7].toLowerCase();
        if(depType.contains("|")){
            depType = depType.split("\\|")[1];
        }
        TerminalToken terminalToken = new TerminalToken(position, word, lemma, pos, gov, depType);
        
        //System.out.println("-> " + terminalToken.toStringPWDep());
        return terminalToken ;
    }
     
     
    public void setWord(String word) {
        this.word = word;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }
 



    public void addCompundToken(String compound) {
        if (this.compound != null) {
            this.compound.add(compound);
        } else {
            this.compound = new ArrayList<>();
            this.compound.add(compound);
        }

    }

    public TerminalToken(int position, String word, String lemma, String pos, int depItsHead, String depType) {
        this.position = position;
        this.word = word;
        this.lemma = lemma;
        this.pos = pos;
        this.itsHead = depItsHead;
        this.depType = depType;
    }

    /**
     * Use a set of allowed terminal lists and/or use filers
     * If a set is null then no filter will be applied 
     * @param allowedLemma
     * @param filteredLemma
     * @return 
     */
    

    public String getDepType() {
        return depType;
    }

    public int getItsHead() {
        return itsHead;
    }

    public String getLemma() {
        return lemma;
    }

    public String getPos() {
        return pos;
    }

    public int getPosition() {
        return position;
    }

    public String getWord() {
        return word;
    }

//    public static TerminalToken fromLine(String line) {
//        String[] split = line.split("\\s+");
//        int position = Integer.parseInt(split[3]);
//        int itsHead = Integer.parseInt(split[4]);
//        String word = split[0];
//        String pos = split[1];
//        String lemma = split[2];
//        String depR = split[5];
//        TerminalToken t = new TerminalToken(position, word, lemma, pos, itsHead, depR);
//        return t;
//    }

    public void setDepType(String depType) {
        this.depType = depType;
    }


//    public static TerminalToken fromLine2(String line) {
//        String[] split = line.split("\\s+");
//        int position = Integer.parseInt(split[0]);
//        int itsHead = Integer.parseInt(split[4]);
//        String word = split[1];
//        String pos = split[3];
//        String lemma = split[2];
//        String depR = split[5].toLowerCase();
//        TerminalToken t = new TerminalToken(position, word, lemma, pos, itsHead, depR);
//        return t;
//    }

    public String toStringWordPosition() {
        return position + "\t" + word;
    }

    public String toStringAnnotated() {
        return position + "\t" + word + "\t" + pos+"\t"+depType;
    }
    public String toStringPWDep() {
        return position + "\t" + word + "\t" + depType;
    }

    public String toString() {
        return word + " ";
    }
    
    public String toStringConll() {
        int headPositionToWrite =this.itsHead+1;
        if("root".equals(depType)){
            headPositionToWrite=0;
        }
        return (position+1) + "\t" + word + "\t" + lemma+ "\t" + pos+"\t"+pos+"\t_\t"+headPositionToWrite+"\t"+depType+"\t_\t_";
    }
}
