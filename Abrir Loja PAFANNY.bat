@echo off
title PAFANNY - Sistema (nao feche esta janela)
cd /d "%~dp0"

if not exist "%~dp0.env.producao.bat" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0gerar-credenciais.ps1"
    pause
)

call "%~dp0.env.producao.bat"

echo.
echo  ============================================
echo   Iniciando o sistema PAFANNY...
echo   Aguarde a mensagem "Started" aparecer abaixo.
echo   NAO FECHE esta janela enquanto estiver usando o site.
echo  ============================================
echo.
start "" cmd /c "timeout /t 8 >nul && start http://localhost:8080/loja.html"
java -jar target\estoque-java-1.0.0.jar
pause
