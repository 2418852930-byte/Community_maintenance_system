#!/bin/sh
# Maven 启动包装脚本：在 Git Bash 下直接调用 Maven 官方 classworlds launcher
# 用法： sh mvn.sh -o clean compile
unset MAVEN_HOME
unset M2_HOME
export JAVA_HOME="D:/02-DevTools/01-Languages/jdk-17"
MW="D:/02-DevTools/01-Languages/apache-maven-3.9.16"
"$JAVA_HOME/bin/java" -cp "$MW/boot/plexus-classworlds-2.11.0.jar" \
  -Dclassworlds.conf="$MW/bin/m2.conf" \
  -Dmaven.home="$MW" \
  -Dmaven.multiModuleProjectDirectory="$MW" \
  org.codehaus.plexus.classworlds.launcher.Launcher "$@"
