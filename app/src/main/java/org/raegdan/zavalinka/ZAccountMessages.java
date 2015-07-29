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

import java.util.HashMap;
import java.util.Map;

/**
 * Storage of messages belonging to one local account.
 * <p/>
 * Created by raegdan on 28.07.15.
 */
public class ZAccountMessages {
    private Map<String, ZMessagesThread> mThreadsByRemoteJid;
    private String mAccountJid;

    public ZAccountMessages(String accountJid) {
        this.mAccountJid = accountJid.toLowerCase();
        this.mThreadsByRemoteJid = new HashMap<String, ZMessagesThread>();
    }

    public String getAccountJid() {
        return mAccountJid;
    }

    public void putMessage(ZMessage m) {
        String remoteJid = m.getRemoteJid();

        if (!mThreadsByRemoteJid.containsKey(remoteJid)) {
            mThreadsByRemoteJid.put(remoteJid, new ZMessagesThread());
        }

        mThreadsByRemoteJid.get(remoteJid).addTimeWise(m);
    }

    public ZMessagesThread getThreadByRemoteJid(String remoteJid) {
        return (mThreadsByRemoteJid.containsKey(remoteJid)) ? mThreadsByRemoteJid.get(remoteJid) : null;
    }
}
