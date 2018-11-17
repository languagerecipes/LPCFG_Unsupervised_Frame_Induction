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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import util.PairKeySparseSymetricValueCollapsed;

/**
 *
 * @author behra
 */
public class MergeStategies {

    public static Map<Integer, Integer> mergeSt1(
            RuleMaps ruleMapsToMerge,
            List<PairKeySparseSymetricValueCollapsed> candRndVarIDPairSet, double portion,@Deprecated int minNumeberOfVariable) throws Exception {

        Set<Integer> idsUsed = new HashSet<>();
        Set<Integer> allIDs = new HashSet<>();
        Map<Integer, Integer> fromToMap = new HashMap<>();
        for (PairKeySparseSymetricValueCollapsed ps : candRndVarIDPairSet) {
            allIDs.add(ps.getIndexC());
            allIDs.add(ps.getIndexR());
        }
//        if (allIDs.size() <= minNumeberOfVariable) {
//            System.err.println(allIDs.size() +" all ids size is " +allIDs.size() +" and break because of < " + minNumeberOfVariable);
//            return fromToMap;
//        }
      //  System.err.println("Candidate size for merge is " + candRndVarIDPairSet.size());
        for (PairKeySparseSymetricValueCollapsed ps : candRndVarIDPairSet) {
            int from = ps.getIndexC();
            int to = ps.getIndexR();
            if (!idsUsed.contains(to) && !idsUsed.contains(from)) {
                //System.err.println("++ Added merging " +from+":"+ ruleMapsToMerge.getSymbolFromID(from) +" " + to+":"+ruleMapsToMerge.getSymbolFromID(to));
                fromToMap.put(from, to);
                idsUsed.add(from);
                idsUsed.add(to);
            }
            if (idsUsed.size() >= (candRndVarIDPairSet.size() * portion)/2) {
            //    System.err.println("Breaking by threshold on usage...");
                break;
            }
        }
       // System.err.println("Merging " + fromToMap.size() + " variable pairs out of "+ candRndVarIDPairSet.size()+" for |RV|=" + allIDs.size());
        return fromToMap;
       
    }

    
    
     public static Map<Integer, Integer> mergeSt112(
            RuleMaps ruleMapsToMerge,
            List<PairKeySparseSymetricValueCollapsed> candRndVarIDPairSet, double portion,@Deprecated int minNumeberOfVariable) throws Exception {

        Set<Integer> idsUsedFrom = new HashSet<>();
        Set<Integer> idsUsedTo = new HashSet<>();
        
        Set<Integer> allIDs = new HashSet<>();
        Map<Integer, Integer> fromToMap = new HashMap<>();
        for (PairKeySparseSymetricValueCollapsed ps : candRndVarIDPairSet) {
            allIDs.add(ps.getIndexC());
            allIDs.add(ps.getIndexR());
        }
//        if (allIDs.size() <= minNumeberOfVariable) {
//            System.err.println(allIDs.size() +" all ids size is " +allIDs.size() +" and break because of < " + minNumeberOfVariable);
//            return fromToMap;
//        }
        System.err.println("Candidate size for merge is " + candRndVarIDPairSet.size());
        for (PairKeySparseSymetricValueCollapsed ps : candRndVarIDPairSet) {
            int from = ps.getIndexC();
            int to = ps.getIndexR();
//            if(idsUsedFrom.contains(from)){
//                from = fromToMap.get(from); // to what it has already been mapped
//            }
//            if (!idsUsedTo.contains(to) && !idsUsedFrom.contains(from)) {
                System.err.println("++ Added merging " +from+":"+ ruleMapsToMerge.getSymbolFromID(from) +" " + to+":"+ruleMapsToMerge.getSymbolFromID(to));
                fromToMap.put(from, to);
//                idsUsed.add(from);
//                idsUsed.add(to);
//            }
//            if (idsUsed.size() >= (candRndVarIDPairSet.size() * portion)/2) {
//                System.err.println("Breaking by threshold on usage...");
//                break;
//            }
        }
        System.err.println("Merging " + fromToMap.size() + " variable pairs out of "+ candRndVarIDPairSet.size()+" for |RV|=" + allIDs.size());
        return fromToMap;
       
    }

}
