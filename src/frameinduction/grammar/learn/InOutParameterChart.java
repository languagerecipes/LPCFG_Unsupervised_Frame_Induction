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
package frameinduction.grammar.learn;

//import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import mhutil.HelperParseChartIO;

/**
 * a 3dimensional matrix: first is a non-terminal
 * this class is never accessed from different threads --- I can remove all concurrent maps
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class InOutParameterChart {

// I guess it is better to buy the space complexity in favour of faster access to the chart by implementing this whole thing using double[] or float[]
// the value of the map is alpha_A-i-j = P(A => w_i ... w_j | A)

    private static final Logger LOGGER = Logger.getLogger( InOutParameterChart.class.getName() );
    //private final int startSymbolID; // used only and only for test and verification!
    private Map<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, IOParam>>> ioMat;
//    static boolean verbose = true;
//

    public InOutParameterChart(int startSymbolID) {
        ioMat = new ConcurrentHashMap<>();
       // this.startSymbolID = startSymbolID;
    }
    
    public Set<Integer> getRowsIndices(){
        Set<Integer> keySet = ioMat.keySet();
        return Collections.unmodifiableSet(keySet);
    }
    
    public Set<Integer> getColoummnsIndicesForRow(int row){
        Set<Integer> keySet = ioMat.get(row).keySet();
        return Collections.unmodifiableSet(keySet);
    }

    public Map<Integer, IOParam> getSentenceSymbolIOStatSumSummary() {
        Map<Integer, IOParam> mapOfInsidesSums = new ConcurrentHashMap<>();
        /// !!!!!! becareful with the  the race condition that can happen here
        Collection<ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, IOParam>>> valuesAtThisRow = ioMat.values();
        for (ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, IOParam>> eachRow : valuesAtThisRow) {
            Collection<ConcurrentHashMap<Integer, IOParam>> mapOfAssginedInsidesSymbols = eachRow.values();

            for (Map<Integer, IOParam> mapOfInsideOutsides : mapOfAssginedInsidesSymbols) {
                for (Entry<Integer, IOParam> symbolInsideOutsideVal : mapOfInsideOutsides.entrySet()) {
                    
                    Integer symbol = symbolInsideOutsideVal.getKey();
                    IOParam value = symbolInsideOutsideVal.getValue();
                    if (mapOfInsidesSums.containsKey(symbol)) {
                        IOParam get = mapOfInsidesSums.get(symbol);
                        get.addThisParamForMerge(value);
                        get.incFrequency(value.getFrequency());
                    } else {
                        IOParam get = new IOParam(value);
                        IOParam putIfAbsent = mapOfInsidesSums.putIfAbsent(symbol, get);
                        // I hope this is ok! :-/
                        if (putIfAbsent != null) {
                            putIfAbsent.addThisParamForMerge(get);
                            putIfAbsent.incFrequency(get.getFrequency());
                        }
                    }
                }

            }
        }
        return mapOfInsidesSums;

    }


    
    private Map<Integer, IOParam> getCell(int start, int end) {
     
        if (ioMat.containsKey(start)) {
            ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, IOParam>> cols = ioMat.get(start);
            if (cols.containsKey(end)) {
                Map<Integer, IOParam> symbolIOWeight = cols.get(end);
                return symbolIOWeight;
            }
        }
        return null;
    }

    /**
     * To keep it logically consistent, we go throuugh adding the whole process
     *
     * @param start
     * @param end
     * @param symbol
     * @param valueToInc
     */
    public synchronized void incInsideParam(int start, int end, int symbol, double valueToInc) {
        
        if (ioMat.containsKey(start)) {
            Map<Integer, ConcurrentHashMap<Integer, IOParam>> endSymbolMap = ioMat.get(start);
            if (endSymbolMap.containsKey(end)) {
                ConcurrentHashMap<Integer, IOParam> rulesAtThisPosition = endSymbolMap.get(end);
                if (rulesAtThisPosition.containsKey(symbol)) {
                    IOParam weightValuePrIO = rulesAtThisPosition.get(symbol);
                    weightValuePrIO.incInside(valueToInc);
                    //    rulesAtThisPosition.put(symbol, weightValuePrIO);
                } else {
                    IOParam iop = new IOParam();
                    iop.incInside(valueToInc);
                    IOParam absent = rulesAtThisPosition.putIfAbsent(symbol, iop);
                    if (absent != null) {
                        absent.addThisParamForMerge(iop);
                    }
                }
            } else {
                ConcurrentHashMap<Integer, IOParam> symbolAtPos = new ConcurrentHashMap<>();
                IOParam iop = new IOParam();
                iop.incInside(valueToInc);
                IOParam putIfAbsent = symbolAtPos.putIfAbsent(symbol, iop);
                if (putIfAbsent != null) {
                    putIfAbsent.addThisParamForMerge(iop);
                }
                ConcurrentHashMap<Integer, IOParam> putIfAbsInnerMap = endSymbolMap.putIfAbsent(end, symbolAtPos);
                if (putIfAbsInnerMap != null) {
                    LOGGER.log(Level.SEVERE, "never tested before for Error code:34782fj");
                    symbolAtPos.entrySet().forEach(entry -> {
                        IOParam putIfAbsent2 = putIfAbsInnerMap.putIfAbsent(entry.getKey(), entry.getValue());
                        if (putIfAbsent2 != null) {
                            putIfAbsent2.incInside(entry.getValue().getInside());
                        }

                    });
                }

            }
        } else {
            ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, IOParam>> columns = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer, IOParam> symbolsAtthisPosition = new ConcurrentHashMap<>();
            IOParam iop = new IOParam();
            iop.incInside(valueToInc);
            IOParam ioParamRacing = symbolsAtthisPosition.putIfAbsent(symbol, iop);
            if(ioParamRacing!=null){
                ioParamRacing.incInside(valueToInc);
            }
            
            ConcurrentHashMap<Integer, IOParam> colMapAbsentRace = columns.putIfAbsent(end, symbolsAtthisPosition);
            if(colMapAbsentRace!=null){
                LOGGER.log(Level.SEVERE, "never tested before for Error code:473y2");
                symbolsAtthisPosition.entrySet().forEach(entry->{
                    IOParam putIfAbsent = colMapAbsentRace.putIfAbsent(entry.getKey(), entry.getValue());
                    if(putIfAbsent!=null){
                        putIfAbsent.incInside(entry.getValue().getInside());
                    }
                    
                });
            
            }
            ioMat.put(start, columns);
        }
       
    }

    /**
     * To speed up process this is added by breaking up the former method
     * incInsideParam it did not work out well due to the sparsity of data cells
     *
     * @param rulesAtThisPosition
     * @param symbol
     * @param valueToInc
     */
    public void updateThisInsideMap(Map<Integer, IOParam> rulesAtThisPosition, int symbol, double valueToInc) {
        if (rulesAtThisPosition.containsKey(symbol)) {
            IOParam weightValuePrIO = rulesAtThisPosition.get(symbol);
            weightValuePrIO.incInside(valueToInc);
        } else {
            IOParam iop = new IOParam();
            iop.incInside(valueToInc);
            rulesAtThisPosition.put(symbol, iop);
        }
    }

    /**
     *
     * @param rulesAtThisPosition
     * @param symbol
     * @param valueToInc
     */
    public void updateThisOutsideMap(Map<Integer, IOParam> rulesAtThisPosition, int symbol, double valueToInc) {
        if (rulesAtThisPosition.containsKey(symbol)) {
            IOParam weightValuePrIO = rulesAtThisPosition.get(symbol);
            weightValuePrIO.incOutside(valueToInc);
        } else {
            IOParam iop = new IOParam();
            iop.incOutside(valueToInc);
            rulesAtThisPosition.put(symbol, iop);
        }
    }

    public Map<Integer, IOParam> getParamMapAt(int start, int end) {
        if (ioMat.containsKey(start)) {
            ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, IOParam>> endSymbolMap = ioMat.get(start);
            if (endSymbolMap.containsKey(end)) {
                Map<Integer, IOParam> rulesAtThisPosition = endSymbolMap.get(end);
                return rulesAtThisPosition;
            } else {
                ConcurrentHashMap<Integer, IOParam> rulesAtThisPosition = new ConcurrentHashMap<>();
                endSymbolMap.put(end, rulesAtThisPosition);
                return rulesAtThisPosition;
            }
        } else {
            ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, IOParam>> columns = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer, IOParam> symbolsAtthisPosition = new ConcurrentHashMap<>();
            columns.put(end, symbolsAtthisPosition);
            ioMat.put(start, columns);
            return symbolsAtthisPosition;
        }
    }

    public void incOutsideParam(int start, int end, int symbol, double valueToInc) {
       
        if (ioMat.containsKey(start)) {
            ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, IOParam>> columns = ioMat.get(start);
            if (columns.containsKey(end)) {
                Map<Integer, IOParam> rulesAtThisPosition = columns.get(end);
                if (rulesAtThisPosition.containsKey(symbol)) { //this is not required as there must be an io for this
                    IOParam weightValuePrIO = rulesAtThisPosition.get(symbol);
                    weightValuePrIO.incOutside(valueToInc);
                    rulesAtThisPosition.put(symbol, weightValuePrIO);
                } else {
                    IOParam iop = new IOParam();
                    iop.incOutside(valueToInc);
                    rulesAtThisPosition.put(symbol, iop);
                }
            } else {

                ConcurrentHashMap<Integer, IOParam> symbolAtPos = new ConcurrentHashMap<>();
                IOParam iop = new IOParam();

                iop.incOutside(valueToInc);
                symbolAtPos.put(symbol, iop);
                columns.put(end, symbolAtPos);
            }
        } else {
            ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, IOParam>> columns = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer, IOParam> symbolsAtthisPosition = new ConcurrentHashMap<>();
            IOParam iop = new IOParam();
            iop.incOutside(valueToInc);
            symbolsAtthisPosition.put(symbol, iop);
            columns.put(end, symbolsAtthisPosition);
            ioMat.put(start, columns);
        }
    }

    public Double getInsideValueFor(int start, int end, int symbol) throws Exception {
        Map<Integer, IOParam> cell = getCell(start, end);
        if (cell != null) {
            if (cell.containsKey(symbol)) {
                IOParam iop = cell.get(symbol);
                return iop.getInside();
            }
        }
//        if (symbol == startSymbolID) {
//            LOGGER.log(Level.WARNING, "* WARNING :  no parse for input -- code 38768");
//        }
        return null;
    }

    public Double getOutsideValueFor(int start, int end, int symbol) throws Exception {

        Map<Integer, IOParam> cell = getCell(start, end);
        if (cell != null) {
            if (cell.containsKey(symbol)) {
                IOParam iop = cell.get(symbol);
                return iop.getOutside();
            } else {
                return -100000.; // i.e., an approximate for log(0)
            }
        } else {
            return -100000.;

        }
    }
}
