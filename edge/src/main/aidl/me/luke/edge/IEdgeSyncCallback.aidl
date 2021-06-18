package me.luke.edge;

import me.luke.edge.VersionedParcelable;

interface IEdgeSyncCallback {

    void onReceive(in VersionedParcelable value, in boolean fromNew);

}
