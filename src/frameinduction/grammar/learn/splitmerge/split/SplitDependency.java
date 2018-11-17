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
package frameinduction.grammar.learn.splitmerge.split;

import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.generate.GenerateGrammarFrameRoleIndepandantsJCLK;
import frameinduction.settings.Settings;
import frameinduction.grammar.SymbolFractory;
import input.preprocess.objects.Fragment;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import mhutil.HelperFragmentMethods;
import org.apache.commons.math3.util.FastMath;
import util.MapUtils;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class SplitDependency {

    private static final Logger LOGGER = Logger.getLogger(SplitDependency.class.getName());
    final private Map<String, Map<String, Double>> ruleMapUnaryRest;
    final private Map<String, Map<String, Double>> ruleMapUnaryToSplit;
    final private Map<String, Map<String, Map<String, Double>>> rulMapBinaryRest;
    final private Map<String, Map<String, Map<String, Double>>> ruleMapBinaryToSplit;
    final private RuleMaps ruleMapToManipulate; // this can be removed , to make sure test it

    final private int tgtSymbolID;
    final private int depLHSLargestCounterIndex;
    final private int newDepIndexCounter1;
    final private int newDepIndexCounter2;

    final private Map<String, String> symbolMappingMemory;

    /**
     * Complete mainintating datastructures to carry information about what has
     * been split to what, merged to what etc.
     *
     * @param stxDepNonTerminalID
     * @param rm
     * @throws Exception
     */
    public SplitDependency(String stxDepNonTerminalID, RuleMaps rm) throws Exception {
        this.tgtSymbolID = rm.getIDFromSymbol(stxDepNonTerminalID) ;
        symbolMappingMemory = Collections.synchronizedMap(new HashMap<>());
        //  String targetRole = SymbolFractory.getUnaryLHSForNonVerbial(roleIndex);
        //   System.out.println(targetRole);
        
        ruleMapUnaryRest = new ConcurrentHashMap<>();
        ruleMapUnaryToSplit = new ConcurrentHashMap<>();
        rulMapBinaryRest = new ConcurrentHashMap<>();
        ruleMapBinaryToSplit = new ConcurrentHashMap<>();
        this.ruleMapToManipulate = new RuleMaps(rm);
        //splitSymbols = ConcurrentHashMap.newKeySet();
        // Set<Integer> roleIndexSet = this.ruleMapToManipulate.getSemanticRoleIndexSet();
        // System.out.println(roleIndexSet);
       // System.err.println("---> herelllskljs");
        this.depLHSLargestCounterIndex = this.ruleMapToManipulate.getLargestSpltDepSplitCounterID();

        //  System.err.println("Largest index " + roleLargestIndex);
        this.newDepIndexCounter1 = this.depLHSLargestCounterIndex + 1;
        this.newDepIndexCounter2 = this.depLHSLargestCounterIndex + 2;
        //   newSymbol1 = SymbolFractory.getUnaryLHSForNonVerbialRole(newRoleIndex1);
        // newSymbol2 = SymbolFractory.getUnaryLHSForNonVerbialRole(newRoleIndex2);

    }

    public RuleMaps performSplit() throws Exception {
        this.prepareUnariesMapForSplit();
        this.prepareBinariesMapForSplit();
        Map<String, Map<String, Double>> splitUnaries = this.splitUnaries();
        Map<String, Map<String, Map<String, Double>>> splitBinaries = this.splitBinaries();

       // MapUtils.printMap(splitBinaries);
        RuleMaps rm = new RuleMaps();
        rm.instNewReverseMaps();

//        for(String lhs: splitBinaries.keySet()){
//            
//            Map<String, Map<String, Double>> get = splitBinaries.get(lhs);
//            for(String rhs1: get.keySet()){
//                
//              for(String rhs2:  get.get(rhs1).keySet()){
//                  System.out.println(lhs+" -> " + rhs1+" " + rhs2);
//              };
//            }
//        }
        //   rm.setKnownIDToSymbolMap(symbolInverseMap);
        //   rm.setKnownSymbolToIDMap(symbolToIDMap);
        addRulesToRuleMapUnary(rm, splitUnaries);
        addRulesToRuleMapBin(rm, splitBinaries);
        addRulesToRuleMapUnary(rm, ruleMapUnaryRest);
        addRulesToRuleMapBin(rm, rulMapBinaryRest);

        return rm;

    }

    /**
     * Checks weather the quiried symbol has the condition for split, here the
     * assumption is that split is taking place for a specific frame index.
     *
     * @param symbol
     * @return
     */
    private boolean conditionForSplit(String symbol) throws Exception {
        return SymbolFractory.isUnaryDepHead(symbol) && (this.ruleMapToManipulate.getIDFromSymbol(symbol)==(tgtSymbolID));
       // return SymbolFractory.isUnaryDepHead(symbol, tgtRole);
    }

    /**
     * Filter out those rules that are related to the frame we are splitting.
     *
     * @param frameHeadNumber
     * @throws IOException
     * @throws Exception
     */
    public void prepareUnariesMapForSplit() throws IOException, Exception {
        Map<Integer, Map<Integer, Double>> reversUnaryRuleMap = this.ruleMapToManipulate.getReversUnaryRuleMap();
        int unaryCountToSplit = 0;
        for (int rhsID : reversUnaryRuleMap.keySet()) {
            String symbolRHS = ruleMapToManipulate.getSymbolFromID(rhsID);
            Map<Integer, Double> lhsForRHS = reversUnaryRuleMap.get(rhsID);
            if (conditionForSplit(symbolRHS)) {
                //System.err.println("symbolRHS condition " + symbolRHS);
                // the right hand side of the root is our target so add the remaining lhss 
                for (Integer lhsID : lhsForRHS.keySet()) {
                    String symbolLHS = ruleMapToManipulate.getSymbolFromID(lhsID);
                    Double prop = lhsForRHS.get(lhsID);
                    if (!Double.isFinite(prop)) {
                        System.err.println("HERE NOT FINITE! ID=92376");
                    }
                    updateUnaryMap(ruleMapUnaryToSplit, symbolLHS, symbolRHS, prop);
                    unaryCountToSplit++;
                }
            } else {
                for (Integer lhsID : lhsForRHS.keySet()) {
                    String symbolLHS = ruleMapToManipulate.getSymbolFromID(lhsID);
                    Double prop = lhsForRHS.get(lhsID);

                    if (conditionForSplit(symbolLHS)) {

                        updateUnaryMap(ruleMapUnaryToSplit, symbolLHS, symbolRHS, prop);
                        unaryCountToSplit++;
                    } else {
                        updateUnaryMap(ruleMapUnaryRest, symbolLHS, symbolRHS, prop);
                    }
                }
            }
        }
        LOGGER.log(Level.FINE, "The set of unary rules requires splitting {0}", unaryCountToSplit);

    }

    public void prepareBinariesMapForSplit() throws IOException, Exception {
        Map<Integer, Map<Integer, Map<Integer, Double>>> revereseBinary = this.ruleMapToManipulate.getReverseBinaryRuleMap();
        int countBinaryRules = 0;
        for (int rhsID1 : revereseBinary.keySet()) {

            String symbolRHS1 = ruleMapToManipulate.getSymbolFromID(rhsID1);
            Map<Integer, Map<Integer, Double>> rhs2LhsPropMap = revereseBinary.get(rhsID1);
            if (conditionForSplit(symbolRHS1)) {
                //System.err.println("symbolRHS1 passed " + symbolRHS1);
                // the right hand side of the root is our target so add the remaining lhss 
                for (Integer rhsID2 : rhs2LhsPropMap.keySet()) {
                    String symbolRHS2 = ruleMapToManipulate.getSymbolFromID(rhsID2);
                    Map<Integer, Double> lhsPropMap = rhs2LhsPropMap.get(rhsID2);
                    for (Integer lhsID : lhsPropMap.keySet()) {

                        Double prop = lhsPropMap.get(lhsID);
                        String symbolLHS = ruleMapToManipulate.getSymbolFromID(lhsID);
                        updateBinaryMap(ruleMapBinaryToSplit, symbolLHS, symbolRHS1, symbolRHS2, prop);
                        countBinaryRules++;
                    }

                }
            } else {
                for (Integer rhsID2 : rhs2LhsPropMap.keySet()) {
                    Map<Integer, Double> lhsPropMap = rhs2LhsPropMap.get(rhsID2);
                    String symbolRHS2 = ruleMapToManipulate.getSymbolFromID(rhsID2);

                    if (conditionForSplit(symbolRHS2)) {

                        for (Integer lhsID : lhsPropMap.keySet()) {
                            String symbolLHS = ruleMapToManipulate.getSymbolFromID(lhsID);
                            Double prop = lhsPropMap.get(lhsID);
                            updateBinaryMap(ruleMapBinaryToSplit, symbolLHS, symbolRHS1, symbolRHS2, prop);
                            countBinaryRules++;
                        }
                    } else {
                        for (Integer lhsID : lhsPropMap.keySet()) {
                            String lhsSymbol = ruleMapToManipulate.getSymbolFromID(lhsID);
                            Double prop = lhsPropMap.get(lhsID);
                            if (conditionForSplit(lhsSymbol)) {

                                updateBinaryMap(ruleMapBinaryToSplit, lhsSymbol, symbolRHS1, symbolRHS2, prop);
                                countBinaryRules++;
                            } else {

                                updateBinaryMap(rulMapBinaryRest, lhsSymbol, symbolRHS1, symbolRHS2, prop);
                            }

                        }

                    }
                }
            }
        }
        LOGGER.log(Level.FINE, "The set of BINARY rules requires splitting {0}", countBinaryRules);

    }

    /**
     * Helper method for updaing unary maps that we are working with.
     *
     * @param ruleMapUnaryToSplit
     * @param lhsID
     * @param rhsID
     * @param prop
     */
    private static void updateUnaryMap(Map<String, Map<String, Double>> ruleMapUnaryToSplit, String lhsID, String rhsID, double prop) throws Exception {
        if (!Double.isFinite(prop)) {
            System.err.println("HERE NOT FINIT updateUnaryMap id:33625  ");
        }
        if (ruleMapUnaryToSplit.containsKey(lhsID)) {
            Map<String, Double> get = ruleMapUnaryToSplit.get(lhsID);
            if (get.containsKey(rhsID)) {
                throw new Exception("id:385625 Error in data flow logic, this should not happen!");
            }
            get.put(rhsID, prop);
        } else {
            Map<String, Double> get = new ConcurrentHashMap<>();
            get.put(rhsID, prop);
            ruleMapUnaryToSplit.put(lhsID, get);
        }
    }

    private static void updateBinaryMap(Map<String, Map<String, Map<String, Double>>> ruleMapBinaryToSplit, String lhsID, String rhsID1, String rhsID2, Double prop) throws Exception {
        if (!Double.isFinite(prop)) {
            System.err.println("HERE NOT FINITE UpdateBinaryMap id:33622  ");
        }
        if (ruleMapBinaryToSplit.containsKey(lhsID)) {
            Map<String, Map<String, Double>> rhsMap = ruleMapBinaryToSplit.get(lhsID);
            if (rhsMap.containsKey(rhsID1)) {
                Map<String, Double> rhs2Map = rhsMap.get(rhsID1);
                if (rhs2Map.containsKey(rhsID2)) {
                    throw new Exception("id:389625 Error in data flow logic, this should not happen!");
                } else {
                    rhs2Map.put(rhsID2, prop);

                }
            } else {
                Map<String, Double> rhs2Map = new ConcurrentHashMap<>();
                rhs2Map.put(rhsID2, prop);
                rhsMap.put(rhsID1, rhs2Map);
            }

        } else {
            Map<String, Map<String, Double>> rhsMap = new ConcurrentHashMap<>();
            Map<String, Double> rhs2Map = new ConcurrentHashMap<>();
            rhs2Map.put(rhsID2, prop);
            rhsMap.put(rhsID1, rhs2Map);
            ruleMapBinaryToSplit.put(lhsID, rhsMap);
        }
    }

    /**
     * The actual split of unaries: make a duplicate of rules (one rule => 2
     * rules) for those that LHS is frame related, each rule is assigned a
     * portion of the param assigned to the original rule.
     *
     */
    private Map<String, Map<String, Double>> splitUnaries() throws Exception {

        Map<String, Map<String, Double>> duplicated = new ConcurrentHashMap<>();
        for (String lhs : this.ruleMapUnaryToSplit.keySet()) {
            Map<String, Double> allRHSs = ruleMapUnaryToSplit.get(lhs);
            String[] splitLHSSymbol = splitSymbol(lhs);
            //  if (splitLHSSymbol.length > 1) {
//                System.err.println("|-->" + lhs + " " + Arrays.toString(splitLHSSymbol));
            Set<Entry<String, Double>> entrySet = allRHSs.entrySet();
            for (Entry<String, Double> ruleRhs : entrySet) {
                String rhsSymbol = ruleRhs.getKey();
                String[] splitRhsSymbol = splitSymbol(rhsSymbol);
                Double param = ruleRhs.getValue();
                updateUnaryMap(duplicated, splitLHSSymbol, splitRhsSymbol, param);
//                   //double[] splitWeights = splitWeights(value, splitLHSSymbol.length);
//                    for (int i = 0; i < splitWeights.length; i++) {
//                        String rhsToReplace = rhsSymbol;
//                        if (conditionForSplit(rhsSymbol)) {
//                            //rhsToReplace = normalizeRHSForLHS(splitLHSSymbol[i], rhsToReplace);
//                        }
//                        updateUnaryMap(duplicated, splitLHSSymbol[i], rhsToReplace, splitWeights[i]);
//                    }
            }
//            } else {
//                duplicated.put(lhs, new ConcurrentHashMap<>(allRHSs));
//            }
        }
        return duplicated;

    }

    /**
     * Generic method to split binaries
     *
     * @return
     * @throws Exception
     */
    private Map<String, Map<String, Map<String, Double>>> splitBinaries() throws Exception {
        Map<String, Map<String, Map<String, Double>>> duplicate = new ConcurrentHashMap<>();
        for (String lhs : this.ruleMapBinaryToSplit.keySet()) {
            Map<String, Map<String, Double>> allRHSs = ruleMapBinaryToSplit.get(lhs);
            String[] splitLHSSymbol = splitSymbol(lhs);

            Set<Entry<String, Map<String, Double>>> entrySetRHSs = allRHSs.entrySet();
            for (Entry<String, Map<String, Double>> ruleRhs : entrySetRHSs) {
                String rhsSymbol1 = ruleRhs.getKey();
                String[] splitRHSSymbol1 = splitSymbol(rhsSymbol1);
                Set<Entry<String, Double>> rhs2valueEntrySet = ruleRhs.getValue().entrySet();

                for (Entry<String, Double> ruleRhs2Param : rhs2valueEntrySet) {
                    double param = ruleRhs2Param.getValue();
                    String rhsSymbol2 = ruleRhs2Param.getKey();
                    String[] splitRHSSymbol2 = splitSymbol(rhsSymbol2);
                    updateMapForSymbols(splitLHSSymbol, splitRHSSymbol1, splitRHSSymbol2, param, duplicate);
                }
            }
        }
        return duplicate;
    }

    /**
     * SplitHeads the weights
     *
     * @param weight
     * @param splitSize
     * @return
     */
    private double[] splitWeights(double weight, int splitSize) throws Exception {

        if (splitSize != 2) {
            throw new Exception("Splitting to more than 2 is not supported yet!");
        }
// did not work        
//        double log100 =FastMath.log(100);
//        double[] randSum = new double[2];
//        randSum[0] = FastMath.log(49)+FastMath.log(weight)- log100;
//        randSum[1] = FastMath.log(51)+FastMath.log(weight)- log100;
//                if(!Double.isFinite(randSum[0])){
//            System.err.println("Portion weight is not finite id:76663");
//        }
//                      if(!Double.isFinite(randSum[1])){
//            System.err.println("Portion weight is not finite id:76663");
//        }
//                      return randSum;

//        double[] randSum = new double[2];
//        double halved = Math.exp(weight)/2;
//        double p100 = halved*0.01 ;
//        
//
//        if (!Double.isFinite(halved)) {
//            System.err.println("Halved Portion weight is not finite id:76663 :: " + weight);
//        }
//        randSum[0] = Math.log(halved+ p100);
//        randSum[1] =  Math.log(halved- p100);
//
//        if (!Double.isFinite(randSum[0])) {
//            System.err.println("Portion weight is not finite id:76663");
//        }
//        if (!Double.isFinite(randSum[1])) {
//            System.err.println("Portion weight is not finite id:76663");
//        }
//        return randSum;
        double log2 = FastMath.log(2);
        double[] randSum = new double[2];
        double halved = weight - log2;
        double log10 = FastMath.log(10);
        double haveldP = halved - log10;

        if (!Double.isFinite(halved)) {
            System.err.println("Halved Portion weight is not finite id:76663 :: " + weight);
        }
        randSum[0] = halved + 1e-7;//MathUtil.logSumExp(halved, haveldP);
        randSum[1] = halved - 1e-7; //MathUtil.logSumExp(halved, -haveldP);

        if (!Double.isFinite(randSum[0])) {
            System.err.println("Portion weight is not finite id:76663");
        }
        if (!Double.isFinite(randSum[1])) {
            System.err.println("Portion weight is not finite id:76663");
        }
        return randSum;

//        
//        double weightP = FastMath.exp(weight-weight*0.01);
//        double portionEach =  weightP / splitSize;
//        if(!Double.isFinite(portionEach)){
//            System.err.println("Portion weight is not finite id:76663");
//        }
//        
//        //System.err.println("-->"+weightP);
//        if(!Double.isFinite(weightP)){
//            System.err.println("WeightP not finite id:8663");
//        }
//        double asymW = weightP * 0.01;
//        // I guess it could be much easier by returning log values for RndWeightsVectors.randSum(splitSize,weightP);
//        double[] randSum = RndWeightsVectors.randSum(splitSize, asymW, 2);
//        
//        //double sum = 0.0;
//        for (int i = 0; i < randSum.length; i++) {
//            // System.err.println("\t-->"+ (randSum[i] + portionEach));
//            //sum+=(randSum[i] + portionEach);
//            randSum[i] = Math.log(randSum[i] + portionEach);
//             if(!Double.isFinite(randSum[i])){
//            System.err.println("Random weight is not finite id:76663");
//        }
//
//        }
//        //System.err.println("\t+"+sum);
        // return randSum;
    }

    protected void addToSplitMemory(String originlSymbol, String[] newSymbols) {
        for (String newSymbol : newSymbols) {
            this.symbolMappingMemory.put(newSymbol, originlSymbol);
        }
    }

    /**
     * Return an array of string, if the array size is greater than 1, then the
     * input symbol must be split and duplicated for the returned symbols
     *
     * @param lhs
     */
    private String[] splitSymbol(String lhs) throws Exception {
        String[] splitSymbol = null;
        if (// the first condition is redundant, still have to make up my mind on how to generalize this class
                // the problem is the assumed dependencies between random variables
                conditionForSplit(lhs)
                ) {

            String lhs1 = SymbolFractory.getChangeDepStxSpltCounterID(lhs, this.newDepIndexCounter1);
            String lhs2 = SymbolFractory.getChangeDepStxSpltCounterID(lhs, this.newDepIndexCounter2);
            splitSymbol = new String[]{lhs1, lhs2};

            String toString = Arrays.toString(splitSymbol);
            LOGGER.log(Level.FINEST, "+Split 1 rule: {0}->{1}", new Object[]{lhs, toString});
           
        } else {
            // if the symbol does not pass the requirement for split
            splitSymbol = new String[]{lhs};
        }

        addToSplitMemory(lhs, splitSymbol);
        return splitSymbol;
    }

    private String normalizeRHSForLHS(String lhsSymbol, String rhsSymbol) {
        int symbolIndex = SymbolFractory.getSymbolIndex(lhsSymbol);
        String newRHS = SymbolFractory.replaceSymbolIndex(rhsSymbol, symbolIndex);
        LOGGER.log(Level.FINE, "[normlize RHS of LHS:  {0} {1} ] to {2}", new Object[]{lhsSymbol, rhsSymbol, newRHS});
        String[] map = {newRHS};
        addToSplitMemory(rhsSymbol, map);
        return newRHS;
    }

    private void addRulesToRuleMapBin(RuleMaps rm, Map<String, Map<String, Map<String, Double>>> rulMapBinaryRest) throws Exception {
        for (Entry<String, Map<String, Map<String, Double>>> binRules : rulMapBinaryRest.entrySet()) {
            String lhsSymbol = binRules.getKey();
            int lhsID = rm.getIDFromSymbol(lhsSymbol);
            Map<String, Map<String, Double>> rhsSet = binRules.getValue();
            for (Entry<String, Map<String, Double>> rhsEntry : rhsSet.entrySet()) {
                String rhs1Symbol = rhsEntry.getKey();
                Map<String, Double> rhs2SetParam = rhsEntry.getValue();
                int rhs1ID = rm.getIDFromSymbol(rhs1Symbol);
                for (Entry<String, Double> rhs2Ent : rhs2SetParam.entrySet()) {
                    String rhs2Symbol = rhs2Ent.getKey();
                    int rhs2ID = rm.getIDFromSymbol(rhs2Symbol);
                    Double param = rhs2Ent.getValue();
                    rm.addReverseBinaryRule(lhsID, rhs1ID, rhs2ID, param);
                }
            }
        }
    }

    private void addRulesToRuleMapUnary(RuleMaps rm, Map<String, Map<String, Double>> unaryRuleMap) throws Exception {

        for (Entry<String, Map<String, Double>> rules : unaryRuleMap.entrySet()) {
            String lhs = rules.getKey();
            Map<String, Double> rhsParamMap = rules.getValue();
            int lhsID = rm.getIDFromSymbol(lhs);
            for (Entry<String, Double> rhs2Ent : rhsParamMap.entrySet()) {
                String rhsSymbol = rhs2Ent.getKey();
                int rhs1ID = rm.getIDFromSymbol(rhsSymbol);
                Double param = rhs2Ent.getValue();
                rm.addReverseUnaryRule(lhsID, rhs1ID, param);
            }
        }

    }

    /**
     * I am not sure about splitting the weights here
     *
     * @param lhs
     * @param rhs1
     * @param rhs2
     * @param param
     * @param duplicate
     * @throws Exception
     */
    protected void updateMapForSymbols(String[] lhs, String[] rhs1, String[] rhs2, double param, Map<String, Map<String, Map<String, Double>>> duplicate) throws Exception {

        int al = lhs.length;
        int bl = rhs1.length;
        int cl = rhs2.length;

        if (al == 1 && bl == 1 && cl == 1) {
            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[0], param);
        } else if (al == 1 && bl == 1 && cl == 2) {
            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[0], param);
            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[1], param);
        } else if (al == 1 && bl == 2 && cl == 1) {

            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[0], param);
            updateBinaryMap(duplicate, lhs[0], rhs1[1], rhs2[0], param);

        } else if (al == 1 && bl == 2 && cl == 2) {

            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[0], param);
            updateBinaryMap(duplicate, lhs[0], rhs1[1], rhs2[0], param);
            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[1], param);
            updateBinaryMap(duplicate, lhs[0], rhs1[1], rhs2[1], param);

        } else if (al == 2 && bl == 1 && cl == 1) {
            double[] splitWeights = splitWeights(param, al);
            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[0], splitWeights[0]);
            updateBinaryMap(duplicate, lhs[1], rhs1[0], rhs2[0], splitWeights[1]);
        } else if (al == 2 && bl == 1 && cl == 2) {
            double[] splitWeights = splitWeights(param, al);
            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[0], splitWeights[0]);
            updateBinaryMap(duplicate, lhs[1], rhs1[0], rhs2[0], splitWeights[1]);
            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[1], splitWeights[0]);
            updateBinaryMap(duplicate, lhs[1], rhs1[0], rhs2[1], splitWeights[1]);

        } else if (al == 2 && bl == 2 && cl == 1) {
            double[] splitWeights = splitWeights(param, al);
            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[0], splitWeights[0]);
            updateBinaryMap(duplicate, lhs[1], rhs1[0], rhs2[0], splitWeights[1]);
            updateBinaryMap(duplicate, lhs[0], rhs1[1], rhs2[0], splitWeights[0]);
            updateBinaryMap(duplicate, lhs[1], rhs1[1], rhs2[0], splitWeights[1]);

        } else if (al == 2 && bl == 2 && cl == 2) {
            double[] splitWeights = splitWeights(param, al);
            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[0], splitWeights[0]);
            updateBinaryMap(duplicate, lhs[0], rhs1[0], rhs2[1], splitWeights[0]);
            updateBinaryMap(duplicate, lhs[0], rhs1[1], rhs2[0], splitWeights[0]);
            updateBinaryMap(duplicate, lhs[0], rhs1[1], rhs2[1], splitWeights[0]);

            updateBinaryMap(duplicate, lhs[1], rhs1[0], rhs2[0], splitWeights[1]);
            updateBinaryMap(duplicate, lhs[1], rhs1[0], rhs2[1], splitWeights[1]);
            updateBinaryMap(duplicate, lhs[1], rhs1[1], rhs2[0], splitWeights[1]);
            updateBinaryMap(duplicate, lhs[1], rhs1[1], rhs2[1], splitWeights[1]);
        }
    }

    /**
     * Method for updated a duplicate map of unaries
     *
     * @param duplicate
     * @param lhs
     * @param rhs
     * @param param
     * @throws Exception
     */
    protected void updateUnaryMap(Map<String, Map<String, Double>> duplicate, String[] lhs, String[] rhs, Double param) throws Exception {
        int al = lhs.length;
        int bl = rhs.length;
        if (al == 1 && bl == 1) {
            updateUnaryMap(duplicate, lhs[0], rhs[0], param);
        } else if (al == 1 && bl == 2) {
            updateUnaryMap(duplicate, lhs[0], rhs[0], param);
            updateUnaryMap(duplicate, lhs[0], rhs[1], param);
        } else if (al == 2 && bl == 1) {
            double[] splitWeights = splitWeights(param, al);
            updateUnaryMap(duplicate, lhs[0], rhs[0], splitWeights[0]);
            updateUnaryMap(duplicate, lhs[1], rhs[0], splitWeights[1]);
        } else if (al == 2 && bl == 2) {
            double[] splitWeights = splitWeights(param, al);
            updateUnaryMap(duplicate, lhs[0], rhs[0], splitWeights[0]);
            updateUnaryMap(duplicate, lhs[1], rhs[0], splitWeights[1]);
            updateUnaryMap(duplicate, lhs[0], rhs[1], splitWeights[0]);
            updateUnaryMap(duplicate, lhs[1], rhs[1], splitWeights[1]);

        }
    }

    public static void main(String[] args) throws IOException, Exception {

//         ConsoleHandler handler = new ConsoleHandler();
//        handler.setLevel(Level.FINEST);
//         LogManager.getLogManager().
        Logger.getLogger(SplitDependency.class.getName()).setLevel(Level.FINER);

        List<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments("../lr_other/stanford-parse.txt", null, null, new Settings(), 10,11);
        System.out.println("Fragment size: " + inputFramentList.size() + " e.g., " + inputFramentList.get(0).toStringPosition());
        Logger.getGlobal().log(Level.FINE, "Fragment size: {0} e.g., {1}", new Object[]{inputFramentList.size(), inputFramentList.get(0).toStringPosition()});

        RuleMaps rm = null;
        
            GenerateGrammarFrameRoleIndepandantsJCLK gg = new GenerateGrammarFrameRoleIndepandantsJCLK();
            gg.genRules(inputFramentList, 1, 5);
            rm = gg.getTheRuleMap();
            //theRuleMap.seralizaRules(path.getInitGrammarFile());
        

        //RuleMaps rm = RuleMaps.fromArchivedFile("temp/0-it.grmr.zip");
        Map<Integer, Map<Integer, Double>> reversUnaryRuleMap = rm.getReversUnaryRuleMap();
        System.out.println("size: " + reversUnaryRuleMap.size());

        SplitDependency spm = new SplitDependency("D_nmod:for", rm);
        RuleMaps performSplit = spm.performSplit();

        System.out.println("------------------");
        
//        for (String s : spm.splitSymbols) {
//            System.out.println(s);
//        }
        MapUtils.printMap(spm.ruleMapBinaryToSplit);

    }

}
