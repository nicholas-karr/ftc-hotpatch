package com.karrmedia.ftchotpatch;

import android.annotation.SuppressLint;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import java.io.File;
import java.io.IOException;

// Keeps track of the code for an opmode and runs it
public class OpModeSupervisor extends LinearOpMode {
    long id;

    // Class and instance of the child OpMode
    Class<SupervisedOpMode> clazz;
    SupervisedOpMode opmode;

    int opmodeVersion = 1;
    DexFileClassLoader loader;

    ElapsedTime runtime;

    public OpModeSupervisor(long id, Class<SupervisedOpMode> clazz) throws IllegalAccessException, InstantiationException {
        this.id = id;
        this.clazz = clazz;

        opmode = clazz.newInstance();
    }

    public long getId() {
        return id;
    }

    @SuppressLint("SdCardPath")
    @Override
    public void runOpMode() {
        try {
            /*File dexFolder = new File("/sdcard/FIRST/hotpatch/");

            //watcher = new FileObserver(new File("/sdcard/FIRST/hotpatch/" + clazz.getPackage().getName().replaceAll("\\.", "/"))) {
            watcher = new FileObserver(dexFolder) {
                @Override
                public void onEvent(int event, @Nullable String path) {
                    RobotLog.e("Hotpatch file update detected, initiating update of all supervised classes");

                    //if (path == null || !path.contains(clazz.getSimpleName())) { return; }
                    if (path == null) { return; }
                    if (event != FileObserver.CLOSE_WRITE && event != FileObserver.MODIFY) { return; }
                }
            };
            watcher.startWatching();

            //loader = new URLClassLoader(new URL[]{new File("/sdcard/FIRST/hotpatch").toURI().toURL()});
            loader = new DexFileClassLoader(getLoadableDexes(dexFolder), null); //this.getClass().getClassLoader());*/

            opmode.firstInit(this);
            opmode.init(this);

            waitForStart();

            while (opModeIsActive()) {
                try {
                    if (SupervisedClassManager.currentVersion > opmodeVersion) {
                        RobotLog.e("Reloading class %s", clazz.getCanonicalName());
                        telemetry.addData("Reloading class %s", clazz.getCanonicalName());
                        telemetry.update();

                        //DexFile dexFile = new DexFile("/sdcard/FIRST/hotpatch/RedTeleOp.dex");

                        //Class newClazz = loader.findClass(clazz.getCanonicalName());
                        //Class newClazz = dexFile.loadClass(clazz.getCanonicalName(), this.getClass().getClassLoader());
                        Class<SupervisedOpMode> newClazz = (Class<SupervisedOpMode>)SupervisedClassManager.get().findOpMode(clazz.getCanonicalName());

                        SupervisedOpMode oldOpmode = opmode;

                        opmode = newClazz.newInstance();
                        clazz = newClazz;
                        opmode.init(this);

                        opmodeVersion = SupervisedClassManager.get().currentVersion;
                    }

                    opmode.loop();
                } catch (Exception e) {
                    RobotLog.e("Exception during opmode loop: %s", e.getMessage());
                    telemetry.addData("Exception during opmode loop: %s", e.getMessage());
                    telemetry.update();
                }
            }
        }
        catch (Exception e) {
            RobotLog.e("Top-level opmode exception: %s", e.getMessage());
            telemetry.addData("Top-level opmode exception: %s", e.getMessage());
            telemetry.update();
        }

    }



    void reloadClass() {

    }

    // Returns a colon-separated list of dex files that can be hotpatched
    String getLoadableDexes(File folder) throws IOException {
        StringBuilder ret = new StringBuilder();

        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                ret.append(file.getCanonicalPath());
                ret.append(File.pathSeparatorChar);
            }
        }

        if (ret.length() != 0) {
            // Remove trailing colon
            ret.deleteCharAt(ret.length() - 1);
        }
        else {
            RobotLog.e("No files found to hotpatch?");
        }

        return ret.toString();
    }

    class DexFileClassLoader extends PathClassLoader {
        public DexFileClassLoader(String dexPath, ClassLoader parent) {
            super(dexPath, parent);
        }

        public Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }
    }
}
