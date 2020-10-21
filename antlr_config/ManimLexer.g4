lexer grammar ManimLexer;

WHITESPACE: [ \n\r\t] -> skip;
SEMI: ';';
COLON: ':';

// boolean literals
TRUE: 'true';
FALSE: 'false';

// operators
ADD: '+';
MINUS: '-';
TIMES: '*';
DOT: '.';
EQUAL: '=';
COMMA: ',';
LT: '<' ;
GT: '>' ;
GE: '>=' ;
LE: '<=' ;
EQ: '==';
NEQ: '!=';
AND: '&&';
OR: '||';


OPEN_PARENTHESIS: '(' ;
CLOSE_PARENTHESIS: ')' ;
CODE_COMMENT: '#' ~('\n' | '\r')* ('\n' | '\r')? -> skip;
LET: 'let';

// Primitive types
NUMBER_TYPE: 'number';
BOOL_TYPE: 'boolean';

NEW: 'new';
COMMENT: 'comment';
SLEEP: 'sleep';
STACK: 'Stack';
IDENT: ('a'..'z' | 'A'..'Z')('0'..'9' | 'a'..'z' | 'A'..'Z' | '_')* ;
fragment DIGIT: '0'..'9' ;
NUMBER: (DIGIT+) (DOT DIGIT+)?;
fragment ESCAPED_CHAR: ('0' | 'b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\');
fragment CHARACTER: ~('\\' | '\'' | '"') | '\\' ESCAPED_CHAR;
STRING: '"' (CHARACTER)* '"';




