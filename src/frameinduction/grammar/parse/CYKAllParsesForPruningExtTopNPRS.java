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
import input.preprocess.objects.Fragment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CYKAllParsesForPruningExtTopNPRS {

    private ExecutorService executor;
    private Collection<CYKParsingCallableAllParsePruningExtTopN> task;

    public CYKAllParsesForPruningExtTopNPRS(int tgtTopNParse, Collection<Fragment> fragmentsAll, RuleMaps theRuleMap, int threadSize) throws InterruptedException {

//        task = new ArrayList<>();
//        double portion = fragmentsAll.size() * 1.0 / threadSize;
//        executor = Executors.newFixedThreadPool(threadSize);
//        int size = fragmentsAll.size();
//        for (int t = 0; t < threadSize - 1; t++) {
//            Collection<Fragment> subList = fragmentsAll.subList((int) Math.floor(t * portion), (int) Math.floor((t + 1) * portion));
//            task.add(new CYKParsingCallableAllParsePruningExtTopN(subList, theRuleMap, tgtTopNParse));
//        }
//        List<Fragment> subList = fragmentsAll.subList((int) Math.floor((threadSize - 1) * portion), fragmentsAll.size());
//        task.add(new CYKParsingCallableAllParsePruningExtTopN(subList, theRuleMap, tgtTopNParse));
        task = new ConcurrentLinkedQueue<>();
        executor = Executors.newFixedThreadPool(threadSize);
        int portion = Math.floorDiv(fragmentsAll.size(), threadSize)+1;
        List<Collection<Fragment>> splitCollectionBySize = util.CollectionUtil.splitCollectionBySize(fragmentsAll, portion);

        //for (Collection<Fragment> fcol : splitCollectionBySize) {
        splitCollectionBySize.parallelStream().forEach(fcol -> {
            task.add(new CYKParsingCallableAllParsePruningExtTopN(fcol, theRuleMap, tgtTopNParse));

        });
    }

    public List<Future<Collection<ParseDataCompact>>> run() throws InterruptedException {
        List<Future<Collection<ParseDataCompact>>> invokeAll = executor.invokeAll(task);
        executor.shutdown();
        return invokeAll;
    }

}
