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
package frameinduction.learn.scripts;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.generate.GenerteGrammarFromClusters;
import mhutil.HelperLearnerMethods;

import frameinduction.grammar.generate.GenerteGrammarFromClusters3;
import frameinduction.grammar.learn.splitmerge.merger.FrameTerminalSymbolsAndSiblings;
import frameinduction.grammar.learn.splitmerge.merger.RuleMapsMergeByMerge;
import frameinduction.grammar.parse.HelperParseMethods;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mhutil.HelperFragmentMethods;
import util.embedding.FrameClusterSimilarityContainer;
import util.embedding.FrameClusterSimilarityContainer1;
import util.embedding.FrameClusterSimilarityInfo;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

public class MergeMany {

    final int numberOfEMIterate = 5;
    final double headWeight = .5;
    final double roleWeight = .75;
    final private WordPairSimilaritySmallMemoryFixedID wordSimilarities;
    private EvaluationResultAndProcessContainer evList;
    int extraParamTuning = 8;
    //private static WordPairSimilarityContainer wsContainer;
    final private double sampleingRatio = 0.001;//.3;//.05; //.1

    private RuleMaps theRuleMap;
    double deltaIncrease = 0.005;
    double delta = 0.02; //.025

    double minSim = .2;//.21;//5;//.13//0.18;//0.22 .41;//.485; 
    private boolean noChangesObserved = false;

    //final private boolean debugPrint = true;
    int maxNumberOFMergesInOneStep = 150;//5; //this number effects the speed of the system the lower the number the better the system' performance and higher chances of trapping in a local minima
    int maxNumberOfMergesInOneStepAfter3Iterations = 10;
    double thresholdSim;

    private int MERGE_TIMELIMIT = Integer.MAX_VALUE;
    private int MERGE_MAIN_TIMELIMIT = MERGE_TIMELIMIT;
    private Collection<Fragment> fgAll;
    long startTime = System.currentTimeMillis();
    Collection<ParseFrame> initparseframes;
    final private boolean logDetailPerformance;

    public void setEvList(EvaluationResultAndProcessContainer evList) {
        this.evList = evList;
    }

    public MergeMany(
            boolean logPerformance,
            Collection<ParseFrame> parseframes,
            WordPairSimilaritySmallMemoryFixedID wsmFix,
            Collection<Fragment> inputFullSizeData) throws Exception {

//        this.evList = evList;
        this.logDetailPerformance = logPerformance;
        initparseframes = parseframes;
        this.wordSimilarities = wsmFix;
        fgAll
                = //(List<Fragment>) 
                inputFullSizeData;
    }

    public Collection<ParseFrame> mergeStart(
            double thresholdSimI,
            double theresholdMin,
            Map<String, Integer> activeDeprUsedInCrtMergeSym) throws Exception {
        thresholdSim = thresholdSimI;
        this.minSim = theresholdMin;
        Map<String, Collection<Fragment>> initialFragmentClustersFullSize
                = HelperFragmentMethods.partitionFragmentsByClustersToMapList(
                        fgAll, initparseframes);
        // Settings settins = new Settings();
        if (evList != null && logDetailPerformance) {
            evList.evaluationFromParsed(999,
                    initparseframes);
            System.err.println("Disc-generative Starting point >> " + evList.getLastEvaluation().toStringShort(" "));
        }

        // GenerteGrammarFromClusters genGrmFromClusters = new GenerteGrammarFromClusters(settins.getActiveDepToIntMap());
        GenerteGrammarFromClusters3 genGrmFromClusters = new GenerteGrammarFromClusters3(activeDeprUsedInCrtMergeSym);
        // fgAll= (List<Fragment>) inputFullSizeData;
        // genGrmFromClusters.genRules(initialFragmentClustersFullSize);
        genGrmFromClusters.genRules(fgAll, initparseframes);
        theRuleMap = genGrmFromClusters.getTheRuleMap();
        int counter = 0;
        int countMerge = 0;
        while (thresholdSim - delta > minSim) {
            countMerge++;
            reduceThresholdSimByDelta(delta);
            mainMergeProcessToTgtedClustNumInner(
                    thresholdSim,
                    initialFragmentClustersFullSize, activeDeprUsedInCrtMergeSym);
            Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrames(fgAll, theRuleMap);
            initialFragmentClustersFullSize
                    = HelperFragmentMethods.partitionFragmentsByClustersToMapList(
                            fgAll, doParsingToBestFrames);
            if (evList != null && logDetailPerformance) {
                evList.evaluationFromParsed(countMerge, doParsingToBestFrames);
               // System.err.println(this.thresholdSim + ">> " + evList.getLastEvaluation().toStringShort(" ")+"\r");
            }
            if (countMerge > 3) {
                maxNumberOFMergesInOneStep = maxNumberOfMergesInOneStepAfter3Iterations;
            }
        }
        System.err.println(counter++ + "> " + thresholdSim + " iterative clustering > \r");

        int maxFinalIteration = countMerge + extraParamTuning;

        // that is 10 minutes
        for (int i = countMerge; i < maxFinalIteration; i++) {
            reduceThresholdSimByDelta(0.01);
            mainMergeProcessToTgtedClustNumInner(
                    thresholdSim,
                    initialFragmentClustersFullSize,
                    activeDeprUsedInCrtMergeSym);
            Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrames(fgAll, theRuleMap);
            initialFragmentClustersFullSize
                    = HelperFragmentMethods.partitionFragmentsByClustersToMapList(
                            fgAll, doParsingToBestFrames);
            if (evList != null && logDetailPerformance) {
                evList.evaluationFromParsed(countMerge, doParsingToBestFrames);
                System.err.println(this.thresholdSim + ">> " + evList.getLastEvaluation().toStringShort(" ")+" >> iterative clustering \r");
            }

        }

        return HelperParseMethods.doParsingToBestFrames(fgAll, theRuleMap);
    }

