package com.valgolang.frontend

// Types (to be used in symbol table also)
abstract class Type : ASTNode()

// Primitive / Data structure distinction requested by code generation
sealed class PrimitiveType : Type()

object NumberType : PrimitiveType() {
    override fun toString(): String {
        return "number"
    }
}

object BoolType : PrimitiveType() {
    override fun toString(): String {
        return "boolean"
    }
}

object CharType : PrimitiveType() {
    override fun toString(): String {
        return "char"
    }
}

object StringType : PrimitiveType() {
    override fun toString(): String {
        return "string"
    }
}

// This is used to collect arguments up into method call node
data class ArgumentNode(val arguments: List<ExpressionNode>) : ASTNode()

object NullType : Type() {
    override fun toString(): String {
        return "null"
    }
}

object ErrorType : Type() {
    override fun toString(): String {
        return "error"
    }
}

object VoidType : Type() {
    override fun toString(): String {
        return "void"
    }
}
