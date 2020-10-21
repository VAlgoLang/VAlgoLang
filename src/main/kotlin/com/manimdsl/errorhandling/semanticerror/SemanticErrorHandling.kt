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

fun incompatibleOperatorTypeError(operator: String, expr1Type: Type, expr2Type: Type? = null, ctx: ParserRuleContext) {
    val errorMessage =
        "Operator \'$operator\' is not compatible with types $expr1Type${if (expr2Type != null) " and $expr2Type" else ""}"

    addSemanticError(errorMessage, getErrorLinePos(ctx))
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

/* Helper function that returns line and character position for errors */
private fun getErrorLinePos(ctx: ParserRuleContext): String {
    val line = ctx.getStart().line
    val pos = ctx.getStart().charPositionInLine
    return "$line:$pos"
}
