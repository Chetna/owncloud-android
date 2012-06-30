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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.net.Uri;
import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.utils.OwnCloudVersion;
import eu.alefzero.webdav.WebdavClient;

public class TransferHandler {

    private Account mAccount;
    private String mPath;
    private Context mContext;
    private AccountManager mAccountMngr;
    private String mOcURL;
    private WebdavClient mWebdavClient;
    
    public TransferHandler(Context context, Account account, String path) {
        mContext = context;
        mAccount = account;
        mPath = path;
        mAccountMngr = AccountManager.get(mContext);
    }
    
    protected Account getAccount() { return mAccount; }
    protected String getPath() { return mPath; }
    protected Context getContext() { return mContext; }
    protected AccountManager getAccountMngr() { return mAccountMngr; }
    protected String getOcURL() { return mOcURL; }
    protected WebdavClient getClient() { return mWebdavClient; }
    
    protected void setUrlAndCredentials() {
        String baseUrl = mAccountMngr.getUserData(mAccount, AccountAuthenticator.KEY_OC_BASE_URL);
        String versionStr = mAccountMngr.getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION);
        OwnCloudVersion version = new OwnCloudVersion(versionStr);
        String webdav_path = AccountUtils.getWebdavPath(version);

        mOcURL = baseUrl + webdav_path;
        String username = mAccount.name;
        username = username.substring(0, mAccount.name.indexOf('@'));

        mWebdavClient = new WebdavClient(Uri.parse(baseUrl + webdav_path));
        mWebdavClient.setCredentials(username, mAccountMngr.getPassword(mAccount));
        mWebdavClient.allowSelfsignedCertificates();
    }

}
