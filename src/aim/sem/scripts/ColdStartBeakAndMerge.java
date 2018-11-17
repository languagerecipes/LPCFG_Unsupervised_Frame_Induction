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
import frameinduction.learn.scripts.MainLearnScript;
import embedding.sim.SimOverlapCoef;
import embedding.sim.SimPearsonCoef;
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import frameinduction.grammar.RuleMaps;
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
import mhutil.HelperRuleMapsMethods;
import semeval.utils.EvaluationResult;
import util.CollectionUtil;
import static util.HelperGeneralInfoMethods.getCurrentClassName;
import util.embedding.ISimMethod;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
public class ColdStartBeakAndMerge {

    private static String VECTOR_FILE = "../lr/vectors/AfterFinPVHFNG-en-900-cLR-5-3.txt";
    public static boolean Logging = true;
    private static Level logLevel = Level.FINE;
    private static String goldFile = "../lr/starsem/golddata/dev.txt";
    private static String treebank = "../lr/treebanks/stanford-parse-conllu.txt";
    private static int maxIterationSplitMErgePerBatch = 1;
    private static String rootNameToUse = "experiment/";
    private static int BATCH_SIZE = 500;
    private static String logLevelRequested = "Fine"; // to implement
    public static void main(String[] args) throws Exception {

        File datasetFile = new File(goldFile);

        LogManager.getLogManager().reset();
        Logger.getGlobal().setUseParentHandlers(true);
        Logger.getGlobal().setLevel(
                logLevel
        );
        boolean printPerformanceDetails = false;

        Settings settings = new Settings();
        List<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments(
                treebank, null, null, settings,
                -1,
                13, datasetFile.getAbsolutePath());
        Iterator<Fragment> iterator = inputFramentList.iterator();
        while (iterator.hasNext()) {
            Fragment s = iterator.next();
            if (s.getTerminals().size() < 4) {
                System.err.println("THE SYSTEM DOES NOT HANDLE VERBS WITH NO ARGUMENT --> REMOVED " + s.toStringPosition("UNKNWOWN"));
                iterator.remove();
            }
        }
                Map<String, Integer> argumentFeature = HelperFragmentMethods.getArgumentFeature(inputFramentList);
        Collections.shuffle(inputFramentList, new Random(0));
        //inputFramentList = new ArrayList<>(inputFramentList.subList(0, Math.min(BATCH_SIZE, inputFramentList.size())));
        System.err.println("Loaded Fragments To Cluster: " + inputFramentList.size());
        ISimMethod isim = new SimPearsonCoef();
                new SimOverlapCoef();
        WordPairSimilaritySmallMemoryFixedID vectorSimilarities = cashVectorSimilarities(inputFramentList,isim);

        List<List<Fragment>> splitCollectionBySize = CollectionUtil.splitCollectionBySize(inputFramentList, BATCH_SIZE);
        Collection<HierachyBuilderResultWarpper> allResylt = new ArrayList<>();
        for (int j = 0; j < splitCollectionBySize.size(); j++) {
            //printStat = new PrintStatistics(printSplitDetailEval, pwEMPlotReport, pwSummaryForTest, evList);

            List<Fragment> fragmentsBatch = splitCollectionBySize.get(j);
            MainLearnScript smg = new MainLearnScript(false,false, rootNameToUse,
                    goldFile,
                    fragmentsBatch, maxIterationSplitMErgePerBatch, vectorSimilarities);
            System.err.println(">>Running " + getCurrentClassName(smg)+" for frg size " + fragmentsBatch.size());
            HierachyBuilderResultWarpper mainSimple = smg.start();

            allResylt.add(mainSimple);
            // you can do more on mainSimple
        }
        Map<Integer, Collection<ParseFrame>> mergeParseFrameMaps = HelperParseChartIO.mergeParseFrameMaps(allResylt);
        //System.err.println("There are results from " + allResylt.size() + "   and  " + mergeParseFrameMaps.size());
        RuleMaps addRuleMaps = HelperRuleMapsMethods.addRuleMaps(allResylt);
        //ExperimentPathBuilder exp = new ExperimentPathBuilder("termpx");
        ExperimentPathBuilder exp = new ExperimentPathBuilder(
                "termxx" + "/ev/", goldFile, false);
        ////////////////////////
        System.err.println("SIZE OF FRAME " + inputFramentList.size());
        EvaluationResultAndProcessContainer evp = new EvaluationResultAndProcessContainer("testing-beak", inputFramentList, exp);
        evp.buildDeafaultBaselines(addRuleMaps);
        // buildDeafaultBaselines(addRuleMaps);
        System.err.println("----------------------------------------------*****************************");
        System.err.println(evp.baselinesToString());
        Collection<ParseFrame> flatParseFrameMaps = HelperParseChartIO.flatParseFrameMaps(mergeParseFrameMaps);
        evp.evaluationFromParsed(0, flatParseFrameMaps);
        EvaluationResult lastEvaluation = evp.getLastEvaluation();
        String toStringShort = lastEvaluation.toStringShort(" ");

        System.err.println("--> " + toStringShort);
        boolean mergeManyLog=true;
        MergeMany mel = new MergeMany(mergeManyLog,
                flatParseFrameMaps, vectorSimilarities, inputFramentList);
        mel.setEvList(evp);
        // last ised .55
        Collection<ParseFrame> mergeStart = mel.mergeStart(.45, .2,argumentFeature);
        RuleMaps theRuleMap = mel.getTheRuleMap(); // probably write it down
        evp.evaluationFromParsed(0, mergeStart);
        EvaluationResult lastEvaluation2 = evp.getLastEvaluation();
        System.err.println(lastEvaluation2.toStringShort(" "));
        System.err.println(evp.baselinesToString());

        //   mergeOuter(vectorSimilarities, inputFramentList, allResylt, gFiles.get(0));
    }

