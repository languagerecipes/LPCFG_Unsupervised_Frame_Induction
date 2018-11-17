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
import frameinduction.grammar.generate.GenerateGrammarFrameRoleIndepandantsJCLK;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import mhutil.HelperFragmentMethods;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class SplitDependencyStratgey1 implements ISplit{

    
    @Override
    public RuleMaps split(RuleMaps rm, int maxThreadhold ) throws Exception {
    if(rm.getSntxDepIDSet().size()>maxThreadhold){
        System.err.println("Skipped dependency splitting ...");
        return rm;
    }else{
        return split(rm);
    }
    }
    /**
     * 
     * @param rm
     * @throws Exception 
     */
    @Override
    public RuleMaps split(RuleMaps rm) throws Exception {
       Set<String> depNodeSet = new HashSet<>(rm.getSntxDepIDSet());
//        List<Integer> asSortedList = new ArrayList<>(frameIndexSet);
//        Collections.sort(asSortedList);
        // System.out.println(frameIndexSet);
        //RuleMaps copyRM = new RuleMaps(rm);
        System.err.println("Splitting " + depNodeSet.size() + " dependencies...");
        for (String nodeSymbolID: depNodeSet) {
          //  System.err.println("Splitting  " + nodeSymbolID +" " );
            SplitDependency spx = new SplitDependency(nodeSymbolID, rm);
            RuleMaps performSplit = spx.performSplit();

            rm = new RuleMaps(performSplit);

        }

        return rm;

    }
    
    public static void main(String[] args) throws  Exception {

        // Map<String, Integer> frameHeadIndices = getFrameHeadIndices(fromArchivedFile);
        
        List<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments("../lr_other/stanford-parse.txt", null, null, new Settings(), 10,11);
        System.out.println("Fragment size: " + inputFramentList.size() + " e.g., " + inputFramentList.get(0).toStringPosition());
        Logger.getGlobal().log(Level.FINE, "Fragment size: {0} e.g., {1}", new Object[]{inputFramentList.size(), inputFramentList.get(0).toStringPosition()});

        RuleMaps rm = null;
        
            GenerateGrammarFrameRoleIndepandantsJCLK gg = new GenerateGrammarFrameRoleIndepandantsJCLK();
            gg.genRules(inputFramentList, 1, 1);
            rm = gg.getTheRuleMap();
            //theRuleMap.seralizaRules(path.getInitGrammarFile());
            
        //RuleMaps rm = RuleMaps.fromArchivedFile("temp\\0-it.grmr.zip");
        for (int i = 0; i <2; i++) {
            
            SplitDependencyStratgey1 spdep = new SplitDependencyStratgey1();
            
            System.out.println("From  " + rm.getSntxDepIDSet().size() +" "+ rm.getSntxDepIDSet());
            rm = spdep.split(rm);
            
            System.out.println("TO: " + rm.getSntxDepIDSet().size() +" "+ rm.getSntxDepIDSet());
            
            System.err.println("-----------");
//            System.out.println("Frame From  " + rm.getFrameHeadIndices().size());
//            SplitFrameHeadsStratgey2 st2 = new SplitFrameHeadsStratgey2();
//            rm = st2.split(rm);
//            System.out.println("Frame TO: " + rm.getFrameHeadIndices().size());
            
        }
//
//        RuleMaps splitAllFrameHeads2 = split(split);
//        System.out.println("To  " + splitAllFrameHeads2.getFrameIndexSet().size());
//
//        RuleMaps splitAllFrameHeads3 = split(splitAllFrameHeads2);
//        System.out.println("To  " + splitAllFrameHeads3.getFrameIndexSet().size());
//
//        RuleMaps splitAllFrameHeads4 = split(splitAllFrameHeads3);
//        System.out.println("To  " + splitAllFrameHeads4.getFrameIndexSet().size());
        //splitAllFrameHeads.seralizaRules("temp\\fi-m1x\\splitAllFrameHeads4");
    }

    @Override
    public RuleMaps split(RuleMaps rm, double porpotionToSplit) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
