package com.seememes.vkbotexperiment.app;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.events.longpoll.GroupLongPollApi;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.callback.MessageNew;
import com.vk.api.sdk.objects.callback.MessageObject;
import com.vk.api.sdk.objects.callback.Type;
import com.vk.api.sdk.objects.callback.messages.CallbackMessage;
import com.vk.api.sdk.objects.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VkCommunicationService {
    private static final Logger log = LoggerFactory.getLogger(VkCommunicationService.class);
    private final TransportClient transportClient;
    private final VkApiClient client;
    private final GroupActor actor;
    private final LongPollHandler handler;


    public VkCommunicationService(
            @Value("${vk.apikey}") String vkApiKey,
            @Value("${vk.groupId}") Long groupId
    ) throws ClientException, ApiException {
        transportClient = new HttpTransportClient();
        client = new VkApiClient(transportClient);

        actor = new GroupActor(groupId, vkApiKey);
        client.groups().setLongPollSettings(actor)
                .enabled(true)
                .messageNew(true)
                .execute();

        handler = new LongPollHandler(client, actor, 1000);
        handler.run();
    }

    private static class LongPollHandler extends GroupLongPollApi {
        private final VkApiClient client;
        private final GroupActor actor;

        protected LongPollHandler(VkApiClient client, GroupActor actor, int waitTime) {
            super(client, actor, waitTime);
            this.client = client;
            this.actor = actor;
        }

        @Override
        public void messageNew(Integer groupId, MessageNew message) {
            log.info(message.toPrettyString());

            Message messageInsides = message.getObject().getMessage();
            Long userSent = messageInsides.getFromId();
            String messageText = messageInsides.getText();

            try {
                client
                        .messages()
                        .sendDeprecated(actor)
                        .message("Вы сказали: " + messageText)
                        .userId(userSent)
                        .randomId(0)
                        .execute();
            } catch (ApiException | ClientException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String parse(CallbackMessage message) {
            try {
                return super.parse(message);
            } catch (NullPointerException e) {
                return null;
            }
        }
    }
}
