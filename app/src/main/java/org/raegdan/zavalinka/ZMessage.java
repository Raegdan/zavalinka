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

/**
 * Message stored in chat storage.
 * <p/>
 * Created by raegdan on 28.07.15.
 */
class ZMessage {
    public static final int DIRECTION_INCOMING = 1;
    public static final int DIRECTION_OUTGOING = -1;
    protected final String mMessageText;
    private final long mTimestamp;
    private final String mRemoteJid;
    private final int mDirection;

    public ZMessage(long timestamp, String remoteJid, int direction, String messageText) {
        this.mTimestamp = timestamp;
        this.mRemoteJid = remoteJid.toLowerCase();
        this.mDirection = direction;
        this.mMessageText = messageText;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public String getMessageText() {
        return mMessageText;
    }

    public String getRemoteJid() {
        return mRemoteJid;
    }

    public int getDirection() {
        return mDirection;
    }
}
