package me.luke.edge;

import me.luke.edge.ModifiedData;

interface IEdgeLiveDataCallback {

    void onReceive(in ModifiedData value);

}
