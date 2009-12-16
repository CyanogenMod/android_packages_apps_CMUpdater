package cmupdaterapp.database;

import java.io.File;
import java.net.URI;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import cmupdaterapp.customTypes.FullThemeList;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;

public class DbAdapter
{
	private static final String TAG = "DbAdapter";
	
	private static final String DATABASE_NAME = "cmupdater.db";
	private static final int DATABASE_VERSION = 3;
	//Themelist
	private static final String DATABASE_TABLE_THEMELIST = "ThemeList";
	public static final String KEY_THEMELIST_ID = "id";
	public static final int KEY_THEMELIST_ID_COLUMN = 0;
	public static final String KEY_THEMELIST_NAME = "name";
	public static final int KEY_THEMELIST_NAME_COLUMN = 1;
	public static final String KEY_THEMELIST_URI = "uri";
	public static final int KEY_THEMELIST_URI_COLUMN = 2;
	public static final String KEY_THEMELIST_ENABLED = "enabled";
	public static final int KEY_THEMELIST_ENABLED_COLUMN = 3;
	public static final String KEY_THEMELIST_FEATURED = "featured";
	public static final int KEY_THEMELIST_FEATURED_COLUMN = 4;
	//Screenshots
	private static final String DATABASE_TABLE_SCREENSHOTS = "Screenshot";
	private static final String THEMELIST_ID_FOREIGNKEYCONSTRAINT = "fk_themelist_id";
	private static final String TRIGGER_THEMELIST_ID_INSERT = "fki_themelist_id";
	private static final String TRIGGER_THEMELIST_ID_UPDATE = "fku_themelist_id";
	private static final String TRIGGER_THEMELIST_ID_DELETE = "fkd_themelist_id";
	public static final String KEY_SCREENSHOTS_ID = "id";
	public static final int KEY_SCREENSHOTS_ID_COLUMN = 0;
	public static final String KEY_SCREENSHOTS_THEMELIST_ID = "themelist_id";
	public static final int KEY_SCREENSHOTS_THEMELIST_ID_COLUMN = 1;
	public static final String KEY_SCREENSHOTS_URI = "uri";
	public static final int KEY_SCREENSHOTS_URI_COLUMN = 2;
	public static final String KEY_SCREENSHOTS_MODIFYDATE = "modifydate";
	public static final int KEY_SCREENSHOTS_MODIFYDATE_COLUMN = 3;
	public static final String KEY_SCREENSHOTS_SCREENSHOT = "screenshot";
	public static final int KEY_SCREENSHOTS_SCREENSHOT_COLUMN = 4;

	private SQLiteDatabase db;
	private DbOpenHelper dbHelper;
	
	public DbAdapter()
	{
		dbHelper = new DbOpenHelper();
	}
	
	public void close()
	{
		db.close();
	}
	
