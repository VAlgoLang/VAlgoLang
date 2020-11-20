package com.manimdsl.errorhandling.semanticerror

import com.manimdsl.errorhandling.ErrorHandler.addSemanticError
import com.manimdsl.frontend.DataStructureType
import com.manimdsl.frontend.Type
import org.antlr.v4.runtime.ParserRuleContext

fun declareAssignError(
    identifier: String, rhsType: Type, lhsType: Type,
    ctx: ParserRuleContext
) {
    addSemanticError(
        "Cannot assign expression of type $rhsType to $identifier, which is of type $lhsType",
        getErrorLinePos(ctx)
    )
}

fun inconsistentTypeError(
    expected: Type,
    ctx: ParserRuleContext
) {
    addSemanticError(
        "All values must be of type $expected",
        getErrorLinePos(ctx)
    )
}

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


fun redeclarationError(
    variable: String, variableType: Type,
    ctx: ParserRuleContext
) {
    addSemanticError("$variable of type $variableType is already declared", getErrorLinePos(ctx))
}

fun undeclaredAssignError(
    variable: String,
    ctx: ParserRuleContext
) {
    addSemanticError("$variable has not been declared", getErrorLinePos(ctx))
}

fun nonDataStructureMethodError(identifier: String, ctx: ParserRuleContext) {
    addSemanticError("$identifier is not a data structure", getErrorLinePos(ctx))
}

fun dataStructureInternalTypeNotPrimitiveError(internalType: Type, ctx: ParserRuleContext) {
    addSemanticError("Data structure internal type must be primitive. Type given: $internalType", getErrorLinePos(ctx))
}

fun incompatibleOperatorTypeError(operator: String, expr1Type: Type, expr2Type: Type? = null, ctx: ParserRuleContext) {
    val errorMessage =
        "Operator \'$operator\' is not compatible with type $expr1Type${if (expr2Type != null) " and $expr2Type" else ""}"

    addSemanticError(errorMessage, getErrorLinePos(ctx))
}

fun unexpectedExpressionTypeError(expected: Set<Type>, actual: Type, ctx: ParserRuleContext) {
    val errorMessage =
        "Expected expression of type ${expected.joinToString(separator = " or ")} but found $actual"

    addSemanticError(errorMessage, getErrorLinePos(ctx))
}

fun unsupportedMethodError(dataStructureType: String, method: String, ctx: ParserRuleContext) {
    addSemanticError("$dataStructureType does not support $method method", getErrorLinePos(ctx))
}

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

fun globalReturnError(ctx: ParserRuleContext) {
    addSemanticError("Cannot return from global scope", getErrorLinePos(ctx))
}

fun breakOrContinueOutsideLoopError(action: String, ctx: ParserRuleContext) {
    addSemanticError("$action cannot occur outside loop", getErrorLinePos(ctx))
}

fun returnTypeError(type: String, expectedType: String, ctx: ParserRuleContext) {
    addSemanticError(
        "Cannot return expression of type $type in a function with return type $expectedType",
        getErrorLinePos(ctx)
    )
}

fun numOfArgsInFunctionCallError(function: String, numArgs: Int, expected: Int, ctx: ParserRuleContext) {
    addSemanticError(
        "$function function does not accept $numArgs arguments (expected: $expected, actual: $numArgs)",
        getErrorLinePos(ctx)
    )
}

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

fun missingReturnError(function: String, type: String, ctx: ParserRuleContext) {
    addSemanticError(
        "Missing return statement in $function function that expects return type of $type",
        getErrorLinePos(ctx)
    )
}

fun voidTypeDeclarationError(identifier: String, ctx: ParserRuleContext) {
    addSemanticError("Cannot instantiate $identifier to function call that has void return type", getErrorLinePos(ctx))
}

fun incompatibleFunctionType(identifier: String, declared: String, called: String, ctx: ParserRuleContext) {
    addSemanticError(
        "Incompatible $identifier function return type of $declared to previous function call expecting type $called",
        getErrorLinePos(ctx)
    )
}

