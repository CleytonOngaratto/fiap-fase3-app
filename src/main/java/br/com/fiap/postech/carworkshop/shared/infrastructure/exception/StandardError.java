package br.com.fiap.postech.carworkshop.shared.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardError {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS z", timezone = "UTC")
    private Instant timestamp;
    private Integer status;
    private String error;
    private String path;

    public static StandardError of(Response.Status httpStatus, Exception e, @Context UriInfo uriInfo) {
        return StandardError.builder()
                .timestamp(Instant.now())
                .status(httpStatus.getStatusCode())
                .error(e.getMessage())
                .path(uriInfo.getRequestUri().toString())
                .build();
    }
}
