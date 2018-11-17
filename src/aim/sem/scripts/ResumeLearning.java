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
package aim.sem.scripts;

import frameinduction.learn.scripts.MainLearnScript;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import mhutil.HelperFragmentMethods;
import frameinduction.grammar.parse.io.HierachyBuilderResultWarpper;
import static util.HelperGeneralInfoMethods.getCurrentClassName;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
public class ResumeLearning {

    public static void main(String[] args) throws Exception {

        List<String> gFiles = new ArrayList<>();
        gFiles.add("../lr/tacl/golddata/dev.txt"); //15

        int datasetIndex = 0;
        File datasetFile = new File(gFiles.get(datasetIndex));

        Logger.getGlobal().setLevel(Level.WARNING);
        String treebankName
                = "stanford-parse-conllu.txt";
        //  "psd-conlu-penn-spd.txt";

        String masterInputParsedFilePath
                = "../lr/treebanks/" + treebankName;
        // "../lr/treebanks/";
        long i = 0;//System.currentTimeMillis();
        int maxIterationSplitMErge = 21;
        String resumeRuleMap = "../experiments\\all\\3it-all.txt-rts-1512464660210-stanford-parse-conllu.txt\\grammar/24-it.grmr.zip.mrgd.zip";
        int j = -999;
        // for (int j = 2; j < 5; j++) {

        String rootNameToUse = j + "it-" + datasetFile.getName() + "-rts-" + System.currentTimeMillis() + "-" + treebankName + "/";
        Settings settings = new Settings();

        List<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments(
                masterInputParsedFilePath, null, null, settings,
                -1,
                13, datasetFile.getAbsolutePath());

        // with a two split and one merge this is 7 merge
        WordPairSimilaritySmallMemoryFixedID vectorSimilarities = cashVectorSimilarities(inputFramentList);
        double mergeStartValue = .4;//.75;//.65;
        double mergeMinValStep2 = .5;//.28; //.35; // this is irrelavent for everyIteraion%2 i removed it 
        /// 
        double mergeEndValueStep1 = .07;//.3;//0.3;//.05;//.4//.3;
        MainLearnScript smg = new MainLearnScript(
                false,
                false,
                rootNameToUse,
                gFiles.get(0),
                inputFramentList, maxIterationSplitMErge, vectorSimilarities,
                mergeMinValStep2, mergeStartValue, mergeEndValueStep1
        );

        System.err.println("Running " + getCurrentClassName(smg));
        HierachyBuilderResultWarpper mainSimple = smg.start(resumeRuleMap);
        //  }
    }

    /**
     * Cash vector similaritites
     *
     * @param inputFramentList
     * @return
     * @throws Exception
     */
    private static WordPairSimilaritySmallMemoryFixedID cashVectorSimilarities(Collection<Fragment> inputFramentList) throws Exception {
        Set<String> makeVocab = HelperFragmentMethods.makeVocab(inputFramentList);
        WordPairSimilaritySmallMemoryFixedID wsmfix = new WordPairSimilaritySmallMemoryFixedID(
                "../lr/vectors/AfterFinPVHFNG-en-900-cLR-5-3.txt", makeVocab, 900);
        return wsmfix;
    }
}
