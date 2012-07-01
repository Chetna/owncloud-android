/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package eu.alefzero.owncloud.files.services;

import java.util.LinkedList;

import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.db.DbHandler;
import eu.alefzero.owncloud.files.services.transer_handlers.DownloadTransfer;
import eu.alefzero.owncloud.files.services.transer_handlers.MkDirTransfer;
import eu.alefzero.owncloud.files.services.transer_handlers.RemoveTransfer;
import eu.alefzero.owncloud.files.services.transer_handlers.OnTransferCompletedListener;
import eu.alefzero.owncloud.files.services.transer_handlers.TransferHandler;
import eu.alefzero.owncloud.ui.fragment.FileDetailFragment;

import android.accounts.Account;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/*
 * DataTransferService is made to unify transfer between user equipment
 * and ownCloud instance. Its duty is to handle all network traffic, failures
 * and postpone network actions when connection is unavailable.
 */
public class DataTransferService extends Service implements OnTransferCompletedListener {

    public final static String EXTRA_TRANSFER_TYPE = "EXTRA_TRANSFER_TYPE";
    public final static String EXTRA_TRANSFER_ACCOUNT = "EXTRA_TRANSFER_ACCOUNT";
    public final static String EXTRA_TRANSFER_DATA1 = "EXTRA_TRANSFER_DATA1";
    public final static String EXTRA_TRANSFER_DATA2 = "EXTRA_TRANSFER_DATA2";
    public final static String EXTRA_TRANSFER_DATA3 = "EXTRA_TRANSFER_DATA3";
    public final static String EXTRA_PENDING_TRANSFER_ID = "EXTRA_PENDING_TRANSFER_ID";

    public final static String TRANSFER_COMPLETED = "eu.alefzero.owncloud.files.services.DataTransferService.TRANSFER_COMPLETED";
    
    /*
     * Dummy action for handle when no transfer type is unavailable
     */
    public final static int TYPE_UNKNOWN = 0;
    public final static int TYPE_DOWNLOAD_FILE = 1;
    public final static int TYPE_UPLOAD_FILE = 2;
    /* 
     * Mkdir transfer type reqires two extra arguments,
     * firs one is account, so url and credentials can be easy retrieved,
     * second one is absolute path to directory which should be created 
     */
    public final static int TYPE_MKDIR = 3;
    public final static int TYPE_SYNCDIR = 4;
    public final static int TYPE_REMOVE = 5;

    private WorkQueue mWorkQueue;
    private ConnectivityManager mConnMngr;
    private EquipmentConnectedReceiver mEqConnRec;
    
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mWorkQueue = new WorkQueue(5);
        mConnMngr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mEqConnRec = new EquipmentConnectedReceiver();
        registerReceiver(mEqConnRec, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("ASD", "destroy");
        unregisterReceiver(mEqConnRec);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return Service.START_STICKY;
        if (!isOnline()) {
            storeDataInDB(intent);
            return Service.START_STICKY;
        }
        
        Log.e("ASD", "doing start command");
        
        TransferHandler r;
        Account account = intent.getParcelableExtra(EXTRA_TRANSFER_ACCOUNT);

        switch (intent.getIntExtra(EXTRA_TRANSFER_TYPE, TYPE_UNKNOWN)) {
            case TYPE_SYNCDIR:
            {
            }
            case TYPE_MKDIR:
            {
                String path = intent.getStringExtra(EXTRA_TRANSFER_DATA1);
                r = new MkDirTransfer(this, account, path);
                break;
            }
            case TYPE_UPLOAD_FILE:
            {
                String localPath = intent.getStringExtra(EXTRA_TRANSFER_DATA1);
                String remotePath = intent.getStringExtra(EXTRA_TRANSFER_DATA2);
            }
            case TYPE_DOWNLOAD_FILE:
            {
                String path = intent.getStringExtra(EXTRA_TRANSFER_DATA1);
                r = new DownloadTransfer(this, account, path);
                break;
            }
            case TYPE_REMOVE:
            {
                String path = intent.getStringExtra(EXTRA_TRANSFER_DATA1);
                r = new RemoveTransfer(this, account, path);
                break;
            }
            case TYPE_UNKNOWN:
            default:
                Log.e("ASD","ASD");
                return Service.START_STICKY;
        }
        
        r.setOnTransferCompletedListener(this);
        
        mWorkQueue.execute(r);
        
        return Service.START_STICKY;
    }
    
