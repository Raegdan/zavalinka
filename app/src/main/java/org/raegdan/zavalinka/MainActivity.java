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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.packet.RosterPacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener, RosterListener {

    private static final int CODE_REQUEST_AUTH = 100;
    ListView lvRoster;
    private ZXmppConnectionKeeper mKeeper;
    private AbstractXMPPConnection mConnection;
    private Roster mRoster;
    private SimpleAdapter mRosterAdapter;
    private List<Map<String, Object>> mRosterList;

    private void initGui() {
        lvRoster = (ListView) findViewById(R.id.lvRoster);

        lvRoster.setOnItemClickListener(this);
    }

    @Override
    public void entriesAdded(Collection<String> addresses) {

    }

    public void entriesUpdated(Collection<String> addresses) {

    }

    @Override
    public void entriesDeleted(Collection<String> addresses) {

    }

    @Override
    public void presenceChanged(Presence presence) {
        new UpdatePresenceTask().execute(presence);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGui();

        initStage1();
    }

    private void initStage1() {
        mKeeper = ZXmppConnectionKeeper.getInstance();
        mConnection = mKeeper.getConnection();

        if (mConnection == null || !mConnection.isAuthenticated()) {
            startActivityForResult(new Intent(this, AuthActivity.class), CODE_REQUEST_AUTH);
        } else {
            initStage2();
        }
    }

    private void initStage2() {
        Routines.debug("initStage2()");

        Routines.debug("startService(new Intent(getApplicationContext(), PersistentConnection.class))");
        startService(new Intent(this, PersistentConnection.class));

        mConnection = mKeeper.getConnection();
        mRoster = Roster.getInstanceFor(mConnection);

        mRoster.addRosterListener(this);
        // mConnection.addAsyncStanzaListener(this, new );

        mRosterList = new ArrayList<Map<String, Object>>();
        String[] from = {"name", "status"};
        int[] to = {R.id.tvRosterItemName, R.id.ivRosterItemStatus};

        mRosterAdapter = new SimpleAdapter(MainActivity.this, mRosterList, R.layout.roster_item, from, to);
        lvRoster.setAdapter(mRosterAdapter);

        new LoadRosterTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_exit:
                stopService(new Intent(this, PersistentConnection.class));
                mKeeper.exit();
                finish();

                break;

            case R.id.action_logoff:
                stopService(new Intent(this, PersistentConnection.class));
                mKeeper.setLogin("", this);
                mKeeper.setPasswd("", this);
                mKeeper.exit();
                finish();

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            initStage2();
        } else {
            finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    private class MessageEventReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {

        }
    }

    private abstract class RosterTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
        private Map<String, Object> createBlankRosterEntry() {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("name", "");
            result.put("status_image", android.R.drawable.presence_offline);
            result.put("unread_image", 0);
            result.put("is_online", false);
            result.put("has_unread", false);
            return result;
        }

        protected Map<String, Object> updateRosterEntry(Map<String, Object> stub, String name, Presence presence, Boolean hasUnreadMessages) {
            Map<String, Object> result =
                    (stub == null) ? createBlankRosterEntry() : stub;

            if (name != null)
                result.put("name", name);

            if (presence != null) {
                if (presence.getType() == Presence.Type.available) {
                    result.put("status_image", android.R.drawable.presence_online);
                    result.put("is_online", true);
                } else {
                    result.put("status_image", android.R.drawable.presence_offline);
                    result.put("is_online", false);
                }
            }

            if (hasUnreadMessages != null) {
                if (hasUnreadMessages) {
                    result.put("unread_image", android.R.drawable.sym_action_chat);
                    result.put("has_unread", true);
                } else {
                    result.put("unread_image", 0);
                    result.put("has_unread", false);
                }
            }

            return result;
        }

        protected void sortRosterList(List<Map<String, Object>> rosterList) {
            Collections.sort(rosterList, new RosterAlphabeticComparator());
            Collections.sort(rosterList, new RosterPresenceComparator());
            Collections.sort(rosterList, new RosterUnreadComparator());
        }

        private class RosterPresenceComparator implements Comparator<Map<String, Object>> {

            @Override
            public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
                return ((Boolean) rhs.get("is_online")).compareTo((Boolean) lhs.get("is_online"));
            }
        }

        private class RosterUnreadComparator implements Comparator<Map<String, Object>> {

            @Override
            public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
                return ((Boolean) rhs.get("has_unread")).compareTo((Boolean) lhs.get("has_unread"));
            }
        }

        private class RosterAlphabeticComparator implements Comparator<Map<String, Object>> {

            @Override
            public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
                return ((String) lhs.get("name")).compareTo((String) rhs.get("name"));
            }
        }
    }

    private class LoadRosterTask extends RosterTask<Void, Void, Void> {

        private boolean isSubscribedTo(RosterEntry entry) {
            return ((entry.getType() == RosterPacket.ItemType.none || entry.getType() == RosterPacket.ItemType.from)
                    && (entry.getStatus() != RosterPacket.ItemStatus.subscribe && entry.getStatus() != RosterPacket.ItemStatus.SUBSCRIPTION_PENDING));
        }

        private void sendSubcriptionRequest(RosterEntry entry) {
            Presence p = new Presence(Presence.Type.subscribe);
            p.setTo(entry.getUser());
            try {
                mConnection.sendStanza(p);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }

        protected Void doInBackground(Void... params) {

            try {
                mRoster.reloadAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (RosterEntry entry : mRoster.getEntries()) {

                if (!isSubscribedTo(entry)) sendSubcriptionRequest(entry);

                Map<String, Object> rosterEntryMap = super.updateRosterEntry(null, entry.getUser(), mRoster.getPresence(entry.getUser()), null);
                mRosterList.add(rosterEntryMap);

                publishProgress();
            }

            sortRosterList(mRosterList);
            publishProgress();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            mRosterAdapter.notifyDataSetChanged();
        }
    }

    private class UpdatePresenceTask extends RosterTask<Presence, Void, Void> {

        private int findEntry(String name) {
            for (int i = 0; i < mRosterList.size(); i++) {
                if (((String) mRosterList.get(i).get("name")).equalsIgnoreCase(name)) {
                    return i;
                }
            }

            return -1;
        }

        @Override
        protected Void doInBackground(Presence... params) {
            String nameWithoutResource = Routines.stripResourceFromJid(params[0].getFrom());

            int i = findEntry(nameWithoutResource);

            if (i < 0) return null;

            Presence bestPresence = mRoster.getPresence(params[0].getFrom());
            Map<String, Object> updatedEntry = super.updateRosterEntry(mRosterList.get(i), null, bestPresence, null);
            mRosterList.set(i, updatedEntry);
            publishProgress();

            super.sortRosterList(mRosterList);
            publishProgress();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            mRosterAdapter.notifyDataSetChanged();
        }
    }
}
