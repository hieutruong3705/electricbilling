package com.phenikaa.electricbilling.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.phenikaa.electricbilling.domain.Bill;
import com.phenikaa.electricbilling.domain.BillStatus;
import com.phenikaa.electricbilling.domain.Customer;
import com.phenikaa.electricbilling.domain.CustomerStatus;
import com.phenikaa.electricbilling.domain.MeterReading;
import com.phenikaa.electricbilling.domain.NotificationLog;
import com.phenikaa.electricbilling.domain.NotificationStatus;
import com.phenikaa.electricbilling.domain.NotificationType;
import com.phenikaa.electricbilling.domain.Payment;
import com.phenikaa.electricbilling.domain.PaymentMethod;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

/** Điều kiện tìm kiếm/lọc cho các màn hình danh sách (UC09). Tham số null/rỗng = không lọc. */
public final class SearchSpecs {

	/** Bộ lọc trạng thái hóa đơn hiển thị (loại trừ nhau). */
	public static final String STATE_UNPAID = "UNPAID";
	public static final String STATE_OVERDUE = "OVERDUE";
	public static final String STATE_PAID = "PAID";

	private SearchSpecs() {
	}

	public static Specification<Customer> customers(String keyword, CustomerStatus status) {
		return (root, query, cb) -> {
			List<Predicate> p = new ArrayList<>();
			if (hasText(keyword)) {
				p.add(cb.or(
						like(cb, root.get("customerCode"), keyword),
						like(cb, root.get("fullName"), keyword),
						like(cb, root.get("meterNumber"), keyword),
						like(cb, root.get("phone"), keyword),
						like(cb, root.get("email"), keyword)));
			}
			if (status != null) {
				p.add(cb.equal(root.get("status"), status));
			}
			return cb.and(p.toArray(new Predicate[0]));
		};
	}

	public static Specification<MeterReading> readings(String keyword, YearMonth period) {
		return (root, query, cb) -> {
			List<Predicate> p = new ArrayList<>();
			if (hasText(keyword)) {
				Join<MeterReading, Customer> c = root.join("customer");
				p.add(customerKeyword(cb, c, keyword));
			}
			if (period != null) {
				p.add(cb.equal(root.get("periodYear"), period.getYear()));
				p.add(cb.equal(root.get("periodMonth"), period.getMonthValue()));
			}
			return cb.and(p.toArray(new Predicate[0]));
		};
	}

	/**
	 * @param customerId chỉ lấy hóa đơn của hộ này (null = tất cả)
	 * @param state      UNPAID (chưa thanh toán còn hạn) / OVERDUE / PAID, rỗng = tất cả
	 */
	public static Specification<Bill> bills(Long customerId, String keyword, YearMonth period, String state,
			LocalDate today) {
		return (root, query, cb) -> {
			List<Predicate> p = new ArrayList<>();
			if (customerId != null) {
				p.add(cb.equal(root.get("customer").get("id"), customerId));
			}
			if (hasText(keyword)) {
				Join<Bill, Customer> c = root.join("customer");
				p.add(cb.or(like(cb, root.get("billNo"), keyword), customerKeyword(cb, c, keyword)));
			}
			if (period != null) {
				p.add(cb.equal(root.get("periodYear"), period.getYear()));
				p.add(cb.equal(root.get("periodMonth"), period.getMonthValue()));
			}
			if (STATE_PAID.equals(state)) {
				p.add(cb.equal(root.get("status"), BillStatus.PAID));
			} else if (STATE_UNPAID.equals(state)) {
				p.add(cb.equal(root.get("status"), BillStatus.UNPAID));
				p.add(cb.greaterThanOrEqualTo(root.get("dueDate"), today));
			} else if (STATE_OVERDUE.equals(state)) {
				p.add(cb.equal(root.get("status"), BillStatus.UNPAID));
				p.add(cb.lessThan(root.get("dueDate"), today));
			}
			return cb.and(p.toArray(new Predicate[0]));
		};
	}

	public static Specification<Payment> payments(String keyword, PaymentMethod method) {
		return (root, query, cb) -> {
			List<Predicate> p = new ArrayList<>();
			if (hasText(keyword)) {
				Join<Payment, Bill> b = root.join("bill");
				Join<Bill, Customer> c = b.join("customer");
				p.add(cb.or(like(cb, root.get("referenceCode"), keyword), like(cb, b.get("billNo"), keyword),
						customerKeyword(cb, c, keyword)));
			}
			if (method != null) {
				p.add(cb.equal(root.get("method"), method));
			}
			return cb.and(p.toArray(new Predicate[0]));
		};
	}

	public static Specification<NotificationLog> notifications(NotificationType type, NotificationStatus status) {
		return (root, query, cb) -> {
			List<Predicate> p = new ArrayList<>();
			if (type != null) {
				p.add(cb.equal(root.get("type"), type));
			}
			if (status != null) {
				p.add(cb.equal(root.get("status"), status));
			}
			return cb.and(p.toArray(new Predicate[0]));
		};
	}

	private static Predicate customerKeyword(CriteriaBuilder cb, Join<?, Customer> c, String keyword) {
		return cb.or(
				like(cb, c.get("customerCode"), keyword),
				like(cb, c.get("fullName"), keyword),
				like(cb, c.get("meterNumber"), keyword));
	}

	/** LIKE không phân biệt hoa thường, thoát ký tự đặc biệt % _ \ của người dùng. */
	private static Predicate like(CriteriaBuilder cb, Expression<String> field, String keyword) {
		String escaped = keyword.trim().toLowerCase()
				.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return cb.like(cb.lower(field), "%" + escaped + "%", '\\');
	}

	private static boolean hasText(String s) {
		return s != null && !s.isBlank();
	}
}
