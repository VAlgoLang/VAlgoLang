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
    | IDENT                                                         #Identifier
    | NEW STACK                                                     #StackCreate
    | method_call                                                   #MethodCallExpression
    | unary_operator=(ADD | MINUS) expr                             #UnaryOperator
    | expr binary_operator=(ADD | MINUS | TIMES) expr               #BinaryExpression;

method_call: IDENT DOT IDENT OPEN_PARENTHESIS arg_list? CLOSE_PARENTHESIS  #MethodCall;

type: NUMBER_TYPE                                                   #NumberType
    | STACK                                                         #StackType;