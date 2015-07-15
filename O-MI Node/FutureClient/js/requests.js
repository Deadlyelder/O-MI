// Generated by CoffeeScript 1.9.3
(function() {
  var requestsExt;

  requestsExt = function(WebOmi) {
    var lastParameters, my;
    my = WebOmi.requests = {};
    my.xmls = {
      readAll: "<?xml version=\"1.0\"?>\n<omi:omiEnvelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:omi=\"omi.xsd\"\n    version=\"1.0\" ttl=\"0\">\n  <omi:read msgformat=\"odf\">\n    <omi:msg xmlns=\"odf.xsd\" xsi:schemaLocation=\"odf.xsd odf.xsd\">\n      <Objects></Objects>\n    </omi:msg>\n  </omi:read>\n</omi:omiEnvelope> ",
      templateMsg: "<?xml version=\"1.0\"?>\n<omi:omiEnvelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:omi=\"omi.xsd\"\n    version=\"1.0\" ttl=\"0\">\n  <omi:read msgformat=\"odf\">\n    <omi:msg xmlns=\"odf.xsd\" xsi:schemaLocation=\"odf.xsd odf.xsd\">\n    </omi:msg>\n  </omi:read>\n</omi:omiEnvelope> \n",
      template: "<?xml version=\"1.0\"?>\n<omi:omiEnvelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:omi=\"omi.xsd\"\n    version=\"1.0\" ttl=\"0\">\n  <omi:read msgformat=\"odf\">\n    <omi:msg xmlns=\"odf.xsd\" xsi:schemaLocation=\"odf.xsd odf.xsd\">\n    </omi:msg>\n  </omi:read>\n</omi:omiEnvelope> \n"
    };
    my.defaults = {};
    my.defaults.empty = function() {
      return {
        request: null,
        ttl: 0,
        callback: null,
        requestID: null,
        odf: null,
        interval: null,
        newest: null,
        oldest: null,
        begin: null,
        end: null,
        resultDoc: null,
        msg: true
      };
    };
    my.defaults.readAll = function() {
      var res;
      res = $.extend({}, my.defaults.empty(), {
        request: "read",
        resultDoc: WebOmi.omi.parseXml(my.xmls.readAll)
      });
      res.odf = res.resultDoc;
      return res;
    };
    my.defaults.readOnce = function() {
      return $.extend({}, my.defaults.empty(), {
        request: "read"
      });
    };
    my.defaults.subscription = function() {
      return $.extend({}, my.defaults.empty(), {
        request: "read",
        interval: 5,
        ttl: 60
      });
    };
    my.defaults.poll = function() {
      return $.extend({}, my.defaults.empty(), {
        request: "read",
        requestID: 1
      });
    };
    my.defaults.write = function() {
      var doc;
      doc = WebOmi.omi.parseXml(my.xmls.templateMsg);
      return $.extend({}, my.defaults.empty(), {
        request: "write",
        odf: WebOmi.omi.createOdf(doc, "Objects")
      });
    };
    my.defaults.cancel = function() {
      return $.extend({}, my.defaults.empty(), {
        request: "cancel",
        requestID: 1,
        odf: null,
        msg: false
      });
    };
    lastParameters = my.defaults;

    /*
    my.set =
      request  : null  # Maybe string (request tag name)
      ttl      : 0     # double
      callback : null  # Maybe string
      requestID: null  # Maybe int
      odf      : null  # Maybe xml
      interval : null  # Maybe number
      newest   : null  # Maybe int
      oldest   : null  # Maybe int
      begin    : null  # Maybe Date
      end      : null  # Maybe Date
      resultDoc: null  # Maybe xml dom document
      msg      : true  # Boolean Is message included
     */
    my.loadParams = function(omiRequestObject) {};
    my.readAll = function(fastForward) {
      WebOmi.formLogic.setRequest(my.xmls.readAll);
      if (fastForward) {
        return WebOmi.formLogic.send(WebOmi.formLogic.buildOdfTreeStr);
      }
    };
    my.addPathToRequest = function(path) {
      var fl, o, odfTreeNode;
      o = WebOmi.omi;
      fl = WebOmi.formLogic;
      odfTreeNode = $(jqesc(path));
      return fl.modifyRequestOdfs(function(currentObjectsHead) {
        var objects;
        if (currentObjectsHead != null) {
          return my.addPathToOdf(odfTreeNode, currentObjectsHead);
        } else {
          objects = o.createOdfObjects(xmlTree);
          my.addPathToOdf(odfTreeNode, objects);
          return msg.appendChild(objects);
        }
      });
    };
    my.removePathFromRequest = function(path) {
      var fl, o, odfTreeNode;
      o = WebOmi.omi;
      fl = WebOmi.formLogic;
      odfTreeNode = $(jqesc(path));
      return fl.modifyRequestOdfs(function(odfObjects) {
        return my.removePathFromOdf(odfTreeNode, odfObjects);
      });
    };
    my.removePathFromOdf = function(odfTreeNode, odfObjects) {
      var allOdfElems, elem, i, id, lastOdfElem, len, maybeChild, node, nodeElems, o;
      o = WebOmi.omi;
      nodeElems = $.makeArray(odfTreeNode.parentsUntil("#Objects", "li"));
      nodeElems.reverse();
      nodeElems.push(odfTreeNode);
      lastOdfElem = odfObjects;
      allOdfElems = (function() {
        var i, len, results;
        results = [];
        for (i = 0, len = nodeElems.length; i < len; i++) {
          node = nodeElems[i];
          id = $(node).children("a").text();
          maybeChild = o.getOdfChild(id, lastOdfElem);
          if (maybeChild != null) {
            lastOdfElem = maybeChild;
          }
          results.push(maybeChild);
        }
        return results;
      })();
      lastOdfElem.parentElement.removeChild(lastOdfElem);
      allOdfElems.pop();
      allOdfElems.reverse();
      for (i = 0, len = allOdfElems.length; i < len; i++) {
        elem = allOdfElems[i];
        if (!o.hasOdfChildren(elem)) {
          elem.parentElement.removeChild(elem);
        }
      }
      return odfObjects;
    };
    my.addPathToOdf = function(odfTreeNode, odfObjects) {
      var currentOdfNode, i, id, len, maybeChild, node, nodeElems, o, obj, odfDoc;
      o = WebOmi.omi;
      odfDoc = odfObjects.ownerDocument || odfObjects;
      nodeElems = $.makeArray(odfTreeNode.parentsUntil("#Objects", "li"));
      nodeElems.reverse();
      nodeElems.push(odfTreeNode);
      currentOdfNode = odfObjects;
      for (i = 0, len = nodeElems.length; i < len; i++) {
        node = nodeElems[i];
        id = $(node).children("a").text();
        maybeChild = o.getOdfChild(id, currentOdfNode);
        if (maybeChild != null) {
          currentOdfNode = maybeChild;
        } else {
          obj = (function() {
            switch (WebOmi.consts.odfTree.get_type(node)) {
              case "object":
                return o.createOdfObject(odfDoc, id);
              case "infoitem":
                return o.createOdfInfoItem(odfDoc, id);
            }
          })();
          currentOdfNode.appendChild(obj);
          currentOdfNode = obj;
        }
      }
      return odfObjects;
    };
    return WebOmi;
  };

  window.WebOmi = requestsExt(window.WebOmi || {});

}).call(this);
