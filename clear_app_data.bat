@echo off
echo Clearing SolarSnap app data...
adb shell pm clear com.solarsnap.app
echo App data cleared. The database will be recreated on next launch.
pause