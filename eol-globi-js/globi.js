globi = function() {
	var globi = {
		version: "0.0.1"
	};
	var url_prefix = "http://trophicgraph.com:8080";

	globi.add_taxon_info = function(scientific_name, div_id, on_click_scientific_name_callback) {
		img_callback = function(error, json) {
			if (!error) {
				if (json.thumbnailURL) {
					img_div = d3.select(div_id)
					.append("span")

					table = img_div.append("table");

					if (json.commonName && json.scientificName && json.infoURL) {
						if (on_click_scientific_name_callback) {
							img_div
							.on("click", function(d) {
								on_click_scientific_name_callback(json.scientificName);
							});
						}

						table.append("tr").append("td")
						.append("img")
						.attr("src", json.thumbnailURL);

						table.append("tr").append("td")
						.text(json.commonName)
						.append("a")
						.attr("href", json.infoURL)
						.attr("target", "_blank")
						.text(" >");
						

						scientific_name_td = table.append("tr").append("td")
						scientific_name_td.html("<i>" + json.scientificName + "</i>");
					}	
				} 
			} 
		};
		d3.json(url_prefix + "/imagesForName/" + encodeURIComponent(scientific_name), img_callback);
	};

	globi.view_interactions = function(div_id, interaction_type, source_target_name, interaction_description, on_click_scientific_name_callback) {
		var uri = url_prefix + "/taxon/" + encodeURIComponent(source_target_name) + "/" + interaction_type;

		d3.json(uri, function(error, json) {
			if (!error) {
				var html_text = "<b>" + interaction_description + "</b>";
				if (json.data && json.data.length == 0) {
					html_text += " <b> nothing</b>";
				}
				d3.select(div_id).html(html_text);
				for (var i = 0; json.data && i <  json.data.length; i++) {
					globi.add_taxon_info(json.data[i], div_id, on_click_scientific_name_callback);
				};				
			}
		});
	};
	return globi;
}();
