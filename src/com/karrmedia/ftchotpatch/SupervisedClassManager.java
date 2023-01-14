package com.karrmedia.ftchotpatch;

import android.annotation.SuppressLint;
import android.os.FileObserver;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.opmode.InstantRunHelper;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMeta;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMetaAndInstance;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

public class SupervisedClassManager {

    public DexFile dexFile;

    public FileObserver watcher;
    public int currentVersion = 0;
    public FakeParentClassLoader loader;

    List<OpModeSupervisor> opmodes = new ArrayList<>();


    static SupervisedClassManager inst;
    public static SupervisedClassManager get() {
        if (inst == null) {
            throw new NullPointerException();
        }
        return inst;
    }


    public static void init(OpModeManager registry) {
        inst = new SupervisedClassManager();

        List<OpModeMetaAndInstance> opmodeMetas = get().getSupervisedOpmodes();

        for (OpModeMetaAndInstance opmode : opmodeMetas) {
            get().opmodes.add((OpModeSupervisor)opmode.instance);
            registry.register(opmode.meta, opmode.instance);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public SupervisedClassManager()  {
        try {
            //todo: replace this with something that isn't deprecated (right now it is inspired by how FTC does it)
            this.dexFile = new DexFile(AppUtil.getInstance().getApplication().getPackageCodePath());

            @SuppressLint("SdCardPath")
            File classDirectory = new File("/sdcard/FIRST/hotpatch/");
            if (!classDirectory.exists()){
                classDirectory.mkdirs();
            }

            @SuppressLint("SdCardPath")
            File updateLock = new File("/sdcard/FIRST/hotpatch/updateLock");
            updateLock.createNewFile();

            watcher = new FileObserver(updateLock.getAbsolutePath()) {
                @Override
                public void onEvent(int event, @Nullable String path) {
                    RobotLog.d("Saw updateLock event of type " + event);

                    // Detect when updateLock has its timestamp changed
                    if (event == FileObserver.ATTRIB) {
                        RobotLog.d("Hotpatch update triggered");

                        loadNewDex();
                        hotpatchAll();
                    }
                }
            };
            watcher.startWatching();

            loadNewDex();
        }
        catch (Exception e) {
            // Error loading this app's dex, no supervised opmodes will be available
        }
    }

    // Extracts additional information from the annotations in getSupervisedClasses()
    public List<OpModeMetaAndInstance> getSupervisedOpmodes() {
        List<Class<? extends SupervisedLinearOpMode>> classes = getSupervisedClasses();

        OpModeMeta.Builder builder = new OpModeMeta.Builder();

        List<OpModeMetaAndInstance> opmodes = new LinkedList<>();
        for (Class<? extends SupervisedLinearOpMode> clazz : classes) {
            Supervised annotation = clazz.getAnnotation(Supervised.class);
            if (annotation == null) { throw null; }

            if (annotation.variations().length != 0 && annotation.name().contains("?")) {
                for (String variation : annotation.variations()) {
                    try {
                        builder.flavor = annotation.autonomous() ? OpModeMeta.Flavor.AUTONOMOUS : OpModeMeta.Flavor.TELEOP;
                        builder.group = annotation.group();
                        builder.name = annotation.name().replaceAll("\\?", variation);
                        builder.autoTransition = annotation.next().replaceAll("\\?", variation);
                        builder.source = OpModeMeta.Source.ANDROID_STUDIO;

                        OpModeSupervisor supervisor = new OpModeSupervisor(clazz, variation, annotation.linear());

                        // Build the new OpMode consisting of the information from the annotation, and the supervisor
                        opmodes.add(new OpModeMetaAndInstance(builder.build(), supervisor, null));
                    } catch (InstantiationException | IllegalAccessException e) {}
                }
            }
            else
            {
                try {
                    builder.flavor = annotation.autonomous() ? OpModeMeta.Flavor.AUTONOMOUS : OpModeMeta.Flavor.TELEOP;
                    builder.group = annotation.group();
                    builder.name = annotation.name();
                    builder.autoTransition = annotation.next();
                    builder.source = OpModeMeta.Source.ANDROID_STUDIO;

                    OpModeSupervisor supervisor = new OpModeSupervisor(clazz, "", annotation.linear());

                    // Build the new OpMode consisting of the information from the annotation, and the supervisor
                    opmodes.add(new OpModeMetaAndInstance(builder.build(), supervisor, null));
                } catch (InstantiationException | IllegalAccessException e) {}
            }
        }

        return opmodes;
    }

    public List<Class<? extends SupervisedLinearOpMode>> getSupervisedClasses()
    {
        // Dumps all class entries from dexFile
        List<String> classNames = new ArrayList<>(Collections.list(dexFile.entries()));

        classNames.addAll(InstantRunHelper.getAllClassNames(AppUtil.getDefContext()));

        List<Class<?>> classes = classNamesToClasses(classNames);
        List<Class<? extends SupervisedLinearOpMode>> opmodes = new LinkedList<>();

        for (Class clazz : classes) {
            // Check if a class has the correct annotation and allow it to opt out with the normal Disabled annotation
            if (clazz.isAnnotationPresent(Supervised.class) && !clazz.isAnnotationPresent(Disabled.class)) {
                opmodes.add(clazz);
            }
        }

        return opmodes;
    }

    public List<Class<?>> classNamesToClasses(Collection<String> classNames)
    {
        List<Class<?>> res = new LinkedList<>();

        // Use whatever class loader created this object - all of our swappable classes are backed by loadable classes in the APK,
        // so they will be loadable by this
        ClassLoader classLoaderToUse = this.getClass().getClassLoader();

        for (String className : classNames)
        {
            // Only consider our code
            if (!className.contains("teamcode")) {
                continue;
            }

            try
            {
                // Map class name back to class
                Class<?> clazz = Class.forName(className, false, classLoaderToUse);
                res.add(clazz);
            }
            catch (NoClassDefFoundError|ClassNotFoundException ex)
            {
                // We can't find that class
            }
        }

        return res;
    }



    @SuppressLint("SdCardPath")
    public void loadNewDex() {
        StringBuilder paths = new StringBuilder();

        for (final File file : Objects.requireNonNull(new File("/sdcard/FIRST/hotpatch").listFiles())) {
            if (file.isFile() && file.getPath().contains("classes")) {
                paths.append(file.getPath());
                paths.append(':');
            }
        }

        // Cut off trailing :
        if (paths.length() > 0) { paths.deleteCharAt(paths.length() - 1); }

        loader = new FakeParentClassLoader(paths.toString(), this.getClass().getClassLoader());

        currentVersion++;
    }

    Class<SupervisedLinearOpMode> findOpMode(String name) {
        try {
            return (Class<SupervisedLinearOpMode>)loader.findClass(name);
        }
        catch (ClassNotFoundException e) {
            try {
                // Give up on updating the class and return the apk's version
                return (Class<SupervisedLinearOpMode>)this.getClass().getClassLoader().loadClass(name);
            }
            catch (ClassNotFoundException e2) {
                // Should be impossible
                throw null;
            }
        }
    }

    void hotpatchAll() {
        for (OpModeSupervisor i : opmodes) {
            if (i.opModeIsActive()) {
                continue;
            }

            i.hotpatch();
        }
    }

    // Just exposes findClass() which sidesteps searching the parent classes for the updated versions
    static class FakeParentClassLoader extends PathClassLoader {
        public FakeParentClassLoader(String dexPath, ClassLoader parent) {
            super(dexPath, parent);
        }

        public Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }
    }
}
