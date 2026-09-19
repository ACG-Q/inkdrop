package com.example.wallpaper.ui.common;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一次性事件包装类
 * 用于 Toast、导航等只应触发一次的事件
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, t -> {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }

    @Override
    public void setValue(T value) {
        pending.set(true);
        super.setValue(value);
    }

    @Override
    public void postValue(T value) {
        pending.set(true);
        super.postValue(value);
    }
}
