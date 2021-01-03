package com.manimdsl.semanticanalysis

import com.manimdsl.ExitStatus
import com.manimdsl.ManimDSLParser
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
        assertEquals(ExitStatus.SEMANTIC_ERROR, semanticErrorStatus)
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
            outputStreamCaptor.toString()
                .contains(Regex("Cannot return expression of type .* in a function with return type .*"))
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
            outputStreamCaptor.toString()
                .contains(Regex(".* function does not accept .* arguments \\(expected: .*, actual: .*\\)"))
        )
    }

    @Test
    fun incorrectArgTypeForFunctionCall() {
        runSyntaxAndSemanticAnalysis("incorrectArgTypeForFunctionCall.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex(".* function does not accept argument .* of type .* \\(expected: .*, actual: .*\\)"))
        )
    }

    @Test
    fun missingReturnInFunction() {
        runSyntaxAndSemanticAnalysis("missingReturnInFunction.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Missing return statement in .* function that expects return type of .*"))
        )
    }

    @Test
    fun voidTypeDeclaration() {
        runSyntaxAndSemanticAnalysis("voidTypeDeclaration.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Cannot instantiate .* to function call that has void return type"))
        )
    }

    @Test
    fun incompatibleForwardDeclarationFunctionType() {
        runSyntaxAndSemanticAnalysis("incompatibleForwardDeclarationFunctionType.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Incompatible .* function return type of .* to previous function call expecting type .*"))
        )
    }

    @Test
    fun incompatibleForwardDeclarationParameterCount() {
        runSyntaxAndSemanticAnalysis("incompatibleForwardDeclarationParameterCount.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Incompatible .* function with 0 parameter\\(s\\) to previous function call with 1 argument\\(s\\)"))
        )
    }

    @Test
    fun incompatibleForwardDeclarationParameterType() {
        runSyntaxAndSemanticAnalysis("incompatibleForwardDeclarationParameterType.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Incompatible parameter .* of type .* to previous function call with argument of type .*"))
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

    @Test
    fun arrayMissingConstructorArguments() {
        runSyntaxAndSemanticAnalysis("arrayMissingConstructorArguments.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex(".* constructor expects .* arguments but only found .*"))
        )
    }

    @Test
    fun arrayInvalidTypesInInitialiserCheck() {
        runSyntaxAndSemanticAnalysis("arrayInvalidTypesInInitialiserCheck.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("All values must be of type .*"))
        )
    }

    @Test
    fun arrayInvalidTypesInConstructorCheck() {
        runSyntaxAndSemanticAnalysis("arrayInvalidTypesInConstructorCheck.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("constructor method on .* does not accept argument .* of type .*"))
        )
    }

    @Test
    fun incorrectNumberOfIndicesFor1DArray() {
        runSyntaxAndSemanticAnalysis("incorrectNumberOfIndicesFor1DArray.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Cannot index a .* array .* times"))
        )
    }

    @Test
    fun incorrectNumberOfIndicesFor2DArray() {
        runSyntaxAndSemanticAnalysis("incorrectNumberOfIndicesFor2DArray.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Cannot index a .* array .* times"))
        )
    }

    @Test
    fun array1DAccessIncorrectIndexType() {
        runSyntaxAndSemanticAnalysis("array1DAccessIncorrectIndexType.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Expected expression of type number but found .*"))
        )
    }

    @Test
    fun array2DAccessIncorrectIndexType() {
        runSyntaxAndSemanticAnalysis("array2DAccessIncorrectIndexType.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Expected expression of type number but found .*"))
        )
    }

    @Test
    fun stackTooManyArgumentsInConstructor() {
        runSyntaxAndSemanticAnalysis("stackTooManyArgumentsInConstructor.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("constructor method on Stack<.*> does not accept .* arguments, expects 0"))
        )
    }

    @Test
    fun binaryTreeInvalidChildAssignment() {
        runSyntaxAndSemanticAnalysis("binaryTreeInvalidChildAssignment.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Cannot assign expression of type .* to .*, which is of type Node<.*>"))
        )
    }

    @Test
    fun binaryTreeInvalidValueAssignment() {
        runSyntaxAndSemanticAnalysis("binaryTreeInvalidValueAssignment.manimdsl")
        assertTrue(
            outputStreamCaptor.toString()
                .contains(Regex("Cannot assign expression of type .* to .*, which is of type .*"))
        )
    }

    @Test
    fun invalidPrimitiveAssignedToNull() {
        runSyntaxAndSemanticAnalysis("invalidPrimitiveAssignedToNull.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Cannot assign null to .* type"))
        )
    }

    @Test
    fun unableToInferTypeFromNull() {
        runSyntaxAndSemanticAnalysis("unableToInferTypeFromNull.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Cannot infer type from null"))
        )
    }

    @Test
    fun incompatible1DArrayInitialisationWith2DValues() {
        runSyntaxAndSemanticAnalysis("array1DInitialisedAs2D.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Incompatible initialisation with .* type"))
        )
    }

    @Test
    fun arrayInitialisedValueDimensionsMustBeTheSame() {
        runSyntaxAndSemanticAnalysis("arrayInitialiseDimensionsNotTheSame.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Cannot initialise 2D array with arrays of different sizes"))
        )
    }

    @Test
    fun invalidForLoopRange() {
        runSyntaxAndSemanticAnalysis("invalidForLoopRange.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("For loop range has to be both number or both character - found start type of .* and end type of .*"))
        )
    }

    @Test
    fun nestedForLoopRedeclarationOfVariables() {
        runSyntaxAndSemanticAnalysis("nestedForLoopRedeclarationOfVariables.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex(".* of type .* is already declared"))
        )
    }

    @Test
    fun arrayConstructorItemsDoNotMatch() {
        runSyntaxAndSemanticAnalysis("incompatibleArrayConstructor.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Array not constructed correctly: size of Array< .* does not match size of > .*"))
        )
    }

    @Test
    fun arrayDimensionGreaterThanTwo() {
        runSyntaxAndSemanticAnalysis("incompatibleArrayDimension.manimdsl")
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Cannot use array with dimension .*: only 1D and 2D arrays supported"))
        )
    }

    @Test
    fun subtitlesInvalid() {
        runSyntaxAndSemanticAnalysis("subtitlesInvalid.manimdsl")
        val output = outputStreamCaptor.toString()
        assertTrue(
            output.contains(Regex("Expected expression of type .* but found .*")) &&
                outputStreamCaptor.toString().contains(Regex("Invalid arguments supplied to annotation @subtitle."))
        )
    }

    @Test
    fun stringImmutabiltiyError() {
        runSyntaxAndSemanticAnalysis("stringImmutabilityError.manimdsl")
        val output = outputStreamCaptor.toString()
        assertTrue(
            output.contains(Regex("Cannot perform array access editing on .* of type string as they are immutable"))
        )
    }

    private fun runSyntaxAndSemanticAnalysis(fileName: String) {
        val inputFile = File("$semanticErrorFilePath/$fileName")
        val parser = ManimDSLParser(inputFile.inputStream())
        val program = parser.parseFile().second
        parser.convertToAst(program)
    }
}
