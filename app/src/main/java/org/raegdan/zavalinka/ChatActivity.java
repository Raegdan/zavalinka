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

import android.annotation.SuppressLint;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends Activity implements View.OnClickListener {

    private final Object mChatTaskLock = new Object();
    private ListView lvChatWindow;
    private BroadcastReceiver mMessageEventReceiver;
    private SimpleAdapter mChatAdapter;
    private List<Map<String, Object>> mChatList;
    private String mRemoteJid;
    private long mLastProcessedTimestamp;
    private Boolean mChatTaskLocked = false;
    private ImageButton btnSendChatMessage;
    private EditText etChatMessage;

    private void initGui() {
        lvChatWindow = (ListView) findViewById(R.id.lvChatWindow);
        mChatList = new ArrayList<Map<String, Object>>();
        String[] from = {"message", "time", "direction"};
        int[] to = {R.id.tvChatItemMessageText, R.id.tvChatItemMisc, R.id.tvChatItemDirection};
        mChatAdapter = new SimpleAdapter(this, mChatList, R.layout.chat_item, from, to);
        lvChatWindow.setAdapter(mChatAdapter);

        btnSendChatMessage = (ImageButton) findViewById(R.id.btnSendChatMessage);
        etChatMessage = (EditText) findViewById(R.id.etChatMessage);

        btnSendChatMessage.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent callingIntent = this.getIntent();

        mRemoteJid = callingIntent.getExtras().getString("remote_jid");
        if (mRemoteJid == null) {
            Routines.toast(getApplicationContext(), "Error! mRemoteJid is somewhat null!");
            finish();
        }

        setTitle(mRemoteJid);
        initGui();

        new UpdateChatThreadTask().execute();
    }

    @Override
    protected void onStart() {
        super.onStart();

        sendUnreadMessageBroadcast(mRemoteJid);

        mMessageEventReceiver = new MessageEventReceiver();
        IntentFilter filter = new IntentFilter("org.raegdan.zavalinka.UNREAD_MESSAGE_EVENT");
        registerReceiver(mMessageEventReceiver, filter);

        new UpdateChatThreadTask().execute();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mMessageEventReceiver);

        sendUnreadMessageBroadcast(mRemoteJid);
    }

    private void sendUnreadMessageBroadcast(String remoteJid) {
        Intent broadcast = new Intent("org.raegdan.zavalinka.UNREAD_MESSAGE_EVENT");
        broadcast.putExtra("remote_jid", remoteJid);
        broadcast.putExtra("has_unread", 0);
        sendBroadcast(broadcast);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSendChatMessage:
                sendMessage();
                break;
        }
    }

    private void sendMessage() {
        String message = etChatMessage.getText().toString();
        if (!Routines.stringsContainData(message)) return;

        XMPPConnection connection = ZXmppConnectionKeeper.getInstance().getConnection();
        if (!connection.isConnected() || !connection.isAuthenticated()) {
            Routines.toast(this, getString(R.string.error_no_connection));
            return;
        }

        new SendMessageTask().execute(message);
    }

    private void afterSendMessage(Boolean sentSuccessfully) {
        if (!sentSuccessfully) {
            Routines.toast(this, getString(R.string.error_failed_to_send));
        } else {
            etChatMessage.setText("");
        }
    }

    private class SendMessageTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            XMPPConnection conn = ZXmppConnectionKeeper.getInstance().getConnection();

            String to = mRemoteJid;
            String from = Routines.stripResourceFromJid(conn.getUser());
            String body = params[0];

            Message msg = new Message();
            msg.setTo(to);
            msg.setFrom(from);
            msg.setBody(body);

            try {
                conn.sendStanza(msg);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean sentSuccessfully) {
            super.onPostExecute(sentSuccessfully);

            afterSendMessage(sentSuccessfully);
        }
    }

    private abstract class ChatTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

        protected List<Map<String, Object>> getWorkingChatList(List<Map<String, Object>> mainChatList) {
            List<Map<String, Object>> workingChatList = new ArrayList<Map<String, Object>>();
            workingChatList.addAll(mainChatList);
            return workingChatList;
        }

        protected void mergeWorkingChatList(List<Map<String, Object>> workingChatList, List<Map<String, Object>> mainChatList) {
            mainChatList.clear();
            mainChatList.addAll(workingChatList);
        }

        protected void lock() {
            while (mChatTaskLocked) {
                try {
                    mChatTaskLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mChatTaskLocked = true;
        }

        protected void unlock() {
            mChatTaskLocked = false;
            mChatTaskLock.notifyAll();
        }

        protected void finishChatTask(List<Map<String, Object>> workingChatList) {
            if (workingChatList != null) {
                mergeWorkingChatList(workingChatList, mChatList);
                mChatAdapter.notifyDataSetChanged();
            }
            unlock();
        }

        protected Object getLockManager() {
            return mChatTaskLock;
        }
    }

    private class UpdateChatThreadTask extends ChatTask<Void, Void, List<Map<String, Object>>> {

        @Override
        protected List<Map<String, Object>> doInBackground(Void... params) {
            synchronized (getLockManager()) {
                lock();

                List<Map<String, Object>> workingChatList = getWorkingChatList(mChatList);
                ZXmppConnectionKeeper keeper = ZXmppConnectionKeeper.getInstance();
                ZMessagesStorage storage = keeper.getZMessagesStorageInstance();

                String account = Routines.stripResourceFromJid(keeper.getConnection().getUser());

                ZMessagesThread thread = storage.getAccountMessages(account).getThreadByRemoteJid(mRemoteJid);

                // Null thread means "ho history for contact yet", so skipping straight to the
                // method end, returning virgin workingChatList.
                if (thread != null) {
                    for (ZMessage msg : thread) {
                        if (msg.getTimestamp() <= mLastProcessedTimestamp) {
                            continue;
                        }

                        mLastProcessedTimestamp = msg.getTimestamp();
                        Map<String, Object> entry = new HashMap<String, Object>();

                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(msg.getTimestamp());
                        @SuppressLint
                                ("SimpleDateFormat") Format f = new SimpleDateFormat("H:mm:ss / dd.MM.yy");
                        entry.put("time", f.format(c.getTime()));

                        switch (msg.getDirection()) {
                            case ZMessage.DIRECTION_INCOMING:
                                entry.put("direction", getString(R.string.symbol_incoming));
                                break;

                            case ZMessage.DIRECTION_OUTGOING:
                                entry.put("direction", getString(R.string.symbol_outgoing));
                                break;
                        }

                        entry.put("message", msg.getMessageText());
                        workingChatList.add(entry);
                    }
                }

                return workingChatList;
            }
        }

        @Override
        protected void onPostExecute(List<Map<String, Object>> workingChatList) {
            synchronized (getLockManager()) {
                finishChatTask(workingChatList);
            }
        }
    }

    private class MessageEventReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras().getString("remote_jid", "").equalsIgnoreCase(mRemoteJid)) {
                new UpdateChatThreadTask().execute();
            }
        }
    }
}
