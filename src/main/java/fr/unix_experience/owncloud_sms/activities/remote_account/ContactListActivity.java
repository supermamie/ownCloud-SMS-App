package fr.unix_experience.owncloud_sms.activities.remote_account;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Vector;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import fr.nrz.androidlib.adapters.AndroidAccountAdapter;
import fr.unix_experience.owncloud_sms.R;
import fr.unix_experience.owncloud_sms.adapters.ContactListAdapter;
import fr.unix_experience.owncloud_sms.engine.ASyncContactLoad;
import fr.unix_experience.owncloud_sms.engine.OCSMSOwnCloudClient;

public class ContactListActivity extends Activity implements ASyncContactLoad {

	static AccountManager _accountMgr;
	ContactListAdapter adapter;
	SwipeRefreshLayout _layout;
	ArrayList<String> objects;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		assert getIntent().getExtras() != null;

		final String accountName = getIntent().getExtras().getString("account");

		// accountName cannot be null, devel error
		assert accountName != null;

		_accountMgr = AccountManager.get(getBaseContext());
		final Account[] myAccountList =
				_accountMgr.getAccountsByType(getString(R.string.account_type));
		
		// Init view
		objects = new ArrayList<String>();
		setContentView(R.layout.restore_activity_contactlist);

		_layout = (SwipeRefreshLayout) findViewById(R.id.contactlist_swipe_container);

		_layout.setColorScheme(android.R.color.holo_blue_bright,
				android.R.color.holo_green_light,
				android.R.color.holo_orange_light,
				android.R.color.holo_red_light);
		
		adapter = new ContactListAdapter(getBaseContext(),
				android.R.layout.simple_spinner_item,
				objects,
				R.layout.contact_list_item,
				R.id.contactname, this);
		
		final Spinner sp = (Spinner) findViewById(R.id.contact_spinner);
		final LinearLayout contactInfos = (LinearLayout) findViewById(R.id.contactinfos_layout);
		final ProgressBar contactProgressBar = (ProgressBar) findViewById(R.id.contactlist_pgbar);
		final TextView contactPhoneList = (TextView) findViewById(R.id.contact_phonelist);

		sp.setVisibility(View.INVISIBLE);
		contactInfos.setVisibility(View.INVISIBLE);

		sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				contactInfos.setVisibility(View.INVISIBLE);

				String contactName = sp.getSelectedItem().toString();
				Vector<String> phoneList = fetchContact(contactName);
				Integer smsCount = 0;
				// @TODO asynctask to load more datas

				if (phoneList.size() > 0) {
					String res = new String("");
					for (String pn: phoneList) {
						res += "- " + pn + "\n";
					}
					contactPhoneList.setText(res);
				} else {
					contactPhoneList.setText(contactName);
				}

				contactInfos.setVisibility(View.VISIBLE);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Nothing to do there
			}

			private Vector<String> fetchContact(String name) {
				Cursor people = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
						null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " = ?",
						new String[]{name}, null);
				people.moveToFirst();

				Vector<String> r = new Vector<>();
				if (people.getCount() == 0) {
					return r;
				}

				String contactId = people.getString(people.getColumnIndex(ContactsContract.Contacts._ID));

				if (people.getString(people.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
						.equalsIgnoreCase("1")) {
					Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							null,
							ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
							new String[]{contactId}, null);
					while (phones.moveToNext()) {
						String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
								.replaceAll(" ", "");
						r.add(phoneNumber);
					}
					phones.close();
				}
				return r;
			}
		});
		sp.setAdapter(adapter);

		for (final Account element : myAccountList) {
			if (element.name.equals(accountName)) {
				// Load "contacts"
				contactProgressBar.setVisibility(View.VISIBLE);
				sp.setVisibility(View.INVISIBLE);
				new ContactLoadTask(element, getBaseContext(), adapter, objects, _layout, contactProgressBar, sp).execute();

				// Add refresh handler
				_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
					@Override
					public void onRefresh() {
						_layout.setRefreshing(true);
						sp.setVisibility(View.INVISIBLE);
						contactProgressBar.setVisibility(View.VISIBLE);
						(new Handler()).post(new Runnable() {
							@Override
							public void run() {
								objects.clear();
								adapter.notifyDataSetChanged();
								new ContactLoadTask(element, getBaseContext(), adapter, objects, _layout, contactProgressBar, sp).execute();
							}
						});
					}
				});
				return;
			}
		}
	}
}