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
package wich.semantics;


import org.antlr.symtab.Symbol;
import org.antlr.symtab.Type;
import org.antlr.symtab.TypedSymbol;
import org.antlr.v4.runtime.misc.NotNull;
import wich.errors.WichErrorHandler;
import wich.parser.WichParser;
import wich.parser.WichParser.ExprContext;
import wich.semantics.symbols.WFunctionSymbol;
import wich.semantics.symbols.WVariableSymbol;


public class TypeAnnotator extends MaintainScopeListener {
	public TypeAnnotator(WichErrorHandler errorHandler) {
		super(errorHandler);
	}

	@Override
	public void exitOp(@NotNull WichParser.OpContext ctx) {
		int op = ctx.operator().start.getType();
		ExprContext lExpr = ctx.expr(0);
		ExprContext rExpr = ctx.expr(1);
		ctx.exprType = SymbolTable.op(op, lExpr, rExpr);
	}

	@Override
	public void exitNegate(@NotNull WichParser.NegateContext ctx) {
		ctx.exprType = ctx.expr().exprType;
	}

	@Override
	public void exitNot(@NotNull WichParser.NotContext ctx) {
		//! expr, expr is a boolean
		ctx.exprType = ctx.expr().exprType;
	}

	@Override
	public void exitCall(@NotNull WichParser.CallContext ctx) {
		Symbol s = currentScope.resolve(ctx.call_expr().ID().getText());
		if ( s!=null && s instanceof WFunctionSymbol ) {
			ctx.exprType = ((WFunctionSymbol) s).getType();
		} else {
			// TODO: add error here
		}
	}

	@Override
	public void exitIndex(@NotNull WichParser.IndexContext ctx) {
		Symbol s = currentScope.resolve(ctx.ID().getText());
		if ( s==null || s instanceof WVariableSymbol ) {
			// TODO: add error here
		}
		// string[i] returns a single character string
		Type idType = ((WVariableSymbol) s).getType();
		if ( idType==SymbolTable._string ) {
			ctx.exprType = SymbolTable._string;
		} else if ( idType==SymbolTable._vector ) {
			ctx.exprType = SymbolTable._float;
		} else {
			// TODO: add error here
		}
	}

	@Override
	public void exitParens(@NotNull WichParser.ParensContext ctx) {
		ctx.exprType = ctx.expr().exprType;
	}

	@Override
	public void exitIdentifier(@NotNull WichParser.IdentifierContext ctx) {
		Symbol s = currentScope.resolve(ctx.ID().getText());
		if ( s!=null && s instanceof WVariableSymbol ) {
			ctx.exprType = ((TypedSymbol) s).getType();
		} else {
			// TODO: add error here
		}
	}

	@Override
	public void exitInteger(@NotNull WichParser.IntegerContext ctx) {
		ctx.exprType = SymbolTable._int;
	}

	@Override
	public void exitFloat(@NotNull WichParser.FloatContext ctx) {
		ctx.exprType = SymbolTable._float;
	}

	@Override
	public void exitVector(@NotNull WichParser.VectorContext ctx) {
		ctx.exprType = SymbolTable._vector;
		// promote element type to fit in a vector
		int targetIndex = SymbolTable._float.getTypeIndex();
		for (ExprContext elem : ctx.expr_list().expr()) {
			TypeHelper.promote(elem, targetIndex);
		}
	}

	@Override
	public void exitString(@NotNull WichParser.StringContext ctx) {
		ctx.exprType = SymbolTable._string;
	}

	@Override
	public void exitAtom(@NotNull WichParser.AtomContext ctx) {
		ctx.exprType = ctx.primary().exprType; // bubble up primary's type to expr node
	}

	@Override
	public void exitVardef(WichParser.VardefContext ctx) {
		Symbol var = currentScope.resolve(ctx.ID().getText());
		// type inference
		if ( var!=null && var instanceof WVariableSymbol ) { // avoid cascading errors
			((TypedSymbol) var).setType(ctx.expr().exprType);
		}
	}
}
