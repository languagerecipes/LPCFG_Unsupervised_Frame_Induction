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
package frameinduction.grammar.learn;

import frameinduction.grammar.RulesCounts;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class IOProcessCompleteDataContainer {
    private final RulesCounts rc;
    private final Map<Integer,IOParam> ioParamSummary;
    final private Collection<InOutParameterChart> paraChartCollection;

    public IOProcessCompleteDataContainer(RulesCounts rc, Collection<InOutParameterChart> paraChartCollection) {
        this.rc = rc;
        this.ioParamSummary = null;
        this.paraChartCollection = paraChartCollection;
    }

    public Collection<InOutParameterChart> getParaChartCollection() {
        return paraChartCollection;
    }

    
    
    
    
    public IOProcessCompleteDataContainer(RulesCounts rc, Map<Integer,IOParam>  ioParamSummary) {
        this.rc = rc;
        this.ioParamSummary = ioParamSummary;
        this.paraChartCollection= null;
    }

    public Map<Integer, IOParam> getIoParamSummary() {
        return ioParamSummary;
    }

    public RulesCounts getRc() {
        return rc;
    }
 
    
}
