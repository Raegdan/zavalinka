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
import android.content.SharedPreferences;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;

/**
 * Created by raegdan on 26.07.15.
 * <p/>
 * Singleton that maintains persistent XMPP connection.
 */


public class ZXmppConnectionKeeper {

    private final static String SERVER_ADDRESS = "raegdan.org";
    private final static String DOMAIN = SERVER_ADDRESS;
    private final static int STANDARD_XMPP_PORT = 5222;
    private final static String KEY_LOGIN = "login";
    private final static String KEY_PASSWD = "passwd";
    private static ZXmppConnectionKeeper sInstance = new ZXmppConnectionKeeper();
    private org.raegdan.zavalinka.ZMessagesStorage mZMessagesStorageInstance;
    private AbstractXMPPConnection mConnection;
    private String mLogin = "";
    private String mPasswd = "";

    private ZXmppConnectionKeeper() {
        mZMessagesStorageInstance = new ZMessagesStorage();
    }

    public static ZXmppConnectionKeeper getInstance() {
        return sInstance;
    }

    public org.raegdan.zavalinka.ZMessagesStorage getzMessagesStorageInstance() {
        return mZMessagesStorageInstance;
    }

    public void setLogin(String login, Context context) {
        this.mLogin = login;
        writeConnectionConfig(context, KEY_LOGIN, login);
    }

    public void setPasswd(String passwd, Context context) {
        this.mPasswd = passwd;
        writeConnectionConfig(context, KEY_PASSWD, passwd);
    }

    public String getSavedLogin(Context context) {
        return readConnectionConfig(context, KEY_LOGIN);
    }

    public String getSavedPasswd(Context context) {
        return readConnectionConfig(context, KEY_PASSWD);
    }

    public AbstractXMPPConnection getConnection() {
        return mConnection;
    }

    public Boolean login() {
        Routines.debug("login() : login = " + mLogin + " ; passwd = " + mPasswd);
        try {
            Routines.debug("disconnectConnection()");
            disconnectConnection();
            Routines.debug("configureConnection()");
            configureConnection();
            Routines.debug("connectConnection()");
            connectConnection();
        } catch (Exception e) {
            Routines.debug("Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        Routines.debug("login() success");
        return true;
    }

    public void exit() {
        disconnectConnection();
    }

    private void configureConnection() {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(mLogin, mPasswd)
                .setServiceName(DOMAIN)
                .setHost(SERVER_ADDRESS)
                .setPort(STANDARD_XMPP_PORT)
                        // I use self-signed cert, so disabling ssl
                        // To be removed for production
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)

                .build();

        mConnection = new XMPPTCPConnection(config);

        ReconnectionManager rm = ReconnectionManager.getInstanceFor(mConnection);
        rm.setFixedDelay(10);
        rm.enableAutomaticReconnection();

        Roster r = Roster.getInstanceFor(mConnection);
        r.setRosterLoadedAtLogin(true);
        r.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
    }

    /**
     * Connects mConnection.
     *
     * @throws IOException    see XMPPTCPConnection javadocs
     * @throws XMPPException  -- // --
     * @throws SmackException -- // --
     */
    private void connectConnection() throws IOException, XMPPException, SmackException {
        mConnection.connect();
        mConnection.login();
    }

    /**
     * Disconnects XMPP client.
     */
    private void disconnectConnection() {
        if (mConnection == null || !mConnection.isConnected()) return;
        mConnection.disconnect();
    }

    private void writeConnectionConfig(Context context, String key, String value) {
        writeConnectionConfig(context, key, value, true);
    }

    private void writeConnectionConfig(Context context, String key, String value, boolean trimValue) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName() + "_" + this.getClass().getSimpleName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString(key, (trimValue) ? value.trim() : value);
        ed.apply();
    }

    private String readConnectionConfig(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName() + "_" + this.getClass().getSimpleName(), Context.MODE_PRIVATE);
        return sp.getString(key, "");
    }
}
