package com.secureguard.mdm.boot.impl

import android.content.Context
import com.secureguard.mdm.boot.api.BootTask
import com.secureguard.mdm.data.repository.SettingsRepository
import com.secureguard.mdm.services.WatermarkOverlayService
import com.secureguard.mdm.utils.FileLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WatermarkBootTask @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : BootTask {

    override suspend fun onBootCompleted() {
        if (!settingsRepository.isWatermarkEnabled()) {
            FileLogger.log(TAG, "Watermark disabled in settings; skipping start.")
            return
        }
        if (!WatermarkOverlayService.hasOverlayPermission(context)) {
            FileLogger.log(TAG, "Overlay permission not granted; skipping start.")
            return
        }
        FileLogger.log(TAG, "Starting WatermarkOverlayService on boot.")
        WatermarkOverlayService.start(context)
    }

    companion object {
        private const val TAG = "WatermarkBootTask"
    }
}
