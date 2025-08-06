const transformProgressResponseUtils = require('./transformProgressResponseUtils.js');

exports.createProgressResponseV28 = async (responseBody) => {
    console.debug("createProgressResponseV28")

    if (responseBody.newStatus === 'DELIVERY_TIMEOUT') {
        const notification = await getNotificationFromDelivery(responseBody)
        const statusHistory = await getStatusHistory(notification)
        responseBody.newStatus = statusHistory.data.notificationStatusHistory[statusHistory.data.notificationStatusHistory.length-2].status
    }

    return responseBody;
}

async function getNotificationFromDelivery(responseBody) {
    return transformProgressResponseUtils.getNotificationFromDelivery(responseBody);
}

async function getStatusHistory(notification) {
    return transformProgressResponseUtils.getStatusHistory(notification);
}