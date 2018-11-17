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
import frameinduction.settings.Settings;
import frameinduction.grammar.SymbolFractory;
import mhutil.HelperLearnerMethods;
import input.preprocess.FragmentGeneration;
import input.preprocess.ProcessDependencyParses;
import input.preprocess.VertFileReader;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.DepandantNode;
import input.preprocess.objects.Sentence;
import input.preprocess.objects.TerminalToken;
import input.preprocess.objects.TerminalType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;


/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 * Modified grammar out of the discussion with Jackie and Laura Here w want to
 * assume assumption for each of the roles
 */
public class GenGrammarRoleFixedIndeOneHPClus {

    // static boolean debugVerbose = false;
    int numRules;
    int startOffsetForFrameHeads = 0;
    RuleMaps ruleMaps;
    boolean debugVerbose = false;
    Map<String, Integer> dependencyTypeTOIdentifierMap;

    public GenGrammarRoleFixedIndeOneHPClus(Map<String, Integer> dependencyTypeTOIdentifierMap) {
        ruleMaps = new RuleMaps();
        this.dependencyTypeTOIdentifierMap = dependencyTypeTOIdentifierMap;
    }

    public GenGrammarRoleFixedIndeOneHPClus(Map<String, Integer> dependencyTypeTOIdentifierMap, int startOffsetForFrameHeads) {
        ruleMaps = new RuleMaps();
        this.dependencyTypeTOIdentifierMap = dependencyTypeTOIdentifierMap;
        this.startOffsetForFrameHeads = startOffsetForFrameHeads;
    }

    private void genRulesFromStartSymbol(int frameCardinality) throws Exception {
        // gen rules from start symbol --> when there are more than one slot to be filled

        String startSymbol = SymbolFractory.getStartSymbol();
        for (int frameIdentifier = 0 + startOffsetForFrameHeads; frameIdentifier < frameCardinality + startOffsetForFrameHeads; frameIdentifier++) {
            String headFrameSymbol = SymbolFractory.getHeadFrameSymbol(frameIdentifier);
            String frameRemainingArguments = SymbolFractory.getFrameRemainingArguments(frameIdentifier);
            ruleMaps.addRule(startSymbol, headFrameSymbol, frameRemainingArguments);
            if (debugVerbose) {
                System.out.format("%s\t%s\t%s\n", startSymbol, headFrameSymbol, frameRemainingArguments);
            }
        }

    }

    private void genRulesForFrameHead(int frameCardinality) throws Exception {
        for (int frameIdentifier = 0 + startOffsetForFrameHeads; frameIdentifier < frameCardinality + startOffsetForFrameHeads; frameIdentifier++) {
            String headFrameSymbolLHS = SymbolFractory.getHeadFrameSymbol(frameIdentifier);
            String rhs1 = SymbolFractory.getLhsForUnaryHeadRole(frameIdentifier);
            String predefinedAssumedDepTypeForHead = SymbolFractory.getHeadVerbDependencyTerminal();
            String rhs2 = SymbolFractory.getUnaryDepHead(predefinedAssumedDepTypeForHead);
            // String[] rhs = SymbolFractory.getCombinedRuledStringRHS(rhs1, rhs2);
            ruleMaps.addRule(headFrameSymbolLHS, rhs1, rhs2);
            if (debugVerbose) {
                System.out.format("%s\t%s\t%s\n", headFrameSymbolLHS, rhs1, rhs2);
            }
        }

    }

