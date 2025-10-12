@echo off

xcopy ".\src\main\java\me\mynameisbob1928\antinbt\modules" ".\modules\src\main\java\me\mynameisbob1928\antinbt\modules" /i /y /v

cd modules
cmd /q /c "gradlew classes"
cd ../