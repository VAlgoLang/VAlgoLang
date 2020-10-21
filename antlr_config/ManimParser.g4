parser grammar ManimParser;

options {
  tokenVocab=ManimLexer;
}

program: (stat SEMI)+ EOF;

stat: SLEEP OPEN_PARENTHESIS expr CLOSE_PARENTHESIS                 #SleepStatement
    | COMMENT OPEN_PARENTHESIS STRING CLOSE_PARENTHESIS             #CommentStatement // when string type defined we can adjust
    | LET IDENT (COLON type)? EQUAL expr                            #DeclarationStatement
    | IDENT EQUAL expr                                              #AssignmentStatement
    | method_call                                                   #MethodCallStatement;

arg_list: expr (COMMA expr)*                                        #ArgumentList;

expr: NUMBER                                                        #NumberLiteral
    | bool                                                          #BooleanLiteral
    | IDENT                                                         #Identifier
    | NEW data_structure_type                                       #DataStructureContructor
    | method_call                                                   #MethodCallExpression
    | unary_operator=(ADD | MINUS) expr                             #UnaryOperator
    | left=expr binary_operator=(ADD | MINUS | TIMES) right=expr    #BinaryExpression
    | left=expr binary_operator=(GT | GE | LE | LT) right=expr      #BinaryExpression
    | left=expr binary_operator=(EQ | NEQ) right=expr               #BinaryExpression
    | left=expr binary_operator=(AND | OR) right=expr               #BinaryExpression
    ;

method_call: IDENT DOT IDENT OPEN_PARENTHESIS arg_list? CLOSE_PARENTHESIS  #MethodCall;

type: data_structure_type                                            #DataStructureType
    | primitive_type                                                 #PrimitiveType;

data_structure_type: STACK '<' primitive_type '>'                    #StackType;

primitive_type: NUMBER_TYPE                                          #NumberType
    | BOOL_TYPE                                                      #BoolType
    ;

bool: TRUE | FALSE;