package com.karrmedia.ftchotpatch;

import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.concurrent.TimeUnit;

public abstract class SupervisedOpMode {
    // Code that runs when the INIT button is pressed (optional)
    public void init() throws Exception {};

    // Code that runs repeatedly during the init stage (optional)
    public void initLoop() throws Exception {}

    // Code that runs when the PLAY button is pressed (optional)
    public void start() throws Exception {}

    // Code that runs repeatedly after the PLAY button is pressed (optional)
    public void loop() throws Exception {}

    // Code that runs when the OpMode is stopped (optional)
    public void stop() throws Exception {}

    // Code that runs after this OpMode is dynamically updated
    public void hotpatch() throws Exception {}


    enum State {
        DEFAULT,
        STOP,
        INIT,
        INIT_LOOP,
        START,
        LOOP,
    }

    public State currentState = State.STOP;

    // Compatibility with LinearOpMode
    public boolean opModeIsActive() {
        return currentState.compareTo(State.START) >= 0;
    }

    public String variation = "";

    public GamepadEx gamepad1 = null;
    public GamepadEx gamepad2 = null;
    public Telemetry telemetry = null;
    public HardwareMap hardwareMap = null;

    public void updateTelemetry(Telemetry telemetry) {
        telemetry.update();
    }

    protected ElapsedTime elapsedRuntime = new ElapsedTime();
    public double getRuntime() { return elapsedRuntime.time(TimeUnit.SECONDS); }
}
