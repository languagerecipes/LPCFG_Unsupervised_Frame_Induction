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
package frameinduction.evaluation;

import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.parse.io.ParseFrame;
import java.util.Collection;

/**
 *
 * @author Behrang QasemiZadeh <me at atmykitchen.info>
 */
public class EvResultWrapperForPruning {

    final private RulesCounts ruleCounts;
    final private Collection<ParseFrame> bestFramesList;

    public EvResultWrapperForPruning(RulesCounts ruleCounts, Collection<ParseFrame> bestFramesList) {
        this.ruleCounts = ruleCounts;
        this.bestFramesList = bestFramesList;
    }

    public Collection<ParseFrame> getBestFramesList() {
        return bestFramesList;
    }

    public RulesCounts getRuleCounts() {
        return ruleCounts;
    }

}
