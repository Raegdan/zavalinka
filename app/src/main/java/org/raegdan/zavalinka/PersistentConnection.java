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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

public class PersistentConnection extends Service implements ConnectionListener, StanzaListener, ChatManagerListener {

    public static final int FOREGROUND_NOTIFICATION_ID = 200;

    public PersistentConnection() {

    }

    private Notification buildForegroundNotification(int iconId, int textId) {
        Notification.Builder nb = new Notification.Builder(this);
        Intent in = new Intent(this, MainActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pi = PendingIntent.getActivity(this, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);

        return nb
                .setContentTitle(getString(R.string.zavalinka_is_running))
                .setContentText(getString(textId))
                .setAutoCancel(false)
                .setContentIntent(pi)
                .setSmallIcon(iconId)

                .getNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Routines.debug("onStartCommand()");

        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(android.R.drawable.presence_online, R.string.online));

        XMPPConnectionKeeper keeper = XMPPConnectionKeeper.getInstance();
        AbstractXMPPConnection connection = keeper.getConnection();

        connection.addConnectionListener(this);
        connection.addAsyncStanzaListener(this, new StanzaTypeFilter(Message.class));

        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        chatManager.setMatchMode(ChatManager.MatchMode.BARE_JID);
        chatManager.addChatListener(this);
//        chatManager.

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Notification that the connection has been successfully connected to the remote endpoint (e.g. the XMPP server).
     * <p>
     * Note that the connection is likely not yet authenticated and therefore only limited operations like registering
     * an account may be possible.
     * </p>
     *
     * @param connection the XMPPConnection which successfully connected to its endpoint.
     */
    @Override
    public void connected(XMPPConnection connection) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(android.R.drawable.presence_offline, R.string.online_noauth));
    }

    /**
     * Notification that the connection has been authenticated.
     *
     * @param connection the XMPPConnection which successfully authenticated.
     * @param resumed    true if a previous XMPP session's stream was resumed.
     */
    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(android.R.drawable.presence_online, R.string.online));
    }

    /**
     * Notification that the connection was closed normally.
     */
    @Override
    public void connectionClosed() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(android.R.drawable.presence_offline, R.string.disconnected));
    }

    /**
     * Notification that the connection was closed due to an exception. When
     * abruptly disconnected it is possible for the connection to try reconnecting
     * to the server.
     *
     * @param e the exception.
     */
    @Override
    public void connectionClosedOnError(Exception e) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(android.R.drawable.presence_offline, R.string.disconnected));
    }

    /**
     * The connection has reconnected successfully to the server. Connections will
     * reconnect to the server when the previous socket connection was abruptly closed.
     */
    @Override
    public void reconnectionSuccessful() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(android.R.drawable.presence_offline, R.string.online_noauth));
    }

    @Override
    public void reconnectingIn(int seconds) {

    }

    @Override
    public void reconnectionFailed(Exception e) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(android.R.drawable.presence_offline, R.string.reconnect_failed));
    }

    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {

    }

    /**
     * Event fired when a new chat is created.
     *
     * @param chat           the chat that was created.
     * @param createdLocally true if the chat was created by the local user and false if it wasn't.
     */
    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {

    }
}
