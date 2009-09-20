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
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class ThemeListActivity extends ListActivity
{
	private static final String TAG = "ThemeListActivity";
	
	private ThemeListDbAdapter themeListDb;
	private Cursor themeListCursor;
	private FullThemeList fullThemeList;
	private LinkedList<ThemeList> fullThemeListList;

	private ListView lv;
	
	private Resources res;
	
	private AdapterContextMenuInfo menuInfo;
	private TextView tv;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		themeListDb = new ThemeListDbAdapter(this);
		Log.d(TAG, "Opening Database");
		themeListDb.open();
		setContentView(R.layout.themelist);
		tv = (TextView) findViewById(R.id.theme_list_info);
		getThemeList();
		lv = getListView();
		registerForContextMenu(lv);
		res = getResources();
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
				int enabled = themeListCursor.getInt(ThemeListDbAdapter.KEY_ENABLED_COLUMN);
				ThemeList newItem = new ThemeList();
				newItem.name = name;
				newItem.url = URI.create(uri);
				if(enabled == 1) newItem.enabled = true;
				else newItem.enabled = false;
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
		if (fullThemeList.getThemeCount() > 0)
			tv.setText(R.string.theme_list_long_press);
		else
			tv.setText(R.string.theme_list_no_themes);
	}
	
	public void onListItemClick(ListView parent, View v,int position, long id)
	{
		super.onListItemClick(parent, v, position, id);
		Log.d(TAG, "Item clicked. Postition: " + id);
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
		menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		ThemeList tl;
		
		switch(item.getItemId())
		{
			case Constants.MENU_THEME_LIST_ADD:
				createNewThemeList(false, null, null, true, 0);
				return true;
			case Constants.MENU_THEME_LIST_CONTEXT_EDIT:
				Log.d(TAG, "Edit clicked");
				tl = ((ThemeList)lv.getAdapter().getItem(menuInfo.position));
				createNewThemeList(true, tl.name, tl.url.toString(), tl.enabled, tl.PrimaryKey);
				break;
			case Constants.MENU_THEME_LIST_CONTEXT_DELETE:
				Log.d(TAG, "Delete clicked");
				tl = ((ThemeList)lv.getAdapter().getItem(menuInfo.position));
				DeleteTheme(tl.PrimaryKey);
				break;
			case Constants.MENU_THEME_LIST_CONTEXT_DISABLE:
				Log.d(TAG, "Selected to disable Theme Server");
				tl = ((ThemeList)lv.getAdapter().getItem(menuInfo.position));
				tl.enabled = false;
				themeListDb.updateTheme(tl.PrimaryKey, tl);
				updateThemeList();
				break;
			case Constants.MENU_THEME_LIST_CONTEXT_ENABLE:
				Log.d(TAG, "Selected to enable Theme Server");
				tl = ((ThemeList)lv.getAdapter().getItem(menuInfo.position));
				tl.enabled = true;
				themeListDb.updateTheme(tl.PrimaryKey, tl);
				updateThemeList();
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
		//TODO: When no item in List: Text to hit menu to add servers
		//TODO: Otherwise display a text at the top: Long press an item to show options
		Intent i = new Intent(ThemeListActivity.this, ThemeListNewActivity.class);
		i.putExtra(Constants.THEME_LIST_NEW_NAME, _name);
		i.putExtra(Constants.THEME_LIST_NEW_URI, _uri);
		i.putExtra(Constants.THEME_LIST_NEW_ENABLED, _enabled);
		i.putExtra(Constants.THEME_LIST_NEW_PRIMARYKEY, _primaryKey);
		i.putExtra(Constants.THEME_LIST_NEW_UPDATE, _update);
		startActivityForResult(i, ThemeListNewActivity.REQUEST_CODE);
	}
	
	@Override
	public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		menu.setHeaderTitle(res.getString(R.string.p_theme_list_context_menu_header));
		menu.add(Menu.NONE, Constants.MENU_THEME_LIST_CONTEXT_EDIT, Menu.NONE, R.string.menu_edit_theme);
		menu.add(Menu.NONE, Constants.MENU_THEME_LIST_CONTEXT_DELETE, Menu.NONE, R.string.menu_delete_theme);
		ThemeList tl = ((ThemeList)lv.getAdapter().getItem(((AdapterContextMenuInfo)menuInfo).position));
		if (tl.enabled)
			menu.add(Menu.NONE, Constants.MENU_THEME_LIST_CONTEXT_DISABLE, Menu.NONE, R.string.menu_disable_theme);
		else
			menu.add(Menu.NONE, Constants.MENU_THEME_LIST_CONTEXT_ENABLE, Menu.NONE, R.string.menu_enable_theme);
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
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		Log.d(TAG, "RequestCode: " + requestCode + " ResultCode: " + resultCode);
		switch(requestCode)
		{
			case ThemeListNewActivity.REQUEST_CODE:
				if(resultCode == RESULT_OK)
				{
					Bundle b = intent.getExtras();
					ThemeList tl = new ThemeList();
					tl.name = b.getString(Constants.THEME_LIST_NEW_NAME);
					tl.url = URI.create(b.getString(Constants.THEME_LIST_NEW_URI));
					tl.enabled = b.getBoolean(Constants.THEME_LIST_NEW_ENABLED);
					if(b.getBoolean(Constants.THEME_LIST_NEW_UPDATE))
						tl.PrimaryKey = b.getInt(Constants.THEME_LIST_NEW_PRIMARYKEY);
					if(!b.getBoolean(Constants.THEME_LIST_NEW_UPDATE))
						themeListDb.insertTheme(tl);
					else
						themeListDb.updateTheme(b.getInt(Constants.THEME_LIST_NEW_PRIMARYKEY), tl);
					updateThemeList();
				}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}
}