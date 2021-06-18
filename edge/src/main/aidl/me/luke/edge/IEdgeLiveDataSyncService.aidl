package me.luke.edge;

import me.luke.edge.EdgeLiveDataPendingValue;
import me.luke.edge.IEdgeLiveDataSyncClient;

interface IEdgeLiveDataSyncService {

    void notifyDataChanged(in String dataId, in String instanceId, in EdgeLiveDataPendingValue value);

    void onClientConnected(in String dataId, in String instanceId, in EdgeLiveDataPendingValue value, in IEdgeLiveDataSyncClient client);

}