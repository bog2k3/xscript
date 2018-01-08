echo outputstream > exclude.txt
xcopy /s /y /EXCLUDE:exclude.txt "..\..\Main\bin" .\
xcopy /s /y /EXCLUDE:exclude.txt "..\..\XGUI\bin" .\
xcopy /s /y /EXCLUDE:exclude.txt "..\..\kernel\bin" .\
xcopy /s /y /EXCLUDE:exclude.txt "..\..\xApp\bin" .\
xcopy /s /y /EXCLUDE:exclude.txt "..\..\XScripter\bin" .\
del /q exclude.txt
pause
