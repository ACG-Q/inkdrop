package io.inkdrop.wallpaper.data.source;

/**
 * 壁纸源操作异常类
 * 统一处理各种源操作错误
 */
public class SourceException extends Exception {
    
    /**
     * 错误类型枚举
     */
    public enum Type {
        /** 网络连接错误 */
        NETWORK,
        /** JSON 解析错误 */
        PARSE,
        /** 请求超时 */
        TIMEOUT,
        /** JS 执行错误 */
        JS_EXECUTION,
        /** 配置无效 */
        INVALID_CONFIG,
        /** 源不可用 */
        SOURCE_UNAVAILABLE,
        /** 未知错误 */
        UNKNOWN
    }
    
    private final Type type;
    
    public SourceException(Type type, String message) {
        super(message);
        this.type = type;
    }
    
    public SourceException(Type type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }
    
    /**
     * 获取错误类型
     *
     * @return 错误类型
     */
    public Type getType() {
        return type;
    }
    
    /**
     * 根据异常创建 SourceException
     *
     * @param e 原始异常
     * @return SourceException
     */
    public static SourceException fromException(Exception e) {
        if (e instanceof SourceException) {
            return (SourceException) e;
        }
        
        String message = e.getMessage();
        if (message == null) {
            message = e.getClass().getSimpleName();
        }
        
        // 根据异常类型判断错误类型
        String className = e.getClass().getSimpleName();
        Type type;
        
        if (className.contains("Timeout") || className.contains("timeout")) {
            type = Type.TIMEOUT;
        } else if (className.contains("JSON") || className.contains("Parse")) {
            type = Type.PARSE;
        } else if (className.contains("IO") || className.contains("Connect")) {
            type = Type.NETWORK;
        } else if (className.contains("JsException")) {
            type = Type.JS_EXECUTION;
        } else {
            type = Type.UNKNOWN;
        }
        
        return new SourceException(type, message, e);
    }
    
    /**
     * 获取用户友好的错误消息
     *
     * @return 错误消息
     */
    public String getUserMessage() {
        switch (type) {
            case NETWORK:
                return "网络连接失败，请检查网络设置";
            case PARSE:
                return "数据解析失败";
            case TIMEOUT:
                return "请求超时，请稍后重试";
            case JS_EXECUTION:
                return "脚本执行错误: " + getMessage();
            case INVALID_CONFIG:
                return "配置无效: " + getMessage();
            case SOURCE_UNAVAILABLE:
                return "壁纸源不可用";
            default:
                return "操作失败: " + getMessage();
        }
    }
}
