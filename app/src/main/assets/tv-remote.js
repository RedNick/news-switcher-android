// TV remote support, injected by the Android app into the switcher page
// after it loads (see MainActivity.kt).
//
// The web page already understands keyboard keys (left/right change channel,
// H shows/hides the extras, M opens the menu, Escape closes it). A TV remote
// has no Tab key, though, so this adds a way to move between the page's
// buttons and tick boxes with up/down and press them with OK.
//
// It only uses the page's public bits (keyboard shortcuts and the
// #unmute-button), so it keeps working as the web page changes.

(function () {
  // The app injects this on every page load. Only set it up once.
  if (window.newsSwitcherRemote) {
    return;
  }

  // Make the selected control obvious from across the room.
  const focusStyle = document.createElement("style");
  focusStyle.textContent =
    ":focus { outline: 4px solid #ffcc00 !important; outline-offset: 2px; }";
  document.head.appendChild(focusStyle);

  function isShowing(element) {
    if (element.hidden || element.disabled) {
      return false;
    }
    // getClientRects is empty when the element or a parent is display:none.
    if (element.getClientRects().length === 0) {
      return false;
    }
    return getComputedStyle(element).visibility !== "hidden";
  }

  function getFocusableControls() {
    const allControls = document.querySelectorAll(
      'button, input, select, a[href], [tabindex]:not([tabindex="-1"])'
    );
    return Array.from(allControls).filter(isShowing);
  }

  // Sends a key to the page as if it was typed on a keyboard.
  function pressKey(keyName) {
    document.body.dispatchEvent(
      new KeyboardEvent("keydown", { key: keyName, bubbles: true })
    );
  }

  // Moves the highlight to the next (+1) or previous (-1) control on screen.
  function moveFocus(direction) {
    const controls = getFocusableControls();
    if (controls.length === 0) {
      return;
    }
    const currentPosition = controls.indexOf(document.activeElement);
    let nextPosition;
    if (currentPosition === -1) {
      nextPosition = direction > 0 ? 0 : controls.length - 1;
    } else {
      nextPosition = (currentPosition + direction + controls.length) % controls.length;
    }
    controls[nextPosition].focus();
  }

  function getUnmuteButton() {
    const unmuteButton = document.getElementById("unmute-button");
    return unmuteButton && isShowing(unmuteButton) ? unmuteButton : null;
  }

  // The OK button: turns the sound on if it's still off, otherwise presses
  // whatever is highlighted. With nothing highlighted it shows/hides the
  // extras, like the H key.
  function pressSelect() {
    const unmuteButton = getUnmuteButton();
    if (unmuteButton) {
      unmuteButton.click();
      return;
    }
    const highlightedControl = document.activeElement;
    if (highlightedControl && highlightedControl !== document.body && isShowing(highlightedControl)) {
      highlightedControl.click();
      return;
    }
    pressKey("h");
  }

  // Used when the app starts, so the viewer doesn't have to press anything
  // to hear the news.
  function turnSoundOn() {
    const unmuteButton = getUnmuteButton();
    if (unmuteButton) {
      unmuteButton.click();
    }
  }

  window.newsSwitcherRemote = { pressKey, moveFocus, pressSelect, turnSoundOn };
})();
