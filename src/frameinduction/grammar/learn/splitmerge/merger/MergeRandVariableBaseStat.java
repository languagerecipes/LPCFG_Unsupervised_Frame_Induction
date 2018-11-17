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
package frameinduction.grammar.learn.splitmerge.merger;

import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.learn.IOParam;
import frameinduction.grammar.learn.InOutParameterChart;
import frameinduction.grammar.learn.MathUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

import util.PairKeySparseSymetric;
import util.PairKeySparseSymetricValueCollapsed;

/**
 *
 * @author behra
 */
public class MergeRandVariableBaseStat {

    final private Collection<InOutParameterChart> parseChartCollection;

    final private RulesCounts rc;
 //   final private RuleMaps ruleMap; // all immutable

    public MergeRandVariableBaseStat(final Collection<InOutParameterChart> parseChartCollection, final RulesCounts rc, final RuleMaps ruleMap) {
        this.parseChartCollection = Collections.unmodifiableCollection(parseChartCollection);
        this.rc = rc;
       // this.ruleMap = ruleMap;
    }

    public List<PairKeySparseSymetricValueCollapsed> getMergeCandidates(
            Set<Integer> targetIDSet) throws Exception {

        double totalFreq = rc.getSumAllSymbolFrequencies();
        Map<PairKeySparseSymetric, DoubleAdder> overlLossMap = new ConcurrentHashMap<>();
        boolean error = false;
        int totalPair =0;
        int totalError=0;
           //     System.err.println("Computing merge candidates...");// examples are listed below ...");
        for (InOutParameterChart ioChart : parseChartCollection) {
            // for each sentence

            Set<Integer> rowsIndices = ioChart.getRowsIndices();
            for (Integer row : rowsIndices) {
                Set<Integer> coloummnsIndicesForRow = ioChart.getColoummnsIndicesForRow(row);
                for (Integer col : coloummnsIndicesForRow) {

                    Map<Integer, IOParam> paramMapAt = ioChart.getParamMapAt(row, col);

                    for (Integer symbol1 : targetIDSet) {
                        if (paramMapAt.containsKey(symbol1)) {
                            IOParam symbol1IO = paramMapAt.get(symbol1);
                            double pwTForSymbol1 = symbol1IO.getInside() + symbol1IO.getOutside();
                            int symbolFreq = rc.getSymbolFreq(symbol1);
                            if (symbolFreq >0) {
                                double relativeFreqSymbol1 = Math.log(symbolFreq) - Math.log(totalFreq);
//                                if(totalPair<3){
//                                 System.err.println("pwTForSymbol1 "+ totalFreq+" "+ relativeFreqSymbol1 +" "
//                                         +  rc.getSymbolFreq(symbol1) +" --> " + 
//                                         ruleMap.getSymbolFromID(symbol1) +"   "+ Math.exp(relativeFreqSymbol1)
//                                 
//                                 );
//                                }
                                for (Integer symbol2 : targetIDSet) {
                                    if (symbol2 > symbol1) {
                                        if (paramMapAt.containsKey(symbol2)) {
                                            IOParam symbol2IO = paramMapAt.get(symbol2);
                                            double pwTForSymbol2 = symbol2IO.getInside() + symbol2IO.getOutside();
                                            double symb2Freg = rc.getSymbolFreq(symbol2);
                                            if (symb2Freg > 0) {
                                                double relativeFreqSymbol2 = Math.log(symb2Freg * 1.0 / totalFreq);

                                                double pInMerged = MathUtil.logSumExp(symbol1IO.getInside() + relativeFreqSymbol1, symbol2IO.getInside() + relativeFreqSymbol2);
                                                double poutMerged = MathUtil.logSumExp(symbol1IO.getOutside(), symbol2IO.getOutside());
                                                double sumTwoSubSymbolProbs = MathUtil.logSumExp(pwTForSymbol1, pwTForSymbol2);
                                                double totalPMerged = pInMerged + poutMerged;
                                                double deltaAtThisNForThisT = totalPMerged - sumTwoSubSymbolProbs;
                                                // now all you need to do is to keep the value for each pair in all the loops and sum them together (i.e., the multiplicatuin )

                                                totalPair++;
                                                if (Double.isFinite(deltaAtThisNForThisT)) {
                                               
                                                    updateStatMapOverlLoss(symbol1, symbol2, deltaAtThisNForThisT, overlLossMap);
                                                }
                                                //System.err.println("At " + row + " " + col + " " + ruleMap.getSymbolFromID(symbol1) + " " +ruleMap.getSymbolFromID(symbol2)+" "+ paramMapAt.get(symbol1).getFrequency() + " " + rc.getSymbolFreq(symbol1) +" " + deltaAtThisNForThisT);
                                                if (!Double.isFinite(deltaAtThisNForThisT)) {
                                                    error = true;
                                                    totalError++;
                                                    //System.err.println("Let's fix it first...");
                                                    // break;
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        if (error) {
            System.err.println("*******************ERROR in probablity computing encountered -- " + totalError +" out of " + totalPair);
        }
        List<PairKeySparseSymetricValueCollapsed> candidSortedList = new ArrayList<>();
        overlLossMap.entrySet().forEach(entry -> {
            candidSortedList.add(new PairKeySparseSymetricValueCollapsed(entry.getKey(), entry.getValue().doubleValue()));
        });
        Collections.sort(candidSortedList, PairKeySparseSymetricValueCollapsed.CMPR_BY_VALUE);

       // System.err.println("******>>>> List of candid pairs ...******************** SIZE  " + candidSortedList.size());
//        int countPrint = 0;
//        for (PairKeySparseSymetricValueCollapsed psk : candidSortedList) {
//            if (countPrint++ > 17) {
//                break;
//            }
//            System.err.println("pks "
//                    + ruleMap.getSymbolFromID(
//                            psk.getIndexR()) + " and " + ruleMap.getSymbolFromID(psk.getIndexC()) + psk.getValue());
//        }

        return candidSortedList;

    }

    private static void updateStatMapOverlLoss(Integer symbol1, Integer symbol2, double deltaAtThisNForThisT, Map<PairKeySparseSymetric, DoubleAdder> overlLossMap) {
        PairKeySparseSymetric pk = new PairKeySparseSymetric(symbol1, symbol2);
        if (overlLossMap.containsKey(pk)) {
            overlLossMap.get(pk).add(deltaAtThisNForThisT);
        } else {
            DoubleAdder doubleAdder = new DoubleAdder();
            doubleAdder.add(deltaAtThisNForThisT);
            DoubleAdder putIfAbsent = overlLossMap.putIfAbsent(pk, doubleAdder);
            if (putIfAbsent != null) {
                putIfAbsent.add(deltaAtThisNForThisT);
            }
        }
    }

}
