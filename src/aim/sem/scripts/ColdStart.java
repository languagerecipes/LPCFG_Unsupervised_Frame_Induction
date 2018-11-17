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

import embedding.sim.SimCosKen;
import frameinduction.learn.scripts.MainLearnScript;
import embedding.sim.SimCosine;
import embedding.sim.SimHarmonicMean;
import embedding.sim.SimKendall;
import embedding.sim.SimKendallCoeff;
import embedding.sim.SimOverlapCoef;
import embedding.sim.SimPearson;
import embedding.sim.SimPearsonCoef;
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
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.ExperimentPathBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.logging.LogManager;
import mhutil.HelperParseChartIO;
import semeval.utils.EvaluationResult;
import static util.HelperGeneralInfoMethods.getCurrentClassName;
import util.embedding.ISimMethod;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
public class ColdStart {

    private static String VECTOR_FILE = "../lr/vectors/AfterFinPVHFNG-en-900-cLR-5-3.txt";

    //public static WordPairSimilarityContainer wspc;
    public static void main(String[] args) throws Exception {

        List<String> gFiles = new ArrayList<>();
        gFiles.add("../lr/starsem/golddata/all.txt"); //15
        int maxIterationSplitMErge = 1;
        int datasetIndex = 0;
        File datasetFile = new File(gFiles.get(datasetIndex));
        LogManager.getLogManager().reset();
        Logger.getGlobal().setUseParentHandlers(true);
        Logger.getGlobal().setLevel(Level.ALL);
        String treebankName
                = "stanford-parse-conllu.txt";
        //"psd-conlu-penn-spd.txt";

        String masterInputParsedFilePath
                = "../lr/treebanks/" + treebankName;
        Settings settings = new Settings();
        List<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments(
                masterInputParsedFilePath, null, null, settings,
                -1,
                13, datasetFile.getAbsolutePath());
        System.err.println("Learning Iteration is set to " + maxIterationSplitMErge);
        
       // ISimMethod sim = new SimKendallCoeff();
// to implement, choosing sim method other than hard coding logging hyperparams in a highperparamlogger         
//      ISimMethod simKendall = new SimKendall();
//      ISimMethod simKendallCoeff = new SimKendallCoeff();
    //ISimMethod  sim = new SimOverlapCoef();
    ISimMethod sim = new SimPearson();
        //ISimMethod sim = new SimPearsonCoef();
        double mergeStartValue = .5;//.75;//.65;
        double mergeMinValStep2 = .5;//.28; //.35; // this is irrelavent for everyIteraion%2 i removed it 
        double mergeEndValueStep1 = .2;//.3;//0.3;//.05;//.4//.3;
        EvaluationResultAndProcessContainer earp 
                = new EvaluationResultAndProcessContainer(
                        "testing", inputFramentList,
                        new ExperimentPathBuilder("test/", gFiles.get(0)));
        Boolean printPerformanceLog = true;
        String rootNameToUse = "test-cold-start/";
        
        WordPairSimilaritySmallMemoryFixedID vectorSimilarities = cashVectorSimilarities(inputFramentList, sim);
        MainLearnScript smg = new MainLearnScript(
                false,
                printPerformanceLog,
                rootNameToUse,
                gFiles.get(0),
                inputFramentList, maxIterationSplitMErge, vectorSimilarities,
                mergeMinValStep2, mergeStartValue, mergeEndValueStep1
        );
        System.err.println("Running " + getCurrentClassName(smg));
        HierachyBuilderResultWarpper mainSimple = smg.start();
        earp.buildDeafaultBaselines(mainSimple.getRuleMap());
        String baselinesToString = earp.baselinesToString();
        System.err.println(baselinesToString);
        Map<Integer, Collection<ParseFrame>> parseFrameMap = mainSimple.getParseFrameMap();
        Collection<ParseFrame> flatParseFrameMaps = HelperParseChartIO.flatParseFrameMaps(parseFrameMap);
        earp.evaluationFromParsed(1, flatParseFrameMaps);
        EvaluationResult lastEvaluation = earp.getLastEvaluation();
        String toStringShort = lastEvaluation.toStringShort(" ");
        System.err.println("sys: " + toStringShort);
        
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
