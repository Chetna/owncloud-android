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

import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.files.services.DataTransferService;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

public class RemoveTransfer extends TransferHandler {
    
    public RemoveTransfer(Context context, Account account, String path) {
        super(context, account, path);
    }

    @Override
    public void run() {
        setUrlAndCredentials();
        FileDataStorageManager m = new FileDataStorageManager(getAccount(), getContext().getContentResolver());
        String[] splitted_filepath = getPath().split("/");
        String path = "";
        for (String s : splitted_filepath) {
            if (s.equals("")) continue;
            path += "/" + URLEncoder.encode(s).replace("+", "%20");
        }
        Log.e("ASD", getOcURL()+path+"");
        DeleteMethod delete = new DeleteMethod(getOcURL()+path);
        int status;
        try {
            status = getClient().executeMethod(delete);
            Log.e("RemoveTransfer", "status returned " + status);
            if (getListener() != null) {
                ContentValues cv = new ContentValues();
                cv.put("ACCOUNT", getAccount().name);
                cv.put("PATH", getPath());
                cv.put("RESULT", status == HttpStatus.SC_OK ||
                                 status == HttpStatus.SC_ACCEPTED ||
                                 status == HttpStatus.SC_NO_CONTENT);
                cv.put("TYPE", DataTransferService.TYPE_REMOVE);

                Log.e("ASD", "informing listener");
                getListener().TransferCompleted(cv);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
