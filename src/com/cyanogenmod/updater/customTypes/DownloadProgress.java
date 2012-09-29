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

package com.cyanogenmod.updater.customTypes;

public class DownloadProgress {
    private final long downloaded;
    private final int total;
    private final String downloadedText;
    private final String speedText;
    private final String remainingTimeText;

    public DownloadProgress(long _downloaded, int _total, String _downloadedText, String _speedText, String _remainingTimeText) {
        downloaded = _downloaded;
        total = _total;
        downloadedText = _downloadedText;
        speedText = _speedText;
        remainingTimeText = _remainingTimeText;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public int getTotal() {
        return total;
    }

    public String getDownloadedText() {
        return downloadedText;
    }

    public String getSpeedText() {
        return speedText;
    }

    public String getRemainingTimeText() {
        return remainingTimeText;
    }
}