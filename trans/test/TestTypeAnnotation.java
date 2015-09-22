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

import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.Test;
import wich.semantics.SymbolTable;
import wich.semantics.TypeHelper;

import static org.junit.Assert.assertEquals;

public class TestTypeAnnotation {

	@Test
	public void testDefineVar() throws Exception {
		String input = "var x = 1";
		String expected =
				"1:int\n";
		annotateTypeAndCheck(input, expected);
	}

	@Test
	public void testOpExpr() throws Exception {
		String input = "var x = 1+2";
		String expected =
				"1:int\n" +
				"2:int\n" +
				"+:int\n";
		annotateTypeAndCheck(input, expected);
	}

	@Test
	public void testThreeOperands() throws Exception {
		String input = "var x = 1+2*3";
		String expected =
				"1:int\n" +
				"2:int\n" +
				"+:int\n" +
				"3:int\n" +
				"*:int\n";
		annotateTypeAndCheck(input, expected);
	}

	@Test
	public void testIfStat() throws Exception {
		String input =
				"var x = 1\n" +
				"var y = 2\n" +
				"if(y > x) {\n" +
				"   var z = 3" +
				"}\n";
		String expected =
				"1:int\n" +
				"2:int\n" +
				"y:int\n" +
				"x:int\n" +
				">:boolean\n" +
				"3:int\n";
		annotateTypeAndCheck(input, expected);
	}

	@Test
	public void testWhile() throws Exception {
		String input =
				"var x = 1\n" +
				"var y = 2\n" +
				"while(y >= x) {\n" +
				"   var z = 3" +
				"}\n";
		String expected =
				"1:int\n" +
				"2:int\n" +
				"y:int\n" +
				"x:int\n" +
				">=:boolean\n" +
				"3:int\n";
		annotateTypeAndCheck(input, expected);
	}

	@Test
	public void testAssign() throws Exception {
		String input =
				"var x = 1\n" +
				"var y = 2\n" +
				"y = x\n";
		String expected =
				"1:int\n" +
				"2:int\n" +
				"x:int\n";
		annotateTypeAndCheck(input, expected);
	}

	@Test
	public void testElementAssign() throws Exception {
		String input =
				"var arr = [1, 2, 3, 4, 5]\n" +
				"arr[3] = 1\n";
		String expected =
				"[1,2,3,4,5]:[]\n" +
				"1:int => float\n" +
				"2:int => float\n" +
				"3:int => float\n" +
				"4:int => float\n" +
				"5:int => float\n" +
				"3:int\n" +
				"1:int\n";
		annotateTypeAndCheck(input, expected);
	}

	@Test
	public void testOpTypePromotion() throws Exception {
		String input = "var x = 1.2 + 1";
		String expected =
				"1.2:float\n" +
				"1:int => float\n" +
				"+:float\n";
		annotateTypeAndCheck(input, expected);
	}

	@Test
	public void testRecursion() throws Exception {
		String input =
				"func fib(x:int) : int {\n" +
				"   if ((x == 0) || (x == 1)) {\n" +
				"       return x\n" +
				"   }\n" +
				"   return fib(x-1) + fib(x-2)\n" +
				"}\n" +
				"print(fib(5))\n";
		String expected =
				"x:int\n" +
				"0:int\n" +
				"==:boolean\n" +
				"x:int\n" +
				"1:int\n" +
				"==:boolean\n" +
				"||:boolean\n" +
				"x:int\n" +
				"x:int\n" +
				"1:int\n" +
				"-:int\n" +
				"fib(x-1):int\n" +
				"x:int\n" +
				"2:int\n" +
				"-:int\n" +
				"fib(x-2):int\n" +
				"+:int\n" +
				"5:int\n" +
				"fib(5):int\n";

				annotateTypeAndCheck(input, expected);
	}

	@Test
	public void testExprInVector() throws Exception {
		String input =
				"func sum(x:int) : int {\n" +
				"   if (x <= 0)\n" +
				"       return 0\n" +
				"   return sum(x-1)+x\n" +
				"}\n" +
				"var vec = [sum(5),1,2,3+3]\n" +
				"print(vec)\n";
		String expected =
				"x:int\n" +
				"0:int\n" +
				"<=:boolean\n" +
				"0:int\n" +
				"sum(x-1):int\n" +
				"x:int\n" +
				"1:int\n" +
				"-:int\n" +
				"x:int\n" +
				"+:int\n" +
				"[sum(5),1,2,3+3]:[]\n" +
				"sum(5):int => float\n" +
				"5:int\n" +
				"1:int => float\n" +
				"2:int => float\n" +
				"3:int\n" +
				"3:int\n" +
				"+:int => float\n";
		annotateTypeAndCheck(input, expected);
	}

	@Test
	public void testParenthesisExpr() throws Exception {
		String input =
				"var x = 1 * (2 + 3)\n" +
				"var vec = [1.0,2.0,x]\n";
		String expected =
				"1:int\n" +
				"(2+3):int\n" +
				"2:int\n" +
				"3:int\n" +
				"+:int\n" +
				"*:int\n" +
				"[1.0,2.0,x]:[]\n" +
				"1.0:float\n" +
				"2.0:float\n" +
				"x:int => float\n";
		annotateTypeAndCheck(input, expected);
	}

	private String getExpressionDump(String input) {
		SymbolTable symtab = new SymbolTable();
		ParserRuleContext tree = CompilerFacade.getAnnotatedParseTree(input, symtab);
		return TypeHelper.dumpWithType(tree);
	}

	private void annotateTypeAndCheck(String input, String expected) {
		String actual = getExpressionDump(input);
		assertEquals(expected, actual);
	}
}
