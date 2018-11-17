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
import frameinduction.evaluation.PrintStatistics;
import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.generate.GenerteGrammarFromClusters;
import mhutil.HelperLearnerMethods;
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import frameinduction.grammar.generate.GenerteGrammarFromClusters3;
import frameinduction.grammar.learn.splitmerge.merger.FrameTerminalSymbolsAndSiblings;
import frameinduction.grammar.learn.splitmerge.merger.RuleMapsMergeByMerge;
import frameinduction.grammar.parse.HelperParseMethods;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mhutil.HelperFragmentMethods;
import util.embedding.FrameClusterSimilarityContainer;
import util.embedding.FrameClusterSimilarityInfo;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

public class MergeAhang {

    final int numberOfEMIterate = 10;
    final double headWeight = .53;
    final double roleWeight = .75;
    final private WordPairSimilaritySmallMemoryFixedID wordSimilarities;

    private PrintStatistics printStat;
    //private static WordPairSimilarityContainer wsContainer;

    //final private PrintWriter pwSummaryForTest;
    final private double sampleingRatio = 0.001;//.3;//.05; //.1

    private RuleMaps theRuleMap;

    double delta = 0.03;
    double deltaIncrease = 0.02;//0.004;
    double minSim = .4;//.45; //.35;//.35;//.30;//.1 //.08;//.145;//0.18;//0.22 .41;//.485; 
    private boolean noChangesObserved = false;

    private boolean debugPrint = false;
    int maxBatchSize;// =Integer.MAX_VALUE;// 60;  // these two are also imortant, very important; this depends on the number of clusters/samples
    int maxBAtchSizeAfterFirstIteration = 10;
    int extraIterations = 7;//50/maxBatchSize; // 7
    double thresholdSim;
    private final Map<String, Collection<Fragment>> inputFullSizeData;
    private int MERGE_TIMELIMIT = 0 * 60 * 60
            * 1000;
    private int MERGE_MAIN_TIMELIMIT = MERGE_TIMELIMIT;
    private List<Fragment> fgAll;
    long startTime = System.currentTimeMillis();

    public MergeAhang(
            //Collection<ParseFrame> parseframes,
            EvaluationResultAndProcessContainer evList,
            boolean debugPrint,
            PrintStatistics printStat,
            WordPairSimilaritySmallMemoryFixedID wsmFix,
            Map<String, Collection<Fragment>> currentClustering) throws Exception {
        this.inputFullSizeData = currentClustering;
        this.debugPrint = debugPrint;
        this.printStat = printStat;
        this.wordSimilarities = wsmFix;
        Settings settins = new Settings();
        GenerteGrammarFromClusters genGrmFromClusters = new GenerteGrammarFromClusters(settins.getActiveDepToIntMap());
        genGrmFromClusters.genRules(currentClustering);
        theRuleMap = genGrmFromClusters.getTheRuleMap();
       // GenerteGrammarFromClusters3 genGrmFromClusters = new GenerteGrammarFromClusters3(settins.getActiveDepToIntMap());
        fgAll = new ArrayList<>(HelperFragmentMethods.flattenFragmentCollections(currentClustering.values()));
        maxBatchSize = fgAll.size(); // be careful with this one
        //    genGrmFromClusters.genRules(fgAll  ,parseframes);

//        wsContainer
//                = new WordPairSimilarityContainer(theRuleMap, wordSimilarities);
    }

    public Collection<ParseFrame> mergeStart(
            double thresholdSimB,
            double thresholdSimE,
            Set<String> activeDeprUsedInCrtMergeSym) throws Exception {
        thresholdSim = thresholdSimB;
        minSim = thresholdSimE;
        Collection<Fragment> fullSizeFragmentCollection
                = HelperFragmentMethods.flattenFragmentCollections(inputFullSizeData.values());

        Collection<ParseFrame> mainMergeProcessToTgtedClustNumInner
                = HelperParseMethods.doParsingToBestFrames(fullSizeFragmentCollection, theRuleMap);

        Map<String, Collection<Fragment>> initialFragmentClustersFullSize
                = HelperFragmentMethods.partitionFragmentsByClustersToMapList(
                        fullSizeFragmentCollection, mainMergeProcessToTgtedClustNumInner);
        int counter = 0;
        int countMerge = 0;

        while (thresholdSim - delta > minSim) {

            countMerge++;
            reduceThresholdSimByDelta(delta);
            mainMergeProcessToTgtedClustNumInner(
                    thresholdSim,
                    initialFragmentClustersFullSize, activeDeprUsedInCrtMergeSym);
            //this if condition must be guided either through hyperparam passing or comeup with a better method, e.g., reducing
            // max batch by a decade factor
            if(thresholdSim<.6){
                 maxBatchSize = maxBAtchSizeAfterFirstIteration;
            }
//            if (countMerge > 2) {
//                maxBatchSize = maxBAtchSizeAfterFirstIteration;
//            }
            if (debugPrint) {
                if (noChangesObserved) {
                    System.err.println("There has beeen no changes");
                } else {
                    printStat.setTabbingForPrint(counter++ + "> " + thresholdSim + " >");
                    printStat.printCurentEvaluationAtThisStage(-999, this.theRuleMap); // do not change -999
                    printStat.setTabbingForPrint("");
                }
            }
        }

        // this can reduce the size of 
//        mainMergeProcessToTgtedClustNumInner
//                = HelperParseMethods.doParsingToBestFrames(fullSizeFragmentCollection, theRuleMap);
//
//        initialFragmentClustersFullSize
//                = HelperFragmentMethods.partitionFragmentsByClustersToMapList(
//                        fullSizeFragmentCollection, mainMergeProcessToTgtedClustNumInner);
        // double finThreshold =;
        int maxFinalIteration = countMerge + extraIterations;
        for (int i = countMerge; i < maxFinalIteration; i++) {
            reduceThresholdSimByDelta(0.02);
            mainMergeProcessToTgtedClustNumInner(
                    thresholdSim,
                    initialFragmentClustersFullSize,
                    activeDeprUsedInCrtMergeSym);
            RulesCounts paramEst
                    = HelperLearnerMethods.estimateParameters(theRuleMap, fgAll);
            theRuleMap.updateParameters(paramEst);
            if (debugPrint && printStat != null) {
                if (i % 2 == 0) {
                    printStat.setTabbingForPrint(i + "> fni " + thresholdSim + " >");
                    printStat.printCurentEvaluationAtThisStage(-999, this.theRuleMap); // do not change -999
                    printStat.setTabbingForPrint("");
                }
            }
        }

        return mainMergeProcessToTgtedClustNumInner;
    }

