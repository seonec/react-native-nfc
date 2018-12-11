package com.novadart.reactnativenfc;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.nfc.tech.TagTechnology;
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
import com.novadart.reactnativenfc.task.SendMifareUltralightCommandTask;
import com.novadart.reactnativenfc.task.SendNFCACommandTask;
import com.novadart.reactnativenfc.task.SendNFCVCommandTask;
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


    public static String[] readableArrayToStringArray(ReadableArray readableArray) {
        String[] string = new String[readableArray.size()];
        for (int i = 0; i < readableArray.size(); i++) {
            string[i] = (String)(readableArray.getString(i));
        }
        return string;
    }


    @ReactMethod
    public void sendCommandsWithCallback(ReadableArray commands, Callback callback) {

        if (getReadTag() == null) {
            callback.invoke("tag is null?");
            return;
        }

        try {

            NfcA tech = NfcA.get(getReadTag());
            tech.setTimeout(2000);
            tech.connect();

            String[] commandArray = readableArrayToStringArray(commands);

            byte[] out = new byte[]{};

            for(int i = 0; i < commands.size(); i++) {
                byte[] command = DataUtils.decodeHexString(commandArray[i]);
                out = tech.transceive(command);
            }

            if (tech.isConnected()) {
                tech.close();
            }

            callback.invoke("true");

        } catch (Exception e) {
            callback.invoke("Ops, something went wrong :-(");
        }


    }



    @ReactMethod
    public void sendCommandWithCallback(ReadableArray command, Callback callback) {
        if (getReadTag() == null) {
            return;
        }
        TagTechnology tech;
        tech = MifareUltralight.get(getReadTag());
        if (tech == null) {
            tech = NfcA.get(getReadTag());
        } else if (tech == null) {
            tech = NfcV.get(getReadTag());
        }
        if (tech == null) {
            Log.e("ReactNativeNFCModule","Unrecognized tag tech");
            return;
        }
        try {
            if (!tech.isConnected()) {
                tech.connect();
            }
        } catch (IOException e) {
            Log.e("ReactNativeNFCModule","IO Error while trying to connect to tag: "+e.getMessage());
            return;
        }
        String[] commandArray = new String[command.size()];
        for (int i = 0; i < command.size(); i++) {
            commandArray[i] = command.getString(i);
        }
        if (tech instanceof NfcA) {
            SendNFCACommandTask task = new SendNFCACommandTask(getReactApplicationContext(), DataUtils.convertStringArrayToByteArray(commandArray), callback);
            task.execute((NfcA)tech);
        } else if (tech instanceof NfcV) {
            SendNFCVCommandTask task = new SendNFCVCommandTask(getReactApplicationContext(), DataUtils.convertStringArrayToByteArray(commandArray), callback);
            task.execute((NfcV)tech);
        } else {
            SendMifareUltralightCommandTask task = new SendMifareUltralightCommandTask(getReactApplicationContext(), DataUtils.convertStringArrayToByteArray(commandArray), callback);
            task.execute((MifareUltralight)tech);
        }

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


    public byte[] sendNFCVCommand(NfcV tech, byte[] cmd) {

        // default result
        byte[] result = new byte[]{(byte)0x00};

        if (getReadTag() == null) {
            return result;
        }

        try {
            tech.connect();
            result = tech.transceive(cmd);
            return result;
        } catch(Exception e) {
            closeNFCVconnection(tech);
            //send EE result => error
            result = new byte[]{(byte)0xEE};
            Log.d(TAG, "resultSendLog " + e );
            return result;
        }
    }



    public byte[] getNFCVRandomNumber(NfcV tech) {

        byte[] ID = tech.getTag().getId();

        // addressed
        byte[] cmd = new byte[11];
        cmd[0] = (byte) 0x22;
        cmd[1] = (byte) 0xB2;
        cmd[2] = (byte) 0x04;

        System.arraycopy(ID, 0, cmd, 3, ID.length);

        byte[] randomnumber = sendNFCVCommand(tech, cmd);

        return randomnumber;

    }


    public byte[] xorPwd(byte[] pwdParam, byte[] randomnumber) {
      /*
          The XOR password has to be calculated with the password and two times the received
          random number from the last GET RANDOM NUMBER command:
          XOR_Password[31:0] = Password[31:0] XOR {Random_Number[15:0],Random_Number[15:0]}.
      */

      // ICODE SLIX2 default pwd
      int[] defaultPwd = new int[]{
        (int)0x0F,
        (int)0x0F,
        (int)0x0F,
        (int)0x0F
      };

      int[] doubleRandomNumber = new int[]{
        (int)randomnumber[0],
        (int)randomnumber[1],
        (int)randomnumber[0],
        (int)randomnumber[1]
      };


      int[] pwd = new int[4];

      if (pwdParam.length != 4) {
        pwd = defaultPwd;
      }else{
        for(int i=0; i < pwdParam.length; i++) {
          pwd[i] = (int)pwdParam[i];
        }
      }

      byte[] result = new byte[4];

      int i = 0;
      for (int b : pwd)
        result[i] = (byte)((b ^ doubleRandomNumber[i++]) & 0xff);

      return result;


    }

    public byte[] destroyNFCV(NfcV tech, byte[] xorPwd) {

      byte[] ID = tech.getTag().getId();

      byte[] cmd = new byte[]{
        (byte) 0x62, // flags
        (byte) 0xB9, // cmd
        (byte) 0x04, // IC mfg code
        0,0,0,0,0,0,0,0, // UID
        (byte) xorPwd[0],
        (byte) xorPwd[1],
        (byte) xorPwd[2],
        (byte) xorPwd[3],
      };

      System.arraycopy(ID, 0, cmd, 3, 8);

      byte[] result = sendNFCVCommand(tech, cmd);

      return result;
    }




    public byte[] setPassword(NfcV tech, byte[] xorPwd) {

      byte[] ID = tech.getTag().getId();

      byte[] cmd = new byte[]{
        (byte) 0x62, // flags
        (byte) 0xB3, // cmd
        (byte) 0x04, // IC mfg code
        0,0,0,0,0,0,0,0, // UID
        (byte) 0x08, // pwd identifier 0x08 destroy
        (byte) xorPwd[0],
        (byte) xorPwd[1],
        (byte) xorPwd[2],
        (byte) xorPwd[3],
      };

      System.arraycopy(ID, 0, cmd, 3, 8);

      byte[] result = sendNFCVCommand(tech, cmd);

      return result;
    }


    public Boolean closeNFCVconnection(NfcV tech) {
        try {
            if (tech.isConnected()) {
                tech.close();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    @ReactMethod
    public void destroyNFCV(String pwd, Callback callback) {

        String result = "false";

        NfcV tech = NfcV.get(getReadTag());

        // 3 bytes response
        byte[] randomnumberResponse = getNFCVRandomNumber(tech);
        closeNFCVconnection(tech);



        if (randomnumberResponse.length == 3) {

          //get last two byte. First byte is flag
          byte[] randomnumber = new byte[]{
            (byte) randomnumberResponse[1],
            (byte) randomnumberResponse[2]
          };

          byte[] xorPwd = xorPwd(DataUtils.decodeHexString(pwd), randomnumber);

          // 1 byte response if all right. 2 bytes response in error
          byte[] setPassword = setPassword(tech, xorPwd);
          closeNFCVconnection(tech);

          Log.d(TAG, "setPasswordLog: " + DataUtils.convertByteArrayToHexString(setPassword) + " length" + setPassword.length );

          if (setPassword.length == 1) {
            //byte[] destroy = destroyNFCV(tech, xorPwd);
            //Log.d(TAG, "destroyLog: " + DataUtils.convertByteArrayToHexString(destroy) + " length" + destroy.length );
            result = "true";
          }

        }

        callback.invoke(result);
    }
}
