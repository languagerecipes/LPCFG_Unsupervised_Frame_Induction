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
package frameinduction.grammar;

import frameinduction.grammar.learn.MathUtil;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.DoubleAdder;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class RulesCounts {

    // outer weights for rules
    private final Map<Integer, Map<Integer, DoubleAccumulator>> owUnaryRules;
    private final Map<Integer, Map<Integer, Map<Integer, DoubleAccumulator>>> owBinaryRules;
    private Map<Integer, AtomicInteger> symbolFrequencyCounter;
    private final AtomicInteger countAllRules;
    private final DoubleAdder currentLikelihood;

    public RulesCounts() {
        
        countAllRules = new AtomicInteger(0);
        currentLikelihood = new DoubleAdder();
        owUnaryRules = new ConcurrentHashMap<>();
        owBinaryRules = new ConcurrentHashMap<>();
        symbolFrequencyCounter = new ConcurrentHashMap<>();
    }

    private void updateSymbolCounter(Integer lhsSymbol, int freq) {
        if (this.symbolFrequencyCounter.containsKey(lhsSymbol)) {
            symbolFrequencyCounter.get(lhsSymbol).addAndGet(freq);
        } else {
            AtomicInteger ai = new AtomicInteger(freq);
            AtomicInteger putIfAbsent = symbolFrequencyCounter.putIfAbsent(lhsSymbol, ai);
            if (putIfAbsent != null) {
                putIfAbsent.addAndGet(freq);
            }
        }
    }

    public synchronized void incOWUnary(int lhs, int rhs, double owp) {
        updateSymbolCounter(lhs, 1); /// assuming that each inc is due to visiting this lhs
        if (owUnaryRules.containsKey(lhs)) {
            Map<Integer, DoubleAccumulator> rhsMapToLhs = owUnaryRules.get(lhs);
            if (rhsMapToLhs.containsKey(rhs)) {
               // DoubleAccumulator get = 
                        rhsMapToLhs.get(rhs).accumulate(owp);
                //get.accumulate(owp);

//                DoubleAccumulator putIfAbsent = rhsMapToLhs.putIfAbsent(rhs, get); // can be remived safely
//                if(putIfAbsent!=null){
//                
//                }
            } else {
                countAllRules.incrementAndGet();
                DoubleAccumulator doubleLogAcc = MathUtil.getDoubleLogAcc();
                doubleLogAcc.accumulate(owp);
                DoubleAccumulator putIfAbsent = rhsMapToLhs.putIfAbsent(rhs, doubleLogAcc);
                if(putIfAbsent!=null){
                    putIfAbsent.accumulate(owp);
                }
            }

        } else {
            Map<Integer, DoubleAccumulator> rhsMapToLhs = new ConcurrentHashMap<>();

            DoubleAccumulator putIfAbsent = rhsMapToLhs.putIfAbsent(rhs, MathUtil.getDoubleLogAcc(owp));
            if(putIfAbsent!=null){
                putIfAbsent.accumulate(owp);
            }
            Map<Integer, DoubleAccumulator> putIfAbsent1 = owUnaryRules.putIfAbsent(lhs, rhsMapToLhs);
            if(putIfAbsent1!=null){
                rhsMapToLhs.entrySet().forEach(entry->{
                    Integer key = entry.getKey();
                    DoubleAccumulator value = entry.getValue();
                    
                    DoubleAccumulator putIfAbsent2 = putIfAbsent1.putIfAbsent(key, value);
                    if (putIfAbsent2 != null) {
                        putIfAbsent2.accumulate(value.doubleValue());
                    }
                    
                });
            }

        }
    }

    public synchronized void incOWBinary(int lhs, int rhs1, int rhs2, double owp) {
        updateSymbolCounter(lhs, 1); /// assuming that each inc is due to visiting this lhs
        if (owBinaryRules.containsKey(lhs)) {
            Map<Integer, Map<Integer, DoubleAccumulator>> rhs1toRhs2Map = owBinaryRules.get(lhs);
            if (rhs1toRhs2Map.containsKey(rhs1)) {
                Map<Integer, DoubleAccumulator> rhs2Map = rhs1toRhs2Map.get(rhs1);
                if (rhs2Map.containsKey(rhs2)) {
                    DoubleAccumulator eralierWeight = rhs2Map.get(rhs2);
                    eralierWeight.accumulate(owp);
                    rhs2Map.put(rhs2, eralierWeight); // can be remived safely
                } else {
                    rhs2Map.put(rhs2, MathUtil.getDoubleLogAcc(owp));

                }

            } else {
                Map<Integer, DoubleAccumulator> rhs2Map = new ConcurrentHashMap<>();
                rhs2Map.put(rhs2, MathUtil.getDoubleLogAcc(owp));
                rhs1toRhs2Map.put(rhs1, rhs2Map);

            }
        } else {
            Map<Integer, Map<Integer, DoubleAccumulator>> rhs1toRhs2Map = new ConcurrentHashMap<>();
            Map<Integer, DoubleAccumulator> rhs2MapToLhs = new ConcurrentHashMap<>();
            rhs2MapToLhs.put(rhs2, MathUtil.getDoubleLogAcc(owp));
            rhs1toRhs2Map.put(rhs1, rhs2MapToLhs);
            owBinaryRules.put(lhs, rhs1toRhs2Map);

        }
    }

    /**
     * Based on the fact that the current likelihood is the sum of
     *
     * @param sentLL
     * @param currentLikelihood
     */
    public synchronized void incCurrentLikelihood(double zParamSentLL) {
        //this.currentLikelihood *= zParamSentLL;
        this.currentLikelihood.add(zParamSentLL); //+= zParamSentLL;

    }

    public Map<Integer, Map<Integer, Map<Integer, DoubleAccumulator>>> getOwBinaryRules() {
        return Collections.unmodifiableMap(owBinaryRules);
    }

    public Map<Integer, Map<Integer, DoubleAccumulator>> getOwUnaryRules() {
        return Collections.unmodifiableMap(owUnaryRules);
    }

    public double getCurrentLikelihood() {

        return currentLikelihood.doubleValue();
    }

    /**
     * A util method to get cardinality of rules, there is no other use to this
     *
     * @return
     */
    public int getRuleCardinalityUnaries() {
        int count = 0;
        for (int key : this.owUnaryRules.keySet()) {
            count += this.owUnaryRules.get(key).size();
        }
        return count;
    }

    public int getRuleCardinalityBinaries() {
        int count = 0;
        for (int key : this.owBinaryRules.keySet()) {
            Map<Integer, Map<Integer, DoubleAccumulator>> get = owBinaryRules.get(key);
            for (int key2 : get.keySet()) {
                count += get.get(key2).size();
            }
        }
        return count;
    }

    /**
     * Add one rule counts map to another (this is used for multi-thread
     * scenario)
     *
     * @param ruleCounts
     * @param threshold
     */
    public synchronized void addRuleCounts(RulesCounts ruleCounts, @Deprecated double threshold) {
        this.currentLikelihood.add(ruleCounts.getCurrentLikelihood()); //+= ruleCounts.getCurrentLikelihood();
        // add Unaries
        for (int ruleLhs : ruleCounts.owUnaryRules.keySet()) {
            Map<Integer, DoubleAccumulator> newRhsSet = ruleCounts.owUnaryRules.get(ruleLhs);
            if (this.owUnaryRules.containsKey(ruleLhs)) {
                Map<Integer, DoubleAccumulator> rhsSetWeight = this.owUnaryRules.get(ruleLhs);
                Set<Map.Entry<Integer, DoubleAccumulator>> entrySet = newRhsSet.entrySet();
                Iterator<Map.Entry<Integer, DoubleAccumulator>> iterator = entrySet.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, DoubleAccumulator> nextRHS = iterator.next();
                    int currentKey = nextRHS.getKey();
                    DoubleAccumulator newWeight = nextRHS.getValue();
                    //if (Double.compare(newWeight, threshold) > 0) {
                    if (rhsSetWeight.containsKey(currentKey)) {
                        DoubleAccumulator wO = rhsSetWeight.get(currentKey);
                        wO.accumulate(newWeight.doubleValue());
                        // rhsSetWeight.put(currentKey, wO);
                    } else {
                        DoubleAccumulator putIfAbsent = rhsSetWeight.putIfAbsent(currentKey, newWeight);
                        if(putIfAbsent!=null){
                            putIfAbsent.accumulate(newWeight.doubleValue());
                        }
                    }
                    // }
                }
            } else {
                this.owUnaryRules.put(ruleLhs, newRhsSet);
            }
        }
        // add binaries
        for (int ruleLhs : ruleCounts.owBinaryRules.keySet()) {
            Map<Integer, Map<Integer, DoubleAccumulator>> newRhsSet = ruleCounts.owBinaryRules.get(ruleLhs);
            if (this.owBinaryRules.containsKey(ruleLhs)) {

                Map<Integer, Map<Integer, DoubleAccumulator>> oRhsSet = this.owBinaryRules.get(ruleLhs);
                Set<Map.Entry<Integer, Map<Integer, DoubleAccumulator>>> entrySet = newRhsSet.entrySet();
                Iterator<Map.Entry<Integer, Map<Integer, DoubleAccumulator>>> iterator = entrySet.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, Map<Integer, DoubleAccumulator>> nextRHSset = iterator.next();
                    int currentKeyRhs1 = nextRHSset.getKey();
                    Map<Integer, DoubleAccumulator> rhs2SetMapNew = nextRHSset.getValue();

                    if (oRhsSet.containsKey(currentKeyRhs1)) {
                        Map<Integer, DoubleAccumulator> rhs2setO = oRhsSet.get(currentKeyRhs1);
                        for (Iterator<Map.Entry<Integer, DoubleAccumulator>> it = rhs2SetMapNew.entrySet().iterator(); it.hasNext();) {
                            Map.Entry<Integer, DoubleAccumulator> newEntry = it.next();
                            Integer key = newEntry.getKey();
                            DoubleAccumulator newWeight = newEntry.getValue();
                            // if (Double.compare(newWeight, threshold) >0) {
                            if (rhs2setO.containsKey(key)) {
                                DoubleAccumulator oldWeight = rhs2setO.get(key);
                                oldWeight.accumulate(newWeight.doubleValue());
                                rhs2setO.put(key, oldWeight);
                            } else {
                                DoubleAccumulator putIfAbsent = rhs2setO.putIfAbsent(key, newWeight);
                                if(putIfAbsent!=null){
                                    putIfAbsent.accumulate(newWeight.doubleValue());
                                }
                            }
                            //  }
                        }

                    } else {
                        oRhsSet.put(currentKeyRhs1, rhs2SetMapNew);

                    }
                }
            } else {
                this.owBinaryRules.put(ruleLhs, newRhsSet);
            }
        }

        // add freqs
        ruleCounts.symbolFrequencyCounter.keySet().forEach(key -> {
            if (this.symbolFrequencyCounter.containsKey(key)) {
                symbolFrequencyCounter.get(key).addAndGet(ruleCounts.symbolFrequencyCounter.get(key).intValue());
            } else {
                AtomicInteger putIfAbsent = this.symbolFrequencyCounter.putIfAbsent(key, new AtomicInteger(ruleCounts.symbolFrequencyCounter.get(key).intValue()));
                if (putIfAbsent != null) {
                    // this should never happen since the keys are unqieu
                    putIfAbsent.addAndGet(ruleCounts.symbolFrequencyCounter.get(key).intValue());
                }
            }
        });

    }

    public synchronized int getSymbolFreq(Integer symbol) {
        return this.symbolFrequencyCounter.getOrDefault(symbol, new AtomicInteger(0)).intValue();
    }
    public synchronized double getSumAllSymbolFrequencies() {
        DoubleAdder aiSum = new DoubleAdder();
        
        this.symbolFrequencyCounter.values().forEach(ai->{
            aiSum.add(ai.intValue());
        });
        return aiSum.doubleValue();
    }
    public synchronized double getSumAllSymbolFrequencies(Set<Integer> forThisSet) {
        DoubleAdder aiSum = new DoubleAdder();
        
        this.symbolFrequencyCounter.entrySet().forEach(e->{
            if(forThisSet.contains(e.getKey())){
            AtomicInteger ai = e.getValue();
            aiSum.add(ai.intValue());
            }
        });
        return  aiSum.doubleValue();
    }

