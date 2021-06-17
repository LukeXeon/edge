package me.luke.edge;

import me.luke.edge.PendingParcelable;
import me.luke.edge.IEdgeLiveDataCallback;

interface IEdgeLiveDataService {

    void setValue(in PendingParcelable value);

    void syncValue(in PendingParcelable value, in IEdgeLiveDataCallback callback);

}