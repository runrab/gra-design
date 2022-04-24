package org.jeecg.modules.system.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ShowLineCharts implements Serializable {
    private static final long serialVersionUID = 1L;
    private String cityName;
    private Integer count;
    private Integer man;
    private Integer wo;
}
