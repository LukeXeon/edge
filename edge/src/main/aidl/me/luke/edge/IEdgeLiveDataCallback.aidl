package me.luke.edge;

import me.luke.edge.PendingParcelable;

interface IEdgeLiveDataCallback {

    void onRemoteChanged(in PendingParcelable value);

}
