package com.novadart.reactnativenfc.task;

import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;

import com.facebook.react.bridge.Callback;

import java.io.IOException;


public class IsTagAvailableTask extends AsyncTask<Tag,Void,Boolean> {
    private final Callback callback;


    public IsTagAvailableTask(Callback callback) {
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Tag... params) {
        Tag tag = params[0];
        NfcA nfc = NfcA.get(tag);
        try {
            nfc.connect();
            nfc.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean data) {
        callback.invoke(data);
    }
}