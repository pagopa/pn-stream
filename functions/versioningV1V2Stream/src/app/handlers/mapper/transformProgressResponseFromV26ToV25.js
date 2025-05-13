const axios = require("axios");

exports.createProgressResponseV25 = async (responseBody) => {
    console.debug("createProgressResponseV25")

    if (responseBody.newStatus === 'RETURNED_TO_SENDER') {
        const notification = await getNotificationFromDelivery(responseBody)
        const statusHistory = await getStatusHistory(notification)
        responseBody.newStatus = statusHistory.data.notificationStatusHistory[statusHistory.data.notificationStatusHistory.length-2].status
    }

    return responseBody;
}

async function getNotificationFromDelivery(responseBody) {
    const deliveryPrivateUrl = `${process.env.BASE_PATH}/delivery-private/notifications/${responseBody.iun}`;
    const notification = await axios.get(deliveryPrivateUrl, {
      headers: {}
    });

    return notification;
}

async function getStatusHistory(notification) {    
    const deliveryPushPrivateUrl = `${process.env.BASE_PATH}/delivery-push-private/${notification.data.iun}/history`;
    const statusHistory = await axios.get(deliveryPushPrivateUrl, {
      headers: {},
      params: {
        createdAt: notification.data.sentAt,
        numberOfRecipients: notification.data.recipients.length
      }
    });

    return statusHistory;
}