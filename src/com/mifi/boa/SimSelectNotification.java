/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mifi.boa;

import com.android.internal.telephony.IccCardConstants;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.List;

public class SimSelectNotification extends BroadcastReceiver {
    private static final String TAG = "Boa_SimSelectNotification";

    @Override
    public void onReceive(Context context, Intent intent) {
        List<SubscriptionInfo> subs = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoList();
        final int detectedType = intent.getIntExtra(
                SubscriptionManager.INTENT_KEY_DETECT_STATUS, 0);
        Log.d(TAG, "sub info update, type = " + detectedType + ", subs = " + subs);
        if (detectedType == SubscriptionManager.EXTRA_VALUE_NOCHANGE) {
            return;
        }

        final TelephonyManager telephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        final int numSlots = telephonyManager.getSimCount();
        final boolean isInProvisioning = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) == 0;

        // Do not create notifications on single SIM devices or when provisioning i.e. Setup Wizard.
        if (numSlots < 2 || isInProvisioning) {
            Log.d(TAG, "numSlots = " + numSlots + ", isInProvisioning = "  + isInProvisioning);
            return;
        }

        // If sim state is not ABSENT or LOADED then ignore
        String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        if (!(IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simStatus) ||
                IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(simStatus))) {
            Log.d(TAG, "sim state is not Absent or Loaded");
        } else {
            Log.d(TAG, "simstatus = " + simStatus);
        }

        int state;
        for (int i = 0; i < numSlots; i++) {
            state = telephonyManager.getSimState(i);
            if (!(state == TelephonyManager.SIM_STATE_ABSENT
                    || state == TelephonyManager.SIM_STATE_READY
                    || state == TelephonyManager.SIM_STATE_UNKNOWN)) {
                Log.d(TAG, "All sims not in valid state yet");
            }
        }

        List<SubscriptionInfo> sil = subscriptionManager.getActiveSubscriptionInfoList();
        if (sil == null || sil.size() < 1) {
            Log.d(TAG, "Subscription list is empty");
            return;
        }

        // Clear defaults for any subscriptions which no longer exist
        subscriptionManager.clearDefaultsForInactiveSubIds();

        boolean dataSelected = SubscriptionManager.isUsableSubIdValue(
                SubscriptionManager.getDefaultDataSubscriptionId());
        boolean smsSelected = SubscriptionManager.isUsableSubIdValue(
                SubscriptionManager.getDefaultSmsSubscriptionId());
        boolean voiceSelected = SubscriptionManager.isUsableSubIdValue(
                SubscriptionManager.getDefaultVoiceSubscriptionId());
        Log.d(TAG, "dataSelected = " + dataSelected + ", smsSelected = " + dataSelected + ", voiceSelected = " + voiceSelected);

        int subid = sil.get(0).getSubscriptionId();

        if (!dataSelected) {
            subscriptionManager.setDefaultDataSubId(subid);
        }
        
        if (!smsSelected) {
            subscriptionManager.setDefaultSmsSubId(subid);
        }
        
        if (!voiceSelected) {
            subscriptionManager.setDefaultVoiceSubId(subid);
        }
    }
}