	public void open()
	{
		File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.EXTERNAL_DATA_DIRECTORY + "/");
		f.mkdirs();
		db = dbHelper.open(f.toString(), DATABASE_NAME, DATABASE_VERSION);
	}
	
	// Insert a new task
	public long insertTheme(ThemeList _theme)
	{
		ContentValues newValues = new ContentValues();
		newValues.put(KEY_THEMELIST_NAME, _theme.name);
		newValues.put(KEY_THEMELIST_URI, _theme.url.toString());
		if(_theme.enabled) newValues.put(KEY_THEMELIST_ENABLED, 1);
		else newValues.put(KEY_THEMELIST_ENABLED, 0);
		if(_theme.featured) newValues.put(KEY_THEMELIST_FEATURED, 1);
		else newValues.put(KEY_THEMELIST_FEATURED, 0);
		return db.insert(DATABASE_TABLE_THEMELIST, null, newValues);
	}
	
	// Remove a theme based on its index
	public boolean removeTheme(long _rowIndex)
	{
		return db.delete(DATABASE_TABLE_THEMELIST, KEY_THEMELIST_ID + "=" + _rowIndex, null) > 0;
	}
	
	// Update a Theme
	public boolean updateTheme(long _rowIndex, ThemeList _theme)
	{
		ContentValues newValue = new ContentValues();
		newValue.put(KEY_THEMELIST_NAME, _theme.name);
		newValue.put(KEY_THEMELIST_URI, _theme.url.toString());
		if(_theme.enabled) newValue.put(KEY_THEMELIST_ENABLED, 1);
		else newValue.put(KEY_THEMELIST_ENABLED, 0);
		if(_theme.featured) newValue.put(KEY_THEMELIST_FEATURED, 1);
		else newValue.put(KEY_THEMELIST_FEATURED, 0);
		return db.update(DATABASE_TABLE_THEMELIST, newValue, KEY_THEMELIST_ID + "=" + _rowIndex, null) > 0;
	}
	
	public Cursor getAllThemesCursor()
	{
		return db.query(DATABASE_TABLE_THEMELIST, new String[] { KEY_THEMELIST_ID, KEY_THEMELIST_NAME, KEY_THEMELIST_URI, KEY_THEMELIST_ENABLED, KEY_THEMELIST_FEATURED }, null, null, null, null, KEY_THEMELIST_NAME);
	}

	public ThemeList getThemeItem(long _rowIndex) throws SQLException
	{
		Cursor cursor = db.query(true, DATABASE_TABLE_THEMELIST, new String[] { KEY_THEMELIST_ID, KEY_THEMELIST_NAME, KEY_THEMELIST_URI, KEY_THEMELIST_ENABLED, KEY_THEMELIST_FEATURED }, KEY_THEMELIST_ID + "=" + _rowIndex, null, null, null, null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst())
		{
			cursor.close();
			throw new SQLException("No Theme item found for row: " + _rowIndex);
		}
		String name = cursor.getString(KEY_THEMELIST_NAME_COLUMN);
		String uri = cursor.getString(KEY_THEMELIST_URI_COLUMN);
		int enabled = cursor.getInt(KEY_THEMELIST_ENABLED_COLUMN);
		int featured = cursor.getInt(KEY_THEMELIST_FEATURED_COLUMN);
		int Key = cursor.getInt(KEY_THEMELIST_ID_COLUMN);
		ThemeList result = new ThemeList();
		result.name = name;
		result.url = URI.create(uri);
		result.PrimaryKey = Key;
		if(enabled == 1) result.enabled = true;
		else result.enabled = false;
		if(featured == 1) result.featured = true;
		else result.featured = false;
		cursor.close();
		return result;
	}
	
	public void UpdateFeaturedThemes(FullThemeList t)
	{
		FullThemeList retValue = new FullThemeList();
		//Get the enabled state of the current Featured Themes
		for (ThemeList tl : t.returnFullThemeList())
		{
			Cursor result = db.query(true, DATABASE_TABLE_THEMELIST, new String[] { KEY_THEMELIST_ID, KEY_THEMELIST_NAME, KEY_THEMELIST_URI, KEY_THEMELIST_ENABLED, KEY_THEMELIST_FEATURED }, KEY_THEMELIST_NAME + "='" + tl.name + "' and " + KEY_THEMELIST_FEATURED + "=1" , null, null, null, null, null);
			if ((result.getCount() == 0) || !result.moveToFirst())
			{
				Log.d(TAG, "Theme " + tl.name + " not found in your List");
				retValue.addThemeToList(tl);
				continue;
			}
			tl.enabled = result.getInt(KEY_THEMELIST_ENABLED_COLUMN) != 0;
			retValue.addThemeToList(tl);
			result.close();
		}
		//Delete all featured Themes
		db.delete(DATABASE_TABLE_THEMELIST, KEY_THEMELIST_FEATURED + "=1", null);
		Log.d(TAG, "Deleted all old Featured Theme Servers");
		//Add all Featured Themes again
		for (ThemeList tl2 : retValue.returnFullThemeList())
		{
			insertTheme(tl2);
		}
		Log.d(TAG, "Updated Featured Theme Servers");
	}

	//Helper Class for opening/creating a Database
	private static class DbOpenHelper
	{
		public SQLiteDatabase open(String path, String name, int version)
		{
			String databasePath = path + name;
			SQLiteDatabase s = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
			if (s.needUpgrade(version))
				update(s, s.getVersion(), version);
			s.setVersion(version);
			return s;
		}
		
		private void update(SQLiteDatabase s, int _oldVersion, int _newVersion)
		{
			Log.d(TAG, "Upgrading from version " + _oldVersion + " to " + _newVersion + ", which will destroy all old data");
			//Drop the old tables and triggers
			Log.d(TAG, "Dropping old Database");
			s.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_THEMELIST_ID_INSERT);
			s.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_THEMELIST_ID_UPDATE);
			s.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_THEMELIST_ID_DELETE);
			s.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SCREENSHOTS);
			s.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_THEMELIST);
			// Create a new one.
			Log.d(TAG, "Create Database");
			s.execSQL(DATABASE_CREATE_THEMELIST);
			s.execSQL(DATABASE_CREATE_SCREENSHOTS);
			s.execSQL(TRIGGER_THEMELISTID_INSERT);
			s.execSQL(TRIGGER_THEMELISTID_UPDATE);
			s.execSQL(TRIGGER_THEMELISTID_DELETE);
		}
		
		// SQL Statements to create a new database.
		private static final String DATABASE_CREATE_THEMELIST =
			"create table " +
			DATABASE_TABLE_THEMELIST +
			" (" +
			KEY_THEMELIST_ID + " integer primary key autoincrement, " +
			KEY_THEMELIST_NAME + " text not null, " +
			KEY_THEMELIST_URI + " text not null, " +
			KEY_THEMELIST_ENABLED + " integer default 0, " +
			KEY_THEMELIST_FEATURED + " integer default 0);";
		
		private static final String DATABASE_CREATE_SCREENSHOTS =
			"create table " +
			DATABASE_TABLE_SCREENSHOTS +
			" (" +
			KEY_SCREENSHOTS_ID + " integer primary key autoincrement, " +
			KEY_SCREENSHOTS_THEMELIST_ID + " integer not null" + 
			" CONSTRAINT " + THEMELIST_ID_FOREIGNKEYCONSTRAINT + " REFERENCES " + DATABASE_TABLE_THEMELIST + "(" + KEY_THEMELIST_ID + ") ON DELETE CASCADE, " +
			KEY_SCREENSHOTS_URI + " text not null, " +
			KEY_SCREENSHOTS_MODIFYDATE + " date not null, " +
			KEY_SCREENSHOTS_SCREENSHOT + " blob);";
		
		//Trigger for foreign Key Constraints (i hate sqlite, hail to ORACLE!)
		private static final String TRIGGER_THEMELISTID_INSERT = 
		"CREATE TRIGGER " + TRIGGER_THEMELIST_ID_INSERT +
		" BEFORE INSERT ON " + DATABASE_TABLE_SCREENSHOTS +
		" FOR EACH ROW BEGIN" +
		" SELECT CASE" +
		" WHEN ((new." + KEY_SCREENSHOTS_THEMELIST_ID + " IS NOT NULL)" +
		" AND ((SELECT " + KEY_THEMELIST_ID + " FROM " + DATABASE_TABLE_THEMELIST +
		" WHERE " + KEY_THEMELIST_ID + " = new." + KEY_SCREENSHOTS_THEMELIST_ID + ") IS NULL))" +
		" THEN RAISE(ABORT, 'insert on table " + DATABASE_TABLE_SCREENSHOTS + 
		" violates foreign key constraint " + THEMELIST_ID_FOREIGNKEYCONSTRAINT + "')" +
		" END;" +
		" END;";
		
		private static final String TRIGGER_THEMELISTID_UPDATE =
		"CREATE TRIGGER " + TRIGGER_THEMELIST_ID_UPDATE +
		" BEFORE UPDATE ON " + DATABASE_TABLE_SCREENSHOTS +
		" FOR EACH ROW BEGIN" + 
		" SELECT CASE" +
		" WHEN ((SELECT " + KEY_THEMELIST_ID + " FROM " + DATABASE_TABLE_THEMELIST +
		" WHERE " + KEY_THEMELIST_ID + " = new." + KEY_SCREENSHOTS_THEMELIST_ID + ") IS NULL)" +
		" THEN RAISE(ABORT, 'update on table " + DATABASE_TABLE_SCREENSHOTS +
		" violates foreign key constraint " + THEMELIST_ID_FOREIGNKEYCONSTRAINT + "')" +
		" END;" +
		" END;";
		
		//Delete cached Screenshots, when ThemeList is removed
		private static final String TRIGGER_THEMELISTID_DELETE =
		"CREATE TRIGGER " + TRIGGER_THEMELIST_ID_DELETE +
		" BEFORE DELETE ON " + DATABASE_TABLE_THEMELIST +
		" FOR EACH ROW BEGIN" +
		" DELETE FROM " + DATABASE_TABLE_SCREENSHOTS +
		" WHERE " + KEY_SCREENSHOTS_THEMELIST_ID + " = old." + KEY_THEMELIST_ID + ";" +
		" END;";
	}
}