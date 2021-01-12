package com.valgolang.errorhandling.semanticerror

import com.valgolang.errorhandling.ErrorHandler.addSemanticError
import com.valgolang.frontend.NullType
import com.valgolang.frontend.Type
import com.valgolang.frontend.datastructures.DataStructureType
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Error for assigning expression of type [rhsType] to variable [identifier] with type [lhsType], where [rhsType] and [lhsType] are different
 *
 * @param identifier
 * @param rhsType
 * @param lhsType
 * @param ctx
 */
fun declareAssignError(
    identifier: String,
    rhsType: Type,
    lhsType: Type,
    ctx: ParserRuleContext
) {
    addSemanticError(
        "Cannot assign expression of type $rhsType to $identifier, which is of type $lhsType",
        getErrorLinePos(ctx)
    )
}

/**
 * Inconsistent type error, used in data structures where all initialised values must be of the same type [expected]
 *
 * @param expected
 * @param ctx
 */
fun inconsistentTypeError(
    expected: Type,
    ctx: ParserRuleContext
) {
    addSemanticError(
        "All values must be of type $expected",
        getErrorLinePos(ctx)
    )
}

/**
 * TODO: Explain properly
 * Missing constructor arguments error
 *
 * @param dataStructureType
 * @param expected
 * @param actual
 * @param ctx
 */
fun missingConstructorArgumentsError(
    dataStructureType: DataStructureType,
    expected: Int,
    actual: Int,
    ctx: ParserRuleContext
) {
    addSemanticError(
        "$dataStructureType constructor expects $expected arguments but only found $actual",
        getErrorLinePos(ctx)
    )
}

/**
 * Error when data structure access performed on variable [identifier] of type [actual], where element assignment cannot be performed
 *
 * @param identifier
 * @param expected
 * @param actual
 * @param ctx
 */
fun incorrectLHSForDataStructureElem(
    identifier: String,
    expected: String,
    actual: Type,
    ctx: ParserRuleContext
) {
    addSemanticError(
        "$identifier is not an $expected, actual type is $actual. Cannot perform element assignment on $identifier",
        getErrorLinePos(ctx)
    )
}

/**
 * Variable redeclaration error
 *
 * @param variable
 * @param variableType
 * @param ctx
 */
fun redeclarationError(
    variable: String,
    variableType: Type,
    ctx: ParserRuleContext
) {
    addSemanticError("$variable of type $variableType is already declared", getErrorLinePos(ctx))
}

/**
 * Error when value assigned to variable [identifier] that has not yet been declared
 *
 * @param identifier
 * @param ctx
 */
fun undeclaredAssignError(
    identifier: String,
    ctx: ParserRuleContext
) {
    addSemanticError("$identifier has not been declared", getErrorLinePos(ctx))
}

/**
 * Error when data structure method applied to variable [identifier] which is not a data structure
 *
 * @param identifier
 * @param ctx
 */
fun nonDataStructureMethodError(identifier: String, ctx: ParserRuleContext) {
    addSemanticError("$identifier is not a data structure", getErrorLinePos(ctx))
}

/**
 * Data structure internal type [internalType] not primitive error
 *
 * @param internalType
 * @param ctx
 */
fun dataStructureInternalTypeNotPrimitiveError(internalType: Type, ctx: ParserRuleContext) {
    addSemanticError("Data structure internal type must be primitive. Type given: $internalType", getErrorLinePos(ctx))
}

/**
 * Error when operator [operator] is incompatible with types [expr1Type] and [expr2Type]
 *
 * @param operator
 * @param expr1Type
 * @param expr2Type
 * @param ctx
 */
fun incompatibleOperatorTypeError(operator: String, expr1Type: Type, expr2Type: Type? = null, ctx: ParserRuleContext) {
    val errorMessage =
        "Operator \'$operator\' is not compatible with type $expr1Type${if (expr2Type != null) " and $expr2Type" else ""}"

    addSemanticError(errorMessage, getErrorLinePos(ctx))
}

/**
 * Unexpected expression type error, where [actual] is not part of the expected types in [expected]
 *
 * @param expected
 * @param actual
 * @param ctx
 */
fun unexpectedExpressionTypeError(expected: Set<Type>, actual: Type, ctx: ParserRuleContext) {
    val errorMessage =
        "Expected expression of type ${expected.joinToString(separator = " or ")} but found $actual"

    addSemanticError(errorMessage, getErrorLinePos(ctx))
}

/**
 * Error when [method] other than those specified for [dataStructureType] is applied
 *
 * @param dataStructureType
 * @param method
 * @param ctx
 */
fun unsupportedMethodError(dataStructureType: String, method: String, ctx: ParserRuleContext) {
    addSemanticError("$dataStructureType does not support $method method", getErrorLinePos(ctx))
}

/**
 * Error when trying to edit element in string as they are immutable
 *
 * @param varName
 * @param ctx
 */
fun stringImmutabilityError(varName: String, ctx: ParserRuleContext) {
    addSemanticError(
        "Cannot perform array access editing on $varName of type string as they are immutable",
        getErrorLinePos(ctx)
    )
}

