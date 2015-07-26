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

    /**
     * To be thrown on attempts to use unconfigured mConnection.
     */
    private class ConnectionNotConfiguredException extends Exception {
        public ConnectionNotConfiguredException(String detailMessage) {
            super(detailMessage);
        }
    }


    private final static String SERVER_ADDRESS = "raegdan.org";
    private final static int STANDARD_XMPP_PORT = 5222;


    private static final int ERROR_GENERAL = 1;
    private static final int ERROR_CONNECTION_NOT_CONFIGURED = 2;

    private AbstractXMPPConnection mConnection;

    // Class state flags
    private Boolean mConnectionConfigured = false;
    private Boolean mConnectionConnected = false;


    private static XMPPConnectionKeeper ourInstance = new XMPPConnectionKeeper();

    public static XMPPConnectionKeeper getInstance() {
        return ourInstance;
    }

    private XMPPConnectionKeeper() {

    }

    /**
     * Simplified variant of configureConnection().
     * Assumes: domain = server = SERVER_ADDRESS ; tcpPort = STANDARD_XMPP_PORT
     *
     * @param login     Username. Assuming "johndoe@doefamily.com" : "johndoe".
     * @param passwd    Password.
     */
    private void configureConnection(String login, String passwd) {
        configureConnection(login, passwd, SERVER_ADDRESS, SERVER_ADDRESS, STANDARD_XMPP_PORT);
    }

    /**
     * Configure connection parameters
     *
     * @param login     Username. Assuming "johndoe@doefamily.com" : "johndoe".
     * @param passwd    Password.
     * @param domain    Domain. Assuming "johndoe@doefamily.com" : "doefamily.com".
     * @param server    XMPP server. May differ from domain."johndoe@doefamily.com" may have "john-s-jabber.doefamily.com" or so.
     * @param tcpPort   TCP port, standard is 5222.
     */
    private void configureConnection(String login, String passwd, String domain, String server, int tcpPort) {

        // TODO disconnect first if connected -- Raegdan

        mConnectionConfigured = false;

        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(login, passwd)
                .setServiceName(domain)
                .setHost(server)
                .setPort(tcpPort)

                .build();

        mConnection = new XMPPTCPConnection(config);

        mConnectionConfigured = true;
    }

    /**
     * Connects mConnection.
     *
     * @throws ConnectionNotConfiguredException     configureConnection() not called prior to connect() or failed.
     * @throws IOException                          see XMPPTCPConnection javadocs
     * @throws XMPPException                        -- // --
     * @throws SmackException                       -- // --
     * @throws NullPointerException                 mConnection not initialized!
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

        mConnectionConnected = true;
    }

    /**
     *
     * @throws ConnectionNotConfiguredException     configureConnection() not called prior to connect() or failed.
     * @throws NullPointerException                 mConnection not initialized!
     */
    private void disconnect() throws ConnectionNotConfiguredException, NullPointerException {
        if (!mConnectionConnected || !mConnectionConfigured) {
            return;     // Harmless case, throwing exception not needed
        }

        if (mConnection == null) {
            throw new NullPointerException("mConnection is null -- call configureConnection() first.");
        }

        mConnection.disconnect();

        mConnectionConnected = false;
    }
}
