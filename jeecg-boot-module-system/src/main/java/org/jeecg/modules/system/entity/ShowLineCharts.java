package org.jeecg.modules.system.entity;

import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.Serializable;

@Data
public class ShowLineCharts implements Serializable {
    private static final long serialVersionUID = 1L;
    private String cityName;
    private Integer count;
    private Integer man;
    private Integer wo;
}
