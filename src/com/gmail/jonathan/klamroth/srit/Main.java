package com.gmail.jonathan.klamroth.srit;

import java.io.File;


/**
 * Created by jonny on 24.06.16.
 */
public class Main {

    public static void main (String[] args) {
        String projectDir;
        String lang;

        if (args.length == 1) {
            projectDir = args[0];
            lang = "";
        } else if (args.length == 2) {
            projectDir = args[0];
            lang = args[1];
        } else {
            Util.exitWithError("args -> project dir[, lang]");
            return;
        }


        File projectDirFile = new File(projectDir);
        if (!projectDirFile.exists())
            Util.exitWithError("project directory not found");


        SmaliResIdTool srit  = new SmaliResIdTool(projectDirFile, lang);
        srit.run();
    }

}
