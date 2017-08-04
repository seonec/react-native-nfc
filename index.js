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


NFC.sendCommand = (command) => {
	return NativeModules.ReactNativeNFC.sendCommand(command);
}

export default NFC;