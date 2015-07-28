/*
 * Zavalinka - simple IM developed as an employment challenge.
 * Copyright © 2015 Kirill «Raegdan» Fomchenko <raegdan-at-raegdan-dot-org>.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raegdan.zavalinka;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.regex.Pattern;

/**
 * Static library for frequently used routines' aliases.
 * <p/>
 * Created by raegdan on 27.07.15.
 */
public class Routines {

    private static final Pattern jidResourceStripper = Pattern.compile("/.*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
    // Debug mode - log to logcat
    public static boolean DEBUG = true;
    public static String DEBUG_TAG = "Routines.debug()";

    /**
     * Alias for standard Android toast with SHORT duration.
     *
     * @param context see javadocs for android.widget.Toast
     * @param text    see javadocs for android.widget.Toast
     */
    public static void toast(Context context, String text) {
        toast(context, text, Toast.LENGTH_SHORT);
    }

    /**
     * Alias for standard Android toast with specified duration.
     *
     * @param context  see javadocs for android.widget.Toast
     * @param text     see javadocs for android.widget.Toast
     * @param duration see javadocs for android.widget.Toast
     */
    public static void toast(Context context, String text, int duration) {
        Toast.makeText(context, text, duration).show();
    }

    /**
     * Checks if provided strings contain data (not null and contain something except whitespace).
     *
     * @param stringsToCheck Strings subjected for testing.
     * @return true if all arguments contain data ; false otherwise.
     */
    public static boolean stringsContainData(String... stringsToCheck) {
        for (String s : stringsToCheck) {
            if (s == null || s.trim().equalsIgnoreCase("")) {
                return false;
            }
        }

        return true;
    }

    public static void debug(String debugMsg) {
        if (DEBUG && (debugMsg != null)) {
            Log.d(DEBUG_TAG, debugMsg);
        }
    }

    public static String stripResourceFromJid(String jid) {
        return jidResourceStripper.matcher(jid).replaceAll("");
    }

    public static long getUnixTimestamp() {
        return System.currentTimeMillis() / 1000L;
    }
}
