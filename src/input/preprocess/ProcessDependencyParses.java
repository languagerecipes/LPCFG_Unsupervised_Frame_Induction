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

import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.Sentence;
import input.preprocess.objects.SimpleSentence;
import input.preprocess.objects.TerminalToken;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 * 
 */
public class ProcessDependencyParses {

//    public static void main(String[] args) throws IOException {
//        VertFileReader vrf = new VertFileReader("../lr_other/stanford-parse.txt");
//        List<TerminalToken> nextSentence = null;
//        Settings settings = new Settings();
//        int count = 0;
//        while ((nextSentence = vrf.getNextSentence()) != null) {
//
//            SimpleSentence simpleSentence = new SimpleSentence(nextSentence);
//            simpleSentence.print();
//            System.out.println("ne " + nextSentence.size());
//            // collapseMWEs(nextSentence);
//            simpleSentence.print();
//            normalizePronouns(nextSentence);
//            simpleSentence.print();
//            Sentence s = new Sentence(nextSentence);
//            FragmentGeneration fg = new FragmentGeneration(s, null, null, null, settings.getTargetPos());
//            System.out.println(" frag ---------");
//            for (Fragment frag : fg.getFragments()) {
//                System.out.println("frag " + frag.toString());
//            }
//            System.out.println("\n===\n");
//            if (++count > 6) {
//                break;
//            }
//        }
//
//    }

    public static void removeWordsThatAreOnlyDepandant(List<TerminalToken> nextSentence) {

        for (int i = 0; i < nextSentence.size(); i++) {
            TerminalToken token = nextSentence.get(i);
            boolean isCompound = isCompund(token.getDepType());
            if (isCompound) {

                int governor = token.getItsHead();
                nextSentence.get(governor - 1).addCompundToken(token.getWord());
                nextSentence.set(i, null);
            }
        }
    }

    public static void collapseMWEs(List<TerminalToken> nextSentence) {

        for (int i = 0; i < nextSentence.size(); i++) {
            TerminalToken token = nextSentence.get(i);
            boolean isCompound = isCompund(token.getDepType());
            if (isCompound) {
                int governor = token.getItsHead();
                nextSentence.get(governor - 1).addCompundToken(token.getWord());
                nextSentence.set(i, null);
            }
        }
        for (int i = 0; i < nextSentence.size(); i++) {
            TerminalToken token = nextSentence.get(i);
            if (token != null) {
                token.replaceWordByCompond();
            }
        }
    }

    public static void getDeepSyntaxTypes(List<TerminalToken> nextSentence) {

        boolean hasPassive = false;
        for (int i = 0; i < nextSentence.size(); i++) {
            TerminalToken token = nextSentence.get(i);
            if (token != null) {
                String dep = token.getDepType();
                String[] split = dep.split("\\|");
                if (split.length > 1) {
                    
                    token.setDepType(split[1]);
                }
            }
        }
    }

    public static void normalizeDepTypes(List<TerminalToken> nextSentence) {

        boolean hasPassive = false;
        for (int i = 0; i < nextSentence.size(); i++) {
            TerminalToken token = nextSentence.get(i);
            if (token != null) {
                String dep = token.getDepType();
                if (dep.equals("nsubjpass")) {
                    token.setDepType("dobj");
                    hasPassive = true;
                } else if (dep.equals("nmod:by")) {
                    if (hasPassive) {
                        token.setDepType("nsubj");
                    } 
                } else {
//                if(dep.startsWith("acl")){
//                    token.setLemma("A_MODIFER_FRAME:"+token.getLemma());
//                    
//                }
//                else if(dep.startsWith("ccomp")){
//                    token.setLemma("A_C_FRAME:"+token.getLemma());
//                }
//                else if(dep.startsWith("xcomp")){
//                    token.setLemma("A_C_FRAME:"+token.getLemma());
//                }

//                if(dep.equals("nsubjpass")){
//                    token.setDepType("dobj");
//                }
                }
            }
        }
    }

    public static void normalizePronouns(List<TerminalToken> nextSentence) {

        for (int i = 0; i < nextSentence.size(); i++) {
            TerminalToken token = nextSentence.get(i);
            if (token != null) {
                String pos = token.getPos();
                if (pos.endsWith("PRP")) {
                    if (token.getWord().equalsIgnoreCase("it")) {
                        token.setLemma("Something");
                    } else if (token.getWord().equalsIgnoreCase("them") || token.getWord().equalsIgnoreCase("they")) {
                        token.setLemma("Something/Someone");
                    } else {
                        token.setLemma("Someone");
                    }
                } else if (pos.endsWith("WP")) {
                    token.setLemma("Someone");
                }
            }
        }
    }

    public static void normalizeNumbers(List<TerminalToken> nextSentence) {

        for (int i = 0; i < nextSentence.size(); i++) {
            TerminalToken token = nextSentence.get(i);
            if (token != null) {
                String pos = token.getPos();
              if(token.getWord().matches("(\\d+|\\d+\\.\\d+|\\d+\\,\\d+|\\d+\\/\\d+)")){
                   token.setLemma("$NUMBER$");
              }else if(token.getWord().matches("\\%")){
                   token.setLemma("$NUMBER$");
              }else if(token.getWord().matches("\\$")){
                   token.setLemma("$NUMBER$");
              }
                    

            }
        }
    }
    
    private static boolean isCompund(String depType) {
        if (depType.startsWith("compound") || depType.startsWith("mwe")) {
            return true;
        } else {
            return false;
        }
    }

}
