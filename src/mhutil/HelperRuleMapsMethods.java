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
package mhutil;

import frameinduction.grammar.RuleMaps;
import input.preprocess.objects.DepandantNode;
import input.preprocess.objects.Fragment;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import frameinduction.grammar.parse.io.HierachyBuilderResultWarpper;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author behra
 */
public class HelperRuleMapsMethods {

    public static RuleMaps interpolateRuleMaps(Collection<HierachyBuilderResultWarpper> hireResult) throws Exception {
        if (hireResult.size() == 0) {
            throw new RuntimeException("An empty list is passed for aggregating rules code:8236456!");
        }
        Iterator<HierachyBuilderResultWarpper> iterator = hireResult.iterator();
        HierachyBuilderResultWarpper get = iterator.next();

        RuleMaps ruleMap = new RuleMaps(get.getRuleMap());
        //for (int i = 1; i < hireResult.size(); i++) {
        while (iterator.hasNext()) {
//            ruleMap.accumulateInverseRuleMaps(ruleMap, true, .5);
//           
            ruleMap.simplyAddThisRuleMapParams(iterator.next().getRuleMap());
        }
        ruleMap.normalizeToOne();
        return ruleMap;
    }

    public static RuleMaps addRuleMaps(Collection<HierachyBuilderResultWarpper> hireResult) throws Exception {
        if (hireResult.size() == 0) {
            throw new RuntimeException("An empty list is passed for aggregating rules code:8236456!");
        }
        Iterator<HierachyBuilderResultWarpper> iterator = hireResult.iterator();
        HierachyBuilderResultWarpper get = iterator.next();

        RuleMaps ruleMap = new RuleMaps(get.getRuleMap());
        ruleMap.pruneRules(Math.log(1e-177));
        //for (int i = 1; i < hireResult.size(); i++) {
        while (iterator.hasNext()) {
//            ruleMap.accumulateInverseRuleMaps(ruleMap, true, .5);
//           
            ruleMap.simplyAddThisRuleMapParams(iterator.next().getRuleMap());
            ruleMap.pruneRules(Math.log(1e-177));
        }
        ruleMap.normalizeToOne();
        ruleMap.pruneRules(Math.log(1e-177));
        return ruleMap;
    }

    public static RuleMaps addRuleMapsCol(Collection<Collection<HierachyBuilderResultWarpper>> hireResult) throws Exception {
        if (hireResult.size() == 0) {
            throw new RuntimeException("An empty list is passed for aggregating rules code:8236456!");
        }
        Iterator<Collection<HierachyBuilderResultWarpper>> iterator = hireResult.iterator();
        Collection<HierachyBuilderResultWarpper> get = iterator.next();
        RuleMaps addRuleMaps = addRuleMaps(get);

        //for (int i = 1; i < hireResult.size(); i++) {
        while (iterator.hasNext()) {
//            ruleMap.accumulateInverseRuleMaps(ruleMap, true, .5);
            RuleMaps addRuleMaps1 = addRuleMaps(iterator.next());
            addRuleMaps.simplyAddThisRuleMapParams(addRuleMaps1);
        }
        addRuleMaps.normalizeToOne();
        addRuleMaps.pruneRules(Math.log(1e-177));
        return addRuleMaps;
    }

    public static RuleMaps addRuleMapsMax(Collection<HierachyBuilderResultWarpper> hireResult) throws Exception {
        if (hireResult.size() == 0) {
            throw new RuntimeException("An empty list is passed for aggregating rules code:8236456!");
        }
        Iterator<HierachyBuilderResultWarpper> iterator = hireResult.iterator();
        HierachyBuilderResultWarpper get = iterator.next();

        RuleMaps ruleMap = new RuleMaps(get.getRuleMap());
        // ruleMap.pruneRules(Math.log(1e-177));
        //for (int i = 1; i < hireResult.size(); i++) {
        while (iterator.hasNext()) {
//            ruleMap.accumulateInverseRuleMaps(ruleMap, true, .5);
//           
            ruleMap.simplyAsssignMaxParamBetweenTheTwo(iterator.next().getRuleMap());
            //  ruleMap.pruneRules(Math.log(1e-177));
        }
        //  ruleMap.normalizeToOne();
        // ruleMap.pruneRules(Math.log(1e-377));
        return ruleMap;
    }

    /**
     * Add terminal nodes that are missing in the given rulemaps to the
     * rulemaps, used this method when using embedding based parsing
     *
     * @param theseAreFragmentToParse
     * @param rm
     */
    public static void addNewTerminals(Collection<Fragment> theseAreFragmentToParse, RuleMaps rm) throws Exception {
        rm.verifySymbolCounterStatus();
        for (Fragment f : theseAreFragmentToParse) {
            List<DepandantNode> terminals = f.getTerminals();
            for (DepandantNode dp : terminals) {

                rm.getIDFromSymbol(dp.getTerminalString());
            }
        }
    }

