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
//import frameinduction.grammar.RuleMapsReader;
//import gnu.trove.iterator.TIntDoubleIterator;
//import gnu.trove.iterator.TIntObjectIterator;
//import gnu.trove.map.TIntDoubleMap;
//import gnu.trove.map.TIntObjectMap;
//import gnu.trove.map.hash.TIntDoubleHashMap;
//import gnu.trove.map.hash.TIntObjectHashMap;
import input.preprocess.objects.FragmentCompact;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class InsideOutside {

    private static final Logger LOGGER = Logger.getLogger(InsideOutside.class.getName());
    private RulesCounts ruleCounts;
    private Double ZParam;
    private final FragmentCompact fc;
    //private final RuleMapsReader rm;
    private final RuleMaps rm;
    private final InOutParameterChart ioChart;
    private final int sentLength;
    

    public InsideOutside(FragmentCompact fc, RuleMaps rm, RulesCounts ruleOuterWeight) throws Exception {
        
        this.fc = fc;
        this.rm = rm;
        ioChart = new InOutParameterChart(rm.getStartSymbolID());
        sentLength = fc.getTerminals().length;
        this.ruleCounts = ruleOuterWeight;
    }

    public double getZParam() {
        return ZParam;
    }

    public void inside() throws Exception {
        for (int k = 1; k <= sentLength; k++) {
            Map<Integer, Double> lhsToUnary = rm.getLhsToUnary(fc.getTerminals()[k - 1]);
            Set<Map.Entry<Integer, Double>> iteratorUnary = lhsToUnary.entrySet();
            for (Map.Entry<Integer, Double> e : iteratorUnary) {
                int lhsSymbol = e.getKey();
                double param = e.getValue();
                ioChart.incInsideParam(k - 1, k, lhsSymbol, param);
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
                                if(insideValueForB!=null&& insideValueForC!=null){
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
        //System.err.println(ZParam);
        if(Settings.SERR_DEBUG){
        if (!Double.isFinite(ZParam)||ZParam==null) {
            if(!Double.isFinite(ZParam)){
                
                int startSymbolID = this.rm.getStartSymbolID();
                String symbolFromID = rm.getSymbolFromID(startSymbolID);
                String toHumanReadable = fc.toHumanReadable(rm);
                LOGGER.log(Level.FINE,"There is start symbol " +symbolFromID+
                        " for input "+
                        toHumanReadable
                        );
                LOGGER.log(Level.FINE,"Let's fix the error by random initialization of the value and adding it to the rm");
                
            }else{
            LOGGER.log(Level.FINE, "Ignorting this --> Z param:: " + ZParam +" for "+ this.fc.toHumanReadable(rm)+" check the cause.");
            
            }
        }
        } 


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
                                        double tempSum = out_A_i_k+thisRuleParam;
                                        double outerWForRule =tempSum + inside_B_i_j + inside_C_j_k; // thisRuleParam ro barda
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
            Map<Integer, Double> lhsToUnaryParamMap = rm.getLhsToUnary(rhs);
            Set<Map.Entry<Integer, Double>> lhsRuleParam = lhsToUnaryParamMap.entrySet();
            for(Map.Entry<Integer, Double> e: lhsRuleParam){
                Integer lhsSymbol = e.getKey();
                Double thisParam = e.getValue();
                double outsideValueForUnaryLhs = ioChart.getOutsideValueFor(k - 1, k, lhsSymbol) +thisParam; //iteratorUnary.value() ro bardar
                this.ruleCounts.incOWUnary(lhsSymbol, rhs, outsideValueForUnaryLhs);
            }
        }
        //System.err.println(ZParam);
        
        
        if (!Double.isFinite(ZParam)) {
            if(Settings.SERR_DEBUG){
             LOGGER.log(Level.FINE, "Ignorting this --> Z param:: " + ZParam +" for "+ this.fc.toHumanReadable(rm)+" check the cause.");
            }
        } else {
           this.ruleCounts.incCurrentLikelihood(ZParam);
        }
        
       
    }

    /**
     * Used for the merge process to collect inside values for each non-terminal
     * @return 
     */
    public InOutParameterChart getIoChart() {
        return ioChart;
    }
    
    
}
