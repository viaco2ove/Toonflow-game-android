@echo off
set ANDROID_HOME=C:\Users\viaco\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\viaco\AppData\Local\Android\Sdk
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%PATH%;C:\Users\viaco\AppData\Local\Android\Sdk\platform-tools
"D:\nvm4w\nodejs\node.exe" "C:\Users\viaco\AppData\Local\npm-cache\_npx\87826a530ba940cc\node_modules\appium\build\lib\appium.js" --address 127.0.0.1 --port 4723 --relaxed-security
