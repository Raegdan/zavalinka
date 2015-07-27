/*
 * Zavalinka - simple IM developed as an employment challenge.
 * Copyright © Kirill «Raegdan» Fomchenko, 2015. All rights reserved.
 * Email: raegdan-at-raegdan-dot-org.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.raegdan.zavalinka;

import android.content.Context;
import android.widget.Toast;

/**
 * Static library for frequently used routines' aliases.
 * <p/>
 * Created by raegdan on 27.07.15.
 */
public class Routines {
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
}
