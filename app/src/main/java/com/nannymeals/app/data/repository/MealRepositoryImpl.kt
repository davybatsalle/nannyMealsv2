package com.nannymeals.app.data.repository

import com.nannymeals.app.data.dao.ChildDao
import com.nannymeals.app.data.dao.MealDao
import com.nannymeals.app.data.entity.MealType
import com.nannymeals.app.data.mapper.toChild
import com.nannymeals.app.data.mapper.toEntity
import com.nannymeals.app.data.mapper.toMeal
import com.nannymeals.app.domain.model.Child
import com.nannymeals.app.domain.model.Meal
import com.nannymeals.app.domain.model.MealItem
import com.nannymeals.app.domain.model.MealReport
import com.nannymeals.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val childDao: ChildDao
) : MealRepository {

    private val userId: String = "local_user"

    override fun getMeals(): Flow<List<Meal>> {
        return mealDao.getMealsWithChildren(userId).map { mealsWithChildren ->
            mealsWithChildren.map { it.toMeal() }
        }
    }

    override fun getMealsByDate(date: LocalDate): Flow<List<Meal>> {
        return mealDao.getMealsWithChildrenByDate(userId, date).map { mealsWithChildren ->
            mealsWithChildren.map { it.toMeal() }
        }
    }

    override fun getMealsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Meal>> {
        return mealDao.getMealsWithChildrenByDateRange(userId, startDate, endDate).map { mealsWithChildren ->
            mealsWithChildren.map { it.toMeal() }
        }
    }

    override suspend fun getMealById(mealId: Long): Meal? {
        return mealDao.getMealWithChildren(mealId)?.toMeal()
    }

    override suspend fun addMeal(meal: Meal, childIds: List<Long>, items: List<MealItem>): Long {
        val entity = meal.toEntity(userId)
        val itemEntities = items.map { it.toEntity(0) } // mealId will be set in DAO
        return mealDao.insertMealWithChildrenAndItems(entity, childIds, itemEntities)
    }

    override suspend fun updateMeal(meal: Meal, childIds: List<Long>, items: List<MealItem>) {
        val entity = meal.toEntity(userId)
        val itemEntities = items.map { it.toEntity(meal.id) }
        mealDao.updateMealWithChildrenAndItems(entity, childIds, itemEntities)
    }

    override suspend fun deleteMeal(mealId: Long) {
        mealDao.deleteMealById(mealId)
    }

    override suspend fun generateReport(startDate: LocalDate, endDate: LocalDate): MealReport {
        val meals = mealDao.getMealsWithChildrenByDateRange(userId, startDate, endDate).first()
        val mealTypeCounts = mealDao.getMealTypeCount(userId, startDate, endDate)
        val mealsPerDay = mealDao.getMealsPerDay(userId, startDate, endDate)
        val totalMeals = mealDao.getTotalMealCount(userId, startDate, endDate)
        val children = childDao.getChildrenByUser(userId).first()

        // Calculate meals per child
        val childMealCounts = mutableMapOf<Child, Int>()
        children.forEach { childEntity ->
            val child = childEntity.toChild()
            val count = meals.count { meal -> 
                meal.children.any { it.id == childEntity.id }
            }
            childMealCounts[child] = count
        }

        // Generate insights
        val insights = generateInsights(meals, mealTypeCounts, mealsPerDay, totalMeals, startDate, endDate)
        
        // Generate recommendations
        val recommendations = generateRecommendations(meals, mealTypeCounts, childMealCounts, children.map { it.toChild() })

        return MealReport(
            startDate = startDate,
            endDate = endDate,
            totalMeals = totalMeals,
            mealTypeCounts = mealTypeCounts.associate { it.mealType to it.count },
            mealsPerDay = mealsPerDay.associate { it.date to it.count },
            childMealCounts = childMealCounts,
            insights = insights,
            recommendations = recommendations
        )
    }

    private fun generateInsights(
        meals: List<com.nannymeals.app.data.entity.MealWithChildren>,
        mealTypeCounts: List<com.nannymeals.app.data.dao.MealTypeCount>,
        mealsPerDay: List<com.nannymeals.app.data.dao.DailyMealCount>,
        totalMeals: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<String> {
        val insights = mutableListOf<String>()

        if (totalMeals == 0) {
            insights.add("Aucun repas enregistré pour cette période.")
            return insights
        }

        // Average meals per day
        val daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
        val avgMealsPerDay = totalMeals.toDouble() / daysInPeriod
        insights.add("Moyenne de %.1f repas enregistrés par jour.".format(avgMealsPerDay))

        // Most common meal type
        val mostCommon = mealTypeCounts.maxByOrNull { it.count }
        mostCommon?.let {
            val typeName = when(it.mealType) {
                MealType.LUNCH -> "Déjeuner"
                MealType.SNACK -> "Collation"
            }
            insights.add("$typeName est le type de repas le plus fréquemment enregistré (${it.count} fois).")
        }

        // Days with meals
        val daysWithMeals = mealsPerDay.size
        val coveragePercent = (daysWithMeals.toDouble() / daysInPeriod * 100).toInt()
        insights.add("Les repas ont été enregistrés sur $daysWithMeals jours sur $daysInPeriod ($coveragePercent% de couverture).")

        // Busiest day
        val busiestDay = mealsPerDay.maxByOrNull { it.count }
        busiestDay?.let {
            insights.add("Jour le plus actif : ${it.date} avec ${it.count} repas enregistrés.")
        }

        return insights
    }

    private fun generateRecommendations(
        meals: List<com.nannymeals.app.data.entity.MealWithChildren>,
        mealTypeCounts: List<com.nannymeals.app.data.dao.MealTypeCount>,
        childMealCounts: Map<Child, Int>,
        children: List<Child>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // Check for missing meal types
        val loggedTypes = mealTypeCounts.map { it.mealType }.toSet()
        val missingTypes = MealType.values().toSet() - loggedTypes
        if (missingTypes.isNotEmpty()) {
            val missingNames = missingTypes.joinToString(", ") { type ->
                when(type) {
                    MealType.LUNCH -> "déjeuner"
                    MealType.SNACK -> "collation"
                }
            }
            recommendations.add("Pensez à enregistrer les repas de type $missingNames pour un meilleur suivi.")
        }

        // Check meal balance
        if (mealTypeCounts.isNotEmpty()) {
            val snackCount = mealTypeCounts.find { it.mealType == MealType.SNACK }?.count ?: 0
            val mainMealCount = mealTypeCounts.filter { it.mealType != MealType.SNACK }.sumOf { it.count }
            if (snackCount > mainMealCount) {
                recommendations.add("Il y a plus de collations que de repas principaux. Pensez à équilibrer avec plus d'entrées de petit-déjeuner, déjeuner ou dîner.")
            }
        }

        // Check for children with few meals
        val avgMealsPerChild = if (childMealCounts.isNotEmpty()) {
            childMealCounts.values.average()
        } else 0.0
        
        childMealCounts.filter { it.value < avgMealsPerChild * 0.5 }.forEach { (child, count) ->
            recommendations.add("${child.name} a moins d'entrées de repas que la moyenne. Vérifiez si les repas sont correctement enregistrés.")
        }

        // Check for allergies and dietary restrictions
        val childrenWithRestrictions = children.filter { it.hasAllergies || it.hasDietaryRestrictions }
        if (childrenWithRestrictions.isNotEmpty()) {
            recommendations.add("N'oubliez pas de vérifier que les repas respectent les restrictions alimentaires pour : ${childrenWithRestrictions.joinToString(", ") { it.name }}")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Excellent travail ! Votre suivi des repas est équilibré et complet.")
        }

        return recommendations
    }
}
