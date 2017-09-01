package com.novadart.reactnativenfc.task;

import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.novadart.reactnativenfc.ReactNativeNFCModule;
import com.novadart.reactnativenfc.parser.NdefParser;
import com.novadart.reactnativenfc.parser.TagParser;


public class NdefProcessingTask extends AsyncTask<Object,Void,WritableMap> {
    private final ReactApplicationContext context;
    public NdefProcessingTask(ReactApplicationContext context) {
        this.context = context;
    }

    @Override
    protected WritableMap doInBackground(Object... params) {
        NdefMessage[] messages = (NdefMessage[]) params[0];
        Tag tag = (Tag) params[1];
        WritableMap wmap = NdefParser.parse(messages,tag);
        wmap.merge(TagParser.parse(tag));
        return wmap;
    }

    @Override
    protected void onPostExecute(WritableMap data) {
        Log.e("ReactNativeNFCModule","Emitting ndef + tag event");
        context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(ReactNativeNFCModule.EVENT_NFC_DISCOVERED, data);
    }
}