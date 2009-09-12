package cmupdaterapp.ui;

import java.util.LinkedList;

import cmupdaterapp.customTypes.FullThemeList;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.database.ThemeListDbAdapter;
import android.app.ListActivity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class ManageThemeList extends ListActivity
{
	private static final String TAG = "ManageThemeActivity";
	
	public final static int RequestCode = 1; 
	
	private ThemeListDbAdapter themeListDb;
	private Cursor themeListCursor;
	private FullThemeList fullThemeList;
	private LinkedList<ThemeList> fullThemeListList;
	
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
		UpdateListAdapter<ThemeList> spAdapterRoms = new UpdateListAdapter<ThemeList>(
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
	public void onDestroy()
	{
		// Close the database
		Log.d(TAG, "Closing Database");
		themeListDb.close();
		super.onDestroy();
	}
}
