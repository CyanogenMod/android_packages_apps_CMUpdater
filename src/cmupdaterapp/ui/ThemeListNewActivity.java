package cmupdaterapp.ui;

import cmupdaterapp.misc.Constants;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class ThemeListNewActivity extends Activity
{
	public final static int REQUEST_CODE = 1;
	
	private String intentName;
	private String intentUri;
	private boolean intentEnabled;
	private boolean intentUpdate;
	private int intentPrimaryKey;
	
	private Button btnSave;
	private Button btnBarcode;
	private EditText etName;
	private EditText etUri;
	private CheckBox cbEnabled;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.themelist_new);
		Intent i = getIntent();
		Bundle b = i.getExtras();
		intentName = b.getString(Constants.THEME_LIST_NEW_NAME);
		intentUri = b.getString(Constants.THEME_LIST_NEW_URI);
		intentEnabled = b.getBoolean(Constants.THEME_LIST_NEW_ENABLED);
		intentUpdate = b.getBoolean(Constants.THEME_LIST_NEW_UPDATE);
		intentPrimaryKey = b.getInt(Constants.THEME_LIST_NEW_PRIMARYKEY);
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		btnSave = (Button) findViewById(R.id.new_theme_list_button_save);
		btnBarcode = (Button) findViewById(R.id.new_theme_list_button_barcode);
		etName = (EditText) findViewById(R.id.new_theme_list_name);
		etUri = (EditText) findViewById(R.id.new_theme_list_uri);
		cbEnabled = (CheckBox) findViewById(R.id.new_theme_list_enabled);
		if(intentName != null)
			etName.setText(intentName);
		if(intentUri != null)
			etUri.setText(intentUri);
		cbEnabled.setChecked(intentEnabled);
		btnSave.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				//TODO: Check the URI
				//TODO: Check that there is text in every field
				Intent i = new Intent();
				i.putExtra(Constants.THEME_LIST_NEW_NAME, etName.getText().toString().trim());
				i.putExtra(Constants.THEME_LIST_NEW_URI, etUri.getText().toString().trim());
				i.putExtra(Constants.THEME_LIST_NEW_ENABLED, cbEnabled.isChecked());
				i.putExtra(Constants.THEME_LIST_NEW_PRIMARYKEY, intentPrimaryKey);
				i.putExtra(Constants.THEME_LIST_NEW_UPDATE, intentUpdate);
				setResult(RESULT_OK, i);
				finish();
			}
		});
		btnBarcode.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}
}