/**
 * Incorrect number of arguments [numArgs] specified when calling [method] on a variable of type [dataStructureType]
 *
 * @param dataStructureType
 * @param method
 * @param numArgs
 * @param expected
 * @param ctx
 */
fun numOfArgsInMethodCallError(
    dataStructureType: String,
    method: String,
    numArgs: Int,
    expected: Int,
    ctx: ParserRuleContext
) {
    // To modify once nex version is merged
    addSemanticError(
        "$method method on $dataStructureType does not accept $numArgs arguments, expects $expected",
        getErrorLinePos(ctx)
    )
}

/**
 * Incorrect type [argType] of argument [argName] specified when calling [method] on a variable of type [dataStructureType]
 *
 * @param dataStructureType
 * @param method
 * @param argType
 * @param argName
 * @param ctx
 */
fun typeOfArgsInMethodCallError(
    dataStructureType: String,
    method: String,
    argType: String,
    argName: String,
    ctx: ParserRuleContext
) {
    // To modify once nex version is merged
    addSemanticError(
        "$method method on $dataStructureType does not accept argument $argName of type $argType",
        getErrorLinePos(ctx)
    )
}

/**
 * Global return error
 *
 * @param ctx
 */
fun globalReturnError(ctx: ParserRuleContext) {
    addSemanticError("Cannot return from global scope", getErrorLinePos(ctx))
}

/**
 * Break or continue outside loop error
 *
 * @param action
 * @param ctx
 */
fun breakOrContinueOutsideLoopError(action: String, ctx: ParserRuleContext) {
    addSemanticError("$action cannot occur outside loop", getErrorLinePos(ctx))
}

/**
 * Return type error, where [type] does not match [expectedType]
 *
 * @param type
 * @param expectedType
 * @param ctx
 */
fun returnTypeError(type: String, expectedType: String, ctx: ParserRuleContext) {
    addSemanticError(
        "Cannot return expression of type $type in a function with return type $expectedType",
        getErrorLinePos(ctx)
    )
}

/**
 * Incorrect number of arguments [numArgs] specified when calling [function]
 *
 * @param function
 * @param numArgs
 * @param expected
 * @param ctx
 */
fun numOfArgsInFunctionCallError(function: String, numArgs: Int, expected: Int, ctx: ParserRuleContext) {
    addSemanticError(
        "$function function does not accept $numArgs arguments (expected: $expected, actual: $numArgs)",
        getErrorLinePos(ctx)
    )
}

/**
 * Incorrect type [argType] of argument [argName] specified when calling [function]
 *
 * @param function
 * @param argType
 * @param argName
 * @param expected
 * @param ctx
 */
fun typeOfArgsInFunctionCallError(
    function: String,
    argType: String,
    argName: String,
    expected: String,
    ctx: ParserRuleContext
) {
    // To modify once nex version is merged
    addSemanticError(
        "$function function does not accept argument $argName of type $argType (expected: $expected, actual: $argType)",
        getErrorLinePos(ctx)
    )
}

/**
 * Missing return error in [function]
 *
 * @param function
 * @param type
 * @param ctx
 */
fun missingReturnError(function: String, type: String, ctx: ParserRuleContext) {
    addSemanticError(
        "Missing return statement in $function function that expects return type of $type",
        getErrorLinePos(ctx)
    )
}

/**
 * Void type declaration error with variable [identifier]
 *
 * @param identifier
 * @param ctx
 */
fun voidTypeDeclarationError(identifier: String, ctx: ParserRuleContext) {
    addSemanticError("Cannot instantiate $identifier to function call that has void return type", getErrorLinePos(ctx))
}

/**
 * Incompatible function type error, where [declared] and [called] do not match
 *
 * @param identifier
 * @param declared
 * @param called
 * @param ctx
 */
fun incompatibleFunctionType(identifier: String, declared: String, called: String, ctx: ParserRuleContext) {
    addSemanticError(
        "Incompatible $identifier function return type of $declared to previous function call expecting type $called",
        getErrorLinePos(ctx)
    )
}

/**
 * Incompatible parameter count error, where [declared] and [called] do not match
 *
 * @param identifier
 * @param declared
 * @param called
 * @param ctx
 */
fun incompatibleParameterCount(identifier: String, declared: Int, called: Int, ctx: ParserRuleContext) {
    addSemanticError(
        "Incompatible $identifier function with $declared parameter(s) to previous function call with $called argument(s)",
        getErrorLinePos(ctx)
    )
}

/**
 * Incompatible parameter type error, where [declared] and [argument] do not match
 *
 * @param identifier
 * @param declared
 * @param argument
 * @param ctx
 */
fun incompatibleParameterType(identifier: String, declared: String, argument: String, ctx: ParserRuleContext) {
    addSemanticError(
        "Incompatible parameter $identifier of type $declared to previous function call with argument of type $argument",
        getErrorLinePos(ctx)
    )
}

