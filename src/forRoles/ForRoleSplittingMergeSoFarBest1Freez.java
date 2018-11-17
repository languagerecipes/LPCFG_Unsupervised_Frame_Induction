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
package forRoles;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import frameinduction.settings.ExperimentPathBuilder;
import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.RulesCounts;
import mhutil.HelperLearnerMethods;
import frameinduction.grammar.learn.IOProcessCompleteDataContainer;
import frameinduction.grammar.learn.InOutParameterChart;
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import mhutil.HelperMergeMethods;
import frameinduction.grammar.learn.splitmerge.split.SplitSemanticRoleStratgey1;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import mhutil.HelperFragmentMethods;
import util.embedding.WordPairSimilarityContainer;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class ForRoleSplittingMergeSoFarBest1Freez {

    static ExecutorService executor;

    public static void main(String[] args) throws IOException, Exception {
        String rootExp = "temp/forroles-filter-rules-all1/";
        // System.err.println("Embedding based EM...");
        String gold = "../lr/tacl/golddata/all.txt"; //15
        String grmrFile
                = "C:\\prj\\unsupervised-frame-induction\\experiments\\all\\3it-all.txt-rts-1512464660210-stanford-parse-conllu.txt\\grammar\\"
                + "24-it.grmr.zip.mrgd.zip";
//                  "C:\\prj\\unsupervised-frame-induction\\experiments\\ambg\\"
//              + "4it-ambg.txt-rts-1512314181668-stanford-parse-conllu.txt\\grammar\\24-it.grmr.zip.mrgd.zip";
        Settings settings = new Settings();
        int threadSize = settings.getNumberOfThread();
        settings.setFrameCardinality(1);

        settings.setSemanticRoleCardinality(1);
        settings.setProportionOfTopParsesToKeep(.1);

        // ISplit isplitMethodHead = new SplitFrameHeadsStratgey2();
        SplitSemanticRoleStratgey1 iSplitSemanticRoles = new SplitSemanticRoleStratgey1(); // replace it with other interfaces

        boolean parseOnlyGold = true;
        int countSplitSoFar = 0;

        ExperimentPathBuilder path = new ExperimentPathBuilder(rootExp,
                gold);
        PrintWriter pwEMPlotReport = new PrintWriter(new FileWriter(path.getPerformanceLogFile()));

        WordPairSimilarityContainer wordSimContainer;
        executor = Executors.newFixedThreadPool(threadSize);
        if (args.length == 0) {
            System.out.println("Using the default settings...");

        }
        Logger.getGlobal().setLevel(Level.WARNING);
        // addd handler for logger to log in a file
        //util.MemoryUtils.getMemoryReport();

        Collection<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments(
                "../lr/treebanks/stanford-parse-conllu.txt", null, null, settings,
                -1,
                13, gold);

        RuleMaps theRuleMap = RuleMaps.fromArchivedFile(grmrFile);
        //RuleMaps fromFile2 = RuleMaps.fromArchivedFile(grmModel);
        //  fromFile.simplyAsssignMaxParamBetweenTheTwo(fromFile2);
        // fromFile.normalizeToOne();
//        WordPairSimilaritySmallMemoryFixedID wsmfix = new WordPairSimilaritySmallMemoryFixedID(
//                "../lr/vectors/AfterFinPVHFNG-en-900-cLR-5-3.txt", fragments, 900);
//        Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrameByCYKEMbd(fragments,wsmfix, fromFile,8);
        //Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrames(fragments, fromFile);

        System.out.println("Binary rules " + theRuleMap.getCountBinaryRules());
        System.out.println("Unary rules " + theRuleMap.getCountUnaryRules());

        String evName = Thread.currentThread().getStackTrace()[1].getClassName();
        EvaluationResultAndProcessContainer evr = new EvaluationResultAndProcessContainer(evName + rootExp, inputFramentList, path);
        evr.buildDeafaultBaselines(theRuleMap).getRuleCounts();
        String baselinesToString = evr.baselinesToString();
        System.err.println("Chosen Baselines Based on  " + path.getGoldData() + "\n" + baselinesToString);

        //theRuleMap.updateParameters(buildDeafaultBaselines);
        System.out.println("Binary rules " + theRuleMap.getCountBinaryRules());
        System.out.println("Unary rules " + theRuleMap.getCountUnaryRules());

        // maybe print out the baseline here too?!
        //System.out.println("BASELINE: " + baselineEval);
        double currentLL = 0.0;
        boolean llDeviationIsOK = false;
        int iteration;
        int splitTimeCounter = 0;

        String fragmentFile = "../lr/tacl/fragments/ambg.frg.txt";
        String goldFile = "../lr/tacl/golddata/ambg.txt";
        boolean writeEvaluation = true;
        for (iteration = 0; iteration < 10; iteration++) {

            splitTimeCounter++;
            long startTime = System.currentTimeMillis();
            double changeInLikelihood = 0;
            double totalTime = 0;
            for (int i = 0; i < 3; i++) {

                RulesCounts estimatedParameters = HelperLearnerMethods.estimateParameters(theRuleMap, inputFramentList);
                theRuleMap.updateParameters(estimatedParameters);

                changeInLikelihood = currentLL - estimatedParameters.getCurrentLikelihood();
                if (iteration != 0 && changeInLikelihood >= 1E-7 && !llDeviationIsOK) {
                    Logger.getGlobal().log(Level.WARNING, "Possible Fault in LL estimation, errors beyond usual rounding errors! {0}", changeInLikelihood);
                }

                long endTime = System.currentTimeMillis();
                totalTime = (endTime - startTime) / 1000.0;
                currentLL = estimatedParameters.getCurrentLikelihood();
                if (iteration == 0) {
                    changeInLikelihood = Double.NaN;
                    currentLL = Double.NaN;
                }
            }

            //   if (iteration>0 && iteration % itBeforeEvaluation == 0 ) {
            // RulesCounts statAboutRulesUsedInGoldFrames = 
            //evr.addEvaluationPoint1(
//            if (writeEvaluation) {
//              RulesCounts collectedRulesForTopParses =    evr.addEvaluationPointMemWise(
//                        theRuleMap, parseOnlyGold, iteration,
//                        settings.getProportionOfTopParsesToKeep(),
//                        currentLL, changeInLikelihood, totalTime).getRuleCounts();
//                evr.toWebChartFile();
//                if(iteration%5==0){
//                theRuleMap.updateParameters(collectedRulesForTopParses);
//                }
//                System.err.println(llDeviationIsOK + "\t" + iteration + "\t" + currentLL + " " + evr.getCurrentIterationEvalSummary());
//                pwEMPlotReport.println(currentLL + " " + evr.getCurrentIterationEvalSummary());
//                MetricForArgumentsTwo mfA = new MetricForArgumentsTwo(1, fragmentFile);
//                EvaluationResult baseline
//                        = mfA.createBaseline1ClusterPerGramaticalRelationship(goldFile, fragmentFile, evr.getExpPath().getOutputFrameFile(iteration));
//                
//                mfA.process(goldFile, evr.getExpPath().getOutputFrameFile(iteration));
//                EvaluationResult evrSum = mfA.getEvrSum();
//                System.err.println("==================== Role Evaluation ====");
//                System.err.println("System: " + evrSum.toStringShort(" "));
//                System.err.println("Baseline: " + baseline.toStringShort(" "));
//            }
            writeEvaluation = false;

            if (iteration % 2 == 0) {
                writeEvaluation = true;
                System.err.println("Splitting me ... ");
//                theRuleMap = isplitMethodHead.split(theRuleMap);
                theRuleMap = iSplitSemanticRoles.split(theRuleMap, 1.0);
                splitTimeCounter = 0;

            } else //if (iteration % 5 == 0) 
            {
                writeEvaluation = true;
                System.err.println("Merging");
                splitTimeCounter = 0;
                IOProcessCompleteDataContainer completeIOParseChartData
                        = HelperLearnerMethods.
                                estimateParametersInThreadsWithDetailedMergeData(theRuleMap, inputFramentList, threadSize);
                RulesCounts ruleCountToupdate2 = completeIOParseChartData.getRc();
                theRuleMap.updateParameters(ruleCountToupdate2);
                Collection<InOutParameterChart> paraChartCollection = completeIOParseChartData.getParaChartCollection();
                // MergeRoles.mergeRoles(paraChartCollection,theRuleMap, completeIOParseChartData.getRc());

                theRuleMap = HelperMergeMethods.mergeSemanticRoles(paraChartCollection, ruleCountToupdate2, theRuleMap, .5, 10);
            }

//            }
            pwEMPlotReport.flush();

            theRuleMap.seralizaRules(path.getGrammarFile(iteration));
        }
//        MetricForArgumentsTwo mfA = new MetricForArgumentsTwo(1, fragmentFile);
//        EvaluationResult baseline
//                = mfA.createBaseline1ClusterPerGramaticalRelationship(goldFile, fragmentFile, evr.getExpPath().getOutputFrameFile(iteration-1));
//
//        mfA.process(goldFile, evr.getExpPath().getOutputFrameFile(iteration-1));
//        EvaluationResult evrSum = mfA.getEvrSum();
//        System.err.println("==================== Role Evaluation Final ====");
//        System.err.println("System: " + evrSum.toStringShort(" "));
//        System.err.println("Baseline: " + baseline.toStringShort(" "));
//        System.err.println("Split done " + countSplitSoFar + " times");
        System.err.println("DONE!");
        executor.shutdown();
        evr.toWebChartFile();
        pwEMPlotReport.close();

    }


}
