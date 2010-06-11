package cmupdaterapp.tasks;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import cmupdaterapp.interfaces.IUpdateCheckService;
import cmupdaterapp.interfaces.IUpdateCheckServiceCallback;
import cmupdaterapp.misc.Log;
import cmupdaterapp.ui.MainActivity;
import cmupdaterapp.ui.R;

public class UpdateCheckTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "UpdateCheckTask";

    private Boolean showDebugOutput = false;

    private IUpdateCheckService myService;
    private boolean mbound;
    private Intent serviceIntent;
    private final ProgressDialog pg;
    private final MainActivity act;

    public UpdateCheckTask(MainActivity a, Boolean _showDebugOutput) {
        showDebugOutput = _showDebugOutput;
        act = a;
	    pg = new ProgressDialog(a);
	    pg.setTitle(R.string.checking_for_updates);
	    pg.setMessage(a.getResources().getString(R.string.checking_for_updates));
	    pg.setIndeterminate(true);
	    pg.setCancelable(true);
	    pg.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            	if (!isCancelled()) {
            		cancel(true);
            	}
            }
        });
    }

    @Override
    protected void onPreExecute() {
    	pg.show();
        serviceIntent = new Intent(IUpdateCheckService.class.getName());
        ComponentName comp = act.startService(serviceIntent);
        if (comp == null)
            Log.e(TAG, "startService failed");
        mbound = act.bindService(serviceIntent, mConnection, 0);
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try {
            //Wait till the Service is bound
            while (myService == null) {
            }
            myService.checkForUpdates();
        }
        catch (RemoteException e) {
            Log.e(TAG, "Exception on calling UpdateCheckService", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (mbound) {
            act.unbindService(mConnection);
            mbound = false;
        }
        boolean stopped = act.stopService(serviceIntent);
        if (showDebugOutput) Log.d(TAG, "UpdateCheckService stopped: " + stopped);
        act.updateLayout();
    }

    @Override
    protected void onCancelled() {
    	if (mbound) {
            act.unbindService(mConnection);
            mbound = false;
        }
        act.stopService(serviceIntent);
    	if (pg != null) {
    		pg.dismiss();
    	}
    	act.updateLayout();
    	super.onCancelled();
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            myService = IUpdateCheckService.Stub.asInterface(service);
            try {
                myService.registerCallback(mCallback);
            }
            catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            try {
                myService.unregisterCallback(mCallback);
            }
            catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
            }
            myService = null;
        }
    };

    private final IUpdateCheckServiceCallback mCallback = new IUpdateCheckServiceCallback.Stub() {
        public void UpdateCheckFinished() throws RemoteException {
        	if (pg != null) {
        		pg.dismiss();
        	}
        }
    };
}