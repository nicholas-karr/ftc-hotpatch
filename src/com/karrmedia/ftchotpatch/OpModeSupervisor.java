package com.karrmedia.ftchotpatch;

import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

// Keeps track of the code for an OpMode and runs it
public class OpModeSupervisor extends LinearOpMode {

    ElapsedTime runtime = new ElapsedTime();

    // Class and instance of the child OpMode
    Class<? extends SupervisedOpMode> clazz;
    String variation;
    SupervisedOpMode opmode;
    boolean linear;

    // Version that is incremented every time the OpMode is replaced
    int opModeVersion = 1;

    GamepadEx gamepad1ex;
    GamepadEx gamepad2ex;

    public OpModeSupervisor(Class<? extends SupervisedOpMode> clazz, String variation, boolean linear) throws IllegalAccessException, InstantiationException {
        this.clazz = clazz;
        this.variation = variation;
        this.linear = linear;
    }

    public boolean isRunning() {
        return opmode.currentState.compareTo(SupervisedOpMode.State.INIT) >= 0;
    }

    public void hotpatch() {
        try {
            RobotLog.e("Reloading class %s", clazz.getCanonicalName());
            if (isRunning()) {
                telemetry.addData("Reloading class %s", clazz.getCanonicalName());
                telemetry.update();
            }

            Class<SupervisedOpMode> newClazz = (Class<SupervisedOpMode>) SupervisedClassManager.get().findOpMode(clazz.getCanonicalName());

            SupervisedOpMode oldOpmode = opmode;

            opmode = newClazz.newInstance();

            // Copy superclass variables over
            opmode.variation = this.variation;
            opmode.gamepad1 = this.gamepad1ex;
            opmode.gamepad2 = this.gamepad2ex;
            opmode.telemetry = this.telemetry;
            opmode.hardwareMap = this.hardwareMap;
            opmode.elapsedRuntime = this.runtime;
            opmode.opModeIsActiveChecker = new SupervisedOpMode.OpModeIsActiveChecker() {
                @Override
                public boolean check() {
                    return !isStopRequested() && isStarted();
                }
            };

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
        catch (Exception e) {
            RobotLog.e("Hotpatch exception: %s", e.getMessage());
            if (isRunning()) {
                telemetry.addData("Hotpatch exception: %s", e.getMessage());
                telemetry.update();
            }
        }
    }

    @Override
    public void runOpMode() {
        try {
            opmode = clazz.newInstance();

            if (this.gamepad1ex == null) {
                this.gamepad1ex = new GamepadEx(gamepad1);
                this.gamepad2ex = new GamepadEx(gamepad2);
            }

            opmode.variation = this.variation;
            opmode.gamepad1 = this.gamepad1ex;
            opmode.gamepad2 = this.gamepad2ex;
            opmode.telemetry = this.telemetry;
            opmode.hardwareMap = this.hardwareMap;
            opmode.elapsedRuntime = this.runtime;
            opmode.opModeIsActiveChecker = new SupervisedOpMode.OpModeIsActiveChecker() {
                @Override
                public boolean check() {
                    return !isStopRequested() && isStarted();
                }
            };

            runtime.reset();
            opmode.currentState = SupervisedOpMode.State.INIT;
            opmode.init();

            if (!linear) {
                while (!isStarted() && !isStopRequested()) {
                    opmode.currentState = SupervisedOpMode.State.INIT_LOOP;
                    opmode.initLoop();
                    idle();
                }
            }
            else {
                waitForStart();
            }

            if (isStopRequested()) {
                opmode.stop();
                return;
            }

            runtime.reset();
            opmode.currentState = SupervisedOpMode.State.START;
            opmode.start();

            if (!linear) {
                opmode.currentState = SupervisedOpMode.State.LOOP;
                while (opModeIsActive() && !isStopRequested()) {
                    try {
                        if (SupervisedClassManager.get().currentVersion > opModeVersion) {
                            hotpatch();
                        }

                        opmode.loop();
                    } catch (Exception e) {
                        RobotLog.e("Exception during opMode loop: %s", e.getMessage());
                        if (isRunning()) {
                            telemetry.addData("Exception during opMode loop: %s", e.getMessage());
                            telemetry.update();
                        }
                    }
                }
            }

            opmode.currentState = SupervisedOpMode.State.STOP;
            opmode.stop();
        }
        catch (Exception e) {
            RobotLog.e("Top-level OpMode exception: %s", e.toString());
            if (isRunning()) {
                telemetry.addData("Top-level OpMode exception: %s", e.toString());
                telemetry.update();
            }
        }

    }
}
