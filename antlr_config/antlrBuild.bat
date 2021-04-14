set LEXER_FILE=VAlgoLangLexer.g4
set PARSER_FILE=VAlgoLangParser.g4

set TARGET_DIR=../src/main/java/antlr

rmdir %TARGET_DIR% /s /q

echo Compiling the lexer..
java -jar ../lib/antlr-4.7-complete.jar %LEXER_FILE% -o %TARGET_DIR% -package antlr -no-listener -visitor -Werror 
echo Compiling the parser..
java -jar ../lib/antlr-4.7-complete.jar %PARSER_FILE% -o %TARGET_DIR% -package antlr -no-listener -visitor -Werror