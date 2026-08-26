package com.ftcverificador;

public interface ProcessingListener {

    void onStatus(String message);

    void onProgress(int current, int total);
}
