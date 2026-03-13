#!/bin/bash
# track-backend Linux 启动脚本（瘦 JAR + lib 依赖目录）
# 部署需上传: track-backend-1.0.0.jar 与 lib/ 目录
# 用法: ./start.sh  或  nohup ./start.sh &

cd "$(dirname "$0")/.."
JAR="track-backend-1.0.0.jar"
LIB="lib"

if [ ! -f "$JAR" ]; then
  echo "错误: 未找到 $JAR，请先执行 mvn clean package 并上传 JAR 与 lib/ 到当前目录"
  exit 1
fi
if [ ! -d "$LIB" ]; then
  echo "错误: 未找到 $LIB/ 目录，请将 target/lib 上传为 lib/"
  exit 1
fi

# 瘦 JAR：用 -cp 指定主类并加载 lib/* 依赖
nohup java -cp "$JAR:$LIB/*" com.track.TrackApplication "$@" > nohup.out 2>&1 &
echo "已后台启动，PID: $!"
echo "查看日志: tail -f nohup.out"
