# Agent Development Instructions

## Logging Standard
All logging within this application MUST use the centralized `com.secureguard.mdm.utils.AppLogger`.

### DO NOT USE `android.util.Log`
Do not call `Log.d`, `Log.i`, `Log.e`, etc directly. 

### Why?
`AppLogger` serves as a proxy. It:
1. Pipes logs to the native `android.util.Log` automatically.
2. Intercepts logs and saves them to an internal JSON file.
3. Automatically classifies them into "App Logs" or "VPN Logs" based on the tag string (Tags containing `VPN`, `Capture`, `pcap`, `firewall`, `netfree`, or `rimon` are marked as VPN logs).
4. Feeds directly into the in-app Log Viewer interface.

### Example Replacement
Instead of:
```kotlin
import android.util.Log
Log.i("MyFeature", "Started properly")
```

Use:
```kotlin
import com.secureguard.mdm.utils.AppLogger
AppLogger.i("MyFeature", "Started properly")
```

### Note for Java classes
Use standard static methods as it exposes `@JvmStatic`:
```java
import com.secureguard.mdm.utils.AppLogger;
AppLogger.i("MyFeature", "Started properly");
```

### Security Policy
**CRITICAL:** Never log passwords, tokens, full Google Accounts IDs (unless explicitly required and masked), or any sensitive PII data to `AppLogger`. Since logs are persisted to a JSON file locally, writing sensitive variables directly to logs introduces a major security leak. Mask or hash sensitive items if their presence must be verified.
