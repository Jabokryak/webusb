package ml_pkg.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.math.BigDecimal;
import java.util.HexFormat;

public class Led {
	private short       id;
	private String      color_name;
	private byte[]      color_rgba;
	private BigDecimal  wavelength;
	private short       slot_number;
	private String      code;
	private String      error;

	public Led(short id, String color_name, byte[] color_rgba, BigDecimal wavelength, short slot_number, String code) {
		this.id             = id;
		this.color_name     = color_name;
		this.color_rgba     = color_rgba;
		this.wavelength     = wavelength;
		this.slot_number    = slot_number;
		this.code           = code;
	}

	public Led(short id, short slot_number, String code) {
		this.id             = id;
		this.slot_number    = slot_number;
		this.code           = code;
	}

	public Led() {
		this.id             = -1;
		this.slot_number    = -1;
	}

	public Led(String error) {
		this.id             = 0;
		this.slot_number    = 0;
		this.error          = error;
	}

	public JsonNode getJsonNode() {
		return JsonNodeFactory.instance.objectNode()
			.put("id", this.id)
			.put("color_name", this.color_name)
			.put("color_rgba", this.color_rgba != null ? HexFormat.of().formatHex(this.color_rgba) : null)
			.put("wavelength", this.wavelength)
			.put("slot_number", this.slot_number)
			.put("code", this.code);
	}

	public short getId() {
		return id;
	}

	public void setId(short id) {
		this.id = id;
	}

	public String getColor_name() {
		return color_name;
	}

	public void setColor_name(String color_name) {
		this.color_name = color_name;
	}

	public byte[] getColor_rgba() {
		return color_rgba;
	}

	public void setColor_rgba(byte[] color_rgba) {
		this.color_rgba = color_rgba;
	}

	public BigDecimal getWavelength() {
		return wavelength;
	}

	public void setWavelength(BigDecimal wavelength) {
		this.wavelength = wavelength;
	}

	public short getSlot_number() {
		return slot_number;
	}

	public void setSlot_number(short slot_number) {
		this.slot_number = slot_number;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getError() {return error;}

	public void setError(String error) {this.error = error;}
}
