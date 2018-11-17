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
import frameinduction.grammar.learn.MathUtil;
import frameinduction.settings.Settings;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Behrang QasemiZadeh <me at atmykitchen.info>
 */
public class RuleMapsMergeByMerge {

    /**
     * Create rule map by removing symbols that are listed in the @param
     * symbolsToFilter
     *
     * @param rm
     * @param margeSymbolMapFromTo
     * @param symbolsToFilter
     * @return 
     * @throws Exception
     */
     public RuleMaps ruleMapsMerge(RuleMaps rm, Map<Integer, Integer> margeSymbolMapFromTo) throws Exception {
        // this.startSymbolID = rm.getStartSymbolID();

        RuleMaps rmOut = RuleMaps.partialRuleMapCopy(rm);
        // RuleMaps rmOut = new RuleMaps();
        //  rmOut.instNewReverseMaps();
        Map<Integer, Map<Integer, Double>> mergedUnaryMap = mergeUnaryMap(rm, margeSymbolMapFromTo);
        Map<Integer, Map<Integer, Map<Integer, Double>>> mergeBinaryMap = mergeBinaryMap(rm, margeSymbolMapFromTo);

        rmOut.setReversUnaryRuleMap(mergedUnaryMap);
        rmOut.setReverseBinaryRuleMap(mergeBinaryMap);
        for (Integer mapFrom : margeSymbolMapFromTo.keySet()) {
            rmOut.removeSympolIDMapping(mapFrom);
        }
        return rmOut;
    }

    /**
     * Main method to merge non-terminals in an unary map NEW: double check
     * Behrang
     *
     * @param mergeMap
     * @throws Exception
     */
    private synchronized Map<Integer, Map<Integer, Double>> mergeUnaryMap(RuleMaps rm, Map<Integer, Integer> mergeMap) throws Exception {
        Map<Integer, Map<Integer, Double>> newReversUnaryRuleMap = new ConcurrentHashMap<>();
        Map<Integer, Map<Integer, Double>> reversUnaryRuleMap = rm.getReversUnaryRuleMap();
        for (Integer rhs : reversUnaryRuleMap.keySet()) {

            if (!mergeMap.containsKey(rhs)) {
                Set<Map.Entry<Integer, Double>> get = reversUnaryRuleMap.get(rhs).entrySet();

                get.forEach((Map.Entry<Integer, Double> lhsEntry) -> {
                    if (!mergeMap.containsKey(lhsEntry.getKey())) {
                        updateMergeUnaryMapBySum(rhs, lhsEntry.getKey(), lhsEntry.getValue(), newReversUnaryRuleMap);
                    } else {

//                        System.err.println("+ Merged symbol " + lhsEntry.getKey() + " " + mergeMap.get(lhsEntry.getKey()));
//                        try{
//                        System.err.println("+ Merged symbol " + rm.getSymbolFromID(lhsEntry.getKey()) + " " + rm.getSymbolFromID(mergeMap.get(lhsEntry.getKey())));
//                        }catch (Exception e){
//                            System.err.println(e);
//                        }
                        Integer newLhs = mergeMap.get(lhsEntry.getKey());
                        updateMergeUnaryMapBySum(rhs, newLhs, lhsEntry.getValue(), newReversUnaryRuleMap);
                    }
                });
            } else {
                Integer newRHS = mergeMap.get(rhs);
                //System.err.println("+ Merged symbol " + rhs + " " + mergeMap.get(rhs));
                Set<Map.Entry<Integer, Double>> get = reversUnaryRuleMap.get(rhs).entrySet();

                get.forEach((Map.Entry<Integer, Double> lhsEntry) -> {
                    if (!mergeMap.containsKey(lhsEntry.getKey())) {
                        updateMergeUnaryMapBySum(newRHS, lhsEntry.getKey(), lhsEntry.getValue(), newReversUnaryRuleMap);
                    } else {
                        Integer newLhs = mergeMap.get(lhsEntry.getKey());
                        updateMergeUnaryMapBySum(newRHS, newLhs, lhsEntry.getValue(), newReversUnaryRuleMap);
                    }
                });

            }
        }
        return newReversUnaryRuleMap;
    }

    /**
     * Main method to merge nodes from a binary map NEW: double check Behrang
     *
     * @param mergeMap
     * @throws Exception
     */
    private synchronized Map<Integer, Map<Integer, Map<Integer, Double>>> mergeBinaryMap(RuleMaps rm, Map<Integer, Integer> mergeMap) throws Exception {
        Map<Integer, Map<Integer, Map<Integer, Double>>> newReversUnaryRuleMap = new ConcurrentHashMap<>();
        Map<Integer, Map<Integer, Map<Integer, Double>>> reversUnaryRuleMap = rm.getReverseBinaryRuleMap();
        for (Integer rhs1 : reversUnaryRuleMap.keySet()) {
            Set<Entry<Integer, Map<Integer, Double>>> entrySetRHS2LHS = reversUnaryRuleMap.get(rhs1).entrySet();
            if (mergeMap.containsKey(rhs1)) {
                Integer newRHS1 = mergeMap.get(rhs1);
                updateMergeInnerBinaryMap(newRHS1, entrySetRHS2LHS, mergeMap, newReversUnaryRuleMap);

            } else {
                updateMergeInnerBinaryMap(rhs1, entrySetRHS2LHS, mergeMap, newReversUnaryRuleMap);

            }
        }
        return newReversUnaryRuleMap;
    }

