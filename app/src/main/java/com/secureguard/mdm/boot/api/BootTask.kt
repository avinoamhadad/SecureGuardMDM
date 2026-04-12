package com.secureguard.mdm.boot.api

/**
 * Interface for tasks that need to be executed when the device boots up.
 * Each task is responsible for its own logic and for checking if it needs to run.
 */
interface BootTask {
    /**
     * This function is called by the MainService after the device has finished booting.
     */
    suspend fun onBootCompleted()
}