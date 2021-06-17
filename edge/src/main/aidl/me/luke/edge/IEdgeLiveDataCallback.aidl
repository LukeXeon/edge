package me.luke.edge;

import me.luke.edge.ParcelableTransporter;

interface IEdgeLiveDataCallback {

    void onRemoteChanged(in ParcelableTransporter value);

}
