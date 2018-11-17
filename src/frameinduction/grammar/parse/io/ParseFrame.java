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
package frameinduction.grammar.parse.io;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class ParseFrame {

    private String frameSenDocIdentifier; // used for evaluation: sentence id + doc id etc. e.g., the id used for WSJ sentences
    private long frameNumber;
    private double likelihood;
   // private String frameNumberStr;//used when matching with gold data only for
    private String head;
    private List<SemRolePair> semanticRoles;
    private String headPositionInSentence;
    private Map<String, List<String>>  rolesAsMap;
    private Long uniqueIntID = null;

    public Long getUniqueIntID() {
        
        if (uniqueIntID == null) {
            
            
            if(true){
            return UUID.nameUUIDFromBytes((headPositionInSentence+frameSenDocIdentifier).getBytes()).getMostSignificantBits();
        }
            
            long parseInt = Long.parseLong(headPositionInSentence);
            //System.err.println(">>> " + parseInt);
            String formattedHead = String.format("%02d", parseInt);
            String substring = frameSenDocIdentifier.substring(1);
            //try{
            uniqueIntID = Long.parseLong(substring+formattedHead);
//            }catch (Exception e){
//                System.err.println(e);
//                System.err.println("Problem in Frame unique id generation assign it a unique random between 0 and 999999");
//                Random r = new Random();
//                
//                uniqueIntID = r.nextInt(999999);
//            }
        } 
          return uniqueIntID;
    }
    
   final public static Comparator<ParseFrame> PARSE_FRAME_LL_COMP = new Comparator<ParseFrame>() {
        @Override
        public int compare(ParseFrame t, ParseFrame t1) {
            return - Double.compare(t.likelihood, t1.likelihood);
        }
    };

    public void setLikelihood(double likelihood) {
        this.likelihood = likelihood;
    }

    public double getLikelihood() {
        return likelihood;
    }
    
    
    
    
    public synchronized Map<String, List<String>> getSemRoleAsMap() {
        if(rolesAsMap!=null){
            return rolesAsMap;
        }else{
        Map<String, List<String>> roles = new HashMap<>();
        for (SemRolePair srp : semanticRoles) {
            if (roles.containsKey(srp.getSemRoleID())) {
                roles.get(srp.getSemRoleID()).add(srp.getLexicalization());
            } else {
                List<String> roleLexList = new ArrayList<>();
                roleLexList.add(srp.getLexicalization());
                roles.put(srp.getSemRoleID(), roleLexList);
            }
        }
        rolesAsMap = roles;
        return roles;
        }
    }
    public synchronized Map<String, String> getRolePositionLabelMap() {
//        if(rolesAsMap!=null){
//            return rolesAsMap;
//        }else{
        Map<String, String> roles = new HashMap<>();
        for (SemRolePair srp : semanticRoles) {
            if (roles.containsKey(srp.getPositionInSentence())) {
                throw new RuntimeException("one position is tagged by more than one label code:637672");
                
            } else {
                
                roles.put(srp.getPositionInSentence(), srp.getSemRoleID());
            }
        }
        
        return roles;
       // }
    }
    
    public synchronized Map<String, SemRolePair> getRolePositionMap() {
//        if(rolesAsMap!=null){
//            return rolesAsMap;
//        }else{
        Map<String, SemRolePair> roles = new HashMap<>();
        for (SemRolePair srp : semanticRoles) {
            if (roles.containsKey(srp.getPositionInSentence())) {
                throw new RuntimeException("one position is tagged by more than one label code:637672");
                
            } else {
                
                roles.put(srp.getPositionInSentence(), srp);
            }
        }
        
        return roles;
       // }
    }
    
    public ParseFrame(String identifier) {
        this.frameSenDocIdentifier = identifier;
        this.semanticRoles = new ArrayList<>();
    }

    public List<SemRolePair> getSemanticRoles() {
        return semanticRoles;
    }
    
    

    public void setHeadPositionInSentence(String headPositionInSentence) {
        this.headPositionInSentence = headPositionInSentence;
    }

//    public void setFrameIdentifier(String frameSenDocIdentifier) {
//        this.frameSenDocIdentifier = frameSenDocIdentifier;
//    }

    public String getFrameSentenceIdentifier() {
        return frameSenDocIdentifier;
    }


/**
 * The string head key
 * @return 
 */    
    public String SentHeadPositionKey(){
        return frameSenDocIdentifier+" "+this.headPositionInSentence;
    }
    
    public String getHeadPositionInSentence() {
        return headPositionInSentence;
    }

    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public void setHead(String head) {
        this.head = head;
    }

   // @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setSemanticRoles(List<SemRolePair> semanticRoles) {
        this.semanticRoles = semanticRoles;
    }

    public void addSemanticRoles(SemRolePair semanticRole) {
        this.semanticRoles.add(semanticRole);
    }

    public int getFrameClusterNumber() {
        
        int i = (int) frameNumber;
        if(frameNumber - i!=0){
            System.err.println(frameNumber +"   " + i);
        }
        return i;
    }

    public String getNumberedHead() {
        return this.head + "." + this.frameNumber;
    }

    public String getHead() {
        return head;
    }
    

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (SemRolePair srp : semanticRoles) {
            sb.append(" ").append(srp.getPositionInSentence()).append(":").append(srp.toString());
        }
        return this.head + "." + this.frameNumber + " " + this.headPositionInSentence + sb.toString();
    }

    public String toStringStd() {
        StringBuilder sb = new StringBuilder();
        for (SemRolePair srp : semanticRoles) {
            sb.append(" ").append(srp.getLexicalization()).append("-:-").append(srp.getPositionInSentence()).append("-:-").append(srp.getSemRoleID());
        }
        return this.headPositionInSentence + " " + this.head + "." + this.frameNumber + sb.toString();
    }
   
    public String toStringStdEvaluation() {
        return frameSenDocIdentifier + " " + this.toStringStd();
    }
 public String toStringStdEvaluationRemoveArgs() {
        return frameSenDocIdentifier + " "+ this.headPositionInSentence + " " + this.head + "." + this.frameNumber;
    }
    public static ParseFrame parseFrameFromLine(String lineStd) {
        return parseFrameFromLine(lineStd,0,false);
    }


    public static ParseFrame parseFrameFromLine(String lineStd, double likelihood, boolean cluterlabelAsHead) {
        String[] split = lineStd.split(" ");
        ParseFrame pf = new ParseFrame(split[0]);
        //int positionHeadInSent = Integer.parseInt(split[1]);
        pf.setHeadPositionInSentence(split[1]);//positionHeadInSent+"");
        //#20728009 5 select.1584 I-:-3-:-R_46^n stock-:-7-:-R_31^n return-:-12-:-R_4^n
        String[] headClusterID = split[2].split("\\.");
        if (cluterlabelAsHead) {
            int headIDInt = headClusterID[0].hashCode();
            pf.setFrameNumber(headIDInt);
            pf.setHead(headClusterID[1]);
        } else {
            int headIDInt = Integer.parseInt(headClusterID[1]);
            pf.setFrameNumber(headIDInt);

            pf.setHead(headClusterID[0]);
        }
        
        List<SemRolePair> sr = new ArrayList<>();
        
        for (int i = 3; i < split.length; i++) {
            String[] roleBits = split[i].split("-:-");
           SemRolePair r = new SemRolePair(roleBits[2], roleBits[0], roleBits[1]);// Integer.parseInt(roleBits[1]));
            sr.add(r);
        }
        pf.setSemanticRoles(sr);
        pf.setLikelihood(likelihood);
        return pf;
    }
    
   public static ParseFrame parseFrameFromLineForGold(String lineStd, int clusterID) {
       String[] split = lineStd.split(" ");
       ParseFrame pf = new ParseFrame(split[0]);
       //int positionHeadInSent = Integer.parseInt(split[1]);
       pf.setHeadPositionInSentence(split[1]);//positionHeadInSent);
       //#20728009 5 select.1584 I-:-3-:-R_46^n stock-:-7-:-R_31^n return-:-12-:-R_4^n
       String[] headClusterID = split[2].split("\\.");

       int headIDInt = clusterID;
       pf.setFrameNumber(headIDInt);
       pf.setHead(headClusterID[0]);


        List<SemRolePair> sr = new ArrayList<>();
        
        for (int i = 3; i < split.length; i++) {
            String[] roleBits = split[i].split("-:-");
           SemRolePair r = new SemRolePair(roleBits[2], roleBits[0], roleBits[1]);//Integer.parseInt(roleBits[1]));
            sr.add(r);
        }
        pf.setSemanticRoles(sr);
       
        return pf;
    }
   
   /**
    * Used for reading gold data, the head is replaced with the fragment type
    * @param lineStd
    * @param clusterID
    * @return 
    */
     public static ParseFrame parseFrameFromLineForGolddataLabels(String lineStd, int clusterID) {
       String[] split = lineStd.split(" ");
       ParseFrame pf = new ParseFrame(split[0]);
      // int positionHeadInSent = Integer.parseInt(split[1]);
       pf.setHeadPositionInSentence(split[1]);//positionHeadInSent);
       //#20728009 5 select.1584 I-:-3-:-R_46^n stock-:-7-:-R_31^n return-:-12-:-R_4^n
       String[] headClusterID = split[2].split("\\.");

       int headIDInt = clusterID;
       pf.setFrameNumber(headIDInt);
       pf.setHead(headClusterID[1]);


        List<SemRolePair> sr = new ArrayList<>();
        
        for (int i = 3; i < split.length; i++) {
            String[] roleBits = split[i].split("-:-");
           SemRolePair r = new SemRolePair(roleBits[2], roleBits[0], roleBits[1]);//Integer.parseInt(roleBits[1]));
            sr.add(r);
        }
        pf.setSemanticRoles(sr);
       
        return pf;
    }
    public static Comparator<ParseFrame> cmpByHeadPosition = new Comparator<ParseFrame>() {

        @Override
        public int compare(ParseFrame o1, ParseFrame o2) {
            
            return Integer.compare(Integer.parseInt(o1.headPositionInSentence), Integer.parseInt(o2.headPositionInSentence));
        }
    };
    
  public static ParseFrame parseGoldFromLine(String lineStd, double likelihood, boolean cluterlabelAsHead) {
        String[] split = lineStd.split(" ");
        ParseFrame pf = new ParseFrame(split[0]);
       // int positionHeadInSent = Integer.parseInt(split[1]);
        pf.setHeadPositionInSentence(split[1]);//positionHeadInSent);
        //#20728009 5 select.1584 I-:-3-:-R_46^n stock-:-7-:-R_31^n return-:-12-:-R_4^n
        String[] headClusterID = split[2].split("\\.");
        if (cluterlabelAsHead) {
            int headIDInt = headClusterID[0].hashCode();
            pf.setFrameNumber(headIDInt);

            pf.setHead(headClusterID[1]);
        } else {
            int headIDInt = Integer.parseInt(headClusterID[1]);
            pf.setFrameNumber(headIDInt);

            pf.setHead(headClusterID[0]);
        }
        
        List<SemRolePair> sr = new ArrayList<>();
        
        for (int i = 3; i < split.length; i++) {
            String[] roleBits = split[i].split("-:-");
           SemRolePair r = new SemRolePair(roleBits[2], roleBits[0], roleBits[1]);//Integer.parseInt(roleBits[1]));
            sr.add(r);
        }
        pf.setSemanticRoles(sr);
        pf.setLikelihood(likelihood);
        return pf;
    }

}
