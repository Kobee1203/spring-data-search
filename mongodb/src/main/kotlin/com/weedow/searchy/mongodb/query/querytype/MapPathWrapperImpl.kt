package com.weedow.searchy.mongodb.query.querytype

import com.querydsl.core.types.dsl.MapPath
import com.querydsl.core.types.dsl.SimpleExpression
import com.weedow.searchy.query.querytype.ElementType
import org.apache.commons.lang3.reflect.FieldUtils

/**
 * [PathWrapper] implementation for [MapPath] that also contains the related [ElementType].
 *
 * @param mapPath [MapPath] to be wrapped
 * @param elementType [ElementType] related to the given `mapPath`
 */
class MapPathWrapperImpl<K, V, E : SimpleExpression<V>>(
    mapPath: MapPath<K, V, E>,
    override val elementType: ElementType
) : PathWrapper<Map<K, V>>, MapPath<K, V, E>(
    mapPath.keyType,
    mapPath.valueType,
    FieldUtils.readField(mapPath, "queryType", true) as Class<E>,
    mapPath.metadata
)