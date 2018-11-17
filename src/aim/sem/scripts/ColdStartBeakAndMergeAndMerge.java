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
package aim.sem.scripts;

import frameinduction.learn.scripts.MergeMany;
import frameinduction.learn.scripts.MergeManyPartialSpanFilter;
import frameinduction.learn.scripts.MainLearnScript;
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.generate.GenerteGrammarFromClusters3;
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
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.ExperimentPathBuilder;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.LogManager;
import mhutil.HelperParseChartIO;
import semeval.utils.EvaluationResult;
import util.CollectionUtil;
import static util.HelperGeneralInfoMethods.getCurrentClassName;
import util.embedding.ISimMethod;
import embedding.sim.SimPearson;
import embedding.sim.SimPearsonCoef;
import java.util.concurrent.ConcurrentLinkedDeque;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
public class ColdStartBeakAndMergeAndMerge {

    private static String VECTOR_FILE = "../lr/vectors/AfterFinPVHFNG-en-900-cLR-5-3.txt";
    public static boolean Logging = true;
    private static Level logLevel = Level.FINE;
    private static String goldFile = "../lr/starsem/golddata/all.txt";
    private static String treebank = "../lr/treebanks/stanford-parse-conllu.txt";
    private static int maxIterationSplitMErgePerBatch = 2;
    private static String rootNameToUse = "experiment/";
    private static int BATCH_SIZE = 100;
    private static int howManyStepForMerge = 3;
    private static String logLevelRequested = "Fine"; // to implement

