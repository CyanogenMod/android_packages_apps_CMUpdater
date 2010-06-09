package cmupdaterapp.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import cmupdaterapp.customTypes.FullThemeList;
import cmupdaterapp.customTypes.Screenshot;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.customization.Customization;
import cmupdaterapp.misc.Log;
import cmupdaterapp.utils.StringUtils;

import java.io.File;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

public class DbAdapter
{
	private static final String TAG = "DbAdapter";
	
	private static Boolean showDebugOutput = false;

	private static final String DATABASE_NAME = "cmupdater.db";
	private static final int DATABASE_VERSION = 5;
	//Themelist
	private static final String DATABASE_TABLE_THEMELIST = "ThemeList";
	private static final String KEY_THEMELIST_ID = "id";
	private static final String INDEX_THEMELIST_ID = "uidx_themelist_id";
	public static final int COLUMN_THEMELIST_ID = 0;
	private static final String KEY_THEMELIST_NAME = "name";
	private static final String INDEX_THEMELIST_NAME = "idx_themelist_name";
	public static final int COLUMN_THEMELIST_NAME = 1;
	private static final String KEY_THEMELIST_URI = "uri";
	private static final String INDEX_THEMELIST_URI = "idx_themelist_uri";
	public static final int COLUMN_THEMELIST_URI = 2;
	private static final String KEY_THEMELIST_ENABLED = "enabled";
	private static final String INDEX_THEMELIST_ENABLED = "idx_themelist_enabled";
	public static final int COLUMN_THEMELIST_ENABLED = 3;
	private static final String KEY_THEMELIST_FEATURED = "featured";
	private static final String INDEX_THEMELIST_FEATURED = "idx_themelist_featured";
	public static final int COLUMN_THEMELIST_FEATURED = 4;
	//Screenshots
	private static final String DATABASE_TABLE_SCREENSHOT = "Screenshot";
	private static final String THEMELIST_ID_FOREIGNKEYCONSTRAINT = "fk_themelist_id";
	private static final String TRIGGER_THEMELIST_ID_INSERT = "fki_themelist_id";
	private static final String TRIGGER_THEMELIST_ID_UPDATE = "fku_themelist_id";
	private static final String TRIGGER_THEMELIST_ID_DELETE = "fkd_themelist_id";
	private static final String KEY_SCREENSHOT_ID = "id";
	private static final String INDEX_SCREENSHOT_ID = "uidx_screenshot_id";
	private static final int COLUMN_SCREENSHOT_ID = 0;
	private static final String KEY_SCREENSHOT_THEMELIST_ID = "themelist_id";
	private static final String INDEX_SCREENSHOT_THEMELIST_ID = "idx_screenshot_themelist_id";
	private static final int COLUMN_SCREENSHOT_THEMELIST_ID = 1;
	private static final String KEY_SCREENSHOT_URI = "uri";
	private static final String INDEX_SCREENSHOT_URI = "idx_screenshot_uri";
	private static final int COLUMN_SCREENSHOT_URI = 2;
	private static final String KEY_SCREENSHOT_MODIFYDATE = "modifydate";
	private static final String INDEX_SCREENSHOT_MODIFYDATE = "idx_screenshot_modifydate";
	private static final int COLUMN_SCREENSHOT_MODIFYDATE = 3;
	private static final String KEY_SCREENSHOT_SCREENSHOT = "screenshot";
	private static final String INDEX_SCREENSHOT_SCREENSHOT = "idx_screenshot_screenshot";
	private static final int COLUMN_SCREENSHOT_SCREENSHOT = 4;

	private SQLiteDatabase db;
	private final DbOpenHelper dbHelper;

	public DbAdapter(Boolean _showDebugOutput)
	{
		showDebugOutput = _showDebugOutput;
		dbHelper = new DbOpenHelper();
	}

	public void close()
	{
		db.close();
	}

