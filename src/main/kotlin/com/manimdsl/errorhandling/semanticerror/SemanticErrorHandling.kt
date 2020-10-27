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

fun incompatibleOperatorTypeError(operator: String, expr1Type: Type, expr2Type: Type? = null, ctx: ParserRuleContext) {
    val errorMessage =
        "Operator \'$operator\' is not compatible with type $expr1Type${if (expr2Type != null) " and $expr2Type" else ""}"

    addSemanticError(errorMessage, getErrorLinePos(ctx))
}

fun unexpectedExpressionTypeError(expected: Type, actual: Type, ctx: ParserRuleContext) {
    val errorMessage =
        "Expected expression of type $expected but found $actual"

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

fun returnTypeError(type: String, expectedType: String, ctx: ParserRuleContext) {
    addSemanticError("Cannot return expression of type $type in a function with return type $expectedType", getErrorLinePos(ctx))
}

fun numOfArgsInFunctionCallError(function: String, numArgs: Int, expected:Int, ctx: ParserRuleContext) {
    addSemanticError("$function function does not accept $numArgs arguments (expected: $expected, actual: $numArgs)", getErrorLinePos(ctx))
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
    addSemanticError("Missing return statement in $function function that expects return type of $type", getErrorLinePos(ctx))
}

fun voidTypeDeclarationError(identifier: String, ctx: ParserRuleContext) {
    addSemanticError("Cannot instantiate $identifier to function call that has void return type", getErrorLinePos(ctx))
}

fun incompatibleFunctionType(identifier: String, declared: String, called: String, ctx: ParserRuleContext) {
    addSemanticError("Incompatible $identifier function return type of $declared to previous function call expecting type $called", getErrorLinePos(ctx))
}

fun incompatibleParameterCount(identifier: String, declared: Int, called: Int, ctx: ParserRuleContext) {
    addSemanticError("Incompatible $identifier function with $declared parameter(s) to previous function call with $called argument(s)", getErrorLinePos(ctx))
}

fun incompatibleParameterType(identifier: String, declared: String, argument: String, ctx: ParserRuleContext) {
    addSemanticError("Incompatible parameter $identifier of type $declared to previous function call with argument of type $argument", getErrorLinePos(ctx))
}

fun incompatibleTypeFromMultipleFunctionCall(identifier: String, ctx: ParserRuleContext) {
    addSemanticError("Function $identifier called in different/incompatible ways", getErrorLinePos(ctx))
}

/* Helper function that returns line and character position for errors */
private fun getErrorLinePos(ctx: ParserRuleContext): String {
    val line = ctx.getStart().line
    val pos = ctx.getStart().charPositionInLine
    return "$line:$pos"
}
