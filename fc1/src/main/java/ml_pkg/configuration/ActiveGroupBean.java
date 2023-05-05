package ml_pkg.configuration;

import ml_pkg.model.IntervalsGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class ActiveGroupBean {
	@Autowired
	private R2dbcEntityTemplate template;

	private Short id;
	private byte onSensorsPointNumber;

	public Short getId() {
		if (this.id == null) {
			try {
				template.getDatabaseClient().sql("""
     				select
     					g.id
     					,array_length(si.time_points, 1) l
					from
						intervals_group g
						left join  sensor_intervals si
							on si.group_id = g.id
					where
						g.is_active"""
				)
					.map(row -> {
						this.id						= row.get("id", Short.class);
						this.onSensorsPointNumber	= row.get("l", Byte.class);

						return this.id;
					})
					.one()
					.toFuture()
					.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

		return this.id;
	}

	public void set(Short r, byte sensor_intervals_number) {
		this.id = r;
		this.onSensorsPointNumber = sensor_intervals_number;
	}

	public void set(IntervalsGroup g) {
		if (g == null)
			this.set((Short) null, (byte) 0);
		else
			this.set(g.getId(), g.getSensor_intervals_quantity());
	}

	public byte getOnSensorsPointNumber() {
		getId();		//Заполним данными, если id = null

		System.out.println("ActiveGroupBean.getOnSensorsPointNumber id = " + this.id + ", onSensorsPointNumber = " + this.onSensorsPointNumber);

		return this.onSensorsPointNumber;
	}

}
