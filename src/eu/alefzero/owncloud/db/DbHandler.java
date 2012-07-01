/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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
package eu.alefzero.owncloud.db;

import java.util.Vector;

import eu.alefzero.owncloud.OwnCloudSession;
import eu.alefzero.owncloud.files.services.DataTransferService;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Custom database helper for ownCloud
 * 
 * @author Bartek Przybylski
 * 
 */
public class DbHandler {
    private SQLiteDatabase mDB;
    private OpenerHepler mHelper;
    private final String mDatabaseName = "ownCloud";
    private final String TABLE_SESSIONS = "sessions";
    private final int mDatabaseVersion = 2;
    
    private final String TABLE_INSTANT_UPLOAD = "instant_upload";
    private final String TABLE_POSTPONED_TRANSFER = "postponed_transfer";

    public DbHandler(Context context) {
        mHelper = new OpenerHepler(context);
        mDB = mHelper.getWritableDatabase();
    }

    public void close() {
        mDB.close();
    }

    public boolean putFileForLater(String filepath, String account) {
        ContentValues cv = new ContentValues();
        cv.put("path", filepath);
        cv.put("account", account);
        return mDB.insert(TABLE_INSTANT_UPLOAD, null, cv) != -1;
    }
    
    public Cursor getAwaitingFiles() {
        return mDB.query(TABLE_INSTANT_UPLOAD, null, null, null, null, null, null);
    }
    
    public void clearFiles() {
        mDB.delete(TABLE_INSTANT_UPLOAD, null, null);
    }
    
    public void putPendingTransfer(Intent intent) {
        ContentValues cv = new ContentValues();
        
        cv.put("account", ((Account)intent.getParcelableExtra(DataTransferService.EXTRA_TRANSFER_ACCOUNT)).name);
        cv.put("transfer_action", intent.getIntExtra(DataTransferService.EXTRA_TRANSFER_TYPE, DataTransferService.TYPE_UNKNOWN));
        cv.put("transfer_data1", intent.getStringExtra(DataTransferService.EXTRA_TRANSFER_DATA1));
        cv.put("transfer_data2", intent.getStringExtra(DataTransferService.EXTRA_TRANSFER_DATA2));
        cv.put("transfer_data3", intent.getStringExtra(DataTransferService.EXTRA_TRANSFER_DATA3));
        
        mDB.insert(TABLE_POSTPONED_TRANSFER, null, cv);
    }
    
    public Intent getNextAvaitingTransfer() {
        Cursor c = mDB.query(TABLE_POSTPONED_TRANSFER, null, null, null, null, null, null);
        Intent ret = null;
        if (c.moveToFirst()) {
            ret = new Intent();
            ret.putExtra(DataTransferService.EXTRA_PENDING_TRANSFER_ID, c.getInt(c.getColumnIndex("_id")));
            ret.putExtra(DataTransferService.EXTRA_TRANSFER_ACCOUNT, c.getString(c.getColumnIndex("account")));
            ret.putExtra(DataTransferService.EXTRA_TRANSFER_TYPE, c.getInt(c.getColumnIndex("transfer_action")));
            ret.putExtra(DataTransferService.EXTRA_TRANSFER_DATA1, c.getString(c.getColumnIndex("transfer_data1")));
            ret.putExtra(DataTransferService.EXTRA_TRANSFER_DATA2, c.getString(c.getColumnIndex("transfer_data2")));
            ret.putExtra(DataTransferService.EXTRA_TRANSFER_DATA3, c.getString(c.getColumnIndex("transfer_data3")));
        }
        c.close();
        return ret;
    }
    
    public void removePendingTransfer(int id) {
        mDB.delete(TABLE_POSTPONED_TRANSFER, "_id = ?", new String[]{String.valueOf(id)});
    }
    
    private class OpenerHepler extends SQLiteOpenHelper {
        public OpenerHepler(Context context) {
            super(context, mDatabaseName, null, mDatabaseVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_INSTANT_UPLOAD + " ("
            		+ " _id INTEGER PRIMARY KEY, "
            		+ " path TEXT,"
            		+ " account TEXT);");
            db.execSQL("CREATE TABLE " + TABLE_POSTPONED_TRANSFER + " ("
                    + " _id INTEGER PRIMARY KEY,"
                    + " account TEXT,"
                    + " transfer_action INTEGER,"
                    + " transfer_data1 TEXT,"
                    + " transfer_data2 TEXT,"
                    + " transfer_data3 TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (newVersion == 2) {
                db.execSQL("CREATE TABLE " + TABLE_POSTPONED_TRANSFER + " ("
                        + " _id INTEGER PRIMARY KEY,"
                        + " account TEXT,"
                        + " transfer_action INTEGER,"
                        + " transfer_data1 TEXT,"
                        + " transfer_data2 TEXT,"
                        + " transfer_data3 TEXT);");
            }
        }
    }
}
