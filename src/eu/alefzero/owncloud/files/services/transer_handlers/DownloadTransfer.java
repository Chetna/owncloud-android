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

import android.accounts.Account;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class DownloadTransfer extends TransferHandler implements Runnable {

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
        getClient().downloadFile(getPath(), file);
    }
}
