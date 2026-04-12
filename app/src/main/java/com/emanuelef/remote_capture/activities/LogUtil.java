package com.emanuelef.remote_capture.activities;
import com.secureguard.mdm.utils.FileLogger;
public class LogUtil {
    public static void logToFile(String message) {
        FileLogger.INSTANCE.log("PCAPdroid-Native", message);
    }
}
