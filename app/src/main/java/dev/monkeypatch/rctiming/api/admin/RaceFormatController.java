package dev.monkeypatch.rctiming.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.monkeypatch.rctiming.api.admin.dto.CreateRaceFormatTemplateRequest;
import dev.monkeypatch.rctiming.api.admin.dto.RaceFormatTemplateDto;
import dev.monkeypatch.rctiming.domain.format.RaceFormatConfig;
import dev.monkeypatch.rctiming.domain.format.RaceFormatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/formats")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class RaceFormatController {

    private static final ObjectMapper YAML_MAPPER =
            new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    private final RaceFormatService raceFormatService;
    private final ObjectMapper jsonObjectMapper;

    public RaceFormatController(RaceFormatService raceFormatService,
                                 ObjectMapper jsonObjectMapper) {
        this.raceFormatService = raceFormatService;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @GetMapping
    public List<RaceFormatTemplateDto> listFormats() {
        return raceFormatService.findAll().stream()
                .map(RaceFormatTemplateDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public RaceFormatTemplateDto getFormat(@PathVariable Long id) {
        return RaceFormatTemplateDto.from(raceFormatService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RaceFormatTemplateDto createFormat(@RequestBody @Valid CreateRaceFormatTemplateRequest request) {
        return RaceFormatTemplateDto.from(
                raceFormatService.create(request.name(), request.config()));
    }

    @PutMapping("/{id}")
    public RaceFormatTemplateDto updateFormat(@PathVariable Long id,
                                               @RequestBody @Valid CreateRaceFormatTemplateRequest request) {
        return RaceFormatTemplateDto.from(
                raceFormatService.update(id, request.name(), request.config()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFormat(@PathVariable Long id) {
        raceFormatService.delete(id);
    }

    @GetMapping(value = "/{id}/export",
                produces = {MediaType.APPLICATION_JSON_VALUE, "application/yaml"})
    public ResponseEntity<String> exportFormat(
            @PathVariable Long id,
            @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept) {
        try {
            RaceFormatConfig config = raceFormatService.exportConfig(id);
            ObjectMapper mapper = accept.contains("yaml") ? YAML_MAPPER : jsonObjectMapper;
            String output = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
            String contentType = accept.contains("yaml") ? "application/yaml" : MediaType.APPLICATION_JSON_VALUE;
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(output);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export format config", e);
        }
    }

    @PostMapping(value = "/import",
                 consumes = {MediaType.APPLICATION_JSON_VALUE, "application/yaml"})
    @ResponseStatus(HttpStatus.CREATED)
    public RaceFormatTemplateDto importFormat(
            @RequestParam(defaultValue = "Imported template") String name,
            @RequestBody String body,
            @RequestHeader("Content-Type") String contentType) {
        try {
            ObjectMapper mapper = contentType.contains("yaml") ? YAML_MAPPER : jsonObjectMapper;
            RaceFormatConfig config = mapper.readValue(body, RaceFormatConfig.class);
            return RaceFormatTemplateDto.from(raceFormatService.importConfig(name, config));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse format config: " + e.getMessage(), e);
        }
    }
}
