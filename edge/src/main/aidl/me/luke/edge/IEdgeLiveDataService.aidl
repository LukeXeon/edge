package me.luke.edge;

import me.luke.edge.ParcelableTransporter;
import me.luke.edge.IEdgeLiveDataCallback;

interface IEdgeLiveDataService {

    void setValue(in ParcelableTransporter value);

    void syncValue(in ParcelableTransporter value, in IEdgeLiveDataCallback callback);

}