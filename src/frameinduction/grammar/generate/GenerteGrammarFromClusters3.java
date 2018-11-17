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
package frameinduction.grammar.generate;

//import frameinduction.Settings;
import frameinduction.grammar.RuleMaps;

import frameinduction.grammar.parse.HelperParseMethods;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.grammar.SymbolFractory;
import frameinduction.grammar.parse.CNFRule;

import frameinduction.grammar.parse.io.SemRolePair;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.DepandantNode;
import input.preprocess.objects.TerminalToken;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import mhutil.HelperFragmentMethods;
import mhutil.HelperParseChartIO;



/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 * Modified grammar out of the discussion with Jackie and Laura Here w want to
 * assume assumption for each of the roles
 */
public class GenerteGrammarFromClusters3 {

    // static boolean debugVerbose = false;
    int numRules;
    RuleMaps ruleMaps;
    boolean debugVerbose = false;

    Map<String, Set<String>> terminalTypesAndInstances;

    //final Map<String, Integer> dependencyToIntegerKeyMapX;

    public GenerteGrammarFromClusters3(   Map<String, Integer> finalDependencyMapForGrammarGen
            ) {
        ruleMaps = new RuleMaps();
        terminalTypesAndInstances = new HashMap<>();
//        this.dependencyToIntegerKeyMap =  finalDependencyMapForGrammarGen;
    }

    private void genRulesFromStartSymbol(Set<Integer> frameCardinality) throws Exception {
        // gen rules from start symbol --> when there are more than one slot to be filled

        String startSymbol = SymbolFractory.getStartSymbol();
        addThisTerminalAndItsType("S", startSymbol);

        for (int frameIdentifier : frameCardinality) {
            String headFrameSymbol = SymbolFractory.getHeadFrameSymbol(frameIdentifier);
            String frameRemainingArguments = SymbolFractory.getFrameRemainingArguments(frameIdentifier);
            //addThisTerminalAndItsType("headFrameSymbol", headFrameSymbol);
            //addThisTerminalAndItsType("frameRemainingArguments", headFrameSymbol);
            ruleMaps.addRule(startSymbol, headFrameSymbol, frameRemainingArguments);
            if (debugVerbose) {
                System.out.format("%s\t%s\t%s\n", startSymbol, headFrameSymbol, frameRemainingArguments);
            }
        }

    }

    private void addRuleForVerbTerminalToLHS(Map<Integer, Set<String>> allClusteredVerbialHeads) throws Exception {
        for (int i : allClusteredVerbialHeads.keySet()) {
            String lhsVerb = SymbolFractory.getLhsForUnaryHeadRole(i);
            Set<String> verbSet = allClusteredVerbialHeads.get(i);
            for (String verb : verbSet) {
                ruleMaps.addRule(lhsVerb, verb);
                if (debugVerbose) {
                    System.out.println(lhsVerb + "->" + verb);
                }
            }
        }
    }

    private void addThisTerminalAndItsType(String type, String terminal){
            if(this.terminalTypesAndInstances.containsKey(type)){
                terminalTypesAndInstances.get(type).add(terminal);
            }else{
                Set<String> ins =new HashSet<>();
                ins.add(terminal);
                terminalTypesAndInstances.put(type, ins);
            }
    }
    private void genRulesForFrameHead(Set<Integer> frameCardinality) throws Exception {
        for (int frameIdentifier : frameCardinality) {
            String headFrameSymbol = SymbolFractory.getHeadFrameSymbol(frameIdentifier);
            String unaryHeadRole = SymbolFractory.getLhsForUnaryHeadRole(frameIdentifier);
            String predefinedAssumedDepTypeForHead = SymbolFractory.getHeadVerbDependencyTerminal();
            String unaryDepHead = SymbolFractory.getUnaryDepHead(predefinedAssumedDepTypeForHead);
            // String[] rhs = SymbolFractory.getCombinedRuledStringRHS(rhs1, rhs2);
            addThisTerminalAndItsType("UnaryHeadRole", unaryHeadRole);
            addThisTerminalAndItsType("UnaryDepHead", unaryDepHead);
            
            ruleMaps.addRule(headFrameSymbol, unaryHeadRole, unaryDepHead);
            if (debugVerbose) {
                System.out.format("%s\t%s\t%s\n", headFrameSymbol, unaryHeadRole, unaryDepHead);
            }
        }

    }

    
    
   
    
//    public Map<String, Integer> getDependencyToIntegerKeyMap() {
//        return new HashMap<>( dependencyToIntegerKeyMap);
//    }
    
