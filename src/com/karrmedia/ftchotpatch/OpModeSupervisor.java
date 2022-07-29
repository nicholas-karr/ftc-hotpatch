package com.karrmedia.ftchotpatch;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.RobotLog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

// Keeps track of the code for an OpMode and runs it
public class OpModeSupervisor extends LinearOpMode {

    // Class and instance of the child OpMode
    Class<? extends SupervisedOpMode> clazz;
    SupervisedOpMode opmode;

    // Version that is incremented every time the OpMode is replaced
    int opModeVersion = 1;

    public OpModeSupervisor(Class<? extends SupervisedOpMode> clazz) throws IllegalAccessException, InstantiationException {
        this.clazz = clazz;

        opmode = clazz.newInstance();
    }

    @Override
    public void runOpMode() {
        try {
            opmode.gamepad1 = this.gamepad1;
            opmode.gamepad2 = this.gamepad2;
            opmode.telemetry = this.telemetry;
            opmode.hardwareMap = this.hardwareMap;

            opmode.currentState = SupervisedOpMode.State.INIT;
            opmode.init();

            while (!isStarted()) {
                opmode.currentState = SupervisedOpMode.State.INIT_LOOP;
                opmode.init_loop();
            }

            opmode.currentState = SupervisedOpMode.State.START;
            opmode.start();

            opmode.currentState = SupervisedOpMode.State.LOOP;
            while (opModeIsActive()) {
                try {
                    if (SupervisedClassManager.get().currentVersion > opModeVersion) {
                        RobotLog.e("Reloading class %s", clazz.getCanonicalName());
                        telemetry.addData("Reloading class %s", clazz.getCanonicalName());
                        telemetry.update();

                        Class<SupervisedOpMode> newClazz = (Class<SupervisedOpMode>)SupervisedClassManager.get().findOpMode(clazz.getCanonicalName());

                        SupervisedOpMode oldOpmode = opmode;

                        opmode = newClazz.newInstance();

                        // Copy all non-transient variables over
                        Field[] newFields = opmode.getClass().getDeclaredFields();
                        for (Field field : oldOpmode.getClass().getDeclaredFields()) {
                            if (Modifier.isTransient(field.getModifiers())) {
                                continue;
                            }

                            // Find the matching field in the new object and copy it over
                            for (Field newField : newFields) {
                                if (newField.getName() == field.getName() && newField.getType() == field.getType()) {
                                    field.setAccessible(true);
                                    newField.setAccessible(true);

                                    newField.set(opmode, field.get(oldOpmode));
                                }
                            }
                        }

                        clazz = newClazz;
                        opmode.hotpatch();

                        opModeVersion = SupervisedClassManager.get().currentVersion;
                    }

                    opmode.loop();
                } catch (Exception e) {
                    RobotLog.e("Exception during opMode loop: %s", e.getMessage());
                    telemetry.addData("Exception during opMode loop: %s", e.getMessage());
                    telemetry.update();
                }
            }

            opmode.currentState = SupervisedOpMode.State.STOP;
            opmode.stop();
        }
        catch (Exception e) {
            RobotLog.e("Top-level opMode exception: %s", e.getMessage());
            telemetry.addData("Top-level opMode exception: %s", e.getMessage());
            telemetry.update();
        }

    }
}
