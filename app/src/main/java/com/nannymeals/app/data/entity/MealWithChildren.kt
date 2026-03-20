package com.nannymeals.app.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class MealWithChildren(
    @Embedded val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MealChildCrossRef::class,
            parentColumn = "mealId",
            entityColumn = "childId"
        )
    )
    val children: List<ChildEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val items: List<MealItemEntity> = emptyList()
)

data class ChildWithMeals(
    @Embedded val child: ChildEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MealChildCrossRef::class,
            parentColumn = "childId",
            entityColumn = "mealId"
        )
    )
    val meals: List<MealEntity>
)