    /**
     * Check dependencies are allowed
     * @param fragmentList 
     */
    private void checkDependencies(Collection<Fragment> fragmentList) {
        // dependencyToIntegerKeyMap = new HashMap<>();

        for (Fragment fg : fragmentList) {
            List<DepandantNode> terminals = fg.getTerminals();
            //   DepandantNode get = terminals.get(terminals.size()-1);
            for (int i = 0; i < terminals.size(); i++) {
                DepandantNode tn = terminals.get(i);

                if (null != tn.getType()) {
                    switch (tn.getType()) {
                        case DependancyRelation:
                            String depType = tn.getTerminalString();
                            //if (!dependencyToIntegerKeyMap.containsKey(depType)) {
                            //    throw new RuntimeException("A dependency appears in your input which is not allowed by the settings. code:647363");
                            //}
                            break;
                        default:
                            break;
                    }
                }

            }

        }

    }
    
    public void genRules(Collection<Fragment> fragmentCollection, Collection<ParseFrame> parsedFrames) throws Exception {
        Map<Long, ParseFrame> parseListToIDMap = HelperParseChartIO.parseListToIDMap(parsedFrames);
        Map<Long, Fragment> fragmentIDMap = HelperFragmentMethods.fragmentIDMap(fragmentCollection);
        if(fragmentIDMap.size()!=parseListToIDMap.size()){
            System.err.println("HERe we have a problemn --------------------------------");
        }
        // check we got everything
        int sizeKeyPF = parseListToIDMap.keySet().size();
        parseListToIDMap.keySet().retainAll(fragmentIDMap.keySet());
        if (parseListToIDMap.keySet().size() != sizeKeyPF) {
            throw new RuntimeException("Fragment map does not contain all the required instances code:fg365267");
        }
        Map<Integer, Set<String>> headSetClusterMap = createHeadClusterMap(parsedFrames);
        genRulesFromStartSymbol(headSetClusterMap.keySet());
        addRuleForVerbTerminalToLHS(headSetClusterMap);
        checkDependencies(fragmentCollection);
        genRulesForFrameHead(headSetClusterMap.keySet());
        for (Map.Entry<Long, Fragment> fragmentEntry : fragmentIDMap.entrySet()) {
            
            Long key = fragmentEntry.getKey();
            Fragment fragment = fragmentEntry.getValue();
            ParseFrame parseFrame = parseListToIDMap.get(key);
            if(parseFrame==null){
                System.err.println(">>>>>>>4328647837777777777777777777777777777777777777777777777777777777777777777");
            }
            int frameClusterNumber = parseFrame.getFrameClusterNumber();
            List<String> toDependencyStringList = fragment.getDependencyStringList(false);
            List<SemRolePair> semanticRoles = parseFrame.getSemanticRoles();
            String head = parseFrame.getHead();
             String get = toDependencyStringList.get(toDependencyStringList.size()-1);
//            if(get.equals("EOS_Terminal")){
//                System.out.println("dj723647toDependencyStringList " + toDependencyStringList.size() + "  "+ semanticRoles.size());
//                }
            if (toDependencyStringList.size() != semanticRoles.size()) {
              //  String get = toDependencyStringList.get(toDependencyStringList.size()-1);
             
            
                System.out.println(parseFrame.toStringStd() +' '+fragment.toString() );
                throw new RuntimeException("SemRole size is different than Dep size " +toDependencyStringList.size()+" vs "+ semanticRoles.size() );
            }
            String frameRemainingArguments = SymbolFractory.getFrameRemainingArguments(frameClusterNumber);
            String unaryHeadRole = SymbolFractory.getLhsForUnaryHeadRole(frameClusterNumber);
            ruleMaps.addRule(unaryHeadRole, head, true);
            for (int i = 0; i < toDependencyStringList.size(); i++) {
                String dep = toDependencyStringList.get(i);
                String unaryDepHead = SymbolFractory.getUnaryDepHead(dep);
                SemRolePair srl = semanticRoles.get(i);
                String word = srl.getLexicalization();
                String roleLabelPreTerminal = srl.getSemRoleID();
               // System.err.println(roleLabelPreTerminal);
                String frameArgument1 = SymbolFractory.getFramesJSntxArgument(frameClusterNumber, dep);
                ruleMaps.addRule(frameRemainingArguments, frameArgument1, frameRemainingArguments, true);
                ruleMaps.addRule(frameRemainingArguments, frameArgument1, SymbolFractory.getEOSSymbol(), true);
                ruleMaps.addRule(frameArgument1, roleLabelPreTerminal, unaryDepHead, true);
                ruleMaps.addRule(roleLabelPreTerminal, word, true);
                ruleMaps.addRule(unaryDepHead, dep, true);
            }
        }
///////////////////////// add rules for the root stuff which I still cannot make much of sense out of it
        ruleMaps.addRule(SymbolFractory.getEOSSymbol(), SymbolFractory.getEOSTerminalSymb());
        if (debugVerbose) {
            System.err.println(SymbolFractory.getEOSSymbol() + " -> " + SymbolFractory.getEOSTerminalSymb());
        }
           String depRootTerminalRHS = SymbolFractory.getHeadVerbDependencyTerminal();
        String depRootNonTerminal = SymbolFractory.getUnaryDepHead(depRootTerminalRHS);
        if (debugVerbose) {
            System.out.println(depRootNonTerminal + "->" + depRootTerminalRHS);
        }
        ruleMaps.addRule(depRootNonTerminal, depRootTerminalRHS);
////////////////////////////

        ruleMaps.buildReverseRandomIndicesEqual();
    
    }

    
   
    
    public RuleMaps getTheRuleMap() throws Exception {
        
        return ruleMaps;
    }

