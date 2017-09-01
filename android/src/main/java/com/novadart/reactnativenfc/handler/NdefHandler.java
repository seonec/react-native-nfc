package com.novadart.reactnativenfc.handler;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.novadart.reactnativenfc.task.NdefProcessingTask;

/**
 * Created by AI on 25/08/2017.
 */

public class NdefHandler extends BaseNFCHandler {
    private final ReactApplicationContext context;

    public NdefHandler(ReactApplicationContext context) {
        this.context = context;
    }

    @Override
    protected void doHandle(Intent intent) {
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if (rawMessages != null) {
            NdefMessage[] messages = new NdefMessage[rawMessages.length];
            for (int i = 0; i < rawMessages.length; i++) {
                messages[i] = (NdefMessage) rawMessages[i];
            }
            NdefProcessingTask task = new NdefProcessingTask(context);
            task.execute(messages);
        }
    }

    @Override
    protected String[] getFilteredIntents() {
        return new String[]{NfcAdapter.ACTION_NDEF_DISCOVERED};
    }
}
