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

import input.preprocess.objects.FragmentCompact;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class InsideOutsideGauP {

    private RulesCounts ruleCounts;
    private Double ZParam;
    private final FragmentCompact fc;
    //private final RuleMapsReader rm;
    private final RuleMaps rm;
    private final InOutParameterChart ioChart;
    private final int sentLength;
    private final int nonZero;
    private final int dimension;
    private final Set<Integer> wordsUsedInLHSDetermination;

    private static final Logger LOGGER = Logger.getLogger(InsideOutsideGauP.class.getName());
    public InsideOutsideGauP(FragmentCompact fc,
            RuleMaps rm, RulesCounts ruleOuterWeight, int reducedDimension, int nz) throws Exception {
        this.dimension = reducedDimension;

        wordsUsedInLHSDetermination = new HashSet<>();
        this.fc = fc;
        this.rm = rm;
        ioChart = new InOutParameterChart(rm.getStartSymbolID());
        sentLength = fc.getTerminals().length;
        this.ruleCounts = ruleOuterWeight;
        this.nonZero = nz;
        //assert(threasholdOnSimilarity>=0);
    }

    public double getZParam() {
        return ZParam;
    }

    private void innerWeigtedUnification(int k, int tID) throws Exception {

        int[] dimensionsTID = getDimensions(tID);

        Map<Integer, Map<Integer, Double>> reversUnaryRuleMap = rm.getReversUnaryRuleMap();
       // reversUnaryRuleMap.keySet().parallelStream().forEach(wordsID -> {
             for (int wordsID : reversUnaryRuleMap.keySet()) {
            int[] dimensionsWordsID = getDimensions(wordsID);
            for (int i = 0; i < dimensionsWordsID.length; i++) {

                if (dimensionsTID[i] == dimensionsWordsID[i]) {
                    try {
                        // if (measureSim > threasholdOnSimilarity) {
                        Map<Integer, Double> lhsToUnary = rm.getLhsToUnary(wordsID);
                        Set<Map.Entry<Integer, Double>> entrySet = lhsToUnary.entrySet();
                        for (Map.Entry<Integer, Double> e : entrySet) {
                            Integer lhsSymbol = e.getKey();

                            Double param = e.getValue();
                            

                            ioChart.incInsideParam(k - 1, k, lhsSymbol,
                                    //Math.log(i+1)+
                                    param
                            );
                            // the original idea was to creat a Gaussian Std Matrix but using log space is a bit problematic here
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(InsideOutsideGauP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }
       // );

    }

    public void inside() throws Exception {
        for (int k = 1; k <= sentLength; k++) {
            if (k % 2 == 1) {
                //if (k == 1) {
                int tID = fc.getTerminals()[k - 1];

                innerWeigtedUnification(k, tID);

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
            int rhs = fc.getTerminals()[k - 1];
            if (k % 2 == 1) {
                doUpdateWeightLikeAKing(rhs, k);
            }
            Map<Integer, Double> lhsToUnaryParamMap = rm.getLhsToUnary(rhs);
            Set<Map.Entry<Integer, Double>> lhsRuleParam = lhsToUnaryParamMap.entrySet();
            for (Map.Entry<Integer, Double> e : lhsRuleParam) {
                Integer lhsSymbol = e.getKey();
                Double thisParam = e.getValue();
                double outsideValueForUnaryLhs = ioChart.getOutsideValueFor(k - 1, k, lhsSymbol) + thisParam; //iteratorUnary.value() ro bardar
                this.ruleCounts.incOWUnary(lhsSymbol, rhs, outsideValueForUnaryLhs);
            }
        }


        if (!Double.isFinite(ZParam)) {
            LOGGER.log(Level.FINE, "--> Z param " + ZParam +"Ignorting this ... investigate: " + this.fc.toHumanReadable(rm));
           

        } else {
            this.ruleCounts.incCurrentLikelihood(ZParam);
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

    private void doUpdateWeightLikeAKing(int rhsWord, int k) throws Exception {
        int rhsDims[] = getDimensions(rhsWord);
        for (int wordTgtUsed : wordsUsedInLHSDetermination) {
         // wordsUsedInLHSDetermination.parallelStream().forEach(wordTgtUsed->{
            int[] dimensionsTgt = getDimensions(wordTgtUsed);
            for (int i = 0; i < dimensionsTgt.length; i++) {

                  if (dimensionsTgt[i] == rhsDims[i]) {

                      try {
                          Map<Integer, Double> lhsToUnaryParamMap = rm.getLhsToUnary(wordTgtUsed);
                          Set<Map.Entry<Integer, Double>> lhsRuleParam = lhsToUnaryParamMap.entrySet();
                          for (Map.Entry<Integer, Double> e : lhsRuleParam) {
                              Integer lhsSymbol = e.getKey();
                              Double thisParam = e.getValue();
                              double outsideValueForUnaryLhs;
                              try {
                                  outsideValueForUnaryLhs = ioChart.getOutsideValueFor(k - 1, k, lhsSymbol) + thisParam; //iteratorUnary.value() ro bardar

                                  this.ruleCounts.incOWUnary(lhsSymbol, wordTgtUsed,
                                          //  Math.log(.3)+ // you can remove MAth.log(30)?!
                                          outsideValueForUnaryLhs);
                              } catch (Exception ex) {
                                  Logger.getLogger(InsideOutsideGauP.class.getName()).log(Level.SEVERE, null, ex);
                              }
                          }
                      } catch (Exception ex) {
                          Logger.getLogger(InsideOutsideGauP.class.getName()).log(Level.SEVERE, null, ex);
                      }
                  }
              }
        }
        //);

    }

    private int[] getDimensions(int tidInput) {
        int[] nzDims = new int[nonZero];
        int tid = tidInput;
        for (int i = 0; i < nonZero; i++) {
            tid += (tid >> i) * 49 + i;
            nzDims[i] = Math.abs(tid % this.dimension);

        }
        return nzDims;

    }

}