    public RuleMaps getTheRuleMap() {
        return theRuleMap;
    }

    public void mainMergeProcessToTgtedClustNumInner(
            double thresholdSim,
            Map<String, Collection<Fragment>> fullSizeFragments,
            Map<String, Integer> activeDepSet) throws IOException, Exception {
//
//        if (false) {
//            if (((System.currentTimeMillis() - startTime) > MERGE_MAIN_TIMELIMIT)) {
//                System.err.println("Limit on training duration is met (code7535Innerloop)");
//                if (thresholdSim > minSim) {
//                    thresholdSim -= delta;
//                    //maxBatchSize*=2;
//                }
//            }
//        }
        Map<String, Collection<Fragment>> sampleClusters
                = null;
        if (true) { // changed smapling
            sampleClusters = HelperFragmentMethods.sampleClustersProportional(
                    fullSizeFragments, sampleingRatio);
        }
        Collection<Fragment> sampleFragmentCollection
                = HelperFragmentMethods.flattenFragmentCollections(sampleClusters.values());
        if (logDetailPerformance) {
            System.err.println("-- Sampled fragment size is "
                    + sampleClusters.size() + " clusters of size "
                    + sampleFragmentCollection.size());
        }

        Collection<ParseFrame> sampledFragmentsParse = HelperParseMethods.doParsingToBestFrames(
                sampleFragmentCollection, theRuleMap);

        theRuleMap = mergeAhangaran(thresholdSim, headWeight, roleWeight,
                activeDepSet, //theRuleMap, //estimatedClusterNum,

                sampledFragmentsParse,
                maxNumberOFMergesInOneStep);

        for (int i = 0; i < numberOfEMIterate; i++) {
            RulesCounts paramEst
                    = HelperLearnerMethods.estimateParameters(theRuleMap, fgAll);
            //  HelperLearnerMethods.estimateParametersEmbeddingOnline(theRuleMap, sampleFragmentCollection,100,.1,wordSimilarities);
            // theRuleMap.updateParametersPartial(paramEst, .3);
            theRuleMap.updateParametersPartial(paramEst, .5);  //.9999
            RulesCounts estimateParametersEmbeddingOnline = HelperLearnerMethods.estimateParametersEmbeddingOnline(theRuleMap, fgAll, 100000, .1, wordSimilarities);
            theRuleMap.updateParametersPartial(estimateParametersEmbeddingOnline, .5);  //.9999

//            if(true){
//            sampleClusters
//                    = HelperFragmentMethods.sampleClustersProportional(fullSizeFragments, sampleingRatio*2);
//                        sampleFragmentCollection
//                    = HelperFragmentMethods.flattenFragmentCollections(sampleClusters.values());
//
//            }
//            sampleClusters
//                    = fullSizeFragments;
//            if (false) { // changed smapling
//                HelperFragmentMethods.sampleClustersProportional(
//                        fullSizeFragments, sampleingRatio);
//            }
//            sampleFragmentCollection
//                    = HelperFragmentMethods.flattenFragmentCollections(sampleClusters.values());
        }

//        Collection<ParseFrame> bestFramesList = null;
//        if (debugPrint) {
//            Collection<Fragment> inputFramentListM = new ArrayList<>();
//            for (Collection<Fragment> fl : fullSizeFragments.values()) {
//                inputFramentListM.addAll(fl);
//            }
//        }
        //return bestFramesList;
    }

