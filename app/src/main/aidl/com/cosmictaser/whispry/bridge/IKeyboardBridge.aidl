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
}
