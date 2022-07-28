package org.firstinspires.ftc.robotcontroller.internal;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.robotcore.internal.opmode.InstantRunHelper;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMeta;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMetaAndClass;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMetaAndInstance;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dalvik.system.DexFile;

public class SupervisedClassLoader {

    public DexFile dexFile;

    public SupervisedClassLoader(DexFile dexFile) throws IOException {
        if (dexFile == null) {
            //this.dexFile = new DexFile(AppUtil.getDefContext().getPackageCodePath());
            this.dexFile = new DexFile(AppUtil.getInstance().getApplication().getPackageCodePath());
        }
    }

    // Extracts additional information from the annotations in getSupervisedClasses()
    public List<OpModeMetaAndInstance> getSupervisedOpmodes() throws IllegalAccessException, InstantiationException {
        List<Class<OpMode>> classes = getSupervisedClasses();

        OpModeMeta.Builder builder = new OpModeMeta.Builder();

        List<OpModeMetaAndInstance> opmodes = new LinkedList<OpModeMetaAndInstance>();
        for (Class clazz : classes) {
            Supervised annotation = (Supervised)clazz.getAnnotation(Supervised.class);

            builder.flavor = annotation.autonomous() ? OpModeMeta.Flavor.AUTONOMOUS : OpModeMeta.Flavor.TELEOP;
            builder.group = annotation.group();
            builder.name = annotation.name();
            builder.autoTransition = annotation.next();
            builder.source = OpModeMeta.Source.ANDROID_STUDIO;

            OpModeSupervisor supervisor = new OpModeSupervisor(annotation.hashCode(), clazz);

            // Build the new opmode consisting of the information from the annotation, and the supervisor
            opmodes.add(new OpModeMetaAndInstance(builder.build(), supervisor, null));
        }

        return opmodes;
    }

    public List<Class<OpMode>> getSupervisedClasses()
    {
        ClassLoader classLoaderToUse = this.getClass().getClassLoader();

        //InputStream stream = classLoaderToUse.getResourceAsStream("org/firstinspires/ftc/teamcode");
        //BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        //Set<String> classNames = reader.lines()
        //        .filter(line -> line.endsWith(".class"))
        //        .collect(Collectors.toSet());


        List<String> classNames = new ArrayList<String>(Collections.list(dexFile.entries()));

        classNames.addAll(InstantRunHelper.getAllClassNames(AppUtil.getDefContext()));

        List<Class> classes = classNamesToClasses(classNames);
        List<Class<OpMode>> opmodes = new LinkedList<Class<OpMode>>();

        for (Class clazz : classes) {
            if (clazz.isAnnotationPresent(Supervised.class) && !clazz.isAnnotationPresent(Disabled.class)) {
                opmodes.add(clazz);
            }
        }

        return opmodes;
    }

    public List<Class> classNamesToClasses(Collection<String> classNames)
    {
        List<Class> res = new LinkedList<Class>();

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
                Class clazz = Class.forName(className, false, classLoaderToUse);
                res.add(clazz);
            }
            catch (NoClassDefFoundError|ClassNotFoundException ex)
            {
                // We can't find that class
                continue;
            }
        }

        return res;
    }
}
