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
import aim.sem.scripts.*;
import embedding.sim.SimCosine;
import embedding.sim.SimHarmonicMean;
import embedding.sim.SimOverlapCoef;
import embedding.sim.SimPearson;
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.io.File;
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
public class FrozenGoodEvaluateColdStartAverageOnDifferent {

    private static String VECTOR_FILE = "../lr/vectors/AfterFinPVHFNG-en-900-cLR-5-3.txt";

    private int timeToPerformReport;
    private StatisticalSummaryValues smvReport;

    //public static WordPairSimilarityContainer wspc;
    public static void main(String[] args) throws Exception {

        System.err.println("Runing " + FrozenGoodEvaluateColdStartAverageOnDifferent.class.getName());
        List<String> gFiles = new ArrayList<>();
        gFiles.add("../lr/starsem/golddata/all.txt"); //15
        int datasetIndex = 0;
        File datasetFile = new File(gFiles.get(datasetIndex));
        LogManager.getLogManager().reset();
        Logger.getGlobal().setUseParentHandlers(true);
        Logger.getGlobal().setLevel(Level.OFF);
        String treebankName
                = "stanford-parse-conllu.txt";
        //"psd-conllu-penn-spd.txt";
        String masterInputParsedFilePath
                = "../lr/treebanks/" + treebankName;
        Settings settings = new Settings();
        List<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments(
                masterInputParsedFilePath, null, null, settings,
                -1,
                13, datasetFile.getAbsolutePath());
        // inputFramentList = inputFramentList.subList(0, subListSize);
        int maxIterationSplitMErge = 1;
        int numberOfExperiments = 10;
        int subListSize = inputFramentList.size();
        for (int i = 1; i < 2; i++) {
            System.err.println("");
            process(gFiles.get(0), inputFramentList, subListSize, i, numberOfExperiments);
        }

    }

