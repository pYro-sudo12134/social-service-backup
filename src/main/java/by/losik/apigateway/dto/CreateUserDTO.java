package by.losik.apigateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "DTO for creating a new user")
public class CreateUserDTO {

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^\\w+$", message = "Username can only contain letters, numbers and underscores")
    @Schema(description = "Username", example = "john_doe", required = true)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "Email address", example = "user@example.com", required = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password", example = "password123", required = true)
    private String password;

    @Schema(description = "User role", example = "USER")
    private Role userRole = Role.USER;

    @Schema(description = "Is user enabled", example = "true")
    private Boolean enabled = true;
}