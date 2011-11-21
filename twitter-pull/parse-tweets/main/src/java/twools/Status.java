package twools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Singleton Object representing a status.
 */
public class Status {
	private static final JsonParser parser = new JsonParser();
	private static final SimpleDateFormat twitterFormat = new SimpleDateFormat(
			"EEE MMM dd HH:mm:ss +0000 yyyy");
	private static final SimpleDateFormat isoFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm'Z'");

	private static Status status = new Status();

	private int httpStatusCode;
	private String id;
	private String userId;
	private String screenname;
	private String createdAt;
	private String text;
	private final ArrayList<String> mentionUsers = new ArrayList<String>();;
	private double lat;
	private double lon;
	private boolean geoBool;
	private JsonObject obj;

	protected Status() {

	}

	public boolean hasGeo() {
		return status.geoBool;
	}

	public String getId() {
		return status.id;
	}

	public String getUserId() {
		return "USER_" + status.userId;
	}

	public String getText() {
		return status.text;
	}

	public String getLat() {
		return Double.toString(status.lat);
	}

	public String getLon() {
		return Double.toString(status.lon);
	}

	public String getCoord() {
		return status.lat + "," + status.lon;
	}

	public String getScreenname() {
		return status.screenname;
	}

	public int getHttpStatusCode() {
		return status.httpStatusCode;
	}

	public String getUserMentions() {
		if (mentionUsers.isEmpty()) {
			return "NONE";
		} else {
			String users = "";
			for (String user : mentionUsers) {
				users += "," + user;
			}
			return users.substring(1);
		}
	}

	public String getCreatedAt() throws ParseException {
		return isoFormat.format(twitterFormat.parse(createdAt));
	}

	public static Status fromJson(String json, float diagSize) throws Exception {
		Preconditions.checkNotNull(json);

		JsonObject obj = (JsonObject) parser.parse(json);
		if (!obj.isJsonNull()) {
			JsonObject user = obj.get("user").getAsJsonObject();
			status.obj = obj;
			status.text = obj.get("text").getAsString()
					.replaceAll("\\r\\n|\\r|\\n", " ");// removing new lines
			status.id = obj.get("id_str").getAsString();
			status.userId = user.get("id_str").getAsString();
			status.screenname = user.get("screen_name").getAsString();
			status.createdAt = obj.get("created_at").getAsString();
			status.getGeolocation(diagSize);
			status.getMentions();

			return status;
		} else {
			return null;
		}
	}

	// Gets the mentions from a user
	private void getMentions() {
		mentionUsers.clear();
		if (!obj.get("entities").isJsonNull()) {
			if (obj.get("entities").getAsJsonObject().get("user_mentions") != null) {
				JsonArray users = obj.get("entities").getAsJsonObject()
						.get("user_mentions").getAsJsonArray();

				for (JsonElement user : users) {
					mentionUsers.add(user.getAsJsonObject().get("id_str")
							.getAsString());
				}
			}

		}

	}

	// Given two JsonArray points, get the distance between them
	private double getDistance(JsonArray pt1, JsonArray pt2) {
		double d = Math.sqrt(Math.pow(pt1.get(0).getAsDouble()
				- pt2.get(0).getAsDouble(), 2)
				+ Math.pow(pt1.get(1).getAsDouble() - pt2.get(1).getAsDouble(),
						2));
		return d;
	}

	// Validate that lat is between -90 and 90
	// and long is between -180 and 180
	private boolean validateCoords(double lat, double lon) {
		if (Math.abs(lat) <= 90 && Math.abs(lon) <= 180)
			return true;
		return false;
	}

	// Get the geolocation of a tweet
	// diagSize corresponds to if the geo coordiantes are in a boundng box, how
	// small the box is...
	private void getGeolocation(float diagSize) {
		double lat, lon;
		lat = lon = 0;
		JsonObject geoTag = null;
		JsonArray coords = null;

		// Geo Location
		if (!obj.get("geo").isJsonNull()) {
			geoTag = obj.get("geo").getAsJsonObject();
			coords = geoTag.get("coordinates").getAsJsonArray();
		} else if (!obj.get("place").isJsonNull()) {
			geoTag = obj.get("place").getAsJsonObject().get("bounding_box")
					.getAsJsonObject();
			coords = geoTag.get("coordinates").getAsJsonArray();
		} else
			return;

		// Point or Polygon
		if (geoTag.get("type").getAsString().equals("Point")) {
			lat = coords.get(0).getAsDouble();
			lon = coords.get(1).getAsDouble();
		} else if (geoTag.get("type").getAsString().equals("Polygon")) {
			JsonArray coordPoints = coords.get(0).getAsJsonArray();

			// Must be a quadralateral
			if (coordPoints.size() == 4) {

				JsonArray pt1 = (JsonArray) coordPoints.get(0);
				JsonArray pt3 = (JsonArray) coordPoints.get(2);

				// Small Area
				if (getDistance(pt1, pt3) < diagSize) {

					for (int i = 0; i < coordPoints.size(); i++) {
						JsonArray coord = (JsonArray) coordPoints.get(i);
						lat += coord.get(1).getAsDouble();
						lon += coord.get(0).getAsDouble();
					}

					lat = lat / coordPoints.size();
					lon = lon / coordPoints.size();

				} else {
					status.geoBool = false;
					return;
				}
			}

		} else {
			System.err.println(geoTag.get("type").getAsString()
					+ " is Not Point or Polygon");
			return;
		}

		if (validateCoords(lat, lon)) {
			// Update the lat and lon
			status.lat = lat;
			status.lon = lon;
			status.geoBool = true;
		} else {
			System.err.println("Lat " + lat + " and Long " + lon
					+ " are not in the proper range.");
			status.geoBool = false;
		}
	}
}
