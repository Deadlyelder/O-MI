// Generated by CoffeeScript 1.9.3
(function() {
  var constsExt;

  constsExt = function($, parent) {
    var afterWaits, my;
    my = parent.consts = {};
    my.codeMirrorSettings = {
      mode: "text/xml",
      lineNumbers: true,
      lineWrapping: true
    };
    afterWaits = [];
    my.afterJquery = function(fn) {
      return afterWaits.push(fn);
    };
    $(function() {
      var fn, i, len, responseCMSettings, results;
      responseCMSettings = $.extend({
        readOnly: true
      }, my.codeMirrorSettings);
      my.requestCodeMirror = CodeMirror.fromTextArea($("#requestArea")[0], my.codeMirrorSettings);
      my.responseCodeMirror = CodeMirror.fromTextArea($("#responseArea")[0], responseCMSettings);
      my.serverUrl = $('#targetService');
      my.odfTreeDom = $('#nodetree');
      my.requestSel = $('.requesttree');
      my.readAllBtn = $('#readall');
      my.sendBtn = $('#send');
      my.odfTreeDom.jstree({
        plugins: ["checkbox", "types"],
        types: {
          "default": {
            icon: "odf-objects glyphicon glyphicon-tree-deciduous"
          },
          object: {
            icon: "odf-object glyphicon glyphicon-folder-open"
          },
          objects: {
            icon: "odf-objects glyphicon glyphicon-tree-deciduous"
          },
          infoitem: {
            icon: "odf-infoitem glyphicon glyphicon-apple"
          }
        },
        checkbox: {
          three_state: false,
          keep_selected_style: true,
          cascade: "up+undetermined",
          tie_selection: true
        }
      });
      my.odfTree = my.odfTreeDom.jstree();
      my.requestSel.jstree({
        core: {
          themes: {
            icons: false
          },
          multiple: false
        }
      }).on("changed.jstree", function(e, data) {
        return console.log(data.node.id);
      });
      my.afterJquery = function(fn) {
        return fn();
      };
      results = [];
      for (i = 0, len = afterWaits.length; i < len; i++) {
        fn = afterWaits[i];
        results.push(fn());
      }
      return results;
    });
    return parent;
  };

  window.WebOmi = constsExt($, window.WebOmi || {});

  window.jqesc = function(mySel) {
    return '#' + mySel.replace(/(:|\.|\[|\]|,|\/)/g, "\\$1");
  };

  String.prototype.trim = String.prototype.trim || function() {
    return String(this).replace(/^\s+|\s+$/g, '');
  };

}).call(this);
