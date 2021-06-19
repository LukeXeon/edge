package me.luke.edge;

import me.luke.edge.VersionedParcelable;

interface IEdgeSyncCallback {

    void onReceive(in boolean fromNew, in VersionedParcelable value);

}
