package com.weedow.spring.data.search.join

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.weedow.spring.data.search.common.model.Address
import com.weedow.spring.data.search.common.model.Person
import com.weedow.spring.data.search.common.model.Vehicle
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.query.criteria.internal.JoinImplementor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Path
import javax.persistence.criteria.Root
import javax.persistence.metamodel.Attribute

@ExtendWith(MockitoExtension::class)
internal class EntityJoinsImplTest {

    companion object {
        private const val ADDRESS_ENTITIES_FIELD = "addressEntities"
        private const val FIRST_NAME_FIELD = "firstName"
        private const val PERSONS_FIELD = "persons"
        private const val VEHICLES_FIELD = "vehicles"
        private const val COUNTRY_FIELD = "country"
        private const val NICK_NAMES_FIELD = "nickNames"

        private const val COUNTRY_PATH = "addressEntities.country"
    }

    @Test
    fun check_already_processed() {
        val entityJoins = EntityJoinsImpl(Person::class.java)

        val entityJoin = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(ADDRESS_ENTITIES_FIELD))
        entityJoins.add(entityJoin)

        // Root class
        assertThat(entityJoins.alreadyProcessed(Address::class.java, Address::class.java.getDeclaredField(PERSONS_FIELD))).isTrue()
        // Join already added
        assertThat(entityJoins.alreadyProcessed(Person::class.java, Person::class.java.getDeclaredField(ADDRESS_ENTITIES_FIELD))).isTrue()
        // Join not already processed
        assertThat(entityJoins.alreadyProcessed(Person::class.java, Person::class.java.getDeclaredField(VEHICLES_FIELD))).isFalse()
    }

    @Test
    fun get_path_for_simple_field() {
        val rootClass = Person::class.java
        val entityJoins = EntityJoinsImpl(rootClass)

        val expectedPath = mock<Path<Any>>()

        val root = mock<Root<Person>>()
        whenever(root.javaType).thenReturn(rootClass)
        whenever(root.get<Any>(FIRST_NAME_FIELD)).thenReturn(expectedPath)

        val path = entityJoins.getPath(FIRST_NAME_FIELD, root)

        assertThat(path).isEqualTo(expectedPath)
    }

    @Test
    fun get_path_for_inner_join_field() {
        val rootClass = Person::class.java
        val entityJoins = EntityJoinsImpl(rootClass)

        val expectedJoin = mock<JoinImplementor<Any, Any>>()

        val root = mock<Root<Person>>()
        whenever(root.javaType).thenReturn(rootClass)
        whenever(root.join<Any, Any>(ADDRESS_ENTITIES_FIELD, JoinType.INNER)).thenReturn(expectedJoin)

        val entityJoin = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(ADDRESS_ENTITIES_FIELD))
        entityJoins.add(entityJoin)

        val join = entityJoins.getPath(ADDRESS_ENTITIES_FIELD, root)

        assertThat(join).isEqualTo(expectedJoin)
    }

    @Test
    fun get_path_for_left_join_field() {
        val rootClass = Person::class.java
        val entityJoins = EntityJoinsImpl(rootClass)

        val expectedJoin = mock<JoinImplementor<Any, Any>>()

        val root = mock<Root<Person>>()
        whenever(root.javaType).thenReturn(rootClass)
        whenever(root.fetch<Any, Any>(ADDRESS_ENTITIES_FIELD, JoinType.LEFT)).thenReturn(expectedJoin)

        val entityJoin = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(ADDRESS_ENTITIES_FIELD), JoinType.LEFT, true)
        entityJoins.add(entityJoin)

        val join = entityJoins.getPath(ADDRESS_ENTITIES_FIELD, root)

        assertThat(join).isEqualTo(expectedJoin)
    }

    @Test
    fun get_path_for_field_in_sub_parent() {
        val rootClass = Person::class.java
        val entityJoins = EntityJoinsImpl(rootClass)

        val expectedPath = mock<Path<Any>>()

        val root = mock<Root<Person>>()
        whenever(root.javaType).thenReturn(rootClass)
        val join = mock<JoinImplementor<Any, Any>>()
        whenever(root.fetch<Any, Any>(ADDRESS_ENTITIES_FIELD, JoinType.LEFT)).thenReturn(join)
        whenever(join.javaType).thenReturn(Address::class.java)
        whenever(join.get<Any>(COUNTRY_FIELD)).thenReturn(expectedPath)

        val entityJoin = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(ADDRESS_ENTITIES_FIELD), JoinType.LEFT, true)
        entityJoins.add(entityJoin)

        val path = entityJoins.getPath(COUNTRY_PATH, root)

        assertThat(path).isEqualTo(expectedPath)
    }

    @Test
    fun get_path_for_field_in_sub_parent_with_default_EntityJoin() {
        val rootClass = Person::class.java
        val entityJoins = EntityJoinsImpl(rootClass)

        val expectedPath = mock<Path<Any>>()

        val root = mock<Root<Person>>()
        whenever(root.javaType).thenReturn(rootClass)
        val join = mock<JoinImplementor<Any, Any>>()
        whenever(root.join<Any, Any>(ADDRESS_ENTITIES_FIELD, JoinType.INNER)).thenReturn(join)
        whenever(join.javaType).thenReturn(Address::class.java)
        whenever(join.get<Any>(COUNTRY_FIELD)).thenReturn(expectedPath)

        val path = entityJoins.getPath(COUNTRY_PATH, root)

        assertThat(path).isEqualTo(expectedPath)
    }

    @Test
    fun get_path_for_field_in_sub_parent_with_inner_join_already_present_in_parent_join() {
        val rootClass = Person::class.java
        val entityJoins = EntityJoinsImpl(rootClass)

        val expectedPath = mock<Path<Any>>()

        val root = mock<Root<Person>>()
        whenever(root.javaType).thenReturn(rootClass)

        val join = mock<JoinImplementor<Person, Any>>()
        whenever(root.joins).thenReturn(setOf(join))

        whenever(join.javaType).thenReturn(Address::class.java)
        whenever(join.get<Any>(COUNTRY_FIELD)).thenReturn(expectedPath)

        val attribute = mock<Attribute<in Person, *>>()
        whenever(join.attribute).thenReturn(attribute)

        whenever(attribute.name).thenReturn(ADDRESS_ENTITIES_FIELD)
        whenever(join.joinType).thenReturn(JoinType.INNER)

        val entityJoin = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(ADDRESS_ENTITIES_FIELD), JoinType.INNER, true)
        entityJoins.add(entityJoin)

        val path = entityJoins.getPath(COUNTRY_PATH, root)

        assertThat(path).isEqualTo(expectedPath)
    }

    @Test
    fun get_path_for_field_in_sub_parent_with_left_join_already_present_in_parent_join() {
        val rootClass = Person::class.java
        val entityJoins = EntityJoinsImpl(rootClass)

        val expectedPath = mock<Path<Any>>()

        val root = mock<Root<Person>>()
        whenever(root.javaType).thenReturn(rootClass)

        val join = mock<JoinImplementor<Person, Any>>()
        whenever(root.fetches).thenReturn(setOf(join))

        whenever(join.javaType).thenReturn(Address::class.java)
        whenever(join.get<Any>(COUNTRY_FIELD)).thenReturn(expectedPath)

        val attribute = mock<Attribute<in Person, *>>()
        whenever(join.attribute).thenReturn(attribute)

        whenever(attribute.name).thenReturn(ADDRESS_ENTITIES_FIELD)
        whenever(join.joinType).thenReturn(JoinType.LEFT)

        val entityJoin = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(ADDRESS_ENTITIES_FIELD), JoinType.LEFT, true)
        entityJoins.add(entityJoin)

        val path = entityJoins.getPath(COUNTRY_PATH, root)

        assertThat(path).isEqualTo(expectedPath)
    }

    @Test
    fun get_joins_when_no_join_is_added() {
        val rootClass = Person::class.java
        val entityJoins = EntityJoinsImpl(rootClass)

        assertThat(entityJoins.getJoins()).isEmpty()
    }

    @Test
    fun get_joins_when_any_joins_are_added() {
        val rootClass = Person::class.java
        val entityJoins = EntityJoinsImpl(rootClass)

        val entityJoin1 = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(ADDRESS_ENTITIES_FIELD))
        entityJoins.add(entityJoin1)
        val entityJoin2 = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(VEHICLES_FIELD), JoinType.LEFT, true)
        entityJoins.add(entityJoin2)
        val entityJoin3 = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(NICK_NAMES_FIELD), JoinType.LEFT, true)
        entityJoins.add(entityJoin3)

        val joins = entityJoins.getJoins()
        assertThat(joins).containsExactlyInAnyOrderEntriesOf(mutableMapOf(
                Address::class.java.canonicalName to entityJoin1,
                Vehicle::class.java.canonicalName to entityJoin2,
                Person::class.java.canonicalName + "." + NICK_NAMES_FIELD to entityJoin3
        ))
    }

    @Test
    fun get_joins_by_using_filter() {
        val rootClass = Person::class.java
        val entityJoins = EntityJoinsImpl(rootClass)

        val entityJoin1 = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(ADDRESS_ENTITIES_FIELD))
        entityJoins.add(entityJoin1)
        val entityJoin2 = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(VEHICLES_FIELD), JoinType.LEFT, true)
        entityJoins.add(entityJoin2)
        val entityJoin3 = EntityJoin(Person::class.java, "", Person::class.java.getDeclaredField(NICK_NAMES_FIELD), JoinType.LEFT, true)
        entityJoins.add(entityJoin3)

        assertThat(entityJoins.getJoins { it.fetched }).containsExactlyInAnyOrderEntriesOf(mutableMapOf(
                Vehicle::class.java.canonicalName to entityJoin2,
                Person::class.java.canonicalName + "." + NICK_NAMES_FIELD to entityJoin3
        ))

        assertThat(entityJoins.getJoins { it.joinType == JoinType.INNER }).containsExactlyInAnyOrderEntriesOf(mutableMapOf(
                Address::class.java.canonicalName to entityJoin1
        ))

        assertThat(entityJoins.getJoins { it.joinName.contains("address", true) }).containsExactlyInAnyOrderEntriesOf(mutableMapOf(
                Address::class.java.canonicalName to entityJoin1
        ))
    }
}