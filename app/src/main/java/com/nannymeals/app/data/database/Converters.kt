package com.nannymeals.app.data.database

import androidx.room.TypeConverter
import com.nannymeals.app.data.entity.FoodCategory
import com.nannymeals.app.data.entity.MealType
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.toString()
    }

    @TypeConverter
    fun toLocalTime(timeString: String?): LocalTime? {
        return timeString?.let { LocalTime.parse(it) }
    }

    @TypeConverter
    fun fromMealType(mealType: MealType): String {
        return mealType.name
    }

    @TypeConverter
    fun toMealType(mealTypeName: String): MealType {
        return MealType.valueOf(mealTypeName)
    }

    @TypeConverter
    fun fromFoodCategory(category: FoodCategory): String {
        return category.name
    }

    @TypeConverter
    fun toFoodCategory(categoryName: String): FoodCategory {
        return FoodCategory.valueOf(categoryName)
    }
}
