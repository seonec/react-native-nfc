package com.novadart.reactnativenfc.task;

import android.nfc.NdefMessage;
import android.os.AsyncTask;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.novadart.reactnativenfc.ReactNativeNFCModule;
import com.novadart.reactnativenfc.parser.NdefParser;


public class NdefProcessingTask extends AsyncTask<NdefMessage[],Void,WritableMap> {
    private final ReactApplicationContext context;
    public NdefProcessingTask(ReactApplicationContext context) {
        this.context = context;
    }

    @Override
    protected WritableMap doInBackground(NdefMessage[]... params) {
        NdefMessage[] messages = params[0];
        return NdefParser.parse(messages);
    }

    @Override
    protected void onPostExecute(WritableMap data) {
        context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(ReactNativeNFCModule.EVENT_NFC_DISCOVERED, data);
    }
}