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
import semeval.utils.EvaluationResult;
import util.embedding.WordPairSimilarityContainer;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class ForRoleSplittingMergeSoFarBest {

    static ExecutorService executor;

    public static void main(String[] args) throws IOException, Exception {
        String rootExp = "forroles-filter-rules-mehr-merge-all/";
       // System.err.println("Embedding based EM...");
        
        Settings settings = new Settings();
        int threadSize = settings.getNumberOfThread();
        settings.setFrameCardinality(1);
       
        settings.setSemanticRoleCardinality(1);
        settings.setProportionOfTopParsesToKeep(.0000000000001);
        
       // ISplit isplitMethodHead = new SplitFrameHeadsStratgey2();
        SplitSemanticRoleStratgey1 iSplitSemanticRoles = new SplitSemanticRoleStratgey1(); // replace it with other interfaces
        
        
        boolean parseOnlyGold = true;
        int countSplitSoFar =0;
        
        
        
      
        
        String evFile = "all.txt";
            String grFile=  "grfile.zip";
//              "C:\\prj\\unsupervised-frame-induction\\FinalScriptDecomposed\\"
//              + "temp\\tacl-ambg\\main\\grammar\\0-it.grmr.zip1511974858839.grfile.zip";
        ExperimentPathBuilder path = new ExperimentPathBuilder(rootExp,"../lr/tacl/golddata/"+evFile);
        PrintWriter pwEMPlotReport = new PrintWriter(new FileWriter(path.getPerformanceLogFile()));
        
        
        
        
        WordPairSimilarityContainer wordSimContainer;
        executor = Executors.newFixedThreadPool(threadSize);
        if (args.length == 0) {
            System.out.println("Using the default settings...");

        }
        Logger.getGlobal().setLevel(Level.WARNING);
        // addd handler for logger to log in a file
        //util.MemoryUtils.getMemoryReport();
        
        String gold = "../lr/tacl/golddata/" + evFile; //15

        Collection<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments(
                "../lr/treebanks/stanford-parse-conllu.txt", null, null, settings,
                -1,
                13, gold);
        
  
      RuleMaps theRuleMap = RuleMaps.fromArchivedFile(grFile);
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
        System.err.println("Chosen Baselines Based on  "+path.getGoldData()+"\n" + baselinesToString);
       // HelperParseMethods.getParseFramesStat(inputFramentList, theRuleMap, threadSize, threadSize)
        //theRuleMap.updateParameters(buildDeafaultBaselines);
        
RulesCounts collectedRulesForTopParsesAgg =    evr.addEvaluationPointMemWise(
                        theRuleMap, parseOnlyGold, 0,
                       0.0000000001,
                        0, 0, 0).getRuleCounts();
                evr.toWebChartFile();
                
                theRuleMap.updateParameters(collectedRulesForTopParsesAgg);
                
        System.out.println("Binary rules " + theRuleMap.getCountBinaryRules());
        System.out.println("Unary rules " + theRuleMap.getCountUnaryRules());

        // maybe print out the baseline here too?!
        //System.out.println("BASELINE: " + baselineEval);
        double currentLL = 0.0;
        boolean llDeviationIsOK = false;
        int iteration;
        int splitTimeCounter = 0;
        
        String fragmentFile = "../lr/tacl/fragments/all.frg.txt";
        String goldFile = "../lr/tacl/golddata/all.txt";
        boolean writeEvaluation =true; 
        for (iteration = 0; iteration < 14; iteration++) {
           
            splitTimeCounter++;
            long startTime = System.currentTimeMillis();
             double changeInLikelihood = 0 ;
             double totalTime = 0;
            for (int i = 0; i < 2; i++) {

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
//                //if(iteration%2==0){
//                theRuleMap.updateParameters(collectedRulesForTopParses);
//                //}
//                System.err.println(llDeviationIsOK + "\t" + iteration + "\t" + currentLL + " " + evr.getCurrentIterationEvalSummary());
//                pwEMPlotReport.println(currentLL + " " + evr.getCurrentIterationEvalSummary());
//                MetricForArgumentsTask21 mfA = new MetricForArgumentsTask21(1, fragmentFile);
//                EvaluationResult baseline
//                        = mfA.createBaseline1ClusterPerGramaticalRelationship(goldFile, fragmentFile, evr.getExpPath().getOutputFrameFile(iteration));
//                
//                mfA.process(goldFile, evr.getExpPath().getOutputFrameFile(iteration));
//                EvaluationResult evrSum = mfA.getEvrSum();
//                System.err.println("==================== Role Evaluation ====");
//                System.err.println("System: " + evrSum.toStringShort(" "));
//                System.err.println("Baseline: " + baseline.toStringShort(" "));
//            }
            writeEvaluation=false;
           
            if (iteration % 2 == 1) {
                writeEvaluation=true;
                System.err.println("Splitting me ... ");
//                theRuleMap = isplitMethodHead.split(theRuleMap);
                theRuleMap = iSplitSemanticRoles.split(theRuleMap, 1.10);
                splitTimeCounter = 0;

            } else //if (iteration % 5 == 0) 
            {
                writeEvaluation=true;
                System.err.println("Merging");
                splitTimeCounter=0;
                IOProcessCompleteDataContainer completeIOParseChartData
                        = HelperLearnerMethods.
                        estimateParametersInThreadsWithDetailedMergeData(theRuleMap, inputFramentList, threadSize);
                RulesCounts ruleCountToupdate2 = completeIOParseChartData.getRc();
                theRuleMap.updateParameters(ruleCountToupdate2);
                Collection<InOutParameterChart> paraChartCollection = completeIOParseChartData.getParaChartCollection();
                // MergeRoles.mergeRoles(paraChartCollection,theRuleMap, completeIOParseChartData.getRc());

                theRuleMap = HelperMergeMethods.mergeSemanticRoles(
                        paraChartCollection, ruleCountToupdate2, theRuleMap, 1, 1);
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
//        System.err.println("DONE!");
        executor.shutdown();
        evr.toWebChartFile();
        pwEMPlotReport.close();

    }

   
    
    private void miniFrameMerge(){}
    


}
