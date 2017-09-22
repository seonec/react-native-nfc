package com.novadart.reactnativenfc.handler;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Parcelable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.novadart.reactnativenfc.NfcDataType;
import com.novadart.reactnativenfc.ReactNativeNFCModule;
import com.novadart.reactnativenfc.task.NdefProcessingTask;
import com.novadart.reactnativenfc.task.TagProcessingTask;


public class TagHandler extends BaseNFCHandler {
    private Tag tag = null;
    private final ReactApplicationContext context;

    public TagHandler(ReactApplicationContext context) {
        this.context = context;
    }

    @Override
    protected void doHandle(Intent intent) {
        tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        TagProcessingTask task = new TagProcessingTask(context);
        task.execute(tag);
    }

    @Override
    protected String[] getFilteredIntents() {
        return new String[]{NfcAdapter.ACTION_TAG_DISCOVERED, NfcAdapter.ACTION_TECH_DISCOVERED};
    }

    public Tag getTag(){
        return tag;
    }
}
