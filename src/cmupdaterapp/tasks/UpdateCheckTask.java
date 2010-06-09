package cmupdaterapp.tasks;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import cmupdaterapp.interfaces.IUpdateCheckService;
import cmupdaterapp.interfaces.IUpdateCheckServiceCallback;
import cmupdaterapp.misc.Log;

public class UpdateCheckTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "UpdateCheckTask";

    private Boolean showDebugOutput = false;

    private IUpdateCheckService myService;
    private final ProgressDialog p;
    private final Context context;
    private boolean mbound;
    private Intent serviceIntent;

    public UpdateCheckTask(Context ctx, ProgressDialog pg, Boolean _showDebugOutput) {
        context = ctx;
        showDebugOutput = _showDebugOutput;
        p = pg;
    }

    @Override
    protected void onPreExecute() {
        serviceIntent = new Intent(IUpdateCheckService.class.getName());
        ComponentName comp = context.startService(serviceIntent);
        if (comp == null)
            Log.e(TAG, "startService failed");
        mbound = context.bindService(serviceIntent, mConnection, 0);
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
            context.unbindService(mConnection);
            mbound = false;
        }
        boolean stopped = context.stopService(serviceIntent);
        if (showDebugOutput) Log.d(TAG, "UpdateCheckService stopped: " + stopped);
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
            p.dismiss();
        }
    };
}