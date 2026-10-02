package com.phenikaa.electricbilling.dto;

import com.phenikaa.electricbilling.domain.Customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Form quản trị viên sửa thông tin hộ (UC08). */
@Getter
@Setter
public class CustomerEditForm {

	@NotBlank(message = "Vui lòng nhập họ tên")
	@Size(min = 2, max = 100, message = "Họ tên từ 2 đến 100 ký tự")
	private String fullName;

	@NotBlank(message = "Vui lòng nhập địa chỉ")
	@Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
	private String address;

	@NotBlank(message = "Vui lòng nhập số điện thoại")
	@Pattern(regexp = "^$|^0\\d{9}$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
	private String phone;

	@NotBlank(message = "Vui lòng nhập email")
	@Email(message = "Email không đúng định dạng")
	@Size(max = 100, message = "Email tối đa 100 ký tự")
	private String email;

	@NotBlank(message = "Vui lòng nhập số công tơ")
	@Pattern(regexp = "^$|^[A-Za-z0-9]{6,15}$", message = "Số công tơ gồm 6–15 ký tự chữ hoặc số")
	private String meterNumber;

	@NotNull(message = "Vui lòng nhập chỉ số ban đầu")
	@Min(value = 0, message = "Chỉ số ban đầu phải từ 0")
	@Max(value = 9_999_999, message = "Chỉ số ban đầu tối đa 9.999.999")
	private Integer initialReading;

	public static CustomerEditForm of(Customer c) {
		CustomerEditForm f = new CustomerEditForm();
		f.setFullName(c.getFullName());
		f.setAddress(c.getAddress());
		f.setPhone(c.getPhone());
		f.setEmail(c.getEmail());
		f.setMeterNumber(c.getMeterNumber());
		f.setInitialReading(c.getInitialReading());
		return f;
	}
}