/**
 * Incompatible type from multiple function calls error
 *
 * @param identifier
 * @param ctx
 */
fun incompatibleTypeFromMultipleFunctionCall(identifier: String, ctx: ParserRuleContext) {
    addSemanticError("Function $identifier called in different/incompatible ways", getErrorLinePos(ctx))
}

/**
 * Non nullable [type] assigned to null type
 *
 * @param type
 * @param ctx
 */
fun nonNullableAssignedToNull(type: String, ctx: ParserRuleContext) {
    addSemanticError("Cannot assign $NullType to a $type type", getErrorLinePos(ctx))
}

/**
 * Unable to infer type from null type
 *
 * @param ctx
 */
fun unableToInferTypeFromNullType(ctx: ParserRuleContext) {
    addSemanticError("Cannot infer type from $NullType", getErrorLinePos(ctx))
}

/**
 * Max array indexing exceeded error
 *
 * @param is2DArray
 * @param indicesSize
 * @param ctx
 */
fun maxArrayIndexingExceededError(is2DArray: Boolean, indicesSize: Int, ctx: ParserRuleContext) {
    addSemanticError("Cannot index a ${if (is2DArray) "2D" else "1D"} array $indicesSize times", getErrorLinePos(ctx))
}

/**
 * Incorrect constructor item size, where [openConstructorSize] and [closeConstructorSize] do not match
 *
 * @param openConstructorSize
 * @param closeConstructorSize
 * @param ctx
 */
fun incorrectConstructorItemSize(openConstructorSize: Int, closeConstructorSize: Int, ctx: ParserRuleContext) {
    addSemanticError(
        "Array not constructed correctly: size of Array< ($openConstructorSize) does not match size of > ($closeConstructorSize)",
        getErrorLinePos(ctx)
    )
}

/**
 * Incompatible array dimension, where [arrayDimension] greater than 2
 *
 * @param arrayDimension
 * @param ctx
 */
fun incompatibleArrayDimension(arrayDimension: Int, ctx: ParserRuleContext) {
    addSemanticError(
        "Cannot use array with dimension $arrayDimension: only 1D and 2D arrays supported",
        getErrorLinePos(ctx)
    )
}

/**
 * Incompatible array dimension with constructor arguments error
 * Error thrown if:
 *   [is2D] true, [argumentsSize] != 2 or
 *   [is2D] false, [argumentsSize] != 1
 *
 * @param is2D
 * @param argumentsSize
 * @param ctx
 */
fun incompatibleArrayDimensionWithConstructorArguments(is2D: Boolean, argumentsSize: Int, ctx: ParserRuleContext) {
    addSemanticError(
        "Cannot initialise ${if (is2D) "2" else "1"}D array with $argumentsSize constructor arguments",
        getErrorLinePos(ctx)
    )
}

/**
 * Incompatible data structure initialisation
 *
 * @param dataStructureType
 * @param ctx
 */
fun incompatibleDataStructureInitialisation(dataStructureType: String, ctx: ParserRuleContext) {
    addSemanticError("Incompatible initialisation with $dataStructureType type", getErrorLinePos(ctx))
}

/**
 * 2D array dimension error
 *
 * @param ctx
 */
fun array2DDimensionError(ctx: ParserRuleContext) {
    addSemanticError("Cannot initialise 2D array with arrays of different sizes", getErrorLinePos(ctx))
}

/**
 * For loop range expressions not number or char
 *
 * @param startType
 * @param endType
 * @param ctx
 */
fun forLoopRangeNotNumberOrChar(startType: String, endType: String, ctx: ParserRuleContext) {
    addSemanticError(
        "For loop range has to be both number or both character - found start type of $startType and end type of $endType",
        getErrorLinePos(ctx)
    )
}

/**
 * For loop range update expression not number
 *
 * @param actual
 * @param ctx
 */
fun forLoopRangeUpdateNotNumber(actual: String, ctx: ParserRuleContext) {
    addSemanticError(
        "For loop range update value of type $actual - only numerical update value allowed",
        getErrorLinePos(ctx)
    )
}

/**
 * For loop identifier being reassigned error
 *
 * @param identifier
 * @param ctx
 */
fun forLoopIdentifierBeingReassignedError(identifier: String, ctx: ParserRuleContext) {
    addSemanticError("Cannot reassign to variable $identifier being used in for loop header", getErrorLinePos(ctx))
}

/**
 * Invalid arguments for annotation error
 *
 * @param annotation
 * @param ctx
 */
fun invalidArgumentsForAnnotationError(annotation: String, ctx: ParserRuleContext) {
    addSemanticError("Invalid arguments supplied to annotation $annotation.", getErrorLinePos(ctx))
}

/**
 * Helper function that returns line and character position for errors
 *
 * @param ctx: [ParserRuleContext] needed to access line and character position of error
 * @return line number and character position in a single string separated by :
 */
private fun getErrorLinePos(ctx: ParserRuleContext): String {
    val line = ctx.getStart().line
    val pos = ctx.getStart().charPositionInLine
    return "$line:$pos"
}