fun incompatibleParameterCount(identifier: String, declared: Int, called: Int, ctx: ParserRuleContext) {
    addSemanticError(
        "Incompatible $identifier function with $declared parameter(s) to previous function call with $called argument(s)",
        getErrorLinePos(ctx)
    )
}

fun incompatibleParameterType(identifier: String, declared: String, argument: String, ctx: ParserRuleContext) {
    addSemanticError(
        "Incompatible parameter $identifier of type $declared to previous function call with argument of type $argument",
        getErrorLinePos(ctx)
    )
}

fun incompatibleTypeFromMultipleFunctionCall(identifier: String, ctx: ParserRuleContext) {
    addSemanticError("Function $identifier called in different/incompatible ways", getErrorLinePos(ctx))
}

fun nonNullableAssignedToNull(nullType: String, type: String, ctx: ParserRuleContext) {
    addSemanticError("Cannot assign $nullType to a $type type", getErrorLinePos(ctx))
}

fun unableToInferType(nullType: String, ctx: ParserRuleContext) {
    addSemanticError("Cannot infer type from $nullType", getErrorLinePos(ctx))
}

fun maxArrayIndexingExceededError(is2DArray: Boolean, indicesSize: Int, ctx: ParserRuleContext) {
    addSemanticError("Cannot index a ${if (is2DArray) "2D" else "1D"} array $indicesSize times", getErrorLinePos(ctx))
}

fun incorrectConstructorItemSize(openConstructorSize: Int, closeConstructorSize: Int, ctx: ParserRuleContext) {
    addSemanticError("Array not constructed correctly: size of Array< ($openConstructorSize) does not match size of > ($closeConstructorSize)", getErrorLinePos(ctx))
}

fun incompatibleArrayDimension(arrayDimension: Int, ctx: ParserRuleContext) {
    addSemanticError("Cannot use array with dimension $arrayDimension: only 1D and 2D arrays supported", getErrorLinePos(ctx))
}

fun incompatibleArrayDimensionWithConstructorArguments(is2D: Boolean, argumentsSize: Int, ctx: ParserRuleContext) {
    addSemanticError("Cannot initialise ${if (is2D) "2" else "1"}D array with $argumentsSize constructor arguments", getErrorLinePos(ctx))
}

fun incompatibleInitialisation(dataStructureType: String, ctx: ParserRuleContext) {
    addSemanticError("Incompatible initialisation with $dataStructureType type", getErrorLinePos(ctx))
}

fun array2DDimensionError(ctx: ParserRuleContext) {
    addSemanticError("Cannot initialise 2D array with arrays of different sizes", getErrorLinePos(ctx))
}

fun forLoopRangeNotNumberOrChar(startType: String, endType: String, ctx: ParserRuleContext) {
    addSemanticError("For loop range has to be both number or both character - found start type of $startType and end type of $endType", getErrorLinePos(ctx))
}

fun forLoopRangeUpdateNotNumber(actual: String, ctx: ParserRuleContext) {
    addSemanticError("For loop range update value of type $actual - only numerical update value allowed", getErrorLinePos(ctx))
}

fun forLoopIdentifierBeingReassignedError(identifier: String, ctx: ParserRuleContext) {
    addSemanticError("Cannot reassign to variable $identifier being used in for loop header", getErrorLinePos(ctx))
}

fun invalidArgumentsForAnnotationError(annotation: String, ctx: ParserRuleContext) {
    addSemanticError("Invalid arguments supplied to annotation ${annotation}.", getErrorLinePos(ctx))
}

/* Helper function that returns line and character position for errors */
private fun getErrorLinePos(ctx: ParserRuleContext): String {
    val line = ctx.getStart().line
    val pos = ctx.getStart().charPositionInLine
    return "$line:$pos"
}
