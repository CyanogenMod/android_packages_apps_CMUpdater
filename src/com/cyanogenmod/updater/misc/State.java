/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import com.cyanogenmod.updater.customTypes.FullUpdateInfo;
import com.cyanogenmod.updater.customization.Customization;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class State {
    private static final String TAG = "State";

    public static void saveState(Context ctx, Serializable mAvailableUpdates) throws IOException {
        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
            File f = new File(ctx.getCacheDir(), Customization.STORED_STATE_FILENAME);
            fos = new FileOutputStream(f);
            oos = new ObjectOutputStream(fos);
            Map<String, Serializable> data = new HashMap<String, Serializable>();
            data.put(Constants.KEY_AVAILABLE_UPDATES, mAvailableUpdates);
            oos.writeObject(data);
            oos.flush();
        } catch (Exception ex) {
            Log.e(TAG, "Exception on saving Instance State", ex);
        } finally {
            if (oos != null) {
                oos.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static FullUpdateInfo loadState(Context ctx) throws IOException {
        FullUpdateInfo availableUpdates = new FullUpdateInfo();
        ObjectInputStream ois = null;
        FileInputStream fis = null;
        try {
            File f = new File(ctx.getCacheDir(), Customization.STORED_STATE_FILENAME);
            fis = new FileInputStream(f);
            ois = new ObjectInputStream(fis);
            Map<String, Serializable> data = (HashMap<String, Serializable>) ois.readObject();
            Object o = data.get(Constants.KEY_AVAILABLE_UPDATES);
            if (o != null) availableUpdates = (FullUpdateInfo) o;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to load stored class", e);
        } catch (FileNotFoundException ex) {
            Log.i(TAG, "No State Info stored");
        } catch (IOException e) {
            Log.e(TAG, "Exception on Loading State", e);
        } finally {
            if (ois != null) {
                ois.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
        return availableUpdates;
    }
}
