# ftc-hotpatch
### Update FTC OpModes on the fly

- Debug running OpModes while updating them
- Deploy app changes without rebooting  

# Installation
1. Clone this repo into a folder adjacent to your FtcRobotController folder.
```
~/dev$ ls
FtcRobotController
~/dev$ git clone https://github.com/VCInventerman/ftc-hotpatch.git
Cloning into 'ftc-hotpatch'...
Unpacking objects: 100% (43/43), 21.48 KiB | 39.00 KiB/s, done.
~/dev$ ls
FtcRobotController  ftc-hotpatch
~/dev$
```
2. Copy the .run folder from this repo to FtcRobotController.
   This installs the script that deploys hotpatches.
```
~/dev$ cp -r ftc-hotpatch/.run FtcRobotController/
```
3. Include this repo's source code to your gradle project.
```
~/dev$ echo "android { sourceSets { main { java { srcDirs('../../ftc-hotpatch/src/') } } } }" >> FtcRobotController/FtcRobotController/build.gradle
```
4. Open the FtcRobotController project in Android Studio. Open FtcOpModeRegister in the folder below.
![Screenshot](https://user-images.githubusercontent.com/16017438/181691727-bf9b20bd-d649-4d59-98d4-a74ff8778ef3.png)
5. Edit FtcOpModeRegister 
```
// Copy this line directly below the other lines starting with "import"
import com.karrmedia.ftchotpatch.SupervisedClassManager;

// Copy this line directly below the line starting with "public void register"
SupervisedClassManager.init(manager);
```
6. Modify an existing OpMode to be hotpatchable
    - Add the following under existing import statements
    ```
    import com.karrmedia.ftchotpatch.SupervisedLinearOpMode;
    import com.karrmedia.ftchotpatch.Supervised;
    ``` 
    - Change the `@TeleOp` or `@Autonomous` annotation to `@Supervised`, and 
      inherit from `SupervisedOpMode` instead of `LinearOpMode` or `OpMode`
    ```diff
    - @TeleOp(name="Basic OpMode", group="Iterative Opmode")
    + @Supervised(name="Basic OpMode", group="Iterative Opmode", autonomous=false)
    - public class BasicOmniOpMode extends LinearOpMode {
    + public class BasicOmniOpMode extends SupervisedOpMode {
    ```
    - Patch existing methods to loop
    ```diff
    - public void runOpMode() {
    + public void init() {
    - waitForStart();
    + }
    + 
    + public void start() {
    - while (opModeIsActive()) {
    + }
    +
    + public void loop() {
      ...
    - }
    ```

# Usage
1. Perform a normal run in debug or release mode.
![Screenshot](https://user-images.githubusercontent.com/16017438/181691811-cbaaac69-2b2f-4267-8737-36aaee7fe770.png)
2. Launch the opmode you want to use.
3. Modify some of its code.
4. Change the current configuration to DeployHotpatch, then press Run/F5. This will preserve your current debugging session.
![Screenshot](https://user-images.githubusercontent.com/16017438/181691686-816c2e1d-cdab-435a-9529-2f2e4c372490.png)
5. Wait a bit until the new code is compiled and applied