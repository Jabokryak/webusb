package ml_pkg.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public record SensorData(
	long id
	,short sensor_id
	,short sensor_description
	,short intervals_group
	,short x
	,short y
	,short z
	,short error
	,short status_register
	,short sensor_interval_number
	,byte sensor_intervals_quantity
) {
	public JsonNode getJsonNode() {
		return JsonNodeFactory.instance.objectNode()
			.putArray(null)
				//.add(this.id)
				//.add(this.sensor_id)
				//.add(this.sensor_description)
				//.add(this.intervals_group)
				.add(this.x)
				.add(this.y)
				.add(this.z)
				.add(this.error)
				.add(this.status_register)
				.add(this.sensor_interval_number);
	}

	/*public JsonNode getArrayJsonNode() {
		ArrayList<JsonNode> nodeArr = new ArrayList(arr.size());

		arr.forEach(msg -> nodeArr.add(msg.asJson()));

		return JsonNodeFactory.instance.objectNode().putArray(null).addAll(nodeArr);
	}*/
}
