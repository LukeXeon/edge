package me.luke.edge;

import me.luke.edge.ReceivedModifiedData;

interface IEdgeLiveDataCallback {

    void onReceive(in ReceivedModifiedData value);

}
