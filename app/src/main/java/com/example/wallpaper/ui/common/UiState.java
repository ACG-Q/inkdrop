package com.example.wallpaper.ui.common;

/**
 * 页面状态基类
 * 封装页面的加载状态、数据、错误信息
 */
public class UiState<T> {

    private final T data;
    private final boolean loading;
    private final String error;

    private UiState(T data, boolean loading, String error) {
        this.data = data;
        this.loading = loading;
        this.error = error;
    }

    public static <T> UiState<T> empty() {
        return new UiState<>(null, false, null);
    }

    public static <T> UiState<T> loading() {
        return new UiState<>(null, true, null);
    }

    public static <T> UiState<T> loading(T data) {
        return new UiState<>(data, true, null);
    }

    public static <T> UiState<T> success(T data) {
        return new UiState<>(data, false, null);
    }

    public static <T> UiState<T> error(String error) {
        return new UiState<>(null, false, error);
    }

    public static <T> UiState<T> error(String error, T data) {
        return new UiState<>(data, false, error);
    }

    public boolean isEmpty() { return data == null && !loading && error == null; }
    public boolean isLoading() { return loading; }
    public boolean isSuccess() { return !loading && error == null; }
    public boolean isError() { return error != null; }

    public UiState<T> withLoading() { return new UiState<>(data, true, null); }
    public UiState<T> withSuccess(T newData) { return new UiState<>(newData, false, null); }
    public UiState<T> withError(String newError) { return new UiState<>(data, false, newError); }

    public T getData() { return data; }
    public String getError() { return error; }
}
