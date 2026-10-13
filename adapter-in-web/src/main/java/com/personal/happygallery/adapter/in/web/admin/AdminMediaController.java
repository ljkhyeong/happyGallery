package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.ImageUploadResponse;
import com.personal.happygallery.adapter.in.web.config.OpenApiSecuritySchemes;
import com.personal.happygallery.adapter.in.web.media.ImageHttp;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController {

    private final ImageMediaUseCase imageMediaUseCase;

    public AdminMediaController(ImageMediaUseCase imageMediaUseCase) {
        this.imageMediaUseCase = imageMediaUseCase;
    }

    @PostMapping(path = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "uploadImage",
            requestBody = @RequestBody(required = true),
            security = {
                    @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER),
                    @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_API_KEY)
            })
    public ImageUploadResponse uploadImage(@RequestPart("file") MultipartFile file) {
        return ImageUploadResponse.from(
                imageMediaUseCase.upload(ImageHttp.read(file), file.getContentType()));
    }

    @GetMapping("/images/{fileName}")
    @Operation(
            operationId = "getAdminImage",
            security = @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER),
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "image/jpeg",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(
                                    mediaType = "image/png",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(
                                    mediaType = "image/webp",
                                    schema = @Schema(type = "string", format = "binary"))
                    }))
    public ResponseEntity<byte[]> getImage(
            @PathVariable String fileName,
            @AuthenticationPrincipal AdminPrincipal admin) {
        admin.requireBearerAdminUserId();
        ImageMediaUseCase.ImageContent image = imageMediaUseCase.get(fileName);
        return ImageHttp.noStore(image);
    }
}