    private String getClusterSize(Map<Integer, Collection<ParseFrame>> clusters) {
        String d = "\t<DIST>";
        for (Collection<ParseFrame> c : clusters.values()) {
            d += " " + c.size();
        }
        return d;
    }

    public RuleMaps mergeAhangaran(double similarityThreashodl,
            double headWeight, double roleWeight, Map<String, Integer> activeDepr,
            Collection<ParseFrame> inputBestParses, final double maxReductionCount) throws IOException, Exception {
        if (noChangesObserved) {
            // this.thresholdSim-=delta;
            reduceThresholdSimByDelta(delta);
            // return theRuleMap ;
        }
        noChangesObserved = false; // make sure that the previous round is preset
        FrameClusterSimilarityContainer fcsc = new FrameClusterSimilarityContainer(
                headWeight, roleWeight, wordSimilarities, inputBestParses, theRuleMap);
        List<FrameClusterSimilarityInfo> similarFrameClusters = fcsc.getSimilarFrameClusters();
        int befFRV = theRuleMap.getFrameHeadSymbolIDSet().size();
        int counter = 0;
//        similarFrameClusters.parallelStream().forEach(cf -> {
//            if (cf.getSimilarityOveral() < similarityThreashodl) {
//                System.err.println("-breaking by sim threahold "
//                        + cf.getSimilarityOveral() + " < " + similarityThreashodl
//                );
//                break;
//            }
//        });
        for (FrameClusterSimilarityInfo cf : similarFrameClusters) {

            if (cf.getSimilarityOveral() < similarityThreashodl) {
                if (evList != null && logDetailPerformance) {
                    System.err.println("-breaking by sim threahold "
                            + cf.getSimilarityOveral() + " < " + similarityThreashodl
                    );
                }
                break;
            }
            if (cf.getFrameClusterID1() != cf.getFrameClusterID2()) {
                counter++;
//                System.err.println("\t" + cf.toString() + " " + cf.getSqushedLLDiff() * cf.getSimilarityOveral() + " \t " + cf.getSqushedLLDiff());
                Map<Integer, Integer> map = FrameTerminalSymbolsAndSiblings.getMap(
                        cf.getFrameClusterID1(), cf.getFrameClusterID2(),
                        theRuleMap, activeDepr.keySet());
                RuleMapsMergeByMerge rmMerge = new RuleMapsMergeByMerge();
                theRuleMap = rmMerge.ruleMapsMerge(theRuleMap, map);
                if (maxReductionCount <= counter) {
                    //System.err.println("-- Reached max allowed number of iterations " + maxReductionCount + "    " + counter);
                    thresholdSim += (deltaIncrease); // put it back there
                    //reduceThresholdSimByDelta(-deltaIncrease);

                    break;
                }
//                if (((System.currentTimeMillis() - startTime) > MERGE_MAIN_TIMELIMIT)) {
//                    System.err.println("Limit on training duration is met (code7535InnerInnerInner)");
//                    break;
//                }
            }

        }
        int afterFRV = theRuleMap.getFrameHeadSymbolIDSet().size();
        //System.err.println("Number of frame FRV changed from " + befFRV + "  to  " + afterFRV + " from iteration " + counter);
        if (befFRV == afterFRV) {
            noChangesObserved = true;
        }
        return theRuleMap;
    }

    private void reduceThresholdSimByDelta(double d) {
        thresholdSim -= d;
        if (thresholdSim < minSim) {

            thresholdSim = minSim;
            //   thresholdSim -= ((minSim - thresholdSim) * 0.05);
        }
    }

}
