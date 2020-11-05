lexer grammar ManimLexer;

WHITESPACE: [ \n\r\t] -> skip;
SEMI: ';';
COLON: ':';
DOT: '.';
EQUAL: '=';
COMMA: ',';

RIGHT: 'right';
LEFT: 'left';
VALUE: 'value';

// boolean literals
TRUE: 'true';
FALSE: 'false';

// operators
ADD: '+';
MINUS: '-';
TIMES: '*';
LT: '<' ;
GT: '>' ;
GE: '>=' ;
LE: '<=' ;
EQ: '==';
NEQ: '!=';
AND: '&&';
OR: '||';
NOT: '!';


OPEN_PARENTHESIS: '(' ;
CLOSE_PARENTHESIS: ')' ;
OPEN_CURLY_BRACKET: '{';
CLOSE_CURLY_BRACKET: '}';
OPEN_SQUARE_BRACKET: '[';
CLOSE_SQUARE_BRACKET: ']';

CODE_COMMENT: '#' ~('\n' | '\r')* ('\n' | '\r')? -> skip;
LET: 'let';
FUN: 'fun';
RETURN: 'return';

// Primitive types
NUMBER_TYPE: 'number';
BOOL_TYPE: 'boolean';

// Keywords
COMMENT: 'comment';
SLEEP: 'sleep';
STACK: 'Stack';
ARRAY: 'Array';
TREE_NODE: 'Node';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
BREAK: 'break';
CONTINUE: 'continue';
STEP_INTO: 'stepInto';
STEP_OVER: 'stepOver';
NULL: 'null';
IDENT: ('a'..'z' | 'A'..'Z')('0'..'9' | 'a'..'z' | 'A'..'Z' | '_')* ;
fragment DIGIT: '0'..'9' ;
NUMBER: (DIGIT+) (DOT DIGIT+)?;
fragment ESCAPED_CHAR: ('0' | 'b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\');
fragment CHARACTER: ~('\\' | '\'' | '"') | '\\' ESCAPED_CHAR;
STRING: '"' (CHARACTER)* '"';




