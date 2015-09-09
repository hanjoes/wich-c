/*
The MIT License (MIT)

Copyright (c) 2015 Terence Parr, Hanzhou Shi, Shuai Yuan, Yuanyuan Zhang

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

grammar Wich;
@header {package wich.parser;
}

file : script ;

script : (statement | function)+ EOF ;

function
	:	'func' ID '(' formal_args ')' (':' type)? '{' statement* '}'
	;

formal_args : formal_arg (',' formal_arg)* ;

formal_arg : ID ':' type ;

type:	'int'
	|	'float'
	|	'string'
	|	'[' ']'
	;

statement
	:	'if' '(' expr ')' statement ('else' statement)?		# If
	|	'while' '(' expr ')' statement						# While
	|	'var' ID ('=' expr)?								# VarDef
	|	ID '=' expr											# Assign
	|	ID '[' expr ']' '=' expr							# ElementAssign
	|	call_expr											# CallStatement
	|	'return' expr										# Return
	|	'{' statement* '}'									# Block
	;

expr:	expr operator expr									# Op
	|	'-' expr											# Negate
	|	'!' expr											# Not
	|	call_expr											# Call
	|	ID '[' expr ']'										# Index
	|	'(' expr ')'										# Parens
	|	primary												# Atom
	;

operator  : '*'|'/'|'+'|'-'|'<'|'<='|'=='|'!='|'>'|'>='|'||'|'&&'|' . ' ; // no precedence
call_expr : ID '(' expr_list ')' ;
expr_list : expr (',' expr)* ;

primary
	:	ID
	|	INT
	|	FLOAT
	|	STRING
	|	'[' expr_list ']'
	;

LINE_COMMENT : '//' .*? ('\n'|EOF) -> channel(HIDDEN) ;
COMMENT      : '/*' .*? '*/'    -> channel(HIDDEN) ;

ID  : [a-zA-Z_] [a-zA-Z0-9_]* ;
INT : [0-9]+ ;
FLOAT
    :   '-'? INT '.' INT EXP?   // 1.35, 1.35E-9, 0.3, -4.5
    |   '-'? INT EXP            // 1e10 -3e4
    ;
fragment EXP :   [Ee] [+\-]? INT ;

STRING :  '"' (ESC | ~["\\])* '"' ;
fragment ESC :   '\\' ["\bfnrt] ;

WS : [ \t\n\r]+ -> channel(HIDDEN) ;