package com.karrmedia.ftchotpatch;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public abstract class SupervisedOpMode {
    // Code that runs when the INIT button is pressed (mandatory)
    public abstract void init();

    // Code that runs repeatedly during the init stage (optional)
    public void init_loop() {}

    // Code that runs when the PLAY button is pressed (optional)
    public void start() {}

    // Code that runs repeatedly after the PLAY button is pressed (optional)
    public void loop() {}

    // Code that runs when the OpMode is stopped (optional)
    public void stop() {}

    // Code that runs after this OpMode is dynamically updated
    public void hotpatch() {}


    // Compatibility with LinearOpMode
    enum State {
        DEFAULT,
        STOP,
        INIT,
        INIT_LOOP,
        START,
        LOOP,
    }

    public State currentState = State.STOP;

    public boolean opModeIsActive() {
        return currentState.compareTo(State.START) >= 0;
    }

    public Gamepad gamepad1 = null;
    public Gamepad gamepad2 = null;
    public Telemetry telemetry = null;
    public HardwareMap hardwareMap = null;

    public void updateTelemetry(Telemetry telemetry) {
        telemetry.update();
    }
}
