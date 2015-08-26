var requestEditor, responseEditor;
var timeout; //Used for request generation timeout
var requestInterval = 1000; // Interval in milliseconds for automatic request generation
var generating = false; // TODO: used as global variable, see submit.js
var iconValue;

/**
 * Loads form options (text input fields) on page 2
 */
function loadOptions() {
	iconValue = $("#icons").find(".selected").attr("alt");
	$("#options").empty();
	$("#options").load("forms/" + iconValue + ".html");
	
	if(omi){
		var save = omi.save[iconValue];
		if(save){
			setTimeout(function(){
				if(save.ttl) $("#ttl").val(save.ttl); 
				if(save.interval) $("#interval").val(save.interval);
				if(save.begin) $("#begin").val(save.begin);
				if(save.end) $("#end").val(save.end);
				if(save.newest) $("#newest").val(save.newest);
				if(save.oldest) $("#oldest").val(save.oldest);
				if(save.callback) $("#callback").val(save.callback);
				if(save.requestID) {
					for(var i = 0; i < save.requestID.length; i++){
						if(i >= $(".requestID").length){
							createInput($($(".requestID")[i - 1]).parent());
						}
						$($(".requestID")[i]).val(save.requestID[i]);
					}
				}
			}, 50);
		}
	}
}
function refreshEditor(editor, id) {
	if (editor === "request") {
		$("#edit .CodeMirror").remove();
		
		requestEditor = CodeMirror.fromTextArea(document.getElementById(id), {
			mode : "application/xml",
			lineNumbers : true,
			lineWrapping: true
		});
		requestEditor.refresh();
	} else if (editor === "response") {
		$("#response .CodeMirror").remove();
		
		responseEditor = CodeMirror.fromTextArea(document.getElementById(id), {
			mode : "application/xml",
			lineNumbers : true,
			lineWrapping: true
		});
		responseEditor.refresh();
	}
}

/**
 * Loads the previous and the next page of the current page
 * @param {Number} page The current page index
 */
function loadSides(page) {
	var prev = $("#page" + (page - 1));
	var next = $("#page" + (page + 1));

	// If previous page exists
	if (prev) {
		if (prev.is(':empty')) {
			prev.load("pages/page" + (page - 1) + ".html");
		}
		prev.removeClass("currentpage behind");
		prev.addClass("prevpage hidden");
		prev.prop("disabled", true);
	}
	
	// If next page exists
	if (next) {
		if (next.is(':empty')) {
			next.load("pages/page" + (page + 1) + ".html");
		}
		next.removeClass("currentpage behind");
		next.addClass("nextpage hidden");
		next.prop("disabled", true);
	}
}

/**
 * sets a timeout to automatically update the request
 * @param {number} interval the time waited before generation (in millis)
 */
function updateRequest(interval){
	// if automatic update allowed by user
	if($("#autorequest").prop("checked") || interval === 0){
		generating = true; //prop generation
		
		$("#editrequest .codemirror").hide();
			
		if(timeout){
			clearTimeout(timeout);
		}
		
		if(interval === 0){
			generateRequest(); // from generate.js
			refreshEditor("request", "editRequest");
			generating = false; //generation done
		} else {
			// show loading "animation"
			var loadSelector = $("#edit .loading");
			loadSelector.show();
			
			// set timeout
			timeout = setTimeout(function(){
				generateRequest(); // from generate.js
				refreshEditor("request", "editRequest");
				
				// hide loading "animation"
				loadSelector.hide();
				
				$("#editrequest .codemirror").show();
				generating = false; //generation done
			}, interval);
		}
	}
}


//Page 2 definitions
$(document).on('click', '.icon', function() {
	if(!$(this).hasClass("selected")){
		$(".icon").removeClass("selected");
		$(this).addClass("selected");
		loadOptions();
		updateRequest(requestInterval);
	}
});

$(document).on("click", "#autorequest", function(){
	updateRequest(requestInterval);
});

$(document).on("click", "#generate", function(){
	updateRequest(requestInterval);
});

$(document).on("click", "#addbutton", function(){
	createInput($(this).parent().children().last());
});

/**
 * Loads pages from separate html files
 * @param {Number} page The current page index
 */
function loadPages(page) {
	$(".page").addClass("behind");

	var selector = $("#page" + page);

	// Preload previous/next pages
	loadSides(page);

	selector.removeClass("prevpage nextpage hidden behind");
	selector.addClass("currentpage");
	selector.prop("disabled", false);
	
	// Load pages 
	if (selector.is(':empty')) {
		selector.load("pages/page" + page + ".html");
		
		if(page === 1){
			// setTimeout(getObjects, 500);
			setTimeout(dataDiscovery, 500);
		}
	}
	
	// Load operation options (page 2)
	if (page === 2) {
		if($('#options').is(':empty')){
			loadOptions();
		}
		$("#options").change(function(){
			updateRequest(requestInterval);
		});
	}
	if(page === 3){
		refreshEditor("response", "responseBox");
	}
}

/**
 * Refreshes codemirror editor
 * @param {string} editor the selected codemirror editor string ("request"/"response")
 * @param {string} id the id of the text area used by the editor
 */


/**
 * Clone an existing DOM element and append it after the element
 * @param {Object} elem The element to be cloned
 */
function createInput(elem) {
	var clone = $(elem).clone();
	$(elem).parent().append(clone);
	clone.val("");
}