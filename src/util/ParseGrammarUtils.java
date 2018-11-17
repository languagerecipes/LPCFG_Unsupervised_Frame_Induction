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
package util;

import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.parse.CNFRule;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class ParseGrammarUtils {
    
    /**
     * 
     * @param cnf
     * @param rc
     * @throws Exception 
     */
    public static void getRulesInChain(CNFRule cnf, RulesCounts rc) throws Exception {
        if (cnf == null || cnf.isTerminal()) {
            return;
        }
        int symbolLhs = cnf.getSymbolLhs();
        CNFRule rhs1 = cnf.getRhs1();
        CNFRule rhs2 = cnf.getRhs2();
        if (rhs1 != null && rhs2 != null) {
            rc.incOWBinary(symbolLhs, rhs1.getSymbolLhs(), rhs2.getSymbolLhs(),cnf.getProb() );
        } else if (rhs1 != null && rhs2 == null) {

            // System.out.println(rm.getSymbolFromID(rhs1.getSymbolLhs()));
            rc.incOWUnary(symbolLhs, rhs1.getSymbolLhs(), cnf.getProb() );
        } else {
            throw new RuntimeException("WHAT ?! should not happen");
        }
        getRulesInChain(rhs1, rc);
        getRulesInChain(rhs2, rc);
    }
}
