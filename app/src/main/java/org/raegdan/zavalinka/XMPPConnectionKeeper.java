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
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;

/**
 * Created by raegdan on 26.07.15.
 * <p/>
 * Singleton that maintains persistent XMPP connection.
 */


public class XMPPConnectionKeeper {

    /*
     *   P U B L I C   P A R T
     */

    // Constants
    private final static String SERVER_ADDRESS = "raegdan.org";
    private final static String DOMAIN = SERVER_ADDRESS;
    private final static int STANDARD_XMPP_PORT = 5222;
    private final static String KEY_LOGIN = "login";
    private final static String KEY_PASSWD = "passwd";
    // Singleton instance
    private static XMPPConnectionKeeper ourInstance = new XMPPConnectionKeeper();


    /*
     *   P R I V A T E   P A R T
     */
    private AbstractXMPPConnection mConnection;
    // Class state flags
    private Boolean mConnectionConfigured = false;
    private Boolean mConnectionConnected = false;
    // Account credentials
    private String mLogin = "";
    private String mPasswd = "";

    private XMPPConnectionKeeper() {

    }

    public static XMPPConnectionKeeper getInstance() {
        return ourInstance;
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

    public Boolean login() {
        if (!mConnectionConfigured) try {
            configureConnection();
        } catch (ConnectionConfigurationException e) {
            e.printStackTrace();
            return false;
        }

        if (mConnectionConnected) try {
            disconnect();
        } catch (ConnectionNotConfiguredException e) {
            e.printStackTrace();
            return false;
        }

        try {
            connect();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void configureConnection() throws ConnectionConfigurationException {

        if (mConnectionConnected) {
            try {
                disconnect();
            } catch (ConnectionNotConfiguredException e) {
                // Should never occur unless static bug
                e.printStackTrace();
            }
        }

        if (mConnectionConfigured) mConnectionConfigured = false;

        if (!Routines.stringsContainData(mLogin, mPasswd)) {
            throw new ConnectionConfigurationException("Please set login and password with respective setters.");
        }
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(mLogin, mPasswd)
                .setServiceName(DOMAIN)
                .setHost(SERVER_ADDRESS)
                .setPort(STANDARD_XMPP_PORT)

                .build();

        mConnection = new XMPPTCPConnection(config);

        mConnectionConfigured = true;
    }

    /**
     * Connects mConnection.
     *
     * @throws ConnectionNotConfiguredException configureConnection() not called prior to connect() or failed.
     * @throws IOException                      see XMPPTCPConnection javadocs
     * @throws XMPPException                    -- // --
     * @throws SmackException                   -- // --
     * @throws NullPointerException             mConnection not initialized!
     */
    private void connect() throws ConnectionNotConfiguredException, IOException, XMPPException, SmackException, NullPointerException {

        if (!mConnectionConfigured) {
            throw new ConnectionNotConfiguredException("Cannot connect() unconfigured connection, call configureConnection() first.");
        }

        // TODO disconnect first if connected -- Raegdan

        mConnectionConnected = false;

        if (mConnection == null) {
            throw new NullPointerException("mConnection is null -- call configureConnection() first.");
        }

        mConnection.connect();
        mConnection.login();

        mConnectionConnected = true;
    }

    /**
     * @throws ConnectionNotConfiguredException configureConnection() not called prior to connect() or failed.
     * @throws NullPointerException             mConnection not initialized!
     */
    private void disconnect() throws NullPointerException, ConnectionNotConfiguredException {
        if (!mConnectionConfigured) {
            throw new ConnectionNotConfiguredException("Cannot disconnect() unconfigured connection, call configureConnection() first.");
        }

        if (mConnection == null) {
            throw new NullPointerException("mConnection is null -- call configureConnection() first.");
        }

        mConnection.disconnect();

        mConnectionConnected = false;
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
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getString(key, "");
    }

    /**
     * To be thrown on attempts to use unconfigured mConnection.
     */
    private class ConnectionNotConfiguredException extends Exception {
        public ConnectionNotConfiguredException(String detailMessage) {
            super(detailMessage);
        }
    }

    private class ConnectionConfigurationException extends Exception {
        public ConnectionConfigurationException(String detailMessage) {
            super(detailMessage);
        }
    }
}
