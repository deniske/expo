package expo.modules.notifications.notifications.service;

import expo.modules.notifications.ExpoNotificationsReconstructor;

public class ExpoNotificationSchedulerService extends NotificationSchedulerService {

  @Override
  protected NotificationsHelper createNotificationHelper() {
    return new NotificationsHelper(this, new ExpoNotificationsReconstructor());
  }

}
