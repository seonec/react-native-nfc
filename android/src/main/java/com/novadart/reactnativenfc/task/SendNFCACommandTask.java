package com.novadart.reactnativenfc.task;

import android.nfc.tech.NfcA;
import android.os.AsyncTask;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.novadart.reactnativenfc.DataUtils;
import com.novadart.reactnativenfc.ReactNativeNFCModule;

import java.io.IOException;


public class SendNFCACommandTask extends AsyncTask<NfcA,Void,String> {
    private final byte[] command;
    private final ReactApplicationContext context;
    private final Callback callback;


    public SendNFCACommandTask(ReactApplicationContext context, byte[] command, Callback callback) {
        this.context = context;
        this.command = command;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(NfcA... params) {
        NfcA tag = params[0];
        try {
            String out = DataUtils.convertByteArrayToHexString(tag.transceive(command));
            tag.close();
            return out;
        }catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(String data) {
        callback.invoke(data);
    }
}