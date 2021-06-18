package me.luke.edge;

import me.luke.edge.VersionedParcelable;
import me.luke.edge.IEdgeSyncCallback;

interface IEdgeSyncService {

    void notifyDataChanged(in int dataId, in String instanceId, in VersionedParcelable value);

    void setCallback(in int dataId, in String instanceId, in IEdgeSyncCallback client);

}