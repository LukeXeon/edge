package me.luke.edge;

import me.luke.edge.EdgeValue;
import me.luke.edge.IEdgeSyncClient;

interface IEdgeSyncService {

    void notifyDataChanged(in String dataId, in String instanceId, in EdgeValue value);

    void onClientConnected(in String dataId, in String instanceId, in EdgeValue value, in IEdgeSyncClient client);

}