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
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AuthActivity extends Activity implements View.OnClickListener {

    EditText etLoginField, etPasswdField;
    Button btnLogin;
    XMPPConnectionKeeper mKeeper;

    /**
     * Binds activity Views to class fields and methods.
     */
    private void initGui() {
        etLoginField = (EditText) findViewById(R.id.etLoginField);
        etPasswdField = (EditText) findViewById(R.id.etPasswdField);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mKeeper = XMPPConnectionKeeper.getInstance();

        initGui();

        if (loadSavedCredentials()) {
            login(etLoginField.getText().toString(), etPasswdField.getText().toString());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_auth, menu);
        return true;
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

    private void login(String login, String passwd) {
        mKeeper.setLogin(login, this);
        mKeeper.setPasswd(passwd, this);

        new LoginTask(this, mKeeper).execute();
    }

    protected void postLogin(Boolean success) {
        if (!success) {
            Routines.toast(this, getString(R.string.login_failed));
            return;
        }

        Routines.toast(this, getString(R.string.login_succeeded));
        this.setResult(RESULT_OK);
        this.finish();
    }

    public void onBtnLoginClick() {
        String login = etLoginField.getText().toString();
        String passwd = etPasswdField.getText().toString();

        if (!Routines.stringsContainData(login, passwd)) {
            Routines.toast(this, getString(R.string.please_fill_credentials));
            return;
        }

        login(login, passwd);
    }

    private boolean loadSavedCredentials() {
        String login = mKeeper.getSavedLogin(this);
        String passwd = mKeeper.getSavedPasswd(this);

        if (Routines.stringsContainData(login, passwd)) {
            etLoginField.setText(login);
            etPasswdField.setText(passwd);

            return true;
        } else {
            return false;
        }

    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLogin:
                onBtnLoginClick();
                break;
        }
    }

    private class LoginTask extends AsyncTask<Void, Void, Boolean> {

        private XMPPConnectionKeeper mKeeper = null;
        private Activity mCallingActivity = null;
        private ProgressDialog d;

        public LoginTask(Activity callingActivity, XMPPConnectionKeeper keeper) {
            this.mCallingActivity = callingActivity;
            this.mKeeper = keeper;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            d = new ProgressDialog(mCallingActivity);
            d.setCancelable(false);
            d.setMessage(getString(R.string.logging_in_progress_msg));
            d.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return mKeeper.login();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            d.dismiss();
            postLogin(aBoolean);
        }
    }
}
