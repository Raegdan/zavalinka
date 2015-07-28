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
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

public class PersistentConnection extends Service implements ConnectionListener, StanzaListener {

    public static final int FOREGROUND_NOTIFICATION_ID = 200;
    private Notification ONLINE_NOTIFICATION, ONLINE_NOAUTH_NOTIFICATION, DISCONNECTED_NOTIFICATION, RECONNECT_FAILED_NOTIFICATION;
    private AbstractXMPPConnection mConnecton;
    private ZXmppConnectionKeeper mKeeper;

    public PersistentConnection() {

    }

    private void updateForegroundNotification(Notification n) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(FOREGROUND_NOTIFICATION_ID, n);
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

        ONLINE_NOTIFICATION = buildForegroundNotification(android.R.drawable.presence_online, R.string.online);
        ONLINE_NOAUTH_NOTIFICATION = buildForegroundNotification(android.R.drawable.presence_offline, R.string.online_noauth);
        DISCONNECTED_NOTIFICATION = buildForegroundNotification(android.R.drawable.presence_offline, R.string.disconnected);
        RECONNECT_FAILED_NOTIFICATION = buildForegroundNotification(android.R.drawable.presence_offline, R.string.reconnect_failed);

        startForeground(FOREGROUND_NOTIFICATION_ID, ONLINE_NOTIFICATION);

        mKeeper = ZXmppConnectionKeeper.getInstance();
        mConnecton = mKeeper.getConnection();

        mConnecton.addConnectionListener(this);
        mConnecton.addAsyncStanzaListener(this, new StanzaTypeFilter(Message.class));

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
        updateForegroundNotification(
                (connection.isAuthenticated()) ?
                        ONLINE_NOTIFICATION :
                        ONLINE_NOAUTH_NOTIFICATION
        );
    }

    /**
     * Notification that the connection has been authenticated.
     *
     * @param connection the XMPPConnection which successfully authenticated.
     * @param resumed    true if a previous XMPP session's stream was resumed.
     */
    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        updateForegroundNotification(ONLINE_NOTIFICATION);
    }

    /**
     * Notification that the connection was closed normally.
     */
    @Override
    public void connectionClosed() {
        updateForegroundNotification(DISCONNECTED_NOTIFICATION);
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
        updateForegroundNotification(DISCONNECTED_NOTIFICATION);
    }

    /**
     * The connection has reconnected successfully to the server. Connections will
     * reconnect to the server when the previous socket connection was abruptly closed.
     */
    @Override
    public void reconnectionSuccessful() {
        updateForegroundNotification(ONLINE_NOTIFICATION);
    }

    @Override
    public void reconnectingIn(int seconds) {

    }

    @Override
    public void reconnectionFailed(Exception e) {
        updateForegroundNotification(RECONNECT_FAILED_NOTIFICATION);
    }

    private void processMessage(Message m) {
        ZMessagesStorage ms = mKeeper.getzMessagesStorageInstance();

        String account = Routines.stripResourceFromJid(mConnecton.getUser());
        String from = Routines.stripResourceFromJid(m.getFrom());
        String to = Routines.stripResourceFromJid(m.getTo());

        String remote;
        int direction;
        if (!to.equalsIgnoreCase(account) && from.equalsIgnoreCase(account)) {
            remote = to;
            direction = ZMessage.DIRECTION_OUTGOING;

        } else if (!from.equalsIgnoreCase(account) && to.equalsIgnoreCase(account)) {
            remote = from;
            direction = ZMessage.DIRECTION_INCOMING;

        } else if (!to.equalsIgnoreCase(account) && !from.equalsIgnoreCase(account)) {
            // This is not our message
            // TODO may be it's a JID alias?
            return;

        } else {
            // Some other weird message
            return;
        }

        ms.getAccountMessages(account).putMessage(new ZMessage(Routines.getUnixTimestamp(), remote, direction, m.getBody()));
        Routines.debug("PersistentConnection.processMessage(): add account=" + account + " remote=" + remote + "direction=" + Integer.toString(direction) + " text=" + m.getBody());
    }

    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
        if (packet.getClass() == Message.class) {
            processMessage((Message) packet);
        }
    }
}
