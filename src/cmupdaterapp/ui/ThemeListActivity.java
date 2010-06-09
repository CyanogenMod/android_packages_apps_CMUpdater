package cmupdaterapp.ui;

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
import android.view.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cmupdaterapp.customTypes.FullThemeList;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.database.DbAdapter;
import cmupdaterapp.featuredThemes.FeaturedThemes;
import cmupdaterapp.listadapters.ThemeListAdapter;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.utils.Preferences;

import java.net.URI;
import java.util.LinkedList;

public class ThemeListActivity extends ListActivity {
    private static final String TAG = "ThemeListActivity";

    private Boolean showDebugOutput = false;

    private DbAdapter themeListDb;
    private Cursor themeListCursor;
    private ListView lv;
    private Resources res;
    private TextView tv;
    private FullThemeList FeaturedThemes = null;
    private Thread FeaturedThemesThread;
    private ProgressDialog FeaturedThemesProgressDialog;
    public static Handler FeaturedThemesProgressHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showDebugOutput = new Preferences(this).displayDebugOutput();
        themeListDb = new DbAdapter(showDebugOutput);
        if (showDebugOutput) Log.d(TAG, "Opening Database");
        themeListDb.open();
        setContentView(R.layout.themelist);
        tv = (TextView) findViewById(R.id.theme_list_info);
        getThemeList();
        lv = getListView();
        registerForContextMenu(lv);
        res = getResources();
    }

    private void getThemeList() {
        themeListCursor = themeListDb.getAllThemesCursor();
        startManagingCursor(themeListCursor);
        updateThemeList();
    }

    private void updateThemeList() {
        themeListCursor.requery();
        FullThemeList fullThemeList = new FullThemeList();
        if (themeListCursor.moveToFirst()) {
            do {
                String name = themeListCursor.getString(DbAdapter.COLUMN_THEMELIST_NAME);
                String uri = themeListCursor.getString(DbAdapter.COLUMN_THEMELIST_URI);
                int pk = themeListCursor.getInt(DbAdapter.COLUMN_THEMELIST_ID);
                int enabled = themeListCursor.getInt(DbAdapter.COLUMN_THEMELIST_ENABLED);
                int featured = themeListCursor.getInt(DbAdapter.COLUMN_THEMELIST_FEATURED);
                ThemeList newItem = new ThemeList();
                newItem.name = name;
                newItem.url = URI.create(uri);
                newItem.enabled = enabled == 1;
                newItem.featured = featured == 1;
                newItem.PrimaryKey = pk;
                fullThemeList.addThemeToList(newItem);
            }
            while (themeListCursor.moveToNext());
        }
        LinkedList<ThemeList> fullThemeListList = fullThemeList.returnFullThemeList();
        ThemeListAdapter<ThemeList> AdapterThemeList = new ThemeListAdapter<ThemeList>(
                this,
                fullThemeListList);
        setListAdapter(AdapterThemeList);
        if (fullThemeList.getThemeCount() > 0)
            tv.setText(R.string.theme_list_long_press);
        else
            tv.setText(R.string.theme_list_no_themes);
        themeListCursor.deactivate();
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
        super.onListItemClick(parent, v, position, id);
        if (showDebugOutput) Log.d(TAG, "Item clicked. Postition: " + id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, Constants.MENU_THEME_LIST_UPDATE_FEATURED, Menu.NONE, R.string.menu_update_featured);
        menu.add(Menu.NONE, Constants.MENU_THEME_LIST_ADD, Menu.NONE, R.string.menu_add_theme);
        SubMenu deleteMenu = menu.addSubMenu(R.string.theme_submenu_delete);
        deleteMenu.setIcon(android.R.drawable.ic_menu_more);
        deleteMenu.add(Menu.NONE, Constants.MENU_THEME_DELETE_ALL, Menu.NONE, R.string.menu_delete_all_themes);
        deleteMenu.add(Menu.NONE, Constants.MENU_THEME_DELETE_ALL_FEATURED, Menu.NONE, R.string.menu_delete_all_featured_themes);
        SubMenu disableMenu = menu.addSubMenu(R.string.theme_submenu_disable);
        disableMenu.setIcon(android.R.drawable.ic_menu_more);
        disableMenu.add(Menu.NONE, Constants.MENU_THEME_DISABLE_ALL, Menu.NONE, R.string.menu_disable_all_themes);
        disableMenu.add(Menu.NONE, Constants.MENU_THEME_DISABLE_ALL_FEATURED, Menu.NONE, R.string.menu_disable_all_featured_themes);
        SubMenu enableMenu = menu.addSubMenu(R.string.theme_submenu_enable);
        enableMenu.setIcon(android.R.drawable.ic_menu_more);
        enableMenu.add(Menu.NONE, Constants.MENU_THEME_ENABLE_ALL, Menu.NONE, R.string.menu_enable_all_themes);
        enableMenu.add(Menu.NONE, Constants.MENU_THEME_ENABLE_ALL_FEATURED, Menu.NONE, R.string.menu_enable_all_featured_themes);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        ThemeList tl;

        switch (item.getItemId()) {
            case Constants.MENU_THEME_LIST_ADD:
                createNewThemeList(false, "", "", true, 0, false);
                return true;
            case Constants.MENU_THEME_LIST_UPDATE_FEATURED:
                new AlertDialog.Builder(ThemeListActivity.this)
                        .setTitle(R.string.featured_themes_dialog_title)
                        .setMessage(R.string.featured_themes_dialog_summary)
                        .setPositiveButton(R.string.featured_themes_dialog_pos, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                updateFeaturedThemes();
                            }
                        })
                        .setNegativeButton(R.string.featured_themes_dialog_neg, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                return true;
            case Constants.MENU_THEME_LIST_CONTEXT_EDIT:
                if (showDebugOutput) Log.d(TAG, "Edit clicked");
                tl = ((ThemeList) lv.getAdapter().getItem(menuInfo.position));
                createNewThemeList(true, tl.name, tl.url.toString(), tl.enabled, tl.PrimaryKey, tl.featured);
                break;
            case Constants.MENU_THEME_LIST_CONTEXT_DELETE:
                Log.d(TAG, "Delete clicked");
                tl = ((ThemeList) lv.getAdapter().getItem(menuInfo.position));
                DeleteTheme(tl.PrimaryKey);
                break;
            case Constants.MENU_THEME_LIST_CONTEXT_DISABLE:
                Log.d(TAG, "Selected to disable Theme Server");
                tl = ((ThemeList) lv.getAdapter().getItem(menuInfo.position));
                tl.enabled = false;
                themeListDb.updateTheme(tl.PrimaryKey, tl);
                updateThemeList();
                break;
            case Constants.MENU_THEME_LIST_CONTEXT_ENABLE:
                Log.d(TAG, "Selected to enable Theme Server");
                tl = ((ThemeList) lv.getAdapter().getItem(menuInfo.position));
                tl.enabled = true;
                themeListDb.updateTheme(tl.PrimaryKey, tl);
                updateThemeList();
                break;
            case Constants.MENU_THEME_DELETE_ALL:
                Log.d(TAG, "Selected to delete all Theme Servers");
                themeListDb.removeAllThemes();
                updateThemeList();
                break;
            case Constants.MENU_THEME_DELETE_ALL_FEATURED:
                Log.d(TAG, "Selected to delete all Featured Theme Servers");
                themeListDb.removeAllFeaturedThemes();
                updateThemeList();
                break;
            case Constants.MENU_THEME_DISABLE_ALL:
                Log.d(TAG, "Selected to disable all Theme Servers");
                themeListDb.disableAllThemes();
                updateThemeList();
                break;
            case Constants.MENU_THEME_DISABLE_ALL_FEATURED:
                Log.d(TAG, "Selected to disable all Featured Theme Servers");
                themeListDb.disableAllFeaturedThemes();
                updateThemeList();
                break;
            case Constants.MENU_THEME_ENABLE_ALL:
                Log.d(TAG, "Selected to enable all Theme Servers");
                themeListDb.enableAllThemes();
                updateThemeList();
                break;
            case Constants.MENU_THEME_ENABLE_ALL_FEATURED:
                Log.d(TAG, "Selected to enable all Featured Theme Servers");
                themeListDb.enableAllFeaturedThemes();
                updateThemeList();
                break;
            default:
                Log.d(TAG, "Unknown Menu ID:" + item.getItemId());
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onDestroy() {
        // Close the database
        if (showDebugOutput) Log.d(TAG, "Closing Database");
        themeListCursor.close();
        themeListDb.close();
        super.onDestroy();
    }

    private void createNewThemeList(final boolean _update, String _name, String _uri, boolean _enabled, final int _primaryKey, boolean _featured) {
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(res.getString(R.string.p_theme_list_context_menu_header));
        menu.add(Menu.NONE, Constants.MENU_THEME_LIST_CONTEXT_EDIT, Menu.NONE, R.string.menu_edit_theme);
        menu.add(Menu.NONE, Constants.MENU_THEME_LIST_CONTEXT_DELETE, Menu.NONE, R.string.menu_delete_theme);
        ThemeList tl = ((ThemeList) lv.getAdapter().getItem(((AdapterContextMenuInfo) menuInfo).position));
        if (tl.enabled)
            menu.add(Menu.NONE, Constants.MENU_THEME_LIST_CONTEXT_DISABLE, Menu.NONE, R.string.menu_disable_theme);
        else
            menu.add(Menu.NONE, Constants.MENU_THEME_LIST_CONTEXT_ENABLE, Menu.NONE, R.string.menu_enable_theme);
    }

    private void DeleteTheme(int position) {
        if (showDebugOutput) Log.d(TAG, "Remove Theme Postition: " + position);
        if (themeListDb.removeTheme(position))
            if (showDebugOutput) Log.d(TAG, "Success");
            else {
                Log.e(TAG, "Fail");
                Toast.makeText(this, R.string.theme_list_delete_error, Toast.LENGTH_LONG).show();
            }
        updateThemeList();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (showDebugOutput) Log.d(TAG, "RequestCode: " + requestCode + " ResultCode: " + resultCode);
        switch (requestCode) {
            case ThemeListNewActivity.REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Bundle b = intent.getExtras();
                    ThemeList tl = new ThemeList();
                    tl.name = b.getString(Constants.THEME_LIST_NEW_NAME);
                    tl.url = URI.create(b.getString(Constants.THEME_LIST_NEW_URI));
                    tl.enabled = b.getBoolean(Constants.THEME_LIST_NEW_ENABLED);
                    tl.featured = b.getBoolean(Constants.THEME_LIST_NEW_FEATURED);
                    if (b.getBoolean(Constants.THEME_LIST_NEW_UPDATE))
                        tl.PrimaryKey = b.getInt(Constants.THEME_LIST_NEW_PRIMARYKEY);
                    if (!b.getBoolean(Constants.THEME_LIST_NEW_UPDATE))
                        themeListDb.insertTheme(tl);
                    else
                        themeListDb.updateTheme(b.getInt(Constants.THEME_LIST_NEW_PRIMARYKEY), tl);
                    updateThemeList();
                }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void updateFeaturedThemes() {
        if (showDebugOutput) Log.d(TAG, "Called Update Featured Themes");
        FeaturedThemesProgressHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (showDebugOutput) Log.d(TAG, "recieved Message");
                if (FeaturedThemesProgressDialog != null)
                    FeaturedThemesProgressDialog.dismiss();
                if (msg.obj instanceof String) {
                    Toast.makeText(ThemeListActivity.this, (CharSequence) msg.obj, Toast.LENGTH_LONG).show();
                    FeaturedThemes = null;
                    ThemeListActivity.this.FeaturedThemesThread.interrupt();
                    FeaturedThemesProgressDialog.dismiss();
                } else if (msg.obj instanceof FullThemeList) {
                    FeaturedThemes = (FullThemeList) msg.obj;
                    ThemeListActivity.this.FeaturedThemesThread.interrupt();
                    FeaturedThemesProgressDialog.dismiss();
                    if (FeaturedThemes != null && FeaturedThemes.getThemeCount() > 0) {
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