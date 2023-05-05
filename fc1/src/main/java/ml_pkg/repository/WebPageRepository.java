package ml_pkg.repository;

import ml_pkg.configuration.ActiveGroupBean;
import ml_pkg.configuration.MessageFluxBean;
import ml_pkg.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

@Repository
public class WebPageRepository {
	private final TransactionalOperator transactionalOperator;

	@Autowired
	private R2dbcEntityTemplate template;

	@Autowired
	private ActiveGroupBean activeGroupId;

	@Autowired
	private MessageFluxBean messageFlux;

	public WebPageRepository(ReactiveTransactionManager transactionManager) {
		this.transactionalOperator = TransactionalOperator.create(transactionManager);
	}

	public Flux<Led> findAllLeds() {
		return template.getDatabaseClient().sql("select * from led q order by q.slot_number")
			.map(row -> new Led(
				row.get("id", Short.class)
				, row.get("color_name", String.class)
				, row.get("color_rgb", byte[].class)
				, row.get("wavelength", BigDecimal.class)
				, row.get("slot_number", Short.class)
				, row.get("code", String.class)
			))
			.all();
	}

	public Flux<Sensor> findActiveSensors() {
		return template.getDatabaseClient().sql("select s.id, s.description, s.sensor_model from sensor s order by s.id")
			.map(row -> new Sensor(
				row.get("id", Short.class)
				,row.get("description", String.class)
				,row.get("sensor_model", String.class)
			))
			.all();
	}

	public Mono<IntervalsGroup> findActiveGroup(Answer answ) {
		System.out.println("findActiveGroup");

		return template.getDatabaseClient().sql("select * from intervals_group g where g.is_active")
			.map(row -> {
				short id = row.get("id", Short.class);

				IntervalsGroup activeGroup = new IntervalsGroup(
					id
					, row.get("one_tick_length", Integer.class)
					, row.get("pixels_per_tick", Short.class)
					, row.get("number_of_ticks", Short.class)
					, new ArrayList<>()
					, new SensorIntervals((short) 0, new ArrayList<>())
				);

				System.out.println("Map. Active group found id = " + id);

				return activeGroup;
			})
			.one()
			.doOnNext(r ->
				answ.add(
					"FINDACTIVEGROUP_INTERVALSGROUP_SELECT_DONE"
					, "Active group found id = " + r.getId()
				)
			)
			.doOnError(e ->
				answ.addError(
					"FINDACTIVEGROUP_INTERVALSGROUP_SELECT_ERR"
					, "An error occurred while selecting active group: " + e.getMessage()
				)
			)
			.flatMap(activeGrp ->
				template.getDatabaseClient().sql("select * from led_intervals l where l.group_id = $1")
					.bind("$1", activeGrp.getId())
					.map(row -> {
						short id = row.get("id", Short.class);
						Integer[] on_points_arr = row.get("on_points", Integer[].class);

						System.out.println("on_points_arr = " + on_points_arr.toString());

						LedIntervals ledIntv = new LedIntervals(
							id
							,row.get("led_id", Short.class)
							,row.get("slot_number", Short.class)
							,new ArrayList<>(Arrays.asList(on_points_arr))
						);

						activeGrp.getLed_intervals().add(ledIntv);

						System.out.println("Found led interval id = " + id);

						return ledIntv;
					})
					.all()
					.doOnNext(q ->
						answ.add(
							"FINDACTIVEGROUP_LEDINTERVALS_SELECT_DONE"
							, "Found led interval id = " + q.getId()
						)
					)
					.doOnError(e -> {
						System.out.println("An error occurred while selecting led intervals: " + e.getMessage());

						answ.addError(
							"FINDACTIVEGROUP_LEDINTERVALS_SELECT_ERR"
							, "An error occurred while selecting led intervals: " + e.getMessage()
						);
					})
					.collectList()
					.map(w -> activeGrp)
					/*.defaultIfEmpty(
						((Supplier<IntervalsGroup>) () -> {
							System.out.println("Led intervals not found");

							return activeGrp;
						})
						.get()
					)*/
			)
			.flatMap(activeGrp ->
				template.getDatabaseClient().sql("""
     					select s.id, public.sort(s.time_points) time_points from sensor_intervals s where s.group_id = $1"""
					)
					.bind("$1", activeGrp.getId())
					.map(row -> {
						short id = row.get("id", Short.class);

						SensorIntervals sensorIntv = new SensorIntervals(
							id
							, new ArrayList<>(Arrays.asList(row.get("time_points", Integer[].class)))
						);

						activeGrp.setSensor_intervals(sensorIntv);

						System.out.println("Found sensor interval id = " + id);

						return sensorIntv;
					})
					.one()
					.doOnNext(q ->
						answ.add(
							"FINDACTIVEGROUP_SENSORINTERVALS_SELECT_DONE"
							,"Found sensor interval id = " + q.getId()
						)
					)
					.doOnError(e ->
						answ.addError(
							"FINDACTIVEGROUP_SENSORINTERVALS_SELECT_ERR"
							,"An error occurred while selecting sensor interval: " + e.getMessage()
						)
					)
					.map(r -> activeGrp)
					.switchIfEmpty(Mono.defer(() -> {
						System.out.println("Sensor intervals not found");

						answ.add(
							"FINDACTIVEGROUP_SENSORINTERVALS_SELECT_EMPTY"
							,"Sensor intervals not found"
						);

						return Mono.just(activeGrp);
					}))
			)
			.doOnSuccess(g -> activeGroupId.set(g))
			.switchIfEmpty(Mono.defer(() -> {
				System.out.println("Active group not found");

				answ.add(
					"FINDACTIVEGROUP_INTERVALSGROUP_SELECT_EMPTY"
					,"Active group not found"
				);

				return Mono.empty();
			}));
			/*.map(r -> {
				long start = System.currentTimeMillis();
				System.out.println("Try go to sleep, thread id: " + Thread.currentThread().getId());

				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Sleep time in ms = "+(System.currentTimeMillis()-start) + ", thread id: " + Thread.currentThread().getId());

				return r;
			});*/
			//.zipWith(Mono.deferContextual(ctx -> Mono.just((Answer) ctx.get(Answer.key))))
			//.contextWrite(Context.of(Answer.key, new Answer(3)));
	}

