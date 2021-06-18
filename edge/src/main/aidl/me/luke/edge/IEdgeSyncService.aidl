package me.luke.edge;

import me.luke.edge.VersionedParcelable;
import me.luke.edge.IEdgeSyncCallback;

interface IEdgeSyncService {

    void notifyDataChanged(in String dataId, in String instanceId, in VersionedParcelable value);

    void setCallback(in String dataId, in String instanceId, in IEdgeSyncCallback client);

}