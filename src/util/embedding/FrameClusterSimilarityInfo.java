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
package util.embedding;

import frameinduction.grammar.learn.MathUtil;
import java.text.DecimalFormat;
import java.util.Comparator;

/**
 *
 * @author Behrang QasemiZadeh <me at atmykitchen.info>
 */
public class FrameClusterSimilarityInfo implements Comparable<FrameClusterSimilarityInfo>{
    int frameClusterID1;
    int frameClusterID2;
    int sizeCluster1;
    int sizeCluster2;
    double similarityOveral;
    double similarityHead;
    double[] similarityRoles;
    double llCluster1;
    double llCluster2;

    
  public FrameClusterSimilarityInfo(int frameClusterID1, int frameClusterID2, int sizeCluster1, int sizeCluster2, double similarityOveral) {
        this.frameClusterID1 = frameClusterID1;
        this.frameClusterID2 = frameClusterID2;
        this.sizeCluster1 = sizeCluster1;
        this.sizeCluster2 = sizeCluster2;
        this.similarityOveral = similarityOveral;
        
    }
  
  public FrameClusterSimilarityInfo(int frameClusterID1, int frameClusterID2, int sizeCluster1, int sizeCluster2, 
          double similarityOveral,double loglikelihoodCluster1, double loglikelihoodCluster2
  
  ) {
        this.frameClusterID1 = frameClusterID1;
        this.frameClusterID2 = frameClusterID2;
        this.sizeCluster1 = sizeCluster1;
        this.sizeCluster2 = sizeCluster2;
        this.similarityOveral = similarityOveral;
          this.llCluster1=loglikelihoodCluster1;
        this.llCluster2=loglikelihoodCluster2;
        
    }
    
  public FrameClusterSimilarityInfo
        (int frameClusterID1,
            int frameClusterID2, 
            int sizeCluster1, int sizeCluster2, 
            double similarityOveral,
            double similarityHead, 
            double loglikelihoodCluster1, double loglikelihoodCluster2) {
        this.frameClusterID1 = frameClusterID1;
        this.frameClusterID2 = frameClusterID2;
        this.sizeCluster1 = sizeCluster1;
        this.sizeCluster2 = sizeCluster2;
        this.similarityOveral = similarityOveral;
        this.similarityHead = similarityHead;
       // this.similarityRoles = similarityRoles;
        this.llCluster1=loglikelihoodCluster1;
        this.llCluster2=loglikelihoodCluster2;
    }
  
    public FrameClusterSimilarityInfo(int frameClusterID1,
            int frameClusterID2, 
            int sizeCluster1, int sizeCluster2, 
            double similarityOveral,
            double similarityHead, 
            double[] similarityRoles, double loglikelihoodCluster1, double loglikelihoodCluster2) {
        this.frameClusterID1 = frameClusterID1;
        this.frameClusterID2 = frameClusterID2;
        this.sizeCluster1 = sizeCluster1;
        this.sizeCluster2 = sizeCluster2;
        this.similarityOveral = similarityOveral;
        this.similarityHead = similarityHead;
        this.similarityRoles = similarityRoles;
        this.llCluster1=loglikelihoodCluster1;
        this.llCluster2=loglikelihoodCluster2;
    }

    public int getFrameClusterID1() {
        return frameClusterID1;
    }

    /**
     * the log-likelihood for cluster 1
     * @return 
     */
    public double getLLCluster1() {
        return llCluster1;
    }
    
    /**
     * the log-likelihood for cluster 2
     * @return 
     */
     public double getLLCluster2() {
        return llCluster2;
    }
    

    public int getFrameClusterID2() {
        return frameClusterID2;
    }

    public double getSimilarityHead() {
        return similarityHead;
    }

    public double getSimilarityOveral() {
        return similarityOveral;
    }

    public double[] getSimilarityRoles() {
        return similarityRoles;
    }

    public int getSizeCluster1() {
        return sizeCluster1;
    }

    public int getSizeCluster2() {
        return sizeCluster2;
    }

    @Override
    public int compareTo(FrameClusterSimilarityInfo t) {
        return Double.compare(this.similarityOveral, t.similarityOveral);
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat(".0000000000");
        return this.frameClusterID1 + " " + this.frameClusterID2 + " " + df.format(this.similarityOveral) + " "
                + similarityOveral +" "
              //  + df.format(similarityHead) + " "
              //  + df.format(similarityRoles) + " "
                + sizeCluster1 + " "
                + sizeCluster2 +" " + df.format(llCluster1) +" "+df.format(llCluster2) ;
    }
     
