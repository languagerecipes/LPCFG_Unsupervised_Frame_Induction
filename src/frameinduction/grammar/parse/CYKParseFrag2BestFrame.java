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
import mhutil.HelperFragmentIOUtils;
import input.preprocess.objects.Fragment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CYKParseFrag2BestFrame {

    //private Set<String> targetLemma;
    private int threadSizex;
    //private String pathToFrameFile;
    private String grammarFile;
    private String outputPath;
    private List<Fragment> unparsedFragments;

    public CYKParseFrag2BestFrame(
            int threadSizex,
            String pathToFrameFile,
            String grammarFile,
            String outputPath
    ) throws IOException {
        this(null, threadSizex,
                pathToFrameFile,
                grammarFile,
                outputPath);
    }

    public CYKParseFrag2BestFrame(
            int threadSizex,
            List<Fragment> fragments,
            String grammarFile,
            String outputPath) throws IOException {
        this.threadSizex = threadSizex;
        this.grammarFile = grammarFile;
        this.outputPath = outputPath;
        unparsedFragments = Collections.unmodifiableList(fragments);
    }

    public CYKParseFrag2BestFrame(Set<String> targetLemma,
            int threadSizex,
            String pathToFrameFile,
            String grammarFile,
            String outputPath
    ) throws IOException {
        // this.targetLemma = targetLemma;
        this.threadSizex = threadSizex;
        //this.pathToFrameFile = pathToFrameFile;
        this.grammarFile = grammarFile;
        this.outputPath = outputPath;
        unparsedFragments = HelperFragmentIOUtils.loadFragments(pathToFrameFile, targetLemma);

    }

    public void parseDumpProcess() throws IOException, InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool(threadSizex);
        int threadSize = threadSizex * 2;
        double portion = unparsedFragments.size() * 1.0 / threadSize;
        List<CYKParsingCallableBestFrame> mtTasks = new ArrayList<>();
        RuleMaps rm = RuleMaps.fromArchivedFile(grammarFile);
        for (int t = 0; t < threadSize - 1; t++) {
            List<Fragment> subList = unparsedFragments.subList((int) Math.floor(t * portion), (int) Math.floor((t + 1) * portion));
            RuleMaps copy = new RuleMaps(
                    rm            );
            mtTasks.add(new CYKParsingCallableBestFrame(subList, copy));
        }
        List<Fragment> subList = unparsedFragments.subList((int) Math.floor((threadSize - 1) * portion), unparsedFragments.size());
        mtTasks.add(new CYKParsingCallableBestFrame(subList, rm));
        Logger.getGlobal().log(Level.INFO, "Invoking PARSE task list of size {0}", mtTasks.size());

        List<Future<Collection<String>>> resultsAll = executor.invokeAll(mtTasks);

        executor.shutdown();

        File outputPathFile = new File(outputPath);
        String parent = outputPathFile.getParent();
        File base = new File(parent);
        if (!base.exists()) {
            base.mkdirs();
        }
        PrintWriter pwMachineReadProp = new PrintWriter(new FileWriter(outputPathFile));
        for (Future<Collection<String>> result : resultsAll) {
            for (String frame : result.get()) {
                pwMachineReadProp.println(frame);
            }
        }
        pwMachineReadProp.close();
        Logger.getGlobal().log(Level.FINER, "Done Process of parsing and dumping results");

    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        Logger.getGlobal().setLevel(Level.FINEST);

        String inputFragment = "../sysgen_lr/fragments/basic-fragment-wsj.txt";
        String outputFragmentPrsd = "temp/temp/e-basic-fragment-wsj.prsd.txt";
        String pathGrammar = "temp/temp/final-output.zip";
        CYKParseFrag2BestFrame pts = new CYKParseFrag2BestFrame(8, inputFragment, pathGrammar, outputFragmentPrsd);
        pts.parseDumpProcess();

    }
}
