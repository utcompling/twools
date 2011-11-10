package fogbow.textgrounder;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Object representing a status.
 */
public class Status {
  private static final JsonParser parser = new JsonParser();
  private static final SimpleDateFormat twitterFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss +0000 yyyy");
  private static final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

  private String id;
  private String userId;
  private String screenname;
  private String createdAt;
  private String lat;
  private String lon;
  private String text;
  private int httpStatusCode;
  private JsonObject jsonObject;
  private String jsonString;
  private boolean geoBool;

  protected Status() {}

  public boolean hasGeo() {return geoBool; }
  public String getId() { return id; }
  public String getUserId() { return "USER_" + userId; }
  public String getText() { return text; }
  public String getLat() { return lat; }
  public String getLon() { return lon; }
  public String getCoord() { return "??:" + lat + "," + lon; }
  public String getScreenname() { return screenname; }
  public int getHttpStatusCode() { return httpStatusCode; }
  public JsonObject getJsonObject() { return jsonObject; }
  public String getJsonString() { return jsonString; }
  public String getCreatedAt() throws ParseException {
    return isoFormat.format(twitterFormat.parse(createdAt));
  }

  public static Status fromJson(String json) throws Exception {
	Preconditions.checkNotNull(json);

	JsonObject obj = (JsonObject) parser.parse(json);
	JsonObject user = obj.get("user").getAsJsonObject();

	Status status = new Status();
	status.text = obj.get("text").getAsString();
	status.id = obj.get("id_str").getAsString();
	status.userId = user.get("id_str").getAsString();
	status.screenname = user.get("screen_name").getAsString();
	status.createdAt = obj.get("created_at").getAsString();
	status.geoBool = false;
	
	double lat, lon;
	lat = lon = 0; 
	
	if (!obj.get("geo").isJsonNull()) {
		JsonObject geo = obj.get("geo").getAsJsonObject();
        JsonArray coords = geo.get("coordinates").getAsJsonArray();
        
		if(geo.get("type").getAsString().equals("Point")) {
			lat = coords.get(0).getAsDouble();
			lon = coords.get(1).getAsDouble();
			status.geoBool = true;
		}
		else if(geo.get("type").getAsString().equals("Polygon")) {
			
			JsonArray coordP = coords.get(0).getAsJsonArray();
			
			//Must be a quadralateral
			if (coordP.size() == 4) {
				
				JsonArray pt1 = (JsonArray) coordP.get(0);
				//JsonArray pt2 = (JsonArray) coordP.get(1);
				JsonArray pt3 = (JsonArray) coordP.get(2);
				//JsonArray pt4 = (JsonArray) coordP.get(3);
				
				double diag = getDistance(pt1, pt3);
				
				//Small Area
				if( diag < .5) {
				
					for(int i = 0; i < coordP.size(); i++) {
						JsonArray coord = (JsonArray)coordP.get(i);
						lat += coord.get(0).getAsDouble();
						lon += coord.get(1).getAsDouble();
					}
					lat = lat / coordP.size();
					lon = lon / coordP.size();
					
					status.geoBool = true;
				}
			}
		}
		else
			System.out.println("Not Point or Polygon");
		
		status.lat = ""+lat;
		status.lon = ""+lon;
	}
	else if(!obj.get("place").isJsonNull()) {
		JsonObject boundingbox = obj.get("place").getAsJsonObject().get("bounding_box").getAsJsonObject();
		JsonArray coords = boundingbox.get("coordinates").getAsJsonArray();
		

		
		if(boundingbox.get("type").getAsString().equals("Point")) {
			lat = coords.get(0).getAsDouble();
			lon = coords.get(1).getAsDouble();
			status.geoBool = true;
		}
		else if(boundingbox.get("type").getAsString().equals("Polygon")) {
		
			JsonArray coordP = coords.get(0).getAsJsonArray();
			
			//Must be a quadralateral
			if (coordP.size() == 4) {
				
				JsonArray pt1 = (JsonArray) coordP.get(0);
				//JsonArray pt2 = (JsonArray) coordP.get(1);
				JsonArray pt3 = (JsonArray) coordP.get(2);
				//JsonArray pt4 = (JsonArray) coordP.get(3);
				
				double diag = getDistance(pt1, pt3);
				
				//Small Area
				if( diag < .5) {
				
					for(int i = 0; i < coordP.size(); i++) {
						JsonArray coord = (JsonArray)coordP.get(i);
						lat += coord.get(0).getAsDouble();
						lon += coord.get(1).getAsDouble();
					}
					lat = lat / coordP.size();
					lon = lon / coordP.size();
					
					status.geoBool = true;
				}
			}
		}
		else
			System.out.println("Not Point or Polygon");
		
		status.lat = ""+lat;
		status.lon = ""+lon;
	}

	// TODO: We need to parse out the other fields.
	status.jsonObject = obj;
	status.jsonString = json;

	return status;
  }
  
  public static double getDistance(JsonArray pt1, JsonArray pt2) {
  	double d = Math.sqrt(
  			Math.pow(pt1.get(0).getAsDouble() - pt2.get(0).getAsDouble(), 2) + 
  			Math.pow(pt1.get(1).getAsDouble() - pt2.get(1).getAsDouble(), 2)
  			);
  			
  	return d;
  }
  
}