    private void addRuleForVerbTerminalToLHS(Set<String> allVerbialHeads) throws Exception {

        //  for (String verb : allVerbialHeads) {
        Iterator<String> iterator = allVerbialHeads.iterator();
        for (int i = 0; iterator.hasNext(); i++) {

            String lhsVerb = SymbolFractory.getLhsForUnaryHeadRole(i + startOffsetForFrameHeads);
                // String[] rhs = {verb,null};

            //System.err.println("Was here!");
            String verb = iterator.next();
            ruleMaps.addRule(lhsVerb, verb);
            if (debugVerbose) {
                System.out.println(lhsVerb + "->" + verb);
            }
        }
        //}

        String depRootTerminalRHS = SymbolFractory.getHeadVerbDependencyTerminal();
        String depRootNonTerminal = SymbolFractory.getUnaryDepHead(depRootTerminalRHS);
        if (debugVerbose) {
            System.out.println(depRootNonTerminal + "->" + depRootTerminalRHS);
        }
        ruleMaps.addRule(depRootNonTerminal, depRootTerminalRHS);

    }

//    private void genRulesForFrameReminderArguments(int frameCardinality, Set<String> depTypes) throws Exception {
//        System.out.println("Generating rules for roles");
//        // binary rules for frame remaining, i.e., when we have exactly two frame slots
//        for (int frameIdentifier = 0; frameIdentifier < frameCardinality; frameIdentifier++) {
//            String frameRemainingArguments = SymbolFractory.getFrameRemainingArguments(frameIdentifier);
//            for (String dep : depTypes) {
//                //      for (int i = 0; i < semanticRoleCardinality; i++) {
//                String frameArgument1 = SymbolFractory.getFramesJSntxArgument(frameIdentifier, dep);
//                ruleMaps.addRule(frameRemainingArguments, frameArgument1, frameRemainingArguments);
//                if (debugVerbose) {
//                    System.out.format("%s\t%s\t%s\n", frameRemainingArguments, frameArgument1, frameRemainingArguments);
//                }
//
//            }
//        }
//        // rules for frame remaining in combination with one of the slot fillers
//        // I am not completely satisfied if this rule is enough .. I guess this let us have combinations of two slot fillers of the same type
//        for (int frameIdentifier = 0; frameIdentifier < frameCardinality; frameIdentifier++) {
//            String frameRemainingArguments = SymbolFractory.getFrameRemainingArguments(frameIdentifier);
//            String eosSymbol = SymbolFractory.getEOSSymbol();
//            //for (int semRoleIdentifier = 0; semRoleIdentifier < semanticRoleCardinality; semRoleIdentifier++) {
//            for (String depType : depTypes) {
//
//                String frameArgument1 = SymbolFractory.getFramesJSntxArgument(frameIdentifier, depType);
//
//                ruleMaps.addRule(frameRemainingArguments, frameArgument1, eosSymbol);
//                if (debugVerbose) {
//                    System.out.format("%s\t%s\t%s\n", frameRemainingArguments, frameArgument1, eosSymbol);
//                }
//            }
//            //}
//        }
//    }
    private void genRuleFromFrameFillerToTerminals(int frameCardinality,
            //int semanticRoleCardinality, 
            Map<String, Set<String>> depTypeSetLexicalizations) throws Exception {
        // maybe add another parameter for number of Ts here
//        Set<String> depTypeSorted = new TreeSet<>(depTypeSetLexicalizations.keySet());
        String eosSymbol = SymbolFractory.getEOSSymbol();
        SortedSet<String> depOrderedToUse = new TreeSet<>(depTypeSetLexicalizations.keySet());
        for (int frameIdentifier = 0 + this.startOffsetForFrameHeads; frameIdentifier < frameCardinality + startOffsetForFrameHeads; frameIdentifier++) {
            String frameRemainingArguments = SymbolFractory.getFrameRemainingArguments(frameIdentifier);
            for (String dep : depOrderedToUse) {
                //      for (int i = 0; i < semanticRoleCardinality; i++) {
                String frameArgument1 = SymbolFractory.getFramesJSntxArgument(frameIdentifier, dep);
                ruleMaps.addRule(frameRemainingArguments, frameArgument1, frameRemainingArguments);
                ruleMaps.addRule(frameRemainingArguments, frameArgument1, eosSymbol);
                if (debugVerbose) {
                    System.out.format("%s\t%s\t%s\n", frameRemainingArguments, frameArgument1, frameRemainingArguments);
                }

            }
        }

        // the assumption is that each role_id represents words from one syntactic relation.
        for (int frameIdentifier = 0 + startOffsetForFrameHeads; frameIdentifier < frameCardinality + startOffsetForFrameHeads; frameIdentifier++) {
            // int roleCounterBasedOnDependeny = 0; // each role represents lexicalization of only one type of dependency

            for (String depType : depOrderedToUse) {
                Integer depID = this.dependencyTypeTOIdentifierMap.get(depType);
                String frameArgumentLHS = SymbolFractory.getFramesJSntxArgument(frameIdentifier, depType);

                String unaryDepHead = SymbolFractory.getUnaryDepHead(depType);
                //            for (int si = 0; si < semanticRoleCardinality; si++) {
                // for (int semRoleIdentifier = 0; semRoleIdentifier < depTypeSetLexicalizations.size(); semRoleIdentifier++) {
                String unaryHeadForNonVerbial = SymbolFractory.getUnaryLHSForNonVerbialRole(depID);
                ruleMaps.addRule(frameArgumentLHS, unaryHeadForNonVerbial, unaryDepHead);
                if (debugVerbose) {
                    System.err.format("%s\t%s\t%s\n", frameArgumentLHS, unaryHeadForNonVerbial, unaryDepHead);
                }
                //}
                // roleCounterBasedOnDependeny++;
            }

        }
        if (debugVerbose) {
            System.err.println("Now adding unary lexicalization rules for roles...");
        }
        //int roleCounterBasedOnDependeny = 0;
        for (String depType : depOrderedToUse) {
            Integer depID = this.dependencyTypeTOIdentifierMap.get(depType);
            String unaryHeadForNonVerbial = SymbolFractory.getUnaryLHSForNonVerbialRole(depID);
            Set<String> depLexicalization = depTypeSetLexicalizations.get(depType);
            if (debugVerbose) {
                System.err.println("There are " + depLexicalization.size() + " words to add  ");
            }
            for (String word : depLexicalization) {
                ruleMaps.addRule(unaryHeadForNonVerbial, word);
                if (debugVerbose) {
                    System.err.println(unaryHeadForNonVerbial + " -> " + word);
                }
            }

            //}
            // roleCounterBasedOnDependeny++;
        }

    }

