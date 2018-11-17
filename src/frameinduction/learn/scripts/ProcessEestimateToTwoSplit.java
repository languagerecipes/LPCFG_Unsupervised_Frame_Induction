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

import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.RulesCounts;
import mhutil.HelperLearnerMethods;
import frameinduction.grammar.learn.splitmerge.split.ISplit;
import frameinduction.grammar.learn.splitmerge.split.SplitFrameHeads;
import frameinduction.grammar.parse.HelperParseMethods;
import mhutil.HelperParseChartIO;
import frameinduction.grammar.parse.io.ParseFrame;
import input.preprocess.objects.Fragment;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import mhutil.HelperFragmentMethods;
import frameinduction.grammar.parse.io.HierachyBuilderResultWarpper;
import mhutil.HelperGrammarLearnMethods;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class ProcessEestimateToTwoSplit implements Callable<HierachyBuilderResultWarpper> {

    //  public static int dimension = 57; 
    // public static int nz = 3;
    Collection<Fragment> inputFramentList;
    RuleMaps theRuleMap;
    boolean split;
    int innerEMIteration;
    //private final int DEF_THREAD_NUM_TO_USE = 128;
    private final int SPLIT_TIMELIMIT = 30 * 60 * 1000;
    long timestart = System.currentTimeMillis();
    private double minChaneNotToTerminateEM = 1E-3;

    public ProcessEestimateToTwoSplit(Collection<Fragment> inputFramentList, RuleMaps theRuleMap, int innerEMIteration, boolean split) {
        this.inputFramentList = inputFramentList;
        this.theRuleMap = new RuleMaps(theRuleMap);
        this.split = split;
        this.innerEMIteration = innerEMIteration;
    }

    @Override
    public HierachyBuilderResultWarpper call() throws Exception {

        if (split) {
            ISplit isplitMethod = new SplitFrameHeads();
            theRuleMap = isplitMethod.split(theRuleMap);

        }

        RulesCounts estimatedParameters
                = //HelperLearnerMethods.estimateParametersGauP(
                //        HelperLearnerMethods.estimateParametersInThreads(theRuleMap, inputFramentList, Settings.DEF_THREAD_NUM_TO_USE);
                HelperLearnerMethods.estimateParametersEmbeddingOnline(theRuleMap, inputFramentList, Integer.MAX_VALUE, .5, MainLearnScript.wsmFix);
//                    HelperLearnerMethods.estimateParametersGauPInThread(Settings.DEF_THREAD_NUM_TO_USE,
//                            theRuleMap, inputFramentList, dimension, nz);
        theRuleMap.updateParameters(estimatedParameters);
        //   double     llChange = estimatedParameters.getCurrentLikelihood();

        for (int iteration = 0; iteration < innerEMIteration; iteration++) {

            estimatedParameters
                    = HelperLearnerMethods.estimateParametersEmbeddingOnline(theRuleMap, inputFramentList, Integer.MAX_VALUE, .5, MainLearnScript.wsmFix);
//                    HelperLearnerMethods.estimateParametersGauPInThread(Settings.DEF_THREAD_NUM_TO_USE,
//                            theRuleMap, inputFramentList, dimension, nz);
            theRuleMap.updateParameters(estimatedParameters);
            //           double diffLL = estimatedParameters.getCurrentLikelihood() - llChange;
//            if (diffLL < minChaneNotToTerminateEM) {
//                //System.err.println("Break after "+ iteration);
//                //System.err.println(diffLL);
//                break;
//            } else {
//                llChange = estimatedParameters.getCurrentLikelihood();
//                System.err.println(">> cntd");
//            }
//            if (SPLIT_TIMELIMIT < System.currentTimeMillis() - timestart) {
//                break;
//            }
        }

//        CYKBestParseFrameParses cykBestMultu
//                = new CYKBestParseFrameParses(
//                        inputFramentList, theRuleMap, Settings.DEF_THREAD_NUM_TO_USE);
//        Map<Long, ParseFrame> doParsingToBestFrames = cykBestMultu.run();
        Map<Long, ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFramesIdMap(inputFramentList, theRuleMap);

        Map<Integer, Collection<Fragment>> partitionFragmentsByClusters
                = HelperFragmentMethods.partitionFragmentsByClustersMap(
                        inputFramentList, doParsingToBestFrames);

        Map<Integer, Collection<ParseFrame>> parseListToClustterMap
                = HelperParseChartIO.parseListToClustterMap(doParsingToBestFrames);
        RulesCounts collectUsedRulesFromTheBestNParses = HelperGrammarLearnMethods.collectUsedRulesFromTheBestNParses(
                theRuleMap, inputFramentList, 1);
        theRuleMap.updateParameters(collectUsedRulesFromTheBestNParses);
        HierachyBuilderResultWarpper fpw = new HierachyBuilderResultWarpper(
                partitionFragmentsByClusters, parseListToClustterMap, theRuleMap);
        return fpw;

    }
}
