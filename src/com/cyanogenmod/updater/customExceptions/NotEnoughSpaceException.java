/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at http://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.customExceptions;

public class NotEnoughSpaceException extends Exception {
    private static final long serialVersionUID = 658447306729869141L;

    public NotEnoughSpaceException(String msg) {
        super(msg);
    }
}
