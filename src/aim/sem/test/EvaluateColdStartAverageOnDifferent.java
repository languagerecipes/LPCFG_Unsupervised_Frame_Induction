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
package aim.sem.test;

import frameinduction.learn.scripts.MainLearnScript;
import embedding.sim.SimHarmonicMean;
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import mhutil.HelperFragmentMethods;
import frameinduction.grammar.parse.io.HierachyBuilderResultWarpper;
import frameinduction.settings.ExperimentPathBuilder;
import java.util.Collections;
import java.util.Random;
import java.util.logging.LogManager;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import semeval.utils.EvaluationResult;
import util.embedding.ISimMethod;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
public class EvaluateColdStartAverageOnDifferent {

    private static String VECTOR_FILE = "../lr/vectors/AfterFinPVHFNG-en-900-cLR-5-3.txt";

    private int timeToPerformReport;
    private StatisticalSummaryValues smvReport;
    private static boolean printDebug;

    static {
        LogManager.getLogManager().reset();
        Logger.getGlobal().setUseParentHandlers(true);
        Logger.getGlobal().setLevel(Level.OFF);
    }

    //public static WordPairSimilarityContainer wspc;
    public static void main(String[] args) throws Exception {
        
        boolean fromFragment = true;
        
        int numberOfExperiments = 3;
        int subListSize = 350;//inputFramentList.size();

        List<String> gFiles = new ArrayList<>();
        gFiles.add("../lr/starsem/golddata/all.txt"); //15
        int datasetIndex = 0;

        List<Fragment> inputFramentList = null;
        if (fromFragment) {
            inputFramentList = HelperFragmentMethods.loadFragments("../lr/starsem/fragments/all.frg.txt/");
        } else {
            // that is from treebank
            System.err.println("Runing " + EvaluateColdStartAverageOnDifferent.class.getName());

            String treebankName
                    = "stanford-parse-conllu.txt";
            //"psd-conlu-penn-spd.txt";
            String masterInputParsedFilePath
                    = "../lr/treebanks/" + treebankName;
            Settings settings = new Settings();
            inputFramentList = HelperFragmentMethods.parseTreeToFragments(
                    masterInputParsedFilePath, null, null, settings,
                    -1,
                    13, gFiles.get(datasetIndex));
        }
        // inputFramentList = inputFramentList.subList(0, Math.min(inputFramentList.size(),subListSize+50));

        for (int trainIteration = 1; trainIteration < 2; trainIteration++) {
            System.err.println("");
            process(gFiles.get(0), inputFramentList, subListSize, trainIteration, numberOfExperiments);
        }

    }

