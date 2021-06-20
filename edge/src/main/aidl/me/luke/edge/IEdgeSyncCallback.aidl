package me.luke.edge;

import me.luke.edge.PendingParcelable;

interface IEdgeSyncCallback {

    void onReceive(in PendingParcelable value);

}
