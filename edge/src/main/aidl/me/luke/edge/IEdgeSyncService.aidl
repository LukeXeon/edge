package me.luke.edge;

import me.luke.edge.EdgeRequest;
import me.luke.edge.IEdgeSyncClient;

interface IEdgeSyncService {

    void notifyDataChanged(in EdgeRequest request);

    void attachToService(in EdgeRequest request, in IEdgeSyncClient client);

}