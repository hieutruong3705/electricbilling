package com.phenikaa.electricbilling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tham số vận hành dạng khóa – giá trị. */
@Entity
@Table(name = "system_config")
@Getter
@Setter
@NoArgsConstructor
public class SystemConfig {

	@Id
	@Column(name = "config_key", length = 60)
	private String key;

	@Column(name = "config_value", nullable = false, length = 255)
	private String value;

	public SystemConfig(String key, String value) {
		this.key = key;
		this.value = value;
	}
}