    private static void process(String goldFile, List<Fragment> inputFramentList,
            int subListSize, int maxIterationSplitMErge, int numberOfExperiments) throws Exception {

        // "../lr/treebanks/";
        long i = 0;//System.currentTimeMillis();

        boolean printMergeDetails = false;
        boolean printPerformanceLog = false;
        boolean printLogInConsol = false;
        ISimMethod sim
                = //new SimGoodmanKruskalGamma();
                //new SimQuasiJaccard();
                //SimPearsonCoef();
                // new SimPearson();
                // new SimOverlapCoef();
                //  new SimCosine();
                new SimHarmonicMean();

        // org.apache.commons.math3.stat.descriptive.SummaryStatistics sc = new SummaryStatistics();
        EvaluationResult sumSystemPerformance = new EvaluationResult();
        EvaluationResult sumBaselines1H = new EvaluationResult();
        int counter = 0;

        System.err.println("Learning Iteration is set to " + maxIterationSplitMErge);

        //inputFramentList = new ArrayList<>(inputFramentList.subList(0, 200));
        double mergeStartValue = .5;//.5;//.75;//.65;
        double mergeMinValStep2 = .5;//.28; //.35; // this is irrelavent for everyIteraion%2
        /// 
        double mergeEndValueStep1 = .05;//.4//.3;
        String folderToStoreResults = "keepResultForAMBG/";

        long totalTimeMilSecond = 0;
        //Random random =  // this may not work, records are read in parallel
        for (int j = 0; j < numberOfExperiments; j++) {
            Collections.shuffle(inputFramentList, new Random(0));
            ArrayList<Fragment> inputFramentListUsed = new ArrayList<>(inputFramentList.subList(0, subListSize));
            ExperimentPathBuilder experimentPathBuilder = new ExperimentPathBuilder(folderToStoreResults, goldFile);
            EvaluationResultAndProcessContainer evpr
                    = new EvaluationResultAndProcessContainer(
                            "test-exp", inputFramentListUsed, experimentPathBuilder);
            long start = System.currentTimeMillis();
            counter++;

            String rootNameToUse = folderToStoreResults + "/" + j + "it-temp" + "/";

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
            double[] evalMeasureVector = lastEvaluation.getEvalMeasureVector();
            sumSystemPerformance.addToStatisticSummaries(evalMeasureVector);
            sumSystemPerformance.sumEvaluatinResult(lastEvaluation);
            String summaryHead = evpr.getCurrentIterationEvalSummary();
            
            if (true) {
                System.err.println("Heads: " + summaryHead);
                System.err.println("bhead: " + evpr.baselinesToString());

                System.err.println("ARGS:  " + currentIterationArgEvalSummary);
                System.err.println("args: " + evpr.baselinesArgsToString());
                
            }
//            

            evpr.toWebChartFile(); //this is not ok, also write rule maps
            evpr.toWebChartRoleFile();
            // System.err.println("><>>> "+ lastEvaluation.toStringShort(" "));
            // you can do more on mainSimple

        }

        System.err.println("Average");
        sumSystemPerformance.normalizeSumEvalByDivTo(numberOfExperiments);
        sumBaselines1H.normalizeSumEvalByDivTo(numberOfExperiments);
        System.err.println(">> System Perfor " + sumSystemPerformance.toStringEvalVectorHuman(" ", false));
        System.err.println(">> Baseline 1cph " + sumBaselines1H.toStringEvalVectorHuman(" ", false));
        System.err.println(sumSystemPerformance.toStringSummaryStats(" "));
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

    /*
    
    
    run:
Runing aim.sem.test.FrozenGoodEvaluateColdStartAverageOnDifferent

Learning Iteration is set to 1
*** Training is done ... Parsing and building results 
Heads: gi:5035 si:5035 gc:27 sc:162 pu:94.02 ipu:66.18 fpu:77.68 bcp:92.87 bcr:54.14 bcf:68.4 vm:82.98 pp:59.29 pr:96.18 pf:73.36 aRi:71.23 dst:76.2
bhead: --- Baselines ---
1h		gi:5035 si:5035 gc:27 sc:168 pu:94.3 ipu:59.4 fpu:72.89 bcp:93.46 bcr:47.83 bcf:63.28 vm:80.77 pp:36.95 pr:93.85 pf:53.03 aRi:50.3 dst:71.48
1c		gi:5035 si:5035 gc:27 sc:1 pu:22.92 ipu:100 fpu:37.29 bcp:9.7 bcr:100 bcf:17.68 vm:� pp:100 pr:9.7 pf:17.68 aRi:0 dst:37.23
r162		gi:5035 si:5035 gc:27 sc:162 pu:24.23 ipu:2.48 fpu:4.5 bcp:12.6 bcr:1.16 bcf:2.12 vm:10.37 pp:0.82 pr:12.59 pf:1.55 aRi:0.36 dst:3.56
r2		gi:5035 si:5035 gc:27 sc:2 pu:22.92 ipu:52.93 fpu:31.99 bcp:9.71 bcr:50.34 bcf:16.29 vm:0.2 pp:50.11 pr:9.72 pf:16.28 aRi:0.04 dst:31.85
---

args: --- Baselines Args ---
1h		gi:7410 si:7410 gc:112 sc:33 pu:38.69 ipu:91.12 fpu:54.32 bcp:23.55 bcr:85.66 bcf:36.94 vm:50.83 pp:89.49 pr:13.65 pf:23.69 aRi:17.47 dst:53.61
1c		gi:7410 si:7410 gc:112 sc:1 pu:12.44 ipu:100 fpu:22.13 bcp:4.34 bcr:100 bcf:8.33 vm:� pp:100 pr:4.34 pf:8.33 aRi:0 dst:22.07
r162		gi:7410 si:7410 gc:112 sc:515 pu:19.12 ipu:2.63 fpu:4.63 bcp:11.13 bcr:1.7 bcf:2.95 vm:26.63 pp:0.51 pr:10.72 pf:0.98 aRi:0.58 dst:2.05
r2		gi:7410 si:7410 gc:112 sc:2 pu:12.44 ipu:53.51 fpu:20.19 bcp:4.36 bcr:50.83 bcf:8.03 vm:0.43 pp:50.17 pr:4.36 pf:8.02 aRi:0.03 dst:20.05
---

ARGS:  gi:7410 si:7410 gc:112 sc:515 pu:91.73 ipu:64.64 fpu:75.84 bcp:89.29 bcr:51.61 bcf:65.41 vm:84.38 pp:56.63 pr:91.38 pf:69.93 aRi:68.9 dst:72.37
Average
>> System Perfor  162 5035 94.02 66.18 77.68 92.87 54.14 68.4 82.98 59.29 96.18 73.36 71.23 76.2
>> Baseline 1cph  168 5035 94.3 59.4 72.89 93.46 47.83 63.28 80.77 36.95 93.85 53.03 50.3 71.48
5873 seconds
BUILD SUCCESSFUL (total time: 97 minutes 58 seconds)

     */
}
