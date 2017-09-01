package com.novadart.reactnativenfc.handler;

import android.content.Intent;
import android.util.Log;

/**
 * Created by AI on 25/08/2017.
 */

public abstract class BaseNFCHandler {
    public final void handle(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            for (String f : getFilteredIntents()) {
                if (intent.getAction().equals(f)) {
                    Log.d("ReactNativeNFCModule", "Intent match: "+this.getClass().toString());
                    doHandle(intent);
                }
            }
        }
    }

    protected abstract void doHandle(Intent intent);

    protected abstract String[] getFilteredIntents();
}