    public static void main(String[] args) throws Exception {

        File datasetFile = new File(goldFile);

        LogManager.getLogManager().reset();
        Logger.getGlobal().setUseParentHandlers(true);
        Logger.getGlobal().setLevel(
                logLevel
        );
        boolean printPerformanceDetails = false;
        int m = 5;
        double mergeFinalMinValueSim = .32; //.2
        Settings settings = new Settings();
        List<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments(
                treebank, null, null, settings,
                -1,
                13, datasetFile.getAbsolutePath());
        Collections.shuffle(inputFramentList, new Random(0));
        Map<String, Integer> argumentFeature = HelperFragmentMethods.getArgumentFeature(inputFramentList);
        inputFramentList = new ArrayList<>(inputFramentList.subList(0, Math.min(700, inputFramentList.size())));
        System.err.println("Loaded Fragments To Cluster: " + inputFramentList.size());
        ISimMethod sim2 = new SimPearsonCoef();
        ISimMethod sim = new SimPearson();
        WordPairSimilaritySmallMemoryFixedID vectorSimilarities = cashVectorSimilarities(inputFramentList,sim);
         WordPairSimilaritySmallMemoryFixedID kendallSims = cashVectorSimilarities(inputFramentList,sim2);
        ExperimentPathBuilder exp = new ExperimentPathBuilder(
                "aim.sem.run.SMMM" + "/ev/", goldFile, false);
        EvaluationResultAndProcessContainer evp = new EvaluationResultAndProcessContainer("testing-beak", inputFramentList, exp);
        List<List<Fragment>> splitCollectionBySize = CollectionUtil.splitCollectionBySize(inputFramentList, BATCH_SIZE);
        Collection<HierachyBuilderResultWarpper> allResylt = new ArrayList<>();
        Collection<HierachyBuilderResultWarpper> interimMerges = new ConcurrentLinkedDeque<>();
        for (int j = 0; j < splitCollectionBySize.size(); j++) {
            //printStat = new PrintStatistics(printSplitDetailEval, pwEMPlotReport, pwSummaryForTest, evList);

            List<Fragment> fragmentsBatch = splitCollectionBySize.get(j);
            MainLearnScript smg = new MainLearnScript(false, false, rootNameToUse,
                    goldFile,
                    fragmentsBatch, maxIterationSplitMErgePerBatch, vectorSimilarities);
            System.err.println("Running " + getCurrentClassName(smg) + fragmentsBatch.size());
            HierachyBuilderResultWarpper mainSimple = smg.start();

            allResylt.add(mainSimple);
            boolean mergeManyLog = false;
            if (false) {
                if (allResylt.size() > howManyStepForMerge) {
                    Map<Integer, Collection<ParseFrame>> mergeParseFrameMaps = HelperParseChartIO.mergeParseFrameMaps(allResylt);
                    Collection<ParseFrame> flatParseFrameMaps = HelperParseChartIO.flatParseFrameMaps(mergeParseFrameMaps);
                    //System.err.println("There are results from " + allResylt.size() + "   and  " + mergeParseFrameMaps.size());
                    // RuleMaps addRuleMaps = HelperRuleMapsMethods.addRuleMaps(allResylt);
                    MergeManyPartialSpanFilter mel = new MergeManyPartialSpanFilter(mergeManyLog,
                            flatParseFrameMaps, vectorSimilarities, inputFramentList);
                    if (flatParseFrameMaps.isEmpty()) {
                        System.err.println("OKkkkkkkkkkkkkkkkkk");
                    }
                    System.err.println(">>> suze " + flatParseFrameMaps.size());
                    mel.setEvList(evp);
                    Collection<ParseFrame> mergeStart = mel.mergeStart(.45, .2, settings.getActiveDepr());
                    System.err.println(">>> here!!! 864726");
                    Map<String, Collection<Fragment>> partitionFragmentsByClustersToMapListPartialSpan
                            = HelperFragmentMethods.partitionFragmentsByClustersToMapListPartialSpan(inputFramentList, mergeStart);
                    // last ised .55
                    if (partitionFragmentsByClustersToMapListPartialSpan.isEmpty()) {
                        System.err.println("WE have a situation here >>>>>>>>>>>>>>>>>>> ");
                    }
                    RuleMaps theRuleMap = mel.getTheRuleMap(); // probably write it down
                    evp.evaluationFromParsed(0, mergeStart);
                    // remember that eventuall I want to use symbols instead of numbers
                    HierachyBuilderResultWarpper ab = new HierachyBuilderResultWarpper(
                            HelperFragmentMethods.convertKeyTypeInt(partitionFragmentsByClustersToMapListPartialSpan),
                            mergeParseFrameMaps, theRuleMap);
                    allResylt.clear();
                    allResylt.add(ab);
                }
            }
            // you can do more on mainSimple
        }
        Iterator<HierachyBuilderResultWarpper> iterator = allResylt.iterator();
        Collection<HierachyBuilderResultWarpper> outer= new ArrayList<>();
        for (int i = 0; i < allResylt.size(); i++) {
            
            Collection<HierachyBuilderResultWarpper> inner= new ArrayList<>();
            for (int j = 0; j < m; j++) {
                if(iterator.hasNext()){
                inner.add(iterator.next());
                }
            }
            if (!inner.isEmpty()) {
                Map<Integer, Collection<ParseFrame>> mergeParseFrameMaps =
                        HelperParseChartIO.mergeParseFrameMaps(inner);
                Map<Integer, Collection<Fragment>> mergeFragmentFromHirResult =
                        HelperFragmentMethods.mergeFragmentFromHirResult(inner);
                Collection<ParseFrame> flatParseFrameMaps = HelperParseChartIO.flatParseFrameMaps(mergeParseFrameMaps);
                Collection<Fragment> flattenFragmentCollections 
                        = HelperFragmentMethods.flattenFragmentCollections(
                                mergeFragmentFromHirResult.values());
                boolean mergeManyLog = true;
                
                MergeManyPartialSpanFilter mel = new MergeManyPartialSpanFilter(mergeManyLog,
                        flatParseFrameMaps, 
                        kendallSims,//vectorSimilarities,
                        flattenFragmentCollections);
                mel.setEvList(evp);
                // last ised .55
                Collection<ParseFrame> mergeStart = mel.mergeStart(.40, .32, settings.getActiveDepr());
                Map<Integer, Collection<ParseFrame>> parseListToClustterMap = HelperParseChartIO.parseListToClustterMap(mergeStart);
                outer.add(new HierachyBuilderResultWarpper(null, parseListToClustterMap, null));
            }
        }
        Map<Integer, Collection<ParseFrame>> mergeParseFrameMaps = HelperParseChartIO.mergeParseFrameMaps(outer);
        //System.err.println("There are results from " + allResylt.size() + "   and  " + mergeParseFrameMaps.size());
        //RuleMaps addRuleMaps = HelperRuleMapsMethods.addRuleMaps(outer);
        //ExperimentPathBuilder exp = new ExperimentPathBuilder("termpx");
        Collection<ParseFrame> flatParseFrameMaps = HelperParseChartIO.flatParseFrameMaps(mergeParseFrameMaps);
        ////////////////////////
        System.err.println("SIZE OF FRAME " + inputFramentList.size());
        RuleMaps dummyRuleMaps = getDummyRuleMaps(flatParseFrameMaps,inputFramentList);
        evp.buildDeafaultBaselines(dummyRuleMaps);
        
        System.err.println("----------------------------------------------*****************************");
        System.err.println(evp.baselinesToString());
        
        evp.evaluationFromParsed(0, flatParseFrameMaps);
        EvaluationResult lastEvaluation = evp.getLastEvaluation();
        String toStringShort = lastEvaluation.toStringShort(" ");

        System.err.println(" ST Before Last Clustering  --> " + toStringShort);
        boolean mergeManyLog = true;
        MergeMany mel = new MergeMany(mergeManyLog,
                flatParseFrameMaps, vectorSimilarities, inputFramentList);
        mel.setEvList(evp);
        
        // last ised .55
        Collection<ParseFrame> mergeStart = mel.mergeStart(.45, mergeFinalMinValueSim, argumentFeature);
        RuleMaps theRuleMap = mel.getTheRuleMap(); // probably write it down
        evp.evaluationFromParsed(0, mergeStart);
        EvaluationResult lastEvaluation2 = evp.getLastEvaluation();
        System.err.println(lastEvaluation2.toStringShort(" "));
        System.err.println(evp.baselinesToString());
        System.err.println("Here return everything so that you can run it for several other ones");

        //   mergeOuter(vectorSimilarities, inputFramentList, allResylt, gFiles.get(0));
    }

    
    

