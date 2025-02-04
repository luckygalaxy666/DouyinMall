package com.hmall.common.config;

import com.hmall.common.utils.UserContext;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@ConditionalOnClass(RabbitTemplate.class)
@Configuration
public class MQConfig {
    @Bean
    public MessageConverter messageConverter(){
        // 1.定义消息转换器
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        // 2.配置自动创建消息id，用于识别不同消息，也可以在业务中基于ID判断是否是重复消息
        jackson2JsonMessageConverter.setCreateMessageIds(true);
        return new AutoMessageConverter(jackson2JsonMessageConverter);
    }

    // 利用消息转换器在消息转换前后添加和解析用户ID

    public static class AutoMessageConverter implements MessageConverter {
        private final MessageConverter messageConverter;

        public AutoMessageConverter(MessageConverter messageConverter) {
            this.messageConverter = messageConverter;
        }

        @Override
        public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {

            // 1.设置用户信息
            Long userId = UserContext.getUser();
            if(userId != null){
                messageProperties.setHeader("user-info", userId);
            }

            return messageConverter.toMessage(object, messageProperties);
        }

        @Override
        public Object fromMessage(Message message) throws MessageConversionException {
            Object userInfo = message.getMessageProperties().getHeader("user-info");
            if(userInfo != null){
                // 1.设置用户信息
                UserContext.setUser(Long.parseLong(userInfo.toString()));
            }
            return messageConverter.fromMessage(message);
        }
    }
}
