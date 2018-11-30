package com.gmail.jonathan.klamroth.srit;

/**
 * Created by jonny on 24.06.16.
 */
public class Util {

    public static void exitWithError (String msg) {
        System.out.println("error" + (msg != null ? ": " + msg : ""));
        System.exit(1);
    }

}
