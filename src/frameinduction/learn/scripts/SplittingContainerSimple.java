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

import mhutil.HelperRuleMapsMethods;
import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.generate.GenerteGrammarFromClusters;
import mhutil.HelperParseChartIO;
import frameinduction.grammar.parse.io.ParseFrame;
import input.preprocess.objects.Fragment;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import mhutil.HelperFragmentMethods;
import frameinduction.grammar.parse.io.HierachyBuilderResultWarpper;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
public class SplittingContainerSimple {

    private Map<String, Integer> finalDependencyMapForGrammarGen;
    int minFragmentSizeToByPassSplitIteration;
    int innerEmLoops;// = 20;
    long SPLIT_TIMELIMIT = 60 * 60 * 60 * 1000; // set it to ten minutes
    long timeStampCalled;
    private Map<Integer, Collection<Fragment>> mergeFragmentMapsRet;
    private Map<Integer, Collection<ParseFrame>> mergeParseFrameMapsRet;
    private RuleMaps aggSumRuleMapsRet;
    Collection<Map<Integer, Collection<Fragment>>> fragHierachy;

    /**
     * Get a collection of fragments as in hierarchies instead of a flatten one
     *
     * @return
     */
    public Collection<Map<Integer, Collection<Fragment>>> getFragHierachy() {
        return fragHierachy;
    }

    public RuleMaps getAggSumRuleMapsRet() {
        return aggSumRuleMapsRet;
    }

    public Map<Integer, Collection<Fragment>> getMergeFragmentMapsRet() {
        return mergeFragmentMapsRet;
    }

    public SplittingContainerSimple(
            Map<String, Integer> finalDependencyMapForGrammarGen, int minFragmentSizeToByPassSplitIteration, int innerLoopEM
    ) {
        timeStampCalled = System.currentTimeMillis();
        this.finalDependencyMapForGrammarGen = finalDependencyMapForGrammarGen;
        this.minFragmentSizeToByPassSplitIteration = minFragmentSizeToByPassSplitIteration;
        innerEmLoops = innerLoopEM;
    }

    public Map<Integer, Collection<ParseFrame>> getMergeParseFrameMapsRet() {
        return mergeParseFrameMapsRet;
    }

    public void main(RuleMaps merfeRuleMap, Map<Integer, Collection<Fragment>> frgmntsClustered, int numThreads,
            boolean forceSplitInner, boolean forceSplitOnce) throws Exception {
        Collection<Collection<HierachyBuilderResultWarpper>> resultsInnerSp = new ConcurrentLinkedQueue<>();
        Collection<HierachyBuilderResultWarpper> splitInner1 = null;

        splitInner1
                = splitIndepandantlyInnerClusters(
                        frgmntsClustered, merfeRuleMap, numThreads, forceSplitOnce); // split at least once

        for (int i = 0; i < 1; i++) { // increase i to get more an more clusters (supposedly fine and pure) 
            FilterHierarchialResultBySize hpsFilte = new FilterHierarchialResultBySize(splitInner1,
                    Math.floorDiv(minFragmentSizeToByPassSplitIteration * (i + 1), 2));
            Collection<HierachyBuilderResultWarpper> toProcess = hpsFilte.getToProcess();
            Collection<HierachyBuilderResultWarpper> splitInner2 = new ConcurrentLinkedQueue<>();
            splitInner2.addAll(hpsFilte.getFiltered());
            if (!toProcess.isEmpty()) {
                Map<Integer, Collection<Fragment>> mergeFragmentFromHirResult
                        = HelperFragmentMethods.mergeFragmentFromHirResult(toProcess);
                Collection<HierachyBuilderResultWarpper> splitIndepandantlyInnerClusters
                        = splitIndepandantlyInnerClusters(
                                mergeFragmentFromHirResult, merfeRuleMap,//addRuleMaps,
                                numThreads, forceSplitInner);

                splitInner2.addAll(splitIndepandantlyInnerClusters);
            }
            splitInner1 = splitInner2;

            if (System.currentTimeMillis() - timeStampCalled > SPLIT_TIMELIMIT) {
                System.err.print("Terminating the split job by time limit set before.\r");
                break;
            }
        }
        //}
        resultsInnerSp.add(splitInner1);

        System.err.print("Aggregating split result ...\r");
        mergeFragmentMapsRet
                = HelperFragmentMethods.mergeFragmentMapsFromHirResultWithIntKey(resultsInnerSp);
        mergeParseFrameMapsRet
                = HelperParseChartIO.mergeParseFrameMaps(splitInner1);
        aggSumRuleMapsRet = HelperRuleMapsMethods.aggSumRuleMaps(resultsInnerSp);
        System.err.print("Split indepa to " + mergeFragmentMapsRet.size() + "  clusters\r");
        fragHierachy
                = new ConcurrentLinkedDeque<>();
        resultsInnerSp.forEach(hr -> {
            Map<Integer, Collection<Fragment>> mergeFragmentFromHirResult
                    = HelperFragmentMethods.mergeFragmentFromHirResult(hr);
            fragHierachy.add(mergeFragmentFromHirResult);
        });

    }

    //final Random r = new Random();
    private Collection<HierachyBuilderResultWarpper> splitIndepandantlyInnerClusters(
            Map<Integer, Collection<Fragment>> fgListCol, RuleMaps prevRM, int threadSize, boolean forceSplit) throws Exception {

        Collection<ProcessEestimateToTwoSplit> forParseToTwo = new ConcurrentLinkedQueue<>();
        fgListCol.values().parallelStream().forEach(fgList -> {
            try {
                GenerteGrammarFromClusters gfc = new GenerteGrammarFromClusters(finalDependencyMapForGrammarGen);
                Map<String, Collection<Fragment>> mp = new ConcurrentHashMap<>();
                mp.put("1", fgList);
                gfc.genRules(79.0, mp, finalDependencyMapForGrammarGen, 1); // was .49
                RuleMaps mpMap = gfc.getTheRuleMap();
                mpMap.simplyAddThisRuleMapParams(new RuleMaps(prevRM));
                ProcessEestimateToTwoSplit pe = new ProcessEestimateToTwoSplit(
                        fgList, mpMap, innerEmLoops, false);
                forParseToTwo.add(pe);
            } catch (Exception ex) {
                Logger.getLogger(SplittingContainerSimple.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        );
        System.err.print("Creating a thread pool " + threadSize + " for " + forParseToTwo.size() + " tasks\r");
        ExecutorService executor = Executors.newFixedThreadPool(threadSize);
        List<Future<HierachyBuilderResultWarpper>> invokeAll1 = executor.invokeAll(forParseToTwo);
        executor.shutdown();
        Collection<HierachyBuilderResultWarpper> hprc = new ConcurrentLinkedQueue<>();
        for (Future<HierachyBuilderResultWarpper> csr : invokeAll1) {
            HierachyBuilderResultWarpper get = csr.get();
            hprc.add(get);
        }

        return hprc;
    }

}
