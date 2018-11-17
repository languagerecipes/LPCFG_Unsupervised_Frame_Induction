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
package utils.io;

import input.preprocess.objects.Sentence;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import utils.io.ParsedFileReader;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
public class GetTextFromTreebanks {

    public static void main(String[] args) throws IOException, Exception {
        String path = "../lr/treebanks";
        File[] listFiles = new File(path).listFiles();
        Writer out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("../from-treebanks.txt"), "UTF-8"));
        for (File f : listFiles) {
            System.err.println(f);
            ParsedFileReader pfr = new ParsedFileReader(f.getAbsolutePath());
            Sentence s;
            while ((s = pfr.getNextSentence()) != null) {
                String wsjPSDID = s.getWsjPSDID();
                String toString = s.toString();
                out.append(wsjPSDID).append("\t").append(toString).append("\n");
            }
        }
        out.close();
    }
}
