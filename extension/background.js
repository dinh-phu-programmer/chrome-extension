chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  let nativeMsg;
  if (message.action === 'openFolder') {
    nativeMsg = { action: 'open', path: message.path };
  } else if (message.action === 'browse') {
    nativeMsg = { action: 'browse' };
  } else {
    return;
  }

  chrome.runtime.sendNativeMessage(
    'com.example.open_folder',
    nativeMsg,
    (response) => {
      if (chrome.runtime.lastError) {
        sendResponse({ success: false, message: chrome.runtime.lastError.message });
      } else {
        sendResponse(response);
      }
    }
  );
  return true; // keep channel open for async response
});
