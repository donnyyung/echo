/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.echo.slack

import com.netflix.spinnaker.echo.api.Notification
import com.netflix.spinnaker.echo.controller.EchoResponse
import com.netflix.spinnaker.echo.notification.NotificationService
import com.netflix.spinnaker.echo.notification.NotificationTemplateEngine
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Slf4j
@Component
@ConditionalOnProperty('slack.enabled')
class SlackNotificationService implements NotificationService {
  private static Notification.Type TYPE = Notification.Type.SLACK

  @Autowired
  SlackService slack

  @Value('${slack.token:}')
  String token

  @Value('${slack.send-compact-messages:false}')
  Boolean sendCompactMessages

  @Autowired
  NotificationTemplateEngine notificationTemplateEngine

  @Override
  boolean supportsType(Notification.Type type) {
    return type == TYPE
  }

  @Override
  EchoResponse.Void handle(Notification notification) {
    def text = notificationTemplateEngine.build(notification, NotificationTemplateEngine.Type.BODY)
    notification.to.each {
      def response
      String address = it.startsWith('#') ? it : "#${it}"
      if (sendCompactMessages) {
        response = slack.sendCompactMessage(token, new CompactSlackMessage(text), address, true)
      } else {
        response = slack.sendMessage(token, new SlackAttachment("Spinnaker Notification", text), address, true)
      }
      log.trace("Received response from Slack: {} {} for message '{}'. {}",
        response?.status, response?.reason, text, response?.body)
    }

    new EchoResponse.Void()
  }
}
