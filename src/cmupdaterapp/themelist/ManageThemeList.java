package cmupdaterapp.themelist;

import java.util.LinkedList;

import cmupdaterapp.customTypes.FullThemeList;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.database.ThemeListDbAdapter;
import cmupdaterapp.ui.Constants;
import cmupdaterapp.ui.Log;
import cmupdaterapp.ui.R;
import android.app.Dialog;
import android.app.ListActivity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class ManageThemeList extends ListActivity
{
	private static final String TAG = "ManageThemeActivity";
	
	public final static int RequestCode = 1; 
	
	private ThemeListDbAdapter themeListDb;
	private Cursor themeListCursor;
	private FullThemeList fullThemeList;
	private LinkedList<ThemeList> fullThemeListList;
	private Dialog dialog; 
	
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
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		ThemeListAdapter<ThemeList> spAdapterRoms = new ThemeListAdapter<ThemeList>(
				this,
				android.R.layout.simple_list_item_1,
				fullThemeListList);
		setListAdapter(spAdapterRoms);
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
		udpateThemeList();
	}
	
	private void udpateThemeList()
	{
		themeListCursor.requery();
		fullThemeList = new FullThemeList();
		if (themeListCursor.moveToFirst())
			do
			{
				String name = themeListCursor.getString(ThemeListDbAdapter.KEY_NAME_COLUMN);
				String uri = themeListCursor.getString(ThemeListDbAdapter.KEY_URI_COLUMN);
				ThemeList newItem = new ThemeList();
				newItem.name = name;
				newItem.url = Uri.parse(uri);
				fullThemeList.addThemeToList(newItem);
			}
			while(themeListCursor.moveToNext());
		fullThemeListList = fullThemeList.returnFullThemeList();
	}
	
	public void onListItemClick(ListView parent, View v,int position, long id)
	{
		//selection.setText(items[position]);
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
				createNewThemeList();
				return true;
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
	
	private void createNewThemeList()
	{
		dialog = new Dialog(this);
		dialog.setContentView(R.layout.newthemeserver);
		Button save = (Button) dialog.findViewById(R.id.new_theme_list_button_save);
		save.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				dialog.dismiss();
			}
		});
		dialog.show();
	}
}