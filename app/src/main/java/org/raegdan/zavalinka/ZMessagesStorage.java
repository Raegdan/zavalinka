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
 * Class that takes, stores and returns chat messages,
 * <p/>
 * Created by raegdan on 28.07.15.
 */
public class ZMessagesStorage {
    protected Map<String, ZAccountMessages> accountMessages;

    public ZMessagesStorage() {
        accountMessages = new HashMap<String, ZAccountMessages>();
    }

    public ZAccountMessages getAccountMessages(String accountJid) {
        if (!accountMessages.containsKey(accountJid)) {
            accountMessages.put(accountJid, new ZAccountMessages(accountJid));
        }

        return accountMessages.get(accountJid);
    }
}
