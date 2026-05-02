@echo off
chcp 65001 > nul
echo ========================================
echo   今日紫微 - 編譯並安裝到模擬器
echo ========================================
echo.

set VERSION=1.0.0

echo [1/2] 編譯 APK (v%VERSION%)...
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot
"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -Xmx4096m -XX:MaxMetaspaceSize=1g -Dorg.gradle.appname=gradlew -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=1g"

if %ERRORLEVEL% neq 0 (
    echo.
    echo [錯誤] 編譯失敗
    pause
    exit /b 1
)

echo.
echo [2/2] 安裝到模擬器...
echo 嘗試連線 MSI App Player (127.0.0.1:21503)...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" connect 127.0.0.1:21503

"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" install -r "app\build\outputs\apk\debug\今日紫微-v%VERSION%.apk"

if %ERRORLEVEL% neq 0 (
    echo.
    echo [提示] 如果安裝失敗，請確認 MSI App Player 已開啟
    pause
    exit /b 1
)

echo.
echo ========================================
echo   安裝完成！正在啟動「今日紫微」
echo ========================================

"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" shell am start -n com.zwds.today/.MainActivity

echo.
echo 完成。
pause
