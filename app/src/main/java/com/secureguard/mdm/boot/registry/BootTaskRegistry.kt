package com.secureguard.mdm.boot.registry

import com.secureguard.mdm.boot.api.BootTask
import com.secureguard.mdm.boot.impl.NetfreeWatchdogBootTask
import com.secureguard.mdm.boot.impl.ShowToastOnBootTask
import com.secureguard.mdm.boot.impl.WatermarkBootTask
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BootTaskRegistry @Inject constructor(
    showToastOnBootTask: ShowToastOnBootTask,
    netfreeWatchdogBootTask: NetfreeWatchdogBootTask,
    watermarkBootTask: WatermarkBootTask
) {
    val allBootTasks: List<BootTask> = listOf(
        showToastOnBootTask,
        netfreeWatchdogBootTask,
        watermarkBootTask
    )
}