    /**
     * Cash vector similaritites
     *
     * @param inputFramentList
     * @return
     * @throws Exception
     */
    private static WordPairSimilaritySmallMemoryFixedID cashVectorSimilarities(Collection<Fragment> inputFramentList) throws Exception {
        Set<String> makeVocab = HelperFragmentMethods.makeVocab(inputFramentList);
        WordPairSimilaritySmallMemoryFixedID wsmfix = new WordPairSimilaritySmallMemoryFixedID(
                VECTOR_FILE, makeVocab, 900);
        return wsmfix;
    }

    private static RuleMaps getDummyRuleMaps(Collection<ParseFrame> mergeParseFrameMaps, List<Fragment> inputFramentList) throws Exception {
        GenerteGrammarFromClusters3 gfc  = new GenerteGrammarFromClusters3(new Settings().getActiveDepToIntMap());
        gfc.genRules(inputFramentList, mergeParseFrameMaps);
        return gfc.getTheRuleMap();
    }
    
      private static WordPairSimilaritySmallMemoryFixedID cashVectorSimilarities(Collection<Fragment> inputFramentList, ISimMethod sim) throws Exception {
        Set<String> makeVocab = HelperFragmentMethods.makeVocab(inputFramentList);
        WordPairSimilaritySmallMemoryFixedID wsmfix = new WordPairSimilaritySmallMemoryFixedID(
                VECTOR_FILE, makeVocab, 900, sim);
        return wsmfix;
    }
}
