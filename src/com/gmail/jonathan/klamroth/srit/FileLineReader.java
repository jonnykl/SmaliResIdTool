package com.gmail.jonathan.klamroth.srit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * Created by jonny on 24.06.16.
 */
public abstract class FileLineReader {

    public FileLineReader (File f) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));

            String line;
            while ((line = reader.readLine()) != null)
                lineRead(line);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public abstract void lineRead (String line);

}
