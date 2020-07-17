package versioned.host.exp.exponent.modules.universal.notifications;

import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;

import org.unimodules.core.Promise;

import java.util.ArrayList;
import java.util.Collection;

import expo.modules.notifications.ScopedNotificationRequest;
import expo.modules.notifications.interfaces.NotificationContent;
import expo.modules.notifications.interfaces.NotificationRequest;
import expo.modules.notifications.interfaces.NotificationTrigger;
import expo.modules.notifications.notifications.NotificationSerializer;
import expo.modules.notifications.notifications.model.Notification;
import expo.modules.notifications.notifications.presentation.ExpoNotificationPresentationModule;
import expo.modules.notifications.notifications.service.NotificationsHelper;
import host.exp.exponent.kernel.ExperienceId;
import host.exp.exponent.notifications.ScopedNotificationsUtils;

public class ScopedExpoNotificationPresentationModule extends ExpoNotificationPresentationModule {
  private final ExperienceId mExperienceId;
  private final ScopedNotificationsUtils mScopedNotificationsUtils;

  public ScopedExpoNotificationPresentationModule(Context context, ExperienceId experienceId) {
    super(context);
    mExperienceId = experienceId;
    mScopedNotificationsUtils = new ScopedNotificationsUtils(context);
  }

  @Override
  protected NotificationRequest createNotificationRequest(String identifier, NotificationContent content, NotificationTrigger trigger) {
    String experienceIdString = mExperienceId == null ? null : mExperienceId.get();
    return new ScopedNotificationRequest(identifier, content, trigger, experienceIdString);
  }

  @Override
  protected ArrayList<Bundle> serializeNotifications(Collection<Notification> notifications) {
    ArrayList<Bundle> serializedNotifications = new ArrayList<>();
    for (Notification notification : notifications) {
      if (mScopedNotificationsUtils.shouldHandleNotification(notification, mExperienceId)) {
        serializedNotifications.add(NotificationSerializer.toBundle(notification));
      }
    }

    return serializedNotifications;
  }

  @Override
  public void dismissNotificationAsync(String identifier, Promise promise) {
    getNotificationsHelper().getAllPresented(new ResultReceiver(null) {
      @Override
      protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        Collection<Notification> notifications = resultData.getParcelableArrayList(NotificationsHelper.NOTIFICATIONS_KEY);
        if (resultCode == NotificationsHelper.SUCCESS_CODE && notifications != null) {
          Notification notification = findNotification(notifications, identifier);
          if (notification == null || !mScopedNotificationsUtils.shouldHandleNotification(notification, mExperienceId)) {
            promise.resolve(null);
            return;
          }

          doDismissNotificationAsync(identifier, promise);
        } else {
          Exception e = resultData.getParcelable(NotificationsHelper.EXCEPTION_KEY);
          promise.reject("ERR_NOTIFICATIONS_FETCH_FAILED", "A list of displayed notifications could not be fetched.", e);
        }
      }
    });
  }

  @Override
  public void dismissAllNotificationsAsync(Promise promise) {
    getNotificationsHelper().getAllPresented(new ResultReceiver(null) {
      @Override
      protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        Collection<Notification> notifications = resultData.getParcelableArrayList(NotificationsHelper.NOTIFICATIONS_KEY);
        if (resultCode == NotificationsHelper.SUCCESS_CODE && notifications != null) {
          ArrayList<String> toDismiss = new ArrayList<>();
          for (Notification notification : notifications) {
            if (mScopedNotificationsUtils.shouldHandleNotification(notification, mExperienceId)) {
              toDismiss.add(notification.getNotificationRequest().getIdentifier());
            }
          }
          dismissSelectedAsync(toDismiss.toArray(new String[0]), promise);
        } else {
          Exception e = resultData.getParcelable(NotificationsHelper.EXCEPTION_KEY);
          promise.reject("ERR_NOTIFICATIONS_FETCH_FAILED", "A list of displayed notifications could not be fetched.", e);
        }
      }
    });
  }

  private void doDismissNotificationAsync(String identifier, final Promise promise) {
    super.dismissNotificationAsync(identifier, promise);
  }

  private void dismissSelectedAsync(String[] identifiers, final Promise promise) {
    getNotificationsHelper().enqueueDismissSelected(identifiers, new ResultReceiver(null) {
      @Override
      protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        if (resultCode == NotificationsHelper.SUCCESS_CODE) {
          promise.resolve(null);
        } else {
          Exception e = resultData.getParcelable(NotificationsHelper.EXCEPTION_KEY);
          promise.reject("ERR_NOTIFICATIONS_DISMISSAL_FAILED", "Notifications could not be dismissed.", e);
        }
      }
    });
  }

  private Notification findNotification(Collection<Notification> notifications, String identifier) {
    for (Notification notification : notifications) {
      if (notification.getNotificationRequest().getIdentifier().equals(identifier)) {
        return notification;
      }
    }
    return null;
  }

  @FunctionalInterface
  private interface DismissNotificationFunction {
    void invoke(String identifier, final Promise promise);
  }
}