	public Mono<Answer> saveActiveGroup(IntervalsGroup tmpGroup, ArrayList<Led> tmpLeds, Answer answ) {
		System.out.println("saveActiveGroup in variables:"
			+ " OneTickLength = " + tmpGroup.getOne_tick_length()
			+ ", PixelsPerTick = " + tmpGroup.getPixels_per_tick()
			+ ", WholeLength = " + tmpGroup.getWhole_length());

		return template.getDatabaseClient().sql("""
				select g.id from intervals_group g where
					g.is_active
					and not exists(select 1 from sensor_data d where d.intervals_group = g.id)"""
			)
			.map(row -> row.get("id", Short.class))
			.one()
			.doOnNext(q -> {
					System.out.println("Found active intervals group without sensor data, id = " + q);

					tmpGroup.setId(q);

					answ.add(
						"SAVEACTIVEGROUP_FOUND_GROUP_WITHOUT_DATA"
						, "Found active intervals group without sensor data, id = " + q
					);
			})
			.flatMap(r ->
				template.getDatabaseClient().sql("""
						update intervals_group set
							one_tick_length = $1
							,pixels_per_tick = $2
							,number_of_ticks = $3
						where id = $4"""
					)
					.bind("$1", tmpGroup.getOne_tick_length())
					.bind("$2", tmpGroup.getPixels_per_tick())
					.bind("$3", tmpGroup.getWhole_length())
					.bind("$4", r)
					.map(q -> {
						answ.add(
							"SAVEACTIVEGROUP_GROUP_UPDATE"
							, "The group id = " + r + " has been updated"
						);

						System.out.println("Update intervals_group complete");

						return r;
					})
					.one()
					.defaultIfEmpty(r)
			)
			.flatMap(r ->
				template.getDatabaseClient().sql(
						"delete from led_intervals where group_id = $1"
					)
					.bind("$1", r)
					.map(c -> {
						answ.add(
							"SAVEACTIVEGROUP_DELETE_LEDINTERVALS"
							,"Led intervals in the amount " + c + " have been deleted, referenced group id = " + r
						);

						System.out.println("Delete led_intervals complete");

						return r;
					})
					.one()
					.doOnError(e ->
						answ.addError(
							"SAVEACTIVEGROUP_DELETE_LEDINTERVALS_ERR"
							,"An error occurred while deleting led intervals (group id = " + r + "): " + e.getMessage()
						)
					)
					.defaultIfEmpty(r)
			)
			.switchIfEmpty(
				template.getDatabaseClient().sql(
						"insert into intervals_group(one_tick_length, pixels_per_tick, number_of_ticks) values ($1, $2, $3)"
					)
					.filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
					.bind("$1", tmpGroup.getOne_tick_length())
					.bind("$2", tmpGroup.getPixels_per_tick())
					.bind("$3", tmpGroup.getWhole_length())
					.map(row -> {
						short id = (short) row.get("id", Short.class);

						tmpGroup.setId(id);

						System.out.println("Insert done, tmpGroup.id = " + id);

						answ.add(
							"SAVEACTIVEGROUP_INSERT_GROUP"
							,"Intervals group inserted, id = " + id
						);

						return id;
					})
					.one()
			)
			//Save led intervals
			.flatMap(r -> {
				if (tmpGroup.getLed_intervals().size() == 0)
					return Mono.just(r);

				return template.getDatabaseClient().inConnectionMany(connection -> {
						var statement = connection.createStatement("""
								insert into led_intervals (group_id, led_id, slot_number, on_points)
									values ($1, $2, $3, $4)"""
							)
							.returnGeneratedValues("id");

						for (int i = tmpGroup.getLed_intervals().size(), j = 0; i > 0; --i, j++) {
							var li = tmpGroup.getLed_intervals().get(j);

							statement
								.bind(0, r)
								.bind(1, li.getLed_id())
								.bind(2, li.getSlot_number())
								.bind(3, li.getOn_points().toArray(new Integer[0]));

							// for the last item, do not call `add`
							if (i > 1)
								statement.add();

							System.out.println(
								String.format("%d group_id = %d, led_id = %d, slot_number = %d, on_points = %s"
									,j
									,r
									,li.getLed_id()
									,li.getSlot_number()
									,Arrays.toString(li.getOn_points().toArray(new Integer[0]))
								)
							);
						}

						return Flux.from(statement.execute())
							.flatMap(result -> result.map((row, rowMetadata) -> row.get("id", Short.class)));
					})
					.map(id -> {
						System.out.println("inserted led intervals, id = " + id);

						return id;
					})
					.collectList()
					.doOnNext(q ->
						answ.add(
							"SAVEACTIVEGROUP_INSERT_LEDINTERVALS"
							,"Led intervals insert count: " + q.size()
						)
					)
					.map(q -> r);
			})
			//Save LED's slot number and code
			.flatMap(r -> {
				if (tmpLeds.size() == 0)
					return Mono.just(r);

				return template.getDatabaseClient().inConnectionMany(connection -> {
						var statement = connection.createStatement("""
								update led set
									slot_number = $2
									,code = $3
								where
									id = $1
									and slot_number != $2
									and code != coalesce($3, chr(7))"""
							)
							.returnGeneratedValues("id");

						for (int i = tmpLeds.size(), j = 0; i > 0; --i, j++) {
							var l = tmpLeds.get(j);

							statement
								.bind(0, l.getId())
								.bind(1, l.getSlot_number())
								.bind(2, l.getCode());

							// for the last item, do not call `add`
							if (i > 1)
								statement.add();

							System.out.println(
								String.format("%d led id = %d, slot_number = %d, code = %s"
									,j
									,l.getId()
									,l.getSlot_number()
									,l.getCode()
								)
							);
						}

						return Flux.from(statement.execute())
							.flatMap(result -> result.map((row, rowMetadata) -> row.get("id", Short.class)));
					})
					.map(id -> {
						System.out.println("updated led, id = " + id);

						return id;
					})
					.collectList()
					.doOnNext(q ->
						answ.add(
							"SAVEACTIVEGROUP_UPDATE_LEDS"
							,"Update LED's slot number and code, count: " + q.size()
						)
					)
					.map(q -> r);
			})
			//Upsert sensor intervals
			.flatMap(r ->
				template.getDatabaseClient().sql("""
						insert into sensor_intervals(group_id, time_points)
							values ($1, public.sort($2))
							on conflict (group_id)
							do update set time_points = public.sort($2)
							returning id, array_length(time_points, 1) l"""
					)
					/*.filter((statement, executeFunction) -> statement
						.returnGeneratedValues("id")
						.returnGeneratedValues("array_length(time_points, 1)")
						.execute())*/
					.bind("$1", r)
					.bind("$2", tmpGroup.getSensor_intervals().getTime_points().toArray(new Integer[0]))
					.map(row -> {
						short id	= row.get("id", Short.class);
						byte l		= row.get("l", Byte.class);

						tmpGroup.getSensor_intervals().setId(id);

						System.out.println("@@@@sensor_intervals.id = " + id
							+ ", time point quantity = " + l
						);

						answ.add(
							"SAVEACTIVEGROUP_UPSERT_SENSORINTERVALS"
							,"Sensor intervals have been upserted"
						);

						return r;
					})
					.one()
			)
			.doOnSuccess(r -> activeGroupId.set(r, tmpGroup.getSensor_intervals_quantity()))
			.map(r -> answ)
			.as(transactionalOperator::transactional)
			.doOnError(e -> {
				System.out.println(e);
				e.printStackTrace();

				answ.addError(
					"SAVEACTIVEGROUP_COMMON_ERR"
					, "Error occurred while saving the data: " + e.getMessage()
				);

				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			})
			.onErrorReturn(answ);
	}

