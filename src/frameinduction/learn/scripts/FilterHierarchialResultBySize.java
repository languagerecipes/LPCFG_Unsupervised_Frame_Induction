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

import java.util.ArrayList;
import java.util.Collection;
import frameinduction.grammar.parse.io.HierachyBuilderResultWarpper;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
public class FilterHierarchialResultBySize {

    Collection<HierachyBuilderResultWarpper> InputResult;
    Collection<HierachyBuilderResultWarpper> filtered;
    Collection<HierachyBuilderResultWarpper> toProcess;

    public FilterHierarchialResultBySize(Collection<HierachyBuilderResultWarpper> InputResult, int sizePerCluster) {
        this.InputResult = InputResult;
        splitBySize(sizePerCluster);
    }

    private void splitBySize(int sizePerCluster) {
        filtered = new ArrayList<>();
        toProcess = new ArrayList<>();
        for (HierachyBuilderResultWarpper hr : InputResult) {
            if (hr.getFragmentSize() > sizePerCluster) {
                toProcess.add(hr);
            } else {
                filtered.add(hr);
            }
        }

    }

    public Collection<HierachyBuilderResultWarpper> getFiltered() {
        return filtered;
    }

    public Collection<HierachyBuilderResultWarpper> getToProcess() {
        return toProcess;
    }

}
