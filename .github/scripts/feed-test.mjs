// Signs in to Quora with a test account in the patched app on an emulator and checks that the page
// patches work on the feed and on an answer page. Run after check_quora_version.py --install, with a
// bundle whose page scripts turn WebView debugging on (the feed test workflow builds one).
//
// Usage: QUORA_TEST_EMAIL=… QUORA_TEST_PASSWORD=… node .github/scripts/feed-test.mjs
// Optional: ADB (path to adb), QUORA_PACKAGE (default com.quora.android), DRY_RUN=1 (fills the
// sign-in form but doesn't send it).
//
// Exit codes: 0 every check passed, or the test was skipped (no account, or Quora asked for more
// than a password: a captcha or a code); 1 a check failed; 2 the app or its pages couldn't be reached.
// The account's email and password are never printed.
import { execFileSync } from 'node:child_process';

const ADB = process.env.ADB || 'adb';
const PACKAGE = process.env.QUORA_PACKAGE || 'com.quora.android';
const PORT = 9222;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const adb = (...args) => {
  try {
    return execFileSync(ADB, args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
  } catch {
    return '';
  }
};
const notice = (msg) => console.log(`::notice title=Feed test::${msg}`);

async function connect() {
  const pid = adb('shell', 'pidof', PACKAGE).split(/\s+/)[0];
  if (!pid) return false;
  adb('forward', '--remove-all');
  adb('forward', `tcp:${PORT}`, `localabstract:webview_devtools_remote_${pid}`);
  return true;
}

async function pages() {
  try {
    const list = await (await fetch(`http://localhost:${PORT}/json`)).json();
    // An answer opened from the feed is a modal page that reports about:blank.
    return list.filter((p) => p.type === 'page' && p.webSocketDebuggerUrl
      && (p.url === 'about:blank' || /^https:\/\/([a-z]+\.)?quora\.com\//.test(p.url)));
  } catch {
    return [];
  }
}

/** Runs an expression in one page and returns its value (promises are awaited). */
function evaluate(page, expression, timeout = 30000) {
  return new Promise((resolve) => {
    const ws = new WebSocket(page.webSocketDebuggerUrl);
    const timer = setTimeout(() => { try { ws.close(); } catch {} resolve(undefined); }, timeout);
    ws.onerror = () => { clearTimeout(timer); resolve(undefined); };
    ws.onopen = () => ws.send(JSON.stringify({
      id: 1, method: 'Runtime.evaluate', params: { expression, returnByValue: true, awaitPromise: true },
    }));
    ws.onmessage = (m) => {
      const d = JSON.parse(m.data);
      if (d.id !== 1) return;
      clearTimeout(timer);
      try { ws.close(); } catch {}
      resolve(d.result?.result?.value);
    };
  });
}

/** Waits until one of the pages returns something truthy for the expression. */
async function waitFor(expression, seconds) {
  const end = Date.now() + seconds * 1000;
  while (Date.now() < end) {
    for (const page of await pages()) {
      const value = await evaluate(page, expression, 10000);
      if (value) return { page, value };
    }
    await sleep(3000);
  }
  return null;
}

const VISIBLE = 'function vis(e){for(;e&&e.nodeType===1;e=e.parentElement){var s=getComputedStyle(e);'
  + "if(s.display==='none'||s.visibility==='hidden')return false;}return true;}";
const FEED = '[class*="dom_annotate_multifeed_bundle_"]';
const feedCount = `document.querySelectorAll('${FEED}').length>=3&&location.pathname==='/'`;
const clickText = (labels) => `(function(){var want=${JSON.stringify(labels)};${VISIBLE}`
  + "var b=[].slice.call(document.querySelectorAll('button,[role=button],a')).find(function(e){"
  + 'return want.indexOf((e.innerText||"").trim())>=0&&vis(e)&&e.getClientRects().length;});'
  + 'if(!b)return false;b.click();return true;})()';

async function signIn(email, password) {
  // The cookie dialog shows in the EU only.
  await waitFor(clickText(['Reject All']), 10);
  if (await waitFor(feedCount, 5)) return 'signed-in';
  const hasForm = "!!document.querySelector('input#email')&&!!document.querySelector('input#password')";
  // The start page's Login button opens the form; "Log in" would send it, so only Login is tapped.
  if (!(await waitFor(hasForm, 3)) && !(await waitFor(clickText(['Login']), 60))) return 'no-login-button';
  const form = await waitFor(hasForm, 30);
  if (!form) return 'no-form';
  const fill = `(function(){var set=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;
    function put(id,v){var i=document.querySelector('input#'+id);i.focus();set.call(i,v);
      i.dispatchEvent(new Event('input',{bubbles:true}));i.dispatchEvent(new Event('change',{bubbles:true}));}
    put('email',${JSON.stringify(email)});put('password',${JSON.stringify(password)});
    return document.querySelector('input#email').value.length>0&&document.querySelector('input#password').value.length>0;})()`;
  if (!(await evaluate(form.page, fill))) return 'fill-failed';
  if (process.env.DRY_RUN === '1') return 'dry-run';
  await sleep(1000);
  if (!(await evaluate(form.page, clickText(['Log in'])))) return 'no-submit';
  if (await waitFor(feedCount, 90)) return 'signed-in';
  // Say why, without printing the page (it may show the address).
  const why = await evaluate(form.page, '(function(){var t=(document.body&&document.body.innerText||"").toLowerCase();'
    + "if(/captcha|robot|recaptcha/.test(t)||document.querySelector('iframe[src*=\"recaptcha\"],iframe[src*=\"captcha\"]'))return 'captcha';"
    + "if(/code|verify|verification|confirm/.test(t))return 'verification';"
    + "if(/incorrect|wrong|invalid|no account/.test(t))return 'rejected';return 'unknown';})()");
  return `not-signed-in:${why || 'page gone'}`;
}

const FEED_CHECKS = `(async function(){${VISIBLE}
  window.scrollBy(0,1500);await new Promise(function(r){setTimeout(r,4000);});window.scrollTo(0,0);
  await new Promise(function(r){setTimeout(r,2000);});
  var out={};
  out.bridge=typeof window.JoFSlashPatches==='object';
  out.feedItems=document.querySelectorAll('${FEED}').length;
  out.askBoxVisible=[].slice.call(document.querySelectorAll('[class*="dom_annotate_multifeed_bundle_AskQ"]')).filter(vis).length;
  var bars=[].slice.call(document.querySelectorAll('.dom_annotate_answer_action_bar_share'));
  out.shareBars=bars.length;
  out.copyButtons=document.querySelectorAll('[data-jofslash-clone] [aria-label="Copy text"],[data-jofslash-clone][aria-label="Copy text"]').length;
  out.shareTextButtons=document.querySelectorAll('[data-jofslash-clone] [aria-label="Share text"],[data-jofslash-clone][aria-label="Share text"]').length;
  out.mostCopiesInOneBar=bars.reduce(function(m,b){var p=b.parentElement;
    var n=p?p.querySelectorAll('[aria-label="Copy text"]').length:0;return Math.max(m,n);},0);
  out.promotedVisible=[].slice.call(document.querySelectorAll('${FEED}')).filter(function(u){
    return vis(u)&&/(^|\\n)(Promoted by|Sponsored)\\b/.test(u.innerText||'');}).length;
  var url='https://www.quora.com/jofslash-feed-test-bookmark';var J=window.JoFSlashPatches;
  if(J){var before=J.isBookmarked(url);J.toggleBookmark(url,'Feed test','');var saved=J.isBookmarked(url);
    J.toggleBookmark(url,'Feed test','');out.bookmarkStore=!before&&saved&&!J.isBookmarked(url);}
  return out;})()`;

// Where to tap to open the first answer in the feed: its question title, in page pixels.
const TITLE_SPOT = `(async function(){${VISIBLE}
  var t=[].slice.call(document.querySelectorAll('[class*="dom_annotate_multifeed_bundle_AnswersBundle"] .puppeteer_test_question_title')).find(vis);
  if(!t)return null;t.scrollIntoView({block:'center'});await new Promise(function(r){setTimeout(r,1500);});
  var r=t.getBoundingClientRect();return {x:r.left+Math.min(r.width/2,100),y:r.top+r.height/2,dpr:window.devicePixelRatio};})()`;

const IS_ANSWER = "/AnswerModal|\\/answer\\//.test(location.href)&&document.readyState==='complete'";

const ANSWER_CHECKS = `(async function(){
  await new Promise(function(r){setTimeout(r,5000);});
  return {bookmarkButton:document.querySelectorAll('[aria-label="Bookmark"],[aria-label="Remove bookmark"]').length,
    copyButtons:document.querySelectorAll('[aria-label="Copy text"]').length};})()`;

/** The screen position of the biggest WebView, from uiautomator. */
function webViewBounds() {
  adb('shell', 'uiautomator', 'dump', '/sdcard/window.xml');
  const xml = adb('shell', 'cat', '/sdcard/window.xml');
  let best = null;
  for (const m of xml.matchAll(/class="android\.webkit\.WebView"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g)) {
    const [l, t, r, b] = m.slice(1).map(Number);
    if (!best || (r - l) * (b - t) > (best.r - best.l) * (best.b - best.t)) best = { l, t, r, b };
  }
  return best;
}

async function main() {
  const email = process.env.QUORA_TEST_EMAIL;
  const password = process.env.QUORA_TEST_PASSWORD;
  if (!email || !password) {
    notice('QUORA_TEST_EMAIL and QUORA_TEST_PASSWORD aren\'t set, so the feed test is skipped.');
    return 0;
  }
  let connected = false;
  for (let i = 0; i < 20 && !connected; i++) {
    connected = (await connect()) && (await pages()).length > 0;
    if (!connected) await sleep(3000);
  }
  if (!connected) {
    console.error('No Quora page to inspect: the app isn\'t running, or WebView debugging is off in this build.');
    return 2;
  }

  const state = await signIn(email, password);
  console.log(`Sign-in: ${state}`);
  if (state === 'dry-run') return 0;
  if (state.startsWith('not-signed-in:captcha') || state.startsWith('not-signed-in:verification')) {
    notice(`Quora asked for more than a password (${state.split(':')[1]}), so the feed test is skipped.`);
    return 0;
  }
  if (state !== 'signed-in') {
    console.error(`Couldn't sign in (${state}).`);
    return 2;
  }

  const feed = await waitFor(feedCount, 30);
  const result = feed ? await evaluate(feed.page, FEED_CHECKS, 60000) : null;
  if (!result) {
    console.error('The feed didn\'t load after signing in.');
    return 2;
  }
  console.log('Feed:', JSON.stringify(result));

  const failures = [];
  const expect = (ok, msg) => { if (!ok) failures.push(msg); };
  expect(result.bridge, 'The patches\' JavaScript bridge is missing.');
  expect(result.feedItems >= 3, 'The feed has fewer than 3 items.');
  expect(result.askBoxVisible === 0, 'The ask box still shows (Hide ask box).');
  expect(result.shareBars > 0, 'No Share buttons in the feed (Quora may have renamed them).');
  expect(result.copyButtons > 0, 'No Copy buttons in the feed (Copy answer text).');
  expect(result.shareTextButtons > 0, 'No Share text buttons in the feed (Share answer text).');
  expect(result.mostCopiesInOneBar <= 1, 'A Share bar has more than one Copy button.');
  expect(result.promotedVisible === 0, 'A promoted item still shows (Remove ads).');
  expect(result.bookmarkStore === true, 'Saving and removing a bookmark through the bridge didn\'t work.');

  // Quora opens answers only on a real tap, so tap the first question title on the screen.
  const spot = await evaluate(feed.page, TITLE_SPOT);
  const view = spot && webViewBounds();
  if (spot && view) {
    const x = Math.round(view.l + spot.x * spot.dpr);
    const y = Math.round(view.t + spot.y * spot.dpr);
    adb('shell', 'input', 'tap', String(x), String(y));
    const answer = await waitFor(IS_ANSWER, 45);
    if (answer) {
      const checks = await evaluate(answer.page, ANSWER_CHECKS);
      console.log('Answer page:', JSON.stringify(checks));
      expect(checks && checks.bookmarkButton > 0, 'No bookmark button on the answer page (Bookmarks).');
      expect(checks && checks.copyButtons > 0, 'No Copy button on the answer page.');
    } else {
      notice('The answer didn\'t open from the feed, so its checks are skipped.');
    }
    adb('shell', 'input', 'keyevent', 'BACK');
  } else {
    notice('No answer to open in the feed, so the answer page checks are skipped.');
  }

  if (failures.length) {
    for (const f of failures) console.log(`::error title=Feed test::${f}`);
    return 1;
  }
  console.log('Every feed check passed.');
  return 0;
}

process.exit(await main());
