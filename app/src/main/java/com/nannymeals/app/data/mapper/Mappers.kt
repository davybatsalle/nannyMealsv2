package com.nannymeals.app.data.mapper

import com.nannymeals.app.data.entity.ChildEntity
import com.nannymeals.app.data.entity.FoodItemEntity
import com.nannymeals.app.data.entity.MealEntity
import com.nannymeals.app.data.entity.MealItemEntity
import com.nannymeals.app.data.entity.MealWithChildren
import com.nannymeals.app.domain.model.Child
import com.nannymeals.app.domain.model.FoodItem
import com.nannymeals.app.domain.model.Meal
import com.nannymeals.app.domain.model.MealItem

fun ChildEntity.toChild(): Child {
    return Child(
        id = id,
        name = name,
        dateOfBirth = dateOfBirth,
        dietaryRestrictions = dietaryRestrictions,
        allergies = allergies,
        parentEmail = parentEmail,
        parentName = parentName
    )
}

fun Child.toEntity(userId: String): ChildEntity {
    return ChildEntity(
        id = id,
        userId = userId,
        name = name,
        dateOfBirth = dateOfBirth,
        dietaryRestrictions = dietaryRestrictions,
        allergies = allergies,
        parentEmail = parentEmail,
        parentName = parentName,
        updatedAt = System.currentTimeMillis()
    )
}

fun MealEntity.toMeal(children: List<Child> = emptyList(), items: List<MealItem> = emptyList()): Meal {
    return Meal(
        id = id,
        date = date,
        time = time,
        mealType = mealType,
        notes = notes,
        children = children,
        items = items
    )
}

fun MealWithChildren.toMeal(): Meal {
    return Meal(
        id = meal.id,
        date = meal.date,
        time = meal.time,
        mealType = meal.mealType,
        notes = meal.notes,
        children = children.map { it.toChild() },
        items = items.map { it.toMealItem() }
    )
}

fun Meal.toEntity(userId: String): MealEntity {
    return MealEntity(
        id = id,
        userId = userId,
        date = date,
        time = time,
        mealType = mealType,
        notes = notes,
        updatedAt = System.currentTimeMillis()
    )
}

// Meal Item mappers
fun MealItemEntity.toMealItem(): MealItem {
    return MealItem(
        id = id,
        name = name,
        category = category,
        portion = portion,
        notes = notes
    )
}

fun MealItem.toEntity(mealId: Long): MealItemEntity {
    return MealItemEntity(
        id = id,
        mealId = mealId,
        name = name,
        category = category,
        portion = portion,
        notes = notes
    )
}

// Food Item catalog mappers
fun FoodItemEntity.toFoodItem(): FoodItem {
    return FoodItem(
        id = id,
        name = name,
        category = category,
        usageCount = usageCount,
        lastUsedDate = lastUsedDate
    )
}

fun FoodItem.toEntity(userId: String): FoodItemEntity {
    return FoodItemEntity(
        id = id,
        userId = userId,
        name = name,
        category = category,
        usageCount = usageCount,
        lastUsedDate = lastUsedDate
    )
}
