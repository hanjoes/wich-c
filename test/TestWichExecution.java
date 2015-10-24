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

import junit.framework.Assert;
import org.antlr.v4.runtime.misc.Triple;
import org.antlr.v4.runtime.misc.Utils;
import org.junit.Before;
import wich.codegen.CompilerUtils;
import wich.errors.ErrorType;
import wich.errors.WichErrorHandler;
import wich.semantics.SymbolTable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

// Assuming the program is running on Unix-like operating systems.
// Please make sure cc is on the program searching path.
public class TestWichExecution extends WichBaseTest {
	protected static final String WORKING_DIR = "/tmp/";
	protected static final String LIB_DIR = "/usr/local/wich/lib";
	protected static final String INCLUDE_DIR = "/usr/local/wich/include";
	protected CompilerUtils.MallocImpl mallocImpl = CompilerUtils.MallocImpl.SYSTEM; // default malloc;

	public TestWichExecution(File input, String baseName) {
		super(input, baseName);
	}

	@Before
	public void setUp() throws Exception {
		File dir = new File(LIB_DIR);
		if ( !dir.exists() ) {
			throw new IllegalArgumentException("Can't find wich runtime library.");
		}
	}

	protected void testCodeGen(CompilerUtils.CodeGenTarget target) throws IOException, InterruptedException {
		WichErrorHandler err = new WichErrorHandler();
		SymbolTable symtab = new SymbolTable();
		URL expectedOutputURL = null;
		switch ( target ) {
			case PLAIN :
				expectedOutputURL =
					CompilerUtils.getResourceFile(TEST_RES_PLAIN_GEND_CODE+"/"+baseName+".c");
				break;
			case REFCOUNTING :
				expectedOutputURL =
					CompilerUtils.getResourceFile(TEST_RES_REFCOUNTING_GEND_CODE+"/"+baseName+".c");
				break;
			case LLVM :
				expectedOutputURL =
						CompilerUtils.getResourceFile(TEST_RES_LLVM_GEND_CODE+"/"+baseName+".c");
				break;
			case MARK_AND_COMPACT:
			case MARK_AND_SWEEP:
				expectedOutputURL =
					CompilerUtils.getResourceFile(TEST_RES_GC_GEND_CODE+"/"+baseName+".c");
				break;
			default :
				err.error(null, ErrorType.UNKNOWN_TARGET, target.toString());
		}
		assertTrue(err.toString(), err.getErrorNum()==0);
		assertNotNull(expectedOutputURL);
		String expPath = expectedOutputURL.getPath();
		String expected = CompilerUtils.readFile(expPath, CompilerUtils.FILE_ENCODING);
		expected = expected.replace("\n\n", "\n"); // strip blank lines
		CompilerUtils.writeFile("/tmp/__expected.c", expected, StandardCharsets.UTF_8);

		String wichInput = CompilerUtils.readFile(input.getAbsolutePath(), CompilerUtils.FILE_ENCODING);
		String actual = CompilerUtils.genCode(wichInput, symtab, err, target);
		assertTrue(err.toString(), err.getErrorNum()==0);
		actual = actual.replace("\n\n", "\n");
		CompilerUtils.writeFile("/tmp/__t.c", actual, StandardCharsets.UTF_8);

		// normalize the file using gnu indent (brew install gnu-indent on OS X)
		exec(
			new String[] {
				"gindent",
				"-bap", "-bad", "-br", "-nce", "-ncs", "-nprs", "-npcs", "-sai", "-saw",
				"-di1", "-brs", "-blf", "--indent-level4", "-nut", "-sob", "-l200",
				"/tmp/__t.c"
			}
		);
		actual = CompilerUtils.readFile("/tmp/__t.c", StandardCharsets.UTF_8);
//		System.out.println("NORMALIZED\n"+actual);

		// format the expected file as well
		exec(
			new String[] {
				"gindent",
				"-bap", "-bad", "-br", "-nce", "-ncs", "-nprs", "-npcs", "-sai", "-saw",
				"-di1", "-brs", "-blf", "--indent-level4", "-nut", "-sob", "-l200",
				"/tmp/__expected.c"
			}
		);
		expected = CompilerUtils.readFile("/tmp/__expected.c", StandardCharsets.UTF_8);

		Assert.assertEquals(expected, actual);
	}

