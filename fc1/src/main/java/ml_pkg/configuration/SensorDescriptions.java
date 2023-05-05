package ml_pkg.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

@Component
public class SensorDescriptions {
	@Autowired
	private R2dbcEntityTemplate template;

	public record SensorDescription(
		short id
		,short sensor_id
		,short position
	) {};

	private HashMap<Short, SensorDescription> descMap;

	public SensorDescription getSensorDescription(int p_position) {
		return descMap.get((short)p_position);
	}

	@PostConstruct
	private void postConstruct() {
		findActiveSensorDescriptions();
	}

	public void findActiveSensorDescriptions() {
		try {
			this.descMap = template.getDatabaseClient().sql("""
					select
						d.id
						,r.sensor_id
						,d.data_position_in_dataset
					from
						sensor_description d
						inner join sensor_resolution r
							on r.id = d.sensor_resolution
					where
						d.is_active
					order by
						d.data_position_in_dataset"""
				)
				.map(r -> new SensorDescription(
					r.get("id", Short.class)
					, r.get("sensor_id", Short.class)
					, r.get("data_position_in_dataset", Short.class)
				))
				.all()
				.collect(HashMap<Short, SensorDescription>::new, (m, r) -> m.put(r.position, r))
				.toFuture()
				.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
