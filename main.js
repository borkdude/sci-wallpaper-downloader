// This code is based on this gist: https://gist.github.com/yogthos/d9d2324016f62d151c9843bdac3c0f23

const { evalString, toJS } = require('@borkdude/sci');
const fs = require('fs');
const http = require('http');
const https = require('https');

process.on('uncaughtException', console.error);

// sci doesn't do JS interop (yet). Let's write a small namespace that we will provide to it.
function saveFile(filename, response, callback) {
  response.pipe(fs.createWriteStream(filename), setTimeout(toJS(callback), 100));
}

function fileExists(filename) {
  return fs.existsSync(filename);
}

function promise(f) {
  return new Promise(toJS(f));
}

function thenP(promise, f) {
  return promise.then(toJS(f));
}

function catchP(promise, f) {
  return promise.catch(toJS(f));
}

function promiseAll(promises) {
  return Promise.all(promises);
}

function getUrl(url, callback) {
  if (url.startsWith("https")) {
    return https.get(url, toJS(callback));
  } else {
    return http.get(url, toJS(callback));
  }
}

function on(obj,name,cb) {
  return obj.on(name, toJS(cb));
}

const sciOptions = {
  namespaces: {
    "node.interop": {
      parseJSON: JSON.parse,
      saveFile: saveFile,
      fileExists: fileExists,
      getUrl: getUrl,
      promise: promise,
      thenP: thenP,
      catchP: catchP,
      promiseAll: promiseAll,
      on: on,
      log: console.log,
      error: console.error
    }
  }
};

// read the Clojure script from disk
const script = fs.readFileSync('script.cljs').toString();

// evaluating the script returns a CLJS function with metadata. To unwrap it, we use toJS.
const parseUrl = evalString(script, sciOptions);

// this is the URL where we start crawling
const startUrl = 'https://www.windowsonearth.org/services/api/json/1.4.0/?galleryType=album&albumId=37434732&albumKey=TBQqg7&nodeId=FDP9N&PageNumber=0&imageId=0&imageKey=&returnModelList=true&PageSize=16&imageSizes=L%2CXL&method=rpc.gallery.getalbum';

// go!
parseUrl(startUrl);
