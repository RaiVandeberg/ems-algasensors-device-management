package com.algaworks.algasensors.device.management.api.controller;
import com.algaworks.algasensors.device.management.api.client.SensorMonitoringClient;
import com.algaworks.algasensors.device.management.api.model.SensorDetailOutput;
import com.algaworks.algasensors.device.management.api.model.SensorInput;
import com.algaworks.algasensors.device.management.api.model.SensorMonitoringOutput;
import com.algaworks.algasensors.device.management.api.model.SensorOutput;
import com.algaworks.algasensors.device.management.common.IdGenerator;
import com.algaworks.algasensors.device.management.domain.model.Sensor;
import com.algaworks.algasensors.device.management.domain.model.SensorId;
import com.algaworks.algasensors.device.management.domain.repository.SensorRepository;
import io.hypersistence.tsid.TSID;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
@Validated
public class SensorController {

    private final SensorRepository sensorRepository;
    private final SensorMonitoringClient sensorMonitoringClient;

    @GetMapping
    public Page<SensorOutput> search(@PageableDefault Pageable pageable){
        Page<Sensor> sensors = sensorRepository.findAll(pageable);
        return sensors.map(this::convertToModel);

    }

    @GetMapping("/{sensorId}")
    public SensorOutput getOne(@PathVariable TSID sensorId) {
        Sensor sensor = getSensorDB(sensorId);
            return convertToModel(sensor);
    }

    @GetMapping("/{sensorId}/detail")
    public SensorDetailOutput getOneWithDetail(@PathVariable TSID sensorId) {
        Sensor sensor = getSensorDB(sensorId);
        SensorMonitoringOutput monitoringOutput = sensorMonitoringClient.getDetail(sensorId);
        SensorOutput sensorOutput = convertToModel(sensor);
        return SensorDetailOutput.builder()
                .monitoring(monitoringOutput)
                .sensor(sensorOutput)
                .build();
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SensorOutput create(@RequestBody SensorInput input) {
        Sensor sensor = Sensor.builder()
                .id(new SensorId(IdGenerator.generateTSID()))
                .name(input.getName())
                .ip(input.getIp())
                .location(input.getLocation())
                .protocol(input.getProtocol())
                .model(input.getModel())
                .enabled(false)
                .build();
         sensor = sensorRepository.saveAndFlush(sensor);


        return convertToModel(sensor);
    }

    @PutMapping("/{sensorId}")
    @ResponseStatus(HttpStatus.OK)
    public SensorOutput update(@PathVariable @NotNull TSID sensorId, @RequestBody @Validated SensorOutput input){
        Sensor sensor = getSensorDB(sensorId);
        sensor.setName(input.getName());
        sensor.setIp(input.getIp());
        sensor.setLocation(input.getLocation());
        sensor.setProtocol(input.getProtocol());
        sensor.setModel(input.getModel());
        sensor = sensorRepository.saveAndFlush(sensor);
        return convertToModel(sensor);
    }


    @PutMapping("/{sensorId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable @NotNull TSID sensorId) {
        Sensor sensor = getSensorDB(sensorId);
        sensor.setEnabled(true);
        sensorRepository.saveAndFlush(sensor);
        sensorMonitoringClient.enableMonitoring(sensorId);
    }

    @DeleteMapping("/{sensorId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable @NotNull TSID sensorId) {
        Sensor sensor = getSensorDB(sensorId);
        sensor.setEnabled(false);
        sensorRepository.saveAndFlush(sensor);
        sensorMonitoringClient.disableMonitoring(sensorId);
    }

    @DeleteMapping("{sensorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @NotNull TSID sensorId) {
        Sensor sensor = getSensorDB(sensorId);
        sensorRepository.delete(sensor);
        sensorMonitoringClient.disableMonitoring(sensorId);

    }

    private SensorOutput convertToModel(Sensor sensor) {
        return SensorOutput.builder()
                .id(sensor.getId().getValue())
                .name(sensor.getName())
                .ip(sensor.getIp())
                .location(sensor.getLocation())
                .protocol(sensor.getProtocol())
                .model(sensor.getModel())
                .enabled(sensor.getEnabled())
                .build();
    }

    private Sensor getSensorDB(TSID sensorId) {
        return sensorRepository.findById(new SensorId(sensorId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

}