    private void genDepUnaryRules(Set<String> depTypeSet) throws Exception {
        for (String dep : depTypeSet) {
            String unaryDepNoneTerminalLhs = SymbolFractory.getUnaryDepHead(dep);
            // String[] rhs = {dep,null};

            ruleMaps.addRule(unaryDepNoneTerminalLhs, dep);
            if (debugVerbose) {
                System.out.println(unaryDepNoneTerminalLhs + "->" + dep);
            }
//            if (binaryRuleMap.containsKey(unaryDepNoneTerminalLhs)) {
//                binaryRuleMap.get(unaryDepNoneTerminalLhs).add(rhs);
//            } else {
//                Set<String[]> rhss = new HashSet();
//                rhss.add(rhs);
//                binaryRuleMap.put(unaryDepNoneTerminalLhs, rhss);
//            }
        }

    }

//    private void genUnaryRulesForRolesToWords(int semanticRoleCardinalit, Set<String> allTerminals) throws Exception {
//
//        allTerminals.stream().//parallel().
//                forEach(word -> {
//                    for (int si = 0; si < semanticRoleCardinalit; si++) {
//                        try {
//                            String unaryHeadForNonVerbialLhs = SymbolFractory.getUnaryLHSForNonVerbialRole(si);
//                            ruleMaps.addRule(unaryHeadForNonVerbialLhs, word);
//                            if (debugVerbose) {
//                                System.out.println(unaryHeadForNonVerbialLhs + " -> " + word);
//                            }
//                        } catch (Exception ex) {
//                            System.err.println("Generation Ex " + ex);
//                            Logger.getLogger(GenGrammarRoleFixedIndeOneHPClus.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
//                });
//    }
    public void genRules(Collection<Fragment> fragmentList) throws Exception {
        Set<String> headSet = ConcurrentHashMap.newKeySet();
       // Set<String> depandantSet = ConcurrentHashMap.newKeySet();
        //   Set<String> depRelationSet = ConcurrentHashMap.newKeySet();

        //fragmentList.stream().parallel().forEach(fg->{
        Map<String, Set<String>> dependencyLexicalizations = new ConcurrentHashMap<>();
        //fragmentList.stream().parallel().forEach(fg->{
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

        Iterator<String> iterator = dependencyLexicalizations.keySet().iterator();
        while (iterator.hasNext()) {
            String nextDep = iterator.next();
            if (!this.dependencyTypeTOIdentifierMap.containsKey(nextDep)) {
                System.err.println("Removed " + nextDep + " arguments -> not allowed by the settins.");
                iterator.remove();
            }

        }

        int frameCardinality = headSet.size();

        genRulesFromStartSymbol(frameCardinality);
//        // generates only S->F_i^h Rem_i^
        genRulesForFrameHead(frameCardinality);
        addRuleForVerbTerminalToLHS(headSet);

        //  genRulesForFrameReminderArguments(frameCardinality, depRelationSet);
        genRuleFromFrameFillerToTerminals(frameCardinality, dependencyLexicalizations);
        genDepUnaryRules(dependencyLexicalizations.keySet());

//        genUnaryRulesForRolesToWords(semanticRoleCardinality, dependencyLexicalizations.keySet());
        // add the special case of EOS
        ruleMaps.addRule(SymbolFractory.getEOSSymbol(), SymbolFractory.getEOSTerminalSymb());
        if (debugVerbose) {
            System.out.println(SymbolFractory.getEOSSymbol() + " -> " + SymbolFractory.getEOSTerminalSymb());
        }

        // binary rules for frame remaining, i.e., when we have exactly two frame slots
    }

//    public RuleMaps getTheRuleMap() throws Exception {
//        ruleMaps.buildReverseRandomIndices();
//        return ruleMaps;
//    }
    public RuleMaps getTheRuleMap(Collection<Fragment> inputFramentList) throws Exception {
        ruleMaps.buildReverseRandomIndices();
        RulesCounts collectUsedRulesX = HelperGrammarLearnMethods.collectUsedRulesInValidParses(ruleMaps, inputFramentList);

       // System.err.println("->The rulemap size BEFORE BR:" + ruleMaps.getCountBinaryRules() + " UR:" + ruleMaps.getCountUnaryRules());
        //int beforeFRVxXX = ruleMaps.getFrameHeadSymbolIDSet().size();
        ruleMaps.updateParameters(collectUsedRulesX);
        //int afterFRVxXX = ruleMaps.getFrameHeadSymbolIDSet().size();
       // System.err.println("->The rulemap size AFTER BR:" + ruleMaps.getCountBinaryRules() + " UR:" + ruleMaps.getCountUnaryRules());
      //  System.err.println("->>>>>>>>Number of frame FRV changed from " + beforeFRVxXX + "  to  " + afterFRVxXX);

        return ruleMaps;
    }

