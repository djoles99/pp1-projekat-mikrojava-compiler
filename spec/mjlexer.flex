
package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\r\n" 	{ }
"\f" 	{ }

"program"   	{ return new_symbol(sym.PROG, yytext());}
"print" 		{ return new_symbol(sym.PRINT, yytext()); }
"read" 			{ return new_symbol(sym.READ, yytext()); }
"return" 		{ return new_symbol(sym.RETURN, yytext()); }
"void" 			{ return new_symbol(sym.VOID, yytext()); }
"class" 		{ return new_symbol(sym.CLASS, yytext()); }
"break"   		{ return new_symbol(sym.BREAK, yytext());}
"if"   			{ return new_symbol(sym.IF, yytext());}
"else"   		{ return new_symbol(sym.ELSE, yytext());}
"const"   		{ return new_symbol(sym.CONST, yytext());}
"new"   		{ return new_symbol(sym.NEW, yytext());}
"extends"   	{ return new_symbol(sym.EXTENDS, yytext());}
"continue"   	{ return new_symbol(sym.CONTINUE, yytext());}
"for"   		{ return new_symbol(sym.FOR, yytext());}
"static"   		{ return new_symbol(sym.STATIC, yytext());}
"namespace"   	{ return new_symbol(sym.NAMESPACE, yytext());}
"using"   		{ return new_symbol(sym.USING, yytext());}
"goto"   		{ return new_symbol(sym.GOTO, yytext());}
"range"   		{ return new_symbol(sym.RANGE, yytext());}
"+" 			{ return new_symbol(sym.PLUS, yytext()); }
"-" 			{ return new_symbol(sym.MINUS, yytext()); }
"=" 			{ return new_symbol(sym.EQUAL, yytext()); }
";" 			{ return new_symbol(sym.SEMI, yytext()); }
"," 			{ return new_symbol(sym.COMMA, yytext()); }
"(" 			{ return new_symbol(sym.LPAREN, yytext()); }
")" 			{ return new_symbol(sym.RPAREN, yytext()); }
"{" 			{ return new_symbol(sym.LBRACE, yytext()); }
"}"				{ return new_symbol(sym.RBRACE, yytext()); }
"["				{ return new_symbol(sym.LBRACKET, yytext()); }
"]"				{ return new_symbol(sym.RBRACKET, yytext()); }
"&&"			{ return new_symbol(sym.AND, yytext()); }
"||"			{ return new_symbol(sym.OR, yytext()); }
"*"				{ return new_symbol(sym.MUL, yytext()); }
"/"				{ return new_symbol(sym.DIV, yytext()); }
"%"				{ return new_symbol(sym.MOD, yytext()); }
"."				{ return new_symbol(sym.DOTACCESS, yytext()); }
":"				{ return new_symbol(sym.COLON, yytext()); }
"++"			{ return new_symbol(sym.INCR, yytext()); }
"--"			{ return new_symbol(sym.DECR, yytext()); }
"=="			{ return new_symbol(sym.EQ, yytext()); }
"!="			{ return new_symbol(sym.NEQ, yytext()); }
">"				{ return new_symbol(sym.GT, yytext()); }
">="			{ return new_symbol(sym.GEQ, yytext()); }
"<"				{ return new_symbol(sym.LT, yytext()); }
"<="			{ return new_symbol(sym.LEQ, yytext()); }
"=>"			{ return new_symbol(sym.LAMBDA, yytext()); }
"@"				{ return new_symbol(sym.SQ, yytext()); }

"//" {yybegin(COMMENT);}
<COMMENT> . {yybegin(COMMENT);}
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

"'"."'" { return new_symbol(sym.CHAR, new Character(yytext().charAt(1))); }
"true"|"false" { return new_symbol(sym.BOOL, new Integer(Boolean.parseBoolean(yytext())?1:0)); }
[0-9]+  { return new_symbol(sym.NUMBER, new Integer(yytext())); }
([a-z]|[A-Z])[a-zA-Z0-9_]* 	{return new_symbol(sym.IDENT, yytext()); }

. { System.err.println("Leksicka greska ("+yytext()+") u liniji "+(yyline+1)); }






