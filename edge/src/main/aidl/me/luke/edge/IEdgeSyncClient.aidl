package me.luke.edge;

import me.luke.edge.EdgeValue;

interface IEdgeSyncClient {

    void onRemoteChanged(in EdgeValue value);

    void onNewClientConnected(in EdgeValue value);
}
