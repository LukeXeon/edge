package me.luke.edge;

import me.luke.edge.EdgeValue;

interface IEdgeSyncCallback {

    void onReceive(in EdgeValue value, in boolean fromNew);

}
