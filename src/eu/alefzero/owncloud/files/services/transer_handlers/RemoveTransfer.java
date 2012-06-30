package eu.alefzero.owncloud.files.services.transer_handlers;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

public class RemoveTransfer extends TransferHandler implements Runnable {

    public RemoveTransfer(Context context, Account account, String path) {
        super(context, account, path);
    }

    @Override
    public void run() {
        setUrlAndCredentials();
        DeleteMethod delete = new DeleteMethod(getOcURL()+getPath());
        int status;
        try {
            status = getClient().executeMethod(delete);
            Log.e("ASD", "status " + status);
        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
