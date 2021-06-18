package me.luke.edge;

import me.luke.edge.EdgeValue;
import me.luke.edge.IEdgeSyncCallback;

interface IEdgeSyncService {

    void notifyDataChanged(in String dataId, in String instanceId, in EdgeValue request);

    void setCallback(in String dataId, in String instanceId, in IEdgeSyncCallback client);

}