    private static void mergeOuter(
            Map<String,Integer> argumentFeature,
            WordPairSimilaritySmallMemoryFixedID vectorSimilarities,
            Collection<Fragment> inputFramentList, Collection<HierachyBuilderResultWarpper> allResylt,
            String goldFile) throws Exception {
        Map<Integer, Collection<ParseFrame>> mergeParseFrameMaps = HelperParseChartIO.mergeParseFrameMaps(allResylt);
        System.err.println("There are results from " + allResylt.size() + "   and  " + mergeParseFrameMaps.size());
        RuleMaps addRuleMaps = HelperRuleMapsMethods.addRuleMaps(allResylt);
        //ExperimentPathBuilder exp = new ExperimentPathBuilder("termpx");
        ExperimentPathBuilder exp = new ExperimentPathBuilder(
                "termpx" + "/ev/", goldFile, false);
        EvaluationResultAndProcessContainer evp = new EvaluationResultAndProcessContainer("testing-break", inputFramentList, exp);
        evp.buildDeafaultBaselines(addRuleMaps);
        Iterator<HierachyBuilderResultWarpper> iterator = allResylt.iterator();
        HierachyBuilderResultWarpper next = iterator.next();
        Map<Integer, Collection<ParseFrame>> parseFrameMap = next.getParseFrameMap();
        Set<String> activeDepr = new Settings().getActiveDepr();
        List<Fragment> soFarFragments = new ArrayList<>();
        Collection<Fragment> flattenFragmentCollections = HelperFragmentMethods.flattenFragmentCollections(next.getPartitionFragmentsByClusters().values());
        soFarFragments.addAll(flattenFragmentCollections);
        while (iterator.hasNext()) {
            HierachyBuilderResultWarpper next1 = iterator.next();
            Collection<Fragment> flattenFragmentCollections1 = HelperFragmentMethods.flattenFragmentCollections(next1.getPartitionFragmentsByClusters().values());
            soFarFragments.addAll(flattenFragmentCollections1);
            Map<Integer, Collection<ParseFrame>> parseFrameMap1 = next1.getParseFrameMap();
            Map<Integer, Collection<ParseFrame>> merged2 = HelperParseChartIO.mergeParseFrameMaps(parseFrameMap, parseFrameMap1);
            Collection<ParseFrame> flatParseFrameMaps = HelperParseChartIO.flatParseFrameMaps(merged2);

            MergeMany mel = new MergeMany(true,
                    flatParseFrameMaps, vectorSimilarities, soFarFragments);
            Collection<ParseFrame> mergeStart = mel.mergeStart(.40,.2, argumentFeature);
            parseFrameMap = HelperParseChartIO.parseListToClustterMap(mergeStart);

        }

        Collection<ParseFrame> flatParseFrameMaps = HelperParseChartIO.flatParseFrameMaps(mergeParseFrameMaps);
        evp.evaluationFromParsed(0, flatParseFrameMaps);
        EvaluationResult lastEvaluation = evp.getLastEvaluation();
        String toStringShort = lastEvaluation.toStringShort(" ");
        System.err.println("--> " + toStringShort);

        evp.evaluationFromParsed(0, HelperParseChartIO.flatParseFrameMaps(parseFrameMap));

        EvaluationResult lastEvaluation2 = evp.getLastEvaluation();
        System.err.println(lastEvaluation2.toStringShort(" "));
        System.err.println(evp.baselinesToString());

    }

