package cmupdaterapp.ui;

import java.net.URI;
import java.util.LinkedList;

import cmupdaterapp.customTypes.FullThemeList;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.database.ThemeListDbAdapter;
import cmupdaterapp.listadapters.ThemeListAdapter;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.ui.R;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

public class ThemeListActivity extends ListActivity
{
	private static final String TAG = "ThemeListActivity";
	
	public final static int RequestCode = 1; 
	
	private ThemeListDbAdapter themeListDb;
	private Cursor themeListCursor;
	private FullThemeList fullThemeList;
	private LinkedList<ThemeList> fullThemeListList;
	private Dialog dialog; 
	
	//New Dialog
	private Button btnSave;
	private Button btnBarcode;
	private EditText etName;
	private EditText etUri;
	private CheckBox cbEnabled;
	
	private ListView lv;
	
	private Resources res;
	
	private AdapterContextMenuInfo menuInfo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//Initialize the Database for Storing the ThemesLists
		themeListDb = new ThemeListDbAdapter(this);
		Log.d(TAG, "Opening Database");
		themeListDb.open();
		//Get the actual ThemeList from the Database
		getThemeList();
		setContentView(R.layout.themelist);
		lv = getListView();
		registerForContextMenu(lv);
		res = getResources();
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
	}
	
	private void getThemeList()
	{
		themeListCursor = themeListDb.getAllThemesCursor();
		startManagingCursor(themeListCursor);
		updateThemeList();
	}
	
	private void updateThemeList()
	{
		themeListCursor.requery();
		fullThemeList = new FullThemeList();
		if (themeListCursor.moveToFirst())
			do
			{
				String name = themeListCursor.getString(ThemeListDbAdapter.KEY_NAME_COLUMN);
				String uri = themeListCursor.getString(ThemeListDbAdapter.KEY_URI_COLUMN);
				int pk = themeListCursor.getInt(ThemeListDbAdapter.KEY_ID_COLUMN);
				ThemeList newItem = new ThemeList();
				newItem.name = name;
				newItem.url = URI.create(uri);
				newItem.PrimaryKey = pk;
				fullThemeList.addThemeToList(newItem);
			}
			while(themeListCursor.moveToNext());
		fullThemeListList = fullThemeList.returnFullThemeList();
		ThemeListAdapter<ThemeList> AdapterThemeList = new ThemeListAdapter<ThemeList>(
				this,
				android.R.layout.simple_list_item_1,
				fullThemeListList);
		setListAdapter(AdapterThemeList);
	}
	
	public void onListItemClick(ListView parent, View v,int position, long id)
	{
		super.onListItemClick(parent, v, position, id);
		Log.d(TAG, "Item clicked. Postition: " + id);
		//getListView().getItemAtPosition(position);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, Constants.MENU_THEME_LIST_ADD, Menu.NONE, R.string.menu_add_theme);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		switch(item.getItemId())
		{
			case Constants.MENU_THEME_LIST_ADD:
				createNewThemeList(false, null, null, true, 0);
				return true;
			case Constants.MENU_THEME_LIST_CONTEXT_EDIT:
				Log.d(TAG, "Edit clicked");
				menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
				ThemeList tl = ((ThemeList)lv.getAdapter().getItem(menuInfo.position));
				createNewThemeList(true, tl.name, tl.url.toString(), tl.enabled, tl.PrimaryKey);
				break;
			case Constants.MENU_THEME_LIST_CONTEXT_DELETE:
				Log.d(TAG, "Delete clicked");
				menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
				DeleteTheme(((ThemeList)lv.getAdapter().getItem(menuInfo.position)).PrimaryKey);
				break;
			default:
				Log.d(TAG, "Unknown Menu ID:" + item.getItemId());
				break;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	@Override
	public void onDestroy()
	{
		// Close the database
		Log.d(TAG, "Closing Database");
		themeListDb.close();
		super.onDestroy();
	}
	
	private void createNewThemeList(final boolean _update, String _name, String _uri, boolean _enabled, final int _primaryKey)
	{
		dialog = new Dialog(this);
		dialog.setContentView(R.layout.themelist_new);
		btnSave = (Button) dialog.findViewById(R.id.new_theme_list_button_save);
		btnBarcode = (Button) dialog.findViewById(R.id.new_theme_list_button_barcode);
		etName = (EditText) dialog.findViewById(R.id.new_theme_list_name);
		etUri = (EditText) dialog.findViewById(R.id.new_theme_list_uri);
		cbEnabled = (CheckBox) dialog.findViewById(R.id.new_theme_list_enabled);
		if(_name != null)
			etName.setText(_name);
		if(_uri != null)
			etUri.setText(_uri);
		cbEnabled.setChecked(_enabled);
		btnSave.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				ThemeList t = new ThemeList();
				t.name = etName.getText().toString().trim();
				t.url = URI.create(etUri.getText().toString().trim());
				t.enabled = cbEnabled.isChecked();
				if(_update == true)
					t.PrimaryKey = _primaryKey;
				//TODO: Check the URI
				//TODO: Check that there is text in every field
				//TODO: Make as startactivityforresult, so theres no dialog window
				if(_update == false)
					themeListDb.insertTheme(t);
				else
					themeListDb.updateTheme(_primaryKey, t);
				dialog.dismiss();
				updateThemeList();
			}
		});
		btnBarcode.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				dialog.dismiss();
			}
		});
		dialog.show();
	}
	
	@Override
	public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		menu.setHeaderTitle(res.getString(R.string.p_theme_list_context_menu_header));
		menu.add(Menu.NONE, Constants.MENU_THEME_LIST_CONTEXT_EDIT, Menu.NONE, res.getString(R.string.menu_edit_theme));
		menu.add(Menu.NONE, Constants.MENU_THEME_LIST_CONTEXT_DELETE, Menu.NONE, R.string.menu_delete_theme);
	}
	
	private void DeleteTheme(int position)
	{
		Log.d(TAG, "Remove Theme Postition: " + position);
		if (themeListDb.removeTheme(position))
			Log.d(TAG, "Success");
		else
			Log.d(TAG, "Fail");
		//TODO: Display a toast on fail
		updateThemeList();
	}
}