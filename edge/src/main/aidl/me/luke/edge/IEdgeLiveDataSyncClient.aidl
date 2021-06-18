package me.luke.edge;

import me.luke.edge.EdgeLiveDataPendingValue;

interface IEdgeLiveDataSyncClient {

    void onRemoteChanged(in EdgeLiveDataPendingValue value);

}