	public Flux<SensorData>  getHistoricalSensorData(LocalDateTime fromDate) {
		return template.getDatabaseClient().sql("""
			select d.id, r.sensor_id, d.sensor_description, d.intervals_group, d.x, d.y, d.z, d.error, d.status_register
				,d.sensor_interval_number
				,array_length(si.time_points, 1) l
			from sensor_data d
				inner join sensor_description sd on sd.id = d.sensor_description
				inner join sensor_resolution r on r.id = sd.sensor_resolution
				inner join sensor_intervals si on si.group_id = d.intervals_group
			where
				d.insert_time >= $1
			order by
				d.id""")
			.bind("$1", fromDate)
			.map(row -> new SensorData(
				row.get("id", Long.class)
				,row.get("sensor_id", Short.class)
				,row.get("sensor_description", Short.class)
				,row.get("intervals_group", Short.class)
				,row.get("x", Short.class)
				,row.get("y", Short.class)
				,row.get("z", Short.class)
				,row.get("error", Short.class)
				,row.get("status_register", Short.class)
				,row.get("sensor_interval_number", Short.class)
				,row.get("l", Byte.class)
			))
			.all();
	}

	public byte getOnSensorsPointNumber() {
		return activeGroupId.getOnSensorsPointNumber();
	}
}
