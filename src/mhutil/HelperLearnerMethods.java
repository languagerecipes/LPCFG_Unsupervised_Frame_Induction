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
package mhutil;

import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.learn.IOParam;
import frameinduction.grammar.learn.IOProcessCompleteDataContainer;
import frameinduction.grammar.learn.InOutParameterChart;
import frameinduction.grammar.learn.InsideOutside;
import frameinduction.grammar.learn.InsideOutsideEmbedding;
import frameinduction.grammar.learn.InsideOutsideGauP;
import frameinduction.grammar.learn.MTInsideOutsideCallable;
import frameinduction.grammar.learn.MTInsideOutsideCallableWithIOParamDetail;
import frameinduction.grammar.learn.MTInsideOutsideCallableWithIOParamSummary;
import frameinduction.grammar.learn.MTInsideOutsideGauPCallable;
import frameinduction.grammar.learn.splitmerge.merger.MergeUtils;
import frameinduction.grammar.parse.HelperParseMethods;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.FragmentCompact;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.CollectionUtil;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class HelperLearnerMethods {

    /**
     * This is the inner estimation processing using the java 8 lambda
     * parallelStream().forEach
     *
     * @param theRuleMap
     * @param fragmentsAll
     * @param threadHasNoUSe
     * @return
     */
    public static RulesCounts estimateParameters(RuleMaps theRuleMap, Collection<Fragment> fragmentsAll) {
        /// from here 
//        if(true){
//        return estimateParametersGauP(theRuleMap, fragmentsAll, 100, 10);
//        }
        final RuleMaps rmToUse = new RuleMaps(theRuleMap);
        RulesCounts rsToKeep = new RulesCounts();
        //  AtomicInteger ai = new AtomicInteger();
        fragmentsAll.parallelStream().unordered()
                .forEach(f -> {

                    try {
                        InsideOutside io = new InsideOutside(new FragmentCompact(f, rmToUse), rmToUse, rsToKeep);
                        io.inside();
                        io.outsideAlgorithm();
//                        if (ai.incrementAndGet() > 1000) {
//                            ai.set(0);
//                          //  System.err.println("Updating parameters in batch of 1000");
//                            synchronized (rmToUse) {
//                                rmToUse.updateParametersPartial(rsToKeep, .2);
//                            }
//                        }
                    } catch (Exception ex) {
                        
                        
                        Logger.getLogger(HelperLearnerMethods.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
        return rsToKeep;
    }

    public static RulesCounts estimateParametersGauP(RuleMaps theRuleMap,
            Collection<Fragment> fragmentsAll, int reducedDimension, int nz) {
        /// from here 
        final RuleMaps rmToUse = new RuleMaps(theRuleMap);
        RulesCounts rsToKeep = new RulesCounts();

        fragmentsAll.parallelStream()
                .forEach(f -> {

                    try {
                        InsideOutsideGauP io = new InsideOutsideGauP(
                                new FragmentCompact(f, rmToUse), rmToUse,
                                rsToKeep, reducedDimension, nz);
                        io.inside();
                        io.outsideAlgorithm();
                    } catch (Exception ex) {
                       Logger.getLogger(HelperLearnerMethods.class.getName()).log(Level.SEVERE, null, ex);
                       
                    }
                });
        return rsToKeep;
    }

    public static RulesCounts estimateParametersGauPInThread(int threadSize, RuleMaps theRuleMap,
            Collection<Fragment> inputFramentList, int reducedDimension, int nonZeroElement) throws InterruptedException, ExecutionException {
        /// from here
        int minThread = Math.min(threadSize, Math.floorDiv(inputFramentList.size(), 15) + 1);
        ExecutorService executor = Executors.newFixedThreadPool(minThread);
        int portion = Math.floorDiv(inputFramentList.size(), threadSize) + 1;
        Collection<Collection<Fragment>> fragmentPortions = CollectionUtil.splitCollectionBySize(inputFramentList, portion);
        List<MTInsideOutsideGauPCallable> mtTasks = new ArrayList<>();
        for (Collection<Fragment> fp : fragmentPortions) {
            mtTasks.add(new MTInsideOutsideGauPCallable(fp, theRuleMap, reducedDimension, nonZeroElement));
        }
        List<Future<RulesCounts>> allRuleCounts = executor.invokeAll(mtTasks);
        executor.shutdown();
        //System.out.println("There are " + allRuleCounts.size() + " to collect");
        RulesCounts rsToKeep = allRuleCounts.get(0).get();
        //System.out.println("Merging results from threads...");
        //System.out.println("Cardinality " + rsToKeep.getRuleCardinality() +"\t\tlikelihood: "+rsToKeep.getCurrentLikelihood());
        for (int j = 1; j < allRuleCounts.size(); j++) {
            RulesCounts rtgPartial = allRuleCounts.get(j).get();
            //System.out.println("Cardinality " + rtgPartial.getRuleCardinality() +"\t\tlikelihood: "+rtgPartial.getCurrentLikelihood());
            rsToKeep.addRuleCounts(rtgPartial, -1);

        }

        return rsToKeep;
    }

  

    public static RulesCounts estimateParametersEmbeddingOnline(RuleMaps theRuleMap, Collection<Fragment> fragmentsAll,
            int batchSize, double rate, WordPairSimilaritySmallMemoryFixedID wspc) {
        /// from here 
        final RuleMaps rmToUse = new RuleMaps(theRuleMap);
        RulesCounts rsToKeep = new RulesCounts();
        AtomicInteger ai = new AtomicInteger();
        fragmentsAll
                .parallelStream().unordered()
                .forEach(f -> {

                    try {
                        InsideOutsideEmbedding io
                                = new InsideOutsideEmbedding(f, rmToUse, rsToKeep, wspc);
                        io.inside();
                        io.outsideAlgorithm();
                    } catch (Exception ex) {
                        Logger.getLogger(HelperLearnerMethods.class.getName()).log(Level.WARNING, null,"Caught error 54545: "+ ex);
                        
                        // Logger.getLogger(TestMTInsideOutsideComputing.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (ai.incrementAndGet() > batchSize) {
                        ai.set(0);
                        //  System.err.println("Updating parameters in batch of 1000");
                        synchronized (rmToUse) {
                            try {
                                rmToUse.updateParametersPartial(rsToKeep, rate);
                            } catch (Exception ex) {
                               // System.err.println( ex);
                               Logger.getLogger(HelperLearnerMethods.class.getName()).log(Level.WARNING, null,"Caught error 54546: " + ex);
                            }
                        }
                    }

                });

        return rsToKeep;
    }

    public static RulesCounts estimateParametersOnline(RuleMaps theRuleMap, List<Fragment> fragmentsAll, int batchSize, double rate) {
        /// from here 
        final RuleMaps rmToUse = new RuleMaps(theRuleMap);
        RulesCounts rsToKeep = new RulesCounts();
        AtomicInteger ai = new AtomicInteger();
        fragmentsAll.parallelStream()
                .forEach(f -> {

                    try {
                        InsideOutside io = new InsideOutside(new FragmentCompact(f, rmToUse), rmToUse, rsToKeep);
                        io.inside();
                        io.outsideAlgorithm();
                        if (ai.incrementAndGet() > batchSize) {
                            ai.set(0);
                            //  System.err.println("Updating parameters in batch of 1000");
                            synchronized (rmToUse) {
                                rmToUse.updateParametersPartial(rsToKeep, rate);
                            }
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(HelperParseMethods.class.getName()).log(Level.SEVERE, null, "codebqag7327"+ex);
                        // Logger.getLogger(TestMTInsideOutsideComputing.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
        return rsToKeep;
    }

    public static RulesCounts estimateParameters(
            RuleMaps theRuleMap, Collection<Fragment> fragmentsAll,
            WordPairSimilaritySmallMemoryFixedID wordSimContainer) {
        /// from here 
        final RuleMaps rmToUse = new RuleMaps(theRuleMap);
        RulesCounts rsToKeep = new RulesCounts();
        fragmentsAll.parallelStream()
                .forEach(f -> {
                    try {
                        InsideOutsideEmbedding io = new InsideOutsideEmbedding(f, rmToUse, rsToKeep, wordSimContainer);
                        io.inside();
                        io.outsideAlgorithm();

                    } catch (Exception ex) {
                        Logger.getLogger(HelperParseMethods.class.getName()).log(Level.SEVERE, null, "codebqag73s27"+ex);
                        // Logger.getLogger(TestMTInsideOutsideComputing.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
        return rsToKeep;
    }

    public static RulesCounts estimateParametersInThreads(RuleMaps theRuleMap, Collection<Fragment> inputFramentList, int threadSize) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(threadSize);
        double portion = inputFramentList.size() * 1.0 / threadSize;
        Collection<MTInsideOutsideCallable> mtTasks = new ConcurrentLinkedDeque<>();
        int size = (int) Math.max(1, Math.floor(portion));
        List<Collection<Fragment>> splitCollectionBySize = CollectionUtil.splitCollectionBySize(inputFramentList, size);

        splitCollectionBySize.parallelStream().forEach(col -> {
            mtTasks.add(new MTInsideOutsideCallable(col, theRuleMap, 0));
        });
        List<Future<RulesCounts>> allRuleCounts = executor.invokeAll(mtTasks);
        executor.shutdown();
        //System.out.println("There are " + allRuleCounts.size() + " to collect");
        RulesCounts rsToKeep = allRuleCounts.get(0).get();
        //System.out.println("Merging results from threads...");
        //System.out.println("Cardinality " + rsToKeep.getRuleCardinality() +"\t\tlikelihood: "+rsToKeep.getCurrentLikelihood());
        for (int j = 1; j < allRuleCounts.size(); j++) {
            RulesCounts rtgPartial = allRuleCounts.get(j).get();
            //System.out.println("Cardinality " + rtgPartial.getRuleCardinality() +"\t\tlikelihood: "+rtgPartial.getCurrentLikelihood());
            rsToKeep.addRuleCounts(rtgPartial, -1);

        }

        return rsToKeep;
    }

    public static IOProcessCompleteDataContainer estimateParametersInThreadsWithMergeData(
            RuleMaps theRuleMap, List<Fragment> inputFramentList, int threadSize) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(threadSize);
        double portion = inputFramentList.size() * 1.0 / threadSize;
        List<MTInsideOutsideCallableWithIOParamSummary> mtTasks = new ArrayList<>();
        for (int t = 0; t < threadSize - 1; t++) {
            List<Fragment> subList = inputFramentList.subList((int) Math.floor(t * portion), (int) Math.floor((t + 1) * portion));
            mtTasks.add(new MTInsideOutsideCallableWithIOParamSummary(subList, theRuleMap, t));
        }
        List<Fragment> subList = inputFramentList.subList((int) Math.floor((threadSize - 1) * portion), inputFramentList.size());
        mtTasks.add(new MTInsideOutsideCallableWithIOParamSummary(subList, theRuleMap, threadSize - 1));
        List<Future<IOProcessCompleteDataContainer>> allRuleCounts = executor.invokeAll(mtTasks);
        executor.shutdown();
        //System.out.println("There are " + allRuleCounts.size() + " to collect");
        IOProcessCompleteDataContainer get = allRuleCounts.get(0).get();
        RulesCounts rsToKeep = get.getRc();
        Map<Integer, IOParam> ioParamSummary = get.getIoParamSummary();
        //System.out.println("Merging results from threads...");
        //System.out.println("Cardinality " + rsToKeep.getRuleCardinality() +"\t\tlikelihood: "+rsToKeep.getCurrentLikelihood());
        for (int j = 1; j < allRuleCounts.size(); j++) {

            IOProcessCompleteDataContainer resultPartial = allRuleCounts.get(j).get();
            Map<Integer, IOParam> ioParamSummaryPartial = resultPartial.getIoParamSummary();
            RulesCounts rtgPartial = resultPartial.getRc();

//            new Thread(new Runnable() {
//                public void run() {
            MergeUtils.summarizeSymbolsInsideOutsides(ioParamSummary, ioParamSummaryPartial);
//                }
//            }).start();

//            new Thread(new Runnable() {
//                public void run() {
            rsToKeep.addRuleCounts(rtgPartial, -1);
//                }
//            }).start();

            //System.out.println("Cardinality " + rtgPartial.getRuleCardinality() +"\t\tlikelihood: "+rtgPartial.getCurrentLikelihood());
        }
        return new IOProcessCompleteDataContainer(rsToKeep, ioParamSummary);

    }

    /**
     * Return the rule counts and detail parsechart
     *
     * @param theRuleMap
     * @param inputFramentList
     * @param threadSize
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static IOProcessCompleteDataContainer estimateParametersInThreadsWithDetailedMergeData(
            RuleMaps theRuleMap, Collection<Fragment> inputFramentList, int threadSize) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(threadSize);
        double portion = inputFramentList.size() * 1.0 / threadSize;
        List<Collection<Fragment>> splitCollectionBySize = CollectionUtil.splitCollectionBySize(inputFramentList, (int) Math.floor(portion) + 1);

        List<MTInsideOutsideCallableWithIOParamDetail> mtTasks = new ArrayList<>();
        AtomicInteger coutnrer = new AtomicInteger();
        splitCollectionBySize.forEach(fragCol -> {
            mtTasks.add(new MTInsideOutsideCallableWithIOParamDetail(fragCol, theRuleMap, coutnrer.incrementAndGet()));
        });

//        for (int t = 0; t < threadSize - 1; t++) {
//            Collection<Fragment> subList = inputFramentList.subList((int) Math.floor(t * portion), (int) Math.floor((t + 1) * portion));
//            mtTasks.add(new MTInsideOutsideCallableWithIOParamDetail(subList, theRuleMap, t));
//        }
//        List<Fragment> subList = inputFramentList.subList((int) Math.floor((threadSize - 1) * portion), inputFramentList.size());
//        mtTasks.add(new MTInsideOutsideCallableWithIOParamDetail(subList, theRuleMap, threadSize - 1));
        List<Future<IOProcessCompleteDataContainer>> allRuleCounts = executor.invokeAll(mtTasks);
        executor.shutdown();
        //System.out.println("There are " + allRuleCounts.size() + " to collect");
        IOProcessCompleteDataContainer get = allRuleCounts.get(0).get();
        RulesCounts rsToKeep = get.getRc();
        Collection<InOutParameterChart> ioCharts = get.getParaChartCollection();
        //System.out.println("Merging results from threads...");
        //System.out.println("Cardinality " + rsToKeep.getRuleCardinality() +"\t\tlikelihood: "+rsToKeep.getCurrentLikelihood());
        for (int j = 1; j < allRuleCounts.size(); j++) {

            IOProcessCompleteDataContainer resultPartial = allRuleCounts.get(j).get();
            //Map<Integer, IOParam> ioParamSummaryPartial = resultPartial.getIoParamSummary();
            RulesCounts rtgPartial = resultPartial.getRc();

            //  MergeUtils.summarizeSymbolsInsideOutsides(ioParamSummary, ioParamSummaryPartial);
            rsToKeep.addRuleCounts(rtgPartial, -1);
            //System.out.println("Cardinality " + rtgPartial.getRuleCardinality() +"\t\tlikelihood: "+rtgPartial.getCurrentLikelihood());
        }
        return new IOProcessCompleteDataContainer(rsToKeep, ioCharts);

    }
}
