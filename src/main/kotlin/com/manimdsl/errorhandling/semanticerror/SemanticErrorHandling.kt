package com.manimdsl.errorhandling.semanticerror

import com.manimdsl.errorhandling.ErrorHandler.addSemanticError
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

fun unsupportedMethodError(dataStructureType: String, method: String, ctx: ParserRuleContext) {
    addSemanticError("$dataStructureType does not support $method method", getErrorLinePos(ctx))
}

fun numOfArgsInMethodCallError(dataStructureType: String, method: String, numArgs: Int, ctx: ParserRuleContext) {
    // To modify once nex version is merged
    addSemanticError("$method method on $dataStructureType does not accept $numArgs arguments", getErrorLinePos(ctx))
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
    addSemanticError("Cannot declare $identifier to function call that has void return type", getErrorLinePos(ctx))
}

/* Helper function that returns line and character position for errors */
private fun getErrorLinePos(ctx: ParserRuleContext): String {
    val line = ctx.getStart().line
    val pos = ctx.getStart().charPositionInLine
    return "$line:$pos"
}
