$(function () {

	function goToByScroll(id){
	      // Remove "link" from the ID
	    id = id.replace("link", "");
	      // Scroll
	    $('html,body').delay(1000).animate({
	        scrollTop: $("#"+id).offset().top},
	        2000);
	    do_blink($('#'+id + "-header"),5);
	}
	
	function do_blink(elm, i){
		  if(i<=0){ elm.fadeIn(500); return;}
		  elm.fadeIn(500, function(){
		    $(this).fadeOut();
		    do_blink(elm, --i);
		  });
		}
	
    $('#fileupload').fileupload({
    	maxNumberOfFiles: 1 ,
    	singleFileUploads: false ,
        dataType: 'json',
        
        done: function (e, data) {
        	$("tr:has(td)").remove();
            $.each(data.result, function (index, file) {
            	
            	
                $("#uploaded-files").append(
                		$('<tr/>')
                		.append($('<td/>').text(file.fileName))
                		.append($('<td/>').text(file.fileSize))
                		.append($('<td/>').text(file.fileType))
                		.append($('<td/>').html("<a href='upload?f="+index+"'>Click</a>"))
                		.append($('<td/>').text("@"+file.twitter))

                		)//end $("#uploaded-files").append()
            });
            goToByScroll("step2");
        },
        
        progressall: function (e, data) {
	        var progress = parseInt(data.loaded / data.total * 100, 10);
	        $('.progress-bar').css(
	            'width',
	            progress + '%'
	        );
   		},
   		
		dropZone: $('#dropzone')
    }).bind('fileuploadsubmit', function (e, data) {
        // The example input, doesn't have to be part of the upload form:
        var twitter = $('#twitter');
        data.formData = {twitter: twitter.val()};        
    });
    
    $.ajax({
        url: "../indoor-positioning/upload?getfiles=true",
        type: 'GET',
        contentType: "json"
    	})
    	.done(function (data) {
    		$("tr:has(td)").remove();
            $.each(data, function (index, file) {
            	
            	
                $("#uploaded-files").append(
                		$('<tr/>')
                		.append($('<td/>').text(file.fileName))
                		.append($('<td/>').text(file.fileSize))
                		.append($('<td/>').text(file.fileType))
                		.append($('<td/>').html("<a href='upload?f="+index+"'>Click</a>"))
                		.append($('<td/>').text("@"+file.twitter))

                		)//end $("#uploaded-files").append()
            }); 
    	 });
 
    $("#start-processing").click(function(){
    	$("#processResultmsg").hide();
    	$("#start-processing").html("Processing started");
    	$("#start-processing").attr("disabled", true);
    	var query = "?space=" + $(".space").val() + "&obstacles=" + $(".obstacles").val() + "&points=" + $(".points").val(); 
    	var promise = $.ajax({url: "../indoor-positioning/api/indoor/processGeoJsons" + query , dataType: "json"});
    	$(".spinner").css('visibility', 'visible');
        
    	promise.done(function (data) {
    		if(typeof(Storage) !== "undefined") {
    			localStorage.setItem("heatmap-uuid", data.uuid);
    		} else {
    			window["heatmap-uuid"] = data.uuid;
    			alert("Sorry! No Web Storage support..")
    		    // Sorry! No Web Storage support..
    		}
        	$(".spinner").css('visibility', 'hidden');
        	$("#start-processing").html("Start Processing");
        	$("#start-processing").attr("disabled", false);
        	$("#processResultmsg").removeClass("alert-danger");
        	$("#processResultmsg").addClass("alert-success");
        	$("#processResultmsgText").html("Files have been succesfully proccessed!");
        	$("#processResultmsg").fadeIn(1000);
        	$("#processResultmsg").delay(2000).fadeOut(1000);
        	goToByScroll("step3");
        });
    	
    	promise.fail(function (data) {
    		$(".spinner").css('visibility', 'hidden');
        	$("#processResultmsg").addClass("alert-danger");
        	$("#processResultmsgText").html(data.statusText + ", Please contact Administrator");
        	$("#processResultmsg").fadeIn(1000);
        	$("#processResultmsg").delay(2000).fadeOut(1000);
        	$("#start-processing").html("Start Processing");
        	$("#start-processing").attr("disabled", false);
    	});
    
    });
    
    $("#generate-heatmap").click(function(){
    	
    	var uuid =  (localStorage.getItem("heatmap-uuid") !== null) ? localStorage.getItem("heatmap-uuid"):window["heatmap-uuid"];
    	//var promise = $.ajax({url: "../indoor-positioning/api/indoor/generateHeatMap/" + uuid, dataType: "json"});
    	var promise = $.ajax({url: "/indoor-positioning/json/heatmap.0.625.json" , dataType: "json"});
    	var promiseObstacles = $.ajax({url: "/indoor-positioning/json/obstacles.json" , dataType: "json"});
    	
    	promise.done(function (data) {

    		var heatData = {
    		          max: data.maximumHeat,
    		          data: data.heatmapDataArray
    		        };
    		        var baseLayer = L.tileLayer(
    		          '',{
    		            attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://cloudmade.com">CloudMade</a>',
    		            maxZoom: 18
    		          }
    		        );
    		        var cfg = {
    		          // radius should be small ONLY if scaleRadius is true (or small radius is intended)
    		          "radius": 2,//0.625,
    		          "maxOpacity": 0.75, 
    		          "scaleRadius": true, 
    		          "useLocalExtrema": true,
    		          latField: 'lat',
    		          lngField: 'lng',
    		          valueField: 'count'
    		        };
    		        var heatmapLayer = new HeatmapOverlay(cfg);
    		        var map = new L.Map('map', {
    		          center: [ -18.847, -10.813 ],
    		          zoom: 1.3,
    		          layers: [baseLayer, heatmapLayer]
    		        });
    		        
    		        heatmapLayer.setData(heatData);
    		        // make accessible for debugging
    		        layer = heatmapLayer;
    		    	promiseObstacles.done(function (data) {
    		    		L.geoJson(data).addTo(map);
    		    	});
        });
    	
    });
   
    
    $("#generate-pathmap").click(function(){
    	
    	var uuid =  (localStorage.getItem("heatmap-uuid") !== null) ? localStorage.getItem("heatmap-uuid"):window["heatmap-uuid"];
    	//var promise = $.ajax({url: "../indoor-positioning/api/indoor/generateDevicesPaths/" + uuid, dataType: "json"});
    	var promise = $.ajax({url: "/indoor-positioning/json/pathmap.0.625.json" , dataType: "json"});
    	var promiseObstacles = $.ajax({url: "/indoor-positioning/json/obstacles.json" , dataType: "json"});
    	
    	promise.done(function (data) {

    		var map = new L.Map('pathMap', {
    	        center: [ -18.847, -10.813 ],
    	        zoom: 1.3,
    	        layers: [
    	            new L.TileLayer('', {
    	                attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://cloudmade.com">CloudMade</a>',
    	                maxZoom: 18
    	            })
    	        ]
    	    });
    		
    		var plArray = [];
    		for(var i=0; i<data.DevicesPathData.length; i++) {
    			for(var j=0; j<data.DevicesPathData[i].length; j++) {
    				plArray.push(L.polyline(data.DevicesPathData[i][j].coords,{weight: data.DevicesPathData[i][j].rate , opacity:1}).addTo(map));
    			}
    	    }
    		
    		promiseObstacles.done(function (data) {
	    		L.geoJson(data).addTo(map);
	    	});
    		
        });
    	
    });
   
    
    
});