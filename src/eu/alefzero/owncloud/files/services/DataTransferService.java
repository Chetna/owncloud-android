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

import eu.alefzero.owncloud.files.services.transer_handlers.DownloadTransfer;
import eu.alefzero.owncloud.files.services.transer_handlers.MkDirTransfer;
import eu.alefzero.owncloud.files.services.transer_handlers.RemoveTransfer;

import android.accounts.Account;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.util.Log;

/*
 * DataTransferService is made to unify transfer between user equipment
 * and ownCloud instance. Its duty is to handle all network traffic, failures
 * and postpone network actions when connection is unavailable.
 */
public class DataTransferService extends Service {

    public final static String EXTRA_TRANSFER_TYPE = "EXTRA_TRANSFER_TYPE";
    public final static String EXTRA_TRANSFER_DATA1 = "EXTRA_TRANSFER_DATA1";
    public final static String EXTRA_TRANSFER_DATA2 = "EXTRA_TRANSFER_DATA2";
    public final static String EXTRA_TRANSFER_DATA3 = "EXTRA_TRANSFER_DATA3";

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
    
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mWorkQueue = new WorkQueue(5);
        mConnMngr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return Service.START_STICKY;

        Runnable r;

        switch (intent.getIntExtra(EXTRA_TRANSFER_TYPE, TYPE_UNKNOWN)) {
            case TYPE_SYNCDIR:
            {
            }
            case TYPE_MKDIR:
            {
                Account account = intent.getParcelableExtra(EXTRA_TRANSFER_DATA1);
                String path = intent.getStringExtra(EXTRA_TRANSFER_DATA2);
                r = new MkDirTransfer(this, account, path);
                break;
            }
            case TYPE_UPLOAD_FILE:
            {
            }
            case TYPE_DOWNLOAD_FILE:
            {
                Account account = intent.getParcelableExtra(EXTRA_TRANSFER_DATA1);
                String path = intent.getStringExtra(EXTRA_TRANSFER_DATA2);
                r = new DownloadTransfer(this, account, path);
                break;
            }
            case TYPE_REMOVE:
            {
                Account account = intent.getParcelableExtra(EXTRA_TRANSFER_DATA1);
                String path = intent.getStringExtra(EXTRA_TRANSFER_DATA2);
                r = new RemoveTransfer(this, account, path);
                break;
            }
            case TYPE_UNKNOWN:
            default:
                Log.e("ASD","ASD");
                return Service.START_STICKY;
        }
        
        mWorkQueue.execute(r);
        
        return Service.START_STICKY;
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
