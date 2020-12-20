package com.weedow.spring.data.search.expression

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.weedow.spring.data.search.join.EntityJoins
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.jpa.domain.Specification
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

@ExtendWith(MockitoExtension::class)
internal class JpaLogicalExpressionTest {

    private lateinit var mockExpression1: Expression
    private lateinit var mockExpression2: Expression

    private lateinit var entityJoins: EntityJoins

    private lateinit var root: Root<Any>
    private lateinit var criteriaQuery: CriteriaQuery<*>

    private lateinit var criteriaBuilder: CriteriaBuilder
    private lateinit var mockPredicate1: Predicate
    private lateinit var mockPredicate2: Predicate

    @BeforeEach
    fun setUp() {
        mockExpression1 = mock(name = "mockExpression1")
        mockExpression2 = mock(name = "mockExpression2")
    }

    private fun setUpSpecification() {
        entityJoins = mock()

        root = mock()
        criteriaQuery = mock()
        criteriaBuilder = mock()

        val mockSpecification1 = mock<Specification<Any>>()
        whenever(mockExpression1.toSpecification<Any>(entityJoins)).thenReturn(mockSpecification1)
        mockPredicate1 = mock(name = "mockPredicate1")
        whenever(mockSpecification1.toPredicate(root, criteriaQuery, criteriaBuilder)).thenReturn(mockPredicate1)

        val mockSpecification2 = mock<Specification<Any>>()
        whenever(mockExpression2.toSpecification<Any>(entityJoins)).thenReturn(mockSpecification2)
        mockPredicate2 = mock(name = "mockPredicate2")
        whenever(mockSpecification2.toPredicate(root, criteriaQuery, criteriaBuilder)).thenReturn(mockPredicate2)
    }

    @Test
    fun to_specification_with_OR_operator() {
        setUpSpecification()

        val predicate = mock<Predicate>()
        val mockPredicateExpression1 = mockPredicate1 as javax.persistence.criteria.Expression<Boolean>
        val mockPredicateExpression2 = mockPredicate2 as javax.persistence.criteria.Expression<Boolean>
        whenever(criteriaBuilder.or(mockPredicateExpression2, mockPredicateExpression1)).thenReturn(predicate)

        val expression = LogicalExpression(LogicalOperator.OR, listOf(mockExpression1, mockExpression2))
        val specification = expression.toSpecification<Any>(entityJoins)

        val result = specification.toPredicate(root, criteriaQuery, criteriaBuilder)

        assertThat(result).isEqualTo(predicate)
    }

    @Test
    fun to_specification_with_AND_operator() {
        setUpSpecification()

        val predicate = mock<Predicate>()
        val mockPredicateExpression1 = mockPredicate1 as javax.persistence.criteria.Expression<Boolean>
        val mockPredicateExpression2 = mockPredicate2 as javax.persistence.criteria.Expression<Boolean>
        whenever(criteriaBuilder.and(mockPredicateExpression2, mockPredicateExpression1)).thenReturn(predicate)

        val expression = LogicalExpression(LogicalOperator.AND, listOf(mockExpression1, mockExpression2))
        val specification = expression.toSpecification<Any>(entityJoins)

        val result = specification.toPredicate(root, criteriaQuery, criteriaBuilder)

        assertThat(result).isEqualTo(predicate)
    }

    @Test
    fun to_field_expressions() {
        assertToFieldExpressions(LogicalOperator.AND, false)
        assertToFieldExpressions(LogicalOperator.OR, false)

        assertToFieldExpressions(LogicalOperator.AND, true)
        assertToFieldExpressions(LogicalOperator.OR, true)
    }

    private fun assertToFieldExpressions(logicalOperator: LogicalOperator, negated: Boolean) {
        val fieldExpression1 = mock<FieldExpression>()
        whenever(mockExpression1.toFieldExpressions(negated)).thenReturn(listOf(fieldExpression1))
        val fieldExpression2 = mock<FieldExpression>()
        whenever(mockExpression2.toFieldExpressions(negated)).thenReturn(listOf(fieldExpression2))

        val expression = LogicalExpression(logicalOperator, listOf(mockExpression1, mockExpression2))
        val fieldExpressions = expression.toFieldExpressions(negated)

        assertThat(fieldExpressions).containsExactly(fieldExpression1, fieldExpression2)
    }
}