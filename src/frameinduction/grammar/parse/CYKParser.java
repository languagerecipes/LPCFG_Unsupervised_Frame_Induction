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
import input.preprocess.objects.FragmentCompact;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CYKParser implements PCFGParser {

    public ParseChart parse(FragmentCompact fc, RuleMaps rm) throws Exception {
        int sentLength = fc.getTerminals().length;
        ParseChart pc = new ParseChart(sentLength);
        for (int k = 1; k <= sentLength; k++) {
            int rhsSymbol = fc.getTerminals()[k - 1];
            String positionInSent = fc.getPositionsInSentence()[k - 1];
            Map<Integer, Double> lhsToUnary = rm.getLhsToUnary(rhsSymbol);
            Set<Map.Entry<Integer, Double>> iteratorUnary = lhsToUnary.entrySet();
            for (Map.Entry<Integer, Double> e : iteratorUnary) {
                int lhsSymbol = e.getKey();
                double param = e.getValue();
                CNFRule cnf = new CNFRule(lhsSymbol, rhsSymbol, k - 1, k, param);
                cnf.setPositionInSent(positionInSent);
                pc.addValue(k - 1, k, cnf);

            }
        }
        for (int width = 2; width <= sentLength; width++) {
            for (int i = 0; i <= sentLength - width; i++) {
                int k = i + width;
                for (int j = i + 1; j <= k - 1; j++) {
                    Map<Integer, Map<Integer, Map<Integer, Double>>> reverseBinaryRuleMap = rm.getReverseBinaryRuleMap();
                    Set<Map.Entry<Integer, Map<Integer, Map<Integer, Double>>>> iteratorLhs = reverseBinaryRuleMap.entrySet();
                    for (Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> e : iteratorLhs) {
                        int rhsB1 = e.getKey();
                        Set<Map.Entry<Integer, Map<Integer, Double>>> iteratorRHSBC = e.getValue().entrySet();
                        for (Map.Entry<Integer, Map<Integer, Double>> e1 : iteratorRHSBC) {
                            int rhsC2 = e1.getKey();
                            Map<Integer, Double> lhsAndParamvalue = e1.getValue();
                            Set<Map.Entry<Integer, Double>> iteratorLHSParam = lhsAndParamvalue.entrySet();
                            for (Map.Entry<Integer, Double> e2 : iteratorLHSParam) {
                                int lhsA = e2.getKey();
                                double lhsToRHS1RhS2Param = e2.getValue();
                                //List<CNFRule> cellLeft = pc.getCell(i, j, rhsB1);
                                List<CNFRule> cellLeft = pc.getCell(i, j);
                                if (cellLeft != null) {
                                    //List<CNFRule> cellRight = pc.getCell(j, k, rhsC2);
                                    List<CNFRule> cellRight = pc.getCell(j, k);
                                    if (cellRight != null) {
                                        for (CNFRule cnfleft : cellLeft) {
                                            if (cnfleft.getSymbolLhs() == rhsB1) {
                                                for (CNFRule cnfright : cellRight) {
                                                    if (cnfright.getSymbolLhs() == rhsC2) {
                                                        double thisProb = lhsToRHS1RhS2Param
                                                                + cnfleft.getProb() 
                                                                + cnfright.getProb();
                                                        //if (thisProb > 0) {
                                                        CNFRule cnfForThis = new CNFRule(lhsA, cnfleft, cnfright, thisProb);
                                                        pc.addValue(i, k, cnfForThis);
                                                        //}
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }

                            }
                        }

                    }

                }

            }

        }

        return pc;
//        this.ZParam = ioChart.getInsideValueFor(0, sentLength, this.rm.getStartSymbolID());
//        //System.out.println("Z param " + ZParam);
//        
//        if (ZParam == 0.0) {
//            System.out.println(Arrays.toString(this.fc.getTerminals()));
//        }
    }

}
