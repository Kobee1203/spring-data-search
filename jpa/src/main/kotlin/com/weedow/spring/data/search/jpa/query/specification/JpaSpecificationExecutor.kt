package com.weedow.spring.data.search.jpa.query.specification

import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Templates
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.JPQLQuery
import com.querydsl.jpa.impl.AbstractJPAQuery
import com.querydsl.jpa.impl.JPAProvider
import com.querydsl.jpa.impl.JPAQuery
import com.weedow.spring.data.search.context.DataSearchContext
import com.weedow.spring.data.search.jpa.query.JpaQueryBuilder
import com.weedow.spring.data.search.jpa.query.addMissingTemplates
import com.weedow.spring.data.search.query.querytype.QEntityRootImpl
import com.weedow.spring.data.search.query.specification.Specification
import com.weedow.spring.data.search.query.specification.SpecificationExecutor
import org.springframework.data.jpa.repository.support.CrudMethodMetadata
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.Querydsl
import org.springframework.data.querydsl.EntityPathResolver
import javax.persistence.EntityManager

/**
 * JPA [SpecificationExecutor] implementation.
 *
 * @param dataSearchContext [DataSearchContext]
 * @param entityInformation [JpaEntityInformation]
 * @param entityManager [EntityManager]
 * @param resolver [EntityPathResolver]
 * @param metadata [CrudMethodMetadata]
 */
open class JpaSpecificationExecutor<T>(
    private val dataSearchContext: DataSearchContext,
    entityInformation: JpaEntityInformation<T, *>,
    private val entityManager: EntityManager,
    resolver: EntityPathResolver,
    private val metadata: CrudMethodMetadata?
) : SpecificationExecutor<T> {

    companion object {
        val TEMPLATES_INITIALIZED = mutableListOf<Templates>()
    }

    private val path: EntityPath<T> = resolver.createPath(entityInformation.javaType)
    private val querydsl: Querydsl = object : Querydsl(entityManager, PathBuilder(path.type, path.metadata)) {
        override fun <T> createQuery(): AbstractJPAQuery<T, JPAQuery<T>> {
            val templates = JPAProvider.getTemplates(entityManager)
            if (!TEMPLATES_INITIALIZED.contains(templates)) {
                templates.addMissingTemplates()
                TEMPLATES_INITIALIZED.add(templates)
            }
            return JPAQuery(entityManager)
        }
    }

    override fun findAll(specification: Specification<T>?): List<T> {
        return createQuery(specification).select(path).fetch()
    }

    /**
     * Creates a new [JPQLQuery] for the given [Specification].
     *
     * @param specification
     * @return the Querydsl [JPQLQuery].
     */
    protected fun createQuery(specification: Specification<T>?): JPQLQuery<*> {
        val query: AbstractJPAQuery<*, *> = doCreateQuery(specification)
        val metadata: CrudMethodMetadata = getRepositoryMethodMetadata() ?: return query
        val type = metadata.lockModeType
        return if (type == null) query else query.setLockMode(type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun doCreateQuery(specification: Specification<T>?): AbstractJPAQuery<T, *> {
        var query: AbstractJPAQuery<T, *> = querydsl.createQuery(path) as AbstractJPAQuery<T, *>

        if (specification != null) {
            val predicate = specification.toPredicate(createQueryBuilder(query))
            if (predicate != Specification.NO_PREDICATE) {
                query = query.where(predicate) as AbstractJPAQuery<T, *>
            }
        }

        return query
    }

    private fun createQueryBuilder(query: AbstractJPAQuery<T, *>) =
        JpaQueryBuilder(dataSearchContext, query, QEntityRootImpl(dataSearchContext.get(path.type)))

    private fun getRepositoryMethodMetadata() = metadata

}