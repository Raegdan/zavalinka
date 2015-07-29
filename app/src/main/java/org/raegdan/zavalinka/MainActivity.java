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
import android.content.IntentFilter;
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
    private static final int CODE_OPEN_CHAT = 200;
    private final Object mRosterTaskLock = new Object();
    private ListView lvRoster;
    private UnreadMessageEventsReceiver mUnreadMessageEventsReceiver;
    private BroadcastReceiver mChatActivityStartedReceiver, mChatActivityStoppedReceiver;
    private ZXmppConnectionKeeper mKeeper;
    private AbstractXMPPConnection mConnection;
    private Roster mRoster;
    private SimpleAdapter mRosterAdapter;
    private List<Map<String, Object>> mRosterList;
    private Boolean mRosterTaskLocked = false;

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
        startPersistentConnectionService();

        mConnection = mKeeper.getConnection();
        mRoster = Roster.getInstanceFor(mConnection);

        mRoster.addRosterListener(this);
        // mConnection.addAsyncStanzaListener(this, new );

        mRosterList = new ArrayList<Map<String, Object>>();
        String[] from = {"name", "status_image", "unread_image"};
        int[] to = {R.id.tvRosterItemName, R.id.ivRosterItemStatus, R.id.ivRosterItemUnreadIndicator};

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

    private void startPersistentConnectionService() {
        Intent i = new Intent(this, PersistentConnection.class);
        i.putExtra("action", PersistentConnection.ACTION_START);
        startService(i);
    }

    private void terminatePersistentConnectionService() {
        Intent i = new Intent(this, PersistentConnection.class);
        i.putExtra("action", PersistentConnection.ACTION_TERMINATE);
        startService(i);
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
                terminatePersistentConnectionService();
                mKeeper.exit();
                finish();

                break;

            case R.id.action_logoff:
                terminatePersistentConnectionService();
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

        switch (requestCode) {
            case CODE_REQUEST_AUTH:
                if (resultCode == RESULT_OK) {
                    initStage2();
                } else {
                    finish();
                }

                break;
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String remoteJid = (String) mRosterList.get(position).get("name");

        startChatActivity(remoteJid);
    }

    private void startChatActivity(String remoteJid) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("remote_jid", remoteJid);
        startActivity(i);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mUnreadMessageEventsReceiver = new UnreadMessageEventsReceiver();

        registerReceiver(mUnreadMessageEventsReceiver, new IntentFilter("org.raegdan.zavalinka.UNREAD_MESSAGE_EVENT"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mUnreadMessageEventsReceiver);
    }

    private class UnreadMessageEventsReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            new UpdateUnreadCountTask().execute(intent.getExtras());
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

        Map<String, Object> updateRosterEntry(Map<String, Object> stub, String name, Presence presence, Boolean hasUnreadMessages) {
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

        List<Map<String, Object>> getWorkingRosterList(List<Map<String, Object>> mainRosterList) {
            List<Map<String, Object>> workingRosterList = new ArrayList<Map<String, Object>>();
            workingRosterList.addAll(mainRosterList);
            return workingRosterList;
        }

        void mergeWorkingRosterList(List<Map<String, Object>> workingRosterList, List<Map<String, Object>> mainRosterList) {
            mainRosterList.clear();
            mainRosterList.addAll(workingRosterList);
        }

        void sortRosterList(List<Map<String, Object>> rosterList) {
            Collections.sort(rosterList, new RosterAlphabeticComparator());
            Collections.sort(rosterList, new RosterPresenceComparator());
            Collections.sort(rosterList, new RosterUnreadComparator());
        }

        int findEntry(String name, List<Map<String, Object>> rosterList) {
            for (int i = 0; i < rosterList.size(); i++) {
                if (((String) rosterList.get(i).get("name")).equalsIgnoreCase(name)) {
                    return i;
                }
            }

            return -1;
        }

        void lock() {
            while (mRosterTaskLocked) {
                try {
                    mRosterTaskLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mRosterTaskLocked = true;
        }

        void unlock() {
            mRosterTaskLocked = false;
            mRosterTaskLock.notifyAll();
        }

        Object getLockManager() {
            return mRosterTaskLock;
        }

        protected void finishRosterTask(List<Map<String, Object>> workingRosterList) {
            if (workingRosterList != null) {
                mergeWorkingRosterList(workingRosterList, mRosterList);
                mRosterAdapter.notifyDataSetChanged();
            }

            unlock();
        }

        private class RosterPresenceComparator implements Comparator<Map<String, Object>> {

            public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
                return ((Boolean) rhs.get("is_online")).compareTo((Boolean) lhs.get("is_online"));
            }
        }

        private class RosterUnreadComparator implements Comparator<Map<String, Object>> {

            public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
                return ((Boolean) rhs.get("has_unread")).compareTo((Boolean) lhs.get("has_unread"));
            }
        }

        private class RosterAlphabeticComparator implements Comparator<Map<String, Object>> {

            public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
                return ((String) lhs.get("name")).compareTo((String) rhs.get("name"));
            }
        }
    }

    private class LoadRosterTask extends RosterTask<Void, Void, List<Map<String, Object>>> {

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

        protected List<Map<String, Object>> doInBackground(Void... params) {
            synchronized (getLockManager()) {
                lock();

                List<Map<String, Object>> workingRosterList = getWorkingRosterList(mRosterList);
                try {
                    mRoster.reloadAndWait();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (RosterEntry entry : mRoster.getEntries()) {

                    if (!isSubscribedTo(entry)) sendSubcriptionRequest(entry);

                    Map<String, Object> rosterEntryMap = updateRosterEntry(null, entry.getUser(), mRoster.getPresence(entry.getUser()), null);
                    workingRosterList.add(rosterEntryMap);
                }

                sortRosterList(workingRosterList);

                return workingRosterList;
            }
        }

        @Override
        protected void onPostExecute(List<Map<String, Object>> workingRosterList) {
            synchronized (getLockManager()) {
                super.onPostExecute(workingRosterList);

                finishRosterTask(workingRosterList);
            }
        }
    }

    private class UpdatePresenceTask extends RosterTask<Presence, Void, List<Map<String, Object>>> {

        @Override
        protected List<Map<String, Object>> doInBackground(Presence... params) {
            synchronized (getLockManager()) {
                lock();

                List<Map<String, Object>> workingRosterList = getWorkingRosterList(mRosterList);

                String nameWithoutResource = Routines.stripResourceFromJid(params[0].getFrom());

                int i = findEntry(nameWithoutResource, workingRosterList);

                if (i < 0) return null;

                Presence bestPresence = mRoster.getPresence(params[0].getFrom());
                Map<String, Object> updatedEntry = updateRosterEntry(workingRosterList.get(i), null, bestPresence, null);
                workingRosterList.set(i, updatedEntry);
                publishProgress();

                sortRosterList(workingRosterList);

                return workingRosterList;
            }
        }

        @Override
        protected void onPostExecute(List<Map<String, Object>> workingRosterList) {
            synchronized (getLockManager()) {
                super.onPostExecute(workingRosterList);

                finishRosterTask(workingRosterList);
            }
        }
    }

    private class UpdateUnreadCountTask extends RosterTask<Bundle, Void, List<Map<String, Object>>> {

        @Override
        protected List<Map<String, Object>> doInBackground(Bundle... params) {
            synchronized (getLockManager()) {
                lock();

                List<Map<String, Object>> workingRosterList = getWorkingRosterList(mRosterList);

                if (!params[0].containsKey("remote_jid")) return null;
                String remoteJid = params[0].getString("remote_jid");
                if (remoteJid == null) return null;

                if (!params[0].containsKey("has_unread")) return null;
                Integer hasUnread = params[0].getInt("has_unread", -1);
                if (hasUnread < 0 || hasUnread > 1) return null;

                int i = findEntry(remoteJid, workingRosterList);
                if (i < 0) return null;

                Map<String, Object> newEntry = updateRosterEntry(workingRosterList.get(i), null, null, (hasUnread == 1));
                workingRosterList.set(i, newEntry);
                publishProgress();

                sortRosterList(workingRosterList);
                publishProgress();

                return workingRosterList;
            }
        }

        @Override
        protected void onPostExecute(List<Map<String, Object>> workingRosterList) {
            synchronized (getLockManager()) {
                super.onPostExecute(workingRosterList);

                finishRosterTask(workingRosterList);
            }
        }
    }
}
