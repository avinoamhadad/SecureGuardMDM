package com.emanuelef.remote_capture.activities;
public class AppState {
    private static AppState instance = new AppState();
    public static AppState getInstance() { return instance; }
    public PathType getCurrentPath() { return PathType.MULTIMEDIA; }
    public void setCurrentPath(PathType t) {}
}