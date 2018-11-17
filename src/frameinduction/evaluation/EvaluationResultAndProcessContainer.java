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
package frameinduction.evaluation;

//import clusteringevaluation.EvaluationResult;
//import clusteringevaluation.MetricForArgumentsTwo;
//import de.hhu.phil.fi.eval.ioutils.EvalResultToChartXML;
//import evaluationscripts.MetricForSenseTwo;
import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.parse.CNFRule;
import frameinduction.grammar.parse.CYKAllParses;
import frameinduction.grammar.parse.CYKAllParsesForPruning;

import frameinduction.grammar.parse.ParseChart;
import frameinduction.grammar.parse.ParseDataCompact;
import mhutil.HelperParseChartIO;
import frameinduction.grammar.parse.io.ParseFrame;
import input.preprocess.objects.Fragment;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import frameinduction.settings.ExperimentPathBuilder;
import frameinduction.settings.Settings;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Map;
import semeval.evaluationscripts.MetricForArgumentsTask21;
import semeval.evaluationscripts.MetricsForFrameGrouping;
import semeval.utils.EvaluationResult;



import util.ParseGrammarUtils;
import mhutil.HelperFragmentIOUtils;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class EvaluationResultAndProcessContainer {
//@BQ: to add eval results for the Arguments too.

    //static final int THREAD_NUMBER = 16;
    private final List<EvaluationResult> evalResutls;
    private final List<EvaluationResult> rndomResutlsForIteration;
    private final List<EvaluationResult> baselines;
//    
    private final List<EvaluationResult> evalResutlsRoles;
    private final List<EvaluationResult> rndomResutlsForIterationRoles;
    private final List<EvaluationResult> baselinesRoles;

    private final List<String> evaluationHumanReadableTitle;
    private final String evaluationName;
    private final ExperimentPathBuilder expPath;

    private final int minGoldClusterSize = 0;
    private final int minInstances = 1;
    private final Collection<Fragment> inputFragmentList;
    private final Collection<Fragment> goldSntxFragmentList;
//Map<String, Map<String, Set<String>>> goldFramesPartionedByLexHead ; // added to speed up experiments with a lot of logging
    private final AtomicInteger innerIterationCounterForDebug = new AtomicInteger();
    // add other information here such as info about input etc.

    private final String tempFragFile;

    public EvaluationResultAndProcessContainer(String evaluationName,
            Collection<Fragment> inputFragmentList, ExperimentPathBuilder path) throws Exception {
        this.evalResutls = new ArrayList<>();
        this.baselines = new ArrayList<>();
        this.rndomResutlsForIteration = new ArrayList<>();

        this.evaluationHumanReadableTitle = new ArrayList<>();
        ///
        this.evalResutlsRoles = new ArrayList<>();
        this.baselinesRoles = new ArrayList<>();
        this.rndomResutlsForIterationRoles = new ArrayList<>();

        this.evaluationName = evaluationName;
        this.inputFragmentList = inputFragmentList;
        this.goldSntxFragmentList = new ArrayList<>();
        Map<String, Fragment> goldDataLoading = HelperFragmentIOUtils.loadFragmentsIDMap(path.getGoldData());
        tempFragFile = path.getExperimentRootFolder() + "fragment" +"."+System.currentTimeMillis()+".tmp";
        File tempFile = new File(tempFragFile);
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
        
        for (Fragment f : this.inputFragmentList) {
            if (goldDataLoading.containsKey(f.getEvaluationID())) {
                goldSntxFragmentList.add(f);
                bw.write(f.toStringPosition("uknown"));
                bw.newLine();

            }
        }
        System.out.println("The gold frame/fragment set is of size " + goldSntxFragmentList.size());
        expPath = path;

        //System.out.println("Fragments are read from file for the evaluation --- change itbehrang--- the temporary file for fragments");
        bw.close();
        //  tempFile.deleteOnExit();

        //  goldFramesPartionedByLexHead = UtilReadProcessEvaluationFiles.readParseResultToHeadClusters(getEvaluationGoldFile());
    }

    public String getEvaluationGoldFile() {
        return this.expPath.getGoldData();
    }

    public String getEvaluationName() {
        return evaluationName;
    }

    /**
     * Current likelihood could be computed here but to avoid the computation we
     * pass it from the learning process
     *
     * @param ruleMap
     * @param onlyGoldData
     * @param iteration
     * @param itargetTopNParses
     * @param currentLikelihood
     * @param changeInLikelihood
     * @param processTime
     * @return
     * @throws Exception
     */
    @Deprecated
    public RulesCounts addEvaluationPoint1(
            RuleMaps ruleMap,
            boolean onlyGoldData,
            int iteration,
            double itargetTopNParses,
            double currentLikelihood, double changeInLikelihood, double processTime) throws Exception {
        Collection<Fragment> framesToParse = null;
        if (onlyGoldData) {

            framesToParse = goldSntxFragmentList;
        } else {
            framesToParse = this.inputFragmentList;
        }
        AtomicInteger countAllValidParses = new AtomicInteger(0);
        //  DoubleAdder likelihoodForThis = new DoubleAdder();
        DoubleAdder sumProbForBestParses = new DoubleAdder();

        AtomicInteger countAllNChosenParses = new AtomicInteger(0);
        CYKAllParses cypAll = new CYKAllParses(framesToParse, ruleMap, Settings.DEF_THREAD_NUM_TO_USE);
        Collection<ParseChart> gotParses = cypAll.run();

        RulesCounts rcForTopNParses = new RulesCounts();
        Collection<String> bestFrameEvaluationStrList = new ConcurrentLinkedQueue<>();
        long timestrat = System.currentTimeMillis();
        DoubleAdder llForTheTopNParses = new DoubleAdder();
        gotParses.forEach(parse -> {
            //for (Future<Collection<ParseChart>> parseColelction : gotParses) {
//            try {
//                parseColelction.get().forEach((ParseChart parse) -> {
            try {

                List<CNFRule> validParsesSortByProb = parse.getSortedValidParses(ruleMap.getStartSymbolID());
                int totalParseForThis = validParsesSortByProb.size();
                int targetTopNParsesX = 1;
                if (totalParseForThis > 1) {
                    int s = (int) Math.ceil(totalParseForThis * itargetTopNParses);
                    targetTopNParsesX = 1 + s;
                }
                int topNF = Math.min(validParsesSortByProb.size(), targetTopNParsesX);
                countAllNChosenParses.addAndGet(topNF);
                for (int i = 0; i < topNF; i++) {
                    ParseGrammarUtils.getRulesInChain(validParsesSortByProb.get(i), rcForTopNParses);
                    llForTheTopNParses.add(validParsesSortByProb.get(i).getProb());
                }
                // for evaluatuion purpose--> the computed number nust eb the same as the reported LL in the EM process.
                //double log = Math.log(parse.sumProbValidParses(ruleMap.getStartSymbolID()));
                //probSum.add(log);
                countAllValidParses.addAndGet(validParsesSortByProb.size());
                if (validParsesSortByProb.size() > 0) {

                    // get the prob
                    sumProbForBestParses.add(validParsesSortByProb.get(0).getProb());
                    // get the string format we use for evaluation and put it in a queue
                    ParseFrame parseTreeToFrame = HelperParseChartIO.parseTreeToFrame(parse.getIdentifier(), validParsesSortByProb.get(0), ruleMap);
                    String retFrameValueStr = parseTreeToFrame.toStringStdEvaluation();
                    bestFrameEvaluationStrList.add(retFrameValueStr);
                }
                //ParseFrame parseTreeToFrameRoleSyntax = HelperParseChartIO.parseTreeToFrameRoleSyntax(mostProbableParse, theRuleMap);

            } catch (Exception ex) {
                Logger.getLogger(EvaluationResultAndProcessContainer.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
//            } catch (Exception ex) {
//                Logger.getLogger(TestWSJ.class.getName()).log(Level.SEVERE, null, ex);
//            }
        //}
        // );

        //System.out.println("0000 " + ruleCounts.getCountBinaryRules());
        //   }
        String frameFile = expPath.getOutputFrameFile(iteration);
        PrintWriter pwGeneratedFrame = new PrintWriter(new FileWriter(frameFile));
        for (String frame : bestFrameEvaluationStrList) {
            pwGeneratedFrame.println(frame);
        }
        pwGeneratedFrame.close();

        MetricsForFrameGrouping msfTwo = new MetricsForFrameGrouping(minGoldClusterSize, minInstances, this.expPath.getGoldData(), frameFile);
        EvaluationResult evRes = msfTwo.process(frameFile);

        //evRes.setChangeInLikelihood(targetTopNParses);
        evRes.setCountValidParses(countAllValidParses.intValue());
        evRes.setLearningIteration(iteration);
        evRes.setNumberOFUnaryRules(ruleMap.getCountUnaryRules());
        //evRes.setNumberOFUnaryRules(FastMath.log(ruleMap.getCountUnaryRules()));
        evRes.setNumberOfBinRules(ruleMap.getCountBinaryRules());
        //evRes.setNumberOfBinRules(FastMath.log(ruleMap.getCountBinaryRules()));
        evRes.setLikelihoodForBestParses(sumProbForBestParses.doubleValue());
        evRes.setLikelihoodForChosenNBestParses(llForTheTopNParses.doubleValue() / countAllNChosenParses.intValue());
        evRes.setLikelihood(currentLikelihood);
        evRes.setChangeInLikelihood(changeInLikelihood);
        evRes.setTimeStamp(timestrat);
        evalResutls.add(evRes);
        EvaluationResult rndOmBaselineForThisClusterNumber = msfTwo.createBaselineRandom((int)evRes.getSysClusterNum());
        this.rndomResutlsForIteration.add(rndOmBaselineForThisClusterNumber);
        Logger.getGlobal().log(Level.FINER, "Done Process of adding an evaluation point");

        // do something with the llForTheTopNParses parses?
//        double[] res
//                = //new double[2]; 
//                {countAllValidParses.intValue(), sumProbForBestParses.doubleValue()};
        //ResultToPass resultToPass = new ResultToPass(rc, sumProbForBestParses.doubleValue(), countAllValidParses.intValue());
        return rcForTopNParses;
    }

    public EvResultWrapperForPruning addEvaluationPointMemWise(
            RuleMaps ruleMap,
            boolean onlyGoldData,
            int iteration,
            double itargetTopNParses,
            @Deprecated double currentLikelihood,
            @Deprecated double changeInLikelihood,
            @Deprecated double time
    ) throws Exception {
        return addEvaluationPointMemWise(ruleMap, onlyGoldData, iteration, itargetTopNParses
        //  , currentLikelihood, currentLikelihood
        );
    }

    public EvResultWrapperForPruning addEvaluationPointMemWise(
            RuleMaps ruleMap,
            boolean onlyGoldData,
            int iteration,
            double itargetTopNPortionToconsider
    ) throws Exception {
        Collection<Fragment> framesToParse = null;

        if (onlyGoldData) {

            framesToParse = goldSntxFragmentList;
        } else {
            framesToParse = this.inputFragmentList;
        }
        return addEvaluationPointMemWise(framesToParse, ruleMap, onlyGoldData, iteration, itargetTopNPortionToconsider);
    }

    
    /**
     *
     * @param framesToParse
     * @param ruleMap used for parsing
     * @param onlyGoldData
     * @param iteration
     * @param itargetTopNPortionToconsider
     * @return
     * @throws Exception
     */
    public EvResultWrapperForPruning addEvaluationPointMemWise(
            Collection<Fragment> framesToParse,
            RuleMaps ruleMap,
            boolean onlyGoldData,
            int iteration,
            double itargetTopNPortionToconsider
    ) throws Exception {
         long currentTimeMillis = System.currentTimeMillis();
        AtomicInteger countAllValidParses = new AtomicInteger(0);
        //  DoubleAdder likelihoodForThis = new DoubleAdder();
        DoubleAdder sumProbForBestParses = new DoubleAdder();
        DoubleAdder llForTheTopNParses = new DoubleAdder();
        DoubleAdder modelLikelihood = new DoubleAdder();
        AtomicInteger countAllNChosenParses = new AtomicInteger(0);
        CYKAllParsesForPruning cypAll = new CYKAllParsesForPruning(
                framesToParse, ruleMap, Settings.DEF_THREAD_NUM_TO_USE, itargetTopNPortionToconsider);
        List<Future<Collection<ParseDataCompact>>> gotParses = cypAll.run();

        Collection<String> bestFrameEvaluationStrList = new ConcurrentLinkedQueue<>();
        Collection<ParseFrame> bestParseFrameList = new ConcurrentLinkedQueue<>();
        // behrang you changed here last night, verify that it works.
        RulesCounts rcForTopNParses = new RulesCounts();
        gotParses.parallelStream().forEach(parseColelction -> {
            // for (Future<Collection<ParseDataCompact>> parseColelction : gotParses) {

            try {
                parseColelction.get().forEach((ParseDataCompact parse) -> {
                    // RulesCounts rcForTopNParsesInner = new RulesCounts();
                    try {

                        countAllNChosenParses.addAndGet(parse.getCountChosenParses());
                        llForTheTopNParses.add(parse.getLlForTheTopNParses());
                        modelLikelihood.add(parse.getLlForAll());
                        // for evaluatuion purpose--> the computed number nust eb the same as the reported LL in the EM process.
                        //double log = Math.log(parse.sumProbValidParses(ruleMap.getStartSymbolID()));
                        //probSum.add(log);
                        countAllValidParses.addAndGet(parse.getCountAllValidParses());
                        sumProbForBestParses.add(parse.getSumProbForBestParse());
                        bestFrameEvaluationStrList.add(parse.getBestFrame().toStringStdEvaluation());
                        bestParseFrameList.add(parse.getBestFrame());
                        rcForTopNParses.addRuleCounts(parse.getRcForTopNParses(), Double.NEGATIVE_INFINITY);
                    } catch (Exception ex) {
                        Logger.getLogger(EvaluationResultAndProcessContainer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                // rcForTopNParses.addRuleCounts(rcForTopNParsesInner, -1000);
            } catch (Exception ex) {
                Logger.getLogger(EvaluationResultAndProcessContainer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        );

        //System.out.println("0000 " + ruleCounts.getCountBinaryRules());
        //   }
        String parsedFrameFile = expPath.getOutputFrameFile(iteration);
        try (PrintWriter pwGeneratedFrame = new PrintWriter(new FileWriter(parsedFrameFile))) {
            for (String frame : bestFrameEvaluationStrList) {
                pwGeneratedFrame.println(frame);
            }
        }

        MetricsForFrameGrouping msfTwo = new MetricsForFrameGrouping(
                minGoldClusterSize, minInstances,
                this.expPath.getGoldData(), parsedFrameFile);
        EvaluationResult evRes = msfTwo.process(parsedFrameFile);

        //evRes.setChangeInLikelihood(targetTopNParses);
        evRes.setCountValidParses(countAllValidParses.intValue());
        evRes.setLearningIteration(iteration);
        evRes.setNumberOFUnaryRules(ruleMap.getCountUnaryRules());
        //evRes.setNumberOFUnaryRules(FastMath.log(ruleMap.getCountUnaryRules()));
        evRes.setNumberOfBinRules(ruleMap.getCountBinaryRules());
        //evRes.setNumberOfBinRules(FastMath.log(ruleMap.getCountBinaryRules()));
        evRes.setLikelihoodForBestParses(sumProbForBestParses.doubleValue());
        evRes.setLikelihoodForChosenNBestParses(llForTheTopNParses.doubleValue() / countAllNChosenParses.intValue());
        evRes.setLikelihood(modelLikelihood.doubleValue());
        
        evRes.setChangeInLikelihood(modelLikelihood.doubleValue());
        evRes.setTimeStamp(currentTimeMillis);

        evalResutls.add(evRes);
        EvaluationResult rndOmBaselineForThisClusterNumber = msfTwo.createBaselineRandom((int)evRes.getSysClusterNum());
        this.rndomResutlsForIteration.add(rndOmBaselineForThisClusterNumber);
        Logger.getGlobal().log(Level.FINER, "Done Process of adding an evaluation point");

        MetricForArgumentsTask21 argMetric = new MetricForArgumentsTask21(minInstances, tempFragFile);
        EvaluationResult evrSumRoles = argMetric.process(this.expPath.getGoldData(), parsedFrameFile);
       
        evalResutlsRoles.add(evrSumRoles);
        EvaluationResult evrRandomRoles = argMetric.createBaselineRandom(this.expPath.getGoldData(), parsedFrameFile,(int) evrSumRoles.getSysClusterNum());
        this.rndomResutlsForIterationRoles.add(evrRandomRoles);

        EvResultWrapperForPruning evrWrapping = new EvResultWrapperForPruning(rcForTopNParses, bestParseFrameList);
        return evrWrapping;
    }

    public ExperimentPathBuilder getExpPath() {
        return expPath;
    }

    public void evaluationFromParsed(
            int iterationLevel,
            Collection<ParseFrame> parseFrameCollection) throws IOException {
        String frameFile = expPath.getOutputFrameFile(this.innerIterationCounterForDebug.incrementAndGet());
        System.out.println("Wrting frame name in " + frameFile);
        PrintWriter pwGeneratedFrame = new PrintWriter(new FileWriter(frameFile));
        for (ParseFrame frame : parseFrameCollection) {
            pwGeneratedFrame.println(frame.toStringStdEvaluation());
        }
        pwGeneratedFrame.close();

        MetricsForFrameGrouping msfTwo = new MetricsForFrameGrouping(
                minGoldClusterSize, minInstances,
                this.expPath.getGoldData(), frameFile);
        EvaluationResult evRes = msfTwo.process(frameFile);

        //evRes.setChangeInLikelihood(targetTopNParses);
        // evRes.setCountValidParses(countAllValidParses.intValue());
        // evRes.setLearningIteration(iteration);
        // evRes.setNumberOFUnaryRules(ruleMap.getCountUnaryRules());
        //evRes.setNumberOFUnaryRules(FastMath.log(ruleMap.getCountUnaryRules()));
        //  evRes.setNumberOfBinRules(ruleMap.getCountBinaryRules());
        //evRes.setNumberOfBinRules(FastMath.log(ruleMap.getCountBinaryRules()));
        // evRes.setLikelihoodForBestParses(sumProbForBestParses.doubleValue());
        // evRes.setLikelihoodForChosenNBestParses(llForTheTopNParses.doubleValue()/countAllNChosenParses.intValue());
        //evRes.setLikelihood(currentLikelihood);
        // evRes.setChangeInLikelihood(changeInLikelihood);
        // evRes.setProcessTime(processTime);
        evalResutls.add(evRes);
        EvaluationResult rndOmBaselineForThisClusterNumber = msfTwo.createBaselineRandom((int)evRes.getSysClusterNum());
        this.rndomResutlsForIteration.add(rndOmBaselineForThisClusterNumber);
        Logger.getGlobal().log(Level.FINER, "Done Process of adding an evaluation point");

        MetricForArgumentsTask21 argMetric = new MetricForArgumentsTask21(minInstances, tempFragFile);
        EvaluationResult evrSumRoles  = argMetric.process(this.expPath.getGoldData(), frameFile);
       
        evalResutlsRoles.add(evrSumRoles);
        EvaluationResult evrRandomRoles = argMetric.createBaselineRandom(this.expPath.getGoldData(), frameFile, (int)evrSumRoles.getSysClusterNum());
        this.rndomResutlsForIterationRoles.add(evrRandomRoles);
       
    }

    public void evaluationFromParsed(Collection<ParseFrame> parseFrameCollection,
            double likelihoodBestParse, double likelihoodForChosenNBestParse, double currentLikelihood) throws IOException {
        //System.out.println("0000 " + ruleCounts.getCountBinaryRules());
        //   }

        int debugITerationCounter = innerIterationCounterForDebug.incrementAndGet();
        String frameFile = expPath.getOutputFrameFile(debugITerationCounter);

        PrintWriter pwGeneratedFrame = new PrintWriter(new FileWriter(frameFile));
        for (ParseFrame frame : parseFrameCollection) {
            pwGeneratedFrame.println(frame.toStringStdEvaluation());
        }
        pwGeneratedFrame.close();

        MetricsForFrameGrouping msfTwo = new MetricsForFrameGrouping(
                minGoldClusterSize, minInstances,
                this.expPath.getGoldData(), frameFile);
        EvaluationResult evRes = msfTwo.process(frameFile);

        //evRes.setChangeInLikelihood(targetTopNParses);
        // evRes.setCountValidParses(countAllValidParses.intValue());
        evRes.setLearningIteration(debugITerationCounter);
        // evRes.setNumberOFUnaryRules(ruleMap.getCountUnaryRules());
        //evRes.setNumberOFUnaryRules(FastMath.log(ruleMap.getCountUnaryRules()));
        //  evRes.setNumberOfBinRules(ruleMap.getCountBinaryRules());
        //evRes.setNumberOfBinRules(FastMath.log(ruleMap.getCountBinaryRules()));
        evRes.setLikelihoodForBestParses(likelihoodBestParse);
        evRes.setLikelihoodForChosenNBestParses(likelihoodForChosenNBestParse);
        evRes.setLikelihood(currentLikelihood);
        // evRes.setChangeInLikelihood(changeInLikelihood);
        // evRes.setProcessTime(processTime);
        evalResutls.add(evRes);
        EvaluationResult rndOmBaselineForThisClusterNumber = msfTwo.createBaselineRandom((int)evRes.getSysClusterNum());
        this.rndomResutlsForIteration.add(rndOmBaselineForThisClusterNumber);
        Logger.getGlobal().log(Level.FINER, "Done Process of adding an evaluation point");

        MetricForArgumentsTask21 argMetric = new MetricForArgumentsTask21(minInstances, tempFragFile);
        EvaluationResult evrSumRoles = argMetric.process(this.expPath.getGoldData(), frameFile);
        evalResutlsRoles.add(evrSumRoles);
        EvaluationResult evrRandomRoles = argMetric.createBaselineRandom(this.expPath.getGoldData(), frameFile, (int)evrSumRoles.getSysClusterNum());
        this.rndomResutlsForIterationRoles.add(evrRandomRoles);
        // do something with the llForTheTopNParses parses?
//        double[] res
//                = //new double[2]; 
//                {countAllValidParses.intValue(), sumProbForBestParses.doubleValue()};
        //ResultToPass resultToPass = new ResultToPass(rc, sumProbForBestParses.doubleValue(), countAllValidParses.intValue());
//        EvResultWrapperForPruning evrWrapping = new EvResultWrapperForPruning(rcForTopNParses, bestParseFrameList);
//        return evrWrapping;
    }

    public String getCurrentIterationEvalSummary() {
        int s = this.evalResutls.size() - 1;
        String resultSummary = "No evaluation results yet";
        if (s >= 0) {
            resultSummary = this.evalResutls.get(s).toStringShort(" ");
        }
        return resultSummary;
    }

    public String getCurrentIterationArgEvalSummary() {
        int s = this.evalResutlsRoles.size() - 1;
        String resultSummary = "No evaluation results yet";
        if (s >= 0) {
            resultSummary = this.evalResutlsRoles.get(s).toStringShort(" ");
        }
        return resultSummary;
    }

    public EvaluationResult getLastEvaluation() {
        int s = this.evalResutls.size() - 1;
        EvaluationResult resultSummary = null;
        if (s >= 0) {
            resultSummary = this.evalResutls.get(s);
        }
        return resultSummary;
    }

    public EvaluationResult getLastEvaluationRole() {
        int s = this.evalResutlsRoles.size() - 1;
        EvaluationResult resultSummary = null;
        if (s >= 0) {
            resultSummary = this.evalResutlsRoles.get(s);
        }
        return resultSummary;
    }

    public String baselinesToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Baselines ---\n");
        for (int i = 0; i < baselines.size(); i++) {
            EvaluationResult get = baselines.get(i);
            sb.append(this.evaluationHumanReadableTitle.get(i)).append("\t\t").append(get.toStringShort(" ")).append("\n");

        }
        sb.append("---\n");
        return sb.toString();
    }

    public String baselinesArgsToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Baselines Args ---\n");
        for (int i = 0; i < baselinesRoles.size(); i++) {
            EvaluationResult get = baselinesRoles.get(i);
            sb.append(this.evaluationHumanReadableTitle.get(i)).append("\t\t").append(get.toStringShort(" ")).append("\n");

        }
        sb.append("---\n");
        return sb.toString();
    }

    /**
     * The file name for the data
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public String toWebChartFile() throws FileNotFoundException, IOException {
        EvalResultToChartXML xmlFusionChartConvertor = new EvalResultToChartXML();
        StringBuilder process = xmlFusionChartConvertor.process("-m", evalResutls, baselines, evaluationHumanReadableTitle, rndomResutlsForIteration);
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(expPath.getChartDataFile()), StandardCharsets.UTF_8);
        writer.append(process);
        writer.close();
        return expPath.getChartDataFile().getPath();

    }

    /**
     * returns the name of the file
     * @return
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public String toWebChartRoleFile() throws FileNotFoundException, IOException {
        EvalResultToChartXML xmlFusionChartConvertor = new EvalResultToChartXML();
        StringBuilder process = xmlFusionChartConvertor.process("-m",
                evalResutlsRoles,
                baselinesRoles,
                evaluationHumanReadableTitle, rndomResutlsForIterationRoles);
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(expPath.getChartDataFileForRoles()), StandardCharsets.UTF_8);
        writer.append(process);
        writer.close();
        return expPath.getChartDataFile().getPath();

    }

    public String toWebChartData() throws FileNotFoundException, IOException {
        EvalResultToChartXML xmlFusionChartConvertor = new EvalResultToChartXML();
        StringBuilder process = xmlFusionChartConvertor.process("-m", evalResutls, baselines, evaluationHumanReadableTitle, rndomResutlsForIteration);
        return process.toString();
//        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(expPath.getChartDataFile()), StandardCharsets.UTF_8);
//        writer.append(process);
//        writer.close();

    }

    public String toWebChartRoleData() throws FileNotFoundException, IOException {
        EvalResultToChartXML xmlFusionChartConvertor = new EvalResultToChartXML();
        StringBuilder process = xmlFusionChartConvertor.process("-m", evalResutlsRoles, baselinesRoles, evaluationHumanReadableTitle, rndomResutlsForIterationRoles);
        return process.toString();
//        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(expPath.getChartDataFile()), StandardCharsets.UTF_8);
//        writer.append(process);
//        writer.close();

    }

    /**
     * Method to create baselines
     *
     * @param initiRuleMap
     * @return
     * @throws Exception
     */
    public EvResultWrapperForPruning buildDeafaultBaselines(RuleMaps initiRuleMap) throws Exception {
        //add the string labels for them
        //RulesCounts ruleCounts = this.addEvaluationPointMem1(initiRuleMap, false,0, 3, Double.NaN, Double.NaN, Double.NaN);
        EvResultWrapperForPruning evrResWrapper = this.addEvaluationPointMemWise(initiRuleMap, false, 0, 0, Double.NaN, Double.NaN, Double.NaN);
        // load the frames that are written initialiity 
        MetricsForFrameGrouping msfTwo
                = new MetricsForFrameGrouping(minGoldClusterSize, minInstances, expPath.getGoldData(), this.expPath.getOutputFrameFile(0));
        EvaluationResult bl1CPerHead = msfTwo.bl1CPerHead();
        evaluationHumanReadableTitle.add("1h");
        baselines.add(bl1CPerHead);
        EvaluationResult blAllin1C = msfTwo.blAllin1C();
        evaluationHumanReadableTitle.add("1c");
        baselines.add(blAllin1C);
        EvaluationResult lastEvaluation = getLastEvaluation();
        int sysClusterNum =(int) lastEvaluation.getSysClusterNum();
        if (sysClusterNum > 1) {
            EvaluationResult createBaselineRandom = msfTwo.createBaselineRandom(sysClusterNum);
            evaluationHumanReadableTitle.add("r" + sysClusterNum);
            baselines.add(createBaselineRandom);
        }
        if (sysClusterNum != 2) {
            EvaluationResult createBaselineRandom = msfTwo.createBaselineRandom(2);
            evaluationHumanReadableTitle.add("r" + 2);
            baselines.add(createBaselineRandom);
        }

        ///////// add arg baselines
        MetricForArgumentsTask21 argMetric = new MetricForArgumentsTask21(minInstances, tempFragFile);
        EvaluationResult bl1CPerStxArg = argMetric.createBaseline1ClusterPerVerbGramaticalRelationship(
                expPath.getGoldData(), tempFragFile
                //this.expPath.getOutputFrameFile(0)//, tempFragFile
        );

        baselinesRoles.add(bl1CPerStxArg);
        EvaluationResult blAllin1CArg = argMetric.createBaselineAllIn1Cluster(expPath.getGoldData(), this.expPath.getOutputFrameFile(0));
        evaluationHumanReadableTitle.add("1c");
        baselinesRoles.add(blAllin1CArg);

        EvaluationResult lastEvaluationArg = getLastEvaluationRole();
        int sysClusterNumArg =(int) lastEvaluationArg.getSysClusterNum();
        if (sysClusterNum > 1) {
            EvaluationResult createBaselineRandomArg = argMetric.createBaselineRandom(expPath.getGoldData(), this.expPath.getOutputFrameFile(0), sysClusterNumArg);
            evaluationHumanReadableTitle.add("r" + sysClusterNum);
            baselinesRoles.add(createBaselineRandomArg);
        }
        if (sysClusterNum != 2) {
            EvaluationResult createBaselineRandomArg = argMetric.createBaselineRandom(expPath.getGoldData(), this.expPath.getOutputFrameFile(0), 2);
            evaluationHumanReadableTitle.add("r" + 2);
            baselinesRoles.add(createBaselineRandomArg);
        }

        return evrResWrapper;
        // we can use this Rule Counts to modify the input grammar
    }

    public List<EvaluationResult> getBaselines() {
        return Collections.unmodifiableList(baselines);
    }

    public List<EvaluationResult> getBaselinesRoles() {
        return Collections.unmodifiableList(baselinesRoles);
    }

    /**
     * In each iteration, the number of system generated clusters are different.
     * This list keeps track of a baseline for the case that the instances are
     * randomly partioned into the m buckets similar as suystem output.
     *
     * @return
     */
    public List<EvaluationResult> getRandomBaselinePerClusterIteration() {
        return rndomResutlsForIteration;
    }

    public List<EvaluationResult> getRandomBaselineRolesPerClusterIteration() {
        return rndomResutlsForIterationRoles;
    }
}
