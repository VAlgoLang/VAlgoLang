lexer grammar ManimLexer;

WHITESPACE: [ \n\r\t] -> skip;
SEMI: ';';
COLON: ':';
DOT: '.';
EQUAL: '=';
COMMA: ',';

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
CODE_COMMENT: '#' ~('\n' | '\r')* ('\n' | '\r')? -> skip;
LET: 'let';
FUN: 'fun';
RETURN: 'return';

// Primitive types
NUMBER_TYPE: 'number';
BOOL_TYPE: 'boolean';

// Keywords
NEW: 'new';
COMMENT: 'comment';
SLEEP: 'sleep';
STACK: 'Stack';
IF: 'if';
ELSE: 'else';

IDENT: ('a'..'z' | 'A'..'Z')('0'..'9' | 'a'..'z' | 'A'..'Z' | '_')* ;
fragment DIGIT: '0'..'9' ;
NUMBER: (DIGIT+) (DOT DIGIT+)?;
fragment ESCAPED_CHAR: ('0' | 'b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\');
fragment CHARACTER: ~('\\' | '\'' | '"') | '\\' ESCAPED_CHAR;
STRING: '"' (CHARACTER)* '"';




