package wich.semantics;

import org.antlr.symtab.Scope;
import org.antlr.symtab.Symbol;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.misc.NotNull;
import wich.errors.ErrorType;
import wich.errors.WichErrorHandler;
import wich.parser.WichBaseListener;

import static wich.errors.ErrorType.SYMBOL_NOT_FOUND;

public class CommonWichListener extends WichBaseListener {
	protected final WichErrorHandler errorHandler;

	protected Scope currentScope;

	protected void pushScope(Scope s) {
		currentScope = s;
	}

	public CommonWichListener(WichErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	protected void popScope() {
		if (currentScope != null) {
			currentScope = currentScope.getEnclosingScope();
		}
	}

	public Type resolveType(@NotNull String typeName) {
		Symbol typeSymbol = currentScope.resolve(typeName);
		if ( typeSymbol instanceof Type ) {
			return (Type)typeSymbol;
		}
		else {
			error(SYMBOL_NOT_FOUND, typeName);
			return null;
		}
	}

	// error support

	protected void error(ErrorType type, String... args) {
		errorHandler.aggregate(type, args);
	}

	protected void error(ErrorType type, Exception e, String... args) {
		errorHandler.aggregate(type, e, args);
	}

	public int getErrorNum() {
		return errorHandler.getErrorNum();
	}

	public String getErrorMessages() {
		return errorHandler.toString();
	}
}