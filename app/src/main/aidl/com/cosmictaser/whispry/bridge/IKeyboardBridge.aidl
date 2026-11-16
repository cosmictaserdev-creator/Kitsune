// SPDX-License-Identifier: MIT
package com.cosmictaser.whispry.bridge;
import android.os.Bundle;
import android.app.PendingIntent;
import com.cosmictaser.whispry.bridge.IKeyboardBridgeCallback;
interface IKeyboardBridge {
    Bundle connect(IKeyboardBridgeCallback callback);
    void disconnect(IKeyboardBridgeCallback callback);
    PendingIntent prepareStart(String targetPackage);
    PendingIntent prepareRetry(String sessionId);
    void command(String sessionId, String action);
    void acknowledge(String sessionId, String resultId);
    /** Protocol 2: tone list + current selection + translation state (no secrets). */
    Bundle tools();
    /** Protocol 2: choose the tone for this dictation (or as default, per Whispry's keyboard tone mode). */
    void selectTone(String presetId);
    /** Protocol 2: speak in any language and insert in [language] when enabled. */
    void setTranslation(boolean enabled, String language);
}
