package by.losik.userservice.mapping;

import by.losik.userservice.dto.CreateUserDTO;
import by.losik.userservice.dto.UpdateUserDTO;
import by.losik.userservice.dto.UserDTO;
import by.losik.userservice.entity.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserDTO toDTO(User user);

    User toEntity(UserDTO userDTO);

    @Mapping(target = "password", ignore = true)
    User toEntity(CreateUserDTO createUserDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "password", ignore = true)
    void updateUserFromDTO(UpdateUserDTO updateUserDTO, @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "password", ignore = true)
    void updateUserFromDTO(UserDTO userDTO, @MappingTarget User user);

    @AfterMapping
    default void afterMapping(@MappingTarget User user) {
        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }
    }
}