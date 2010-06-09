package cmupdaterapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.utils.Preferences;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ThemeListNewActivity extends Activity
{
	private static final String TAG = "ThemeListNewActivity";
	public final static int REQUEST_CODE = 1;

	private Boolean showDebugOutput = false;

    private boolean intentFeatured;
	private EditText etUri;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		showDebugOutput = new Preferences(this).displayDebugOutput();
		setContentView(R.layout.themelist_new);
		Intent i = getIntent();
		Bundle b = i.getExtras();
		final String intentName = b.getString(Constants.THEME_LIST_NEW_NAME).trim();
		final String intentUri = b.getString(Constants.THEME_LIST_NEW_URI).trim();
        boolean intentEnabled = b.getBoolean(Constants.THEME_LIST_NEW_ENABLED);
		intentFeatured = b.getBoolean(Constants.THEME_LIST_NEW_FEATURED);
		final boolean intentUpdate = b.getBoolean(Constants.THEME_LIST_NEW_UPDATE);
		final int intentPrimaryKey = b.getInt(Constants.THEME_LIST_NEW_PRIMARYKEY);

        Button btnSave = (Button) findViewById(R.id.new_theme_list_button_save);
        Button btnCancel = (Button) findViewById(R.id.new_theme_list_button_cancel);
        Button btnBarcode = (Button) findViewById(R.id.new_theme_list_button_barcode);
		final EditText etName = (EditText) findViewById(R.id.new_theme_list_name);
		etUri = (EditText) findViewById(R.id.new_theme_list_uri);
		final CheckBox cbEnabled = (CheckBox) findViewById(R.id.new_theme_list_enabled);
		if(intentName != null)
			etName.setText(intentName);
		if(intentUri != null)
			etUri.setText(intentUri);
		cbEnabled.setChecked(intentEnabled);
		btnSave.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if(etName.getText().toString().trim().equalsIgnoreCase(""))
				{
					if (showDebugOutput) Log.d(TAG, "Entered name was empty");
					Toast.makeText(ThemeListNewActivity.this, R.string.theme_list_new_wrong_name, Toast.LENGTH_LONG).show();
					return;
				}
				if(!URLUtil.isValidUrl(etUri.getText().toString().trim()))
				{
					if (showDebugOutput) Log.d(TAG, "Entered URL was not valid: " + etUri.getText().toString());
					Toast.makeText(ThemeListNewActivity.this, R.string.theme_list_new_wrong_uri, Toast.LENGTH_LONG).show();
					return;
				}
				Intent i = new Intent();
				//Check if the Theme has Changed.
				//If the Name and the Url has changed, the theme gets updated without the featured flag
				//If the user only changes the enabled state, the feature state will remain
				String tempName = etName.getText().toString().trim();
				String tempUri = etUri.getText().toString().trim();
				if (intentFeatured &&
						(!intentName.equalsIgnoreCase(tempName) || !intentUri.equalsIgnoreCase(tempUri)))
				{
					intentFeatured = false;
				}
				i.putExtra(Constants.THEME_LIST_NEW_NAME, tempName);
				i.putExtra(Constants.THEME_LIST_NEW_URI, tempUri);
				i.putExtra(Constants.THEME_LIST_NEW_ENABLED, cbEnabled.isChecked());
				i.putExtra(Constants.THEME_LIST_NEW_PRIMARYKEY, intentPrimaryKey);
				i.putExtra(Constants.THEME_LIST_NEW_UPDATE, intentUpdate);
				i.putExtra(Constants.THEME_LIST_NEW_FEATURED, intentFeatured);
				setResult(RESULT_OK, i);
				finish();
			}
		});
		btnCancel.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		btnBarcode.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				IntentIntegrator.initiateScan(ThemeListNewActivity.this);
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		if (showDebugOutput) Log.d(TAG, "onActivityResult requestCode: "+requestCode+" resultCode: "+resultCode);
		switch(requestCode)
		{
			case IntentIntegrator.REQUEST_CODE:
				IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
				if (null != scanResult)
				{
					String result = scanResult.getContents();
					if (null != result && !result.equals("") )
					{
						if (showDebugOutput) Log.d(TAG, "Scanned URL: " + result);
						if(URLUtil.isValidUrl(result))
						{
							etUri.setText(result);
							break;
						}
						else
						{
							Toast.makeText(getBaseContext(), R.string.p_invalid_url, Toast.LENGTH_LONG).show();
							if (showDebugOutput) Log.d(TAG, "Scanned URL not valid: " + result);
						}
					}
					else
						Toast.makeText(getBaseContext(), R.string.barcode_scan_no_result, Toast.LENGTH_LONG).show();
				}
				else
					Toast.makeText(getBaseContext(), R.string.barcode_scan_no_result, Toast.LENGTH_LONG).show();
				break;
			default:
				if (showDebugOutput) Log.d(TAG, "Wrong Request Code");
				break;
		}
	}
}