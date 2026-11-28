package mx.uv.proyectos;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos")
@Tag(name = "Proyectos", description = "Seguimiento de proyectos estudiantiles (PR05)")
public class ProyectoController {
    private final ProyectoService servicio;

    public ProyectoController(ProyectoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista los proyectos, opcionalmente filtrados por estado")
    @ApiResponse(responseCode = "200", description = "Lista de proyectos")
    @ApiResponse(responseCode = "400", description = "Estado de filtro inválido")
    public List<ProyectoResponse> listar(@RequestParam(required = false) Estado estado) {
        return servicio.listar(estado).stream().map(ProyectoResponse::new).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un proyecto por id")
    @ApiResponse(responseCode = "200", description = "Proyecto encontrado")
    @ApiResponse(responseCode = "400", description = "Id inválido")
    @ApiResponse(responseCode = "404", description = "Proyecto inexistente")
    public ProyectoResponse obtener(@PathVariable Long id) {
        return new ProyectoResponse(servicio.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Registra un proyecto (inicia en REGISTRADO)")
    @ApiResponse(responseCode = "201", description = "Proyecto creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "Título duplicado")
    public ResponseEntity<ProyectoResponse> crear(@Valid @RequestBody ProyectoRequest request) {
        Proyecto creado = servicio.crear(request);
        return ResponseEntity.created(URI.create("/api/proyectos/" + creado.getId()))
            .body(new ProyectoResponse(creado));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado de un proyecto respetando las transiciones permitidas")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Estado ausente o desconocido")
    @ApiResponse(responseCode = "404", description = "Proyecto inexistente")
    @ApiResponse(responseCode = "409", description = "Transición no permitida")
    public ProyectoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return new ProyectoResponse(servicio.cambiarEstado(id, request.getEstado()));
    }
}
