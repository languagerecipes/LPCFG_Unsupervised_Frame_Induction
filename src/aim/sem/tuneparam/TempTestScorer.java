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
package aim.sem.tuneparam;

import semeval.run.Task21;

/**
 *
 * @author Behrang Qasemizadeh (zadeh@phil.hhu.de)
 */
public class TempTestScorer {
    public static void main(String[] args) throws Exception {
        String[] ards = {"task-1.txt", "output.txt","-verbose"};
        Task21.main(ards);
        
    }
}