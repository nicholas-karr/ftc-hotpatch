package org.firstinspires.ftc.robotcontroller.internal;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.io.ByteArrayOutputStream;

public interface SupervisedOpMode {
    // Runs the first time an opmode is created
    public void firstInit(LinearOpMode opmode);

    // Runs after each patch to allow transient variables since as motors to be obtained
    public void init(LinearOpMode opmode);

    // Runs repeatedly while the current opmode is active
    public void loop();

    // Runs when this opmode's code is being replaces by anothers
    // Serializes non-transient fields for use in the new object
    //public ByteArrayOutputStream serialize();
}
