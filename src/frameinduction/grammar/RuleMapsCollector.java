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
package frameinduction.grammar;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class RuleMapsCollector {

   

    

    // at some stage change to immutable?!
    private Map<Integer, Set<Integer>> unaryRuleMap;
    private Map<Integer, Map<Integer, Set<Integer>>> binaryRuleMap;

    public RuleMapsCollector() {
      
        this.unaryRuleMap = new ConcurrentHashMap<>();
        this.binaryRuleMap = new ConcurrentHashMap<>();
        
    }

    public Map<Integer, Map<Integer, Set<Integer>>> getBinaryRuleMap() {
        return binaryRuleMap;
    }

    public Map<Integer, Set<Integer>> getUnaryRuleMap() {
        return unaryRuleMap;
    }
    
   public boolean containRule(int lhs, int rhs){
       if(this.unaryRuleMap.containsKey(lhs)){
         return  this.unaryRuleMap.get(lhs).contains(rhs);
       }else{
           return false;
       }
   }

    
     public boolean containRule(int lhs, int rhs, int rhs2){
       if(this.binaryRuleMap.containsKey(lhs)){
         if( this.binaryRuleMap.get(lhs).containsKey(rhs)){
             return binaryRuleMap.get(lhs).get(rhs).contains(rhs2);
         }else{
             return false;
         }
       }else{
           return false;
       }
   }
  

    public synchronized void addRule(int idLHS, int idRHS1, int idRHS2) throws Exception {
        if (binaryRuleMap.containsKey(idLHS)) {
            Map<Integer, Set<Integer>> rhsSet = binaryRuleMap.get(idLHS);
            if (rhsSet.containsKey(idRHS1)) {
                Set<Integer> rhs2Set = rhsSet.get(idRHS1);
                //if(rhs2Set.contains(idRHS2)){
               // }else{
                rhs2Set.add(idRHS2);
                //            countBinaryRules.addAndGet(1);
                //}
                
            } else {
                Set<Integer> rhs2Set = ConcurrentHashMap.newKeySet();
                rhs2Set.add(idRHS2);
                rhsSet.put(idRHS1, rhs2Set);
                //            countBinaryRules.addAndGet(1);
            }
        } else {
            Map<Integer, Set<Integer>> binrayRuleRHS = new ConcurrentHashMap<>();
            Set<Integer> rhs2Set = ConcurrentHashMap.newKeySet();
//            countBinaryRules.addAndGet(1);
            rhs2Set.add(idRHS2);
            binrayRuleRHS.put(idRHS1, rhs2Set);
            binaryRuleMap.put(idLHS, binrayRuleRHS);
        }

    }

    /**
     * Main method to add unary rules
     *
     * @param lhs
     * @param rhs1
     * @throws Exception
     */
    public synchronized void addRule(int idLHS, int idRHS1) throws Exception {
        
        if (unaryRuleMap.containsKey(idLHS)) {
            Set<Integer> rhsSet = unaryRuleMap.get(idLHS);
//            if (rhsSet.contains(idRHS1)) {
//                throw new Exception("A rule was asserted twice, an error in your grammar generation!");
//            } else {
               // countUnaryRules.incrementAndGet();
                rhsSet.add(idRHS1);
       //     }

        } else {
            Set<Integer> rhsSet = ConcurrentHashMap.newKeySet();
            rhsSet.add(idRHS1);
           // countUnaryRules.incrementAndGet();
            unaryRuleMap.put(idLHS, rhsSet);
        }
    }

  



}
