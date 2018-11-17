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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class IOParam {

    private DoubleAccumulator inside;
    private DoubleAccumulator outside;
    private AtomicInteger frequency;
    //        double inside;
    //        double outside;

    public IOParam() {
        frequency = new AtomicInteger(1);
        inside = new DoubleAccumulator((double x, double y) -> MathUtil.logSumExp(x, y), Double.NEGATIVE_INFINITY);
        outside = new DoubleAccumulator((double x, double y) -> MathUtil.logSumExp(x, y), Double.NEGATIVE_INFINITY);
        //             inside = new DoubleAccumulator((x,y)->(x+ y), 0.0);
        //             outside = new DoubleAccumulator((x,y)->(x+ y), 0.0);
    }

    public IOParam(IOParam ioParam) {
        frequency = new AtomicInteger(1);
        inside = new DoubleAccumulator((double x, double y) -> MathUtil.logSumExp(x, y), ioParam.getInside());
        outside = new DoubleAccumulator((double x, double y) -> MathUtil.logSumExp(x, y), ioParam.getOutside());
        //             inside = new DoubleAccumulator((x,y)->(x+ y), 0.0);
        //             outside = new DoubleAccumulator((x,y)->(x+ y), 0.0);
    }

    public IOParam(double inside, double outside) {
        //this.inside = new DoubleAccumulator((x,y)->MathUtil.logSumExp(x, y), 0.0);
        this();
        this.inside.accumulate(inside);
        // this.outside = new DoubleAdder();
        this.outside.accumulate(inside);
    }

    @Override
    public String toString() {
        return inside + " <-> " + outside;
    }

    public double getInside() {
        return inside.doubleValue();
    }

    public double getOutside() {
        return outside.doubleValue();
    }

    public void incInside(double insideToInc) {
        inside.accumulate(insideToInc);
    }

    public void incOutside(double insideToInc) {
        outside.accumulate(insideToInc);
    }

    public void incFrequency(int val) {
        this.frequency.addAndGet(val);
    }

    public int getFrequency() {
        return frequency.intValue();
    }

    
    
    /**
     * Used during merge
     *
     * @param io
     */
    public void addThisParamForMerge(IOParam io) {
        inside.accumulate(io.getInside());
        outside.accumulate(io.getOutside());
        
    }

}
