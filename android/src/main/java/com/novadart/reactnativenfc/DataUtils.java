package com.novadart.reactnativenfc;


import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

public class DataUtils {

    public static WritableMap cloneWritableMap(WritableMap map){
        if(map == null){
            return null;
        }
        WritableMap r = new WritableNativeMap();

        ReadableMapKeySetIterator iter = map.keySetIterator();
        while (iter.hasNextKey()){
            String key = iter.nextKey();
            ReadableType type = map.getType(key);
            switch (type){
                case Array: r.putArray(key, cloneReadableArray(map.getArray(key))); break;
                case Boolean: r.putBoolean(key, map.getBoolean(key)); break;
                case Map: r.putMap(key, cloneReadableMap(map.getMap(key))); break;
                case Null: r.putNull(key); break;
                case Number: r.putDouble(key, map.getDouble(key)); break;
                case String: r.putString(key, map.getString(key)); break;
            }
        }
        return r;
    }


    public static WritableMap cloneReadableMap(ReadableMap map){
        if(map == null){
            return null;
        }
        WritableMap r = new WritableNativeMap();

        ReadableMapKeySetIterator iter = map.keySetIterator();
        while (iter.hasNextKey()){
            String key = iter.nextKey();
            ReadableType type = map.getType(key);
            switch (type){
                case Array: r.putArray(key, cloneReadableArray(map.getArray(key))); break;
                case Boolean: r.putBoolean(key, map.getBoolean(key)); break;
                case Map: r.putMap(key, cloneReadableMap(map.getMap(key))); break;
                case Null: r.putNull(key); break;
                case Number: r.putDouble(key, map.getDouble(key)); break;
                case String: r.putString(key, map.getString(key)); break;
            }
        }
        return r;
    }


    public static WritableArray cloneWritableArray(WritableArray arr){
        if(arr == null){
            return null;
        }
        WritableArray r = new WritableNativeArray();

        for (int i=0; i<arr.size(); i++){
            ReadableType type = arr.getType(i);
            switch (type){
                case Array: r.pushArray(cloneReadableArray(arr.getArray(i))); break;
                case Boolean: r.pushBoolean(arr.getBoolean(i)); break;
                case Map: r.pushMap(cloneReadableMap(arr.getMap(i))); break;
                case Null: r.pushNull(); break;
                case Number: r.pushDouble(arr.getDouble(i)); break;
                case String: r.pushString(arr.getString(i)); break;
            }
        }

        return r;
    }


    public static WritableArray cloneReadableArray(ReadableArray arr){
        if(arr == null){
            return null;
        }
        WritableArray r = new WritableNativeArray();

        for (int i=0; i<arr.size(); i++){
            ReadableType type = arr.getType(i);
            switch (type){
                case Array: r.pushArray(cloneReadableArray(arr.getArray(i))); break;
                case Boolean: r.pushBoolean(arr.getBoolean(i)); break;
                case Map: r.pushMap(cloneReadableMap(arr.getMap(i))); break;
                case Null: r.pushNull(); break;
                case Number: r.pushDouble(arr.getDouble(i)); break;
                case String: r.pushString(arr.getString(i)); break;
            }
        }

        return r;
    }

    public static String convertByteArrayToHexString(byte[] inarray) {
        if (inarray == null) {
            return "";
        }
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        String out = "";

        for (j = 0; j < inarray.length; ++j) {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    public static byte[] convertStringArrayToByteArray(String[] inarray) {
        if (inarray == null) {
            return new byte[]{};
        }
        byte[] out = new byte[inarray.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = Byte.parseByte(inarray[i],16);
        }
        return out;
    }

    private static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: " + hexChar);
        }
        return digit;
    }

    public static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    public static byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

}