    private void storeDataInDB(Intent intent) {
        DbHandler db = new DbHandler(this);
        db.putPendingTransfer(intent);
        db.close();
    }

    private boolean isOnline() {
        return mConnMngr.getActiveNetworkInfo() != null && mConnMngr.getActiveNetworkInfo().isConnected();
    }
    
    @Override
    public void TransferCompleted(ContentValues values) {
        int type = values.getAsInteger("TYPE");
        Account account = AccountUtils.getAccountByName(values.getAsString("ACCOUNT"), this);
        Intent intent = new Intent(TRANSFER_COMPLETED);
        switch (type) {
            case TYPE_MKDIR:
            case TYPE_UPLOAD_FILE:
            case TYPE_DOWNLOAD_FILE:
            {
                boolean result = values.getAsBoolean("RESULT");
                intent.putExtra("TYPE", TYPE_DOWNLOAD_FILE);
                intent.putExtra("RESULT", result);
                if (result)
                    intent.putExtra("PATH", values.getAsString("PATH"));
                break;
            }
            case TYPE_REMOVE:
            {
                boolean result = values.getAsBoolean("RESULT");
                String path = values.getAsString("PATH");
                intent.putExtra("TYPE", TYPE_REMOVE);
                if (result) {
                    FileDataStorageManager dataMngr = new FileDataStorageManager(account, getContentResolver());
                    OCFile file = dataMngr.getFileByPath(path);
                    dataMngr.removeFile(file);
                    intent.putExtra("RESULT", true);
                } else {
                    intent.putExtra("RESULT", false);
                }
                break;
            }
            default:
                intent = null;
        }
        if (intent != null)
            sendBroadcast(intent);
        
    }
    
    private class EquipmentConnectedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final NetworkInfo netInfo = mConnMngr.getActiveNetworkInfo();
            if (netInfo == null) {
                Log.e("DataTransferService", "Connection lost");
                return;
            }
            if (netInfo.isConnected()) {
                DbHandler db = new DbHandler(context);
                Intent i;
                while ((i = db.getNextAvaitingTransfer()) != null) {
                    
                    Account account = AccountUtils.getAccountByName(i.getStringExtra(EXTRA_TRANSFER_ACCOUNT), DataTransferService.this);
                    if (account != null) {
                        i.putExtra(EXTRA_TRANSFER_ACCOUNT, account);
                        onStartCommand(i, 0, 0);
                    } else {
                        Log.e("ASD", "Account " + i.getStringExtra(EXTRA_TRANSFER_ACCOUNT) + " doesnt exists anymore");
                    }
                    db.removePendingTransfer(i.getIntExtra(EXTRA_PENDING_TRANSFER_ID, 0));
                }
                db.close();
            }
        }
        
    }
    
    private class WorkQueue {
        private final int mThreadsNum;
        private final PoolWorker[] mThreads;
        private final LinkedList<Runnable> mQueue;

        public WorkQueue(int threadNumber) {
            mThreadsNum = threadNumber;
            mQueue = new LinkedList<Runnable>();
            mThreads = new PoolWorker[mThreadsNum];
            
            for (int i = 0; i < mThreadsNum; ++i) {
                mThreads[i] = new PoolWorker();
                mThreads[i].start();
            }
        }

        public void execute(Runnable r) {
            synchronized (mQueue) {
                mQueue.addLast(r);
                mQueue.notify();
            }
        }

        private class PoolWorker extends Thread {
            public void run() {
                Runnable r;
                
                while (true) {
                    synchronized(mQueue) {
                        while (mQueue.isEmpty()) {
                            try {
                                mQueue.wait();
                            } catch (InterruptedException e) {}
                        }
                        r = mQueue.removeFirst();
                    }
                    try {
                        r.run();
                    } catch (RuntimeException e) {
                        Log.e("", "");
                    }
                }
            }
        }
    }
}
