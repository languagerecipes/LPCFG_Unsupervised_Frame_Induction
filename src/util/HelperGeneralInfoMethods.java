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

import java.lang.instrument.Instrumentation;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class HelperGeneralInfoMethods {
    private static final double BYTE_PER_MEGA =1048576;

    public static void getMemoryReport() {
        double heapSize = Runtime.getRuntime().totalMemory() / BYTE_PER_MEGA;
        double heapMaxSize = Runtime.getRuntime().maxMemory() / BYTE_PER_MEGA;
        double heapFreeSize = Runtime.getRuntime().freeMemory() / BYTE_PER_MEGA;
        System.out.println("===");
        System.out.println("HeapSize: MB " + heapSize);
        System.out.println("HeapMaxSize: MB " + heapMaxSize);
//        System.out.println("HeapFreeSize: MB " + heapFreeSize);
        System.out.println("---");
    }
  
    public static String getCurrentClassName(Object o) {

        String className = o.getClass().getName();
        return className;
    }
    
  
    

}
