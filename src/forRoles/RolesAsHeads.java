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
package forRoles;

import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.generate.GenerteGrammarFromClusters;
import frameinduction.grammar.parse.HelperParseMethods;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import mhutil.HelperFragmentMethods;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
public class RolesAsHeads {
    public static void main(String[] args) throws IOException, Exception {
        List<Fragment> loadFragments = HelperFragmentMethods.loadFragments("../lr/tacl/fragments/ambg.frg.txt");
        Collection<Fragment> transformFragmentsWithRaH = HelperFragmentMethods.transformFragmentsWithRaH(loadFragments,1);
        Settings s = new Settings();
        GenerteGrammarFromClusters gfc = new GenerteGrammarFromClusters(s.getActiveDepToIntMap());
        Map<String, Collection<Fragment>> fragmentsAsMapWithOneKey = HelperFragmentMethods.fragmentsAsMapWithOneKey(transformFragmentsWithRaH);
        gfc.genRules(fragmentsAsMapWithOneKey);
        RuleMaps theRuleMap = gfc.getTheRuleMap();
        Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrames(transformFragmentsWithRaH, theRuleMap);
        doParsingToBestFrames.forEach(p->{
            System.err.println(p);
        });
    }
    
}
