package com.novadart.reactnativenfc.parser;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.nfc.tech.TagTechnology;
import android.util.Log;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.novadart.reactnativenfc.DataUtils;

import java.io.IOException;

public class TagParser {

    public static WritableMap parse(Tag tag){
        if (tag == null) return null;
        WritableMap result = new WritableNativeMap();


        WritableMap data = new WritableNativeMap();

        WritableArray techList = new WritableNativeArray();
        String[] techListStr = tag.getTechList();
        boolean isNfcA = false;
        boolean isMifareUltraLight = false;
        boolean isNfcV = false;
        boolean isNfcB = false;
        boolean isNfcF = false;
        if(techListStr != null){
            for (String s: techListStr) {
                techList.pushString(s);
                if (s.equals("android.nfc.tech.NfcA")) {
                    isNfcA = true;
                } else if (s.equals("android.nfc.tech.MifareUltralight")) {
                    isMifareUltraLight = true;
                } else if (s.equals("android.nfc.tech.NfcV")) {
                    isNfcV = true;
                } else if (s.equals("android.nfc.tech.NfcB")) {
                    isNfcB = true;
                } else if (s.equals("android.nfc.tech.NfcF")) {
                    isNfcF = true;
                }
            }
        }

        data.putArray("techList",techList);
        data.putString("description",tag.toString());
        data.putString("id", DataUtils.convertByteArrayToHexString(tag.getId()));
        TagTechnology tech = null;
        Log.e("ReactNativeNFCModule", "Trying to recognize tag tech");
        try {

            if (isMifareUltraLight) {
                tech = MifareUltralight.get(tag);
                Log.e("ReactNativeNFCModule", "Tag tech: MifareUltralight");
            } else if (isNfcA) {
                tech = NfcA.get(tag);
                Log.e("ReactNativeNFCModule", "Tag tech: NfcA");
            } else if (isNfcV) {
                tech = NfcV.get(tag);
                Log.e("ReactNativeNFCModule", "Tag tech: NfcV");
            } else if (isNfcB) {
                tech = NfcB.get(tag);
                Log.e("ReactNativeNFCModule", "Tag tech: NfcB");
            } else if (isNfcF) {
                tech = NfcF.get(tag);
                Log.e("ReactNativeNFCModule", "Tag tech: NfcF");
            }
            if (tech == null) {
                throw new IOException("Unrecognized tag tech");
            }

            tech.connect();
            Log.e("ReactNativeNFCModule", "Connected");
            data.putString("signature",DataUtils.convertByteArrayToHexString(readSignature(tech)));
            byte[] versionByteArray = readVersion(tech);
            if (versionByteArray.length == 8) {
                byte v = versionByteArray[6];
                switch (v) {
                    case 15:
                        result.putString("type", "DL_NTAG_213");
                        break;
                    case 17:
                        result.putString("type", "DL_NTAG_215");
                        break;
                    case 19:
                        result.putString("type", "DL_NTAG_216");
                        break;
                }
            }
            data.putString("version",DataUtils.convertByteArrayToHexString(versionByteArray));
            data.putString("memory",DataUtils.convertByteArrayToHexString(readMem(tech)));


        } catch (IOException e) {
            Log.e("ReactNativeNFCModule", "Errore: "+e.getMessage());
        } finally {
            if (tech != null) {
                try {
                    tech.close();
                } catch (IOException e) {

                }
            }
        }
        result.putMap("data",data);
        return result;
    }




    private static byte[] readMem(TagTechnology tech) throws IOException{
        if (tech == null) {
            throw new IOException("Null tech");
        }

        byte[] mem = new byte[60];
        for (int i = 0; i < mem.length/4; i++){
            try {
                byte[] memtemp = readPage(tech, i);
                System.arraycopy(memtemp, 0, mem, i * 4, 4);
            } catch (IOException e){
                Log.e("ReactNativeNFCModule", "Errore: "+e.getMessage());
            }
        }

        return mem;
    }

    private static byte[] readPage(TagTechnology tech, int pageNum) throws IOException{
        Log.e("ReactNativeNFCModule", "Reading page "+pageNum);
        return doCommand(tech,new byte[]{(byte) 0x30, (byte) pageNum});
    }

    private static byte[] readVersion(TagTechnology tech) throws IOException{
        Log.e("ReactNativeNFCModule", "Reading version");
        return doCommand(tech,new byte[]{(byte) 0x60});
    }

    private static byte[] readSignature(TagTechnology tech) throws IOException{
        Log.e("ReactNativeNFCModule", "Reading signature");
        if (tech instanceof NfcV) {
            return doCommand(tech,new byte[]{(byte) 0xBD});
        } else {
            return doCommand(tech, new byte[]{(byte) 0x3c, (byte) 0x0});
        }
    }
    private static byte[] doCommand(TagTechnology tech, byte[] command) throws IOException{
        if (tech instanceof MifareUltralight){
            MifareUltralight mf;
            mf = (MifareUltralight)tech;
            return mf.transceive(command);
        } else if (tech instanceof NfcA){
            NfcA nfca;
            nfca = (NfcA)tech;
            return nfca.transceive(command);
        } else if (tech instanceof NfcV){
            NfcV nfc;
            nfc = (NfcV) tech;
            return nfc.transceive(command);
        }
        return null;
    }
}
