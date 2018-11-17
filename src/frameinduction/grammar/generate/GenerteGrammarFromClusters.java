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
import frameinduction.grammar.RulesCounts;
import mhutil.HelperGrammarLearnMethods;
import frameinduction.grammar.parse.HelperParseMethods;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.grammar.SymbolFractory;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.DepandantNode;
import input.preprocess.objects.TerminalToken;
import input.preprocess.objects.TerminalType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mhutil.HelperFragmentIOUtils;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 * Modified grammar out of the discussion with Jackie and Laura Here w want to
 * assume assumption for each of the roles
 */
public class GenerteGrammarFromClusters {

    // static boolean debugVerbose = false;
    int numRules;
    RuleMaps ruleMaps;
    boolean debugVerbose = false;

    Map<String, Set<String>> terminalTypesAndInstances;

    final Map<String, Integer> dependencyToIntegerKeyMap;

    public GenerteGrammarFromClusters(   Map<String, Integer> finalDependencyMapForGrammarGen
            ) {
        ruleMaps = new RuleMaps();
        terminalTypesAndInstances = new HashMap<>();
        this.dependencyToIntegerKeyMap =  finalDependencyMapForGrammarGen;
    }

    private void genRulesFromStartSymbol(Set<Integer> frameCardinality) throws Exception {
        // gen rules from start symbol --> when there are more than one slot to be filled

        String startSymbol = SymbolFractory.getStartSymbol();
        addThisTerminalAndItsType("S", startSymbol);
 
        for (int frameIdentifier : frameCardinality) {
            String headFrameSymbol = SymbolFractory.getHeadFrameSymbol(frameIdentifier);
            String frameRemainingArguments = SymbolFractory.getFrameRemainingArguments(frameIdentifier);
            addThisTerminalAndItsType("headFrameSymbol", headFrameSymbol);
            addThisTerminalAndItsType("frameRemainingArguments", headFrameSymbol);
            ruleMaps.addRule(startSymbol, headFrameSymbol, frameRemainingArguments);
            if (debugVerbose) {
                System.out.format("%s\t%s\t%s\n", startSymbol, headFrameSymbol, frameRemainingArguments);
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
    
    private void genRulesForFrameReminderArguments(Map<Integer, Map<String, Set<String>>> forClusterdependencyLexicalizations, Map<String, Integer> depTypesMap) throws Exception {
        for (int frameIdentifier : forClusterdependencyLexicalizations.keySet()) {
            String frameRemainingArguments = SymbolFractory.getFrameRemainingArguments(frameIdentifier);
            Map<String, Set<String>> getDepSetFOrThis = forClusterdependencyLexicalizations.get(frameIdentifier);
            for (String dep : getDepSetFOrThis.keySet()) {
//                Integer depId = depTypesMap.get(dep);
//                //      for (int i = 0; i < semanticRoleCardinality; i++) {
                String frameArgument1 = SymbolFractory.getFramesJSntxArgument(frameIdentifier, dep);
                ruleMaps.addRule(frameRemainingArguments, frameArgument1, frameRemainingArguments);
                if (debugVerbose) {
                    System.out.format("%s\t%s\t%s\n", frameRemainingArguments, frameArgument1, frameRemainingArguments);
                }
            }
        }
        // rules for frame remaining in combination with one of the slot fillers
        // I am not completely satisfied if this rule is enough .. I guess this let us have combinations of two slot fillers of the same type
        for (int frameIdentifier : forClusterdependencyLexicalizations.keySet()) {
            String frameRemainingArguments = SymbolFractory.getFrameRemainingArguments(frameIdentifier);
            String eosSymbol = SymbolFractory.getEOSSymbol();
            //for (int semRoleIdentifier = 0; semRoleIdentifier < semanticRoleCardinality; semRoleIdentifier++) {
            Map<String, Set<String>> getDepSetFOrThis = forClusterdependencyLexicalizations.get(frameIdentifier);
            for (String dep : getDepSetFOrThis.keySet()) {
                String frameArgument1 = SymbolFractory.getFramesJSntxArgument(frameIdentifier, dep);

                ruleMaps.addRule(frameRemainingArguments, frameArgument1, eosSymbol);
                if (debugVerbose) {
                    System.out.format("%s\t%s\t%s\n", frameRemainingArguments, frameArgument1, eosSymbol);
                }
            }
            //}
        }
    }

    private void genRuleFromFrameFillerToTerminals(
            Map<Integer, Map<String, Set<String>>> forClusterdependencyLexicalizations,
            Map<String, Integer> depTypesMap, Map<String, Set<String>> depLexicalizationMapSet) throws Exception {

        for (int frameIdentifier : forClusterdependencyLexicalizations.keySet()) {

            for (String depType : forClusterdependencyLexicalizations.get(frameIdentifier).keySet()) {
                
                int roleCounterBasedOnDependeny = depTypesMap.get(depType);
                String frameArgumentLHS = SymbolFractory.getFramesJSntxArgument(frameIdentifier, depType);
                String unaryDepHead = SymbolFractory.getUnaryDepHead(depType);
                //            for (int si = 0; si < semanticRoleCardinality; si++) {
                // for (int semRoleIdentifier = 0; semRoleIdentifier < depTypeSetLexicalizations.size(); semRoleIdentifier++) {
                String unaryHeadForNonVerbial = SymbolFractory.getUnaryLHSForNonVerbialRole(roleCounterBasedOnDependeny);
                ruleMaps.addRule(frameArgumentLHS, unaryHeadForNonVerbial, unaryDepHead);
                if (debugVerbose) {
                    System.err.format("%s\t%s\t%s\n", frameArgumentLHS, unaryHeadForNonVerbial, unaryDepHead);
                }
            }

        }
        for (String depTString : depTypesMap.keySet()) {
            int depID = depTypesMap.get(depTString);
            if(depLexicalizationMapSet.containsKey(depTString)){
            Set<String> depLexicalization = depLexicalizationMapSet.get(depTString);
            String unaryHeadForNonVerbial = SymbolFractory.getUnaryLHSForNonVerbialRole(depID);
            if (debugVerbose) {
                System.err.println("There are " + depLexicalization.size() + " words to add  ");
            }
            for (String word : depLexicalization) {

                ruleMaps.addRule(unaryHeadForNonVerbial, word,true);
                if (debugVerbose) {
                    System.err.println(unaryHeadForNonVerbial + " -> " + word);
                }
            }}
        }

    }

    private void genDepUnaryRules(Set<String> depTypeSet) throws Exception {
        for (String dep : depTypeSet) {
            String unaryDepNoneTerminalLhs = SymbolFractory.getUnaryDepHead(dep);
            ruleMaps.addRule(unaryDepNoneTerminalLhs, dep,true);
            if (debugVerbose) {
                System.out.println(unaryDepNoneTerminalLhs + "->" + dep);
            }
        }

    }


    
    public Map<String, Integer> getDependencyToIntegerKeyMap() {
        return new HashMap<>( dependencyToIntegerKeyMap);
    }
    
    private void creatDependnecyMap(Map<String, Collection<Fragment>> fragmentListClusterMap){
       // dependencyToIntegerKeyMap = new HashMap<>();

        //int dependencyMapCounter = 1;
        for (String cluster : fragmentListClusterMap.keySet()) {
            Collection<Fragment> fragmentList = fragmentListClusterMap.get(cluster);
            for (Fragment fg : fragmentList) {
                List<DepandantNode> terminals = fg.getTerminals();
                //   DepandantNode get = terminals.get(terminals.size()-1);
                for (int i = 0; i < terminals.size(); i++) {
                    DepandantNode tn = terminals.get(i);

                    if (null != tn.getType()) {
                        switch (tn.getType()) {
                            case DependancyRelation:
                                
                                String depType = tn.getTerminalString();
                                if (!dependencyToIntegerKeyMap.containsKey(depType)) {
//                                    dependencyToIntegerKeyMap.put(depType,
//                                           dependencyMapCounter 
//                                    );
                                    
                                    throw new RuntimeException(
                                            "A dependency appears in your input which is not "
                                                    + "allowed by the settings. code:647363 " + depType);
                                  //  dependencyToIntegerKeyMap.put(depType, dependencyMapCounter++);
                                }
                                break;
                            default:
                                break;
                        }
                    }

                }
                
            }
        }

    }
    
    public void genRules(Map<String, Collection<Fragment>> fragmentListClusterMap) throws Exception {
        Map<Integer, Set<String>> headSetClusterMap = new ConcurrentHashMap<>();
        Map<Integer, Map<String, Set<String>>> forClusterdependencyLexicalizations = new ConcurrentHashMap<>();
       
        Map<String, Set<String>> depLexicalization = new HashMap<>();
        creatDependnecyMap(fragmentListClusterMap);
        //fragmentList.stream().parallel().forEach(fg->{
        //int clusterCounter = 1;
        
        for (String cluster : fragmentListClusterMap.keySet()) {
            int clusterCounter= Integer.parseInt(cluster);
            Collection<Fragment> fragmentList = fragmentListClusterMap.get(cluster);
            Set<String> headSet = ConcurrentHashMap.newKeySet();
            headSetClusterMap.put(clusterCounter, headSet);
            Map<String, Set<String>> dependencyLexicalizations = new ConcurrentHashMap<>();
            forClusterdependencyLexicalizations.put(clusterCounter, dependencyLexicalizations);

            for (Fragment fg : fragmentList) {
                List<DepandantNode> terminals = fg.getTerminals();
                //   DepandantNode get = terminals.get(terminals.size()-1);
                for (int i = 0; i < terminals.size(); i++) {
                    DepandantNode tn = terminals.get(i);

                    if (null != tn.getType()) {
                        switch (tn.getType()) {
                            case HEAD:

                                headSet.add(tn.getTerminalString());
                                break;
                            case Argument:
                                DepandantNode tnDep = terminals.get(i + 1);
                                String argumentLexicalization = tn.getTerminalString();
                                if (tnDep.getType() != TerminalType.DependancyRelation) {
                                    
                                    throw new RuntimeException("ERROR! " + tnDep.getType() );
                                }
                                String depType = tnDep.getTerminalString();
                                if (depLexicalization.containsKey(depType)) {
                                    depLexicalization.get(depType).add(argumentLexicalization);
                                } else {
                                    Set<String> lexicalization = ConcurrentHashMap.newKeySet();
                                    lexicalization.add(argumentLexicalization);
                                    //System.err.println(argumentLexicalization);
                                    Set<String> putIfAbsent = depLexicalization.putIfAbsent(depType, lexicalization);
                                    if (putIfAbsent != null) {
                                        System.err.println("--- eriiirioru");
                                    }

                                }
                                if (dependencyLexicalizations.containsKey(depType)) {
                                    dependencyLexicalizations.get(depType).add(argumentLexicalization);
                                } else {
                                    Set<String> lexicalization = ConcurrentHashMap.newKeySet();
                                    lexicalization.add(argumentLexicalization);
                                    //System.err.println(argumentLexicalization);
                                    Set<String> putIfAbsent = dependencyLexicalizations.putIfAbsent(depType, lexicalization);
                                    if (putIfAbsent != null) {
                                        putIfAbsent.add(argumentLexicalization);
                                    }
                                }
                                i++;
                                //depandantSet.add(tn.getTerminalString());
                                break;
//                        case DependancyRelation:
//                            depRelationSet.add(tn.getTerminalString());
//                            break;
                            default:
                                break;
                        }
                    }

                }
                //});
            }
        }
        // int frameCardinality = headSet.size();

        genRulesFromStartSymbol(headSetClusterMap.keySet());

        genRulesForFrameHead(headSetClusterMap.keySet());
        addRuleForVerbTerminalToLHS(headSetClusterMap);
/////////////////////////
        String depRootTerminalRHS = SymbolFractory.getHeadVerbDependencyTerminal();
        String depRootNonTerminal = SymbolFractory.getUnaryDepHead(depRootTerminalRHS);
        if (debugVerbose) {
            System.out.println(depRootNonTerminal + "->" + depRootTerminalRHS);
        }
        ruleMaps.addRule(depRootNonTerminal, depRootTerminalRHS);
////////////////////////////
        genRulesForFrameReminderArguments(
                forClusterdependencyLexicalizations, dependencyToIntegerKeyMap);
        genRuleFromFrameFillerToTerminals(
                forClusterdependencyLexicalizations, dependencyToIntegerKeyMap, depLexicalization);
        genDepUnaryRules(dependencyToIntegerKeyMap.keySet());
//
////        genUnaryRulesForRolesToWords(semanticRoleCardinality, dependencyLexicalizations.keySet());
//        // add the special case of EOS
        ruleMaps.addRule(SymbolFractory.getEOSSymbol(), SymbolFractory.getEOSTerminalSymb());
        if (debugVerbose) {
            System.err.println(SymbolFractory.getEOSSymbol() + " -> " + SymbolFractory.getEOSTerminalSymb());
        }

        // binary rules for frame remaining, i.e., when we have exactly two frame slots
      //  System.err.println("here...");
    ruleMaps.buildReverseRandomIndicesEqual();
     // System.err.println("Removing redundant (over-generated) rules ");
      List<Fragment> fgAll = new ArrayList<>();
      fragmentListClusterMap.values().forEach(list->{
          fgAll.addAll(list);
      });
        RulesCounts collectUsedRulesX = HelperGrammarLearnMethods.collectUsedRulesInValidParses(ruleMaps, fgAll);
        //System.err.println("->The rulemap size BEFORE BR:" + ruleMaps.getCountBinaryRules() + " UR:" + ruleMaps.getCountUnaryRules());
       // int beforeFRVxXX = ruleMaps.getFrameHeadSymbolIDSet().size();
      //  ruleMaps.updateParameters(collectUsedRulesX);
        ruleMaps.filterRulesEqualWeight(collectUsedRulesX);
        ruleMaps.normalizeToOne();
        //int afterFRVxXX = ruleMaps.getFrameHeadSymbolIDSet().size();
       // System.err.println("->The rulemap size AFTER BR:" + ruleMaps.getCountBinaryRules() + " UR:" + ruleMaps.getCountUnaryRules());
       // System.err.println("->>>>>>>>Number of frame FRV changed from " + beforeFRVxXX + "  to  " + afterFRVxXX);
    
    }

    
    public <T> void  genRules(
            double probSumTo,
            Map<T, Collection<Fragment>> fragmentListClusterMap, 
            Map<String, Integer> dependencyToIntegerKeyMap, 
            int startFrameIDCounter) throws Exception {
//        this.dependencyToIntegerKeyMap = dependencyToIntegerKeyMap;
        //System.err.println("Dep map size is " +dependencyToIntegerKeyMap.size());
        Map<Integer, Set<String>> headSetClusterMap = new ConcurrentHashMap<>();
        Map<Integer, Map<String, Set<String>>> forClusterdependencyLexicalizations = new ConcurrentHashMap<>();
        Map<String, Set<String>> depLexicalization = new HashMap<>();
        
        //fragmentList.stream().parallel().forEach(fg->{
        //int clusterCounter = 1;
        
        for (T cluster : fragmentListClusterMap.keySet()) {
            //int clusterCounter= Integer.parseInt(cluster);
            int clusterCounter = startFrameIDCounter++;
            Collection<Fragment> fragmentList = fragmentListClusterMap.get(cluster);
            Set<String> headSet = ConcurrentHashMap.newKeySet();
            headSetClusterMap.put(clusterCounter, headSet);
            Map<String, Set<String>> dependencyLexicalizations = new ConcurrentHashMap<>();
            forClusterdependencyLexicalizations.put(clusterCounter, dependencyLexicalizations);

            for (Fragment fg : fragmentList) {
                List<DepandantNode> terminals = fg.getTerminals();
                //   DepandantNode get = terminals.get(terminals.size()-1);
                for (int i = 0; i < terminals.size(); i++) {
                    DepandantNode tn = terminals.get(i);

                    if (null != tn.getType()) {
                        switch (tn.getType()) {
                            case HEAD:

                                headSet.add(tn.getTerminalString());
                                break;
                            case Argument:
                                DepandantNode tnDep = terminals.get(i + 1);
                                String argumentLexicalization = tn.getTerminalString();
                                if (tnDep.getType() != TerminalType.DependancyRelation) {
                                    throw new RuntimeException("ERROR!");
                                }
                                String depType = tnDep.getTerminalString();
                                if (depLexicalization.containsKey(depType)) {
                                    depLexicalization.get(depType).add(argumentLexicalization);
                                } else {
                                    Set<String> lexicalization = ConcurrentHashMap.newKeySet();
                                    lexicalization.add(argumentLexicalization);
                                    //System.err.println(argumentLexicalization);
                                    Set<String> putIfAbsent = depLexicalization.putIfAbsent(depType, lexicalization);
                                    if (putIfAbsent != null) {
                                        System.err.println("--- eriiirioru");
                                    }

                                }
                                if (dependencyLexicalizations.containsKey(depType)) {
                                    dependencyLexicalizations.get(depType).add(argumentLexicalization);
                                } else {
                                    Set<String> lexicalization = ConcurrentHashMap.newKeySet();
                                    lexicalization.add(argumentLexicalization);
                                    //System.err.println(argumentLexicalization);
                                    Set<String> putIfAbsent = dependencyLexicalizations.putIfAbsent(depType, lexicalization);
                                    if (putIfAbsent != null) {
                                        putIfAbsent.add(argumentLexicalization);
                                    }
                                }
                                i++;
                                //depandantSet.add(tn.getTerminalString());
                                break;
//                        case DependancyRelation:
//                            depRelationSet.add(tn.getTerminalString());
//                            break;
                            default:
                                break;
                        }
                    }

                }
                //});
            }
        }
        // int frameCardinality = headSet.size();

        genRulesFromStartSymbol(headSetClusterMap.keySet());

        genRulesForFrameHead(headSetClusterMap.keySet());
        addRuleForVerbTerminalToLHS(headSetClusterMap);
/////////////////////////
        String depRootTerminalRHS = SymbolFractory.getHeadVerbDependencyTerminal();
        String depRootNonTerminal = SymbolFractory.getUnaryDepHead(depRootTerminalRHS);
        if (debugVerbose) {
            System.out.println(depRootNonTerminal + "->" + depRootTerminalRHS);
        }
        ruleMaps.addRule(depRootNonTerminal, depRootTerminalRHS,true);
////////////////////////////
        genRulesForFrameReminderArguments(
                forClusterdependencyLexicalizations, dependencyToIntegerKeyMap);
        genRuleFromFrameFillerToTerminals(forClusterdependencyLexicalizations, dependencyToIntegerKeyMap, depLexicalization);
        genDepUnaryRules(dependencyToIntegerKeyMap.keySet());
//
////        genUnaryRulesForRolesToWords(semanticRoleCardinality, dependencyLexicalizations.keySet());
//        // add the special case of EOS
        ruleMaps.addRule(SymbolFractory.getEOSSymbol(), SymbolFractory.getEOSTerminalSymb(),true);
        if (debugVerbose) {
            System.err.println(SymbolFractory.getEOSSymbol() + " -> " + SymbolFractory.getEOSTerminalSymb());
        }

        // binary rules for frame remaining, i.e., when we have exactly two frame slots
    
    ruleMaps.buildReverseRandomIndices(probSumTo);
     // System.err.println("Removing redundant (over-generated) rules ");
      List<Fragment> fgAll = new ArrayList<>();
      fragmentListClusterMap.values().forEach(list->{
          fgAll.addAll(list);
      });
        RulesCounts collectUsedRulesX = HelperGrammarLearnMethods.collectUsedRulesInValidParses(ruleMaps, fgAll);
      //  System.err.println("->The rulemap size BEFORE BR:" + ruleMaps.getCountBinaryRules() + " UR:" + ruleMaps.getCountUnaryRules());
       // int beforeFRVxXX = ruleMaps.getFrameHeadSymbolIDSet().size();
        ruleMaps.updateParameters(collectUsedRulesX);
      //  int afterFRVxXX = ruleMaps.getFrameHeadSymbolIDSet().size();
       // System.err.println("->The rulemap size AFTER BR:" + ruleMaps.getCountBinaryRules() + " UR:" + ruleMaps.getCountUnaryRules());
       // System.err.println("->>>>>>>>Number of frame FRV changed from " + beforeFRVxXX + "  to  " + afterFRVxXX);
    
    }

//    public RuleMaps getRawRuleMaps(){
//      return ruleMaps;
//    }
    
    public RuleMaps getTheRuleMap() throws Exception {
        
        return ruleMaps;
    }

    public static void main(String[] args) throws IOException, Exception {
        String vertFile2 = "minimal.prs.vert";
        //VertFileReader vrf = new VertFileReader(vertFile2);
        List<TerminalToken> nextSentence;
        //List<Fragmenat> loadFragments = HelperFragmentIOUtils.loadFragments(vertFile2);
        Map<String, Collection<Fragment>> fragmentsAll = HelperFragmentIOUtils.loadParseFramesAsFragmentsToClusterMap(
                new File("../hac-clustering-result/data/syseval/system-evaluation-file--0.6-59"));
//        System.out.println("Fragment size: " + fragmentsAll.size());
//        //GenerateGrammar gg = new GenerateGrammarCaseGrammar();
        Settings s = new Settings();
        GenerteGrammarFromClusters gg = new GenerteGrammarFromClusters(s.getActiveDepToIntMap());
        gg.debugVerbose = true;
        gg.genRules(fragmentsAll);
        RuleMaps theRuleMap = gg.getTheRuleMap();

        System.err.println("Now building ... ");
        theRuleMap.buildWeightMapsFromReverse();
//        Collection<ParseFrame> bestParseUsingCYK = HelperParseMethods.doParsingToBestFrames(fragmentsAll, theRuleMap);
//        for (ParseFrame ps : bestParseUsingCYK) {
//            System.out.println(ps);
//        }
        //     theRuleMap.seralizaRules("test-file-rules-star.zip");

        for (Collection<Fragment> fTop : fragmentsAll.values()) {

            // RulesCounts estimateParameters = HelperLearnerMethods.estimateParameters(theRuleMap, fTop);
//        theRuleMap.updateParameters(estimateParameters);
            for (Fragment d : fTop) {
               // System.out.println(d);
                ParseFrame doParsingToBestFrame = HelperParseMethods.doParsingToBestFrame(d, theRuleMap);
            }
        }
    }
}
