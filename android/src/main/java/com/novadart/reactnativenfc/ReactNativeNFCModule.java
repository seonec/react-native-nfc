package com.novadart.reactnativenfc;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
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
import com.novadart.reactnativenfc.task.NdefProcessingTask;
import com.novadart.reactnativenfc.task.SendNFCACommandTask;
import com.novadart.reactnativenfc.task.TagProcessingTask;

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
    private boolean useNfcReaderMode = true;
    private Tag lastTagRead = null;

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
            return lastTagRead;
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

    @ReactMethod
    public void setReaderMode(Boolean value) {
        if (value != useNfcReaderMode) {
            stopNfcReader(getCurrentActivity(), mNfcAdapter);
            useNfcReaderMode = value;
            setupNfcReader(getCurrentActivity(), mNfcAdapter);
        }
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
            setupNfcReader(getCurrentActivity(), mNfcAdapter);
        } else {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(getReactApplicationContext());
        }
    }

    @Override
    public void onHostPause() {
        if (mNfcAdapter != null) {
            stopNfcReader(getCurrentActivity(), mNfcAdapter);
        }
    }

    @Override
    public void onHostDestroy() { }

    public void setupNfcReader(final Activity activity, NfcAdapter adapter) {
        if (useNfcReaderMode) {
            adapter.enableReaderMode(activity,
                    new NfcAdapter.ReaderCallback() {
                        @Override
                        public void onTagDiscovered(Tag tag) {
                            lastTagRead = tag;
                            Ndef ndef = Ndef.get(tag);

                            Log.e("ReactNativeNFCModule","ndef = "+ndef);

                            if (ndef != null) {
                                Log.e("ReactNativeNFCModule","Is NDEF");
                                NdefMessage message = ndef.getCachedNdefMessage();
                                Log.e("ReactNativeNFCModule","message = "+message);
                                NdefProcessingTask task = new NdefProcessingTask(getReactApplicationContext());
                                task.execute(new NdefMessage[]{message},tag);
                            } else {
                                TagProcessingTask task = new TagProcessingTask(getReactApplicationContext());
                                task.execute(tag);
                            }

                        }
                    },
                    NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_NFC_F|NfcAdapter.FLAG_READER_NFC_V|NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
                    ,null);
        } else {
            Log.e("ReactNativeNFCModule", "Setup foreground dispatch");
            final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
            adapter.enableForegroundDispatch(activity, pendingIntent, null, null);
        }
    }

    public void stopNfcReader(final Activity activity, NfcAdapter adapter) {
        if (useNfcReaderMode) {
            adapter.disableReaderMode(activity);
        } else {
            adapter.disableForegroundDispatch(activity);
        }
    }
}
