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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * Stores messaging thread between one local and one remote account (JID).
 * <p/>
 * Created by raegdan on 28.07.15.
 */
public class ZMessagesThread extends LinkedList<ZMessage> {
    public void addTimeWise(ZMessage msg) {

        this.add(msg);
        Collections.sort(this, new TimeWiseComparator());
    }

    private class TimeWiseComparator implements Comparator<ZMessage> {

        @Override
        public int compare(ZMessage lhs, ZMessage rhs) {
            return Long
                    .valueOf(lhs.getTimestamp())
                    .compareTo(rhs.getTimestamp());
        }
    }
}
