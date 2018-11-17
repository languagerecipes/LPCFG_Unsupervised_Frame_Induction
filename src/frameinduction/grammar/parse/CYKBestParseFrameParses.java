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
package frameinduction.grammar.parse;

import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.parse.io.ParseFrame;
import input.preprocess.objects.Fragment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CYKBestParseFrameParses {

    private ExecutorService executor;
    private Collection<CYKParsingCallableBestParseFrame> task;

    public CYKBestParseFrameParses(Collection<Fragment> fragmentsAll, RuleMaps theRuleMap, int threadSize) throws InterruptedException {

        task = new ConcurrentLinkedQueue<>();
        executor = Executors.newFixedThreadPool(threadSize);
        int portion = Math.floorDiv(fragmentsAll.size(), threadSize) + 1;
        List<Collection<Fragment>> splitCollectionBySize = util.CollectionUtil.
                splitCollectionBySize(fragmentsAll, portion);

        //for (Collection<Fragment> fcol : splitCollectionBySize) {
        splitCollectionBySize.forEach(fcol -> {
            task.add(new CYKParsingCallableBestParseFrame(fcol, theRuleMap));

        });
//        for (int t = 0; t < threadSize - 1; t++) {
//            List<Fragment> subList = fragmentsAll.subList((int) Math.floor(t * portion), (int) Math.floor((t + 1) * portion));
//            task.add(new CYKParsingCallableAllParse(subList, theRuleMap));
//        }

        //   List<Fragment> subList = fragmentsAll.subList((int) Math.floor((threadSize - 1) * portion), fragmentsAll.size());
        //   task.add(new CYKParsingCallableAllParse(subList, theRuleMap));
    }

    public Map<Long, ParseFrame> run() throws InterruptedException {
        List<Future<Collection<ParseFrame>>> invokeAll = executor.invokeAll(task);
//        Collection<ParseChart> pcChartCol = new ConcurrentLinkedDeque<>();
        Map<Long, ParseFrame> parseCharQueue = new ConcurrentHashMap<>();
        invokeAll.forEach(cnsmr -> {
            try {
                Collection<ParseFrame> get = cnsmr.get();
                get.parallelStream().forEach(pf -> {
                    parseCharQueue.put(pf.getUniqueIntID(), pf);
                });
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(CYKBestParseFrameParses.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        executor.shutdown();
        return parseCharQueue;
    }

}
