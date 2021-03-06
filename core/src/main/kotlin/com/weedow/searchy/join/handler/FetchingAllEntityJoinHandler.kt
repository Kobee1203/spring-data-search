package com.weedow.searchy.join.handler

import com.querydsl.core.JoinType
import com.weedow.searchy.join.JoinInfo
import com.weedow.searchy.query.querytype.PropertyInfos

/**
 * [EntityJoinHandler] implementation to fetch all fields (entity fields and nested fields included) with any Join Annotation.
 *
 * Technically, it creates a `LEFT JOIN FETCH`.
 *
 * _Example: `A` has a relationship with `B` and `B` has a relationship with `C`.
 * When we search for `A`, we retrieve `A` with data from `B` and `C`._
 */
class FetchingAllEntityJoinHandler : EntityJoinHandler {

    override fun supports(propertyInfos: PropertyInfos): Boolean {
        return true
    }

    override fun handle(propertyInfos: PropertyInfos): JoinInfo {
        return JoinInfo(JoinType.LEFTJOIN, true)
    }

}