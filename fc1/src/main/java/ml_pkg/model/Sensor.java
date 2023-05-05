package ml_pkg.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class Sensor {
	private short       id;
	private String      description;
	private String      sensor_model;
	private String      error;

	public Sensor(short id, String description, String sensor_model) {
		this.id             = id;
		this.description    = description;
		this.sensor_model   = sensor_model;
	}

	public Sensor(String error) {
		this.id             = 0;
		this.error          = error;
	}

	public JsonNode getJsonNode() {
		return JsonNodeFactory.instance.objectNode()
			.put("id",			this.id)
			.put("description",	this.description)
			.put("sensor_model",	this.sensor_model);
	}

	public short getId() {
		return id;
	}
	public void setId(short id) {
		this.id = id;
	}

	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public String getSensor_model() {
		return this.sensor_model;
	}
	public void setSensor_model(String sensor_model) {
		this.sensor_model = sensor_model;
	}

	public String getError() {return this.error;}
	public void setError(String error) {this.error = error;}
}
