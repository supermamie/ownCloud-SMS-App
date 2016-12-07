package fr.unix_experience.owncloud_sms.engine;

import android.accounts.Account;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/*
 *  Copyright (c) 2014-2016, Loic Blot <loic.blot@unix-experience.fr>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public interface ASyncSMSRecovery {
	class SMSRecoveryTask extends AsyncTask<Void, Void, Void> {
		private final Context _context;
		private final Account _account;

		public SMSRecoveryTask(Context context, Account account) {
			_context = context;
			_account = account;
		}

		@Override
		protected Void doInBackground(Void... params) {
			// This feature is only available for Android 4.4 and greater
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
				return null;
			}
			Log.i(ASyncSMSRecovery.TAG, "Starting background recovery");

			Long start = (long) 0;
			JSONObject obj = new OCSMSOwnCloudClient(_context, _account).retrieveSomeMessages(start, 500);
			try {
				while (obj.getLong("last_id") != start) {
					start = obj.getLong("last_id");
					obj = new OCSMSOwnCloudClient(_context, _account).retrieveSomeMessages(start, 500);
				}
			} catch (JSONException e) {
				Log.e(ASyncSMSRecovery.TAG, "Missing last_id field!");
			}
			Log.i(ASyncSMSRecovery.TAG, "Finishing background recovery");
			return null;
		}
	}

	String TAG = ASyncSMSRecovery.class.getSimpleName();
}