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
    private XMPPConnectionKeeper mKeeper;
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
        mKeeper = XMPPConnectionKeeper.getInstance();
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

    private class RosterPresenceComparator implements Comparator<Map<String, Object>> {

        @Override
        public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
            return ((Integer) rhs.get("status_prio")).compareTo((Integer) lhs.get("status_prio"));
        }
    }

    private class RosterAlphabeticComparator implements Comparator<Map<String, Object>> {

        @Override
        public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
            return ((String) lhs.get("name")).compareTo((String) rhs.get("name"));
        }
    }

    private class LoadRosterTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            try {
                mRoster.reloadAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (RosterEntry entry : mRoster.getEntries()) {
                Map<String, Object> rosterEntryMap = new HashMap<String, Object>();

                rosterEntryMap.put("name", entry.getUser());

                Integer statusImage;
                Integer statusPrio;
                if (mRoster.getPresence(entry.getUser()).getType() == Presence.Type.available) {
                    statusImage = android.R.drawable.presence_online;
                    statusPrio = 100;
                } else {
                    statusImage = android.R.drawable.presence_offline;
                    statusPrio = 0;
                }

                if ((entry.getType() == RosterPacket.ItemType.none || entry.getType() == RosterPacket.ItemType.from)
                        && (entry.getStatus() != RosterPacket.ItemStatus.subscribe && entry.getStatus() != RosterPacket.ItemStatus.SUBSCRIPTION_PENDING)) {

                    Presence p = new Presence(Presence.Type.subscribe);
                    p.setTo(entry.getUser());
                    try {
                        mConnection.sendStanza(p);
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }
                }

                rosterEntryMap.put("status", statusImage);
                rosterEntryMap.put("status_prio", statusPrio);

                mRosterList.add(rosterEntryMap);

                publishProgress();
            }

            Collections.sort(mRosterList, new RosterAlphabeticComparator());
            publishProgress();

            Collections.sort(mRosterList, new RosterPresenceComparator());
            publishProgress();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            mRosterAdapter.notifyDataSetChanged();
        }
    }

    private class UpdatePresenceTask extends AsyncTask<Presence, Void, Void> {

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
            String nameWithoutResource = params[0].getFrom().replaceAll("/.*$", "");

            Routines.debug("UpdatePresenceTask.doInBackground() started, name = " + nameWithoutResource);

            int i = findEntry(nameWithoutResource);
            Routines.debug("UpdatePresenceTask.doInBackground(): i = " + Integer.toString(i));

            if (i < 0) return null;

            Presence p = mRoster.getPresence(params[0].getFrom());

            Integer statusImage;
            Integer statusPrio;
            if (p.getType() == Presence.Type.available) {
                statusImage = android.R.drawable.presence_online;
                statusPrio = 100;
            } else {
                statusImage = android.R.drawable.presence_offline;
                statusPrio = 0;
            }

            Map<String, Object> entry = mRosterList.get(i);
            entry.put("status", statusImage);
            entry.put("status_prio", statusPrio);

            publishProgress();

            Collections.sort(mRosterList, new RosterAlphabeticComparator());
            publishProgress();

            Collections.sort(mRosterList, new RosterPresenceComparator());
            publishProgress();

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
