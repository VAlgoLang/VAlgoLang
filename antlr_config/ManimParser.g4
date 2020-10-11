parser grammar ManimParser;

options {
  tokenVocab=ManimLexer;
}

program: (stat SEMI | COMMENT)+ EOF;

stat: SLEEP OPEN_PARENTHESIS expr CLOSE_PARENTHESIS                 #SleepStatement
    | COMMENT OPEN_PARENTHESIS expr CLOSE_PARENTHESIS               #CommentStatement
    | LET IDENT (COLON type)? EQUAL expr                            #DeclarationStatement
    | IDENT EQUAL expr                                              #AssignmentStatement
    | method_call                                                   #MethodCallStatement;
arg_list: expr (COMMA expr)*                                        #ArgumentList;

expr: NUMBER                                                        #NumberLiteral
    | NEW STACK                                                     #StackCreate
    | IDENT                                                         #Identifier
    | method_call                                                   #MethodCallExpression
    | unary_operator=(ADD | MINUS) expr                             #UnaryOperator
    | expr binary_operator=(ADD | MINUS | TIMES) expr               #BinaryExpression;

method_call: IDENT DOT IDENT OPEN_PARENTHESIS arg_list? CLOSE_PARENTHESIS  #MethodCall;

type: INT                                                    #IntType
    | STACK                                                  #StackType;

// TODO: semantic analysis needs to take care of expressions like 1 --- 2
// TODO: type checking needs to be VERY good
// TODO: method checking on types
// n.b. we do not allow statements like new Stack . push(1); should we?