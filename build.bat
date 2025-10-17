@echo off

del ".\modules\src\main\java\me\mynameisbob1928\antinbt\modules\*.java"
xcopy ".\src\main\java\me\mynameisbob1928\antinbt\modules" ".\modules\src\main\java\me\mynameisbob1928\antinbt\modules" /i /y /v
rmdir ".\src\main\java\me\mynameisbob1928\antinbt\modules" /S /Q

cmd /q /c "gradlew build"

cd modules
cmd /q /c "gradlew classes"
cd ../

xcopy ".\modules\src\main\java\me\mynameisbob1928\antinbt\modules" ".\src\main\java\me\mynameisbob1928\antinbt\modules" /i /y /v
del .\src\main\java\me\mynameisbob1928\antinbt\modules\placeholder.txt

