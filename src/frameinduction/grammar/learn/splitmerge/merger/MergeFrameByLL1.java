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
import frameinduction.grammar.learn.IOParam;
import frameinduction.grammar.learn.IOProcessCompleteDataContainer;
import frameinduction.grammar.learn.MathUtil;
import frameinduction.settings.Settings;
import frameinduction.grammar.SymbolFractory;
import java.util.ArrayList;
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
public class MergeFrameByLL1 {

    public static RuleMaps mergeTuples(RuleMaps ruleMapsMerge, Settings settings, ArrayList<MergeTuple> reduceClusterSizesTo) throws Exception {
        int numberOfFramesVariable = ruleMapsMerge.getNumberOfFramesVariable();
//        
//        if (numberOfFramesVariable <= 3) {
//            return ruleMapsMerge;
//        }
//        int target = (int) (numberOfFramesVariable * .55);
//        System.err.println("** MERGE Target number of F random variables after merging is " + target +" from initial size of " + numberOfFramesVariable);

        Collections.reverse(reduceClusterSizesTo);
        for (int i = 0; i < reduceClusterSizesTo.size()*.25; i++) {
            MergeTuple mergeTuple = reduceClusterSizesTo.get(i);
            // for (MergeTuple cf : reduceClusterSizesTo) {
            String sym1 = mergeTuple.getSymbol1();
            String sym2 = mergeTuple.getSybmol2();
            int symbol2 = SymbolFractory.getFrameIndex(sym2);
            int symbol1 = SymbolFractory.getFrameIndex(sym1);
            Logger.getGlobal().log(Level.INFO, "Candidate frame for merging: frames "+sym1 + " and " + sym2
                    + " " + SymbolFractory.getFrameIndex(sym2)
                    + " " + SymbolFractory.getFrameIndex(sym1) +" "+mergeTuple.getLossEsitmated());

//            System.err.println("Candidate frame for merging: frames "+sym1 + " and " + sym2
//                    + " " + SymbolFractory.getFrameIndex(sym2)
//                    + " " + SymbolFractory.getFrameIndex(sym1) +" "+mergeTuple.getLossEsitmated());
            Map<Integer, Integer> map = FrameTerminalSymbolsAndSiblings.getMap(
                    symbol1, symbol2,
                    ruleMapsMerge, settings.getActiveDepr());
            RuleMapsMergeByMerge rmMerge = new RuleMapsMergeByMerge();
            ruleMapsMerge = rmMerge.ruleMapsMerge(ruleMapsMerge, map);
         //   int newNmbrOfFramesVariable = ruleMapsMerge.getNumberOfFramesVariable();
//            if (newNmbrOfFramesVariable <= target || newNmbrOfFramesVariable <4) {
//                System.err.println("Breaking from merge process ... new |F| is " + newNmbrOfFramesVariable);
//                break;
//            }
        }

        return ruleMapsMerge;
    }

    public static ArrayList<MergeTuple> getCandidMergeSymbols(
            IOProcessCompleteDataContainer estimatedParam,
            RuleMaps ruleMap) throws Exception {

        Map<Integer, IOParam> ioParamSummary = estimatedParam.getIoParamSummary();
        //Map<String, IOParam> targetMap  = new ConcurrentHashMap<>();

        List<MergeIOParamContaier> targetList = new ArrayList<>();
        for (Integer symbolID : ioParamSummary.keySet()) {
            String symbolFromID = ruleMap.getSymbolFromID(symbolID);
            if (isTargetSymbol(symbolFromID)) {
                //targetMap.put(symbolFromID,ioParamSummary.get(symbolID));
                targetList.add(new MergeIOParamContaier(ioParamSummary.get(symbolID), symbolFromID));
            }
        }
        int sumAllRulesUsed = MergeUtils.getSumAllRulesUsed(ioParamSummary);

        ConcurrentLinkedDeque<MergeTuple> tuplesList = new ConcurrentLinkedDeque<>();

        //targetMap.entrySet().forEach(terminalParam -> {
        for (int i = 0; i < targetList.size() - 1; i++) {
            MergeIOParamContaier paramContainer = targetList.get(i);
            String symbol1 = paramContainer.getSymbol();
            IOParam ioParam = paramContainer.getIoParam();
            int frequency = ioParam.getFrequency();
           
            double inside = ioParam.getInside();
            double outside = ioParam.getInside();
            double relativeFreq = Math.log(frequency * 1.0 / sumAllRulesUsed);
            double sumInsideOutsideSplit1 = inside + outside;

            //targetMap.entrySet().forEach(terminalParamInner -> {
            for (int j = i + 1; j < targetList.size(); j++) {

                MergeIOParamContaier paramContainer2 = targetList.get(j);
                String symbol2 = paramContainer2.getSymbol();
                IOParam ioParam2 = paramContainer2.getIoParam();
                //String symbol2 = terminalParamInner.getKey();
                if (!symbol1.equals(symbol2)) {

                    //IOParam ioParam2 = terminalParamInner.getValue();
                    int frequency2 = ioParam2.getFrequency();
                    double inside2 = ioParam2.getInside();
                    double outside2 = ioParam2.getInside();
                    double relativeFreq2 = Math.log(frequency2 * 1.0 / sumAllRulesUsed);
                    double sumInsideOutsideSplit2 = inside2 + outside2;
                    //double pInMerge = relativeFreq *inside + relativeFreq2 * inside2;
                    double pInMerge = MathUtil.logSumExp(relativeFreq + inside, relativeFreq2 + inside2);
                    double pOutMerge = MathUtil.logSumExp(outside, outside2);
                    double pEstimatedMerge = pInMerge * pOutMerge;
                    double pUnmerged
                            = sumInsideOutsideSplit1 + sumInsideOutsideSplit2;
                    double approaxLoss = pEstimatedMerge - pUnmerged;
                    MergeTuple mergeTuple = new MergeTuple(symbol1, symbol2, approaxLoss);
                    tuplesList.add(mergeTuple);
                } else {
                    System.err.println("Should not happen! and you can remove this if");
                }

                //  });
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
