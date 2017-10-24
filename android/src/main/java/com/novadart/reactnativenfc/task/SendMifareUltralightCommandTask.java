package com.novadart.reactnativenfc.task;

import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcV;
import android.nfc.tech.TagTechnology;
import android.os.AsyncTask;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.novadart.reactnativenfc.DataUtils;

import java.io.IOException;


public class SendMifareUltralightCommandTask extends AsyncTask<MifareUltralight,Void,String> {
    private final byte[] command;
    private final ReactApplicationContext context;
    private final Callback callback;


    public SendMifareUltralightCommandTask(ReactApplicationContext context, byte[] command, Callback callback) {
        this.context = context;
        this.command = command;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(MifareUltralight... params) {
        MifareUltralight tag = params[0];
        try {
            String out = DataUtils.convertByteArrayToHexString(tag.transceive(command));
            tag.close();
            return out;
        }catch (IOException e) {
            Log.e("ReactNativeNFCModule", "Error: "+e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(String data) {
        callback.invoke(data);
    }
}