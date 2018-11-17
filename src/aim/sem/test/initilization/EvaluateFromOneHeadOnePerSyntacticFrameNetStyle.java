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
package aim.sem.test.initilization;

import frameinduction.evaluation.EvResultWrapperForPruning;
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.generate.GenerteGrammarFromClusters3;
import frameinduction.grammar.learn.splitmerge.split.ISplit;
import frameinduction.grammar.learn.splitmerge.split.SplitFrameHeads;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.ExperimentPathBuilder;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import mhutil.HelperFragmentMethods;
import mhutil.HelperLearnerMethods;
import mhutil.HelperParseChartIO;
import semeval.utils.EvaluationResult;

/**
 *
 * @author Zadeh
 */
public class EvaluateFromOneHeadOnePerSyntacticFrameNetStyle {
    public static void main(String[] args) throws IOException, Exception {
        List<Fragment> inputFramentList = HelperFragmentMethods.loadFragments("../lr/starsem/fragments/all.frg.txt/");
        System.err.println(inputFramentList.size() +" is the size of input fragment list");
        Map<String, Collection<Fragment>> partitionFragmentsByHeadVerb 
                = HelperFragmentMethods.partitionFragmentsByHeadVerb(inputFramentList);
        AtomicInteger counter = new AtomicInteger();
        Collection<ParseFrame> parseAll = new ConcurrentLinkedQueue<ParseFrame>();
        partitionFragmentsByHeadVerb.values().forEach(col->{
            try {
                Collection<ParseFrame> parseFragmentToFrame = HelperParseChartIO.parseFragmentToFrame(col, counter.incrementAndGet());
                parseAll.addAll(parseFragmentToFrame);
            } catch (Exception ex) {
                Logger.getLogger(EvaluateFromOneHeadOnePerSyntacticFrameNetStyle.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        Settings s = new Settings();
        GenerteGrammarFromClusters3 gc3 = new GenerteGrammarFromClusters3(s.getActiveDepToIntMap());
        gc3.genRules(inputFramentList, parseAll);
        RuleMaps theRuleMap = gc3.getTheRuleMap();
        
        
         
        EvaluationResultAndProcessContainer erpc 
                = new EvaluationResultAndProcessContainer("1hpcgc3", inputFramentList, new ExperimentPathBuilder("heretest"));
        erpc.buildDeafaultBaselines(theRuleMap);
        String baselinesToString = erpc.baselinesToString();
        System.err.println(baselinesToString);
        
        
        if(false){
        ISplit isplit = new SplitFrameHeads();
        theRuleMap = isplit.split(theRuleMap);
        }else{
            RuleMaps copy = new RuleMaps(theRuleMap);
            theRuleMap.simplyAddThisRuleMapParams(copy);
            theRuleMap.normalizeToOne();
    }
       // theRuleMap.normalizeToOne();
        erpc.addEvaluationPointMemWise(theRuleMap, true, 1, 2);
        
        EvResultWrapperForPruning addEvaluationPointMemWise = erpc.addEvaluationPointMemWise(theRuleMap, true, 1, 2);
        RulesCounts statisticsAboutRulesAreHere = addEvaluationPointMemWise.getRuleCounts();
        EvaluationResult lastEvaluation = erpc.getLastEvaluation();
        System.err.println("GR3 INIT: "+lastEvaluation.toStringShort(" "));
        for (int aFewIterations = 0; aFewIterations < 3; aFewIterations++) {
            RulesCounts estimateParameters = HelperLearnerMethods.estimateParameters(theRuleMap, inputFramentList);
            theRuleMap.updateParameters(estimateParameters);
        }
        erpc.addEvaluationPointMemWise(theRuleMap, true, 1, 2);
        
        EvaluationResult afterFewParameterEstimation = erpc.getLastEvaluation();
        System.err.println(""+afterFewParameterEstimation.toStringShort(" "));
        
       

        EvaluationResult afterRandomSplit = erpc.getLastEvaluation();
        System.err.println(""+afterRandomSplit.toStringShort(" "));
        
        for (int aFewIterations = 0; aFewIterations < 3; aFewIterations++) {
            RulesCounts estimateParameters = HelperLearnerMethods.estimateParameters(theRuleMap, inputFramentList);
            theRuleMap.updateParameters(estimateParameters);
        }
        erpc.addEvaluationPointMemWise(theRuleMap, true, 1, 2);
        
        EvaluationResult afterSplitWithFewParameterEstimation = erpc.getLastEvaluation();
        System.err.println(""+afterSplitWithFewParameterEstimation.toStringShort(" "));
        
        
      //  HelperMergeMethods.mergeFramesByLookingAtHeads(parseChartCollection, statisticsAboutRulesAreHere, s, theRuleMap, 0, 0);
        
    }
}
