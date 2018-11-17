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
package frameinduction.grammar.learn;

import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.RuleMaps;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;

import input.preprocess.objects.FragmentCompact;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class InsideOutsideEmbedding {
    private static final double HYPER_PARAM_MIN_THRE_SIM =.3; //.45; //.3 // // hyper paratmeter ,,, the large the value the higher purity and lower ipu
    private static final Logger LOGGER = Logger.getLogger(InsideOutsideEmbedding.class.getName());
    private RulesCounts ruleCounts;
    private Double ZParam;
    private final FragmentCompact fc;
   // private final Fragment fragment;
    //private final RuleMapsReader rm;
    private final RuleMaps rm;
    private final InOutParameterChart ioChart;
    private final int sentLength;
    private final WordPairSimilaritySmallMemoryFixedID wsContainer;
    private final double threasholdOnSimilarity;
    private final Set<Integer> idsUsedInUnification;
    //int dimensionTemp = 177;

    public InsideOutsideEmbedding(Fragment fragment,
            RuleMaps rm, RulesCounts ruleOuterWeight, WordPairSimilaritySmallMemoryFixedID wsContainer) throws Exception {
        this.wsContainer = wsContainer;
        this.fc = new FragmentCompact(fragment, rm);
        this.rm = rm;
        //this.fragment =fragment;
        ioChart = new InOutParameterChart(rm.getStartSymbolID());
        sentLength = fc.getTerminals().length;
        this.ruleCounts = ruleOuterWeight;
        threasholdOnSimilarity = HYPER_PARAM_MIN_THRE_SIM;// was 0.01 30d;
        idsUsedInUnification = new HashSet<>();
        //assert(threasholdOnSimilarity>=0);
    }

    public double getZParam() {
        return ZParam;
    }

    private void innerWeigtedUnification0(int k, int tID) throws Exception {
        Map<Integer, Map<Integer, Double>> reversUnaryRuleMap = rm.getReversUnaryRuleMap();
        String symbolFromID = rm.getSymbolFromID(tID);
        
        for (int wordsID : reversUnaryRuleMap.keySet()) {
            String wordInMap = rm.getSymbolFromID(wordsID);
            Double measureSim = this.wsContainer.getSimValue(symbolFromID, wordInMap);
            if (measureSim > threasholdOnSimilarity) {
               
                this.idsUsedInUnification.add(wordsID);
                Map<Integer, Double> lhsToUnary = rm.getLhsToUnary(wordsID);
                Set<Map.Entry<Integer, Double>> entrySet = lhsToUnary.entrySet();
                for (Map.Entry<Integer, Double> e : entrySet) {
                    Integer lhsSymbol = e.getKey();
                    Double param = e.getValue();
                    // \theta^log(sim(v,t)) gicvs good results too
                    Double multiply
                            = param
                            + Math.log(
                            measureSim  )
                            ;
                    if (!Double.isFinite(multiply)) {
                        
                        LOGGER.log(Level.FINE,"HERE EMBD: " + multiply + " " + measureSim + " " + param + " " + lhsSymbol + " " + rm.getSymbolFromID(lhsSymbol));
                        
                        ioChart.incInsideParam(k - 1, k, lhsSymbol, param);

                    } else {
                        ioChart.incInsideParam(k - 1, k, lhsSymbol, multiply);
                    }
                }
            }
        }
    }


    public void inside() throws Exception {
        for (int k = 1; k <= sentLength; k++) {
            if (k % 2 == 1) {
                //if (k == 1) {
                int tID = fc.getTerminals()[k - 1];
                innerWeigtedUnification0(k, tID);

            } else {
                Map<Integer, Double> lhsToUnary = rm.getLhsToUnary(fc.getTerminals()[k - 1]);
                Set<Map.Entry<Integer, Double>> iteratorUnary = lhsToUnary.entrySet();
                for (Map.Entry<Integer, Double> e : iteratorUnary) {
                    int lhsSymbol = e.getKey();
                    double param = e.getValue();
                    ioChart.incInsideParam(k - 1, k, lhsSymbol, param);
                }
            }
        }
        for (int width = 2; width <= sentLength; width++) {
            for (int i = 0; i <= sentLength - width; i++) {
                int k = i + width;
                for (int j = i + 1; j <= k - 1; j++) {
                    Map<Integer, Map<Integer, Map<Integer, Double>>> reverseBinaryRuleMap = rm.getReverseBinaryRuleMap();
                    Set<Map.Entry<Integer, Map<Integer, Map<Integer, Double>>>> iteratorLhs = reverseBinaryRuleMap.entrySet();
                    for (Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> e2 : iteratorLhs) {
                        int rhsB = e2.getKey();
                        Set<Map.Entry<Integer, Map<Integer, Double>>> iteratorRHSBC = e2.getValue().entrySet();
                        for (Map.Entry<Integer, Map<Integer, Double>> e3 : iteratorRHSBC) {
                            int rhsC = e3.getKey();
                            Map<Integer, Double> lhsAndParamvalue = e3.getValue();
                            Set<Map.Entry<Integer, Double>> iteratorLHSParam = lhsAndParamvalue.entrySet();
                            for (Map.Entry<Integer, Double> e4 : iteratorLHSParam) {
                                int lhsA = e4.getKey();
                                double lhsToRHS1RhS2Param = e4.getValue();
                                Double insideValueForB = ioChart.getInsideValueFor(i, j, rhsB);
                                Double insideValueForC = ioChart.getInsideValueFor(j, k, rhsC);
                                if (insideValueForB != null && insideValueForC != null) {
                                    double insideA_ik
                                            = lhsToRHS1RhS2Param
                                            + insideValueForB
                                            + insideValueForC;
                                    ioChart.incInsideParam(i, k, lhsA, insideA_ik);
                                }
                            }
                        }

                    }

                }

            }

        }

        this.ZParam = ioChart.getInsideValueFor(0, sentLength, this.rm.getStartSymbolID());
        //System.err.println(ZParam);        //System.err.println(ZParam);

        if (ZParam == null || !Double.isFinite(ZParam)) {
            
                LOGGER.log(Level.WARNING, "--> Z param " + ZParam + " Ignorting this ... investigate: " + this.fc.toHumanReadable(rm));
            
            
        }

//        else {
//            this.ZParam = ioChart.getInsideValueFor(0, sentLength, this.rm.getStartSymbolID());
//        }
//        if (ZParam == 0.0) {
//            System.err.println("Zero Z param " + ZParam);
//            System.err.println(Arrays.toString(this.fc.getTerminals()));
//        }
    }


    public void outsideAlgorithm() throws Exception {
        ioChart.incOutsideParam(0, sentLength, this.rm.getStartSymbolID(), (-ZParam));
        for (int width = this.sentLength; width >= 2; width--) {
            for (int i = this.sentLength - width; i >= 0; i--) {
                int k = i + width;
                for (int j = k - 1; j >= i + 1; j--) {
                    Map<Integer, IOParam> paramMapAtIJ = ioChart.getParamMapAt(i, j);
                    Map<Integer, Map<Integer, Map<Integer, Double>>> reverseBinaryRuleMap = rm.getReverseBinaryRuleMap();
                    Set<Map.Entry<Integer, Map<Integer, Map<Integer, Double>>>> entrySetLHS = reverseBinaryRuleMap.entrySet();
                    for (Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> eRHSBC : entrySetLHS) {
                        Integer rhsB = eRHSBC.getKey();
                        Map<Integer, Map<Integer, Double>> lhsInnerParam = eRHSBC.getValue();
                        Double inside_B_i_j = ioChart.getInsideValueFor(i, j, rhsB);
                        if (inside_B_i_j != null) {
                            double outside_partial_B_i_j = -Double.MAX_VALUE;
                            Set<Map.Entry<Integer, Map<Integer, Double>>> entrySet = lhsInnerParam.entrySet();
                            for (Map.Entry<Integer, Map<Integer, Double>> e : entrySet) {
                                Integer rhsC = e.getKey();
                                Map<Integer, Double> lhsAndParamvalue = e.getValue();
                                Double inside_C_j_k = ioChart.getInsideValueFor(j, k, rhsC);
                                if (inside_C_j_k != null) {
                                    double outside_partial_C_j_k = -Double.MAX_VALUE;
                                    for (Map.Entry<Integer, Double> eLP : lhsAndParamvalue.entrySet()) {
                                        Integer lhsA = eLP.getKey();
                                        Double thisRuleParam = eLP.getValue();
                                        double out_A_i_k = ioChart.getOutsideValueFor(i, k, lhsA);
                                        double tempSum = out_A_i_k + thisRuleParam;
                                        double outerWForRule = tempSum + inside_B_i_j + inside_C_j_k; // thisRuleParam ro barda
                                        this.ruleCounts.incOWBinary(lhsA, rhsB, rhsC, outerWForRule);
                                        outside_partial_B_i_j = MathUtil.logSumExp(outside_partial_B_i_j, tempSum + inside_C_j_k);
                                        outside_partial_C_j_k = MathUtil.logSumExp(outside_partial_C_j_k, tempSum + inside_B_i_j);
                                    }
                                    // compute and update outside for C
                                    this.ioChart.incOutsideParam(j, k, rhsC, outside_partial_C_j_k);
                                }
                            }
                            /// compute outside for B
                            this.ioChart.updateThisOutsideMap(paramMapAtIJ, rhsB, outside_partial_B_i_j);
                        }
                    }
                }
            }
        }

        // now let's do the computation for unaries
        for (int k = this.sentLength; k >= 1; k--) {
            int rhsWord = fc.getTerminals()[k - 1];
            if (k % 2 == 1) {
                updateEmbBasedOutside(rhsWord, k);
            }
            Map<Integer, Double> lhsToUnaryParamMap = rm.getLhsToUnary(rhsWord);
            Set<Map.Entry<Integer, Double>> lhsRuleParam = lhsToUnaryParamMap.entrySet();
            for (Map.Entry<Integer, Double> e : lhsRuleParam) {
                Integer lhsSymbol = e.getKey();
                Double thisParam = e.getValue();
                double outsideValueForUnaryLhs = ioChart.getOutsideValueFor(k - 1, k, lhsSymbol) + thisParam; //iteratorUnary.value() ro bardar
                this.ruleCounts.incOWUnary(lhsSymbol, rhsWord, outsideValueForUnaryLhs);
            }
        }
        //System.err.println(ZParam);

        if (!Double.isFinite(ZParam)) {

            LOGGER.log(Level.WARNING, "--> Z param " + ZParam + " Ignorting this ... investigate: " + this.fc.toHumanReadable(rm));

        } else {
            this.ruleCounts.incCurrentLikelihood(ZParam);
        }

    }

    private void updateEmbBasedOutside(int rhsWord, int k) throws Exception {
        String rhsWordStr = this.rm.getSymbolFromID(rhsWord);
        for (int wordTgtUsed : this.idsUsedInUnification) {
            
            String tgtStr=this.rm.getSymbolFromID(wordTgtUsed);
            Double measureSim = this.wsContainer.getSimValue(rhsWordStr, tgtStr);
            if (measureSim > threasholdOnSimilarity) {
                measureSim=Math.log(measureSim);
                Map<Integer, Double> lhsToUnaryParamMap = rm.getLhsToUnary(wordTgtUsed);
                Set<Map.Entry<Integer, Double>> lhsRuleParam = lhsToUnaryParamMap.entrySet();
                for (Map.Entry<Integer, Double> e : lhsRuleParam) {
                    Integer lhsSymbol = e.getKey();
                    Double thisParam = e.getValue();
                    
                    double outsideValueForUnaryLhs = 
                            ioChart.getOutsideValueFor(k - 1, k, lhsSymbol)
                            + thisParam + measureSim; 
                    this.ruleCounts.incOWUnary(lhsSymbol, wordTgtUsed,
                            outsideValueForUnaryLhs);
                }
            }
            //}
        }

    }


    /**
     * Used for the merge process to collect inside values for each non-terminal
     *
     * @return
     */
    public InOutParameterChart getIoChart() {
        return ioChart;
    }

}
