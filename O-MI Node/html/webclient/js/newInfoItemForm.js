// Generated by CoffeeScript 1.9.3
(function() {
  (function(consts) {
    var cloneAbove;
    cloneAbove = function() {
      var model, that;
      that = $(this);
      model = that.prev().clone();
      model.find("input").val("");
      return that.prev().after(model);
    };
    return consts.afterJquery(function() {
      return $('.btn-clone-above').on('click', cloneAbove);
    });
  })(WebOmi.consts);

}).call(this);