	protected void executeAndCheck(String wichFileName,
								   String expected,
								   boolean valgrind,
								   CompilerUtils.CodeGenTarget target)
		throws IOException, InterruptedException
	{
		String executable = compileC(wichFileName, target);
		String output = executeC(executable);
		System.out.println(output);
		assertEquals(expected, output);
		if ( valgrind ) {
			valgrindCheck(executable);
		}
	}

	protected void valgrindCheck(String executable) throws IOException, InterruptedException {
		// For Intellij users you need to set PATH environment variable in Run/Debug configuration,
		// since Intellij doesn't inherit environment variables from system.
		String errSummary = exec(new String[]{"valgrind", executable}).c;
		assertEquals("Valgrind memcheck failed...", 0, getErrorNumFromSummary(errSummary));
	}

	protected int getErrorNumFromSummary(String errSummary) {
		if (errSummary == null || errSummary.length() == 0) return -1;
		String[] lines = errSummary.split("\n");
		//Sample: ==15358== ERROR SUMMARY: 0 errors from 0 contexts (suppressed: 0 from 0)
		String summary = lines[lines.length-1];
		return Integer.parseInt(summary.substring(summary.indexOf(":") + 1, summary.lastIndexOf("errors")).trim());
	}

	protected String compileC(String wichInputFilename, CompilerUtils.CodeGenTarget target)
		throws IOException, InterruptedException
	{
		// Translate to C file.
		SymbolTable symtab = new SymbolTable();
		WichErrorHandler err = new WichErrorHandler();
		String wichInput = CompilerUtils.readFile(wichInputFilename, CompilerUtils.FILE_ENCODING);
		String actual = CompilerUtils.genCode(wichInput, symtab, err, target);
		assertTrue(err.toString(), err.getErrorNum()==0);
		String generatedFileName = WORKING_DIR + baseName + ".c";
		CompilerUtils.writeFile(generatedFileName, actual, StandardCharsets.UTF_8);
		// Compile C code and return the path to the executable.
		String executable = "./" + baseName;
		File execF = new File(executable);
		if ( execF.exists() ) {
			execF.delete();
		}
		List<String> cc = new ArrayList<>();
		cc.addAll(
			Arrays.asList(
				"cc", "-g", "-o", executable,
				generatedFileName,
				"-L", LIB_DIR,
				"-D" + target.flag,
				"-I", INCLUDE_DIR, "-std=c99", "-O0"
		    )
		);
		for (String lib : target.libs) {
			cc.add("-l"+lib);
		}
		String[] cmd = cc.toArray(new String[cc.size()]);
		if ( mallocImpl!=CompilerUtils.MallocImpl.SYSTEM ) {
			cc.addAll(Arrays.asList("-l"+mallocImpl.lib, "-lmalloc_common"));
		}
		final Triple<Integer, String, String> result = exec(cmd);
		String cmdS = Utils.join(cmd, " ");
		System.out.println(cmdS);
		if ( result.a!=0 ) {
			throw new RuntimeException("failed compilation of "+generatedFileName+" with result code "+result.a+
									   " from\n"+
			                           cmdS+"\nstderr:\n"+result.c);
		}
//		System.out.println(result.c);
		return executable;
	}

	protected Triple<Integer, String, String> exec(String[] cmd) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(Arrays.asList(cmd)).directory(new File(WORKING_DIR));
		Process process = pb.start();
		int resultCode = process.waitFor();
		String stdout = dump(process.getInputStream());
		String stderr = dump(process.getErrorStream());
		Triple<Integer, String, String> ret = new Triple<>(resultCode, stdout, stderr);
		return ret;
	}

	protected String dump(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		StringBuilder out = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			out.append(line);
			out.append(System.getProperty("line.separator"));
		}
		return out.toString();
	}

	protected String executeC(String executable) throws IOException, InterruptedException {
		Triple<Integer, String, String> result = exec(new String[]{"./"+executable});
		if ( result.a!=0 ) {
			throw new RuntimeException("failed execution of "+executable+" with result code "+result.a+"; stderr:\n"+result.c);
		}
		return result.b;
	}
}
