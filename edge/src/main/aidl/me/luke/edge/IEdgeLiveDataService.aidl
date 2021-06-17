package me.luke.edge;

import me.luke.edge.ParcelableTransporter;
import me.luke.edge.IEdgeLiveDataCallback;

interface IEdgeLiveDataService {

    void setValueRemote(in ParcelableTransporter value);

    void registerCallback(in IEdgeLiveDataCallback callback);

}