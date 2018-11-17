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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class Settings {
    
   

    public static int DEF_THREAD_NUM_TO_USE = 24;
    public static boolean SERR_DEBUG = false;
    public String printSettings(){
        StringBuilder sb = new StringBuilder();
        sb.append("|F| (start)=" + frameCardinality +"");
        sb.append("|R| (start)=" + semanticRoleCardinality+"");
        sb.append("Dep-list =[" + depR+"]");
        sb.append("Tgt-head-pos ="+targetPos+"");
        return sb.toString();
    }
    
    
    private boolean updatedWithInitialCounts = true;
    private int numberOfThread = 32;
    private int frameCardinality = 2;
    private int semanticRoleCardinality = 2;
    private int numberOfEMIterations = 3420;
    private int maxNumberOfFragmentInstances = 0;
    private double proportionOfTopParsesToKeep = .5;
   static private String depR
            = //"ats;mod;obj;suj;de_obj;p_obj.o;a_obj;ato";
      //     "cbow;"+
            
"subj;"+
            
            "nsubj;"
            +
            "dobj;"
            + "iobj;"
            + "ccomp;"
             
           + "xcomp;"
            + "nmod:in;"
            + "nmod:to;"
            + "nmod:for;"
            +"nmod;"
            + "nmod:at;"
            + "nmod:on;"
            + "nmod:by;"
            + "acl;"
            + "ccomp;"
            + "xcomp;"
            + "dvcl;"
            + "advmod;"
            + "acomp;"
//            +allnmodes;
//    String allnmodes=
          +  "nmod:about;" +
"nmod:above;" +
"nmod:according_to;" +
"nmod:across;" +
"nmod:after;" +
"nmod:against;" +
"nmod:agent;" +
"nmod:along;" +
"nmod:amid;" +
"nmod:among;" +
"nmod:around;" +
"nmod:as;" +
"nmod:as_of;" +
"nmod:at;" +
"nmod:because_of;" +
"nmod:before;" +
"nmod:below;" +
"nmod:between;" +
"nmod:by;" +
"nmod:despite;" +
"nmod:due_to;" +
"nmod:during;" +
"nmod:for;" +
"nmod:from;" +
"nmod:in;" +
"nmod:into;" +
"nmod:next_to;" +
"nmod:of;" +
"nmod:off;" +
"nmod:on;" +
"nmod:out_of;" +
"nmod:outside;" +
"nmod:over;" +
"nmod:past;" +
"nmod:since;" +
"nmod:starting;" +
"nmod:through;" +
"nmod:throughout;" +
"nmod:till;" +
"nmod:tmod;" +
"nmod:to;" +
"nmod:together_with;" +
"nmod:toward;" +
"nmod:under;" +
"nmod:until;" +
"nmod:up;" +
"nmod:upon;" +
"nmod:via;" +
"nmod:while;" +
"nmod:with;" +
"nmod:within;" +
"nmod:without;";

   static private Set<String>  depSet = new HashSet<>();
    static{
        List<String> asList = Arrays.asList(depR.split(";"));
        for(String asLiString:asList){
              depSet.add(asLiString);
        }
//        for (int i = 0; i < 400; i++) {
//            depSet.add(i+"ww");
//            
//            
//        }
        
    }
    
    private String targetPos = "V.*";

    public void setFrameCardinality(int frameCardinality) {
        this.frameCardinality = frameCardinality;
    }

    public void setProportionOfTopParsesToKeep(double proportionOfTopParsesToKeep) {
        this.proportionOfTopParsesToKeep = proportionOfTopParsesToKeep;
    }

    
    public double getProportionOfTopParsesToKeep() {
        return proportionOfTopParsesToKeep;
    }

    public boolean isUpdatedWithInitialCounts() {
        return updatedWithInitialCounts;
    }

   
    
    
    

    public void setSemanticRoleCardinality(int semanticRoleCardinality) {
        this.semanticRoleCardinality = semanticRoleCardinality;
    }

    
    
    public String getTargetPos() {
        return targetPos;
    }

    public int getNumberOfThread() {
        return numberOfThread;
    }

    public Set<String> getActiveDepr() {
        //Set<String> sep = new HashSet<>(Arrays.asList(depR.split(";")));
        return depSet;
    }

    public void setDepSet(Set<String> depSet) {
        this.depSet = depSet;
    }
    
   
    
    /**
     * mapping from dependency relations to a set of intergers particularly for grammar generation whenever dep types are required to be assigned to a unique integer key such as used in fixed role grammar
     * @return 
     */
    public Map<String,Integer> getActiveDepToIntMap(){ 
        Set<String> activeDepr = getActiveDepr();
    
        Map<String,Integer> finalDependencyMapForGrammarGen = new HashMap<>();
        int depCounter=0;
        for(String dep: activeDepr){
            finalDependencyMapForGrammarGen.put(dep,++depCounter);
        }
        return Collections.unmodifiableMap(finalDependencyMapForGrammarGen);
        
    }

    public int getMaxNumberOfFragmentInstances() {
        return maxNumberOfFragmentInstances;
    }

    public int getFrameCardinality() {
        return frameCardinality;
    }

    public int getSemanticRoleCardinality() {
        return semanticRoleCardinality;
    }

    public int getNumberOfEMIterations() {
        return numberOfEMIterations;
    }
    
    
//    public Settings() {
//        loadSettings("settings.txt");
//    }

    
    public Settings() {
     
    }

    public Settings(String file) {
        loadSettings(file);
    }
    private void loadSettings(String file){
        try {
            System.out.println("Trying to load settings from settings.txt");
            BufferedReader br = new BufferedReader(new FileReader(new File(file)));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    String[] split = line.split("=");
                    if (split.length == 2) {
                        String attribute = split[0].trim().toLowerCase();
                        String value = split[1].trim();
                        if ("semantic_role_max_cardinality".equals(attribute)) {
                            semanticRoleCardinality = Integer.parseInt(value.trim());
                            System.out.println("Semantic role cardinality is set to " + semanticRoleCardinality);
                        } else if ("frame_max_cardinality".equals(attribute)) {
                            frameCardinality = Integer.parseInt(value.trim());
                            System.out.println("Frame cardinality is set to " + semanticRoleCardinality);
                        } else if ("number_of_threads".equals(attribute)) {
                            numberOfThread = Integer.parseInt(value);
                            System.out.println("Number of threads set to " + numberOfThread);
                        } else if ("target_head_pos".equals(attribute)) {
                            targetPos = value;
                            System.out.println("Target head PoS is set to " + targetPos);
                        } else if ("target_dep_relations".equals(attribute)) {
                            if ("null".equalsIgnoreCase(value)) {
                                depR = null;
                            } else {
                                depR = value;
                            }
                            System.out.println("The target dependency relations are set to " + depR);
                        } else if ("max_learning_instances".equals(attribute)) {
                            int maxInstanceFragment = Integer.parseInt(value.trim());
                            if (maxInstanceFragment < 0) {
                                System.out.println("Invalid maximum number ofor learning instances; your input is ignored and the value is set to unlimited ");
                            } else {
                                maxNumberOfFragmentInstances = maxInstanceFragment;
                                System.out.println("Maximum number of learning instances are " + maxNumberOfFragmentInstances);
                            }

                        }else if ("number_of_em_iterations".equals(attribute)) {
                            numberOfEMIterations = Integer.parseInt(value);
                            System.out.println("Number of EM iterations set to " + numberOfEMIterations);
                        }
                    } else if (line.trim().length() > 0) {
                        System.out.println("! skipped line --> " + line);
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

//    public void setDependnecySet(Set<String> dependency) {
//        this.
//       
//    }

}