    public RuleMaps getTheRuleMap() {
        return theRuleMap;
    }

    public void mainMergeProcessToTgtedClustNumInner(
            double thresholdSim,
            Map<String, Collection<Fragment>> fullSizeFragments,
            Set<String> activeDepSet) throws IOException, Exception {

        if (false) {
            if (((System.currentTimeMillis() - startTime) > MERGE_MAIN_TIMELIMIT)) {
                System.err.println("Limit on training duration is met (code7535Innerloop)");
                if (thresholdSim > minSim) {
                    thresholdSim -= delta;
                    //maxBatchSize*=2;
                }
            }
        }
        Map<String, Collection<Fragment>> sampleClusters
                = fullSizeFragments;
        if (true) { // changed smapling
            sampleClusters = HelperFragmentMethods.sampleClustersProportional(
                    fullSizeFragments, sampleingRatio);
        }
        Collection<Fragment> sampleFragmentCollection
                = HelperFragmentMethods.flattenFragmentCollections(sampleClusters.values());
        if (debugPrint) {
            System.err.println("-- Sampled fragment size is "
                    + sampleClusters.size() + " clusters of size "
                    + sampleFragmentCollection.size());
        }
        Collection<ParseFrame> sampledFragmentsParse = HelperParseMethods.doParsingToBestFrames(
                sampleFragmentCollection, theRuleMap);

        theRuleMap = mergeAhangaran(thresholdSim, headWeight, roleWeight,
                activeDepSet, //theRuleMap, //estimatedClusterNum,

                sampledFragmentsParse,
                maxBatchSize);

        for (int i = 0; i < 3; i++) {
            RulesCounts paramEst
                    = HelperLearnerMethods.estimateParameters(theRuleMap, fgAll);
            //   HelperLearnerMethods.estimateParametersEmbeddingOnline(theRuleMap, fgAll,50,.1,wordSimilarities);
            // theRuleMap.updateParametersPartial(paramEst, .3);
            //theRuleMap.updateParametersPartial(paramEst,.8);
            theRuleMap.updateParameters(paramEst);
        }

    }

    private String getClusterSize(Map<Integer, Collection<ParseFrame>> clusters) {
        String d = "\t<DIST>";
        for (Collection<ParseFrame> c : clusters.values()) {
            d += " " + c.size();
        }
        return d;
    }

    public RuleMaps mergeAhangaran(double similarityThreashodl,
            double headWeight, double roleWeight, Set<String> activeDepr,
            Collection<ParseFrame> inputBestParses, final double maxReductionCount) throws IOException, Exception {
        if (noChangesObserved) {
            reduceThresholdSimByDelta(delta);
        }
        noChangesObserved = false; // make sure that the previous round is preset
        FrameClusterSimilarityContainer fcsc = new FrameClusterSimilarityContainer(
                headWeight, roleWeight, wordSimilarities, inputBestParses, theRuleMap);
        List<FrameClusterSimilarityInfo> similarFrameClusters = fcsc.getSimilarFrameClusters();
        int befFRV = theRuleMap.getFrameHeadSymbolIDSet().size();
        int counter = 0;
        for (FrameClusterSimilarityInfo cf : similarFrameClusters) {
            if (cf.getSimilarityOveral() < similarityThreashodl) {
                if (debugPrint) {
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
                        theRuleMap, activeDepr);
                RuleMapsMergeByMerge rmMerge = new RuleMapsMergeByMerge();
                theRuleMap = rmMerge.ruleMapsMerge(theRuleMap, map);
                if (maxReductionCount <= counter) {
                    if (debugPrint) {
                        System.err.println("-- Reached max allowed number of iterations " + maxReductionCount + "    " + counter);
                    }
                    thresholdSim += (deltaIncrease); // put it back there
                    //reduceThresholdSimByDelta(-deltaIncrease);
                    break;
                }

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
            thresholdSim -= ((minSim - thresholdSim) * 0.05);
        }
    }

}
