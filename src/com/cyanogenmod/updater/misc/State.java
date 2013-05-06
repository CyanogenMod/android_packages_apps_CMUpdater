/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.misc;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

public class State {
    private static final String TAG = "State";
    private static final String FILENAME = "cmupdater.state";

    public static void saveState(Context context, LinkedList<UpdateInfo> availableUpdates) {
        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
            File f = new File(context.getCacheDir(), FILENAME);
            fos = new FileOutputStream(f);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(availableUpdates);
            oos.flush();
        } catch (IOException e) {
            Log.e(TAG, "Exception on saving instance state", e);
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                // ignored, can't do anything anyway
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static LinkedList<UpdateInfo> loadState(Context context) {
        LinkedList<UpdateInfo> availableUpdates = new LinkedList<UpdateInfo>();
        ObjectInputStream ois = null;
        FileInputStream fis = null;
        try {
            File f = new File(context.getCacheDir(), FILENAME);
            fis = new FileInputStream(f);
            ois = new ObjectInputStream(fis);

            Object o = ois.readObject();
            if (o != null && o instanceof LinkedList<?>) {
                availableUpdates = (LinkedList<UpdateInfo>) o;
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to load stored class", e);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Unexpected state file format", e);
        } catch (FileNotFoundException e) {
            Log.i(TAG, "No state info stored");
        } catch (IOException e) {
            Log.e(TAG, "Exception on loading state", e);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                // ignored, can't do anything anyway
            }
        }
        return availableUpdates;
    }
}
