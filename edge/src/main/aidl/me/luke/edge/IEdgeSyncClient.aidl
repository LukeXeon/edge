package me.luke.edge;

import me.luke.edge.EdgeValue;

interface IEdgeSyncClient {

    void onDataChanged(in EdgeValue value);

    void onNewClientConnected(in EdgeValue value);
}
