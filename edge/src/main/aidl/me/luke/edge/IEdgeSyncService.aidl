package me.luke.edge;

import me.luke.edge.ModifiedData;
import me.luke.edge.IEdgeSyncCallback;
import android.os.ParcelUuid;

interface IEdgeSyncService {

    void notifyDataChanged(in int dataId, in ParcelUuid instanceId, in ModifiedData value);

    ParcelUuid setCallback(in int dataId, in ModifiedData value, in IEdgeSyncCallback callback);
}