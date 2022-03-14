package com.ibm.cta.Recorder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * @author Rahul Krishna <i.m.ralk@gmail.com>
 */
public final class Recorder {
    public static void recordCall(String caller, String callee) {
        // Save the edge information in a csv
        // System time in millis(), caller, callee
        Date timeNow = new Date(System.currentTimeMillis());
        String callTrace = String.format("%-40s\t%-60s\t%-60s", timeNow.toString(), caller, callee);
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("CallTrace.csv", true)))) {
            out.println(callTrace);
        } catch (IOException ex) {
            System.err.println(ex);
            System.exit(-1);
        }
    }
}
