package com.phenikaa.electricbilling.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordForm {

	@NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
	private String currentPassword;

	@NotBlank(message = "Vui lòng nhập mật khẩu mới")
	@Pattern(regexp = "^$|^(?=.*[A-Za-z])(?=.*\\d).{8,100}$",
			message = "Mật khẩu tối thiểu 8 ký tự, gồm ít nhất 1 chữ và 1 số")
	private String newPassword;

	@NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
	private String confirmPassword;
}