//    public void pruneRuleMap() {
//        int countRemoveUnary = 0;
//        Set<Map.Entry<Integer, Map<Integer, DoubleAccumulator>>> entrySet = this.owUnaryRules.entrySet();
//        Iterator<Map.Entry<Integer, Map<Integer, DoubleAccumulator>>> iteratorUnary = entrySet.iterator();
//        while (iteratorUnary.hasNext()) {
//            Map.Entry<Integer, Map<Integer, DoubleAccumulator>> next = iteratorUnary.next();
//            Map<Integer, DoubleAccumulator> innerMap = next.getValue();
//            Iterator<Map.Entry<Integer, Double>> iteratorInner = innerMap.entrySet().iterator();
//            while (iteratorInner.hasNext()) {
//                Map.Entry<Integer, Double> innerRule = iteratorInner.next();
//                if (innerRule.getValue() == 0.0) {
//                    iteratorInner.remove();
//                    countRemoveUnary++;
//                }
//            }
//        }
//
//        int countRemoveBinary = 0;
//        Iterator<Map.Entry<Integer, Map<Integer, Map<Integer, Double>>>> iteratorBin = this.owBinaryRules.entrySet().iterator();
//        while (iteratorBin.hasNext()) {
//            Iterator<Map.Entry<Integer, Map<Integer, Double>>> iteratorBinIn = iteratorBin.next().getValue().entrySet().iterator();
//            while (iteratorBinIn.hasNext()) {
//                Iterator<Map.Entry<Integer, Double>> iteratorRhs2 = iteratorBinIn.next().getValue().entrySet().iterator();
//                while (iteratorRhs2.hasNext()) {
//                    Map.Entry<Integer, Double> next = iteratorRhs2.next();
//                    if (next.getValue() == 0.0) {
//                        iteratorRhs2.remove();
//                        countRemoveBinary++;
//                    }
//                }
//            }
//        }
//
//       // System.out.println("\tRemoved Rules: Unaries: " + countRemoveUnary +"\tBinaries: " + countRemoveBinary);
//
//    }
}
