package com.manimdsl.semanticanalysis

import com.manimdsl.frontend.ExitStatus
import com.manimdsl.frontend.ManimDSLParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.stream.Stream
import kotlin.streams.asStream


class InvalidSemanticTests {
    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    companion object {
        private const val semanticErrorFilePath = "src/test/testFiles/invalid/semanticErrors"

        @JvmStatic
        fun data(): Stream<Arguments> {
            return File(semanticErrorFilePath).walk().filter { it.isFile }
                .map { Arguments.of(it.path) }.asStream()
        }

        @JvmStatic
        @BeforeAll
        internal fun initialiseBinFolder() {
            File("src/test/testFiles/bin").mkdir()
        }


        @JvmStatic
        @AfterAll
        internal fun afterAll() {
            File("src/test/testFiles/bin").deleteRecursively()
        }
    }

    // To capture standard out e.g. use of println
    @BeforeEach
    internal fun setUp() {
        System.setOut(PrintStream(outputStreamCaptor))
    }

    // Reset system out to stdout
    @AfterEach
    internal fun tearDown() {
        System.setOut(standardOut)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun invalidSemanticErrorTests(fileName: String) {
        val inputFile = File(fileName)
        val parser = ManimDSLParser(inputFile.inputStream())
        val program = parser.parseFile().second
        val semanticErrorStatus = parser.convertToAst(program).exitStatus
        assertEquals(semanticErrorStatus, ExitStatus.SEMANTIC_ERROR)
    }

    @Test
    fun redeclaredVariable() {
        runSyntaxAndSemanticAnalysis("redeclaredVariable.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* of type .* is already declared"))
        )
    }

    @Test
    fun incompatibleTypesOnDeclaration() {
        runSyntaxAndSemanticAnalysis("incompatibleExplicitCast.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Cannot assign expression of type .* to .*, which is of type .*"))
        )
    }

    @Test
    fun incompatibleTypesOnAssignment() {
        runSyntaxAndSemanticAnalysis("incompatibleTypesAssignment.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Cannot assign expression of type .* to .*, which is of type .*"))
        )
    }

    @Test
    fun undeclaredVariableUsage() {
        runSyntaxAndSemanticAnalysis("undeclaredVariableUse.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* has not been declared"))
        )
    }

    @Test
    fun primitiveTypeMethodCall() {
        runSyntaxAndSemanticAnalysis("methodCallOnPrimitiveType.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* is not a data structure"))
        )
    }

    @Test
    fun incorrectMethodNameForDataStructure() {
        runSyntaxAndSemanticAnalysis("incorrectMethodName.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* does not support .* method"))
        )
    }

    @Test
    fun incorrectArgumentTypesForDataStructureMethod() {
        runSyntaxAndSemanticAnalysis("incorrectArgTypeForPush.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* method on .* does not accept argument .* of type .*"))
        )
    }

    @Test
    fun globalReturn() {
        runSyntaxAndSemanticAnalysis("globalReturn.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains("Cannot return from global scope")
        )
    }

    @Test
    fun incompatibleReturnType() {
        runSyntaxAndSemanticAnalysis("incompatibleReturnType.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Cannot return expression of type .* in a function with return type .*"))
        )
    }

    @Test
    fun redeclaredParameters() {
        runSyntaxAndSemanticAnalysis("redeclaredParameterInFunction.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* of type .* is already declared"))
        )
    }

    @Test
    fun undeclaredFunctionCall() {
        runSyntaxAndSemanticAnalysis("undeclaredFunctionCall.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* has not been declared"))
        )
    }

    @Test
    fun incorrectArgNumForFunctionCall() {
        runSyntaxAndSemanticAnalysis("incorrectArgNumForFunctionCall.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* function does not accept .* arguments \\(expected: .*, actual: .*\\)"))
        )
    }

    @Test
    fun incorrectArgTypeForFunctionCall() {
        runSyntaxAndSemanticAnalysis("incorrectArgTypeForFunctionCall.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* function does not accept argument .* of type .* \\(expected: .*, actual: .*\\)"))
        )
    }

    @Test
    fun missingReturnInFunction() {
        runSyntaxAndSemanticAnalysis("missingReturnInFunction.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Missing return statement in .* function that expects return type of .*"))
        )
    }

    @Test
    fun voidTypeDeclaration() {
        runSyntaxAndSemanticAnalysis("voidTypeDeclaration.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Cannot instantiate .* to function call that has void return type"))
        )
    }

    @Test
    fun incompatibleForwardDeclarationFunctionType() {
        runSyntaxAndSemanticAnalysis("incompatibleForwardDeclarationFunctionType.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Incompatible .* function return type of .* to previous function call expecting type .*"))
        )
    }

    @Test
    fun incompatibleForwardDeclarationParameterCount() {
        runSyntaxAndSemanticAnalysis("incompatibleForwardDeclarationParameterCount.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Incompatible .* function with 0 parameter\\(s\\) to previous function call with 1 argument\\(s\\)"))
        )
    }

    @Test
    fun incompatibleForwardDeclarationParameterType() {
        runSyntaxAndSemanticAnalysis("incompatibleForwardDeclarationParameterType.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Incompatible parameter .* of type .* to previous function call with argument of type .*"))
        )
    }

    @Test
    fun incompatibleForwardDeclarationMultipleFunctionCallType() {
        runSyntaxAndSemanticAnalysis("incompatibleForwardDeclarationMultipleFunctionCallType.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Function .* called in different/incompatible ways"))
        )
    }

    @Test
    fun undeclaredFunctionForwardDeclaration() {
        runSyntaxAndSemanticAnalysis("undeclaredFunctionForwardDeclaration.manimdsl")
        assertTrue(
                outputStreamCaptor.toString().contains(Regex(".* has not been declared"))
        )
    }

    @Test
    fun incompatibleOperatorTypeError() {
        runSyntaxAndSemanticAnalysis("incompatibleOperatorTypeError.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Operator .* is not compatible with type .* and .*"))
        )
    }

    @Test
    fun incorrectScopingInIfStatement() {
        runSyntaxAndSemanticAnalysis("incorrectScopingInIfStatement.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* has not been declared"))
        )
    }

    @Test
    fun ifStatementsBranchesMustAllHaveReturns() {
        runSyntaxAndSemanticAnalysis("ifStatementsBranchesMustAllHaveReturns.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Missing return statement in .* function that expects return type of .*"))
        )
    }

    @Test
    fun ifStatementsBranchesMustAllHaveReturns2() {
        runSyntaxAndSemanticAnalysis("ifStatementsBranchesMustAllHaveReturns2.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Missing return statement in .* function that expects return type of .*"))
        )
    }


    private fun runSyntaxAndSemanticAnalysis(fileName: String) {
        val inputFile = File("$semanticErrorFilePath/$fileName")
        val parser = ManimDSLParser(inputFile.inputStream())
        val program = parser.parseFile().second
        parser.convertToAst(program)
    }


}