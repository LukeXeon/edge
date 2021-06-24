package me.luke.edge;

import me.luke.edge.ModifiedData;
import me.luke.edge.IEdgeLiveDataCallback;
import android.os.ParcelUuid;

interface IEdgeSyncService {

    void notifyDataChanged(in int dataId, in ModifiedData value);

    void setLiveDataCallback(in int dataId, in ModifiedData value, in IEdgeLiveDataCallback callback);
}