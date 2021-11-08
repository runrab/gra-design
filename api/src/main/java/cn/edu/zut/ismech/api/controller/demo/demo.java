package cn.edu.zut.ismech.api.controller.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin //跨域
@RequestMapping("/v")
public class demo {
    @GetMapping("demo")
    public String getDemo(){
        return "demoV1";
    }
}
