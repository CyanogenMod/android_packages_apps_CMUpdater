package com.cyanogenmod.updater.interfaces;
import com.cyanogenmod.updater.interfaces.IUpdateCheckServiceCallback;

interface IUpdateCheckService
{    
    void checkForUpdates();
    void registerCallback(in IUpdateCheckServiceCallback cb);
    void unregisterCallback(in IUpdateCheckServiceCallback cb);
}