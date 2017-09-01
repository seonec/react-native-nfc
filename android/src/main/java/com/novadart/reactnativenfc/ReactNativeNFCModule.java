package com.novadart.reactnativenfc;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.novadart.reactnativenfc.handler.BaseNFCHandler;
import com.novadart.reactnativenfc.handler.NdefHandler;
import com.novadart.reactnativenfc.handler.TagHandler;
import com.novadart.reactnativenfc.task.IsTagAvailableTask;
import com.novadart.reactnativenfc.task.SendNFCACommandTask;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ReactNativeNFCModule extends ReactContextBaseJavaModule implements ActivityEventListener,LifecycleEventListener {

    public static final String EVENT_NFC_DISCOVERED = "__NFC_DISCOVERED";

    // caches the last message received, to pass it to the listeners when it reconnects
    private WritableMap startupNfcData;
    private boolean startupNfcDataRetrieved = false;

    private boolean startupIntentProcessed = false;

    private NfcAdapter mNfcAdapter;

    private List<BaseNFCHandler> handlers;
    private NdefHandler ndefHandler;
    private TagHandler tagHandler;
    private Callback cb = null;

    public ReactNativeNFCModule(ReactApplicationContext reactContext) {
        super(reactContext);

        ndefHandler = new NdefHandler(reactContext);
        tagHandler = new TagHandler(reactContext);

        handlers = new LinkedList<>();
        handlers.add(ndefHandler);
        handlers.add(tagHandler);

        reactContext.addActivityEventListener(this);
        reactContext.addLifecycleEventListener(this);
        Log.d("ReactNativeNFCModule", "Starting");
        onHostResume();
    }

    @Override
    public String getName() {
        return "ReactNativeNFC";
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    }

    @Override
    public void onNewIntent(Intent intent) {
        handleIntent(intent,false);
    }

    private void handleIntent(Intent intent, boolean startupIntent) {
        Log.d("ReactNativeNFCModule", "Incoming intent "+intent.getAction());
        for (BaseNFCHandler handler : handlers) {
            handler.handle(intent);
        }
        Log.d("ReactNativeNFCModule", "End intent handling "+intent.getAction());
    }

    private Tag getReadTag(){
        if (tagHandler.getTag() != null) {
            return tagHandler.getTag();
        } else if (ndefHandler.getTag() != null) {
            return ndefHandler.getTag();
        } else {
            return null;
        }
    }

    /**
     * This method is used to retrieve the NFC data was acquired before the React Native App was loaded.
     * It should be called only once, when the first listener is attached.
     * Subsequent calls will return null;
     *
     * @param callback callback passed by javascript to retrieve the nfc data
     */
    @ReactMethod
    public void getStartUpNfcData(Callback callback){
        if(!startupNfcDataRetrieved){
            callback.invoke(DataUtils.cloneWritableMap(startupNfcData));
            startupNfcData = null;
            startupNfcDataRetrieved = true;
        } else {
            callback.invoke();
        }
    }


    @ReactMethod
    public void sendCommandWithCallback(ReadableArray command, Callback callback) {
        if (getReadTag() == null) {
            return;
        }
        NfcA nfc = NfcA.get(getReadTag());
        try {
            if (!nfc.isConnected()) {
                nfc.connect();
            }
        } catch (IOException e) {
            return;
        }
        String[] commandArray = new String[command.size()];
        for (int i = 0; i < command.size(); i++) {
            commandArray[i] = command.getString(i);
        }
        SendNFCACommandTask task = new SendNFCACommandTask(getReactApplicationContext(),DataUtils.convertStringArrayToByteArray(commandArray),callback);
        task.execute(nfc);
    }

    @ReactMethod
    public void isTagAvailable(Callback callback) {
        if (getReadTag() == null) {
            callback.invoke(false);
            return;
        }
        IsTagAvailableTask task = new IsTagAvailableTask(callback);
        task.execute(getReadTag());
    }

    @Override
    public void onHostResume() {
        if(!startupIntentProcessed){
            if(getReactApplicationContext().getCurrentActivity() != null){
                // necessary because NFC might cause the activity to start and we need to catch that data too
                handleIntent(getReactApplicationContext().getCurrentActivity().getIntent(),true);
            }
            startupIntentProcessed = true;
        }

        if (mNfcAdapter != null) {
            setupForegroundDispatch(getCurrentActivity(), mNfcAdapter);
        } else {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(getReactApplicationContext());
        }
    }

    @Override
    public void onHostPause() {
        if (mNfcAdapter != null) {
            stopForegroundDispatch(getCurrentActivity(), mNfcAdapter);
        }
    }

    @Override
    public void onHostDestroy() { }

    public void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        Log.e("ReactNativeNFCModule", "Setup foreground dispatch");
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
        adapter.enableForegroundDispatch(activity, pendingIntent, null, null);
    }

    public void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }
}
