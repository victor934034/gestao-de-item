package com.app.stockmaster.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.stockmaster.R
import com.app.stockmaster.data.local.dao.ItemDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import android.app.PendingIntent
import android.content.Intent
import com.app.stockmaster.MainActivity

@HiltWorker
class StockNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val itemDao: ItemDao,
    private val itemRepository: com.app.stockmaster.data.repository.ItemRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "low_stock_alerts"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SHOW_LOW_STOCK = "com.app.stockmaster.ACTION_SHOW_LOW_STOCK"
    }

    override suspend fun doWork(): Result {
        // Sync with bridge before checking stock to ensure shared data is up to date
        try {
            itemRepository.syncWithBridge()
        } catch (e: Exception) {
            android.util.Log.e("StockWorker", "Sync failed during background work", e)
        }

        val lowStockItems = itemDao.getLowStockItems().first()
        
        if (lowStockItems.isNotEmpty()) {
            showNotification(lowStockItems.size)
        }
        
        return Result.success()
    }

    private fun showNotification(itemCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de Estoque Baixo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações quando itens estão abaixo do estoque mínimo"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_SHOW_LOW_STOCK
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon instead of generic dialog alert
            .setContentTitle("Alerta de Estoque Baixo")
            .setContentText("Você tem $itemCount itens precisando de reposição!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "VER ITENS",
                pendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
