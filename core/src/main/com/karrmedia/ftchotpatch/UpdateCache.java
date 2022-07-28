package org.firstinspires.ftc.robotcontroller.internal;

import android.annotation.SuppressLint;
import android.os.FileObserver;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.RobotLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dalvik.system.PathClassLoader;

public class UpdateCache {
    static public UpdateCache inst;

    FileObserver watcher;

    public int ver = 0;
    //ByteBuffer[] bufs;
    public FakeParentClassLoader loader;
    List<File> dexPaths = new ArrayList<>();

    @SuppressLint("SdCardPath")
    public UpdateCache() {
        watcher = new FileObserver(new File("/sdcard/FIRST/hotpatch/updateLock")) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                RobotLog.d("Saw updateLock event of type " + event);

                // Detect when updateLock has its timestamp changed
                if (event == FileObserver.ATTRIB) {
                    RobotLog.d("Hotpatch update triggered");

                    loadNewDex();
                }
            }
        };
        watcher.startWatching();

        loadNewDex();
    }

    @SuppressLint("SdCardPath")
    public void loadNewDex() {
        dexPaths.clear();
        StringBuilder builder = new StringBuilder();

        for (final File file : Objects.requireNonNull(new File("/sdcard/FIRST/hotpatch").listFiles())) {
            if (file.isFile() && file.getPath().contains("classes")) {
                dexPaths.add(file);
                builder.append(file.getPath());
                builder.append(':');
            }
        }
        if (builder.length() != 0) { builder.deleteCharAt(builder.length() - 1); }

        loader = new FakeParentClassLoader(builder.toString(), this.getClass().getClassLoader());

        ver++;


        // Leave old buffer behind for the old ClassLoader
            /*bufs = new ByteBuffer[dexPaths.size()];

            for (int i = 0; i < bufs.length; i++) {
                try {
                    FileInputStream s = new FileInputStream(dexPaths.get(i));
                    FileChannel channel = s.getChannel();
                    MappedByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                    bufs[i] = buf;
                }
                catch (Exception e) {
                    RobotLog.e("Dex loading failure: " + e.getMessage());
                }
            }

            loader = new InMemoryDexClassLoader(bufs, this.getClass().getClassLoader());*/


    }

    class FakeParentClassLoader extends PathClassLoader {
        public FakeParentClassLoader(String dexPath, ClassLoader parent) {
            super(dexPath, parent);
        }

        public Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }
    }
}