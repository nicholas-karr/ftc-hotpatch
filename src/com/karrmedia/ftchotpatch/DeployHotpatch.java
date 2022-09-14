package com.karrmedia.ftchotpatch;

import android.annotation.SuppressLint;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class DeployHotpatch {
    @SuppressLint("SdCardPath")
    static String root = "/sdcard/FIRST/hotpatch";
    static String dexDir = /* args.get(0) + */ "/TeamCode/build/intermediates/dex/debug/mergeProjectDexDebug";
    static String[] adbCommand = new String[] { "", "-c", "adb" };

    public static void main(String args[])
    {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            adbCommand[0] = "cmd.exe";
            adbCommand[1] = "/C";
        }
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix") || os.contains("mac")) {
            adbCommand[0] = "/bin/bash";
        }

        // Path to folder containing FtcController project is passed as the 1st argument
        dexDir = args[0] + dexDir;

        try {
            // Create directories that hold classes if they don't exist
            createRemoteStructure();

            List<String> dexes = getDexesInDirectory(dexDir);
            pushFiles(dexes, root);

            // Trigger hotpatch by updating attributes of updateLock
            touch(root + "/updateLock");
        }
        catch (IOException e) {
            System.out.println("Failure during hotpatch: " + e.getMessage());
        }
    }

    // Execute adb command
    static String adb(String cmd) throws IOException {
        // Create process
        Process process = Runtime.getRuntime().exec(adbCommand[0] + " " + adbCommand[1] + " " + adbCommand[2] + " " + cmd);

        // Read stdout to a string
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ( (line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        String result = builder.toString();

        return result;
    }

    // Run a command on a device through adb
    static String remoteShell(String cmd) throws IOException {
        return adb("shell " + cmd);
    }

    // Update a pre-existing file's create and modify time to the current time on another device
    static void touch(String filePath) throws IOException {
        remoteShell("touch -c " + filePath);
    }

    static void createRemoteStructure() throws IOException {
        remoteShell("mkdir -p " + root);
    }

    static List<String> getDexesInDirectory(String dirPath) {
        File dir = new File(dirPath);
        List<String> ret = new ArrayList<String>();

        File[] directoryListing = dir.listFiles();
        for (File file : directoryListing) {
            if (file.getPath().endsWith(".dex")) {
                ret.add(file.getPath());
            }
        }

        return ret;
    }

    static void pushFile(String source, String dest) throws IOException {
        adb("push " + source + " " + dest);
    }

    static void pushFiles(List<String> source, String destDir) throws IOException {
        for (String i : source) {
            pushFile(i, destDir);
        }
    }
}