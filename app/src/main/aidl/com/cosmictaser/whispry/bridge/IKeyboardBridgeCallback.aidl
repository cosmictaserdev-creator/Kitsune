// SPDX-License-Identifier: MIT
package com.cosmictaser.whispry.bridge;
import android.os.Bundle;
oneway interface IKeyboardBridgeCallback {
    void onState(in Bundle state);
}
