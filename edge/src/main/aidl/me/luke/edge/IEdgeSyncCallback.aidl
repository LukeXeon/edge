package me.luke.edge;

import me.luke.edge.ReceivedModifiedData;

interface IEdgeSyncCallback {

    void onReceive(in ReceivedModifiedData value);

}
