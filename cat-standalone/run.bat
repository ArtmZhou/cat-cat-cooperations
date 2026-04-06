@echo off
chcp 65001 >nul
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -jar cat-standalone\target\cat-standalone-1.0.0-SNAPSHOT.jar
pause