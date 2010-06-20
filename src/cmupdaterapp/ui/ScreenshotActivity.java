package cmupdaterapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import cmupdaterapp.customTypes.Screenshot;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.listadapters.ScreenshotGridViewAdapter;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.tasks.DownloadImageTask;
import cmupdaterapp.utils.Preferences;

public class ScreenshotActivity extends Activity {
    private static ScreenshotGridViewAdapter imageAdapter;
    private DownloadImageTask downloadImageTask;

    public static void NotifyChange() {
        imageAdapter.notifyDataSetChanged();
    }

    public static Screenshot getItem(int position) {
        return (Screenshot) imageAdapter.getItem(position);
    }

    public static int getScreenshotSize() {
        return imageAdapter.getCount();
    }

    public static void AddScreenshot(Screenshot s) {
        imageAdapter.AddScreenshot(s);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screenshots);
        Intent i = getIntent();
        Bundle b = i.getExtras();
        UpdateInfo ui = (UpdateInfo) b.get(Constants.SCREENSHOTS_UPDATE);

        GridView gridview = (GridView) findViewById(R.id.gridview);
        imageAdapter = new ScreenshotGridViewAdapter(this, ui.screenshots.size());

        gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                //Only start the Activity, when the Image is loaded
                if (imageAdapter.GetRealScreenshotSize() > position) {
                    Intent i = new Intent(ScreenshotActivity.this, ScreenshotDetailActivity.class);
                    i.putExtra(Constants.SCREENSHOTS_POSITION, position);
                    startActivity(i);
                }
            }
        });

        //In onCreate, cause when pressing back from Detail, the old Screenshots remain in the List
        downloadImageTask = new DownloadImageTask(this, new Preferences(this).displayDebugOutput());
        downloadImageTask.execute(ui);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Stop image downloading
        downloadImageTask.cancel(true);
        imageAdapter.Destroy();
        imageAdapter.ClearScreenshots();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        //We need the ACTION_DOWN here, cause otherwise when you press back on the screenshotdetailactivity,
        //the ACTION_UP event is sent to this activity, and this will result in going back twice
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN) {
            //Stop the Activity on BackKey
            onDestroy();
            finish();
            return true;
        } else
            return super.dispatchKeyEvent(event);
    }
}