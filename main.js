/*

This code is based on this gist: https://gist.github.com/yogthos/d9d2324016f62d151c9843bdac3c0f23

*/

const { evalString, toJS } = require('@borkdude/sci');
const fs = require('fs');
const { readFileSync } = fs;
const http = require('http');
const https = require('https');

process.on('uncaughtException', console.error);

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
  bindings: {
    parseJSON: JSON.parse,
    saveFile: saveFile,
    fileExists: fileExists,
    getUrl: getUrl,
    promise: promise,
    thenP: thenP,
    catchP: catchP,
    promiseAll: promiseAll,
    on: on,
    println: console.log,
    error: console.error
  }
};

const script = readFileSync('script.cljs').toString();
const parseUrl = toJS(evalString(script, sciOptions));
const startUrl = 'https://www.windowsonearth.org/services/api/json/1.4.0/?galleryType=album&albumId=37434732&albumKey=TBQqg7&nodeId=FDP9N&PageNumber=0&imageId=0&imageKey=&returnModelList=true&PageSize=16&imageSizes=L%2CXL&method=rpc.gallery.getalbum';

parseUrl(startUrl);

