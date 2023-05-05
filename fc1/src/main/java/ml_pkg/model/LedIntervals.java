package ml_pkg.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;

public class LedIntervals {
	private short id;
	private short led_id;
	private short slot_number;
	private ArrayList<Integer> on_points;

	public LedIntervals(short id, short led_id, short slot_number, ArrayList<Integer> on_points) {
		System.out.println(
			String.format("LedIntervals.constructor id = %d, led_id = %d, slot_number = %d, on_points = %s"
				,id
				,led_id
				,slot_number
				,Arrays.toString(on_points.toArray(new Integer[0]))
			)
		);

		this.id = id;
		this.led_id = led_id;
		this.slot_number = slot_number;
		this.on_points = on_points;
	}

	public short getId() {
		return id;
	}

	public void setId(short id) {
		this.id = id;
	}

	public short getLed_id() {
		return led_id;
	}

	public void setLed_id(short led_id) {
		this.led_id = led_id;
	}

	public short getSlot_number() {
		return slot_number;
	}

	public void setSlot_number(short slot_number) {
		this.slot_number = slot_number;
	}

	public ArrayList<Integer> getOn_points() {
		return on_points;
	}

	public void setOn_points(ArrayList<Integer> on_points) {
		this.on_points = on_points;
	}

	public JsonNode getJsonNode() {
		System.out.println("LedIntervals.getJsonNode"
			+ " id = "				+ this.id
			+ ", led_id = "			+ this.led_id
			+ ", slot_number = "	+ this.slot_number
			+ ", on_points = "		+ this.on_points.toString()
		);

		ObjectNode ret = JsonNodeFactory.instance.objectNode()
			.put("id", this.id)
			.put("led_id", this.led_id)
			.put("slot_number", this.slot_number);

		if (this.on_points != null && this.on_points.size() > 0) {
			ArrayNode arrNode = JsonNodeFactory.instance.arrayNode(this.on_points.size());

			this.on_points.forEach(arrNode::add);

			ret.putArray("on_points").addAll(arrNode);
		}

		return ret;
	}
}
