package com.zhh.common.exception;

/**
 * Created by zhong.huihao on 2020/8/11.
 */

public class BuzException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String msg;
    private int code = 500;

    public BuzException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public BuzException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public BuzException(String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public BuzException(String msg, int code, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }


}
