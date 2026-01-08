exports.createProgressResponseV28 = (responseBody) => {
    console.debug("createProgressResponseV28")

    const element = responseBody.element;

    if (element.category === 'COMPLETELY_UNREACHABLE_CREATION_REQUEST'
        || element.category === 'DIGITAL_DELIVERY_CREATION_REQUEST'
        || element.category === 'NOTIFICATION_VIEWED_CREATION_REQUEST'
        ) {
        delete element.legalFactId;
    }
    return responseBody;
}