    /**
     * Read all the rulemaps in a folder, sum and then average params for all
     * the rules and their params into one single rulemaps
     *
     * @param ruleMapsInHPRCWrap
     * @return
     * @throws Exception
     */
    public static RuleMaps aggSumRuleMaps(Collection<Collection<HierachyBuilderResultWarpper>> ruleMapsInHPRCWrap) throws Exception {

        RuleMaps sum = new RuleMaps();
        sum.buildReverseRandomIndices();
AtomicInteger ai = new AtomicInteger();
        ruleMapsInHPRCWrap.forEach(rmCol -> {
            rmCol.forEach((rmWrap) -> {
                try {
                    sum.simplyAddThisRuleMapParams(rmWrap.getRuleMap());
                   // sum.seralizaRules("texttt"+ai.incrementAndGet()+".zip");
                } catch (Exception ex) {
                    
                    Logger.getLogger(HelperRuleMapsMethods.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        });

        sum.normalizeToOne();

        return sum;

    }

    public static RuleMaps aggSumRuleMaps(String folderName) throws Exception {
        File root = new File(folderName);
        File[] listFiles = root.listFiles();

        int divCounter = 1;
        if (listFiles.length < 1) {
            throw new Exception("No grammar files found ... code:23833");
        }
        RuleMaps sum = RuleMaps.fromArchivedFile(listFiles[0].getAbsolutePath());
        for (int i = 1; i < listFiles.length; i++) {

            RuleMaps fromArchivedFile = RuleMaps.fromArchivedFile(listFiles[i].getAbsolutePath());
            sum.simplyAddThisRuleMapParams(fromArchivedFile);
            divCounter++;
        }
        sum.normalizeToOne();
        if (divCounter > 1) {
            // sum.normalizeParametersByDevidingTo(divCounter);
        }
        return sum;

    }

    /**
     * Read all the rulemaps in a folder, sum and then average params for all
     * the rules and their params into one single rulemaps
     *
     * @param folderName
     * @return
     * @throws Exception
     */
    @Deprecated
    public static RuleMaps aggSumRuleMapsByMaxParams(String folderName) throws Exception {
        File root = new File(folderName);
        File[] listFiles = root.listFiles();

        int divCounter = 1;
        if (listFiles.length < 1) {
            throw new Exception("No grammar files found ... code:23833");
        }
        RuleMaps sum = RuleMaps.fromArchivedFile(listFiles[0].getAbsolutePath());
        for (int i = 1; i < listFiles.length; i++) {

            RuleMaps fromArchivedFile = RuleMaps.fromArchivedFile(listFiles[i].getAbsolutePath());
            sum.simplyAsssignMaxParamBetweenTheTwo(fromArchivedFile);
            divCounter++;
        }
        sum.normalizeToOne();
        if (divCounter > 1) {
            // sum.normalizeParametersByDevidingTo(divCounter);
        }
        return sum;

    }
@Deprecated
    public static RuleMaps aggSumRuleMapsByMaxParams(String folderName, String filterFileNameEnding) throws Exception {
        File root = new File(folderName);
        FilenameFilter textFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(filterFileNameEnding)) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] listFiles = root.listFiles(textFilter);

        int divCounter = 1;
        if (listFiles.length < 1) {
            return null;
            //throw new Exception("No grammar files found ... code:23833");
        }
        RuleMaps sum = RuleMaps.fromArchivedFile(listFiles[0].getAbsolutePath());
        for (int i = 1; i < listFiles.length; i++) {

            RuleMaps fromArchivedFile = RuleMaps.fromArchivedFile(listFiles[i].getAbsolutePath());
            sum.simplyAsssignMaxParamBetweenTheTwo(fromArchivedFile);
            divCounter++;
        }
        sum.normalizeToOne();
        sum.normalizeParametersByDevidingTo(4);
        if (divCounter > 1) {
            // sum.normalizeParametersByDevidingTo(divCounter);
        }
        return sum;

    }

    public static RuleMaps aggSumRuleMapsByMaxParams(Collection<RuleMaps> rmCollection) throws Exception {
        Iterator<RuleMaps> iterator = rmCollection.iterator();
        RuleMaps sum = new RuleMaps(iterator.next());
        while (iterator.hasNext()) {

            sum.simplyAsssignMaxParamBetweenTheTwo(iterator.next());

        }
      //  sum.normalizeToOne();

        return sum;

    }

    public static void main(String[] args) throws IOException {
        RuleMaps fromArchivedFile = RuleMaps.fromArchivedFile("C:\\prj\\baseline-system-semeval2019\\Frame_Induction_LPCFG\\temp\\4it-dev.txt-nzrprts-1532164452593-stanford-parse-conllu.txt\\grammar\\3-it.grmr.zip2-split.grmr.zip");
        fromArchivedFile.setClusterIDCounterStatus();
        AtomicInteger clusterIndexCounter = fromArchivedFile.getClusterIndexCounter();
        System.err.println(clusterIndexCounter);
    }

    
}