    private static void process(String goldFile, List<Fragment> inputFramentList,
            int subListSize, int maxIterationSplitMErge, int numberOfExperiments) throws Exception {

        // "../lr/treebanks/";
        long i = 0;//System.currentTimeMillis();

        boolean printMergeDetails = false;
        boolean printPerformanceLog = false;
        //boolean printLogInConsol = false;
        ISimMethod sim
                = //new SimGoodmanKruskalGamma();
                // new SimQuasiJaccard();
                //SimPearsonCoef();
                // new SimPearson();
                // new SimOverlapCoef();
                new SimHarmonicMean();
        //  new SimCosKen();
        //new SimCosine();
        // new simKLDiv();

        //  new SimLinITBased1();
        // org.apache.commons.math3.stat.descriptive.SummaryStatistics sc = new SummaryStatistics();
        EvaluationResult sumSystemPerformance = new EvaluationResult();
        EvaluationResult sumBaselines1H = new EvaluationResult();
        int counter = 0;

        System.err.println("Learning Iteration is set to " + maxIterationSplitMErge);

        //inputFramentList = new ArrayList<>(inputFramentList.subList(0, 200));
        double mergeStartValue = .4;//.75;//.65;
        double mergeMinValStep2 = .5;//.28; //.35; // this is irrelavent for everyIteraion%2
        /// 
        double mergeEndValueStep1 = .07;//.3;//0.3;//.05;//.4//.3;
        String folderToStoreResults = "telsemShekastehShod/";
        long totalTimeMilSecond = 0;
        //Random random =  // this may not work, records are read in parallel
        for (int j = 0; j < numberOfExperiments; j++) {
            Collections.shuffle(inputFramentList, new Random());
            ArrayList<Fragment> inputFramentListUsed = new ArrayList<>(inputFramentList.subList(0, Math.min(subListSize,inputFramentList.size())));
            EvaluationResultAndProcessContainer evpr
                    = new EvaluationResultAndProcessContainer(
                            "test-exp", inputFramentListUsed, new ExperimentPathBuilder(folderToStoreResults, goldFile));
            long start = System.currentTimeMillis();
            counter++;

            String rootNameToUse = j + "it-temp" + "/";

            // with a two split and one merge this is 7 merge
            WordPairSimilaritySmallMemoryFixedID vectorSimilarities = cashVectorSimilarities(inputFramentListUsed, sim);

            MainLearnScript smg = new MainLearnScript(
                    printMergeDetails,
                    printPerformanceLog,
                    rootNameToUse,
                    goldFile,
                    inputFramentListUsed, maxIterationSplitMErge, vectorSimilarities,
                    mergeMinValStep2, mergeStartValue, mergeEndValueStep1
            );

            HierachyBuilderResultWarpper mainSimple = smg.start();
            evpr.buildDeafaultBaselines(mainSimple.getRuleMap());
            List<EvaluationResult> baselines = evpr.getBaselines();
            EvaluationResult oneHead = baselines.get(0);
            oneHead.refreshStats();
//            System.err.println(oneHead.toStringShort(" "));
            sumBaselines1H.sumEvaluatinResult(oneHead);
            evpr.addEvaluationPointMemWise(mainSimple.getRuleMap(), true, j, 1);
            long end = System.currentTimeMillis();
            totalTimeMilSecond += (end - start);
            String currentIterationArgEvalSummary = evpr.getCurrentIterationArgEvalSummary();
            EvaluationResult lastEvaluation = evpr.getLastEvaluation();
            sumSystemPerformance.sumEvaluatinResult(lastEvaluation);
            sumSystemPerformance.addToStatisticSummaries(lastEvaluation.getEvalMeasureVector());

            String summaryHead = evpr.getCurrentIterationEvalSummary();
            if (false) {
//            System.err.println("bhead: "+ evpr.baselinesToString());
                System.err.println("\nHeads: " + summaryHead);
//            System.err.println("ARGS:  " + currentIterationArgEvalSummary);
                System.err.println("bhead: " + evpr.baselinesToString());
//            
            }
            evpr.toWebChartFile();
            evpr.toWebChartRoleFile();
            // System.err.println("><>>> "+ lastEvaluation.toStringShort(" "));
            // you can do more on mainSimple

        }

        System.err.println("Average");
        sumSystemPerformance.normalizeSumEvalByDivTo(numberOfExperiments);
        sumBaselines1H.normalizeSumEvalByDivTo(numberOfExperiments);
        String toStringSummaryStats = sumSystemPerformance.toStringSummaryStats(" ");
        System.err.println(">> stats " + toStringSummaryStats);
        System.err.println(">> System Perfor " + sumSystemPerformance.toStringEvalVectorHuman(" ", false));
        System.err.println(">> Baseline 1cph " + sumBaselines1H.toStringEvalVectorHuman(" ", false));
        System.err.println(totalTimeMilSecond / (1000) + " seconds");
        //System.err.println(totalTimeMilSecond / (1000) + " seconds");

    }

    /**
     * Cash vector similaritites
     *
     * @param inputFramentList
     * @return
     * @throws Exception
     */
    private static WordPairSimilaritySmallMemoryFixedID cashVectorSimilarities(Collection<Fragment> inputFramentList, ISimMethod sim) throws Exception {
        Set<String> makeVocab = HelperFragmentMethods.makeVocab(inputFramentList);
        WordPairSimilaritySmallMemoryFixedID wsmfix = new WordPairSimilaritySmallMemoryFixedID(
                VECTOR_FILE, makeVocab, 900, sim);
        return wsmfix;
    }
}
