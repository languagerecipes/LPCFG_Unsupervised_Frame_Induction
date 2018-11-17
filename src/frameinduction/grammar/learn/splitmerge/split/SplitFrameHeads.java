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
package frameinduction.grammar.learn.splitmerge.split;

import frameinduction.grammar.RuleMaps;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class SplitFrameHeads implements ISplit {

    /**
     *
     * @param rm
     * @return
     * @throws Exception
     */
    @Override
    public RuleMaps split(RuleMaps rm) throws Exception {
        Set<Integer> frameIndexSet = new HashSet<>(rm.getFrameIndexSet());
        // System.out.println(frameIndexSet);
        RuleMaps copyRM = new RuleMaps(rm);

        for (int i : frameIndexSet) {

          //  System.out.println(copyRM.getFrameHeadLargestIndex());
            // System.out.println("Remove spit " + i);
            SplitHeads sp = new SplitHeads(i, copyRM);
            RuleMaps performSplit = sp.performSplit();

            copyRM = new RuleMaps(performSplit);
           // System.out.println(copyRM.getFrameIndexSet());
            //System.out.println("-----");

        }
        return new RuleMaps(copyRM);

    }

    public static void main(String[] args) throws IOException, Exception {

        // Map<String, Integer> frameHeadIndices = getFrameHeadIndices(fromArchivedFile);
        RuleMaps ruleMAp = RuleMaps.fromArchivedFile("temp/test-why.1.zip");
        System.out.println("From  " + ruleMAp.getFrameIndexSet().size());
        SplitFrameHeads spf = new SplitFrameHeads();
        RuleMaps splitAllFrameHeads = spf.split(ruleMAp);
        System.out.println("TO: " + splitAllFrameHeads.getFrameIndexSet().size());
//
//        RuleMaps splitAllFrameHeads2 = split(split);
//        System.out.println("To  " + splitAllFrameHeads2.getFrameIndexSet().size());
//
//        RuleMaps splitAllFrameHeads3 = split(splitAllFrameHeads2);
//        System.out.println("To  " + splitAllFrameHeads3.getFrameIndexSet().size());
//
//        RuleMaps splitAllFrameHeads4 = split(splitAllFrameHeads3);
//        System.out.println("To  " + splitAllFrameHeads4.getFrameIndexSet().size());
        // splitAllFrameHeads.seralizaRules("temp\\fi-m1x\\splitAllFrameHeads4");
    }

    @Override
    public RuleMaps split(RuleMaps rm, int limitOnNumberOfCategories) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RuleMaps split(RuleMaps rm, double porpotionToSplit) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
