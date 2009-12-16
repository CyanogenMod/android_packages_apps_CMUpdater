package cmupdaterapp.ui;

import java.net.URI;
import java.util.LinkedList;

import cmupdaterapp.customTypes.FullThemeList;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.database.DbAdapter;
import cmupdaterapp.featuredThemes.FeaturedThemes;
import cmupdaterapp.listadapters.ThemeListAdapter;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.ui.R;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ThemeListActivity extends ListActivity
{
	private static final String TAG = "ThemeListActivity";
	
	private DbAdapter themeListDb;
	private Cursor themeListCursor;
	private FullThemeList fullThemeList;
	private LinkedList<ThemeList> fullThemeListList;

	private ListView lv;
	
	private Resources res;
	
	private AdapterContextMenuInfo menuInfo;
	private TextView tv;
	
	private FullThemeList FeaturedThemes = null;
	private Thread FeaturedThemesThread;
	private ProgressDialog FeaturedThemesProgressDialog;
	public static Handler FeaturedThemesProgressHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		themeListDb = new DbAdapter();
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
		updateThemeList();
	}
	
	private void updateThemeList()
	{
		themeListCursor.requery();
		fullThemeList = new FullThemeList();
		if (themeListCursor.moveToFirst())
			do
			{
				String name = themeListCursor.getString(DbAdapter.KEY_THEMELIST_NAME_COLUMN);
				String uri = themeListCursor.getString(DbAdapter.KEY_THEMELIST_URI_COLUMN);
				int pk = themeListCursor.getInt(DbAdapter.KEY_THEMELIST_ID_COLUMN);
				int enabled = themeListCursor.getInt(DbAdapter.KEY_THEMELIST_ENABLED_COLUMN);
				int featured = themeListCursor.getInt(DbAdapter.KEY_THEMELIST_FEATURED_COLUMN);
				ThemeList newItem = new ThemeList();
				newItem.name = name;
				newItem.url = URI.create(uri);
				if(enabled == 1) newItem.enabled = true;
				else newItem.enabled = false;
				if(featured == 1) newItem.featured = true;
				else newItem.featured = false;
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
		themeListCursor.deactivate();
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
		menu.add(Menu.NONE, Constants.MENU_THEME_LIST_UPDATE_FEATURED, Menu.NONE, R.string.menu_update_featured);
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
				createNewThemeList(false, "", "", true, 0, false);
				return true;
			case Constants.MENU_THEME_LIST_UPDATE_FEATURED:
				new AlertDialog.Builder(ThemeListActivity.this)
				.setTitle(R.string.featured_themes_dialog_title)
				.setMessage(R.string.featured_themes_dialog_summary)
				.setPositiveButton(R.string.featured_themes_dialog_pos, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
						updateFeaturedThemes();
					}
				})
				.setNegativeButton(R.string.featured_themes_dialog_neg, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				})
				.show();
				return true;
			case Constants.MENU_THEME_LIST_CONTEXT_EDIT:
				Log.d(TAG, "Edit clicked");
				tl = ((ThemeList)lv.getAdapter().getItem(menuInfo.position));
				createNewThemeList(true, tl.name, tl.url.toString(), tl.enabled, tl.PrimaryKey, tl.featured);
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
		themeListCursor.close();
		themeListDb.close();
		super.onDestroy();
	}
	
	private void createNewThemeList(final boolean _update, String _name, String _uri, boolean _enabled, final int _primaryKey, boolean _featured)
	{
		Intent i = new Intent(ThemeListActivity.this, ThemeListNewActivity.class);
		i.putExtra(Constants.THEME_LIST_NEW_NAME, _name);
		i.putExtra(Constants.THEME_LIST_NEW_URI, _uri);
		i.putExtra(Constants.THEME_LIST_NEW_ENABLED, _enabled);
		i.putExtra(Constants.THEME_LIST_NEW_PRIMARYKEY, _primaryKey);
		i.putExtra(Constants.THEME_LIST_NEW_UPDATE, _update);
		i.putExtra(Constants.THEME_LIST_NEW_FEATURED, _featured);
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
		{
			Log.d(TAG, "Fail");
			Toast.makeText(this, R.string.theme_list_delete_error, Toast.LENGTH_LONG).show();
		}
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
					tl.featured = b.getBoolean(Constants.THEME_LIST_NEW_FEATURED);
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
	
	private void updateFeaturedThemes()
	{
		Log.d(TAG, "Called Update Featured Themes");
		FeaturedThemesProgressHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				Log.d(TAG, "recieved Message");
				if (FeaturedThemesProgressDialog != null)
					FeaturedThemesProgressDialog.dismiss();
				if (msg.obj instanceof String)
				{
					Toast.makeText(ThemeListActivity.this, (CharSequence) msg.obj, Toast.LENGTH_LONG).show();
					FeaturedThemes = null;
					ThemeListActivity.this.FeaturedThemesThread.interrupt();
					FeaturedThemesProgressDialog.dismiss();
				}
				else if (msg.obj instanceof FullThemeList)
				{
					FeaturedThemes = (FullThemeList) msg.obj;
					ThemeListActivity.this.FeaturedThemesThread.interrupt();
					FeaturedThemesProgressDialog.dismiss();
					if (FeaturedThemes != null && FeaturedThemes.getThemeCount() > 0)
					{
						themeListDb.UpdateFeaturedThemes(FeaturedThemes);
						updateThemeList();
						Toast.makeText(ThemeListActivity.this, R.string.featured_themes_finished_toast, Toast.LENGTH_LONG).show();
					}
				}
	        }
	    };

	    FeaturedThemesProgressDialog = ProgressDialog.show(this, res.getString(R.string.featured_themes_progress_title), res.getString(R.string.featured_themes_progress_body), true);
	    FeaturedThemesThread = new Thread(new FeaturedThemes(this));
	    FeaturedThemesThread.start();
	}
}