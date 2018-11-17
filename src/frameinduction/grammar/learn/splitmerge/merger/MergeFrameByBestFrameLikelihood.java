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
import frameinduction.grammar.learn.splitmerge.merger.RuleMapsMergeByMerge;
import frameinduction.grammar.learn.MathUtil;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.Settings;
import frameinduction.grammar.SymbolFractory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class MergeFrameByBestFrameLikelihood {
public static RuleMaps mergeTuples(RuleMaps ruleMapsMerge, Settings settings, ArrayList<MergeTuple> reduceClusterSizesTo) throws Exception {
    return  mergeTuples( ruleMapsMerge,  settings.getActiveDepr(),  reduceClusterSizesTo,.7);
}
    public static RuleMaps mergeTuples(RuleMaps ruleMapsMerge, Set<String> getActiveDepr, ArrayList<MergeTuple> reduceClusterSizesTo, double portion) throws Exception {
//        int numberOfFramesVariable = ruleMapsMerge.getNumberOfFramesVariable();
//        int target = (int) (numberOfFramesVariable * .4);
////        if (numberOfFramesVariable <= 3) {
////            return ruleMapsMerge;
////        }
//        System.err.println("Target number of F random variables after merging is " + target);

       
        for (int i = 0; i < reduceClusterSizesTo.size() * portion; i++) {
            MergeTuple mergeTuple = reduceClusterSizesTo.get(i);
            // for (MergeTuple cf : reduceClusterSizesTo) {
            String sym1 = mergeTuple.getSymbol1();
            String sym2 = mergeTuple.getSybmol2();
            int symbol2 = SymbolFractory.getFrameIndex(sym2);
            int symbol1 = SymbolFractory.getFrameIndex(sym1);
            Logger.getGlobal().log(Level.INFO, "Candidate frame for merging: frames " + sym1 + " and " + sym2
                    + " " + SymbolFractory.getFrameIndex(sym2)
                    + " " + SymbolFractory.getFrameIndex(sym1) + " " + mergeTuple.getLossEsitmated());

            System.err.println("Candidate frame for merging: frames " + sym1 + " and " + sym2
                    + " " + SymbolFractory.getFrameIndex(sym2)
                    + " " + SymbolFractory.getFrameIndex(sym1) + " " + mergeTuple.getLossEsitmated());
            Map<Integer, Integer> map = FrameTerminalSymbolsAndSiblings.getMap(
                    symbol1, symbol2,
                    ruleMapsMerge, getActiveDepr);
            RuleMapsMergeByMerge rmMerge = new RuleMapsMergeByMerge();
            ruleMapsMerge = rmMerge.ruleMapsMerge(ruleMapsMerge, map);
            int newNmbrOfFramesVariable = ruleMapsMerge.getNumberOfFramesVariable();
//            if (newNmbrOfFramesVariable <= target) {
//                break;
//            }
        }

        return ruleMapsMerge;
    }

    public static ArrayList<MergeTuple> getCandidMergeSymbolsFromBest(
            //IOProcessCompleteDataContainer estimatedParam,
            Collection<ParseFrame> parseFrameList,
            RuleMaps ruleMap) throws Exception {

    //    Map<Integer, List<ParseFrame>> frameByCategory = new HashMap<>();
        Map<Integer, Double> frameStatSummary = new HashMap<>();
        Map<Integer, Integer> frameStatCount = new HashMap<>();
        int totalNumberOfFrames = parseFrameList.size();
        
        for (ParseFrame pf : parseFrameList) {
//            if (frameByCategory.containsKey(frameNumber)) {
//                frameByCategory.get(frameNumber).add(pf);
//            } else {
//                List<ParseFrame> pfList = new ArrayList<>();
//                pfList.add(pf);
//                frameByCategory.put(frameNumber, pfList);
//            }

            int frameNumber = pf.getFrameClusterNumber();
            if (frameStatSummary.containsKey(frameNumber)) {
                Integer get = frameStatCount.get(frameNumber);
               
                frameStatCount.put(frameNumber,++get);
                double pfLL = pf.getLikelihood();
                double fls = frameStatSummary.get(frameNumber);
                double ll =
                       // MathUtil.logSumExp(fls,pfLL);
                fls+pfLL;
                frameStatSummary.put(frameNumber, ll);
                
            } else {
                frameStatCount.put(frameNumber,1);
                frameStatSummary.put(frameNumber, pf.getLikelihood());
            }
            
        }
        
        
        
        //Map<Integer, IOParam> ioParamSummary = estimatedParam.getIoParamSummary();
        //Map<String, IOParam> targetMap  = new ConcurrentHashMap<>();
//
//        List<MergeIOParamContaier> targetList = new ArrayList<>();
//        for (Integer symbolID : ioParamSummary.keySet()) {
//            String symbolFromID = ruleMap.getSymbolFromID(symbolID);
//            if (isTargetSymbol(symbolFromID)) {
//                //targetMap.put(symbolFromID,ioParamSummary.get(symbolID));
//                targetList.add(new MergeIOParamContaier(ioParamSummary.get(symbolID), symbolFromID));
//            }
//        }
     //   int sumAllRulesUsed = MergeUtils.getSumAllRulesUsed(ioParamSummary);

        ConcurrentLinkedDeque<MergeTuple> tuplesList = new ConcurrentLinkedDeque<>();

        //targetMap.entrySet().forEach(terminalParam -> {
        for (int frameID1 : frameStatCount.keySet()) {
            Integer countFrame1 = frameStatCount.get(frameID1);
            double sumProbabInsideFrame1 = frameStatSummary.get(frameID1);
            double portionFrame1 = countFrame1 * 1.0; // / totalNumberOfFrames;
            for (int frameID2 : frameStatCount.keySet()) {
                
                if (frameID1 > frameID2) {
                    // filter this one   

                    Integer countFrame2 = frameStatCount.get(frameID2);
                    double sumProbabInsideFrame2 = frameStatSummary.get(frameID2);
                    double portionFrame2 = countFrame2 * 1.0;// /totalNumberOfFrames;
                    double probabUnmerged = MathUtil.logSumExp(sumProbabInsideFrame1, sumProbabInsideFrame2);;
                    double insideForMerged = MathUtil.logSumExp(
                            Math.log(portionFrame1) + sumProbabInsideFrame1,
                            Math.log(portionFrame2) + sumProbabInsideFrame2); // good results with having the log around portions

                    //  double approxProbFinal = insideForMerged;// +  MathUtil.logSumExp(1, 1); // good results obtained by adding 2 here, since it is a scaling factor it can be removed
                    double approaxLoss = MathUtil.logSumExp(insideForMerged, -probabUnmerged);
                    double loss = insideForMerged-probabUnmerged;
                    String symbol1 = SymbolFractory.getHeadFrameSymbol(frameID1);
                    String symbol2 = SymbolFractory.getHeadFrameSymbol(frameID2);

                    MergeTuple mergeTuple = new MergeTuple(symbol1, symbol2, loss);
                    tuplesList.add(mergeTuple);
//                } else {
//                    System.err.println("Should not happen! and you can remove this if");
//                }

                    //  });
                }
            }
        }
        //);
        ArrayList<MergeTuple> arrayList = new ArrayList<>(tuplesList);
        Collections.sort(arrayList);

        return arrayList;

    }

    public static List<String> getFinalSymbolListForFilter(ArrayList<MergeTuple> arrayList) {
        int toremoveInital = (int) Math.floor(arrayList.size() / 2);
        
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < toremoveInital; i++) {
            MergeTuple get = arrayList.get(i);
            updateMap(get.getSymbol1(), map);
            updateMap(get.getSybmol2(), map);

        }
        List<Entry<String, Integer>> entriesSortedByValues = entriesSortedByValues(map);
        int toremove = (int) Math.floor(entriesSortedByValues.size() / 2);
        System.out.println("toRemove size " + toremove);
        List<String> toRemove = new ArrayList<>();
        for (int i = 0; i < toremove; i++) {
            String key = entriesSortedByValues.get(i).getKey();
            toRemove.add(key);

        }
        for (String k : toRemove) {
            System.out.println(k);
        }
        return toRemove;
    }

    private static void updateMap(String s, Map<String, Integer> counterMap) {
        if (counterMap.containsKey(s)) {
            Integer get = counterMap.get(s) + 1;
            counterMap.put(s, get);
        } else {
            counterMap.put(s, 1);
        }
    }

    static <K, V extends Comparable<? super V>>
            List<Entry<K, V>> entriesSortedByValues(Map<K, V> map) {

        List<Entry<K, V>> sortedEntries = new ArrayList<Entry<K, V>>(map.entrySet());

        Collections.sort(sortedEntries,
                new Comparator<Entry<K, V>>() {
            @Override
            public int compare(Entry<K, V> e1, Entry<K, V> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        }
        );

        return sortedEntries;
    }

    /**
     * Method where the variable is filtered
     *
     * @param symbolFromID
     * @return
     */
    private static boolean isTargetSymbol(String symbolFromID) {
        return SymbolFractory.isFrameHeadRule(symbolFromID);
    }

}
