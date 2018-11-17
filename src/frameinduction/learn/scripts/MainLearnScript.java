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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import frameinduction.evaluation.PrintStatistics;
import frameinduction.settings.ExperimentPathBuilder;
import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.generate.GenerteGrammarFromClusters;
import mhutil.HelperGrammarLearnMethods;
import mhutil.HelperLearnerMethods;
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import frameinduction.grammar.generate.GenerteGrammarFromClusters3;
import frameinduction.grammar.learn.IOProcessCompleteDataContainer;
import frameinduction.grammar.learn.InOutParameterChart;
import frameinduction.grammar.learn.splitmerge.split.ISplit;
import frameinduction.grammar.learn.splitmerge.split.SplitFrameHeads;
import frameinduction.grammar.parse.HelperParseMethods;
import mhutil.HelperParseChartIO;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import mhutil.HelperFragmentMethods;
import frameinduction.grammar.parse.io.HierachyBuilderResultWarpper;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static mhutil.HelperFragmentMethods.partitionFragmentsByHeadVerb;
import mhutil.HelperMergeMethods;
import util.CollectionUtil;
import util.embedding.WordPairSimilarityContainer;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 * Be careful with the way RuleMaps is used, copied and split. For instance,
 * instead of parsing everything to two , you can easuly parse everything to 4
 * by not copying RuleMaps at the first place.
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class MainLearnScript {

    static WordPairSimilarityContainer wspc;
    private boolean logPerformance = true;
    private boolean printMergeDetails = true;
    private boolean printAfterSplit = false;
    private int counterForThisMethod = 1;
    private int dummyCounterForFilesResult = 0;
    private int mergeSplitIteration; // passed as an argument
    private int innerEmLoops = 7;//20;// a small value can increase the speed and lessen the required runtime ... this hyperparamter needs to be set
    private int whatIsYourGuessOfTheMajorityClassSize = 25; //was 15
//15;//15;// 25;
    private double mergeMinCutoffPointEnd = .4; // a larger value can make the system faster; this may not however be the case when the output model is expanded through a split process
    private double mergeMinCutoffPointEndIdea2 = .2;

    double mergeStart = 0.55;//0.35;//.5;//0.65;// i used .4 0.375;
    final private int mergeEveryXSplit = 1;//2; // this must be set with respect to the total number of iterations
    PrintStatistics printStat;
    //ExperimentPathBuilder generalPath;
    String rootExp = "test-resume/init-grammar/";
    EvaluationResultAndProcessContainer evList;
    String finalOutput = "final-something-for-grammar.zip";
    boolean forceSplitOnce = true;
    boolean forceSplitInside = false;

    int numThreads = 16;
    private String finalFrameFile = "init-cluster-";

    MainLearnScript(
            boolean printPerformanceLog,
            String rootNameToUse, String get, List<Fragment> inputFramentList, int maxIterationSplitMErge, WordPairSimilaritySmallMemoryFixedID vectorSimilarities) throws Exception {
        this(false, printPerformanceLog, rootNameToUse, get, inputFramentList, maxIterationSplitMErge, vectorSimilarities);
    }

    /**
     * Returns the evaluation materials for this script
     *
     * @return
     */
    public EvaluationResultAndProcessContainer getEvList() {
        if (logPerformance) {
            return evList;
        } else {
            return null;
        }
    }

    final private Collection<Fragment> flattenFragmentCollections;
    final private Map<String, Integer> finalDependencyMapForGrammarGen;

    private int estimatedClusterNum;
    //private final PrintWriter pwSummaryForTest;
    //boolean printAdditionalInfo = true;

    public static WordPairSimilaritySmallMemoryFixedID wsmFix;
    Map<String, Collection<Fragment>> inputFramentMapWithHeadAsKey;
    Map<Integer, Collection<Fragment>> inputFramentMapWithIntKey;

    //  RuleMaps lastSplitMap = null;
    String goldFilex;
    final Settings settings;
    //   private double lastKnownLL;
    // final  private boolean trainByLikelihood = false;
    private static final Logger LOGGER = Logger.getLogger(MainLearnScript.class.getName());

    ;
    

    /**
     *
     * @param printMergeDetails
     * @param logPerformance
     * @param expReportRootPath is the path in which the system dumps its states
     * into the disk; these files are used also for reporting clustering
     * evaluation measures
     * @param goldFile is the path to the goldFile
     * @param inputFramentListFlat the list of input fragments to be used for
     * parameter fitting
     * @param mergeSplitIteration is the requested number of iterations, note
     * that this value depends on the hyperparam value of @param
     * mergeEveryXSplit
     * @param wsmFix encapsulates pairwise vector similarities of words; for
     * storage, the ConcurrentHashMaps of java is used; this param is usually
     * reference to a pre-loaded wsmFix
     * @deprecated  @param mergeMinCutoffPointEndIdea2 the cutt off for the even iterations; BQ: this has not been used in current evaluation, the learn script can be altered .. to be written
     * @param startValueOfMergeSim
     * @param minSimForStep1
     * @throws Exception
     */
    public MainLearnScript(
            Boolean printMergeDetails,
            Boolean logPerformance,
            String expReportRootPath,
            String goldFile,
            Collection<Fragment> inputFramentListFlat,
            int mergeSplitIteration,
            WordPairSimilaritySmallMemoryFixedID wsmFix, @Deprecated double mergeMinCutoffPointEndIdea2,
            double startValueOfMergeSim, double minSimForStep1) throws Exception {
        this(printMergeDetails, logPerformance, expReportRootPath, goldFile, inputFramentListFlat, mergeSplitIteration, wsmFix);
        this.mergeMinCutoffPointEndIdea2 = mergeMinCutoffPointEndIdea2;
        this.mergeStart = startValueOfMergeSim;
        this.mergeMinCutoffPointEnd = minSimForStep1;
    }

    public MainLearnScript(
            Boolean printMergeDetails,
            Boolean logPerformance,
            String expReportRootPath,
            String goldFile,
            Collection<Fragment> inputFramentListFlat,
            int mergeSplitIteration,
            WordPairSimilaritySmallMemoryFixedID wsmFix) throws Exception {
        this.printMergeDetails = printMergeDetails;
        this.logPerformance = logPerformance;
        this.mergeSplitIteration = mergeSplitIteration;
        MainLearnScript.wsmFix = wsmFix;
     //   this.goldFile = goldFile;
        // make sure there is no duplicate record in the input
        Map<Long, Fragment> fragmentIDMap = HelperFragmentMethods.fragmentIDMap(inputFramentListFlat);
        flattenFragmentCollections = fragmentIDMap.values();
        this.inputFramentMapWithHeadAsKey = partitionFragmentsByHeadVerb(flattenFragmentCollections);
        inputFramentMapWithIntKey = new ConcurrentHashMap<>();
        
        this.finalDependencyMapForGrammarGen =  HelperFragmentMethods.getArgumentFeature(inputFramentListFlat);
        AtomicInteger clusterCounter = new AtomicInteger();
        inputFramentMapWithHeadAsKey.entrySet().parallelStream().forEach(entry -> {
            int incrementAndGet = clusterCounter.incrementAndGet();
            inputFramentMapWithIntKey.put(incrementAndGet, entry.getValue());
        });

        settings = new Settings();
        settings.setFrameCardinality(1);

       
                
       // generalPath = new ExperimentPathBuilder(expReportRootPath, false);
        if (goldFile != null) {
            System.err.println("setting gold datapath ");
            ExperimentPathBuilder path = new ExperimentPathBuilder(
                    expReportRootPath + "/ev/", goldFile, false);
            evList = new EvaluationResultAndProcessContainer(rootExp,
                    inputFramentListFlat, path);
            printStat = new PrintStatistics(printMergeDetails, logPerformance,
                    path, evList);
        }
        
    }

    public void buildPrintBaselines(RuleMaps initRuleMap) throws Exception {
        System.err.println("XXX Baselines for the chosen evaluation datasets");
        System.err.println("Gold file: " + evList.getEvaluationGoldFile() + " " + evList.getEvaluationName());
        //pwSummaryForTest.println("Gold file: " + evList.getEvaluationGoldFile() + " " + evList.getEvaluationName());
        evList.buildDeafaultBaselines(initRuleMap);
        System.err.println(evList.baselinesToString());

    }

    /**
     * Create an initial generative model and encode it using PCFG rules // this
     * can be changed in serveral ways// current implemntation uses
     *
     * @return
     * @throws Exception
     */
    public RuleMaps genInitialModel() throws Exception {
        RuleMaps initRuleMap = null;

        Collection<Map<String, Collection<Fragment>>> breakFragmentClusters
                = HelperFragmentMethods.breakFragmentClustersWithRandom(inputFramentMapWithHeadAsKey, 3); //this was 2
        int i = 1;
        
        for (Map<String, Collection<Fragment>> entry : breakFragmentClusters) {
            GenerteGrammarFromClusters gg = new GenerteGrammarFromClusters(this.finalDependencyMapForGrammarGen);
            gg.genRules(entry);
            // gg.genRules(0.51 / breakFragmentClusters.size(), entry, finalDependencyMapForGrammarGen,10* i++);
            if (initRuleMap == null) {
                LOGGER.log(Level.FINEST, "the model is initilized from null using GenerteGrammarFromClusters model"); // GenerteGrammarFromClusters replace by the Interface
                initRuleMap = gg.getTheRuleMap();
            } else {
                initRuleMap.simplyAddThisRuleMapParams(gg.getTheRuleMap());
                
            }

        }
       
        initRuleMap.normalizeToOne();
        ISplit isplitMethod = new SplitFrameHeads();
        initRuleMap = isplitMethod.split(initRuleMap);

//        Map<Integer, Collection<Fragment>>
//                partitionFragmentsByHeadVerbIntKWithHash 
//                = 
//                HelperFragmentMethods.partitionFragmentsByHeadVerbIntoBuckets(flattenFragmentCollections,1);
//        GenerteGrammarFromClusters gg = new GenerteGrammarFromClusters(this.finalDependencyMapForGrammarGen);
//        System.err.println(">>>>>>>>>>>>>>>>>>>>>>>> "+partitionFragmentsByHeadVerbIntKWithHash.size());
//        gg.genRules(0.51, partitionFragmentsByHeadVerbIntKWithHash, finalDependencyMapForGrammarGen,0);
//        initRuleMap = gg.getTheRuleMap();
//        
//        initRuleMap.normalizeToOne();
//        gg = new GenerteGrammarFromClusters(this.finalDependencyMapForGrammarGen);
//        gg.genRules(.49, 
//                this.inputFramentMapWithHeadAsKey,
//                finalDependencyMapForGrammarGen, 1);
//        RuleMaps theRuleMap = gg.getTheRuleMap();
//
////        gg = new GenerteGrammarFromClusters(settings.getActiveDepToIntMap());
////        gg.genInitialModel(1.0, convertKeyType, finalDependencyMapForGrammarGen, convertKeyType.size() + 1);
////        RuleMaps theRuleMap = gg.getTheRuleMap();
//        initRuleMap.simplyAddThisRuleMapParams(theRuleMap);
        return initRuleMap;

    }

    public HierachyBuilderResultWarpper start() throws Exception {
        RuleMaps genRules = genInitialModel();
        return theProcess(genRules);
    }

    public HierachyBuilderResultWarpper start(String pathToARuleMap) throws Exception {
        RuleMaps fromArchivedFile = RuleMaps.fromArchivedFile(pathToARuleMap);
        return theProcess(fromArchivedFile);
    }

    private HierachyBuilderResultWarpper theProcess(RuleMaps initRuleMap) throws Exception {

        if (printAfterSplit || printMergeDetails) {
            buildPrintBaselines(initRuleMap);
        }
        RuleMaps rmMemPrev = initRuleMap;
        
        MainLearnScript.wspc = new WordPairSimilarityContainer(initRuleMap, wsmFix);
//        Map<Integer, Collection<Fragment>> inputFramentListLoop = new ConcurrentHashMap<>(inputFramentMapWithIntKey);
        Map<Integer, Collection<Fragment>> inputFramentListLoop = new ConcurrentHashMap<>();
        if(false){
        inputFramentListLoop.put(1, new ConcurrentLinkedDeque<>(this.flattenFragmentCollections));
        }else{
            
            Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrames(flattenFragmentCollections, initRuleMap);
            System.err.println("<<< do pars " + doParsingToBestFrames.size());
            inputFramentListLoop = HelperFragmentMethods.partitionFragmentsByClusters(flattenFragmentCollections, doParsingToBestFrames);
        }

//        Map<Integer, Collection<Fragment>> partitionFragmentsByHeadVerbIntoBuckets = HelperFragmentMethods
//                .partitionFragmentsByHeadVerbIntoBuckets(flattenFragmentCollections, 1);
//        inputFramentListLoop = partitionFragmentsByHeadVerbIntoBuckets;
        Collection<ParseFrame> mergedClusters = null;

        for (int splitMergeIterationI = 0; splitMergeIterationI < mergeSplitIteration; splitMergeIterationI++) {

            SplittingContainerSimple splitting
                    = new SplittingContainerSimple(
                            finalDependencyMapForGrammarGen,
                            whatIsYourGuessOfTheMajorityClassSize, innerEmLoops);
            System.err.print("Splitting: " + forceSplitOnce +"\r");
            splitting.main(rmMemPrev, inputFramentListLoop, numThreads, forceSplitInside, forceSplitOnce);
            Map<Integer, Collection<ParseFrame>> mergeParseFrameMapsRet = splitting.getMergeParseFrameMapsRet();
            mergedClusters = HelperParseChartIO.flatParseFrameMaps(mergeParseFrameMapsRet);

//            GenerteGrammarFromClusters3 gc3 = new GenerteGrammarFromClusters3(settings.getActiveDepToIntMap());
//            gc3.genRules(flattenFragmentCollections, mergedClusters);
//            rmMemPrev = gc3.getTheRuleMap();
//            
            GenerteGrammarFromClusters gcx = new GenerteGrammarFromClusters(this.finalDependencyMapForGrammarGen);
            Map<String, Collection<Fragment>> partitionFragmentsByClusters
                    = HelperFragmentMethods.partitionFragmentsByClustersToMapList(flattenFragmentCollections, mergedClusters);
            gcx.genRules(partitionFragmentsByClusters);
            rmMemPrev = gcx.getTheRuleMap();
            if (logPerformance) {
                printStat.getEvalContainer().evaluationFromParsed(innerEmLoops, mergedClusters);
                String currentIterationEvalSummary = printStat.getEvalContainer().getCurrentIterationEvalSummary();
                System.err.println(">> current summar >> " + currentIterationEvalSummary);
            }

            for (int x = 0; x < innerEmLoops; x++) {

                RulesCounts estimateParametersEmbeddingOnline = HelperLearnerMethods.estimateParametersEmbeddingOnline(
                        rmMemPrev, flattenFragmentCollections,
                        Integer.MAX_VALUE, 0.2, wsmFix);
                //RulesCounts rc = HelperLearnerMethods.estimateParameters(rmMemPrev, flattenFragmentCollections);
                rmMemPrev.updateParameters(estimateParametersEmbeddingOnline);
//                 rmMemPrev = HelperMergeMethods.mergeSemanticRoles(
//                            paraChartCollection, ruleCountToupdate2, rmMemPrev, .3, 3);
            }

            if (logPerformance) {
                printStat.setTabbingForPrint("SP ITERATION " + splitMergeIterationI + ">");
                printStat.printCurentEvaluationAtThisStage(-999, rmMemPrev);
                printStat.setTabbingForPrint("");
            }

            Collection<ParseFrame> flatParseFrameMaps = HelperParseMethods.doParsingToBestFrames(flattenFragmentCollections, rmMemPrev);
            inputFramentListLoop = HelperFragmentMethods.partitionFragmentsByClusters(flattenFragmentCollections, flatParseFrameMaps);

            if (splitMergeIterationI % mergeEveryXSplit == 0) {
                Map<String, Collection<Fragment>> convertKeyType
                        = HelperFragmentMethods.convertKeyType(inputFramentListLoop);
                //System.err.println("Merege ahang 3");
                MergeAhangGR3 merge = new MergeAhangGR3(
                        this.finalDependencyMapForGrammarGen,
                        flatParseFrameMaps,
                                evList, printMergeDetails, printStat,
                                MainLearnScript.wsmFix, convertKeyType);
//                MergeAhang merge
//                        = new MergeAhang(
//                                //flatParseFrameMaps,
//                                evList, printMergeDetails, printStat,
//                                MainLearnScript.wsmFix, convertKeyType
//                        );

                if (mergeStart - mergeMinCutoffPointEnd < 0) {
                    System.err.println("never intended to work in this way; assumed an order relationship for decesnding order");
                }
                mergedClusters = merge.mergeStart(mergeStart, mergeMinCutoffPointEnd,
                        settings.getActiveDepr());
                inputFramentListLoop
                        = HelperFragmentMethods.partitionFragmentsByClusters(
                                flattenFragmentCollections, mergedClusters);
                rmMemPrev  = merge.getTheRuleMap();
              //  if (printAfterSplit) {
//                    String mergFilePath = printStat.getEvalContainer().getGrammarFile(counterForThisMethod++) + ".mrgd.zip";
//                    rmMemPrev.seralizaRules(mergFilePath);
              //  }
                //rmMemPrev = mergeRoles(rmMemPrev); //optionally roles can be merged
            } else {
                System.err.println("Step other merging");
                MergeMany mel = new MergeMany(printMergeDetails,
                        flatParseFrameMaps, MainLearnScript.wsmFix, flattenFragmentCollections);
                mel.setEvList(this.evList);
                // last ised .55
                mel.mergeStart(this.mergeStart, mergeMinCutoffPointEndIdea2,finalDependencyMapForGrammarGen);

                rmMemPrev = mel.getTheRuleMap(); // probably write it down
                // rmMemPrev = mergeRoles(rmMemPrev); optionally roles can be merged
               // mergedClusters = // check its effect on the result
                

            }
            if (logPerformance) {
                printStat.setTabbingForPrint("ITERATION " + splitMergeIterationI + " splt2>");
                printStat.printCurentEvaluationAtThisStage(-999, rmMemPrev);
                printStat.setTabbingForPrint("");
            }
            System.err.print("*** Finished training iteratin  " + splitMergeIterationI + "\r");
        }
        
        System.err.print("\r*** Training is done ... Parsing and building results \n");
        //RuleMaps makeInterMediateGrammarFile = makeInterMediateGrammarFile(inputFramentListLoop, finalOutput,mergedClusters);
        HierachyBuilderResultWarpper hbrc;
        if (true) {
            Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrames(flattenFragmentCollections, rmMemPrev);
            Map<Integer, Collection<ParseFrame>> pfMap = HelperParseChartIO.parseListToClustterMap(doParsingToBestFrames);
            hbrc = new HierachyBuilderResultWarpper(inputFramentMapWithIntKey, pfMap, rmMemPrev);
        } else {
            hbrc = new HierachyBuilderResultWarpper(null, null, rmMemPrev);
        }
        if (printStat != null) {
            printStat.close();
        }
 

        return hbrc;
    }

    private void removeUnusedRules(RuleMaps rmMemPrev) throws Exception {
        RulesCounts collectUsedRulesFromTheBestNParses = HelperGrammarLearnMethods.collectUsedRulesFromTheBestNParses(
                rmMemPrev, flattenFragmentCollections, 1);
        rmMemPrev.updateParameters(collectUsedRulesFromTheBestNParses);
    }

    private Map<Integer, Collection<Fragment>> splitInputToHeadClustrs(Collection<Fragment> fgInput) {
        Map<String, Collection<Fragment>> map = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        fgInput.parallelStream().forEach(fg -> {
            String head = fg.getHead();
            if (map.containsKey(head)) {
                map.get(head).add(fg);
            } else {
                Collection<Fragment> f = new ConcurrentLinkedQueue<>();
                f.add(fg);
                Collection<Fragment> putIfAbsent = map.putIfAbsent(head, f);
                if (putIfAbsent != null) {
                    putIfAbsent.add(fg);
                }
            }
        });
        Map<Integer, Collection<Fragment>> mapI = new ConcurrentHashMap<>();
        map.values().forEach(v -> {
            mapI.put(ai.incrementAndGet(), v);
        });
        return mapI;

    }

    private Map<Integer, Collection<Fragment>> splitInputToRandomClustrs(Collection<Fragment> fgInput) {
        //   Map<String,Collection<Fragment>> map = new ConcurrentHashMap<>();
        List<Collection<Fragment>> splitCollectionBySize = CollectionUtil.splitCollectionBySize(fgInput, estimatedClusterNum / 2);
        AtomicInteger ai = new AtomicInteger();

        Map<Integer, Collection<Fragment>> mapI = new ConcurrentHashMap<>();
        splitCollectionBySize.forEach(v -> {
            mapI.put(ai.incrementAndGet(), v);
        });
        return mapI;

    }

//    private void printObtainedClustersToFragments(
//            String outputFilePath,
//            Map<Integer, Collection<Fragment>> fargmentClusters
//    ) throws IOException {
//        PrintWriter pw = new PrintWriter(new FileWriter(new File(outputFilePath)));
//        for (int cluster : fargmentClusters.keySet()) {
//            Collection<Fragment> get = fargmentClusters.get(cluster);
//            for (Fragment fragment : get) {
//
//                String toStringPosition = fragment.toStringPosition(cluster + "");
//                pw.println(toStringPosition);
//            }
//        }
//        pw.close();
//    }
//    private void printObtainedClustersToFragmentsTemp(
//            String outputFilePath,
//            Map<Integer, Collection<Fragment>> fargmentClusters
//    ) throws IOException {
//        PrintWriter pw = new PrintWriter(new FileWriter(new File(outputFilePath)));
//        for (int cluster : fargmentClusters.keySet()) {
//            Collection<Fragment> get = fargmentClusters.get(cluster);
//            for (Fragment fragment : get) {
//
//                String toStringPosition = fragment.toStringPosition(cluster + "");
//                pw.println(toStringPosition);
//            }
//        }
//        pw.close();
//    }
    private RuleMaps makeInterMediateGrammarFile(
            Map<Integer, Collection<Fragment>> frgmntsClustered, String fileSuffix, Collection<ParseFrame> mergedClusters) throws Exception {
        GenerteGrammarFromClusters3 gfcg = new GenerteGrammarFromClusters3(settings.getActiveDepToIntMap());
        //Map<String, Collection<Fragment>> convertKeyType = HelperFragmentMethods.convertKeyType(frgmntsClustered);

        gfcg.genRules(flattenFragmentCollections, mergedClusters);
        RuleMaps theRuleMap = gfcg.getTheRuleMap();

        RulesCounts collectUsedRulesInValidParses
                = HelperGrammarLearnMethods.collectUsedRulesFromTheBestNParses(theRuleMap, flattenFragmentCollections, 1);
        theRuleMap.updateParameters(collectUsedRulesInValidParses);

        for (int i = 0; i < 5; i++) {
            RulesCounts estimateParameters = HelperLearnerMethods.estimateParameters(theRuleMap, flattenFragmentCollections);
            theRuleMap.updateParameters(estimateParameters);
        }
        // add the thing for removing rules which are not used!

//        String fileName = generalPath.getGrammarFile(counterForThisMethod++) + fileSuffix;
//        theRuleMap.seralizaRules(fileName);
        return theRuleMap;
    }

    private RuleMaps makeInterMediateGrammarFile2(
            Map<Integer, Collection<Fragment>> frgmntsClustered, String fileSuffix, Collection<ParseFrame> mergedClusters) throws Exception {
        GenerteGrammarFromClusters gfcg = new GenerteGrammarFromClusters(settings.getActiveDepToIntMap());

        //gfcg.genInitialModel(flattenFragmentCollections,mergedClusters);
        Map<String, Collection<Fragment>> convertKeyType = HelperFragmentMethods.convertKeyType(frgmntsClustered);
        gfcg.genRules(convertKeyType);
        RuleMaps theRuleMap = gfcg.getTheRuleMap();

        RulesCounts collectUsedRulesInValidParses
                = HelperGrammarLearnMethods.collectUsedRulesFromTheBestNParses(theRuleMap, flattenFragmentCollections, 1);
        theRuleMap.updateParameters(collectUsedRulesInValidParses);

        for (int i = 0; i < 5; i++) {
            RulesCounts estimateParameters = HelperLearnerMethods.estimateParameters(theRuleMap, flattenFragmentCollections);
            theRuleMap.updateParameters(estimateParameters);
        }
        // add the thing for removing rules which are not used!

       // String fileName = generalPath.getGrammarFile(counterForThisMethod++) + fileSuffix;
       // theRuleMap.seralizaRules(fileName);
        return theRuleMap;
    }

//    private void dumRuleMap(RuleMaps theRuleMap, String nameSuffix) throws IOException {
//        String fileName = generalPath.getGrammarFile(counterForThisMethod++) + nameSuffix;
//        theRuleMap.seralizaRules(fileName);
//
//    }

//    private RuleMaps mergeByHeadLikelihood(MainLearnScript rmMemPrev,Map<Integer,Colleciton<Fragment>> inputFramentListLoop) {
////        rmMemPrev = makeInterMediateGrammarFile(
////                inputFramentListLoop,
////                mergedClusters);
////        IOProcessCompleteDataContainer estimateParameters = HelperLearnerMethods.estimateParametersInThreadsWithDetailedMergeData(
////                rmMemPrev, flattenFragmentCollections, Settings.DEF_THREAD_NUM_TO_USE);
////
////        //  HelperMergeMethods.mergeFramesByLookingAtHeads(estimateParameters.getParaChartCollection(), estimateParameters.getRc(), settings, rmMemPrev, .2, 5);
////        //rmMemPrev = HelperMergeMethods.mergeSntxDependencies(estimateParameters.getParaChartCollection(), estimateParameters.getRc(), rmMemPrev, .3, 5);
////        rmMemPrev = HelperMergeMethods.mergeSemanticRoles(estimateParameters.getParaChartCollection(), estimateParameters.getRc(), rmMemPrev, .1, 5);
////        rmMemPrev = HelperMergeMethods.mergeFramesByLookingAtHeads(estimateParameters.getParaChartCollection(), estimateParameters.getRc(), settings, rmMemPrev, .1, 5);
//
//    }
    private RuleMaps mergeRoles(RuleMaps rmMemPrev) throws Exception {
        IOProcessCompleteDataContainer completeIOParseChartData
                = HelperLearnerMethods.
                        estimateParametersInThreadsWithDetailedMergeData(rmMemPrev, new ArrayList<>(flattenFragmentCollections), numThreads);
        RulesCounts ruleCountToupdate2 = completeIOParseChartData.getRc();
        rmMemPrev.updateParameters(ruleCountToupdate2);
        Collection<InOutParameterChart> paraChartCollection = completeIOParseChartData.getParaChartCollection();
        //     MergeRoles.mergeRoles(paraChartCollection,rmMemPrev, completeIOParseChartData.getRc());
        rmMemPrev = HelperMergeMethods.mergeSemanticRoles(
                paraChartCollection, ruleCountToupdate2, rmMemPrev, .8, 3);

        for (int x = 0; x < innerEmLoops; x++) {

            RulesCounts estimateParametersEmbeddingOnline = HelperLearnerMethods.estimateParametersEmbeddingOnline(
                    rmMemPrev, flattenFragmentCollections,
                    50, .02, wsmFix);
            // RulesCounts estimateParametersEmbeddingOnline = HelperLearnerMethods.estimateParameters(rmMemPrev, flattenFragmentCollections);

            rmMemPrev.updateParameters(estimateParametersEmbeddingOnline);
//                 rmMemPrev = HelperMergeMethods.mergeSemanticRoles(
//                            paraChartCollection, ruleCountToupdate2, rmMemPrev, .3, 3);
        }

        return rmMemPrev;
    }

}
