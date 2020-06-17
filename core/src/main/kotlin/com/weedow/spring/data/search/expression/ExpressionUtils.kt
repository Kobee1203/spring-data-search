package com.weedow.spring.data.search.expression

import com.weedow.spring.data.search.field.FieldInfo

/**
 * [Expression] utility methods.
 */
object ExpressionUtils {
    /**
     * Create an [Expression] to test whether the field value is equals to the given [value].
     *
     * @param fieldInfo Field information
     * @param value value to test
     */
    fun equals(fieldInfo: FieldInfo, value: Any): Expression {
        return SimpleExpression(Operator.EQUALS, fieldInfo, value)
    }

    /**
     * Create an [Expression] to test whether the field value contains the given [value].
     *
     * @param fieldInfo Field information
     * @param value value to test
     */
    fun contains(fieldInfo: FieldInfo, value: Any): Expression {
        return SimpleExpression(Operator.CONTAINS, fieldInfo, value)
    }

    /**
     * Create an [Expression] to test whether the field value contains the given [value], ignoring case.
     *
     * @param fieldInfo Field information
     * @param value value to test
     */
    fun icontains(fieldInfo: FieldInfo, value: Any): Expression {
        return SimpleExpression(Operator.ICONTAINS, fieldInfo, value)
    }

    /**
     * Create an [Expression] to test whether the field value is greater than the given [value].
     *
     * @param fieldInfo Field information
     * @param value value to test
     */
    fun greaterThan(fieldInfo: FieldInfo, value: Any): Expression {
        return SimpleExpression(Operator.GREATER_THAN, fieldInfo, value)
    }

    /**
     * Create an [Expression] to test whether the field value is greater or equals than the given [value].
     *
     * @param fieldInfo Field information
     * @param value value to test
     */
    fun greaterThanOrEquals(fieldInfo: FieldInfo, value: Any): Expression {
        return SimpleExpression(Operator.GREATER_THAN_OR_EQUALS, fieldInfo, value)
    }

    /**
     * Create an [Expression] to test whether the field value is less than the given [value].
     *
     * @param fieldInfo Field information
     * @param value value to test
     */
    fun lessThan(fieldInfo: FieldInfo, value: Any): Expression {
        return SimpleExpression(Operator.LESS_THAN, fieldInfo, value)
    }

    /**
     * Create an [Expression] to test whether the field value is less or equals than the given [value].
     *
     * @param fieldInfo Field information
     * @param value value to test
     */
    fun lessThanOrEquals(fieldInfo: FieldInfo, value: Any): Expression {
        return SimpleExpression(Operator.LESS_THAN_OR_EQUALS, fieldInfo, value)
    }

    /**
     * Create an [Expression] to test whether the field value is between [lowValue] and [highValue].
     *
     * @param fieldInfo Field information
     * @param lowValue field value to test
     * @param highValue field value to test
     */
    fun between(fieldInfo: FieldInfo, lowValue: Any, highValue: Any): Expression {
        return and(greaterThan(fieldInfo, lowValue), lessThan(fieldInfo, highValue))
    }

    /**
     * Create an [Expression] to test whether the field value is contained in the given list of [values].
     *
     * @param fieldInfo Field information
     * @param values value to test
     */
    fun `in`(fieldInfo: FieldInfo, values: Collection<*>): Expression {
        return SimpleExpression(Operator.IN, fieldInfo, values)
    }

    /**
     * Create a negation of the given [expression].
     */
    fun not(expression: Expression): Expression {
        return NotExpression(expression)
    }

    /**
     * Create a conjunction of the given [expressions].
     */
    fun and(vararg expressions: Expression): Expression {
        return LogicalExpression(LogicalOperator.AND, listOf(*expressions))
    }

    /**
     * Create a disjunction of the given [expressions].
     */
    fun or(vararg expressions: Expression): Expression {
        return LogicalExpression(LogicalOperator.OR, listOf(*expressions))
    }
}