    /**
     *
     */
    public static final Comparator<FrameClusterSimilarityInfo> COMP_BY_SIZE_INFO1 = new Comparator<FrameClusterSimilarityInfo>() {

        @Override
        public int compare(FrameClusterSimilarityInfo o1, FrameClusterSimilarityInfo o2) {
        return - Double.compare(
                Math.log(Math.log(o1.sizeCluster1 + o1.sizeCluster2) )*
                        o1.similarityOveral,
                Math.log(Math.log(o2.sizeCluster1 + o2.sizeCluster2 ))*
                        o2.similarityOveral
        );
        }
    };
    /**
     *
     */
    public static final Comparator<FrameClusterSimilarityInfo> COMP_BY_SIZE_INFO = new Comparator<FrameClusterSimilarityInfo>() {

        @Override
        public int compare(FrameClusterSimilarityInfo o1, FrameClusterSimilarityInfo o2) {
        return - Double.compare(
                Math.log(o1.sizeCluster1 + o1.sizeCluster2 )*
                        o1.similarityOveral,
                Math.log(o2.sizeCluster1 + o2.sizeCluster2 )*
                        o2.similarityOveral
        );
        }
    };
    public static final Comparator<FrameClusterSimilarityInfo> COMP_BY_SIZE_INFOAA = new Comparator<FrameClusterSimilarityInfo>() {

        @Override
        public int compare(FrameClusterSimilarityInfo o1, FrameClusterSimilarityInfo o2) {
        return - Double.compare(
               getScorForCompInf2(o1),
                getScorForCompInf2(o2)
        );
        }
    };
    
    
    public double getSqushedLLDiff(){
        double diff1 = Math.abs(this.llCluster2
                ///this.sizeCluster2 
                - 
                this.llCluster1
                  //      /this.sizeCluster1
        );
        return squash(diff1);
    }
      
     public static final Comparator<FrameClusterSimilarityInfo> COMP_BY_LL_AND_SIM = new Comparator<FrameClusterSimilarityInfo>() {

        @Override
        public int compare(FrameClusterSimilarityInfo o1, FrameClusterSimilarityInfo o2) {
            
            
//            double diff1 = Math.abs(o1.llCluster2/o1.sizeCluster2 - o1.llCluster1/o1.sizeCluster1);
//           
//            double diff2 = Math.abs(o2.llCluster2/o2.sizeCluster2 - o2.llCluster1/o2.sizeCluster1);
            
//            double sumInverse1 = 1/Math.abs(o1.llCluster2+o1.llCluster1);
//            double sumInverse2 = 1/Math.abs(o2.llCluster2+o2.llCluster1);
            
            
        return  
                
//                -Double.compare(
//               getScorForCompInfSt(o1),
//                getScorForCompInfSt(o2)
//        )
                
                -Double.compare(
              o1.getSqushedLLDiff() * o1.similarityOveral,
                o2.getSqushedLLDiff()* o2.similarityOveral
        )

//                
                ;
        }
    };
     
     private static double squash(double input){
         return 1.0/(1.0+Math.exp(-input));
     }
    
     public static final Comparator<FrameClusterSimilarityInfo> COMP_BY_SIZE_INFOPasComplex = new Comparator<FrameClusterSimilarityInfo>() {

        @Override
        public int compare(FrameClusterSimilarityInfo o1, FrameClusterSimilarityInfo o2) {
        return  
                
//                -Double.compare(
//               getScorForCompInfSt(o1),
//                getScorForCompInfSt(o2)
//        )
                -Double.compare(
              2000* Math.exp(o1.llCluster2+o1.llCluster1)+ o1.similarityOveral,
                2000* Math.exp( o2.llCluster2+o2.llCluster1)+ o2.similarityOveral
        )
//                  -Double.compare(
//              Math.exp(o1.llCluster2)*Math.exp(o1.llCluster1)* o1.similarityOveral* o1.similarityOveral+o1.similarityOveral,
//                 Math.exp(o2.llCluster2)*Math.exp(o2.llCluster1)* o2.similarityOveral* o2.similarityOveral+o2.similarityOveral
//        )
                 
                
//                * Double.compare(
//               o1.similarityOveral,
//                o2.similarityOveral
//        )
//                
                ;
        }
    };
     
     public static final Comparator<FrameClusterSimilarityInfo> COMP_BY_SIZE_INFOPasComplex2 = new Comparator<FrameClusterSimilarityInfo>() {

        @Override
        public int compare(FrameClusterSimilarityInfo o1, FrameClusterSimilarityInfo o2) {
            
            return -Double.compare(
                    o1.llCluster1 * o1.llCluster2,
                    o2.llCluster1 * o2.llCluster2
            );
        }
    };

