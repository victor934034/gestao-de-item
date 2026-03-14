package com.app.stockmaster.data.repository

import com.app.stockmaster.data.local.dao.CategoryDao
import com.app.stockmaster.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    
    suspend fun insertCategory(category: CategoryEntity) = categoryDao.insertCategory(category)
    
    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)
}
