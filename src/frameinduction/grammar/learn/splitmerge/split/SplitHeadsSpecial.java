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
import frameinduction.grammar.learn.MathUtil;
import frameinduction.grammar.SymbolFractory;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.util.FastMath;


/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class SplitHeadsSpecial {

    private static final Logger LOGGER = Logger.getLogger(SplitHeadsSpecial.class.getName());

    final private Set<String> setRelatedTarget;
    final private Map<String, Map<String, Double>> ruleMapUnaryRest;
    final private Map<String, Map<String, Double>> ruleMapUnaryToSplit;
    final private Set<String> splitSymbols ;
    final private Map<String, Map<String, Map<String, Double>>> rulMapBinaryRest;
    final private Map<String, Map<String, Map<String, Double>>> ruleMapBinaryToSplit;
    //  private final static int OFST_SYM1 = 1000; // offset for id of split symbol 1
    // private final static int OFST_SYM2 = 20000; // offset for id of split symbol 2
    final private RuleMaps ruleMapToManipulate;
    final private int tgtFrameIndex;
    final private int frameHeadLargestIndex;
    final private int newFrameIndex1;
    final private int newFrameIndex2;
    final private Map<String,String> symbolMappingMemory;
    //final private Map<String, Set<String>> clusterToUse;
    final private RuleMaps newRuleMapRules;
    /**
     * Probably need to add datastructures to carry information about what has
     * been split to what, merged to what etc.
     *
     * @param frameIndex
     * @param rm
     * @param newRuleMapRules
     * @throws Exception
     */
    public SplitHeadsSpecial(int frameIndex, RuleMaps rm, RuleMaps newRuleMapRules) throws Exception {
        this.tgtFrameIndex = frameIndex;
       this.newRuleMapRules = newRuleMapRules;
        symbolMappingMemory = Collections.synchronizedMap(new HashMap<>());
        String targetFrameHead = SymbolFractory.getLhsForUnaryHeadRole1(frameIndex);
        //System.err.println(targetFrameHead);
        String headFrameSymbol = SymbolFractory.getHeadFrameSymbol(frameIndex);
       // System.err.println(headFrameSymbol);
        String frameRemain = SymbolFractory.getFrameRemainingArguments(frameIndex);
      //  System.err.println("R: " + frameRemain);
        String jointSyntaxFrame = SymbolFractory.getFramesJSntxArgument(frameIndex, "");
     //   System.err.println("J: " + jointSyntaxFrame);
        setRelatedTarget = new HashSet<>();
        setRelatedTarget.add(targetFrameHead);
        setRelatedTarget.add(headFrameSymbol);
        setRelatedTarget.add(frameRemain);
        setRelatedTarget.add(jointSyntaxFrame);
        ruleMapUnaryRest = new ConcurrentHashMap<>();
        ruleMapUnaryToSplit = new ConcurrentHashMap<>();
        rulMapBinaryRest = new ConcurrentHashMap<>();
        ruleMapBinaryToSplit = new ConcurrentHashMap<>();
        this.ruleMapToManipulate = new RuleMaps(rm);
        splitSymbols = ConcurrentHashMap.newKeySet();
        this.frameHeadLargestIndex = this.ruleMapToManipulate.getFrameHeadLargestIndex();
       // System.err.println("Largest index " + frameHeadLargestIndex);
        this.newFrameIndex1 = this.frameHeadLargestIndex + 1;
        this.newFrameIndex2 = this.frameHeadLargestIndex + 2;
        
        
     //   System.err.println("New ones are " + newFrameIndex1 +" " +newFrameIndex2 );
        //String headFrameSymbol1 = SymbolFractory.getHeadFrameSymbol( this.newFrameIndex1);
       // String headFrameSymbol2 = SymbolFractory.getHeadFrameSymbol(this.newFrameIndex2);
        
        //symbolMapping.
        

    }

    public RuleMaps performSplit() throws Exception {
//        Map<String, Integer> symbolToIDMap = this.ruleMapToManipulate.getSymbolToIDMap();
//        Map<Integer, String> symbolInverseMap = this.ruleMapToManipulate.getSymbolInverseMap();
//       


        

        
        
        this.updateUnariesForSplitFrameHead();
        this.updateBinariesForSplitFrameHead();
        Map<String, Map<String, Double>> splitUnaries = this.splitUnaries();
        Map<String, Map<String, Map<String, Double>>> splitBinaries = this.splitBinaries();

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
    private boolean conditionForSplit(String symbol) {
        for (String symbolTgt : setRelatedTarget) {
            if (symbol.startsWith(symbolTgt)) {
                splitSymbols.add(symbol);
                return true;
            }
        }
        return false;
    }

    /**
     * Filter out those rules that are related to the frame we are splitting.
     *
     * @param frameHeadNumber
     * @throws IOException
     * @throws Exception
     */
    public void updateUnariesForSplitFrameHead() throws IOException, Exception {
        Map<Integer, Map<Integer, Double>> reversUnaryRuleMap = this.ruleMapToManipulate.getReversUnaryRuleMap();
        int unaryCountToSplit = 0;
        for (int rhsID : reversUnaryRuleMap.keySet()) {
            String symbolRHS = ruleMapToManipulate.getSymbolFromID(rhsID);
            Map<Integer, Double> lhsForRHS = reversUnaryRuleMap.get(rhsID);
            if (conditionForSplit(symbolRHS)) {
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

    public void updateBinariesForSplitFrameHead() throws IOException, Exception {
        Map<Integer, Map<Integer, Map<Integer, Double>>> revereseBinary = this.ruleMapToManipulate.getReverseBinaryRuleMap();
        int countBinaryRules = 0;
        for (int rhsID1 : revereseBinary.keySet()) {
            String symbolRHS1 = ruleMapToManipulate.getSymbolFromID(rhsID1);
            Map<Integer, Map<Integer, Double>> rhs2LhsPropMap = revereseBinary.get(rhsID1);
            if (conditionForSplit(symbolRHS1)) {
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

    private void updateBinaryMap(Map<String, Map<String, Map<String, Double>>> ruleMapBinaryToSplit, String lhsID, String rhsID1, String rhsID2, Double prop) throws Exception {
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

    private void updateAddToBinaryMap(Map<String, Map<String, Map<String, Double>>> ruleBin, String lhs, Map<String, Map<String, Double>> rhssToCopyFrom) throws Exception {

        if (ruleBin.containsKey(lhs)) {
            throw new RuntimeException("ID: 562356 This is not acceptable according to the implemented logic");
        } else if (lhs.equals(SymbolFractory.getStartSymbol())) {
            String startSymbol = SymbolFractory.getStartSymbol();
            double param = this.ruleMapToManipulate.getBinaryRuleParam(
                    startSymbol, SymbolFractory.getHeadFrameSymbol(this.tgtFrameIndex),
                    SymbolFractory.getFrameRemainingArguments(this.tgtFrameIndex));
            //System.err.println(param);
            String headFrameSymbol1 = SymbolFractory.getHeadFrameSymbol(this.newFrameIndex1);
            String frameRemainingArguments1 = SymbolFractory.getFrameRemainingArguments(this.newFrameIndex1);
            String headFrameSymbol2 = SymbolFractory.getHeadFrameSymbol(this.newFrameIndex2);
            String frameRemainingArguments2 = SymbolFractory.getFrameRemainingArguments(this.newFrameIndex2);
            double[] splitWeights = this.splitWeights(param, 2);
            updateBinaryMap(ruleBin, startSymbol, headFrameSymbol1, frameRemainingArguments1, splitWeights[0]);
            updateBinaryMap(ruleBin, startSymbol, headFrameSymbol2, frameRemainingArguments2, splitWeights[1]);

        } else {
            System.err.println("NEVER HERE YET!");
            Map<String, Map<String, Double>> rhssDeepCopy = new ConcurrentHashMap<>();
            for (String rhs1 : rhssToCopyFrom.keySet()) {
                boolean rhs1ToDuplicate = false;
                if (conditionForSplit(rhs1)) {
                    rhs1ToDuplicate = true;
                    //   System.err.println("JERE " + rhs1);
                }
                Map<String, Double> rhs2SetOriginal = rhssToCopyFrom.get(rhs1);
                ConcurrentHashMap<String, Double> rhs2SetDeepCopyEdited = new ConcurrentHashMap<>();
                for (String rhs2Symb : rhs2SetOriginal.keySet()) {

                    Double thisRuleParam = rhs2SetOriginal.get(rhs2Symb);

                    boolean rhs1ToDuplicate2 = false;
                    if (conditionForSplit(rhs2Symb)) {
                        rhs1ToDuplicate2 = true;
                    }

                    if (!rhs1ToDuplicate && !rhs1ToDuplicate2) {
                        //  System.err.println("++HERE!!!!!");
                        rhs2SetDeepCopyEdited.put(rhs2Symb, thisRuleParam);
                    } else if (rhs1ToDuplicate && rhs1ToDuplicate2) {
                        System.err.println("Both must be changed ?!HEre This is a temporary solution, change and generaize by adding isDepandantToGrammarGenneration");

                    } else if (rhs1ToDuplicate) {
                        System.err.println("Only first one");
                        String[] splitSymbol1 = splitSymbol(rhs1);
                        String[] splitSymbol2 = splitSymbol(rhs2Symb);
                        int toSplitWeight = splitSymbol1.length * splitSymbol2.length;

                    } else if (rhs1ToDuplicate2) {
                        System.err.println("only second one!");
                    }

                }
                rhssDeepCopy.put(rhs1, rhs2SetDeepCopyEdited);
            }
            ruleBin.put(lhs, rhssDeepCopy);
        }

    }

    private boolean areDepandantVariables(String symbol1, String symbol2) {
        boolean lhsForUnaryHead = SymbolFractory.isLhsForUnaryHead(symbol1, this.tgtFrameIndex);
        //  boolean lhsForUnaryHead = SymbolFractory.isLhsForUnaryHead(symbol1, this.frameIndex);
//        if () {
//            String lhs1 = SymbolFractory.getLhsForUnaryHeadRole(frameIndex + OFST_SYM1);
//            String lhs2 = SymbolFractory.getLhsForUnaryHeadRole(frameIndex + OFST_SYM2);
//            splitSymbol = new String[]{lhs1, lhs2};
//
//            String toString = Arrays.toString(splitSymbol);
//            LOGGER.log(Level.FINE, "SplitHeads 1 rule: {0}->{1}", new Object[]{lhs, toString});
//            
//
//        } else if (SymbolFractory.isAFramesJArgumentSyntax(lhs)) {
//            String depRelJntSyntaxArg = SymbolFractory.getDepRelJntSyntaxArg(lhs);
//            String lhs1 = SymbolFractory.getFramesJSntxArgument(frameIndex + OFST_SYM1, depRelJntSyntaxArg);
//            String lhs2 = SymbolFractory.getFramesJSntxArgument(frameIndex + OFST_SYM2, depRelJntSyntaxArg);
//            splitSymbol = new String[]{lhs1, lhs2};
//
//            String toString = Arrays.toString(splitSymbol);
//            LOGGER.log(Level.FINE, "SplitHeads 2 rule: {0}->{1}", new Object[]{lhs, toString});
//
//        } else if (SymbolFractory.isFrameRemainingArguments(lhs)) {
//            String lhs1 = SymbolFractory.getFrameRemainingArguments(frameIndex + OFST_SYM1);
//            String lhs2 = SymbolFractory.getFrameRemainingArguments(frameIndex + OFST_SYM2);
//            splitSymbol = new String[]{lhs1, lhs2};
//
//            String toString = Arrays.toString(splitSymbol);
//            LOGGER.log(Level.FINE, "SplitHeads 3 rule: {0}->{1}", new Object[]{lhs, toString});
//        } else if (SymbolFractory.isFrameHeadRule(lhs)) {
//            String lhs1 = SymbolFractory.getHeadFrameSymbol(frameIndex + OFST_SYM1);
//            String lhs2 = SymbolFractory.getHeadFrameSymbol(frameIndex + OFST_SYM2);
//            splitSymbol = new String[]{lhs1, lhs2};
//
//            String toString = Arrays.toString(splitSymbol);
//            LOGGER.log(Level.FINE, "SplitHeads 4 rule: {0}->{1}", new Object[]{lhs, toString});
//
//        } else {
//            // if the symbol does not pass the requirement for split
//            splitSymbol = new String[]{lhs};
//        }
        return true;

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
            if (splitLHSSymbol.length > 1) {
                Set<Entry<String, Double>> entrySet = allRHSs.entrySet();
                for (Entry<String, Double> ruleRhs : entrySet) {
                    String rhsSymbol = ruleRhs.getKey();
                    Double value = ruleRhs.getValue();
                    double[] splitWeights = splitWeights(value, splitLHSSymbol.length);
                    for (int i = 0; i < splitWeights.length; i++) {
                        String rhsToReplace = rhsSymbol;
                        if (conditionForSplit(rhsSymbol)) {
                            rhsToReplace = normalizeRHSForLHS(splitLHSSymbol[i], rhsToReplace);
                        }
                        updateUnaryMap(duplicated, splitLHSSymbol[i], rhsToReplace, splitWeights[i]);
                    }
                }
            } else {
                duplicated.put(lhs, new ConcurrentHashMap<>(allRHSs));
            }
        }
       return duplicated;
       
    }

    private Map<String, Map<String, Map<String, Double>>> splitBinaries() throws Exception {
        Map<String, Map<String, Map<String, Double>>> duplicate = new ConcurrentHashMap<>();
        for (String lhs : this.ruleMapBinaryToSplit.keySet()) {

            Map<String, Map<String, Double>> allRHSs = ruleMapBinaryToSplit.get(lhs);
            String[] splitLHSSymbol = splitSymbol(lhs);

            if (splitLHSSymbol.length > 1) {
                Set<Entry<String, Map<String, Double>>> entrySet = allRHSs.entrySet();
                for (Entry<String, Map<String, Double>> ruleRhs : entrySet) {
                    String rhsSymbol1 = ruleRhs.getKey();
                    Set<Entry<String, Double>> rhs2valueEntrySet = ruleRhs.getValue().entrySet();
                    for (Entry<String, Double> ruleRhs2Param : rhs2valueEntrySet) {
                        double param = ruleRhs2Param.getValue();
                        String rhsSymbol2 = ruleRhs2Param.getKey();
                        double[] splitWeights = splitWeights(param, splitLHSSymbol.length);
                        for (int i = 0; i < splitWeights.length; i++) {
                            String rhs1ToReplace = rhsSymbol1;
                            String rhs2ToReplace = rhsSymbol2;
                            if (conditionForSplit(rhs1ToReplace)) {
                                rhs1ToReplace = normalizeRHSForLHS(splitLHSSymbol[i], rhs1ToReplace);
                            }
                            if (conditionForSplit(rhs2ToReplace)) {
                                rhs2ToReplace = normalizeRHSForLHS(splitLHSSymbol[i], rhs2ToReplace);
                            }
                            updateBinaryMap(duplicate, splitLHSSymbol[i], rhs1ToReplace, rhs2ToReplace, splitWeights[i]);
                        }
                    }
                }
            } else {

                updateAddToBinaryMap(duplicate, lhs, allRHSs);
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
        randSum[0] = halved +1e-4;//MathUtil.logSumExp(halved, haveldP);
        randSum[1] = halved -1e-4; //MathUtil.logSumExp(halved, -haveldP);
       
        
        
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

    
    private void addToSplitMemory(String originlSymbol, String[] newSymbols){
        for(String newSymbol: newSymbols){
            this.symbolMappingMemory.put(newSymbol,originlSymbol);
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
        if (SymbolFractory.isLhsForUnaryHead(lhs, this.tgtFrameIndex)) {
            String lhs1 = SymbolFractory.getLhsForUnaryHeadRole(this.newFrameIndex1);
            String lhs2 = SymbolFractory.getLhsForUnaryHeadRole(this.newFrameIndex2);
            splitSymbol = new String[]{lhs1, lhs2};
            
            String toString = Arrays.toString(splitSymbol);
            LOGGER.log(Level.FINE, "Split 1 rule: {0}->{1}", new Object[]{lhs, toString});

        } else if (SymbolFractory.isAFramesJArgumentSyntax(lhs)) {
            String depRelJntSyntaxArg = SymbolFractory.getDepRelJntSyntaxArg(lhs);
            String lhs1 = SymbolFractory.getFramesJSntxArgument(this.newFrameIndex1, depRelJntSyntaxArg);
            String lhs2 = SymbolFractory.getFramesJSntxArgument(this.newFrameIndex2, depRelJntSyntaxArg);
            splitSymbol = new String[]{lhs1, lhs2};

            String toString = Arrays.toString(splitSymbol);
            LOGGER.log(Level.FINE, "Split 2 rule: {0}->{1}", new Object[]{lhs, toString});

        } else if (SymbolFractory.isFrameRemainingArguments(lhs)) {
            String lhs1 = SymbolFractory.getFrameRemainingArguments(this.newFrameIndex1);
            String lhs2 = SymbolFractory.getFrameRemainingArguments(this.newFrameIndex2);
            splitSymbol = new String[]{lhs1, lhs2};

            String toString = Arrays.toString(splitSymbol);
            LOGGER.log(Level.FINE, "Split 3 rule: {0}->{1}", new Object[]{lhs, toString});
        } else if (SymbolFractory.isFrameHeadRule(lhs)) {
            String lhs1 = SymbolFractory.getHeadFrameSymbol(this.newFrameIndex1);
            String lhs2 = SymbolFractory.getHeadFrameSymbol(this.newFrameIndex2);
            splitSymbol = new String[]{lhs1, lhs2};

            String toString = Arrays.toString(splitSymbol);
            LOGGER.log(Level.FINE, "Split 4 rule: {0}->{1}", new Object[]{lhs, toString});

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
        addToSplitMemory(rhsSymbol,map);
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

    public static void main(String[] args) throws IOException, Exception {

//         ConsoleHandler handler = new ConsoleHandler();
//        handler.setLevel(Level.FINEST);
//         LogManager.getLogManager().
        Logger.getLogger(SplitHeadsSpecial.class.getName()).setLevel(Level.FINER);

        RuleMaps rm = RuleMaps.fromArchivedFile("temp/temp/0-si-grammar.zip");
        Map<Integer, Map<Integer, Double>> reversUnaryRuleMap = rm.getReversUnaryRuleMap();
        System.out.println(reversUnaryRuleMap.size());
        String letSayTarget = SymbolFractory.getLhsForUnaryHeadRole1(0);
        //SplitHeadsSpecial spm = new SplitHeadsSpecial(0, rm);
       // RuleMaps performSplit = spm.performSplit();

//        System.out.println("------------------");
//        System.err.println("-------");
//        for (String s : spm.splitSymbols) {
//            System.out.println(s);
//        }
        //MapUtils.printMap(spm.ruleMapBinaryToSplit);
    }
}
