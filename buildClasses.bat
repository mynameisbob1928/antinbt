@echo off

del ".\modules\src\main\java\me\mynameisbob1928\antinbt\modules\*.java"
xcopy ".\src\main\java\me\mynameisbob1928\antinbt\modules" ".\modules\src\main\java\me\mynameisbob1928\antinbt\modules" /i /y /v

cd modules
cmd /q /c "gradlew classes"
cd ../