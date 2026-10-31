// Takes the Google Play screenshots of the switcher page.
//
// The real page plays broadcasters' live streams, which can't go in store
// screenshots (the footage belongs to them). So this swaps YouTube's player
// for a stand-in that shows a plain "studio" backdrop and reports "playing",
// and everything else on screen is the real page.
//
// Usage (needs Playwright):
//   node play-store/take_screenshots.js <folder with the news-switcher web page>
//
// Writes PNGs into play-store/graphics/phone-screenshots and tv-screenshots.

const path = require("path");
const http = require("http");
const fs = require("fs");
const { chromium } = require("playwright");

const webPageFolder = process.argv[2];
if (!webPageFolder) {
  console.error("Usage: node take_screenshots.js <news-switcher web folder>");
  process.exit(1);
}

const graphicsFolder = path.join(__dirname, "graphics");
const tvRemoteScriptPath = path.join(__dirname, "..", "app", "src", "main", "assets", "tv-remote.js");

// A time partway through an hour, so the countdown to the next switch reads well.
const SCREENSHOT_TIME = new Date("2026-11-02T18:47:30Z");

// Rendered at 960x540 CSS pixels and 2x density, which is how the page looks
// on a phone held sideways or on a TV, giving 1920x1080 images.
const VIEWPORT = { width: 960, height: 540 };
const PIXEL_DENSITY = 2;

// Stands in for YouTube's IFrame API: no network, no footage.
const FAKE_YOUTUBE_API = `
  window.YT = {
    PlayerState: { PLAYING: 1 },
    Player: class {
      constructor(iframe, options) {
        this.iframe = iframe;
        setTimeout(() => {
          options.events.onReady();
          options.events.onStateChange({ data: 1 });
        }, 50);
      }
      playVideo() {}
      unMute() {}
      mute() {}
      isMuted() { return false; }
      destroy() { this.iframe.remove(); }
    },
  };
  window.onYouTubeIframeAPIReady();
`;

// What the stand-in player shows: a dark studio-style backdrop.
const PLACEHOLDER_PLAYER_PAGE = `<!doctype html><html><body style="margin:0;height:100vh;
  background:
    radial-gradient(ellipse at 30% 35%, rgba(255,255,255,0.18), transparent 45%),
    radial-gradient(ellipse at 75% 65%, rgba(230,57,70,0.35), transparent 50%),
    linear-gradient(160deg, #0d1b2a 0%, #1b263b 45%, #3a0d12 100%);"></body></html>`;

// Serves the web page folder on a local port.
function startLocalServer(folder) {
  const contentTypes = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css" };
  const server = http.createServer((request, response) => {
    const requestedPath = request.url.split("?")[0];
    const filePath = path.join(folder, requestedPath === "/" ? "index.html" : requestedPath);
    if (!filePath.startsWith(path.resolve(folder)) || !fs.existsSync(filePath)) {
      response.writeHead(404);
      response.end();
      return;
    }
    response.writeHead(200, { "Content-Type": contentTypes[path.extname(filePath)] || "text/plain" });
    response.end(fs.readFileSync(filePath));
  });
  return new Promise((resolve) => server.listen(0, () => resolve(server)));
}

async function openSwitcherPage(browser, pageUrl, { asTv }) {
  const context = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: PIXEL_DENSITY });
  const page = await context.newPage();
  await page.clock.install({ time: SCREENSHOT_TIME });
  await page.route("https://www.youtube.com/iframe_api", (route) =>
    route.fulfill({ contentType: "text/javascript", body: FAKE_YOUTUBE_API })
  );
  await page.route("https://www.youtube.com/embed/**", (route) =>
    route.fulfill({ contentType: "text/html", body: PLACEHOLDER_PLAYER_PAGE })
  );
  await page.goto(pageUrl);
  if (asTv) {
    await page.addScriptTag({ path: tvRemoteScriptPath });
  }
  await page.clock.runFor(1500); // Let the stand-in player "start".
  return page;
}

async function main() {
  const server = await startLocalServer(path.resolve(webPageFolder));
  const pageUrl = `http://localhost:${server.address().port}/`;
  const browser = await chromium.launch();
  const phoneFolder = path.join(graphicsFolder, "phone-screenshots");
  const tvFolder = path.join(graphicsFolder, "tv-screenshots");

  // Phone 1: playing, with the channel info and countdown showing.
  let page = await openSwitcherPage(browser, pageUrl, { asTv: false });
  await page.screenshot({ path: path.join(phoneFolder, "1-now-playing.png") });

  // Phone 2: the settings menu with the channel list.
  await page.click("#menu-button");
  await page.clock.runFor(500);
  await page.screenshot({ path: path.join(phoneFolder, "2-choose-channels.png") });

  // Phone 3: skipped ahead to the next channel.
  await page.click("#menu-button");
  await page.click("#next-button");
  await page.clock.runFor(1500);
  await page.screenshot({ path: path.join(phoneFolder, "3-next-channel.png") });
  await page.context().close();

  // TV 1: the remote's highlight on the Next button.
  page = await openSwitcherPage(browser, pageUrl, { asTv: true });
  await page.evaluate(() => {
    newsSwitcherRemote.moveFocus(1);
    newsSwitcherRemote.moveFocus(1);
  });
  await page.screenshot({ path: path.join(tvFolder, "1-remote-controls.png") });

  // TV 2: the menu open, with a channel's tick box highlighted.
  await page.evaluate(() => newsSwitcherRemote.pressKey("m"));
  await page.clock.runFor(500);
  await page.evaluate(() => {
    for (let step = 0; step < 3; step++) newsSwitcherRemote.moveFocus(1);
  });
  await page.screenshot({ path: path.join(tvFolder, "2-menu-on-tv.png") });
  await page.context().close();

  await browser.close();
  server.close();
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
