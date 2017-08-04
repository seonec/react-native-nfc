package com.novadart.reactnativenfc.parser;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.nfc.tech.TagTechnology;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.novadart.reactnativenfc.DataUtils;
import com.novadart.reactnativenfc.NfcDataType;

import java.io.IOException;

public class TagParser {

    public static WritableMap parse(Tag tag){
        if (tag == null) return null;
        WritableMap result = new WritableNativeMap();

        result.putString("type", NfcDataType.TAG.name());

        WritableMap data = new WritableNativeMap();

        WritableArray techList = new WritableNativeArray();
        String[] techListStr = tag.getTechList();
        boolean isNfcA = false;
        boolean isMifareUltraLight = false;
        if(techListStr != null){
            for (String s: techListStr) {
                techList.pushString(s);
                if (s.equals("android.nfc.tech.NfcA")) {
                    isNfcA = true;
                } else if (s.equals("android.nfc.tech.MifareUltralight")) {
                    isMifareUltraLight = true;
                }
            }
        }

        data.putArray("techList",techList);
        data.putString("description",tag.toString());
        data.putString("id", DataUtils.convertByteArrayToHexString(tag.getId()));
        TagTechnology tech = null;
        try {

            if (isMifareUltraLight) {
                tech = MifareUltralight.get(tag);
            } else if (isNfcA) {
                tech = NfcA.get(tag);
            }
            if (tech == null) {
                throw new IOException("Unrecognized tag tech");
            }
            tech.connect();
            data.putString("memory",DataUtils.convertByteArrayToHexString(readMem(tech)));
            data.putString("version",DataUtils.convertByteArrayToHexString(readVersion(tech)));
            data.putString("signature",DataUtils.convertByteArrayToHexString(readSignature(tech)));

        } catch (IOException e) {
            data.putString("memory",e.getMessage());
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

        byte[] mem = new byte[160];
        int retry = 5;
        for (int i = 0; i < mem.length/4; i++){
            try {
                byte[] memtemp = readPage(tech, i);
                System.arraycopy(memtemp, 0, mem, i * 4, 4);
            } catch (IOException e){
                retry--;
                if (retry <= 0){
                    break;
                } else {
                    i--;
                }
            }
        }

        return mem;
    }

    private static byte[] readPage(TagTechnology tech, int pageNum) throws IOException{
        return doCommand(tech,new byte[]{(byte) 0x30, (byte) pageNum});
    }

    private static byte[] readVersion(TagTechnology tech) throws IOException{
        return doCommand(tech,new byte[]{(byte) 0x60});
    }

    private static byte[] readSignature(TagTechnology tech) throws IOException{
        return doCommand(tech,new byte[]{(byte) 0x3c, (byte) 0x0});
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
        }
        return null;
    }
}
