'use strict';

import { NativeModules, DeviceEventEmitter } from 'react-native';

export const NfcDataType = {
    NDEF : "NDEF",
    TAG : "TAG"
};

export const NdefRecordType = {
    TEXT : "TEXT",
    URI : "URI",
    MIME : "MIME"
};


let _registeredToEvents = false;
const _listeners = [];
const _commandListeners = [];

let _registerToEvents = () => {
    if(!_registeredToEvents){
        NativeModules.ReactNativeNFC.getStartUpNfcData(_notifyListeners);
        DeviceEventEmitter.addListener('__NFC_DISCOVERED', _notifyListeners);
        DeviceEventEmitter.addListener('__NFC_COMMAND', _notifyCommandListeners);
        _registeredToEvents = true;
    }
};

let _notifyListeners = (data) => {
    if(data){
        for(let i in _listeners){
            _listeners[i](data);
        }
    }
};

let _notifyCommandListeners = (data) => {
    if(data){
        for(let i in _commandListeners){
            _commandListeners[i](data);
        }
    }
};

const NFC = {};

NFC.addListener = (callback) => {
    _listeners.push(callback);
    _registerToEvents();
};

NFC.addCommandListener = (callback) => {
    _commandListeners.push(callback);
    _registerToEvents();
};

  //comandi: http://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf
NFC.sendCommand = (command) => {
	return NativeModules.ReactNativeNFC.sendCommand(command);
}

NFC.readPage = (pageNum, callback) => {
    NativeModules.ReactNativeNFC.sendCommandWithCallback(["30",pageNum],callback);
}

NFC.readUID = (callback) => {
    NativeModules.ReactNativeNFC.sendCommandWithCallback(["30","0"],(data) => {
        //legge le prime 4 pagine, a noi servono i bytes 0-2 + 4-7
        let uid = data.substr(0,6)+""+data.substr(8,8);
        callback(uid);
    });
}

NFC.readVersion = (callback) => {
    NativeModules.ReactNativeNFC.sendCommandWithCallback(["60"],callback);
}

NFC.readSignature = (callback) => {
    NativeModules.ReactNativeNFC.sendCommandWithCallback(["3C","00"],callback);
}

export default NFC;