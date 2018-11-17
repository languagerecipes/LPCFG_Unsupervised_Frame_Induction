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
package frameinduction.settings;

import java.io.File;
import java.util.Scanner;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class ExperimentPathBuilder {

    private static final String OUTPUT_DIR = "output/";
    private static final String GRAMR_DIR = "grammar/";
    private static final String PERFORMANCE_LOG_FILE_NAME = "performance-log.text";
    private static final String CHART_FILE_NAME = "chart-file-data.xml";
    private static final String CHART_FILE_NAME_FOR_ROLES = "roles_chart-file-data.xml";
    static private final String INIT_GRAMMAR_FILE_NAME = "init-si-grammar.zip";
    static private final String OUTPUT_FRAME_FILE_NAME = "-iteration-frames.txt";
   static private final String GRAMMAR_FILE_ENDING = "-it.grmr.zip";
   static private final String FRAGMENT_FILES_TO_LEARN_FROM = "input-fragments-for-learning.txt";
   
   
    public String getPathToInputFragmentForLearning() {
        return root + FRAGMENT_FILES_TO_LEARN_FROM;
    }
    private String pathToVector = "../lr/vectors/AfterFinPVHFNG-en-900-cLR-5-3.txt";
    private String goldData
            = //"../golddata/annotations/framenet.txt";
           null;// "../lr/golddata/annotations/framenet-ambig.txt";
    private String root;

    public String getOutputFrameFile(int iteration, String suffixFileName) {
        return root + OUTPUT_DIR + iteration + OUTPUT_FRAME_FILE_NAME+"."+suffixFileName;
        
    }
     public String getOutputFrameFile(int iteration) {
        return root + OUTPUT_DIR + iteration + OUTPUT_FRAME_FILE_NAME;
        
    }
    public void setGoldData(String goldData) {
        this.goldData = goldData;
    }

    public String getPathToVector() {
        return pathToVector;
    }

    
    
    public ExperimentPathBuilder(String root, String goldData) {
        this(root);
        this.goldData = goldData;
    }
    
    public ExperimentPathBuilder(String root, String goldData, boolean warnOverwite) {
        this(root, warnOverwite);
        this.goldData = goldData;
    }

    public String getExperimentRootFolder() {
        return root;
    }

    public ExperimentPathBuilder(String root) {
        this( root, false);
    }
    public ExperimentPathBuilder(String root, boolean warn) {
        this.root = root;
        File rootFile = new File(root);
        // read the setting files
        if (!rootFile.exists()) {
            rootFile.mkdirs();
        } else {
            if(warn){
            System.out.println("The specified root directory exists... do you wish to continute? (y=yes,anything elese=no)");
            Scanner scanner = new Scanner(System.in);
            if (scanner.next().equalsIgnoreCase("y")) {
                System.out.println("This will be fun!");
            } else {

                System.out.println("Maybe next time ... Goodbye!");
                System.exit(0);
            }}
        }
        File grammar = new File(root + GRAMR_DIR);
        if (!grammar.exists()) {
            grammar.mkdir();
        }
        File out = new File(root + OUTPUT_DIR);
        if (!out.exists()) {
            out.mkdir();
        }
    }

    public String getGoldData() {
        return goldData;
    }

    public String getGrammarPath() {
        return root + GRAMR_DIR;
    }

    public String getOutputPath() {
        return root + OUTPUT_DIR;
    }

    public String getInitGrammarFile() {
        return root + GRAMR_DIR + INIT_GRAMMAR_FILE_NAME;
    }

    public String getGrammarFile(int iteration) {
        
        return root + GRAMR_DIR + iteration + GRAMMAR_FILE_ENDING;
    }

    

    public File getChartDataFile() {
        return new File(root + CHART_FILE_NAME);
    }
public File getChartDataFileForRoles() {
        return new File(root + CHART_FILE_NAME_FOR_ROLES);
    }
    public File getPerformanceLogFile() {
        return new File(root + PERFORMANCE_LOG_FILE_NAME);
    }

}
