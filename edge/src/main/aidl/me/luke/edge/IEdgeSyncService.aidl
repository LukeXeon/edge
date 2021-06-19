package me.luke.edge;

import me.luke.edge.VersionedParcelable;
import me.luke.edge.IEdgeSyncCallback;
import android.os.ParcelUuid;

interface IEdgeSyncService {

    void notifyDataChanged(in int dataId, in ParcelUuid instanceId, in VersionedParcelable value);

    void setCallback(in int dataId, in ParcelUuid instanceId, in VersionedParcelable value, in IEdgeSyncCallback callback);
}