    private static void mergeOuter2(
            Map<String,Integer> argumentFeature,
            WordPairSimilaritySmallMemoryFixedID vectorSimilarities,
            Collection<Fragment> inputFramentList, Collection<HierachyBuilderResultWarpper> allResylt,
            String goldFile) throws Exception {
        Map<Integer, Collection<ParseFrame>> mergeParseFrameMaps = HelperParseChartIO.mergeParseFrameMaps(allResylt);
        System.err.println("There are results from " + allResylt.size() + "   and  " + mergeParseFrameMaps.size());
        RuleMaps addRuleMaps = HelperRuleMapsMethods.addRuleMaps(allResylt);
        //ExperimentPathBuilder exp = new ExperimentPathBuilder("termpx");
        ExperimentPathBuilder exp = new ExperimentPathBuilder(
                "termpx" + "/ev/", goldFile, false);
        EvaluationResultAndProcessContainer evp = new EvaluationResultAndProcessContainer("testing-break", inputFramentList, exp);
        evp.buildDeafaultBaselines(addRuleMaps);
        List<Collection<HierachyBuilderResultWarpper>> splitCollectionBySize = CollectionUtil.splitCollectionBySize(allResylt, 2);

        while (splitCollectionBySize.size() > 1) {
            List<HierachyBuilderResultWarpper> newSplitCollectionBySize = new ArrayList<>();
            for (Collection<HierachyBuilderResultWarpper> collection : splitCollectionBySize) {
                Iterator<HierachyBuilderResultWarpper> iterator = collection.iterator();
                HierachyBuilderResultWarpper next = iterator.next();
                Map<Integer, Collection<ParseFrame>> parseFrameMap = next.getParseFrameMap();
                Map<Integer, Collection<Fragment>> fragments = next.getPartitionFragmentsByClusters();
                Set<String> activeDepr = new Settings().getActiveDepr();
                List<Fragment> soFarFragments = new ArrayList<>();
                Collection<Fragment> flattenFragmentCollections = HelperFragmentMethods.flattenFragmentCollections(next.getPartitionFragmentsByClusters().values());
                soFarFragments.addAll(flattenFragmentCollections);
                while (iterator.hasNext()) {
                    HierachyBuilderResultWarpper next1 = iterator.next();
                    Map<Integer, Collection<Fragment>> partitionFragmentsByClusters = next1.getPartitionFragmentsByClusters();
                    Collection<Fragment> flattenFragmentCollections1 = HelperFragmentMethods.flattenFragmentCollections(partitionFragmentsByClusters.values());
                    fragments = HelperFragmentMethods.mergeFragmentMaps(fragments, partitionFragmentsByClusters);
                    soFarFragments.addAll(flattenFragmentCollections1);
                    Map<Integer, Collection<ParseFrame>> parseFrameMap1 = next1.getParseFrameMap();
                    Map<Integer, Collection<ParseFrame>> merged2 = HelperParseChartIO.mergeParseFrameMaps(parseFrameMap, parseFrameMap1);
                    Collection<ParseFrame> flatParseFrameMaps = HelperParseChartIO.flatParseFrameMaps(merged2);

                    MergeMany mel = null;
                    try {
                        mel = new MergeMany(true,
                                flatParseFrameMaps, vectorSimilarities, soFarFragments);
                    } catch (Exception ex) {
                        Logger.getLogger(ColdStartBeakAndMerge.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Collection<ParseFrame> mergeStart = null;
                    try {
                        mergeStart = mel.mergeStart(.50, .2,argumentFeature);
                    } catch (Exception ex) {
                        Logger.getLogger(ColdStartBeakAndMerge.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    parseFrameMap = HelperParseChartIO.parseListToClustterMap(mergeStart);

                }
                HierachyBuilderResultWarpper hrw = new HierachyBuilderResultWarpper(fragments, parseFrameMap, null);
                newSplitCollectionBySize.add(hrw);
                if (newSplitCollectionBySize.size() > 1) {
                    splitCollectionBySize = CollectionUtil.splitCollectionBySize(newSplitCollectionBySize, 2);
                } else {
                    Map<Integer, Collection<ParseFrame>> parseFrameMapFinal = newSplitCollectionBySize.get(0).getParseFrameMap();
                    Collection<ParseFrame> flatParseFrameMaps = HelperParseChartIO.flatParseFrameMaps(parseFrameMapFinal);
                    evp.evaluationFromParsed(0, flatParseFrameMaps);
                    EvaluationResult lastEvaluation = evp.getLastEvaluation();
                    String toStringShort = lastEvaluation.toStringShort(" ");
                    System.err.println("--> " + toStringShort);
                    evp.evaluationFromParsed(0, HelperParseChartIO.flatParseFrameMaps(parseFrameMapFinal));

                    EvaluationResult lastEvaluation2 = evp.getLastEvaluation();
                    System.err.println(lastEvaluation2.toStringShort(" "));
                    System.err.println(evp.baselinesToString());
                }
            }

        }

    }

    /**
     * Cash vector similaritites
     *
     * @param inputFramentList
     * @return
     * @throws Exception
     */
    private static WordPairSimilaritySmallMemoryFixedID cashVectorSimilarities(
            Collection<Fragment> inputFramentList, ISimMethod isim) throws Exception {
        Set<String> makeVocab = HelperFragmentMethods.makeVocab(inputFramentList);
        WordPairSimilaritySmallMemoryFixedID wsmfix = new WordPairSimilaritySmallMemoryFixedID(
                VECTOR_FILE, makeVocab, 900,isim);
        return wsmfix;
    }
}
