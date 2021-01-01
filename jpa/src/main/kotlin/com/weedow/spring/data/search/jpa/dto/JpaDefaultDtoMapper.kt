package com.weedow.spring.data.search.jpa.dto

import com.weedow.spring.data.search.dto.DtoMapper
import org.apache.commons.beanutils.PropertyUtils
import org.hibernate.Hibernate
import org.hibernate.internal.util.collections.IdentitySet


/**
 * Default [DtoMapper] implementation.
 *
 * There is no conversion to a specific DTO.
 *
 * But the entity is fully loaded to prevent LazyInitializationException when the Entity is serialized.
 *
 * @param entityInitializer [Initializer] used to load the given the Entity. Default is [EntityInitializer].
 */
class JpaDefaultDtoMapper<T>(
    private val entityInitializer: Initializer<Any> = EntityInitializer()
) : DtoMapper<T, T> {

    /**
     * Returns the given Entity bean directly
     *
     * @param source Entity bean
     * @return The same Entity bean as [source]
     */
    override fun map(source: T): T {
        // Set of objects already initialized to prevent cycles
        val initializedObjects = IdentitySet()
        entityInitializer.initialize(source!!, initializedObjects)

        return source
    }

    /** @suppress */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JpaDefaultDtoMapper<*>

        if (entityInitializer != other.entityInitializer) return false

        return true
    }

    /** @suppress */
    override fun hashCode(): Int {
        return entityInitializer.hashCode()
    }


}

/**
 * Interface to initialize recursively an object if it is not initialized, and prevent cycles if an object is already initialized.
 */
interface Initializer<T> {

    /**
     * Method to be implemented.
     *
     * @param obj Object to be initialized
     * @param initializedObjects Set of objects already initialized
     */
    fun doInitialize(obj: T, initializedObjects: IdentitySet)

    /**
     * Method to call.
     * This method calls the [doInitialize] method when the given object must be initialized.
     *
     * @param obj Object to be initialized
     * @param initializedObjects Set of objects already initialized
     */
    fun initialize(obj: T, initializedObjects: IdentitySet) {
        // Prevent cycles
        if (!initializedObjects.contains(obj)) {
            initializedObjects.add(obj)

            // Check if the object is initialized
            if (!Hibernate.isInitialized(obj)) {
                Hibernate.initialize(obj)
            }

            // Initialize recursively
            doInitialize(obj, initializedObjects)
        }
    }

}

/**
 * Entity Initializer.
 */
class EntityInitializer() : Initializer<Any> {

    private var propertyInitializer: Initializer<Any> = PropertyInitializer(this)
    private var mapInitializer: Initializer<Map<*, *>> = MapInitializer(this)
    private var collectionInitializer: Initializer<Collection<*>> = CollectionInitializer(this)

    /**
     * Secondary constructor to override one or more inner Initializers.
     */
    constructor(
        propertyInitializer: PropertyInitializer,
        mapInitializer: Initializer<Map<*, *>>,
        collectionInitializer: Initializer<Collection<*>>
    ) : this() {
        this.propertyInitializer = propertyInitializer
        this.mapInitializer = mapInitializer
        this.collectionInitializer = collectionInitializer
    }

    override fun doInitialize(obj: Any, initializedObjects: IdentitySet) {
        val propertyDescriptors = PropertyUtils.getPropertyDescriptors(obj)
        propertyDescriptors.forEach { propertyDescriptor ->
            val propertyType = propertyDescriptor.propertyType
            val property = PropertyUtils.getProperty(obj, propertyDescriptor.name)

            if (!isSkippedProperty(property, propertyType)) {
                when {
                    Map::class.java.isAssignableFrom(propertyType) -> {
                        mapInitializer.initialize(property as Map<*, *>, initializedObjects)
                    }
                    Collection::class.java.isAssignableFrom(propertyType) -> {
                        collectionInitializer.initialize(property as Collection<*>, initializedObjects)
                    }
                    else -> {
                        propertyInitializer.initialize(property, initializedObjects)
                    }
                }
            }
        }
    }

    private fun isSkippedProperty(property: Any?, propertyType: Class<*>) =
        property == null
                || propertyType.isPrimitive
                || propertyType.isEnum
                || propertyType.isArray
                || propertyType.isAnonymousClass
                || propertyType.kotlin.javaPrimitiveType != null
}

/**
 * Property Initializer
 */
class PropertyInitializer(
    private val entityInitializer: EntityInitializer
) : Initializer<Any> {

    override fun doInitialize(obj: Any, initializedObjects: IdentitySet) {
        entityInitializer.initialize(obj, initializedObjects)
    }
}

/**
 * Map Initializer
 */
class MapInitializer(
    private val entityInitializer: EntityInitializer
) : Initializer<Map<*, *>> {

    override fun doInitialize(obj: Map<*, *>, initializedObjects: IdentitySet) {
        obj.keys.forEach {
            entityInitializer.initialize(it!!, initializedObjects)
        }

        obj.values.forEach {
            entityInitializer.initialize(it!!, initializedObjects)
        }
    }
}

/**
 * Collection Initializer
 */
class CollectionInitializer(
    private val entityInitializer: EntityInitializer
) : Initializer<Collection<*>> {

    override fun doInitialize(obj: Collection<*>, initializedObjects: IdentitySet) {
        obj.forEach { item ->
            entityInitializer.initialize(item!!, initializedObjects)
        }
    }
}