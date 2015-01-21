/*
 * Copyright (C) 2012-2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
                // Do nothing here
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
                // Do nothing here
            }
        }
        return availableUpdates;
    }
}
