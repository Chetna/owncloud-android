package eu.alefzero.owncloud.files.services.transer_handlers;

import android.content.ContentValues;

public interface OnTransferCompletedListener {
    public void TransferCompleted(ContentValues values);
}
