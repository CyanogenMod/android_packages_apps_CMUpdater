package cmupdaterapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.ui.Log;

public class ThemeListDbAdapter
{
	private static final String DATABASE_NAME = "cmupdater";
	private static final String DATABASE_TABLE = "ThemeList";
	private static final int DATABASE_VERSION = 1;
	public static final String KEY_ID = "_id";
	public static final String KEY_NAME = "name";
	public static final int KEY_NAME_COLUMN = 1;
	public static final String KEY_URI = "uri";
	public static final int KEY_URI_COLUMN = 2;
	private SQLiteDatabase db;
	private final Context context;
	private ThemeListDbOpenHelper dbHelper;
	
	public ThemeListDbAdapter(Context _context)
	{
		this.context = _context;
		dbHelper = new ThemeListDbOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public void close()
	{
		db.close();
	}
	
	public void open() throws SQLiteException
	{
		try
		{
			db = dbHelper.getWritableDatabase();
		}
		catch (SQLiteException ex)
		{
			db = dbHelper.getReadableDatabase();
		}
	}
	
	// Insert a new task
	public long insertTheme(ThemeList _theme)
	{
		// 	Create a new row of values to insert.
		ContentValues newTaskValues = new ContentValues();
		// Assign values for each row.
		newTaskValues.put(KEY_NAME, _theme.name);
		newTaskValues.put(KEY_URI, _theme.url);
		// Insert the row.
		return db.insert(DATABASE_TABLE, null, newTaskValues);
	}
	
	// Remove a theme based on its index
	public boolean removeTheme(long _rowIndex)
	{
		return db.delete(DATABASE_TABLE, KEY_ID + "=" + _rowIndex, null) > 0;
	}
	
	// Update a Theme
	public boolean updateTheme(long _rowIndex, ThemeList _theme)
	{
		ContentValues newValue = new ContentValues();
		newValue.put(KEY_NAME, _theme.name);
		newValue.put(KEY_URI, _theme.url);
		return db.update(DATABASE_TABLE, newValue, KEY_ID + "=" + _rowIndex, null) > 0;
	}
	
	public Cursor getAllThemesCursor()
	{
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_NAME, KEY_URI }, null, null, null, null, null);
	}

	public Cursor setCursorToThemeItem(long _rowIndex) throws SQLException
	{
		Cursor result = db.query(true, DATABASE_TABLE, new String[] {KEY_ID, KEY_NAME}, KEY_ID + "=" + _rowIndex, null, null, null, null, null);
		if ((result.getCount() == 0) || !result.moveToFirst())
		{
			throw new SQLException("No Theme items found for row: " + _rowIndex);
		}
		return result;
	}

	public ThemeList getThemeItem(long _rowIndex) throws SQLException
	{
		Cursor cursor = db.query(true, DATABASE_TABLE, new String[] {KEY_ID, KEY_NAME}, KEY_ID + "=" + _rowIndex, null, null, null, null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst())
		{
			throw new SQLException("No Theme item found for row: " + _rowIndex);
		}
		String name = cursor.getString(KEY_NAME_COLUMN);
		String uri = cursor.getString(KEY_URI_COLUMN);
		ThemeList result = new ThemeList();
		result.name = name;
		result.url = uri;
		return result;
	}

	private static class ThemeListDbOpenHelper extends SQLiteOpenHelper
	{
		public ThemeListDbOpenHelper(Context context, String name, CursorFactory factory, int version)
		{
			super(context, name, factory, version);
		}
		// SQL Statement to create a new database.
		private static final String DATABASE_CREATE = "create table " +
		DATABASE_TABLE + " (" + KEY_ID + " integer primary key autoincrement, " + KEY_NAME + " text not null, "
		+ KEY_URI + " text not null);";
		
		@Override
		public void onCreate(SQLiteDatabase _db)
		{
			_db.execSQL(DATABASE_CREATE);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion)
		{
			Log.d("ThemeListDBAdapter", "Upgrading from version " + _oldVersion + " to " + _newVersion + ", which will destroy all old data");
			//Drop the old table.
			_db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			// Create a new one.
			onCreate(_db);
		}
	}
}