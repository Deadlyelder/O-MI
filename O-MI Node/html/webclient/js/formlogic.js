// Generated by CoffeeScript 1.10.0
(function() {
  "use strict";
  var formLogicExt,
    hasProp = {}.hasOwnProperty,
    slice = [].slice;

  formLogicExt = function($, WebOmi) {
    var consts, my;
    my = WebOmi.formLogic = {};
    my.setRequest = function(xml) {
      var mirror;
      mirror = WebOmi.consts.requestCodeMirror;
      if (xml == null) {
        mirror.setValue("");
      } else if (typeof xml === "string") {
        mirror.setValue(xml);
      } else {
        mirror.setValue(new XMLSerializer().serializeToString(xml));
      }
      return mirror.autoFormatAll();
    };
    my.getRequest = function() {
      var str;
      str = WebOmi.consts.requestCodeMirror.getValue();
      return WebOmi.omi.parseXml(str);
    };
    my.modifyRequest = function(callback) {
      var req;
      req = my.getRequest();
      callback();
      return WebOmi.requests.generate();
    };
    my.getRequestOdf = function() {
      var str;
      WebOmi.error("getRequestOdf is deprecated");
      str = WebOmi.consts.requestCodeMirror.getValue();
      return o.evaluateXPath(str, '//odf:Objects')[0];
    };
    my.clearResponse = function(doneCallback) {
      var mirror;
      mirror = WebOmi.consts.responseCodeMirror;
      mirror.setValue("");
      return WebOmi.consts.responseDiv.slideUp({
        complete: function() {
          if (doneCallback != null) {
            return doneCallback();
          }
        }
      });
    };
    my.setResponse = function(xml, doneCallback) {
      var mirror;
      mirror = WebOmi.consts.responseCodeMirror;
      if (typeof xml === "string") {
        mirror.setValue(xml);
      } else {
        mirror.setValue(new XMLSerializer().serializeToString(xml));
      }
      mirror.autoFormatAll();
      WebOmi.consts.responseDiv.slideDown({
        complete: function() {
          mirror.refresh();
          if (doneCallback != null) {
            return doneCallback();
          }
        }
      });
      return mirror.refresh();
    };
    my.callbackSubscriptions = {};
    my.waitingForResponse = false;
    my.waitingForRequestID = false;
    consts = WebOmi.consts;
    consts.afterJquery(function() {
      consts.callbackResponseHistoryModal = $('.callbackResponseHistory');
      consts.callbackResponseHistoryModal.on('shown.bs.modal', function() {
        return my.updateHistoryCounter(true);
      }).on('hide.bs.modal', function() {
        return my.updateHistoryCounter(true);
      });
      consts.responseListCollection = $('.responseListCollection');
      consts.responseListCloneTarget = $('.responseList.cloneTarget');
      return consts.historyCounter = $('.label.historyCounter');
    });
    my.updateHistoryCounter = function(toZero) {
      var orginal, ref, requestID, sub, sum, update;
      if (toZero == null) {
        toZero = false;
      }
      update = function(sub) {
        if (toZero) {
          return sub.userSeenCount = sub.receivedCount;
        }
      };
      orginal = parseInt(consts.historyCounter.text());
      sum = 0;
      ref = my.callbackSubscriptions;
      for (requestID in ref) {
        if (!hasProp.call(ref, requestID)) continue;
        sub = ref[requestID];
        update(sub);
        sum += sub.receivedCount - sub.userSeenCount;
      }
      if (sum === 0) {
        consts.historyCounter.text(sum).removeClass("label-warning").addClass("label-default");
      } else {
        consts.historyCounter.text(sum).removeClass("label-default").addClass("label-warning");
      }
      if (sum > orginal) {
        return WebOmi.util.flash(consts.historyCounter.parent());
      }
    };
    my.handleSubscriptionHistory = function(responseString) {
      var addHistory, cbSub, cloneElem, createHistory, createShortenedPath, getPath, getPathValues, getShortenedPath, htmlformat, info, infoItemPathValues, infoitems, insertToTrie, maybeRequestID, moveHistoryHeaders, omi, pathPrefixTrie, pathValues, ref, requestID, response, returnStatus;
      omi = WebOmi.omi;
      response = omi.parseXml(responseString);
      maybeRequestID = Maybe(omi.evaluateXPath(response, "//omi:requestID/text()")[0]);
      requestID = (maybeRequestID.bind(function(idNode) {
        var textId;
        textId = idNode.textContent.trim();
        if (textId.length > 0) {
          return Maybe(parseInt(textId));
        } else {
          return None;
        }
      })).__v;
      if ((requestID != null)) {
        cbSub = my.callbackSubscriptions[requestID];
        if (cbSub != null) {
          cbSub.receivedCount += 1;
        } else {
          if (my.waitingForRequestID || !my.waitingForResponse) {
            my.waitingForRequestID = false;
            my.callbackSubscriptions[requestID] = {
              receivedCount: 1,
              userSeenCount: 0,
              responses: [responseString]
            };
          } else {
            return false;
          }
        }
      } else if (!my.waitingForResponse) {
        requestID = "not given";
        my.callbackSubscriptions[requestID] = {
          receivedCount: 1,
          userSeenCount: 0,
          responses: [responseString]
        };
      } else {
        return false;
      }
      getPath = function(xmlNode) {
        var id, init;
        id = omi.getOdfId(xmlNode);
        if ((id != null) && id !== "Objects") {
          init = getPath(xmlNode.parentNode);
          return init + "/" + id;
        } else {
          return id;
        }
      };
      pathPrefixTrie = {};
      insertToTrie = function(root, string) {
        var head, tail;
        if (string.length === 0) {
          return root;
        } else {
          head = string[0], tail = 2 <= string.length ? slice.call(string, 1) : [];
          if (root[head] == null) {
            root[head] = {};
          }
          return insertToTrie(root[head], tail);
        }
      };
      createShortenedPath = function(path) {
        var _, i, j, originalLast, prefixShorted, ref, ref1, shortedInit;
        prefixShorted = getShortenedPath(pathPrefixTrie, path);
        ref = prefixShorted.split("/"), shortedInit = 2 <= ref.length ? slice.call(ref, 0, i = ref.length - 1) : (i = 0, []), _ = ref[i++];
        ref1 = path.split("/"), _ = 2 <= ref1.length ? slice.call(ref1, 0, j = ref1.length - 1) : (j = 0, []), originalLast = ref1[j++];
        shortedInit.push(originalLast);
        return shortedInit.join("/");
      };
      getShortenedPath = function(tree, path, shortening) {
        var child, key, keys, tail;
        if (shortening == null) {
          shortening = false;
        }
        if (path.length === 0) {
          return "";
        }
        keys = Object.keys(tree);
        key = path[0], tail = 2 <= path.length ? slice.call(path, 1) : [];
        child = tree[key];
        if (child == null) {
          WebOmi.debug("Error: prefix tree failure: does not exist");
          return;
        }
        if (key === "/") {
          return "/" + getShortenedPath(child, tail);
        }
        if (keys.length === 1) {
          if (shortening) {
            return getShortenedPath(child, tail, true);
          } else {
            return "..." + getShortenedPath(child, tail, true);
          }
        } else {
          return key + getShortenedPath(child, tail);
        }
      };
      getPathValues = function(infoitemXmlNode) {
        var i, infoItemName, j, len, path, pathObject, ref, results, value, valuesXml;
        valuesXml = omi.evaluateXPath(infoitemXmlNode, "./odf:value");
        path = getPath(infoitemXmlNode);
        insertToTrie(pathPrefixTrie, path);
        ref = path.split("/"), pathObject = 2 <= ref.length ? slice.call(ref, 0, i = ref.length - 1) : (i = 0, []), infoItemName = ref[i++];
        results = [];
        for (j = 0, len = valuesXml.length; j < len; j++) {
          value = valuesXml[j];
          results.push({
            path: path,
            pathObject: pathObject.join('/'),
            infoItemName: infoItemName,
            shortPath: function() {
              return createShortenedPath(path);
            },
            value: value,
            stringValue: value.textContent.trim()
          });
        }
        return results;
      };
      cloneElem = function(target, callback) {
        return WebOmi.util.cloneElem(target, function(cloned) {
          return cloned.slideDown(null, function() {
            return consts.callbackResponseHistoryModal.modal('handleUpdate');
          });
        });
      };
      moveHistoryHeaders = function(latestDom) {
        var olderH;
        olderH = consts.callbackResponseHistoryModal.find('.olderSubsHeader');
        return latestDom.after(olderH);
      };
      createHistory = function(requestID) {
        var newList;
        newList = cloneElem(consts.responseListCloneTarget);
        moveHistoryHeaders(newList);
        newList.removeClass("cloneTarget").show();
        newList.find('.requestID').text(requestID);
        return newList;
      };
      returnStatus = function(count, returnCode) {
        var row;
        row = $("<tr/>").addClass((function() {
          switch (Math.floor(returnCode / 100)) {
            case 2:
              return "success";
            case 3:
              return "warning";
            case 4:
              return "danger";
          }
        })()).addClass("respRet").append($("<th/>").text(count)).append($("<th>returnCode</th>")).append($("<th/>").text(returnCode));
        row.tooltip({
          title: "click to show the XML"
        }).on('click', function() {
          var codeMirrorContainer, dataRows, responseCodeMirror, tmpRow, tmpTr;
          if (row.data.dataRows) {
            tmpRow = row.nextUntil('.respRet');
            tmpRow.remove();
            row.after(row.data.dataRows);
            return delete row.data.dataRows;
          } else {
            dataRows = row.nextUntil('.respRet');
            row.data.dataRows = dataRows.clone();
            dataRows.remove();
            tmpTr = $('<tr/>');
            codeMirrorContainer = $('<td colspan=3/>');
            tmpTr.append(codeMirrorContainer);
            row.after(tmpTr);
            responseCodeMirror = CodeMirror(codeMirrorContainer[0], WebOmi.consts.responseCMSettings);
            responseCodeMirror.setValue(responseString);
            return responseCodeMirror.autoFormatAll();
          }
        });
        return row;
      };
      htmlformat = function(pathValues) {
        var i, len, pathValue, results, row;
        results = [];
        for (i = 0, len = pathValues.length; i < len; i++) {
          pathValue = pathValues[i];
          row = $("<tr/>").append($("<td/>")).append($("<td/>").append($('<span class="hidden-lg hidden-md" />').text(pathValue.shortPath)).append($('<span class="hidden-xs hidden-sm" />').text(pathValue.pathObject + '/').append($('<b/>').text(pathValue.infoItemName))).tooltip({
            container: consts.callbackResponseHistoryModal,
            title: pathValue.path
          })).append($("<td/>").tooltip({
            title: pathValue.value.attributes.dateTime.value
          }).append($("<code/>").text(pathValue.stringValue)));
          results.push(row);
        }
        return results;
      };
      addHistory = function(requestID, pathValues) {
        var callbackRecord, dataTable, newHistory, responseList, returnS;
        callbackRecord = my.callbackSubscriptions[requestID];
        responseList = (callbackRecord.selector != null) && callbackRecord.selector.length > 0 ? callbackRecord.selector : (newHistory = createHistory(requestID), my.callbackSubscriptions[requestID].selector = newHistory, newHistory);
        dataTable = responseList.find(".dataTable");
        returnS = returnStatus(callbackRecord.receivedCount, 200);
        return dataTable.prepend(htmlformat(pathValues)).prepend(returnS);
      };
      infoitems = omi.evaluateXPath(response, "//odf:InfoItem");
      infoItemPathValues = (function() {
        var i, len, results;
        results = [];
        for (i = 0, len = infoitems.length; i < len; i++) {
          info = infoitems[i];
          results.push(getPathValues(info));
        }
        return results;
      })();
      pathValues = (ref = []).concat.apply(ref, infoItemPathValues);
      addHistory(requestID, pathValues);
      return !my.waitingForResponse;
    };
    my.createWebSocket = function(onopen, onclose, onmessage, onerror) {
      var server, socket;
      WebOmi.debug("Creating WebSocket.");
      consts = WebOmi.consts;
      server = consts.serverUrl.val();
      socket = new WebSocket(server);
      socket.onopen = onopen;
      socket.onclose = function() {
        return onclose;
      };
      socket.onmessage = onmessage;
      socket.onerror = onerror;
      return my.socket = socket;
    };
    my.send = function(callback) {
      var request, server;
      consts = WebOmi.consts;
      my.clearResponse();
      server = consts.serverUrl.val();
      request = consts.requestCodeMirror.getValue();
      if (server.startsWith("ws://") || server.startsWith("wss://")) {
        return my.wsSend(request, callback);
      } else {
        return my.httpSend(callback);
      }
    };
    my.wsCallbacks = [];
    my.wsSend = function(request, callback) {
      var maybeParsedXml, maybeVerbXml, omi, onclose, onerror, onmessage, onopen;
      if (!my.socket || my.socket.readyState !== WebSocket.OPEN) {
        onopen = function() {
          WebOmi.debug("WebSocket connected.");
          return my.wsSend(request, callback);
        };
        onclose = function() {
          return WebOmi.debug("WebSocket disconnected.");
        };
        onerror = function(error) {
          return WebOmi.debug("WebSocket error: ", error);
        };
        onmessage = my.handleWSMessage;
        return my.createWebSocket(onopen, onclose, onmessage, onerror);
      } else {
        WebOmi.debug("Sending request via WebSocket.");
        my.waitingForResponse = true;
        if (callback != null) {
          my.wsCallbacks.push(callback);
        }
        omi = WebOmi.omi;
        maybeParsedXml = Maybe(omi.parseXml(request));
        maybeVerbXml = maybeParsedXml.bind(function(parsedXml) {
          var verbResult;
          verbResult = omi.evaluateXPath(parsedXml, "//omi:omiEnvelope/*")[0];
          return Maybe(verbResult);
        });
        maybeVerbXml.fmap(function(verbXml) {
          var isSubscriptionReq, maybeCallback, maybeInterval, verb;
          verb = verbXml.tagName;
          maybeCallback = Maybe(verbXml.attributes.callback);
          maybeInterval = Maybe(verbXml.attributes.interval);
          isSubscriptionReq = maybeCallback.exists(function(c) {
            return c.value === "0";
          }) && verb === "omi:read" && maybeInterval.isDefined;
          if (isSubscriptionReq) {
            return my.waitingForRequestID = true;
          }
        });
        return my.socket.send(request);
      }
    };
    my.httpSend = function(callback) {
      var request, server;
      WebOmi.debug("Sending request with HTTP POST.");
      consts = WebOmi.consts;
      server = consts.serverUrl.val();
      request = consts.requestCodeMirror.getValue();
      consts.progressBar.css("width", "95%");
      return $.ajax({
        type: "POST",
        url: server,
        data: request,
        contentType: "text/xml",
        processData: false,
        dataType: "text",
        error: function(response) {
          consts.progressBar.css("width", "100%");
          my.setResponse(response.responseText);
          consts.progressBar.css("width", "0%");
          consts.progressBar.hide();
          return window.setTimeout((function() {
            return consts.progressBar.show();
          }), 2000);
        },
        success: function(response) {
          consts.progressBar.css("width", "100%");
          my.setResponse(response);
          consts.progressBar.css("width", "0%");
          consts.progressBar.hide();
          window.setTimeout((function() {
            return consts.progressBar.show();
          }), 2000);
          if ((callback != null)) {
            return callback(response);
          }
        }
      });
    };
    my.handleWSMessage = function(message) {
      var cb, i, len, ref, response;
      consts = WebOmi.consts;
      response = message.data;
      if (!my.handleSubscriptionHistory(response)) {
        consts.progressBar.css("width", "100%");
        my.setResponse(response);
        consts.progressBar.css("width", "0%");
        consts.progressBar.hide();
        window.setTimeout((function() {
          return consts.progressBar.show();
        }), 2000);
        my.waitingForResponse = false;
      } else {
        my.updateHistoryCounter();
      }
      ref = my.wsCallbacks;
      for (i = 0, len = ref.length; i < len; i++) {
        cb = ref[i];
        cb(response);
      }
      return my.wsCallbacks = [];
    };
    my.buildOdfTree = function(objectsNode) {
      var evaluateXPath, genData, objChildren, tree, treeData;
      tree = WebOmi.consts.odfTree;
      evaluateXPath = WebOmi.omi.evaluateXPath;
      objChildren = function(xmlNode) {
        return evaluateXPath(xmlNode, './odf:InfoItem | ./odf:Object');
      };
      genData = function(xmlNode, parentPath) {
        var child, name, path;
        switch (xmlNode.nodeName) {
          case "Objects":
            name = xmlNode.nodeName;
            return {
              id: idesc(name),
              text: name,
              state: {
                opened: true
              },
              type: "objects",
              children: (function() {
                var i, len, ref, results;
                ref = objChildren(xmlNode);
                results = [];
                for (i = 0, len = ref.length; i < len; i++) {
                  child = ref[i];
                  results.push(genData(child, name));
                }
                return results;
              })()
            };
          case "Object":
            name = WebOmi.omi.getOdfId(xmlNode);
            path = parentPath + "/" + name;
            return {
              id: idesc(path),
              text: name,
              type: "object",
              children: (function() {
                var i, len, ref, results;
                ref = objChildren(xmlNode);
                results = [];
                for (i = 0, len = ref.length; i < len; i++) {
                  child = ref[i];
                  results.push(genData(child, path));
                }
                return results;
              })()
            };
          case "InfoItem":
            name = WebOmi.omi.getOdfId(xmlNode);
            path = parentPath + "/" + name;
            return {
              id: idesc(path),
              text: name,
              type: "infoitem",
              children: [
                genData({
                  nodeName: "MetaData"
                }, path)
              ]
            };
          case "MetaData":
            path = parentPath + "/MetaData";
            return {
              id: idesc(path),
              text: "MetaData",
              type: "metadata",
              children: []
            };
        }
      };
      treeData = genData(objectsNode);
      tree.settings.core.data = [treeData];
      return tree.refresh();
    };
    my.buildOdfTreeStr = function(responseString) {
      var objectsArr, omi, parsed;
      omi = WebOmi.omi;
      parsed = omi.parseXml(responseString);
      objectsArr = omi.evaluateXPath(parsed, "//odf:Objects");
      if (objectsArr.length !== 1) {
        return WebOmi.error("failed to get single Objects odf root");
      } else {
        return my.buildOdfTree(objectsArr[0]);
      }
    };
    return WebOmi;
  };

  window.WebOmi = formLogicExt($, window.WebOmi || {});

  (function(consts, requests, formLogic) {
    return consts.afterJquery(function() {
      var controls, inputVar, makeRequestUpdater, ref;
      consts.readAllBtn.on('click', function() {
        return requests.readAll(true);
      });
      consts.sendBtn.on('click', function() {
        return formLogic.send();
      });
      consts.resetAllBtn.on('click', function() {
        var child, closetime, i, len, ref;
        requests.forceLoadParams(requests.defaults.empty());
        closetime = 1500;
        ref = consts.odfTree.get_children_dom('Objects');
        for (i = 0, len = ref.length; i < len; i++) {
          child = ref[i];
          consts.odfTree.close_all(child, closetime);
        }
        formLogic.clearResponse();
        return $('.clearHistory').trigger('click');
      });
      consts.ui.odf.ref.on("changed.jstree", function(_, data) {
        var odfTreePath;
        switch (data.action) {
          case "select_node":
            odfTreePath = data.node.id;
            return formLogic.modifyRequest(function() {
              return requests.params.odf.add(odfTreePath);
            });
          case "deselect_node":
            odfTreePath = data.node.id;
            formLogic.modifyRequest(function() {
              return requests.params.odf.remove(odfTreePath);
            });
            return $(jqesc(odfTreePath)).children(".jstree-children").find(".jstree-node").each(function(_, node) {
              return consts.odfTree.deselect_node(node, true);
            });
        }
      });
      consts.ui.request.ref.on("select_node.jstree", function(_, data) {
        var i, input, isReadReq, isRequestIdReq, len, readReqWidgets, reqName, ui;
        reqName = data.node.id;
        WebOmi.debug(reqName);
        if (reqName === "readReq") {
          return consts.ui.request.set("read");
        } else {
          ui = WebOmi.consts.ui;
          readReqWidgets = [ui.newest, ui.oldest, ui.begin, ui.end];
          isReadReq = (function() {
            switch (reqName) {
              case "readAll":
              case "read":
              case "readReq":
                return true;
              default:
                return false;
            }
          })();
          isRequestIdReq = (function() {
            switch (reqName) {
              case "cancel":
              case "poll":
                return true;
              default:
                return false;
            }
          })();
          for (i = 0, len = readReqWidgets.length; i < len; i++) {
            input = readReqWidgets[i];
            input.ref.prop('disabled', !isReadReq);
            input.set(null);
            input.ref.trigger("input");
          }
          ui.requestID.ref.prop('disabled', !isRequestIdReq);
          if (!isRequestIdReq) {
            ui.requestID.set(null);
            ui.requestID.ref.trigger("input");
          }
          ui.interval.ref.prop('disabled', reqName !== 'subscription');
          ui.interval.set(null);
          ui.interval.ref.trigger("input");
          return formLogic.modifyRequest(function() {
            var newHasMsg;
            requests.params.name.update(reqName);
            newHasMsg = requests.defaults[reqName]().msg;
            return requests.params.msg.update(newHasMsg);
          });
        }
      });
      makeRequestUpdater = function(input) {
        return function(val) {
          return formLogic.modifyRequest(function() {
            return requests.params[input].update(val);
          });
        };
      };
      ref = consts.ui;
      for (inputVar in ref) {
        if (!hasProp.call(ref, inputVar)) continue;
        controls = ref[inputVar];
        if (controls.bindTo != null) {
          controls.bindTo(makeRequestUpdater(inputVar));
        }
      }
      return null;
    });
  })(window.WebOmi.consts, window.WebOmi.requests, window.WebOmi.formLogic);

  $(function() {
    return $('.optional-parameters > a').on('click', function() {
      var glyph;
      glyph = $(this).find('span.glyphicon');
      if (glyph.hasClass('glyphicon-menu-right')) {
        glyph.removeClass('glyphicon-menu-right');
        return glyph.addClass('glyphicon-menu-down');
      } else {
        glyph.removeClass('glyphicon-menu-down');
        return glyph.addClass('glyphicon-menu-right');
      }
    });
  });

}).call(this);