	public void open()
	{
		File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Customization.EXTERNAL_DATA_DIRECTORY + "/");
		f.mkdirs();
		db = dbHelper.open(f.toString());
	}

	// Insert a new Theme
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
		return db.delete(DATABASE_TABLE_THEMELIST, KEY_THEMELIST_ID + "= ?", new String[]{ Long.toString(_rowIndex) }) > 0;
	}
	
	public int getThemeCount()
	{
		Cursor c = db.query(DATABASE_TABLE_THEMELIST, new String[] {KEY_THEMELIST_ID}, null, null, null, null, null);
		int ret = c.getCount();
		c.close();
		return ret;
	}

	// Removes all themes
	public boolean removeAllThemes()
	{
		return db.delete(DATABASE_TABLE_THEMELIST, null, null) > 0;
	}

	// Removes all themes
	public boolean removeAllFeaturedThemes()
	{
		return db.delete(DATABASE_TABLE_THEMELIST, KEY_THEMELIST_FEATURED + "= ?", new String[]{ "1" }) > 0;
	}

	// Disable all Themes
	public boolean disableAllThemes()
	{
		ContentValues newValue = new ContentValues();
		newValue.put(KEY_THEMELIST_ENABLED, 0);
		return db.update(DATABASE_TABLE_THEMELIST, newValue, null, null) > 0;
	}

	// Disable all Featured Themes
	public boolean disableAllFeaturedThemes()
	{
		ContentValues newValue = new ContentValues();
		newValue.put(KEY_THEMELIST_ENABLED, 0);
		return db.update(DATABASE_TABLE_THEMELIST, newValue, KEY_THEMELIST_FEATURED + "= ?", new String[]{ "1" }) > 0;
	}

	// Enable all Themes
	public boolean enableAllThemes()
	{
		ContentValues newValue = new ContentValues();
		newValue.put(KEY_THEMELIST_ENABLED, 1);
		return db.update(DATABASE_TABLE_THEMELIST, newValue, null, null) > 0;
	}

	// enable all Featured Themes
	public boolean enableAllFeaturedThemes()
	{
		ContentValues newValue = new ContentValues();
		newValue.put(KEY_THEMELIST_ENABLED, 1);
		return db.update(DATABASE_TABLE_THEMELIST, newValue, KEY_THEMELIST_FEATURED + "= ?", new String[]{ "1" }) > 0;
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
		return db.update(DATABASE_TABLE_THEMELIST, newValue, KEY_THEMELIST_ID + "= ?", new String[]{ Long.toString(_rowIndex) }) > 0;
	}

	public Cursor getAllThemesCursor()
	{
		return db.query(DATABASE_TABLE_THEMELIST, new String[] { KEY_THEMELIST_ID, KEY_THEMELIST_NAME, KEY_THEMELIST_URI, KEY_THEMELIST_ENABLED, KEY_THEMELIST_FEATURED }, null, null, null, null, KEY_THEMELIST_NAME);
	}

	public ThemeList getThemeItem(long _rowIndex) throws SQLException
	{
		Cursor cursor = db.query(true, DATABASE_TABLE_THEMELIST, new String[] { KEY_THEMELIST_ID, KEY_THEMELIST_NAME, KEY_THEMELIST_URI, KEY_THEMELIST_ENABLED, KEY_THEMELIST_FEATURED }, KEY_THEMELIST_ID + "= ?", new String[]{ Long.toString(_rowIndex) }, null, null, null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst())
		{
			cursor.close();
			throw new SQLException("No Theme item found for row: " + _rowIndex);
		}
		String name = cursor.getString(COLUMN_THEMELIST_NAME);
		String uri = cursor.getString(COLUMN_THEMELIST_URI);
		int enabled = cursor.getInt(COLUMN_THEMELIST_ENABLED);
		int featured = cursor.getInt(COLUMN_THEMELIST_FEATURED);
		int Key = cursor.getInt(COLUMN_THEMELIST_ID);
		ThemeList result = new ThemeList();
		result.name = name;
		result.url = URI.create(uri);
		result.PrimaryKey = Key;
        result.enabled = enabled == 1;
        result.featured = featured == 1;
		cursor.close();
		return result;
	}

	public void UpdateFeaturedThemes(FullThemeList t)
	{
		FullThemeList retValue = new FullThemeList();
		//Get the enabled state of the current Featured Themes
		for (ThemeList tl : t.returnFullThemeList())
		{
			Cursor result = db.query(true, DATABASE_TABLE_THEMELIST, new String[] { KEY_THEMELIST_ID, KEY_THEMELIST_NAME, KEY_THEMELIST_URI, KEY_THEMELIST_ENABLED, KEY_THEMELIST_FEATURED }, KEY_THEMELIST_NAME + "= ? and " + KEY_THEMELIST_FEATURED + "= ?" , new String[]{ tl.name, "1" }, null, null, null, null);
			if ((result.getCount() == 0) || !result.moveToFirst())
			{
				if (showDebugOutput) Log.d(TAG, "Theme " + tl.name + " not found in your List");
				retValue.addThemeToList(tl);
				continue;
			}
			tl.enabled = result.getInt(COLUMN_THEMELIST_ENABLED) != 0;
			retValue.addThemeToList(tl);
			result.close();
		}
		//Delete all featured Themes
		db.delete(DATABASE_TABLE_THEMELIST, KEY_THEMELIST_FEATURED + "= ?", new String[]{ "1" });
		if (showDebugOutput) Log.d(TAG, "Deleted all old Featured Theme Servers");
		//Add all Featured Themes again
		for (ThemeList tl2 : retValue.returnFullThemeList())
		{
			insertTheme(tl2);
		}
		if (showDebugOutput) Log.d(TAG, "Updated Featured Theme Servers");
	}

	//SCREENSHOTS

	// Remove a Screenshot based on its index
	public boolean removeScreenshot(long _rowIndex)
	{
		return db.delete(DATABASE_TABLE_SCREENSHOT, KEY_SCREENSHOT_ID + "= ?", new String[]{ Long.toString(_rowIndex) }) > 0;
	}

	// Remove all Screenshots for given Theme except the ones in the parameter
	public boolean removeScreenshotExcept(int ForeignKey, String[] primaryKeysNotToRemove)
	{
		if (primaryKeysNotToRemove == null || primaryKeysNotToRemove.length == 0)
			return false;
		String temp = StringUtils.arrayToString(primaryKeysNotToRemove, ",");
		return db.delete(DATABASE_TABLE_SCREENSHOT, KEY_SCREENSHOT_THEMELIST_ID + "= ? AND "
				+ KEY_SCREENSHOT_ID + " not in (?)", new String[]{ Integer.toString(ForeignKey), temp }) > 0;
	}

	// Remove a Screenshot based on its FeaturedThemeIndex
	public boolean removeAllScreenshotsForTheme(long FeaturedThemeId)
	{
		return db.delete(DATABASE_TABLE_SCREENSHOT, KEY_SCREENSHOT_THEMELIST_ID + "= ?", new String[]{ Long.toString(FeaturedThemeId) }) > 0;
	}

	// Insert a new Screenshot
	public long insertScreenshot(Screenshot _screenshot)
	{
		
		ContentValues newValues = new ContentValues();
		newValues.put(KEY_SCREENSHOT_THEMELIST_ID, _screenshot.ForeignThemeListKey);
		newValues.put(KEY_SCREENSHOT_URI, _screenshot.url.toString());
		newValues.put(KEY_SCREENSHOT_MODIFYDATE, _screenshot.getModifyDate());
		newValues.put(KEY_SCREENSHOT_SCREENSHOT, _screenshot.getPictureAsByteArray());
		return db.insert(DATABASE_TABLE_SCREENSHOT, null, newValues);
	}

	//Get all Screenshots for a Theme
	public List<Screenshot> getAllScreenshotsForTheme(long _themeIndex) throws SQLException
	{
		Cursor cursor = db.query(true, DATABASE_TABLE_SCREENSHOT,
				new String[]
				           {
							KEY_SCREENSHOT_ID,
							KEY_SCREENSHOT_THEMELIST_ID,
							KEY_SCREENSHOT_URI,
							KEY_SCREENSHOT_MODIFYDATE,
							KEY_SCREENSHOT_SCREENSHOT
				           }, KEY_SCREENSHOT_THEMELIST_ID + "= ?", new String[]{ Long.toString(_themeIndex) }, null, null, null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst())
		{
			cursor.close();
			return null;
		}

		List<Screenshot> result = new LinkedList<Screenshot>();

		cursor.moveToFirst();
		do
		{
			Screenshot item = new Screenshot();
			item.PrimaryKey = cursor.getInt(COLUMN_SCREENSHOT_ID);
			item.ForeignThemeListKey = cursor.getInt(COLUMN_SCREENSHOT_THEMELIST_ID);
			item.url = URI.create(cursor.getString(COLUMN_SCREENSHOT_URI));
			item.setModifyDate(cursor.getString(COLUMN_SCREENSHOT_MODIFYDATE));
			item.setBitmapFromByteArray(cursor.getBlob(COLUMN_SCREENSHOT_SCREENSHOT));
			result.add(item);
		} while (cursor.moveToNext());
		cursor.close();
		return result;
	}

	//Get single Screenshots by Id
	public Screenshot getScreenshotById(long _index) throws SQLException
	{
		Cursor cursor = db.query(true, DATABASE_TABLE_SCREENSHOT,
				new String[]
				           {
							KEY_SCREENSHOT_ID,
							KEY_SCREENSHOT_THEMELIST_ID,
							KEY_SCREENSHOT_URI,
							KEY_SCREENSHOT_MODIFYDATE,
							KEY_SCREENSHOT_SCREENSHOT
				           }, KEY_SCREENSHOT_ID + "= ?", new String[]{ Long.toString(_index) }, null, null, null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst())
		{
			cursor.close();
			throw new SQLException("No Screenshot found for Key: " + _index);
		}

		Screenshot result = new Screenshot();
		result.PrimaryKey = cursor.getInt(COLUMN_SCREENSHOT_ID);
		result.ForeignThemeListKey = cursor.getInt(COLUMN_SCREENSHOT_THEMELIST_ID);
		result.url = URI.create(cursor.getString(COLUMN_SCREENSHOT_URI));
		result.setModifyDate(cursor.getString(COLUMN_SCREENSHOT_MODIFYDATE));
		result.setBitmapFromByteArray(cursor.getBlob(COLUMN_SCREENSHOT_SCREENSHOT));
		cursor.close();
		return result;
	}

	//Checks if a Screenshot already exists. Cause the Primary Key is Stored in the Screenshot Object
	//The contains() Method will not work cause theres no Primary Key on Download
	//Will return a Screenshotobject with only the PrimaryKey if found, otherwise -1,the Modifydate and the blob
	public Screenshot ScreenshotExists(int ForeignKey, String Url) throws SQLException
	{
		Screenshot retValue = new Screenshot();
		Cursor cursor = db.query(true, DATABASE_TABLE_SCREENSHOT,
				new String[]
				           {
							KEY_SCREENSHOT_ID,
							KEY_SCREENSHOT_THEMELIST_ID,
							KEY_SCREENSHOT_URI,
							KEY_SCREENSHOT_MODIFYDATE,
							KEY_SCREENSHOT_SCREENSHOT
				           }, KEY_SCREENSHOT_THEMELIST_ID + "= ? AND " +
				           KEY_SCREENSHOT_URI + "= ?",
				           new String[]{ Integer.toString(ForeignKey), Url }, null, null, null, null);
		if ((cursor.getCount() != 0) && cursor.moveToFirst())
		{
			retValue.PrimaryKey = cursor.getInt(COLUMN_SCREENSHOT_ID);
			retValue.ForeignThemeListKey = cursor.getInt(COLUMN_SCREENSHOT_THEMELIST_ID);
			retValue.url = URI.create(cursor.getString(COLUMN_SCREENSHOT_URI));
			retValue.setModifyDate(cursor.getString(COLUMN_SCREENSHOT_MODIFYDATE));
			retValue.setBitmapFromByteArray(cursor.getBlob(COLUMN_SCREENSHOT_SCREENSHOT));
		}
		cursor.close();
		return retValue;
	}

	// Update a Screenshot
	public boolean updateScreenshot(long _rowIndex, Screenshot _screenshot)
	{
		ContentValues newValue = new ContentValues();
		newValue.put(KEY_SCREENSHOT_THEMELIST_ID, _screenshot.ForeignThemeListKey);
		newValue.put(KEY_SCREENSHOT_URI, _screenshot.url.toString());
		newValue.put(KEY_SCREENSHOT_MODIFYDATE, _screenshot.getModifyDate());
		newValue.put(KEY_SCREENSHOT_SCREENSHOT, _screenshot.getPictureAsByteArray());
		return db.update(DATABASE_TABLE_SCREENSHOT, newValue, KEY_SCREENSHOT_ID + "= ?", new String[]{ Long.toString(_rowIndex) }) > 0;
	}

	// Delete All Screenshots
	public void deleteAllScreenshot()
	{
		db.execSQL("DELETE FROM " + DATABASE_TABLE_SCREENSHOT + ";");
	}

	//Helper Class for opening/creating a Database
	private static class DbOpenHelper
	{
		public SQLiteDatabase open(String path)
		{
			if (!path.endsWith("/"))
				path += "/";
			String databasePath = path + DbAdapter.DATABASE_NAME;
			SQLiteDatabase s = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
			if (s.needUpgrade(DbAdapter.DATABASE_VERSION))
				update(s, s.getVersion());
			s.setVersion(DbAdapter.DATABASE_VERSION);
			return s;
		}

		private void update(SQLiteDatabase s, int _oldVersion)
		{
			if (showDebugOutput) Log.d(TAG, "Upgrading from version " + _oldVersion + " to " + DbAdapter.DATABASE_VERSION + ", which will destroy all old data");
			//Drop the old tables and triggers
			if (showDebugOutput) Log.d(TAG, "Dropping old Database");
			s.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_THEMELIST_ID_INSERT);
			s.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_THEMELIST_ID_UPDATE);
			s.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_THEMELIST_ID_DELETE);

			s.execSQL("DROP INDEX IF EXISTS " + INDEX_THEMELIST_ID);
			s.execSQL("DROP INDEX IF EXISTS " + INDEX_THEMELIST_NAME);
			s.execSQL("DROP INDEX IF EXISTS " + INDEX_THEMELIST_URI);
			s.execSQL("DROP INDEX IF EXISTS " + INDEX_THEMELIST_ENABLED);		
			s.execSQL("DROP INDEX IF EXISTS " + INDEX_THEMELIST_FEATURED);
			s.execSQL("DROP INDEX IF EXISTS " + INDEX_SCREENSHOT_ID);
			s.execSQL("DROP INDEX IF EXISTS " + INDEX_SCREENSHOT_THEMELIST_ID);
			s.execSQL("DROP INDEX IF EXISTS " + INDEX_SCREENSHOT_URI);
			s.execSQL("DROP INDEX IF EXISTS " + INDEX_SCREENSHOT_MODIFYDATE);
			s.execSQL("DROP INDEX IF EXISTS " + INDEX_SCREENSHOT_SCREENSHOT);

			s.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SCREENSHOT);
			s.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_THEMELIST);

			// Create a new one.
			if (showDebugOutput) Log.d(TAG, "Create Database");
			s.execSQL(DATABASE_CREATE_THEMELIST);
			s.execSQL(DATABASE_CREATE_SCREENSHOTS);

			s.execSQL(INDEX_THEMELIST_THEMELIST_ID);
			s.execSQL(INDEX_THEMELIST_THEMELIST_NAME);
			s.execSQL(INDEX_THEMELIST_THEMELIST_URI);
			s.execSQL(INDEX_THEMELIST_THEMELIST_ENABLED);		
			s.execSQL(INDEX_THEMELIST_THEMELIST_FEATURED);
			s.execSQL(INDEX_SCREENSHOT_SCREENSHOT_ID);
			s.execSQL(INDEX_SCREENSHOT_SCREENSHOT_THEMELIST_ID);
			s.execSQL(INDEX_SCREENSHOT_SCREENSHOT_URI);
			s.execSQL(INDEX_SCREENSHOT_SCREENSHOT_MODIFYDATE);
			s.execSQL(INDEX_SCREENSHOT_SCREENSHOT_SCREENSHOT);

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
			DATABASE_TABLE_SCREENSHOT +
			" (" +
			KEY_SCREENSHOT_ID + " integer primary key autoincrement, " +
			KEY_SCREENSHOT_THEMELIST_ID + " integer not null" + 
			" CONSTRAINT " + THEMELIST_ID_FOREIGNKEYCONSTRAINT + " REFERENCES " + DATABASE_TABLE_THEMELIST + "(" + KEY_THEMELIST_ID + ") ON DELETE CASCADE, " +
			KEY_SCREENSHOT_URI + " text not null, " +
			KEY_SCREENSHOT_MODIFYDATE + " date not null, " +
			KEY_SCREENSHOT_SCREENSHOT + " blob);";

		//Trigger for foreign Key Constraints (i hate sqlite, hail to ORACLE!)
		private static final String TRIGGER_THEMELISTID_INSERT = 
		"CREATE TRIGGER " + TRIGGER_THEMELIST_ID_INSERT +
		" BEFORE INSERT ON " + DATABASE_TABLE_SCREENSHOT +
		" FOR EACH ROW BEGIN" +
		" SELECT CASE" +
		" WHEN ((new." + KEY_SCREENSHOT_THEMELIST_ID + " IS NOT NULL)" +
		" AND ((SELECT " + KEY_THEMELIST_ID + " FROM " + DATABASE_TABLE_THEMELIST +
		" WHERE " + KEY_THEMELIST_ID + " = new." + KEY_SCREENSHOT_THEMELIST_ID + ") IS NULL))" +
		" THEN RAISE(ABORT, 'insert on table " + DATABASE_TABLE_SCREENSHOT + 
		" violates foreign key constraint " + THEMELIST_ID_FOREIGNKEYCONSTRAINT + "')" +
		" END;" +
		" END;";

		private static final String TRIGGER_THEMELISTID_UPDATE =
		"CREATE TRIGGER " + TRIGGER_THEMELIST_ID_UPDATE +
		" BEFORE UPDATE ON " + DATABASE_TABLE_SCREENSHOT +
		" FOR EACH ROW BEGIN" + 
		" SELECT CASE" +
		" WHEN ((SELECT " + KEY_THEMELIST_ID + " FROM " + DATABASE_TABLE_THEMELIST +
		" WHERE " + KEY_THEMELIST_ID + " = new." + KEY_SCREENSHOT_THEMELIST_ID + ") IS NULL)" +
		" THEN RAISE(ABORT, 'update on table " + DATABASE_TABLE_SCREENSHOT +
		" violates foreign key constraint " + THEMELIST_ID_FOREIGNKEYCONSTRAINT + "')" +
		" END;" +
		" END;";

		//Delete cached Screenshots, when ThemeList is removed
		private static final String TRIGGER_THEMELISTID_DELETE =
		"CREATE TRIGGER " + TRIGGER_THEMELIST_ID_DELETE +
		" BEFORE DELETE ON " + DATABASE_TABLE_THEMELIST +
		" FOR EACH ROW BEGIN" +
		" DELETE FROM " + DATABASE_TABLE_SCREENSHOT +
		" WHERE " + KEY_SCREENSHOT_THEMELIST_ID + " = old." + KEY_THEMELIST_ID + ";" +
		" END;";

		//Indeces ThemeList
		private static final String INDEX_THEMELIST_THEMELIST_ID =
		"CREATE UNIQUE INDEX IF NOT EXISTS " + INDEX_THEMELIST_ID +
		" ON " + DATABASE_TABLE_THEMELIST + "(" + KEY_THEMELIST_ID + ");";

		private static final String INDEX_THEMELIST_THEMELIST_NAME =
		"CREATE INDEX IF NOT EXISTS " + INDEX_THEMELIST_NAME +
		" ON " + DATABASE_TABLE_THEMELIST + "(" + KEY_THEMELIST_NAME + ");";

		private static final String INDEX_THEMELIST_THEMELIST_URI =
		"CREATE INDEX IF NOT EXISTS " + INDEX_THEMELIST_URI +
		" ON " + DATABASE_TABLE_THEMELIST + "(" + KEY_THEMELIST_URI + ");";

		private static final String INDEX_THEMELIST_THEMELIST_ENABLED =
		"CREATE INDEX IF NOT EXISTS " + INDEX_THEMELIST_ENABLED +
		" ON " + DATABASE_TABLE_THEMELIST + "(" + KEY_THEMELIST_ENABLED + ");";

		private static final String INDEX_THEMELIST_THEMELIST_FEATURED =
		"CREATE INDEX IF NOT EXISTS " + INDEX_THEMELIST_FEATURED +
		" ON " + DATABASE_TABLE_THEMELIST + "(" + KEY_THEMELIST_FEATURED + ");";

		//Indeces Screenshots
		private static final String INDEX_SCREENSHOT_SCREENSHOT_ID =
		"CREATE UNIQUE INDEX IF NOT EXISTS " + INDEX_SCREENSHOT_ID +
		" ON " + DATABASE_TABLE_SCREENSHOT + "(" + KEY_SCREENSHOT_ID + ");";

		private static final String INDEX_SCREENSHOT_SCREENSHOT_THEMELIST_ID =
		"CREATE INDEX IF NOT EXISTS " + INDEX_SCREENSHOT_THEMELIST_ID +
		" ON " + DATABASE_TABLE_SCREENSHOT + "(" + KEY_SCREENSHOT_THEMELIST_ID + ");";

		private static final String INDEX_SCREENSHOT_SCREENSHOT_URI =
		"CREATE INDEX IF NOT EXISTS " + INDEX_SCREENSHOT_URI +
		" ON " + DATABASE_TABLE_SCREENSHOT + "(" + KEY_SCREENSHOT_URI + ");";

		private static final String INDEX_SCREENSHOT_SCREENSHOT_MODIFYDATE =
		"CREATE INDEX IF NOT EXISTS " + INDEX_SCREENSHOT_MODIFYDATE +
		" ON " + DATABASE_TABLE_SCREENSHOT + "(" + KEY_SCREENSHOT_MODIFYDATE + ");";

		private static final String INDEX_SCREENSHOT_SCREENSHOT_SCREENSHOT =
		"CREATE INDEX IF NOT EXISTS " + INDEX_SCREENSHOT_SCREENSHOT +
		" ON " + DATABASE_TABLE_SCREENSHOT + "(" + KEY_SCREENSHOT_SCREENSHOT + ");";
	}
}