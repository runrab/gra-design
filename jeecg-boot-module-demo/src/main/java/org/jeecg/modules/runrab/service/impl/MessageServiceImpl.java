package org.jeecg.modules.runrab.service.impl;

import org.jeecg.modules.runrab.entity.Message;
import org.jeecg.modules.runrab.mapper.MessageMapper;
import org.jeecg.modules.runrab.service.IMessageService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 留言信息
 * @Author runrab
 * @Date:   2022-04-02
 * @Version: V1.0
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

}