    /**
     * Inner method to merge binary map
     *
     * @param rhs1
     * @param entrySetRHS2LHS
     * @param mergeMap
     * @param newReversUnaryRuleMap
     */
    private synchronized void updateMergeInnerBinaryMap(int rhs1,
            Set<Entry<Integer, Map<Integer, Double>>> entrySetRHS2LHS,
            Map<Integer, Integer> mergeMap, Map<Integer, Map<Integer, Map<Integer, Double>>> newReversUnaryRuleMap) {
        entrySetRHS2LHS.forEach((Entry<Integer, Map<Integer, Double>> rhs2Map) -> {
            Integer rhs2 = rhs2Map.getKey();
            Map<Integer, Double> lhsMap = rhs2Map.getValue();
            if (mergeMap.containsKey(rhs2)) {
                Integer newRHS2 = mergeMap.get(rhs2);
                lhsMap.entrySet().forEach(lhsParamMap -> {
                    Integer lhs = lhsParamMap.getKey();
                    Double param = lhsParamMap.getValue();
                    if (mergeMap.containsKey(lhs)) {
                        Integer newLHS = mergeMap.get(lhs);
                        updateMergeBinaryMapBySum(rhs1, newRHS2, newLHS, param, newReversUnaryRuleMap);
                    } else {
                        updateMergeBinaryMapBySum(rhs1, newRHS2, lhs, param, newReversUnaryRuleMap);
                    }

                });
            } else {
                lhsMap.entrySet().forEach(lhsParamMap -> {
                    Integer lhs = lhsParamMap.getKey();
                    Double param = lhsParamMap.getValue();
                    if (mergeMap.containsKey(lhs)) {
                        Integer newLHS = mergeMap.get(lhs);
                        updateMergeBinaryMapBySum(rhs1, rhs2, newLHS, param, newReversUnaryRuleMap);
                    } else {
                        updateMergeBinaryMapBySum(rhs1, rhs2, lhs, param, newReversUnaryRuleMap);
                    }

                });

            }
        });
    }

    /**
     * Method to update unary map by summing param of node which is going to be
     * merged
     *
     * @param newrhs
     * @param newlhs
     * @param weight
     * @param newReversUnaryRuleMap
     */
    private static synchronized void updateMergeUnaryMapBySum(int rhs, int lhs, double weight, Map<Integer, Map<Integer, Double>> newReversUnaryRuleMap) {

        if(!Double.isFinite(weight)){
            if(Settings.SERR_DEBUG){
            System.err.println("Merege  ... Here is NaN problem code:36562");
            }
          weight =  Double.MIN_VALUE;
        }
        if (newReversUnaryRuleMap.containsKey(rhs)) {
            Map<Integer, Double> getLHSSet = newReversUnaryRuleMap.get(rhs);
            if (getLHSSet.containsKey(lhs)) {
                Double get = getLHSSet.get(lhs);
                // maybe the sum is not required?!
                double logSumExp = MathUtil.logSumExp(get, weight)-Math.log(2.0);
                if (Double.isFinite(logSumExp)) {
                    getLHSSet.put(lhs, logSumExp);
                } else {
                    System.err.println("Here is a NaN value code:476372");
                }
            } else {
                // here you need put if absent etc.

                getLHSSet.put(lhs, weight);
            }

        } else {

            Map<Integer, Double> lhsToThisrhsMap = new ConcurrentHashMap<>();
            lhsToThisrhsMap.put(lhs, weight);
            newReversUnaryRuleMap.put(rhs, lhsToThisrhsMap);

        }
    }

    /**
     * Method to merge a binary map with the given param from the item which
     * must be merged Maybe here add some interpolation for the weights?!
     *
     * @param newRHS1
     * @param newRHS2
     * @param newLHS
     * @param param
     * @param newReversBinaryRuleMap
     */
    private synchronized void updateMergeBinaryMapBySum(Integer rhs1,
            Integer rhs2,
            Integer lhs,
            Double param,
            Map<Integer, Map<Integer, Map<Integer, Double>>> newReversBinaryRuleMap) {
         if(!Double.isFinite(param)){
             if(Settings.SERR_DEBUG){
            System.err.println("Merege  ... Here is NaN problem code:364562");
            param=Math.log(.0000000000000001);
             }
        }
        if (newReversBinaryRuleMap.containsKey(rhs1)) {
            Map<Integer, Map<Integer, Double>> rhs2LHSParamMap = newReversBinaryRuleMap.get(rhs1);
            if (rhs2LHSParamMap.containsKey(rhs2)) {
                Map<Integer, Double> lhsTOParam = rhs2LHSParamMap.get(rhs2);
                if (lhsTOParam.containsKey(lhs)) {
                    Double param1 = lhsTOParam.get(lhs);
                    double logSumExp = MathUtil.logSumExp(param1, param);//-Math.log(2.0);
                    if (!Double.isFinite(logSumExp)) {
                        if(Settings.SERR_DEBUG){
                        System.err.println("Merege  ... Here is NaN problem code:364562");
                        }
                    }
                    lhsTOParam.put(lhs, logSumExp);
                } else {
                    lhsTOParam.put(lhs, param);
                }
            } else {
                Map<Integer, Double> lhsTOParam = new ConcurrentHashMap<>();
                lhsTOParam.put(lhs, param);
                // add put if absent
                rhs2LHSParamMap.put(rhs2, lhsTOParam);
            }
        } else {
            Map<Integer, Map<Integer, Double>> rhs2LHSParamMap = new ConcurrentHashMap<>();
            Map<Integer, Double> lhsTOParam = new ConcurrentHashMap<>();
            lhsTOParam.put(lhs, param);
            // add put if absent
            rhs2LHSParamMap.put(rhs2, lhsTOParam);
            newReversBinaryRuleMap.put(rhs1, rhs2LHSParamMap);
        }

    }

}