     public static final Comparator<FrameClusterSimilarityInfo> COMP_BY_SIZE_INFOPasComplex4 = new Comparator<FrameClusterSimilarityInfo>() {

        @Override
        public int compare(FrameClusterSimilarityInfo o1, FrameClusterSimilarityInfo o2) {
            
            return -Double.compare(
                   Math.abs(o1.llCluster1- o1.llCluster2),
                   Math.abs( o2.llCluster1 - o2.llCluster2)
            );
        }
    };
     
     public static final Comparator<FrameClusterSimilarityInfo> COMP_BY_SIZE_INFOPasComplex5 = new Comparator<FrameClusterSimilarityInfo>() {

        @Override
        public int compare(FrameClusterSimilarityInfo o1, FrameClusterSimilarityInfo o2) {
            
            return -Double.compare(
                  (MathUtil.logSumExp(o1.llCluster1-o1.sizeCluster1, o1.llCluster2-o1.sizeCluster2))*o1.similarityOveral,
                 (MathUtil.logSumExp(o2.llCluster1-o2.sizeCluster1, o2.llCluster2-o2.sizeCluster2)*o2.similarityOveral)
            );
        }
    };
     
       public static final Comparator<FrameClusterSimilarityInfo> COMP_BY_SIZE_INFOPasComplex6 = new Comparator<FrameClusterSimilarityInfo>() {

        @Override
        public int compare(FrameClusterSimilarityInfo o1, FrameClusterSimilarityInfo o2) {
           // System.err.println(o1);
           // System.err.println(o2);
            return -Double.compare(
                  Math.max(Math.exp(o1.llCluster1)/o1.sizeCluster1, Math.exp(o1.llCluster2)/o1.sizeCluster2)*o1.similarityOveral,
                 Math.max(Math.exp(o2.llCluster1)/o2.sizeCluster1, Math.exp(o2.llCluster2)/o2.sizeCluster2)*o2.similarityOveral
            );
        }
    };

      public static final Comparator<FrameClusterSimilarityInfo> COMP_BY_SIZE_INFOPasComplex3 = new Comparator<FrameClusterSimilarityInfo>() {

        @Override
        public int compare(FrameClusterSimilarityInfo o1, FrameClusterSimilarityInfo o2) {
        return - Double.compare(
               o1.llCluster1*o1.sizeCluster1+o1.llCluster2*o1.sizeCluster2,
                o2.llCluster1*o2.sizeCluster1+o2.llCluster2*o2.sizeCluster2
        );
        }
    };
     
     
     
    
    private static double getScorForCompInf3(FrameClusterSimilarityInfo c){
       double score =  ( Math.log(c.sizeCluster1+1) + Math.log(c.sizeCluster2+1 ))*
                        c.similarityOveral +  c.similarityOveral; 
       return score;
    }
    
     private static double getScorForCompInf5(FrameClusterSimilarityInfo c){
       double score =  Math.log(Math.log(Math.min(c.sizeCluster1,c.sizeCluster2)))
                      *  c.similarityOveral; 
       return score;
    }
     private static double getScorForCompInf2(FrameClusterSimilarityInfo c){
       double score =  Math.log
                    (2*(c.sizeCluster1*c.sizeCluster2)/(c.sizeCluster1+c.sizeCluster2))
                      *  c.similarityOveral; 
       return score;
    }
     
       private static double getScorForCompInfSt(FrameClusterSimilarityInfo c){
          // System.err.println(c.llCluster1*c.llCluster2);
//       double score = // Math.log
//                    (2*(c.llCluster1*c.llCluster2
//                           // *c.sizeCluster1*c.sizeCluster2
//                            )
//                            /(
//                                    c.llCluster1
//                                       //     *c.sizeCluster1
//                                            +
//                                            c.llCluster2
//                                           // *c.sizeCluster2
//                                    )
//                    )
//                   //   *  c.similarityOveral
//               ; 
//       
       double score2 =  
                    (c.llCluster1+c.llCluster2)
                           // *c.sizeCluster1*c.sizeCluster2
                            
                            /(
                                   MathUtil.logSumExp( c.llCluster1
                                       //     *c.sizeCluster1
                                            ,
                                            c.llCluster2
                                           // *c.sizeCluster2
                                    )
                    )
                     *  c.similarityOveral
               ; 
       return score2;
    }
}
