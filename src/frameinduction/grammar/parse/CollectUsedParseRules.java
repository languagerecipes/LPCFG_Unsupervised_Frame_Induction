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
import input.preprocess.objects.FragmentCompact;

import java.util.Collection;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import java.util.logging.Logger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CollectUsedParseRules implements Callable<Collection<ParseChart>> {

    final private List<Fragment> fcp;
    final private RuleMaps theRuleMap;
    //int id;

    public CollectUsedParseRules(List<Fragment> fcp, RuleMaps theRuleMap) {
        this.fcp = fcp;
        this.theRuleMap = theRuleMap;
        //this.id = id;

    }

    @Override
    public Collection<ParseChart> call() {

        Collection<ParseChart> parseCharQueue = new ConcurrentLinkedQueue<>();

        for (Fragment fc : fcp) {
            try {
                CYKParser cyk = new CYKParser();
                ParseChart parse = cyk.parse(new FragmentCompact(fc, theRuleMap), theRuleMap);
                parseCharQueue.add(parse);
            } catch (Exception ex) {
                Logger.getLogger(CollectUsedParseRules.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return parseCharQueue;
    }

}
