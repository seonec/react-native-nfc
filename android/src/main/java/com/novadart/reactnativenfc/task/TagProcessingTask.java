package com.novadart.reactnativenfc.task;

import android.content.Intent;
import android.nfc.Tag;
import android.os.AsyncTask;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.novadart.reactnativenfc.ReactNativeNFCModule;
import com.novadart.reactnativenfc.parser.TagParser;


public class TagProcessingTask extends AsyncTask<Tag,Void,WritableMap> {
    private final ReactApplicationContext context;
    public TagProcessingTask(ReactApplicationContext context) {
        this.context = context;
    }

    @Override
    protected WritableMap doInBackground(Tag... params) {
        Tag tag = params[0];
        return TagParser.parse(tag);
    }

    @Override
    protected void onPostExecute(WritableMap data) {
        context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(ReactNativeNFCModule.EVENT_NFC_DISCOVERED, data);
    }
}
