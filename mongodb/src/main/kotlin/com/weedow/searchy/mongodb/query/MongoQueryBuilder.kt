package com.weedow.searchy.mongodb.query

import com.querydsl.core.JoinType
import com.querydsl.core.types.*
import com.querydsl.core.types.dsl.CollectionPathBase
import com.querydsl.core.types.dsl.DateExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.TimeExpression
import com.weedow.searchy.context.SearchyContext
import com.weedow.searchy.mongodb.query.querytype.PathWrapper
import com.weedow.searchy.mongodb.query.querytype.QEntityJoinImpl
import com.weedow.searchy.mongodb.utils.JSResourceUtils
import com.weedow.searchy.query.QueryBuilder
import com.weedow.searchy.query.querytype.*
import com.weedow.searchy.utils.Keyword
import org.bson.BsonJavaScript
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.data.mongodb.repository.support.SpringDataMongodbQuery
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*


/**
 * MongoDB [QueryBuilder] implementation.
 *
 * @param searchyContext [SearchyContext]
 * @param query [SpringDataMongodbQuery]
 * @param qEntityRoot [QEntityRoot]
 */
class MongoQueryBuilder<T>(
    private val searchyContext: SearchyContext,
    private val query: SpringDataMongodbQuery<T>,
    override val qEntityRoot: QEntityRoot<T>
) : QueryBuilder<T> {

    companion object {
        val MAP_CONTAINS_VALUE_JS: String = JSResourceUtils.load(DefaultResourceLoader().getResource("classpath:map_contains_value.js"))

        val DEFAULT_TIME_ZONE: TimeZone = TimeZone.getDefault()
    }

    override fun distinct() {
        query.distinct()
    }

    override fun join(qPath: QPath<*>, joinType: JoinType, fetched: Boolean): QEntityJoin<*> {
        val propertyInfos = qPath.propertyInfos
        val elementType = propertyInfos.elementType

        val joinAnnotation = propertyInfos.annotations.firstOrNull { searchyContext.isJoinAnnotation(it.annotationClass.java) }

        val aliasType = when (elementType) {
            ElementType.SET,
            ElementType.LIST,
            ElementType.COLLECTION,
            ElementType.ARRAY
            -> {
                propertyInfos.parameterizedTypes[0]
            }
            ElementType.MAP -> propertyInfos.parameterizedTypes[1]
            ElementType.ENTITY -> propertyInfos.type
            else -> if (joinAnnotation == null) propertyInfos.type else throw IllegalArgumentException("Could not identify the alias type for the QPath of type '$elementType': $qPath")
        }

        if (joinAnnotation != null) {
            val alias = createAlias(aliasType, qPath)

            val path = qPath.path
            val join = join(elementType, path, alias)

            return QEntityJoinImpl(join, propertyInfos)
        }

        val qEntity = createQEntity(aliasType, qPath)
        return QEntityJoinImpl(qEntity, propertyInfos)
    }

    private fun <E> createQEntity(entityClass: Class<E>, qPath: QPath<*>): QEntity<E> {
        return searchyContext.get(entityClass) { entityClazz ->
            QEntityImpl(searchyContext, entityClazz, qPath.path.metadata)
        }
    }

    private fun <E> createAlias(aliasType: Class<E>, qPath: QPath<*>): QEntity<E> {
        return searchyContext.get(aliasType) { entityClazz ->
            QEntityAliasImpl(entityClazz, qPath.propertyInfos.fieldName)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E> join(elementType: ElementType, path: Path<*>, alias: QEntity<E>): QEntity<*> {
        val join = when (elementType) {
            ElementType.SET,
            ElementType.LIST,
            ElementType.COLLECTION,
            -> query.join(path as CollectionPathBase<*, E, *>, alias)
            else -> query.join(path as Path<E>, alias)
        }

        join.on()

        return alias
    }

    override fun and(x: Expression<Boolean>, y: Expression<Boolean>): Predicate {
        return Expressions.predicate(Ops.AND, x, y)
    }

    override fun and(vararg restrictions: Predicate): Predicate {
        return Expressions.predicate(Ops.AND, *restrictions)
    }

    override fun or(x: Expression<Boolean>, y: Expression<Boolean>): Predicate {
        return Expressions.predicate(Ops.OR, x, y)
    }

    override fun or(vararg restrictions: Predicate): Predicate {
        return Expressions.predicate(Ops.OR, *restrictions)
    }

    override fun not(restriction: Expression<Boolean>): Predicate {
        return Expressions.predicate(Ops.NOT, restriction)
    }

    override fun equal(x: Expression<*>, value: Any): Predicate {
        val expressionValue = convertValueToExpression(value)
        return when {
            x is PathWrapper<*> -> {
                when (x.elementType) {
                    ElementType.MAP_KEY -> Expressions.predicate(Ops.CONTAINS_KEY, x, expressionValue)
                    ElementType.MAP_VALUE -> {
                        var path = x.metadata.name
                        var p = x.metadata.parent
                        while (p != null && !p.metadata.isRoot) {
                            path = p.metadata.name + "." + path
                            p = p.metadata.parent
                        }
                        val func = BsonJavaScript(MAP_CONTAINS_VALUE_JS.replace("{PATH}", path).replace("{VALUE}", "\"$expressionValue\""))
                        Expressions.predicate(Ops.EQ, Expressions.stringPath("\$where"), Expressions.constant(func))
                    }
                    else -> throw IllegalArgumentException("ElementType '${x.elementType}' from the PathWrapper is not supported")
                }
            }
            isCurrentDate(expressionValue) -> {
                Expressions.predicate(Ops.BETWEEN, x, asStartOfDay(), asEndOfDay())
            }
            else -> {
                Expressions.predicate(Ops.EQ, x, expressionValue)
            }
        }
    }

    override fun isNull(x: Expression<*>): Predicate {
        val type = x.type
        return when {
            Map::class.java.isAssignableFrom(type) -> {
                Expressions.predicate(Ops.MAP_IS_EMPTY, x)
            }
            Collection::class.java.isAssignableFrom(type) -> {
                Expressions.predicate(Ops.COL_IS_EMPTY, x)
            }
            else -> {
                Expressions.predicate(Ops.IS_NULL, x)
            }
        }
    }

    override fun like(x: Expression<String>, value: String): Predicate {
        val expressionValue = Expressions.constant(value.replace("*", "%"))
        return Expressions.predicate(Ops.LIKE, x, expressionValue)
    }

    override fun ilike(x: Expression<String>, value: String): Predicate {
        val expressionValue = Expressions.constant(value.replace("*", "%").toLowerCase())
        // TODO Replace the line below by 'return Expressions.predicate(Ops.LIKE_IC, x, expressionValue)' when Ops.LIKE_IC will be supported natively by querydsl-mongodb
        // see: https://github.com/querydsl/querydsl/pull/2775
        return Expressions.predicate(Ops.MATCHES_IC, x, ExpressionUtils.likeToRegex(expressionValue))
    }

    override fun lessThan(x: Expression<*>, value: Any): Predicate {
        val expressionValue = convertValueToExpression(value)
        val exprValue = if (isCurrentDate(expressionValue)) asStartOfDay() else expressionValue
        return Expressions.predicate(Ops.LT, x, exprValue)
    }

    override fun lessThanOrEquals(x: Expression<*>, value: Any): Predicate {
        val expressionValue = convertValueToExpression(value)
        val exprValue = if (isCurrentDate(expressionValue)) asStartOfDay() else expressionValue
        return Expressions.predicate(Ops.LOE, x, exprValue)
    }

    override fun greaterThan(x: Expression<*>, value: Any): Predicate {
        val expressionValue = convertValueToExpression(value)
        val exprValue = if (isCurrentDate(expressionValue)) asEndOfDay() else expressionValue
        return Expressions.predicate(Ops.GT, x, exprValue)
    }

    override fun greaterThanOrEquals(x: Expression<*>, value: Any): Predicate {
        val expressionValue = convertValueToExpression(value)
        val exprValue = if (isCurrentDate(expressionValue)) asEndOfDay() else expressionValue
        return Expressions.predicate(Ops.GOE, x, exprValue)
    }

    override fun `in`(x: Expression<*>, values: Collection<*>): Predicate {
        return if (values.size == 1) {
            equal(x, values.iterator().next()!!)
        } else {
            val expressionValue = Expressions.constant(values)
            Expressions.predicate(Ops.IN, x, expressionValue)
        }
    }

    private fun convertValueToExpression(value: Any): Expression<*> {
        return when {
            Keyword.CURRENT_DATE === value -> DateExpression.currentDate()
            // TODO Add Support (querydsl-mongodb does not support it)
            Keyword.CURRENT_TIME === value -> TimeExpression.currentTime()
            // TODO Replace Expressions.asDateTime(LocalDateTime.now()) by DateTimeExpression.currentTimestamp() when Ops.DateTimeOps.CURRENT_TIMESTAMP will be supported natively by querydsl-mongodb
            // see: https://github.com/querydsl/querydsl/pull/2774
            Keyword.CURRENT_DATE_TIME === value -> Expressions.asDateTime(LocalDateTime.now()) // DateTimeExpression.currentTimestamp()
            else -> Expressions.constant(value)
        }
    }

    //
    // TODO Methods below to remove when Ops.DateTimeOps.CURRENT_DATE will be supported natively by querydsl-mongodb
    // see: https://github.com/querydsl/querydsl/pull/2774
    //

    private fun isCurrentDate(expression: Expression<*>) = expression is Operation && expression.operator == Ops.DateTimeOps.CURRENT_DATE

    private fun asStartOfDay() = Expressions.asDate(Date.from(LocalDate.now().atTime(LocalTime.MIN).atZone(DEFAULT_TIME_ZONE.toZoneId()).toInstant()))

    private fun asEndOfDay() = Expressions.asDate(Date.from(LocalDate.now().atTime(LocalTime.MAX).atZone(DEFAULT_TIME_ZONE.toZoneId()).toInstant()))

}