    public RuleMaps getRawRuleMaps() {
        return ruleMaps;
    }

    public static void main(String[] args) throws IOException, Exception {
        String vertFile2 = "minimal.prs.vert";
        VertFileReader vrf = new VertFileReader(vertFile2);
        List<TerminalToken> nextSentence;
        List<Fragment> fragmentsAll = new ArrayList<>();
        int count = 0;
        Settings settings = new Settings();
        while ((nextSentence = vrf.getNextSentence()) != null) {
            if (count++ > 1) {
                break;
            }
            ProcessDependencyParses.normalizePronouns(nextSentence);
            ProcessDependencyParses.normalizeDepTypes(nextSentence);
            Sentence sent = new Sentence(nextSentence);
            // System.out.println("sent " + sent.toString());
            FragmentGeneration fg = new FragmentGeneration(sent, null, null, settings.getActiveDepr(), settings.getTargetPos());
            List<Fragment> fragments = fg.getFragments();
            fragmentsAll.addAll(fragments);
            //System.out.println("Sent: " + nextSentence.toString());
            //System.out.println(fragments.size());
//            for (Fragment f : fragments) {
//                System.out.println("- " + f.toStringTrype());
//            }

        }
        //   fragmentsAll=   fragmentsAll.subList(0, 2);
        for (Fragment fg : fragmentsAll) {
            System.out.println("!->! " + fg.toStringPosition());
        }
//        System.out.println("Fragment size: " + fragmentsAll.size());
//        //GenerateGrammar gg = new GenerateGrammarCaseGrammar();
        GenGrammarRoleFixedIndeOneHPClus gg = new GenGrammarRoleFixedIndeOneHPClus(settings.getActiveDepToIntMap(), 8);
        gg.debugVerbose = false;
        gg.genRules(fragmentsAll);
        RuleMaps theRuleMap = gg.getTheRuleMap(fragmentsAll);
//        
        Collection<ParseFrame> bestParseUsingCYK = HelperParseMethods.doParsingToBestFrames(fragmentsAll, theRuleMap);
        for (ParseFrame ps : bestParseUsingCYK) {
            System.out.println(ps);
        }
        theRuleMap.seralizaRules("test-file-rules-star.zip");
        RulesCounts estimateParameters = HelperLearnerMethods.estimateParameters(theRuleMap, fragmentsAll);
        theRuleMap.updateParameters(estimateParameters);
        Collection<ParseFrame> bestParseUsingCYK2 = HelperParseMethods.doParsingToBestFrames(fragmentsAll, theRuleMap);
    }
}