    public static void main(String[] args) throws IOException, Exception {
        String vertFile2 = "minimal.prs.vert";
        //VertFileReader vrf = new VertFileReader(vertFile2);
        List<TerminalToken> nextSentence;
        //List<Fragmenat> loadFragments = FragmentIOUtils.loadFragments(vertFile2);
        List<Fragment> fragmentsAll = 
                HelperFragmentMethods.loadFragments("../lr/ro-hunger-grames/hg-fragments.txt");
        
        Collection<ParseFrame> loadParseFrameFromFile = 
                HelperParseChartIO.loadParseFrameFromFile("../lr/ro-hunger-grames/4-iteration-frames.txt");
//        System.out.println("Fragment size: " + fragmentsAll.size());
//        //GenerateGrammar gg = new GenerateGrammarCaseGrammar();
        Settings s = new Settings();
        GenerteGrammarFromClusters3 gg = new GenerteGrammarFromClusters3(s.getActiveDepToIntMap());
        gg.debugVerbose = true;
        gg.genRules(fragmentsAll,loadParseFrameFromFile);
        RuleMaps theRuleMap = gg.getTheRuleMap();

        System.err.println("Now building ... ");
        theRuleMap.buildWeightMapsFromReverse();
        theRuleMap.seralizaRules("testing-rulemaps.zip");
//        Collection<ParseFrame> bestParseUsingCYK = HelperParseMethods.doParsingToBestFrames(fragmentsAll, theRuleMap);
//        for (ParseFrame ps : bestParseUsingCYK) {
//            System.out.println(ps);
//        }
        //     theRuleMap.seralizaRules("test-file-rules-star.zip");
        List<Future<Collection<CNFRule>>> bestParseUsingCYK = 
                HelperParseMethods.bestParseUsingCYK(fragmentsAll, theRuleMap, 2);
    }

    private Map<Integer, Set<String>> createHeadClusterMap(Collection<ParseFrame> parsedFrames) {
        Map<Integer, Set<String>> theMap = new HashMap<>();
        for (ParseFrame pf : parsedFrames) {
            int frameClusterNumber = pf.getFrameClusterNumber();
            if (theMap.containsKey(frameClusterNumber)) {
                Set<String> get = theMap.get(frameClusterNumber);
                get.add(pf.getHead());
            } else {
                Set<String> get = new HashSet<>();
                get.add(pf.getHead());
                theMap.put(frameClusterNumber, get);
            }
        }
        return theMap;
    }
}
