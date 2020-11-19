lexer grammar ManimLexer;

WHITESPACE: [ \n\r\t] -> skip;
SEMI: ';';
COLON: ':';
DOT: '.';
EQUAL: '=';
COMMA: ',';
AT: '@';
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
DIVIDE: '/';
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
CHAR_TYPE: 'char';

// Keywords
COMMENT: 'comment';
SLEEP: 'sleep';
STACK: 'Stack';
ARRAY: 'Array';
TREE: 'Tree';
TREE_NODE: 'Node';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
IN: 'in';
RANGE: 'range';
BREAK: 'break';
CONTINUE: 'continue';
STEP_INTO: 'stepInto';
STEP_OVER: 'stepOver';
TO_NUMBER: 'toNumber';
TO_CHAR: 'toChar';

ROOT: 'root';
NULL: 'null';
IDENT: ('a'..'z' | 'A'..'Z')('0'..'9' | 'a'..'z' | 'A'..'Z' | '_')* ;
fragment DIGIT: '0'..'9' ;
NUMBER: (DIGIT+) (DOT DIGIT+)?;
fragment ESCAPED_CHAR: ('0' | 'b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\');
fragment CHARACTER: ~('\\' | '\'' | '"') | '\\' ESCAPED_CHAR;
STRING: '"' (CHARACTER)* '"';
CHAR_LITER:'\'' CHARACTER '\'';


