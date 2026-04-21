package com.logic.DTO;

import com.logic.entity.enums.Gender;
import lombok.Data;
import org.apache.catalina.User;

@Data
public class GuestDTO {
    private Long id;
    private User user;
    private String name;
    private Gender gender;
    private Integer age;
}
