package com.app.stockmaster.service

import android.content.Context
import android.net.Uri
import com.app.stockmaster.data.local.entity.ItemEntity
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManagementService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun exportToCsv(uri: Uri, items: List<ItemEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext Result.failure(Exception("Could not open output stream"))
            val writer = CSVWriter(OutputStreamWriter(outputStream))
            
            // Header
            writer.writeNext(arrayOf("Name", "SKU", "Barcode", "Category", "Cost Price", "Sale Price", "Current Stock", "Min Stock Alert"))
            
            // Data
            items.forEach { item ->
                writer.writeNext(arrayOf(
                    item.name,
                    item.sku,
                    item.barcode,
                    item.category,
                    item.costPrice.toString(),
                    item.salePrice.toString(),
                    item.currentStock.toString(),
                    item.minStockAlert.toString()
                ))
            }
            
            writer.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromCsv(uri: Uri): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext Result.failure(Exception("Could not open input stream"))
            val reader = CSVReader(InputStreamReader(inputStream))
            
            val items = mutableListOf<ItemEntity>()
            val lines = reader.readAll()
            
            // Skip header
            if (lines.isNotEmpty()) {
                for (i in 1 until lines.size) {
                    val line = lines[i]
                    if (line.size >= 8) {
                        items.add(ItemEntity(
                            id = 0,
                            name = line[0],
                            sku = line[1],
                            barcode = line[2],
                            category = line[3],
                            costPrice = line[4].toDoubleOrNull() ?: 0.0,
                            salePrice = line[5].toDoubleOrNull() ?: 0.0,
                            currentStock = line[6].toIntOrNull() ?: 0,
                            minStockAlert = line[7].toIntOrNull() ?: 5,
                            updatedAt = System.currentTimeMillis()
                        ))
                    }
                }
            }
            
            reader.close()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
