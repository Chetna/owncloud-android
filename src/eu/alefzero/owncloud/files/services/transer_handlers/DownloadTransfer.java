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
package eu.alefzero.owncloud.files.services.transer_handlers;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.files.services.DataTransferService;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class DownloadTransfer extends TransferHandler {

    public DownloadTransfer(Context context, Account account, String path) {
        super(context, account, path);
    }

    @Override
    public void run() {
        setUrlAndCredentials();
        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File(sdCard.getAbsolutePath() + "/owncloud/" + getAccount().name + getPath());
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] splitted_filepath = getPath().split("/");
        String path = "";
        for (String s : splitted_filepath) {
            if (s.equals("")) continue;
            path += "/" + URLEncoder.encode(s).replace("+", "%20");
        }
        Log.e("ASD", path);
        boolean result = getClient().downloadFile(path, file);
        Log.e("SAD", "downloadin done " +result);
        if (getListener() != null) {
            ContentValues cv = new ContentValues();
            cv.put("TYPE", DataTransferService.TYPE_DOWNLOAD_FILE);
            cv.put("RESULT", result);
            cv.put("ACCOUNT", getAccount().name);
            if (result)
                cv.put("PATH", file.getAbsolutePath());
            getListener().TransferCompleted(cv);
        }
    }
}
