parser grammar ManimParser;

options {
  tokenVocab=ManimLexer;
}

program: function* stat EOF;

function: FUN IDENT OPEN_PARENTHESIS param_list? CLOSE_PARENTHESIS (COLON type)? OPEN_CURLY_BRACKET statements=stat? CLOSE_CURLY_BRACKET;

param_list: param (COMMA param)*                                    #ParameterList;

param: IDENT COLON type                                                  #Parameter;

stat: SLEEP OPEN_PARENTHESIS expr CLOSE_PARENTHESIS SEMI                 #SleepStatement
    | COMMENT OPEN_PARENTHESIS STRING CLOSE_PARENTHESIS SEMI             #CommentStatement // when string type defined we can adjust
    | LET IDENT (COLON type)? EQUAL expr SEMI                            #DeclarationStatement
    | IDENT EQUAL expr SEMI                                              #AssignmentStatement
    | IF OPEN_PARENTHESIS ifCond=expr CLOSE_PARENTHESIS
    OPEN_CURLY_BRACKET ifStat=stat? CLOSE_CURLY_BRACKET
     elseIf*
    (ELSE OPEN_CURLY_BRACKET elseStat=stat? CLOSE_CURLY_BRACKET)?        #IfStatement
    | stat1=stat stat2=stat                                              #ConsecutiveStatement
    | method_call SEMI                                                   #MethodCallStatement
    | RETURN expr SEMI                                                  #ReturnStatement;


elseIf: ELSE IF OPEN_PARENTHESIS elifCond=expr CLOSE_PARENTHESIS OPEN_CURLY_BRACKET elifStat=stat? CLOSE_CURLY_BRACKET;

arg_list: expr (COMMA expr)*                                        #ArgumentList;

expr: NUMBER                                                        #NumberLiteral
    | bool                                                          #BooleanLiteral
    | IDENT                                                         #Identifier
    | NEW data_structure_type                                       #DataStructureContructor
    | method_call                                                   #MethodCallExpression
    | unary_operator=(ADD | MINUS | NOT) expr                       #UnaryOperator
    | left=expr binary_operator=(ADD | MINUS | TIMES) right=expr    #BinaryExpression
    | left=expr binary_operator=(GT | GE | LE | LT) right=expr      #BinaryExpression
    | left=expr binary_operator=(EQ | NEQ) right=expr               #BinaryExpression
    | left=expr binary_operator=(AND | OR) right=expr               #BinaryExpression
    ;

method_call: IDENT DOT IDENT OPEN_PARENTHESIS arg_list? CLOSE_PARENTHESIS  #MethodCall
           | IDENT OPEN_PARENTHESIS arg_list? CLOSE_PARENTHESIS            #FunctionCall;

type: data_structure_type                                            #DataStructureType
    | primitive_type                                                 #PrimitiveType;

data_structure_type: STACK LT primitive_type GT                      #StackType;

primitive_type: NUMBER_TYPE                                          #NumberType
    | BOOL_TYPE                                                      #BoolType
    ;

bool: TRUE | FALSE;