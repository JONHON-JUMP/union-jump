@echo off
chcp 65001 >nul
title JUMP 前端开发环境

cd /d %~dp0

echo ================================
echo 启动 JUMP 前端开发环境
echo 当前目录：%cd%
echo ================================

set NODE_OPTIONS=--openssl-legacy-provider

npm run dev

pause