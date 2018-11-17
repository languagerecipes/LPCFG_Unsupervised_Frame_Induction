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

import frameinduction.grammar.SymbolFractory;
import input.preprocess.objects.Sentence;
import input.preprocess.objects.TerminalType;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.DepandantNode;
import input.preprocess.objects.TerminalToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class FragmentGenerationNoStx implements IFragmentGeneration {

    private final static int MIN_FRAGMENT_LENGTH = 4; // min length is set to 4, i.e., fragments with at least one syntactic dependant are considered
    private final Sentence inputSentence;
    private final Set<String> allowedLemma, filteredLemma;
    private final Set<String> depRel;
    private List<Integer> listOfVerbialHeads = null;
    private List<List<Integer>> listOfListOfDepandantsToHead = null;
    private final String headPosPattern;


    
    public FragmentGenerationNoStx(Sentence inputSentence, Set<String> allowedLemma, 
            Set<String> filteredLemma, Set<String> depRel, String posPattern) {
        this.headPosPattern = posPattern;
        this.inputSentence = inputSentence;
        this.allowedLemma = allowedLemma;
        this.filteredLemma = filteredLemma;
        this.depRel = depRel;
        generateSentnceFragmentsIndices();
        
    }

    /**
     * Get the list of terminals given current settings (allowed, filterd, depRel)
     */
    private void generateSentnceFragmentsIndices() {
        listOfListOfDepandantsToHead = new ArrayList<>();
        this.listOfVerbialHeads = new ArrayList<>();
        // given allowed and filtered constraints, decide what words are verbial heads and add them to the list
        for (TerminalToken t : this.inputSentence.getSentenceTerminals()) {
            if (isVerbialHead(t)) {
                this.listOfVerbialHeads.add(t.getPosition());
            }
        }
        //for each of the decided heads, find their depandanrs
        for (int i = 0; i < listOfVerbialHeads.size(); i++) {
            int positionVerbalHead = listOfVerbialHeads.get(i);
            List<Integer> makeDepandantT_n = makeDepandantT_n(positionVerbalHead);
            listOfListOfDepandantsToHead.add(makeDepandantT_n);
        }
    }
    
    /**
     * Given constrains of allowedLemma and filteredLemma decides if a token is verbial head or not
     * @param t
     * @return 
     */
    private boolean isVerbialHead(TerminalToken t) {
        //if (t.getPos().startsWith("V")) {
        
        if(Pattern.matches(headPosPattern,t.getPos())){
            if (allowedLemma == null || allowedLemma.contains(t.getLemma())) {
                if (filteredLemma == null || !filteredLemma.contains(t.getLemma())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Given current depRel constraint, decide what is a dependant to the head and what is not.
     * @param verbalHeadPositions
     * @return 
     */
     private List<Integer> makeDepandantT_n(int verbalHeadPositions) {
        List<Integer> tintList = new ArrayList<Integer>();
        int sentenceSize = this.inputSentence.getSentenceTerminals().size();


         for (int i = 0; i < 2; i++) {
             if (verbalHeadPositions > i) {
                
                 TerminalToken tokenBefore = this.inputSentence.getSentenceTerminals().get(verbalHeadPositions - (i + 1));
                 tintList.add(tokenBefore.getPosition());
             }
         }

         for (int i = 1; i < 4; i++) {

             if (verbalHeadPositions < sentenceSize - i) {
                 TerminalToken tokeAfter = this.inputSentence.getSentenceTerminals().get(verbalHeadPositions + i);
                 tintList.add(tokeAfter.getPosition());
             }
         }
//       
        return tintList;
    }
     
    
    @Override
    public List<Fragment> getFragments() {

        List<Fragment> toParse = new ArrayList<>();
        for (int headVerbIndex = 0; headVerbIndex < listOfVerbialHeads.size(); headVerbIndex++) {
            
            List<DepandantNode> fragmentNodes = new ArrayList<>();

            String headWord = getTerminalAt(listOfVerbialHeads.get(headVerbIndex)).getLemma();
           // String headTerminalSymbol = SymbolFractory.getHeadVerbTerminal(headWord);
            String headVerbDependencyTerminal = SymbolFractory.getHeadVerbDependencyTerminal();
            
           // fragment.add(headTerminalSymbol);
            DepandantNode tn = new DepandantNode(headWord, TerminalType.HEAD, listOfVerbialHeads.get(headVerbIndex)+"");
            DepandantNode tnDep = new DepandantNode(headVerbDependencyTerminal,
                    TerminalType.DependancyRelationHead,0+"");
            
            fragmentNodes.add(tn);
            fragmentNodes.add(tnDep);
            
            List<Integer> t_nListDepandants = listOfListOfDepandantsToHead.get(headVerbIndex);
            for (int i = 0; i < t_nListDepandants.size(); i++) {
                int depPosition = t_nListDepandants.get(i);
                TerminalToken terminalAt = getTerminalAt(depPosition);
                String word = terminalAt.getLemma();
                String dep = "nmod"; // everything is just a modifier
                //String wordCanonicalForm = SymbolFractory.getSemanticRoleFillerTerminal(word);
                fragmentNodes.add(new DepandantNode(word, TerminalType.Argument, depPosition+""));
                fragmentNodes.add(new DepandantNode(dep, TerminalType.DependancyRelation,0+""));
                
            }      
                
           
            if (fragmentNodes.size() >= MIN_FRAGMENT_LENGTH) {
           
                Fragment fragment = new Fragment(fragmentNodes, this.inputSentence.getWsjPSDID());
                
                fragment.addEOSNode();
                toParse.add(fragment);
            }
        }
        return toParse;
    }
    
    private TerminalToken getTerminalAt(int i) {
        // note that here the position starts from 1!!!
        return this.inputSentence.getSentenceTerminals().get(i );
    }

  

}
