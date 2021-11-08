package cn.edu.zut.ismech.api.bean;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class User {
    private String name;
    private String password;
    private String token;
    private String uuid;
    //权限  0超级管理员 1管理员 2vip 3 用户 4游客 5其它人群

    private int privacy;

}
