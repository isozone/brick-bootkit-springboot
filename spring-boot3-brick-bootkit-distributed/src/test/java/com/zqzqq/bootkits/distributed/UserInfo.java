package com.zqzqq.bootkits.distributed;

/**
 * 测试用 DTO，验证自定义对象的序列化往返。
 */
public class UserInfo {

    private Long id;
    private String name;

    public UserInfo() {
    }

    public UserInfo(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}